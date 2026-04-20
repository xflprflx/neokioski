package com.xflprflx.neokioski.models.template;

import com.xflprflx.neokioski.models.enums.Role;
import jakarta.persistence.*;
import java.util.Objects;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_task_template_alerts")
/**
 * Entidade que configura alertas automáticos associados às tarefas para avisar sobre vencimentos.
 */
public class TaskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_template_id", nullable = false)
    private TaskTemplate taskTemplate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_step_id", nullable = false)
    private TaskStepDefinition targetStep;

    @ManyToOne
    @JoinColumn(name = "start_step_id")
    private TaskStepDefinition startStep;

    @Column(nullable = false)
    private int timeoutMinutes;

    @Column(columnDefinition = "TEXT")
    private String alertMessage;

    private String alertSound;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "tb_task_alert_roles", joinColumns = @JoinColumn(name = "task_alert_id"))
    @Column(name = "role")
    private Set<Role> alertRoles = new HashSet<>();

    public TaskAlert() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TaskTemplate getTaskTemplate() {
        return taskTemplate;
    }

    public void setTaskTemplate(TaskTemplate taskTemplate) {
        this.taskTemplate = taskTemplate;
    }

    public TaskStepDefinition getTargetStep() {
        return targetStep;
    }

    public void setTargetStep(TaskStepDefinition targetStep) {
        this.targetStep = targetStep;
    }

    public TaskStepDefinition getStartStep() {
        return startStep;
    }

    public void setStartStep(TaskStepDefinition startStep) {
        this.startStep = startStep;
    }

    public int getTimeoutMinutes() {
        return timeoutMinutes;
    }

    public void setTimeoutMinutes(int timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public String getAlertSound() {
        return alertSound;
    }

    public void setAlertSound(String alertSound) {
        this.alertSound = alertSound;
    }

    public Set<Role> getAlertRoles() {
        return alertRoles;
    }

    public void setAlertRoles(Set<Role> alertRoles) {
        this.alertRoles = alertRoles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskAlert taskAlert = (TaskAlert) o;
        return Objects.equals(id, taskAlert.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
