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
/**
 * Serviço responsável por gerenciar a lógica de negócios relacionada às mensagens de chat (envio, edição, exclusão e reações).
 */
public class ChatService {

    private final ChatMessageRepository repository;
    private final MessageReactionRepository reactionRepository;
    private final Map<Long, List<Consumer<ChatMessage>>> listeners = Collections.synchronizedMap(new HashMap<>());

    public ChatService(ChatMessageRepository repository, MessageReactionRepository reactionRepository) {
        this.repository = repository;
        this.reactionRepository = reactionRepository;
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ChatMessage sendMessage(String content, User user, Group group) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return sendMessage(content, user, group, null, null, null);
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ChatMessage sendMessage(String content, User user, Group group, ChatMessage replyTo, String alertSound, String alertRoles) {
        ChatMessage message = new ChatMessage(content, user, group, replyTo);
        message.setAlertSound(alertSound);
        message.setAlertRoles(alertRoles);
        message = repository.save(message);
        broadcast(group.getId(), message);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return message;
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ChatMessage sendVolatileMessage(String content, User user, Group group, ChatMessage replyTo, String alertSound, String alertRoles) {
        // Mensagem VOLÁTIL: Não salvamos no banco
        ChatMessage message = new ChatMessage(content, user, group, replyTo);
        message.setAlertSound(alertSound);
        message.setAlertRoles(alertRoles);
        
        // Atribuir ID temporário negativo para que a UI aceite como mensagem válida mas temporária
        message.setId(-1L * System.currentTimeMillis());
        
        broadcast(group.getId(), message);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return message;
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ChatMessage sendMessageWithTask(String content, User user, Group group, Task task, String alertSound, String alertRoles) {
        ChatMessage message = new ChatMessage(content, user, group, null);
        message.setAlertSound(alertSound);
        message.setAlertRoles(alertRoles);
        message.setTask(task);
        message = repository.save(message);
        broadcast(group.getId(), message);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return message;
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ChatMessage sendSystemAlertForTask(String content, Task task) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return sendSystemAlertForTask(content, task, null, null);
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ChatMessage sendSystemAlertForTask(String content, Task task, String alertSound, String alertRoles) {
        // Alerta VOLÁTIL: Não salvamos no banco para economizar espaço
        String finalContent = (content == null || content.isBlank()) ? "Alerta de tempo esgotado" : content;
        
        ChatMessage alertMsg = new ChatMessage(finalContent, null, task.getGroup(), null);
        alertMsg.setAlertSound(alertSound);
        alertMsg.setAlertRoles(alertRoles);
        
        // Atribuir ID negativo para que a UI aceite como mensagem válida mas temporária
        alertMsg.setId(-1L * System.currentTimeMillis());
        
        // Responder à primeira mensagem da tarefa (geralmente o card original)
        List<ChatMessage> msgs = repository.findByTaskOrderByIdAsc(task);
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (!msgs.isEmpty()) {
            alertMsg.setReplyTo(msgs.get(0));
        }

        broadcast(task.getGroup().getId(), alertMsg);

        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return alertMsg;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void broadcastSoundOnly(String alertSound, String alertRoles, Group group) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (alertSound == null || alertSound.isEmpty()) return;
        
        ChatMessage ephemeral = new ChatMessage("(Alerta Sonoro)", null, group, null);
        ephemeral.setAlertSound(alertSound);
        ephemeral.setAlertRoles(alertRoles);
        broadcast(group.getId(), ephemeral);
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ChatMessage linkTaskToMessage(Long messageId, Task task) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return repository.findById(messageId).map(msg -> {
            msg.setTask(task);
            ChatMessage saved = repository.save(msg);
            broadcast(saved.getGroup().getId(), saved);
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return saved;
        }).orElse(null);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<ChatMessage> getMessagesByGroup(Group group, java.time.Instant start, java.time.Instant end) {
        // Busca todas as mensagens (sem fetch)
        List<ChatMessage> all = repository.findByGroupAndTimestampBetweenOrderByTimestampAsc(group, start, end);
        // Busca mensagens com task e suas coleções carregadas eagerly
        List<ChatMessage> withTask = repository.findByGroupWithTaskAndTimestampBetweenOrderByTimestampAsc(group, start, end);
        // Substitui os objetos com task pelo equivalente com coleções inicializadas
        Map<Long, ChatMessage> taskMap = new HashMap<>();
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (ChatMessage m : withTask) taskMap.put(m.getId(), m);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return all.stream()
                .map(m -> taskMap.getOrDefault(m.getId(), m))
                .toList();
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<ChatMessage> getMessagesByGroup(Group group) {
        // Busca todas as mensagens (sem fetch)
        List<ChatMessage> all = repository.findByGroupOrderByTimestampAsc(group);
        // Busca mensagens com task e suas coleções carregadas eagerly
        List<ChatMessage> withTask = repository.findByGroupWithTaskOrderByTimestampAsc(group);
        // Substitui os objetos com task pelo equivalente com coleções inicializadas
        Map<Long, ChatMessage> taskMap = new HashMap<>();
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (ChatMessage m : withTask) taskMap.put(m.getId(), m);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return all.stream()
                .map(m -> taskMap.getOrDefault(m.getId(), m))
                .toList();
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<ChatMessage> searchMessages(Group group, String query, int page, int size) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return repository.searchMessages(group, query, org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
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
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void editMessage(Long messageId, String newContent, User user) {
        repository.findById(messageId).ifPresent(message -> {
            // Apenas o autor pode editar (ou sistema se necessário, mas aqui assumimos autor)
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
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
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void deleteMessage(Long messageId, User user) {
        repository.findById(messageId).ifPresent(message -> {
            // Apenas o autor ou ADMIN (opcional, mas foco no autor por enquanto) pode excluir
            boolean isAdmin = user.getRole() == com.xflprflx.neokioski.models.enums.Role.ADMIN;
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if ((message.getUser().equals(user) || isAdmin) && !message.isDeleted()) {
                message.setDeleted(true);
                repository.save(message);
                broadcast(message.getGroup().getId(), message);
            }
        });
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void register(Long groupId, Consumer<ChatMessage> listener) {
        listeners.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void unregister(Long groupId, Consumer<ChatMessage> listener) {
        List<Consumer<ChatMessage>> groupListeners = listeners.get(groupId);
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (groupListeners != null) {
            groupListeners.remove(listener);
        }
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private void broadcast(Long groupId, ChatMessage message) {
        List<Consumer<ChatMessage>> groupListeners = listeners.get(groupId);
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (groupListeners != null) {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (Consumer<ChatMessage> listener : groupListeners) {
                /*
                 * Inicia um bloco try-catch para tratar possíveis exceções.
                 */
                try {
                    listener.accept(message);
                } catch (Exception e) {
                    System.err.println("Erro ao notificar listener de Chat no grupo " + groupId + ": " + e.getMessage());
                }
            }
        }
    }
}
