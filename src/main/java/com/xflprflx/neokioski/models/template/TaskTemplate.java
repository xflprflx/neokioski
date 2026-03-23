package com.xflprflx.neokioski.models.template;

import com.xflprflx.neokioski.models.Group;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tb_task_templates")
public class TaskTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private boolean showSendButton;
    @Column(nullable = false)
    private boolean requiresSignature;
    @ManyToMany
    @JoinTable(
            name = "tb_task_templates_groups",
            joinColumns = @JoinColumn(name = "task_template_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private List<Group> groups = new ArrayList<>();
    @OneToMany(mappedBy = "taskTemplate")
    private List<TemplateStepRelation> templateStepRelation = new ArrayList<>();
    @OneToMany(mappedBy = "taskTemplate")
    private List<TemplateFieldRelation> templateFieldRelation = new ArrayList<>();

    public TaskTemplate() {
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

    public boolean isShowSendButton() {
        return showSendButton;
    }

    public void setShowSendButton(boolean showSendButton) {
        this.showSendButton = showSendButton;
    }

    public boolean isRequiresSignature() {
        return requiresSignature;
    }

    public void setRequiresSignature(boolean requiresSignature) {
        this.requiresSignature = requiresSignature;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void addGroups(Group group) {
        this.groups.add(group);
    }

    public List<TemplateStepRelation> getTemplateStepRelation() {
        return templateStepRelation;
    }

    public List<TemplateFieldRelation> getTemplateFieldRelation() {
        return templateFieldRelation;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskTemplate that = (TaskTemplate) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
