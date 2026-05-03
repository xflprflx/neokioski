package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.template.TaskFieldDefinition;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import com.xflprflx.neokioski.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes Unitários para TaskService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do Serviço de Tarefas (TaskService)")
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private TemplateStepRelationRepository stepRelationRepository;
    @Mock private ChatService chatService;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private TaskAlertService taskAlertService;
    @Mock private TaskTemplateRepository taskTemplateRepository;
    @Mock private TaskFieldDefinitionRepository taskFieldDefinitionRepository;
    @Mock private TaskStepDefinitionRepository taskStepDefinitionRepository;
    @Mock private TaskSlaService taskSlaService;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(
                taskRepository,
                stepRelationRepository,
                chatService,
                chatMessageRepository,
                taskAlertService,
                taskTemplateRepository,
                taskFieldDefinitionRepository,
                taskStepDefinitionRepository,
                taskSlaService
        );
    }

    @Test
    @DisplayName("Deve tocar em todas as coleções críticas para inicialização profunda (Deep Touch)")
    void initializeTaskData_ShouldTouchAllCriticalCollections() {
        Task task = mock(Task.class);
        TaskTemplate template = mock(TaskTemplate.class);

        when(task.getTaskTemplate()).thenReturn(template);
        when(task.getSlaResults()).thenReturn(new LinkedHashSet<>());
        when(task.getTaskEvents()).thenReturn(new LinkedHashSet<>());
        when(task.getTaskFieldValues()).thenReturn(new LinkedHashSet<>());
        when(task.getTaskStepDefinitions()).thenReturn(new LinkedHashSet<>());
        when(template.getTemplateStepRelation()).thenReturn(new LinkedHashSet<>());
        when(template.getAuthorizingUsers()).thenReturn(new LinkedHashSet<>());
        when(template.getAlertRoles()).thenReturn(new HashSet<>());

        taskService.initializeTaskData(task);

        verify(task, atLeastOnce()).getSlaResults();
        verify(task, atLeastOnce()).getTaskEvents();
    }

    @Test
    @DisplayName("isTaskFinalized - diversos cenários")
    void isTaskFinalized_scenarios() {
        Task task = new Task();
        task.setFinalizedAt(Instant.now());
        assertTrue(taskService.isTaskFinalized(task));

        task.setFinalizedAt(null);
        assertFalse(taskService.isTaskFinalized(task));
    }

    @Test
    @DisplayName("createTask - deve criar tarefa com sucesso")
    void createTask_successfully() {
        TaskTemplate template = new TaskTemplate();
        template.setId(1L);
        User creator = new User();
        Group group = new Group();
        group.setId(1L);

        when(taskTemplateRepository.findByIdWithMetadata(1L)).thenReturn(Optional.of(template));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        Task task = taskService.createTask(template, Collections.emptyMap(), creator, group);

        assertNotNull(task);
        verify(taskRepository).save(any());
    }

    @Test
    @DisplayName("executeStep - deve executar passo e disparar notificações")
    void executeStep_successfully() {
        Long taskId = 1L;
        Task task = buildTask(null, null);
        task.setGroup(new Group());
        task.getGroup().setId(1L);
        TaskTemplate template = task.getTaskTemplate();
        template.setId(1L);
        
        TaskStepDefinition step = buildStep(2L);
        User user = buildUser(Role.ADMIN);
        user.setUsername("testuser");

        TemplateStepRelation rel = buildRelation(step, false);
        rel.setSendNotification(true);
        rel.setNotificationMessage("Tarefa @usuario em @resposta");

        when(taskRepository.findByIdForExecution(taskId)).thenReturn(Optional.of(task));
        when(stepRelationRepository.findByTaskTemplateIdAndTaskStepDefinitionId(1L, 2L)).thenReturn(Optional.of(rel));
        when(taskRepository.save(any())).thenReturn(task);

        taskService.executeStep(taskId, step, user, "OK");

        assertEquals(step, task.getCurrentStep());
        verify(chatService).sendVolatileMessage(contains("testuser"), eq(user), any(), any(), any(), any());
    }

    @Test
    @DisplayName("findSimilarTask - deve encontrar tarefa similar")
    void findSimilarTask_found() {
        TaskTemplate template = new TaskTemplate();
        Group group = new Group();
        TaskFieldDefinition fd = new TaskFieldDefinition();
        fd.setId(1L);
        
        TaskFieldValue fv = new TaskFieldValue();
        fv.setTaskFieldDefinition(fd);
        fv.setValue("Valor1");

        Task existing = new Task();
        existing.getTaskFieldValues().add(fv);

        when(taskRepository.findByGroupAndTemplate(group, template)).thenReturn(List.of(existing));

        Optional<Task> result = taskService.findSimilarTask(template, group, Map.of(fd, "Valor1"));

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("getAllowedNextSteps - retorna globais e entry points quando pendente")
    void getAllowedNextSteps_pendingTask() {
        TaskTemplate template = new TaskTemplate();
        TaskStepDefinition step1 = buildStep(1L);
        TemplateStepRelation rel1 = buildRelation(step1, false);
        
        TaskStepDefinition globalStep = buildStep(2L);
        TemplateStepRelation relGlobal = buildRelation(globalStep, false);
        relGlobal.setGlobal(true);

        template.getTemplateStepRelation().addAll(Set.of(rel1, relGlobal));
        Task task = buildTaskWithTemplate(template);
        task.setCurrentStep(null);

        List<TaskStepDefinition> allowed = taskService.getAllowedNextSteps(task);

        assertTrue(allowed.contains(step1));
        assertTrue(allowed.contains(globalStep));
    }

    @Test
    @DisplayName("broadcast - deve notificar ouvintes")
    void broadcast_notifiesListeners() {
        Task task = new Task();
        List<Task> received = new ArrayList<>();
        taskService.register(1L, received::add);
        taskService.broadcast(1L, task);
        assertEquals(1, received.size());
    }

    // =====================================================================
    // Auxiliares de fábrica
    // =====================================================================

    private Task buildTask(Instant finalizedAt, TaskStepDefinition currentStep) {
        Task task = new Task();
        task.setId(1L);
        task.setFinalizedAt(finalizedAt);
        task.setCurrentStep(currentStep);
        task.setTaskEvents(new LinkedHashSet<>());
        TaskTemplate template = new TaskTemplate();
        task.setTaskTemplate(template);
        return task;
    }

    private Task buildTaskWithTemplate(TaskTemplate template) {
        Task task = new Task();
        task.setId(1L);
        task.setTaskTemplate(template);
        return task;
    }

    private TaskStepDefinition buildStep(Long id) {
        TaskStepDefinition step = new TaskStepDefinition();
        step.setId(id);
        step.setLabel("Step " + id);
        return step;
    }

    private TemplateStepRelation buildRelation(TaskStepDefinition step, boolean isFinalizer) {
        TemplateStepRelation rel = new TemplateStepRelation();
        rel.setTaskStepDefinition(step);
        rel.setFinalizer(isFinalizer);
        return rel;
    }

    private User buildUser(Role role) {
        User user = new User();
        user.setRole(role);
        return user;
    }
}