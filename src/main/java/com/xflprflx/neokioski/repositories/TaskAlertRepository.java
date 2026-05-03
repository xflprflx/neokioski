package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * Repositório para persistência e gerenciamento das configurações de alertas das tarefas (TaskAlert).
 */
public interface TaskAlertRepository extends JpaRepository<TaskAlert, Long> {
    List<TaskAlert> findByTaskTemplateId(Long taskTemplateId);
}
