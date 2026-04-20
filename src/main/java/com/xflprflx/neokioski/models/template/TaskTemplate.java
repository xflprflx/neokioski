package com.xflprflx.neokioski.models.template;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.enums.Role;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private boolean showSendButton = true;
    @Column(nullable = false)
    private boolean requiresSignature;
    @Column(nullable = false)
    private boolean playAlertSound = false;
    @Column(nullable = false)
    private boolean registraNoRelatorioLoja = false;
    private String alertSound;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_responsible_role")
    private Role defaultResponsibleRole;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "tb_task_template_alert_roles", joinColumns = @JoinColumn(name = "task_template_id"))
    @Column(name = "role")
    private Set<Role> alertRoles = new HashSet<>();
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_task_templates_groups",
            joinColumns = @JoinColumn(name = "task_template_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private List<Group> groups = new ArrayList<>();
    @OneToMany(mappedBy = "taskTemplate")
    private List<TemplateStepRelation> templateStepRelation = new ArrayList<>();
    @OneToMany(mappedBy = "taskTemplate")
    private List<TemplateFieldRelation> templateFieldRelation = new ArrayList<>();
    @OneToMany(mappedBy = "taskTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskAlert> alerts = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tb_task_templates_authorizer_users",
            joinColumns = @JoinColumn(name = "task_template_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private List<User> authorizingUsers = new ArrayList<>();

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

    public boolean isPlayAlertSound() {
        return playAlertSound;
    }

    public void setPlayAlertSound(boolean playAlertSound) {
        this.playAlertSound = playAlertSound;
    }

    public boolean isRegistraNoRelatorioLoja() {
        return registraNoRelatorioLoja;
    }

    public void setRegistraNoRelatorioLoja(boolean registraNoRelatorioLoja) {
        this.registraNoRelatorioLoja = registraNoRelatorioLoja;
    }

    public String getAlertSound() {
        return alertSound;
    }

    public void setAlertSound(String alertSound) {
        this.alertSound = alertSound;
    }

    public Role getDefaultResponsibleRole() {
        return defaultResponsibleRole;
    }

    public void setDefaultResponsibleRole(Role defaultResponsibleRole) {
        this.defaultResponsibleRole = defaultResponsibleRole;
    }

    public Set<Role> getAlertRoles() {
        return alertRoles;
    }

    public void setAlertRoles(Set<Role> alertRoles) {
        this.alertRoles = alertRoles;
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

    public List<User> getAuthorizingUsers() {
        return authorizingUsers;
    }

    public void setAuthorizingUsers(List<User> authorizingUsers) {
        this.authorizingUsers = authorizingUsers;
    }

    public List<TaskAlert> getAlerts() {
        return alerts;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setAlerts(List<TaskAlert> alerts) {
        this.alerts = alerts;
    }

    public void addAlert(TaskAlert alert) {
        this.alerts.add(alert);
        alert.setTaskTemplate(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskTemplate that = (TaskTemplate) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && defaultResponsibleRole == that.defaultResponsibleRole;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, defaultResponsibleRole);
    }
}
