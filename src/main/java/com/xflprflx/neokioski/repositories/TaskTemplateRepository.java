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
    
    @Query("SELECT t FROM TaskTemplate t LEFT JOIN FETCH t.alerts WHERE t.id = :id")
    Optional<TaskTemplate> findByIdWithGroups(@Param("id") Long id);

    @Query("SELECT t FROM TaskTemplate t JOIN t.groups g WHERE g.id = :groupId ORDER BY t.sortOrder ASC, t.name ASC")
    List<TaskTemplate> findByGroupId(@Param("groupId") Long groupId);

    // #improve_n+1 - EntityGraph para trazer todo o grafo principal do template numa só query
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
        "groups", "authorizingUsers", "alerts", "templateStepRelation", "templateStepRelation.taskStepDefinition", "templateFieldRelation", "templateFieldRelation.taskFieldDefinition"
    })
    @Query("SELECT t FROM TaskTemplate t WHERE t.id = :id")
    Optional<TaskTemplate> findByIdWithFullGraph(@Param("id") Long id);
}
