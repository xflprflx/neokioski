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
                            @org.springframework.context.annotation.Lazy ChatService chatService,
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
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void scheduleAlertsForTask(Task task) {
        if (task == null || task.getId() == null) return;

        // Se já estamos dentro de uma transação (ex: chamado do executeStep),
        // reutilizar a task recebida em vez de recarregar do banco.
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            scheduleAlertsInternal(task);
        } else {
            // #improve_n+1 - Usar findByIdForExecution para trazer task + coleções via EntityGraph em query única
            transactionTemplate.executeWithoutResult(status -> {
                Task freshTask = taskRepository.findByIdForExecution(task.getId()).orElse(null);
                if (freshTask == null || freshTask.getTaskTemplate() == null) return;
                scheduleAlertsInternal(freshTask);
            });
        }
    }

    private void scheduleAlertsInternal(Task task) {
        com.xflprflx.neokioski.models.template.TaskTemplate template = task.getTaskTemplate();

        // Tentar usar os alerts já carregados; se não estiverem inicializados, buscar do repo
        java.util.Collection<TaskAlert> alerts;
        try {
            alerts = template.getAlerts();
            if (alerts == null) return;
            alerts.size(); // forçar inicialização se lazy
        } catch (org.hibernate.LazyInitializationException e) {
            com.xflprflx.neokioski.models.template.TaskTemplate attachedTemplate =
                    taskTemplateRepository.findByIdWithAlerts(template.getId()).orElse(null);
            if (attachedTemplate == null || attachedTemplate.getAlerts() == null) return;
            alerts = attachedTemplate.getAlerts();
        }

        for (TaskAlert alert : alerts) {
            if (task.getTriggeredAlertIds().contains(alert.getId())) continue;

            boolean reachedTarget = task.getTaskEvents().stream()
                    .anyMatch(e -> e.getTaskStepDefinition() != null &&
                            e.getTaskStepDefinition().getId().equals(alert.getTargetStep().getId()));
            if (reachedTarget) continue;

            Instant startTime = null;
            if (alert.getStartStep() != null) {
                java.util.Optional<Instant> startEventTime = task.getTaskEvents().stream()
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
                startTime = task.getCreatedAt() != null ? task.getCreatedAt() : Instant.now();
            }

            long timeoutValue = alert.getTimeoutValue();
            Instant alertTime = startTime.plus(timeoutValue, alert.getTimeoutUnit().toChronoUnit());

            String cacheKey = getCacheKey(task, alert);

            if (alertTime.isBefore(Instant.now())) {
                executeTransactionalTrigger(task.getId(), alert);
                scheduledAlerts.remove(cacheKey);
            } else {
                ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                    executeTransactionalTrigger(task.getId(), alert);
                    scheduledAlerts.remove(cacheKey);
                }, alertTime);

                ScheduledFuture<?> oldFuture = scheduledAlerts.put(cacheKey, future);
                if (oldFuture != null && !oldFuture.isDone()) {
                    oldFuture.cancel(false);
                }
            }
        }
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void cancelAlertByTargetStep(Task task, TaskStepDefinition step) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task == null || task.getId() == null || step == null) return;

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            cancelAlertByTargetStepInternal(task, step);
        } else {
            // #improve_n+1 - Usar findByIdForExecution para evitar cascata de SELECTs lazy
            transactionTemplate.executeWithoutResult(status -> {
                Task freshTask = taskRepository.findByIdForExecution(task.getId()).orElse(null);
                if (freshTask == null || freshTask.getTaskTemplate() == null) return;
                cancelAlertByTargetStepInternal(freshTask, step);
            });
        }
    }

    private void cancelAlertByTargetStepInternal(Task task, TaskStepDefinition step) {
        com.xflprflx.neokioski.models.template.TaskTemplate template = task.getTaskTemplate();

        java.util.Collection<TaskAlert> alerts;
        try {
            alerts = template.getAlerts();
            if (alerts == null) return;
            alerts.size();
        } catch (org.hibernate.LazyInitializationException e) {
            com.xflprflx.neokioski.models.template.TaskTemplate attachedTemplate =
                    taskTemplateRepository.findById(template.getId()).orElse(null);
            if (attachedTemplate == null || attachedTemplate.getAlerts() == null) return;
            alerts = attachedTemplate.getAlerts();
        }

        for (TaskAlert alert : alerts) {
            if (alert.getTargetStep().getId().equals(step.getId())) {
                cancelSingleAlert(task, alert);
            }
        }

        if (task.isAlerted()) {
            task.setAlerted(false);
            Task finalSaved = taskRepository.save(task);
            taskService.broadcast(finalSaved.getGroup().getId(), finalSaved);
        }
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
                 * Retorna o resultado final processado pelo métodx.
                 */
                return true;
            }
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return false;
        });

        // #improve_n+1 - Usar findByIdForExecution para garantir que group/template estejam carregados para o broadcast
        transactionTemplate.executeWithoutResult(status -> {
            Task freshTask = taskRepository.findByIdForExecution(task.getId()).orElse(null);
            if (freshTask != null && freshTask.isAlerted()) {
                freshTask.setAlerted(false);
                Task finalSaved = taskRepository.save(freshTask);
                taskService.broadcast(finalSaved.getGroup().getId(), finalSaved);
            }
        });
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Executa o disparo do alerta dentro de uma transação própria.
     *
     * OTIMIZAÇÃO N+1:
     * Usa findByIdForExecution (EntityGraph completo) para trazer task + coleções
     * numa única query, evitando os ~11 SELECTs extras que ocorriam com findById.
     * Além disso, pré-carrega o template com findByIdWithMetadata para garantir que
     * authorizingUsers, mentionsMappings e alerts estejam disponíveis em memória
     * para o triggerAlertInternal sem queries adicionais.
     */
    private void executeTransactionalTrigger(Long taskId, TaskAlert alert) {
        transactionTemplate.executeWithoutResult(status -> {
            // #improve_n+1 - EntityGraph completo para evitar cascata de SELECTs lazy
            Task freshTask = taskRepository.findByIdForExecution(taskId).orElse(null);
            if (freshTask == null) return;

            // #improve_n+1 - Pré-carregar o template com metadados completos (authorizingUsers, alerts, mentionsMappings)
            // para que triggerAlertInternal e initializeTaskData não disparem queries adicionais
            if (freshTask.getTaskTemplate() != null) {
                taskTemplateRepository.findByIdWithMetadata(freshTask.getTaskTemplate().getId())
                        .ifPresent(freshTask::setTaskTemplate);
            }

            if (!freshTask.getTriggeredAlertIds().contains(alert.getId())) {
                // Double check: verificar se a tarefa já alcançou o passo-alvo (em memória, sem query)
                boolean reachedTarget = freshTask.getTaskEvents().stream()
                        .anyMatch(e -> e.getTaskStepDefinition() != null &&
                                e.getTaskStepDefinition().getId().equals(alert.getTargetStep().getId()));

                if (!reachedTarget) {
                    triggerAlertInternal(freshTask, alert);
                }
            }
        });
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private void triggerAlertInternal(Task task, TaskAlert alert) {
        String message = alert.getAlertMessage();
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (message == null || message.isBlank()) {
            message = "ALERTA: A tarefa '" + task.getTaskTemplate().getName() + "' está pendente há mais de " +
                    alert.getTimeoutValue() + " " + alert.getTimeoutUnit().getLabel().toLowerCase() + "!";
        }

        // Resolver menções customizadas (mapeamento por grupo)
        message = com.xflprflx.neokioski.utils.TaskMentionUtils.resolveCustomMentions(message, task);

        java.util.Collection<com.xflprflx.neokioski.models.User> authorizers = task.getTaskTemplate().getAuthorizingUsers();
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
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private String getCacheKey(Task task, TaskAlert alert) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return task.getId() + "_" + alert.getId();
    }
}
