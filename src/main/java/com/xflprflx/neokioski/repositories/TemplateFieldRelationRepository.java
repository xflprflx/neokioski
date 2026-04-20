package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TemplateFieldRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * Repositório para gerenciamento da relação que mapeia quais campos pertencem a qual template (TemplateFieldRelation).
 */
public interface TemplateFieldRelationRepository extends JpaRepository<TemplateFieldRelation, Long> {
    
    @Query("SELECT r FROM TemplateFieldRelation r " +
           "JOIN FETCH r.taskFieldDefinition tfd " +
           "LEFT JOIN FETCH tfd.taskFieldOptions " +
           "WHERE r.taskTemplate.id = :taskTemplateId " +
           "ORDER BY r.order ASC")
    List<TemplateFieldRelation> findByTaskTemplateIdOrderByOrderAsc(@Param("taskTemplateId") Long taskTemplateId);

    void deleteByTaskTemplateId(Long taskTemplateId);
}
