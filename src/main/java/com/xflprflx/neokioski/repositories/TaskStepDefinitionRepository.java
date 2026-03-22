package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskStepDefinitionRepository extends JpaRepository<TaskStepDefinition, Long> {
}
