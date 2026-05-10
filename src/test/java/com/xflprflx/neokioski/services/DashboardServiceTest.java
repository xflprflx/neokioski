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
 * Unit Test: Lógica do Dashboard.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: Lógica do Dashboard (KPIs e Rankings)")
public class DashboardServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TaskSlaResultRepository slaResultRepository;
    @Mock private TaskTemplateRepository templateRepository;
    @Mock private TemplateStepRelationRepository stepRelationRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Group groupA;
    private TaskTemplate templateA;
    private User user1;
    private User user2;
    private TaskStepDefinition stepFinalizer;

    @BeforeEach
    void setUp() {
        groupA = new Group();
        groupA.setId(1L);
        groupA.setName("Grupo A");

        templateA = new TaskTemplate();
        templateA.setId(1L);
        templateA.setName("Template A");

        user1 = new User();
        user1.setId(1L);
        user1.setName("User 1");
        user1.setUsername("user1");

        user2 = new User();
        user2.setId(2L);
        user2.setName("User 2");
        user2.setUsername("user2");

        stepFinalizer = new TaskStepDefinition();
        stepFinalizer.setId(10L);
        stepFinalizer.setLabel("PASSO DELIVERY");
    }

    @Test
    @DisplayName("Cálculo de Estatísticas Básicas (KPIs de Ativas vs Concluídas)")
    void shouldCalculateBasicStatsCorrectly() {
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        Task t1 = createTask(1L, user1, null, groupA, templateA); 
        Task t2 = createTask(2L, user1, Instant.now(), groupA, templateA); 
        
        TaskSlaResult sla = new TaskSlaResult();
        sla.setStatus(SlaStatus.DELAYED);
        t2.getSlaResults().add(sla); 

        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(Arrays.asList(t1, t2));
        when(stepRelationRepository.findByTaskTemplateIdIn(any())).thenReturn(Collections.emptyList());

        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, groupA, null, null, null, null, null, null, null);

        assertAll("Verificação de KPIs Básicos",
            () -> assertEquals(1, data.stats().activeTasks()),
            () -> assertEquals(1, data.stats().completedTasks()),
            () -> assertEquals(1, data.stats().completedDelayed())
        );
    }

    @Test
    @DisplayName("Cálculo de Ranking de Criadores")
    void shouldCalculateRankingCorrectly() {
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        Task t1 = createTask(1L, user1, null, groupA, templateA);
        Task t2 = createTask(2L, user2, null, groupA, templateA);
        Task t3 = createTask(3L, user1, null, groupA, templateA);

        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(Arrays.asList(t1, t2, t3));
        
        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, groupA, null, null, null, null, null, null, null);

        assertEquals(2, data.userRanking().size());
        assertEquals("User 1", data.userRanking().get(0).label());
    }

    @Test
    @DisplayName("Geração de Nuvem de Palavras")
    void shouldGenerateWordCloudCorrectly() {
        Task t1 = new Task();
        t1.setTaskTemplate(templateA);
        TaskSlaResult r1 = new TaskSlaResult();
        r1.setStatus(SlaStatus.DELAYED);
        r1.setJustificationComments("Problema na rede e falha na rede");
        t1.getSlaResults().add(r1);

        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(Collections.singletonList(t1));
        when(stepRelationRepository.findByTaskTemplateIdIn(any())).thenReturn(Collections.emptyList());

        DashboardService.DashboardData data = dashboardService.getDashboardData(Instant.now(), Instant.now(), null, null, null, null, null, null, null, null);

        boolean foundRede = data.wordCloud().stream().anyMatch(w -> w.justification().equalsIgnoreCase("rede") && w.count() == 2L);
        assertTrue(foundRede);
    }

    @Test
    @DisplayName("Aplicação de Filtros de Busca")
    void shouldApplyFiltersCorrectly() {
        Task t1 = createTask(1L, user1, null, groupA, templateA);
        TaskSlaResult r1 = new TaskSlaResult();
        r1.setStatus(SlaStatus.DELAYED);
        r1.setJustificationComments("Texto de busca");
        t1.getSlaResults().add(r1);

        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(Collections.singletonList(t1));

        // Corrigido: keywordFilter é o 10º argumento
        DashboardService.DashboardData data = dashboardService.getDashboardData(Instant.now(), Instant.now(), groupA, templateA, null, null, null, null, null, "texto");
        assertEquals(1, data.stats().activeTasks());

        DashboardService.DashboardData dataEmpty = dashboardService.getDashboardData(Instant.now(), Instant.now(), groupA, templateA, null, null, null, null, null, "nada");
        assertEquals(0, dataEmpty.stats().activeTasks());
    }

    @Test
    @DisplayName("Ranking de Concluidores deve priorizar countsAsCompletion sobre isFinalizer")
    void shouldPrioritizeCountsAsCompletionOverFinalizer() {
        // Given: template com step "EM ANDAMENTO" (countsAsCompletion=true) e "COLETADO" (isFinalizer=true)
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        TaskStepDefinition stepAndamento = new TaskStepDefinition();
        stepAndamento.setId(20L);
        stepAndamento.setLabel("EM ANDAMENTO");
        stepAndamento.setColorTheme("azul");

        TaskStepDefinition stepColetado = new TaskStepDefinition();
        stepColetado.setId(21L);
        stepColetado.setLabel("COLETADO");
        stepColetado.setColorTheme("verde");

        TemplateStepRelation relAndamento = new TemplateStepRelation();
        relAndamento.setTaskStepDefinition(stepAndamento);
        relAndamento.setCountsAsCompletion(true);
        relAndamento.setFinalizer(false);
        TaskTemplate tmpl = new TaskTemplate();
        tmpl.setId(1L);
        tmpl.setName("Template A");
        relAndamento.setTaskTemplate(tmpl);

        TemplateStepRelation relColetado = new TemplateStepRelation();
        relColetado.setTaskStepDefinition(stepColetado);
        relColetado.setCountsAsCompletion(false);
        relColetado.setFinalizer(true);
        relColetado.setTaskTemplate(tmpl);

        // Tarefa com dois eventos: user1 fez EM ANDAMENTO, user2 fez COLETADO
        Task t1 = createTask(1L, user1, Instant.now(), groupA, tmpl);

        TaskEvent evAndamento = new TaskEvent();
        evAndamento.setUser(user1);
        evAndamento.setTaskStepDefinition(stepAndamento);
        evAndamento.setTimestamp(Instant.now().minusSeconds(60));
        evAndamento.setTask(t1);
        t1.getTaskEvents().add(evAndamento);

        TaskEvent evColetado = new TaskEvent();
        evColetado.setUser(user2);
        evColetado.setTaskStepDefinition(stepColetado);
        evColetado.setTimestamp(Instant.now());
        evColetado.setTask(t1);
        t1.getTaskEvents().add(evColetado);

        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(List.of(t1));
        when(stepRelationRepository.findByTaskTemplateIdIn(any())).thenReturn(List.of(relAndamento, relColetado));

        // When
        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, groupA, null, null, null, null, null, null, null);

        // Then: o concluidor deve ser user1 (EM ANDAMENTO com countsAsCompletion) e NÃO user2 (COLETADO com isFinalizer)
        assertEquals(1, data.finalizerRanking().size(), "Deveria haver exatamente 1 concluidor no ranking");
        assertEquals("User 1", data.finalizerRanking().get(0).label(),
                "Concluidor deveria ser User 1 (step EM ANDAMENTO com countsAsCompletion=true), e não User 2 (isFinalizer)");
    }

    @Test
    @DisplayName("Ranking de Concluidores deve usar isFinalizer como fallback quando nenhum step tem countsAsCompletion")
    void shouldFallbackToFinalizerWhenNoCountsAsCompletion() {
        // Given: template onde nenhum step tem countsAsCompletion, apenas isFinalizer
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        TaskStepDefinition stepColetado = new TaskStepDefinition();
        stepColetado.setId(21L);
        stepColetado.setLabel("COLETADO");
        stepColetado.setColorTheme("verde");

        TemplateStepRelation relColetado = new TemplateStepRelation();
        relColetado.setTaskStepDefinition(stepColetado);
        relColetado.setCountsAsCompletion(false);
        relColetado.setFinalizer(true);
        TaskTemplate tmpl = new TaskTemplate();
        tmpl.setId(1L);
        tmpl.setName("Template A");
        relColetado.setTaskTemplate(tmpl);

        Task t1 = createTask(1L, user1, Instant.now(), groupA, tmpl);

        TaskEvent evColetado = new TaskEvent();
        evColetado.setUser(user2);
        evColetado.setTaskStepDefinition(stepColetado);
        evColetado.setTimestamp(Instant.now());
        evColetado.setTask(t1);
        t1.getTaskEvents().add(evColetado);

        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(List.of(t1));
        when(stepRelationRepository.findByTaskTemplateIdIn(any())).thenReturn(List.of(relColetado));

        // When
        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, groupA, null, null, null, null, null, null, null);

        // Then: fallback para isFinalizer — concluidor deve ser user2
        assertEquals(1, data.finalizerRanking().size());
        assertEquals("User 2", data.finalizerRanking().get(0).label(),
                "Sem countsAsCompletion, o fallback deve usar isFinalizer para identificar o concluidor");
    }

    @Test
    @DisplayName("Tarefas sem step de conclusão atingido NÃO devem contabilizar no ranking de concluidores")
    void shouldNotCountTasksWithoutCompletionStep() {
        // Given: template com step "EM ANDAMENTO" (não é conclusão nem finalizador) e "COLETADO" (isFinalizer)
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();

        TaskStepDefinition stepAndamento = new TaskStepDefinition();
        stepAndamento.setId(20L);
        stepAndamento.setLabel("EM ANDAMENTO");
        stepAndamento.setColorTheme("azul");

        TaskStepDefinition stepColetado = new TaskStepDefinition();
        stepColetado.setId(21L);
        stepColetado.setLabel("COLETADO");
        stepColetado.setColorTheme("verde");

        TemplateStepRelation relAndamento = new TemplateStepRelation();
        relAndamento.setTaskStepDefinition(stepAndamento);
        relAndamento.setCountsAsCompletion(false);
        relAndamento.setFinalizer(false);
        TaskTemplate tmpl = new TaskTemplate();
        tmpl.setId(1L);
        tmpl.setName("Template A");
        relAndamento.setTaskTemplate(tmpl);

        TemplateStepRelation relColetado = new TemplateStepRelation();
        relColetado.setTaskStepDefinition(stepColetado);
        relColetado.setCountsAsCompletion(false);
        relColetado.setFinalizer(true);
        relColetado.setTaskTemplate(tmpl);

        // Tarefa 1: recém-criada (só tem evento de criação, sem step)
        Task t1 = createTask(1L, user1, null, groupA, tmpl);
        TaskEvent evCriacao = new TaskEvent();
        evCriacao.setUser(user1);
        evCriacao.setTaskStepDefinition(null); // evento de criação não tem step
        evCriacao.setTimestamp(Instant.now().minusSeconds(120));
        evCriacao.setTask(t1);
        t1.getTaskEvents().add(evCriacao);

        // Tarefa 2: em andamento (ainda NÃO atingiu o step finalizador)
        Task t2 = createTask(2L, user1, null, groupA, tmpl);
        TaskEvent evCriacao2 = new TaskEvent();
        evCriacao2.setUser(user1);
        evCriacao2.setTaskStepDefinition(null);
        evCriacao2.setTimestamp(Instant.now().minusSeconds(120));
        evCriacao2.setTask(t2);
        t2.getTaskEvents().add(evCriacao2);

        TaskEvent evAndamento = new TaskEvent();
        evAndamento.setUser(user2);
        evAndamento.setTaskStepDefinition(stepAndamento);
        evAndamento.setTimestamp(Instant.now().minusSeconds(60));
        evAndamento.setTask(t2);
        t2.getTaskEvents().add(evAndamento);

        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(List.of(t1, t2));
        when(stepRelationRepository.findByTaskTemplateIdIn(any())).thenReturn(List.of(relAndamento, relColetado));

        // When
        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, groupA, null, null, null, null, null, null, null);

        // Then: nenhuma tarefa atingiu o step de conclusão ou finalizador — ranking vazio
        assertTrue(data.finalizerRanking().isEmpty(),
                "Tarefas recém-criadas ou em andamento (sem atingir step de conclusão) NÃO devem aparecer no ranking de concluidores");
    }

    @Test
    @DisplayName("Cálculo de TMA deve IGNORAR passos marcados como finalizadores")
    void shouldExcludeFinalizerStepsFromTmaCalculation() {
        // Given: template com um passo intermediário (100s) e um finalizador (50s)
        Instant start = Instant.now().minusSeconds(7200);
        Instant end = Instant.now();

        TaskStepDefinition stepIntermediario = new TaskStepDefinition();
        stepIntermediario.setId(30L);
        stepIntermediario.setLabel("INTERMEDIARIO");

        TaskStepDefinition stepFinal = new TaskStepDefinition();
        stepFinal.setId(31L);
        stepFinal.setLabel("FINAL");

        TemplateStepRelation relInter = new TemplateStepRelation();
        relInter.setTaskStepDefinition(stepIntermediario);
        relInter.setFinalizer(false);
        relInter.setTaskTemplate(templateA);

        TemplateStepRelation relFinal = new TemplateStepRelation();
        relFinal.setTaskStepDefinition(stepFinal);
        relFinal.setFinalizer(true); // FINALIZADOR
        relFinal.setTaskTemplate(templateA);

        Task t1 = createTask(1L, user1, Instant.now(), groupA, templateA);
        
        // Evento 1: Começa INTERMEDIARIO às T-200s
        TaskEvent ev1 = new TaskEvent();
        ev1.setTaskStepDefinition(stepIntermediario);
        ev1.setTimestamp(Instant.now().minusSeconds(200));
        ev1.setTask(t1);
        t1.getTaskEvents().add(ev1);

        // Evento 2: Muda para FINAL às T-100s (Duração INTERMEDIARIO = 100s)
        TaskEvent ev2 = new TaskEvent();
        ev2.setTaskStepDefinition(stepFinal);
        ev2.setTimestamp(Instant.now().minusSeconds(100));
        ev2.setTask(t1);
        t1.getTaskEvents().add(ev2);

        // Finaliza às T-50s (Duração FINAL = 50s)
        t1.setFinalizedAt(Instant.now().minusSeconds(50));

        when(taskRepository.findAllByCreatedAtBetween(any(), any())).thenReturn(List.of(t1));
        when(stepRelationRepository.findByTaskTemplateIdIn(any())).thenReturn(List.of(relInter, relFinal));

        // When
        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, groupA, null, null, null, null, null, null, null);

        // Then: TMA per Step deve conter apenas INTERMEDIARIO (100s)
        var tmaData = data.tmaPerStep().get(0);
        assertTrue(tmaData.seriesValues().containsKey("INTERMEDIARIO"));
        assertEquals(100.0, tmaData.seriesValues().get("INTERMEDIARIO"), 1.0, "Duração do intermediário deve ser 100s");
        
        assertFalse(tmaData.seriesValues().containsKey("FINAL"), "Passo finalizador não deve constar no breakdown de TMA");
    }

    private Task createTask(Long id, User creator, Instant finalizedAt, Group group, TaskTemplate template) {
        Task task = new Task();
        task.setId(id);
        task.setCreator(creator);
        task.setFinalizedAt(finalizedAt);
        task.setGroup(group);
        task.setTaskTemplate(template);
        task.setCreatedAt(Instant.now());
        task.setSlaResults(new LinkedHashSet<>());
        task.setTaskEvents(new LinkedHashSet<>());
        return task;
    }
}
