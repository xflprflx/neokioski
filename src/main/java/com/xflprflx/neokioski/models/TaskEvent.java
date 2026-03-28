package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_task_events")
public class TaskEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant timestamp;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "task_step_definition_id")
    private TaskStepDefinition taskStepDefinition;

    @Column(columnDefinition = "TEXT")
    private String response;

    public TaskEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskEvent taskEvent = (TaskEvent) o;
        return Objects.equals(id, taskEvent.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
