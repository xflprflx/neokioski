package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.template.TaskAlert;
import com.xflprflx.neokioski.repositories.TaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TaskAlertService {

    private final TaskRepository taskRepository;
    private final ChatService chatService;

    public TaskAlertService(TaskRepository taskRepository, ChatService chatService) {
        this.taskRepository = taskRepository;
        this.chatService = chatService;
    }

    @Scheduled(fixedRate = 60000) // Rodar a cada 1 minuto
    @Transactional
    public void processAlerts() {
        List<Task> pendingTasks = taskRepository.findAllPending();

        for (Task task : pendingTasks) {
            List<TaskAlert> alerts = task.getTaskTemplate().getAlerts();
            if (alerts == null || alerts.isEmpty()) continue;

            for (TaskAlert alert : alerts) {
                // Se já disparou este alerta, pula
                if (task.getTriggeredAlertIds().contains(alert.getId())) continue;

                // Verificar se o step alvo já foi atingido (olhando taskEvents)
                boolean reachedTarget = task.getTaskEvents().stream()
                        .anyMatch(e -> e.getTaskStepDefinition() != null && 
                                       e.getTaskStepDefinition().getId().equals(alert.getTargetStep().getId()));

                if (reachedTarget) continue;

                // Verificar tempo de tolerância (desde a criação)
                long minutesSinceCreation = Duration.between(task.getCreatedAt(), Instant.now()).toMinutes();
                if (minutesSinceCreation >= alert.getTimeoutMinutes()) {
                    triggerAlert(task, alert);
                }
            }
        }
    }

    private void triggerAlert(Task task, TaskAlert alert) {
        String message = alert.getAlertMessage();
        if (message == null || message.isBlank()) {
            message = "ALERTA: A tarefa '" + task.getTaskTemplate().getName() + "' está pendente há mais de " + alert.getTimeoutMinutes() + " minutos!";
        }

        // 1. Primeiro lidar com @autorizadores (para evitar que @autor faça match na substring)
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

        // 2. Depois lidar com @autor e @criador
        if (task.getCreator() != null) {
            String authorMention = "@" + task.getCreator().getUsername();
            message = message.replace("@autor", authorMention);
            message = message.replace("@criador", authorMention);
        }

        // Enviar para o chat do grupo linkado à tarefa (user = null = mensagem de sistema)
        chatService.sendSystemAlertForTask(message, task);
        
        // Marcar tarefa como alertada e registrar o disparo
        task.setAlerted(true);
        task.getTriggeredAlertIds().add(alert.getId());
        taskRepository.save(task);
    }
}
