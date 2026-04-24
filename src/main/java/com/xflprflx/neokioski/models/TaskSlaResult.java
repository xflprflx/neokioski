package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.enums.SlaStatus;
import com.xflprflx.neokioski.models.enums.TaskSlaUnit;
import com.xflprflx.neokioski.models.template.TaskSlaConfig;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_task_sla_results")
@BatchSize(size = 100)
/**
 * Entidade que consolida o resultado do tempo de nível de serviço (SLA) para uma determinada tarefa.
 */
public class TaskSlaResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_config_id", nullable = false)
    private TaskSlaConfig slaConfig;

    @Column(nullable = false)
    private Integer targetValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskSlaUnit unit;

    @Column(nullable = false)
    private Long actualSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlaStatus status;

    @Column(nullable = false)
    private Instant completedAt;

    public TaskSlaResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public TaskSlaConfig getSlaConfig() {
        return slaConfig;
    }

    public void setSlaConfig(TaskSlaConfig slaConfig) {
        this.slaConfig = slaConfig;
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

    public Long getActualSeconds() {
        return actualSeconds;
    }

    public void setActualSeconds(Long actualSeconds) {
        this.actualSeconds = actualSeconds;
    }

    public SlaStatus getStatus() {
        return status;
    }

    public void setStatus(SlaStatus status) {
        this.status = status;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskSlaResult that = (TaskSlaResult) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
