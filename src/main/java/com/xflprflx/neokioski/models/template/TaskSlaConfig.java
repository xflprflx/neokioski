package com.xflprflx.neokioski.models.template;

import com.xflprflx.neokioski.models.enums.TaskSlaUnit;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_task_sla_configs")
/**
 * Entidade de configuração que define os parâmetros de limite de tempo (SLA) para os passos de um template de tarefa.
 */
public class TaskSlaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_template_id", nullable = false)
    private TaskTemplate taskTemplate;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "source_step_id")
    private TaskStepDefinition sourceStep;

    @ManyToOne
    @JoinColumn(name = "target_step_id")
    private TaskStepDefinition targetStep;

    @Column(nullable = false)
    private Integer targetValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskSlaUnit unit = TaskSlaUnit.MINUTES;

    @Column(nullable = false)
    private Instant validFrom;

    private Instant validTo;

    public TaskSlaConfig() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TaskStepDefinition getSourceStep() {
        return sourceStep;
    }

    public void setSourceStep(TaskStepDefinition sourceStep) {
        this.sourceStep = sourceStep;
    }

    public TaskStepDefinition getTargetStep() {
        return targetStep;
    }

    public void setTargetStep(TaskStepDefinition targetStep) {
        this.targetStep = targetStep;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
    }

    public TaskSlaUnit getUnit() {
        return unit;
    }

    public void setUnit(TaskSlaUnit unit) {
        this.unit = unit;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskSlaConfig that = (TaskSlaConfig) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
