package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
/**
 * Repositório de acesso a dados para definições de campos customizados das tarefas (TaskFieldDefinition).
 */
public interface TaskFieldDefinitionRepository extends JpaRepository<TaskFieldDefinition, Long> {
}
