package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.repositories.ChatMessageRepository;
import com.xflprflx.neokioski.repositories.MessageReactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: ChatService — Mensagens, Reações e Broadcast")
class ChatServiceTest {

    @Mock
    private ChatMessageRepository repository;

    @Mock
    private MessageReactionRepository reactionRepository;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private ChatService chatService;

    private Group group;
    private User user;
    private ChatMessage savedMessage;

    @BeforeEach
    void setUp() {
        group = new Group();
        group.setId(1L);

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole(Role.SELLER);

        savedMessage = new ChatMessage("Olá", user, group, null);
        savedMessage.setId(10L);
    }

    // =====================================================================
    // sendMessage
    // =====================================================================

    @Test
    @DisplayName("sendMessage(simples) delega para a assinatura completa e persiste")
    void sendMessage_simpleOverload_persistsMessage() {
        when(repository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessage result = chatService.sendMessage("Olá", user, group);

        assertNotNull(result);
        verify(repository).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("sendMessage(completo) persiste e define alertSound e alertRoles")
    void sendMessage_fullOverload_setsAlertFields() {
        when(repository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(10L);
            return m;
        });

        ChatMessage result = chatService.sendMessage("Alerta", user, group, null, "beep.mp3", "ADMIN");

        assertNotNull(result);
        assertEquals("beep.mp3", result.getAlertSound());
        assertEquals("ADMIN", result.getAlertRoles());
    }

    // =====================================================================
    // sendVolatileMessage
    // =====================================================================

    @Test
    @DisplayName("sendVolatileMessage não persiste no banco mas atribui ID negativo")
    void sendVolatileMessage_doesNotPersist_hasNegativeId() {
        ChatMessage result = chatService.sendVolatileMessage("Volátil", user, group, null, null, null);

        assertNotNull(result);
        assertTrue(result.getId() < 0, "Mensagem volátil deve ter ID negativo");
        verify(repository, never()).save(any());
    }

    // =====================================================================
    // sendMessageWithTask
    // =====================================================================

    @Test
    @DisplayName("sendMessageWithTask persiste mensagem vinculada à tarefa")
    void sendMessageWithTask_persistsMessageWithTask() {
        Task task = new Task();
        task.setId(99L);
        when(repository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(11L);
            return m;
        });

        ChatMessage result = chatService.sendMessageWithTask("Tarefa criada", user, group, task, null, null);

        assertNotNull(result);
        assertEquals(task, result.getTask());
        verify(repository).save(any(ChatMessage.class));
    }

    // =====================================================================
    // sendSystemAlertForTask
    // =====================================================================

    @Test
    @DisplayName("sendSystemAlertForTask(simples) delega para a assinatura completa")
    void sendSystemAlertForTask_simpleOverload_delegatesToFullSignature() {
        Task task = buildTask();
        when(repository.findByTaskOrderByIdAsc(task)).thenReturn(List.of());

        ChatMessage result = chatService.sendSystemAlertForTask("Alerta", task);

        assertNotNull(result);
    }

    @Test
    @DisplayName("sendSystemAlertForTask com conteúdo nulo usa mensagem padrão")
    void sendSystemAlertForTask_nullContent_usesDefaultMessage() {
        Task task = buildTask();
        when(repository.findByTaskOrderByIdAsc(task)).thenReturn(List.of());

        ChatMessage result = chatService.sendSystemAlertForTask(null, task, null, null);

        assertEquals("Alerta de tempo esgotado", result.getContent());
    }

    @Test
    @DisplayName("sendSystemAlertForTask com conteúdo em branco usa mensagem padrão")
    void sendSystemAlertForTask_blankContent_usesDefaultMessage() {
        Task task = buildTask();
        when(repository.findByTaskOrderByIdAsc(task)).thenReturn(List.of());

        ChatMessage result = chatService.sendSystemAlertForTask("   ", task, null, null);

        assertEquals("Alerta de tempo esgotado", result.getContent());
    }

