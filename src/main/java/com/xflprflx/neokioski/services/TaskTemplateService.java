package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.repositories.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
/**
 * Serviço que orquestra a criação e administração dos templates estruturais utilizados nas tarefas.
 */
public class TaskTemplateService {

    private final TaskTemplateRepository taskTemplateRepository;
    private final TaskFieldDefinitionRepository taskFieldDefinitionRepository;
    private final TaskStepDefinitionRepository taskStepDefinitionRepository;
    private final TemplateFieldRelationRepository templateFieldRelationRepository;
    private final TemplateStepRelationRepository templateStepRelationRepository;
    private final TaskFieldOptionRepository taskFieldOptionRepository;
    private final TaskAlertRepository taskAlertRepository;
    private final TaskSlaConfigRepository taskSlaConfigRepository;
    private final TaskTemplateMentionRepository taskTemplateMentionRepository;
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private TaskRepository taskRepository;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private TaskAlertService taskAlertService;

    public TaskTemplateService(TaskTemplateRepository taskTemplateRepository,
                               TaskFieldDefinitionRepository taskFieldDefinitionRepository,
                               TaskStepDefinitionRepository taskStepDefinitionRepository,
                               TemplateFieldRelationRepository templateFieldRelationRepository,
                               TemplateStepRelationRepository templateStepRelationRepository,
                               TaskFieldOptionRepository taskFieldOptionRepository,
                               TaskAlertRepository taskAlertRepository,
                               TaskSlaConfigRepository taskSlaConfigRepository,
                               TaskTemplateMentionRepository taskTemplateMentionRepository) {
        this.taskTemplateRepository = taskTemplateRepository;
        this.taskFieldDefinitionRepository = taskFieldDefinitionRepository;
        this.taskStepDefinitionRepository = taskStepDefinitionRepository;
        this.templateFieldRelationRepository = templateFieldRelationRepository;
        this.templateStepRelationRepository = templateStepRelationRepository;
        this.taskFieldOptionRepository = taskFieldOptionRepository;
        this.taskAlertRepository = taskAlertRepository;
        this.taskSlaConfigRepository = taskSlaConfigRepository;
        this.taskTemplateMentionRepository = taskTemplateMentionRepository;
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public TaskTemplate saveTemplate(TaskTemplate template, 
                                     List<TemplateFieldRelation> fieldRelations, 
                                     List<TemplateStepRelation> stepRelations,
                                     List<TaskAlert> alerts,
                                     List<TaskSlaConfig> slas,
                                     List<TaskTemplateMention> mentionsMappings) {
        
        TaskTemplate savedTemplate = taskTemplateRepository.save(template);

        // Reconciliação de coleções para evitar o "delete-all-and-reinsert"
        reconcileFields(savedTemplate, fieldRelations);
        reconcileSteps(savedTemplate, stepRelations);
        reconcileAlerts(savedTemplate, alerts);
        manageSlas(savedTemplate, slas);
        reconcileMentions(savedTemplate, mentionsMappings);

        // Reagendar alertas das tarefas ativas após a alteração das regras do template
        try {
            java.util.List<com.xflprflx.neokioski.models.Task> pendings = taskRepository.findAllPending();
            for (com.xflprflx.neokioski.models.Task pt : pendings) {
                if (pt.getTaskTemplate() != null && pt.getTaskTemplate().getId().equals(savedTemplate.getId())) {
                    taskAlertService.scheduleAlertsForTask(pt);
                }
            }
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao tentar reagendar alertas das tarefas ativas: " + e.getMessage());
        }

        return savedTemplate;
    }

    private void reconcileFields(TaskTemplate template, List<TemplateFieldRelation> incoming) {
        Map<Long, TemplateFieldRelation> existingMap = template.getTemplateFieldRelation().stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(TemplateFieldRelation::getId, r -> r));

        Set<Long> incomingIds = incoming.stream()
                .map(TemplateFieldRelation::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 1. Remover quem sumiu (Orphan Removal)
        template.getTemplateFieldRelation().removeIf(r -> r.getId() != null && !incomingIds.contains(r.getId()));

        // 2. Atualizar ou Adicionar
        for (TemplateFieldRelation fr : incoming) {
            TaskFieldDefinition fieldDef = fr.getTaskFieldDefinition();
            if (fieldDef.getTaskFieldOptions() != null) {
                taskFieldOptionRepository.saveAll(fieldDef.getTaskFieldOptions());
            }
            taskFieldDefinitionRepository.save(fieldDef);

            if (fr.getId() != null && existingMap.containsKey(fr.getId())) {
                TemplateFieldRelation existing = existingMap.get(fr.getId());
                existing.setOrder(fr.getOrder());
                existing.setRequired(fr.isRequired());
                existing.setTaskFieldDefinition(fieldDef);
            } else {
                fr.setTaskTemplate(template);
                template.getTemplateFieldRelation().add(fr);
            }
        }
    }

    private void reconcileSteps(TaskTemplate template, List<TemplateStepRelation> incoming) {
        Map<Long, TemplateStepRelation> existingMap = template.getTemplateStepRelation().stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(TemplateStepRelation::getId, r -> r));

        Set<Long> incomingIds = incoming.stream()
                .map(TemplateStepRelation::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 1. Remover quem sumiu (Orphan Removal)
        template.getTemplateStepRelation().removeIf(r -> r.getId() != null && !incomingIds.contains(r.getId()));

        for (TemplateStepRelation sr : incoming) {
            taskStepDefinitionRepository.save(sr.getTaskStepDefinition());

            if (sr.getId() != null && existingMap.containsKey(sr.getId())) {
                TemplateStepRelation existing = existingMap.get(sr.getId());
                // Atualizar propriedades simples
                existing.setOrder(sr.getOrder());
                existing.setFinalizer(sr.isFinalizer());
                existing.setSendNotification(sr.isSendNotification());
                existing.setNotificationMessage(sr.getNotificationMessage());
                existing.setShouldPulse(sr.isShouldPulse());
                existing.setGlobal(sr.isGlobal());
                existing.setHasResponseField(sr.isHasResponseField());
                existing.setResponseRequired(sr.isResponseRequired());
                existing.setResponseFieldLabel(sr.getResponseFieldLabel());
                existing.setResponseIsCompact(sr.isResponseIsCompact());
                existing.setResponseInputType(sr.getResponseInputType());
                existing.setAlertSound(sr.getAlertSound());
                existing.setResponsibleRole(sr.getResponsibleRole());

                // Reconciliar sub-coleções (Set<Role>, Set<TaskStepDefinition>, Set<TaskFieldOption>)
                existing.getAlertRoles().clear();
                existing.getAlertRoles().addAll(sr.getAlertRoles());

                existing.getAllowedNextSteps().clear();
                existing.getAllowedNextSteps().addAll(sr.getAllowedNextSteps());

                existing.getResponseOptions().clear();
                existing.getResponseOptions().addAll(sr.getResponseOptions());
            } else {
                sr.setTaskTemplate(template);
                template.getTemplateStepRelation().add(sr);
            }
        }
    }

    private void reconcileAlerts(TaskTemplate template, List<TaskAlert> incoming) {
        Map<Long, TaskAlert> existingMap = template.getAlerts().stream()
                .filter(a -> a.getId() != null)
                .collect(Collectors.toMap(TaskAlert::getId, a -> a));

        Set<Long> incomingIds = incoming.stream()
                .map(TaskAlert::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 1. Remover quem sumiu (Orphan Removal)
        template.getAlerts().removeIf(a -> a.getId() != null && !incomingIds.contains(a.getId()));

        for (TaskAlert alert : incoming) {
            if (alert.getId() != null && existingMap.containsKey(alert.getId())) {
                TaskAlert existing = existingMap.get(alert.getId());
                existing.setAlertMessage(alert.getAlertMessage());
                existing.setAlertSound(alert.getAlertSound());
                existing.setTimeoutMinutes(alert.getTimeoutMinutes());
                existing.setStartStep(alert.getStartStep());
                existing.setTargetStep(alert.getTargetStep());
                existing.getAlertRoles().clear();
                existing.getAlertRoles().addAll(alert.getAlertRoles());
            } else {
                alert.setTaskTemplate(template);
                template.getAlerts().add(alert);
            }
        }
    }

    private void reconcileMentions(TaskTemplate template, List<TaskTemplateMention> incoming) {
        Map<Long, TaskTemplateMention> existingMap = template.getMentionsMappings().stream()
                .filter(m -> m.getId() != null)
                .collect(Collectors.toMap(TaskTemplateMention::getId, m -> m));

        Set<Long> incomingIds = incoming.stream()
                .map(TaskTemplateMention::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 1. Remover quem sumiu (Orphan Removal)
        template.getMentionsMappings().removeIf(m -> m.getId() != null && !incomingIds.contains(m.getId()));

        for (TaskTemplateMention mention : incoming) {
            if (mention.getId() != null && existingMap.containsKey(mention.getId())) {
                TaskTemplateMention existing = existingMap.get(mention.getId());
                existing.setPlaceholder(mention.getPlaceholder());
                existing.setGroup(mention.getGroup());
                existing.getUsers().clear();
                existing.getUsers().addAll(mention.getUsers());
            } else {
                mention.setTaskTemplate(template);
                template.getMentionsMappings().add(mention);
            }
        }
    }

    private void manageSlas(TaskTemplate template, List<TaskSlaConfig> incoming) {
        java.time.Instant now = java.time.Instant.now();
        List<TaskSlaConfig> activeSlas = taskSlaConfigRepository.findByTaskTemplateAndValidToIsNull(template);
        for (TaskSlaConfig activeSla : activeSlas) {
            activeSla.setValidTo(now);
        }
        if (incoming != null) {
            for (TaskSlaConfig sla : incoming) {
                sla.setId(null);
                sla.setTaskTemplate(template);
                if (sla.getValidFrom() == null) sla.setValidFrom(now);
                sla.setValidTo(null);
                taskSlaConfigRepository.save(sla);
            }
        }
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TaskTemplate> findAll() {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return taskTemplateRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder", "name"));
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public TaskTemplate getTemplateById(Long id) {
        /*
         * Retorna o resultado final processado pelo métodx com metadados completos para o editor.
         */
        return taskTemplateRepository.findByIdWithMetadata(id).orElse(null);
    }


    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TaskTemplate> getTemplatesByGroupId(Long groupId) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return taskTemplateRepository.findByGroupId(groupId);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TemplateFieldRelation> getFieldRelationsByTemplateId(Long templateId) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return templateFieldRelationRepository.findByTaskTemplateIdOrderByOrderAsc(templateId);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TemplateStepRelation> getStepRelationsByTemplateId(Long templateId) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return templateStepRelationRepository.findByTaskTemplateIdOrderByOrderAsc(templateId);
    }

    @Transactional
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void deleteTemplate(Long id) {
        templateFieldRelationRepository.deleteByTaskTemplateId(id);
        templateStepRelationRepository.deleteByTaskTemplateId(id);
        List<TaskAlert> alerts = taskAlertRepository.findByTaskTemplateId(id);
        taskAlertRepository.deleteAll(alerts);
        taskTemplateRepository.deleteById(id);
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TaskSlaConfig> getSlaConfigsByTemplateId(Long templateId) {
        TaskTemplate template = taskTemplateRepository.getReferenceById(templateId);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return taskSlaConfigRepository.findByTaskTemplateAndValidToIsNull(template);
    }
}
