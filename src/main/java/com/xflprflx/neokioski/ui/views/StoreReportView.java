package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.xflprflx.neokioski.services.TaskService;
import jakarta.annotation.security.PermitAll;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@PermitAll
@Route(value = "relatorio-loja", layout = MainLayout.class)
@PageTitle("Relatório de Loja | NeoKioski")
public class StoreReportView extends VerticalLayout {

    private final TaskService taskService;
    private final Grid<TaskService.StoreReportEntry> grid = new Grid<>(TaskService.StoreReportEntry.class, false);
    private final DatePicker startDatePicker = new DatePicker("Data Inicial");
    private final DatePicker endDatePicker = new DatePicker("Data Final");

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public StoreReportView(TaskService taskService) {
        this.taskService = taskService;

        setSizeFull();
        setPadding(true);

        H2 title = new H2("Relatório de Loja");
        
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        startDatePicker.addValueChangeListener(e -> updateGrid());
        endDatePicker.addValueChangeListener(e -> updateGrid());

        HorizontalLayout filters = new HorizontalLayout(startDatePicker, endDatePicker);
        filters.setAlignItems(Alignment.BASELINE);

        configureGrid();

        add(title, filters, grid);
        setFlexGrow(1, grid);

        updateGrid();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.addColumn(TaskService.StoreReportEntry::templateName).setHeader("Tarefa").setSortable(true);
        grid.addColumn(entry -> formatter.format(entry.createdAt())).setHeader("Criação").setSortable(true);
        grid.addColumn(entry -> formatter.format(entry.collectedAt())).setHeader("Coleta").setSortable(true);
        grid.addColumn(TaskService.StoreReportEntry::creatorName).setHeader("Criador").setSortable(true);
        grid.addColumn(TaskService.StoreReportEntry::collectorName).setHeader("Finalizado por").setSortable(true);
        grid.addColumn(TaskService.StoreReportEntry::deliveryNumber).setHeader("Delivery").setSortable(true);
        
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private void updateGrid() {
        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) return;

        Instant start = startDatePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = endDatePicker.getValue().atTime(23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant();

        List<TaskService.StoreReportEntry> data = taskService.getStoreReportData(start, end);
        grid.setItems(data);
    }
}
