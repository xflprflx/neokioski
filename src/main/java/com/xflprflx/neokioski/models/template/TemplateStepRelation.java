package com.xflprflx.neokioski.models.template;


import jakarta.persistence.*;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_template_id")
    private TaskTemplate taskTemplate;
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_step_definition_id")
    private TaskStepDefinition taskStepDefinition;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TemplateStepRelation that = (TemplateStepRelation) o;
        return order == that.order && isFinalizer == that.isFinalizer && Objects.equals(id, that.id) && Objects.equals(taskTemplate, that.taskTemplate) && Objects.equals(taskStepDefinition, that.taskStepDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, order, isFinalizer, taskTemplate, taskStepDefinition);
    }
}
