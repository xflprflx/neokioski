package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.BeforeEvent;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.services.GroupService;
import com.xflprflx.neokioski.services.TaskTemplateService;
import com.xflprflx.neokioski.services.UserService;
import com.xflprflx.neokioski.models.User;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@RolesAllowed("ADMIN")
@Route(value = "task-template-editor", layout = MainLayout.class)
@PageTitle("Editor de Template de Tarefa | NeoKioski")
public class TaskTemplateEditorView extends VerticalLayout implements HasUrlParameter<Long> {

    private Long editingTemplateId = null;

    // UI Top Components
    private TextField nameField = new TextField("Nome do Template");
    private MultiSelectComboBox<Group> groupComboBox = new MultiSelectComboBox<>("Grupos");
    private Checkbox showSendButton = new Checkbox(true);
    private Checkbox requireSignature = new Checkbox();
    private MultiSelectComboBox<User> authorizingUsersCombo = new MultiSelectComboBox<>("Usuários Autorizadores");
    private Checkbox playAlertSoundUI = new Checkbox();

    // Tabs
    private Tabs tabs = new Tabs();
    private Tab formFieldsTab = new Tab("Campos do Formulário");
    private Tab workflowTab = new Tab("Fluxo de Trabalho");
    private VerticalLayout tabContent = new VerticalLayout();
    private VerticalLayout workflowPreview = new VerticalLayout();

    // Sugestões de menção
    private Div suggestionWrapper;
    private ListBox<User> suggestionList;
    private String activeMentionId = "";

    // DTOs para Grid dinâmico antes de Salvar no BD
    public static class FieldDTO {
        public String label;
        public InputType inputType;
        public boolean isRequired;
        public String description;
        public List<String> options = new ArrayList<>();
    }

    public static class StepDTO {
        public String label;
        public String colorTheme;
        public boolean isFinalizer;
        public boolean mentionAuthor;
        public String mentionMessage;
        public List<String> allowedNextStepLabels = new ArrayList<>();
        public boolean mentionPreviousActor;
        public String mentionPreviousActorMessage;
        public boolean isGlobal;
        public boolean shouldPulse;
        public boolean hasResponseField;
        public boolean isResponseRequired;
        public String responseFieldLabel;
        public boolean responseIsCompact;
        public InputType responseInputType = InputType.TEXT;
        public List<String> responseOptions = new ArrayList<>();
    }

    private List<FieldDTO> fields = new ArrayList<>();
    private List<StepDTO> steps = new ArrayList<>();

    private Grid<FieldDTO> fieldsGrid = new Grid<>(FieldDTO.class, false);
    private Grid<StepDTO> stepsGrid = new Grid<>(StepDTO.class, false);

    private FieldDTO draggedField;
    private StepDTO draggedStep;


    private final TaskTemplateService templateService;
    private final GroupService groupService;
    private final UserService userService;

    public TaskTemplateEditorView(TaskTemplateService templateService, GroupService groupService, UserService userService) {
        this.templateService = templateService;
        this.groupService = groupService;
        this.userService = userService;

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

        configureSuggestions();
        showFormFieldsTab(); // Inicia com aba de campos
    }

    private void configureSuggestions() {
        suggestionList = new ListBox<>();
        suggestionList.setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(user -> {
            Span name = new Span(user.getName());
            Span username = new Span("@" + user.getUsername());
            username.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            username.getStyle().set("color", "var(--lumo-secondary-text-color)");
            username.getStyle().set("margin-left", "8px");
            return new HorizontalLayout(name, username);
        }));

