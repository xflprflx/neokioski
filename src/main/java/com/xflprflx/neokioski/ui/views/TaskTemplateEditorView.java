package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.Component;
import com.xflprflx.neokioski.models.enums.TaskSlaUnit;
import com.xflprflx.neokioski.models.template.*;
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
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.IntegerField;
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
import com.xflprflx.neokioski.services.SoundService;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.ui.utils.TaskUIUtils;
import jakarta.annotation.security.RolesAllowed;

import java.util.*;
import java.util.stream.Collectors;

@RolesAllowed("ADMIN")
@Route(value = "task-template-editor", layout = MainLayout.class)
@PageTitle("Editor de Template de Tarefa | NeoKioski")
/**
 * View avançada de edição em formulário utilizada para criar e configurar detalhadamente os templates das tarefas.
 */
public class TaskTemplateEditorView extends VerticalLayout implements HasUrlParameter<Long> {

    private Long editingTemplateId = null;

    // UI Top Components
    private TextField nameField = new TextField("Nome do Template");
    private MultiSelectComboBox<Group> groupComboBox = new MultiSelectComboBox<>("Grupos");
    private Checkbox showSendButton = new Checkbox(true);
    private Checkbox requireSignature = new Checkbox();
    private MultiSelectComboBox<User> authorizingUsersCombo = new MultiSelectComboBox<>("Usuários Autorizadores");
    private Checkbox playAlertSoundUI = new Checkbox();
    private ComboBox<String> globalAlertSoundCombo = new ComboBox<>();
    private Button playGlobalSoundBtn = new Button(VaadinIcon.PLAY.create());
    private MultiSelectComboBox<Role> globalAlertRolesCombo = new MultiSelectComboBox<>("Perfis que ouvem (vazio = todos)");
    private ComboBox<Role> defaultResponsibleRoleCombo = new ComboBox<>("Setor Responsável Inicial");
    private Checkbox registraNoRelatorioLojaUI = new Checkbox();
    private IntegerField sortOrderField = new IntegerField("Ordem de Exibição");

    // Tabs
    private Tabs tabs = new Tabs();
    private Tab formFieldsTab = new Tab("Campos do Formulário");
    private Tab workflowTab = new Tab("Fluxo de Trabalho");
    private Tab alertsTab = new Tab("Alertas");
    private Tab slasTab = new Tab("SLA (Metas)");
    private Tab mentionsTab = new Tab("Menções Dinâmicas");
    private VerticalLayout tabContent = new VerticalLayout();
    private VerticalLayout workflowPreview = new VerticalLayout();
    private VerticalLayout chatLayout = new VerticalLayout();
    private Scroller chatScroller = new Scroller();
    
    private boolean isSimulatedSent = false;
    private boolean isSimulatedFinalized = false;
    private String simulatedCurrentStepLabel = null;
    private List<String> simulatedLog = new ArrayList<>();
    
    private Map<String, Component> requestFields = new HashMap<>();
    private Map<String, Component> stepResponseFields = new HashMap<>();

    // Sugestões de menção
    private Div suggestionWrapper;
    private ListBox<User> suggestionList;
    private String activeMentionId = "";

    // DTOs para Grid dinâmico antes de Salvar no BD
    public static class FieldDTO {
        public Long id;
        public Long relationId;
        public String label;
        public InputType inputType;
        public boolean isRequired;
        public String description;
        public List<String> options = new ArrayList<>();
    }

    public static class StepDTO {
        public Long id;
        public Long relationId;
        public String label;
        public String colorTheme;
        public boolean isFinalizer;
        public boolean sendNotification;
        public String notificationMessage;
        public List<String> allowedNextStepLabels = new ArrayList<>();
        public boolean isGlobal;
        public boolean shouldPulse;
        public boolean hasResponseField;
        public boolean isResponseRequired;
        public String responseFieldLabel;
        public boolean responseIsCompact;
        public InputType responseInputType = InputType.TEXT;
        public List<String> responseOptions = new ArrayList<>();
        public String alertSound;
        public Set<Role> alertRoles = new HashSet<>();
        public Role responsibleRole;
    }

    public static class AlertDTO {
        public Long id;
        public String startStepLabel;
        public String targetStepLabel;
        public int timeoutMinutes;
        public String alertMessage;
        public String alertSound;
        public Set<Role> alertRoles = new HashSet<>();
    }

    public static class SlaDTO {
        public Long id;
        public String name;
        public String sourceStepLabel;
        public String targetStepLabel;
        public int targetValue;
        public TaskSlaUnit unit = TaskSlaUnit.MINUTES;
        public java.time.Instant validFrom = java.time.Instant.now();
        public String justificationOptions;
    }
    private List<FieldDTO> fields = new ArrayList<>();
    private List<StepDTO> steps = new ArrayList<>();
    private List<AlertDTO> alerts = new ArrayList<>();
    private List<SlaDTO> slas = new ArrayList<>();
    private List<TaskTemplateMention> mentionMappings = new ArrayList<>();


    private Grid<FieldDTO> fieldsGrid = new Grid<>(FieldDTO.class, false);
    private Grid<StepDTO> stepsGrid = new Grid<>(StepDTO.class, false);
    private Grid<AlertDTO> alertsGrid = new Grid<>(AlertDTO.class, false);
    private Grid<SlaDTO> slasGrid = new Grid<>(SlaDTO.class, false);
    private Grid<Group> mentionsGrid = new Grid<>(Group.class, false);

    private FieldDTO draggedField;
    private StepDTO draggedStep;


    private final TaskTemplateService templateService;
    private final GroupService groupService;
    private final UserService userService;
    private final SoundService soundService;

