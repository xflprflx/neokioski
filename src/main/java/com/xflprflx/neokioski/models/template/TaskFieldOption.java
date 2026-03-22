package com.xflprflx.neokioski.models.template;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "tb_task_field_options")
public class TaskFieldOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String optionLabel;
    @Column(nullable = false)
    private String optionValue;

    public TaskFieldOption() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOptionLabel() {
        return optionLabel;
    }

    public void setOptionLabel(String optionLabel) {
        this.optionLabel = optionLabel;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskFieldOption that = (TaskFieldOption) o;
        return Objects.equals(id, that.id) && Objects.equals(optionLabel, that.optionLabel) && Objects.equals(optionValue, that.optionValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, optionLabel, optionValue);
    }
}
