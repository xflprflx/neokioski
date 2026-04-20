package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.xflprflx.neokioski.models.StoreReportConfig;
import com.xflprflx.neokioski.services.StoreReportConfigService;
import com.xflprflx.neokioski.services.TaskService;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("ADMIN")
@Route(value = "modelos-relatorio", layout = MainLayout.class)
@PageTitle("Modelos de Relatório | NeoKioski")
/**
 * View que exibe as configurações de relatório das lojas cadastradas em formato de listagem (Grid).
 */
public class StoreReportConfigListView extends VerticalLayout {
    Grid<StoreReportConfig> grid = new Grid<>(StoreReportConfig.class);
    StoreReportConfigForm form;
    Dialog configDialog;
    StoreReportConfigService service;
    TaskService taskService;

    public StoreReportConfigListView(StoreReportConfigService service, TaskService taskService) {
        this.service = service;
        this.taskService = taskService;
        addClassName("store-report-config-list-view");
        setSizeFull();
        configureGrid();
        configureForm();

        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid);
        content.addClassNames("content");
        content.setSizeFull();
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return content;
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureForm() {
        form = new StoreReportConfigForm(taskService);
        form.addSaveListener(this::saveConfig);
        form.addDeleteListener(this::deleteConfig);
        form.addCloseListener(e -> closeEditor());

        configDialog = new Dialog();
        configDialog.setHeaderTitle("Modelo de Relatório");
        configDialog.add(form);
        configDialog.setWidth("800px");
        configDialog.setHeight("90%");
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void saveConfig(StoreReportConfigForm.SaveEvent event) {
        /*
         * Inicia um bloco try-catch para evitar que erros internos quebrem a renderização da interface.
         */
        try {
            service.save(event.getConfig());
            updateList();
            closeEditor();
        } catch (RuntimeException e) {
            Notification notification = Notification.show(e.getMessage(), 5000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void deleteConfig(StoreReportConfigForm.DeleteEvent event) {
        service.delete(event.getConfig());
        updateList();
        closeEditor();
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureGrid() {
        grid.addClassNames("config-grid");
        grid.setSizeFull();
        grid.setColumns("identifier", "revision", "title", "owner", "templateCode", "startDate", "endDate");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.asSingleSelect().addValueChangeListener(event ->
                editConfig(event.getValue()));
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private HorizontalLayout getToolbar() {
        Button addConfigButton = new Button("Novo Modelo");
        addConfigButton.addClickListener(click -> addConfig());

        var toolbar = new HorizontalLayout(addConfigButton);
        toolbar.addClassName("toolbar");
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return toolbar;
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void editConfig(StoreReportConfig config) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (config == null) {
            closeEditor();
        } else {
            form.setConfig(config);
            configDialog.open();
            addClassName("editing");
        }
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void closeEditor() {
        form.setConfig(null);
        configDialog.close();
        removeClassName("editing");
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void addConfig() {
        grid.asSingleSelect().clear();
        editConfig(new StoreReportConfig());
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void updateList() {
        grid.setItems(service.findAll());
    }
}
