package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.enums.TaskSlaUnit;
import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.repositories.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskTemplateService Unit Tests")
class TaskTemplateServiceTest {

    @Mock private TaskTemplateRepository taskTemplateRepository;
    @Mock private TaskFieldDefinitionRepository taskFieldDefinitionRepository;
    @Mock private TaskStepDefinitionRepository taskStepDefinitionRepository;
    @Mock private TemplateFieldRelationRepository templateFieldRelationRepository;
    @Mock private TemplateStepRelationRepository templateStepRelationRepository;
    @Mock private TaskFieldOptionRepository taskFieldOptionRepository;
    @Mock private TaskAlertRepository taskAlertRepository;
    @Mock private TaskSlaConfigRepository taskSlaConfigRepository;
    @Mock private TaskTemplateMentionRepository taskTemplateMentionRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private TaskAlertService taskAlertService;

    @InjectMocks
    private TaskTemplateService taskTemplateService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        java.lang.reflect.Field repoField = TaskTemplateService.class.getDeclaredField("taskRepository");
        repoField.setAccessible(true);
        repoField.set(taskTemplateService, taskRepository);

        java.lang.reflect.Field alertField = TaskTemplateService.class.getDeclaredField("taskAlertService");
        alertField.setAccessible(true);
        alertField.set(taskTemplateService, taskAlertService);
    }

    @Test
    @DisplayName("Deve salvar um novo template com sucesso")
    void shouldSaveNewTemplateSuccessfully() {
        // Given
        TaskTemplate template = new TaskTemplate();
        template.setName("Novo Template");
        
        when(taskTemplateRepository.save(any())).thenReturn(template);
        when(taskRepository.findAllPending()).thenReturn(Collections.emptyList());

        // When
        TaskTemplate saved = taskTemplateService.saveTemplate(template, 
                Collections.emptyList(), Collections.emptyList(), 
                Collections.emptyList(), Collections.emptyList(), 
                Collections.emptyList());

        // Then
        assertNotNull(saved);
        verify(taskTemplateRepository).save(template);
    }

    @Test
    @DisplayName("Deve atualizar um template existente e reconciliar coleções")
    void shouldUpdateExistingTemplateAndReconcile() {
        // Given
        Long templateId = 1L;
        TaskTemplate existing = new TaskTemplate();
        existing.setId(templateId);
        existing.setName("Antigo");

        TaskTemplate incoming = new TaskTemplate();
        incoming.setId(templateId);
        incoming.setName("Novo");

        TaskFieldDefinition fieldDef = new TaskFieldDefinition();
        fieldDef.setLabel("Campo Novo");
        TemplateFieldRelation fieldRel = new TemplateFieldRelation();
        fieldRel.setTaskFieldDefinition(fieldDef);

        TaskStepDefinition stepDef = new TaskStepDefinition();
        stepDef.setLabel("Passo Novo");
        TemplateStepRelation stepRel = new TemplateStepRelation();
        stepRel.setTaskStepDefinition(stepDef);

        when(taskTemplateRepository.findById(templateId)).thenReturn(Optional.of(existing));
        when(taskTemplateRepository.save(any())).thenReturn(existing);

        // When
        taskTemplateService.saveTemplate(incoming, 
                List.of(fieldRel), List.of(stepRel), 
                Collections.emptyList(), Collections.emptyList(), 
                Collections.emptyList());

        // Then
        assertEquals("Novo", existing.getName());
        verify(taskFieldDefinitionRepository).save(fieldDef);
        verify(taskStepDefinitionRepository).save(stepDef);
    }

    @Test
    @DisplayName("Deve deletar um template logicamente")
    void shouldDeleteTemplateLogically() {
        // Given
        Long id = 1L;
        TaskTemplate template = new TaskTemplate();
        template.setId(id);
        template.setName("Template Teste");

        when(taskTemplateRepository.findById(id)).thenReturn(Optional.of(template));

        // When
        taskTemplateService.deleteTemplate(id);

        // Then
        assertTrue(template.isDeleted());
        assertTrue(template.getName().contains("[EXCLUÍDO]"));
        verify(taskTemplateRepository).save(template);
    }

    @Test
    @DisplayName("Deve restaurar um template deletado")
    void shouldRestoreTemplate() {
        // Given
        Long id = 1L;
        TaskTemplate template = new TaskTemplate();
        template.setId(id);
        template.setName("[EXCLUÍDO] Template (1)");
        template.setDeleted(true);

        when(taskTemplateRepository.findById(id)).thenReturn(Optional.of(template));
        when(taskTemplateRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        taskTemplateService.restoreTemplate(id);

        // Then
        assertFalse(template.isDeleted());
        assertEquals("Template", template.getName());
        verify(taskTemplateRepository).save(template);
    }

    @Test
    @DisplayName("Deve buscar todos os templates ativos")
    void shouldFindAllActiveTemplates() {
        // When
        taskTemplateService.findAll(false);

        // Then
        verify(taskTemplateRepository).findAllByDeletedFalse(any(Sort.class));
    }

    @Test
    @DisplayName("Deve buscar template por ID com metadados")
    void shouldGetTemplateByIdWithMetadata() {
        // Given
        Long id = 1L;
        when(taskTemplateRepository.findByIdWithMetadata(id)).thenReturn(Optional.of(new TaskTemplate()));

        // When
        taskTemplateService.getTemplateById(id);

        // Then
        verify(taskTemplateRepository).findByIdWithMetadata(id);
    }

    @Test
    @DisplayName("Deve reconciliar SLAs invalidando os antigos")
    void shouldReconcileSlasAndInvalidateOldOnes() {
        // Given
        TaskTemplate template = new TaskTemplate();
        template.setId(1L);
        
        TaskSlaConfig active = new TaskSlaConfig();
        active.setId(10L);
        active.setName("SLA Antigo");
        
        TaskSlaConfig incoming = new TaskSlaConfig();
        incoming.setId(10L);
        incoming.setName("SLA Alterado");
        incoming.setUnit(TaskSlaUnit.MINUTES);
        incoming.setTargetValue(30);

        when(taskSlaConfigRepository.findByTaskTemplateAndValidToIsNull(template)).thenReturn(List.of(active));
        when(taskTemplateRepository.save(any())).thenReturn(template);

        // When
        taskTemplateService.saveTemplate(template, 
                Collections.emptyList(), Collections.emptyList(), 
                Collections.emptyList(), List.of(incoming), 
                Collections.emptyList());

        // Then
        assertNotNull(active.getValidTo());
        verify(taskSlaConfigRepository, times(2)).save(any(TaskSlaConfig.class));
    }
}
