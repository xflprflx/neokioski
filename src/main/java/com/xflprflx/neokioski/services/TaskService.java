package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.repositories.*;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
/**
 * Serviço principal para gerenciar o ciclo de vida completo de uma tarefa (criação, edição, atualização de passos e deleção).
 */
public class TaskService {

    private final TaskRepository taskRepository;
    private final TemplateStepRelationRepository stepRelationRepository;
    private final ChatService chatService;
    private final ChatMessageRepository chatMessageRepository;
    private final TaskAlertService taskAlertService;
    private final TaskTemplateRepository taskTemplateRepository;
    private final TaskFieldDefinitionRepository taskFieldDefinitionRepository;
    private final TaskStepDefinitionRepository taskStepDefinitionRepository;
    private final TaskSlaService taskSlaService;

    private final java.util.Map<Long, List<Consumer<Task>>> listeners =
            Collections.synchronizedMap(new java.util.HashMap<>());
    private final List<Consumer<Task>> globalListeners = new CopyOnWriteArrayList<>();

    public TaskService(TaskRepository taskRepository,
                       TemplateStepRelationRepository stepRelationRepository,
                       @org.springframework.context.annotation.Lazy ChatService chatService,
                       ChatMessageRepository chatMessageRepository,
                       @org.springframework.context.annotation.Lazy TaskAlertService taskAlertService,
                       TaskTemplateRepository taskTemplateRepository,
                       TaskFieldDefinitionRepository taskFieldDefinitionRepository,
                       TaskStepDefinitionRepository taskStepDefinitionRepository,
                       TaskSlaService taskSlaService) {
        this.taskRepository = taskRepository;
        this.stepRelationRepository = stepRelationRepository;
        this.chatService = chatService;
        this.chatMessageRepository = chatMessageRepository;
        this.taskAlertService = taskAlertService;
        this.taskTemplateRepository = taskTemplateRepository;
        this.taskFieldDefinitionRepository = taskFieldDefinitionRepository;
        this.taskStepDefinitionRepository = taskStepDefinitionRepository;
        this.taskSlaService = taskSlaService;
    }

