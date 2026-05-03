package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.enums.TaskSlaUnit;
import com.xflprflx.neokioski.models.template.TaskAlert;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.repositories.TaskRepository;
import com.xflprflx.neokioski.repositories.TaskTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskAlertService Unit Tests")
class TaskAlertServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ChatService chatService;
    @Mock private TaskScheduler taskScheduler;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private TaskTemplateRepository taskTemplateRepository;
    @Mock private TaskService taskService;

    private TaskAlertService alertService;

    @BeforeEach
    void setUp() throws Exception {
        alertService = new TaskAlertService(
                taskRepository,
                chatService,
                taskScheduler,
                transactionTemplate,
                taskTemplateRepository,
                taskService
        );

        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            if (callback != null) {
                callback.accept(mock(TransactionStatus.class));
            }
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("scheduleAlertsForTask - deve agendar alertas para o futuro")
    void shouldScheduleAlertsInFuture() {
        Task task = new Task();
        task.setId(1L);
        task.setCreatedAt(Instant.now());

        TaskTemplate template = new TaskTemplate();
        template.setId(1L);
        task.setTaskTemplate(template);

        TaskAlert alert = new TaskAlert();
        alert.setId(10L);
        alert.setTimeoutValue(30);
        alert.setTimeoutUnit(TaskSlaUnit.MINUTES);
        alert.setTargetStep(new TaskStepDefinition());
        alert.getTargetStep().setId(99L);
        
        template.setAlerts(new LinkedHashSet<>(Set.of(alert)));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        doReturn(mock(ScheduledFuture.class)).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));

        alertService.scheduleAlertsForTask(task);

        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("scheduleAlertsForTask - deve executar imediatamente se o tempo já passou")
    void shouldExecuteImmediatelyIfTimePassed() {
        Task task = new Task();
        task.setId(1L);
        task.setCreatedAt(Instant.now().minusSeconds(3600));

        TaskTemplate template = new TaskTemplate();
        template.setId(1L);
        task.setTaskTemplate(template);

        TaskAlert alert = new TaskAlert();
        alert.setId(10L);
        alert.setTimeoutValue(10);
        alert.setTimeoutUnit(TaskSlaUnit.MINUTES);
        alert.setTargetStep(new TaskStepDefinition());
        alert.getTargetStep().setId(99L);
        
        template.setAlerts(new LinkedHashSet<>(Set.of(alert)));

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        Group group = new Group();
        group.setId(1L);
        task.setGroup(group);
        when(taskRepository.save(any())).thenReturn(task);

        alertService.scheduleAlertsForTask(task);

        verify(chatService).sendSystemAlertForTask(any(), eq(task), any(), any());
    }

    @Test
    @DisplayName("cancelAllAlertsForTask - deve cancelar alertas agendados")
    void shouldCancelAllAlerts() {
        Task task = new Task();
        task.setId(1L);
        
        TaskTemplate template = new TaskTemplate();
        template.setId(1L);
        task.setTaskTemplate(template);
        TaskAlert alert = new TaskAlert();
        alert.setId(10L);
        alert.setTimeoutValue(60);
        alert.setTimeoutUnit(TaskSlaUnit.MINUTES);
        alert.setTargetStep(new TaskStepDefinition());
        alert.getTargetStep().setId(99L);
        template.setAlerts(new LinkedHashSet<>(Set.of(alert)));

        ScheduledFuture future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        alertService.scheduleAlertsForTask(task);
        alertService.cancelAllAlertsForTask(task);

        verify(future).cancel(false);
    }
}