package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.services.*;
import com.xflprflx.neokioski.ui.components.TaskDetailsDialog;
import com.xflprflx.neokioski.ui.utils.TaskUIUtils;
import jakarta.annotation.security.RolesAllowed;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@PageTitle("Relatório Geral de Tarefas | NeoKioski")
@Route(value = "relatorio-geral", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class GeneralTaskReportView extends VerticalLayout {

    private final TaskService taskService;
    private final UserService userService;
    private final TaskTemplateService templateService;
    private final GroupService groupService;
    private final ExcelService excelService;
    private final PdfService pdfService;

    private DatePicker startDate;
    private DatePicker endDate;
    private ComboBox<User> creatorCombo;
    private ComboBox<TaskTemplate> templateCombo;
    private ComboBox<Group> groupCombo;
    private TextField searchField;
    private Grid<Task> grid;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public GeneralTaskReportView(TaskService taskService, UserService userService, 
                                 TaskTemplateService templateService, GroupService groupService,
                                 ExcelService excelService, PdfService pdfService) {
        this.taskService = taskService;
        this.userService = userService;
        this.templateService = templateService;
        this.groupService = groupService;
        this.excelService = excelService;
        this.pdfService = pdfService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(new H2("Relatório Geral de Tarefas"));

        createFilterLayout();
        createGridLayout();
        
        refreshGrid();
    }

    private void createFilterLayout() {
        HorizontalLayout filterRow1 = new HorizontalLayout();
        filterRow1.setWidthFull();
        filterRow1.setAlignItems(Alignment.END);

        startDate = new DatePicker("De");
        startDate.setValue(LocalDate.now());
        
        endDate = new DatePicker("Até");
        endDate.setValue(LocalDate.now());

        creatorCombo = new ComboBox<>("Criador");
        creatorCombo.setItems(userService.findAll());
        creatorCombo.setItemLabelGenerator(User::getName);
        creatorCombo.setClearButtonVisible(true);

        templateCombo = new ComboBox<>("Template");
        templateCombo.setItems(templateService.findAll());
        templateCombo.setItemLabelGenerator(TaskTemplate::getName);
        templateCombo.setClearButtonVisible(true);

        groupCombo = new ComboBox<>("Grupo");
        groupCombo.setItems(groupService.findAll());
        groupCombo.setItemLabelGenerator(Group::getName);
        groupCombo.setClearButtonVisible(true);

        filterRow1.add(startDate, endDate, creatorCombo, templateCombo, groupCombo);

        HorizontalLayout filterRow2 = new HorizontalLayout();
        filterRow2.setWidthFull();
        filterRow2.setAlignItems(Alignment.END);

        searchField = new TextField("Busca de valor (Campo/Resposta)");
        searchField.setPlaceholder("Digite algo para buscar...");
        searchField.setWidth("350px");
        searchField.setClearButtonVisible(true);

        Button filterBtn = new Button("Filtrar", VaadinIcon.FILTER.create(), e -> refreshGrid());
        filterBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button clearBtn = new Button("Limpar", e -> clearFilters());

        Button excelBtn = new Button("Baixar Excel", VaadinIcon.TABLE.create(), e -> downloadExcel());
        excelBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        Button pdfBtn = new Button("Baixar PDF", VaadinIcon.FILE_TEXT.create(), e -> downloadPdf());
        pdfBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

        filterRow2.add(searchField, filterBtn, clearBtn, excelBtn, pdfBtn);

        add(filterRow1, filterRow2);
    }

    private void clearFilters() {
        startDate.setValue(LocalDate.now());
        endDate.setValue(LocalDate.now());
        creatorCombo.clear();
        templateCombo.clear();
        groupCombo.clear();
        searchField.clear();
        refreshGrid();
    }

    private void createGridLayout() {
        grid = new Grid<>(Task.class, false);
        grid.setSizeFull();

        grid.addColumn(Task::getId).setHeader("ID").setAutoWidth(true).setSortable(true);
        
        grid.addColumn(t -> dateTimeFormatter.format(t.getCreatedAt()))
                .setHeader("Criada em")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(t -> t.getTaskTemplate() != null ? t.getTaskTemplate().getName() : "-")
                .setHeader("Template")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(t -> t.getGroup() != null ? t.getGroup().getName() : "-")
                .setHeader("Grupo")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(t -> t.getCreator() != null ? t.getCreator().getName() : "Sistema")
                .setHeader("Criador")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addComponentColumn(t -> {
            if (t.getCurrentStep() == null) return new Span("Pendente");
            Span badge = new Span(t.getCurrentStep().getLabel());
            TaskUIUtils.applyThemeStyles(badge, t.getCurrentStep().getColorTheme());
            badge.getStyle().set("padding", "2px 8px").set("border-radius", "8px").set("font-size", "12px");
            return badge;
        }).setHeader("Status").setSortable(true).setAutoWidth(true);

        grid.addComponentColumn(t -> {
            Button btn = new Button("Ver Detalhes", VaadinIcon.EYE.create(), e -> {
                new TaskDetailsDialog(t).open();
            });
            btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return btn;
        }).setHeader("Ações").setAutoWidth(true);

        add(grid);
    }

    private void refreshGrid() {
        grid.setItems(getFilteredTasks());
    }

    private List<Task> getFilteredTasks() {
        Instant start = startDate.getValue() != null ? 
                startDate.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant end = endDate.getValue() != null ? 
                endDate.getValue().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant() : null;

        return taskService.findTasksWithFilters(
                start, 
                end, 
                creatorCombo.getValue(), 
                templateCombo.getValue(), 
                groupCombo.getValue(), 
                searchField.getValue()
        );
    }

    private void downloadExcel() {
        List<Task> tasks = getFilteredTasks();
        if (tasks.isEmpty()) {
            Notification.show("Nenhum dado encontrado para exportação.");
            return;
        }
        StreamResource resource = new StreamResource("relatorio_tarefas.xlsx", () -> excelService.generateGeneralTaskReport(tasks));
        triggerDownload(resource);
    }

    private void downloadPdf() {
        List<Task> tasks = getFilteredTasks();
        if (tasks.isEmpty()) {
            Notification.show("Nenhum dado encontrado para exportação.");
            return;
        }
        StreamResource resource = new StreamResource("relatorio_tarefas.pdf", () -> pdfService.generateGeneralTaskReportPdf(tasks));
        triggerDownload(resource);
    }

    private void triggerDownload(StreamResource resource) {
        Anchor anchor = new Anchor(resource, "");
        anchor.getElement().setAttribute("download", true);
        anchor.getElement().getStyle().set("display", "none");
        add(anchor);
        anchor.getElement().executeJs("this.click(); setTimeout(() => this.remove(), 1000);");
    }
}
