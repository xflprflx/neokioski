package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.TaskEvent;
import com.xflprflx.neokioski.models.TaskSlaResult;
import com.xflprflx.neokioski.models.enums.SlaStatus;
import com.xflprflx.neokioski.models.enums.TaskSlaUnit;
import com.xflprflx.neokioski.models.template.TaskSlaConfig;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.repositories.TaskSlaConfigRepository;
import com.xflprflx.neokioski.repositories.TaskSlaResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: TaskSlaService — Cálculo e Gerenciamento de SLA")
class TaskSlaServiceTest {

    @Mock
    private TaskSlaConfigRepository configRepository;

    @Mock
    private TaskSlaResultRepository resultRepository;

    @InjectMocks
    private TaskSlaService service;

    private Task task;
    private TaskTemplate template;
    private TaskStepDefinition targetStep;

    @BeforeEach
    void setUp() {
        template = new TaskTemplate();
        template.setId(1L);

        targetStep = new TaskStepDefinition();
        targetStep.setId(5L);

        task = new Task();
        task.setId(1L);
        task.setTaskTemplate(template);
        task.setCreatedAt(Instant.now().minusSeconds(7200)); // 2 horas atrás
        task.setTaskEvents(new LinkedHashSet<>());
        task.setSlaResults(new LinkedHashSet<>());
    }

    // =====================================================================
    // calculateAndSaveSla
    // =====================================================================

