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
    private final java.util.Map<Long, List<Consumer<Task>>> listeners =
            Collections.synchronizedMap(new java.util.HashMap<>());
    private final List<Consumer<Task>> globalListeners = new CopyOnWriteArrayList<>();

    public TaskService(TaskRepository taskRepository,
                       TemplateStepRelationRepository stepRelationRepository,
                       ChatService chatService,
                       ChatMessageRepository chatMessageRepository) {
        this.taskRepository = taskRepository;
        this.stepRelationRepository = stepRelationRepository;
        this.chatService = chatService;
        this.chatMessageRepository = chatMessageRepository;
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
        broadcast(group.getId(), saved);
        return saved;
    }

    @Transactional
    public Task executeStep(Long taskId, TaskStepDefinition step, User user, String response) {
        return taskRepository.findById(taskId).map(task -> {
            task.setCurrentStep(step);

            TaskEvent event = new TaskEvent();
            event.setTaskStepDefinition(step);
            event.setUser(user);
            event.setTimestamp(Instant.now());
            event.setResponse(response);
            task.getTaskEvents().add(event);

            Task saved = taskRepository.save(task);

            // Verificar se deve mencionar o autor ou o ator anterior
            stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(
                    task.getTaskTemplate().getId(), step.getId()
            ).ifPresent(relation -> {
                // Buscar a mensagem original do card da tarefa para dar "Reply"
                ChatMessage originalMsg = chatMessageRepository.findByTask(task).orElse(null);

                // Menção ao Autor
                if (relation.isMentionAuthor() && relation.getMentionMessage() != null && !relation.getMentionMessage().isEmpty()) {
                    String message = relation.getMentionMessage();
                    if (task.getCreator() != null) {
                        message = message.replace("@autor", "@" + task.getCreator().getUsername());
                    }
                    chatService.sendMessage(message, user, task.getGroup(), originalMsg);
                }

                // Menção ao Ator Anterior
                if (relation.isMentionPreviousActor() && relation.getMentionPreviousActorMessage() != null && !relation.getMentionPreviousActorMessage().isEmpty()) {
                    String message = relation.getMentionPreviousActorMessage();
                    List<TaskEvent> events = task.getTaskEvents().stream()
                            .sorted(java.util.Comparator.comparing(TaskEvent::getTimestamp))
                            .toList();
                            
                    if (events.size() >= 2) {
                        User previousActor = events.get(events.size() - 2).getUser();
                        if (previousActor != null) {
                            message = message.replace("@autor_anterior", "@" + previousActor.getUsername());
                            chatService.sendMessage(message, user, task.getGroup(), originalMsg);
                        }
                    }
                }
            });

            broadcast(task.getGroup().getId(), saved);
            return saved;
        }).orElseThrow(() -> new RuntimeException("Task não encontrada: " + taskId));
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getTasksByGroup(Group group) {
        return taskRepository.findByGroupWithCollections(group);
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
                        .collect(Collectors.toList());
            } else {
                result = new ArrayList<>(nextSteps);
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

    private void broadcast(Long groupId, Task task) {
        List<Consumer<Task>> groupListeners = listeners.get(groupId);
        if (groupListeners != null) groupListeners.forEach(l -> l.accept(task));
        globalListeners.forEach(l -> l.accept(task));
    }
}
