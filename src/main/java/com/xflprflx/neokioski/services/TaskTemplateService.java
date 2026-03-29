package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskTemplateService {

    private final TaskTemplateRepository taskTemplateRepository;
    private final TaskFieldDefinitionRepository taskFieldDefinitionRepository;
    private final TaskStepDefinitionRepository taskStepDefinitionRepository;
    private final TemplateFieldRelationRepository templateFieldRelationRepository;
    private final TemplateStepRelationRepository templateStepRelationRepository;
    private final TaskFieldOptionRepository taskFieldOptionRepository;
    private final TaskAlertRepository taskAlertRepository;

    public TaskTemplateService(TaskTemplateRepository taskTemplateRepository,
                               TaskFieldDefinitionRepository taskFieldDefinitionRepository,
                               TaskStepDefinitionRepository taskStepDefinitionRepository,
                               TemplateFieldRelationRepository templateFieldRelationRepository,
                               TemplateStepRelationRepository templateStepRelationRepository,
                               TaskFieldOptionRepository taskFieldOptionRepository,
                               TaskAlertRepository taskAlertRepository) {
        this.taskTemplateRepository = taskTemplateRepository;
        this.taskFieldDefinitionRepository = taskFieldDefinitionRepository;
        this.taskStepDefinitionRepository = taskStepDefinitionRepository;
        this.templateFieldRelationRepository = templateFieldRelationRepository;
        this.templateStepRelationRepository = templateStepRelationRepository;
        this.taskFieldOptionRepository = taskFieldOptionRepository;
        this.taskAlertRepository = taskAlertRepository;
    }

    @Transactional
    public TaskTemplate saveTemplate(TaskTemplate template, 
                                     List<TemplateFieldRelation> fieldRelations, 
                                     List<TemplateStepRelation> stepRelations,
                                     List<TaskAlert> alerts) {
        
        // Limpa apenas os alertas antigos da coleção que foram excluídos na interface (para orphanRemoval funcionar adequadamente),
        // preservando os que ficaram para não quebrar a identidade e trocar de ID
        if (template.getAlerts() != null && alerts != null) {
            java.util.Set<Long> incomingIds = alerts.stream()
                .map(TaskAlert::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
            template.getAlerts().removeIf(a -> a.getId() != null && !incomingIds.contains(a.getId()));
        }

        TaskTemplate savedTemplate = taskTemplateRepository.save(template);

        if (savedTemplate.getId() != null) {
            templateFieldRelationRepository.deleteByTaskTemplateId(savedTemplate.getId());
            templateStepRelationRepository.deleteByTaskTemplateId(savedTemplate.getId());
        }

        for (TemplateFieldRelation fr : fieldRelations) {
            TaskFieldDefinition fieldDef = fr.getTaskFieldDefinition();
            if (fieldDef.getTaskFieldOptions() != null) {
                taskFieldOptionRepository.saveAll(fieldDef.getTaskFieldOptions());
            }
            taskFieldDefinitionRepository.save(fieldDef);
            fr.setTaskTemplate(savedTemplate);
            templateFieldRelationRepository.save(fr);
        }

        // Os definitions precisam ser salvos antes de vincularmos aos alertas 
        // para evitar o erro de 'TransientPropertyValueException'
        for (TemplateStepRelation sr : stepRelations) {
            taskStepDefinitionRepository.save(sr.getTaskStepDefinition());
        }

        for (TemplateStepRelation sr : stepRelations) {
            sr.setTaskTemplate(savedTemplate);
            templateStepRelationRepository.save(sr);
        }

        // Agora que os passos (targetStep) já estão com IDs, podemos persistir os alertas novos.
        if (alerts != null) {
            for (TaskAlert alert : alerts) {
                alert.setTaskTemplate(savedTemplate);
                taskAlertRepository.save(alert);
            }
        }

        return savedTemplate;
    }

    public List<TaskTemplate> findAll() {
        return taskTemplateRepository.findAll();
    }

    public TaskTemplate getTemplateById(Long id) {
        return taskTemplateRepository.findByIdWithGroups(id).orElse(null);
    }

    public List<TaskTemplate> getTemplatesByGroupId(Long groupId) {
        return taskTemplateRepository.findByGroupId(groupId);
    }

    public List<TemplateFieldRelation> getFieldRelationsByTemplateId(Long templateId) {
        return templateFieldRelationRepository.findByTaskTemplateIdOrderByOrderAsc(templateId);
    }

    public List<TemplateStepRelation> getStepRelationsByTemplateId(Long templateId) {
        return templateStepRelationRepository.findByTaskTemplateIdOrderByOrderAsc(templateId);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        templateFieldRelationRepository.deleteByTaskTemplateId(id);
        templateStepRelationRepository.deleteByTaskTemplateId(id);
        List<TaskAlert> alerts = taskAlertRepository.findByTaskTemplateId(id);
        taskAlertRepository.deleteAll(alerts);
        taskTemplateRepository.deleteById(id);
    }
}
