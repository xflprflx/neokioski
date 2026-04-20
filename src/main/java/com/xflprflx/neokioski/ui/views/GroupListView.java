package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.services.GroupService;
import com.xflprflx.neokioski.services.UserService;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("ADMIN")
@Route(value = "grupos", layout = MainLayout.class)
@PageTitle("Grupos | NeoKioski")
/**
 * View que exibe uma listagem em tabela (Grid) de todos os grupos do sistema, permitindo pesquisa e edição.
 */
public class GroupListView extends VerticalLayout {
    Grid<Group> grid = new Grid<>(Group.class);
    TextField filterText = new TextField();
    GroupForm form;
    GroupService service;
    UserService userService;

    public GroupListView(GroupService service, UserService userService) {
        this.service = service;
        this.userService = userService;
        addClassName("group-list-view");
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
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureForm() {
        form = new GroupForm();
        form.setWidth("25em");
        form.setAvailableUsers(userService.findAll());
        form.addSaveListener(this::saveGroup);
        form.addDeleteListener(this::deleteGroup);
        form.addCloseListener(e -> closeEditor());
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void saveGroup(GroupForm.SaveEvent event) {
        service.save(event.getGroup());
        updateList();
        closeEditor();
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void deleteGroup(GroupForm.DeleteEvent event) {
        service.delete(event.getGroup());
        updateList();
        closeEditor();
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureGrid() {
        grid.addClassNames("group-grid");
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(Group::getName).setHeader("Nome").setSortable(true);
        grid.addColumn(group -> group.isActive() ? "Sim" : "Não").setHeader("Ativo").setSortable(true);
        grid.addColumn(group -> group.getUsers() == null ? 0 : group.getUsers().size()).setHeader("Usuários");
        grid.addComponentColumn(group -> {
            Button chatButton = new Button(VaadinIcon.CHAT.create());
            chatButton.addClickListener(e -> UI.getCurrent().navigate(ChatView.class, group.getId()));
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return chatButton;
        }).setHeader("Chat");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.asSingleSelect().addValueChangeListener(event ->
                editGroup(event.getValue()));
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filtrar por nome...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button addGroupButton = new Button("Novo Grupo");
        addGroupButton.addClickListener(click -> addGroup());

        var toolbar = new HorizontalLayout(filterText, addGroupButton);
        toolbar.addClassName("toolbar");
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return toolbar;
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void editGroup(Group group) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (group == null) {
            closeEditor();
        } else {
            form.setGroup(group);
            form.setVisible(true);
            addClassName("editing");
        }
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void closeEditor() {
        form.setGroup(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void addGroup() {
        grid.asSingleSelect().clear();
        editGroup(new Group());
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void updateList() {
        grid.setItems(service.findAll(filterText.getValue()));
    }
}
