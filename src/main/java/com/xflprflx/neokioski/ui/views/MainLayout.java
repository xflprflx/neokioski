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
import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.security.SecurityService;
import com.xflprflx.neokioski.services.GroupService;
import com.xflprflx.neokioski.services.TaskService;
import com.xflprflx.neokioski.services.UserService;
import com.xflprflx.neokioski.services.SoundService;
import com.xflprflx.neokioski.services.ChatService;
import com.xflprflx.neokioski.models.ChatMessage;
import jakarta.annotation.security.PermitAll;

import java.util.ArrayList;
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
    private final UserService userService;
    private final ChatService chatService;
    private final VerticalLayout drawerContainer = new VerticalLayout();
    private User currentUser;
    private List<String> availableSounds = new ArrayList<>();
    private Consumer<Task> globalTaskListener;
    private Consumer<ChatMessage> chatMessageListener;

    public MainLayout(AuthenticationContext authContext, GroupService groupService,
                      SecurityService securityService, TaskService taskService, UserService userService, 
                      SoundService soundService, ChatService chatService) {
        this.authContext = authContext;
        this.groupService = groupService;
        this.securityService = securityService;
        this.taskService = taskService;
        this.userService = userService;
        this.chatService = chatService;
        
        // Obter nomes de todos os sons disponíveis
        this.availableSounds = soundService.getAvailableAlertSounds();
        
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
            nav.addItem(new SideNavItem("Usuários", UserListView.class, VaadinIcon.USER_CARD.create()));
            nav.addItem(new SideNavItem("Templates", TaskTemplateListView.class, VaadinIcon.FILE_TEXT.create()));
            nav.addItem(new SideNavItem("Relatório Loja", StoreReportView.class, VaadinIcon.CHART.create()));
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
            showChatNav(group);
        }
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        
        // Injetar dados de áudio no cliente via URL
        StringBuilder jsBuilder = new StringBuilder();
        jsBuilder.append("window.audioData = window.audioData || {};");
        
        // Mapear cada som disponível para sua URL estática
        for (String sound : availableSounds) {
            String url = "/sounds/" + sound.replace("#", "%23");
            System.out.println("SOUND: Mapping " + sound + " to URL: " + url);
            jsBuilder.append(String.format("window.audioData['%s'] = '%s';", sound, url));
        }
        
        // Fallback para o som antigo se não estiver na lista nova
        if (!availableSounds.contains("pedido-loja")) {
             jsBuilder.append("window.audioData['pedido-loja'] = '/sounds/pedido-loja';");
        }

        jsBuilder.append(
            "window.playAlertSound = function(soundName) {" +
            "  console.log('SOUND: Play Alert Sound triggered:', soundName);" +
            "  const url = window.audioData[soundName];" +
            "  if(url) {" +
            "    const audio = new Audio(url);" +
            "    audio.play().catch(e => console.error('Audio play failed:', e));" +
            "  } else { console.warn('SOUND: Audio URL missing for ', soundName); }" +
            "};" +
            "window.playStoreOrderSound = function() { window.playAlertSound('pedido-loja'); };"
        );
        attachEvent.getUI().getPage().executeJs(jsBuilder.toString());

        // Capturar usuário atual e ID para a closure
        securityService.getAuthenticatedInternalUser().ifPresent(u -> {
            this.currentUser = u;
            final Long currentUserId = u.getId();

            // Registrar listener global para som de pedido
            this.globalTaskListener = task -> {
                if (task.getTaskTemplate() != null && task.getTaskTemplate().isPlayAlertSound()) {
                    boolean isNewTask = task.getCurrentStep() == null;
                    
                    if (isNewTask && currentUserId != null) {
                        userService.findById(currentUserId).ifPresent(user -> {
                            // Verificar Role se configurado
                            boolean hasRoleAccess = true;
                            if (task.getTaskTemplate().getAlertRoles() != null && !task.getTaskTemplate().getAlertRoles().isEmpty()) {
                                hasRoleAccess = task.getTaskTemplate().getAlertRoles().contains(user.getRole());
                            }

                            if (user.isNotifyStoreOrder() && hasRoleAccess) {
                                String sound = task.getTaskTemplate().getAlertSound();
                                if (sound == null || sound.isEmpty()) sound = "pedido-loja";
                                
                                final String soundName = sound;
                                getUI().ifPresent(ui -> ui.access(() -> {
                                    System.out.println("DEBUG SOUND: Disparando JS playAlertSound para " + soundName);
                                    ui.getPage().executeJs("if(window.playAlertSound) { window.playAlertSound($0); }", soundName);
                                }));
                            }
                        });
                    }
                }
            };
            taskService.registerGlobal(globalTaskListener);

            // Registrar listener de mensagens para alertas temporais e steps
            this.chatMessageListener = message -> {
                System.out.println("DEBUG SOUND: Recebido no MainLayout para grupo. User=" + u.getUsername() + ", alertSound=" + message.getAlertSound() + ", roles=" + message.getAlertRoles());
                if (message.getAlertSound() != null && !message.getAlertSound().isEmpty()) {
                    // Verificar Role se configurado
                    boolean hasRoleAccess = true;
                    if (message.getAlertRoles() != null && !message.getAlertRoles().isEmpty()) {
                        hasRoleAccess = false;
                        String[] roles = message.getAlertRoles().split(",");
                        for (String rStr : roles) {
                            if (currentUser.getRole() != null && currentUser.getRole().name().equals(rStr.trim())) {
                                hasRoleAccess = true;
                                break;
                            }
                        }
                    }
                    boolean notifyOrder = (currentUser != null && currentUser.isNotifyStoreOrder());
                    System.out.println("DEBUG SOUND: hasRoleAccess=" + hasRoleAccess + ", notifyStoreOrder=" + notifyOrder);

                    if (hasRoleAccess && notifyOrder) {
                        getUI().ifPresent(ui -> ui.access(() -> {
                            System.out.println("DEBUG SOUND: Disparando JS playAlertSound para " + message.getAlertSound());
                            ui.getPage().executeJs("if(window.playAlertSound) { window.playAlertSound($0); }", message.getAlertSound());
                        }));
                    }
                }
            };
            
            // Ouvir mensagens de todos os grupos do usuário para tocar sons de alertas/steps
            groupService.findActiveGroupsByUser(u).forEach(group -> {
                chatService.register(group.getId(), chatMessageListener);
            });
        });
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (globalTaskListener != null) {
            taskService.unregisterGlobal(globalTaskListener);
        }
        if (chatMessageListener != null && currentUser != null) {
            groupService.findActiveGroupsByUser(currentUser).forEach(group -> {
                chatService.unregister(group.getId(), chatMessageListener);
            });
        }
    }
}
