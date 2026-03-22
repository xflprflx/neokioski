package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.services.GroupService;
import com.xflprflx.neokioski.services.TaskTemplateService;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;

@RolesAllowed("ADMIN")
@Route(value = "task-template-editor", layout = MainLayout.class)
@PageTitle("Editor de Template de Tarefa | NeoKioski")
public class TaskTemplateEditorView extends VerticalLayout {

    private final TaskTemplateService templateService;
    private final GroupService groupService;

    // UI Top Components
    private TextField nameField = new TextField("Nome do Template");
    private MultiSelectComboBox<Group> groupComboBox = new MultiSelectComboBox<>("Grupos");
    private Checkbox showSendButton = new Checkbox();
    private Checkbox requireSignature = new Checkbox();

    // Tabs
    private Tabs tabs = new Tabs();
    private Tab formFieldsTab = new Tab("Campos do Formulário");
    private Tab workflowTab = new Tab("Fluxo de Trabalho");
    private VerticalLayout tabContent = new VerticalLayout();
    private VerticalLayout workflowPreview = new VerticalLayout();

    // DTOs para Grid dinâmico antes de Salvar no BD
    public static class FieldDTO {
        public String label;
        public InputType inputType;
        public boolean isRequired;
        public String description;
    }

    public static class StepDTO {
        public String label;
        public String colorTheme;
        public boolean isFinalizer;
    }

    private List<FieldDTO> fields = new ArrayList<>();
    private List<StepDTO> steps = new ArrayList<>();

    private Grid<FieldDTO> fieldsGrid = new Grid<>(FieldDTO.class, false);
    private Grid<StepDTO> stepsGrid = new Grid<>(StepDTO.class, false);

    public TaskTemplateEditorView(TaskTemplateService templateService, GroupService groupService) {
        this.templateService = templateService;
        this.groupService = groupService;

        setSizeFull();
        setPadding(true);

        H2 title = new H2("Editor de Template de Tarefa");
        
        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.setSizeFull();
        mainContent.setSpacing(true);

        VerticalLayout leftSide = new VerticalLayout();
        leftSide.setPadding(false);
        leftSide.add(createTopConfiguration(), createTabs(), tabContent, createFooter());

        setupWorkflowPreviewLayout();
        updateWorkflowPreview();

        mainContent.add(leftSide, workflowPreview);
        mainContent.setFlexGrow(3, leftSide);
        mainContent.setFlexGrow(1, workflowPreview);

        add(title, mainContent);

        showFormFieldsTab(); // Inicia com aba de campos
    }

    private Component createTopConfiguration() {
        HorizontalLayout firstRow = new HorizontalLayout();
        firstRow.setWidthFull();
        
        nameField.setWidthFull();
        groupComboBox.setWidthFull();
        groupComboBox.setItems(groupService.findAll(""));
        groupComboBox.setItemLabelGenerator(Group::getName);

        firstRow.add(nameField, groupComboBox);

        VerticalLayout secondRow = new VerticalLayout();
        secondRow.setPadding(false);
        secondRow.setSpacing(true);

        HorizontalLayout showSendBtnLayout = new HorizontalLayout();
        showSendBtnLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Span sendBtnText = new Span("Mostrar botão ENVIAR?");
        showSendBtnLayout.add(showSendButton, sendBtnText);

        HorizontalLayout reqSignatureLayout = new HorizontalLayout();
        reqSignatureLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Span reqSigText = new Span("Requer autorização?");
        reqSignatureLayout.add(requireSignature, reqSigText);

        secondRow.add(showSendBtnLayout, reqSignatureLayout);

        VerticalLayout configLayout = new VerticalLayout(firstRow, secondRow);
        configLayout.setPadding(false);
        return configLayout;
    }

