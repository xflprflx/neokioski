package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.TaskEvent;
import com.xflprflx.neokioski.models.TaskSlaResult;
import com.xflprflx.neokioski.models.enums.SlaStatus;
import com.xflprflx.neokioski.models.template.TaskSlaConfig;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.repositories.TaskSlaConfigRepository;
import com.xflprflx.neokioski.repositories.TaskSlaResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
/**
 * Serviço que calcula prazos, atualiza os resultados e gerencia a inteligência atrelada ao Nível de Serviço (SLA) das tarefas.
 */
public class TaskSlaService {

    private final TaskSlaConfigRepository configRepository;
    private final TaskSlaResultRepository resultRepository;

    public TaskSlaService(TaskSlaConfigRepository configRepository, TaskSlaResultRepository resultRepository) {
        this.configRepository = configRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void calculateAndSaveSla(Task task, TaskStepDefinition reachedStep) {
        List<TaskSlaConfig> configs = configRepository.findValidConfigs(task.getTaskTemplate(), task.getCreatedAt());

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (TaskSlaConfig config : configs) {
            // Se o config define um targetStep e ele foi atingido agora
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (config.getTargetStep() != null && config.getTargetStep().equals(reachedStep)) {
                processSla(task, config);
            }
        }
    }

    @Transactional
    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void calculateFinalTaskSla(Task task) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task.getFinalizedAt() == null) return;
        
        List<TaskSlaConfig> configs = configRepository.findValidConfigs(task.getTaskTemplate(), task.getCreatedAt());

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (TaskSlaConfig config : configs) {
            // Se o config não define targetStep, assume-se que é o SLA da tarefa total (finalização)
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (config.getTargetStep() == null) {
                processSla(task, config);
            }
        }
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public List<TaskSlaResult> getResultsForTask(Task task) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return resultRepository.findByTask(task);
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private void processSla(Task task, TaskSlaConfig config) {
        Instant studyStartTime = getStartTime(task, config);
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (studyStartTime == null) return;

        Instant endTime = config.getTargetStep() == null ? task.getFinalizedAt() : Instant.now();
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (endTime == null) return;

        Duration duration = Duration.between(studyStartTime, endTime);
        long actualSeconds = duration.getSeconds();

        // Calcular segundos alvo baseado na unidade
        long targetSeconds = switch (config.getUnit()) {
            case SECONDS -> config.getTargetValue();
            case MINUTES -> config.getTargetValue() * 60L;
            case HOURS -> config.getTargetValue() * 3600L;
        };

        SlaStatus status = actualSeconds <= targetSeconds ? SlaStatus.ON_TIME : SlaStatus.DELAYED;

        TaskSlaResult result = new TaskSlaResult();
        result.setTask(task);
        result.setSlaConfig(config);
        result.setTargetValue(config.getTargetValue());
        result.setUnit(config.getUnit());
        result.setActualSeconds(actualSeconds);
        result.setStatus(status);
        result.setCompletedAt(endTime);

        resultRepository.save(result);
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (task.getSlaResults() != null) {
            task.getSlaResults().add(result);
        }
    }

    /**
     * Métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private Instant getStartTime(Task task, TaskSlaConfig config) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (config.getSourceStep() == null) {
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return task.getCreatedAt();
        }

        // Busca o evento mais recente do passo de origem
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return task.getTaskEvents().stream()
                .filter(e -> config.getSourceStep().equals(e.getTaskStepDefinition()))
                .max(Comparator.comparing(TaskEvent::getTimestamp))
                .map(TaskEvent::getTimestamp)
                .orElse(null);
    }
}
