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
import java.util.LinkedHashSet;

@Entity
@Table(name = "tb_task_templates")
@Cacheable
@org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
@org.hibernate.annotations.BatchSize(size = 50)
/**
 * Entidade que define o modelo ou gabarito estrutural utilizado na criação de novas tarefas.
 */
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

    // #improve_n+1 - Remoção do EAGER forçado nas coleções administrativas que causavam queries em massa quando uma Task era listada
    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "tb_task_template_alert_roles", joinColumns = @JoinColumn(name = "task_template_id"))
    @Column(name = "role")
    @org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<Role> alertRoles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tb_task_templates_groups",
            joinColumns = @JoinColumn(name = "task_template_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    private Set<Group> groups = new LinkedHashSet<>();

    @OneToMany(mappedBy = "taskTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("order ASC")
    @org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<TemplateStepRelation> templateStepRelation = new LinkedHashSet<>();

    @OneToMany(mappedBy = "taskTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<TemplateFieldRelation> templateFieldRelation = new LinkedHashSet<>();

    @OneToMany(mappedBy = "taskTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<TaskAlert> alerts = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tb_task_templates_authorizer_users",
            joinColumns = @JoinColumn(name = "task_template_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @org.hibernate.annotations.Fetch(org.hibernate.annotations.FetchMode.SUBSELECT)
    @org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<User> authorizingUsers = new HashSet<>();

    @OneToMany(mappedBy = "taskTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @org.hibernate.annotations.Cache(usage = org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE)
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<TaskTemplateMention> mentionsMappings = new HashSet<>();

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

    public Set<Group> getGroups() {
        return groups;
    }

    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    /**
     * Adiciona um grupo à lista de grupos associados a este template.
     *
     * @param group O grupo a ser adicionado.
     */
    public void addGroups(Group group) {
        /*
         * Insere o grupo na coleção local da entidade.
         */
        this.groups.add(group);
    }

    public Set<TemplateStepRelation> getTemplateStepRelation() {
        return templateStepRelation;
    }

    public Set<TemplateFieldRelation> getTemplateFieldRelation() {
        return templateFieldRelation;
    }

    public Set<User> getAuthorizingUsers() {
        return authorizingUsers;
    }

    public void setAuthorizingUsers(Set<User> authorizingUsers) {
        this.authorizingUsers = authorizingUsers;
    }

    public Set<TaskAlert> getAlerts() {
        return alerts;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setAlerts(Set<TaskAlert> alerts) {
        this.alerts = alerts;
    }

    public Set<TaskTemplateMention> getMentionsMappings() {
        return mentionsMappings;
    }

    public void setMentionsMappings(Set<TaskTemplateMention> mentionsMappings) {
        this.mentionsMappings = mentionsMappings;
    }

    /**
     * Adiciona um alerta de tarefa configurado e atualiza a referência deste
     * template dentro do alerta (relação bidirecional).
     *
     * @param alert O alerta de tarefa a ser adicionado.
     */
    public void addAlert(TaskAlert alert) {
        /*
         * Adiciona o alerta na lista de alertas deste template.
         */
        this.alerts.add(alert);

        /*
         * Configura este template como a referência (pai) no objeto do alerta.
         */
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
