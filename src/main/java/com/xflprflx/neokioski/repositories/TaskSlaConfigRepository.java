package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskSlaConfig;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

/**
 * Repositório de configuração das regras e limites de tempo (SLA) para tarefas (TaskSlaConfig).
 */
public interface TaskSlaConfigRepository extends JpaRepository<TaskSlaConfig, Long> {
    
    @Query("SELECT c FROM TaskSlaConfig c WHERE c.taskTemplate = :template " +
           "AND c.validFrom <= :date AND (c.validTo IS NULL OR c.validTo > :date)")
    List<TaskSlaConfig> findValidConfigs(TaskTemplate template, Instant date);

    List<TaskSlaConfig> findByTaskTemplateAndValidToIsNull(TaskTemplate taskTemplate);
}
