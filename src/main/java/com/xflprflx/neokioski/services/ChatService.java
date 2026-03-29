package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.MessageReaction;
import com.xflprflx.neokioski.models.MessageEdit;
import com.xflprflx.neokioski.models.Task;
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
        return sendMessage(content, user, group, null);
    }

    @Transactional
    public ChatMessage sendMessage(String content, User user, Group group, ChatMessage replyTo) {
        ChatMessage message = new ChatMessage(content, user, group, replyTo);
        message = repository.save(message);
        broadcast(group.getId(), message);
        return message;
    }

    @Transactional
    public ChatMessage linkTaskToMessage(Long messageId, Task task) {
        return repository.findById(messageId).map(msg -> {
            msg.setTask(task);
            ChatMessage saved = repository.save(msg);
            broadcast(saved.getGroup().getId(), saved);
            return saved;
        }).orElse(null);
    }

    public List<ChatMessage> getMessagesByGroup(Group group) {
        // Busca todas as mensagens (sem fetch)
        List<ChatMessage> all = repository.findByGroupOrderByTimestampAsc(group);
        // Busca mensagens com task e suas coleções carregadas eagerly
        List<ChatMessage> withTask = repository.findByGroupWithTaskOrderByTimestampAsc(group);
        // Substitui os objetos com task pelo equivalente com coleções inicializadas
        Map<Long, ChatMessage> taskMap = new HashMap<>();
        for (ChatMessage m : withTask) taskMap.put(m.getId(), m);
        return all.stream()
                .map(m -> taskMap.getOrDefault(m.getId(), m))
                .toList();
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

    @Transactional
    public void editMessage(Long messageId, String newContent, User user) {
        repository.findById(messageId).ifPresent(message -> {
            // Apenas o autor pode editar (ou sistema se necessário, mas aqui assumimos autor)
            if (message.getUser().equals(user) && !message.isDeleted()) {
                MessageEdit edit = new MessageEdit(message.getContent(), message);
                message.getEdits().add(edit);
                message.setContent(newContent);
                repository.save(message);
                broadcast(message.getGroup().getId(), message);
            }
        });
    }

    @Transactional
    public void deleteMessage(Long messageId, User user) {
        repository.findById(messageId).ifPresent(message -> {
            // Apenas o autor ou ADMIN (opcional, mas foco no autor por enquanto) pode excluir
            boolean isAdmin = user.getRole() == com.xflprflx.neokioski.models.enums.Role.ADMIN;
            if ((message.getUser().equals(user) || isAdmin) && !message.isDeleted()) {
                message.setDeleted(true);
                repository.save(message);
                broadcast(message.getGroup().getId(), message);
            }
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
