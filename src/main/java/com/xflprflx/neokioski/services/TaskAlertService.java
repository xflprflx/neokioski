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
/**
 * Serviço responsável por gerenciar os alertas de vencimento e tempo (SLA) das tarefas e disparar eventos assíncronos.
 */
public class TaskAlertService {

    private final TaskRepository taskRepository;
    private final ChatService chatService;
    private final TaskScheduler taskScheduler;
    private final TransactionTemplate transactionTemplate;
    private final com.xflprflx.neokioski.repositories.TaskTemplateRepository taskTemplateRepository;
    private final TaskService taskService;

    // Chave no formato: "{taskId}_{alertId}"
    private final Map<String, ScheduledFuture<?>> scheduledAlerts = new ConcurrentHashMap<>();

    public TaskAlertService(TaskRepository taskRepository, 
                            ChatService chatService, 
                            TaskScheduler taskScheduler, 
                            TransactionTemplate transactionTemplate,
                            com.xflprflx.neokioski.repositories.TaskTemplateRepository taskTemplateRepository,
                            @org.springframework.context.annotation.Lazy TaskService taskService) {
        this.taskRepository = taskRepository;
        this.chatService = chatService;
        this.taskScheduler = taskScheduler;
        this.transactionTemplate = transactionTemplate;
        this.taskTemplateRepository = taskTemplateRepository;
        this.taskService = taskService;
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void scheduleAlertsForTask(Task task) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task == null || task.getId() == null) return;
        
