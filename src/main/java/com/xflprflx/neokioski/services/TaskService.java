package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TemplateStepRelationRepository stepRelationRepository;
    private final java.util.Map<Long, List<Consumer<Task>>> listeners =
            Collections.synchronizedMap(new java.util.HashMap<>());

    public TaskService(TaskRepository taskRepository,
                       TemplateStepRelationRepository stepRelationRepository) {
        this.taskRepository = taskRepository;
        this.stepRelationRepository = stepRelationRepository;
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
    public Task executeStep(Long taskId, TaskStepDefinition step, User user) {
        return taskRepository.findById(taskId).map(task -> {
            task.setCurrentStep(step);

            TaskEvent event = new TaskEvent();
            event.setTaskStepDefinition(step);
            event.setUser(user);
            event.setTimestamp(Instant.now());
            task.getTaskEvents().add(event);

            Task saved = taskRepository.save(task);
            broadcast(task.getGroup().getId(), saved);
            return saved;
        }).orElseThrow(() -> new RuntimeException("Task não encontrada: " + taskId));
    }

    public Optional<Task> findById(Long id) {
        return taskRepository.findById(id);
    }

    public List<Task> getTasksByGroup(Group group) {
        return taskRepository.findByGroupOrderByCreatedAtDesc(group);
    }

    public List<Task> getMyTasks(Group group, User user) {
        return taskRepository.findMyTasksByGroup(group, user);
    }

    public List<Task> getPendingTasks(Group group) {
        return taskRepository.findPendingByGroup(group);
    }

    public void register(Long groupId, Consumer<Task> listener) {
        listeners.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unregister(Long groupId, Consumer<Task> listener) {
        List<Consumer<Task>> groupListeners = listeners.get(groupId);
        if (groupListeners != null) groupListeners.remove(listener);
    }

    private void broadcast(Long groupId, Task task) {
        List<Consumer<Task>> groupListeners = listeners.get(groupId);
        if (groupListeners != null) groupListeners.forEach(l -> l.accept(task));
    }
}
