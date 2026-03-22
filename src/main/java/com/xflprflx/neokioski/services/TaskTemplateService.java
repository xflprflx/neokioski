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

    public TaskTemplateService(TaskTemplateRepository taskTemplateRepository,
                               TaskFieldDefinitionRepository taskFieldDefinitionRepository,
                               TaskStepDefinitionRepository taskStepDefinitionRepository,
                               TemplateFieldRelationRepository templateFieldRelationRepository,
                               TemplateStepRelationRepository templateStepRelationRepository,
                               TaskFieldOptionRepository taskFieldOptionRepository) {
        this.taskTemplateRepository = taskTemplateRepository;
        this.taskFieldDefinitionRepository = taskFieldDefinitionRepository;
        this.taskStepDefinitionRepository = taskStepDefinitionRepository;
        this.templateFieldRelationRepository = templateFieldRelationRepository;
        this.templateStepRelationRepository = templateStepRelationRepository;
        this.taskFieldOptionRepository = taskFieldOptionRepository;
    }

    @Transactional
    public TaskTemplate saveTemplate(TaskTemplate template, 
                                     List<TemplateFieldRelation> fieldRelations, 
                                     List<TemplateStepRelation> stepRelations) {
        
        TaskTemplate savedTemplate = taskTemplateRepository.save(template);

        for (TemplateFieldRelation fr : fieldRelations) {
            TaskFieldDefinition fieldDef = fr.getTaskFieldDefinition();
            if (fieldDef.getTaskFieldOptions() != null) {
                taskFieldOptionRepository.saveAll(fieldDef.getTaskFieldOptions());
            }
            taskFieldDefinitionRepository.save(fieldDef);
            fr.setTaskTemplate(savedTemplate);
            templateFieldRelationRepository.save(fr);
        }

        for (TemplateStepRelation sr : stepRelations) {
            taskStepDefinitionRepository.save(sr.getTaskStepDefinition());
            sr.setTaskTemplate(savedTemplate);
            templateStepRelationRepository.save(sr);
        }

        return savedTemplate;
    }

    public List<TaskTemplate> findAll() {
        return taskTemplateRepository.findAll();
    }
}
