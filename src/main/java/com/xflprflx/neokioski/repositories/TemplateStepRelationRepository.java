package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * Repositório para gerenciamento da ordenação e vínculo dos passos em relação aos templates de tarefas (TemplateStepRelation).
 */
public interface TemplateStepRelationRepository extends JpaRepository<TemplateStepRelation, Long> {

    // #improve_n+1 - Carrega o step via LOAD graph evitando query extra para obter nomes dos passos
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"taskStepDefinition"}, type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    List<TemplateStepRelation> findByTaskTemplateIdOrderByOrderAsc(Long taskTemplateId);

    // #improve_n+1 - Previne fetch lazy em TaskStepDefinition, TaskTemplate (e seus subgrafos se necessários não devem ser forçados a não ser que preciso, default LAZY)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"taskStepDefinition"}, type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    java.util.Optional<TemplateStepRelation> findByTaskTemplateIdAndTaskStepDefinitionId(Long taskTemplateId, Long taskStepDefinitionId);

    // #improve_n+1 - Método para pre-cachear as relations de forma em lote, evitando consultas pontuais em Loop
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"taskStepDefinition"}, type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    @org.springframework.data.jpa.repository.Query("SELECT r FROM TemplateStepRelation r WHERE r.taskTemplate.id IN :templateIds")
    List<TemplateStepRelation> findByTaskTemplateIdIn(@org.springframework.data.repository.query.Param("templateIds") java.util.Collection<Long> templateIds);

    void deleteByTaskTemplateId(Long taskTemplateId);
}
