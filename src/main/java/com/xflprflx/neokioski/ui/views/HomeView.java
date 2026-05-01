package com.xflprflx.neokioski.ui.views;
// v2 - forced refresh for i18n

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.security.SecurityService;
import com.xflprflx.neokioski.services.DashboardService;
import com.xflprflx.neokioski.services.GroupService;
import com.xflprflx.neokioski.services.TaskTemplateService;
import com.xflprflx.neokioski.services.UserService;
import com.xflprflx.neokioski.ui.utils.TaskUIUtils;
import jakarta.annotation.security.PermitAll;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Início | NeoKioski")
@PermitAll
@JavaScript("https://cdn.jsdelivr.net/npm/apexcharts")
/**
 * View da tela inicial (Dashboard) que apresenta os principais gráficos, métricas pendentes e alertas para o usuário logado.
 */
public class HomeView extends Div {

    private final DashboardService dashboardService;
    private final SecurityService securityService;
    private final GroupService groupService;
    private final TaskTemplateService templateService;
    private final com.xflprflx.neokioski.services.TaskService taskService;
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private java.util.function.Consumer<com.xflprflx.neokioski.models.Task> taskListener;

    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private ComboBox<Group> groupFilter;
    private ComboBox<TaskTemplate> templateFilter;
    private ComboBox<User> creatorFilter;
    private ComboBox<User> finalizerFilter;
    private ComboBox<String> slaFilter;
    private ComboBox<String> justificationFilter;

    private Div kpiGrid;
    private Div rankingHeaderDiv;
    private Div finalizerRankingHeaderDiv;
    private Div userRankingChartDiv;
    private Div finalizerRankingChartDiv;
    private Div tmaStackedChartDiv;
    private Div mixedChartDiv;
    private Div slaPerformanceChartDiv;
    private Div justificationDonutChartDiv;
    private Div wordCloudDiv;
    private Div recentCommentsDiv;
    
    private DashboardService.DashboardData lastDashboardData;

