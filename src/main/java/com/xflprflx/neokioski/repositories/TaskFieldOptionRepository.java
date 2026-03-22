package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.template.TaskFieldOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskFieldOptionRepository extends JpaRepository<TaskFieldOption, Long> {
}
