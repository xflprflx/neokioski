package com.xflprflx.neokioski.ui.views;
// v2 - forced refresh

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.MessageReaction;
import com.xflprflx.neokioski.models.MessageEdit;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.TaskFieldValue;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.combobox.ComboBox;

import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.security.SecurityService;
import com.xflprflx.neokioski.services.ChatService;
import com.xflprflx.neokioski.services.GroupService;
import com.xflprflx.neokioski.services.TaskService;
import com.xflprflx.neokioski.services.UserService;
import com.xflprflx.neokioski.services.TaskTemplateService;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import com.xflprflx.neokioski.models.template.TemplateFieldRelation;
import com.xflprflx.neokioski.models.template.TemplateStepRelation;
import com.xflprflx.neokioski.models.template.TaskFieldOption;
import com.xflprflx.neokioski.models.template.InputType;
import com.xflprflx.neokioski.ui.utils.TaskUIUtils;
import jakarta.annotation.security.PermitAll;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@PermitAll
@Route(value = "chat", layout = MainLayout.class)
@PageTitle("Chat | NeoKioski")
public class ChatView extends VerticalLayout implements HasUrlParameter<Long> {

    private final ChatService chatService;
    private final GroupService groupService;
    private final SecurityService securityService;
    private final UserService userService;
    private final TaskTemplateService taskTemplateService;
    private final TaskService taskService;

    private Group group;
    private User currentUser;
    
    private final VerticalLayout messageContainer = new VerticalLayout();
    private final Scroller scroller = new Scroller(messageContainer);
    private final MessageInput messageInput = new MessageInput();
    private final H2 groupName = new H2();
    private final HorizontalLayout replyBar = new HorizontalLayout();
    private final HorizontalLayout templateBar = new HorizontalLayout();
    private Div suggestionWrapper;
    private ListBox<User> suggestionList;
    
    // Contexto de menções
    private Div activeSuggestionWrapper;
    private ListBox<User> activeSuggestionList;
    private final Map<String, Div> suggestionWrappers = new HashMap<>();
    private final Map<String, ListBox<User>> suggestionLists = new HashMap<>();
    private String activeMentionId = "main-input";
    private final Map<Long, HorizontalLayout> messageWrappers = new HashMap<>();
    private final Map<Long, Long> taskIdToMessageId = new HashMap<>();
    
    // Novas áreas de layout
    private final VerticalLayout chatArea = new VerticalLayout();
    private final VerticalLayout taskArea = new VerticalLayout();
    private final FlexLayout taskListArea = new FlexLayout();
    private final SplitLayout chatVerticalSplit = new SplitLayout();
    private final VerticalLayout bottomControls = new VerticalLayout();
    private final Scroller templateScroller = new Scroller(templateBar);
    private final SplitLayout mainSplit = new SplitLayout(chatArea, taskArea);
    
    // Filtros de tarefas
    private boolean showTasksInChat = true;
    private String filterOwner = "todas";
    private String filterStatus = "todos";
    private ComboBox<String> ownerFilter;
    private ComboBox<String> statusFilter;
    private com.vaadin.flow.component.datepicker.DatePicker startDatePicker;
    private com.vaadin.flow.component.datepicker.DatePicker endDatePicker;
    private com.vaadin.flow.component.checkbox.Checkbox showTasksInChatCheckbox;
    private com.vaadin.flow.component.checkbox.Checkbox notifyStoreOrderCheckbox;
    private com.vaadin.flow.component.checkbox.Checkbox notifyChatSoundsCheckbox;
    
    private Consumer<ChatMessage> listener;
    private Consumer<Task> taskListener;

    private ChatMessage replyTo = null;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public ChatView(ChatService chatService, GroupService groupService, SecurityService securityService, UserService userService, TaskTemplateService taskTemplateService, TaskService taskService) {
        this.chatService = chatService;
        this.groupService = groupService;
        this.securityService = securityService;
        this.userService = userService;
        this.taskTemplateService = taskTemplateService;
        this.taskService = taskService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        
        configureUI();
    }

    private void configureUI() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("overflow", "hidden");

        // Header de Controle (Toggle e Configurações)
        HorizontalLayout controls = createTopHeader();
        
        // Área de Chat
        chatArea.setSizeFull();
        chatArea.setPadding(false);
        chatArea.setSpacing(false);
        
        messageContainer.setWidthFull();
        messageContainer.setPadding(true);
        messageContainer.setSpacing(true);

        scroller.setSizeFull();
        scroller.getStyle().set("min-height", "0");
        messageInput.setWidthFull();
        messageInput.getStyle().set("flex-shrink", "0");

        messageInput.addSubmitListener(event -> {
            if (group != null && currentUser != null) {
                // Ao enviar, forçar data para hoje para ver a própria mensagem (conforme solicitado pelo usuário)
                java.time.LocalDate today = java.time.LocalDate.now();
                if (!today.equals(startDatePicker.getValue()) || !today.equals(endDatePicker.getValue())) {
                    startDatePicker.setValue(today);
                    endDatePicker.setValue(today);
                    // loadMessages() será chamado pelos ValueChangeListeners
                }
                chatService.sendMessage(event.getValue(), currentUser, group, replyTo, null, null);
                cancelReply();
            }
        });

        configureReplyBar();
        configureTemplateBar();
        
        bottomControls.setSizeFull();
        bottomControls.setPadding(false);
        bottomControls.setSpacing(false);
        bottomControls.getStyle().set("min-height", "0");
        bottomControls.getStyle().set("background", "var(--lumo-base-color)");
        bottomControls.getStyle().set("z-index", "10");
        templateScroller.getStyle().set("min-height", "0");
        bottomControls.add(replyBar, templateScroller, messageInput);
        bottomControls.setFlexGrow(1, templateScroller);
        bottomControls.setFlexGrow(0, replyBar);
        bottomControls.setFlexGrow(0, messageInput);
        
        chatVerticalSplit.setSizeFull();
        chatVerticalSplit.getStyle().set("min-height", "0");
        chatVerticalSplit.setOrientation(SplitLayout.Orientation.VERTICAL);
        chatVerticalSplit.addToPrimary(scroller);
        chatVerticalSplit.addToSecondary(bottomControls);
        chatVerticalSplit.setSplitterPosition(75); // Iniciar com 75% para o chat
        
        chatArea.getStyle().set("min-height", "0");
        chatArea.add(chatVerticalSplit);


        // Área de Tarefas
        configureTaskArea();

        // Split Layout
        mainSplit.setSizeFull();
        mainSplit.getStyle().set("min-height", "0");
        mainSplit.setSplitterPosition(50);
        
