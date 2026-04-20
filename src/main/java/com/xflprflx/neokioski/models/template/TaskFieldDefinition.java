package com.xflprflx.neokioski.models.template;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tb_task_field_definitions")
/**
 * Entidade que representa a definição estrutural de um campo customizável pertencente a um template.
 */
public class TaskFieldDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String label;
    @Column(nullable = false)
    private String description;
    @Enumerated(EnumType.STRING)
    private InputType inputType;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "task_field_definition_id")
    private List<TaskFieldOption> taskFieldOptions = new ArrayList<>();

    public TaskFieldDefinition() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public InputType getInputType() {
        return inputType;
    }

    public void setInputType(InputType inputType) {
        this.inputType = inputType;
    }

    public List<TaskFieldOption> getTaskFieldOptions() {
        return taskFieldOptions;
    }

    /**
     * Adiciona uma opção selecionável disponível para este campo.
     *
     * @param taskFieldOption A opção de campo a ser inserida.
     */
    public void addTaskFieldOption(TaskFieldOption taskFieldOption) {
        /*
         * Insere a opção na lista de alternativas para este campo.
         */
        this.taskFieldOptions.add(taskFieldOption);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskFieldDefinition that = (TaskFieldDefinition) o;
        return Objects.equals(id, that.id) && Objects.equals(label, that.label) && Objects.equals(description, that.description) && inputType == that.inputType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, label, description, inputType);
    }
}
