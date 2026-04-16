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
public class TaskSlaService {

    private final TaskSlaConfigRepository configRepository;
    private final TaskSlaResultRepository resultRepository;

    public TaskSlaService(TaskSlaConfigRepository configRepository, TaskSlaResultRepository resultRepository) {
        this.configRepository = configRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional
    public void calculateAndSaveSla(Task task, TaskStepDefinition reachedStep) {
        List<TaskSlaConfig> configs = configRepository.findValidConfigs(task.getTaskTemplate(), task.getCreatedAt());

        for (TaskSlaConfig config : configs) {
            // Se o config define um targetStep e ele foi atingido agora
            if (config.getTargetStep() != null && config.getTargetStep().equals(reachedStep)) {
                processSla(task, config);
            }
        }
    }

    @Transactional
    public void calculateFinalTaskSla(Task task) {
        if (task.getFinalizedAt() == null) return;
        
        List<TaskSlaConfig> configs = configRepository.findValidConfigs(task.getTaskTemplate(), task.getCreatedAt());

        for (TaskSlaConfig config : configs) {
            // Se o config não define targetStep, assume-se que é o SLA da tarefa total (finalização)
            if (config.getTargetStep() == null) {
                processSla(task, config);
            }
        }
    }

    public List<TaskSlaResult> getResultsForTask(Task task) {
        return resultRepository.findByTask(task);
    }

    private void processSla(Task task, TaskSlaConfig config) {
        Instant studyStartTime = getStartTime(task, config);
        if (studyStartTime == null) return;

        Instant endTime = config.getTargetStep() == null ? task.getFinalizedAt() : Instant.now();
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
        if (task.getSlaResults() != null) {
            task.getSlaResults().add(result);
        }
    }

    private Instant getStartTime(Task task, TaskSlaConfig config) {
        if (config.getSourceStep() == null) {
            return task.getCreatedAt();
        }

        // Busca o evento mais recente do passo de origem
        return task.getTaskEvents().stream()
                .filter(e -> config.getSourceStep().equals(e.getTaskStepDefinition()))
                .max(Comparator.comparing(TaskEvent::getTimestamp))
                .map(TaskEvent::getTimestamp)
                .orElse(null);
    }
}
