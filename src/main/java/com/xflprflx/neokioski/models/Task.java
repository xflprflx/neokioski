package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "tb_tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant createdAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_template_id", nullable = false)
    private TaskTemplate taskTemplate;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne
    @JoinColumn(name = "current_step_id")
    private TaskStepDefinition currentStep;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "task_id")
    @OrderBy("id ASC")
    private Set<TaskEvent> taskEvents = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_task_task_step_definitions",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "task_step_defintion_id")
    )
    private List<TaskStepDefinition> taskStepDefinitions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    private Set<TaskFieldValue> taskFieldValues = new LinkedHashSet<>();

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

    public List<TaskStepDefinition> getTaskStepDefinitions() {
        return taskStepDefinitions;
    }

    public void setTaskStepDefinitions(List<TaskStepDefinition> taskStepDefinitions) {
        this.taskStepDefinitions = taskStepDefinitions;
    }

    public Set<TaskFieldValue> getTaskFieldValues() {
        return taskFieldValues;
    }

    public void setTaskFieldValues(Set<TaskFieldValue> taskFieldValues) {
        this.taskFieldValues = taskFieldValues;
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
