package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.MessageReaction;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.ChatMessageRepository;
import com.xflprflx.neokioski.repositories.MessageReactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Service
public class ChatService {

    private final ChatMessageRepository repository;
    private final MessageReactionRepository reactionRepository;
    private final Map<Long, List<Consumer<ChatMessage>>> listeners = Collections.synchronizedMap(new HashMap<>());

    public ChatService(ChatMessageRepository repository, MessageReactionRepository reactionRepository) {
        this.repository = repository;
        this.reactionRepository = reactionRepository;
    }

    @Transactional
    public ChatMessage sendMessage(String content, User user, Group group) {
        ChatMessage message = new ChatMessage(content, user, group);
        message = repository.save(message);
        broadcast(group.getId(), message);
        return message;
    }

    public List<ChatMessage> getMessagesByGroup(Group group) {
        return repository.findByGroupOrderByTimestampAsc(group);
    }

    @Transactional
    public void toggleReaction(Long messageId, User user, String emoji) {
        repository.findById(messageId).ifPresent(message -> {
            reactionRepository.findByChatMessageAndUserAndEmoji(message, user, emoji)
                    .ifPresentOrElse(
                            reaction -> {
                                message.getReactions().remove(reaction);
                                reactionRepository.delete(reaction);
                            },
                            () -> {
                                MessageReaction reaction = new MessageReaction(emoji, user, message);
                                message.getReactions().add(reaction);
                                reactionRepository.save(reaction);
                            }
                    );
            broadcast(message.getGroup().getId(), message);
        });
    }

    public void register(Long groupId, Consumer<ChatMessage> listener) {
        listeners.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unregister(Long groupId, Consumer<ChatMessage> listener) {
        List<Consumer<ChatMessage>> groupListeners = listeners.get(groupId);
        if (groupListeners != null) {
            groupListeners.remove(listener);
        }
    }

    private void broadcast(Long groupId, ChatMessage message) {
        List<Consumer<ChatMessage>> groupListeners = listeners.get(groupId);
        if (groupListeners != null) {
            groupListeners.forEach(listener -> listener.accept(message));
        }
    }
}
