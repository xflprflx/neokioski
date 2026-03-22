package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TemplateFieldRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateFieldRelationRepository extends JpaRepository<TemplateFieldRelation, Long> {
}
