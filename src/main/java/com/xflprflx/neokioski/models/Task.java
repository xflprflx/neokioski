package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.Instant;
import java.util.*;

/**
 * Entidade que representa uma tarefa no sistema, associada a um template, passos e a um solicitante.
 */
@Entity
@Table(name = "tb_tasks")
@BatchSize(size = 100)
@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "Task.findAllByCreatedAtBetween",
                attributeNodes = {
                        @NamedAttributeNode("creator"),
                        @NamedAttributeNode("group"),
                        @NamedAttributeNode("currentStep"),
                        @NamedAttributeNode(value = "taskTemplate", subgraph = "taskTemplate.full")
                },
                subgraphs = {
                        @NamedSubgraph(
                                name = "taskTemplate.full",
                                attributeNodes = {
                                        @NamedAttributeNode(value = "templateStepRelation", subgraph = "stepRelation.full"),
                                        @NamedAttributeNode(value = "templateFieldRelation", subgraph = "fieldRelation.full")
                                }
                        ),
                        @NamedSubgraph(
                                name = "stepRelation.full",
                                attributeNodes = {
                                        @NamedAttributeNode("taskStepDefinition"),
                                        @NamedAttributeNode("allowedNextSteps")
                                }
                        ),
                        @NamedSubgraph(
                                name = "fieldRelation.full",
                                attributeNodes = {
                                        @NamedAttributeNode("taskFieldDefinition")
                                }
                        )
                }
        )
})
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant createdAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_template_id", nullable = false)
    private TaskTemplate taskTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_step_id")
    private TaskStepDefinition currentStep;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    @OrderBy("id ASC")
    @BatchSize(size = 100)
    private Set<TaskEvent> taskEvents = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tb_task_task_step_definitions",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "task_step_definition_id")
    )
    @BatchSize(size = 100)
    private Set<TaskStepDefinition> taskStepDefinitions = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    @BatchSize(size = 100)
    private Set<TaskFieldValue> taskFieldValues = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "tb_task_triggered_alerts", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "alert_id")
    @BatchSize(size = 100)
    private Set<Long> triggeredAlertIds = new HashSet<>();

    private boolean isAlerted = false;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 100)
    private Set<TaskSlaResult> slaResults = new LinkedHashSet<>();

    public Task() {
        this.createdAt = Instant.now();
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


    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public TaskStepDefinition getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(TaskStepDefinition currentStep) {
        this.currentStep = currentStep;
    }

    public Set<TaskEvent> getTaskEvents() {
        return taskEvents;
    }

    public void setTaskEvents(Set<TaskEvent> taskEvents) {
        this.taskEvents = taskEvents;
    }

    public Set<TaskStepDefinition> getTaskStepDefinitions() {
        return taskStepDefinitions;
    }

    public void setTaskStepDefinitions(Set<TaskStepDefinition> taskStepDefinitions) {
        this.taskStepDefinitions = taskStepDefinitions;
    }

    public Set<TaskFieldValue> getTaskFieldValues() {
        return taskFieldValues;
    }

    public void setTaskFieldValues(Set<TaskFieldValue> taskFieldValues) {
        this.taskFieldValues = taskFieldValues;
    }

    public Set<Long> getTriggeredAlertIds() {
        return triggeredAlertIds;
    }

    public void setTriggeredAlertIds(Set<Long> triggeredAlertIds) {
        this.triggeredAlertIds = triggeredAlertIds;
    }

    public boolean isAlerted() {
        return isAlerted;
    }

    public void setAlerted(boolean isAlerted) {
        this.isAlerted = isAlerted;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(Instant finalizedAt) {
        this.finalizedAt = finalizedAt;
    }

    public Set<TaskSlaResult> getSlaResults() {
        return slaResults;
    }

    public void setSlaResults(Set<TaskSlaResult> slaResults) {
        this.slaResults = slaResults;
    }

    /**
     * Auxiliar de performance para a UI. 
     * Evita chamadas ao serviço para checar o estado básico de conclusão.
     */
    public boolean isFinalized() {
        return finalizedAt != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
