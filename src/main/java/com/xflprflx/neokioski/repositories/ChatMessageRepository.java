package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(value = "ChatMessage.withTaskFull")
    @Query("""
           SELECT DISTINCT m FROM ChatMessage m
           WHERE m.group = :group
             AND m.task IS NOT NULL
             AND m.timestamp BETWEEN :start AND :end
           ORDER BY m.timestamp ASC
           """)
    List<ChatMessage> findByGroupWithTaskAndTimestampBetweenOrderByTimestampAsc(
            @Param("group") Group group, 
            @Param("start") Instant start, 
            @Param("end") Instant end);

    @EntityGraph(value = "ChatMessage.minimal")
    @Query("SELECT m FROM ChatMessage m WHERE m.group = :group AND m.timestamp BETWEEN :start AND :end ORDER BY m.timestamp ASC")
    List<ChatMessage> findByGroupAndTimestampBetweenOrderByTimestampAsc(
            @Param("group") Group group, 
            @Param("start") Instant start, 
            @Param("end") Instant end);

    @EntityGraph(value = "ChatMessage.withTaskFull")
    @Query("""
           SELECT DISTINCT m FROM ChatMessage m
           WHERE m.group = :group
             AND m.task IS NOT NULL
           ORDER BY m.timestamp ASC
           """)
    List<ChatMessage> findByGroupWithTaskOrderByTimestampAsc(@Param("group") Group group);

    @EntityGraph(value = "ChatMessage.minimal")
    @Query("SELECT m FROM ChatMessage m WHERE m.group = :group ORDER BY m.timestamp ASC")
    List<ChatMessage> findByGroupOrderByTimestampAsc(@Param("group") Group group);

    java.util.List<ChatMessage> findByTaskOrderByIdAsc(com.xflprflx.neokioski.models.Task task);

 /**
     * Pesquisa mensagens de chat baseando-se em uma string de consulta em vários campos.
     * @param group Grupo do chat.
     * @param query Texto a buscar.
     * @param pageable Controle de paginação.
     * @return Lista paginada de mensagens encontradas.
     */
    @EntityGraph(value = "ChatMessage.minimal")
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
