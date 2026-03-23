package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
           SELECT DISTINCT m FROM ChatMessage m
           LEFT JOIN FETCH m.task t
           LEFT JOIN FETCH t.taskFieldValues
           LEFT JOIN FETCH t.taskEvents te
           LEFT JOIN FETCH te.taskStepDefinition
           LEFT JOIN FETCH t.taskTemplate
           LEFT JOIN FETCH t.currentStep
           WHERE m.group = :group
             AND m.task IS NOT NULL
           ORDER BY m.timestamp ASC
           """)
    List<ChatMessage> findByGroupWithTaskOrderByTimestampAsc(@Param("group") Group group);

    @Query("SELECT m FROM ChatMessage m WHERE m.group = :group ORDER BY m.timestamp ASC")
    List<ChatMessage> findByGroupOrderByTimestampAsc(@Param("group") Group group);
}
