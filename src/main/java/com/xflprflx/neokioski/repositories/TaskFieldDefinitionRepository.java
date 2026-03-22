package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskFieldDefinitionRepository extends JpaRepository<TaskFieldDefinition, Long> {
}