    @Test
    @DisplayName("sendSystemAlertForTask com mensagem existente define replyTo")
    void sendSystemAlertForTask_existingMessages_setsReplyTo() {
        Task task = buildTask();
        ChatMessage firstMsg = new ChatMessage("Original", user, group, null);
        firstMsg.setId(1L);
        when(repository.findByTaskOrderByIdAsc(task)).thenReturn(List.of(firstMsg));

        ChatMessage result = chatService.sendSystemAlertForTask("Alerta", task, null, null);

        assertEquals(firstMsg, result.getReplyTo());
    }

    @Test
    @DisplayName("sendSystemAlertForTask sem mensagens existentes não define replyTo")
    void sendSystemAlertForTask_noExistingMessages_noReplyTo() {
        Task task = buildTask();
        when(repository.findByTaskOrderByIdAsc(task)).thenReturn(List.of());

        ChatMessage result = chatService.sendSystemAlertForTask("Alerta", task, null, null);

        assertNull(result.getReplyTo());
    }

    // =====================================================================
    // broadcastSoundOnly
    // =====================================================================

    @Test
    @DisplayName("broadcastSoundOnly com alertSound nulo não faz nada")
    void broadcastSoundOnly_nullSound_doesNothing() {
        chatService.broadcastSoundOnly(null, null, group);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("broadcastSoundOnly com alertSound vazio não faz nada")
    void broadcastSoundOnly_emptySound_doesNothing() {
        chatService.broadcastSoundOnly("", null, group);

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("broadcastSoundOnly com som válido emite para ouvintes registrados")
    void broadcastSoundOnly_validSound_broadcastsToListeners() {
        List<ChatMessage> received = new ArrayList<>();
        chatService.register(group.getId(), received::add);

        chatService.broadcastSoundOnly("beep.mp3", "ADMIN", group);

        assertEquals(1, received.size());
        assertEquals("beep.mp3", received.get(0).getAlertSound());
    }

    // =====================================================================
    // linkTaskToMessage
    // =====================================================================

    @Test
    @DisplayName("linkTaskToMessage com ID inexistente retorna null")
    void linkTaskToMessage_messageNotFound_returnsNull() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        ChatMessage result = chatService.linkTaskToMessage(999L, new Task());

        assertNull(result);
    }

    @Test
    @DisplayName("linkTaskToMessage com mensagem existente vincula tarefa e persiste")
    void linkTaskToMessage_messageFound_linksTaskAndSaves() {
        ChatMessage msg = new ChatMessage("Conteúdo", user, group, null);
        msg.setId(1L);
        Task task = new Task();
        task.setId(5L);
        when(repository.findById(1L)).thenReturn(Optional.of(msg));
        when(repository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessage result = chatService.linkTaskToMessage(1L, task);

        assertNotNull(result);
        assertEquals(task, result.getTask());
        verify(repository).save(msg);
    }

    // =====================================================================
    // getMessagesByGroup
    // =====================================================================

    @Test
    @DisplayName("getMessagesByGroup(com período) inicializa dados da tarefa nas mensagens com task")
    void getMessagesByGroup_withPeriod_initializesTaskData() {
        Task task = new Task();
        ChatMessage msgWithTask = new ChatMessage("X", user, group, null);
        msgWithTask.setTask(task);
        ChatMessage msgWithoutTask = new ChatMessage("Y", user, group, null);
        when(repository.findByGroupAndTimestampBetweenOrderByTimestampAsc(any(), any(), any()))
                .thenReturn(List.of(msgWithTask, msgWithoutTask));

        List<ChatMessage> result = chatService.getMessagesByGroup(group,
                java.time.Instant.now().minusSeconds(3600), java.time.Instant.now());

        assertEquals(2, result.size());
        verify(taskService).initializeTaskData(task);
    }

    @Test
    @DisplayName("getMessagesByGroup(sem período) inicializa dados da tarefa nas mensagens com task")
    void getMessagesByGroup_withoutPeriod_initializesTaskData() {
        Task task = new Task();
        ChatMessage msgWithTask = new ChatMessage("X", user, group, null);
        msgWithTask.setTask(task);
        when(repository.findByGroupOrderByTimestampAsc(group)).thenReturn(List.of(msgWithTask));

        List<ChatMessage> result = chatService.getMessagesByGroup(group);

        assertEquals(1, result.size());
        verify(taskService).initializeTaskData(task);
    }

    // =====================================================================
    // getMessageById
    // =====================================================================

    @Test
    @DisplayName("getMessageById com ID inexistente retorna Optional vazio")
    void getMessageById_notFound_returnsEmpty() {
        when(repository.findByIdWithFullGraph(999L)).thenReturn(Optional.empty());

        Optional<ChatMessage> result = chatService.getMessageById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getMessageById com mensagem vinculada a tarefa inicializa os dados da tarefa")
    void getMessageById_foundWithTask_initializesTaskData() {
        Task task = new Task();
        ChatMessage msg = new ChatMessage("X", user, group, null);
        msg.setTask(task);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));

        Optional<ChatMessage> result = chatService.getMessageById(1L);

        assertTrue(result.isPresent());
        verify(taskService).initializeTaskData(task);
    }

    @Test
    @DisplayName("getMessageById com mensagem sem tarefa não chama initializeTaskData")
    void getMessageById_foundWithoutTask_doesNotInitialize() {
        ChatMessage msg = new ChatMessage("X", user, group, null);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));

