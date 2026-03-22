package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.time.Instant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByGroupOrderByTimestampAsc(Group group);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.group = :group AND m.timestamp > :lastRead AND m.user != :user")
    long countUnreadMessages(@Param("group") Group group, @Param("lastRead") Instant lastRead, @Param("user") com.xflprflx.neokioski.models.User user);
}
