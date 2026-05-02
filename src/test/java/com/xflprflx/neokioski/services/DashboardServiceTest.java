package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.enums.SlaStatus;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import com.xflprflx.neokioski.repositories.TaskRepository;
import com.xflprflx.neokioski.repositories.TaskSlaResultRepository;
import com.xflprflx.neokioski.repositories.TaskTemplateRepository;
import com.xflprflx.neokioski.repositories.TemplateStepRelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * TESTE UNITÁRIO DE DOMÍNIO: DashboardService.
 * 
 * INTENCIONALIDADE:
 * Este componente valida a camada de agregação e inteligência de negócio do Dashboard.
 * Diferente dos testes de integração (IT), o foco aqui é a pureza algorítmica:
 * 1. Agrupamento de KPIs (Tarefas Ativas vs Concluídas).
 * 2. Cálculo de métricas de SLA (On Time vs Delayed).
 * 3. Lógica de Rankings e identificação de atores (Criadores e Finalizadores).
 * 
 * ESTRATÉGIA DE ISOLAMENTO (MOCKING):
 * Utilizamos o Mockito para desacoplar o serviço de suas dependências de infraestrutura (JPA/Database).
 * Isso garante que o teste seja:
 * - Determinístico: O resultado depende apenas do estado injetado nos Mocks.
 * - De Alta Performance: Execução em memória sem bootstrapping de banco ou containers.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: Lógica do Dashboard (KPIs e Rankings)")
public class DashboardServiceTest {

    /**
     * Injeção de dependências via Mockito. 
     * Os repositórios são mockados para isolar a lógica de agregação do Java 
     * da complexidade das consultas SQL reais.
     */
    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private TaskSlaResultRepository slaResultRepository;
    
    @Mock
    private TaskTemplateRepository templateRepository;
    
    @Mock
    private TemplateStepRelationRepository stepRelationRepository;

    /**
     * @InjectMocks cria uma instância REAL do DashboardService e "injeta" nela
     * os objetos @Mock criados acima. Assim, quando o serviço chamar um repositório,
     * ele chamará o nosso objeto falso controlado.
     */
    @InjectMocks
    private DashboardService dashboardService;

    // Objetos de modelo que usaremos para construir nossos cenários de teste
    private Group groupA;
    private TaskTemplate templateA;
    private User user1;
    private User user2;
    private TaskStepDefinition stepFinalizer;

    /**
     * Configuração que roda antes de cada métodx de @Test.
     * Serve para inicializar objetos básicos e evitar repetição de código.
     */
    @BeforeEach
    void setUp() {
        // Inicializa um grupo fictício (Damos um ID manual para simular persistência)
        groupA = new Group();
        groupA.setId(1L);
        groupA.setName("Grupo A");

        // Inicializa um template fictício
        templateA = new TaskTemplate();
        templateA.setId(1L);
        templateA.setName("Template A");

        // Inicializa usuários (Simulando registros que viriam do banco)
        user1 = new User();
        user1.setId(1L);
        user1.setName("User 1");
        user1.setUsername("user1");

        user2 = new User();
        user2.setId(2L);
        user2.setName("User 2");
        user2.setUsername("user2");

        // Cria uma definição de passo que usaremos para testar a identificação de quem concluiu a tarefa
        stepFinalizer = new TaskStepDefinition();
        stepFinalizer.setId(10L);
        stepFinalizer.setLabel("PASSO DELIVERY");
    }

    /**
     * CENÁRIO 1: Validação de Estatísticas Básicas (KPIs).
     * Testa se o serviço separa corretamente tarefas Ativas de Concluídas e calcula o SLA.
     */
    @Test
    @DisplayName("Cálculo de Estatísticas Básicas (KPIs de Ativas vs Concluídas)")
    void shouldCalculateBasicStatsCorrectly() {
        // --- 1. ARRANGE (Preparação/Configuração) ---
        Instant start = Instant.now().minusSeconds(3600); // Filtro: de 1 hora atrás
        Instant end = Instant.now(); // Até agora

        // Criamos t1: Uma tarefa que está ATIVA (finalizedAt é null)
        Task t1 = createTask(1L, user1, null, groupA, templateA); 
        
        // Criamos t2: Uma tarefa que está CONCLUÍDA (finalizedAt tem valor)
        Task t2 = createTask(2L, user1, Instant.now(), groupA, templateA); 
        
        // Forçamos um atraso em t2 para testar se o contador de "Atrasadas" funciona
        TaskSlaResult sla = new TaskSlaResult();
        sla.setStatus(SlaStatus.DELAYED); // Status: ATRASADO
        t2.getSlaResults().add(sla); 

        // ENSINANDO O MOCK (Stubbing): 
        // "Quando o repositório for chamado para buscar tarefas entre QUAISQUER datas, retorne t1 e t2"
        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(Arrays.asList(t1, t2));
        
        // Criamos um metadado falso para popular o cache de relações do serviço
        TemplateStepRelation rel = new TemplateStepRelation();
        rel.setTaskTemplate(templateA);
        rel.setTaskStepDefinition(stepFinalizer);
        rel.setHasResponseField(true);
        rel.setResponseFieldLabel("DELIVERY");
        
        // "Quando o repositório buscar relações para os templates, retorne este metadado"
        when(stepRelationRepository.findByTaskTemplateIdIn(any())).thenReturn(Collections.singletonList(rel));

        // --- 2. ACT (Ação/Execução) ---
        // Chamamos o métodx que queremos testar. Ele usará nossos Mocks internamente.
        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, groupA, null, null, null, null, null, null, null);