    public Optional<com.xflprflx.neokioski.models.enums.Role> getResponsibleRole(Task task) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task.getCurrentStep() == null) {
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return Optional.ofNullable(task.getTaskTemplate().getDefaultResponsibleRole());
        }
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(
                task.getTaskTemplate().getId(), task.getCurrentStep().getId()
        ).map(TemplateStepRelation::getResponsibleRole);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    @Transactional
    public Task createTask(TaskTemplate template,
                           Map<TaskFieldDefinition, String> fieldValues,
                           User creator,
                           Group group) {
        // #improve_n+1 - Recarregar o template com metadados completos para garantir que ele esteja atachado e pronto
        TaskTemplate attachedTemplate = taskTemplateRepository.findByIdWithMetadata(template.getId())
                .orElseThrow(() -> new RuntimeException("Template não encontrado: " + template.getId()));

        Task task = new Task();
        task.setTaskTemplate(attachedTemplate);
        task.setCreator(creator);
        task.setGroup(group);
        task.setCreatedAt(Instant.now());

        // Definir o primeiro step não finalizador como currentStep
        // Salvar valores dos campos
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (Map.Entry<TaskFieldDefinition, String> entry : fieldValues.entrySet()) {
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
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
        // #improve_n+1 - Deep touch para garantir que a task transmitida esteja 100% pronta para a UI
        initializeTaskData(saved);
        taskAlertService.scheduleAlertsForTask(saved);
        broadcast(group.getId(), saved);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return saved;
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public Task executeStep(Long taskId, TaskStepDefinition step, User user, String response) {
        Task task = taskRepository.findByIdWithCollections(taskId).orElseThrow(() -> new RuntimeException("Task não encontrada: " + taskId));

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
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
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (relationOpt.isPresent()) {
                isFinalizerNow = relationOpt.get().isFinalizer();
            } else {
                String label = step.getLabel();
                isFinalizerNow = stepRelationRepository.findByTaskTemplateIdOrderByOrderAsc(task.getTaskTemplate().getId())
                        .stream()
                        .filter(r -> r.getTaskStepDefinition().getLabel().equals(label))
                        .anyMatch(TemplateStepRelation::isFinalizer);
            }

            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (isFinalizerNow && task.getFinalizedAt() == null) {
                task.setFinalizedAt(Instant.now());
                taskSlaService.calculateFinalTaskSla(task);
            }

            taskSlaService.calculateAndSaveSla(task, step);

            Task saved = taskRepository.save(task);

            // Verificar se deve processar menções e disparar sons
            stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(
                    task.getTaskTemplate().getId(), step.getId()
            ).ifPresent(relation -> {
                // Buscar a mensagem original do card da tarefa para dar "Reply"
                List<ChatMessage> msgs = chatMessageRepository.findByTaskOrderByIdAsc(task);
                ChatMessage originalMsg = msgs.isEmpty() ? null : msgs.get(0);
                
                String stepSound = relation.getAlertSound();

                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (relation.isSendNotification() && relation.getNotificationMessage() != null && !relation.getNotificationMessage().isEmpty()) {
                    String message = relation.getNotificationMessage();

                    // 1. Processar @criador
                    /*
                     * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                     */
                    if (task.getCreator() != null) {
                        String creatorMention = "@" + task.getCreator().getUsername();
                        message = message.replace("@criador", creatorMention);
                    }

                    // 2. Processar @autorizadores
                    java.util.Collection<User> authorizers = task.getTaskTemplate().getAuthorizingUsers();
                    /*
                     * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                     */
                    if (authorizers != null && !authorizers.isEmpty()) {
                        String authorizersMention = authorizers.stream()
                            .map(u -> "@" + u.getUsername())
                            .collect(java.util.stream.Collectors.joining(" "));
                        message = message.replace("@autorizadores", authorizersMention);
                    }

                    // 3. Processar @usuario (quem está executando a ação agora)
                    /*
                     * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                     */
                    if (user != null) {
                        message = message.replace("@usuario", "@" + user.getUsername());
                    }

                    // 4. Processar @resposta
                    message = message.replace("@resposta", response != null ? response : "");

                    // 5. Processar @autor_anterior
                    List<TaskEvent> events = task.getTaskEvents().stream()
                            .sorted(java.util.Comparator.comparing(TaskEvent::getTimestamp))
                            .toList();
                            
                    /*
                     * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                     */
                    if (events.size() >= 2) {
                        User previousAuthor = events.get(events.size() - 2).getUser();
                        /*
                         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                         */
                        if (previousAuthor != null) {
                            message = message.replace("@autor_anterior", "@" + previousAuthor.getUsername());
                        }
                    } else {
                        // Se não há autor anterior, remove a tag para não ficar visível
                        message = message.replace("@autor_anterior", "");
                    }
                    
                    String rolesStr = relation.getAlertRoles() != null ? relation.getAlertRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")) : null;
                    chatService.sendVolatileMessage(message, user, task.getGroup(), originalMsg, stepSound, rolesStr);
                }
            });

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (isTaskFinalized(saved)) {
            taskAlertService.cancelAllAlertsForTask(saved);
        } else {
            taskAlertService.cancelAlertByTargetStep(saved, step);
            taskAlertService.scheduleAlertsForTask(saved);
        }
        initializeTaskData(saved);
        broadcast(task.getGroup().getId(), saved);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return saved;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public Optional<Task> findById(Long id) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return taskRepository.findById(id);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<Task> getTasksByGroup(Group group) {
        List<Task> tasks = taskRepository.findByGroupWithCollections(group);
        tasks.forEach(this::initializeTaskData);
        return tasks;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<Task> getTasksByGroup(Group group, Instant start, Instant end) {
        List<Task> tasks = taskRepository.findByGroupAndCreatedAtBetweenOrderByCreatedAtDesc(group, start, end);
        tasks.forEach(this::initializeTaskData);
        return tasks;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<Task> getMyTasks(Group group, User user) {
        List<Task> tasks = taskRepository.findMyTasksByGroup(group, user);
        tasks.forEach(this::initializeTaskData);
        return tasks;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<Task> getPendingTasks(Group group) {
        List<Task> tasks = taskRepository.findPendingByGroup(group);
        tasks.forEach(this::initializeTaskData);
        return tasks;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public long countActiveTasks() {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return taskRepository.countAllPending();
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public long countActiveTasksByGroup(Group group) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return taskRepository.countPendingByGroup(group);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public long countActiveTasksByGroups(List<Group> groups) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (groups == null || groups.isEmpty()) return 0;
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return taskRepository.countPendingByGroups(groups);
    }

    public Map<String, Long> getTaskDistributionByGroup(User user, GroupService groupService) {
        List<Object[]> results;
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (user.getRole() == com.xflprflx.neokioski.models.enums.Role.ADMIN) {
            results = taskRepository.countTasksByGroup();
        } else {
            List<Group> userGroups = groupService.findActiveGroupsByUser(user);
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (userGroups.isEmpty()) return Collections.emptyMap();
            results = taskRepository.countTasksByGroupIn(userGroups);
        }
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return results.stream().collect(Collectors.toMap(
                r -> (String) r[0],
                r -> (Long) r[1]
        ));
    }

    public Map<String, Long> getTaskActivityLast7Days(User user, GroupService groupService) {
        java.time.Instant start = java.time.Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        List<Object[]> results;
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (user.getRole() == com.xflprflx.neokioski.models.enums.Role.ADMIN) {
            results = taskRepository.countTasksByDay(start);
        } else {
            List<Group> userGroups = groupService.findActiveGroupsByUser(user);
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (userGroups.isEmpty()) return Collections.emptyMap();
            results = taskRepository.countTasksByDayAndGroups(start, userGroups);
        }
        
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return results.stream().collect(Collectors.toMap(
                r -> r[0].toString(),
                r -> (Long) r[1],
                (v1, v2) -> v1,
                TreeMap::new
        ));
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TaskStepDefinition> getAllowedNextSteps(Task task) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (isTaskFinalized(task)) return Collections.emptyList();

        // Otimização: Tentar usar a coleção já carregada no template para evitar N+1 no broadcast
        java.util.Collection<TemplateStepRelation> allRels;
        if (task.getTaskTemplate() != null && Hibernate.isInitialized(task.getTaskTemplate().getTemplateStepRelation())) {
            allRels = task.getTaskTemplate().getTemplateStepRelation();
        } else {
            allRels = stepRelationRepository.findByTaskTemplateIdOrderByOrderAsc(task.getTaskTemplate().getId());
        }
        
        List<TaskStepDefinition> globalSteps = allRels.stream()
                .filter(TemplateStepRelation::isGlobal)
                .map(TemplateStepRelation::getTaskStepDefinition)
                .collect(Collectors.toList());

        // Regra de Autorização
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task.getTaskTemplate().isRequiresSignature()) {
            if (task.getCurrentStep() == null) {
                // Se ainda não autorizado, apenas AUTORIZADO e REJEITADO são permitidos
                return allRels.stream()
                        .map(TemplateStepRelation::getTaskStepDefinition)
                        .filter(s -> s.getLabel().equalsIgnoreCase("AUTORIZADO") || s.getLabel().equalsIgnoreCase("REJEITADO"))
                        .collect(Collectors.toList());
            }
            // Se já foi autorizado ou rejeitado, o código segue o fluxo normal abaixo para encontrar os próximos passos do workflow
        }

        // Identificar passos que são destinos de alguma transição (restritos)
        // #fix - Passos globais não devem restringir pontos de entrada
        Set<Long> restrictedStepIds = allRels.stream()
                .filter(r -> !r.isGlobal())
                .flatMap(r -> r.getAllowedNextSteps().stream())
                .map(TaskStepDefinition::getId)
                .collect(Collectors.toSet());

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task.getCurrentStep() == null) {
            // Se pendente, mostrar todos os passos que não são destino de ninguém (entry points) + globais
            List<TaskStepDefinition> entrySteps = allRels.stream()
                    .filter(r -> !r.isGlobal() && !restrictedStepIds.contains(r.getTaskStepDefinition().getId()))
                    .map(TemplateStepRelation::getTaskStepDefinition)
                    .collect(Collectors.toList());

            List<TaskStepDefinition> result = new ArrayList<>(entrySteps);
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (TaskStepDefinition gs : globalSteps) {
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (!result.contains(gs)) result.add(gs);
            }
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return result;
        }

        Optional<TemplateStepRelation> currentRelOpt = allRels.stream()
                .filter(r -> r.getTaskStepDefinition().getId().equals(task.getCurrentStep().getId()))
                .findFirst();

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (currentRelOpt.isPresent()) {
            TemplateStepRelation rel = currentRelOpt.get();
            java.util.Collection<TaskStepDefinition> nextSteps = rel.getAllowedNextSteps();
            
            List<TaskStepDefinition> result;
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
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
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (task.getTaskTemplate().isRequiresSignature()) {
                result = result.stream()
                        .filter(s -> !s.getLabel().equalsIgnoreCase("AUTORIZADO") && !s.getLabel().equalsIgnoreCase("REJEITADO"))
                        .collect(Collectors.toList());
            }

            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (TaskStepDefinition gs : globalSteps) {
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (!result.contains(gs) && !gs.getId().equals(task.getCurrentStep().getId())) {
                    result.add(gs);
                }
            }
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return result;
        }

        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return globalSteps;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void register(Long groupId, Consumer<Task> listener) {
        listeners.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void unregister(Long groupId, Consumer<Task> listener) {
        List<Consumer<Task>> groupListeners = listeners.get(groupId);
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (groupListeners != null) groupListeners.remove(listener);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void registerGlobal(Consumer<Task> listener) {
        globalListeners.add(listener);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void unregisterGlobal(Consumer<Task> listener) {
        globalListeners.remove(listener);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public boolean isTaskFinalized(Task task) {
        if (task.getFinalizedAt() != null) return true;
        if (task.getCurrentStep() == null) return false;
        
        // Usar a coleção já carregada do template para evitar query N+1
        return task.getTaskTemplate().getTemplateStepRelation().stream()
                .filter(r -> r.getTaskStepDefinition().getId().equals(task.getCurrentStep().getId()))
                .findFirst()
                .map(TemplateStepRelation::isFinalizer)
                .orElse(false);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public Instant getFinalizedTimestamp(Task task) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task.getFinalizedAt() != null) return task.getFinalizedAt();
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (!isTaskFinalized(task)) return null;
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return task.getTaskEvents().stream()
                .filter(ev -> ev.getTaskStepDefinition() != null)
                .max(Comparator.comparing(TaskEvent::getTimestamp))
                .map(TaskEvent::getTimestamp)
                .orElse(task.getCreatedAt());
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public boolean shouldTaskPulse(Task task) {
        if (task.getCurrentStep() == null) return false;

        // Usar a coleção já carregada do template para evitar query N+1
        return task.getTaskTemplate().getTemplateStepRelation().stream()
                .filter(r -> r.getTaskStepDefinition().getId().equals(task.getCurrentStep().getId()))
                .findFirst()
                .map(TemplateStepRelation::isShouldPulse)
                .orElse(false);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public boolean isTaskPending(Task task) {
        // Uma tarefa é pendente se não tem step OU se o step atual não é finalizador
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return task.getCurrentStep() == null || !isTaskFinalized(task);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public boolean canUserViewTask(Task task, User user) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (user == null) return false;
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (user.getRole() == com.xflprflx.neokioski.models.enums.Role.ADMIN) return true;

        TaskTemplate template = task.getTaskTemplate();
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (template.isRequiresSignature() && task.getCurrentStep() == null) {
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return template.getAuthorizingUsers().stream()
                    .anyMatch(u -> u.getId().equals(user.getId()));
        }
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return true;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public boolean canUserExecuteStep(Task task, User user) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (user == null) return false;

        TaskTemplate template = task.getTaskTemplate();
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (template.isRequiresSignature() && task.getCurrentStep() == null) {
            // Na fase de autorização, apenas quem está na lista pode agir
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return template.getAuthorizingUsers().stream()
                    .anyMatch(u -> u.getId().equals(user.getId()));
        }

        // Caso padrão (já autorizado ou sem requerimento): No NeoKioski quem vê o grupo pode agir.
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return true;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void broadcast(Long groupId, Task task) {
        Runnable doBroadcast = () -> {
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
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doBroadcast.run();
                }
            });
        } else {
            doBroadcast.run();
        }
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<StoreReportEntry> getStoreReportData(Instant start, Instant end) {
        List<Task> tasks = taskRepository.findAllByCreatedAtBetween(start, end);

        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return tasks.stream()
                .filter(t -> t.getTaskTemplate().isRegistraNoRelatorioLoja())
                .filter(this::isTaskFinalized)
                .filter(t -> t.getCurrentStep() != null && t.getCurrentStep().getLabel().equalsIgnoreCase("COLETADO"))
                .map(this::mapToStoreReportEntry)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StoreReportEntry::collectedAt))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<String> getAvailableReportAttributes() {
        Set<String> attributes = new TreeSet<>();
        
        // Atributos base
        attributes.add("ID");
        attributes.add("TEMPLATE");
        attributes.add("CREATED_AT");
        attributes.add("COLLECTED_AT");
        attributes.add("CREATOR");
        attributes.add("COLLECTOR");

        // Buscar templates que registram no relatório para sugerir seus campos e passos
        List<TaskTemplate> templates = taskTemplateRepository.findAll().stream()
                .filter(TaskTemplate::isRegistraNoRelatorioLoja)
                .collect(Collectors.toList());

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (TaskTemplate template : templates) {
            // Campos (Fields)
            template.getTemplateFieldRelation().forEach(rel -> {
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (rel.getTaskFieldDefinition() != null) {
                    attributes.add("FIELD:" + rel.getTaskFieldDefinition().getLabel());
                }
            });
            // Passos (Steps)
            template.getTemplateStepRelation().forEach(rel -> {
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (rel.getTaskStepDefinition() != null && rel.isHasResponseField()) {
                    attributes.add("STEP:" + rel.getTaskStepDefinition().getLabel());
                }
            });
        }
        
        // Adicionar todos os campos e passos ativos como fallback se a lista estiver pequena
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (attributes.size() <= 6) {
            taskFieldDefinitionRepository.findAll().forEach(f -> attributes.add("FIELD:" + f.getLabel()));
            taskStepDefinitionRepository.findAll().forEach(s -> attributes.add("STEP:" + s.getLabel()));
        }

        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return new ArrayList<>(attributes);
    }
    
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public String getTaskAttributeValue(Task task, String attributeKey) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (attributeKey == null || attributeKey.isBlank()) return "";

        switch (attributeKey.toUpperCase()) {
            case "ID": return String.valueOf(task.getId());
            case "TEMPLATE": return task.getTaskTemplate().getName();
            case "CREATED_AT": return formatInstant(task.getCreatedAt());
            case "COLLECTED_AT": return formatInstant(getCollectedAt(task));
            case "CREATOR": return task.getCreator() != null ? task.getCreator().getName() : "Sistema";
            case "COLLECTOR": return getCollectorName(task);
            default:
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (attributeKey.startsWith("FIELD:")) {
                    String label = attributeKey.substring(6);
                    /*
                     * Retorna o resultado final processado pelo métodx.
                     */
                    return task.getTaskFieldValues().stream()
                            .filter(fv -> fv.getTaskFieldDefinition() != null && 
                                          fv.getTaskFieldDefinition().getLabel().equalsIgnoreCase(label))
                            .map(TaskFieldValue::getValue)
                            .findFirst().orElse("");
                } else if (attributeKey.startsWith("STEP:")) {
                    String label = attributeKey.substring(5);
                    /*
                     * Retorna o resultado final processado pelo métodx.
                     */
                    return task.getTaskEvents().stream()
                            .filter(e -> e.getTaskStepDefinition() != null && 
                                         e.getTaskStepDefinition().getLabel().equalsIgnoreCase(label))
                            .map(TaskEvent::getResponse)
                            .findFirst().orElse("");
                }
                /*
                 * Retorna o resultado final processado pelo métodx.
                 */
                return "";
        }
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private String formatInstant(Instant instant) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (instant == null) return "";
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(instant);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private Instant getCollectedAt(Task task) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return task.getTaskEvents().stream()
                .filter(e -> e.getTaskStepDefinition() != null && e.getTaskStepDefinition().getLabel().equalsIgnoreCase("COLETADO"))
                .map(TaskEvent::getTimestamp)
                .findFirst().orElse(null);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private String getCollectorName(Task task) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return task.getTaskEvents().stream()
                .filter(e -> e.getTaskStepDefinition() != null && e.getTaskStepDefinition().getLabel().equalsIgnoreCase("COLETADO"))
                .map(e -> e.getUser() != null ? e.getUser().getName() : "Sistema")
                .findFirst().orElse("");
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private StoreReportEntry mapToStoreReportEntry(Task task) {
        String delivery = "";
        User collector = null;
        Instant collectedAt = null;

        // Buscar o número da Delivery e quem coletou
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (TaskEvent event : task.getTaskEvents()) {
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (event.getTaskStepDefinition() != null) {
                // Verificar se este step é o de coleta
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (event.getTaskStepDefinition().getLabel().equalsIgnoreCase("COLETADO")) {
                    collector = event.getUser();
                    collectedAt = event.getTimestamp();
                }

                // Tentar encontrar a "DELIVERY" nas respostas dos passos
                Optional<TemplateStepRelation> rel = stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(
                        task.getTaskTemplate().getId(), event.getTaskStepDefinition().getId()
                );

                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (rel.isPresent() && rel.get().getResponseFieldLabel() != null
                        && rel.get().getResponseFieldLabel().toUpperCase().contains("DELIVERY")) {
                    /*
                     * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                     */
                    if (event.getResponse() != null && !event.getResponse().isBlank()) {
                        delivery = event.getResponse();
                    }
                }
            }
        }

        // Se não achou no step response, procurar nos campos iniciais (TaskFieldValue)
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (delivery.isEmpty()) {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (TaskFieldValue fv : task.getTaskFieldValues()) {
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (fv.getTaskFieldDefinition() != null &&
                        fv.getTaskFieldDefinition().getLabel().toUpperCase().contains("DELIVERY")) {
                    delivery = fv.getValue();
                    break;
                }
            }
        }

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (collector == null) return null;

        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return new StoreReportEntry(
                task.getId(),
                task.getTaskTemplate().getName(),
                task.getCreatedAt(),
                collectedAt,
                task.getCreator() != null ? task.getCreator().getName() : "Sistema",
                collector.getName(),
                delivery,
                task
        );
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<Task> findTasksWithFilters(Instant start, Instant end, User creator, TaskTemplate template, Group group, String search) {
        Specification<Task> spec = Specification.where((root, query, cb) -> cb.conjunction());

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (start != null && end != null) {
            spec = spec.and((root, query, cb) -> cb.between(root.get("createdAt"), start, end));
        }

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (creator != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("creator"), creator));
        }

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (template != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("taskTemplate"), template));
        }

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (group != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("group"), group));
        }

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (search != null && !search.isBlank()) {
            final String searchPattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                query.distinct(true); // Evitar duplicatas por causa dos joins
                
                // Join com TaskFieldValue
                Join<Task, TaskFieldValue> fieldJoin = root.join("taskFieldValues", JoinType.LEFT);
                Predicate fieldPredicate = cb.like(cb.lower(fieldJoin.get("value")), searchPattern);
                
                // Join com TaskEvent
                Join<Task, TaskEvent> eventJoin = root.join("taskEvents", JoinType.LEFT);
                Predicate eventPredicate = cb.like(cb.lower(eventJoin.get("response")), searchPattern);
                
                /*
                 * Retorna o resultado final processado pelo métodx.
                 */
                return cb.or(fieldPredicate, eventPredicate);
            });
        }

        // #improve_n+1 - EntityGraph no findAll de Specification já adicionado no repository para trazer os Lazy ToOne principais.
        // O subselect continuará lidando com ToMany collections.

        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return taskRepository.findAll(spec, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public record StoreReportEntry(
            Long taskId,
            String templateName,
            Instant createdAt,
            Instant collectedAt,
            String creatorName,
            String collectorName,
            String deliveryNumber,
            Task task
    ) {}
    /**
     * Inicializa profundamente os dados de uma tarefa e seu template associado para uso seguro na interface (Vaadin).
     * Este método deve ser chamado dentro de uma transação ativa.
     */
    @Transactional(readOnly = true)
    public void initializeTaskData(Task task) {
        if (task == null) return;

        // #improve_n+1 - Removido o recarregamento manual individual (findByIdWithMetadata).
        // Agora confiamos no @BatchSize anotado nos modelos, o que permite ao Hibernate carregar
        // os metadados de TODOS os templates de uma só vez quando o primeiro 'Deep Touch' ocorrer abaixo.
        
        // Tocar profundamente no template associado para garantir que sub-coleções Lazy (como allowedNextSteps)
        // também sejam inicializadas via Batch Loading.
        if (task.getTaskTemplate() != null) {
            task.getTaskTemplate().getTemplateStepRelation().forEach(rel -> {
                // Tocar no passo e suas propriedades básicas
                if (rel.getTaskStepDefinition() != null) {
                    rel.getTaskStepDefinition().getLabel();
                    rel.getTaskStepDefinition().getColorTheme();
                }
                
                // Tocar nos passos permitidos seguintes (inicializa via BatchSize)
                rel.getAllowedNextSteps().forEach(ans -> {
                    if (ans != null) ans.getLabel();
                });

                // Tocar nas opções de resposta (inicializa via BatchSize)
                rel.getResponseOptions().forEach(opt -> {
                    if (opt != null) opt.getOptionLabel();
                });
                
                // Tocar nos papéis de alerta do passo
                rel.getAlertRoles().size();
            });
            task.getTaskTemplate().getAuthorizingUsers().size();
            task.getTaskTemplate().getAlertRoles().size();
        }
        
        // Inicializar resultados de SLA e configurações relacionadas
        task.getSlaResults().forEach(sr -> {
            if (sr.getSlaConfig() != null) sr.getSlaConfig().getName();
        });
        
        // Inicializar histórico de eventos e autores
        task.getTaskEvents().forEach(ev -> {
            if (ev.getUser() != null) ev.getUser().getName();
            if (ev.getTaskStepDefinition() != null) ev.getTaskStepDefinition().getLabel();
        });
        
        // Inicializar valores de campos e definições (dispara Batch Loading de TaskFieldDefinition)
        task.getTaskFieldValues().forEach(fv -> {
            if (fv.getTaskFieldDefinition() != null) {
                fv.getTaskFieldDefinition().getLabel();
                fv.getTaskFieldDefinition().getInputType();
            }
        });
        
        // Inicializar definições de passos percorridos
        if (task.getTaskStepDefinitions() != null) {
            task.getTaskStepDefinitions().forEach(sd -> {
                if (sd != null) sd.getLabel();
            });
        }
        
        // Inicializar o Template e todas as suas regras de transição (crucial para UI de botões)
        if (task.getTaskTemplate() != null) {
            task.getTaskTemplate().getTemplateStepRelation().forEach(rel -> {
                // Tocar no passo e suas propriedades básicas
                if (rel.getTaskStepDefinition() != null) {
                    rel.getTaskStepDefinition().getLabel();
                    rel.getTaskStepDefinition().getColorTheme();
                }
                
                // Tocar nos passos permitidos seguintes
                rel.getAllowedNextSteps().forEach(ans -> {
                    if (ans != null) ans.getLabel();
                });

                // Tocar nas opções de resposta (se houver campo de input)
                rel.getResponseOptions().forEach(opt -> {
                    if (opt != null) opt.getOptionLabel();
                });
                
                // Tocar nos papéis de alerta do passo
                rel.getAlertRoles().size();
            });
            task.getTaskTemplate().getAuthorizingUsers().size();
            task.getTaskTemplate().getAlertRoles().size();
        }
    }
}
