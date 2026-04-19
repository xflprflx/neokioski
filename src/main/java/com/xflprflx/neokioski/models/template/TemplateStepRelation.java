package com.xflprflx.neokioski.models.template;


import com.xflprflx.neokioski.models.enums.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

@Entity
@Table(name = "tb_template_step_relations")
public class TemplateStepRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "step_order", nullable = false)
    private int order;
    @Column(nullable = false)
    private boolean isFinalizer;
    @Column(nullable = false, name = "send_notification")
    private boolean sendNotification;
    @Column(nullable = false)
    private boolean shouldPulse;
    @Column(columnDefinition = "TEXT", name = "notification_message")
    private String notificationMessage;
    @Column(nullable = false)
    private boolean isGlobal = false;
    @Column(nullable = false)
    private boolean hasResponseField = false;
    @Column(nullable = false)
    private boolean isResponseRequired = false;

    @Column(nullable = true)
    private String responseFieldLabel = "Resposta / Comentário";

    @Column(nullable = false)
    private boolean responseIsCompact = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_input_type")
    private InputType responseInputType = InputType.TEXT;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "template_step_relation_id")
    @OrderBy("id ASC")
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private List<TaskFieldOption> responseOptions = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_template_id")
    private TaskTemplate taskTemplate;
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_step_definition_id")
    private TaskStepDefinition taskStepDefinition;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_template_step_next_allowed",
            joinColumns = @JoinColumn(name = "template_step_relation_id"),
            inverseJoinColumns = @JoinColumn(name = "next_step_definition_id")
    )
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private List<TaskStepDefinition> allowedNextSteps = new ArrayList<>();

    private String alertSound;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsible_role")
    private Role responsibleRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "tb_template_step_relation_alert_roles", joinColumns = @JoinColumn(name = "relation_id"))
    @Column(name = "role")
    @BatchSize(size = 20)
    private Set<Role> alertRoles = new HashSet<>();

    public TemplateStepRelation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isFinalizer() {
        return isFinalizer;
    }

    public void setFinalizer(boolean finalizer) {
        isFinalizer = finalizer;
    }

    public boolean isSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(boolean sendNotification) {
        this.sendNotification = sendNotification;
    }

    public String getNotificationMessage() {
        return notificationMessage;
    }

    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }

    public TaskTemplate getTaskTemplate() {
        return taskTemplate;
    }

    public void setTaskTemplate(TaskTemplate taskTemplate) {
        this.taskTemplate = taskTemplate;
    }

    public TaskStepDefinition getTaskStepDefinition() {
        return taskStepDefinition;
    }

    public void setTaskStepDefinition(TaskStepDefinition taskStepDefinition) {
        this.taskStepDefinition = taskStepDefinition;
    }

    public List<TaskStepDefinition> getAllowedNextSteps() {
        return allowedNextSteps;
    }

    public void setAllowedNextSteps(List<TaskStepDefinition> allowedNextSteps) {
        this.allowedNextSteps = allowedNextSteps;
    }


    public boolean isShouldPulse() {
        return shouldPulse;
    }

    public void setShouldPulse(boolean shouldPulse) {
        this.shouldPulse = shouldPulse;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    public void setGlobal(boolean global) {
        isGlobal = global;
    }

    public boolean isHasResponseField() {
        return hasResponseField;
    }

    public void setHasResponseField(boolean hasResponseField) {
        this.hasResponseField = hasResponseField;
    }

    public boolean isResponseRequired() {
        return isResponseRequired;
    }

    public void setResponseRequired(boolean responseRequired) {
        isResponseRequired = responseRequired;
    }

    public String getResponseFieldLabel() {
        return responseFieldLabel;
    }

    public void setResponseFieldLabel(String responseFieldLabel) {
        this.responseFieldLabel = responseFieldLabel;
    }

    public boolean isResponseIsCompact() {
        return responseIsCompact;
    }

    public void setResponseIsCompact(boolean responseIsCompact) {
        this.responseIsCompact = responseIsCompact;
    }

    public InputType getResponseInputType() {
        return responseInputType;
    }

    public void setResponseInputType(InputType responseInputType) {
        this.responseInputType = responseInputType;
    }

    public List<TaskFieldOption> getResponseOptions() {
        return responseOptions;
    }

    public void setResponseOptions(List<TaskFieldOption> responseOptions) {
        this.responseOptions = responseOptions;
    }

    public String getAlertSound() {
        return alertSound;
    }

    public void setAlertSound(String alertSound) {
        this.alertSound = alertSound;
    }

    public Role getResponsibleRole() {
        return responsibleRole;
    }

    public void setResponsibleRole(Role responsibleRole) {
        this.responsibleRole = responsibleRole;
    }

    public Set<Role> getAlertRoles() {
        return alertRoles;
    }

    public void setAlertRoles(Set<Role> alertRoles) {
        this.alertRoles = alertRoles;
    }

    public void addResponseOption(TaskFieldOption option) {
        this.responseOptions.add(option);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TemplateStepRelation that = (TemplateStepRelation) o;
        return order == that.order && isFinalizer == that.isFinalizer && 
               sendNotification == that.sendNotification && 
               shouldPulse == that.shouldPulse &&
               isGlobal == that.isGlobal &&
               hasResponseField == that.hasResponseField &&
               isResponseRequired == that.isResponseRequired &&
               responseIsCompact == that.responseIsCompact &&
               Objects.equals(id, that.id) && 
               Objects.equals(notificationMessage, that.notificationMessage) && 
               Objects.equals(taskTemplate, that.taskTemplate) && 
               Objects.equals(taskStepDefinition, that.taskStepDefinition) &&
               responseInputType == that.responseInputType &&
               Objects.equals(responseOptions, that.responseOptions) &&
               Objects.equals(responseFieldLabel, that.responseFieldLabel) &&
               responsibleRole == that.responsibleRole;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, order, isFinalizer, sendNotification, shouldPulse, isGlobal, 
                            hasResponseField, isResponseRequired, responseIsCompact,
                            getNotificationMessage(), 
                            taskTemplate, taskStepDefinition, responseFieldLabel, responsibleRole);
    }

}
