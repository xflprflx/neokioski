package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.services.TaskTemplateService;
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Templates de Tarefas")
@Route(value = "task-templates", layout = MainLayout.class)
@RolesAllowed("ADMIN")
/**
 * View de listagem que apresenta os templates de tarefas criados, permitindo o gerenciamento e acesso ao editor.
 */
public class TaskTemplateListView extends VerticalLayout {

    private final TaskTemplateService service;
    private final Grid<TaskTemplate> grid = new Grid<>(TaskTemplate.class, false);

    public TaskTemplateListView(TaskTemplateService service) {
        this.service = service;

        setSizeFull();

        Button createBtn = new Button("Novo Template", VaadinIcon.PLUS.create());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(TaskTemplateEditorView.class)));

        configureGrid();

        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(createBtn, grid);
        
        updateList();
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureGrid() {
        grid.addColumn(TaskTemplate::getName).setHeader("Nome do Template");
        grid.addColumn(TaskTemplate::getSortOrder).setHeader("Ordem").setWidth("100px").setFlexGrow(0);
        grid.addColumn(t -> t.isShowSendButton() ? "Sim" : "Não").setHeader("Botão Enviar");
        grid.addColumn(t -> t.isRequiresSignature() ? "Sim" : "Não").setHeader("Requer Assinatura");

        grid.addComponentColumn(t -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(TaskTemplateEditorView.class, t.getId())));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> {
                service.deleteTemplate(t.getId());
                updateList();
            });

            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return new HorizontalLayout(editBtn, deleteBtn);
        }).setHeader("Ações");
    }

    /**
     * Método ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void updateList() {
        grid.setItems(service.findAll());
    }
}
