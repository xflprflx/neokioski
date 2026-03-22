package com.xflprflx.neokioski.models.template;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "tb_task_step_definitions")
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
        if (o == null || getClass() != o.getClass()) return false;
        TaskStepDefinition that = (TaskStepDefinition) o;
        return Objects.equals(id, that.id) && Objects.equals(label, that.label) && Objects.equals(colorTheme, that.colorTheme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, colorTheme);
    }
}
