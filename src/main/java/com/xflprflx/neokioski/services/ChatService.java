package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.repositories.ChatMessageRepository;
import com.xflprflx.neokioski.repositories.MessageReactionRepository;
import org.hibernate.Hibernate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Serviço responsável por gerenciar a lógica de negócios relacionada às mensagens de chat (envio, edição, exclusão e reações).
 */
@Service
public class ChatService {

    private final ChatMessageRepository repository;
    private final MessageReactionRepository reactionRepository;
    private final TaskService taskService;
    private final Map<Long, List<Consumer<ChatMessage>>> listeners = Collections.synchronizedMap(new HashMap<>());

    public ChatService(ChatMessageRepository repository, 
                       MessageReactionRepository reactionRepository,
                       @org.springframework.context.annotation.Lazy TaskService taskService) {
        this.repository = repository;
        this.reactionRepository = reactionRepository;
        this.taskService = taskService;
    }

    /**
     * Métodx que encapsula parte da lógica de negócio e regras do serviço.
     */
    @Transactional
    public ChatMessage sendMessage(String content, User user, Group group) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return sendMessage(content, user, group, null, null, null);
    }

    /**
     * Métodx que encapsula parte da lógica de negócio e regras do serviço.
     */
    @Transactional
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
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ChatMessage sendSystemAlertForTask(String content, Task task) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return sendSystemAlertForTask(content, task, null, null);
    }

    @Transactional
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Recupera mensagens de um grupo em um intervalo de tempo, utilizando uma estratégia de
     * "Enrichment" (Enriquecimento) para evitar o problema de N+1 queries.
     *
     * A estratégia consiste em:
     * 1. Buscar a sequência cronológica de todas as mensagens (Busca Base).
     * 2. Buscar especificamente as mensagens que contenham tarefas (Busca de Especializada).
     * 3. Forçar a inicialização de coleções Lazy internas da Task (Value Fields, SLAs)
     *    enquanto a sessão do Hibernate está ativa.
     * 4. Substituir as instâncias simples pelas enriquecidas na lista final.
     */
    @Transactional(readOnly = true)
    public List<ChatMessage> getMessagesByGroup(Group group, Instant start, Instant end) {
        // Passo 1: Busca base leve para manter a ordem e timeline correta.
        // Utiliza o grafo 'ChatMessage.minimal' para carregar apenas dados essenciais do chat.
        List<ChatMessage> all = repository.findByGroupAndTimestampBetweenOrderByTimestampAsc(group, start, end);
        
        // Passo 2: Busca especializada apenas para mensagens com Task.
        // Aciona o grafo 'ChatMessage.withTaskFull' para trazer os dados de tarefas.
        List<ChatMessage> withTask = repository.findByGroupWithTaskAndTimestampBetweenOrderByTimestampAsc(group, start, end);
        
        // Passo 3: Inicialização profunda via Multistage Batching.
        initializeMessagesTaskGraph(withTask);

        // Passo 4: Mapeamento e Swap.
        // Criamos um mapa das versões "ricas" das mensagens para substituição na lista timeline.
        Map<Long, ChatMessage> taskMap = new HashMap<>();
        for (ChatMessage m : withTask) taskMap.put(m.getId(), m);
        
        // Retornamos a timeline original, mas com os objetos Task totalmente carregados onde existirem
        return all.stream()
                .map(m -> taskMap.getOrDefault(m.getId(), m))
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Versão simplificada de busca de mensagens por grupo (timeline completa).
     * Mantém a mesma estratégia de "Swap" e "Enrichment" para garantir que tarefas 
     * acopladas às mensagens sejam enviadas à View com suas coleções Lazy pré-inicializadas.
     */
    public List<ChatMessage> getMessagesByGroup(Group group) {
        // Busca timeline original (usando grafo minimalista)
        List<ChatMessage> all = repository.findByGroupOrderByTimestampAsc(group);
        
        // Busca mensagens com tarefas para enriquecimento (usando grafo completo de tarefas)
        List<ChatMessage> withTask = repository.findByGroupWithTaskOrderByTimestampAsc(group);
        
        // Inicialização profunda para evitar LazyInitException na UI
        initializeMessagesTaskGraph(withTask);
        
        // Mapeia instâncias ricas
        Map<Long, ChatMessage> taskMap = new HashMap<>();
        for (ChatMessage m : withTask) taskMap.put(m.getId(), m);

        // Retorna timeline original com objetos ricos
        return all.stream()
                .map(m -> taskMap.getOrDefault(m.getId(), m))
                .toList();
    }

    private void initializeMessagesTaskGraph(List<ChatMessage> messagesWithTasks) {
        if (messagesWithTasks == null || messagesWithTasks.isEmpty()) return;

        // Extrai as tarefas únicas das mensagens
        java.util.List<Task> tasks = messagesWithTasks.stream()
                .filter(msg -> msg != null && msg.getTask() != null)
                .map(ChatMessage::getTask)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        // Delega o carregamento em lote para o TaskService
        taskService.initializeTaskData(tasks);
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<ChatMessage> searchMessages(Group group, String query, int page, int size) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return repository.searchMessages(group, query, PageRequest.of(page, size));
    }

    @Transactional
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void register(Long groupId, Consumer<ChatMessage> listener) {
        listeners.computeIfAbsent(groupId, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
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
