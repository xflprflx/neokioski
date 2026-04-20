package com.xflprflx.neokioski.ui.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    private Div kpiGrid;
    private Div rankingHeaderDiv;
    private Div finalizerRankingHeaderDiv;
    private Div userRankingChartDiv;
    private Div finalizerRankingChartDiv;
    private Div tmaStackedChartDiv;
    private Div mixedChartDiv;
    
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

        setSizeFull();
        addClassName("dashboard-view-container");
        setId("dashboard-home");
        getStyle()
            .set("padding", "32px")
            .set("box-sizing", "border-box");

        createHeaderSection();
        createFilterSection();
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
                    String clickedName = categoryNode.isTextual() ? categoryNode.textValue() : categoryNode.toString().replace("\"", "");
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
                    String templateStr = categoryNode.isString() ? categoryNode.stringValue() : categoryNode.toString().replace("\"", "");
                    
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
                    String clickedName = categoryNode.isString() ? categoryNode.stringValue() : categoryNode.toString().replace("\"", "");
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

        updateDashboard();
    }

    @Override
    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        com.vaadin.flow.component.UI ui = attachEvent.getUI();
        taskListener = task -> ui.access(this::updateDashboard);
        taskService.registerGlobal(taskListener);
    }

    @Override
    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
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
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void createHeaderSection() {
        User user = securityService.getAuthenticatedInternalUser().orElse(null);
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (user == null) return;

        Div welcome = new Div();
        welcome.addClassNames("welcome-section", "dashboard-header", "animate-in");
        welcome.getStyle().set("animation-delay", "0.1s");
        
        Div titleRow = new Div();
        titleRow.getStyle().set("display", "flex").set("justify-content", "space-between").set("align-items", "center").set("width", "100%");
        
        H1 title = new H1("Olá, " + user.getName() + " 👋");
        title.getStyle().set("margin", "0").set("font-size", "2rem").set("font-weight", "800");
        
        Button themeToggle = new Button(VaadinIcon.ADJUST.create(), e -> {
            UI.getCurrent().getElement().executeJs(
                "const theme = document.documentElement.getAttribute('theme') === 'dark' ? 'light' : 'dark';" +
                "document.documentElement.setAttribute('theme', theme);" +
                "localStorage.setItem('dashboard-theme', theme);" +
                "window.dispatchEvent(new Event('theme-changed'));"
            );
        });
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.getStyle().set("margin-left", "auto").set("cursor", "pointer");
        
        titleRow.add(title, themeToggle);
        
        Span subtitle = new Span("Painel de Visibilidade Operacional");
        subtitle.getStyle().set("color", "var(--dash-text-sec)").set("font-weight", "500");
        
        welcome.add(titleRow, subtitle);
        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(welcome);
    }
    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void createFilterSection() {
        Div filters = new Div();
        filters.addClassNames("filter-section", "dashboard-filters", "animate-in");
        filters.getStyle()
                .set("animation-delay", "0.2s")
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "16px")
                .set("align-items", "center")
                .set("margin-bottom", "24px")
                .set("width", "100%");

        startDatePicker = new DatePicker("De");
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        startDatePicker.setLocale(Locale.forLanguageTag("pt-BR"));

        endDatePicker = new DatePicker("Até");
        endDatePicker.setValue(LocalDate.now());
        endDatePicker.setLocale(Locale.forLanguageTag("pt-BR"));

        groupFilter = new ComboBox<>("Grupo");
        groupFilter.setItems(groupService.findAll());
        groupFilter.setItemLabelGenerator(Group::getName);
        groupFilter.setPlaceholder("Todos os grupos");
        groupFilter.setClearButtonVisible(true);

        templateFilter = new ComboBox<>("Tipo de Tarefa");
        templateFilter.setItems(templateService.findAll());
        templateFilter.setItemLabelGenerator(TaskTemplate::getName);
        templateFilter.setPlaceholder("Todos os tipos");
        templateFilter.setClearButtonVisible(true);

        startDatePicker.addValueChangeListener(e -> updateDashboard());
        endDatePicker.addValueChangeListener(e -> updateDashboard());
        groupFilter.addValueChangeListener(e -> updateDashboard());
        templateFilter.addValueChangeListener(e -> updateDashboard());

        creatorFilter = new ComboBox<>("Criador");
        creatorFilter.setItems(userService.findAll());
        creatorFilter.setItemLabelGenerator(User::getName);
        creatorFilter.setPlaceholder("Todos os criadores");
        creatorFilter.setClearButtonVisible(true);
        creatorFilter.addValueChangeListener(e -> updateDashboard());

        finalizerFilter = new ComboBox<>("Concluidor (Delivery)");
        finalizerFilter.setItems(userService.findAll());
        finalizerFilter.setItemLabelGenerator(User::getName);
        finalizerFilter.setPlaceholder("Todos os concluidores");
        finalizerFilter.setClearButtonVisible(true);
        finalizerFilter.addValueChangeListener(e -> updateDashboard());

        filters.add(startDatePicker, endDatePicker, groupFilter, templateFilter, creatorFilter, finalizerFilter);
        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(filters);
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
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
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void createChartsSection() {
        Div chartsContainer = new Div();
        chartsContainer.addClassNames("charts-container", "animate-in");
        chartsContainer.getStyle()
                .set("display", "flex")
                .set("gap", "24px")
                .set("margin-top", "32px")
                .set("width", "100%")
                .set("flex-wrap", "wrap"); 

        // Coluna 1: Ranking Criadores
        Div rankingCol = createChartCard("30%");
        userRankingChartDiv = new Div();
        userRankingChartDiv.setId("user-ranking-chart");
        userRankingChartDiv.getStyle().set("flex-grow", "1");
        rankingHeaderDiv = createChartHeader("Ranking Criadores de Tarefas");
        rankingCol.add(rankingHeaderDiv, userRankingChartDiv);

        // Coluna 2: Ranking Concluidores
        Div finalizerCol = createChartCard("30%");
        finalizerRankingChartDiv = new Div();
        finalizerRankingChartDiv.setId("finalizer-ranking-chart");
        finalizerRankingChartDiv.getStyle().set("flex-grow", "1");
        finalizerRankingHeaderDiv = createChartHeader("Ranking Concluidores (Delivery)");
        finalizerCol.add(finalizerRankingHeaderDiv, finalizerRankingChartDiv);

        // Coluna 3: TMA
        Div tmaCol = createChartCard("30%");
        tmaStackedChartDiv = new Div();
        tmaStackedChartDiv.setId("tma-stacked-chart");
        tmaStackedChartDiv.getStyle().set("flex-grow", "1");
        tmaCol.add(createChartHeader("TMA Médio Total por Tipo"), tmaStackedChartDiv);

        chartsContainer.add(rankingCol, finalizerCol, tmaCol);
        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(chartsContainer);

        Div bottomChartContainer = new Div();
        bottomChartContainer.addClassNames("charts-container", "animate-in");
        bottomChartContainer.getStyle()
                .set("display", "flex")
                .set("margin-top", "24px")
                .set("width", "100%");

        Div mixedCol = new Div();
        mixedCol.addClassNames("chart-card");
        mixedCol.getStyle()
                .set("animation-delay", "0.6s")
                .set("flex", "1 1 100%")
                .set("background", "var(--dash-card-bg, #ffffff)")
                .set("border-radius", "16px")
                .set("padding", "24px")
                .set("box-shadow", "0 4px 6px -1px rgba(0,0,0,0.1)")
                .set("border", "1px solid var(--dash-border, #e2e8f0)")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("height", "350px");

        mixedChartDiv = new Div();
        mixedChartDiv.setId("mixed-history-chart");
        mixedChartDiv.getStyle().set("flex-grow", "1");
        
        mixedCol.add(createChartHeader("Evolução Diária - Tarefas e SLA"), mixedChartDiv);

        bottomChartContainer.add(mixedCol);
        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(bottomChartContainer);
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Div createChartCard(String flexBasis) {
        Div col = new Div();
        col.addClassNames("chart-card", "animate-in");
        col.getStyle()
                .set("flex", "1 1 " + flexBasis)
                .set("background", "var(--dash-card-bg, #ffffff)")
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
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Div createChartHeader(String title) {
        Div header = new Div();
        header.addClassName("chart-title");
        header.setText(title);
        header.getStyle().set("margin-bottom", "-20px").set("z-index", "10").set("position", "relative");
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return header;
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
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

        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, group, type, activeCreatorFilter, activeFinalizerFilter, null);
        this.lastDashboardData = data;

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
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
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
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void addKpiToGrid(String label, String value, VaadinIcon icon, String theme, String sparkId, int index) {
        Div cardWrapper = createStatCard(label, value, icon, theme, sparkId);
        cardWrapper.addClassName("animate-in");
        cardWrapper.getStyle().set("animation-delay", (0.35 + (index * 0.05)) + "s");
        kpiGrid.add(cardWrapper);
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
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
            labelSpan.getStyle().set("color", "rgba(0, 0, 0, 0.6)");
            valueSpan.getStyle().set("color", "var(--dash-text)");
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
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
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
                        
                        const isDark = () => document.documentElement.getAttribute('theme') === 'dark';
                        const getThemeColor = () => isDark() ? '#f8fafc' : '#1e293b';
                        const getChartTheme = () => isDark() ? 'dark' : 'light';

                        const commonOptions = {
                            chart: {
                                fontFamily: 'Inter, sans-serif',
                                toolbar: { show: false },
                                background: 'transparent'
                            },
                            theme: { mode: getChartTheme() }
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
                        
                        const theme = getChartTheme();
                        
                        const renderSpark = (id, data, color) => {
                            const el = document.querySelector("#" + id);
                            /*
                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                             */
                            if (el) {
                                el.innerHTML = "";
                                new ApexCharts(el, { ...sparkOptions, series: [{ data }], colors: [color], theme: { mode: theme } }).render();
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
                            xaxis: { categories: %s, labels: { style: { colors: getThemeColor() } } },
                            yaxis: { labels: { style: { colors: getThemeColor() } } },
                            legend: { show: false },
                            dataLabels: { enabled: true },
                            theme: { mode: theme }
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
                            xaxis: { categories: %s, labels: { style: { colors: getThemeColor() } } },
                            yaxis: { labels: { style: { colors: getThemeColor() } } },
                            legend: { show: false },
                            dataLabels: { enabled: true },
                            theme: { mode: theme }
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
                                xaxis: { categories: %s, labels: { style: { colors: getThemeColor() } } },
                                yaxis: { 
                                    labels: { 
                                        style: { colors: getThemeColor() },
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
                                            /*
                                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                             */
                                            if (!val || val === 0) return "0s";
                                            const h = Math.floor(val / 3600);
                                            const m = Math.floor((val %% 3600) / 60);
                                            const s = Math.floor(val %% 60);
                                            /*
                                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                             */
                                            if (h > 0) return h + "h " + m + "m " + s + "s";
                                            /*
                                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                             */
                                            if (m > 0) return m + "m " + s + "s";
                                            /*
                                             * Retorna o componente renderizado ou o valor da operação atual.
                                             */
                                            return s + "s";
                                        };
                                        const isD = isDark();
                                        const bgColor = isD ? '#1e293b' : '#ffffff';
                                        const textColor = isD ? '#f8fafc' : '#1e293b';
                                        const borderColor = isD ? '#334155' : '#e2e8f0';
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
                                    const h = Math.floor(val / 3600);
                                    const m = Math.floor((val %% 3600) / 60);
                                    /*
                                     * Retorna o componente renderizado ou o valor da operação atual.
                                     */
                                    return h > 0 ? h + "h " + m + "m" : m + "m";
                                }, style: { colors: [isDark() ? '#fff' : '#000'] } },
                                legend: { show: false },
                                theme: { mode: theme }
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
                                xaxis: { categories: %s, labels: { style: { colors: getThemeColor() } } },
                                yaxis: [
                                    { seriesName: 'Total de Tarefas', labels: { style: { colors: getThemeColor() } } },
                                    { seriesName: 'Total de Tarefas', show: false },
                                    { opposite: true, seriesName: '%% No Prazo', labels: { style: { colors: getThemeColor() }, formatter: (v) => v.toFixed(1) + '%%' }, min: 0, max: 100 }
                                ],
                                dataLabels: { enabled: true, enabledOnSeries: [2], formatter: (v) => v.toFixed(1) + "%%", style: { colors: [getThemeColor()] }, background: { enabled: true, foreColor: isDark() ? '#000' : '#fff' } },
                                legend: { position: 'bottom', labels: { colors: getThemeColor() } }
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

            UI.getCurrent().getPage().executeJs(js);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
