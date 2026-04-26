package com.xflprflx.neokioski.models.template;

import jakarta.persistence.*;

import org.hibernate.annotations.BatchSize;
import java.util.Objects;

@Entity
@Table(name = "tb_task_step_definitions")
@BatchSize(size = 100)
/**
 * Entidade que define um passo específico no fluxo (pipeline) de um template de tarefa.
 */
public class TaskStepDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String colorTheme;

    public TaskStepDefinition() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getColorTheme() {
        return colorTheme;
    }

    public void setColorTheme(String colorTheme) {
        this.colorTheme = colorTheme;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskStepDefinition that)) return false;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
