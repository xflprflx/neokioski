package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.TaskEvent;
import com.xflprflx.neokioski.models.enums.SlaStatus;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import com.xflprflx.neokioski.repositories.TaskRepository;
import com.xflprflx.neokioski.repositories.TaskSlaResultRepository;
import com.xflprflx.neokioski.repositories.TaskTemplateRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final TaskRepository taskRepository;
    private final TaskSlaResultRepository slaResultRepository;
    private final TaskTemplateRepository templateRepository;
    private final com.xflprflx.neokioski.repositories.TemplateStepRelationRepository stepRelationRepository;

    public DashboardService(TaskRepository taskRepository,
                            TaskSlaResultRepository slaResultRepository,
                            TaskTemplateRepository templateRepository,
                            com.xflprflx.neokioski.repositories.TemplateStepRelationRepository stepRelationRepository) {
        this.taskRepository = taskRepository;
        this.slaResultRepository = slaResultRepository;
        this.templateRepository = templateRepository;
        this.stepRelationRepository = stepRelationRepository;
    }

    public DashboardData getDashboardData(Instant start, Instant end, Group group, TaskTemplate type, String creatorNameFilter, String finalizerNameFilter, String dateFilterStr) {
        // Buscar todas as tasks no período base de dados limitados pelas DatePickers com JOIN FETCH (Otimização Fase 2)
        List<Task> tasks = taskRepository.findAllByCreatedAtBetweenWithAssociations(start, end);

        // Pré-carregar todas as relações de etapas com JOIN FETCH (Otimização Fase 2)
        List<TemplateStepRelation> allRelations = stepRelationRepository.findAllWithAssociations();
        Map<String, Boolean> deliveryStepsMap = allRelations.stream()
                .filter(r -> r.isHasResponseField() && r.getResponseFieldLabel() != null && 
                            r.getResponseFieldLabel().toUpperCase().contains("DELIVERY"))
                .collect(Collectors.toMap(
                    r -> r.getTaskTemplate().getId() + ":" + r.getTaskStepDefinition().getId(),
                    r -> true,
                    (v1, v2) -> v1
                ));

        // 1. Filtros Globais Rígidos (Data Boundary e Grupo afetam absolutamente TUDO)
        List<Task> globalBaseTasks = tasks.stream()
                .filter(t -> group == null || group.equals(t.getGroup()))
                .collect(Collectors.toList());

        // 2. Ranking Base (Menu de Criadores) -> Afetado por Tipo, Concluidor e Data Clique. Ignora clique em Criador.
        List<Task> rankingBaseTasks = globalBaseTasks.stream()
                .filter(t -> type == null || type.equals(t.getTaskTemplate()))
                .filter(t -> finalizerNameFilter == null || Objects.equals(getTaskFinalizerName(t, deliveryStepsMap), finalizerNameFilter))
                .filter(t -> dateFilterStr == null || formatDate(t.getCreatedAt()).equals(dateFilterStr))
                .collect(Collectors.toList());

        List<ChartDataPoint> userRanking = rankingBaseTasks.stream()
                .filter(t -> t.getCreator() != null)
                .collect(Collectors.groupingBy(t -> t.getCreator().getName(), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ChartDataPoint(e.getKey(), e.getValue().doubleValue()))
                .sorted(Comparator.comparing(ChartDataPoint::value).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // 2b. Ranking Concluidores (Menu de Concluidores) -> Afetado por Tipo, Criador e Data Clique. Ignora clique em Concluidor.
        List<Task> finalizerBaseTasks = globalBaseTasks.stream()
                .filter(t -> type == null || type.equals(t.getTaskTemplate()))
                .filter(t -> creatorNameFilter == null || (t.getCreator() != null && t.getCreator().getName().equals(creatorNameFilter)))
                .filter(t -> dateFilterStr == null || formatDate(t.getCreatedAt()).equals(dateFilterStr))
                .collect(Collectors.toList());

        List<ChartDataPoint> finalizerRanking = finalizerBaseTasks.stream()
                .map(t -> getTaskFinalizerName(t, deliveryStepsMap))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ChartDataPoint(e.getKey(), e.getValue().doubleValue()))
                .sorted(Comparator.comparing(ChartDataPoint::value).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // 3. TMA Base (Menu de Tipos) -> Afetado por Criador, Concluidor e Data Clique. Ignora clique em Tipo.
        List<Task> tmaBaseTasks = globalBaseTasks.stream()
                .filter(t -> creatorNameFilter == null || (t.getCreator() != null && t.getCreator().getName().equals(creatorNameFilter)))
                .filter(t -> finalizerNameFilter == null || Objects.equals(getTaskFinalizerName(t, deliveryStepsMap), finalizerNameFilter))
                .filter(t -> dateFilterStr == null || formatDate(t.getCreatedAt()).equals(dateFilterStr))
                .collect(Collectors.toList());

        // 4. Historico Base (Menu de Datas) -> Afetado por Tipo, Criador e Concluidor Clique. Ignora clique em Data.
        List<Task> historyBaseTasks = globalBaseTasks.stream()
                .filter(t -> type == null || type.equals(t.getTaskTemplate()))
                .filter(t -> creatorNameFilter == null || (t.getCreator() != null && t.getCreator().getName().equals(creatorNameFilter)))
                .filter(t -> finalizerNameFilter == null || Objects.equals(getTaskFinalizerName(t, deliveryStepsMap), finalizerNameFilter))
                .collect(Collectors.toList());

        // 5. Strict Base (Variáveis centrais da tela KPIs, afetado por TODOS OS SELETORES EM CADEIA)
        List<Task> strictFilteredTasks = globalBaseTasks.stream()
                .filter(t -> type == null || type.equals(t.getTaskTemplate()))
                .filter(t -> creatorNameFilter == null || (t.getCreator() != null && t.getCreator().getName().equals(creatorNameFilter)))
                .filter(t -> finalizerNameFilter == null || Objects.equals(getTaskFinalizerName(t, deliveryStepsMap), finalizerNameFilter))
                .filter(t -> dateFilterStr == null || formatDate(t.getCreatedAt()).equals(dateFilterStr))
                .collect(Collectors.toList());

        // KPI Calculations (Restritos ao extremo)
        long active = strictFilteredTasks.stream().filter(t -> t.getFinalizedAt() == null).count();
        List<Task> completed = strictFilteredTasks.stream().filter(t -> t.getFinalizedAt() != null).toList();
        long completedTotal = completed.size();

        long onTime = 0;
        long delayed = 0;
        for (Task t : completed) {
            // No NeoKioski, uma task pode ter múltiplos SLA results (um por step). 
            // Para o dashboard, consideramos se o SLA FINAL da task (calculado na finalização) foi respeitado.
            // Se houver qualquer atraso nos passos, ou se o SLA global foi atingido.
            boolean hasDelay = t.getSlaResults().stream().anyMatch(r -> r.getStatus() == SlaStatus.DELAYED);
            if (hasDelay) delayed++;
            else onTime++;
        }

        double onTimePct = completedTotal == 0 ? 0 : (double) onTime / completedTotal * 100;
        double iqmTma = calculateInterquartileMeanTma(completed);

        DashboardStats stats = new DashboardStats(active, completedTotal, onTime, delayed, onTimePct, iqmTma);

        // Gráfico Misto: Histórico Diário (USADO PELOS SPARK-LINES DOS CARDS NO strictFilteredTasks)
        List<MixedChartPoint> history = historyBaseTasks.stream()
                .collect(Collectors.groupingBy(t -> formatDate(t.getCreatedAt()), TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> {
                    String date = e.getKey();
                    List<Task> dayTasks = e.getValue();
                    long dTotal = dayTasks.size();
                    long dCompleted = dayTasks.stream().filter(t -> t.getFinalizedAt() != null).count();
                    long dOnTime = dayTasks.stream().filter(t -> t.getFinalizedAt() != null && 
                            t.getSlaResults().stream().noneMatch(r -> r.getStatus() == SlaStatus.DELAYED)).count();
                    long dDelayed = dCompleted - dOnTime;
                    double dPct = dCompleted == 0 ? 0 : (double) dOnTime / dCompleted * 100;
                    return new MixedChartPoint(date, dTotal, dOnTime, dDelayed, dPct);
                })
                .collect(Collectors.toList());

        // TMA por Etapa (Gráfico Vertical Empilhado) USANDO A BASE ESPECÍFICA DO TMA
        List<Task> completedForTma = tmaBaseTasks.stream().filter(t -> t.getFinalizedAt() != null).toList();
        Map<String, List<Task>> tasksByTemplate = completedForTma.stream()
                .collect(Collectors.groupingBy(t -> t.getTaskTemplate().getName()));

        List<StackedBarData> tmaPerStep = new ArrayList<>();

        for (Map.Entry<String, List<Task>> entry : tasksByTemplate.entrySet()) {
            String templateName = entry.getKey();
            List<Task> templateTasks = entry.getValue();

            Map<String, Long> stepTotalSeconds = new LinkedHashMap<>();
            Map<String, Integer> stepCounts = new LinkedHashMap<>();

            for (Task task : templateTasks) {
                List<TaskEvent> events = task.getTaskEvents().stream()
                        .sorted(Comparator.comparing(TaskEvent::getTimestamp))
                        .toList();

                if (events.isEmpty()) continue;

                for (int i = 0; i < events.size(); i++) {
                    TaskEvent currentEvent = events.get(i);
                    String stepName = currentEvent.getTaskStepDefinition() != null ? 
                            currentEvent.getTaskStepDefinition().getLabel() : "Criação";

                    Instant startTime = currentEvent.getTimestamp();
                    Instant endTime;

                    if (i < events.size() - 1) {
                        endTime = events.get(i + 1).getTimestamp();
                    } else {
                        endTime = task.getFinalizedAt();
                    }

                    if (endTime != null && startTime != null && !endTime.isBefore(startTime)) {
                        long durationSeconds = Duration.between(startTime, endTime).getSeconds();
                        
                        stepTotalSeconds.merge(stepName, durationSeconds, Long::sum);
                        stepCounts.merge(stepName, 1, Integer::sum);
                    }
                }
            }

            double trueTemplateAvg = templateTasks.stream()
                    .filter(t -> t.getFinalizedAt() != null)
                    .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getFinalizedAt()).toSeconds())
                    .average().orElse(0);

            Map<String, Double> seriesValues = new LinkedHashMap<>();
            for (Map.Entry<String, Long> stepEntry : stepTotalSeconds.entrySet()) {
                String step = stepEntry.getKey();
                long totalTime = stepEntry.getValue();
                int count = stepCounts.getOrDefault(step, 1);
                double tma = (double) totalTime / count;
                seriesValues.put(step, tma);
            }

            tmaPerStep.add(new StackedBarData(templateName, trueTemplateAvg, seriesValues));
        }

        return new DashboardData(stats, userRanking, finalizerRanking, history, tmaPerStep);
    }

    private String getTaskFinalizerName(Task t, Map<String, Boolean> deliveryStepsMap) {
        if (t.getTaskEvents() == null || t.getTaskEvents().isEmpty()) return null;
        
        // Buscar o evento que preencheu o campo de Delivery
        for (TaskEvent event : t.getTaskEvents()) {
            if (event.getTaskStepDefinition() == null) continue;
            
            String key = t.getTaskTemplate().getId() + ":" + event.getTaskStepDefinition().getId();
            if (deliveryStepsMap.getOrDefault(key, false)) {
                return event.getUser() != null ? event.getUser().getName() : null;
            }
        }
        return null;
    }

    private double calculateInterquartileMeanTma(List<Task> completed) {
        if (completed.isEmpty()) return 0;
        
        List<Long> durations = completed.stream()
                .map(t -> Duration.between(t.getCreatedAt(), t.getFinalizedAt()).toSeconds())
                .sorted()
                .collect(Collectors.toList());

        int size = durations.size();
        if (size < 4) {
            return durations.stream().mapToLong(Long::longValue).average().orElse(0);
        }

        int q1Idx = size / 4;
        int q3Idx = size * 3 / 4;

        // Pegar o miolo (50% central)
        List<Long> middleDurations = durations.subList(q1Idx, q3Idx);
        return middleDurations.stream().mapToLong(Long::longValue).average().orElse(0); // Retornar em segundos
    }

    private String formatDate(Instant instant) {
        return DateTimeFormatter.ofPattern("dd/MM")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    // DTOs
    public record DashboardStats(
        long activeTasks,
        long completedTasks,
        long completedOnTime,
        long completedDelayed,
        double onTimePercentage,
        double averageResponseTimeSeconds
    ) {}

    public record ChartDataPoint(String label, double value) {}

    public record MixedChartPoint(
        String date,
        long total,
        long onTime,
        long delayed,
        double percentage
    ) {}

    public record StackedBarData(
        String category,
        double totalAvgSeconds,
        Map<String, Double> seriesValues
    ) {}

    public record DashboardData(
        DashboardStats stats,
        List<ChartDataPoint> userRanking,
        List<ChartDataPoint> finalizerRanking,
        List<MixedChartPoint> history,
        List<StackedBarData> tmaPerStep
    ) {}
}
