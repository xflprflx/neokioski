package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
@Repository
/**
 * Repositório responsável por consultar e armazenar modelos (templates) de tarefas (TaskTemplate).
 */
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {
    
    // #improve_n+1 - Múltiplos FETCH para Lists geram MultipleBagFetchException. Mas para Sets e To-Ones é seguro.
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
        "alerts", "alerts.alertRoles", "alerts.startStep", "alerts.targetStep",
        "templateStepRelation", "templateStepRelation.taskStepDefinition", 
        "templateStepRelation.responseOptions", "templateStepRelation.allowedNextSteps", "templateStepRelation.alertRoles",
        "templateFieldRelation", "templateFieldRelation.taskFieldDefinition", "templateFieldRelation.taskFieldDefinition.taskFieldOptions",
        "alertRoles", "authorizingUsers", "groups"
    }, type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT t FROM TaskTemplate t WHERE t.id = :id")
    Optional<TaskTemplate> findByIdWithMetadata(@Param("id") Long id);

    @Query("SELECT t FROM TaskTemplate t LEFT JOIN FETCH t.alerts WHERE t.id = :id")
    Optional<TaskTemplate> findByIdWithAlerts(@Param("id") Long id);

    // #improve_n+1 - Evitar que a lista geral carregue sub-colecoes lazy e cause queries secundárias
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"alertRoles", "authorizingUsers", "templateStepRelation", "templateStepRelation.taskStepDefinition", "templateFieldRelation", "templateFieldRelation.taskFieldDefinition", "templateFieldRelation.taskFieldDefinition.taskFieldOptions"}, type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT t FROM TaskTemplate t JOIN t.groups g WHERE g.id = :groupId ORDER BY t.sortOrder ASC, t.name ASC")
    List<TaskTemplate> findByGroupId(@Param("groupId") Long groupId);

    // #improve_n+1 - Como as dependências viraram Lazy, a listagem geral só puxa o Template em si
    @Override
    @Query("SELECT t FROM TaskTemplate t ORDER BY t.sortOrder ASC, t.name ASC")
    List<TaskTemplate> findAll();
}
