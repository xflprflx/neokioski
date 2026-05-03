package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.TaskSlaResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repositório para consulta e armazenamento dos resultados e violações de SLA (TaskSlaResult).
 */
public interface TaskSlaResultRepository extends JpaRepository<TaskSlaResult, Long> {
    List<TaskSlaResult> findByTask(Task task);
}
