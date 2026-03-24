package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.taskFieldValues LEFT JOIN FETCH t.taskEvents WHERE t.group = :group ORDER BY t.createdAt DESC")
    List<Task> findByGroupWithCollections(@Param("group") Group group);

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN t.taskEvents e WHERE t.group = :group AND (t.creator = :user OR e.user = :user) ORDER BY t.createdAt DESC")
    List<Task> findMyTasksByGroup(@Param("group") Group group, @Param("user") User user);

    @Query("""
           SELECT t FROM Task t
           LEFT JOIN TemplateStepRelation sr
             ON sr.taskTemplate = t.taskTemplate
            AND sr.taskStepDefinition = t.currentStep
           WHERE t.group = :group
             AND (t.currentStep IS NULL OR sr IS NULL OR sr.isFinalizer = false)
           ORDER BY t.createdAt DESC
           """)
    List<Task> findPendingByGroup(@Param("group") Group group);


}
