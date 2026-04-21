package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
/**
 * Repositório para gerenciamento da ordenação e vínculo dos passos em relação aos templates de tarefas (TemplateStepRelation).
 */
public interface TemplateStepRelationRepository extends JpaRepository<TemplateStepRelation, Long> {
    @EntityGraph("TemplateStepRelation.findByTaskTemplateIdAndTaskStepDefinitionId")
    List<TemplateStepRelation> findByTaskTemplateIdOrderByOrderAsc(Long taskTemplateId);

    void deleteByTaskTemplateId(Long taskTemplateId);

    @EntityGraph("TemplateStepRelation.findByTaskTemplateIdAndTaskStepDefinitionId")
    Optional<TemplateStepRelation> findByTaskTemplateIdAndTaskStepDefinitionId(Long taskTemplateId, Long taskStepDefinitionId);
}
