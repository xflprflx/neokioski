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
import com.vaadin.flow.component.checkbox.Checkbox;
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
    private final Checkbox showDeletedCheckbox = new Checkbox("Exibir templates excluídos");

    public TaskTemplateListView(TaskTemplateService service) {
        this.service = service;

        setSizeFull();

        Button createBtn = new Button("Novo Template", VaadinIcon.PLUS.create());
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(TaskTemplateEditorView.class)));

        showDeletedCheckbox.addValueChangeListener(e -> updateList());

        configureGrid();

        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(new HorizontalLayout(createBtn, showDeletedCheckbox), grid);
        
        updateList();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureGrid() {
        grid.addColumn(TaskTemplate::getName).setHeader("Nome do Template");
        grid.addColumn(TaskTemplate::getSortOrder).setHeader("Ordem").setWidth("100px").setFlexGrow(0);
        grid.addColumn(t -> t.isShowSendButton() ? "Sim" : "Não").setHeader("Botão Enviar");
        grid.addColumn(t -> t.isRequiresSignature() ? "Sim" : "Não").setHeader("Requer Assinatura");

        grid.setPartNameGenerator(t -> t.isDeleted() ? "deleted-row" : null);

        grid.addComponentColumn(t -> {
            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(TaskTemplateEditorView.class, t.getId())));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.setVisible(!t.isDeleted());
            deleteBtn.addClickListener(e -> {
                service.deleteTemplate(t.getId());
                updateList();
            });

            Button restoreBtn = new Button(VaadinIcon.ARROW_BACKWARD.create());
            restoreBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            restoreBtn.setVisible(t.isDeleted());
            restoreBtn.setTooltipText("Restaurar Template");
            restoreBtn.addClickListener(e -> {
                service.restoreTemplate(t.getId());
                updateList();
            });

            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return new HorizontalLayout(editBtn, deleteBtn, restoreBtn);
        }).setHeader("Ações");
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void updateList() {
        grid.setItems(service.findAll(showDeletedCheckbox.getValue()));
    }
}
