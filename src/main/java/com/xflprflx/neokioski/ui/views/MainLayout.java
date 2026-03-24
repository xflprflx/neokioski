package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.security.SecurityService;
import com.xflprflx.neokioski.services.GroupService;
import com.xflprflx.neokioski.services.TaskService;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.function.Consumer;

/**
 * Layout principal da aplicação que define o cabeçalho (Header) e o menu lateral (Drawer).
 * Todas as outras vistas (@Route) são renderizadas dentro deste layout.
 */
@PermitAll
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final transient AuthenticationContext authContext;
    private final GroupService groupService;
    private final SecurityService securityService;
    private final TaskService taskService;
    private final VerticalLayout drawerContainer = new VerticalLayout();
    private final VerticalLayout taskList = new VerticalLayout();
    private Consumer<Task> taskListener;
    private Long registeredGroupId;

    // Filtros ativos
    private String filterOwner = "todas"; // "minhas", "todas", ou username de um membro
    private String filterStatus = "todos"; // "todos" ou label de um step
    private Group currentGroup;
    private User currentUser;

    public MainLayout(AuthenticationContext authContext, GroupService groupService,
                      SecurityService securityService, TaskService taskService) {
        this.authContext = authContext;
        this.groupService = groupService;
        this.securityService = securityService;
        this.taskService = taskService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("NeoKioski");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        Button logout = new Button("Sair");
        logout.addClickListener(event -> authContext.logout());
        logout.getStyle().set("cursor", "pointer");
        logout.getStyle().set("margin-right", "20px");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void createDrawer() {
        drawerContainer.setSizeFull();
        drawerContainer.setPadding(false);
        drawerContainer.setSpacing(false);

        showPrimaryNav();

        Scroller scroller = new Scroller(drawerContainer);
        scroller.setClassName(LumoUtility.Padding.SMALL);
        addToDrawer(scroller);
    }

    private void showPrimaryNav() {
        drawerContainer.removeAll();
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Início", HomeView.class, VaadinIcon.DASHBOARD.create()));

        SideNavItem chatsItem = new SideNavItem("Chats", (String) null, VaadinIcon.CHAT.create());
        chatsItem.getElement().addEventListener("click", e -> showChatNav(null));
        nav.addItem(chatsItem);

        if (authContext.hasAnyRole("ADMIN")) {
            nav.addItem(new SideNavItem("Grupos", GroupListView.class, VaadinIcon.USERS.create()));
            nav.addItem(new SideNavItem("Templates", TaskTemplateListView.class, VaadinIcon.FILE_TEXT.create()));
        }

        drawerContainer.add(nav);
    }

    private void showChatNav(Group selectedGroup) {
        drawerContainer.removeAll();

        Button backButton = new Button("Voltar", VaadinIcon.ARROW_LEFT.create(), e -> showPrimaryNav());
        backButton.setWidthFull();
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.getStyle().set("margin-bottom", "0.5em");

        SideNav chatNav = new SideNav();
        chatNav.setLabel("Meus Grupos");

        securityService.getAuthenticatedInternalUser().ifPresent(user -> {
            this.currentUser = user;
            groupService.findActiveGroupsByUser(user)
                    .forEach(group -> {
                        String url = RouteConfiguration.forSessionScope().getUrl(ChatView.class, group.getId());
                        SideNavItem item = new SideNavItem(group.getName(), url, VaadinIcon.GROUP.create());
                        chatNav.addItem(item);
                    });
        });

        drawerContainer.add(backButton, chatNav);

        // Painel de tarefas apenas se há grupo selecionado
        if (selectedGroup != null && currentUser != null) {
            drawerContainer.add(buildTaskPanel(selectedGroup));
        }
    }

    private VerticalLayout buildTaskPanel(Group group) {
        VerticalLayout panel = new VerticalLayout();
        panel.setPadding(false);
        panel.setSpacing(false);
        panel.setWidthFull();

        // Divisor e título
        Div divider = new Div();
        divider.getStyle()
                .set("border-top", "1px solid var(--lumo-contrast-10pct)")
                .set("margin", "var(--lumo-space-s) 0");

        Span sectionTitle = new Span("\uD83D\uDCCB Tarefas em Andamento");
        sectionTitle.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("display", "block");

        // Container da lista de tasks (usando o campo da classe agora)
        taskList.setPadding(false);
        taskList.setSpacing(false);
        taskList.setWidthFull();

        // ── Dropdown 1: Responsável ──────────────────────────────────────
        com.vaadin.flow.component.combobox.ComboBox<String> ownerFilter =
                new com.vaadin.flow.component.combobox.ComboBox<>("Responsável");
        ownerFilter.setWidthFull();
        ownerFilter.setClearButtonVisible(true);
        ownerFilter.setPlaceholder("Todas");

        java.util.List<String> ownerOptions = new java.util.ArrayList<>();
        ownerOptions.add("Minhas");
        // Um item por membro do grupo
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
        filterOwner = "todas";

        ownerFilter.addValueChangeListener(e -> {
            String val = e.getValue();
            if (val == null || val.equals("Todas")) filterOwner = "todas";
            else if (val.equals("Minhas")) filterOwner = "minhas";
            else filterOwner = val; // username
            refreshTaskList(this.taskList, group);
        });

        // ── Dropdown 2: Status ──────────────────────────────────────────
        com.vaadin.flow.component.combobox.ComboBox<String> statusFilter =
                new com.vaadin.flow.component.combobox.ComboBox<>("Status");
        statusFilter.setWidthFull();
        statusFilter.setClearButtonVisible(true);
        statusFilter.setPlaceholder("Todos");

        // Popular com steps distintos das tasks do grupo
        java.util.List<String> stepOptions = new java.util.ArrayList<>();
        stepOptions.add("Todos");
        stepOptions.add("Pendentes");
        stepOptions.add("Finalizadas");
        
        // Adicionar "Pendente" explicitamente se houver tasks sem step
        boolean hasNoStepTasks = taskService.getTasksByGroup(group).stream()
                .anyMatch(t -> t.getCurrentStep() == null);
        if (hasNoStepTasks) {
            stepOptions.add("Pendente");
        }

        taskService.getTasksByGroup(group).stream()
                .filter(t -> t.getCurrentStep() != null)
                .map(t -> t.getCurrentStep().getLabel())
                .distinct()
                .sorted()
                .forEach(stepOptions::add);
        statusFilter.setItems(stepOptions);
        statusFilter.setValue("Todos");
        filterStatus = "todos";

        statusFilter.addValueChangeListener(e -> {
            String val = e.getValue();
            filterStatus = (val == null || val.equals("Todos")) ? "todos" : val;
            refreshTaskList(this.taskList, group);
        });

        VerticalLayout filters = new VerticalLayout(ownerFilter, statusFilter);
        filters.setPadding(false);
        filters.setSpacing(true);
        filters.getStyle().set("padding", "0 var(--lumo-space-xs)");

        // Carregar tasks iniciais
        refreshTaskList(this.taskList, group);

        panel.add(divider, sectionTitle, filters, taskList);
        return panel;
    }

    private void refreshTaskList(VerticalLayout taskList, Group group) {
        taskList.removeAll();

        // Buscar todas as tasks do grupo
        java.util.List<Task> tasks = taskService.getTasksByGroup(group);

        // Filtrar por responsável
        tasks = tasks.stream().filter(t -> {
            if (filterOwner.equals("todas")) return true;
            if (filterOwner.equals("minhas")) {
                if (currentUser == null) return true;
                // criador OU participante num evento
                boolean isCreator = t.getCreator() != null && t.getCreator().getId().equals(currentUser.getId());
                boolean participated = t.getTaskEvents().stream()
                        .anyMatch(ev -> ev.getUser() != null && ev.getUser().getId().equals(currentUser.getId()));
                return isCreator || participated;
            }
            // username de membro específico
            return t.getCreator() != null && t.getCreator().getUsername().equals(filterOwner);
        }).collect(java.util.stream.Collectors.toList());

        // Filtrar por status
        if (!filterStatus.equals("todos")) {
            tasks = tasks.stream()
                    .filter(t -> {
                        if (filterStatus.equals("Pendentes")) return taskService.isTaskPending(t);
                        if (filterStatus.equals("Finalizadas")) return taskService.isTaskFinalized(t);
                        if (filterStatus.equals("Pendente")) return t.getCurrentStep() == null;
                        return t.getCurrentStep() != null && t.getCurrentStep().getLabel().equals(filterStatus);
                    })
                    .collect(java.util.stream.Collectors.toList());
        }

        if (tasks.isEmpty()) {
            Span empty = new Span("Nenhuma tarefa encontrada.");
            empty.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("padding", "var(--lumo-space-s)");
            taskList.add(empty);
            return;
        }

        for (Task task : tasks) {
            taskList.add(buildTaskCompactCard(task, taskList, group));
        }
    }

    private Div buildTaskCompactCard(Task task, VerticalLayout taskList, Group group) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-left", "3px solid var(--lumo-primary-color)")
                .set("border-radius", "8px")
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("margin-bottom", "var(--lumo-space-xs)")
                .set("background", "var(--lumo-base-color)")
                .set("cursor", "pointer");

        // Título
        Span titleSpan = new Span(task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "Sem título");
        titleSpan.getStyle()
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("display", "block");

        // Status + criador
        String stepLabel = task.getCurrentStep() != null ? task.getCurrentStep().getLabel() : "Pendente";
        String stepColor = task.getCurrentStep() != null ? task.getCurrentStep().getColorTheme() : "var(--lumo-contrast-20pct)";
        String creatorUsername = task.getCreator() != null ? "@" + task.getCreator().getUsername() : "";

        Span statusSpan = new Span("Status: ");
        statusSpan.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("color", "var(--lumo-secondary-text-color)");

        Span badge = new Span(stepLabel);
        badge.getStyle()
                .set("background-color", stepColor != null ? stepColor : "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("padding", "1px 6px")
                .set("border-radius", "8px")
                .set("font-size", "var(--lumo-font-size-xxs)")
                .set("font-weight", "bold");

        Span creatorSpan = new Span(" | " + creatorUsername);
        creatorSpan.getStyle().set("font-size", "var(--lumo-font-size-xxs)").set("color", "var(--lumo-secondary-text-color)");

        HorizontalLayout infoRow = new HorizontalLayout(statusSpan, badge, creatorSpan);
        infoRow.setSpacing(false);
        infoRow.setAlignItems(FlexComponent.Alignment.CENTER);
        infoRow.getStyle().set("gap", "4px");

        // Botões de ação
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.getStyle().set("margin-top", "var(--lumo-space-xs)");

        // Botão "Concluir" — executa o primeiro step finalizador disponível
        if (task.getTaskTemplate() != null) {
            com.xflprflx.neokioski.services.TaskTemplateService taskTemplateService = null;
            // Não temos acesso ao TaskTemplateService aqui diretamente no MainLayout,
            // então o botão "Concluir" via painel usa um step convenção (o step is finalizer do template)
            // Simplificado: apenas exibir "Ver Detalhes"
        }

        Button detailsBtn = new Button("Ver Detalhes", e -> openTaskDetailsDialogInMainLayout(task));
        detailsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        actions.add(detailsBtn);

        card.add(titleSpan, infoRow, actions);
        return card;
    }

    private void openTaskDetailsDialogInMainLayout(Task task) {
        Dialog dialog = new Dialog();
        dialog.setWidth("480px");
        dialog.setHeaderTitle("🗂 " + (task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "Tarefa"));

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        if (task.getCurrentStep() != null) {
            String stepLabel = task.getCurrentStep().getLabel();
            String colorTheme = task.getCurrentStep().getColorTheme();
            Span stepBadge = new Span(stepLabel);
            stepBadge.getStyle()
                    .set("background-color", colorTheme != null ? colorTheme : "var(--lumo-primary-color-10pct)")
                    .set("color", "var(--lumo-primary-text-color)")
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "bold");
            
            HorizontalLayout statusRow = new HorizontalLayout(new Span("Status: "), stepBadge);
            statusRow.setAlignItems(FlexComponent.Alignment.CENTER);
            
            if (taskService.isTaskFinalized(task)) {
                Span finalizedBadge = new Span("Finalizada");
                finalizedBadge.getStyle()
                    .set("background-color", "var(--lumo-success-color)")
                    .set("color", "white")
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "bold")
                    .set("margin-left", "var(--lumo-space-s)");
                statusRow.add(finalizedBadge);
            }
            content.add(statusRow);
        } else {
            content.add(new Span("Status: Pendente"));
        }
        if (task.getCreator() != null) {
            content.add(new Span("Criado por: @" + task.getCreator().getUsername()));
        }
        if (!task.getTaskFieldValues().isEmpty()) {
            Span lbl = new Span("Campos:");
            lbl.getStyle().set("font-weight", "bold");
            content.add(lbl);
            for (var fv : task.getTaskFieldValues()) {
                String fieldLabel = fv.getTaskFieldDefinition() != null ? fv.getTaskFieldDefinition().getLabel() : "Campo";
                content.add(new Span("• " + fieldLabel + ": " + fv.getValue()));
            }
        }
        if (!task.getTaskEvents().isEmpty()) {
            Span histLbl = new Span("Histórico:");
            histLbl.getStyle().set("font-weight", "bold");
            content.add(histLbl);
            for (var ev : task.getTaskEvents()) {
                String who = ev.getUser() != null ? "@" + ev.getUser().getUsername() : "sistema";
                String step = ev.getTaskStepDefinition() != null ? ev.getTaskStepDefinition().getLabel() : "";
                content.add(new Span("• " + who + " → " + step));
            }
        }

        Scroller scroller = new Scroller(content);
        scroller.setMaxHeight("360px");
        dialog.add(scroller);
        dialog.getFooter().add(new Button("Fechar", e -> dialog.close()));
        dialog.open();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (!event.getActiveChain().isEmpty() && event.getActiveChain().get(0) instanceof ChatView) {
            // Extrair groupId da URL para carregar tasks do grupo
            String location = event.getLocation().getPath();
            // URL padrão: /chat/123
            String[] parts = location.split("/");
            Group group = null;
            if (parts.length >= 2) {
                try {
                    long groupId = Long.parseLong(parts[parts.length - 1]);
                    group = groupService.findById(groupId).orElse(null);
                } catch (NumberFormatException ignored) {}
            }
            currentGroup = group;
            showChatNav(group);
            setupTaskListener();
        }
    }

    private void setupTaskListener() {
        if (taskListener != null && registeredGroupId != null) {
            taskService.unregister(registeredGroupId, taskListener);
        }

        if (currentGroup != null) {
            taskListener = task -> {
                getUI().ifPresent(ui -> ui.access(() -> {
                    refreshTaskList(this.taskList, currentGroup);
                }));
            };
            registeredGroupId = currentGroup.getId();
            taskService.register(registeredGroupId, taskListener);
        }
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        if (taskListener != null && registeredGroupId != null) {
            taskService.unregister(registeredGroupId, taskListener);
        }
        super.onDetach(detachEvent);
    }
}
