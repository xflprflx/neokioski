package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.repositories.*;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                               TaskSlaConfigRepository taskSlaConfigRepository) {
        this.taskTemplateRepository = taskTemplateRepository;
        this.taskFieldDefinitionRepository = taskFieldDefinitionRepository;
        this.taskStepDefinitionRepository = taskStepDefinitionRepository;
        this.templateFieldRelationRepository = templateFieldRelationRepository;
        this.templateStepRelationRepository = templateStepRelationRepository;
        this.taskFieldOptionRepository = taskFieldOptionRepository;
        this.taskAlertRepository = taskAlertRepository;
        this.taskSlaConfigRepository = taskSlaConfigRepository;
    }

    @Transactional
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public TaskTemplate saveTemplate(TaskTemplate template, 
                                     List<TemplateFieldRelation> fieldRelations, 
                                     List<TemplateStepRelation> stepRelations,
                                     List<TaskAlert> alerts,
                                     List<TaskSlaConfig> slas) {
        
        // Limpa apenas os alertas antigos da coleção que foram excluídos na interface (para orphanRemoval funcionar adequadamente),
        // preservando os que ficaram para não quebrar a identidade e trocar de ID
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (template.getAlerts() != null && alerts != null) {
            java.util.Set<Long> incomingIds = alerts.stream()
                .map(TaskAlert::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
            template.getAlerts().removeIf(a -> a.getId() != null && !incomingIds.contains(a.getId()));
        }

        TaskTemplate savedTemplate = taskTemplateRepository.save(template);

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (savedTemplate.getId() != null) {
            templateFieldRelationRepository.deleteByTaskTemplateId(savedTemplate.getId());
            templateStepRelationRepository.deleteByTaskTemplateId(savedTemplate.getId());
        }

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (TemplateFieldRelation fr : fieldRelations) {
            TaskFieldDefinition fieldDef = fr.getTaskFieldDefinition();
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (fieldDef.getTaskFieldOptions() != null) {
                taskFieldOptionRepository.saveAll(fieldDef.getTaskFieldOptions());
            }
            taskFieldDefinitionRepository.save(fieldDef);
            fr.setTaskTemplate(savedTemplate);
            templateFieldRelationRepository.save(fr);
        }

        // Os definitions precisam ser salvos antes de vincularmos aos alertas 
        // para evitar o erro de 'TransientPropertyValueException'
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (TemplateStepRelation sr : stepRelations) {
            taskStepDefinitionRepository.save(sr.getTaskStepDefinition());
        }

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (TemplateStepRelation sr : stepRelations) {
            sr.setTaskTemplate(savedTemplate);
            templateStepRelationRepository.save(sr);
        }

        // Agora que os passos (targetStep) já estão com IDs, podemos persistir os alertas novos.
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (alerts != null) {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (TaskAlert alert : alerts) {
                alert.setTaskTemplate(savedTemplate);
                taskAlertRepository.save(alert);
            }
        }

        // Persistir SLAs através de Vigência (Imutabilidade para hitórico)
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (savedTemplate.getId() != null) {
            // Buscamos as configurações que estão ATIVAS atualmente
            List<TaskSlaConfig> activeSlas = taskSlaConfigRepository.findByTaskTemplateAndValidToIsNull(savedTemplate);
            
            // "Encerramos" a vigência das configurações atuais.
            // Quando novas configurações forem salvas abaixo, elas serão as novas versões vigentes.
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (TaskSlaConfig activeSla : activeSlas) {
                activeSla.setValidTo(java.time.Instant.now());
                taskSlaConfigRepository.save(activeSla);
            }
        }

        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (slas != null) {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (TaskSlaConfig sla : slas) {
                // Garantimos que cada entrada seja salva como um NOVO registro histórico
                sla.setId(null); 
                sla.setTaskTemplate(savedTemplate);
                // validFrom já vem preenchido como now() do DTO, mas garantimos aqui se necessário
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (sla.getValidFrom() == null) sla.setValidFrom(java.time.Instant.now());
                sla.setValidTo(null);
                taskSlaConfigRepository.save(sla);
            }
        }

        // Tentar reajustar cronômetros das tarefas pendentes em memória (via Dynamic Scheduling)
        /*
         * Inicia um bloco try-catch para tratar possíveis exceções.
         */
        try {
            java.util.List<com.xflprflx.neokioski.models.Task> pendings = taskRepository.findAllPending();
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (com.xflprflx.neokioski.models.Task pt : pendings) {
                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (pt.getTaskTemplate() != null && pt.getTaskTemplate().getId().equals(savedTemplate.getId())) {
                    taskAlertService.scheduleAlertsForTask(pt);
                }
            }
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao tentar reagendar alertas das tarefas ativas após edição do template: " + e.getMessage());
        }

        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return savedTemplate;
    }

    @Transactional(readOnly = true)
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TaskTemplate> findAll() {
        List<TaskTemplate> templates = taskTemplateRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder", "name"));
        initializeTemplateGraph(templates);
        return templates;
    }

    @Transactional(readOnly = true)
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public TaskTemplate getTemplateById(Long id) {
        TaskTemplate template = taskTemplateRepository.findByIdWithGroups(id).orElse(null);
        if (template != null) {
            initializeTemplateGraph(java.util.Collections.singletonList(template));
        }
        return template;
    }

    @Transactional(readOnly = true)
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TaskTemplate> getTemplatesByGroupId(Long groupId) {
        List<TaskTemplate> templates = taskTemplateRepository.findByGroupId(groupId);
        initializeTemplateGraph(templates);
        return templates;
    }

    private void initializeTemplateGraph(List<TaskTemplate> templates) {
        if (templates == null || templates.isEmpty()) return;

        // Com o @BatchSize(size = 100), iterar sobre os templates acionando a
        // inicialização NÃO causa N+1. O Hibernate carregará lotes de 100 itens.
        templates.forEach(tt -> {
            if (tt == null) return;
            
            Hibernate.initialize(tt.getTemplateStepRelation());
            Hibernate.initialize(tt.getAuthorizingUsers());
            
            java.util.Set<com.xflprflx.neokioski.models.template.TemplateFieldRelation> fieldRels = tt.getTemplateFieldRelation();
            Hibernate.initialize(fieldRels);
            
            if (fieldRels != null) {
                fieldRels.forEach(fr -> {
                    if (fr != null && fr.getTaskFieldDefinition() != null) {
                        Hibernate.initialize(fr.getTaskFieldDefinition().getTaskFieldOptions());
                    }
                });
            }
        });
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TemplateFieldRelation> getFieldRelationsByTemplateId(Long templateId) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return templateFieldRelationRepository.findByTaskTemplateIdOrderByOrderAsc(templateId);
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TemplateStepRelation> getStepRelationsByTemplateId(Long templateId) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return templateStepRelationRepository.findByTaskTemplateIdOrderByOrderAsc(templateId);
    }

    @Transactional
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void deleteTemplate(Long id) {
        templateFieldRelationRepository.deleteByTaskTemplateId(id);
        templateStepRelationRepository.deleteByTaskTemplateId(id);
        List<TaskAlert> alerts = taskAlertRepository.findByTaskTemplateId(id);
        taskAlertRepository.deleteAll(alerts);
        taskTemplateRepository.deleteById(id);
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TaskSlaConfig> getSlaConfigsByTemplateId(Long templateId) {
        TaskTemplate template = taskTemplateRepository.findById(templateId).orElse(null);
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (template == null) return new java.util.ArrayList<>();
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return taskSlaConfigRepository.findByTaskTemplateAndValidToIsNull(template);
    }
}
