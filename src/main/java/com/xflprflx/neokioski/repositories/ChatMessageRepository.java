package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
/**
 * Repositório para persistência e consultas relacionadas à entidade de mensagens de chat (ChatMessage).
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // #improve_n+1 - Carregamento otimizado via EntityGraph (To-Ones) + BatchSize (Coleções)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {
        "user", "group", "task", "task.taskTemplate", "task.currentStep", 
        "task.slaResults", "task.slaResults.slaConfig",
        "task.taskFieldValues", "task.taskFieldValues.taskFieldDefinition",
        "replyTo", "replyTo.user", "replyTo.task", "replyTo.task.taskTemplate"
    }, type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    List<ChatMessage> findByGroupAndTimestampBetweenOrderByTimestampAsc(
            @Param("group") Group group, 
            @Param("start") Instant start, 
            @Param("end") Instant end);

    // #improve_n+1 - Carregamento otimizado via EntityGraph (To-Ones) + BatchSize (Coleções)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "group", "task", "task.taskTemplate", "task.currentStep"}, type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT m FROM ChatMessage m WHERE m.group = :group ORDER BY m.timestamp ASC")
    List<ChatMessage> findByGroupOrderByTimestampAsc(@Param("group") Group group);

    // #improve_n+1 - Carregamento atômico profundo para broadcast em tempo real
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "group", "task", "task.taskTemplate", "task.currentStep"}, type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT m FROM ChatMessage m WHERE m.id = :id")
    java.util.Optional<ChatMessage> findByIdWithFullGraph(@Param("id") Long id);

    java.util.List<ChatMessage> findByTaskOrderByIdAsc(com.xflprflx.neokioski.models.Task task);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "group", "task", "task.taskTemplate", "task.currentStep"}, type = org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD)
    @Query("""
           SELECT DISTINCT m FROM ChatMessage m
           LEFT JOIN m.task t
           LEFT JOIN t.taskFieldValues fv
           LEFT JOIN t.taskEvents ev
           WHERE m.group = :group
             AND (
               LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(fv.value) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(ev.response) LIKE LOWER(CONCAT('%', :query, '%'))
             )
           ORDER BY m.timestamp DESC
           """)
    List<ChatMessage> searchMessages(@Param("group") Group group, @Param("query") String query, org.springframework.data.domain.Pageable pageable);
}
