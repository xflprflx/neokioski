package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.enums.Role;

/**
 * Formulário visual utilizado para cadastro e edição dos usuários do sistema e suas permissões (Roles).
 */
public class UserForm extends FormLayout {
    TextField name = new TextField("Nome");
    TextField username = new TextField("Username");
    PasswordField password = new PasswordField("Senha");
    ComboBox<Role> role = new ComboBox<>("Papel");
    Checkbox isActive = new Checkbox("Ativo");
    Checkbox notifyStoreOrder = new Checkbox("Notificar pedido loja?");
    Checkbox notifyChatSounds = new Checkbox("Habilitar sons de chat?");

    Button save = new Button("Salvar");
    Button delete = new Button("Excluir");
    Button resetPassword = new Button("Gerar Senha");
    Button close = new Button("Cancelar");

    Binder<User> binder = new BeanValidationBinder<>(User.class);

    public UserForm() {
        addClassName("user-form");
        
        binder.forField(name).asRequired().bind(User::getName, User::setName);
        binder.forField(username).asRequired().bind(User::getUsername, User::setUsername);
        binder.forField(role).asRequired().bind(User::getRole, User::setRole);
        binder.forField(isActive).bind(User::isActive, User::setActive);
        binder.forField(notifyStoreOrder).bind(User::isNotifyStoreOrder, User::setNotifyStoreOrder);
        binder.forField(notifyChatSounds).bind(User::isNotifyChatSounds, User::setNotifyChatSounds);
        
        // Senha: Bind manual para não carregar a hash do banco, mas salvar o que for digitado
        binder.forField(password)
            .withValidator(pass -> {
                // Se for novo usuário, a senha é obrigatória
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (binder.getBean() != null && binder.getBean().getId() == null) {
                    /*
                     * Retorna o componente renderizado ou o valor da operação atual.
                     */
                    return pass != null && !pass.isEmpty();
                }
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
                return true;
            }, "A senha é obrigatória para novos usuários")
            .withValidator(pass -> {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (pass == null || pass.isEmpty()) return true;
                // Critérios de senha forte: min 6, [A-Z], [a-z], [0-9], [@$!%*?&]
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
                return pass.length() >= 6 && 
                       pass.matches(".*[A-Z].*") && 
                       pass.matches(".*[a-z].*") && 
                       pass.matches(".*\\d.*") && 
                       pass.matches(".*[@$!%*?&].*");
            }, "A senha deve conter ao menos 6 caracteres, incluindo maiúsculas, minúsculas, números e símbolos (@$!%*?&)")
            .bind(user -> "", (user, pass) -> {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (pass != null && !pass.isEmpty()) {
                    user.setPassword(pass);
                } else if (user.getId() != null) {
                    // Para usuários existentes, se estiver em branco, garantimos que seja null
                    // para o UserService saber que não deve alterar
                    user.setPassword(null);
                }
            });

        role.setItems(Role.values());
        role.setItemLabelGenerator(Role::getDescription);

        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(name, username, password, role, isActive, notifyStoreOrder, notifyChatSounds, createButtonsLayout());
    }

    /**
     * Métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, binder.getBean())));
        resetPassword.addClickListener(event -> fireEvent(new ResetPasswordEvent(this, binder.getBean())));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        binder.addStatusChangeListener(e -> save.setEnabled(binder.isValid()));

        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return new HorizontalLayout(save, delete, resetPassword, close);
    }

    /**
     * Métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void validateAndSave() {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (binder.isValid()) {
            fireEvent(new SaveEvent(this, binder.getBean()));
        }
    }

    /**
     * Métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void setUser(User user) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (user != null && user.getId() != null) {
            // Limpamos a senha para que o service saiba que, se continuar nulo/vazio, 
            // deve manter a senha original do banco
            user.setPassword(null);
        }
        binder.setBean(user);
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (user != null && user.getId() != null) {
            password.setPlaceholder("Deixe em branco para manter");
            resetPassword.setVisible(true);
        } else {
            password.setPlaceholder("");
            resetPassword.setVisible(false);
        }
    }

    // Events
    public static abstract class UserFormEvent extends ComponentEvent<UserForm> {
        private User user;

        protected UserFormEvent(UserForm source, User user) {
            super(source, false);
            this.user = user;
        }

        /**
         * Métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
         */
        public User getUser() {
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return user;
        }
    }

    public static class SaveEvent extends UserFormEvent {
        SaveEvent(UserForm source, User user) {
            super(source, user);
        }
    }

    public static class DeleteEvent extends UserFormEvent {
        DeleteEvent(UserForm source, User user) {
            super(source, user);
        }
    }

    public static class CloseEvent extends UserFormEvent {
        CloseEvent(UserForm source) {
            super(source, null);
        }
    }

    public static class ResetPasswordEvent extends UserFormEvent {
        ResetPasswordEvent(UserForm source, User user) {
            super(source, user);
        }
    }

    /**
     * Métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return addListener(DeleteEvent.class, listener);
    }

    /**
     * Métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return addListener(SaveEvent.class, listener);
    }

    /**
     * Métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public Registration addResetPasswordListener(ComponentEventListener<ResetPasswordEvent> listener) {
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return addListener(ResetPasswordEvent.class, listener);
    }

    /**
     * Métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public Registration addCloseListener(ComponentEventListener<CloseEvent> listener) {
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return addListener(CloseEvent.class, listener);
    }
}
