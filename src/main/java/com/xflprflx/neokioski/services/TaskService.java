package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TemplateStepRelationRepository stepRelationRepository;
    private final ChatService chatService;
    private final ChatMessageRepository chatMessageRepository;
    private final TaskAlertService taskAlertService;
    private final java.util.Map<Long, List<Consumer<Task>>> listeners =
            Collections.synchronizedMap(new java.util.HashMap<>());
    private final List<Consumer<Task>> globalListeners = new CopyOnWriteArrayList<>();

    public TaskService(TaskRepository taskRepository,
                       TemplateStepRelationRepository stepRelationRepository,
                       ChatService chatService,
                       ChatMessageRepository chatMessageRepository,
                       TaskAlertService taskAlertService) {
        this.taskRepository = taskRepository;
        this.stepRelationRepository = stepRelationRepository;
        this.chatService = chatService;
        this.chatMessageRepository = chatMessageRepository;
        this.taskAlertService = taskAlertService;
    }

    @Transactional
    public Task createTask(TaskTemplate template,
                           Map<TaskFieldDefinition, String> fieldValues,
                           User creator,
                           Group group) {
        Task task = new Task();
        task.setTaskTemplate(template);
        task.setCreator(creator);
        task.setGroup(group);
        task.setCreatedAt(Instant.now());

        // Definir o primeiro step não finalizador como currentStep
        // Salvar valores dos campos
        for (Map.Entry<TaskFieldDefinition, String> entry : fieldValues.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                TaskFieldValue fv = new TaskFieldValue();
                fv.setTaskFieldDefinition(entry.getKey());
                fv.setValue(entry.getValue());
                task.getTaskFieldValues().add(fv);
            }
        }

        // Registrar evento de criação (sem step — status inicial = Pendente)
        TaskEvent event = new TaskEvent();
        event.setUser(creator);
        event.setTimestamp(Instant.now());
        task.getTaskEvents().add(event);

        Task saved = taskRepository.save(task);
        taskAlertService.scheduleAlertsForTask(saved);

        String alertSound = template.isPlayAlertSound() ? template.getAlertSound() : null;
        String alertRoles = (template.getAlertRoles() != null && !template.getAlertRoles().isEmpty())
                ? template.getAlertRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(","))
                : null;

        chatService.sendSystemAlertForTask("Uma nova tarefa de '" + template.getName() + "' foi criada.", saved, alertSound, alertRoles);
        broadcast(group.getId(), saved);
        return saved;
    }

    @Transactional
    public Task executeStep(Long taskId, TaskStepDefinition step, User user, String response) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task não encontrada: " + taskId));

        if (!canUserExecuteStep(task, user)) {
            throw new RuntimeException("Usuário não autorizado a executar esta ação nesta tarefa.");
        }

        task.setCurrentStep(step);
        task.getTaskStepDefinitions().add(step);

            TaskEvent event = new TaskEvent();
            event.setTaskStepDefinition(step);
            event.setUser(user);
            event.setTimestamp(Instant.now());
            event.setResponse(response);
            task.getTaskEvents().add(event);

            // Determinar se o step a ser salvo é finalizador *agora*:
            boolean isFinalizerNow = false;
            Optional<TemplateStepRelation> relationOpt = stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(
                    task.getTaskTemplate().getId(), step.getId()
            );
            if (relationOpt.isPresent()) {
                isFinalizerNow = relationOpt.get().isFinalizer();
            } else {
                String label = step.getLabel();
                isFinalizerNow = stepRelationRepository.findByTaskTemplateIdOrderByOrderAsc(task.getTaskTemplate().getId())
                        .stream()
                        .filter(r -> r.getTaskStepDefinition().getLabel().equals(label))
                        .anyMatch(TemplateStepRelation::isFinalizer);
            }

            if (isFinalizerNow && task.getFinalizedAt() == null) {
                task.setFinalizedAt(Instant.now());
            }

            Task saved = taskRepository.save(task);

            // Verificar se deve mencionar o autor ou o autor anterior e disparar sons
            stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(
                    task.getTaskTemplate().getId(), step.getId()
            ).ifPresent(relation -> {
                // Buscar a mensagem original do card da tarefa para dar "Reply"
                List<ChatMessage> msgs = chatMessageRepository.findByTaskOrderByIdAsc(task);
                ChatMessage originalMsg = msgs.isEmpty() ? null : msgs.get(0);
                
                boolean soundPlayed = false;
                String stepSound = relation.getAlertSound();

                // Menção ao Autor da Tarefa (Criador)
                if (relation.isMentionAuthor() && relation.getMentionMessage() != null && !relation.getMentionMessage().isEmpty()) {
                    String message = relation.getMentionMessage();
                    if (task.getCreator() != null) {
                        String creatorMention = "@" + task.getCreator().getUsername();
                        message = message.replace("@criador", creatorMention);
                    }

                    // Processar @autorizadores se estiver na mensagem
                    List<User> authorizers = task.getTaskTemplate().getAuthorizingUsers();
                    if (authorizers != null && !authorizers.isEmpty()) {
                        String authorizersMention = authorizers.stream()
                            .map(u -> "@" + u.getUsername())
                            .collect(java.util.stream.Collectors.joining(" "));
                        message = message.replace("@autorizadores", authorizersMention);
                    }

                    // Processar @usuario (quem está executando a ação agora)
                    if (user != null) {
                        message = message.replace("@usuario", "@" + user.getUsername());
                    }

                    // Adicionar variável @resposta
                    message = message.replace("@resposta", response != null ? response : "");
                    
                    String rolesStr = relation.getAlertRoles() != null ? relation.getAlertRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")) : null;
                    chatService.sendMessage(message, user, task.getGroup(), originalMsg, soundPlayed ? null : stepSound, rolesStr);
                    soundPlayed = true;
                }

                // Menção ao Autor Anterior
                if (relation.isMentionPreviousActor() && relation.getMentionPreviousActorMessage() != null && !relation.getMentionPreviousActorMessage().isEmpty()) {
                    String message = relation.getMentionPreviousActorMessage();
                    
                    // Processar @autorizadores se estiver na mensagem
                    List<User> authorizers = task.getTaskTemplate().getAuthorizingUsers();
                    if (authorizers != null && !authorizers.isEmpty()) {
                        String authorizersMention = authorizers.stream()
                            .map(u -> "@" + u.getUsername())
                            .collect(java.util.stream.Collectors.joining(" "));
                        message = message.replace("@autorizadores", authorizersMention);
                    }

                    // Processar @criador / @autor se estiver na mensagem
                    if (task.getCreator() != null) {
                        String creatorMention = "@" + task.getCreator().getUsername();
                        message = message.replace("@criador", creatorMention);
                    }

                    // Processar @usuario (quem está executando a ação agora)
                    if (user != null) {
                        message = message.replace("@usuario", "@" + user.getUsername());
                    }

                    List<TaskEvent> events = task.getTaskEvents().stream()
                            .sorted(java.util.Comparator.comparing(TaskEvent::getTimestamp))
                            .toList();
                            
                    if (events.size() >= 2) {
                        User previousAuthor = events.get(events.size() - 2).getUser();
                        if (previousAuthor != null) {
                            message = message.replace("@autor_anterior", "@" + previousAuthor.getUsername());
                            // Adicionar variável @resposta
                            message = message.replace("@resposta", response != null ? response : "");
                            
                            String rolesStr = relation.getAlertRoles() != null ? relation.getAlertRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")) : null;
                            chatService.sendMessage(message, user, task.getGroup(), originalMsg, soundPlayed ? null : stepSound, rolesStr);
                            soundPlayed = true;
                        }
                    }
                }
                
                // Se existe um som para a etapa mas nenhuma das menções foi enviada, dispara um alerta comum de sistema apenas para tocar o som
                if (!soundPlayed && stepSound != null && !stepSound.isEmpty()) {
                    String rolesStr = relation.getAlertRoles() != null ? relation.getAlertRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")) : null;
                    chatService.sendSystemAlertForTask("A tarefa avançou para a etapa: " + step.getLabel(), saved, stepSound, rolesStr);
                }
            });

        if (isTaskFinalized(saved)) {
            taskAlertService.cancelAllAlertsForTask(saved);
        } else {
            taskAlertService.cancelAlertByTargetStep(saved, step);
            taskAlertService.scheduleAlertsForTask(saved);
        }

        broadcast(task.getGroup().getId(), saved);
        return saved;
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getTasksByGroup(Group group) {
        return taskRepository.findByGroupWithCollections(group);
    }

    public List<Task> getTasksByGroup(Group group, Instant start, Instant end) {
        return taskRepository.findByGroupAndCreatedAtBetweenOrderByCreatedAtDesc(group, start, end);
    }

    public List<Task> getMyTasks(Group group, User user) {
        return taskRepository.findMyTasksByGroup(group, user);
    }

    public List<Task> getPendingTasks(Group group) {
        return taskRepository.findPendingByGroup(group);
    }

    public List<TaskStepDefinition> getAllowedNextSteps(Task task) {
        if (isTaskFinalized(task)) return Collections.emptyList();

        List<TemplateStepRelation> allRels = stepRelationRepository.findByTaskTemplateIdOrderByOrderAsc(task.getTaskTemplate().getId());
        
        List<TaskStepDefinition> globalSteps = allRels.stream()
                .filter(TemplateStepRelation::isGlobal)
                .map(TemplateStepRelation::getTaskStepDefinition)
                .collect(Collectors.toList());

        // Regra de Autorização
        if (task.getTaskTemplate().isRequiresSignature()) {
            if (task.getCurrentStep() == null) {
                // Se ainda não autorizado, apenas AUTORIZADO e REJEITADO são permitidos
                return allRels.stream()
                        .map(TemplateStepRelation::getTaskStepDefinition)
                        .filter(s -> s.getLabel().equalsIgnoreCase("AUTORIZADO") || s.getLabel().equalsIgnoreCase("REJEITADO"))
                        .collect(Collectors.toList());
            } else if (task.getTaskStepDefinitions().size() == 1 && task.getCurrentStep().getLabel().equalsIgnoreCase("AUTORIZADO")) {
                // Se acabou de autorizar (é o primeiro e único step executado), libera os passos reais iniciais
                Set<Long> restrictedStepIds = allRels.stream()
                        .flatMap(r -> r.getAllowedNextSteps().stream())
                        .map(TaskStepDefinition::getId)
                        .collect(Collectors.toSet());

                return allRels.stream()
                        .filter(r -> !r.isGlobal() 
                                     && !restrictedStepIds.contains(r.getTaskStepDefinition().getId())
                                     && !r.getTaskStepDefinition().getLabel().equalsIgnoreCase("AUTORIZADO")
                                     && !r.getTaskStepDefinition().getLabel().equalsIgnoreCase("REJEITADO"))
                        .map(TemplateStepRelation::getTaskStepDefinition)
                        .collect(Collectors.toList());
            }
        }

        // Identificar passos que são destinos de alguma transição (restritos)
        Set<Long> restrictedStepIds = allRels.stream()
                .flatMap(r -> r.getAllowedNextSteps().stream())
                .map(TaskStepDefinition::getId)
                .collect(Collectors.toSet());

        if (task.getCurrentStep() == null) {
            // Se pendente, mostrar todos os passos que não são destino de ninguém (entry points) + globais
            List<TaskStepDefinition> entrySteps = allRels.stream()
                    .filter(r -> !r.isGlobal() && !restrictedStepIds.contains(r.getTaskStepDefinition().getId()))
                    .map(TemplateStepRelation::getTaskStepDefinition)
                    .collect(Collectors.toList());
            
            List<TaskStepDefinition> result = new ArrayList<>(entrySteps);
            for (TaskStepDefinition gs : globalSteps) {
                if (!result.contains(gs)) result.add(gs);
            }
            return result;
        }

        Optional<TemplateStepRelation> currentRelOpt = allRels.stream()
                .filter(r -> r.getTaskStepDefinition().getId().equals(task.getCurrentStep().getId()))
                .findFirst();

        if (currentRelOpt.isPresent()) {
            TemplateStepRelation rel = currentRelOpt.get();
            List<TaskStepDefinition> nextSteps = rel.getAllowedNextSteps();
            
            List<TaskStepDefinition> result;
            if (nextSteps.isEmpty()) {
                result = allRels.stream()
                        .map(TemplateStepRelation::getTaskStepDefinition)
                        .filter(s -> !s.getId().equals(task.getCurrentStep().getId()))
                        .filter(s -> !restrictedStepIds.contains(s.getId()))
                        .collect(Collectors.toList());
            } else {
                result = new ArrayList<>(nextSteps);
            }

            // Se o template requer autorização e já passou do primeiro passo, 
            // removemos AUTORIZADO e REJEITADO de qualquer fluxo subsequente
            if (task.getTaskTemplate().isRequiresSignature()) {
                result = result.stream()
                        .filter(s -> !s.getLabel().equalsIgnoreCase("AUTORIZADO") && !s.getLabel().equalsIgnoreCase("REJEITADO"))
                        .collect(Collectors.toList());
            }

            for (TaskStepDefinition gs : globalSteps) {
                if (!result.contains(gs) && !gs.getId().equals(task.getCurrentStep().getId())) {
                    result.add(gs);
                }
            }
            return result;
        }

        return globalSteps;
    }

    public void register(Long groupId, Consumer<Task> listener) {
        listeners.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unregister(Long groupId, Consumer<Task> listener) {
        List<Consumer<Task>> groupListeners = listeners.get(groupId);
        if (groupListeners != null) groupListeners.remove(listener);
    }

    public void registerGlobal(Consumer<Task> listener) {
        globalListeners.add(listener);
    }

    public void unregisterGlobal(Consumer<Task> listener) {
        globalListeners.remove(listener);
    }

    public boolean isTaskFinalized(Task task) {
        if (task.getFinalizedAt() != null) return true;
        if (task.getCurrentStep() == null) return false;
        
        // Tentar encontrar a relação exata pelo ID do Step
        Optional<TemplateStepRelation> relationOpt = stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(
                task.getTaskTemplate().getId(), task.getCurrentStep().getId()
        );

        if (relationOpt.isPresent()) {
            return relationOpt.get().isFinalizer();
        }

        // Fallback: Se a relação exata não existe (ex: template atualizado), 
        // buscar por Label no template atual
        String currentLabel = task.getCurrentStep().getLabel();
        return stepRelationRepository.findByTaskTemplateIdOrderByOrderAsc(task.getTaskTemplate().getId())
                .stream()
                .filter(r -> r.getTaskStepDefinition().getLabel().equals(currentLabel))
                .anyMatch(TemplateStepRelation::isFinalizer);
    }

    public Instant getFinalizedTimestamp(Task task) {
        if (task.getFinalizedAt() != null) return task.getFinalizedAt();
        if (!isTaskFinalized(task)) return null;
        return task.getTaskEvents().stream()
                .filter(ev -> ev.getTaskStepDefinition() != null)
                .max(Comparator.comparing(TaskEvent::getTimestamp))
                .map(TaskEvent::getTimestamp)
                .orElse(task.getCreatedAt());
    }

    public boolean shouldTaskPulse(Task task) {
        if (task.getCurrentStep() == null) return false;

        Optional<TemplateStepRelation> relationOpt = stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(
                task.getTaskTemplate().getId(), task.getCurrentStep().getId()
        );

        if (relationOpt.isPresent()) {
            return relationOpt.get().isShouldPulse();
        }

        String currentLabel = task.getCurrentStep().getLabel();
        return stepRelationRepository.findByTaskTemplateIdOrderByOrderAsc(task.getTaskTemplate().getId())
                .stream()
                .filter(r -> r.getTaskStepDefinition().getLabel().equals(currentLabel))
                .anyMatch(TemplateStepRelation::isShouldPulse);
    }

    public boolean isTaskPending(Task task) {
        // Uma tarefa é pendente se não tem step OU se o step atual não é finalizador
        return task.getCurrentStep() == null || !isTaskFinalized(task);
    }

    public boolean canUserViewTask(Task task, User user) {
        if (user == null) return false;
        if (user.getRole() == com.xflprflx.neokioski.models.enums.Role.ADMIN) return true;

        TaskTemplate template = task.getTaskTemplate();
        if (template.isRequiresSignature() && task.getCurrentStep() == null) {
            return template.getAuthorizingUsers().contains(user);
        }
        return true;
    }

    public boolean canUserExecuteStep(Task task, User user) {
        if (user == null) return false;

        TaskTemplate template = task.getTaskTemplate();
        if (template.isRequiresSignature() && task.getCurrentStep() == null) {
            // Na fase de autorização, apenas quem está na lista pode agir
            return template.getAuthorizingUsers().contains(user);
        }

        // Caso padrão (já autorizado ou sem requerimento): No NeoKioski quem vê o grupo pode agir.
        return true;
    }

    private void broadcast(Long groupId, Task task) {
        List<Consumer<Task>> groupListeners = listeners.get(groupId);
        if (groupListeners != null) {
            for (Consumer<Task> l : groupListeners) {
                try {
                    l.accept(task);
                } catch (Exception e) {
                    System.err.println("Erro ao notificar listener de Task no grupo " + groupId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        for (Consumer<Task> l : globalListeners) {
            try {
                l.accept(task);
            } catch (Exception e) {
                System.err.println("Erro ao notificar listener global de Task: " + e.getMessage());
            }
        }
    }

    public List<StoreReportEntry> getStoreReportData(Instant start, Instant end) {
        List<Task> tasks = taskRepository.findAllByCreatedAtBetween(start, end);

        return tasks.stream()
                .filter(t -> t.getTaskTemplate().isRegistraNoRelatorioLoja())
                .filter(this::isTaskFinalized)
                .filter(t -> t.getCurrentStep() != null && t.getCurrentStep().getLabel().equalsIgnoreCase("COLETADO"))
                .map(this::mapToStoreReportEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private StoreReportEntry mapToStoreReportEntry(Task task) {
        String delivery = "";
        User collector = null;
        Instant collectedAt = null;

        // Buscar o número da Delivery e quem coletou
        for (TaskEvent event : task.getTaskEvents()) {
            if (event.getTaskStepDefinition() != null) {
                // Verificar se este step é o de coleta
                if (event.getTaskStepDefinition().getLabel().equalsIgnoreCase("COLETADO")) {
                    collector = event.getUser();
                    collectedAt = event.getTimestamp();
                }

                // Tentar encontrar a "DELIVERY" nas respostas dos passos
                Optional<TemplateStepRelation> rel = stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(
                        task.getTaskTemplate().getId(), event.getTaskStepDefinition().getId()
                );

                if (rel.isPresent() && rel.get().getResponseFieldLabel() != null
                        && rel.get().getResponseFieldLabel().toUpperCase().contains("DELIVERY")) {
                    if (event.getResponse() != null && !event.getResponse().isBlank()) {
                        delivery = event.getResponse();
                    }
                }
            }
        }

        // Se não achou no step response, procurar nos campos iniciais (TaskFieldValue)
        if (delivery.isEmpty()) {
            for (TaskFieldValue fv : task.getTaskFieldValues()) {
                if (fv.getTaskFieldDefinition() != null &&
                        fv.getTaskFieldDefinition().getLabel().toUpperCase().contains("DELIVERY")) {
                    delivery = fv.getValue();
                    break;
                }
            }
        }

        if (collector == null) return null;

        return new StoreReportEntry(
                task.getId(),
                task.getTaskTemplate().getName(),
                task.getCreatedAt(),
                collectedAt,
                task.getCreator() != null ? task.getCreator().getName() : "Sistema",
                collector.getName(),
                delivery
        );
    }

    public record StoreReportEntry(
            Long taskId,
            String templateName,
            Instant createdAt,
            Instant collectedAt,
            String creatorName,
            String collectorName,
            String deliveryNumber
    ) {}
}
