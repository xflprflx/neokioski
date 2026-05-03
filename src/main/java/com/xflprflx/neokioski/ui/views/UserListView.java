package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.services.UserService;
import com.xflprflx.neokioski.services.PresenceService;
import com.xflprflx.neokioski.security.SecurityService;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("ADMIN")
@Route(value = "usuarios", layout = MainLayout.class)
@PageTitle("Usuários | NeoKioski")
/**
 * View de interface responsável por listar os usuários ativos do sistema com recursos de busca e ações de modificação.
 */
public class UserListView extends VerticalLayout {
    Grid<User> grid = new Grid<>(User.class);
    TextField filterText = new TextField();
    Checkbox showDeleted = new Checkbox("Mostrar excluídos");
    UserForm form;
    UserService service;
    PresenceService presenceService;
    SecurityService securityService;
    private PresenceService.PresenceEvent lastEvent;
    private java.util.function.Consumer<PresenceService.PresenceEvent> presenceListener;

    public UserListView(UserService service, PresenceService presenceService, SecurityService securityService) {
        this.service = service;
        this.presenceService = presenceService;
        this.securityService = securityService;
        addClassName("user-list-view");
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
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.addClassNames("content");
        content.setSizeFull();
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return content;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureForm() {
        form = new UserForm();
        form.setWidth("25em");
        form.addSaveListener(this::saveUser);
        form.addDeleteListener(this::deleteUser);
        form.addAnonymizeListener(this::anonymizeUser);
        form.addResetPasswordListener(this::resetPassword);
        form.addCloseListener(e -> closeEditor());
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void saveUser(UserForm.SaveEvent event) {
        service.save(event.getUser());
        updateList();
        closeEditor();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void deleteUser(UserForm.DeleteEvent event) {
        service.delete(event.getUser());
        updateList();
        closeEditor();
    }

    private void anonymizeUser(UserForm.AnonymizeEvent event) {
        service.anonymizeUser(event.getUser());
        updateList();
        closeEditor();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void resetPassword(UserForm.ResetPasswordEvent event) {
        String tempPass = service.generateTemporaryPassword(event.getUser().getId());
        
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Senha Temporária Gerada");
        
        Span text = new Span("A nova senha para o usuário ");
        Span username = new Span(event.getUser().getUsername());
        username.getStyle().set("font-weight", "bold");
        Span suffix = new Span(" é:");
        
        H3 hash = new H3(tempPass);
        hash.getStyle().set("color", "var(--lumo-primary-color)");
        hash.getStyle().set("text-align", "center");
        hash.getStyle().set("background", "var(--lumo-contrast-5pct)");
        hash.getStyle().set("padding", "10px");
        hash.getStyle().set("border-radius", "4px");
        hash.getStyle().set("font-family", "monospace");
        hash.getStyle().set("margin", "0");

        Button copyBtn = new Button(VaadinIcon.COPY.create(), e -> {
            getUI().ifPresent(ui -> {
                ui.getPage().executeJs("navigator.clipboard.writeText($0)", tempPass);
                Notification.show("Copiado para a área de transferência!");
            });
        });
        copyBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY);
        copyBtn.setTooltipText("Copiar senha");

        HorizontalLayout hashLayout = new HorizontalLayout(hash, copyBtn);
        hashLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        hashLayout.setSpacing(true);
        
        VerticalLayout layout = new VerticalLayout(new HorizontalLayout(text, username, suffix), hashLayout);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        dialog.add(layout);
        
        Button closeBtn = new Button("Fechar", e -> dialog.close());
        closeBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeBtn);
        
        dialog.open();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureGrid() {
        grid.addClassNames("user-grid");
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(User::getName).setHeader("Nome").setSortable(true);
        grid.addColumn(User::getUsername).setHeader("Username").setSortable(true);
        grid.addColumn(user -> user.getRole() != null ? user.getRole().getDescription() : "").setHeader("Papel").setSortable(true);
        grid.addColumn(user -> user.isActive() ? "Sim" : "Não").setHeader("Ativo");
        grid.addColumn(user -> user.isNotifyStoreOrder() ? "Sim" : "Não").setHeader("Alerta Loja");
        
        grid.addComponentColumn(user -> {
            boolean online = presenceService.isOnline(user.getId());
            com.vaadin.flow.component.html.Span dot = new com.vaadin.flow.component.html.Span();
            dot.getStyle()
                .set("width", "10px")
                .set("height", "10px")
                .set("border-radius", "50%")
                .set("display", "inline-block")
                .set("margin-right", "8px")
                .set("background-color", online ? "var(--lumo-success-color)" : "var(--lumo-contrast-30pct)");
            
            com.vaadin.flow.component.html.Span text = new com.vaadin.flow.component.html.Span(online ? "Online" : "Offline");
            text.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            
            HorizontalLayout layout = new HorizontalLayout(dot, text);
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(false);
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return layout;
        }).setHeader("Status").setSortable(true).setComparator(user -> presenceService.isOnline(user.getId()));

        grid.setPartNameGenerator(user -> user.isDeleted() ? "deleted-row" : null);

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.asSingleSelect().addValueChangeListener(event ->
                editUser(event.getValue()));
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filtrar por nome...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        showDeleted.addValueChangeListener(e -> updateList());
        showDeleted.setVisible(securityService.getAuthenticatedInternalUser()
                .map(u -> u.getRole() == com.xflprflx.neokioski.models.enums.Role.ADMIN)
                .orElse(false));

        Button addUserButton = new Button("Novo Usuário");
        addUserButton.addClickListener(click -> addUser());

        var toolbar = new HorizontalLayout(filterText, showDeleted, addUserButton);
        toolbar.addClassName("toolbar");
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return toolbar;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void editUser(User user) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (user == null) {
            closeEditor();
        } else {
            form.setUser(user);
            form.setVisible(true);
            addClassName("editing");
        }
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void closeEditor() {
        form.setUser(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void addUser() {
        grid.asSingleSelect().clear();
        editUser(new User());
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void updateList() {
        grid.setItems(service.findAll(filterText.getValue(), showDeleted.getValue()));
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        presenceListener = event -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                grid.getDataProvider().refreshAll();
            }));
        };
        presenceService.addListener(presenceListener);
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (presenceListener != null) {
            presenceService.removeListener(presenceListener);
        }
    }
}
