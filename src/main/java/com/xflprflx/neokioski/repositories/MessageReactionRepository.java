package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.MessageReaction;
import com.xflprflx.neokioski.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositório para persistência e consultas relacionadas às reações de mensagens de chat (MessageReaction).
 */
public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    Optional<MessageReaction> findByChatMessageAndUserAndEmoji(ChatMessage chatMessage, User user, String emoji);
}
