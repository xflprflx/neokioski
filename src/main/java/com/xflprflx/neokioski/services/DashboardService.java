package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.TaskEvent;
import com.xflprflx.neokioski.models.TaskSlaResult;
import com.xflprflx.neokioski.models.enums.SlaStatus;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import com.xflprflx.neokioski.repositories.TaskRepository;
import com.xflprflx.neokioski.repositories.TaskSlaResultRepository;
import com.xflprflx.neokioski.repositories.TaskTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
/**
 * Serviço que consolida os dados e lógicas necessárias para a exibição dos painéis gerenciais e métricas do dashboard.
 */
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

    @Transactional(readOnly = true)
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public DashboardData getDashboardData(Instant start, Instant end, Group group, TaskTemplate type, String creatorNameFilter, String finalizerNameFilter, String dateFilterStr, String slaNameFilter, String justificationFilter, String keywordFilter) {
        // Buscar todas as tasks no período base de dados limitados pelas DatePickers
        List<Task> tasks = taskRepository.findAllByCreatedAtBetween(start, end);

        // 1. Filtros Globais Rígidos (Data Boundary e Grupo afetam absolutamente TUDO)
        List<Task> globalBaseTasks = tasks.stream()
                .filter(t -> group == null || group.equals(t.getGroup()))
                .collect(Collectors.toList());

        // #improve_n+1 pre-cache em lote para evitar query N+1 em loop
        Map<String, TemplateStepRelation> relCache = new HashMap<>();
        Set<Long> uniqueTemplateIds = globalBaseTasks.stream()
                .map(t -> t.getTaskTemplate() != null ? t.getTaskTemplate().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<TemplateStepRelation> allRels = new ArrayList<>();
        if (!uniqueTemplateIds.isEmpty()) {
            allRels = stepRelationRepository.findByTaskTemplateIdIn(uniqueTemplateIds);
            for (TemplateStepRelation r : allRels) {
                if (r.getTaskTemplate() != null && r.getTaskStepDefinition() != null) {
                    relCache.put(r.getTaskTemplate().getId() + "-" + r.getTaskStepDefinition().getId(), r);
                }
            }
        } else {
            // Se não há tasks, podemos pular o processamento pesado
            return new DashboardData(new DashboardStats(0, 0, 0, 0, 0, 0), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        }

        // Mapear temas das etapas para cores para uso no gráfico de TMA
        Map<String, String> stepThemes = new HashMap<>();
        for (TemplateStepRelation r : allRels) {
            if (r.getTaskStepDefinition() != null) {
                stepThemes.putIfAbsent(r.getTaskStepDefinition().getLabel(), r.getTaskStepDefinition().getColorTheme());
            }
        }

        // 2. Ranking Base (Menu de Criadores) -> Afetado por Tipo, Concluidor e Data Clique. Ignora clique em Criador.
        List<Task> rankingBaseTasks = globalBaseTasks.stream()
                .filter(t -> type == null || type.equals(t.getTaskTemplate()))
                .filter(t -> finalizerNameFilter == null || Objects.equals(getTaskFinalizerName(t, relCache), finalizerNameFilter))
                .filter(t -> dateFilterStr == null || formatDate(t.getCreatedAt()).equals(dateFilterStr))
                .filter(t -> slaNameFilter == null || t.getSlaResults().stream().anyMatch(r -> r.getSlaConfig() != null && Objects.equals(r.getSlaConfig().getName(), slaNameFilter)))
                .filter(t -> justificationFilter == null || t.getSlaResults().stream().anyMatch(r -> Objects.equals(r.getJustification(), justificationFilter)))
                .filter(t -> keywordFilter == null || keywordFilter.isBlank() || t.getSlaResults().stream().anyMatch(r -> 
                        r.getJustificationComments() != null && r.getJustificationComments().toLowerCase().contains(keywordFilter.toLowerCase())))
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
                .filter(t -> slaNameFilter == null || t.getSlaResults().stream().anyMatch(r -> r.getSlaConfig() != null && Objects.equals(r.getSlaConfig().getName(), slaNameFilter)))
                .filter(t -> justificationFilter == null || t.getSlaResults().stream().anyMatch(r -> Objects.equals(r.getJustification(), justificationFilter)))
                .filter(t -> keywordFilter == null || keywordFilter.isBlank() || t.getSlaResults().stream().anyMatch(r -> 
                        r.getJustificationComments() != null && r.getJustificationComments().toLowerCase().contains(keywordFilter.toLowerCase())))
                .collect(Collectors.toList());

        List<ChartDataPoint> finalizerRanking = finalizerBaseTasks.stream()
                .map(t -> getTaskFinalizerName(t, relCache))
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
                .filter(t -> finalizerNameFilter == null || Objects.equals(getTaskFinalizerName(t, relCache), finalizerNameFilter))
                .filter(t -> dateFilterStr == null || formatDate(t.getCreatedAt()).equals(dateFilterStr))
                .filter(t -> slaNameFilter == null || t.getSlaResults().stream().anyMatch(r -> r.getSlaConfig() != null && Objects.equals(r.getSlaConfig().getName(), slaNameFilter)))
                .filter(t -> justificationFilter == null || t.getSlaResults().stream().anyMatch(r -> Objects.equals(r.getJustification(), justificationFilter)))
                .filter(t -> keywordFilter == null || keywordFilter.isBlank() || t.getSlaResults().stream().anyMatch(r -> 
                        r.getJustificationComments() != null && r.getJustificationComments().toLowerCase().contains(keywordFilter.toLowerCase())))
                .collect(Collectors.toList());

        // 4. Historico Base (Menu de Datas) -> Afetado por Tipo, Criador e Concluidor Clique. Ignora clique em Data.
        List<Task> historyBaseTasks = globalBaseTasks.stream()
                .filter(t -> type == null || type.equals(t.getTaskTemplate()))
                .filter(t -> creatorNameFilter == null || (t.getCreator() != null && t.getCreator().getName().equals(creatorNameFilter)))
                .filter(t -> finalizerNameFilter == null || Objects.equals(getTaskFinalizerName(t, relCache), finalizerNameFilter))
                .filter(t -> slaNameFilter == null || t.getSlaResults().stream().anyMatch(r -> r.getSlaConfig() != null && Objects.equals(r.getSlaConfig().getName(), slaNameFilter)))
                .filter(t -> justificationFilter == null || t.getSlaResults().stream().anyMatch(r -> Objects.equals(r.getJustification(), justificationFilter)))
                .filter(t -> keywordFilter == null || keywordFilter.isBlank() || t.getSlaResults().stream().anyMatch(r -> 
                        r.getJustificationComments() != null && r.getJustificationComments().toLowerCase().contains(keywordFilter.toLowerCase())))
                .collect(Collectors.toList());

        // 5. Strict Base (Variáveis centrais da tela KPIs, afetado por TODOS OS SELETORES EM CADEIA)
        List<Task> strictFilteredTasks = globalBaseTasks.stream()
                .filter(t -> type == null || type.equals(t.getTaskTemplate()))
                .filter(t -> creatorNameFilter == null || (t.getCreator() != null && t.getCreator().getName().equals(creatorNameFilter)))
                .filter(t -> finalizerNameFilter == null || Objects.equals(getTaskFinalizerName(t, relCache), finalizerNameFilter))
                .filter(t -> dateFilterStr == null || formatDate(t.getCreatedAt()).equals(dateFilterStr))
                .filter(t -> slaNameFilter == null || t.getSlaResults().stream().anyMatch(r -> r.getSlaConfig() != null && Objects.equals(r.getSlaConfig().getName(), slaNameFilter)))
                .filter(t -> justificationFilter == null || t.getSlaResults().stream().anyMatch(r -> Objects.equals(r.getJustification(), justificationFilter)))
                .filter(t -> keywordFilter == null || keywordFilter.isBlank() || t.getSlaResults().stream().anyMatch(r -> 
                        r.getJustificationComments() != null && r.getJustificationComments().toLowerCase().contains(keywordFilter.toLowerCase())))
                .collect(Collectors.toList());

        // KPI Calculations (Restritos ao extremo)
        long active = strictFilteredTasks.stream().filter(t -> t.getFinalizedAt() == null).count();
        List<Task> completed = strictFilteredTasks.stream().filter(t -> t.getFinalizedAt() != null).toList();
        long completedTotal = completed.size();

        long onTime = 0;
        long delayed = 0;
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (Task t : completed) {
            // No NeoKioski, uma task pode ter múltiplos SLA results (um por step). 
            // Para o dashboard, consideramos se o SLA FINAL da task (calculado na finalização) foi respeitado.
            // Se houver qualquer atraso nos passos, ou se o SLA global foi atingido.
            boolean hasDelay = t.getSlaResults().stream().anyMatch(r -> r.getStatus() == SlaStatus.DELAYED);
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
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
                    /*
                     * Retorna o resultado final processado pelo métodx.
                     */
                    return new MixedChartPoint(date, dTotal, dOnTime, dDelayed, dPct);
                })
                .collect(Collectors.toList());

        // TMA por Etapa (Gráfico Vertical Empilhado) USANDO A BASE ESPECÍFICA DO TMA
        List<Task> completedForTma = tmaBaseTasks.stream().filter(t -> t.getFinalizedAt() != null).toList();
        Map<String, List<Task>> tasksByTemplate = completedForTma.stream()
                .collect(Collectors.groupingBy(t -> t.getTaskTemplate().getName()));

        List<StackedBarData> tmaPerStep = new ArrayList<>();

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (Map.Entry<String, List<Task>> entry : tasksByTemplate.entrySet()) {
            String templateName = entry.getKey();
            List<Task> templateTasks = entry.getValue();

                Map<String, Double> stepTotalSeconds = new LinkedHashMap<>();
                Map<String, Integer> stepCounts = new LinkedHashMap<>();

            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (Task task : templateTasks) {
                List<TaskEvent> events = task.getTaskEvents().stream()
                        .sorted(Comparator.comparing(TaskEvent::getTimestamp))
                        .toList();

                /*
                 * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                 */
                if (events.isEmpty()) continue;

                /*
                 * Inicia um laço de repetição (for) para iterar sobre os elementos.
                 */
                for (int i = 0; i < events.size(); i++) {
                    TaskEvent currentEvent = events.get(i);
                    String stepName = currentEvent.getTaskStepDefinition() != null ? 
                            currentEvent.getTaskStepDefinition().getLabel() : "Criação";

                    // Filtrar steps finalizadores — não contribuem para o TMA
                    // (são instantâneos e poluem o gráfico sem valor analítico)
                    if (currentEvent.getTaskStepDefinition() != null) {
                        String cacheKey = task.getTaskTemplate().getId() + "-" + currentEvent.getTaskStepDefinition().getId();
                        TemplateStepRelation rel = relCache.get(cacheKey);
                        if (rel != null && rel.isFinalizer()) continue;
                    }

                    Instant startTime = currentEvent.getTimestamp();
                    Instant endTime;

                    /*
                     * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                     */
                    if (i < events.size() - 1) {
                        endTime = events.get(i + 1).getTimestamp();
                    } else {
                        endTime = task.getFinalizedAt();
                    }

                    /*
                     * Avalia uma condição (if) para desvio de fluxo da lógica principal.
                     */
                    if (endTime != null && startTime != null && !endTime.isBefore(startTime)) {
                        double durationSeconds = Duration.between(startTime, endTime).toMillis() / 1000.0;
                        
                        stepTotalSeconds.merge(stepName, durationSeconds, Double::sum);
                        stepCounts.merge(stepName, 1, Integer::sum);
                    }
                }
            }

            double trueTemplateAvg = templateTasks.stream()
                    .filter(t -> t.getFinalizedAt() != null)
                    .mapToDouble(t -> Duration.between(t.getCreatedAt(), t.getFinalizedAt()).toMillis() / 1000.0)
                    .average().orElse(0);

            Map<String, Double> seriesValues = new LinkedHashMap<>();
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (Map.Entry<String, Double> stepEntry : stepTotalSeconds.entrySet()) {
                String step = stepEntry.getKey();
                double totalTime = stepEntry.getValue();
                int count = stepCounts.getOrDefault(step, 1);
                double tma = (double) totalTime / count;
                seriesValues.put(step, tma);
            }

            tmaPerStep.add(new StackedBarData(templateName, trueTemplateAvg, seriesValues));
        }

        // === INSIGHTS DE SLA ===
        // Performance por nome de SLA
        List<TaskSlaResult> allSlaResults = strictFilteredTasks.stream()
                .filter(t -> t.getSlaResults() != null)
                .flatMap(t -> t.getSlaResults().stream())
                .toList();

        Map<String, List<TaskSlaResult>> resultsBySlaName = allSlaResults.stream()
                .filter(r -> r.getSlaConfig() != null && r.getSlaConfig().getName() != null)
                .collect(Collectors.groupingBy(r -> r.getSlaConfig().getName()));

        List<SlaInsightData> slaInsights = resultsBySlaName.entrySet().stream()
                .map(entry -> {
                    String slaName = entry.getKey();
                    List<TaskSlaResult> results = entry.getValue();
                    long total = results.size();
                    long slaOnTime = results.stream().filter(r -> r.getStatus() == SlaStatus.ON_TIME).count();
                    long slaDelayed = total - slaOnTime;
                    double pct = total == 0 ? 0 : (double) slaOnTime / total * 100;
                    return new SlaInsightData(slaName, total, slaOnTime, slaDelayed, pct);
                })
                .sorted(Comparator.comparing(SlaInsightData::total).reversed())
                .toList();

        // Ranking de justificativas mais frequentes
        List<JustificationRankData> justificationRanking = allSlaResults.stream()
                .filter(r -> r.getStatus() == SlaStatus.DELAYED)
                .filter(r -> r.getJustification() != null && !r.getJustification().isBlank())
                .collect(Collectors.groupingBy(TaskSlaResult::getJustification, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new JustificationRankData(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(JustificationRankData::count).reversed())
                .limit(10)
                .toList();

        // Coleta de comentários opcionais e nuvem de palavras com identificação da tarefa
        List<RecentCommentData> recentComments = strictFilteredTasks.stream()
                .filter(t -> t.getSlaResults() != null)
                .flatMap(t -> t.getSlaResults().stream()
                        .filter(r -> r.getStatus() == SlaStatus.DELAYED)
                        .filter(r -> r.getJustificationComments() != null && !r.getJustificationComments().isBlank())
                        .map(r -> new RecentCommentData(
                                t.getId(),
                                t.getTaskTemplate() != null ? t.getTaskTemplate().getName() : "Tarefa",
                                r.getJustificationComments()
                        )))
                .sorted(Comparator.comparing(RecentCommentData::comment).reversed())
                .limit(20)
                .toList();

        // Gerar nuvem de palavras simplificada (ignorando tamanho para permitir siglas, usando lista de exclusão)
        List<String> stopWords = Arrays.asList(
                "o", "a", "os", "as", "um", "uma", "uns", "umas",
                "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
                "por", "para", "com", "que", "se", "sua", "seu", "suas", "seus",
                "este", "esta", "estes", "estas", "esse", "essa", "esses", "essas",
                "aquele", "aquela", "aqueles", "aquelas", "isso", "isto", "aquilo",
                "pela", "pelo", "pelas", "pelos", "ao", "aos", "à", "às",
                "e", "ou", "mas", "como", "qual", "quais",
                "eu", "tu", "ele", "ela", "nós", "vós", "eles", "elas",
                "me", "te", "nos", "vos", "lhe", "lhes",
                "já", "só", "até", "nem", "também", "mais", "muito", "pouco",
                "não", "sim", "foi", "era", "ser", "são", "tem", "ter", "tinha", "está", "estava", "estão",
                "quando", "onde", "quem", "porque", "pois"
        );

        Map<String, Long> wordFreq = allSlaResults.stream()
                .filter(r -> r.getStatus() == SlaStatus.DELAYED)
                .filter(r -> r.getJustificationComments() != null && !r.getJustificationComments().isBlank())
                .flatMap(r -> Arrays.stream(r.getJustificationComments().toLowerCase()
                        .replaceAll("[^a-zA-Z0-9áéíóúàèìòùâêîôûãõç.\\-]", " ")
                        .split("\\s+")))
                .filter(w -> !w.isBlank())
                .filter(w -> !stopWords.contains(w))
                .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

        List<JustificationRankData> wordCloud = wordFreq.entrySet().stream()
                .map(e -> new JustificationRankData(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(JustificationRankData::count).reversed())
                .limit(30)
                .toList();

        return new DashboardData(stats, userRanking, finalizerRanking, history, tmaPerStep, slaInsights, justificationRanking, recentComments, wordCloud, stepThemes);
    }

    /**
     * Identifica o nome do concluidor de uma tarefa para fins de ranking no Dashboard.
     *
     * <p>A cadeia de prioridade é:</p>
     * <ol>
     *   <li>{@code countsAsCompletion} — passo explicitamente marcado como "pontua conclusão" no template.</li>
     *   <li>{@code isFinalizer} — fallback para templates legados que não utilizam o novo flag.</li>
     * </ol>
     *
     * <p>Retorna {@code null} quando a tarefa ainda não atingiu nenhum passo relevante,
     * evitando que interações intermediárias (criação, avanço de passo) contabilizem pontos
     * indevidamente no ranking de concluidores.</p>
     *
     * @param t        a tarefa cujo concluidor será identificado.
     * @param relCache cache de {@link TemplateStepRelation} pré-carregado em lote para evitar N+1.
     * @return o nome do usuário concluidor, ou {@code null} se a tarefa não atingiu o passo de conclusão.
     */
    private String getTaskFinalizerName(Task t, Map<String, TemplateStepRelation> relCache) {
        if (t.getTaskEvents() == null || t.getTaskEvents().isEmpty()) return null;

        List<TaskEvent> events = new ArrayList<>(t.getTaskEvents());
        events.sort(Comparator.comparing(TaskEvent::getTimestamp).reversed());

        // 1. Prioridade máxima: passo marcado com countsAsCompletion
        for (TaskEvent event : events) {
            if (event.getTaskStepDefinition() == null) continue;

            String cacheKey = t.getTaskTemplate().getId() + "-" + event.getTaskStepDefinition().getId();
            TemplateStepRelation relation = relCache.get(cacheKey);

            if (relation != null && relation.isCountsAsCompletion()) {
                return event.getUser() != null ? event.getUser().getName() : null;
            }
        }

        // 2. Fallback: passo finalizador (comportamento legado)
        for (TaskEvent event : events) {
            if (event.getTaskStepDefinition() == null) continue;

            String cacheKey = t.getTaskTemplate().getId() + "-" + event.getTaskStepDefinition().getId();
            TemplateStepRelation relation = relCache.get(cacheKey);

            if (relation != null && relation.isFinalizer()) {
                return event.getUser() != null ? event.getUser().getName() : null;
            }
        }

        // Nenhum passo de conclusão atingido — não contabiliza no ranking
        return null;
    }


    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private double calculateInterquartileMeanTma(List<Task> completed) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (completed.isEmpty()) return 0;
        
        List<Long> durations = completed.stream()
                .map(t -> Duration.between(t.getCreatedAt(), t.getFinalizedAt()).toSeconds())
                .sorted()
                .collect(Collectors.toList());

        int size = durations.size();
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (size < 4) {
            /*
             * Retorna o resultado final processado pelo métodx.
             */
            return durations.stream().mapToLong(Long::longValue).average().orElse(0);
        }

        int q1Idx = size / 4;
        int q3Idx = size * 3 / 4;

        // Pegar o miolo (50% central)
        List<Long> middleDurations = durations.subList(q1Idx, q3Idx);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return middleDurations.stream().mapToLong(Long::longValue).average().orElse(0); // Retornar em segundos
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private String formatDate(Instant instant) {
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return DateTimeFormatter.ofPattern("dd/MM")
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    // DTOs
    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public record DashboardStats(
        long activeTasks,
        long completedTasks,
        long completedOnTime,
        long completedDelayed,
        double onTimePercentage,
        double averageResponseTimeSeconds
    ) {}

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public record ChartDataPoint(String label, double value) {}

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public record MixedChartPoint(
        String date,
        long total,
        long onTime,
        long delayed,
        double percentage
    ) {}

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public record StackedBarData(
        String category,
        double totalAvgSeconds,
        Map<String, Double> seriesValues
    ) {}

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public record SlaInsightData(
        String slaName,
        long total,
        long onTime,
        long delayed,
        double onTimePct
    ) {}

    public record JustificationRankData(
        String justification,
        long count
    ) {}

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public record RecentCommentData(
        Long taskId,
        String taskName,
        String comment
    ) {}

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public record DashboardData(
        DashboardStats stats,
        List<ChartDataPoint> userRanking,
        List<ChartDataPoint> finalizerRanking,
        List<MixedChartPoint> history,
        List<StackedBarData> tmaPerStep,
        List<SlaInsightData> slaInsights,
        List<JustificationRankData> justificationRanking,
        List<RecentCommentData> recentComments,
        List<JustificationRankData> wordCloud,
        Map<String, String> stepThemes
    ) {}
}
