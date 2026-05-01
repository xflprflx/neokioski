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
import com.xflprflx.neokioski.ui.utils.TaskUIUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formulário da interface para a configuração de regras e parâmetros dos relatórios específicos das lojas.
 */
public class StoreReportConfigForm extends VerticalLayout {
    TextField identifier = new TextField("ID / Identificador");
    TextField revision = new TextField("Revisão");
    TextField title = new TextField("Título");
    TextField owner = new TextField("Responsável / Owner");
    TextField templateCode = new TextField("Código do Template");
    DatePicker startDate = new DatePicker("Data de Início");
    DatePicker endDate = new DatePicker("Data de Fim");
    
    {
        startDate.setLocale(Locale.forLanguageTag("pt-BR"));
        startDate.setI18n(TaskUIUtils.getPtBrI18n());
        endDate.setLocale(Locale.forLanguageTag("pt-BR"));
        endDate.setI18n(TaskUIUtils.getPtBrI18n());
    }
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

        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(headerFields, 
            new H4("Colunas do Relatório"), 
            createAddColumnLayout(), 
            columnsGrid,
            createButtonsLayout());
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return removeBtn;
        }).setHeader("Ações").setFlexGrow(0).setWidth("80px");
        columnsGrid.setAllRowsVisible(true);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component createAddColumnLayout() {
        TextField labelField = new TextField("Rótulo");
        ComboBox<String> keyField = new ComboBox<>("Atributo");
        keyField.setItems(taskService.getAvailableReportAttributes());
        keyField.setAllowCustomValue(true);
        keyField.setPlaceholder("Selecione ou digite...");

        Button addBtn = new Button("Adicionar Coluna", VaadinIcon.PLUS.create());
        addBtn.addClickListener(e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!labelField.getValue().isBlank() && keyField.getValue() != null && !keyField.getValue().isBlank()) {
                columnsList.add(new ReportColumnConfig(keyField.getValue(), labelField.getValue()));
                columnsGrid.setItems(columnsList);
                labelField.clear();
                keyField.clear();
            }
        });

        HorizontalLayout layout = new HorizontalLayout(labelField, keyField, addBtn);
        layout.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return layout;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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

        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return new HorizontalLayout(save, delete, close);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void setConfig(StoreReportConfig config) {
        binder.setBean(config);
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (config != null) {
            columnsList = new ArrayList<>(config.getColumns());
            columnsGrid.setItems(columnsList);
        }
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void validateAndSave() {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        /**
         * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
         */
        public StoreReportConfig getConfig() {
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return addListener(DeleteEvent.class, listener);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return addListener(SaveEvent.class, listener);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return addListener(CloseEvent.class, listener);
    }
}
