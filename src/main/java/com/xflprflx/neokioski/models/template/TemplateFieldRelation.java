package com.xflprflx.neokioski.models.template;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "tb_template_field_relations")
@org.hibernate.annotations.BatchSize(size = 50)
/**
 * Entidade que mapeia o relacionamento entre um template de tarefa e a definição de um campo de entrada.
 */
public class TemplateFieldRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "field_order", nullable = false)
    private int order;
    @Column(nullable = false)
    private boolean isRequired;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_template_id")
    private TaskTemplate taskTemplate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_field_definition_id")
    private TaskFieldDefinition taskFieldDefinition;

    public TemplateFieldRelation() {
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

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public TaskTemplate getTaskTemplate() {
        return taskTemplate;
    }

    public void setTaskTemplate(TaskTemplate taskTemplate) {
        this.taskTemplate = taskTemplate;
    }

    public TaskFieldDefinition getTaskFieldDefinition() {
        return taskFieldDefinition;
    }

    public void setTaskFieldDefinition(TaskFieldDefinition taskFieldDefinition) {
        this.taskFieldDefinition = taskFieldDefinition;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TemplateFieldRelation that = (TemplateFieldRelation) o;
        return order == that.order && isRequired == that.isRequired && Objects.equals(id, that.id) && Objects.equals(taskTemplate, that.taskTemplate) && Objects.equals(taskFieldDefinition, that.taskFieldDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, order, isRequired, taskTemplate, taskFieldDefinition);
    }
}
