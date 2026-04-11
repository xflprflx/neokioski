package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import com.xflprflx.neokioski.models.StoreReportConfig;

public class StoreReportConfigForm extends FormLayout {
    TextField identifier = new TextField("ID / Identificador");
    TextField revision = new TextField("Revisão");
    TextField title = new TextField("Título");
    TextField owner = new TextField("Responsável / Owner");
    TextField templateCode = new TextField("Código do Template");
    DatePicker startDate = new DatePicker("Data de Início");
    DatePicker endDate = new DatePicker("Data de Fim");

    Button save = new Button("Salvar");
    Button delete = new Button("Excluir");
    Button close = new Button("Cancelar");

    Binder<StoreReportConfig> binder = new BeanValidationBinder<>(StoreReportConfig.class);

    public StoreReportConfigForm() {
        addClassName("store-report-config-form");

        binder.bindInstanceFields(this);

        add(identifier, revision, title, owner, templateCode, startDate, endDate, createButtonsLayout());
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

    private void validateAndSave() {
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, binder.getBean()));
        }
    }

    public void setConfig(StoreReportConfig config) {
        binder.setBean(config);
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