        // --- 3. ASSERT (Afirmação/Validação) ---
        // Verificamos se a "matemática" do serviço transformou os dados brutos dos mocks
        // nas estatísticas esperadas para a interface do usuário.
        
        // Validação de Integridade: Esperamos 1 tarefa ativa e 1 concluída
        assertAll("Verificação de KPIs Básicos",
            () -> assertEquals(1, data.stats().activeTasks(), "A contagem de tarefas ativas está errada"),
            () -> assertEquals(1, data.stats().completedTasks(), "A contagem de tarefas concluídas está errada"),
            () -> assertEquals(0, data.stats().completedOnTime(), "Deveria haver 0 tarefas no prazo"),
            () -> assertEquals(1, data.stats().completedDelayed(), "Deveria haver 1 tarefa atrasada")
        );
    }

    /**
     * CENÁRIO 2: Validação de Rankings de Criadores.
     * Garante que o serviço agrupa as tarefas por usuário e ordena os mais produtivos.
     */
    @Test
    @DisplayName("Cálculo de Ranking de Criadores (Volume de Tarefas por Usuário)")
    void shouldCalculateRankingCorrectly() {
        // --- 1. ARRANGE ---
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Cenário: User 1 criou 2 tarefas, User 2 criou apenas 1 tarefa
        Task t1 = createTask(1L, user1, null, groupA, templateA);
        Task t2 = createTask(2L, user2, null, groupA, templateA);
        Task t3 = createTask(3L, user1, null, groupA, templateA);

        // Configura o mock para retornar as 3 tarefas acima
        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(Arrays.asList(t1, t2, t3));
        
        // --- 2. ACT ---
        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, groupA, null, null, null, null, null, null, null);

        // --- 3. ASSERT ---
        // O ranking deve conter apenas os 2 usuários envolvidos
        assertEquals(2, data.userRanking().size(), "O ranking deveria ter 2 usuários");
        
        // O User 1 deve ser o primeiro da lista porque tem mais tarefas (2 vs 1)
        assertEquals("User 1", data.userRanking().get(0).label(), "O primeiro do ranking deveria ser o User 1");
        assertEquals(2.0, data.userRanking().get(0).value(), "O User 1 deveria ter valor 2.0 (2 tarefas)");
    }

    /**
     * CENÁRIO 3: Identificação de Finalizador (Concluidor).
     * Este é o teste mais complexo: verifica se o sistema olha para os eventos da tarefa
     * e descobre quem executou o passo marcado como "Delivery" nos metadados.
     */
    @Test
    @DisplayName("Identificação de Finalizador de Tarefa via Eventos (Cache de Relações)")
    void shouldIdentifyFinalizerUsingCache() {
        // --- 1. ARRANGE ---
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        // Tarefa concluída criada pelo User 1
        Task t1 = createTask(1L, user1, Instant.now(), groupA, templateA);
        
        // Simulamos que o User 2 foi quem executou a ação de entrega (evento)
        TaskEvent event = new TaskEvent();
        event.setUser(user2);
        event.setTaskStepDefinition(stepFinalizer); // Vinculamos ao passo de Delivery
        t1.getTaskEvents().add(event);

        // Ensinamos o mock a retornar t1
        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(Collections.singletonList(t1));
        
        // REGRA DE OURO: Configuramos a relação que ensina o Dashboard: 
        // "O passo 'stepFinalizer' é o que define o fechamento da tarefa"
        TemplateStepRelation rel = new TemplateStepRelation();
        rel.setTaskTemplate(templateA);
        rel.setTaskStepDefinition(stepFinalizer);
        rel.setHasResponseField(true);
        rel.setResponseFieldLabel("CONCLUIR DELIVERY"); // String chave que o serviço procura
        
        // Ensinamos o mock de relações a retornar essa regra
        when(stepRelationRepository.findByTaskTemplateIdIn(any())).thenReturn(Collections.singletonList(rel));

        // --- 2. ACT ---
        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, groupA, null, null, null, null, null, null, null);

        // --- 3. ASSERT ---
        // Mesmo t1 tendo sido criada pelo User 1, quem deve aparecer no ranking de FINALIZADORES é o User 2
        assertEquals(1, data.finalizerRanking().size(), "Deveria haver 1 finalizador no ranking");
        assertEquals("User 2", data.finalizerRanking().get(0).label(), "O finalizador identificado deveria ser o User 2");
    }

    /**
     * métodx AUXILIAR (Factory).
     * Centraliza a criação de objetos Task para manter o código dos testes limpo.
     */
    private Task createTask(Long id, User creator, Instant finalizedAt, Group group, TaskTemplate template) {
        Task task = new Task();
        task.setId(id);
        task.setCreator(creator);
        task.setFinalizedAt(finalizedAt);
        task.setGroup(group);
        task.setTaskTemplate(template);
        task.setCreatedAt(Instant.now());
        // Inicializamos as coleções vazias para evitar NullPointerException ao adicionar itens
        task.setSlaResults(new LinkedHashSet<>());
        task.setTaskEvents(new LinkedHashSet<>());
        return task;
    }
}
