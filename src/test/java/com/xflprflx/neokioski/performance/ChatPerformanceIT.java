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
import com.xflprflx.neokioski.services.TaskService;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
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
import org.flywaydb.core.Flyway;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@ActiveProfiles("test")
class ChatPerformanceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

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
    private TaskService taskService;

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

    @BeforeEach
    void setUp() {
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("DROP SCHEMA public CASCADE").executeUpdate();
            entityManager.createNativeQuery("CREATE SCHEMA public").executeUpdate();
            return null;
        });
        flyway.migrate();

        transactionTemplate.execute(status -> {
            Group group = new Group();
            group.setName("Test Group");
            group.setActive(true);
            testGroup = groupRepository.save(group);
            
            User user = new User();
            user.setUsername("tester");
            user.setName("Tester");
            user.setPassword("pass");
            user.setRole(Role.SELLER);
            user = userRepository.save(user);
            
            TaskTemplate template = new TaskTemplate();
            template.setName("Test Template");
            template = templateRepository.save(template);
            
            // Criar algumas mensagens com tarefas para testar a inicialização
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
        
        entityManager.clear();
    }

    @Test
    void getMessagesByGroup_ShouldLoadDataEfficientlyAndAvoidLazyExceptions() {
        Statistics stats = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        // Executar a busca fora de uma transação do teste para simular o comportamento da UI
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
