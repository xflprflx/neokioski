package com.xflprflx.neokioski.models;

import com.xflprflx.neokioski.models.template.TaskTemplate;
import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "tb_groups")
@Cacheable
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
/**
 * Entidade que representa um grupo de usuários no sistema, utilizado para permissões e agrupar conversas.
 */
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private boolean isActive;

    @ManyToMany
    @JoinTable(
            name = "tb_users_groups",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<User> users = new HashSet<>();

    @ManyToMany(mappedBy = "groups")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<TaskTemplate> taskTemplates = new ArrayList<>();

    public Group() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public List<TaskTemplate> getTaskTemplates() {
        return taskTemplates;
    }

    /**
     * Adiciona um template de tarefa aos permitidos/vinculados a este grupo.
     *
     * @param taskTemplate O template de tarefa a ser adicionado.
     */
    public void addTaskTemplate(TaskTemplate taskTemplate) {
        /*
         * Insere o template de tarefa na coleção local do grupo.
         */
        this.taskTemplates.add(taskTemplate);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(id, group.id) && Objects.equals(name, group.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
