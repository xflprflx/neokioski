package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateStepRelationRepository extends JpaRepository<TemplateStepRelation, Long> {
}
