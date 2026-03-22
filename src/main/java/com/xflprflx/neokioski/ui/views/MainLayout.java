package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
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
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.ChatMessage;
import com.xflprflx.neokioski.repositories.ChatMessageRepository;
import com.xflprflx.neokioski.security.SecurityService;
import com.xflprflx.neokioski.services.ChatService;
import com.xflprflx.neokioski.services.GroupService;
import com.xflprflx.neokioski.services.UserGroupStateService;
import com.xflprflx.neokioski.services.MessageNotificationService;
import jakarta.annotation.security.PermitAll;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * Layout principal da aplicação que define o cabeçalho (Header) e o menu lateral (Drawer).
 * Todas as outras vistas (@Route) são renderizadas dentro deste layout.
 */
@PermitAll // Garante que apenas usuários autenticados vejam o layout base
public class MainLayout extends AppLayout implements AfterNavigationObserver {

    private final transient AuthenticationContext authContext;
    private final GroupService groupService;
    private final SecurityService securityService;
    private final ChatService chatService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserGroupStateService userGroupStateService;
    private final MessageNotificationService notificationService;
    private final VerticalLayout drawerContainer = new VerticalLayout();

    private Consumer<ChatMessage> globalChatListener;

    /**
     * Construtor que monta a estrutura básica da página.
     */
    public MainLayout(AuthenticationContext authContext, GroupService groupService,
                      SecurityService securityService, ChatService chatService,
                      ChatMessageRepository chatMessageRepository, UserGroupStateService userGroupStateService,
                      MessageNotificationService notificationService) {
        this.authContext = authContext;
        this.groupService = groupService;
        this.securityService = securityService;
        this.chatService = chatService;
        this.chatMessageRepository = chatMessageRepository;
        this.userGroupStateService = userGroupStateService;
        this.notificationService = notificationService;
        createHeader(); // Cria o topo da página
        createDrawer(); // Cria a barra de navegação lateral
    }

    /**
     * Configura o cabeçalho com o logo, o botão de menu e o botão de sair.
     */
    private void createHeader() {
        H1 logo = new H1("NeoKioski"); // Título/Logo da aplicação
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
        chatsItem.getElement().addEventListener("click", e -> showChatNav());
        nav.addItem(chatsItem);

        if (authContext.hasAnyRole("ADMIN")) {
            nav.addItem(new SideNavItem("Grupos", GroupListView.class, VaadinIcon.USERS.create()));
        }

        drawerContainer.add(nav);
    }

    private void showChatNav() {
        drawerContainer.removeAll();

        Button backButton = new Button("Voltar", VaadinIcon.ARROW_LEFT.create(), e -> showPrimaryNav());
        backButton.setWidthFull();
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.getStyle().set("margin-bottom", "1em");

        SideNav chatNav = new SideNav();
        chatNav.setLabel("Meus Grupos");

        User currentUser = securityService.getAuthenticatedInternalUser().orElse(null);

        groupService.findAll().stream()
                .filter(Group::isActive)
                .forEach(group -> {
                    String url = RouteConfiguration.forSessionScope().getUrl(ChatView.class, group.getId());
                    SideNavItem item = new SideNavItem(group.getName(), url, VaadinIcon.GROUP.create());
                    
                    if (currentUser != null) {
                        Instant lastRead = userGroupStateService.getLastReadTimestamp(currentUser, group);
                        long unreadCount = chatMessageRepository.countUnreadMessages(group, lastRead, currentUser);
                        if (unreadCount > 0) {
                            boolean hasMentions = notificationService.getUnreadNotificationsForUser(currentUser).stream()
                                    .anyMatch(n -> n.getMessage().getGroup().getId().equals(group.getId()));
                            
                            Span badge = new Span(hasMentions ? "@ " + unreadCount : String.valueOf(unreadCount));
                            badge.getElement().getThemeList().add("badge error primary small pill");
                            badge.getStyle().set("margin-inline-start", "auto");
                            
                            if (hasMentions) {
                                badge.addClassName("unread-mention-badge");
                            }
                            
                            item.setSuffixComponent(badge);
                        }
                    }

                    chatNav.addItem(item);
                });

        drawerContainer.add(backButton, chatNav);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (!event.getActiveChain().isEmpty() && event.getActiveChain().get(0) instanceof ChatView) {
            showChatNav();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI ui = attachEvent.getUI();
        
        ui.getPage().executeJs(
            "if (!document.getElementById('neokioski-mentions-style')) {" +
            "  const style = document.createElement('style');" +
            "  style.id = 'neokioski-mentions-style';" +
            "  style.innerHTML = '@keyframes pulse-mention { 0% { background-color: rgba(255, 0, 0, 0.1); box-shadow: 0 0 0 rgba(255,0,0,0.4); } 50% { background-color: rgba(255, 0, 0, 0.6); box-shadow: 0 0 10px rgba(255,0,0,0.6); transform: scale(1.05); } 100% { background-color: rgba(255, 0, 0, 0.1); box-shadow: 0 0 0 rgba(255,0,0,0.4); } } .unread-mention-badge { animation: pulse-mention 1.5s infinite; transition: all 0.3s; }';" +
            "  document.head.appendChild(style);" +
            "}"
        );
        
        globalChatListener = message -> {
            ui.access(() -> {
                // Atualiza a navegação para refletir novos badges
                if (drawerContainer.getChildren().anyMatch(c -> c instanceof SideNav && "Meus Grupos".equals(((SideNav)c).getLabel()))) {
                    showChatNav();
                }
            });
        };
        chatService.registerGlobal(globalChatListener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (globalChatListener != null) {
            chatService.unregisterGlobal(globalChatListener);
        }
    }
}