    public HomeView(DashboardService dashboardService, 
                    SecurityService securityService, 
                    GroupService groupService, 
                    TaskTemplateService templateService,
                    com.xflprflx.neokioski.services.TaskService taskService,
                    UserService userService) {
        this.dashboardService = dashboardService;
        this.securityService = securityService;
        this.groupService = groupService;
        this.templateService = templateService;
        this.taskService = taskService;
        this.userService = userService;

        addClassName("dashboard-view-container");
        setId("dashboard-home");
        getStyle()
            .set("padding", "12px 24px")
            .set("box-sizing", "border-box");

        createTopSection();
        createKpiSection();
        createChartsSection();

        getElement().addEventListener("creator-clicked", e -> {
            /*
             * Inicia um bloco try-catch para evitar que erros internos quebrem a renderização da interface.
             */
            try {
                var categoryNode = e.getEventData().get("event.detail.category");
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (categoryNode != null && !categoryNode.isNull()) {
                    String clickedName = categoryNode.toString().replaceAll("^\"|\"$", "");
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (clickedName != null) {
                        creatorFilter.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>())
                            .filter(u -> u.getName().equalsIgnoreCase(clickedName))
                            .findFirst()
                            .ifPresent(creatorFilter::setValue);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).addEventData("event.detail.category");

        getElement().addEventListener("tma-clicked", e -> {
            /*
             * Inicia um bloco try-catch para evitar que erros internos quebrem a renderização da interface.
             */
            try {
                var categoryNode = e.getEventData().get("event.detail.category");
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (categoryNode != null && !categoryNode.isNull()) {
                    String templateStr = categoryNode.toString().replaceAll("^\"|\"$", "");
                    
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (templateStr != null && !templateStr.isEmpty()) {
                        final String searchName = templateStr;
                        templateService.findAll().stream()
                            .filter(t -> t.getName().equalsIgnoreCase(searchName))
                            .findFirst()
                            .ifPresent(t -> templateFilter.setValue(t));
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).addEventData("event.detail.category");

        getElement().addEventListener("finalizer-clicked", e -> {
            /*
             * Inicia um bloco try-catch para evitar que erros internos quebrem a renderização da interface.
             */
            try {
                var categoryNode = e.getEventData().get("event.detail.category");
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (categoryNode != null && !categoryNode.isNull()) {
                    String clickedName = categoryNode.toString().replaceAll("^\"|\"$", "");
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (clickedName != null) {
                        finalizerFilter.getDataProvider().fetch(new com.vaadin.flow.data.provider.Query<>())
                            .filter(u -> u.getName().equalsIgnoreCase(clickedName))
                            .findFirst()
                            .ifPresent(finalizerFilter::setValue);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).addEventData("event.detail.category");

        getElement().addEventListener("sla-clicked", e -> {
            try {
                var categoryNode = e.getEventData().get("event.detail.category");
                if (categoryNode != null && !categoryNode.isNull()) {
                    String clickedSla = categoryNode.toString().replaceAll("^\"|\"$", "");
                    if (clickedSla != null) {
                        slaFilter.setValue(clickedSla);
                    }
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }).addEventData("event.detail.category");

        getElement().addEventListener("justification-clicked", e -> {
            try {
                var categoryNode = e.getEventData().get("event.detail.category");
                if (categoryNode != null && !categoryNode.isNull()) {
                    String clickedJust = categoryNode.toString().replaceAll("^\"|\"$", "");
                    if (clickedJust != null) {
                        justificationFilter.setValue(clickedJust);
                    }
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }).addEventData("event.detail.category");

        updateDashboard();
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        com.vaadin.flow.component.UI ui = attachEvent.getUI();
        taskListener = task -> ui.access(this::updateDashboard);
        taskService.registerGlobal(taskListener);
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (taskListener != null) {
            taskService.unregisterGlobal(taskListener);
        }
        super.onDetach(detachEvent);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void createTopSection() {
        VerticalLayout topLayout = new VerticalLayout();
        topLayout.addClassNames("dashboard-header", "animate-in");
        topLayout.setWidthFull();
        topLayout.setPadding(false);
        topLayout.setSpacing(false);
        topLayout.getStyle()
                .set("animation-delay", "0.1s")
                .set("margin-bottom", "20px")
                .set("background", "var(--dash-card-bg)")
                .set("padding", "12px 24px")
                .set("border-radius", "12px")
                .set("box-shadow", "var(--dash-shadow)")
                .set("border", "1px solid var(--dash-border)")
                .set("grid-column", "span 12");

        Span title = new Span("Painel de Visibilidade Operacional");
        title.getStyle()
                .set("font-size", "1.1rem")
                .set("font-weight", "700")
                .set("color", "var(--dash-text-sec)")
                .set("margin-bottom", "12px")
                .set("letter-spacing", "0.05em")
                .set("text-transform", "uppercase");

        HorizontalLayout filtersRow = new HorizontalLayout();
        filtersRow.setWidthFull();
        filtersRow.setSpacing(true);
        filtersRow.setPadding(false);
        filtersRow.getStyle().set("overflow-x", "auto");

        startDatePicker = new DatePicker("De");
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        startDatePicker.setLocale(Locale.forLanguageTag("pt-BR"));
        startDatePicker.setI18n(TaskUIUtils.getPtBrI18n());
        startDatePicker.setWidth("140px");

        endDatePicker = new DatePicker("Até");
        endDatePicker.setValue(LocalDate.now());
        endDatePicker.setLocale(Locale.forLanguageTag("pt-BR"));
        endDatePicker.setI18n(TaskUIUtils.getPtBrI18n());
        endDatePicker.setWidth("140px");

        groupFilter = new ComboBox<>("Grupo");
        groupFilter.setItems(groupService.findAll());
        groupFilter.setItemLabelGenerator(Group::getName);
        groupFilter.setPlaceholder("Todos");
        groupFilter.setClearButtonVisible(true);
        groupFilter.setWidth("180px");

        templateFilter = new ComboBox<>("Tipo");
        templateFilter.setItems(templateService.findAll());
        templateFilter.setItemLabelGenerator(TaskTemplate::getName);
        templateFilter.setPlaceholder("Todos");
        templateFilter.setClearButtonVisible(true);
        templateFilter.setWidth("180px");

        List<User> allUsers = userService.findAll();
        creatorFilter = new ComboBox<>("Criador");
        creatorFilter.setItems(allUsers);
        creatorFilter.setItemLabelGenerator(User::getName);
        creatorFilter.setPlaceholder("Todos");
        creatorFilter.setClearButtonVisible(true);
        creatorFilter.setWidth("180px");

        finalizerFilter = new ComboBox<>("Concluidor");
        finalizerFilter.setItems(allUsers);
        finalizerFilter.setItemLabelGenerator(User::getName);
        finalizerFilter.setPlaceholder("Todos");
        finalizerFilter.setClearButtonVisible(true);
        finalizerFilter.setWidth("180px");

        slaFilter = new ComboBox<>("SLA");
        slaFilter.setPlaceholder("Todos");
        slaFilter.setClearButtonVisible(true);
        slaFilter.setWidth("150px");

        justificationFilter = new ComboBox<>("Justificativa");
        justificationFilter.setPlaceholder("Todas");
        justificationFilter.setClearButtonVisible(true);
        justificationFilter.setWidth("150px");

        startDatePicker.addValueChangeListener(e -> updateDashboard());
        endDatePicker.addValueChangeListener(e -> updateDashboard());
        groupFilter.addValueChangeListener(e -> updateDashboard());
        templateFilter.addValueChangeListener(e -> updateDashboard());
        creatorFilter.addValueChangeListener(e -> updateDashboard());
        finalizerFilter.addValueChangeListener(e -> updateDashboard());
        slaFilter.addValueChangeListener(e -> updateDashboard());
        justificationFilter.addValueChangeListener(e -> updateDashboard());

        filtersRow.add(startDatePicker, endDatePicker, groupFilter, templateFilter, creatorFilter, finalizerFilter, slaFilter, justificationFilter);
        
        topLayout.add(title, filtersRow);
        add(topLayout);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void createKpiSection() {
        kpiGrid = new Div();
        kpiGrid.addClassNames("kpi-container", "animate-in");
        kpiGrid.getStyle().set("animation-delay", "0.3s");
        // Garantir que a grid de KPIs centralize os itens e use auto-fit
        kpiGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(160px, 1fr))") // Redimensionamento fluido
                .set("gap", "16px")
                .set("justify-content", "center")
                .set("grid-column", "span 12")
                .set("width", "100%");
        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(kpiGrid);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void createChartsSection() {
        // --- LINHA 2: Evolução Diária ---
        Div evolutionRow = new Div();
        evolutionRow.addClassNames("charts-container", "animate-in");
        evolutionRow.getStyle().set("margin-top", "24px").set("width", "100%");

        Div mixedCol = createChartCard("100%");
        mixedCol.getStyle().set("height", "350px");
        mixedChartDiv = new Div();
        mixedChartDiv.setId("mixed-history-chart");
        mixedChartDiv.getStyle().set("flex-grow", "1");
        mixedCol.add(createChartHeader("Evolução Diária - Tarefas e SLA"), mixedChartDiv);
        evolutionRow.add(mixedCol);
        add(evolutionRow);

        // --- LINHA 3: TMA Médio Total por Tipo ---
        Div tmaRow = new Div();
        tmaRow.addClassNames("charts-container", "animate-in");
        tmaRow.getStyle().set("margin-top", "24px").set("width", "100%");

        Div tmaCol = createChartCard("100%");
        tmaStackedChartDiv = new Div();
        tmaStackedChartDiv.setId("tma-stacked-chart");
        tmaStackedChartDiv.getStyle().set("flex-grow", "1");
        tmaCol.add(createChartHeader("TMA Médio Total por Tipo"), tmaStackedChartDiv);
        tmaRow.add(tmaCol);
        add(tmaRow);

        // --- LINHA 4: Rankings (Criador e Concluidor) ---
        Div rankingRow = new Div();
        rankingRow.addClassNames("charts-container", "animate-in");
        rankingRow.getStyle()
                .set("display", "flex")
                .set("gap", "24px")
                .set("margin-top", "24px")
                .set("width", "100%")
                .set("flex-wrap", "nowrap"); // Forçar mesma linha

        // Ranking Criadores
        Div rankingCol = createChartCard("50%");
        rankingCol.getStyle().set("flex", "1");
        userRankingChartDiv = new Div();
        userRankingChartDiv.setId("user-ranking-chart");
        userRankingChartDiv.getStyle().set("flex-grow", "1");
        rankingHeaderDiv = createChartHeader("Ranking Criadores de Tarefas");
        rankingCol.add(rankingHeaderDiv, userRankingChartDiv);

        // Ranking Concluidores
        Div finalizerCol = createChartCard("50%");
        finalizerCol.getStyle().set("flex", "1");
        finalizerRankingChartDiv = new Div();
        finalizerRankingChartDiv.setId("finalizer-ranking-chart");
        finalizerRankingChartDiv.getStyle().set("flex-grow", "1");
        finalizerRankingHeaderDiv = createChartHeader("Ranking Concluidores (Delivery)");
        finalizerCol.add(finalizerRankingHeaderDiv, finalizerRankingChartDiv);

        rankingRow.add(rankingCol, finalizerCol);
        add(rankingRow);

        // --- LINHA 5: Performance por SLA ---
        Div slaPerformanceRow = new Div();
        slaPerformanceRow.addClassNames("charts-container", "animate-in");
        slaPerformanceRow.getStyle().set("margin-top", "24px").set("width", "100%");

        Div slaPerformanceCol = createChartCard("100%");
        slaPerformanceChartDiv = new Div();
        slaPerformanceChartDiv.setId("sla-performance-chart");
        slaPerformanceChartDiv.getStyle().set("flex-grow", "1");
        slaPerformanceCol.add(createChartHeader("Performance por SLA"), slaPerformanceChartDiv);
        slaPerformanceRow.add(slaPerformanceCol);
        add(slaPerformanceRow);

        // --- LINHA 6: Justificativas (Fim do Dashboard) ---
        Div justificationsRow = new Div();
        justificationsRow.addClassNames("charts-container", "animate-in");
        justificationsRow.getStyle()
                .set("margin-top", "24px")
                .set("width", "100%");

        Div justificationCol = createChartCard("100%");
        
        HorizontalLayout justificationContent = new HorizontalLayout();
        justificationContent.setSizeFull();
        justificationContent.setSpacing(true);

        justificationDonutChartDiv = new Div();
        justificationDonutChartDiv.setId("justification-donut-chart");
        justificationDonutChartDiv.getStyle().set("flex", "1");

        Div textAnalysisDiv = new Div();
        textAnalysisDiv.getStyle()
                .set("flex", "1")
                .set("min-width", "0")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("overflow", "hidden");

        Span textAnalysisTitle = new Span("Palavras Frequentes e Comentários");
        textAnalysisTitle.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "14px")
                .set("margin-bottom", "8px")
                .set("color", "var(--dash-text-sec)");

        wordCloudDiv = new Div();
        wordCloudDiv.setId("word-cloud-container");
        wordCloudDiv.getStyle()
                .set("flex", "1")
                .set("margin-top", "10px")
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("align-content", "start")
                .set("justify-content", "center")
                .set("gap", "8px")
                .set("overflow-y", "auto");
        
        recentCommentsDiv = new Div();
        recentCommentsDiv.setId("recent-comments-container");
        recentCommentsDiv.getStyle()
                .set("flex", "1")
                .set("margin-top", "10px")
                .set("font-size", "12px")
                .set("overflow-y", "auto")
                .set("border-top", "1px solid #e2e8f0")
                .set("padding-top", "10px");

        textAnalysisDiv.add(textAnalysisTitle, wordCloudDiv, recentCommentsDiv);
        justificationContent.add(justificationDonutChartDiv, textAnalysisDiv);
        
        justificationCol.add(createChartHeader("Justificativas de Atraso"), justificationContent);
        justificationsRow.add(justificationCol);
        add(justificationsRow);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Div createChartCard(String flexBasis) {
        Div col = new Div();
        col.addClassNames("chart-card", "animate-in");
        col.getStyle()
                .set("flex", "1 1 " + flexBasis)
                .set("background", "var(--dash-card-bg, #ffffff)")
                .set("color", "#1e293b")
                .set("border-radius", "16px")
                .set("padding", "24px")
                .set("box-shadow", "0 4px 6px -1px rgba(0,0,0,0.1)")
                .set("border", "1px solid var(--dash-border, #e2e8f0)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("height", "400px");
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return col;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Div createChartHeader(String title) {
        Div header = new Div();
        header.addClassName("chart-title");
        header.setText(title);
        header.getStyle()
                .set("margin-bottom", "-20px")
                .set("z-index", "10")
                .set("position", "relative")
                .set("color", "#1e293b");
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return header;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void updateDashboard() {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) return;

        Instant start = startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDatePicker.getValue().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Group group = groupFilter.getValue();
        TaskTemplate type = templateFilter.getValue();
        User creator = creatorFilter.getValue();
        String activeCreatorFilter = creator != null ? creator.getName() : null;
        User finalizer = finalizerFilter.getValue();
        String activeFinalizerFilter = finalizer != null ? finalizer.getName() : null;
        String activeSlaFilter = slaFilter.getValue();
        String activeJustFilter = justificationFilter.getValue();

        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, group, type, activeCreatorFilter, activeFinalizerFilter, null, activeSlaFilter, activeJustFilter);
        this.lastDashboardData = data;

        // Atualizar itens dos novos filtros se estiverem vazios
        if (slaFilter.getGenericDataView().getItems().count() == 0) {
            slaFilter.setItems(data.slaInsights().stream().map(d -> d.slaName()).sorted().collect(Collectors.toList()));
        }
        if (justificationFilter.getGenericDataView().getItems().count() == 0) {
            justificationFilter.setItems(data.justificationRanking().stream().map(d -> d.justification()).sorted().collect(Collectors.toList()));
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (activeCreatorFilter != null) {
             rankingHeaderDiv.setText("Ranking (Filtro: " + activeCreatorFilter + ")");
        } else {
             rankingHeaderDiv.setText("Ranking Criadores de Tarefas");
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (activeFinalizerFilter != null) {
             finalizerRankingHeaderDiv.setText("Ranking (Filtro: " + activeFinalizerFilter + ")");
        } else {
             finalizerRankingHeaderDiv.setText("Ranking Concluidores (Delivery)");
        }

        renderKpiCards(data.stats());
        renderCharts(data);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void renderKpiCards(DashboardService.DashboardStats stats) {
        kpiGrid.removeAll();

        long totalSeconds = (long) stats.averageResponseTimeSeconds();
        String tmaFormatted = String.format("%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);

        addKpiToGrid("% SLA Global", String.format("%.1f%%", stats.onTimePercentage()), VaadinIcon.CHART_LINE, "green", "spark-pct", 5);
        addKpiToGrid("TMA", tmaFormatted, VaadinIcon.HOURGLASS, "yellow", "spark-tma", 4);
        addKpiToGrid("Atrasadas", String.valueOf(stats.completedDelayed()), VaadinIcon.WARNING, "red", "spark-delayed", 3);
        addKpiToGrid("No Prazo", String.valueOf(stats.completedOnTime()), VaadinIcon.TIMER, "green", "spark-ontime", 2);
        addKpiToGrid("Total Concluídas", String.valueOf(stats.completedTasks()), VaadinIcon.CHECK_CIRCLE, "purple", "spark-total", 1);
        addKpiToGrid("Tarefas Ativas", String.valueOf(stats.activeTasks()), VaadinIcon.TASKS, "blue", "spark-active", 0);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void addKpiToGrid(String label, String value, VaadinIcon icon, String theme, String sparkId, int index) {
        Div cardWrapper = createStatCard(label, value, icon, theme, sparkId);
        cardWrapper.addClassName("animate-in");
        cardWrapper.getStyle().set("animation-delay", (0.35 + (index * 0.05)) + "s");
        kpiGrid.add(cardWrapper);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Div createStatCard(String label, String value, VaadinIcon icon, String theme, String sparklineId) {
        String[] colors = TaskUIUtils.getThemeColors(theme);
        String bgColor = colors[0];
        String borderColor = colors[1];
        boolean isDark = Boolean.parseBoolean(colors[2]);

        Div card = new Div();
        card.addClassName("stat-card");
        card.getStyle()
                .set("background", bgColor)
                .set("border", "1px solid " + borderColor)
                .set("border-left", "8px solid " + borderColor) // Borda esquerda mais grossa
                .set("border-radius", "20px") // Menos arredondado para evitar corte do label
                .set("height", "130px")
                .set("width", "100%") // Dinâmico! Responde à grade
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "center")
                .set("position", "relative")
                .set("overflow", "hidden")
                .set("box-sizing", "border-box")
                .set("padding-left", "20px") // Padding ajustado
                .set("padding-right", "60px") // MUITO IMPORTANTE: Evita que os textos batam no ícone
                .set("box-shadow", "var(--lumo-box-shadow-m)");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-label");
        labelSpan.getStyle()
                .set("margin-bottom", "2px")
                .set("font-size", "0.75rem")
                .set("line-height", "1.2")
                .set("z-index", "10")
                .set("position", "relative");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("stat-value");
        valueSpan.getStyle()
                .set("font-size", "1.6rem") // Responsivo (menor que o 2.4rem estourado)
                .set("font-weight", "bold")
                .set("z-index", "10")
                .set("position", "relative");

        Div iconWrapper = new Div(icon.create());
        iconWrapper.addClassName("stat-icon");
        iconWrapper.getStyle()
                .set("z-index", "10")
                .set("position", "absolute")
                .set("right", "20px")
                .set("top", "20px")
                .set("font-size", "1.8rem");

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (isDark) {
            labelSpan.getStyle().set("color", "rgba(255, 255, 255, 0.7)");
            valueSpan.getStyle().set("color", "#ffffff");
            iconWrapper.getStyle().set("color", "#ffffff");
        } else {
            // Forçar texto escuro — no dark mode do Vaadin, --dash-text é branco
            // mas o fundo do card é pastel claro, tornando o texto ilegível.
            labelSpan.getStyle().set("color", "rgba(0, 0, 0, 0.6)");
            valueSpan.getStyle().set("color", "#1e293b");
            iconWrapper.getStyle().set("color", borderColor);
        }

        Div sparkline = new Div();
        sparkline.setId(sparklineId);
        sparkline.addClassName("sparkline-container");
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (isDark) sparkline.getStyle().set("opacity", "0.3");

        card.add(labelSpan, valueSpan, iconWrapper, sparkline);
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return card;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void renderCharts(DashboardService.DashboardData data) {
        /*
         * Inicia um bloco try-catch para evitar que erros internos quebrem a renderização da interface.
         */
        try {
            String rankingSeries = objectMapper.writeValueAsString(data.userRanking().stream().map(d -> d.value()).toList());
            String rankingLabels = objectMapper.writeValueAsString(data.userRanking().stream().map(d -> d.label()).toList());
            
            String finalizerSeries = objectMapper.writeValueAsString(data.finalizerRanking().stream().map(d -> d.value()).toList());
            String finalizerLabels = objectMapper.writeValueAsString(data.finalizerRanking().stream().map(d -> d.label()).toList());
            
            String mixedTotal = objectMapper.writeValueAsString(data.history().stream().map(d -> d.total()).toList());
            String mixedOnTime = objectMapper.writeValueAsString(data.history().stream().map(d -> d.onTime()).toList());
            String mixedDelayed = objectMapper.writeValueAsString(data.history().stream().map(d -> d.delayed()).toList());
            String mixedPct = objectMapper.writeValueAsString(data.history().stream().map(d -> d.percentage()).toList());
            String mixedLabels = objectMapper.writeValueAsString(data.history().stream().map(d -> d.date()).toList());

            // Processar TMA
            List<String> tmaCategories = data.tmaPerStep().stream().map(d -> d.category()).toList();
            List<Double> tmaTotals = data.tmaPerStep().stream().map(d -> d.totalAvgSeconds()).toList();

            Map<String, Map<String, Double>> detailedTmaMap = new HashMap<>();
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (DashboardService.StackedBarData d : data.tmaPerStep()) {
                detailedTmaMap.put(d.category(), d.seriesValues());
            }

            String stackedSeriesJson = objectMapper.writeValueAsString(tmaTotals);
            String stackedCategoriesJson = objectMapper.writeValueAsString(tmaCategories);
            String detailedTmaJson = objectMapper.writeValueAsString(detailedTmaMap);

            String js = String.format("""
                (function() {
                    const tryRenderAll = () => {
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (typeof ApexCharts === 'undefined') {
                            setTimeout(tryRenderAll, 100);
                            return;
                        }
                        
                        const isDark = () => (document.documentElement.getAttribute('theme') || '').includes('dark');
                        const tc = '#1e293b';

                        const commonOptions = {
                            chart: {
                                fontFamily: 'Inter, sans-serif',
                                toolbar: { show: false },
                                background: 'transparent',
                                foreColor: tc
                            }
                        };

                        const sparkOptions = {
                            chart: { type: 'area', height: 60, sparkline: { enabled: true }, animations: { enabled: true }, background: 'transparent' },
                            stroke: { curve: 'smooth', width: 2 },
                            fill: { opacity: 0.3, type: 'gradient', gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.1 } },
                            tooltip: { enabled: false }
                        };

                        const totalData = %s;
                        const onTimeData = %s;
                        const delayedData = %s;
                        const pctData = %s;
                        
                        
                        const renderSpark = (id, data, color) => {
                            const el = document.querySelector("#" + id);
                            /*
                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                             */
                            if (el) {
                                el.innerHTML = "";
                                new ApexCharts(el, { ...sparkOptions, series: [{ data }], colors: [color] }).render();
                            }
                        };

                        renderSpark('spark-active', totalData, '#3b82f6');
                        renderSpark('spark-total', totalData, '#8b5cf6');
                        renderSpark('spark-ontime', onTimeData, '#10b981');
                        renderSpark('spark-delayed', delayedData, '#ef4444');
                        renderSpark('spark-pct', pctData, '#10b981');
                        renderSpark('spark-tma', pctData, '#f59e0b');

                        const userRankingOptions = {
                            ...commonOptions,
                            series: [{ name: 'Tarefas Criadas', data: %s }],
                            chart: { 
                                ...commonOptions.chart, 
                                type: 'bar', 
                                height: '100%%',
                                events: {
                                    click: function(event, chartContext, config) {
                                        /*
                                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                         */
                                        if (config && config.dataPointIndex !== undefined && config.dataPointIndex !== -1) {
                                            let category;
                                            const cats = chartContext.w.config.xaxis.categories || chartContext.w.globals.labels;
                                            /*
                                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                             */
                                            if (cats && cats[config.dataPointIndex]) category = cats[config.dataPointIndex];
                                            /*
                                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                             */
                                            if (category) {
                                                const rootEl = document.getElementById('dashboard-home');
                                                /*
                                                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                                 */
                                                if (rootEl) {
                                                    rootEl.dispatchEvent(new CustomEvent('creator-clicked', { 
                                                        detail: { category: String(category) }, bubbles: true, composed: true 
                                                    }));
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            plotOptions: { bar: { horizontal: true, borderRadius: 6, barHeight: '50%%', distributed: true } },
                            colors: ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#6366f1', '#14b8a6', '#f43f5e', '#ec4899', '#84cc16'],
                            xaxis: { categories: %s },
                            yaxis: { },
                            legend: { show: false },
                            dataLabels: { enabled: true }
                        };

                        const finalizerRankingOptions = {
                            ...commonOptions,
                            series: [{ name: 'Conclusões', data: %s }],
                            chart: { 
                                ...commonOptions.chart, 
                                type: 'bar', 
                                height: '100%%',
                                events: {
                                    click: function(event, chartContext, config) {
                                        /*
                                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                         */
                                        if (config && config.dataPointIndex !== undefined && config.dataPointIndex !== -1) {
                                            let category;
                                            const cats = chartContext.w.config.xaxis.categories || chartContext.w.globals.labels;
                                            /*
                                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                             */
                                            if (cats && cats[config.dataPointIndex]) category = cats[config.dataPointIndex];
                                            /*
                                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                             */
                                            if (category) {
                                                const rootEl = document.getElementById('dashboard-home');
                                                /*
                                                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                                 */
                                                if (rootEl) {
                                                    rootEl.dispatchEvent(new CustomEvent('finalizer-clicked', { 
                                                        detail: { category: String(category) }, bubbles: true, composed: true 
                                                    }));
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            plotOptions: { bar: { horizontal: true, borderRadius: 6, barHeight: '50%%', distributed: true } },
                            colors: ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#6366f1', '#14b8a6', '#f43f5e', '#ec4899', '#84cc16'],
                            xaxis: { categories: %s },
                            yaxis: { },
                            legend: { show: false },
                            dataLabels: { enabled: true }
                        };

                        const detailedTma = %s;
                        const tmaOptions = {
                                ...commonOptions,
                                series: [{ name: 'TMA Total', data: %s }],
                                chart: { 
                                    ...commonOptions.chart, 
                                    type: 'bar', 
                                    height: '100%%',
                                    events: {
                                        click: function(event, chartContext, config) {
                                            /*
                                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                             */
                                            if (config && config.dataPointIndex !== undefined && config.dataPointIndex !== -1) {
                                                let category;
                                                const cats = chartContext.w.config.xaxis.categories || chartContext.w.globals.labels;
                                                /*
                                                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                                 */
                                                if (cats && cats[config.dataPointIndex]) category = cats[config.dataPointIndex];
                                                /*
                                                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                                 */
                                                if (category) {
                                                    const rootEl = document.getElementById('dashboard-home');
                                                    /*
                                                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                                     */
                                                    if (rootEl) {
                                                        rootEl.dispatchEvent(new CustomEvent('tma-clicked', { 
                                                            detail: { category: String(category) }, bubbles: true, composed: true 
                                                        }));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                plotOptions: { bar: { horizontal: false, borderRadius: 4, columnWidth: '40%%' } },
                                colors: ['#3b82f6'],
                                xaxis: { categories: %s },
                                yaxis: { 
                                    labels: { 
                                        formatter: function(val) {
                                            const h = Math.floor(val / 3600);
                                            const m = Math.floor((val %% 3600) / 60);
                                            /*
                                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                             */
                                            if (h > 0) return h + "h " + m + "m";
                                            /*
                                             * Retorna o componente renderizado ou o valor da operação atual.
                                             */
                                            return m + "m";
                                        }
                                    } 
                                },
                                tooltip: {
                                    custom: function({series, seriesIndex, dataPointIndex, w}) {
                                        const category = w.globals.labels[dataPointIndex];
                                        const totalVal = series[seriesIndex][dataPointIndex];
                                        const formatTime = (val) => {
                                            if (!val || val === 0) return "0s";
                                            if (val < 60) return val.toFixed(1) + "s";
                                            const totalSecs = Math.round(val);
                                            const h = Math.floor(totalSecs / 3600);
                                            const m = Math.floor((totalSecs %% 3600) / 60);
                                            const s = totalSecs %% 60;
                                            if (h > 0) return h + "h " + m + "m " + s + "s";
                                            return m + "m " + s + "s";
                                        };
                                        const bgColor = '#ffffff';
                                        const textColor = '#1e293b';
                                        const borderColor = '#e2e8f0';
                                        let tHTML = '<div style="padding: 12px; background: ' + bgColor + '; color: ' + textColor + '; min-width: 220px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); border-radius: 6px; border: 1px solid ' + borderColor + ';">';
                                        tHTML += '<div style="font-weight: bold; font-size: 14px; margin-bottom: 8px; border-bottom: 1px solid ' + borderColor + '; padding-bottom: 6px;">' + category + '</div>';
                                        tHTML += '<div style="margin-bottom: 10px; font-weight: 600; color: #3b82f6;">Tempo Total: ' + formatTime(totalVal) + '</div>';
                                        /*
                                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                         */
                                        if (detailedTma && detailedTma[category]) {
                                            /*
                                             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
                                             */
                                            for (const [stepName, stepVal] of Object.entries(detailedTma[category])) {
                                                tHTML += '<div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px;">';
                                                tHTML += '<span style="opacity: 0.8; max-width: 150px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">- ' + stepName + '</span>';
                                                tHTML += '<span style="font-weight: 500;">' + formatTime(stepVal) + '</span>';
                                                tHTML += '</div>';
                                            }
                                        }
                                        tHTML += '</div>';
                                        /*
                                         * Retorna o componente renderizado ou o valor da operação atual.
                                         */
                                        return tHTML;
                                    }
                                },
                                dataLabels: { enabled: true, formatter: (val) => { 
                                    if (!val || val === 0) return "0s";
                                    const totalSecs = Math.round(val * 10) / 10;
                                    const h = Math.floor(totalSecs / 3600);
                                    const m = Math.floor((totalSecs %% 3600) / 60);
                                    const s = (totalSecs %% 60).toFixed(h > 0 || m > 0 ? 0 : 1);
                                    if (h > 0) return h + "h " + m + "m " + s + "s";
                                    if (m > 0) return m + "m " + s + "s";
                                    return s + "s";
                                }, style: { colors: [tc], fontSize: '10px' } },
                                legend: { show: false }
                        };

                        const mixedChartOptions = {
                                ...commonOptions,
                                series: [
                                    { name: 'Total de Tarefas', type: 'column', data: %s },
                                    { name: 'No Prazo', type: 'column', data: %s },
                                    { name: '%% No Prazo', type: 'line', data: %s }
                                ],
                                chart: { ...commonOptions.chart, type: 'line', height: '100%%' },
                                stroke: { width: [0, 0, 4], curve: 'smooth' },
                                plotOptions: { bar: { columnWidth: '50%%', borderRadius: 4 } },
                                colors: ['#3b82f6', '#10b981', '#f59e0b'],
                                xaxis: { categories: %s },
                                yaxis: [
                                    { seriesName: 'Total de Tarefas', labels: { formatter: (v) => Math.floor(v) } },
                                    { seriesName: 'Total de Tarefas', show: false },
                                    { opposite: true, seriesName: '%% No Prazo', labels: { formatter: (v) => v.toFixed(0) + '%%' }, min: 0, max: 100 }
                                ],
                                dataLabels: { enabled: true, enabledOnSeries: [2], formatter: (v) => v.toFixed(0) + "%%", style: { colors: [tc] }, background: { enabled: true, foreColor: '#fff' } },
                                tooltip: {
                                    y: {
                                        formatter: function(val, { seriesIndex }) {
                                            if (seriesIndex === 2) return val.toFixed(0) + "%%";
                                            return val;
                                        }
                                    }
                                },
                                legend: { position: 'bottom' }
                        };

                        // Renderização
                        const userRankingEl = document.querySelector("#user-ranking-chart");
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (userRankingEl) { userRankingEl.innerHTML = ""; new ApexCharts(userRankingEl, userRankingOptions).render(); }
                        
                        const finalizerRankingEl = document.querySelector("#finalizer-ranking-chart");
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (finalizerRankingEl) { finalizerRankingEl.innerHTML = ""; new ApexCharts(finalizerRankingEl, finalizerRankingOptions).render(); }
                        
                        const tmaEl = document.querySelector("#tma-stacked-chart");
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (tmaEl) { tmaEl.innerHTML = ""; new ApexCharts(tmaEl, tmaOptions).render(); }
                        
                        const mixedEl = document.querySelector("#mixed-history-chart");
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (mixedEl) { mixedEl.innerHTML = ""; new ApexCharts(mixedEl, mixedChartOptions).render(); }
                    };

                    tryRenderAll();
                    window.removeEventListener('theme-changed', tryRenderAll);
                    window.addEventListener('theme-changed', tryRenderAll);
                })();
                """, mixedTotal, mixedOnTime, mixedDelayed, mixedPct, rankingSeries, rankingLabels, finalizerSeries, finalizerLabels, detailedTmaJson, stackedSeriesJson, stackedCategoriesJson, mixedTotal, mixedOnTime, mixedPct, mixedLabels);

            // ====== SLA INSIGHTS CHARTS ======
            // Performance por SLA (barras agrupadas)
            List<String> slaNames = data.slaInsights().stream().map(d -> d.slaName()).toList();
            List<Long> slaOnTimeData = data.slaInsights().stream().map(d -> d.onTime()).toList();
            List<Long> slaDelayedData = data.slaInsights().stream().map(d -> d.delayed()).toList();
            
            String slaNamesJson = objectMapper.writeValueAsString(slaNames);
            String slaOnTimeJson = objectMapper.writeValueAsString(slaOnTimeData);
            String slaDelayedJson = objectMapper.writeValueAsString(slaDelayedData);

            // Justificativas (donut)
            List<String> justLabels = data.justificationRanking().stream().map(d -> d.justification()).toList();
            List<Long> justValues = data.justificationRanking().stream().map(d -> d.count()).toList();
            
            String justLabelsJson = objectMapper.writeValueAsString(justLabels);
            String justValuesJson = objectMapper.writeValueAsString(justValues);

            String slaJs = String.format("""
                (function() {
                    const tryRender = () => {
                        if (typeof ApexCharts === 'undefined') {
                            setTimeout(tryRender, 100);
                            return;
                        }
                        
                        const isDark = () => (document.documentElement.getAttribute('theme') || '').includes('dark');
                        const tc = '#1e293b';

                        // Performance por SLA
                        const slaPerformanceEl = document.querySelector("#sla-performance-chart");
                        if (slaPerformanceEl) {
                            slaPerformanceEl.innerHTML = "";
                            const slaNames = %s;
                            const slaOnTime = %s;
                            const slaDelayed = %s;
                            
                            if (slaNames.length > 0) {
                                new ApexCharts(slaPerformanceEl, {
                                    chart: {
                                        type: 'bar',
                                        height: '100%%',
                                        stacked: true,
                                        toolbar: { show: false },
                                        foreColor: tc,
                                        events: {
                                            click: function(event, chartContext, config) {
                                                if (config && config.dataPointIndex !== undefined && config.dataPointIndex !== -1) {
                                                    const category = chartContext.w.config.xaxis.categories[config.dataPointIndex];
                                                    if (category) {
                                                        const rootEl = document.getElementById('dashboard-home');
                                                        if (rootEl) {
                                                            rootEl.dispatchEvent(new CustomEvent('sla-clicked', { 
                                                                detail: { category: String(category) }, bubbles: true, composed: true 
                                                            }));
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    series: [
                                        { name: 'No Prazo', data: slaOnTime },
                                        { name: 'Atrasado', data: slaDelayed }
                                    ],
                                    colors: ['#10b981', '#ef4444'],
                                    plotOptions: {
                                        bar: { horizontal: false, borderRadius: 4, columnWidth: '50%%' }
                                    },
                                    xaxis: {
                                        categories: slaNames,
                                        labels: { trim: true, maxHeight: 80 }
                                    },
                                    yaxis: {
                                    },
                                    legend: {
                                        position: 'top'
                                    },
                                    dataLabels: { 
                                        enabled: true,
                                        formatter: function(val, opts) {
                                            const total = slaOnTime[opts.dataPointIndex] + slaDelayed[opts.dataPointIndex];
                                            const pct = total > 0 ? ((val / total) * 100).toFixed(1) : 0;
                                            return val + ' (' + pct + '%%)';
                                        }
                                    },
                                    tooltip: {
                                        y: {
                                            formatter: function(val, opts) {
                                                const total = slaOnTime[opts.dataPointIndex] + slaDelayed[opts.dataPointIndex];
                                                const pct = total > 0 ? ((val / total) * 100).toFixed(1) : 0;
                                                return val + ' (' + pct + '%%)';
                                            }
                                        }
                                    }
                                }).render();
                            } else {
                                slaPerformanceEl.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%%;color:var(--lumo-secondary-text-color);font-size:14px;">Nenhum dado de SLA disponível</div>';
                            }
                        }

                        // Donut de Justificativas
                        const justDonutEl = document.querySelector("#justification-donut-chart");
                        if (justDonutEl) {
                            justDonutEl.innerHTML = "";
                            const justLabels = %s;
                            const justValues = %s;
                            
                            if (justLabels.length > 0) {
                                new ApexCharts(justDonutEl, {
                                    chart: {
                                        type: 'donut',
                                        height: '100%%',
                                        toolbar: { show: false },
                                        foreColor: tc,
                                        events: {
                                            dataPointSelection: function(event, chartContext, config) {
                                                const category = chartContext.w.config.labels[config.dataPointIndex];
                                                if (category) {
                                                    const rootEl = document.getElementById('dashboard-home');
                                                    if (rootEl) {
                                                        rootEl.dispatchEvent(new CustomEvent('justification-clicked', { 
                                                            detail: { category: String(category) }, bubbles: true, composed: true 
                                                        }));
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    series: justValues,
                                    labels: justLabels,
                                    colors: ['#ef4444', '#f59e0b', '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6', '#6366f1', '#84cc16', '#f43f5e', '#0ea5e9'],
                                    plotOptions: {
                                        pie: {
                                            donut: {
                                                size: '55%%',
                                                labels: {
                                                    show: true,
                                                    total: {
                                                        show: true,
                                                        label: 'Total',
                                                        color: tc
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    legend: {
                                        position: 'bottom'
                                    },
                                    dataLabels: {
                                        enabled: true,
                                        formatter: function(val, opts) {
                                            const raw = opts.w.globals.series[opts.seriesIndex];
                                            return raw + ' (' + val.toFixed(0) + '%%)';
                                        }
                                    }
                                }).render();
                            } else {
                                justDonutEl.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%%;color:var(--lumo-secondary-text-color);font-size:14px;">Nenhuma justificativa registrada</div>';
                            }
                        }

                        // Nuvem de Palavras
                        const wordCloudEl = document.querySelector("#word-cloud-container");
                        if (wordCloudEl) {
                            wordCloudEl.innerHTML = "";
                            const words = %s; // [{justification: '...', count: 12}]
                            if (words.length > 0) {
                                const maxCount = Math.max(...words.map(w => w.count));
                                words.forEach(w => {
                                    const span = document.createElement('span');
                                    span.innerText = w.justification;
                                    const size = 12 + (w.count / maxCount) * 14;
                                    const opacity = 0.6 + (w.count / maxCount) * 0.4;
                                    span.style.fontSize = size + 'px';
                                    span.style.opacity = opacity;
                                    span.style.color = '#3b82f6';
                                    span.style.fontWeight = w.count > maxCount * 0.5 ? 'bold' : 'normal';
                                    span.style.padding = '2px 4px';
                                    span.style.wordBreak = 'break-word';
                                    span.style.overflowWrap = 'break-word';
                                    wordCloudEl.appendChild(span);
                                });
                            } else {
                                wordCloudEl.innerHTML = '<div style="color:var(--lumo-secondary-text-color);font-size:12px;">Sem dados</div>';
                            }
                        }

                        // Comentários Recentes
                        const commentsEl = document.querySelector("#recent-comments-container");
                        if (commentsEl) {
                            commentsEl.innerHTML = "";
                            const comments = %s;
                            if (comments.length > 0) {
                                comments.forEach(c => {
                                    const div = document.createElement('div');
                                    div.style.marginBottom = '6px';
                                    div.style.paddingLeft = '8px';
                                    div.style.borderLeft = '2px solid #3b82f6';
                                    div.style.fontStyle = 'italic';
                                    div.style.wordBreak = 'break-word';
                                    div.style.overflowWrap = 'break-word';
                                    div.innerText = c;
                                    commentsEl.appendChild(div);
                                });
                            } else {
                                commentsEl.innerHTML = '<div style="color:var(--lumo-secondary-text-color);font-size:12px;">Nenhum comentário textual</div>';
                            }
                        }
                    };
                    tryRender();
                    window.addEventListener('theme-changed', tryRender);
                })();
                """, slaNamesJson, slaOnTimeJson, slaDelayedJson, justLabelsJson, justValuesJson, objectMapper.writeValueAsString(data.wordCloud()), objectMapper.writeValueAsString(data.recentComments()));

            UI.getCurrent().getPage().executeJs(js);
            UI.getCurrent().getPage().executeJs(slaJs);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
