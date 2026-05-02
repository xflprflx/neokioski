package com.xflprflx.neokioski.performance;

import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.repositories.GroupRepository;
import com.xflprflx.neokioski.repositories.TaskRepository;
import com.xflprflx.neokioski.repositories.TaskTemplateRepository;
import com.xflprflx.neokioski.repositories.UserRepository;
import com.xflprflx.neokioski.services.ChatService;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de Integração de Performance para o Sistema de Chat.
 * 
 * FOCO:
 * Este teste não valida apenas a funcionalidade, mas a EFICIÊNCIA do código.
 * Ele foca em dois problemas clássicos de ORM (JPA/Hibernate):
 * 1. Problema N+1: Quando o carregamento de uma lista dispara múltiplas queries
 * adicionais.
 * 2. LazyInitializationException: Quando a UI tenta acessar dados que não foram
 * carregados na transação.
 * 
 * INFRAESTRUTURA:
 * Usa Testcontainers para garantir que as estatísticas de query sejam idênticas
 * ao banco real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Performance IT: Eficiência de Carregamento do Chat")
class ChatPerformanceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * MAPEAMENTO DINÂMICO DE PROPRIEDADES (DYNAMIC PROPERTY SOURCE).
     * 
     * Por que este métodx é estático e usa @DynamicPropertySource?
     * No Testcontainers, o banco de dados sobe em uma porta aleatória do host para evitar conflitos
     * se houver outros bancos rodando. Como o Spring Boot precisa saber a URL exata ANTES de 
     * criar o DataSource, este métodx intercepta a inicialização e injeta as propriedades reais
     * vindas do container Docker que acabamos de subir.
     * 
     * O que estamos configurando aqui:
     * 1. spring.datasource.*: URL, User e Password para o Hibernate/JPA.
     * 2. spring.flyway.*: Garante que as migrações de banco rodem no mesmo container.
     * 3. generate_statistics: Ativa o contador de queries do Hibernate (usado no Assert do teste).
     * 
     * @param registry O registro de propriedades do Spring Environment.
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
    private ChatService chatService;

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

    private Group testGroup;

    /**
     * CONFIGURAÇÃO DE AMBIENTE (SET UP).
     * 
     * Este métodx prepara um cenário 100% limpo e controlado antes de cada teste.
     * Ele segue a estratégia de "Isolamento Total", garantindo que dados de um teste
     * anterior não influenciem os resultados de performance deste.
     */
    @BeforeEach
    void setUp() {
        // 1. ESTRATÉGIA NUCLEAR DE LIMPEZA
        // Em vez de deletar linha por linha (lento e complexo devido a FKs), 
        // dropamos o schema 'public' inteiro e o recriamos. Isso é instantâneo no Postgres.
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("DROP SCHEMA public CASCADE").executeUpdate();
            entityManager.createNativeQuery("CREATE SCHEMA public").executeUpdate();
            return null;
        });
        
        // 2. RECONSTRUÇÃO DO ESQUEMA
        // Roda as migrações do Flyway para criar todas as tabelas e constraints na versão atual.
        flyway.migrate();

        // 3. POPULAÇÃO DA MASSA DE DADOS (DATA FIXTURE)
        // Criamos as entidades básicas necessárias para simular um chat real.
        transactionTemplate.execute(status -> {
            // Criando o Grupo de teste
            Group group = new Group();
            group.setName("Test Group");
            group.setActive(true);
            testGroup = groupRepository.save(group);

            // Criando o Usuário (Autor das mensagens)
            User user = new User();
            user.setUsername("tester");
            user.setName("Tester");
            user.setPassword("pass");
            user.setRole(Role.SELLER);
            user = userRepository.save(user);

            // Criando o Template da Tarefa (Contexto do negócio)
            TaskTemplate template = new TaskTemplate();
            template.setName("Test Template");
            template = templateRepository.save(template);

            // 4. SIMULAÇÃO DE CARGA
            // Criamos 5 mensagens com tarefas associadas para testar se o carregamento
            // do histórico do chat será eficiente.
            for (int i = 0; i < 5; i++) {
                Task task = new Task();
                task.setGroup(testGroup);
                task.setCreator(user);
                task.setTaskTemplate(template);
                task = taskRepository.save(task);

                chatService.sendMessageWithTask("Task " + i, user, testGroup, task, null, null);
            }
            return null;
        });

        // 5. LIMPEZA DO CACHE (L1 CACHE)
        // O EntityManager do JPA mantém objetos em memória. Para testar performance REAL
        // e garantir que o serviço irá ao banco de dados, precisamos limpar esse cache.
        entityManager.clear();
    }

    /**
     * Este teste é crucial para a UX e estabilidade do servidor.
     *
     * CENÁRIO:
     * Carregamos o histórico de chat de um grupo que possui múltiplas mensagens
     * e tarefas associadas.
     *
     * VALIDAÇÃO 1 (EFICIÊNCIA):
     * Usamos a API de Statistics do Hibernate para contar quantas queries o
     * ChatService disparou. Se disparar muitas (ex: uma por mensagem), o teste
     * falha.
     * Isso força o uso de 'JOIN FETCH' ou '@EntityGraph' no Repository.
     *
     * VALIDAÇÃO 2 (ESTADO):
     * Tentamos acessar coleções Lazy (SLA, Eventos) FORA da transação do serviço.
     * Isso garante que o serviço fez o "Deep Touch" ou "Prefetch" necessário,
     * evitando que a tela do usuário apresente erro de
     * "LazyInitializationException".
     */
    @Test
    @DisplayName("Carregar mensagens por grupo e validar N+1 e Lazy Loading")
    void getMessagesByGroup_ShouldLoadDataEfficientlyAndAvoidLazyExceptions() {
        Statistics stats = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // Executar a busca fora de uma transação do teste para simular o comportamento
        // da UI
        List<ChatMessage> messages = chatService.getMessagesByGroup(testGroup);

        assertNotNull(messages);
        assertFalse(messages.isEmpty());

        // Verificar N+1 e Explosão Cartesiana
        long queryCount = stats.getQueryExecutionCount();
        System.out.println(">>> CHAT LOAD QUERIES: " + queryCount);
        assertTrue(queryCount < 20, "Carregamento do chat disparou muitas queries (" + queryCount + ")");

        // VERIFICAÇÃO CRÍTICA: Tentar acessar coleções lazy FORA da sessão do serviço
        for (ChatMessage msg : messages) {
            if (msg.getTask() != null) {
                Task task = msg.getTask();
                assertDoesNotThrow(() -> {
                    task.getSlaResults().size();
                    task.getTaskEvents().size();
                    if (task.getTaskTemplate() != null) {
                        task.getTaskTemplate().getTemplateStepRelation().size();
                    }
                }, "Erro de LazyInitializationException detectado no carregamento da tarefa no chat!");
            }
        }
    }
}