        suggestionList.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                applyMention(event.getValue());
                suggestionList.setValue(null);
            }
        });

        suggestionWrapper = new Div(suggestionList);
        suggestionWrapper.getStyle()
                .set("position", "absolute")
                .set("z-index", "10005")
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.15)")
                .set("border-radius", "8px")
                .set("padding", "4px")
                .set("border", "1px solid var(--lumo-contrast-10pct)");
        suggestionWrapper.setVisible(false);

        getElement().executeJs(
            "window.nkChatServer = {" +
            "  show: (query) => $0.$server.showSuggestions(query)," +
            "  hide: () => $0.$server.hideSuggestions()," +
            "  setActive: (id) => $0.$server.setActiveTargetById(id)" +
            "};",
            getElement()
        );
    }

    @com.vaadin.flow.component.ClientCallable
    public void showSuggestions(String query) {
        List<User> suggestions = userService.findAll().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(query.toLowerCase()) || 
                             u.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        // Adicionar sugestão virtual para o Autor da Tarefa
        if ("autor".contains(query.toLowerCase()) || "@autor".contains(query.toLowerCase())) {
            User virtualAuthor = new User();
            virtualAuthor.setName("Autor da Tarefa");
            virtualAuthor.setUsername("autor");
            suggestions.add(0, virtualAuthor);
        }
        
        // Adicionar sugestão virtual para o Autor Anterior
        if ("autor_anterior".contains(query.toLowerCase()) || "@autor_anterior".contains(query.toLowerCase())) {
            User virtualPrev = new User();
            virtualPrev.setName("Autor Anterior");
            virtualPrev.setUsername("autor_anterior");
            suggestions.add(1, virtualPrev);
        }

        if (suggestions.isEmpty()) {
            suggestionWrapper.setVisible(false);
        } else {
            suggestionList.setItems(suggestions);
            suggestionWrapper.setVisible(true);
            
            // Posicionar próximo ao elemento ativo se possível
            suggestionWrapper.getStyle().set("bottom", "100px");
            suggestionWrapper.getStyle().set("left", "50px");
        }
    }

    @com.vaadin.flow.component.ClientCallable
    public void hideSuggestions() {
        suggestionWrapper.setVisible(false);
    }

    @com.vaadin.flow.component.ClientCallable
    public void setActiveTargetById(String mentionId) {
        this.activeMentionId = mentionId;
    }

    private void applyMention(User user) {
        String mention = "@" + user.getUsername() + " ";
        getElement().executeJs(
            "const findT = (el) => {\n" +
            "  if (!el) return null;\n" +
            "  if (el.tagName === 'TEXTAREA') return el;\n" +
            "  if (el.shadowRoot) {\n" +
            "    const internal = el.shadowRoot.querySelector('textarea, vaadin-message-input-textarea');\n" +
            "    if (internal) return findT(internal);\n" +
            "  }\n" +
            "  return el.querySelector('textarea');\n" +
            "};\n" +
            "const host = document.querySelector('[mention-id=\"' + $1 + '\"]');\n" +
            "const target = findT(host);\n" +
            "if (!target) return;\n" +
            "const val = target.value;\n" +
            "const pos = target.selectionStart;\n" +
            "const textBefore = val.substring(0, pos);\n" +
            "const textAfter = val.substring(pos);\n" +
            "const atIndex = textBefore.lastIndexOf('@');\n" +
            "const newVal = textBefore.substring(0, atIndex) + $0 + textAfter;\n" +
            "target.value = newVal;\n" +
            "const newPos = atIndex + $0.length;\n" +
            "target.focus();\n" +
            "target.setSelectionRange(newPos, newPos);\n" +
            "target.dispatchEvent(new Event('input', { bubbles: true, composed: true }));\n" +
            "target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));",
            mention, activeMentionId
        );
        hideSuggestions();
    }

    private void setupMentionLogic(com.vaadin.flow.dom.Element element, String mentionId) {
        element.executeJs(
            "const mentionId = $0;\n" +
            "const findT = (el) => {\n" +
            "  if (el.tagName === 'TEXTAREA') return el;\n" +
            "  if (el.shadowRoot) {\n" +
            "    const internal = el.shadowRoot.querySelector('textarea, vaadin-message-input-textarea');\n" +
            "    if (internal) return findT(internal);\n" +
            "  }\n" +
            "  return el.querySelector('textarea');\n" +
            "};\n" +
            "const host = this;\n" +
            "const target = findT(host);\n" +
            "if (!target) return;\n" +
            "const handleInput = e => {\n" +
            "  const val = target.value;\n" +
            "  const pos = target.selectionStart;\n" +
            "  const textBefore = val.substring(0, pos);\n" +
            "  const atIndex = textBefore.lastIndexOf('@');\n" +
            "  if (atIndex !== -1 && (atIndex === 0 || textBefore[atIndex-1] === ' ')) {\n" +
            "    const query = textBefore.substring(atIndex + 1);\n" +
            "    if (!query.includes(' ')) {\n" +
            "      if (window.nkChatServer) window.nkChatServer.setActive(mentionId);\n" +
            "      if (window.nkChatServer) window.nkChatServer.show(query);\n" +
            "      return;\n" +
            "    }\n" +
            "  }\n" +
            "  if (window.nkChatServer) window.nkChatServer.hide();\n" +
            "};\n" +
            "target.addEventListener('input', handleInput);\n" +
            "target.addEventListener('focus', () => { if (window.nkChatServer) window.nkChatServer.setActive(mentionId); });",
            mentionId
        );
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
        authorizingUsersCombo.setItems(userService.findAll());
        authorizingUsersCombo.setItemLabelGenerator(User::getName);
        authorizingUsersCombo.setWidthFull();
        authorizingUsersCombo.setVisible(false);
        
        requireSignature.addValueChangeListener(e -> authorizingUsersCombo.setVisible(e.getValue()));
        
        reqSignatureLayout.add(requireSignature, reqSigText);

        HorizontalLayout playAlertSoundLayout = new HorizontalLayout();
        playAlertSoundLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Span playAlertSoundText = new Span("Tocar alerta sonoro?");
        playAlertSoundLayout.add(playAlertSoundUI, playAlertSoundText);

        secondRow.add(showSendBtnLayout, reqSignatureLayout, authorizingUsersCombo, playAlertSoundLayout);

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
            fieldsGrid.addComponentColumn(item -> {
                Span icon = new Span(VaadinIcon.MENU.create());
                icon.getStyle().set("cursor", "move");
                return icon;
            }).setHeader("Reordenar").setFlexGrow(0).setWidth("100px");
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
                Button editBtn = new Button(VaadinIcon.EDIT.create());
                editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                editBtn.addClickListener(e -> openFieldDialog(item));

                Button deleteBtn = new Button(VaadinIcon.TRASH.create());
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                deleteBtn.addClickListener(e -> {
                    fields.remove(item);
                    fieldsGrid.getDataProvider().refreshAll();
                });
                return new HorizontalLayout(editBtn, deleteBtn);
            }).setHeader("Ações");
            fieldsGrid.setItems(fields);

            // Configurar Drag and Drop para reordenação
            fieldsGrid.setRowsDraggable(true);
            fieldsGrid.setDropMode(GridDropMode.BETWEEN);

            fieldsGrid.addDragStartListener(e -> draggedField = e.getDraggedItems().get(0));

            fieldsGrid.addDropListener(e -> {
                FieldDTO targetField = e.getDropTargetItem().orElse(null);
                GridDropLocation dropLocation = e.getDropLocation();

                if (draggedField != null && targetField != null && !draggedField.equals(targetField)) {
                    fields.remove(draggedField);
                    int targetIndex = fields.indexOf(targetField);
                    if (dropLocation == GridDropLocation.BELOW) {
                        targetIndex++;
                    }
                    fields.add(targetIndex, draggedField);
                    fieldsGrid.getDataProvider().refreshAll();
                }
            });

            fieldsGrid.addDragEndListener(e -> draggedField = null);
        }


        tabContent.add(addFieldBtn, fieldsGrid);
    }

    private void openFieldDialog() {
        openFieldDialog(null);
    }

    private void openFieldDialog(FieldDTO existingDto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existingDto == null ? "Novo Campo" : "Editar Campo");

        TextField labelField = new TextField("Rótulo do Campo");
        ComboBox<InputType> typeCombo = new ComboBox<>("Tipo de Entrada", InputType.values());
        typeCombo.setItemLabelGenerator(InputType::name);
        Checkbox requiredCheck = new Checkbox("Obrigatório");
        TextField descField = new TextField("Descrição");
        com.vaadin.flow.component.textfield.TextArea optionsField = new com.vaadin.flow.component.textfield.TextArea("Opções (separadas por vírgula)");
        optionsField.setHelperText("Ex: Opção A, Opção B, Opção C");
        optionsField.setWidthFull();
        optionsField.setVisible(false);

        typeCombo.addValueChangeListener(e -> {
            InputType t = e.getValue();
            boolean needsOptions = t == InputType.RADIO || t == InputType.CHECKBOX || t == InputType.DROPDOWN || t == InputType.MULTISELECT_DROPDOWN;
            optionsField.setVisible(needsOptions);
        });

        if (existingDto != null) {
            labelField.setValue(existingDto.label != null ? existingDto.label : "");
            typeCombo.setValue(existingDto.inputType);
            requiredCheck.setValue(existingDto.isRequired);
            descField.setValue(existingDto.description != null ? existingDto.description : "");
            if (existingDto.options != null && !existingDto.options.isEmpty()) {
                optionsField.setValue(String.join(", ", existingDto.options));
            }
        }

        VerticalLayout layout = new VerticalLayout(labelField, typeCombo, optionsField, requiredCheck, descField);
        dialog.add(layout);

        Button saveBtn = new Button("Salvar", e -> {
            if (labelField.isEmpty() || typeCombo.isEmpty()) {
                Notification.show("Rótulo e Tipo são obrigatórios");
                return;
            }
            FieldDTO dto = (existingDto != null) ? existingDto : new FieldDTO();
            dto.label = labelField.getValue();
            dto.inputType = typeCombo.getValue();
            dto.isRequired = requiredCheck.getValue();
            dto.description = descField.getValue();
            if (optionsField.isVisible() && !optionsField.isEmpty()) {
                dto.options = List.of(optionsField.getValue().split("\\s*,\\s*"));
            } else {
                dto.options = new ArrayList<>();
            }
            
            if (existingDto == null) {
                fields.add(dto);
            }
            
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
            stepsGrid.addComponentColumn(item -> {
                Span icon = new Span(VaadinIcon.MENU.create());
                icon.getStyle().set("cursor", "move");
                return icon;
            }).setHeader("Reordenar").setFlexGrow(0).setWidth("100px");
            stepsGrid.addColumn(dto -> dto.label).setHeader("Rótulo da Etapa");
            stepsGrid.addColumn(dto -> dto.colorTheme).setHeader("Cor/Tema");
            stepsGrid.addComponentColumn(dto -> {
                Checkbox cb = new Checkbox();
                cb.setValue(dto.isFinalizer);
                cb.setReadOnly(true);
                return cb;
            }).setHeader("Finalizador");
            stepsGrid.addComponentColumn(item -> {
                Button editBtn = new Button(VaadinIcon.EDIT.create());
                editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                editBtn.addClickListener(e -> openStepDialog(item));

                Button deleteBtn = new Button(VaadinIcon.TRASH.create());
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                deleteBtn.addClickListener(e -> {
                    steps.remove(item);
                    stepsGrid.getDataProvider().refreshAll();
                    updateWorkflowPreview();
                });
                return new HorizontalLayout(editBtn, deleteBtn);
            }).setHeader("Ações");
            stepsGrid.setItems(steps);

            // Configurar Drag and Drop para reordenação
            stepsGrid.setRowsDraggable(true);
            stepsGrid.setDropMode(GridDropMode.BETWEEN);

            stepsGrid.addDragStartListener(e -> draggedStep = e.getDraggedItems().get(0));

            stepsGrid.addDropListener(e -> {
                StepDTO targetStep = e.getDropTargetItem().orElse(null);
                GridDropLocation dropLocation = e.getDropLocation();

                if (draggedStep != null && targetStep != null && !draggedStep.equals(targetStep)) {
                    steps.remove(draggedStep);
                    int targetIndex = steps.indexOf(targetStep);
                    if (dropLocation == GridDropLocation.BELOW) {
                        targetIndex++;
                    }
                    steps.add(targetIndex, draggedStep);
                    stepsGrid.getDataProvider().refreshAll();
                    updateWorkflowPreview();
                }
            });

            stepsGrid.addDragEndListener(e -> draggedStep = null);
        }


        tabContent.add(addStepBtn, stepsGrid);
    }

    private void openStepDialog() {
        openStepDialog(null);
    }

    private void openStepDialog(StepDTO existingDto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existingDto == null ? "Nova Etapa" : "Editar Etapa");

        TextField labelField = new TextField("Rótulo da Etapa");
        ComboBox<String> colorCombo = new ComboBox<>("Tema", List.of("blue", "yellow", "green", "red", "purple", "orange", "cyan", "pink", "gray", "black"));
        Checkbox finalizerCheck = new Checkbox("É Finalizador?");
        Checkbox globalCheck = new Checkbox("É Global?");
        Checkbox shouldPulseCheck = new Checkbox("Deve pulsar?");
        
        Checkbox mentionAuthorCheck = new Checkbox("Mencionar autor");
        com.vaadin.flow.component.textfield.TextArea mentionMessageField = new com.vaadin.flow.component.textfield.TextArea("Mensagem da menção");
        mentionMessageField.setWidthFull();
        mentionMessageField.setPlaceholder("Ex: Olá @autor, favor verificar...");
        mentionMessageField.setVisible(false);
        mentionMessageField.getElement().setAttribute("mention-id", "mention-message-input");

        Checkbox hasResponseCheck = new Checkbox("Habilitar campo de resposta");
        Checkbox responseRequiredCheck = new Checkbox("Resposta obrigatória");
        TextField responseLabelField = new TextField("Rótulo do campo de resposta");
        responseLabelField.setPlaceholder("Ex: Motivo da Reprovação");
        responseLabelField.setWidthFull();
        
        ComboBox<InputType> responseInputTypeCombo = new ComboBox<>("Tipo de Resposta", InputType.values());
        responseInputTypeCombo.setWidthFull();
        responseInputTypeCombo.setValue(InputType.TEXT);
        
        com.vaadin.flow.component.textfield.TextArea responseOptionsField = new com.vaadin.flow.component.textfield.TextArea("Opções de Resposta (separadas por vírgula)");
        responseOptionsField.setWidthFull();
        responseOptionsField.setHelperText("Ex: Opção 1, Opção 2");
        responseOptionsField.setVisible(false);

        responseInputTypeCombo.addValueChangeListener(e -> {
            InputType t = e.getValue();
            boolean needsOptions = t == InputType.RADIO || t == InputType.CHECKBOX || t == InputType.DROPDOWN || t == InputType.MULTISELECT_DROPDOWN;
            responseOptionsField.setVisible(needsOptions);
        });

        Checkbox responseIsCompactCheck = new Checkbox("Campo Compacto (TextField)");
        
        responseRequiredCheck.setVisible(false);
        responseLabelField.setVisible(false);
        responseInputTypeCombo.setVisible(false);
        responseOptionsField.setVisible(false);
        responseIsCompactCheck.setVisible(false);

        hasResponseCheck.addValueChangeListener(ev -> {
            boolean visible = ev.getValue();
            responseRequiredCheck.setVisible(visible);
            responseLabelField.setVisible(visible);
            responseInputTypeCombo.setVisible(visible);
            responseIsCompactCheck.setVisible(visible && responseInputTypeCombo.getValue() == InputType.TEXT);
            
            if (visible) {
                InputType t = responseInputTypeCombo.getValue();
                boolean needsOptions = t == InputType.RADIO || t == InputType.CHECKBOX || t == InputType.DROPDOWN || t == InputType.MULTISELECT_DROPDOWN;
                responseOptionsField.setVisible(needsOptions);
            } else {
                responseOptionsField.setVisible(false);
                responseRequiredCheck.setValue(false);
            }
        });

        responseInputTypeCombo.addValueChangeListener(ev -> {
            responseIsCompactCheck.setVisible(hasResponseCheck.getValue() && ev.getValue() == InputType.TEXT);
        });

        if (existingDto != null) {
            labelField.setValue(existingDto.label);
            colorCombo.setValue(existingDto.colorTheme);
            finalizerCheck.setValue(existingDto.isFinalizer);
            globalCheck.setValue(existingDto.isGlobal);
            shouldPulseCheck.setValue(existingDto.shouldPulse);
            mentionAuthorCheck.setValue(existingDto.mentionAuthor);
            mentionMessageField.setValue(existingDto.mentionMessage != null ? existingDto.mentionMessage : "");
            mentionMessageField.setVisible(existingDto.mentionAuthor);
            
            hasResponseCheck.setValue(existingDto.hasResponseField);
            responseRequiredCheck.setValue(existingDto.isResponseRequired);
            responseLabelField.setValue(existingDto.responseFieldLabel != null ? existingDto.responseFieldLabel : "Resposta / Comentário");
            responseIsCompactCheck.setValue(existingDto.responseIsCompact);
            responseInputTypeCombo.setValue(existingDto.responseInputType != null ? existingDto.responseInputType : InputType.TEXT);
            if (existingDto.responseOptions != null && !existingDto.responseOptions.isEmpty()) {
                responseOptionsField.setValue(String.join(", ", existingDto.responseOptions));
            }
            
            boolean visible = existingDto.hasResponseField;
            responseRequiredCheck.setVisible(visible);
            responseLabelField.setVisible(visible);
            responseInputTypeCombo.setVisible(visible);
            responseIsCompactCheck.setVisible(visible && responseInputTypeCombo.getValue() == InputType.TEXT);
            
            InputType t = responseInputTypeCombo.getValue();
            boolean needsOptions = t == InputType.RADIO || t == InputType.CHECKBOX || t == InputType.DROPDOWN || t == InputType.MULTISELECT_DROPDOWN;
            responseOptionsField.setVisible(visible && needsOptions);
        }

        mentionAuthorCheck.addValueChangeListener(ev -> {
            mentionMessageField.setVisible(ev.getValue());
        });

        Checkbox mentionPrevCheck = new Checkbox("Mencionar autor anterior");
        com.vaadin.flow.component.textfield.TextArea mentionPrevMessageField = new com.vaadin.flow.component.textfield.TextArea("Mensagem da menção anterior");
        mentionPrevMessageField.setWidthFull();
        mentionPrevMessageField.setPlaceholder("Ex: Olá @autor_anterior, favor verificar...");
        mentionPrevMessageField.setVisible(false);
        mentionPrevMessageField.getElement().setAttribute("mention-id", "mention-prev-message-input");

        mentionPrevCheck.addValueChangeListener(ev -> {
            mentionPrevMessageField.setVisible(ev.getValue());
        });

        if (existingDto != null) {
            mentionPrevCheck.setValue(existingDto.mentionPreviousActor);
            mentionPrevMessageField.setValue(existingDto.mentionPreviousActorMessage != null ? existingDto.mentionPreviousActorMessage : "");
            mentionPrevMessageField.setVisible(existingDto.mentionPreviousActor);
        }

        MultiSelectComboBox<String> nextStepsCombo = new MultiSelectComboBox<>("Próximas Etapas Permitidas");
        nextStepsCombo.setItems(steps.stream().map(s -> s.label).collect(Collectors.toList()));
        
        if (existingDto != null && existingDto.allowedNextStepLabels != null) {
            nextStepsCombo.setValue(new HashSet<>(existingDto.allowedNextStepLabels));
        }
        nextStepsCombo.setWidthFull();
        nextStepsCombo.setHelperText("Se vazio, todas as etapas serão permitidas.");

        setupMentionLogic(mentionMessageField.getElement(), "mention-message-input");
        setupMentionLogic(mentionPrevMessageField.getElement(), "mention-prev-message-input");

        VerticalLayout layout = new VerticalLayout(labelField, colorCombo, finalizerCheck, globalCheck, shouldPulseCheck,
                mentionAuthorCheck, mentionMessageField, 
                mentionPrevCheck, mentionPrevMessageField,
                hasResponseCheck, responseRequiredCheck, responseLabelField, responseInputTypeCombo, responseOptionsField, responseIsCompactCheck,
                nextStepsCombo, suggestionWrapper);
        dialog.add(layout);

        Button saveBtn = new Button("Salvar", e -> {
            if (labelField.isEmpty() || colorCombo.isEmpty()) {
                Notification.show("Rótulo e Tema são obrigatórios");
                return;
            }
            StepDTO dto = existingDto != null ? existingDto : new StepDTO();
            dto.label = labelField.getValue();
            dto.colorTheme = colorCombo.getValue();
            dto.isFinalizer = finalizerCheck.getValue();
            dto.isGlobal = globalCheck.getValue();
            dto.shouldPulse = shouldPulseCheck.getValue();
            dto.mentionAuthor = mentionAuthorCheck.getValue();
            dto.mentionMessage = mentionMessageField.getValue();
            dto.mentionPreviousActor = mentionPrevCheck.getValue();
            dto.mentionPreviousActorMessage = mentionPrevMessageField.getValue();
            dto.hasResponseField = hasResponseCheck.getValue();
            dto.isResponseRequired = responseRequiredCheck.getValue();
            dto.responseFieldLabel = responseLabelField.getValue();
            dto.responseIsCompact = responseIsCompactCheck.getValue();
            dto.responseInputType = responseInputTypeCombo.getValue();
            if (responseOptionsField.isVisible() && !responseOptionsField.isEmpty()) {
                dto.responseOptions = List.of(responseOptionsField.getValue().split("\\s*,\\s*"));
            } else {
                dto.responseOptions = new ArrayList<>();
            }
            dto.allowedNextStepLabels = new ArrayList<>(nextStepsCombo.getValue());
            
            if (existingDto == null) {
                steps.add(dto);
            }
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
            step.getStyle().set("color", "#dc6d00");
        } else if ("green".equals(colorScheme)) {
            step.getStyle().set("background-color", "#e8f5e9");
            step.getStyle().set("border", "1px solid #a5d6a7");
            step.getStyle().set("color", "#2e7d32");
        } else if ("red".equals(colorScheme)) {
            step.getStyle().set("background-color", "#ffebee");
            step.getStyle().set("border", "1px solid #ef9a9a");
            step.getStyle().set("color", "#c62828");
        } else if ("purple".equals(colorScheme)) {
            step.getStyle().set("background-color", "#f3e5f5");
            step.getStyle().set("border", "1px solid #ce93d8");
            step.getStyle().set("color", "#7b1fa2");
        } else if ("orange".equals(colorScheme)) {
            step.getStyle().set("background-color", "#fff3e0");
            step.getStyle().set("border", "1px solid #ffcc80");
            step.getStyle().set("color", "#ef6c00");
        } else if ("cyan".equals(colorScheme)) {
            step.getStyle().set("background-color", "#e0f7fa");
            step.getStyle().set("border", "1px solid #80deea");
            step.getStyle().set("color", "#00838f");
        } else if ("pink".equals(colorScheme)) {
            step.getStyle().set("background-color", "#fce4ec");
            step.getStyle().set("border", "1px solid #f48fb1");
            step.getStyle().set("color", "#c2185b");
        } else if ("gray".equals(colorScheme)) {
            step.getStyle().set("background-color", "#f5f5f5");
            step.getStyle().set("border", "1px solid #e0e0e0");
            step.getStyle().set("color", "#616161");
        } else if ("black".equals(colorScheme)) {
            step.getStyle().set("background-color", "#212121");
            step.getStyle().set("border", "1px solid #000000");
            step.getStyle().set("color", "#ffffff");
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
            getUI().ifPresent(ui -> ui.navigate(TaskTemplateListView.class));
        });
        
        footer.add(saveBtn, cancelBtn);
        return footer;
    }

    private void saveTemplate() {
        TaskTemplate t;
        if (editingTemplateId != null) {
            t = templateService.getTemplateById(editingTemplateId);
            if (t == null) t = new TaskTemplate();
        } else {
            t = new TaskTemplate();
        }

        t.setName(nameField.getValue());
        t.setShowSendButton(showSendButton.getValue());
        t.setRequiresSignature(requireSignature.getValue());
        
        if (t.isRequiresSignature()) {
            if (authorizingUsersCombo.getValue().isEmpty()) {
                Notification.show("Selecione pelo menos um autorizador", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            boolean hasAuthorize = steps.stream().anyMatch(s -> s.label.equalsIgnoreCase("AUTORIZADO"));
            boolean hasReject = steps.stream().anyMatch(s -> s.label.equalsIgnoreCase("REJEITADO"));
            
            if (!hasAuthorize || !hasReject) {
                Notification.show("Templates com autorização exigem os passos 'AUTORIZADO' e 'REJEITADO'", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
        }
        
        t.getAuthorizingUsers().clear();
        t.getAuthorizingUsers().addAll(authorizingUsersCombo.getValue());
        
        t.setPlayAlertSound(playAlertSoundUI.getValue());
        t.getGroups().clear();
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

            if (dto.options != null && !dto.options.isEmpty()) {
                for (String optStr : dto.options) {
                    TaskFieldOption option = new TaskFieldOption();
                    option.setOptionLabel(optStr.trim());
                    option.setOptionValue(optStr.trim());
                    def.addTaskFieldOption(option);
                }
            }

            TemplateFieldRelation rel = new TemplateFieldRelation();
            rel.setTaskFieldDefinition(def);
            rel.setRequired(dto.isRequired);
            rel.setOrder(fOrder++);
            fieldRelations.add(rel);
        }

        List<TemplateStepRelation> stepRelations = new ArrayList<>();
        int sOrder = 1;

        // Primeiro passo: criar todos os TaskStepDefinition e mapeá-los para uso posterior
        Map<String, TaskStepDefinition> defMap = new HashMap<>();
        for (StepDTO dto : steps) {
            TaskStepDefinition def = new TaskStepDefinition();
            def.setLabel(dto.label);
            def.setColorTheme(dto.colorTheme);
            defMap.put(dto.label, def);
        }

        // Segundo passo: criar as relações e associar as próximas etapas permitidas
        for (StepDTO dto : steps) {
            TaskStepDefinition def = defMap.get(dto.label);
            TemplateStepRelation rel = new TemplateStepRelation();
            rel.setTaskStepDefinition(def);
            rel.setOrder(sOrder++);
            rel.setFinalizer(dto.isFinalizer);
            rel.setGlobal(dto.isGlobal);
            rel.setShouldPulse(dto.shouldPulse);
            rel.setMentionAuthor(dto.mentionAuthor);
            rel.setMentionMessage(dto.mentionMessage);
            rel.setMentionPreviousActor(dto.mentionPreviousActor);
            rel.setMentionPreviousActorMessage(dto.mentionPreviousActorMessage);
            rel.setHasResponseField(dto.hasResponseField);
            rel.setResponseRequired(dto.isResponseRequired);
            rel.setResponseFieldLabel(dto.responseFieldLabel);
            rel.setResponseIsCompact(dto.responseIsCompact);
            rel.setResponseInputType(dto.responseInputType);
            
            if (dto.responseOptions != null && !dto.responseOptions.isEmpty()) {
                for (String optStr : dto.responseOptions) {
                    TaskFieldOption opt = new TaskFieldOption();
                    opt.setOptionLabel(optStr.trim());
                    opt.setOptionValue(optStr.trim());
                    rel.addResponseOption(opt);
                }
            }

            if (dto.allowedNextStepLabels != null && !dto.allowedNextStepLabels.isEmpty()) {
                for (String nextLabel : dto.allowedNextStepLabels) {
                    TaskStepDefinition nextDef = defMap.get(nextLabel);
                    if (nextDef != null) {
                        rel.getAllowedNextSteps().add(nextDef);
                    }
                }
            }
            stepRelations.add(rel);
        }

        templateService.saveTemplate(t, fieldRelations, stepRelations);
        Notification.show("Template Salvo com Sucesso!");
        getUI().ifPresent(ui -> ui.navigate(TaskTemplateListView.class));
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long parameter) {
        if (parameter != null) {
            this.editingTemplateId = parameter;
            TaskTemplate t = templateService.getTemplateById(parameter);
            if (t != null) {
                nameField.setValue(t.getName());
                showSendButton.setValue(t.isShowSendButton());
                requireSignature.setValue(t.isRequiresSignature());
                authorizingUsersCombo.setValue(new HashSet<>(t.getAuthorizingUsers()));
                authorizingUsersCombo.setVisible(t.isRequiresSignature());
                playAlertSoundUI.setValue(t.isPlayAlertSound());
                groupComboBox.setValue(t.getGroups());

                fields.clear();
                for (TemplateFieldRelation fr : templateService.getFieldRelationsByTemplateId(parameter)) {
                    FieldDTO dto = new FieldDTO();
                    dto.label = fr.getTaskFieldDefinition().getLabel();
                    dto.inputType = fr.getTaskFieldDefinition().getInputType();
                    dto.description = fr.getTaskFieldDefinition().getDescription();
                    dto.isRequired = fr.isRequired();
                    if (fr.getTaskFieldDefinition().getTaskFieldOptions() != null) {
                        for (TaskFieldOption opt : fr.getTaskFieldDefinition().getTaskFieldOptions()) {
                            dto.options.add(opt.getOptionLabel());
                        }
                    }
                    fields.add(dto);
                }
                fieldsGrid.getDataProvider().refreshAll();

                steps.clear();
                for (TemplateStepRelation sr : templateService.getStepRelationsByTemplateId(parameter)) {
                    StepDTO dto = new StepDTO();
                    dto.label = sr.getTaskStepDefinition().getLabel();
                    dto.colorTheme = sr.getTaskStepDefinition().getColorTheme();
                    dto.isFinalizer = sr.isFinalizer();
                    dto.isGlobal = sr.isGlobal();
                    dto.shouldPulse = sr.isShouldPulse();
                    dto.mentionAuthor = sr.isMentionAuthor();
                    dto.mentionMessage = sr.getMentionMessage();
                    dto.mentionPreviousActor = sr.isMentionPreviousActor();
                    dto.mentionPreviousActorMessage = sr.getMentionPreviousActorMessage();
                    dto.hasResponseField = sr.isHasResponseField();
                    dto.isResponseRequired = sr.isResponseRequired();
                    dto.responseFieldLabel = sr.getResponseFieldLabel();
                    dto.responseIsCompact = sr.isResponseIsCompact();
                    dto.responseInputType = sr.getResponseInputType();
                    if (sr.getResponseOptions() != null) {
                        dto.responseOptions = sr.getResponseOptions().stream()
                                .map(TaskFieldOption::getOptionLabel)
                                .collect(Collectors.toList());
                    }
                    dto.allowedNextStepLabels = sr.getAllowedNextSteps().stream()
                            .map(TaskStepDefinition::getLabel)
                            .collect(Collectors.toList());
                    steps.add(dto);
                }
                stepsGrid.getDataProvider().refreshAll();
                updateWorkflowPreview();
            }
        }
    }
}
