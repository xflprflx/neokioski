package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_task_events")
@BatchSize(size = 100)
/**
 * Entidade que registra os eventos e histórico do ciclo de vida de uma tarefa (criação, avanço de passo, conclusão).
 */
public class TaskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String response;

    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_step_definition_id")
    private TaskStepDefinition taskStepDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    @JsonIgnore
    private Task task;

    public TaskEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TaskStepDefinition getTaskStepDefinition() {
        return taskStepDefinition;
    }

    public void setTaskStepDefinition(TaskStepDefinition taskStepDefinition) {
        this.taskStepDefinition = taskStepDefinition;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskEvent taskEvent = (TaskEvent) o;
        return Objects.equals(id, taskEvent.id) && 
               Objects.equals(timestamp, taskEvent.timestamp) && 
               Objects.equals(user, taskEvent.user) && 
               Objects.equals(taskStepDefinition, taskEvent.taskStepDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, user, taskStepDefinition);
    }
}
