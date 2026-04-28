package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskTemplateMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskTemplateMentionRepository extends JpaRepository<TaskTemplateMention, Long> {
    List<TaskTemplateMention> findByTaskTemplateId(Long templateId);
    List<TaskTemplateMention> findByTaskTemplateIdAndGroupId(Long templateId, Long groupId);
    void deleteByTaskTemplateId(Long templateId);
}