        chatService.getMessageById(1L);

        verify(taskService, never()).initializeTaskData(any());
    }

    // =====================================================================
    // searchMessages
    // =====================================================================

    @Test
    @DisplayName("searchMessages com mensagem contendo tarefa inicializa os dados da tarefa")
    void searchMessages_withTask_initializesTaskData() {
        Task task = new Task();
        ChatMessage msgWithTask = new ChatMessage("busca", user, group, null);
        msgWithTask.setTask(task);
        when(repository.searchMessages(any(), any(), any())).thenReturn(List.of(msgWithTask));

        List<ChatMessage> result = chatService.searchMessages(group, "busca", 0, 10);

        assertEquals(1, result.size());
        verify(taskService).initializeTaskData(task);
    }

    // =====================================================================
    // toggleReaction
    // =====================================================================

    @Test
    @DisplayName("toggleReaction adiciona reação quando ela não existe")
    void toggleReaction_reactionDoesNotExist_addsReaction() {
        ChatMessage msg = new ChatMessage("Oi", user, group, null);
        msg.setId(1L);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));
        when(reactionRepository.findByChatMessageAndUserAndEmoji(msg, user, "👍"))
                .thenReturn(Optional.empty());

        chatService.toggleReaction(1L, user, "👍");

        verify(reactionRepository).save(any(MessageReaction.class));
        assertEquals(1, msg.getReactions().size());
    }

    @Test
    @DisplayName("toggleReaction remove reação quando ela já existe")
    void toggleReaction_reactionExists_removesReaction() {
        ChatMessage msg = new ChatMessage("Oi", user, group, null);
        msg.setId(1L);
        MessageReaction existing = new MessageReaction("👍", user, msg);
        msg.getReactions().add(existing);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));
        when(reactionRepository.findByChatMessageAndUserAndEmoji(msg, user, "👍"))
                .thenReturn(Optional.of(existing));

        chatService.toggleReaction(1L, user, "👍");

        verify(reactionRepository).delete(existing);
        assertTrue(msg.getReactions().isEmpty());
    }

    // =====================================================================
    // editMessage
    // =====================================================================

    @Test
    @DisplayName("editMessage pelo autor da mensagem atualiza o conteúdo e persiste")
    void editMessage_authorEditsMessage_updatesContent() {
        ChatMessage msg = new ChatMessage("Original", user, group, null);
        msg.setId(1L);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));

        chatService.editMessage(1L, "Editado", user);

        assertEquals("Editado", msg.getContent());
        verify(repository).save(msg);
    }

    @Test
    @DisplayName("editMessage por usuário diferente não altera a mensagem")
    void editMessage_wrongUser_doesNotEdit() {
        User otherUser = new User();
        otherUser.setId(2L);
        ChatMessage msg = new ChatMessage("Original", user, group, null);
        msg.setId(1L);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));

        chatService.editMessage(1L, "Tentativa", otherUser);

        assertEquals("Original", msg.getContent());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("editMessage em mensagem já excluída não realiza edição")
    void editMessage_deletedMessage_doesNotEdit() {
        ChatMessage msg = new ChatMessage("Original", user, group, null);
        msg.setId(1L);
        msg.setDeleted(true);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));

        chatService.editMessage(1L, "Editado", user);

        assertEquals("Original", msg.getContent());
        verify(repository, never()).save(any());
    }

    // =====================================================================
    // deleteMessage
    // =====================================================================

    @Test
    @DisplayName("deleteMessage pelo autor marca mensagem como excluída e persiste")
    void deleteMessage_authorDeletesOwnMessage_marksAsDeleted() {
        ChatMessage msg = new ChatMessage("Mensagem", user, group, null);
        msg.setId(1L);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chatService.deleteMessage(1L, user);

        assertTrue(msg.isDeleted());
        verify(repository).save(msg);
    }

    @Test
    @DisplayName("deleteMessage por ADMIN em mensagem de outro usuário é permitido")
    void deleteMessage_adminDeletesOthersMessage_marksAsDeleted() {
        User admin = new User();
        admin.setId(2L);
        admin.setRole(Role.ADMIN);
        ChatMessage msg = new ChatMessage("Mensagem", user, group, null);
        msg.setId(1L);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        chatService.deleteMessage(1L, admin);

        assertTrue(msg.isDeleted());
        verify(repository).save(msg);
    }

    @Test
    @DisplayName("deleteMessage por usuário sem permissão não exclui a mensagem")
    void deleteMessage_unauthorizedUser_doesNotDelete() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setRole(Role.SELLER);
        ChatMessage msg = new ChatMessage("Mensagem", user, group, null);
        msg.setId(1L);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));

        chatService.deleteMessage(1L, otherUser);

        assertFalse(msg.isDeleted());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("deleteMessage em mensagem já excluída não duplo-exclui")
    void deleteMessage_alreadyDeleted_doesNothing() {
        ChatMessage msg = new ChatMessage("Mensagem", user, group, null);
        msg.setId(1L);
        msg.setDeleted(true);
        when(repository.findByIdWithFullGraph(1L)).thenReturn(Optional.of(msg));

        chatService.deleteMessage(1L, user);

        verify(repository, never()).save(any());
    }

    // =====================================================================
    // register / unregister
    // =====================================================================

    @Test
    @DisplayName("register adiciona ouvinte que recebe mensagens do grupo")
    void register_listenerReceivesMessages() {
        List<ChatMessage> received = new ArrayList<>();
        chatService.register(group.getId(), received::add);
        when(repository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        chatService.sendMessage("Teste", user, group);

        assertEquals(1, received.size());
    }

    @Test
    @DisplayName("unregister remove ouvinte que para de receber mensagens")
    void unregister_listenerStopsReceivingMessages() {
        List<ChatMessage> received = new ArrayList<>();
        Consumer<ChatMessage> listener = received::add;
        chatService.register(group.getId(), listener);
        chatService.unregister(group.getId(), listener);
        when(repository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        chatService.sendMessage("Teste", user, group);

        assertTrue(received.isEmpty());
    }

    @Test
    @DisplayName("unregister para grupo sem ouvintes não lança exceção")
    void unregister_noListenersForGroup_doesNotThrow() {
        assertDoesNotThrow(() -> chatService.unregister(999L, msg -> {}));
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    private Task buildTask() {
        Task task = new Task();
        task.setId(1L);
        task.setGroup(group);
        return task;
    }
}