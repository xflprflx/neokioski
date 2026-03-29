package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {
    
    @Query("SELECT t FROM TaskTemplate t LEFT JOIN FETCH t.alerts WHERE t.id = :id")
    Optional<TaskTemplate> findByIdWithGroups(@Param("id") Long id);

    @Query("SELECT t FROM TaskTemplate t JOIN t.groups g WHERE g.id = :groupId")
    List<TaskTemplate> findByGroupId(@Param("groupId") Long groupId);
}
