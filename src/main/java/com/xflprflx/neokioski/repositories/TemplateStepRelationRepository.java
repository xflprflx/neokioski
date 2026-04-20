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
    List<TemplateStepRelation> findByTaskTemplateIdOrderByOrderAsc(Long taskTemplateId);
    java.util.Optional<TemplateStepRelation> findByTaskTemplateIdAndTaskStepDefinitionId(Long taskTemplateId, Long taskStepDefinitionId);
    void deleteByTaskTemplateId(Long taskTemplateId);
}
