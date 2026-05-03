package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.template.TaskAlert;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.repositories.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link AlertReloader}.
 * Explica o "porquê" das validações para garantir que todx alerta seja reativado no start.
 * 
 * Premissas:
 * 1. Apenas tarefas não finalizadas devem ser processadas.
 * 2. Somente tarefas com templates que possuem alertas configurados devem disparar o agendamento.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Recarregador de Alertas - Testes de Cobertura")
class AlertReloaderTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskAlertService taskAlertService;

    @InjectMocks
    private AlertReloader alertReloader;

    @Test
    @DisplayName("Deve finalizar silenciosamente quando não houver tarefas pendentes")
    void shouldDoNothingWhenNoPendingTasks() {
        // Given: Um cenário onde o banco de dados não retorna nenhuma tarefa aberta
        when(taskRepository.findAllPending()).thenReturn(Collections.emptyList());

        // When: O métodx de recarregamento é acionado (simulando ApplicationReadyEvent)
        alertReloader.reloadPendingAlerts();

        // Then: Nenhuma chamada deve ser feita ao serviço de agendamento em RAM
        verify(taskAlertService, never()).scheduleAlertsForTask(any());
    }

    @Test
    @DisplayName("Não deve agendar alertas para tarefas que não possuem um template associado")
    void shouldNotScheduleWhenTaskHasNoTemplate() {
        // Given: Uma tarefa pendente sem template (inconsistência de dados)
        Task task = mock(Task.class);
        when(task.getTaskTemplate()).thenReturn(null);
        when(taskRepository.findAllPending()).thenReturn(List.of(task));

        // When: O métodx processa a tarefa
        alertReloader.reloadPendingAlerts();

        // Then: O agendamento deve ser ignorado para evitar NullPointerException
        verify(taskAlertService, never()).scheduleAlertsForTask(any());
    }

    @Test
    @DisplayName("Não deve agendar alertas se o template da tarefa não tiver alertas configurados")
    void shouldNotScheduleWhenTemplateHasNoAlerts() {
        // Given: Tarefas cujos templates existem, mas não possuem regras de alerta definidas
        Task taskWithNullAlerts = mock(Task.class);
        TaskTemplate templateNull = mock(TaskTemplate.class);
        when(taskWithNullAlerts.getTaskTemplate()).thenReturn(templateNull);
        when(templateNull.getAlerts()).thenReturn(null);

        Task taskWithEmptyAlerts = mock(Task.class);
        TaskTemplate templateEmpty = mock(TaskTemplate.class);
        when(taskWithEmptyAlerts.getTaskTemplate()).thenReturn(templateEmpty);
        when(templateEmpty.getAlerts()).thenReturn(Collections.emptySet());

        when(taskRepository.findAllPending()).thenReturn(List.of(taskWithNullAlerts, taskWithEmptyAlerts));

        // When: O métodx itera sobre todx a lista
        alertReloader.reloadPendingAlerts();

        // Then: Nenhuma tarefa deve ser agendada no serviço de RAM
        verify(taskAlertService, never()).scheduleAlertsForTask(any());
    }

    @Test
    @DisplayName("Deve agendar alertas corretamente para tarefas válidas com configurações de alerta")
    void shouldScheduleAlertsForValidTasksWithAlerts() {
        // Given: Uma tarefa legítima com um template contendo 2 alertas
        Task task = mock(Task.class);
        TaskTemplate template = mock(TaskTemplate.class);
        TaskAlert alert1 = mock(TaskAlert.class);
        TaskAlert alert2 = mock(TaskAlert.class);
        
        when(task.getTaskTemplate()).thenReturn(template);
        when(template.getAlerts()).thenReturn(Set.of(alert1, alert2));
        when(taskRepository.findAllPending()).thenReturn(List.of(task));

        // When: O sistema inicia e o recarregador é invocado
        alertReloader.reloadPendingAlerts();

        // Then: O serviço de agendamento deve ser chamado exatamente uma vez para esta tarefa
        verify(taskAlertService, times(1)).scheduleAlertsForTask(task);
    }

    @Test
    @DisplayName("Deve processar múltiplas tarefas de forma independente, agendando apenas as que possuem alertas")
    void shouldProcessMultipleTasksIndependently() {
        // Given: Um mix de tarefas (uma válida com alerta e uma inválida sem alerta)
        Task validTask = mock(Task.class);
        TaskTemplate validTemplate = mock(TaskTemplate.class);
        when(validTask.getTaskTemplate()).thenReturn(validTemplate);
        when(validTemplate.getAlerts()).thenReturn(Set.of(mock(TaskAlert.class)));

        Task invalidTask = mock(Task.class); // Sem template ou alertas
        
        when(taskRepository.findAllPending()).thenReturn(List.of(validTask, invalidTask));

        // When: O métodx percorre todx a lista de pendências
        alertReloader.reloadPendingAlerts();

        // Then: Apenas a tarefa válida deve ser encaminhada para agendamento
        verify(taskAlertService, times(1)).scheduleAlertsForTask(validTask);
        verify(taskAlertService, never()).scheduleAlertsForTask(invalidTask);
    }
}
