package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.repositories.TaskRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AlertReloader {

    private final TaskRepository taskRepository;
    private final TaskAlertService taskAlertService;

    public AlertReloader(TaskRepository taskRepository, TaskAlertService taskAlertService) {
        this.taskRepository = taskRepository;
        this.taskAlertService = taskAlertService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void reloadPendingAlerts() {
        System.out.println(">>> Inicializando o agendador de alertas dinâmicos...");
        List<Task> pendingTasks = taskRepository.findAllPending();
        int count = 0;
        
        for (Task task : pendingTasks) {
            if (task.getTaskTemplate() != null && task.getTaskTemplate().getAlerts() != null && !task.getTaskTemplate().getAlerts().isEmpty()) {
                taskAlertService.scheduleAlertsForTask(task);
                count += task.getTaskTemplate().getAlerts().size();
            }
        }
        
        System.out.println(">>> Foram agendados " + count + " gatilhos em RAM para tarefas pendentes.");
    }
}
