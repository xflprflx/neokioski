package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.services.UserService;
import com.xflprflx.neokioski.services.PresenceService;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("ADMIN")
@Route(value = "usuarios", layout = MainLayout.class)
@PageTitle("Usuários | NeoKioski")
public class UserListView extends VerticalLayout {
    Grid<User> grid = new Grid<>(User.class);
    TextField filterText = new TextField();
    UserForm form;
    UserService service;
    PresenceService presenceService;
    private PresenceService.PresenceEvent lastEvent;
    private java.util.function.Consumer<PresenceService.PresenceEvent> presenceListener;

    public UserListView(UserService service, PresenceService presenceService) {
        this.service = service;
        this.presenceService = presenceService;
        addClassName("user-list-view");
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
        form = new UserForm();
        form.setWidth("25em");
        form.addSaveListener(this::saveUser);
        form.addDeleteListener(this::deleteUser);
        form.addCloseListener(e -> closeEditor());
    }

    private void saveUser(UserForm.SaveEvent event) {
        service.save(event.getUser());
        updateList();
        closeEditor();
    }

    private void deleteUser(UserForm.DeleteEvent event) {
        service.delete(event.getUser());
        updateList();
        closeEditor();
    }

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
            return layout;
        }).setHeader("Status").setSortable(true).setComparator(user -> presenceService.isOnline(user.getId()));

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.asSingleSelect().addValueChangeListener(event ->
                editUser(event.getValue()));
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filtrar por nome...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button addUserButton = new Button("Novo Usuário");
        addUserButton.addClickListener(click -> addUser());

        var toolbar = new HorizontalLayout(filterText, addUserButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    public void editUser(User user) {
        if (user == null) {
            closeEditor();
        } else {
            form.setUser(user);
            form.setVisible(true);
            addClassName("editing");
        }
    }

    private void closeEditor() {
        form.setUser(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    private void addUser() {
        grid.asSingleSelect().clear();
        editUser(new User());
    }

    private void updateList() {
        grid.setItems(service.findAll(filterText.getValue()));
    }

    @Override
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
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (presenceListener != null) {
            presenceService.removeListener(presenceListener);
        }
    }
}
