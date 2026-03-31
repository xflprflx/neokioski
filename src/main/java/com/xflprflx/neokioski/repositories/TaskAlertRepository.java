package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAlertRepository extends JpaRepository<TaskAlert, Long> {
    List<TaskAlert> findByTaskTemplateId(Long taskTemplateId);
}
