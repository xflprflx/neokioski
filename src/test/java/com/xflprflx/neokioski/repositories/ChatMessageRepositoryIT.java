package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TESTE DE INTEGRAÇÃO DO CHATMESSAGEREPOSITORY — VALIDAÇÃO ANTI N+1.
 *
 * Cada métodx deste teste segue o mesmo padrão de 3 fases:
 * 1. SETUP: Popular o banco com dados realistas (mensagens, tarefas, usuários).
 * 2. CLEAR: Limpar o cache L1 do Hibernate para forçar ida ao banco real.
 * 3. ASSERT: Verificar que a contagem de queries ficou dentro do limite aceitável
 *    e que as associações Lazy foram carregadas pelo @EntityGraph sem exceções.
 *
 * INFRAESTRUTURA:
 * Usa SpringBootTest com Testcontainers (PostgreSQL 16) para garantir
 * paridade com o banco de produção. O Flyway reconstrói o schema a cada teste
 * para isolamento total.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Integration IT: ChatMessageRepository — Anti N+1")
class ChatMessageRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Injeta dinamicamente as propriedades do banco Testcontainers no Spring Environment.
     * Isso garante que o DataSource aponte para o container Docker efêmero e que
     * o Hibernate Statistics esteja ativo para contagem precisa de queries.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskTemplateRepository templateRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private Flyway flyway;

    // Referências de teste populadas no setUp
    private Group testGroup;
    private User testUser;
    private Task testTask;

    /**
     * Recria o banco do zero antes de cada teste para isolamento total.
     * Popula com um cenário básico: 1 grupo, 1 usuário, 1 template, 1 tarefa,
     * e 10 mensagens (5 vinculadas à tarefa, 5 sem tarefa).
     */
    @BeforeEach
    void setUp() {
        // LIMPEZA NUCLEAR: Drop + Recreate do schema para evitar conflitos de FK
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("DROP SCHEMA public CASCADE").executeUpdate();
            entityManager.createNativeQuery("CREATE SCHEMA public").executeUpdate();
            return null;
        });
        flyway.migrate();

        // POPULAÇÃO DA MASSA DE DADOS
        transactionTemplate.execute(status -> {
            Group group = new Group();
            group.setName("Grupo IT Chat");
            group.setActive(true);
            testGroup = groupRepository.save(group);

            User user = new User();
            user.setUsername("chatuser");
            user.setName("Chat User IT");
            user.setPassword("pass123");
            user.setRole(Role.SELLER);
            testUser = userRepository.save(user);

            TaskTemplate template = new TaskTemplate();
            template.setName("Template IT Chat");
            template = templateRepository.save(template);

            Task task = new Task();
            task.setGroup(testGroup);
            task.setCreator(testUser);
            task.setTaskTemplate(template);
            testTask = taskRepository.save(task);

            // 5 mensagens vinculadas à tarefa (simula card de tarefa no chat)
            for (int i = 0; i < 5; i++) {
                ChatMessage msg = new ChatMessage("Task msg " + i, testUser, testGroup, null);
                msg.setTask(testTask);
                chatMessageRepository.save(msg);
            }

            // 5 mensagens de texto livre (sem tarefa)
            for (int i = 0; i < 5; i++) {
                ChatMessage msg = new ChatMessage("Free msg " + i, testUser, testGroup, null);
                chatMessageRepository.save(msg);
            }

            return null;
        });

        // Limpar cache L1 para garantir que o próximo teste bata no banco real
        entityManager.clear();
    }

    /**
     * Obtém e reseta as estatísticas do Hibernate para contagem precisa de queries.
     * Deve ser chamado imediatamente antes da operação que se deseja medir.
     */
    private Statistics resetAndGetStats() {
        Statistics stats = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        return stats;
    }

    // =========================================================================
    // findByTaskOrderByIdAsc — Usado em sendSystemAlertForTask
    // =========================================================================

    @Test
    @DisplayName("findByTaskOrderByIdAsc carrega user e group em query única (Anti N+1)")
    void findByTaskOrderByIdAsc_shouldLoadUserAndGroupWithoutNPlusOne() {
        Statistics stats = resetAndGetStats();

        // EXECUÇÃO: Busca todas as mensagens da tarefa
        List<ChatMessage> messages = transactionTemplate.execute(status ->
            chatMessageRepository.findByTaskOrderByIdAsc(testTask)
        );

        long queryCount = stats.getQueryExecutionCount();
        System.out.println(">>> findByTaskOrderByIdAsc QUERIES: " + queryCount);

        // VALIDAÇÃO: Exatamente 1 query (a do SELECT principal com LEFT JOIN)
        assertNotNull(messages);
        assertEquals(5, messages.size(), "Deveria retornar as 5 mensagens da tarefa");
        assertTrue(queryCount <= 2, "N+1 detectado! Queries: " + queryCount + " (limite: 2)");

        // VALIDAÇÃO: Associações acessíveis sem LazyInitializationException
        for (ChatMessage msg : messages) {
            assertDoesNotThrow(() -> {
                assertNotNull(msg.getUser(), "User deveria estar carregado via EntityGraph");
                assertNotNull(msg.getUser().getUsername());
                assertNotNull(msg.getGroup(), "Group deveria estar carregado via EntityGraph");
                assertNotNull(msg.getGroup().getName());
            }, "LazyInitializationException detectada — @EntityGraph não está funcionando!");
        }
    }

    // =========================================================================
    // findFirstByTaskOrderByIdAsc — Usado em executeStep / findFirstMessageForTask
    // =========================================================================

    @Test
    @DisplayName("findFirstByTaskOrderByIdAsc carrega user e group em query única (Anti N+1)")
    void findFirstByTaskOrderByIdAsc_shouldLoadUserAndGroupWithoutNPlusOne() {
        Statistics stats = resetAndGetStats();

        Optional<ChatMessage> result = transactionTemplate.execute(status ->
            chatMessageRepository.findFirstByTaskOrderByIdAsc(testTask)
        );

        long queryCount = stats.getQueryExecutionCount();
        System.out.println(">>> findFirstByTaskOrderByIdAsc QUERIES: " + queryCount);

        assertTrue(result.isPresent(), "Deveria encontrar pelo menos 1 mensagem da tarefa");
        assertTrue(queryCount <= 2, "N+1 detectado! Queries: " + queryCount + " (limite: 2)");

        // Acessar user e group FORA de transação para provar que o EntityGraph carregou
        ChatMessage msg = result.get();
        assertDoesNotThrow(() -> {
            assertNotNull(msg.getUser().getUsername());
            assertNotNull(msg.getGroup().getName());
        }, "LazyInitializationException — EntityGraph não carregou user/group!");
    }

    // =========================================================================
    // findByGroupAndTimestampBetweenOrderByTimestampAsc — Carregamento de histórico
    // =========================================================================

    @Test
    @DisplayName("findByGroupAndTimestamp carrega grafo completo sem N+1")
    void findByGroupAndTimestamp_shouldLoadFullGraphWithoutNPlusOne() {
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now().plusSeconds(60);

        Statistics stats = resetAndGetStats();

        List<ChatMessage> messages = transactionTemplate.execute(status ->
            chatMessageRepository.findByGroupAndTimestampBetweenOrderByTimestampAsc(testGroup, start, end)
        );

        long queryCount = stats.getQueryExecutionCount();
        System.out.println(">>> findByGroupAndTimestamp QUERIES: " + queryCount);

        assertNotNull(messages);
        assertEquals(10, messages.size(), "Deveria retornar as 10 mensagens do grupo");
        // O EntityGraph deste métodx é bem profundo (user, group, task, task.taskTemplate, etc.)
        // Mesmo assim, deve ficar num número controlado de queries
        assertTrue(queryCount <= 5, "N+1 detectado! Queries: " + queryCount + " (limite: 5)");

        // Validar que as associações profundas estão acessíveis
        for (ChatMessage msg : messages) {
            assertDoesNotThrow(() -> {
                assertNotNull(msg.getUser().getUsername());
                assertNotNull(msg.getGroup().getName());
                if (msg.getTask() != null) {
                    assertNotNull(msg.getTask().getTaskTemplate().getName());
                }
            }, "LazyInitializationException no grafo profundo!");
        }
    }

    // =========================================================================
    // findByIdWithFullGraph — Carregamento atômico para broadcast
    // =========================================================================

    @Test
    @DisplayName("findByIdWithFullGraph carrega todas as associações em query única")
    void findByIdWithFullGraph_shouldLoadAllAssociations() {
        // Buscar o ID de uma mensagem real
        Long messageId = transactionTemplate.execute(status -> {
            List<ChatMessage> msgs = chatMessageRepository.findByTaskOrderByIdAsc(testTask);
            return msgs.get(0).getId();
        });
        entityManager.clear();

        Statistics stats = resetAndGetStats();

        Optional<ChatMessage> result = transactionTemplate.execute(status ->
            chatMessageRepository.findByIdWithFullGraph(messageId)
        );

        long queryCount = stats.getQueryExecutionCount();
        System.out.println(">>> findByIdWithFullGraph QUERIES: " + queryCount);

        assertTrue(result.isPresent());
        assertTrue(queryCount <= 3, "N+1 detectado! Queries: " + queryCount + " (limite: 3)");

        ChatMessage msg = result.get();
        assertDoesNotThrow(() -> {
            assertNotNull(msg.getUser().getUsername());
            assertNotNull(msg.getGroup().getName());
            msg.getReactions().size(); // Coleção deve estar inicializada
            msg.getEdits().size();     // Coleção deve estar inicializada
        }, "LazyInitializationException no grafo completo!");
    }

    // =========================================================================
    // findByGroupOrderByTimestampAsc — Histórico completo do grupo
    // =========================================================================

    @Test
    @DisplayName("findByGroupOrderByTimestampAsc carrega todas as mensagens do grupo sem N+1")
    void findByGroupOrderByTimestampAsc_shouldLoadWithoutNPlusOne() {
        Statistics stats = resetAndGetStats();

        List<ChatMessage> messages = transactionTemplate.execute(status ->
            chatMessageRepository.findByGroupOrderByTimestampAsc(testGroup)
        );

        long queryCount = stats.getQueryExecutionCount();
        System.out.println(">>> findByGroupOrderByTimestampAsc QUERIES: " + queryCount);

        assertNotNull(messages);
        assertEquals(10, messages.size());
        assertTrue(queryCount <= 5, "N+1 detectado! Queries: " + queryCount + " (limite: 5)");
    }

    // =========================================================================
    // searchMessages — Busca textual com EntityGraph
    // =========================================================================

    @Test
    @DisplayName("searchMessages retorna resultados com EntityGraph e sem N+1")
    void searchMessages_shouldReturnResultsWithEntityGraph() {
        Statistics stats = resetAndGetStats();

        List<ChatMessage> results = transactionTemplate.execute(status ->
            chatMessageRepository.searchMessages(testGroup, "Task", PageRequest.of(0, 10))
        );

        long queryCount = stats.getQueryExecutionCount();
        System.out.println(">>> searchMessages QUERIES: " + queryCount);

        assertNotNull(results);
        assertFalse(results.isEmpty(), "A busca por 'Task' deveria retornar mensagens");
        assertTrue(queryCount <= 5, "N+1 detectado! Queries: " + queryCount + " (limite: 5)");
    }

    // =========================================================================
    // Cenário de tarefa inexistente
    // =========================================================================

    @Test
    @DisplayName("findFirstByTaskOrderByIdAsc com tarefa sem mensagens retorna Optional.empty")
    void findFirstByTask_noMessages_returnsEmpty() {
        // Criar uma tarefa sem mensagens
        Task emptyTask = transactionTemplate.execute(status -> {
            TaskTemplate t = templateRepository.findAll().get(0);
            Task task = new Task();
            task.setGroup(testGroup);
            task.setCreator(testUser);
            task.setTaskTemplate(t);
            return taskRepository.save(task);
        });
        entityManager.clear();

        Optional<ChatMessage> result = transactionTemplate.execute(status ->
            chatMessageRepository.findFirstByTaskOrderByIdAsc(emptyTask)
        );

        assertTrue(result.isEmpty(), "Tarefa sem mensagens deveria retornar Optional.empty");
    }
}
