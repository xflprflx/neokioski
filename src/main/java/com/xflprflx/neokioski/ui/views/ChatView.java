package com.xflprflx.neokioski.ui.views;
// v2 - forced refresh

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.xflprflx.neokioski.models.*;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.template.*;
import com.xflprflx.neokioski.security.SecurityService;
import com.xflprflx.neokioski.services.*;
import com.xflprflx.neokioski.ui.components.TaskDetailsDialog;
import com.xflprflx.neokioski.ui.utils.TaskUIUtils;
import jakarta.annotation.security.PermitAll;

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
    private final PresenceService presenceService;
    private final TaskSlaService taskSlaService;

    private Group group;
    private User currentUser;
    private List<TaskTemplate> groupTemplates = new ArrayList<>();
    
    private final VerticalLayout messageContainer = new VerticalLayout();
    private final Scroller scroller = new Scroller(messageContainer);
    private final MessageInput messageInput = new MessageInput();
    private final HorizontalLayout onlineUsersAvatars = new HorizontalLayout();
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
    private String filterSector = "todos";
    private ComboBox<String> ownerFilter;
    private ComboBox<String> statusFilter;
    private ComboBox<String> sectorFilter;
    private com.vaadin.flow.component.datepicker.DatePicker startDatePicker;
    private com.vaadin.flow.component.datepicker.DatePicker endDatePicker;
    private com.vaadin.flow.component.checkbox.Checkbox showTasksInChatCheckbox;
    private com.vaadin.flow.component.checkbox.Checkbox notifyStoreOrderCheckbox;
    private com.vaadin.flow.component.checkbox.Checkbox notifyChatSoundsCheckbox;
    
    private Consumer<ChatMessage> listener;
    private Consumer<Task> taskListener;
    private Consumer<PresenceService.PresenceEvent> presenceListener;

    private ChatMessage replyTo = null;

    // Busca
    private int searchPage = 0;
    private String lastSearchQuery = "";
    private final Dialog searchDialog = new Dialog();
    private final VerticalLayout searchResultsContainer = new VerticalLayout();
    private final Button showMoreSearchBtn = new Button("Mostrar Mais");

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Map<Long, HorizontalLayout> taskCardWrappersInSideList = new HashMap<>();

    public ChatView(ChatService chatService, GroupService groupService, SecurityService securityService, UserService userService, TaskTemplateService taskTemplateService, TaskService taskService, PresenceService presenceService, TaskSlaService taskSlaService) {
        this.chatService = chatService;
        this.groupService = groupService;
        this.securityService = securityService;
        this.userService = userService;
        this.taskTemplateService = taskTemplateService;
        this.taskService = taskService;
        this.presenceService = presenceService;
        this.taskSlaService = taskSlaService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        
        configureUI();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (group != null && currentUser != null) {
                // Ao enviar, forçar data para hoje para ver a própria mensagem (conforme solicitado pelo usuário)
                java.time.LocalDate today = java.time.LocalDate.now();
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
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
        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(controls, mainSplit);
        setFlexGrow(1, mainSplit);
        configureSuggestions();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
            if (e.isFromClient()) loadMessages(); // Recarregar mensagens para aplicar filtro
        });

        // Controles de áudio
        notifyStoreOrderCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Alerta Pedidos", currentUser != null && currentUser.isNotifyStoreOrder());
        notifyStoreOrderCheckbox.addValueChangeListener(e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (currentUser != null && e.isFromClient()) {
                userService.updateNotifyStoreOrder(currentUser.getId(), e.getValue());
                currentUser.setNotifyStoreOrder(e.getValue());
                Notification.show("Alerta de pedidos " + (e.getValue() ? "ativado" : "desativado"), 2000, Notification.Position.BOTTOM_END);
            }
        });

        notifyChatSoundsCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Sons Chat", currentUser != null && currentUser.isNotifyChatSounds());
        notifyChatSoundsCheckbox.addValueChangeListener(e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (currentUser != null && e.isFromClient()) {
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
        startDatePicker.addValueChangeListener(e -> {
            if (e.isFromClient()) refreshAllComponents();
        });

        endDatePicker = new com.vaadin.flow.component.datepicker.DatePicker("Fim");
        endDatePicker.setValue(java.time.LocalDate.now());
        endDatePicker.setWidth("140px");
        endDatePicker.addValueChangeListener(e -> {
            if (e.isFromClient()) refreshAllComponents();
        });

        HorizontalLayout dateFilters = new HorizontalLayout(startDatePicker, endDatePicker);
        dateFilters.setSpacing(true);
        dateFilters.setAlignItems(FlexComponent.Alignment.CENTER);
        dateFilters.getStyle().set("margin-left", "var(--lumo-space-m)");

        onlineUsersAvatars.setSpacing(false);
        onlineUsersAvatars.getStyle().set("margin-left", "var(--lumo-space-s)");

        // Busca no Header
        com.vaadin.flow.component.textfield.TextField searchField = new com.vaadin.flow.component.textfield.TextField();
        searchField.setPlaceholder("Buscar no chat...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidth("250px");
        searchField.addKeyDownListener(com.vaadin.flow.component.Key.ENTER, e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (searchField.getValue() != null && !searchField.getValue().isBlank()) {
                openSearchDialog(searchField.getValue());
            }
        });

        Button searchBtn = new Button(VaadinIcon.SEARCH.create(), e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (searchField.getValue() != null && !searchField.getValue().isBlank()) {
                openSearchDialog(searchField.getValue());
            }
        });
        searchBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout searchLayout = new HorizontalLayout(searchField, searchBtn);
        searchLayout.setSpacing(false);
        searchLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        searchLayout.getStyle().set("margin-left", "var(--lumo-space-m)");

        HorizontalLayout header = new HorizontalLayout(groupName, onlineUsersAvatars, searchLayout, toggleButtons, showTasksInChatCheckbox, dateFilters, audioControls);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setPadding(true);
        header.setSpacing(true);
        header.getStyle().set("background", "var(--lumo-base-color)");
        header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        header.getStyle().set("min-height", "60px");
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return header;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
            if (!e.isFromClient()) return;
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
            if (!e.isFromClient()) return;
            String val = e.getValue();
            filterStatus = (val == null || val.equals("Todos")) ? "todos" : val;
            refreshTaskList();
        });

        sectorFilter = new ComboBox<>("Setor");
        sectorFilter.setWidthFull();
        sectorFilter.setClearButtonVisible(true);
        sectorFilter.addValueChangeListener(e -> {
            if (!e.isFromClient()) return;
            String val = e.getValue();
            filterSector = (val == null || val.equals("Todos")) ? "todos" : val;
            refreshTaskList();
        });

        HorizontalLayout filters = new HorizontalLayout(ownerFilter, sectorFilter, statusFilter);
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void refreshAllComponents() {
        loadMessages();
        refreshTaskList();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void refreshTaskList() {
        if (group == null || startDatePicker == null || endDatePicker == null || startDatePicker.getValue() == null || endDatePicker.getValue() == null) return;
        
        // Converter LocalDate para Instant (Início e Fim do dia)
        java.time.Instant start = startDatePicker.getValue().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        java.time.Instant end = endDatePicker.getValue().atTime(23, 59, 59, 999_000_000).atZone(java.time.ZoneId.systemDefault()).toInstant();

        List<Task> tasks = taskService.getTasksByGroup(group, start, end);
        
        // Aplicar filtros e atualizar a UI de forma granular
        Set<Long> tasksThatShouldBeVisible = tasks.stream()
                .filter(t -> taskService.canUserViewTask(t, currentUser))
                .filter(this::matchesFilters)
                .map(Task::getId)
                .collect(Collectors.toSet());

        // Remover cards que não devem mais estar visíveis
        List<Long> toRemove = taskCardWrappersInSideList.keySet().stream()
                .filter(id -> !tasksThatShouldBeVisible.contains(id))
                .toList();
        toRemove.forEach(id -> {
            HorizontalLayout wrapper = taskCardWrappersInSideList.remove(id);
            if (wrapper != null) taskListArea.remove(wrapper);
        });

        // Adicionar ou Atualizar cards
        tasks.stream()
                .filter(t -> tasksThatShouldBeVisible.contains(t.getId()))
                .sorted(java.util.Comparator.comparing(Task::getCreatedAt))
                .forEach(this::addOrUpdateTaskInSideList);

        updateEmptyState();
    }

    private boolean matchesFilters(Task t) {
        // Filtro de Responsável
        if (!filterOwner.equals("todas")) {
            if (filterOwner.equals("minhas")) {
                if (currentUser == null) return true;
                boolean isCreator = t.getCreator() != null && t.getCreator().getId().equals(currentUser.getId());
                boolean participated = t.getTaskEvents().stream()
                        .anyMatch(ev -> ev.getUser() != null && ev.getUser().getId().equals(currentUser.getId()));
                if (!isCreator && !participated) return false;
            } else {
                if (t.getCreator() == null || !t.getCreator().getUsername().equals(filterOwner)) return false;
            }
        }

        // Filtro de Status
        if (!filterStatus.equals("todos")) {
            if (filterStatus.equals("Pendentes")) {
                if (taskService.isTaskFinalized(t)) return false;
            } else if (filterStatus.equals("Finalizadas")) {
                if (!taskService.isTaskFinalized(t)) return false;
            } else {
                if (t.getCurrentStep() == null || !t.getCurrentStep().getLabel().equals(filterStatus)) return false;
            }
        }

        // Filtro de Setor
        if (!filterSector.equals("todos")) {
            if (taskService.isTaskFinalized(t)) return false;
            if (!taskService.getResponsibleRole(t)
                    .map(r -> r.getDescription().equals(filterSector))
                    .orElse(false)) return false;
        }

        return true;
    }

    private void addOrUpdateTaskInSideList(Task task) {
        HorizontalLayout existing = taskCardWrappersInSideList.get(task.getId());
        HorizontalLayout newCard = createTaskCardLayout(task, null);
        newCard.setWidth("auto");
        newCard.setId("side-task-" + task.getId());

        if (existing != null) {
            taskListArea.replace(existing, newCard);
        } else {
            // Tentar manter a ordem cronológica na inserção
            taskListArea.add(newCard);
        }
        taskCardWrappersInSideList.put(task.getId(), newCard);
    }

    private void updateEmptyState() {
        // Remover estado vazio se existir
        taskListArea.getChildren()
                .filter(c -> c instanceof VerticalLayout && c.getElement().getChildCount() > 0 && c.getElement().getChild(1).getText().equals("Painel Zerado!"))
                .findFirst()
                .ifPresent(taskListArea::remove);

        if (taskCardWrappersInSideList.isEmpty()) {
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
        }
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void updateFilterOptions() {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (group == null) return;

        // Responsável
        List<String> ownerOptions = new ArrayList<>();
        ownerOptions.add("Minhas");
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        // Status (Baseado nos passos possíveis dos templates do grupo para ser performático)
        List<String> stepOptions = new ArrayList<>();
        stepOptions.add("Todos");
        stepOptions.add("Pendentes");
        stepOptions.add("Finalizadas");
        
        groupTemplates.stream()
                .flatMap(template -> template.getTemplateStepRelation().stream())
                .filter(rel -> rel.getTaskStepDefinition() != null)
                .map(rel -> rel.getTaskStepDefinition().getLabel())
                .distinct()
                .sorted()
                .forEach(stepOptions::add);
        
        statusFilter.setItems(stepOptions);
        statusFilter.setValue("Todos");

        // Setor
        List<String> sectorOptions = new ArrayList<>();
        sectorOptions.add("Todos");
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (Role r : Role.values()) {
            sectorOptions.add(r.getDescription());
        }
        sectorFilter.setItems(sectorOptions);
        sectorFilter.setValue("Todos");
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void loadTemplates() {
        templateBar.removeAll();
        this.groupTemplates = taskTemplateService.getTemplatesByGroupId(group.getId());
        
        if (groupTemplates.isEmpty()) {
            templateScroller.setVisible(false);
            return;
        }

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (TaskTemplate t : groupTemplates) {
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

        // #improve_n+1 - Usar coleção já carregada no EntityGraph em vez de chamar o serviço
        List<TemplateFieldRelation> fields = template.getTemplateFieldRelation().stream()
                .sorted(Comparator.comparingInt(TemplateFieldRelation::getOrder))
                .collect(Collectors.toList());
                
        java.util.LinkedHashMap<com.xflprflx.neokioski.models.template.TaskFieldDefinition, com.vaadin.flow.component.Component> fieldComponents = new java.util.LinkedHashMap<>();

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (TemplateFieldRelation rel : fields) {
            InputType type = rel.getTaskFieldDefinition().getInputType();
            String label = rel.getTaskFieldDefinition().getLabel();
            String desc = rel.getTaskFieldDefinition().getDescription();
            List<TaskFieldOption> options = rel.getTaskFieldDefinition().getTaskFieldOptions();
            
            com.vaadin.flow.component.Component fieldComponent = createInputComponent(type, label, desc, rel.isRequired(), options, "", true);

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (fieldComponent != null) {
                fieldComponents.put(rel.getTaskFieldDefinition(), fieldComponent);
                card.add(fieldComponent);
            }
        }

        Button submitBtn = new Button("Enviar", e -> {
            boolean hasError = false;
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (TemplateFieldRelation rel : fields) {
                com.vaadin.flow.component.Component comp = fieldComponents.get(rel.getTaskFieldDefinition());
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (rel.isRequired()) {
                    String value = extractFieldValue(comp);
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (value == null || value.trim().isEmpty()) {
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
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

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (hasError) {
                Notification.show("Preencha os campos obrigatórios em " + template.getName(), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            java.util.Map<com.xflprflx.neokioski.models.template.TaskFieldDefinition, String> fieldValues = new java.util.LinkedHashMap<>();
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (java.util.Map.Entry<com.xflprflx.neokioski.models.template.TaskFieldDefinition, com.vaadin.flow.component.Component> entry : fieldComponents.entrySet()) {
                String value = extractFieldValue(entry.getValue());
                fieldValues.put(entry.getKey(), value);
                
                // Auto-copy para o clipboard (especialmente se for lista, o separador será \n)
                String valueForClipboard = extractFieldValue(entry.getValue(), "\n");
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (!valueForClipboard.isBlank()) {
                    copyToClipboard(valueForClipboard);
                }
                
                // Limpar campo após envio
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (template.isShowSendButton()) {
            card.add(submitBtn);
        }

        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return card;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(suggestionWrapper);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureSuggestionList(ListBox<User> list) {
        list.setRenderer(new com.vaadin.flow.data.renderer.ComponentRenderer<>(user -> {
            boolean online = user.getId() != null && presenceService.isOnline(user.getId());
            com.vaadin.flow.component.html.Span dot = new com.vaadin.flow.component.html.Span();
            dot.getStyle()
                .set("width", "8px")
                .set("height", "8px")
                .set("border-radius", "50%")
                .set("display", "inline-block")
                .set("background-color", online ? "var(--lumo-success-color)" : "var(--lumo-contrast-20pct)")
                .set("margin-right", "4px");

            Span name = new Span(user.getName());
            Span username = new Span("@" + user.getUsername());
            username.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            username.getStyle().set("color", "var(--lumo-secondary-text-color)");
            username.getStyle().set("margin-left", "8px");
            
            HorizontalLayout row = new HorizontalLayout(dot, name, username);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setSpacing(false);
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return row;
        }));

        list.addValueChangeListener(event -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (event.getValue() != null) {
                applyMention(event.getValue());
                list.setValue(null);
            }
        });
    }

    @com.vaadin.flow.component.ClientCallable
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void showSuggestions(String query) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (activeSuggestionWrapper == null || activeSuggestionList == null) {
            this.activeSuggestionWrapper = suggestionWrapper;
            this.activeSuggestionList = suggestionList;
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (activeSuggestionWrapper == null) return;
        }

        List<User> suggestions = userService.findAll().stream()
                .filter(u -> !u.equals(currentUser))
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
            activeSuggestionWrapper.setVisible(false);
        } else {
            activeSuggestionList.setItems(suggestions);
            activeSuggestionWrapper.setVisible(true);
            activeSuggestionWrapper.getStyle().set("z-index", "10005");
        }
    }

    @com.vaadin.flow.component.ClientCallable
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void hideSuggestions() {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (activeSuggestionWrapper != null) {
            activeSuggestionWrapper.setVisible(false);
        }
    }

    @com.vaadin.flow.component.ClientCallable
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void setActiveTargetById(String mentionId) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (mentionId == null) {
             mentionId = "main-input";
        }

        Div wrapper = suggestionWrappers.get(mentionId);
        ListBox<User> list = suggestionLists.get(mentionId);
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (wrapper != null && list != null) {
            this.activeMentionId = mentionId;
            this.activeSuggestionWrapper = wrapper;
            this.activeSuggestionList = list;
        }
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
            "target.dispatchEvent(new Event('change', { bubbles: true, composed: true }));\n" +
            "if (host && host.value !== undefined) host.value = newVal;",
            mention, activeMentionId
        );
        hideSuggestions();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void configureReplyBar() {
        replyBar.setWidthFull();
        replyBar.setPadding(true);
        replyBar.setSpacing(true);
        replyBar.setAlignItems(FlexComponent.Alignment.CENTER);
        replyBar.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        replyBar.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        replyBar.setVisible(false);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void cancelReply() {
        this.replyTo = null;
        replyBar.setVisible(false);
    }

    private Long lastLoadedGroupId;

    @Override
    public void setParameter(BeforeEvent event, Long groupId) {
        if (groupId != null && !groupId.equals(lastLoadedGroupId)) {
            this.lastLoadedGroupId = groupId;
            Optional<Group> groupOpt = groupService.findById(groupId);
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (groupOpt.isEmpty()) {
            event.forwardTo("");
            return;
        }

        this.group = groupOpt.get();
        
        Optional<User> authUser = securityService.getAuthenticatedInternalUser();
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (authUser.isPresent()) {
            this.currentUser = userService.findById(authUser.get().getId()).orElse(null);
        } else {
            this.currentUser = null;
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (currentUser != null) {
            notifyStoreOrderCheckbox.setValue(currentUser.isNotifyStoreOrder());
            notifyChatSoundsCheckbox.setValue(currentUser.isNotifyChatSounds());
        }

        // Verificação de associação ao grupo
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void loadMessages() {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (group == null || startDatePicker == null || endDatePicker == null || startDatePicker.getValue() == null || endDatePicker.getValue() == null) return;
        
        messageContainer.removeAll();
        messageWrappers.clear();

        // Converter LocalDate para Instant (Início e Fim do dia)
        java.time.LocalDate startL = startDatePicker.getValue();
        java.time.LocalDate endL = endDatePicker.getValue();
        
        java.time.Instant start = startL.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
        java.time.Instant end = endL.atTime(23, 59, 59, 999_000_000).atZone(java.time.ZoneId.systemDefault()).toInstant();

        List<ChatMessage> messages = chatService.getMessagesByGroup(group, start, end);
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (ChatMessage msg : messages) {
            addOrUpdateMessageInUI(msg);
        }
        scrollToBottom();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void addOrUpdateMessageInUI(ChatMessage msg) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (msg.getTask() != null) {
            taskIdToMessageId.put(msg.getTask().getId(), msg.getId());

            // Filtro de Autorização
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!taskService.canUserViewTask(msg.getTask(), currentUser)) {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (messageWrappers.containsKey(msg.getId())) {
                    messageContainer.remove(messageWrappers.get(msg.getId()));
                    messageWrappers.remove(msg.getId());
                }
                return;
            }

            // Se visibilidade de tarefas no chat desativada, não mostrar
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!showTasksInChat) {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (messageWrappers.containsKey(msg.getId())) {
                    messageContainer.remove(messageWrappers.get(msg.getId()));
                    messageWrappers.remove(msg.getId());
                }
                return;
            }
        }
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (messageWrappers.containsKey(msg.getId())) {
            HorizontalLayout oldWrapper = messageWrappers.get(msg.getId());
            HorizontalLayout newWrapper;
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
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
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private HorizontalLayout createMessageLayout(ChatMessage msg) {
        boolean isSystemMessage = msg.getUser() == null;
        boolean isMine = !isSystemMessage && currentUser != null && msg.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (isSystemMessage) {
            VerticalLayout sysLayout = new VerticalLayout();
            sysLayout.setPadding(false);
            sysLayout.setSpacing(false);
            sysLayout.setAlignItems(FlexComponent.Alignment.CENTER);

            // Replicamos a lógica de reply para mensagens de alerta linkarem para a tarefa
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (msg.getReplyTo() != null) {
                ChatMessage repliedMsg = msg.getReplyTo();
                Span replyAuthor = new Span(repliedMsg.getUser() != null ? repliedMsg.getUser().getName() : "Sistema");
                replyAuthor.getStyle().set("font-weight", "bold");
                replyAuthor.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
                
                String replyContent;
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
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
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return sysWrapper;
        }

        // Se a mensagem foi excluída e o usuário não é ADMIN, mostrar apenas o indicador
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return wrapper;
        }

        // Verificação de segurança extra (não deveria ocorrer após o check acima)
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (msg.getUser() == null) {
            HorizontalLayout fallback = new HorizontalLayout();
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return fallback;
        }

        // Avatar
        Avatar avatar = new Avatar(msg.getUser() != null ? msg.getUser().getName() : "?");
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (isMine) {
            avatar.getStyle().set("margin-left", "var(--lumo-space-m)");
        } else {
            avatar.getStyle().set("margin-right", "var(--lumo-space-m)");
        }

        // Nome (Cabeçalho)
        Span name = new Span(isMine ? "Você" : msg.getUser().getName());
        name.getStyle().set("font-weight", "var(--lumo-font-weight-bold)");
        name.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        boolean isOnline = presenceService.isOnline(msg.getUser().getId());
        Span statusDot = new Span();
        statusDot.getStyle()
            .set("width", "8px")
            .set("height", "8px")
            .set("border-radius", "50%")
            .set("display", "inline-block")
            .set("margin-left", "4px")
            .set("background-color", isOnline ? "var(--lumo-success-color)" : "var(--lumo-contrast-30pct)");
        
        String statusText = isOnline ? "Online" : "Offline";
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!isOnline && msg.getUser().getLastSeen() != null) {
            statusText += " (Visto em: " + dateTimeFormatter.format(msg.getUser().getLastSeen()) + ")";
        }
        statusDot.getElement().setAttribute("title", statusText);
        
        HorizontalLayout header = new HorizontalLayout(name, statusDot);
        header.setSpacing(false);
        header.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        // Citação (Reply)
        VerticalLayout quote = new VerticalLayout();
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (msg.getReplyTo() != null) {
            ChatMessage repliedMsg = msg.getReplyTo();
            Span replyAuthor = new Span(repliedMsg.getUser().getName());
            replyAuthor.getStyle().set("font-weight", "bold");
            replyAuthor.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
            
            String replyContent;
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
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
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (msg.isDeleted()) messageText.getStyle().set("opacity", "0.6");
        content.add(messageText);
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
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
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
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
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        // Badges de SLA
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (task.getSlaResults() != null) {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (TaskSlaResult result : task.getSlaResults()) {
                String slaName = result.getSlaConfig() != null ? result.getSlaConfig().getName() : "SLA";
                boolean onTime = result.getStatus() == com.xflprflx.neokioski.models.enums.SlaStatus.ON_TIME;
                
                Span slaBadge = new Span((onTime ? "✅ " : "🔴 ") + slaName);
                slaBadge.getStyle()
                        .set("padding", "2px 8px")
                        .set("border-radius", "8px")
                        .set("font-size", "10px")
                        .set("font-weight", "bold")
                        .set("background-color", onTime ? "var(--lumo-success-color-10pct)" : "var(--lumo-error-color-10pct)")
                        .set("color", onTime ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)")
                        .set("border", "1px solid " + (onTime ? "var(--lumo-success-color-50pct)" : "var(--lumo-error-color-50pct)"));
                
                slaBadge.getElement().setAttribute("title", onTime ? "Dentro do prazo" : "Atrasado");
                statusRow.add(slaBadge);
            }
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
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!task.getTaskFieldValues().isEmpty()) {
            Div fieldsDiv = new Div();
            fieldsDiv.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("border-radius", "8px")
                    .set("padding", "var(--lumo-space-s)")
                    .set("margin-bottom", "var(--lumo-space-s)")
                    .set("font-size", "var(--lumo-font-size-s)");
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
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

        java.util.Collection<TemplateStepRelation> allRelations = task.getTaskTemplate().getTemplateStepRelation();
        List<TaskStepDefinition> allowedSteps = taskService.getAllowedNextSteps(task);
        
        // Mapear os steps permitidos de volta para suas relações para ter acesso aos metadados (cores, campos de resposta, etc)
        List<TemplateStepRelation> visibleRelations = allRelations.stream()
                .filter(r -> r.getTaskStepDefinition() != null && allowedSteps.stream()
                        .anyMatch(as -> as.getId().equals(r.getTaskStepDefinition().getId())))
                .collect(Collectors.toList());

        // Mapeamento de StepId -> Campo de Resposta
        Map<Long, com.vaadin.flow.component.Component> responseFieldsByStepId = new HashMap<>();
        
        // Criar campos de resposta se a tarefa não estiver finalizada
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!taskService.isTaskFinalized(task)) {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (TemplateStepRelation sr : visibleRelations) {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
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

        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (TemplateStepRelation sr : visibleRelations) {
            TaskStepDefinition step = sr.getTaskStepDefinition();
            
            // Verificação de permissão de execução (regra administrativa/autorização)
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!taskService.canUserExecuteStep(task, currentUser)) {
                continue;
            }

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (task.getCurrentStep() != null && step.getId().equals(task.getCurrentStep().getId())) {
                continue;
            }

            Button stepBtn = new Button(step.getLabel(), ev -> {
                com.vaadin.flow.component.Component comp = responseFieldsByStepId.get(step.getId());
                String response = extractFieldValue(comp);
                
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (sr.isResponseRequired() && (response == null || response.isBlank())) {
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
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
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (!responseForClipboard.isBlank()) {
                    copyToClipboard(responseForClipboard);
                }

                Task updatedTask = taskService.executeStep(task.getId(), step, currentUser, response);

                // Verificar SLAs atrasados pendentes de justificativa
                java.util.List<com.xflprflx.neokioski.models.TaskSlaResult> pendingJustifications = 
                        taskSlaService.getPendingJustifications(updatedTask);
                if (!pendingJustifications.isEmpty()) {
                    new com.xflprflx.neokioski.ui.components.SlaJustificationDialog(
                            taskSlaService, pendingJustifications, null
                    ).open();
                }
            });
            stepBtn.setEnabled(!taskService.isTaskFinalized(task));
            stepBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (step.getColorTheme() != null && !step.getColorTheme().equalsIgnoreCase("gray")) {
                TaskUIUtils.applyThemeStyles(stepBtn, step.getColorTheme());
            } else if (sr.isFinalizer()) {
                stepBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            } else {
                stepBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            }
            actions.add(stepBtn);
        }

        // Botão Ver Detalhes
        Button detailsBtn = new Button("Ver Detalhes", ev -> new TaskDetailsDialog(task).open());
        detailsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        actions.add(detailsBtn);

        card.add(actions);

        // Wrapper alinhado à esquerda (tasks são neutras, não têm side mine/other)
        HorizontalLayout wrapper = new HorizontalLayout(card);
        wrapper.setWidthFull();
        wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        wrapper.getStyle().set("padding-top", "var(--lumo-space-xs)");
        wrapper.getStyle().set("padding-bottom", "var(--lumo-space-xs)");
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return wrapper;
    }

    private com.vaadin.flow.component.Component createInputComponent(InputType type, String label, String desc, boolean required, java.util.Collection<TaskFieldOption> optionsObj, String placeholder, boolean isCompact) {
        List<String> options = optionsObj != null ? optionsObj.stream().map(TaskFieldOption::getOptionLabel).toList() : new ArrayList<>();
        com.vaadin.flow.component.Component fieldComponent = null;

        switch (type) {
            case TEXT:
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (isCompact) {
                    com.vaadin.flow.component.textfield.TextField tf = new com.vaadin.flow.component.textfield.TextField(label);
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
                } else {
                    com.vaadin.flow.component.textfield.TextArea ta = new com.vaadin.flow.component.textfield.TextArea(label);
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (desc != null) ta.setHelperText(desc);
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (placeholder != null) ta.setPlaceholder(placeholder);
                    ta.setWidthFull();
                    ta.setRequiredIndicatorVisible(required);
                    fieldComponent = ta;
                }
                break;
            case DATE:
                com.vaadin.flow.component.datepicker.DatePicker dp = new com.vaadin.flow.component.datepicker.DatePicker(label);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (desc != null) dp.setHelperText(desc);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (placeholder != null) dp.setPlaceholder(placeholder);
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
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (placeholder != null) tp.setPlaceholder(placeholder);
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
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
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
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (desc != null) radio.setHelperText(desc);
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
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (placeholder != null) combo.setPlaceholder(placeholder);
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
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (placeholder != null) multiCombo.setPlaceholder(placeholder);
                multiCombo.setWidthFull();
                multiCombo.setRequiredIndicatorVisible(required);
                multiCombo.setItems(options);
                fieldComponent = multiCombo;
                break;
            case TEXT_LIST:
                com.vaadin.flow.component.combobox.MultiSelectComboBox<String> textList = new com.vaadin.flow.component.combobox.MultiSelectComboBox<>(label);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (desc != null) textList.setHelperText(desc);
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (placeholder != null) textList.setPlaceholder(placeholder);
                textList.setWidthFull();
                textList.setRequiredIndicatorVisible(required);
                textList.setAllowCustomValue(true);
                
                // Inicializar com opções se existirem
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (options != null && !options.isEmpty()) {
                    textList.setItems(options);
                }

                textList.addCustomValueSetListener(e -> {
                    String customValue = e.getDetail();
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
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
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return fieldComponent;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private String extractFieldValue(com.vaadin.flow.component.Component comp) {
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return extractFieldValue(comp, ", ");
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private String extractFieldValue(com.vaadin.flow.component.Component comp, String separator) {
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
                return col.stream().map(Object::toString).collect(Collectors.joining(separator));
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
            return val.toString();
        }
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return "";
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void copyToClipboard(String text) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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


    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private Div renderMessageContent(String content) {
        Div container = new Div();
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (content == null || content.isEmpty()) return container;

        // Regex to find mentions like @username
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@(\\w+)");
        java.util.regex.Matcher matcher = pattern.matcher(content);

        int lastEnd = 0;
        while (matcher.find()) {
            // Append text before the mention
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
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
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (lastEnd < content.length()) {
            container.add(new Span(content.substring(lastEnd)));
        }

        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return container;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private boolean isUserMentioned(String content, String username) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (content == null || username == null) return false;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@" + java.util.regex.Pattern.quote(username) + "(\\b|$)");
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return pattern.matcher(content).find();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void setupRealTimeUpdate() {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (listener != null && group != null) {
            chatService.unregister(group.getId(), listener);
        }

        listener = msg -> {
            // Ignorar mensagens sem ID (nulo) - as voláteis agora usam ID negativo
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (msg.getId() == null) return;

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (getUI().isPresent()) {
                getUI().get().access(() -> {
                    boolean isNew = !messageWrappers.containsKey(msg.getId());
                    addOrUpdateMessageInUI(msg);
                    scrollToBottom();
                    
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (msg.getUser() == null || !msg.getUser().getId().equals(currentUser.getId())) {
                        boolean isReplyToMe = msg.getReplyTo() != null && 
                                             msg.getReplyTo().getUser() != null &&
                                             msg.getReplyTo().getUser().getId().equals(currentUser.getId());
                        boolean mentioned = currentUser != null && isUserMentioned(msg.getContent(), currentUser.getUsername());
                        
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (isNew) {
                            /*
                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                             */
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
                            /*
                             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                             */
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
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (getUI().isPresent() && group != null && task != null) {
                getUI().get().access(() -> {
                    /*
                     * Inicia um bloco try-catch para evitar que erros internos quebrem a renderização da interface.
                     */
                    try {
                        // Atualizar o card na linha do chat se existir
                        Long messageId = taskIdToMessageId != null ? taskIdToMessageId.get(task.getId()) : null;
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (messageId != null && messageWrappers.containsKey(messageId)) {
                            // Atualizar o card no chat usando a task já inicializada (sem re-fetch)
                            HorizontalLayout oldWrapper = messageWrappers.get(messageId);
                            HorizontalLayout newWrapper = createTaskCardLayout(task, null);
                            newWrapper.setId("msg-" + messageId);
                            messageContainer.replace(oldWrapper, newWrapper);
                            messageWrappers.put(messageId, newWrapper);
                        }
                        // Atualizar a lista de tarefas lateral de forma granular
                        if (taskService.canUserViewTask(task, currentUser) && matchesFilters(task)) {
                            addOrUpdateTaskInSideList(task);
                        } else {
                            HorizontalLayout existing = taskCardWrappersInSideList.remove(task.getId());
                            if (existing != null) taskListArea.remove(existing);
                        }
                        updateEmptyState();
                    } catch (Exception e) {
                        System.err.println("Erro no taskListener do ChatView: " + e.getMessage());
                    }
                });
            }
        };

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (group != null) {
            chatService.register(group.getId(), listener);
            taskService.register(group.getId(), taskListener);
        }
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void scrollToBottom() {
        scroller.getElement().executeJs("this.scrollTo(0, this.scrollHeight)");
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        presenceListener = event -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                // Apenas atualizar avatares visuais, sem recarregar mensagens do banco
                updateOnlineUserAvatars();
            }));
        };
        presenceService.addListener(presenceListener);
        updateOnlineUserAvatars();
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (group != null && listener != null) {
            chatService.unregister(group.getId(), listener);
        }
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (taskListener != null && group != null) {
            taskService.unregister(group.getId(), taskListener);
        }
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (presenceListener != null) {
            presenceService.removeListener(presenceListener);
        }
        
        super.onDetach(detachEvent);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void openSearchDialog(String query) {
        this.lastSearchQuery = query;
        this.searchPage = 0;
        
        searchDialog.setHeaderTitle("Resultados da Busca: " + query);
        searchDialog.setWidth("500px");
        searchDialog.setHeight("600px");
        
        searchResultsContainer.removeAll();
        searchResultsContainer.setPadding(false);
        searchResultsContainer.setSpacing(true);
        
        Scroller searchScroller = new Scroller(searchResultsContainer);
        searchScroller.setSizeFull();
        
        showMoreSearchBtn.setWidthFull();
        showMoreSearchBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        showMoreSearchBtn.addClickListener(e -> {
            searchPage++;
            performSearch(lastSearchQuery, true);
        });
        
        VerticalLayout content = new VerticalLayout(searchScroller, showMoreSearchBtn);
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);
        
        searchDialog.removeAll();
        searchDialog.add(content);
        
        performSearch(query, false);
        searchDialog.open();
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void performSearch(String query, boolean append) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (group == null) return;
        
        List<ChatMessage> results = chatService.searchMessages(group, query, searchPage, 20);
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!append) {
            searchResultsContainer.removeAll();
        }
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (results.isEmpty()) {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!append) {
                Span noResults = new Span("Nenhum resultado encontrado.");
                noResults.getStyle().set("color", "var(--lumo-secondary-text-color)");
                noResults.getStyle().set("padding", "var(--lumo-space-m)");
                searchResultsContainer.add(noResults);
            }
            showMoreSearchBtn.setVisible(false);
        } else {
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (ChatMessage msg : results) {
                searchResultsContainer.add(createSearchResultItem(msg));
            }
            showMoreSearchBtn.setVisible(results.size() >= 20);
        }
    }

    private com.vaadin.flow.component.Component createSearchResultItem(ChatMessage msg) {
        HorizontalLayout item = new HorizontalLayout();
        item.setWidthFull();
        item.setPadding(true);
        item.setSpacing(true);
        item.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        item.getStyle().set("cursor", "pointer");
        item.getStyle().set("transition", "background 0.2s");
        item.getElement().addEventListener("mouseenter", e -> item.getStyle().set("background", "var(--lumo-contrast-5pct)"));
        item.getElement().addEventListener("mouseleave", e -> item.getStyle().set("background", "transparent"));
        
        Avatar avatar = new Avatar(msg.getUser() != null ? msg.getUser().getName() : "S");
        avatar.addThemeVariants(com.vaadin.flow.component.avatar.AvatarVariant.LUMO_XSMALL);
        
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        
        HorizontalLayout topRow = new HorizontalLayout();
        topRow.setSpacing(true);
        
        Span name = new Span(msg.getUser() != null ? msg.getUser().getName() : "Sistema");
        name.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-xs)");
        
        Span date = new Span(dateTimeFormatter.format(msg.getTimestamp()));
        date.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "var(--lumo-font-size-xxs)");
        
        topRow.add(name, date);
        
        String contentText;
        String icon = "";
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (msg.getTask() != null) {
            icon = "📋 ";
            // Concatenar valores de campos e respostas para o preview se a query bateu neles
            StringBuilder sb = new StringBuilder();
            sb.append(msg.getTask().getTaskTemplate().getName());
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (TaskFieldValue fv : msg.getTask().getTaskFieldValues()) {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (fv.getValue().toLowerCase().contains(lastSearchQuery.toLowerCase())) {
                    sb.append(" | ").append(fv.getValue());
                }
            }
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (TaskEvent ev : msg.getTask().getTaskEvents()) {
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (ev.getResponse() != null && ev.getResponse().toLowerCase().contains(lastSearchQuery.toLowerCase())) {
                    sb.append(" | ").append(ev.getResponse());
                }
            }
            contentText = sb.toString();
        } else {
            contentText = msg.getContent();
        }
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (contentText.length() > 100) {
            contentText = contentText.substring(0, 97) + "...";
        }
        
        Span text = new Span(icon + contentText);
        text.getStyle().set("font-size", "var(--lumo-font-size-s)");
        
        info.add(topRow, text);
        item.add(avatar, info);
        item.setFlexGrow(1, info);
        
        item.addClickListener(e -> {
            searchDialog.close();
            navigateToMessage(msg);
        });
        
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return item;
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void navigateToMessage(ChatMessage msg) {
        java.time.LocalDate msgDate = msg.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        
        // Se a data estiver fora do range, ajustar
        boolean rangeChanged = false;
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (msgDate.isBefore(startDatePicker.getValue())) {
            startDatePicker.setValue(msgDate);
            rangeChanged = true;
        }
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (msgDate.isAfter(endDatePicker.getValue())) {
            endDatePicker.setValue(msgDate);
            rangeChanged = true;
        }
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (rangeChanged) {
            // loadMessages será chamado pelos listeners de data, então aguardamos um pouco
            // Ou chamamos manualmente e depois scrollamos
            loadMessages();
        }

        // Scroll e Highlight
        getElement().executeJs(
            "setTimeout(() => {" +
            "  const el = document.getElementById('msg-" + msg.getId() + "');" +
            "  if(el) {" +
            "    el.scrollIntoView({behavior: 'smooth', block: 'center'});" +
            "    el.style.transition = 'box-shadow 0.5s, background 0.5s';" +
            "    const oldBg = el.style.background;" +
            "    el.style.boxShadow = '0 0 0 4px var(--lumo-primary-color-50pct)';" +
            "    el.style.background = 'var(--lumo-primary-color-10pct)';" +
            "    setTimeout(() => { " +
            "      el.style.boxShadow = 'none'; " +
            "      el.style.background = oldBg;" +
            "    }, 2000);" +
            "  }" +
            "}, 300);"
        );
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void updateOnlineUserAvatars() {
        getUI().ifPresent(ui -> ui.access(() -> {
            onlineUsersAvatars.removeAll();
            if (group == null) return;
            Set<Long> onlineIds = presenceService.getOnlineUserIds();
            
            // Usar os usuários do grupo já em memória, sem ir ao banco
            for (User user : group.getUsers()) {
                if (!onlineIds.contains(user.getId())) continue;
                com.vaadin.flow.component.avatar.Avatar avatar = new com.vaadin.flow.component.avatar.Avatar(user.getName());
                avatar.getStyle()
                    .set("width", "24px")
                    .set("height", "24px")
                    .set("border", "2px solid var(--lumo-success-color)")
                    .set("border-radius", "50%")
                    .set("margin-left", "-8px"); // Sobreposição para visual Google Docs

                if (onlineUsersAvatars.getComponentCount() == 0) {
                    avatar.getStyle().set("margin-left", "0");
                }

                avatar.getElement().setAttribute("title", user.getName() + " (Online)");
                onlineUsersAvatars.add(avatar);
            }
        }));
    }
}
