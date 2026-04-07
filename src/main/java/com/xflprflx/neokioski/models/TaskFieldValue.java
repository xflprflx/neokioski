package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.template.TaskFieldDefinition;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "tb_task_field_values")
public class TaskFieldValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String value;

    @ManyToOne
    @JoinColumn(name = "task_field_definition_id")
    private TaskFieldDefinition taskFieldDefinition;

    public TaskFieldValue() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TaskFieldDefinition getTaskFieldDefinition() {
        return taskFieldDefinition;
    }

    public void setTaskFieldDefinition(TaskFieldDefinition taskFieldDefinition) {
        this.taskFieldDefinition = taskFieldDefinition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskFieldValue that = (TaskFieldValue) o;
        return Objects.equals(id, that.id) && 
               Objects.equals(value, that.value) && 
               Objects.equals(taskFieldDefinition, that.taskFieldDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value, taskFieldDefinition);
    }
}
