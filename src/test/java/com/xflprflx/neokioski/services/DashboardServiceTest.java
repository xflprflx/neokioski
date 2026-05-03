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
