package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemplateStepRelationRepository extends JpaRepository<TemplateStepRelation, Long> {
    List<TemplateStepRelation> findByTaskTemplateIdOrderByOrderAsc(Long taskTemplateId);
    java.util.Optional<TemplateStepRelation> findByTaskTemplateIdAndTaskStepDefinitionId(Long taskTemplateId, Long taskStepDefinitionId);
    @Query("SELECT r FROM TemplateStepRelation r JOIN FETCH r.taskTemplate JOIN FETCH r.taskStepDefinition")
    List<TemplateStepRelation> findAllWithAssociations();

    void deleteByTaskTemplateId(Long taskTemplateId);
}
