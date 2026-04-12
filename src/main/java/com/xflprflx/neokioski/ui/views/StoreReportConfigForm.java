package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import com.xflprflx.neokioski.models.ReportColumnConfig;
import com.xflprflx.neokioski.models.StoreReportConfig;
import com.xflprflx.neokioski.models.enums.ReportOrientation;
import com.xflprflx.neokioski.services.TaskService;

import java.util.ArrayList;
import java.util.List;

public class StoreReportConfigForm extends VerticalLayout {
    TextField identifier = new TextField("ID / Identificador");
    TextField revision = new TextField("Revisão");
    TextField title = new TextField("Título");
    TextField owner = new TextField("Responsável / Owner");
    TextField templateCode = new TextField("Código do Template");
    DatePicker startDate = new DatePicker("Data de Início");
    DatePicker endDate = new DatePicker("Data de Fim");
    Select<ReportOrientation> orientation = new Select<>("Orientação");
    
    Grid<ReportColumnConfig> columnsGrid = new Grid<>(ReportColumnConfig.class, false);
    List<ReportColumnConfig> columnsList = new ArrayList<>();

    Button save = new Button("Salvar");
    Button delete = new Button("Excluir");
    Button close = new Button("Cancelar");

    Binder<StoreReportConfig> binder = new BeanValidationBinder<>(StoreReportConfig.class);
    private final TaskService taskService;

    public StoreReportConfigForm(TaskService taskService) {
        this.taskService = taskService;
        addClassName("store-report-config-form");
        setPadding(true);
        setSpacing(true);

        orientation.setItems(ReportOrientation.values());
        orientation.setValue(ReportOrientation.PORTRAIT);

        binder.bindInstanceFields(this);

        FormLayout headerFields = new FormLayout(identifier, revision, title, owner, templateCode, startDate, endDate, orientation);
        headerFields.setWidthFull();

        configureColumnsGrid();

        add(headerFields, 
            new H4("Colunas do Relatório"), 
            createAddColumnLayout(), 
            columnsGrid,
            createButtonsLayout());
    }

    private void configureColumnsGrid() {
        columnsGrid.addColumn(ReportColumnConfig::getLabel).setHeader("Rótulo (Label)");
        columnsGrid.addColumn(ReportColumnConfig::getAttributeKey).setHeader("Atributo (Key)");
        columnsGrid.addComponentColumn(col -> {
            Button removeBtn = new Button(VaadinIcon.TRASH.create());
            removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            removeBtn.addClickListener(e -> {
                columnsList.remove(col);
                columnsGrid.setItems(columnsList);
            });
            return removeBtn;
        }).setHeader("Ações").setFlexGrow(0).setWidth("80px");
        columnsGrid.setAllRowsVisible(true);
    }

    private Component createAddColumnLayout() {
        TextField labelField = new TextField("Rótulo");
        ComboBox<String> keyField = new ComboBox<>("Atributo");
        keyField.setItems(taskService.getAvailableReportAttributes());
        keyField.setAllowCustomValue(true);
        keyField.setPlaceholder("Selecione ou digite...");

        Button addBtn = new Button("Adicionar Coluna", VaadinIcon.PLUS.create());
        addBtn.addClickListener(e -> {
            if (!labelField.getValue().isBlank() && keyField.getValue() != null && !keyField.getValue().isBlank()) {
                columnsList.add(new ReportColumnConfig(keyField.getValue(), labelField.getValue()));
                columnsGrid.setItems(columnsList);
                labelField.clear();
                keyField.clear();
            }
        });

        HorizontalLayout layout = new HorizontalLayout(labelField, keyField, addBtn);
        layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);
        return layout;
    }

    private Component createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, binder.getBean())));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));

        return new HorizontalLayout(save, delete, close);
    }

    public void setConfig(StoreReportConfig config) {
        binder.setBean(config);
        if (config != null) {
            columnsList = new ArrayList<>(config.getColumns());
            columnsGrid.setItems(columnsList);
        }
    }

    private void validateAndSave() {
        if (binder.isValid()) {
            StoreReportConfig config = binder.getBean();
            config.setColumns(new ArrayList<>(columnsList));
            fireEvent(new SaveEvent(this, config));
        }
    }

    // Events
    public static abstract class StoreReportConfigFormEvent extends ComponentEvent<StoreReportConfigForm> {
        private StoreReportConfig config;

        protected StoreReportConfigFormEvent(StoreReportConfigForm source, StoreReportConfig config) {
            super(source, false);
            this.config = config;
        }

        public StoreReportConfig getConfig() {
            return config;
        }
    }

    public static class SaveEvent extends StoreReportConfigFormEvent {
        SaveEvent(StoreReportConfigForm source, StoreReportConfig config) {
            super(source, config);
        }
    }

    public static class DeleteEvent extends StoreReportConfigFormEvent {
        DeleteEvent(StoreReportConfigForm source, StoreReportConfig config) {
            super(source, config);
        }
    }

    public static class CloseEvent extends StoreReportConfigFormEvent {
        CloseEvent(StoreReportConfigForm source) {
            super(source, null);
        }
    }

    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        return addListener(DeleteEvent.class, listener);
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
        return addListener(CloseEvent.class, listener);
    }
}
