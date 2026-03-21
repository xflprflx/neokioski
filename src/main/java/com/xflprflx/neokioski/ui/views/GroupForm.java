package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.User;

import java.util.List;

public class GroupForm extends FormLayout {
    TextField name = new TextField("Nome");
    Checkbox active = new Checkbox("Ativo");
    MultiSelectComboBox<User> users = new MultiSelectComboBox<>("Usuários");

    Button save = new Button("Salvar");
    Button delete = new Button("Excluir");
    Button close = new Button("Cancelar");

    Binder<Group> binder = new BeanValidationBinder<>(Group.class);

    public GroupForm() {
        addClassName("group-form");
        binder.bindInstanceFields(this);

        users.setItemLabelGenerator(User::getName);

        add(name, active, users, createButtonsLayout());
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

    public void setGroup(Group group) {
        binder.setBean(group);
    }

    public void setAvailableUsers(List<User> userList) {
        users.setItems(userList);
    }

    // Events
    public static abstract class GroupFormEvent extends ComponentEvent<GroupForm> {
        private Group group;

        protected GroupFormEvent(GroupForm source, Group group) {
            super(source, false);
            this.group = group;
        }

        public Group getGroup() {
            return group;
        }
    }

    public static class SaveEvent extends GroupFormEvent {
        SaveEvent(GroupForm source, Group group) {
            super(source, group);
        }
    }

    public static class DeleteEvent extends GroupFormEvent {
        DeleteEvent(GroupForm source, Group group) {
            super(source, group);
        }
    }

    public static class CloseEvent extends GroupFormEvent {
        CloseEvent(GroupForm source) {
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
