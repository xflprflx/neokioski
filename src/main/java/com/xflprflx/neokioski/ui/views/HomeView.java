package com.xflprflx.neokioski.ui.views;
// v2 - forced refresh for i18n

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
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
import com.xflprflx.neokioski.ui.components.TaskDetailsDialog;
import com.xflprflx.neokioski.ui.utils.TaskUIUtils;
import com.xflprflx.neokioski.models.Task;
import jakarta.annotation.security.PermitAll;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
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
    private TextField keywordFilter; // Mantido para estado, mas fora do layout
    private String activeKeyword;

    private Div kpiGrid;
    private HorizontalLayout rankingHeaderDiv;
    private HorizontalLayout finalizerRankingHeaderDiv;
    private Div userRankingChartDiv;
    private Div finalizerRankingChartDiv;
    private Div tmaStackedChartDiv;
    private Div mixedChartDiv;
    private Div slaPerformanceChartDiv;
    private Div justificationDonutChartDiv;
    private Div wordCloudDiv;
    private Div recentCommentsDiv;
    
    private Button clearSlaHeaderBtn;
    private Button clearJustHeaderBtn;
    private Button floatingClearAllBtn;
    private Button clearFiltersTopBtn;
    
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
                        if (clickedSla.equals(slaFilter.getValue())) {
                            slaFilter.clear();
                        } else {
                            slaFilter.setValue(clickedSla);
                        }
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
                        if (clickedJust.equals(justificationFilter.getValue())) {
                            justificationFilter.clear();
                        } else {
                            justificationFilter.setValue(clickedJust);
                        }
                    }
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }).addEventData("event.detail.category");

        getElement().addEventListener("keyword-clicked", e -> {
            try {
                var keywordNode = e.getEventData().get("event.detail.keyword");
                if (keywordNode != null && !keywordNode.isNull()) {
                    String keyword = keywordNode.toString().replaceAll("^\"|\"$", "");
                    if (keyword != null) {
                        if (keyword.equalsIgnoreCase(activeKeyword)) {
                            keywordFilter.clear(); // O listener já chamará updateDashboard()
                        } else {
                            keywordFilter.setValue(keyword); // O listener já chamará updateDashboard()
                        }
                    }
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }).addEventData("event.detail.keyword");

        getElement().addEventListener("comment-task-clicked", e -> {
            try {
                var taskIdNode = e.getEventData().get("event.detail.taskId");
                if (taskIdNode != null && !taskIdNode.isNull()) {
                    long taskId = taskIdNode.asLong();
                    taskService.findById(taskId).ifPresent(task -> {
                        getUI().ifPresent(ui -> ui.access(() -> {
                            new TaskDetailsDialog(task).open();
                        }));
                    });
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }).addEventData("event.detail.taskId");

        createFloatingActionButton();
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

    private void createFloatingActionButton() {
        floatingClearAllBtn = new Button("Limpar Tudo", VaadinIcon.ERASER.create(), e -> clearAllFilters());
        floatingClearAllBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        floatingClearAllBtn.setId("fab-clear-all");
        floatingClearAllBtn.getStyle()
                .set("position", "fixed")
                .set("bottom", "40px")
                .set("right", "40px")
                .set("z-index", "1000")
                .set("box-shadow", "0 10px 15px -3px rgba(0, 0, 0, 0.3)")
                .set("border-radius", "30px")
                .set("padding", "0 25px")
                .set("height", "50px")
                .set("cursor", "move")
                .set("transition", "transform 0.2s ease, box-shadow 0.2s ease");
        
        floatingClearAllBtn.getElement().addEventListener("mouseenter", e -> floatingClearAllBtn.getStyle().set("transform", "scale(1.05)"));
        floatingClearAllBtn.getElement().addEventListener("mouseleave", e -> floatingClearAllBtn.getStyle().set("transform", "scale(1)"));
        
        // JS para tornar o FAB arrastável
        floatingClearAllBtn.getElement().executeJs(
            "const el = this;" +
            "let isDragging = false;" +
            "let startX, startY, startRight, startBottom;" +
            "let moved = false;" +
            
            "el.addEventListener('mousedown', e => {" +
            "  isDragging = true;" +
            "  moved = false;" +
            "  startX = e.clientX;" +
            "  startY = e.clientY;" +
            "  const style = window.getComputedStyle(el);" +
            "  startRight = parseInt(style.right);" +
            "  startBottom = parseInt(style.bottom);" +
            "  el.style.transition = 'none';" + // Desativa transição durante o drag
            "  e.preventDefault();" +
            "});" +
            
            "document.addEventListener('mousemove', e => {" +
            "  if (!isDragging) return;" +
            "  const dx = e.clientX - startX;" +
            "  const dy = e.clientY - startY;" +
            "  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) moved = true;" +
            "  el.style.right = (startRight - dx) + 'px';" +
            "  el.style.bottom = (startBottom - dy) + 'px';" +
            "});" +
            
            "document.addEventListener('mouseup', e => {" +
            "  if (!isDragging) return;" +
            "  isDragging = false;" +
            "  el.style.transition = 'transform 0.2s ease, box-shadow 0.2s ease';" +
            "  if (moved) {" +
            "    el.addEventListener('click', e => e.stopPropagation(), { once: true, capture: true });" +
            "  }" +
            "});"
        );

        floatingClearAllBtn.setVisible(false);
        add(floatingClearAllBtn);
    }

    private void clearAllFilters() {
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
        groupFilter.clear();
        templateFilter.clear();
        creatorFilter.clear();
        finalizerFilter.clear();
        slaFilter.clear();
        justificationFilter.clear();
        activeKeyword = null;
        keywordFilter.clear();
        updateDashboard();
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

        FlexLayout filtersRow = new FlexLayout();
        filtersRow.setWidthFull();
        filtersRow.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        filtersRow.getStyle()
                .set("gap", "12px")
                .set("align-items", "flex-end");

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

        keywordFilter = new TextField("Palavra-Chave");
        keywordFilter.setPlaceholder("Busca na nuvem...");
        keywordFilter.setClearButtonVisible(true);
        keywordFilter.setWidth("180px");
        keywordFilter.setPrefixComponent(VaadinIcon.SEARCH.create());

        startDatePicker.addValueChangeListener(e -> updateDashboard());
        endDatePicker.addValueChangeListener(e -> updateDashboard());
        groupFilter.addValueChangeListener(e -> updateDashboard());
        templateFilter.addValueChangeListener(e -> updateDashboard());
        creatorFilter.addValueChangeListener(e -> updateDashboard());
        finalizerFilter.addValueChangeListener(e -> updateDashboard());
        slaFilter.addValueChangeListener(e -> updateDashboard());
        justificationFilter.addValueChangeListener(e -> updateDashboard());
        keywordFilter.addValueChangeListener(e -> {
            activeKeyword = (e.getValue() == null || e.getValue().isBlank()) ? null : e.getValue();
            updateDashboard();
        });

        clearFiltersTopBtn = new Button("Limpar Filtros", VaadinIcon.ERASER.create(), e -> clearAllFilters());
        clearFiltersTopBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        clearFiltersTopBtn.getStyle()
                .set("margin-top", "auto")
                .set("color", "var(--lumo-error-text-color)");
        clearFiltersTopBtn.setVisible(false);

        filtersRow.add(startDatePicker, endDatePicker, groupFilter, templateFilter, creatorFilter, finalizerFilter, slaFilter, justificationFilter, clearFiltersTopBtn);
        
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
                .set("grid-template-columns", "repeat(auto-fit, minmax(220px, 1fr))") // Redimensionamento fluido com mínimo seguro
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
        tmaStackedChartDiv.getStyle()
                .set("flex-grow", "1")
                .set("padding-top", "24px"); // Espaço para não disputar com o título do card
        tmaCol.add(createChartHeader("TMA por Tipo e Step"), tmaStackedChartDiv);
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
                .set("flex-wrap", "wrap"); // Permitir empilhamento em telas menores

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
        finalizerRankingHeaderDiv = createChartHeader("Ranking Concluidores");
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
        
        Button clearSlaBtn = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> { slaFilter.clear(); });
        clearSlaBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        clearSlaBtn.setTooltipText("Limpar filtro de SLA");
        clearSlaBtn.setVisible(false); // Visível via updateDashboard
        this.clearSlaHeaderBtn = clearSlaBtn;

        slaPerformanceCol.add(createChartHeader("Performance por SLA", clearSlaBtn), slaPerformanceChartDiv);
        slaPerformanceRow.add(slaPerformanceCol);
        add(slaPerformanceRow);

        // --- LINHA 6: Justificativas (Fim do Dashboard) ---
        Div justificationsRow = new Div();
        justificationsRow.addClassNames("charts-container", "animate-in");
        justificationsRow.getStyle()
                .set("margin-top", "24px")
                .set("width", "100%");

        Div justificationCol = createChartCard("100%");
        
        FlexLayout justificationContent = new FlexLayout();
        justificationContent.setWidthFull();
        justificationContent.setFlexWrap(FlexLayout.FlexWrap.NOWRAP); // Forçar lado a lado para manter altura fixa e scrolls isolados
        justificationContent.getStyle()
                .set("gap", "24px")
                .set("flex", "1")
                .set("min-height", "0")
                .set("padding-top", "25px"); 
        justificationContent.addClassName("custom-scrollbar");

        justificationDonutChartDiv = new Div();
        justificationDonutChartDiv.setId("justification-donut-chart");
        justificationDonutChartDiv.getStyle()
                .set("flex", "1 1 0") // Proporcional, mas permite encolher
                .set("min-height", "0")
                .set("padding", "0 10px");

        Div cloudCol = new Div();
        cloudCol.getStyle()
                .set("flex", "1 1 0")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("min-height", "0");
        Span cloudTitle = new Span("Nuvem de Justificativas");
        cloudTitle.getStyle().set("font-weight", "bold").set("font-size", "14px").set("margin-bottom", "8px").set("color", "var(--dash-text-sec)");
        wordCloudDiv = new Div();
        wordCloudDiv.setId("word-cloud-container");
        wordCloudDiv.getStyle()
                .set("flex", "1")
                .set("min-height", "0")
                .set("overflow", "hidden"); // Nuvem fixa, sem scroll próprio
        cloudCol.add(cloudTitle, wordCloudDiv);

        Div commentsCol = new Div();
        commentsCol.getStyle()
                .set("flex", "1.2 1 0") // Tabela tem prioridade leve de espaço
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("min-width", "0")
                .set("min-height", "0");
        Span commentsTitle = new Span("Comentários Recentes");
        commentsTitle.getStyle().set("font-weight", "bold").set("font-size", "14px").set("margin-bottom", "8px").set("color", "var(--dash-text-sec)");
        recentCommentsDiv = new Div();
        recentCommentsDiv.setId("recent-comments-container");
        recentCommentsDiv.getStyle()
                .set("flex", "1")
                .set("min-height", "0")
                .set("overflow-y", "auto");
        commentsCol.add(commentsTitle, recentCommentsDiv);

        justificationContent.add(justificationDonutChartDiv, cloudCol, commentsCol);
        
        Button clearJustBtn = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> { justificationFilter.clear(); activeKeyword = null; keywordFilter.clear(); });
        clearJustBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        clearJustBtn.setTooltipText("Limpar filtros de justificativa e nuvem");
        clearJustBtn.setVisible(false);
        this.clearJustHeaderBtn = clearJustBtn;

        justificationCol.add(createChartHeader("Justificativas de Atraso", clearJustBtn), justificationContent);
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
                .set("min-width", "350px") 
                .set("height", "400px")
                .set("overflow", "hidden"); // Garante que o conteúdo não escape dxs limites dx card
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return col;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private HorizontalLayout createChartHeader(String title, Component... extra) {
        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("chart-title-row");
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle()
                .set("margin-bottom", "-20px")
                .set("z-index", "10")
                .set("position", "relative");

        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("color", "#1e293b");

        header.add(titleSpan);
        if (extra.length > 0) {
            HorizontalLayout actionArea = new HorizontalLayout(extra);
            actionArea.setSpacing(false);
            header.add(actionArea);
        }

        return header;
    }

    private void updateHeaderText(HorizontalLayout header, String text) {
        if (header != null) {
            header.getChildren()
                .filter(c -> c instanceof Span)
                .findFirst()
                .ifPresent(c -> ((Span) c).setText(text));
        }
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

        DashboardService.DashboardData data = dashboardService.getDashboardData(start, end, group, type, activeCreatorFilter, activeFinalizerFilter, null, activeSlaFilter, activeJustFilter, keywordFilter.getValue());
        this.lastDashboardData = data;

        // Atualizar visibilidade dos botões de limpeza
        boolean hasFilter = group != null || type != null || creator != null || finalizer != null || activeSlaFilter != null || activeJustFilter != null || activeKeyword != null;
        if (clearFiltersTopBtn != null) clearFiltersTopBtn.setVisible(hasFilter);
        if (floatingClearAllBtn != null) floatingClearAllBtn.setVisible(hasFilter);
        if (clearSlaHeaderBtn != null) clearSlaHeaderBtn.setVisible(activeSlaFilter != null);
        if (clearJustHeaderBtn != null) clearJustHeaderBtn.setVisible(activeJustFilter != null || activeKeyword != null);

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
             updateHeaderText(rankingHeaderDiv, "Ranking (Filtro: " + activeCreatorFilter + ")");
        } else {
             updateHeaderText(rankingHeaderDiv, "Ranking Criadores de Tarefas");
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (activeFinalizerFilter != null) {
             updateHeaderText(finalizerRankingHeaderDiv, "Ranking (Filtro: " + activeFinalizerFilter + ")");
        } else {
             updateHeaderText(finalizerRankingHeaderDiv, "Ranking Concluidores");
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
                .set("padding-right", "20px") // Padding uniforme
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
                .set("z-index", "1")
                .set("position", "absolute")
                .set("right", "15px")
                .set("bottom", "-10px") // Move para o fundo como marca d'água
                .set("opacity", "0.15") // Marca d'água sutil para evitar conflito com texto
                .set("font-size", "4.5rem");

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

            // Processar TMA para Barras Empilhadas (Horizontal Stacked) - Ordenado por tempo total (maior para menor)
            List<DashboardService.StackedBarData> sortedTmaData = new java.util.ArrayList<>(data.tmaPerStep());
            sortedTmaData.sort(java.util.Comparator.comparingDouble(DashboardService.StackedBarData::totalAvgSeconds).reversed());
            
            List<String> tmaCategories = sortedTmaData.stream().map(d -> d.category()).toList();
            
            // Coletar todos os nomes únicos de etapas e mapear temas para cores
            Set<String> allSteps = new java.util.LinkedHashSet<>();
            for (DashboardService.StackedBarData d : sortedTmaData) {
                allSteps.addAll(d.seriesValues().keySet());
            }

            // Mapear temas das etapas para cores usando o mapa fornecido pelo serviço (seguro contra LazyInitializationException)
            Map<String, String> stepToTheme = data.stepThemes();

            List<String> tmaColorList = new java.util.ArrayList<>();
            List<Map<String, Object>> tmaSeriesList = new java.util.ArrayList<>();
            for (String step : allSteps) {
                // Resolver Cor
                String theme = stepToTheme.get(step);
                String color;
                if ("Aguardando".equals(step) || theme == null) {
                    color = "#b0bec5"; // Cor neutra para aguardando/sem tema
                } else if ("black".equalsIgnoreCase(theme)) {
                    color = "#212121";
                } else {
                    color = com.xflprflx.neokioski.ui.utils.TaskUIUtils.getThemeColors(theme)[1];
                }
                tmaColorList.add(color);

                Map<String, Object> series = new HashMap<>();
                series.put("name", step);
                List<Double> stepValues = new java.util.ArrayList<>();
                for (DashboardService.StackedBarData d : sortedTmaData) {
                    stepValues.add(d.seriesValues().getOrDefault(step, 0.0));
                }
                series.put("data", stepValues);
                tmaSeriesList.add(series);
            }

            String stackedSeriesJson = objectMapper.writeValueAsString(tmaSeriesList);
            String stackedCategoriesJson = objectMapper.writeValueAsString(tmaCategories);
            String stackedColorsJson = objectMapper.writeValueAsString(tmaColorList);

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

                        // Dados do TMA por Tipo — renderizados como charts individuais por tipo
                        const tmaAllSeries = %s;
                        const tmaAllColors = %s;
                        const tmaAllCategories = %s;

                        const formatTmaTime = (v) => {
                            const ts = Math.round(v * 10) / 10;
                            const h = Math.floor(ts / 3600);
                            const m = Math.floor((ts %% 3600) / 60);
                            const s = (ts %% 60).toFixed(h > 0 || m > 0 ? 0 : 1);
                            if (h > 0) return h + 'h ' + m + 'm ' + s + 's';
                            if (m > 0) return m + 'm ' + s + 's';
                            return s + 's';
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
                        if (tmaEl) {
                            tmaEl.innerHTML = '';
                            tmaEl.style.display = 'flex';
                            tmaEl.style.flexDirection = 'column';
                            tmaEl.style.gap = '8px';

                            tmaAllCategories.forEach((cat, catIdx) => {
                                const catSeries = [];
                                const catColors = [];
                                tmaAllSeries.forEach((s, sIdx) => {
                                    const val = s.data[catIdx];
                                    if (val && val > 0) {
                                        catSeries.push({ name: s.name, data: [val] });
                                        catColors.push(tmaAllColors[sIdx]);
                                    }
                                });
                                if (catSeries.length === 0) return;

                                const totalSecs = catSeries.reduce((sum, s) => sum + s.data[0], 0);

                                const wrapper = document.createElement('div');
                                wrapper.style.background = 'rgba(0,0,0,0.02)'; // Fundo sutil para agrupar o tipo
                                wrapper.style.borderRadius = '12px';
                                wrapper.style.padding = '16px';
                                wrapper.style.marginBottom = '20px';
                                wrapper.style.border = '1px solid rgba(0,0,0,0.05)'; // Borda fina para definição

                                const tmaSpanId = 'tma-total-' + catIdx;

                                const header = document.createElement('div');
                                header.style.display = 'flex';
                                header.style.alignItems = 'baseline';
                                header.style.gap = '12px';
                                header.style.padding = '0 8px';
                                header.style.cursor = 'pointer';
                                header.innerHTML = '<span style="font-weight:700;font-size:13px;color:' + tc + ';">' + cat + '</span>'
                                    + '<span id="' + tmaSpanId + '" style="font-size:10px;color:#3b82f6;font-weight:700;background:rgba(59,130,246,0.1);padding:2px 10px;border-radius:12px;text-transform:uppercase;">TMA: ' + formatTmaTime(totalSecs) + '</span>';
                                header.onclick = () => {
                                    const rootEl = document.getElementById('dashboard-home');
                                    if (rootEl) rootEl.dispatchEvent(new CustomEvent('tma-clicked', { detail: { category: String(cat) }, bubbles: true, composed: true }));
                                };
                                wrapper.appendChild(header);

                                const chartContainer = document.createElement('div');
                                wrapper.appendChild(chartContainer);
                                tmaEl.appendChild(wrapper);

                                const updateTmaHeader = (chartInstance) => {
                                    let visibleTotal = 0;
                                    const series = chartInstance.w.config.series;
                                    const collapsed = chartInstance.w.globals.collapsedSeriesIndices || [];
                                    series.forEach((s, i) => {
                                        if (collapsed.indexOf(i) === -1) {
                                            const v = s.data[0];
                                            if (v && !isNaN(v)) visibleTotal += v;
                                        }
                                    });
                                    const tmaSpan = document.getElementById(tmaSpanId);
                                    if (tmaSpan) tmaSpan.textContent = 'TMA: ' + formatTmaTime(visibleTotal);
                                };

                                new ApexCharts(chartContainer, {
                                    ...commonOptions,
                                    series: catSeries,
                                    chart: {
                                        ...commonOptions.chart,
                                        type: 'bar',
                                        stacked: true,
                                        height: 75 + Math.max(0, Math.ceil(catSeries.length / 4) - 1) * 22,
                                        events: {
                                            mounted: function(chart) { updateTmaHeader(chart); },
                                            legendClick: function(chart, sIdx, opts) { setTimeout(() => updateTmaHeader(chart), 100); },
                                            click: function(event, chartContext, config) {
                                                if (config && config.dataPointIndex !== undefined && config.dataPointIndex !== -1) {
                                                    const rootEl = document.getElementById('dashboard-home');
                                                    if (rootEl) rootEl.dispatchEvent(new CustomEvent('tma-clicked', { detail: { category: String(cat) }, bubbles: true, composed: true }));
                                                }
                                            }
                                        }
                                    },
                                    colors: catColors,
                                    plotOptions: { bar: { horizontal: true, borderRadius: 3, barHeight: '45%%' } },
                                    stroke: { width: 1, colors: ['#fff'] },
                                    grid: { padding: { left: 0, right: 10, top: -25, bottom: -8 } },
                                    xaxis: {
                                        categories: [''],
                                        labels: { show: false },
                                        axisBorder: { show: false },
                                        axisTicks: { show: false }
                                    },
                                    yaxis: { show: false },
                                    legend: {
                                        show: true,
                                        position: 'bottom',
                                        horizontalAlign: 'left',
                                        fontSize: '10px',
                                        markers: { radius: 12, width: 8, height: 8 },
                                        itemMargin: { horizontal: 6, vertical: 2 },
                                        formatter: function(seriesName, opts) {
                                            const val = opts.w.config.series[opts.seriesIndex].data[0];
                                            if (!val || isNaN(val)) return seriesName;
                                            return seriesName + ' (' + formatTmaTime(val) + ')';
                                        }
                                    },
                                    tooltip: {
                                        shared: true,
                                        intersect: false,
                                        custom: function({series, seriesIndex, dataPointIndex, w}) {
                                            let totalVal = 0;
                                            let itemsHtml = '';
                                            const collapsed = w.globals.collapsedSeriesIndices || [];
                                            w.config.series.forEach((s, idx) => {
                                                if (collapsed.indexOf(idx) !== -1) return;
                                                const val = s.data[0];
                                                if (val && val > 0 && !isNaN(val)) {
                                                    totalVal += val;
                                                    const color = w.config.colors[idx];
                                                    itemsHtml += '<div style="display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:3px;font-size:11px;">'
                                                        + '<div style="display:flex;align-items:center;gap:6px;"><span style="width:8px;height:8px;border-radius:50%%;background:' + color + ';"></span><span>' + s.name + '</span></div>'
                                                        + '<span style="font-weight:600;">' + formatTmaTime(val) + '</span></div>';
                                                }
                                            });
                                            return '<div style="padding:10px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;box-shadow:0 4px 6px rgba(0,0,0,.08);min-width:200px;color:#1e293b;">'
                                                + '<div style="font-weight:bold;font-size:13px;margin-bottom:3px;">' + cat + '</div>'
                                                + '<div style="font-size:12px;color:#3b82f6;font-weight:600;margin-bottom:8px;border-bottom:1px solid #f1f5f9;padding-bottom:6px;">TMA Total: ' + formatTmaTime(totalVal) + '</div>'
                                                + itemsHtml + '</div>';
                                        }
                                    },
                                    dataLabels: {
                                        enabled: true,
                                        formatter: function(val) {
                                            if (!val || val === 0 || isNaN(val)) return '';
                                            return formatTmaTime(val);
                                        },
                                        style: { fontSize: '9px' }
                                    }
                                }).render();
                            });
                        }
                        
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
                """, mixedTotal, mixedOnTime, mixedDelayed, mixedPct, rankingSeries, rankingLabels, finalizerSeries, finalizerLabels, stackedSeriesJson, stackedColorsJson, stackedCategoriesJson, mixedTotal, mixedOnTime, mixedPct, mixedLabels);

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

                        // Nuvem de Palavras (Versão Refinada com Gingado)
                        const wordCloudEl = document.querySelector("#word-cloud-container");
                        if (wordCloudEl) {
                            wordCloudEl.innerHTML = "";
                            const words = %s;
                            if (words.length > 0) {
                                wordCloudEl.style.display = 'flex';
                                wordCloudEl.style.flexWrap = 'wrap';
                                wordCloudEl.style.alignItems = 'baseline';
                                wordCloudEl.style.justifyContent = 'center';
                                wordCloudEl.style.gap = '10px 15px'; 
                                wordCloudEl.style.padding = '10px';
                                wordCloudEl.style.width = '100%%';
                                wordCloudEl.style.maxWidth = '100%%';
                                wordCloudEl.style.margin = '0';
                                wordCloudEl.style.overflow = 'hidden';
                                
                                const maxCount = Math.max(...words.map(w => w.count));
                                const palette = ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ef4444', '#06b6d4'];
                                
                                // Embaralhar as palavras para um look de nuvem mais natural
                                const shuffledWords = [...words].sort(() => Math.random() - 0.5);
                                
                                shuffledWords.forEach((w, i) => {
                                    const span = document.createElement('span');
                                    span.innerText = w.justification;
                                    
                                    // Escala de tamanho equilibrada (12px a 28px) para o novo layout
                                    const size = 12 + (w.count / maxCount) * 16;
                                    const opacity = 0.6 + (w.count / maxCount) * 0.4;
                                    const tilt = ((i * 13) %% 11) - 5; 
                                    const originalColor = palette[i %% palette.length];

                                    span.style.fontSize = size + 'px';
                                    span.style.opacity = opacity;
                                    span.style.color = originalColor;
                                    span.style.fontWeight = w.count > maxCount * 0.5 ? '700' : '500';
                                    
                                    const isActive = "%s" && w.justification.toLowerCase() === "%s".toLowerCase();
                                    if (isActive) {
                                        span.style.color = '#2563eb';
                                        span.style.fontWeight = '900';
                                        span.style.textDecoration = 'underline';
                                        span.style.background = 'rgba(37, 99, 235, 0.1)';
                                        span.style.padding = '2px 8px';
                                        span.style.borderRadius = '4px';
                                        span.style.transform = 'rotate(0deg) scale(1.1)';
                                    } else {
                                        span.style.transform = `rotate(${tilt}deg)`;
                                    }

                                    span.style.transition = 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)';
                                    span.style.display = 'inline-block';
                                    span.style.cursor = 'pointer';
                                    
                                    span.onmouseover = () => {
                                        span.style.transform = `scale(1.15) rotate(0deg)`;
                                        span.style.opacity = '1';
                                        span.style.color = '#2563eb';
                                        span.style.textShadow = '0 0 10px rgba(37,99,235,0.2)';
                                        span.style.zIndex = '100';
                                    };
                                    span.onmouseout = () => {
                                        if (isActive) {
                                            span.style.transform = 'rotate(0deg) scale(1.1)';
                                            span.style.opacity = '1';
                                            span.style.color = '#2563eb';
                                        } else {
                                            span.style.transform = `rotate(${tilt}deg)`;
                                            span.style.opacity = opacity;
                                            span.style.color = originalColor;
                                        }
                                        span.style.textShadow = 'none';
                                        span.style.zIndex = '1';
                                    };
                                    span.onclick = () => {
                                        const rootEl = document.getElementById('dashboard-home');
                                        if (rootEl) {
                                            rootEl.dispatchEvent(new CustomEvent('keyword-clicked', { 
                                                detail: { keyword: w.justification },
                                                bubbles: true, 
                                                composed: true 
                                            }));
                                        }
                                    };
                                    
                                    wordCloudEl.appendChild(span);
                                });
                            } else {
                                wordCloudEl.innerHTML = '<div style="color:var(--lumo-secondary-text-color);font-size:12px;padding:20px;">Sem dados</div>';
                            }
                        }

                        // Comentários Recentes (Tabela)
                        const commentsEl = document.querySelector("#recent-comments-container");
                        if (commentsEl) {
                            commentsEl.innerHTML = "";
                            const comments = %s; // [{taskName: '...', comment: '...'}]
                            if (comments.length > 0) {
                                const table = document.createElement('table');
                                table.style.width = '100%%';
                                table.style.borderCollapse = 'collapse';
                                table.style.fontSize = '11px';
                                
                                const thead = document.createElement('thead');
                                thead.innerHTML = `
                                    <tr style="border-bottom: 2px solid #e2e8f0; text-align: left; color: var(--dash-text-sec);">
                                        <th style="padding: 4px;">Comentário</th>
                                        <th style="padding: 4px;">Ação</th>
                                    </tr>
                                `;
                                table.appendChild(thead);
                                
                                const tbody = document.createElement('tbody');
                                comments.forEach(c => {
                                    const tr = document.createElement('tr');
                                    tr.style.borderBottom = '1px solid #f1f5f9';
                                    tr.innerHTML = `
                                        <td style="padding: 6px 4px; font-style: italic; color: #475569; word-break: break-word;">
                                            ${c.comment}
                                        </td>
                                        <td style="padding: 6px 4px; vertical-align: top; width: 100px;">
                                            <a href="javascript:void(0)" 
                                               style="font-weight: bold; color: #3b82f6; text-decoration: none; font-size: 10px; white-space: nowrap;"
                                               onmouseover="this.style.textDecoration='underline'"
                                               onmouseout="this.style.textDecoration='none'"
                                               onclick="document.getElementById('dashboard-home').dispatchEvent(new CustomEvent('comment-task-clicked', { detail: { taskId: ${c.taskId} }, bubbles: true, composed: true }))">
                                               Ver detalhes
                                            </a>
                                        </td>
                                    `;
                                    tbody.appendChild(tr);
                                });
                                table.appendChild(tbody);
                                commentsEl.appendChild(table);
                            } else {
                                commentsEl.innerHTML = '<div style="color:var(--lumo-secondary-text-color);font-size:12px;padding:10px;">Nenhum comentário textual</div>';
                            }
                        }
                    };
                    tryRender();
                    window.addEventListener('theme-changed', tryRender);
                })();
                """, slaNamesJson, slaOnTimeJson, slaDelayedJson, justLabelsJson, justValuesJson, objectMapper.writeValueAsString(data.wordCloud()), activeKeyword != null ? activeKeyword : "", activeKeyword != null ? activeKeyword : "", objectMapper.writeValueAsString(data.recentComments()));

            UI.getCurrent().getPage().executeJs(js);
            UI.getCurrent().getPage().executeJs(slaJs);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