    @Test
    @DisplayName("calculateAndSaveSla - step correspondente é processado e resultado é salvo (DELAYED)")
    void calculateAndSaveSla_matchingStep_savesDelayedResult() {
        TaskSlaConfig config = buildConfig(targetStep, null, TaskSlaUnit.HOURS, 1);
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateAndSaveSla(task, targetStep);

        ArgumentCaptor<TaskSlaResult> captor = ArgumentCaptor.forClass(TaskSlaResult.class);
        verify(resultRepository).save(captor.capture());
        assertEquals(SlaStatus.DELAYED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("calculateAndSaveSla - step correspondente dentro do prazo gera status ON_TIME")
    void calculateAndSaveSla_matchingStep_savesOnTimeResult() {
        task.setCreatedAt(Instant.now().minusSeconds(1800)); // 30 min atrás
        TaskSlaConfig config = buildConfig(targetStep, null, TaskSlaUnit.HOURS, 1); // alvo: 1h
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateAndSaveSla(task, targetStep);

        ArgumentCaptor<TaskSlaResult> captor = ArgumentCaptor.forClass(TaskSlaResult.class);
        verify(resultRepository).save(captor.capture());
        assertEquals(SlaStatus.ON_TIME, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("calculateAndSaveSla - step diferente do config não gera processamento")
    void calculateAndSaveSla_nonMatchingStep_doesNotSave() {
        TaskStepDefinition otherStep = new TaskStepDefinition();
        otherStep.setId(99L);
        TaskSlaConfig config = buildConfig(otherStep, null, TaskSlaUnit.HOURS, 1);
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateAndSaveSla(task, targetStep);

        verify(resultRepository, never()).save(any());
    }

    @Test
    @DisplayName("calculateAndSaveSla - config com targetStep nulo é ignorado no fluxo de step")
    void calculateAndSaveSla_configWithNullTargetStep_ignored() {
        TaskSlaConfig config = buildConfig(null, null, TaskSlaUnit.HOURS, 1);
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateAndSaveSla(task, targetStep);

        verify(resultRepository, never()).save(any());
    }

    @Test
    @DisplayName("calculateAndSaveSla - unidade MINUTES calcula corretamente")
    void calculateAndSaveSla_minutesUnit_calculatesCorrectTargetSeconds() {
        task.setCreatedAt(Instant.now().minusSeconds(3700)); // >1h01 atrás
        TaskSlaConfig config = buildConfig(targetStep, null, TaskSlaUnit.MINUTES, 60); // alvo: 60 min
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateAndSaveSla(task, targetStep);

        ArgumentCaptor<TaskSlaResult> captor = ArgumentCaptor.forClass(TaskSlaResult.class);
        verify(resultRepository).save(captor.capture());
        assertEquals(SlaStatus.DELAYED, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("calculateAndSaveSla - unidade SECONDS calcula corretamente")
    void calculateAndSaveSla_secondsUnit_calculatesCorrectTargetSeconds() {
        task.setCreatedAt(Instant.now().minusSeconds(100));
        TaskSlaConfig config = buildConfig(targetStep, null, TaskSlaUnit.SECONDS, 3600); // alvo: 3600s
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateAndSaveSla(task, targetStep);

        ArgumentCaptor<TaskSlaResult> captor = ArgumentCaptor.forClass(TaskSlaResult.class);
        verify(resultRepository).save(captor.capture());
        assertEquals(SlaStatus.ON_TIME, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("calculateAndSaveSla - com sourceStep definido usa timestamp do evento como início")
    void calculateAndSaveSla_withSourceStep_usesEventTimestampAsStart() {
        TaskStepDefinition sourceStep = new TaskStepDefinition();
        sourceStep.setId(3L);

        TaskEvent event = new TaskEvent();
        event.setTaskStepDefinition(sourceStep);
        event.setTimestamp(Instant.now().minusSeconds(1800)); // 30 min atrás
        task.getTaskEvents().add(event);

        TaskSlaConfig config = buildConfig(targetStep, sourceStep, TaskSlaUnit.HOURS, 1); // alvo: 1h
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateAndSaveSla(task, targetStep);

        ArgumentCaptor<TaskSlaResult> captor = ArgumentCaptor.forClass(TaskSlaResult.class);
        verify(resultRepository).save(captor.capture());
        assertEquals(SlaStatus.ON_TIME, captor.getValue().getStatus()); // 30min < 1h
    }

    @Test
    @DisplayName("calculateAndSaveSla - sourceStep definido mas sem evento correspondente não processa")
    void calculateAndSaveSla_sourceStepWithNoMatchingEvent_doesNotSave() {
        TaskStepDefinition sourceStep = new TaskStepDefinition();
        sourceStep.setId(3L);
        // Nenhum evento com esse step

        TaskSlaConfig config = buildConfig(targetStep, sourceStep, TaskSlaUnit.HOURS, 1);
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateAndSaveSla(task, targetStep);

        verify(resultRepository, never()).save(any());
    }

    @Test
    @DisplayName("calculateAndSaveSla - resultado é adicionado à coleção de SLA da tarefa")
    void calculateAndSaveSla_savesResultToTaskSlaResults() {
        TaskSlaConfig config = buildConfig(targetStep, null, TaskSlaUnit.HOURS, 1);
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));
        when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculateAndSaveSla(task, targetStep);

        assertEquals(1, task.getSlaResults().size());
    }

    // =====================================================================
    // calculateFinalTaskSla
    // =====================================================================

    @Test
    @DisplayName("calculateFinalTaskSla - tarefa sem finalizedAt retorna sem processar")
    void calculateFinalTaskSla_nullFinalizedAt_doesNothing() {
        task.setFinalizedAt(null);

        service.calculateFinalTaskSla(task);

        verifyNoInteractions(configRepository);
        verifyNoInteractions(resultRepository);
    }

    @Test
    @DisplayName("calculateFinalTaskSla - config de tarefa total (targetStep nulo) é processado")
    void calculateFinalTaskSla_configWithNullTargetStep_savesResult() {
        task.setCreatedAt(Instant.now().minusSeconds(3600)); // 1h atrás
        task.setFinalizedAt(Instant.now());
        TaskSlaConfig config = buildConfig(null, null, TaskSlaUnit.HOURS, 2); // alvo: 2h → ON_TIME
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateFinalTaskSla(task);

        ArgumentCaptor<TaskSlaResult> captor = ArgumentCaptor.forClass(TaskSlaResult.class);
        verify(resultRepository).save(captor.capture());
        assertEquals(SlaStatus.ON_TIME, captor.getValue().getStatus());
    }

    @Test
    @DisplayName("calculateFinalTaskSla - config com targetStep não nulo é ignorado")
    void calculateFinalTaskSla_configWithNonNullTargetStep_ignored() {
        task.setFinalizedAt(Instant.now());
        TaskSlaConfig config = buildConfig(targetStep, null, TaskSlaUnit.HOURS, 1);
        when(configRepository.findValidConfigs(any(), any())).thenReturn(List.of(config));

        service.calculateFinalTaskSla(task);

        verify(resultRepository, never()).save(any());
    }

    // =====================================================================
    // getResultsForTask
    // =====================================================================

    @Test
    @DisplayName("getResultsForTask delega ao repositório de resultados")
    void getResultsForTask_delegatesToRepository() {
        TaskSlaResult result = new TaskSlaResult();
        when(resultRepository.findByTask(task)).thenReturn(List.of(result));

        List<TaskSlaResult> results = service.getResultsForTask(task);

        assertEquals(1, results.size());
        verify(resultRepository).findByTask(task);
    }

    // =====================================================================
    // updateJustification
    // =====================================================================

    @Test
    @DisplayName("updateJustification - ID não encontrado lança RuntimeException")
    void updateJustification_notFound_throwsException() {
        when(resultRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> service.updateJustification(99L, "justificativa", "comentários"));
    }

    @Test
    @DisplayName("updateJustification - resultado encontrado atualiza justificativa e persiste")
    void updateJustification_found_updatesAndSaves() {
        TaskSlaResult result = new TaskSlaResult();
        result.setId(1L);
        when(resultRepository.findById(1L)).thenReturn(Optional.of(result));

        service.updateJustification(1L, "Motivo X", "Comentário Y");

        assertEquals("Motivo X", result.getJustification());
        assertEquals("Comentário Y", result.getJustificationComments());
        verify(resultRepository).save(result);
    }

    // =====================================================================
    // getPendingJustifications
    // =====================================================================

    @Test
    @DisplayName("getPendingJustifications - slaResults nulo retorna lista vazia")
    void getPendingJustifications_nullSlaResults_returnsEmpty() {
        task.setSlaResults(null);

        List<TaskSlaResult> pending = service.getPendingJustifications(task);

        assertTrue(pending.isEmpty());
    }

    @Test
    @DisplayName("getPendingJustifications - resultados sem atraso são excluídos")
    void getPendingJustifications_onTimeResults_returnsEmpty() {
        TaskSlaResult onTime = new TaskSlaResult();
        onTime.setStatus(SlaStatus.ON_TIME);
        task.getSlaResults().add(onTime);

        List<TaskSlaResult> pending = service.getPendingJustifications(task);

        assertTrue(pending.isEmpty());
    }

    @Test
    @DisplayName("getPendingJustifications - resultado atrasado com justificativa preenchida é excluído")
    void getPendingJustifications_delayedWithJustification_excluded() {
        TaskSlaConfig config = new TaskSlaConfig();
        config.setJustificationOptions("Opção A, Opção B");

        TaskSlaResult delayed = new TaskSlaResult();
        delayed.setStatus(SlaStatus.DELAYED);
        delayed.setSlaConfig(config);
        delayed.setJustification("Opção A");
        task.getSlaResults().add(delayed);

        List<TaskSlaResult> pending = service.getPendingJustifications(task);

        assertTrue(pending.isEmpty());
    }

    @Test
    @DisplayName("getPendingJustifications - resultado atrasado sem justificativa e com opções é incluído")
    void getPendingJustifications_delayedWithoutJustification_included() {
        TaskSlaConfig config = new TaskSlaConfig();
        config.setJustificationOptions("Opção A, Opção B");

        TaskSlaResult delayed = new TaskSlaResult();
        delayed.setStatus(SlaStatus.DELAYED);
        delayed.setSlaConfig(config);
        delayed.setJustification(null);
        task.getSlaResults().add(delayed);

        List<TaskSlaResult> pending = service.getPendingJustifications(task);

        assertEquals(1, pending.size());
    }

    @Test
    @DisplayName("getPendingJustifications - resultado atrasado sem config de justificativa é excluído")
    void getPendingJustifications_delayedWithNoJustificationOptions_excluded() {
        TaskSlaConfig config = new TaskSlaConfig();
        config.setJustificationOptions(null);

        TaskSlaResult delayed = new TaskSlaResult();
        delayed.setStatus(SlaStatus.DELAYED);
        delayed.setSlaConfig(config);
        task.getSlaResults().add(delayed);

        List<TaskSlaResult> pending = service.getPendingJustifications(task);

        assertTrue(pending.isEmpty());
    }

    // =====================================================================
    // Fábrica de auxiliares de teste
    // =====================================================================

    private TaskSlaConfig buildConfig(TaskStepDefinition targetStep,
                                      TaskStepDefinition sourceStep,
                                      TaskSlaUnit unit,
                                      int targetValue) {
        TaskSlaConfig config = new TaskSlaConfig();
        config.setTargetStep(targetStep);
        config.setSourceStep(sourceStep);
        config.setUnit(unit);
        config.setTargetValue(targetValue);
        return config;
    }
}