        getStyle().set("min-height", "0");
        add(controls, mainSplit);
        setFlexGrow(1, mainSplit);
        configureSuggestions();
    }

    private HorizontalLayout createTopHeader() {
        groupName.getStyle().set("margin", "0").set("font-size", "var(--lumo-font-size-l)");
        
        Button showChatOnly = new Button(VaadinIcon.CHAT.create(), e -> mainSplit.setSplitterPosition(100));
        showChatOnly.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        showChatOnly.setTooltipText("Apenas Chat");

        Button showSplit = new Button(VaadinIcon.TABLE.create(), e -> mainSplit.setSplitterPosition(50));
        showSplit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        showSplit.setTooltipText("Dividido");

        Button showTasksOnly = new Button(VaadinIcon.TASKS.create(), e -> mainSplit.setSplitterPosition(0));
        showTasksOnly.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        showTasksOnly.setTooltipText("Apenas Tarefas");

        HorizontalLayout toggleButtons = new HorizontalLayout(showChatOnly, showSplit, showTasksOnly);
        toggleButtons.setSpacing(false);

        showTasksInChatCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Tarefas no chat", showTasksInChat);
        showTasksInChatCheckbox.getStyle().set("margin-left", "auto").set("margin-right", "var(--lumo-space-m)");
        showTasksInChatCheckbox.addValueChangeListener(e -> {
            showTasksInChat = e.getValue();
            loadMessages(); // Recarregar mensagens para aplicar filtro
        });

        // Controles de áudio
        notifyStoreOrderCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Alerta Pedidos", currentUser != null && currentUser.isNotifyStoreOrder());
        notifyStoreOrderCheckbox.addValueChangeListener(e -> {
            if (currentUser != null) {
                userService.updateNotifyStoreOrder(currentUser.getId(), e.getValue());
                currentUser.setNotifyStoreOrder(e.getValue());
                Notification.show("Alerta de pedidos " + (e.getValue() ? "ativado" : "desativado"), 2000, Notification.Position.BOTTOM_END);
            }
        });

        notifyChatSoundsCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Sons Chat", currentUser != null && currentUser.isNotifyChatSounds());
        notifyChatSoundsCheckbox.addValueChangeListener(e -> {
            if (currentUser != null) {
                userService.updateNotifyChatSounds(currentUser.getId(), e.getValue());
                currentUser.setNotifyChatSounds(e.getValue());
                Notification.show("Sons do chat " + (e.getValue() ? "ativados" : "desativados"), 2000, Notification.Position.BOTTOM_END);
            }
        });

        HorizontalLayout audioControls = new HorizontalLayout(VaadinIcon.VOLUME_UP.create(), notifyStoreOrderCheckbox, notifyChatSoundsCheckbox);
        audioControls.setSpacing(true);
        audioControls.setAlignItems(FlexComponent.Alignment.CENTER);
        audioControls.getStyle().set("margin-right", "var(--lumo-space-m)");

        // Filtros de Data
        startDatePicker = new com.vaadin.flow.component.datepicker.DatePicker("Início");
        startDatePicker.setValue(java.time.LocalDate.now());
        startDatePicker.setWidth("140px");
        startDatePicker.addValueChangeListener(e -> refreshAllComponents());

        endDatePicker = new com.vaadin.flow.component.datepicker.DatePicker("Fim");
        endDatePicker.setValue(java.time.LocalDate.now());
        endDatePicker.setWidth("140px");
        endDatePicker.addValueChangeListener(e -> refreshAllComponents());

        HorizontalLayout dateFilters = new HorizontalLayout(startDatePicker, endDatePicker);
        dateFilters.setSpacing(true);
        dateFilters.setAlignItems(FlexComponent.Alignment.CENTER);
        dateFilters.getStyle().set("margin-left", "var(--lumo-space-m)");

        HorizontalLayout header = new HorizontalLayout(groupName, toggleButtons, showTasksInChatCheckbox, dateFilters, audioControls);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle().set("background", "var(--lumo-base-color)");
        header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        header.getStyle().set("min-height", "60px");
        return header;
    }

    private void configureTaskArea() {
        taskArea.setSizeFull();
        taskArea.setPadding(true);
        taskArea.setSpacing(true);
        taskArea.getStyle().set("background", "var(--lumo-contrast-5pct)");
        taskArea.getStyle().set("border-left", "1px solid var(--lumo-contrast-10pct)");
        taskArea.getStyle().set("overflow", "hidden");

        Span title = new Span("📋 Tarefas do Grupo");
        title.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-l)");

        // Filtros
        ownerFilter = new ComboBox<>("Responsável");
        ownerFilter.setWidthFull();
        ownerFilter.setClearButtonVisible(true);
        ownerFilter.addValueChangeListener(e -> {
            String val = e.getValue();
            if (val == null || val.equals("Todas")) filterOwner = "todas";
            else if (val.equals("Minhas")) filterOwner = "minhas";
            else filterOwner = val;
            refreshTaskList();
        });

        statusFilter = new ComboBox<>("Status");
        statusFilter.setWidthFull();
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> {
            String val = e.getValue();
            filterStatus = (val == null || val.equals("Todos")) ? "todos" : val;
            refreshTaskList();
        });

        HorizontalLayout filters = new HorizontalLayout(ownerFilter, statusFilter);
        filters.setWidthFull();

        taskListArea.setWidthFull();
        taskListArea.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        taskListArea.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        taskListArea.setAlignItems(FlexComponent.Alignment.START);
        taskListArea.getStyle().set("gap", "var(--lumo-space-m)");
        taskListArea.getStyle().set("padding-bottom", "var(--lumo-space-xl)");

        Scroller taskScroller = new Scroller(taskListArea);
        taskScroller.setWidthFull();
        taskScroller.getStyle().set("flex-grow", "1");
        taskScroller.getStyle().set("min-height", "0");

        taskArea.add(title, filters, taskScroller);
        taskArea.setFlexGrow(1, taskScroller);
    }

    private void refreshAllComponents() {
        loadMessages();
        refreshTaskList();
    }

    private void refreshTaskList() {
        if (group == null || startDatePicker == null || endDatePicker == null || startDatePicker.getValue() == null || endDatePicker.getValue() == null) return;
        taskListArea.removeAll();

        // Converter LocalDate para Instant (Início e Fim do dia)
        java.time.Instant start = startDatePicker.getValue().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        java.time.Instant end = endDatePicker.getValue().atTime(23, 59, 59, 999_000_000).atZone(java.time.ZoneId.systemDefault()).toInstant();

        List<Task> tasks = taskService.getTasksByGroup(group, start, end);
        
        // Filtro de Autorização
        tasks = tasks.stream()
                .filter(t -> taskService.canUserViewTask(t, currentUser))
                .collect(Collectors.toList());

        // Filtrar por responsável
        tasks = tasks.stream().filter(t -> {
            if (filterOwner.equals("todas")) return true;
            if (filterOwner.equals("minhas")) {
                if (currentUser == null) return true;
                boolean isCreator = t.getCreator() != null && t.getCreator().getId().equals(currentUser.getId());
                boolean participated = t.getTaskEvents().stream()
                        .anyMatch(ev -> ev.getUser() != null && ev.getUser().getId().equals(currentUser.getId()));
                return isCreator || participated;
            }
            return t.getCreator() != null && t.getCreator().getUsername().equals(filterOwner);
        }).collect(Collectors.toList());

        // Filtrar por status
        if (!filterStatus.equals("todos")) {
            tasks = tasks.stream()
                    .filter(t -> {
                        if (filterStatus.equals("Pendentes")) return !taskService.isTaskFinalized(t);
                        if (filterStatus.equals("Finalizadas")) return taskService.isTaskFinalized(t);
                        return t.getCurrentStep() != null && t.getCurrentStep().getLabel().equals(filterStatus);
                    })
                    .collect(Collectors.toList());
        }

        if (tasks.isEmpty()) {
            VerticalLayout emptyState = new VerticalLayout();
            emptyState.setWidthFull();
            emptyState.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
            emptyState.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            emptyState.getStyle().set("margin-top", "var(--lumo-space-xl)");
            emptyState.getStyle().set("padding", "var(--lumo-space-xl)");
            
            com.vaadin.flow.component.html.Image img = new com.vaadin.flow.component.html.Image("themes/neokioski/equipe.png", "Equipe");
            img.setMaxWidth("400px");
            img.getStyle().set("opacity", "0.9");
            img.getStyle().set("margin-bottom", "var(--lumo-space-m)");
            
            Span title = new Span("Painel Zerado!");
            title.getStyle()
                 .set("font-size", "var(--lumo-font-size-xl)")
                 .set("font-weight", "bold")
                 .set("color", "var(--lumo-primary-color)");
                 
            Span subtitle = new Span("Nenhuma tarefa encontrada com os filtros atuais.");
            subtitle.getStyle()
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("font-size", "var(--lumo-font-size-m)");
            
            emptyState.add(img, title, subtitle);
            taskListArea.add(emptyState);
        } else {
            tasks.sort(java.util.Comparator.comparing(Task::getCreatedAt));
            for (Task task : tasks) {
                HorizontalLayout cardWrapper = createTaskCardLayout(task, null);
                cardWrapper.setWidth("auto");
                taskListArea.add(cardWrapper);
            }
        }
    }

    private void updateFilterOptions() {
        if (group == null) return;

        // Responsável
        List<String> ownerOptions = new ArrayList<>();
        ownerOptions.add("Minhas");
        if (group.getUsers() != null) {
            group.getUsers().stream()
                    .filter(u -> currentUser == null || !u.getId().equals(currentUser.getId()))
                    .map(User::getUsername)
                    .sorted()
                    .forEach(ownerOptions::add);
        }
        ownerOptions.add("Todas");
        ownerFilter.setItems(ownerOptions);
        ownerFilter.setValue("Todas");

        // Status
        List<String> stepOptions = new ArrayList<>();
        stepOptions.add("Todos");
        stepOptions.add("Pendentes");
        stepOptions.add("Finalizadas");
        
        taskService.getTasksByGroup(group).stream()
                .filter(t -> t.getCurrentStep() != null)
                .map(t -> t.getCurrentStep().getLabel())
                .distinct()
                .sorted()
                .forEach(stepOptions::add);
        statusFilter.setItems(stepOptions);
        statusFilter.setValue("Todos");
    }

    private void configureTemplateBar() {
        templateBar.setWidthFull();
        templateBar.setPadding(true);
        templateBar.setSpacing(true);
        templateBar.setAlignItems(FlexComponent.Alignment.START);
        templateBar.getStyle().set("background-color", "var(--lumo-base-color)");
        templateBar.getStyle().set("min-height", "100px"); // Altura mínima para não colapsar
        
        templateScroller.setWidthFull();
        templateScroller.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        templateScroller.getStyle().set("flex-shrink", "0"); 
        templateScroller.setVisible(false);
    }

    private void loadTemplates() {
        templateBar.removeAll();
        List<TaskTemplate> templates = taskTemplateService.getTemplatesByGroupId(group.getId());
        if (templates.isEmpty()) {
            templateScroller.setVisible(false);
            return;
        }

        for (TaskTemplate t : templates) {
            templateBar.add(createTemplateFormCard(t));
        }
        templateScroller.setVisible(true);
    }

    private com.vaadin.flow.component.Component createTemplateFormCard(TaskTemplate template) {
        VerticalLayout card = new VerticalLayout();
        card.setWidth("280px"); // Card mais estreito para caber melhor lateralmente
        card.getStyle().set("flex-shrink", "0");
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("margin-bottom", "4px");

        String emoji = TaskUIUtils.resolveTemplateEmoji(template.getName());
        Span title = new Span(emoji + " " + template.getName());
        title.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-s)");
        card.add(title);

        List<TemplateFieldRelation> fields = taskTemplateService.getFieldRelationsByTemplateId(template.getId());
        java.util.LinkedHashMap<com.xflprflx.neokioski.models.template.TaskFieldDefinition, com.vaadin.flow.component.Component> fieldComponents = new java.util.LinkedHashMap<>();

        for (TemplateFieldRelation rel : fields) {
            InputType type = rel.getTaskFieldDefinition().getInputType();
            String label = rel.getTaskFieldDefinition().getLabel();
            String desc = rel.getTaskFieldDefinition().getDescription();
            List<TaskFieldOption> options = rel.getTaskFieldDefinition().getTaskFieldOptions();
            
            com.vaadin.flow.component.Component fieldComponent = createInputComponent(type, label, desc, rel.isRequired(), options, "", true);

            if (fieldComponent != null) {
                fieldComponents.put(rel.getTaskFieldDefinition(), fieldComponent);
                card.add(fieldComponent);
            }
        }

        Button submitBtn = new Button("Enviar", e -> {
            boolean hasError = false;
            for (TemplateFieldRelation rel : fields) {
                com.vaadin.flow.component.Component comp = fieldComponents.get(rel.getTaskFieldDefinition());
                if (rel.isRequired()) {
                    String value = extractFieldValue(comp);
                    if (value == null || value.trim().isEmpty()) {
                        if (comp instanceof com.vaadin.flow.component.HasValidation hv) {
                            hv.setInvalid(true);
                            hv.setErrorMessage("Obrigatório");
                        }
                        hasError = true;
                    } else if (comp instanceof com.vaadin.flow.component.HasValidation hv) {
                        hv.setInvalid(false);
                    }
                }
            }

            if (hasError) {
                Notification.show("Preencha os campos obrigatórios em " + template.getName(), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            java.util.Map<com.xflprflx.neokioski.models.template.TaskFieldDefinition, String> fieldValues = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<com.xflprflx.neokioski.models.template.TaskFieldDefinition, com.vaadin.flow.component.Component> entry : fieldComponents.entrySet()) {
                String value = extractFieldValue(entry.getValue());
                fieldValues.put(entry.getKey(), value);
                
                // Auto-copy para o clipboard (especialmente se for lista, o separador será \n)
                String valueForClipboard = extractFieldValue(entry.getValue(), "\n");
                if (!valueForClipboard.isBlank()) {
                    copyToClipboard(valueForClipboard);
                }
                
                // Limpar campo após envio
                if (entry.getValue() instanceof com.vaadin.flow.component.HasValue<?, ?> hv) {
                    hv.clear();
                }
            }

            Task task = taskService.createTask(template, fieldValues, currentUser, group);
            
            String alertSound = template.isPlayAlertSound() ? template.getAlertSound() : null;
            String alertRoles = (template.getAlertRoles() != null && !template.getAlertRoles().isEmpty())
                    ? template.getAlertRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.joining(","))
                    : null;
            
            chatService.sendMessageWithTask("📋 Tarefa: " + template.getName(), currentUser, group, task, alertSound, alertRoles);
            
            Notification.show("Tarefa '" + template.getName() + "' criada!", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        submitBtn.setWidthFull();

        if (template.isShowSendButton()) {
            card.add(submitBtn);
        }

        return card;
    }

    private void configureSuggestions() {
        suggestionList = new ListBox<>();
        suggestionWrapper = new Div(suggestionList);
        suggestionWrapper.setId("main-suggestion-wrapper");
        suggestionWrapper.getStyle()
                .set("position", "absolute")
                .set("bottom", "80px")
                .set("left", "20px")
                .set("z-index", "10005")
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.15)")
                .set("border-radius", "8px")
                .set("padding", "4px")
                .set("border", "1px solid var(--lumo-contrast-10pct)");
        suggestionWrapper.setVisible(false);

        activeSuggestionWrapper = suggestionWrapper;
        activeSuggestionWrapper = suggestionWrapper;
        activeSuggestionList = suggestionList;
        activeMentionId = "main-input";

        String mainId = "main-input";
        messageInput.getElement().setAttribute("mention-id", mainId);
        suggestionWrappers.put(mainId, suggestionWrapper);
        suggestionLists.put(mainId, suggestionList);

        // Expor funções globalmente no carregar da página
        com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
            "window.nkChatServer = {" +
            "  show: (query) => $0.$server.showSuggestions(query)," +
            "  hide: () => $0.$server.hideSuggestions()," +
            "  setActive: (id) => $0.$server.setActiveTargetById(id)" +
            "};",
            getElement()
        );

        configureSuggestionList(suggestionList);
        setupMentionLogic(messageInput.getElement(), mainId);
        add(suggestionWrapper);
    }

    private void configureSuggestionList(ListBox<User> list) {
        list.setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(user -> {
            Span name = new Span(user.getName());
            Span username = new Span("@" + user.getUsername());
            username.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            username.getStyle().set("color", "var(--lumo-secondary-text-color)");
            username.getStyle().set("margin-left", "8px");
            return new HorizontalLayout(name, username);
        }));

        list.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                applyMention(event.getValue());
                list.setValue(null);
            }
        });
    }

    @com.vaadin.flow.component.ClientCallable
    public void showSuggestions(String query) {
        if (activeSuggestionWrapper == null || activeSuggestionList == null) {
            this.activeSuggestionWrapper = suggestionWrapper;
            this.activeSuggestionList = suggestionList;
            if (activeSuggestionWrapper == null) return;
        }

        List<User> suggestions = userService.findAll().stream()
                .filter(u -> !u.equals(currentUser))
                .filter(u -> u.getUsername().toLowerCase().contains(query.toLowerCase()) || 
                             u.getName().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

        // Adicionar sugestão virtual para o Criador da Tarefa
        if ("criador".contains(query.toLowerCase()) || "@criador".contains(query.toLowerCase())) {
            User virtualCreator = new User();
            virtualCreator.setName("Criador da Tarefa (Original)");
            virtualCreator.setUsername("criador");
            suggestions.add(0, virtualCreator);
        }

        // Adicionar sugestão virtual para os Autorizadores
        if ("autorizadores".contains(query.toLowerCase()) || "@autorizadores".contains(query.toLowerCase())) {
            User virtualAuts = new User();
            virtualAuts.setName("Usuários Autorizadores");
            virtualAuts.setUsername("autorizadores");
            suggestions.add(0, virtualAuts);
        }

        // Adicionar sugestão virtual para o Usuário Atual (Executante)
        if ("usuario".contains(query.toLowerCase()) || "@usuario".contains(query.toLowerCase())) {
            User virtualUser = new User();
            virtualUser.setName("Usuário Atual (Executante)");
            virtualUser.setUsername("usuario");
            suggestions.add(0, virtualUser);
        }

        // Adicionar sugestão virtual para o Autor Anterior
        if ("autor_anterior".contains(query.toLowerCase()) || "@autor_anterior".contains(query.toLowerCase())) {
            User virtualPrev = new User();
            virtualPrev.setName("Autor Anterior");
            virtualPrev.setUsername("autor_anterior");
            suggestions.add(suggestions.size() > 1 ? 2 : suggestions.size(), virtualPrev);
        }

        if (suggestions.isEmpty()) {
            activeSuggestionWrapper.setVisible(false);
        } else {
            activeSuggestionList.setItems(suggestions);
            activeSuggestionWrapper.setVisible(true);
            activeSuggestionWrapper.getStyle().set("z-index", "10005");
        }
    }

    @com.vaadin.flow.component.ClientCallable
    public void hideSuggestions() {
        if (activeSuggestionWrapper != null) {
            activeSuggestionWrapper.setVisible(false);
        }
    }

    @com.vaadin.flow.component.ClientCallable
    public void setActiveTargetById(String mentionId) {
        if (mentionId == null) {
             mentionId = "main-input";
        }

        Div wrapper = suggestionWrappers.get(mentionId);
        ListBox<User> list = suggestionLists.get(mentionId);
        
        if (wrapper != null && list != null) {
            this.activeMentionId = mentionId;
            this.activeSuggestionWrapper = wrapper;
            this.activeSuggestionList = list;
        }
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
            "target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));\n" +
            "if (host && host.value !== undefined) host.value = newVal;",
            mention, activeMentionId
        );
        hideSuggestions();
    }

    private void setupMentionLogic(Element element, String mentionId) {
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

    private void configureReplyBar() {
        replyBar.setWidthFull();
        replyBar.setPadding(true);
        replyBar.setSpacing(true);
        replyBar.setAlignItems(FlexComponent.Alignment.CENTER);
        replyBar.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        replyBar.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        replyBar.setVisible(false);
    }

    private void setReplyTo(ChatMessage message) {
        this.replyTo = message;
        replyBar.removeAll();
        
        Span replyingTo = new Span("Respondendo a ");
        replyingTo.getStyle().set("margin-right", "8px");
        replyingTo.getStyle().set("font-size", "var(--lumo-font-size-s)");
        replyingTo.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        Span author = new Span(message.getUser().getName());
        author.getStyle().set("font-weight", "bold");
        author.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        String previewText = message.getContent();
        if (previewText.length() > 50) previewText = previewText.substring(0, 47) + "...";
        Span preview = new Span(": " + previewText);
        preview.getStyle().set("font-size", "var(--lumo-font-size-s)");
        preview.getStyle().set("overflow", "hidden");
        preview.getStyle().set("text-overflow", "ellipsis");
        preview.getStyle().set("white-space", "nowrap");
        
        Button cancelBtn = new Button(VaadinIcon.CLOSE.create(), e -> cancelReply());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        
        HorizontalLayout info = new HorizontalLayout(replyingTo, author, preview);
        info.setSpacing(false);
        info.getStyle().set("flex-grow", "1");
        info.setAlignItems(FlexComponent.Alignment.CENTER);
        
        replyBar.add(info, cancelBtn);
        replyBar.setVisible(true);
        messageInput.getElement().executeJs("this.querySelector('textarea').focus()");
    }

    private void cancelReply() {
        this.replyTo = null;
        replyBar.setVisible(false);
    }

    @Override
    public void setParameter(BeforeEvent event, Long groupId) {
        Optional<Group> groupOpt = groupService.findById(groupId);
        if (groupOpt.isEmpty()) {
            event.forwardTo("");
            return;
        }

        this.group = groupOpt.get();
        
        Optional<User> authUser = securityService.getAuthenticatedInternalUser();
        if (authUser.isPresent()) {
            this.currentUser = userService.findById(authUser.get().getId()).orElse(null);
        } else {
            this.currentUser = null;
        }

        if (currentUser != null) {
            notifyStoreOrderCheckbox.setValue(currentUser.isNotifyStoreOrder());
            notifyChatSoundsCheckbox.setValue(currentUser.isNotifyChatSounds());
        }

        // Verificação de associação ao grupo
        if (currentUser == null || !group.getUsers().contains(currentUser)) {
            // Se não for membro, redireciona para a home
            event.forwardTo("");
            // Mostramos uma notificação após o redirecionamento (na home)
            com.vaadin.flow.component.UI.getCurrent().access(() -> {
                Notification n = new Notification("Você não tem permissão para acessar este chat.");
                n.setDuration(3000);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                n.open();
            });
            return;
        }

        this.groupName.setText(group.getName());
        loadMessages();
        loadTemplates();
        updateFilterOptions();
        refreshTaskList();
        setupRealTimeUpdate();
    }

    private void loadMessages() {
        if (group == null || startDatePicker == null || endDatePicker == null || startDatePicker.getValue() == null || endDatePicker.getValue() == null) return;
        
        messageContainer.removeAll();
        messageWrappers.clear();

        // Converter LocalDate para Instant (Início e Fim do dia)
        java.time.LocalDate startL = startDatePicker.getValue();
        java.time.LocalDate endL = endDatePicker.getValue();
        
        java.time.Instant start = startL.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        java.time.Instant end = endL.atTime(23, 59, 59, 999_000_000).atZone(java.time.ZoneId.systemDefault()).toInstant();

        List<ChatMessage> messages = chatService.getMessagesByGroup(group, start, end);
        for (ChatMessage msg : messages) {
            addOrUpdateMessageInUI(msg);
        }
        scrollToBottom();
    }

    private void addOrUpdateMessageInUI(ChatMessage msg) {
        if (msg.getTask() != null) {
            taskIdToMessageId.put(msg.getTask().getId(), msg.getId());

            // Filtro de Autorização
            if (!taskService.canUserViewTask(msg.getTask(), currentUser)) {
                if (messageWrappers.containsKey(msg.getId())) {
                    messageContainer.remove(messageWrappers.get(msg.getId()));
                    messageWrappers.remove(msg.getId());
                }
                return;
            }

            // Se visibilidade de tarefas no chat desativada, não mostrar
            if (!showTasksInChat) {
                if (messageWrappers.containsKey(msg.getId())) {
                    messageContainer.remove(messageWrappers.get(msg.getId()));
                    messageWrappers.remove(msg.getId());
                }
                return;
            }
        }
        if (messageWrappers.containsKey(msg.getId())) {
            HorizontalLayout oldWrapper = messageWrappers.get(msg.getId());
            HorizontalLayout newWrapper;
            if (msg.getTask() != null) {
                newWrapper = createTaskCardLayout(msg.getTask(), msg);
            } else {
                newWrapper = createMessageLayout(msg);
            }
            newWrapper.setId("msg-" + msg.getId());
            messageContainer.replace(oldWrapper, newWrapper);
            messageWrappers.put(msg.getId(), newWrapper);
        } else {
            HorizontalLayout wrapper;
            if (msg.getTask() != null) {
                wrapper = createTaskCardLayout(msg.getTask(), msg);
            } else {
                wrapper = createMessageLayout(msg);
            }
            wrapper.setId("msg-" + msg.getId());
            messageContainer.add(wrapper);
            messageWrappers.put(msg.getId(), wrapper);
        }
    }

    private HorizontalLayout createMessageLayout(ChatMessage msg) {
        boolean isSystemMessage = msg.getUser() == null;
        boolean isMine = !isSystemMessage && currentUser != null && msg.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;

        if (isSystemMessage) {
            VerticalLayout sysLayout = new VerticalLayout();
            sysLayout.setPadding(false);
            sysLayout.setSpacing(false);
            sysLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            // Replicamos a lógica de reply para mensagens de alerta linkarem para a tarefa
            if (msg.getReplyTo() != null) {
                ChatMessage repliedMsg = msg.getReplyTo();
                Span replyAuthor = new Span(repliedMsg.getUser() != null ? repliedMsg.getUser().getName() : "Sistema");
                replyAuthor.getStyle().set("font-weight", "bold");
                replyAuthor.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
                
                String replyContent;
                if (repliedMsg.getTask() != null) {
                    replyContent = "📋 " + (repliedMsg.getTask().getTaskTemplate() != null ? repliedMsg.getTask().getTaskTemplate().getName() : "Tarefa");
                } else {
                    replyContent = repliedMsg.getContent();
                }
                
                Span replyText = new Span(replyContent);
                replyText.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                replyText.getStyle().set("opacity", "0.8");
                replyText.getStyle().set("white-space", "nowrap");
                replyText.getStyle().set("overflow", "hidden");
                replyText.getStyle().set("text-overflow", "ellipsis");
                
                VerticalLayout sysQuote = new VerticalLayout(replyAuthor, replyText);
                sysQuote.setSpacing(false);
                sysQuote.setPadding(false);
                sysQuote.getStyle().set("background-color", "rgba(0, 0, 0, 0.08)");
                sysQuote.getStyle().set("border-left", "3px solid var(--lumo-primary-color)");
                sysQuote.getStyle().set("padding", "var(--lumo-space-xs) var(--lumo-space-s)");
                sysQuote.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
                sysQuote.getStyle().set("border-radius", "4px");
                sysQuote.setWidth("fit-content");
                sysQuote.setMaxWidth("300px");
                sysQuote.getStyle().set("cursor", "pointer");
                sysQuote.addClickListener(e -> {
                    getElement().executeJs("const el = document.getElementById('msg-" + repliedMsg.getId() + "'); if(el) el.scrollIntoView({behavior: 'smooth', block: 'center'});");
                    // Adicionar uma animação rápida de destaque no alvo
                    getElement().executeJs("const el = document.getElementById('msg-" + repliedMsg.getId() + "'); if(el) { el.style.transition = 'box-shadow 0.3s'; el.style.boxShadow = '0 0 0 4px var(--lumo-primary-color-50pct)'; setTimeout(() => { el.style.boxShadow = 'none'; }, 1500); }");
                });
                sysLayout.add(sysQuote);
            }

            Span sysText = new Span("⚙️ " + msg.getContent());
            sysText.getStyle()
                .set("font-style", "italic")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
            
            sysLayout.add(sysText);

            Div sysBubble = new Div(sysLayout);
            sysBubble.getStyle()
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("border-radius", "12px");
            HorizontalLayout sysWrapper = new HorizontalLayout(sysBubble);
            sysWrapper.setWidthFull();
            sysWrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            return sysWrapper;
        }

        // Se a mensagem foi excluída e o usuário não é ADMIN, mostrar apenas o indicador
        if (msg.isDeleted() && !isAdmin) {
            Span deletedText = new Span("Esta mensagem foi apagada.");
            deletedText.getStyle().set("font-style", "italic");
            deletedText.getStyle().set("color", "var(--lumo-secondary-text-color)");
            deletedText.getStyle().set("font-size", "var(--lumo-font-size-s)");

            Div content = new Div(deletedText);
            content.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
            content.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
            content.getStyle().set("border-radius", "18px");

            HorizontalLayout wrapper = new HorizontalLayout(content);
            wrapper.setWidthFull();
            wrapper.setJustifyContentMode(isMine ? FlexComponent.JustifyContentMode.END : FlexComponent.JustifyContentMode.START);
            return wrapper;
        }

        // Verificação de segurança extra (não deveria ocorrer após o check acima)
        if (msg.getUser() == null) {
            HorizontalLayout fallback = new HorizontalLayout();
            return fallback;
        }

        // Avatar
        Avatar avatar = new Avatar(msg.getUser() != null ? msg.getUser().getName() : "?");
        if (isMine) {
            avatar.getStyle().set("margin-left", "var(--lumo-space-m)");
        } else {
            avatar.getStyle().set("margin-right", "var(--lumo-space-m)");
        }

        // Nome (Cabeçalho)
        Span name = new Span(isMine ? "Você" : msg.getUser().getName());
        name.getStyle().set("font-weight", "var(--lumo-font-weight-bold)");
        name.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        HorizontalLayout header = new HorizontalLayout(name);
        header.setSpacing(false);
        header.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
        header.setAlignItems(FlexComponent.Alignment.BASELINE);

        // Citação (Reply)
        VerticalLayout quote = new VerticalLayout();
        if (msg.getReplyTo() != null) {
            ChatMessage repliedMsg = msg.getReplyTo();
            Span replyAuthor = new Span(repliedMsg.getUser().getName());
            replyAuthor.getStyle().set("font-weight", "bold");
            replyAuthor.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
            
            String replyContent;
            if (repliedMsg.getTask() != null) {
                replyContent = "📋 " + (repliedMsg.getTask().getTaskTemplate() != null ? repliedMsg.getTask().getTaskTemplate().getName() : "Tarefa");
            } else {
                replyContent = repliedMsg.getContent();
            }
            
            Span replyText = new Span(replyContent);
            replyText.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            replyText.getStyle().set("opacity", "0.8");
            replyText.getStyle().set("white-space", "nowrap");
            replyText.getStyle().set("overflow", "hidden");
            replyText.getStyle().set("text-overflow", "ellipsis");
            
            quote.add(replyAuthor, replyText);
            quote.setSpacing(false);
            quote.setPadding(false);
            quote.getStyle().set("background-color", "rgba(0, 0, 0, 0.05)");
            quote.getStyle().set("border-left", "3px solid var(--lumo-primary-color)");
            quote.getStyle().set("padding", "var(--lumo-space-xs) var(--lumo-space-s)");
            quote.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
            quote.getStyle().set("border-radius", "4px");
            quote.setWidthFull();
            quote.getStyle().set("cursor", "pointer");
            quote.addClickListener(e -> {
                getElement().executeJs("const el = document.getElementById('msg-" + repliedMsg.getId() + "'); if(el) el.scrollIntoView({behavior: 'smooth', block: 'center'});");
            });
        } else {
            quote.setVisible(false);
        }

        Div content = new Div();
        content.add(quote);
        
        if (msg.isDeleted()) {
            Span deletedBadge = new Span("APAGADA");
            deletedBadge.getStyle()
                    .set("background-color", "var(--lumo-error-color)")
                    .set("color", "white")
                    .set("padding", "2px 8px")
                    .set("border-radius", "4px")
                    .set("font-size", "10px")
                    .set("font-weight", "bold")
                    .set("display", "inline-block")
                    .set("margin-bottom", "4px")
                    .set("box-shadow", "0 2px 4px rgba(0,0,0,0.2)");
            
            Div badgeWrapper = new Div(deletedBadge);
            badgeWrapper.getStyle().set("margin-bottom", "4px");
            content.add(badgeWrapper);
        }

        Div messageText = renderMessageContent(msg.getContent());
        if (msg.isDeleted()) messageText.getStyle().set("opacity", "0.6");
        content.add(messageText);
        
        if (!msg.getEdits().isEmpty()) {
            Span editedTag = new Span(" (editada)");
            editedTag.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
            editedTag.getStyle().set("opacity", "0.6");
            content.add(editedTag);
        }
        content.getStyle().set("padding", "var(--lumo-space-s)");
        content.getStyle().set("font-size", "var(--lumo-font-size-m)");
        content.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        content.getStyle().set("word-break", "break-word");
        content.getStyle().set("padding-left", "15px");
        content.getStyle().set("padding-right", "15px");
        content.getStyle().set("border-radius", "18px");
        content.getStyle().set("min-width", "80px");
        content.getStyle().set("text-align", "left");
        content.setMaxWidth("80%");

        if (isMine) {
            content.getStyle().set("background-color", "var(--lumo-primary-color)");
            content.getStyle().set("color", "white");
        } else {
            content.getStyle().set("background-color", "var(--lumo-contrast-10pct)");
        }

        // Hora
        Span time = new Span(dateTimeFormatter.format(msg.getTimestamp()));
        time.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
        time.getStyle().set("color", "var(--lumo-secondary-text-color)");
        time.getStyle().set("margin-top", "var(--lumo-space-xs)");

        // Reações
        HorizontalLayout reactionsLayout = new HorizontalLayout();
        reactionsLayout.setSpacing(true);
        reactionsLayout.setPadding(false);
        reactionsLayout.getStyle().set("margin-top", "var(--lumo-space-xs)");

        Map<String, List<MessageReaction>> reactionsByEmoji = msg.getReactions().stream()
                .collect(Collectors.groupingBy(MessageReaction::getEmoji));

        reactionsByEmoji.forEach((emoji, reactions) -> {
            Span badge = new Span(emoji + " " + reactions.size());
            badge.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
            badge.getStyle().set("padding", "2px 8px");
            badge.getStyle().set("border-radius", "12px");
            badge.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            badge.getStyle().set("cursor", "pointer");
            
            boolean iReacted = reactions.stream().anyMatch(r -> r.getUser().getId().equals(currentUser.getId()));
            if (iReacted) {
                badge.getStyle().set("border", "1px solid var(--lumo-primary-color)");
                badge.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
            }

            badge.addClickListener(e -> chatService.toggleReaction(msg.getId(), currentUser, emoji));
            reactionsLayout.add(badge);
        });

        // Botão de Reação
        Button reactBtn = new Button(VaadinIcon.SMILEY_O.create());
        reactBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        reactBtn.getStyle().set("opacity", "0.5");
        
        ContextMenu contextMenu = new ContextMenu(reactBtn);
        contextMenu.setOpenOnClick(true);
        String[] commonEmojis = {"👍", "❤️", "😂", "😮", "😢", "🔥"};
        for (String emoji : commonEmojis) {
            contextMenu.addItem(emoji, e -> chatService.toggleReaction(msg.getId(), currentUser, emoji));
        }

        // Botão de Resposta
        Button replyBtn = new Button(VaadinIcon.REPLY.create(), e -> setReplyTo(msg));
        replyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        replyBtn.getStyle().set("opacity", "0.5");

        // Botão de Edição
        Button editBtn = new Button(VaadinIcon.EDIT.create(), e -> openEditDialog(msg));
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        editBtn.getStyle().set("opacity", "0.5");
        editBtn.setVisible(isMine && !msg.isDeleted());

        // Botão de Exclusão
        Button deleteBtn = new Button(VaadinIcon.TRASH.create(), e -> openDeleteDialog(msg));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        deleteBtn.getStyle().set("opacity", "0.5");
        deleteBtn.getStyle().set("color", "var(--lumo-error-text-color)");
        deleteBtn.setVisible((isMine || isAdmin) && !msg.isDeleted());

        // Botão de Histórico (Só para ADMIN)
        Button historyBtn = new Button(VaadinIcon.TIMER.create(), e -> openHistoryDialog(msg));
        historyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        historyBtn.getStyle().set("opacity", "0.5");
        historyBtn.setVisible(isAdmin && (!msg.getEdits().isEmpty() || msg.isDeleted()));

        HorizontalLayout footer = new HorizontalLayout();
        footer.setSpacing(true);
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        
        if (isMine) {
            footer.add(historyBtn, deleteBtn, editBtn, replyBtn, reactBtn, reactionsLayout, time);
        } else {
            footer.add(time, reactionsLayout, reactBtn, replyBtn, editBtn, deleteBtn, historyBtn);
        }

        VerticalLayout bodyLayout = new VerticalLayout(header, content, footer);
        bodyLayout.setSpacing(false);
        bodyLayout.setPadding(false);
        bodyLayout.setAlignItems(isMine ? FlexComponent.Alignment.END : FlexComponent.Alignment.START);

        HorizontalLayout messageLayout = new HorizontalLayout();
        messageLayout.setSpacing(false);
        messageLayout.setPadding(false);
        messageLayout.setMaxWidth("90%");

        if (isMine) {
            messageLayout.add(bodyLayout, avatar);
            messageLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        } else {
            messageLayout.add(avatar, bodyLayout);
            messageLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        }

        HorizontalLayout wrapper = new HorizontalLayout(messageLayout);
        wrapper.setWidthFull();
        wrapper.getStyle().set("padding-top", "var(--lumo-space-xs)");
        wrapper.getStyle().set("padding-bottom", "var(--lumo-space-xs)");
        wrapper.setJustifyContentMode(isMine ? FlexComponent.JustifyContentMode.END : FlexComponent.JustifyContentMode.START);

        return wrapper;
    }


    /**
     * Cria o card visual de uma task para exibir no fluxo de mensagens.
     */
    private HorizontalLayout createTaskCardLayout(Task task, ChatMessage sourceMsgForAlert) {
        boolean isSystemAlert = sourceMsgForAlert != null && sourceMsgForAlert.getUser() == null
                && sourceMsgForAlert.getContent() != null && !sourceMsgForAlert.getContent().isBlank();

        String themeColor = task.getCurrentStep() != null ? task.getCurrentStep().getColorTheme() : "gray";
        String[] colors = TaskUIUtils.getThemeColors(themeColor);
        String bgColor = colors[0];
        String borderColor = colors[1];
        boolean isDark = Boolean.parseBoolean(colors[2]);

        // Container principal do card
        Div card = new Div();
        card.getStyle()
                .set("border", "1px solid " + borderColor)
                .set("border-left", "6px solid " + borderColor)
                .set("border-radius", "12px")
                .set("padding", "var(--lumo-space-m)")
                .set("background", bgColor)
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("max-width", "480px")
                .set("min-width", "280px");

        if (isDark) {
            card.getStyle().set("color", "#ffffff");
            card.getStyle().set("--lumo-body-text-color", "#ffffff");
            card.getStyle().set("--lumo-secondary-text-color", "rgba(255, 255, 255, 0.7)");
            card.getStyle().set("--lumo-contrast-5pct", "rgba(255, 255, 255, 0.1)");
            card.getStyle().set("--lumo-contrast-10pct", "rgba(255, 255, 255, 0.2)");
            card.getStyle().set("--lumo-contrast-20pct", "rgba(255, 255, 255, 0.3)");
        } else {
            // Para cards claros, garantir que o texto secundário tenha contraste suficiente
            card.getStyle().set("--lumo-secondary-text-color", "rgba(0, 0, 0, 0.65)");
        }

        if (taskService.shouldTaskPulse(task)) {
            String rgb = TaskUIUtils.getThemeColorRGB(themeColor);
            
            card.getStyle().set("border", "2px solid rgb(" + rgb + ")");
            card.getStyle().set("border-left", "6px solid rgb(" + rgb + ")");
            
            String pulseName = "pulse-" + (themeColor != null ? themeColor : "gray");
            card.getElement().executeJs(
                "if (!document.getElementById('" + pulseName + "')) {" +
                "  const s = document.createElement('style');" +
                "  s.id = '" + pulseName + "';" +
                "  s.textContent = '@keyframes " + pulseName + " { 0% { box-shadow: 0 0 0 0 rgba(" + rgb + ", 0.7); } 70% { box-shadow: 0 0 0 15px rgba(" + rgb + ", 0); } 100% { box-shadow: 0 0 0 0 rgba(" + rgb + ", 0); } }';" +
                "  document.head.appendChild(s);" +
                "}" +
                "this.style.animation = '" + pulseName + " 2s infinite';"
            );
        }

        if (task.isAlerted()) {
            card.getStyle().set("border", "3px solid #ff1744");
            card.getStyle().set("border-left", "8px solid #ff1744");
            
            card.getElement().executeJs(
                "if (!document.getElementById('alert-pulse')) {" +
                "  const s = document.createElement('style');" +
                "  s.id = 'alert-pulse';" +
                "  s.textContent = '@keyframes alert-pulse { 0% { box-shadow: 0 0 0 0 rgba(255, 23, 68, 0.7); } 70% { box-shadow: 0 0 0 20px rgba(255, 23, 68, 0); } 100% { box-shadow: 0 0 0 0 rgba(255, 23, 68, 0); } }';" +
                "  document.head.appendChild(s);" +
                "}" +
                "this.style.animation = 'alert-pulse 1.5s infinite';"
            );
        }

        // Banner de alerta (mensagem de sistema)
        if (isSystemAlert) {
            Div alertBanner = new Div();
            alertBanner.getStyle()
                .set("background-color", "rgba(255, 23, 68, 0.12)")
                .set("border", "1px solid rgba(255, 23, 68, 0.4)")
                .set("border-radius", "8px")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "#ff1744")
                .set("font-weight", "bold");
            alertBanner.add(new Span("⏰ " + (sourceMsgForAlert != null ? sourceMsgForAlert.getContent() : "")));
            card.add(alertBanner);
        }

        // Emoji + template name (cabeçalho)
        String templateName = task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "Tarefa";
        String emoji = TaskUIUtils.resolveTemplateEmoji(templateName);
        Span header = new Span(emoji + " " + templateName);
        header.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-m)")
                .set("display", "block")
                .set("margin-bottom", "var(--lumo-space-xs)");
        card.add(header);

        // Status badge
        HorizontalLayout statusRow = new HorizontalLayout();
        statusRow.setSpacing(true);
        statusRow.setPadding(false);
        statusRow.setWidthFull();
        statusRow.setAlignItems(FlexComponent.Alignment.CENTER);
        statusRow.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        Span statusLabel = new Span("Status: ");
        statusLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
        statusLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");

        if (task.getCurrentStep() != null) {
            String stepLabel = task.getCurrentStep().getLabel();
            String colorTheme = task.getCurrentStep().getColorTheme();
            Span stepBadge = new Span("⏳ " + stepLabel);
            stepBadge.getStyle()
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "bold");
            TaskUIUtils.applyThemeStyles(stepBadge, colorTheme);
            statusRow.add(statusLabel, stepBadge);
        } else {
            boolean reqAuth = task.getTaskTemplate() != null && task.getTaskTemplate().isRequiresSignature();
            Span pendenteBadge = new Span("⏳ " + (reqAuth ? "Pendente de Autorização" : "Pendente"));
            pendenteBadge.getStyle()
                    .set("background-color", "var(--lumo-contrast-20pct)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "bold");
            statusRow.add(statusLabel, pendenteBadge);
        }

        // Cronômetro
        Span timerSpan = new Span("⏱️ --:--");
        timerSpan.addClassName("task-timer-display");
        timerSpan.getStyle()
                .set("font-variant-numeric", "tabular-nums")
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-left", "auto");
        
        statusRow.add(timerSpan);

        Instant start = task.getCreatedAt();
        boolean finalized = taskService.isTaskFinalized(task);
        Instant end = finalized ? taskService.getFinalizedTimestamp(task) : null;
        
        long startMs = start.toEpochMilli();
        long endMs = (end != null) ? end.toEpochMilli() : 0;

        card.getElement().executeJs(
            "const el = this.querySelector('.task-timer-display');" +
            "if (el) {" +
            "  const start = parseInt($0);" +
            "  const finalized = $1;" +
            "  const end = parseInt($2);" +
            "  const format = (ms) => {" +
            "    if (ms < 0) ms = 0;" +
            "    const s = Math.floor(ms / 1000);" +
            "    const h = Math.floor(s / 3600);" +
            "    const m = Math.floor((s % 3600) / 60);" +
            "    const sec = s % 60;" +
            "    return (h > 0 ? h + ':' : '') + (m < 10 ? '0' + m : m) + ':' + (sec < 10 ? '0' + sec : sec);" +
            "  };" +
            "  if (finalized) {" +
            "    el.innerText = '⏱️ ' + format(end - start);" +
            "  } else {" +
            "    const update = () => {" +
            "      if (!el.isConnected) return;" +
            "      el.innerText = '⏱️ ' + format(Date.now() - start);" +
            "      setTimeout(update, 1000);" +
            "    };" +
            "    update();" +
            "  }" +
            "}",
            String.valueOf(startMs), finalized, String.valueOf(endMs)
        );

        // Designado (criador como referência inicial)
        Span creatorSpan = new Span(" | Criado por: @" + (task.getCreator() != null ? task.getCreator().getUsername() : "?"));
        creatorSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
        creatorSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        statusRow.add(creatorSpan);
        card.add(statusRow);

        // Campos preenchidos
        if (!task.getTaskFieldValues().isEmpty()) {
            Div fieldsDiv = new Div();
            fieldsDiv.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "8px")
                    .set("padding", "var(--lumo-space-s)")
                    .set("margin-bottom", "var(--lumo-space-s)")
                    .set("font-size", "var(--lumo-font-size-s)");
            for (TaskFieldValue fv : task.getTaskFieldValues()) {
                String fieldLabel = fv.getTaskFieldDefinition() != null ? fv.getTaskFieldDefinition().getLabel() : "Campo";
                String valStr = fv.getValue();
                
                Div row = new Div();
                row.getStyle().set("display", "flex").set("align-items", "center").set("gap", "4px");
                
                Span lbl = new Span(fieldLabel + ": ");
                lbl.getStyle().set("font-weight", "bold");
                
                Span val = new Span(valStr);
                
                // Botão de cópia discreto
                Button copyBtn = new Button(VaadinIcon.COPY.create(), e -> {
                    // Para cópia manual, também respeitamos o formato SAP se for lista
                    // Como fv.getValue() já é uma string consolidada, se for uma lista separada por vírgula no DB,
                    // talvez precisemos re-formatar. Mas geralmente o usuário quer o que está ali.
                    // Se o valor contiver vírgulas e for suspeito de lista, poderíamos trocar por \n.
                    // No entanto, para ser preciso, deveríamos usar o valor original se disponível.
                    // Aqui fv.getValue() é a string salva.
                    copyToClipboard(valStr.replace(", ", "\n"));
                });
                copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
                copyBtn.getStyle().set("padding", "0").set("height", "auto").set("min-width", "auto");
                copyBtn.setTooltipText("Copiar valor");

                row.add(lbl, val, copyBtn);
                fieldsDiv.add(row);
            }
            card.add(fieldsDiv);
        }

        List<TemplateStepRelation> allRelations = taskTemplateService.getStepRelationsByTemplateId(task.getTaskTemplate().getId());
        List<TaskStepDefinition> allowedSteps = taskService.getAllowedNextSteps(task);
        
        // Mapear os steps permitidos de volta para suas relações para ter acesso aos metadados (cores, campos de resposta, etc)
        List<TemplateStepRelation> visibleRelations = allRelations.stream()
                .filter(r -> allowedSteps.contains(r.getTaskStepDefinition()))
                .collect(Collectors.toList());

        // Mapeamento de StepId -> Campo de Resposta
        Map<Long, com.vaadin.flow.component.Component> responseFieldsByStepId = new HashMap<>();
        
        // Criar campos de resposta se a tarefa não estiver finalizada
        if (!taskService.isTaskFinalized(task)) {
            for (TemplateStepRelation sr : visibleRelations) {
                if (sr.isHasResponseField()) {
                    String stepLabel = sr.getTaskStepDefinition().getLabel();
                    String label = sr.getResponseFieldLabel() != null ? sr.getResponseFieldLabel() : "Resposta / Comentário";
                    InputType type = sr.getResponseInputType() != null ? sr.getResponseInputType() : InputType.TEXT;
                    String placeholder = "Preencha se " + stepLabel;
                    
                    com.vaadin.flow.component.Component responseComponent = createInputComponent(type, label, null, sr.isResponseRequired(), sr.getResponseOptions(), placeholder, sr.isResponseIsCompact());

                    responseFieldsByStepId.put(sr.getTaskStepDefinition().getId(), responseComponent);
                    card.add(responseComponent);
                }
            }
        }

        // Botões dos steps
        FlexLayout actions = new FlexLayout();
        actions.getStyle().set("flex-wrap", "wrap");
        actions.getStyle().set("gap", "var(--lumo-space-s)");

        for (TemplateStepRelation sr : visibleRelations) {
            TaskStepDefinition step = sr.getTaskStepDefinition();
            
            // Verificação de permissão de execução (regra administrativa/autorização)
            if (!taskService.canUserExecuteStep(task, currentUser)) {
                continue;
            }

            if (task.getCurrentStep() != null && step.getId().equals(task.getCurrentStep().getId())) {
                continue;
            }

            Button stepBtn = new Button(step.getLabel(), ev -> {
                com.vaadin.flow.component.Component comp = responseFieldsByStepId.get(step.getId());
                String response = extractFieldValue(comp);
                
                if (sr.isResponseRequired() && (response == null || response.isBlank())) {
                    if (comp instanceof com.vaadin.flow.component.HasValidation hv) {
                        hv.setInvalid(true);
                        hv.setErrorMessage("A resposta é obrigatória para avançar.");
                    }
                    Notification n = new Notification("A resposta é obrigatória para avançar para '" + step.getLabel() + "'.");
                    n.setDuration(3000);
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    n.open();
                    return;
                } else if (comp instanceof com.vaadin.flow.component.HasValidation hv) {
                    hv.setInvalid(false);
                }

                // Auto-copy da resposta (especialmente se for lista para o SAP)
                String responseForClipboard = extractFieldValue(comp, "\n");
                if (!responseForClipboard.isBlank()) {
                    copyToClipboard(responseForClipboard);
                }

                taskService.executeStep(task.getId(), step, currentUser, response);
            });
            stepBtn.setEnabled(!taskService.isTaskFinalized(task));
            stepBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            if (sr.isFinalizer()) {
                stepBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            } else {
                stepBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            }
            actions.add(stepBtn);
        }

        // Botão Ver Detalhes
        Button detailsBtn = new Button("Ver Detalhes", ev -> openTaskDetailsDialog(task));
        detailsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        actions.add(detailsBtn);

        card.add(actions);

        // Wrapper alinhado à esquerda (tasks são neutras, não têm side mine/other)
        HorizontalLayout wrapper = new HorizontalLayout(card);
        wrapper.setWidthFull();
        wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        wrapper.getStyle().set("padding-top", "var(--lumo-space-xs)");
        wrapper.getStyle().set("padding-bottom", "var(--lumo-space-xs)");
        return wrapper;
    }

    private com.vaadin.flow.component.Component createInputComponent(InputType type, String label, String desc, boolean required, List<TaskFieldOption> optionsObj, String placeholder, boolean isCompact) {
        List<String> options = optionsObj != null ? optionsObj.stream().map(TaskFieldOption::getOptionLabel).toList() : new ArrayList<>();
        com.vaadin.flow.component.Component fieldComponent = null;

        switch (type) {
            case TEXT:
                if (isCompact) {
                    com.vaadin.flow.component.textfield.TextField tf = new com.vaadin.flow.component.textfield.TextField(label);
                    if (desc != null) tf.setHelperText(desc);
                    if (placeholder != null) tf.setPlaceholder(placeholder);
                    tf.setWidthFull();
                    tf.setRequiredIndicatorVisible(required);
                    fieldComponent = tf;
                } else {
                    com.vaadin.flow.component.textfield.TextArea ta = new com.vaadin.flow.component.textfield.TextArea(label);
                    if (desc != null) ta.setHelperText(desc);
                    if (placeholder != null) ta.setPlaceholder(placeholder);
                    ta.setWidthFull();
                    ta.setRequiredIndicatorVisible(required);
                    fieldComponent = ta;
                }
                break;
            case DATE:
                com.vaadin.flow.component.datepicker.DatePicker dp = new com.vaadin.flow.component.datepicker.DatePicker(label);
                if (desc != null) dp.setHelperText(desc);
                if (placeholder != null) dp.setPlaceholder(placeholder);
                dp.setWidthFull();
                dp.setRequiredIndicatorVisible(required);
                fieldComponent = dp;
                break;
            case TIME:
                com.vaadin.flow.component.timepicker.TimePicker tp = new com.vaadin.flow.component.timepicker.TimePicker(label);
                if (desc != null) tp.setHelperText(desc);
                if (placeholder != null) tp.setPlaceholder(placeholder);
                tp.setWidthFull();
                tp.setRequiredIndicatorVisible(required);
                fieldComponent = tp;
                break;
            case NUMBER:
                com.vaadin.flow.component.textfield.NumberField nf = new com.vaadin.flow.component.textfield.NumberField(label);
                if (desc != null) nf.setHelperText(desc);
                if (placeholder != null) nf.setPlaceholder(placeholder);
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
                if (desc != null) radio.setHelperText(desc);
                radio.setItems(options);
                radio.setRequiredIndicatorVisible(required);
                fieldComponent = radio;
                break;
            case DROPDOWN:
                com.vaadin.flow.component.combobox.ComboBox<String> combo = new com.vaadin.flow.component.combobox.ComboBox<>(label);
                if (desc != null) combo.setHelperText(desc);
                if (placeholder != null) combo.setPlaceholder(placeholder);
                combo.setWidthFull();
                combo.setRequiredIndicatorVisible(required);
                combo.setItems(options);
                fieldComponent = combo;
                break;
            case MULTISELECT_DROPDOWN:
                com.vaadin.flow.component.combobox.MultiSelectComboBox<String> multiCombo = new com.vaadin.flow.component.combobox.MultiSelectComboBox<>(label);
                if (desc != null) multiCombo.setHelperText(desc);
                if (placeholder != null) multiCombo.setPlaceholder(placeholder);
                multiCombo.setWidthFull();
                multiCombo.setRequiredIndicatorVisible(required);
                multiCombo.setItems(options);
                fieldComponent = multiCombo;
                break;
            case TEXT_LIST:
                com.vaadin.flow.component.combobox.MultiSelectComboBox<String> textList = new com.vaadin.flow.component.combobox.MultiSelectComboBox<>(label);
                if (desc != null) textList.setHelperText(desc);
                if (placeholder != null) textList.setPlaceholder(placeholder);
                textList.setWidthFull();
                textList.setRequiredIndicatorVisible(required);
                textList.setAllowCustomValue(true);
                
                // Inicializar com opções se existirem
                if (options != null && !options.isEmpty()) {
                    textList.setItems(options);
                }

                textList.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    if (customValue == null || customValue.isBlank()) return;

                    // Capturar valores atuais ANTES de alterar os itens para evitar perda de seleção
                    Set<String> currentValues = new LinkedHashSet<>(textList.getValue());
                    currentValues.add(customValue);

                    // Atualizar lista de itens totais (DataView) para que o novo valor seja um item válido
                    Set<String> allItems = new LinkedHashSet<>(textList.getListDataView().getItems().collect(Collectors.toList()));
                    allItems.add(customValue);
                    textList.setItems(allItems);

                    // Definir a nova seleção completa, garantindo que os chips anteriores permaneçam
                    textList.setValue(currentValues);
                });
                fieldComponent = textList;
                break;
        }
        return fieldComponent;
    }

    private String extractFieldValue(com.vaadin.flow.component.Component comp) {
        return extractFieldValue(comp, ", ");
    }

    private String extractFieldValue(com.vaadin.flow.component.Component comp, String separator) {
        if (comp == null) return "";
        if (comp instanceof com.vaadin.flow.component.HasValue<?, ?> hv) {
            Object val = hv.getValue();
            if (val == null) return "";
            if (val instanceof java.util.Collection<?> col) {
                return col.stream().map(Object::toString).collect(Collectors.joining(separator));
            }
            if (val instanceof java.time.LocalDate ld) {
                return ld.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            }
            if (val instanceof java.time.LocalTime lt) {
                return lt.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
            }
            return val.toString();
        }
        return "";
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isBlank()) return;
        
        getElement().executeJs(
            "navigator.clipboard.writeText($0).then(() => {" +
            "  console.log('Copiado para o clipboard');" +
            "}).catch(err => {" +
            "  console.error('Erro ao copiar: ', err);" +
            "});",
            text
        );
        
        Notification n = new Notification("Copiado!");
        n.setDuration(1500);
        n.setPosition(Notification.Position.BOTTOM_END);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        n.open();
    }

    private void openTaskDetailsDialog(Task task) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setHeaderTitle("Detalhes da Tarefa");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        String tName = task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "";
        Span title = new Span(TaskUIUtils.resolveTemplateEmoji(tName) + " " + (tName.isEmpty() ? "Tarefa" : tName));
        title.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-l)");
        content.add(title);

        if (task.getCurrentStep() != null) {
            String stepLabel = task.getCurrentStep().getLabel();
            String colorTheme = task.getCurrentStep().getColorTheme();
            Span stepBadge = new Span(stepLabel);
            stepBadge.getStyle()
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "bold");
            
            TaskUIUtils.applyThemeStyles(stepBadge, colorTheme);
            
            HorizontalLayout statusRow = new HorizontalLayout(new Span("Status: "), stepBadge);
            statusRow.setAlignItems(FlexComponent.Alignment.CENTER);
            content.add(statusRow);
        }

        if (task.getCreator() != null) {
            content.add(new Span("Criado por: @" + task.getCreator().getUsername()));
        }

        if (!task.getTaskFieldValues().isEmpty()) {
            Span fieldsTitle = new Span("Campos:");
            fieldsTitle.getStyle().set("font-weight", "bold");
            content.add(fieldsTitle);
            for (TaskFieldValue fv : task.getTaskFieldValues()) {
                String lbl = fv.getTaskFieldDefinition() != null ? fv.getTaskFieldDefinition().getLabel() : "Campo";
                String valStr = fv.getValue();
                
                HorizontalLayout row = new HorizontalLayout(new Span("• " + lbl + ": " + valStr));
                row.setSpacing(true);
                row.setAlignItems(FlexComponent.Alignment.CENTER);
                
                Button copyBtn = new Button(VaadinIcon.COPY.create(), e -> copyToClipboard(valStr.replace(", ", "\n")));
                copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
                copyBtn.setTooltipText("Copiar");
                
                row.add(copyBtn);
                content.add(row);
            }
        }

        if (!task.getTaskEvents().isEmpty()) {
            Span histTitle = new Span("Histórico:");
            histTitle.getStyle().set("font-weight", "bold");
            content.add(histTitle);
            for (com.xflprflx.neokioski.models.TaskEvent ev : task.getTaskEvents()) {
                String who = ev.getUser() != null ? "@" + ev.getUser().getUsername() : "sistema";
                String when = ev.getTimestamp() != null ? dateTimeFormatter.format(ev.getTimestamp()) : "";
                String step = ev.getTaskStepDefinition() != null ? ev.getTaskStepDefinition().getLabel() : "";
                String line = "• " + who + " → " + step + " (" + when + ")";
                content.add(new Span(line));
                if (ev.getResponse() != null && !ev.getResponse().isBlank()) {
                    String respStr = ev.getResponse();
                    Span resp = new Span("  Resp: " + respStr);
                    resp.getStyle().set("font-style", "italic").set("color", "var(--lumo-secondary-text-color)").set("margin-left", "20px");
                    
                    Button copyBtn = new Button(VaadinIcon.COPY.create(), e -> copyToClipboard(respStr.replace(", ", "\n")));
                    copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
                    copyBtn.getStyle().set("margin-left", "8px");
                    copyBtn.setTooltipText("Copiar resposta");
                    
                    HorizontalLayout respRow = new HorizontalLayout(resp, copyBtn);
                    respRow.setSpacing(false);
                    respRow.setAlignItems(FlexComponent.Alignment.CENTER);
                    content.add(respRow);
                }
            }
        }

        Scroller scroller2 = new Scroller(content);
        scroller2.setMaxHeight("400px");
        dialog.add(scroller2);
        dialog.getFooter().add(new Button("Fechar", ev -> dialog.close()));
        dialog.open();
    }

    private Div renderMessageContent(String content) {
        Div container = new Div();
        if (content == null || content.isEmpty()) return container;

        // Regex to find mentions like @username
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        int lastEnd = 0;
        while (matcher.find()) {
            // Append text before the mention
            if (matcher.start() > lastEnd) {
                container.add(new Span(content.substring(lastEnd, matcher.start())));
            }

            String username = matcher.group(1);
            Span mention = new Span("@" + username);
            mention.getStyle()
                    .set("font-weight", "bold")
                    .set("color", "var(--lumo-primary-text-color)")
                    .set("background", "var(--lumo-primary-color-10pct)")
                    .set("padding", "0 4px")
                    .set("border-radius", "4px")
                    .set("white-space", "nowrap")
                    .set("display", "inline-block")
                    .set("vertical-align", "middle");
            container.add(mention);

            lastEnd = matcher.end();
        }

        // Append remaining text
        if (lastEnd < content.length()) {
            container.add(new Span(content.substring(lastEnd)));
        }

        return container;
    }

    private boolean isUserMentioned(String content, String username) {
        if (content == null || username == null) return false;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@" + java.util.regex.Pattern.quote(username) + "(\\b|$)");
        return pattern.matcher(content).find();
    }

    private void setupRealTimeUpdate() {
        if (listener != null && group != null) {
            chatService.unregister(group.getId(), listener);
        }

        listener = msg -> {
            // Ignorar mensagens sem ID (nulo) - as voláteis agora usam ID negativo
            if (msg.getId() == null) return;

            if (getUI().isPresent()) {
                getUI().get().access(() -> {
                    boolean isNew = !messageWrappers.containsKey(msg.getId());
                    addOrUpdateMessageInUI(msg);
                    scrollToBottom();
                    
                    if (msg.getUser() == null || !msg.getUser().getId().equals(currentUser.getId())) {
                        boolean isReplyToMe = msg.getReplyTo() != null && 
                                             msg.getReplyTo().getUser() != null &&
                                             msg.getReplyTo().getUser().getId().equals(currentUser.getId());
                        boolean mentioned = currentUser != null && isUserMentioned(msg.getContent(), currentUser.getUsername());
                        
                        if (isNew) {
                            if (isReplyToMe) {
                                String title = msg.getUser() != null ? msg.getUser().getName() : "Sistema";
                                Notification n = new Notification(title + " respondeu a você!");
                                n.setDuration(5000);
                                n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                                n.open();
                            } else if (mentioned) {
                                String title = msg.getUser() != null ? msg.getUser().getName() : "Sistema";
                                Notification n = new Notification(title + " mencionou você!");
                                n.setDuration(5000);
                                n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                                n.open();
                            }
                        } else {
                            if (mentioned && !msg.isDeleted()) {
                                String title = msg.getUser() != null ? msg.getUser().getName() : "Sistema";
                                Notification n = new Notification(title + " mencionou você na edição da mensagem!");
                                n.setDuration(5000);
                                n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                                n.open();
                            }
                        }
                    }
                });
            }
        };

        taskListener = task -> {
            if (getUI().isPresent() && group != null && task != null) {
                getUI().get().access(() -> {
                    try {
                        // Atualizar o card na linha do chat se existir
                        Long messageId = taskIdToMessageId != null ? taskIdToMessageId.get(task.getId()) : null;
                        if (messageId != null) {
                            chatService.getMessagesByGroup(group).stream()
                                .filter(m -> m.getId().equals(messageId))
                                .findFirst()
                                .ifPresent(this::addOrUpdateMessageInUI);
                        }
                        // Atualizar a lista de tarefas lateral sempre
                        refreshTaskList();
                    } catch (Exception e) {
                        System.err.println("Erro no taskListener do ChatView: " + e.getMessage());
                    }
                });
            }
        };

        if (group != null) {
            chatService.register(group.getId(), listener);
            taskService.register(group.getId(), taskListener);
        }
    }

    private void openEditDialog(ChatMessage msg) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        dialog.setMaxWidth("90%");
        dialog.setHeaderTitle("Editar Mensagem");

        com.vaadin.flow.component.textfield.TextArea textArea = new com.vaadin.flow.component.textfield.TextArea();
        textArea.setValue(msg.getContent());
        textArea.setWidthFull();
        textArea.setMinHeight("100px");

        ListBox<User> dialogSuggestionList = new ListBox<>();
        dialogSuggestionList.setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(user -> {
            Span name = new Span(user.getName());
            name.getStyle().set("font-weight", "bold");
            Span username = new Span("@" + user.getUsername());
            username.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            username.getStyle().set("color", "var(--lumo-secondary-text-color)");
            
            VerticalLayout info = new VerticalLayout(name, username);
            info.getStyle()
                    .set("display", "flex")
                    .set("flex-direction", "column")
                    .set("gap", "2px")
                    .set("white-space", "nowrap");
            info.setSpacing(false);
            info.setPadding(false);
            return info;
        }));

        Div dialogSuggestionWrapper = new Div(dialogSuggestionList);
        dialogSuggestionWrapper.setId("dialog-suggestion-wrapper-" + System.currentTimeMillis());
        dialogSuggestionWrapper.getStyle()
                .set("position", "absolute")
                .set("z-index", "10001")
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.15)")
                .set("border-radius", "8px")
                .set("padding", "4px")
                .set("border", "1px solid var(--lumo-contrast-10pct)");
        dialogSuggestionWrapper.setVisible(false);

        configureSuggestionList(dialogSuggestionList);
        // Ao focar ou digitar, definir este target como ativo
        String dialogId = "dialog-" + System.currentTimeMillis();
        textArea.getElement().setAttribute("mention-id", dialogId);
        suggestionWrappers.put(dialogId, dialogSuggestionWrapper);
        suggestionLists.put(dialogId, dialogSuggestionList);
        
        this.activeMentionId = dialogId;
        this.activeMentionId = dialogId;
        this.activeSuggestionWrapper = dialogSuggestionWrapper;
        this.activeSuggestionList = dialogSuggestionList;

        setupMentionLogic(textArea.getElement(), dialogId);

        Button saveBtn = new Button("Salvar", e -> {
            chatService.editMessage(msg.getId(), textArea.getValue(), currentUser);
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button cancelBtn = new Button("Cancelar", e -> dialog.close());
        
        Div container = new Div(textArea, dialogSuggestionWrapper);
        container.getStyle().set("position", "relative");
        
        dialog.add(container);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void openDeleteDialog(ChatMessage msg) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Excluir Mensagem");
        dialog.add(new Span("Tem certeza que deseja excluir esta mensagem? Esta ação não pode ser desfeita."));

        Button deleteBtn = new Button("Excluir", e -> {
            chatService.deleteMessage(msg.getId(), currentUser);
            dialog.close();
        });
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        
        Button cancelBtn = new Button("Cancelar", e -> dialog.close());
        
        dialog.getFooter().add(cancelBtn, deleteBtn);
        dialog.open();
    }

    private void openHistoryDialog(ChatMessage msg) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Histórico de Edições");
        dialog.setWidth("500px");

        VerticalLayout historyContainer = new VerticalLayout();
        historyContainer.setPadding(false);
        historyContainer.setSpacing(true);

        // Mensagem Atual (ou deletada)
        Div currentVersion = new Div();
        currentVersion.getStyle().set("padding", "var(--lumo-space-s)");
        currentVersion.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        currentVersion.getStyle().set("border-radius", "8px");
        currentVersion.getStyle().set("width", "100%");
        
        Span currentLabel = new Span(msg.isDeleted() ? "Conteúdo Original (na exclusão):" : "Conteúdo Atual:");
        currentLabel.getStyle().set("font-weight", "bold");
        currentLabel.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        
        Div currentContent = new Div(new Span(msg.getContent()));
        currentContent.getStyle().set("margin-top", "var(--lumo-space-xs)");
        
        currentVersion.add(currentLabel, currentContent);
        historyContainer.add(currentVersion);

        // Edições Anteriores
        for (MessageEdit edit : msg.getEdits()) {
            Div editEntry = new Div();
            editEntry.getStyle().set("padding", "var(--lumo-space-s)");
            editEntry.getStyle().set("border-left", "2px solid var(--lumo-contrast-20pct)");
            editEntry.getStyle().set("width", "100%");
            
            Span editTime = new Span("Editado em " + dateTimeFormatter.format(edit.getEditTimestamp()));
            editTime.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
            editTime.getStyle().set("color", "var(--lumo-secondary-text-color)");
            
            Div oldContent = new Div(new Span(edit.getOldContent()));
            oldContent.getStyle().set("margin-top", "var(--lumo-space-xs)");
            oldContent.getStyle().set("opacity", "0.8");
            
            editEntry.add(editTime, oldContent);
            historyContainer.add(editEntry);
        }

        if (msg.getEdits().isEmpty() && !msg.isDeleted()) {
            historyContainer.add(new Span("Nenhuma edição realizada."));
        }

        Scroller historyScroller = new Scroller(historyContainer);
        historyScroller.setMaxHeight("400px");
        dialog.add(historyScroller);
        
        Button closeBtn = new Button("Fechar", e -> dialog.close());
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private void scrollToBottom() {
        scroller.getElement().executeJs("this.scrollTo(0, this.scrollHeight)");
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        if (listener != null && group != null) {
            chatService.unregister(group.getId(), listener);
        }
        if (taskListener != null && group != null) {
            taskService.unregister(group.getId(), taskListener);
        }
        super.onDetach(detachEvent);
    }
}
