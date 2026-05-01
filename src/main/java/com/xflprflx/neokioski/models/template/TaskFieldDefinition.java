package com.xflprflx.neokioski.models.template;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;

@Entity
@Table(name = "tb_task_field_definitions")
@Cacheable
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
@org.hibernate.annotations.BatchSize(size = 50)
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
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "task_field_definition_id")
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    @org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<TaskFieldOption> taskFieldOptions = new LinkedHashSet<>();

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

    public Set<TaskFieldOption> getTaskFieldOptions() {
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