    private Component createTabs() {
        tabs.add(formFieldsTab, workflowTab);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(formFieldsTab)) {
                showFormFieldsTab();
            } else {
                showWorkflowTab();
            }
        });
        return tabs;
    }

    private void showFormFieldsTab() {
        tabContent.removeAll();
        
        Button addFieldBtn = new Button("Adicionar Novo Campo");
        addFieldBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addFieldBtn.addClickListener(e -> openFieldDialog());

        if (fieldsGrid.getColumns().isEmpty()) {
            fieldsGrid.addComponentColumn(item -> new Span(VaadinIcon.MENU.create())).setHeader("Reordenar").setFlexGrow(0).setWidth("100px");
            fieldsGrid.addColumn(dto -> dto.label).setHeader("Rótulo do Campo");
            fieldsGrid.addColumn(dto -> dto.inputType != null ? dto.inputType.name() : "").setHeader("Tipo de Entrada");
            fieldsGrid.addComponentColumn(dto -> {
                Checkbox cb = new Checkbox();
                cb.setValue(dto.isRequired);
                cb.setReadOnly(true);
                return cb;
            }).setHeader("Obrigatório");
            fieldsGrid.addColumn(dto -> dto.description).setHeader("Descrição");
            fieldsGrid.addComponentColumn(item -> {
                Button deleteBtn = new Button(VaadinIcon.TRASH.create());
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                deleteBtn.addClickListener(e -> {
                    fields.remove(item);
                    fieldsGrid.getDataProvider().refreshAll();
                });
                return deleteBtn;
            }).setHeader("Ações");
            fieldsGrid.setItems(fields);
        }

        tabContent.add(addFieldBtn, fieldsGrid);
    }

    private void openFieldDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Novo Campo");

        TextField labelField = new TextField("Rótulo do Campo");
        ComboBox<InputType> typeCombo = new ComboBox<>("Tipo de Entrada", InputType.values());
        typeCombo.setItemLabelGenerator(InputType::name);
        Checkbox requiredCheck = new Checkbox("Obrigatório");
        TextField descField = new TextField("Descrição");

        VerticalLayout layout = new VerticalLayout(labelField, typeCombo, requiredCheck, descField);
        dialog.add(layout);

        Button saveBtn = new Button("Salvar", e -> {
            if (labelField.isEmpty() || typeCombo.isEmpty()) {
                Notification.show("Rótulo e Tipo são obrigatórios");
                return;
            }
            FieldDTO dto = new FieldDTO();
            dto.label = labelField.getValue();
            dto.inputType = typeCombo.getValue();
            dto.isRequired = requiredCheck.getValue();
            dto.description = descField.getValue();
            fields.add(dto);
            fieldsGrid.getDataProvider().refreshAll();
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void showWorkflowTab() {
        tabContent.removeAll();
        
        Button addStepBtn = new Button("Adicionar Nova Etapa");
        addStepBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addStepBtn.addClickListener(e -> openStepDialog());

        if (stepsGrid.getColumns().isEmpty()) {
            stepsGrid.addComponentColumn(item -> new Span(VaadinIcon.MENU.create())).setHeader("Reordenar").setFlexGrow(0).setWidth("100px");
            stepsGrid.addColumn(dto -> dto.label).setHeader("Rótulo da Etapa");
            stepsGrid.addColumn(dto -> dto.colorTheme).setHeader("Cor/Tema");
            stepsGrid.addComponentColumn(dto -> {
                Checkbox cb = new Checkbox();
                cb.setValue(dto.isFinalizer);
                cb.setReadOnly(true);
                return cb;
            }).setHeader("Finalizador");
            stepsGrid.addComponentColumn(item -> {
                Button deleteBtn = new Button(VaadinIcon.TRASH.create());
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                deleteBtn.addClickListener(e -> {
                    steps.remove(item);
                    stepsGrid.getDataProvider().refreshAll();
                    updateWorkflowPreview();
                });
                return deleteBtn;
            }).setHeader("Ações");
            stepsGrid.setItems(steps);
        }

        tabContent.add(addStepBtn, stepsGrid);
    }

    private void openStepDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nova Etapa");

        TextField labelField = new TextField("Rótulo da Etapa");
        ComboBox<String> colorCombo = new ComboBox<>("Tema", List.of("blue", "yellow", "green"));
        Checkbox finalizerCheck = new Checkbox("É Finalizador?");
        
        VerticalLayout layout = new VerticalLayout(labelField, colorCombo, finalizerCheck);
        dialog.add(layout);

        Button saveBtn = new Button("Salvar", e -> {
            if (labelField.isEmpty() || colorCombo.isEmpty()) {
                Notification.show("Rótulo e Tema são obrigatórios");
                return;
            }
            StepDTO dto = new StepDTO();
            dto.label = labelField.getValue();
            dto.colorTheme = colorCombo.getValue();
            dto.isFinalizer = finalizerCheck.getValue();
            steps.add(dto);
            stepsGrid.getDataProvider().refreshAll();
            updateWorkflowPreview();
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void setupWorkflowPreviewLayout() {
        workflowPreview.setWidth("350px");
        workflowPreview.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        workflowPreview.getStyle().set("border-radius", "8px");
        workflowPreview.setAlignItems(FlexComponent.Alignment.CENTER);
    }

    private void updateWorkflowPreview() {
        workflowPreview.removeAll();
        H3 previewTitle = new H3("Pré-visualização do Fluxo");
        workflowPreview.add(previewTitle);

        if (steps.isEmpty()) {
            workflowPreview.add(new Span("Nenhuma etapa definida."));
            return;
        }

        for (int i = 0; i < steps.size(); i++) {
            StepDTO s = steps.get(i);
            VaadinIcon icon = VaadinIcon.COG;
            
            if (s.isFinalizer) {
                icon = VaadinIcon.STAR;
            } else if (i == 0) {
                icon = VaadinIcon.PENCIL;
            }

            workflowPreview.add(createPreviewStep(s.label, s.colorTheme, icon));
            if (i < steps.size() - 1) {
                workflowPreview.add(new Span(VaadinIcon.ARROW_DOWN.create()));
            }
        }
    }

    private Div createPreviewStep(String label, String colorScheme, VaadinIcon icon) {
        Div step = new Div();
        step.setWidthFull();
        step.setHeight("80px");
        step.getStyle().set("border-radius", "8px");
        step.getStyle().set("display", "flex");
        step.getStyle().set("align-items", "center");
        step.getStyle().set("justify-content", "center");
        step.getStyle().set("flex-direction", "column");
        step.getStyle().set("font-weight", "bold");

        if ("blue".equals(colorScheme)) {
            step.getStyle().set("background-color", "#e3f2fd");
            step.getStyle().set("border", "1px solid #90caf9");
            step.getStyle().set("color", "#1565c0");
        } else if ("yellow".equals(colorScheme)) {
            step.getStyle().set("background-color", "#fff9c4");
            step.getStyle().set("border", "1px solid #fff59d");
            step.getStyle().set("color", "#f57f17");
        } else if ("green".equals(colorScheme)) {
            step.getStyle().set("background-color", "#e8f5e9");
            step.getStyle().set("border", "1px solid #a5d6a7");
            step.getStyle().set("color", "#2e7d32");
        } else {
            step.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        }

        step.add(icon.create());
        step.add(new Span(label));
        return step;
    }

    private HorizontalLayout createFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.getStyle().set("margin-top", "auto");
        footer.getStyle().set("padding-top", "20px");
        
        Button saveBtn = new Button("Salvar Alterações");
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.addClickListener(e -> saveTemplate());

        Button cancelBtn = new Button("Cancelar", e -> {
            fields.clear();
            steps.clear();
            fieldsGrid.getDataProvider().refreshAll();
            stepsGrid.getDataProvider().refreshAll();
            updateWorkflowPreview();
            nameField.clear();
            groupComboBox.clear();
        });
        
        footer.add(saveBtn, cancelBtn);
        return footer;
    }

    private void saveTemplate() {
        TaskTemplate t = new TaskTemplate();
        t.setName(nameField.getValue());
        t.setShowSendButton(showSendButton.getValue());
        t.setRequiresSignature(requireSignature.getValue());
        if (groupComboBox.getValue() != null) {
            for (Group g : groupComboBox.getValue()) {
                t.addGroups(g);
            }
        }

        List<TemplateFieldRelation> fieldRelations = new ArrayList<>();
        int fOrder = 1;
        for (FieldDTO dto : fields) {
            TaskFieldDefinition def = new TaskFieldDefinition();
            def.setLabel(dto.label);
            def.setInputType(dto.inputType);
            def.setDescription(dto.description);

            TemplateFieldRelation rel = new TemplateFieldRelation();
            rel.setTaskFieldDefinition(def);
            rel.setRequired(dto.isRequired);
            rel.setOrder(fOrder++);
            fieldRelations.add(rel);
        }

        List<TemplateStepRelation> stepRelations = new ArrayList<>();
        int sOrder = 1;
        for (StepDTO dto : steps) {
            TaskStepDefinition def = new TaskStepDefinition();
            def.setLabel(dto.label);
            def.setColorTheme(dto.colorTheme);

            TemplateStepRelation rel = new TemplateStepRelation();
            rel.setTaskStepDefinition(def);
            rel.setOrder(sOrder);
            rel.setFinalizer(dto.isFinalizer);
            stepRelations.add(rel);
            sOrder++;
        }

        templateService.saveTemplate(t, fieldRelations, stepRelations);
        Notification.show("Template Salvo com Sucesso!");
    }
}
