package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.TaskEvent;
import com.xflprflx.neokioski.models.TaskFieldValue;
import com.xflprflx.neokioski.models.TaskSlaResult;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import com.xflprflx.neokioski.repositories.ChatMessageRepository;
import com.xflprflx.neokioski.repositories.TaskFieldDefinitionRepository;
import com.xflprflx.neokioski.repositories.TaskRepository;
import com.xflprflx.neokioski.repositories.TaskStepDefinitionRepository;
import com.xflprflx.neokioski.repositories.TaskTemplateRepository;
import com.xflprflx.neokioski.repositories.TemplateStepRelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TemplateStepRelationRepository stepRelationRepository;
    @Mock
    private ChatService chatService;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private TaskAlertService taskAlertService;
    @Mock
    private TaskTemplateRepository taskTemplateRepository;
    @Mock
    private TaskFieldDefinitionRepository taskFieldDefinitionRepository;
    @Mock
    private TaskStepDefinitionRepository taskStepDefinitionRepository;
    @Mock
    private TaskSlaService taskSlaService;

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
    void initializeTaskData_ShouldTouchAllCriticalCollections() {
        // Arrange
        Task task = mock(Task.class);
        TaskTemplate template = mock(TaskTemplate.class);
        
        // Mocks para as coleções
        Set<TaskSlaResult> slaResults = mock(Set.class);
        Set<TaskEvent> events = mock(Set.class);
        Set<TaskFieldValue> fieldValues = mock(Set.class);
        Set<TaskStepDefinition> stepDefinitions = mock(Set.class);
        Set<TemplateStepRelation> relations = mock(Set.class);

        when(template.getId()).thenReturn(1L);
        when(task.getTaskTemplate()).thenReturn(template);
        when(taskTemplateRepository.findByIdWithMetadata(1L)).thenReturn(java.util.Optional.of(template));
        
        when(task.getSlaResults()).thenReturn(slaResults);
        when(task.getTaskEvents()).thenReturn(events);
        when(task.getTaskFieldValues()).thenReturn(fieldValues);
        when(task.getTaskStepDefinitions()).thenReturn(stepDefinitions);
        when(template.getTemplateStepRelation()).thenReturn(relations);

        // Act
        taskService.initializeTaskData(task);

        // Assert
        verify(taskTemplateRepository).findByIdWithMetadata(1L);
        verify(task, atLeastOnce()).getSlaResults();
        verify(task, atLeastOnce()).getTaskEvents();
        verify(task, atLeastOnce()).getTaskFieldValues();
        verify(task, atLeastOnce()).getTaskStepDefinitions();
        verify(template, atLeastOnce()).getTemplateStepRelation();
    }
}
