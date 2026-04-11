package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.xflprflx.neokioski.models.StoreReportConfig;
import com.xflprflx.neokioski.services.StoreReportConfigService;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("ADMIN")
@Route(value = "modelos-relatorio", layout = MainLayout.class)
@PageTitle("Modelos de Relatório | NeoKioski")
public class StoreReportConfigListView extends VerticalLayout {
    Grid<StoreReportConfig> grid = new Grid<>(StoreReportConfig.class);
    StoreReportConfigForm form;
    StoreReportConfigService service;

    public StoreReportConfigListView(StoreReportConfigService service) {
        this.service = service;
        addClassName("store-report-config-list-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }

    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void configureForm() {
        form = new StoreReportConfigForm();
        form.setWidth("25em");
        form.addSaveListener(this::saveConfig);
        form.addDeleteListener(this::deleteConfig);
        form.addCloseListener(e -> closeEditor());
    }

    private void saveConfig(StoreReportConfigForm.SaveEvent event) {
        try {
            service.save(event.getConfig());
            updateList();
            closeEditor();
        } catch (RuntimeException e) {
            Notification notification = Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteConfig(StoreReportConfigForm.DeleteEvent event) {
        service.delete(event.getConfig());
        updateList();
        closeEditor();
    }

    private void configureGrid() {
        grid.addClassNames("config-grid");
        grid.setSizeFull();
        grid.setColumns("identifier", "revision", "title", "owner", "templateCode", "startDate", "endDate");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.asSingleSelect().addValueChangeListener(event ->
                editConfig(event.getValue()));
    }

    private HorizontalLayout getToolbar() {
        Button addConfigButton = new Button("Novo Modelo");
        addConfigButton.addClickListener(click -> addConfig());

        var toolbar = new HorizontalLayout(addConfigButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    public void editConfig(StoreReportConfig config) {
        if (config == null) {
            closeEditor();
        } else {
            form.setConfig(config);
            form.setVisible(true);
            addClassName("editing");
        }
    }

    private void closeEditor() {
        form.setConfig(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    private void addConfig() {
        grid.asSingleSelect().clear();
        editConfig(new StoreReportConfig());
    }

    private void updateList() {
        grid.setItems(service.findAll());
    }
}
