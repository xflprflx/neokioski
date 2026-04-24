package com.xflprflx.neokioski.performance;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.enums.SlaStatus;
import com.xflprflx.neokioski.models.enums.TaskSlaUnit;
import com.xflprflx.neokioski.models.template.TaskSlaConfig;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import com.xflprflx.neokioski.repositories.*;
import com.xflprflx.neokioski.services.DashboardService;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.transaction.support.TransactionTemplate;
import org.flywaydb.core.Flyway;

import java.time.Instant;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TESTE DE INTEGRAÇÃO DE PERFORMANCE (ANTI N+1) E FUNCIONAL.
 * Este teste utiliza um banco de dados real (PostgreSQL via Docker) para garantir que
 * o Dashboard seja rápido (poucas queries) e correto (cálculos precisos).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // Sobe o contexto Spring sem subir um servidor HTTP real
@ActiveProfiles("test") // Utiliza o arquivo application-test.yml para configurações específicas
public class DashboardPerformanceIT {

    // Define um container Docker do PostgreSQL versão 16 Alpine (leve e estável)
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    // Bloco estático para garantir que o container suba ANTES do Spring tentar se conectar ao banco
    static {
        postgres.start();
    }

    /**
     * Registra as propriedades do banco de dados dinamicamente.
     * Como o Testcontainers sobe o banco em uma porta aleatória, precisamos ler essa porta
     * e injetar nas configurações do Spring em tempo de execução.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl); // URL de conexão (jdbc:postgresql://localhost:PORTA/...)
        registry.add("spring.datasource.username", postgres::getUsername); // Usuário padrão do container
        registry.add("spring.datasource.password", postgres::getPassword); // Senha padrão do container
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true"); // Habilita métricas do Hibernate para contar queries
        registry.add("spring.flyway.enabled", () -> "true"); // Garante que o Flyway rode as migrações no banco do container
    }

    // Injeção de dependências do Spring
    @Autowired
    private DashboardService dashboardService; // O serviço que vamos testar

    @Autowired
    private EntityManager entityManager; // Interface do JPA para interagir com o banco e métricas do Hibernate

    @Autowired
    private TransactionTemplate transactionTemplate; // Auxiliar para executar códigos dentro de uma transação manual

    @Autowired
    private Flyway flyway; // Gerenciador de migrações para reconstruir o banco

    // Repositórios necessários para popular os dados de teste
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private TaskTemplateRepository templateRepository;
    @Autowired
    private TaskStepDefinitionRepository stepDefinitionRepository;
    @Autowired
    private TemplateStepRelationRepository stepRelationRepository;
    @Autowired
    private TaskSlaConfigRepository slaConfigRepository;

    // Variáveis de suporte que serão preenchidas no setUp e usadas no teste
    private Group group;
    private TaskTemplate template;
    private User user;
    private TaskStepDefinition step;
    private TaskSlaConfig slaConfig;

    /**
     * Executado antes de CADA métodx de teste.
     * Prepara um ambiente 100% limpo e com dados de exemplo.
     */
    @BeforeEach
    void setUp() {
        // ESTRATÉGIA NUCLEAR: Limpeza total do banco de dados para evitar conflitos de FK ou dados sujos
        transactionTemplate.execute(status -> {
            // Dropa o schema public (apaga tudo: tabelas, constraints, sequências)
            entityManager.createNativeQuery("DROP SCHEMA public CASCADE").executeUpdate();
            // Recria o schema public vazio
            entityManager.createNativeQuery("CREATE SCHEMA public").executeUpdate();
            return null;
        });
        
        // Roda o Flyway para criar todas as tabelas e constraints na versão mais recente
        flyway.migrate();

        // Criação da massa de dados em uma transação para garantir persistência imediata
        transactionTemplate.execute(status -> {
            // 1. Cria um Grupo (Necessário para o filtro do dashboard)
            group = new Group();
            group.setName("Grupo Teste IT");
            group = groupRepository.save(group);

            // 2. Cria um Usuário (O criador das tarefas)
            user = new User();
            user.setName("Usuário Teste IT");
            user.setUsername("testit");
            user.setPassword("password");
            user.setRole(Role.ADMIN);
            user = userRepository.save(user);

            // 3. Cria um Template de Tarefa
            template = new TaskTemplate();
            template.setName("Template Teste IT");
            template = templateRepository.save(template);

            // 4. Cria uma Definição de Passo (Etapa da tarefa)
            step = new TaskStepDefinition();
            step.setLabel("PASSO FINALIZADOR DELIVERY");
            step.setColorTheme("blue"); // Campo obrigatório no Postgres
            step = stepDefinitionRepository.save(step);

            // 5. Relaciona o Passo ao Template (Necessário para a lógica do Dashboard encontrar concluidores)
            TemplateStepRelation rel = new TemplateStepRelation();
            rel.setTaskTemplate(template);
            rel.setTaskStepDefinition(step);
            rel.setHasResponseField(true);
            rel.setResponseFieldLabel("DELIVERY");
            stepRelationRepository.save(rel);

            // 6. Configura o SLA (Tempo limite) para este template
            slaConfig = new TaskSlaConfig();
            slaConfig.setTaskTemplate(template);
            slaConfig.setName("SLA Teste IT"); // Campo obrigatório
            slaConfig.setTargetValue(60); // 60 unidades
            slaConfig.setUnit(TaskSlaUnit.MINUTES); // 60 minutos
            slaConfig.setValidFrom(Instant.now()); // Início da validade agora
            slaConfig = slaConfigRepository.save(slaConfig);

            // 7. Cria 10 TAREFAS para teste (5 concluídas e 5 ativas)
            for (int i = 0; i < 10; i++) {
                Task task = new Task();
                task.setTaskTemplate(template);
                task.setCreator(user);
                task.setGroup(group);
                task.setCreatedAt(Instant.now());
                
                // Finaliza as primeiras 5 tarefas para termos métricas de "concluídas"
                if (i < 5) {
                    task.setFinalizedAt(Instant.now());
                }

                task.setSlaResults(new LinkedHashSet<>());
                task.setTaskEvents(new LinkedHashSet<>());
                
                // Cria um resultado de SLA para cada tarefa
                TaskSlaResult sla = new TaskSlaResult();
                // Simula um atraso (DELAYED) apenas na primeira tarefa concluída para testar o KPI de atraso
                sla.setStatus(i == 0 ? SlaStatus.DELAYED : SlaStatus.ON_TIME);
                sla.setTargetValue(60);
                sla.setUnit(TaskSlaUnit.MINUTES);
                sla.setActualSeconds(1200L); // Tempo gasto real
                sla.setCompletedAt(Instant.now());
                sla.setSlaConfig(slaConfig);
                sla.setTask(task);
                task.getSlaResults().add(sla); // Adiciona na coleção da Task (Cascata salvará o SLA)

                // Simula um evento de conclusão para cada tarefa
                TaskEvent event = new TaskEvent();
                event.setUser(user);
                event.setTaskStepDefinition(step);
                event.setTimestamp(Instant.now());
                task.getTaskEvents().add(event); // Adiciona na coleção da Task (Cascata salvará o Evento)

                taskRepository.save(task); // Salva a tarefa e todos os seus relacionamentos (SLA e Eventos)
            }
            return null;
        });

        // Limpa o cache de primeiro nível do Hibernate para forçar o DashboardService a ir no banco de verdade
        entityManager.clear();
    }

