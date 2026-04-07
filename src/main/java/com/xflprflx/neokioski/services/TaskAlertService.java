package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.template.TaskAlert;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.repositories.TaskRepository;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class TaskAlertService {

    private final TaskRepository taskRepository;
    private final ChatService chatService;
    private final TaskScheduler taskScheduler;
    private final TransactionTemplate transactionTemplate;
    private final com.xflprflx.neokioski.repositories.TaskTemplateRepository taskTemplateRepository;

    // Chave no formato: "{taskId}_{alertId}"
    private final Map<String, ScheduledFuture<?>> scheduledAlerts = new ConcurrentHashMap<>();

    public TaskAlertService(TaskRepository taskRepository, 
                            ChatService chatService, 
                            TaskScheduler taskScheduler, 
                            TransactionTemplate transactionTemplate,
                            com.xflprflx.neokioski.repositories.TaskTemplateRepository taskTemplateRepository) {
        this.taskRepository = taskRepository;
        this.chatService = chatService;
        this.taskScheduler = taskScheduler;
        this.transactionTemplate = transactionTemplate;
        this.taskTemplateRepository = taskTemplateRepository;
    }

    public void scheduleAlertsForTask(Task task) {
        if (task == null || task.getId() == null) return;
        
        transactionTemplate.executeWithoutResult(status -> {
            Task freshTask = taskRepository.findById(task.getId()).orElse(null);
            if (freshTask == null || freshTask.getTaskTemplate() == null) return;
            
            com.xflprflx.neokioski.models.template.TaskTemplate attachedTemplate = 
                taskTemplateRepository.findById(freshTask.getTaskTemplate().getId()).orElse(null);
                
            if (attachedTemplate == null || attachedTemplate.getAlerts() == null) return;
            
            for (TaskAlert alert : attachedTemplate.getAlerts()) {
                if (freshTask.getTriggeredAlertIds().contains(alert.getId())) continue;

                boolean reachedTarget = freshTask.getTaskEvents().stream()
                        .anyMatch(e -> e.getTaskStepDefinition() != null && 
                                       e.getTaskStepDefinition().getId().equals(alert.getTargetStep().getId()));
                if (reachedTarget) continue;

                Instant startTime = null;
                if (alert.getStartStep() != null) {
                    java.util.Optional<Instant> startEventTime = freshTask.getTaskEvents().stream()
                            .filter(e -> e.getTaskStepDefinition() != null && 
                                         e.getTaskStepDefinition().getId().equals(alert.getStartStep().getId()))
                            .map(com.xflprflx.neokioski.models.TaskEvent::getTimestamp)
                            .min(java.util.Comparator.naturalOrder());
                            
                    if (startEventTime.isPresent()) {
                         startTime = startEventTime.get();
                    } else {
                         // Gatilho condicional de início ainda não alcançado, não agenda alerta agora.
                         continue;
                    }
                } else {
                    startTime = freshTask.getCreatedAt() != null ? freshTask.getCreatedAt() : Instant.now();
                }

                long timeoutMinutes = alert.getTimeoutMinutes();
                Instant alertTime = startTime.plus(timeoutMinutes, ChronoUnit.MINUTES);
                
                String cacheKey = getCacheKey(freshTask, alert);

                if (alertTime.isBefore(Instant.now())) {
                    executeTransactionalTrigger(freshTask.getId(), alert);
                    scheduledAlerts.remove(cacheKey);
                } else {
                    ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                        executeTransactionalTrigger(freshTask.getId(), alert);
                        scheduledAlerts.remove(cacheKey);
                    }, alertTime);
                    
                    ScheduledFuture<?> oldFuture = scheduledAlerts.put(cacheKey, future);
                    if (oldFuture != null && !oldFuture.isDone()) {
                        oldFuture.cancel(false);
                    }
                }
            }
        });
    }

    public void cancelAlertByTargetStep(Task task, TaskStepDefinition step) {
        if (task == null || task.getId() == null || step == null) return;
        
        transactionTemplate.executeWithoutResult(status -> {
            Task freshTask = taskRepository.findById(task.getId()).orElse(null);
            if (freshTask == null || freshTask.getTaskTemplate() == null) return;
            
            com.xflprflx.neokioski.models.template.TaskTemplate attachedTemplate = 
                taskTemplateRepository.findById(freshTask.getTaskTemplate().getId()).orElse(null);
                
            if (attachedTemplate == null || attachedTemplate.getAlerts() == null) return;
            
            for (TaskAlert alert : attachedTemplate.getAlerts()) {
                if (alert.getTargetStep().getId().equals(step.getId())) {
                    cancelSingleAlert(freshTask, alert);
                }
            }
        });
    }

    public void cancelAllAlertsForTask(Task task) {
        if (task == null || task.getId() == null) return;
        String prefix = task.getId() + "_";
        
        // Cancele iterando sobre os itens que pertencem a essa tarefa
        scheduledAlerts.keySet().removeIf(cacheKey -> {
            if (cacheKey.startsWith(prefix)) {
                ScheduledFuture<?> future = scheduledAlerts.get(cacheKey);
                if (future != null && !future.isDone()) {
                    future.cancel(false);
                }
                return true;
            }
            return false;
        });
    }

    private void cancelSingleAlert(Task task, TaskAlert alert) {
        String cacheKey = getCacheKey(task, alert);
        ScheduledFuture<?> future = scheduledAlerts.remove(cacheKey);
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    private void executeTransactionalTrigger(Long taskId, TaskAlert alert) {
        transactionTemplate.executeWithoutResult(status -> {
            Task freshTask = taskRepository.findById(taskId).orElse(null);
            if (freshTask != null && !freshTask.getTriggeredAlertIds().contains(alert.getId())) {
                
                // Double check target reach inside memory just in case
                boolean reachedTarget = freshTask.getTaskEvents().stream()
                        .anyMatch(e -> e.getTaskStepDefinition() != null && 
                                       e.getTaskStepDefinition().getId().equals(alert.getTargetStep().getId()));
                
                if (!reachedTarget) {
                    triggerAlertInternal(freshTask, alert);
                }
            }
        });
    }

    private void triggerAlertInternal(Task task, TaskAlert alert) {
        String message = alert.getAlertMessage();
        if (message == null || message.isBlank()) {
            message = "ALERTA: A tarefa '" + task.getTaskTemplate().getName() + "' está pendente há mais de " + alert.getTimeoutMinutes() + " minutos!";
        }

        List<com.xflprflx.neokioski.models.User> authorizers = task.getTaskTemplate().getAuthorizingUsers();
        if (authorizers != null && !authorizers.isEmpty()) {
            String authorizersMention = authorizers.stream()
                .map(u -> "@" + u.getUsername())
                .collect(java.util.stream.Collectors.joining(" "));
            
            if (message.contains("@autorizadores")) {
                message = message.replace("@autorizadores", authorizersMention);
            } else {
                message = message + " - Atenção: " + authorizersMention;
            }
        }

        if (task.getCreator() != null) {
            String authorMention = "@" + task.getCreator().getUsername();
            message = message.replace("@criador", authorMention);
        }

        chatService.sendSystemAlertForTask(message, task);
        
        task.setAlerted(true);
        task.getTriggeredAlertIds().add(alert.getId());
        taskRepository.save(task);
    }

    private String getCacheKey(Task task, TaskAlert alert) {
        return task.getId() + "_" + alert.getId();
    }
}
