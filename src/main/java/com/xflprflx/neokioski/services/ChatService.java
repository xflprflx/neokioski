package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.repositories.ChatMessageRepository;
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
    private final Map<Long, List<Consumer<ChatMessage>>> listeners = Collections.synchronizedMap(new HashMap<>());

    public ChatService(ChatMessageRepository repository) {
        this.repository = repository;
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