        transactionTemplate.executeWithoutResult(status -> {
            Task freshTask = taskRepository.findById(task.getId()).orElse(null);
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (freshTask == null || freshTask.getTaskTemplate() == null) return;
            
            com.xflprflx.neokioski.models.template.TaskTemplate attachedTemplate = 
                taskTemplateRepository.findById(freshTask.getTaskTemplate().getId()).orElse(null);
                
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (attachedTemplate == null || attachedTemplate.getAlerts() == null) return;
            
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (TaskAlert alert : attachedTemplate.getAlerts()) {
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (freshTask.getTriggeredAlertIds().contains(alert.getId())) continue;

                boolean reachedTarget = freshTask.getTaskEvents().stream()
                        .anyMatch(e -> e.getTaskStepDefinition() != null && 
                                       e.getTaskStepDefinition().getId().equals(alert.getTargetStep().getId()));
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (reachedTarget) continue;

                Instant startTime = null;
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (alert.getStartStep() != null) {
                    java.util.Optional<Instant> startEventTime = freshTask.getTaskEvents().stream()
                            .filter(e -> e.getTaskStepDefinition() != null && 
                                         e.getTaskStepDefinition().getId().equals(alert.getStartStep().getId()))
                            .map(com.xflprflx.neokioski.models.TaskEvent::getTimestamp)
                            .min(java.util.Comparator.naturalOrder());
                            
                    /*
                     * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                     */
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

                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (alertTime.isBefore(Instant.now())) {
                    executeTransactionalTrigger(freshTask.getId(), alert);
                    scheduledAlerts.remove(cacheKey);
                } else {
                    ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                        executeTransactionalTrigger(freshTask.getId(), alert);
                        scheduledAlerts.remove(cacheKey);
                    }, alertTime);
                    
                    ScheduledFuture<?> oldFuture = scheduledAlerts.put(cacheKey, future);
                    /*
                     * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                     */
                    if (oldFuture != null && !oldFuture.isDone()) {
                        oldFuture.cancel(false);
                    }
                }
            }
        });
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void cancelAlertByTargetStep(Task task, TaskStepDefinition step) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task == null || task.getId() == null || step == null) return;
        
        transactionTemplate.executeWithoutResult(status -> {
            Task freshTask = taskRepository.findById(task.getId()).orElse(null);
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (freshTask == null || freshTask.getTaskTemplate() == null) return;
            
            com.xflprflx.neokioski.models.template.TaskTemplate attachedTemplate = 
                taskTemplateRepository.findById(freshTask.getTaskTemplate().getId()).orElse(null);
                
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (attachedTemplate == null || attachedTemplate.getAlerts() == null) return;
            
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (TaskAlert alert : attachedTemplate.getAlerts()) {
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (alert.getTargetStep().getId().equals(step.getId())) {
                    cancelSingleAlert(freshTask, alert);
                }
            }

            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (freshTask.isAlerted()) {
                freshTask.setAlerted(false);
                Task finalSaved = taskRepository.save(freshTask);
                taskService.broadcast(finalSaved.getGroup().getId(), finalSaved);
            }
        });
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void cancelAllAlertsForTask(Task task) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task == null || task.getId() == null) return;
        String prefix = task.getId() + "_";
        
        // Cancele iterando sobre os itens que pertencem a essa tarefa
        scheduledAlerts.keySet().removeIf(cacheKey -> {
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (cacheKey.startsWith(prefix)) {
                ScheduledFuture<?> future = scheduledAlerts.get(cacheKey);
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (future != null && !future.isDone()) {
                    future.cancel(false);
                }
                /*
                 * Retorna o resultado final processado pelo método.
                 */
                return true;
            }
            /*
             * Retorna o resultado final processado pelo método.
             */
            return false;
        });

        transactionTemplate.executeWithoutResult(status -> {
            Task freshTask = taskRepository.findById(task.getId()).orElse(null);
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (freshTask != null && freshTask.isAlerted()) {
                freshTask.setAlerted(false);
                Task finalSaved = taskRepository.save(freshTask);
                taskService.broadcast(finalSaved.getGroup().getId(), finalSaved);
            }
        });
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private void cancelSingleAlert(Task task, TaskAlert alert) {
        String cacheKey = getCacheKey(task, alert);
        ScheduledFuture<?> future = scheduledAlerts.remove(cacheKey);
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private void executeTransactionalTrigger(Long taskId, TaskAlert alert) {
        transactionTemplate.executeWithoutResult(status -> {
            Task freshTask = taskRepository.findById(taskId).orElse(null);
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (freshTask != null && !freshTask.getTriggeredAlertIds().contains(alert.getId())) {
                
                // Double check target reach inside memory just in case
                boolean reachedTarget = freshTask.getTaskEvents().stream()
                        .anyMatch(e -> e.getTaskStepDefinition() != null && 
                                       e.getTaskStepDefinition().getId().equals(alert.getTargetStep().getId()));
                
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (!reachedTarget) {
                    triggerAlertInternal(freshTask, alert);
                }
            }
        });
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private void triggerAlertInternal(Task task, TaskAlert alert) {
        String message = alert.getAlertMessage();
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (message == null || message.isBlank()) {
            message = "ALERTA: A tarefa '" + task.getTaskTemplate().getName() + "' está pendente há mais de " + alert.getTimeoutMinutes() + " minutos!";
        }

        List<com.xflprflx.neokioski.models.User> authorizers = task.getTaskTemplate().getAuthorizingUsers();
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (authorizers != null && !authorizers.isEmpty()) {
            String authorizersMention = authorizers.stream()
                .map(u -> "@" + u.getUsername())
                .collect(java.util.stream.Collectors.joining(" "));
            
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (message.contains("@autorizadores")) {
                message = message.replace("@autorizadores", authorizersMention);
            } else {
                message = message + " - Atenção: " + authorizersMention;
            }
        }

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task.getCreator() != null) {
            String authorMention = "@" + task.getCreator().getUsername();
            message = message.replace("@criador", authorMention);
        }

        String alertRolesStr = alert.getAlertRoles() != null ? alert.getAlertRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(",")) : null;
        chatService.sendSystemAlertForTask(message, task, alert.getAlertSound(), alertRolesStr);
        
        task.setAlerted(true);
        task.getTriggeredAlertIds().add(alert.getId());
        Task saved = taskRepository.save(task);
        taskService.broadcast(saved.getGroup().getId(), saved);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private String getCacheKey(Task task, TaskAlert alert) {
        /*
         * Retorna o resultado final processado pelo método.
         */
        return task.getId() + "_" + alert.getId();
    }
}