    /**
     * O TESTE PRINCIPAL: Verifica se o Dashboard carrega dados corretos sem explodir em queries (Anti N+1).
     */
    @Test
    void shouldLoadDashboardDataWithoutNPlusOne() {
        // Define o intervalo de busca (últimas 24 horas até 1 hora no futuro)
        Instant start = Instant.now().minusSeconds(86400);
        Instant end = Instant.now().plusSeconds(3600);

        // Acessa as estatísticas de baixo nível do Hibernate
        Session session = entityManager.unwrap(Session.class);
        Statistics stats = session.getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true); // Liga o contador de queries
        stats.clear(); // Zera o contador

        // EXECUÇÃO: Chama o métodx principal do serviço que queremos validar
        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, group, null, null, null, null);

        // CONTROLE: Quantas queries o banco executou para gerar esse dashboard?
        long queryCount = stats.getQueryExecutionCount();
        System.out.println(">>> DASHBOARD PERFORMANCE CHECK: Executadas " + queryCount + " queries.");
        
        // 1. VALIDAÇÃO DE PERFORMANCE (Limite de 5 queries para evitar o problema N+1)
        assertTrue(queryCount <= 5,
            "REGRESSÃO DE PERFORMANCE: O dashboard executou " + queryCount + 
            " queries (N+1 detectado!). O limite é 5.");

        // 2. VALIDAÇÃO FUNCIONAL (Garante que os cálculos matemáticos do serviço estão certos)
        assertNotNull(data, "O retorno do dashboard não deve ser nulo");
        assertNotNull(data.stats(), "As estatísticas do dashboard não devem ser nulas");
        
        // Verifica se os KPIs batem com o que populamos no setUp
        assertEquals(5, data.stats().activeTasks(), "Deveria haver 5 tarefas ativas (finalizedAt == null)");
        assertEquals(5, data.stats().completedTasks(), "Deveria haver 5 tarefas concluídas (finalizedAt != null)");
        assertEquals(4, data.stats().completedOnTime(), "Deveria haver 4 tarefas concluídas no prazo (Status != DELAYED)");
        assertEquals(1, data.stats().completedDelayed(), "Deveria haver 1 tarefa atrasada (Status == DELAYED)");
        assertEquals(80.0, data.stats().onTimePercentage(), 0.1, "O percentual no prazo deveria ser 80% (4 de 5)");
        
        // 3. VALIDAÇÃO DE RANKINGS (Garante que a agregação SQL/Stream funcionou)
        assertFalse(data.userRanking().isEmpty(), "O ranking de usuários não deveria estar vazio");
        assertEquals("Usuário Teste IT", data.userRanking().get(0).label(), "O usuário correto deveria estar no topo");
        assertEquals(10.0, data.userRanking().get(0).value(), "O usuário deveria ter o total de 10 tasks atribuídas");
    }
}
