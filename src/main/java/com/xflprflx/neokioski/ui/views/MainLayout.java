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
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
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
        }
    }



    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
    }

}