    public TaskTemplateEditorView(TaskTemplateService templateService, GroupService groupService, UserService userService, SoundService soundService) {
        this.templateService = templateService;
        this.groupService = groupService;
        this.userService = userService;
        this.soundService = soundService;

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
        updateCardsPreview();

        mainContent.add(leftSide, workflowPreview);
        mainContent.setFlexGrow(3, leftSide);
        mainContent.setFlexGrow(1, workflowPreview);

        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(title, mainContent);

        configureSuggestions();
        showFormFieldsTab(); // Inicia com aba de campos
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureSuggestions() {
        suggestionList = new ListBox<>();
        suggestionList.setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(user -> {
            Span name = new Span(user.getName());
            Span username = new Span("@" + user.getUsername());
            username.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            username.getStyle().set("color", "var(--lumo-secondary-text-color)");
            username.getStyle().set("margin-left", "8px");
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return new HorizontalLayout(name, username);
        }));

        suggestionList.addValueChangeListener(event -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
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
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void showSuggestions(String query) {
        List<User> suggestions = userService.findAll().stream()
                .filter(u -> u.getUsername().toLowerCase().contains(query.toLowerCase()) || 
                             u.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        // Adicionar sugestão virtual para o Criador da Tarefa
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if ("criador".contains(query.toLowerCase()) || "@criador".contains(query.toLowerCase())) {
            User virtualCreator = new User();
            virtualCreator.setName("Criador da Tarefa (Original)");
            virtualCreator.setUsername("criador");
            suggestions.add(0, virtualCreator);
        }
        
        // Adicionar sugestão virtual para os Autorizadores
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if ("autorizadores".contains(query.toLowerCase()) || "@autorizadores".contains(query.toLowerCase())) {
            User virtualAuts = new User();
            virtualAuts.setName("Usuários Autorizadores");
            virtualAuts.setUsername("autorizadores");
            suggestions.add(0, virtualAuts);
        }

        // Adicionar sugestão virtual para o Usuário Atual (Executante)
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if ("usuario".contains(query.toLowerCase()) || "@usuario".contains(query.toLowerCase())) {
            User virtualUser = new User();
            virtualUser.setName("Usuário Atual (Executante)");
            virtualUser.setUsername("usuario");
            suggestions.add(0, virtualUser);
        }
        
        // Adicionar sugestão virtual para o Autor Anterior
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if ("autor_anterior".contains(query.toLowerCase()) || "@autor_anterior".contains(query.toLowerCase())) {
            User virtualPrev = new User();
            virtualPrev.setName("Autor Anterior");
            virtualPrev.setUsername("autor_anterior");
            suggestions.add(suggestions.size() > 1 ? 2 : suggestions.size(), virtualPrev);
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void hideSuggestions() {
        suggestionWrapper.setVisible(false);
    }

    @com.vaadin.flow.component.ClientCallable
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void setActiveTargetById(String mentionId) {
        this.activeMentionId = mentionId;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component createTopConfiguration() {
        HorizontalLayout firstRow = new HorizontalLayout();
        firstRow.setWidthFull();
        
        nameField.setWidthFull();
        groupComboBox.setWidthFull();
        groupComboBox.setItems(groupService.findAll(""));
        groupComboBox.setItemLabelGenerator(Group::getName);

        sortOrderField.setWidth("150px");
        sortOrderField.setTooltipText("Define a ordem de exibição (menor para maior)");

        firstRow.add(nameField, groupComboBox, sortOrderField);

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
        Span playAlertSoundText = new Span("Tocar som na criação?");
        
        globalAlertSoundCombo.setPlaceholder("Selecione o som...");
        globalAlertSoundCombo.setItems(soundService.getAvailableAlertSounds());
        globalAlertSoundCombo.setClearButtonVisible(true);
        globalAlertSoundCombo.setVisible(false);
        playGlobalSoundBtn.setVisible(false);
        playGlobalSoundBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);

        globalAlertRolesCombo.setVisible(false);
        globalAlertRolesCombo.setItems(Role.values());
        globalAlertRolesCombo.setItemLabelGenerator(Role::getDescription);

        playAlertSoundUI.addValueChangeListener(e -> {
            globalAlertSoundCombo.setVisible(e.getValue());
            playGlobalSoundBtn.setVisible(e.getValue());
            globalAlertRolesCombo.setVisible(e.getValue());
        });

        playGlobalSoundBtn.addClickListener(e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (globalAlertSoundCombo.getValue() != null) {
                getElement().executeJs("if(window.playAlertSound) { window.playAlertSound($0); }", globalAlertSoundCombo.getValue());
            }
        });

        playAlertSoundLayout.add(playAlertSoundUI, playAlertSoundText, globalAlertSoundCombo, playGlobalSoundBtn, globalAlertRolesCombo);

        HorizontalLayout registraRelatorioLayout = new HorizontalLayout();
        registraRelatorioLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        Span registraRelatorioText = new Span("Registra no relatório de loja?");
        registraRelatorioLayout.add(registraNoRelatorioLojaUI, registraRelatorioText);

        defaultResponsibleRoleCombo.setItems(Role.values());
        defaultResponsibleRoleCombo.setItemLabelGenerator(Role::getDescription);
        defaultResponsibleRoleCombo.setClearButtonVisible(true);
        defaultResponsibleRoleCombo.setWidthFull();

        secondRow.add(showSendBtnLayout, reqSignatureLayout, authorizingUsersCombo, playAlertSoundLayout, registraRelatorioLayout, defaultResponsibleRoleCombo);

        // Listeners para atualizar o preview
        nameField.addValueChangeListener(e -> updateCardsPreview());
        showSendButton.addValueChangeListener(e -> updateCardsPreview());
        requireSignature.addValueChangeListener(e -> updateCardsPreview());

        VerticalLayout configLayout = new VerticalLayout(firstRow, secondRow);
        configLayout.setPadding(false);
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return configLayout;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component createTabs() {
        tabs.add(formFieldsTab, workflowTab, alertsTab, slasTab, mentionsTab);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (event.getSelectedTab().equals(formFieldsTab)) {
                showFormFieldsTab();
            } else if (event.getSelectedTab().equals(workflowTab)) {
                showWorkflowTab();
            } else if (event.getSelectedTab().equals(alertsTab)) {
                showAlertsTab();
            } else if (event.getSelectedTab().equals(mentionsTab)) {
                showMentionsTab();
            } else {
                showSlaTab();
            }
        });
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return tabs;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void showSlaTab() {
        tabContent.removeAll();
        
        Button addSlaBtn = new Button("Adicionar Nova Meta de SLA");
        addSlaBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addSlaBtn.addClickListener(e -> openSlaDialog());

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (slasGrid.getColumns().isEmpty()) {
            slasGrid.addColumn(dto -> dto.name).setHeader("Nome da Meta");
            slasGrid.addColumn(dto -> dto.sourceStepLabel != null && !dto.sourceStepLabel.isBlank() ? dto.sourceStepLabel : "Criação").setHeader("Início");
            slasGrid.addColumn(dto -> dto.targetStepLabel != null && !dto.targetStepLabel.isBlank() ? dto.targetStepLabel : "Finalização").setHeader("Alvo");
            slasGrid.addColumn(dto -> dto.targetValue + " " + dto.unit.getLabel()).setHeader("Meta");
            slasGrid.addComponentColumn(item -> {
                Button editBtn = new Button(VaadinIcon.EDIT.create());
                editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                editBtn.addClickListener(e -> openSlaDialog(item));

                Button deleteBtn = new Button(VaadinIcon.TRASH.create());
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                deleteBtn.addClickListener(e -> {
                    slas.remove(item);
                    slasGrid.getDataProvider().refreshAll();
                });
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
                return new HorizontalLayout(editBtn, deleteBtn);
            }).setHeader("Ações");
            slasGrid.setItems(slas);
        }

        tabContent.add(addSlaBtn, slasGrid);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void openSlaDialog() {
        openSlaDialog(null);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void openSlaDialog(SlaDTO existingDto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existingDto == null ? "Nova Meta de SLA" : "Editar Meta");

        TextField nameField = new TextField("Nome da Meta (ex: Tempo de Atendimento)");
        nameField.setWidthFull();

        ComboBox<String> startStepCombo = new ComboBox<>("Etapa de Início (Opcional)");
        startStepCombo.setItems(steps.stream().map(s -> s.label).collect(Collectors.toList()));
        startStepCombo.setWidthFull();
        startStepCombo.setClearButtonVisible(true);
        startStepCombo.setHelperText("Se vazio, conta a partir da criação da solicitação.");

        ComboBox<String> targetStepCombo = new ComboBox<>("Etapa Alvo (Opcional)");
        targetStepCombo.setItems(steps.stream().map(s -> s.label).collect(Collectors.toList()));
        targetStepCombo.setWidthFull();
        targetStepCombo.setClearButtonVisible(true);
        targetStepCombo.setHelperText("Se vazio, conta até o encerramento da tarefa.");

        TextField timeoutField = new TextField("Meta de Tempo");
        timeoutField.setWidth("120px");

        ComboBox<TaskSlaUnit> unitCombo = new ComboBox<>("Unidade");
        unitCombo.setItems(TaskSlaUnit.values());
        unitCombo.setItemLabelGenerator(TaskSlaUnit::getLabel);
        unitCombo.setWidth("150px");
        unitCombo.setValue(TaskSlaUnit.MINUTES);

        HorizontalLayout timeLayout = new HorizontalLayout(timeoutField, unitCombo);
        timeLayout.setWidthFull();

        com.vaadin.flow.component.textfield.TextArea justificationOptionsField = new com.vaadin.flow.component.textfield.TextArea("Opções de Justificativa (separadas por vírgula)");
        justificationOptionsField.setWidthFull();
        justificationOptionsField.setHelperText("Ex: Falta de Pessoal, Problema Técnico, Alta Demanda. Se preenchido, o sistema exigirá justificativa quando este SLA for descumprido.");
        justificationOptionsField.setPlaceholder("Deixe vazio para não exigir justificativa");

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (existingDto != null) {
            nameField.setValue(existingDto.name);
            startStepCombo.setValue(existingDto.sourceStepLabel);
            targetStepCombo.setValue(existingDto.targetStepLabel);
            timeoutField.setValue(String.valueOf(existingDto.targetValue));
            unitCombo.setValue(existingDto.unit);
            if (existingDto.justificationOptions != null) {
                justificationOptionsField.setValue(existingDto.justificationOptions);
            }
        }

        VerticalLayout layout = new VerticalLayout(nameField, startStepCombo, targetStepCombo, timeLayout, justificationOptionsField);
        dialog.add(layout);

        Button saveBtn = new Button("Salvar", e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (nameField.isEmpty() || timeoutField.isEmpty()) {
                Notification.show("Nome e Meta são obrigatórios");
                return;
            }
            /*
             * Inicia um bloco try-catch para evitar que erros internos quebrem a renderização da interface.
             */
            try {
                int timeout = Integer.parseInt(timeoutField.getValue());
                SlaDTO dto = (existingDto != null) ? existingDto : new SlaDTO();
                dto.name = nameField.getValue();
                dto.sourceStepLabel = startStepCombo.getValue();
                dto.targetStepLabel = targetStepCombo.getValue();
                dto.targetValue = timeout;
                dto.unit = unitCombo.getValue();
                dto.justificationOptions = justificationOptionsField.getValue();
                
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (existingDto == null) {
                    slas.add(dto);
                }
                
                slasGrid.getDataProvider().refreshAll();
                dialog.close();
            } catch (NumberFormatException ex) {
                Notification.show("Tempo deve ser um número");
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void showAlertsTab() {
        tabContent.removeAll();
        
        Button addAlertBtn = new Button("Adicionar Novo Alerta Temporal");
        addAlertBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addAlertBtn.addClickListener(e -> openAlertDialog());

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (alertsGrid.getColumns().isEmpty()) {
            alertsGrid.addColumn(dto -> dto.targetStepLabel).setHeader("Step Alvo");
            alertsGrid.addColumn(dto -> dto.timeoutMinutes).setHeader("Tempo (m)");
            alertsGrid.addColumn(dto -> dto.alertMessage).setHeader("Mensagem");
            alertsGrid.addComponentColumn(item -> {
                Button editBtn = new Button(VaadinIcon.EDIT.create());
                editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                editBtn.addClickListener(e -> openAlertDialog(item));

                Button deleteBtn = new Button(VaadinIcon.TRASH.create());
                deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
                deleteBtn.addClickListener(e -> {
                    alerts.remove(item);
                    alertsGrid.getDataProvider().refreshAll();
                });
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
                return new HorizontalLayout(editBtn, deleteBtn);
            }).setHeader("Ações");
            alertsGrid.setItems(alerts);
        }

        tabContent.add(addAlertBtn, alertsGrid);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void openAlertDialog() {
        openAlertDialog(null);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void openAlertDialog(AlertDTO existingDto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existingDto == null ? "Novo Alerta Temporal" : "Editar Alerta");

        ComboBox<String> startStepCombo = new ComboBox<>("Etapa Inicial / Gatilho (Opcional)");
        startStepCombo.setItems(steps.stream().map(s -> s.label).collect(Collectors.toList()));
        startStepCombo.setWidthFull();
        startStepCombo.setClearButtonVisible(true);
        startStepCombo.setHelperText("Se vazio, conta a partir da criação da solicitação.");

        ComboBox<String> stepCombo = new ComboBox<>("Etapa Alvo (Interrompe o Alerta)");
        stepCombo.setItems(steps.stream().map(s -> s.label).collect(Collectors.toList()));
        stepCombo.setWidthFull();

        TextField timeoutField = new TextField("Tempo Limite (minutos)");
        timeoutField.setWidthFull();

        com.vaadin.flow.component.textfield.TextArea messageField = new com.vaadin.flow.component.textfield.TextArea("Mensagem de Alerta");
        messageField.setWidthFull();
        messageField.setPlaceholder("Use @criador para mencionar o dono da tarefa.");
        messageField.getElement().setAttribute("mention-id", "alert-message-input");
        setupMentionLogic(messageField.getElement(), "alert-message-input");

        ComboBox<String> soundCombo = new ComboBox<>("Som de Alerta");
        soundCombo.setItems(soundService.getAvailableAlertSounds());
        soundCombo.setWidthFull();
        soundCombo.setClearButtonVisible(true);
        soundCombo.setPlaceholder("Sem som");

        Button playBtn = new Button(VaadinIcon.PLAY.create(), e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (soundCombo.getValue() != null) {
                getElement().executeJs("if(window.playAlertSound) { window.playAlertSound($0); }", soundCombo.getValue());
            }
        });
        playBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);

        MultiSelectComboBox<Role> rolesCombo = new MultiSelectComboBox<>("Perfis que ouvem (vazio = todos)");
        rolesCombo.setItems(Role.values());
        rolesCombo.setItemLabelGenerator(Role::getDescription);
        rolesCombo.setWidthFull();

        HorizontalLayout soundLayout = new HorizontalLayout(soundCombo, playBtn, rolesCombo);
        soundLayout.setWidthFull();
        soundLayout.setAlignItems(FlexComponent.Alignment.END);

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (existingDto != null) {
            startStepCombo.setValue(existingDto.startStepLabel);
            stepCombo.setValue(existingDto.targetStepLabel);
            timeoutField.setValue(String.valueOf(existingDto.timeoutMinutes));
            messageField.setValue(existingDto.alertMessage != null ? existingDto.alertMessage : "");
            soundCombo.setValue(existingDto.alertSound);
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (existingDto.alertRoles != null) rolesCombo.setValue(existingDto.alertRoles);
        }

        VerticalLayout layout = new VerticalLayout(startStepCombo, stepCombo, timeoutField, messageField, soundLayout, suggestionWrapper);
        dialog.add(layout);

        Button saveBtn = new Button("Salvar", e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (stepCombo.isEmpty() || timeoutField.isEmpty()) {
                Notification.show("Etapa Alvo e Tempo são obrigatórios");
                return;
            }
            /*
             * Inicia um bloco try-catch para evitar que erros internos quebrem a renderização da interface.
             */
            try {
                int timeout = Integer.parseInt(timeoutField.getValue());
                AlertDTO dto = (existingDto != null) ? existingDto : new AlertDTO();
                dto.startStepLabel = startStepCombo.getValue();
                dto.targetStepLabel = stepCombo.getValue();
                dto.timeoutMinutes = timeout;
                dto.alertMessage = messageField.getValue();
                dto.alertSound = soundCombo.getValue();
                dto.alertRoles = new HashSet<>(rolesCombo.getValue());
                
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (existingDto == null) {
                    alerts.add(dto);
                }
                
                
                alertsGrid.getDataProvider().refreshAll();
                updateCardsPreview();
                dialog.close();
            } catch (NumberFormatException ex) {
                Notification.show("Tempo deve ser um número");
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void showFormFieldsTab() {
        tabContent.removeAll();
        
        Button addFieldBtn = new Button("Adicionar Novo Campo");
        addFieldBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addFieldBtn.addClickListener(e -> openFieldDialog());

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (fieldsGrid.getColumns().isEmpty()) {
            fieldsGrid.addComponentColumn(item -> {
                Span icon = new Span(VaadinIcon.MENU.create());
                icon.getStyle().set("cursor", "move");
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
                return icon;
            }).setHeader("Reordenar").setFlexGrow(0).setWidth("100px");
            fieldsGrid.addColumn(dto -> dto.label).setHeader("Rótulo do Campo");
            fieldsGrid.addColumn(dto -> dto.inputType != null ? dto.inputType.name() : "").setHeader("Tipo de Entrada");
            fieldsGrid.addComponentColumn(dto -> {
                Checkbox cb = new Checkbox();
                cb.setValue(dto.isRequired);
                cb.setReadOnly(true);
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
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
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
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

                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (draggedField != null && targetField != null && !draggedField.equals(targetField)) {
                    fields.remove(draggedField);
                    int targetIndex = fields.indexOf(targetField);
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (dropLocation == GridDropLocation.BELOW) {
                        targetIndex++;
                    }
                    fields.add(targetIndex, draggedField);
                    fieldsGrid.getDataProvider().refreshAll();
                    updateCardsPreview();
                }
            });

            fieldsGrid.addDragEndListener(e -> draggedField = null);
        }


        tabContent.add(addFieldBtn, fieldsGrid);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void openFieldDialog() {
        openFieldDialog(null);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (existingDto != null) {
            labelField.setValue(existingDto.label != null ? existingDto.label : "");
            typeCombo.setValue(existingDto.inputType);
            requiredCheck.setValue(existingDto.isRequired);
            descField.setValue(existingDto.description != null ? existingDto.description : "");
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (existingDto.options != null && !existingDto.options.isEmpty()) {
                optionsField.setValue(String.join(", ", existingDto.options));
            }
        }

        VerticalLayout layout = new VerticalLayout(labelField, typeCombo, optionsField, requiredCheck, descField);
        dialog.add(layout);

        Button saveBtn = new Button("Salvar", e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (labelField.isEmpty() || typeCombo.isEmpty()) {
                Notification.show("Rótulo e Tipo são obrigatórios");
                return;
            }
            FieldDTO dto = (existingDto != null) ? existingDto : new FieldDTO();
            dto.label = labelField.getValue();
            dto.inputType = typeCombo.getValue();
            dto.isRequired = requiredCheck.getValue();
            dto.description = descField.getValue();
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (optionsField.isVisible() && !optionsField.isEmpty()) {
                dto.options = List.of(optionsField.getValue().split("\\s*,\\s*"));
            } else {
                dto.options = new ArrayList<>();
            }
            
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (existingDto == null) {
                fields.add(dto);
            }
            
            
            fieldsGrid.getDataProvider().refreshAll();
            updateCardsPreview();
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void showWorkflowTab() {
        tabContent.removeAll();
        
        Button addStepBtn = new Button("Adicionar Nova Etapa");
        addStepBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addStepBtn.addClickListener(e -> openStepDialog());

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (stepsGrid.getColumns().isEmpty()) {
            stepsGrid.addComponentColumn(item -> {
                Span icon = new Span(VaadinIcon.MENU.create());
                icon.getStyle().set("cursor", "move");
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
                return icon;
            }).setHeader("Reordenar").setFlexGrow(0).setWidth("100px");
            stepsGrid.addColumn(dto -> dto.label).setHeader("Rótulo da Etapa");
            stepsGrid.addColumn(dto -> dto.colorTheme).setHeader("Cor/Tema");
            stepsGrid.addComponentColumn(dto -> {
                Checkbox cb = new Checkbox();
                cb.setValue(dto.isFinalizer);
                cb.setReadOnly(true);
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
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
                    updateCardsPreview();
                });
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
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

                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (draggedStep != null && targetStep != null && !draggedStep.equals(targetStep)) {
                    steps.remove(draggedStep);
                    int targetIndex = steps.indexOf(targetStep);
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (dropLocation == GridDropLocation.BELOW) {
                        targetIndex++;
                    }
                    steps.add(targetIndex, draggedStep);
                    stepsGrid.getDataProvider().refreshAll();
                    updateCardsPreview();
                }
            });

            stepsGrid.addDragEndListener(e -> draggedStep = null);
        }


        tabContent.add(addStepBtn, stepsGrid);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void openStepDialog() {
        openStepDialog(null);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void openStepDialog(StepDTO existingDto) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existingDto == null ? "Nova Etapa" : "Editar Etapa");

        TextField labelField = new TextField("Rótulo da Etapa");
        ComboBox<String> colorCombo = new ComboBox<>("Tema", List.of("blue", "yellow", "green", "red", "purple", "orange", "cyan", "pink", "gray", "black"));
        Checkbox finalizerCheck = new Checkbox("É Finalizador?");
        Checkbox globalCheck = new Checkbox("É Global?");
        Checkbox shouldPulseCheck = new Checkbox("Deve pulsar?");
        
        Checkbox mentionAuthorCheck = new Checkbox("Disparar Mensagem de Notificação / Menções");
        com.vaadin.flow.component.textfield.TextArea mentionMessageField = new com.vaadin.flow.component.textfield.TextArea("Corpo da Mensagem");
        mentionMessageField.setWidthFull();
        mentionMessageField.setPlaceholder("Use @criador, @autor_anterior, @usuario, @resposta...");
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
            
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (existingDto != null) {
            labelField.setValue(existingDto.label);
            colorCombo.setValue(existingDto.colorTheme);
            finalizerCheck.setValue(existingDto.isFinalizer);
            globalCheck.setValue(existingDto.isGlobal);
            shouldPulseCheck.setValue(existingDto.shouldPulse);
            mentionAuthorCheck.setValue(existingDto.sendNotification);
            mentionMessageField.setValue(existingDto.notificationMessage != null ? existingDto.notificationMessage : "");
            mentionMessageField.setVisible(existingDto.sendNotification);
            
            hasResponseCheck.setValue(existingDto.hasResponseField);
            responseRequiredCheck.setValue(existingDto.isResponseRequired);
            responseLabelField.setValue(existingDto.responseFieldLabel != null ? existingDto.responseFieldLabel : "Resposta / Comentário");
            responseIsCompactCheck.setValue(existingDto.responseIsCompact);
            responseInputTypeCombo.setValue(existingDto.responseInputType != null ? existingDto.responseInputType : InputType.TEXT);
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
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

        MultiSelectComboBox<String> nextStepsCombo = new MultiSelectComboBox<>("Próximas Etapas Permitidas");
        nextStepsCombo.setItems(steps.stream().map(s -> s.label).collect(Collectors.toList()));
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (existingDto != null && existingDto.allowedNextStepLabels != null) {
            nextStepsCombo.setValue(new HashSet<>(existingDto.allowedNextStepLabels));
        }
        nextStepsCombo.setWidthFull();
        nextStepsCombo.setHelperText("Se vazio, todas as etapas serão permitidas.");

        setupMentionLogic(mentionMessageField.getElement(), "mention-message-input");

        ComboBox<String> soundCombo = new ComboBox<>("Som ao entrar nesta etapa");
        soundCombo.setItems(soundService.getAvailableAlertSounds());
        soundCombo.setWidthFull();
        soundCombo.setClearButtonVisible(true);
        soundCombo.setPlaceholder("Sem som");

        Button playBtn = new Button(VaadinIcon.PLAY.create(), e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (soundCombo.getValue() != null) {
                getElement().executeJs("if(window.playAlertSound) { window.playAlertSound($0); }", soundCombo.getValue());
            }
        });
        playBtn.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);

        MultiSelectComboBox<Role> rolesCombo = new MultiSelectComboBox<>("Perfis que ouvem (vazio = todos)");
        rolesCombo.setItems(Role.values());
        rolesCombo.setItemLabelGenerator(Role::getDescription);
        rolesCombo.setWidthFull();

        HorizontalLayout soundLayout = new HorizontalLayout(soundCombo, playBtn, rolesCombo);
        soundLayout.setWidthFull();
        soundLayout.setAlignItems(FlexComponent.Alignment.END);

        ComboBox<Role> responsibleRoleCombo = new ComboBox<>("Setor Responsável");
        responsibleRoleCombo.setItems(Role.values());
        responsibleRoleCombo.setItemLabelGenerator(Role::getDescription);
        responsibleRoleCombo.setWidthFull();
        responsibleRoleCombo.setClearButtonVisible(true);

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (existingDto != null) {
            soundCombo.setValue(existingDto.alertSound);
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (existingDto.alertRoles != null) rolesCombo.setValue(existingDto.alertRoles);
            responsibleRoleCombo.setValue(existingDto.responsibleRole);
        }

        VerticalLayout layout = new VerticalLayout(
            labelField, colorCombo, globalCheck, finalizerCheck, shouldPulseCheck,
            responsibleRoleCombo,
            mentionAuthorCheck, mentionMessageField,
            hasResponseCheck, responseRequiredCheck, responseLabelField, responseInputTypeCombo, responseOptionsField, responseIsCompactCheck,
            soundLayout,
            nextStepsCombo, suggestionWrapper
        );
        dialog.add(layout);

        Button saveBtn = new Button("Salvar", e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
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
            dto.sendNotification = mentionAuthorCheck.getValue();
            dto.notificationMessage = mentionMessageField.getValue();
            dto.hasResponseField = hasResponseCheck.getValue();
            dto.isResponseRequired = responseRequiredCheck.getValue();
            dto.responseFieldLabel = responseLabelField.getValue();
            dto.responseIsCompact = responseIsCompactCheck.getValue();
            dto.responseInputType = responseInputTypeCombo.getValue();
            dto.alertSound = soundCombo.getValue();
            dto.alertRoles = new HashSet<>(rolesCombo.getValue());
            dto.responsibleRole = responsibleRoleCombo.getValue();
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (responseOptionsField.isVisible() && !responseOptionsField.isEmpty()) {
                dto.responseOptions = List.of(responseOptionsField.getValue().split("\\s*,\\s*"));
            } else {
                dto.responseOptions = new ArrayList<>();
            }
            dto.allowedNextStepLabels = new ArrayList<>(nextStepsCombo.getValue());
            
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (existingDto == null) {
                steps.add(dto);
            }
            stepsGrid.getDataProvider().refreshAll();
            updateCardsPreview();
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancelar", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void setupWorkflowPreviewLayout() {
        workflowPreview.setWidth("450px");
        workflowPreview.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        workflowPreview.getStyle().set("border-radius", "8px");
        workflowPreview.setPadding(false);
        workflowPreview.setSpacing(false);
        workflowPreview.getStyle().set("background", "var(--lumo-contrast-5pct)");
        workflowPreview.getStyle().set("overflow", "hidden");

        HorizontalLayout headerPreview = new HorizontalLayout();
        headerPreview.setWidthFull();
        headerPreview.setPadding(true);
        headerPreview.setAlignItems(FlexComponent.Alignment.CENTER);
        headerPreview.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerPreview.getStyle().set("background", "var(--lumo-base-color)");
        headerPreview.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        
        H3 previewTitle = new H3("Mini-Chat Preview");
        previewTitle.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-m)");
        
        Button resetBtn = new Button(VaadinIcon.REFRESH.create(), e -> {
            isSimulatedSent = false;
            isSimulatedFinalized = false;
            simulatedCurrentStepLabel = null;
            simulatedLog.clear();
            updateCardsPreview();
        });
        resetBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        resetBtn.setTooltipText("Limpar Chat e Reiniciar Simulação");
        
        headerPreview.add(previewTitle, resetBtn);
        
        chatLayout.setWidthFull();
        chatLayout.setPadding(true);
        chatLayout.setSpacing(true);
        
        chatScroller.setContent(chatLayout);
        chatScroller.setWidthFull();
        chatScroller.setHeightFull();
        
        workflowPreview.add(headerPreview, chatScroller);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void updateCardsPreview() {
        chatLayout.removeAll();

        // 1. O card de solicitação
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!isSimulatedSent) {
            chatLayout.add(createRequestCardPreview());
        } else {
            chatLayout.add(createRequestCardSnapshot());
            
            // 2. Mensagens do log (se houver)
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (String msg : simulatedLog) {
                chatLayout.add(createChatBubble("Sistema", msg));
            }

            // 3. O card da etapa atual
            chatLayout.add(createStepCardPreview(simulatedCurrentStepLabel));
        }

        chatLayout.getElement().executeJs("setTimeout(() => { this.scrollTop = this.scrollHeight; }, 50);");
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component createChatBubble(String user, String content) {
        Div container = new Div();
        container.setWidthFull();
        container.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "flex-start")
                .set("margin-bottom", "var(--lumo-space-s)");

        Span userLabel = new Span(user);
        userLabel.getStyle()
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("font-weight", "bold")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "8px");

        Div bubble = new Div();
        bubble.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "0 12px 12px 12px")
                .set("padding", "8px 12px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("max-width", "85%")
                .set("box-shadow", "var(--lumo-box-shadow-xs)");

        // Renderizar menções no conteúdo simulado
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("@\\w+").matcher(content);
        int lastPos = 0;
        while (m.find()) {
            bubble.add(new Span(content.substring(lastPos, m.start())));
            Span mention = new Span(m.group());
            mention.getStyle()
                    .set("color", "var(--lumo-primary-text-color)")
                    .set("font-weight", "bold")
                    .set("background", "var(--lumo-primary-color-10pct)")
                    .set("padding", "0 4px")
                    .set("border-radius", "4px");
            bubble.add(mention);
            lastPos = m.end();
        }
        bubble.add(new Span(content.substring(lastPos)));

        container.add(userLabel, bubble);
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return container;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component createRequestCardPreview() {
        requestFields.clear();
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle().set("background-color", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (isSimulatedSent) {
            card.getStyle().set("opacity", "0.6");
        }

        String tName = nameField.getValue() != null && !nameField.getValue().isEmpty() ? nameField.getValue() : "Nome do Template";
        String emoji = TaskUIUtils.resolveTemplateEmoji(tName);
        Span title = new Span(emoji + " " + tName);
        title.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)");
        card.add(title);

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (FieldDTO f : fields) {
            Component input = createInputComponentPreview(f.inputType, f.label, f.description, f.isRequired, f.options, "");
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (isSimulatedSent && input instanceof com.vaadin.flow.component.HasEnabled he) {
                he.setEnabled(false);
            }
            requestFields.put(f.label, input);
            card.add(input);
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!isSimulatedSent) {
            Button submitBtn = new Button("Enviar (Simulação)", e -> {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (steps.isEmpty()) {
                    Notification.show("Defina ao menos uma etapa para simular o envio.");
                    return;
                }

                // Validações Reais
                /*
                 * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
                 */
                for (FieldDTO f : fields) {
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (f.isRequired) {
                        Component comp = requestFields.get(f.label);
                        String val = extractFieldValue(comp);
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (val == null || val.isBlank()) {
                            /*
                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                             */
                            if (comp instanceof com.vaadin.flow.component.HasValidation hv) {
                                hv.setInvalid(true);
                                hv.setErrorMessage("Este campo é obrigatório.");
                            }
                            Notification.show("O campo '" + f.label + "' é obrigatório.");
                            return;
                        } else if (comp instanceof com.vaadin.flow.component.HasValidation hv) {
                            hv.setInvalid(false);
                        }
                    }
                }

                // Registrar snapshot e mensagens de log (simples)
                simulatedLog.add("🆕 Nova solicitação criada.");

                isSimulatedSent = true;
                simulatedCurrentStepLabel = null; // Inicia como Pendente
                
                updateCardsPreview();
            });
            submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            submitBtn.setWidthFull();

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (showSendButton.getValue()) {
                card.add(submitBtn);
            } else {
                Span info = new Span("Botão enviar desativado nas configurações");
                info.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("color", "var(--lumo-secondary-text-color)");
                card.add(info);
            }
        } else {
            Span sent = new Span("✓ Solicitação enviada");
            sent.getStyle().set("color", "var(--lumo-success-text-color)").set("font-size", "var(--lumo-font-size-xs)");
            card.add(sent);
        }

        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return card;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component createRequestCardSnapshot() {
        VerticalLayout card = new VerticalLayout();
        card.setWidthFull();
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle().set("background", "var(--lumo-contrast-5pct)").set("border-radius", "8px").set("border", "1px solid var(--lumo-contrast-10pct)");

        Span title = new Span("Resumo da Solicitação");
        title.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)");
        card.add(title);

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (FieldDTO f : fields) {
            Component field = requestFields.get(f.label);
            String val = extractFieldValue(field);
            
            Div row = new Div();
            row.setWidthFull();
            Span lbl = new Span(f.label + ": ");
            lbl.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-xs)");
            Span valSpan = new Span(val != null && !val.isBlank() ? val : "-");
            valSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            row.add(lbl, valSpan);
            card.add(row);
        }
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return card;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component createStepCardPreview(String label) {
        StepDTO currentStep = label != null ? steps.stream().filter(s -> s.label.equals(label)).findFirst().orElse(null) : null;
        
        // Se label for null, estamos no estado "Pendente"
        boolean isPending = (currentStep == null);

        String themeColor = isPending ? "gray" : (currentStep != null ? currentStep.colorTheme : "gray");
        String[] colors = TaskUIUtils.getThemeColors(themeColor);
        String bgColor = colors[0];
        String borderColor = colors[1];
        boolean isDark = Boolean.parseBoolean(colors[2]);

        Div card = new Div();
        card.getStyle()
                .set("border", "1px solid " + borderColor)
                .set("border-left", "6px solid " + borderColor)
                .set("border-radius", "12px")
                .set("padding", "var(--lumo-space-m)")
                .set("background", bgColor)
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("width", "100%");

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (isDark) {
            card.getStyle().set("color", "#ffffff");
            card.getStyle().set("--lumo-secondary-text-color", "rgba(255, 255, 255, 0.7)");
            card.getStyle().set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.2)");
        }

        String tName = nameField.getValue() != null && !nameField.getValue().isEmpty() ? nameField.getValue() : "Nome do Template";
        String emoji = TaskUIUtils.resolveTemplateEmoji(tName);
        Span header = new Span(emoji + " " + tName);
        header.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-m)").set("display", "block").set("margin-bottom", "var(--lumo-space-xs)");
        card.add(header);

        HorizontalLayout statusRow = new HorizontalLayout();
        statusRow.setAlignItems(FlexComponent.Alignment.CENTER);
        Span statusLabel = new Span("Status: ");
        statusLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        String badgeText = isPending ? "⏳ Pendente" : (isSimulatedFinalized ? "✅ " + label : "💡 " + label);
        Span statusBadge = new Span(badgeText);
        statusBadge.getStyle()
                .set("padding", "2px 10px")
                .set("border-radius", "12px")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "bold");
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (isPending) {
            statusBadge.getStyle().set("background-color", "var(--lumo-contrast-20pct)").set("color", "var(--lumo-secondary-text-color)");
        } else {
            TaskUIUtils.applyThemeStyles(statusBadge, currentStep != null ? currentStep.colorTheme : "default");
        }
        statusRow.add(statusLabel, statusBadge);

        // Cronômetro (simulado)
        Span timer = new Span(isSimulatedFinalized ? "⏱️ 00:45" : "⏱️ 00:01");
        timer.getStyle().set("font-size", "var(--lumo-font-size-xs)").set("color", "var(--lumo-secondary-text-color)").set("margin-left", "auto");
        statusRow.add(timer);
        card.add(statusRow);

        // Campos do Template (Exibição)
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!fields.isEmpty()) {
            Div fieldsDiv = new Div();
            fieldsDiv.getStyle().set("background", "var(--lumo-contrast-5pct)").set("border-radius", "8px").set("padding", "var(--lumo-space-s)").set("margin-top", "var(--lumo-space-s)").set("font-size", "var(--lumo-font-size-s)");
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (FieldDTO f : fields) {
                Div row = new Div();
                Span lbl = new Span(f.label + ": ");
                lbl.getStyle().set("font-weight", "bold");
                row.add(lbl, new Span("Exemplo de Valor"));
                fieldsDiv.add(row);
            }
            card.add(fieldsDiv);
        }

        // Identificar as próximas etapas permitidas para mostrar os campos de resposta preditivos
        Set<String> restrictedStepLabels = steps.stream()
                .filter(s -> s.allowedNextStepLabels != null)
                .flatMap(s -> s.allowedNextStepLabels.stream())
                .collect(Collectors.toSet());

        List<StepDTO> nextSteps = new ArrayList<>();
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (isPending) {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (StepDTO s : steps) {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (!restrictedStepLabels.contains(s.label) || s.isGlobal) {
                    nextSteps.add(s);
                }
            }
        } else if (currentStep != null) {
            List<String> nextAllowed = currentStep.allowedNextStepLabels != null ? currentStep.allowedNextStepLabels : new ArrayList<>();
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (StepDTO s : steps) {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (s.label.equals(currentStep.label)) continue;
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (!nextAllowed.isEmpty()) {
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (nextAllowed.contains(s.label) || s.isGlobal) {
                        nextSteps.add(s);
                    }
                } else {
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (!restrictedStepLabels.contains(s.label) || s.isGlobal) {
                        nextSteps.add(s);
                    }
                }
            }
        }

        // Limpar o mapa de inputs do passo anterior
        stepResponseFields.clear();

        // Adicionar campos de resposta de todas as próximas etapas possíveis (comportamento real do NeoKioski)
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!isSimulatedFinalized) {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (StepDTO nextStep : nextSteps) {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (nextStep.hasResponseField) {
                    Component respInput = createInputComponentPreview(
                        nextStep.responseInputType, 
                        nextStep.responseFieldLabel != null ? nextStep.responseFieldLabel : "Resposta (" + nextStep.label + ")", 
                        null, 
                        nextStep.isResponseRequired, 
                        nextStep.responseOptions.stream().map(o -> {
                            TaskFieldOption opt = new TaskFieldOption();
                            opt.setOptionLabel(o);
                            /*
                             * Retorna o componente renderizado ou o valor da operação atual.
                             */
                            return opt;
                        }).collect(Collectors.toList()), 
                        "Preencha se " + nextStep.label
                    );
                    stepResponseFields.put(nextStep.label, respInput);
                    card.add(respInput);
                }
            }
        }

        // Botões de Ação
        FlexLayout actions = new FlexLayout();
        actions.getStyle().set("flex-wrap", "wrap").set("gap", "var(--lumo-space-s)").set("margin-top", "var(--lumo-space-m)");

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (isSimulatedFinalized && !isPending) {
            Span finalizedInfo = new Span("✓ Fluxo Finalizado");
            finalizedInfo.getStyle()
                    .set("font-size", "var(--lumo-font-size-xxs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("margin-top", "var(--lumo-space-s)");
            card.add(finalizedInfo);
        } else {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (StepDTO nextStep : nextSteps) {
                Button btn = createSimulatedStepButton(nextStep);
                actions.add(btn);
            }
        }

        card.add(actions);
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return card;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Button createSimulatedStepButton(StepDTO targetStep) {
        Button btn = new Button(targetStep.label, e -> {
            // Validação de resposta obrigatória para a etapa ESPECÍFICA de destino
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (targetStep.hasResponseField && targetStep.isResponseRequired) {
                Component input = stepResponseFields.get(targetStep.label);
                String val = extractFieldValue(input);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (val == null || val.isBlank()) {
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (input instanceof com.vaadin.flow.component.HasValidation hv) {
                        hv.setInvalid(true);
                        hv.setErrorMessage("A resposta é obrigatória para " + targetStep.label);
                    }
                    Notification.show("A resposta para '" + targetStep.label + "' é obrigatória.");
                    return;
                }
            }

            simulatedCurrentStepLabel = targetStep.label;
            isSimulatedFinalized = targetStep.isFinalizer;
            
            // Mensagem de menção da etapa (Unificada)
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (targetStep.sendNotification && targetStep.notificationMessage != null && !targetStep.notificationMessage.isBlank()) {
                String message = targetStep.notificationMessage;
                // Simulação básica de tags para o preview
                message = message.replace("@criador", "@criador");
                message = message.replace("@autor_anterior", "@autor_anterior");
                message = message.replace("@usuario", "@usuario");
                simulatedLog.add(message);
            }
            
            updateCardsPreview();
        });
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (targetStep.colorTheme != null && !targetStep.colorTheme.equalsIgnoreCase("gray")) {
            TaskUIUtils.applyThemeStyles(btn, targetStep.colorTheme);
        } else if (targetStep.isFinalizer) {
            btn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        } else {
            btn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        }
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return btn;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Component createInputComponentPreview(InputType type, String label, String desc, boolean required, List<?> optionsList, String placeholder) {
        List<String> options = new ArrayList<>();
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (optionsList != null) {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (Object o : optionsList) {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (o instanceof TaskFieldOption tfo) options.add(tfo.getOptionLabel());
                else options.add(o.toString());
            }
        }

        Component fieldComponent = null;
        switch (type) {
            case TEXT:
            default:
                TextField tf = new TextField(label);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (desc != null) tf.setHelperText(desc);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (placeholder != null) tf.setPlaceholder(placeholder);
                tf.setWidthFull();
                tf.setRequiredIndicatorVisible(required);
                fieldComponent = tf;
                break;
            case DATE:
                com.vaadin.flow.component.datepicker.DatePicker dp = new com.vaadin.flow.component.datepicker.DatePicker(label);
                dp.setLocale(Locale.forLanguageTag("pt-BR"));
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (desc != null) dp.setHelperText(desc);
                dp.setWidthFull();
                dp.setRequiredIndicatorVisible(required);
                fieldComponent = dp;
                break;
            case TIME:
                com.vaadin.flow.component.timepicker.TimePicker tp = new com.vaadin.flow.component.timepicker.TimePicker(label);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (desc != null) tp.setHelperText(desc);
                tp.setWidthFull();
                tp.setRequiredIndicatorVisible(required);
                fieldComponent = tp;
                break;
            case NUMBER:
                com.vaadin.flow.component.textfield.NumberField nf = new com.vaadin.flow.component.textfield.NumberField(label);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (desc != null) nf.setHelperText(desc);
                nf.setWidthFull();
                nf.setRequiredIndicatorVisible(required);
                fieldComponent = nf;
                break;
            case CHECKBOX:
                com.vaadin.flow.component.checkbox.CheckboxGroup<String> cbGroup = new com.vaadin.flow.component.checkbox.CheckboxGroup<>();
                cbGroup.setLabel(label);
                cbGroup.setItems(options);
                cbGroup.setRequiredIndicatorVisible(required);
                fieldComponent = cbGroup;
                break;
            case RADIO:
                com.vaadin.flow.component.radiobutton.RadioButtonGroup<String> radio = new com.vaadin.flow.component.radiobutton.RadioButtonGroup<>();
                radio.setLabel(label);
                radio.setItems(options);
                radio.setRequiredIndicatorVisible(required);
                fieldComponent = radio;
                break;
            case DROPDOWN:
                com.vaadin.flow.component.combobox.ComboBox<String> combo = new com.vaadin.flow.component.combobox.ComboBox<>(label);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (desc != null) combo.setHelperText(desc);
                combo.setWidthFull();
                combo.setRequiredIndicatorVisible(required);
                combo.setItems(options);
                fieldComponent = combo;
                break;
            case MULTISELECT_DROPDOWN:
                com.vaadin.flow.component.combobox.MultiSelectComboBox<String> multiCombo = new com.vaadin.flow.component.combobox.MultiSelectComboBox<>(label);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (desc != null) multiCombo.setHelperText(desc);
                multiCombo.setWidthFull();
                multiCombo.setRequiredIndicatorVisible(required);
                multiCombo.setItems(options);
                fieldComponent = multiCombo;
                break;
        }

        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return fieldComponent;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private String extractFieldValue(Component comp) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (comp == null) return "";
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (comp instanceof com.vaadin.flow.component.HasValue<?, ?> hv) {
            Object val = hv.getValue();
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (val == null) return "";
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (val instanceof java.util.Collection<?> col) {
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
                return col.stream().map(Object::toString).collect(Collectors.joining(", "));
            }
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (val instanceof java.time.LocalDate ld) {
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
                return ld.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            }
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (val instanceof java.time.LocalTime lt) {
                /*
                 * Retorna o componente renderizado ou o valor da operação atual.
                 */
                return lt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
            }
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return String.valueOf(val);
        }
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return "";
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return footer;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void saveTemplate() {
        TaskTemplate t;
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (editingTemplateId != null) {
            t = templateService.getTemplateById(editingTemplateId);
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (t == null) t = new TaskTemplate();
        } else {
            t = new TaskTemplate();
        }

        t.setName(nameField.getValue());
        t.setShowSendButton(showSendButton.getValue());
        t.setRequiresSignature(requireSignature.getValue());
        t.setSortOrder(sortOrderField.getValue() != null ? sortOrderField.getValue() : 0);
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (t.isRequiresSignature()) {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (authorizingUsersCombo.getValue().isEmpty()) {
                Notification.show("Selecione pelo menos um autorizador", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            boolean hasAuthorize = steps.stream().anyMatch(s -> s.label.equalsIgnoreCase("AUTORIZADO"));
            boolean hasReject = steps.stream().anyMatch(s -> s.label.equalsIgnoreCase("REJEITADO"));
            
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!hasAuthorize || !hasReject) {
                Notification.show("Templates com autorização exigem os passos 'AUTORIZADO' e 'REJEITADO'", 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (registraNoRelatorioLojaUI.getValue()) {
            boolean hasColetadoFinalizer = steps.stream()
                    .anyMatch(s -> s.label.equalsIgnoreCase("COLETADO") && s.isFinalizer);

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!hasColetadoFinalizer) {
                com.vaadin.flow.component.notification.Notification.show("Templates com relatório de loja exigem o passo 'COLETADO' marcado como Finalizador", 5000, com.vaadin.flow.component.notification.Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            boolean hasDeliveryInFields = fields.stream()
                    .anyMatch(f -> f.label.toUpperCase().contains("DELIVERY"));

            boolean hasDeliveryInSteps = steps.stream()
                    .anyMatch(s -> s.hasResponseField && s.responseFieldLabel != null && s.responseFieldLabel.toUpperCase().contains("DELIVERY"));

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!hasDeliveryInFields && !hasDeliveryInSteps) {
                com.vaadin.flow.component.notification.Notification.show("Templates com relatório de loja exigem um campo (no formulário ou resposta de etapa) contendo 'DELIVERY' no rótulo", 5000, com.vaadin.flow.component.notification.Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
        }
        t.getAuthorizingUsers().clear();
        t.getAuthorizingUsers().addAll(authorizingUsersCombo.getValue());
        t.setPlayAlertSound(playAlertSoundUI.getValue());
        t.setAlertSound(globalAlertSoundCombo.getValue());
        t.getAlertRoles().clear();
        t.getAlertRoles().addAll(globalAlertRolesCombo.getValue());
        t.setDefaultResponsibleRole(defaultResponsibleRoleCombo.getValue());
        t.setRegistraNoRelatorioLoja(registraNoRelatorioLojaUI.getValue());
        t.getGroups().clear();
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (groupComboBox.getValue() != null) {
            t.getGroups().addAll(groupComboBox.getValue());
        }

        List<TemplateFieldRelation> fieldRelations = new ArrayList<>();
        int fOrder = 1;
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (FieldDTO dto : fields) {
            TaskFieldDefinition def = new TaskFieldDefinition();
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (dto.id != null) {
                def.setId(dto.id);
            }
            def.setLabel(dto.label);
            def.setInputType(dto.inputType);
            def.setDescription(dto.description);

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (dto.options != null && !dto.options.isEmpty()) {
                /*
                 * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
                 */
                for (String optStr : dto.options) {
                    TaskFieldOption option = new TaskFieldOption();
                    option.setOptionLabel(optStr.trim());
                    option.setOptionValue(optStr.trim());
                    def.addTaskFieldOption(option);
                }
            }

            TemplateFieldRelation rel = new TemplateFieldRelation();
            rel.setId(dto.relationId); // <--- PRESERVA O ID DA RELAÇÃO
            rel.setTaskFieldDefinition(def);
            rel.setRequired(dto.isRequired);
            rel.setOrder(fOrder++);
            fieldRelations.add(rel);
        }

        List<TemplateStepRelation> stepRelations = new ArrayList<>();
        int sOrder = 1;

        // Primeiro passo: criar todos os TaskStepDefinition e mapeá-los para uso posterior
        Map<String, TaskStepDefinition> defMap = new HashMap<>();
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (StepDTO dto : steps) {
            TaskStepDefinition def = new TaskStepDefinition();
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (dto.id != null) {
                def.setId(dto.id);
            }
            def.setLabel(dto.label);
            def.setColorTheme(dto.colorTheme);
            defMap.put(dto.label, def);
        }

        // Segundo passo: criar as relações e associar as próximas etapas permitidas
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (StepDTO dto : steps) {
            TaskStepDefinition def = defMap.get(dto.label);
            TemplateStepRelation rel = new TemplateStepRelation();
            rel.setId(dto.relationId); // <--- PRESERVA O ID DA RELAÇÃO
            rel.setTaskStepDefinition(def);
            rel.setOrder(sOrder++);
            rel.setFinalizer(dto.isFinalizer);
            rel.setGlobal(dto.isGlobal);
            rel.setShouldPulse(dto.shouldPulse);
            rel.setSendNotification(dto.sendNotification);
            rel.setNotificationMessage(dto.notificationMessage);
            rel.setHasResponseField(dto.hasResponseField);
            rel.setResponseRequired(dto.isResponseRequired);
            rel.setResponseFieldLabel(dto.responseFieldLabel);
            rel.setResponseIsCompact(dto.responseIsCompact);
            rel.setResponseInputType(dto.responseInputType);
            rel.setAlertSound(dto.alertSound);
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (dto.alertRoles != null) rel.getAlertRoles().addAll(dto.alertRoles);
            rel.setResponsibleRole(dto.responsibleRole);
            
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (dto.responseOptions != null && !dto.responseOptions.isEmpty()) {
                /*
                 * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
                 */
                for (String optStr : dto.responseOptions) {
                    TaskFieldOption opt = new TaskFieldOption();
                    opt.setOptionLabel(optStr.trim());
                    opt.setOptionValue(optStr.trim());
                    rel.addResponseOption(opt);
                }
            }

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (dto.allowedNextStepLabels != null && !dto.allowedNextStepLabels.isEmpty()) {
                /*
                 * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
                 */
                for (String nextLabel : dto.allowedNextStepLabels) {
                    TaskStepDefinition nextDef = defMap.get(nextLabel);
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (nextDef != null) {
                        rel.getAllowedNextSteps().add(nextDef);
                    }
                }
            }
            stepRelations.add(rel);
        }

        List<TaskAlert> taskAlerts = new ArrayList<>();
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (AlertDTO dto : alerts) {
            TaskAlert alert = new TaskAlert();
            TaskStepDefinition target = defMap.get(dto.targetStepLabel);
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (target != null) {
                alert.setId(dto.id);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (dto.startStepLabel != null && !dto.startStepLabel.isBlank()) {
                     alert.setStartStep(defMap.get(dto.startStepLabel));
                }
                alert.setTargetStep(target);
                alert.setTimeoutMinutes(dto.timeoutMinutes);
                alert.setAlertMessage(dto.alertMessage);
                alert.setAlertSound(dto.alertSound);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (dto.alertRoles != null) alert.getAlertRoles().addAll(dto.alertRoles);
                taskAlerts.add(alert);
            }
        }


        List<TaskSlaConfig> taskSlas = new ArrayList<>();
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (SlaDTO dto : slas) {
            TaskSlaConfig config = new TaskSlaConfig();
            if (dto.id != null) {
                config.setId(dto.id);
            }
            config.setName(dto.name);
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (dto.sourceStepLabel != null && !dto.sourceStepLabel.isBlank()) {
                config.setSourceStep(defMap.get(dto.sourceStepLabel));
            }
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (dto.targetStepLabel != null && !dto.targetStepLabel.isBlank()) {
                config.setTargetStep(defMap.get(dto.targetStepLabel));
            }
            config.setTargetValue(dto.targetValue);
            config.setUnit(dto.unit);
            config.setValidFrom(dto.validFrom);
            config.setJustificationOptions(dto.justificationOptions);
            taskSlas.add(config);
        }

        // Validação de Menções Dinâmicas (Obrigatório por requisito do usuário)
        Set<String> placeholders = extractPlaceholders();
        Set<Group> selectedGroups = groupComboBox.getValue();
        
        for (String p : placeholders) {
            for (Group g : selectedGroups) {
                boolean isMapped = mentionMappings.stream()
                        .anyMatch(m -> m.getPlaceholder().equalsIgnoreCase(p) && 
                                       m.getGroup().getId().equals(g.getId()) && 
                                       m.getUsers() != null && 
                                       !m.getUsers().isEmpty());
                
                if (!isMapped) {
                    Notification.show("A menção '" + p + "' deve ser mapeada para o grupo '" + g.getName() + "' antes de salvar.", 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    tabs.setSelectedTab(mentionsTab);
                    showMentionsTab();
                    return;
                }
            }
        }

        // Limpar mapeamentos órfãos (de grupos ou placeholders que foram removidos)
        mentionMappings.removeIf(m -> selectedGroups.stream().noneMatch(sg -> sg.getId().equals(m.getGroup().getId())) || 
                                      placeholders.stream().noneMatch(p -> p.equalsIgnoreCase(m.getPlaceholder())));

        templateService.saveTemplate(t, fieldRelations, stepRelations, taskAlerts, taskSlas, mentionMappings);
        Notification.show("Template Salvo com Sucesso!");
        getUI().ifPresent(ui -> ui.navigate(TaskTemplateListView.class));
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void setParameter(BeforeEvent event, @OptionalParameter Long parameter) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (parameter != null) {
            this.editingTemplateId = parameter;
            TaskTemplate t = templateService.getTemplateById(parameter);
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (t != null) {
                nameField.setValue(t.getName());
                showSendButton.setValue(t.isShowSendButton());
                requireSignature.setValue(t.isRequiresSignature());
                authorizingUsersCombo.setValue(new HashSet<>(t.getAuthorizingUsers()));
                authorizingUsersCombo.setVisible(t.isRequiresSignature());
                playAlertSoundUI.setValue(t.isPlayAlertSound());
                globalAlertSoundCombo.setValue(t.getAlertSound());
                globalAlertRolesCombo.setValue(new HashSet<>(t.getAlertRoles()));
                globalAlertSoundCombo.setVisible(t.isPlayAlertSound());
                globalAlertRolesCombo.setVisible(t.isPlayAlertSound());
                playGlobalSoundBtn.setVisible(t.isPlayAlertSound());
                defaultResponsibleRoleCombo.setValue(t.getDefaultResponsibleRole());
                registraNoRelatorioLojaUI.setValue(t.isRegistraNoRelatorioLoja());
                sortOrderField.setValue(t.getSortOrder());
                groupComboBox.setValue(t.getGroups());

                fields.clear();
                // Usar as relações já carregadas no objeto t para evitar LazyInitializationException
                List<TemplateFieldRelation> sortedFieldRels = t.getTemplateFieldRelation().stream()
                        .sorted(Comparator.comparingInt(TemplateFieldRelation::getOrder))
                        .collect(Collectors.toList());
                for (TemplateFieldRelation fr : sortedFieldRels) {
                    FieldDTO dto = new FieldDTO();
                    dto.id = fr.getTaskFieldDefinition().getId();
                    dto.relationId = fr.getId(); // <--- IMPORTANTE
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
                // Usar as relações já carregadas no objeto t para evitar LazyInitializationException
                List<TemplateStepRelation> sortedStepRels = t.getTemplateStepRelation().stream()
                        .sorted(Comparator.comparingInt(TemplateStepRelation::getOrder))
                        .collect(Collectors.toList());
                for (TemplateStepRelation sr : sortedStepRels) {
                    StepDTO dto = new StepDTO();
                    dto.id = sr.getTaskStepDefinition().getId();
                    dto.relationId = sr.getId(); // <--- IMPORTANTE
                    dto.label = sr.getTaskStepDefinition().getLabel();
                    dto.colorTheme = sr.getTaskStepDefinition().getColorTheme();
                    dto.isFinalizer = sr.isFinalizer();
                    dto.isGlobal = sr.isGlobal();
                    dto.shouldPulse = sr.isShouldPulse();
                    dto.sendNotification = sr.isSendNotification();
                    dto.notificationMessage = sr.getNotificationMessage();
                    dto.hasResponseField = sr.isHasResponseField();
                    dto.isResponseRequired = sr.isResponseRequired();
                    dto.responseFieldLabel = sr.getResponseFieldLabel();
                    dto.responseIsCompact = sr.isResponseIsCompact();
                    dto.responseInputType = sr.getResponseInputType();
                    dto.alertSound = sr.getAlertSound();
                    dto.alertRoles = new HashSet<>(sr.getAlertRoles());
                    dto.responsibleRole = sr.getResponsibleRole();
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
                
                alerts.clear();
                /*
                 * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
                 */
                for (TaskAlert alert : t.getAlerts()) {
                    AlertDTO dto = new AlertDTO();
                    dto.id = alert.getId();
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (alert.getStartStep() != null) {
                        dto.startStepLabel = alert.getStartStep().getLabel();
                    }
                    dto.targetStepLabel = alert.getTargetStep().getLabel();
                    dto.timeoutMinutes = alert.getTimeoutMinutes();
                    dto.alertMessage = alert.getAlertMessage();
                    dto.alertSound = alert.getAlertSound();
                    dto.alertRoles = new HashSet<>(alert.getAlertRoles());
                    alerts.add(dto);
                }
                alertsGrid.getDataProvider().refreshAll();
                
                slas.clear();
                /*
                 * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
                 */
                for (TaskSlaConfig sla : templateService.getSlaConfigsByTemplateId(parameter)) {
                    SlaDTO dto = new SlaDTO();
                    dto.id = sla.getId();
                    dto.name = sla.getName();
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (sla.getSourceStep() != null) dto.sourceStepLabel = sla.getSourceStep().getLabel();
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (sla.getTargetStep() != null) dto.targetStepLabel = sla.getTargetStep().getLabel();
                    dto.targetValue = sla.getTargetValue();
                    dto.unit = sla.getUnit();
                    dto.validFrom = sla.getValidFrom();
                    dto.justificationOptions = sla.getJustificationOptions();
                    slas.add(dto);
                }
                slasGrid.getDataProvider().refreshAll();
                
                mentionMappings.clear();
                mentionMappings.addAll(t.getMentionsMappings());

                updateCardsPreview();
            }
        }
    }

    private Set<String> extractPlaceholders() {
        Set<String> placeholders = new HashSet<>();
        Set<String> reserved = Set.of("@criador", "@autorizadores", "@usuario", "@autor_anterior", "@resposta");

        // Buscar usernames reais para não confundi-los com menções dinâmicas
        Set<String> realUsernames = userService.findAll().stream()
                .map(u -> "@" + u.getUsername().toLowerCase())
                .collect(Collectors.toSet());

        for (AlertDTO alert : alerts) {
            if (alert.alertMessage != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("@\\w+").matcher(alert.alertMessage);
                while (m.find()) {
                    String p = m.group();
                    if (!reserved.contains(p.toLowerCase()) && !realUsernames.contains(p.toLowerCase())) {
                        placeholders.add(p);
                    }
                }
            }
        }

        for (StepDTO step : steps) {
            if (step.notificationMessage != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("@\\w+").matcher(step.notificationMessage);
                while (m.find()) {
                    String p = m.group();
                    if (!reserved.contains(p.toLowerCase()) && !realUsernames.contains(p.toLowerCase())) {
                        placeholders.add(p);
                    }
                }
            }
        }
        return placeholders;
    }

    private void showMentionsTab() {
        tabContent.removeAll();
        Set<String> placeholders = extractPlaceholders();

        if (placeholders.isEmpty()) {
            tabContent.add(new Span("Nenhuma menção customizada encontrada nos textos de alerta ou workflow. Use @ seguido de um nome (ex: @mencao1) para criar menções dinâmicas."));
            return;
        }

        H3 title = new H3("Mapeamento de Menções por Grupo");
        Span help = new Span("Para cada grupo que utiliza este template, defina quais usuários serão notificados para cada etiqueta.");
        help.getStyle().set("font-size", "var(--lumo-font-size-s)").set("color", "var(--lumo-secondary-text-color)");

        mentionsGrid.removeAllColumns();
        mentionsGrid.addColumn(Group::getName).setHeader("Grupo").setFrozen(true).setWidth("200px").setFlexGrow(0);

        for (String p : placeholders) {
            mentionsGrid.addComponentColumn(group -> {
                MultiSelectComboBox<User> userCombo = new MultiSelectComboBox<>();
                userCombo.setItems(group.getUsers());
                userCombo.setItemLabelGenerator(User::getName);
                userCombo.setWidthFull();

                // Buscar valor existente
                TaskTemplateMention mapping = mentionMappings.stream()
                        .filter(m -> m.getGroup().getId().equals(group.getId()) && m.getPlaceholder().equalsIgnoreCase(p))
                        .findFirst().orElse(null);

                if (mapping != null) {
                    userCombo.setValue(mapping.getUsers());
                }

                userCombo.addValueChangeListener(e -> {
                    TaskTemplateMention m = mentionMappings.stream()
                            .filter(map -> map.getGroup().getId().equals(group.getId()) && map.getPlaceholder().equalsIgnoreCase(p))
                            .findFirst().orElse(null);
                    
                    if (m == null) {
                        m = new TaskTemplateMention();
                        m.setGroup(group);
                        m.setPlaceholder(p);
                        mentionMappings.add(m);
                    }
                    m.setUsers(new HashSet<>(e.getValue()));
                });

                return userCombo;
            }).setHeader(p);
        }

        // Recarregar os grupos selecionados com os usuários para evitar LazyInitializationException no Grid
        List<Group> selectedGroups = groupComboBox.getValue().stream()
                .map(g -> groupService.findById(g.getId()).orElse(g))
                .collect(Collectors.toList());
        mentionsGrid.setItems(selectedGroups);

        tabContent.add(title, help, mentionsGrid);
    }
}
