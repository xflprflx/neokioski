package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.enums.NotificationType;
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

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ChatService {

    private final ChatMessageRepository repository;
    private final MessageReactionRepository reactionRepository;
    private final UserService userService;
    private final MessageNotificationService notificationService;
    private final Map<Long, List<Consumer<ChatMessage>>> listeners = Collections.synchronizedMap(new HashMap<>());

    public ChatService(ChatMessageRepository repository, MessageReactionRepository reactionRepository, UserService userService, MessageNotificationService notificationService) {
        this.repository = repository;
        this.reactionRepository = reactionRepository;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ChatMessage sendMessage(String content, User user, Group group) {
        return sendMessage(content, user, group, null);
    }

    @Transactional
    public ChatMessage sendMessage(String content, User user, Group group, ChatMessage replyTo) {
        ChatMessage messageToSave = new ChatMessage(content, user, group, replyTo);
        final ChatMessage message = repository.save(messageToSave);

        // Disparando notificações se for resposta
        if (replyTo != null && replyTo.getUser() != null && !replyTo.getUser().getId().equals(user.getId())) {
            notificationService.createNotification(replyTo.getUser(), message, NotificationType.REPLY);
        }

        // Detectando menções
        List<User> groupUsers = userService.findAll(); // Poderia ser findAll do grupo, mas filtrando na memória por hora
        for (User u : groupUsers) {
            if (!u.getId().equals(user.getId()) && isUserMentioned(content, u.getUsername())) {
                notificationService.createNotification(u, message, NotificationType.MENTION);
            }
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast(group.getId(), message);
                }
            });
        } else {
            broadcast(group.getId(), message);
        }
        
        return message;
    }

    private boolean isUserMentioned(String content, String username) {
        if (content == null || username == null) return false;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@" + java.util.regex.Pattern.quote(username) + "(\\b|$)");
        return pattern.matcher(content).find();
    }

    public List<ChatMessage> getMessagesByGroup(Group group) {
        return repository.findByGroupOrderByTimestampAsc(group);
    }

    private final List<Consumer<ChatMessage>> globalListeners = new CopyOnWriteArrayList<>();

    public void registerGlobal(Consumer<ChatMessage> listener) {
        globalListeners.add(listener);
    }

    public void unregisterGlobal(Consumer<ChatMessage> listener) {
        globalListeners.remove(listener);
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
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        broadcast(message.getGroup().getId(), message);
                    }
                });
            } else {
                broadcast(message.getGroup().getId(), message);
            }
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
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            broadcast(message.getGroup().getId(), message);
                        }
                    });
                } else {
                    broadcast(message.getGroup().getId(), message);
                }
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
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            broadcast(message.getGroup().getId(), message);
                        }
                    });
                } else {
                    broadcast(message.getGroup().getId(), message);
                }
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
        globalListeners.forEach(listener -> listener.accept(message));
    }
}
