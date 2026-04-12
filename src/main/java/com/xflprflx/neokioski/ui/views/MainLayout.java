package com.xflprflx.neokioski.ui.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.xflprflx.neokioski.services.PresenceService;
import jakarta.annotation.security.PermitAll;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
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
    private final PresenceService presenceService;
    private final VerticalLayout drawerContainer = new VerticalLayout();
    private User currentUser;
    private List<String> availableSounds = new ArrayList<>();
    private Consumer<ChatMessage> chatMessageListener;

    public MainLayout(AuthenticationContext authContext, GroupService groupService,
                      SecurityService securityService, TaskService taskService, UserService userService, 
                      SoundService soundService, ChatService chatService, PresenceService presenceService) {
        this.authContext = authContext;
        this.groupService = groupService;
        this.securityService = securityService;
        this.taskService = taskService;
        this.userService = userService;
        this.chatService = chatService;
        this.presenceService = presenceService;
        
        // Obter nomes de todos os sons disponíveis
        this.availableSounds = soundService.getAvailableAlertSounds();
        
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("NeoKioski");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        Button logout = new Button("Sair");
        logout.addClickListener(event -> {
            if (currentUser != null) {
                presenceService.forceOffline(currentUser.getId());
            }
            authContext.logout();
        });
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
            nav.addItem(new SideNavItem("Modelos de Relatório", StoreReportConfigListView.class, VaadinIcon.EDIT.create()));
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

    private String loadAudioAsBase64(String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return "";
            byte[] bytes = is.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        
        securityService.getAuthenticatedInternalUser().ifPresent(u -> {
            presenceService.markOnline(u.getId());
        });
        
        // Carregar sons fixos do chat
        String newMessageB64 = loadAudioAsBase64("themes/neokioski/sounds/new-message.mp3");
        String replyToMeB64 = loadAudioAsBase64("themes/neokioski/sounds/reply-to-me.mp3");
        String mentionedB64 = loadAudioAsBase64("themes/neokioski/sounds/i-was-mentioned.mp3");
        String removeOrEditB64 = loadAudioAsBase64("themes/neokioski/sounds/remove-or-edit.mp3");

        // Injetar dados de áudio no cliente
        StringBuilder jsBuilder = new StringBuilder();
        jsBuilder.append("window.audioData = window.audioData || {};");
        
        // Mapear cada som disponível para sua URL estática
        for (String sound : availableSounds) {
            String url = "/sounds/" + sound.replace("#", "%23");
            jsBuilder.append(String.format("window.audioData['%s'] = '%s';", sound, url));
        }
        
        // Injetar sons Base64
        jsBuilder.append(String.format("window.audioData['new-message'] = 'data:audio/mp3;base64,%s';", newMessageB64));
        jsBuilder.append(String.format("window.audioData['reply-to-me'] = 'data:audio/mp3;base64,%s';", replyToMeB64));
        jsBuilder.append(String.format("window.audioData['i-was-mentioned'] = 'data:audio/mp3;base64,%s';", mentionedB64));
        jsBuilder.append(String.format("window.audioData['remove-or-edit'] = 'data:audio/mp3;base64,%s';", removeOrEditB64));

        if (!availableSounds.contains("pedido-loja")) {
             jsBuilder.append("window.audioData['pedido-loja'] = '/sounds/pedido-loja';");
        }

        jsBuilder.append(
            "window.playAlertSound = function(soundName) {" +
            "  console.log('SOUND: Play Alert Sound triggered:', soundName);" +
            "  const url = window.audioData[soundName];" +
            "  if(url && url.length > 50) {" +
            "    const audio = new Audio(url);" +
            "    audio.play().catch(e => console.error('Audio play failed (prob autoplay policy):', e));" +
            "  } else if(url) {" +
            "    const audio = new Audio(url);" +
            "    audio.play().catch(e => console.error('Audio play failed:', e));" +
            "  } else { console.warn('SOUND: Audio URL missing for ', soundName); }" +
            "};" +
            "window.playStoreOrderSound = function() { window.playAlertSound('pedido-loja'); };"
        );
        attachEvent.getUI().getPage().executeJs(jsBuilder.toString());
        
        showPermissionDialog();

        // Capturar usuário atual e ID para a closure
        securityService.getAuthenticatedInternalUser().ifPresent(u -> {
            this.currentUser = u;
            // Registrar listener de mensagens para alertas temporais, steps e chat comum
            this.chatMessageListener = message -> {
                if (getUI().isEmpty()) return;
                
                System.out.println("[DEBUG-SOUND] Mensagem recebida por: " + u.getUsername() + " (ID: " + u.getId() + ")");

                // Buscar usuário fresco para garantir preferências atuais (NotifyStoreOrder, etc)
                userService.findById(u.getId()).ifPresentOrElse(freshUser -> {
                    
                    System.out.println("[DEBUG-SOUND] Usuário: " + freshUser.getUsername() + 
                                     " | Role: " + (freshUser.getRole() != null ? freshUser.getRole().name() : "NULL") +
                                     " | Pedidos: " + freshUser.isNotifyStoreOrder() + 
                                     " | Chat: " + freshUser.isNotifyChatSounds());

                    // Prioridade 1: Alertas operacionais (AlertSound preenchido)
                    if (message.getAlertSound() != null && !message.getAlertSound().isEmpty()) {
                        System.out.println("[DEBUG-SOUND] Operacional: " + message.getAlertSound() + " | Roles: " + message.getAlertRoles());
                        
                        boolean hasRoleAccess = true;
                        if (message.getAlertRoles() != null && !message.getAlertRoles().isEmpty()) {
                            hasRoleAccess = false;
                            String[] roles = message.getAlertRoles().split(",");
                            for (String rStr : roles) {
                                String userRole = (freshUser.getRole() != null) ? freshUser.getRole().name() : "NULL";
                                String targetRole = rStr.trim();
                                
                                if (userRole.equalsIgnoreCase(targetRole)) {
                                    hasRoleAccess = true;
                                    break;
                                }
                            }
                        }
                        
                        System.out.println("[DEBUG-SOUND] hasRoleAccess: " + hasRoleAccess);

                        if (hasRoleAccess && freshUser.isNotifyStoreOrder()) {
                            getUI().ifPresent(ui -> ui.access(() -> {
                                System.out.println("[DEBUG-SOUND] Tocar JS: " + message.getAlertSound());
                                ui.getPage().executeJs("if(window.playAlertSound) { window.playAlertSound($0); }", message.getAlertSound());
                            }));
                        } else {
                            System.out.println("[DEBUG-SOUND] Bloqueado: Access=" + hasRoleAccess + ", Notify=" + freshUser.isNotifyStoreOrder());
                        }
                        return;
                    }
                    
                    // Prioridade 2: Sons de chat
                    boolean isSystemMessageWithNoSound = (message.getUser() == null && (message.getAlertSound() == null || message.getAlertSound().isEmpty()));
                    
                    if (freshUser.isNotifyChatSounds() && !isSystemMessageWithNoSound) {
                        if (message.getUser() == null || !message.getUser().getId().equals(freshUser.getId())) {
                            getUI().ifPresent(ui -> ui.access(() -> {
                                String soundToPlay = null;
                                
                                boolean isReplyToMe = message.getReplyTo() != null && 
                                                     message.getReplyTo().getUser() != null &&
                                                     message.getReplyTo().getUser().getId().equals(freshUser.getId());
                                
                                boolean mentioned = message.getContent() != null && 
                                                   message.getContent().contains("@" + freshUser.getUsername());

                                if (isReplyToMe) {
                                    soundToPlay = "reply-to-me";
                                } else if (mentioned) {
                                    soundToPlay = "i-was-mentioned";
                                } else if (message.getAlertSound() == null || message.getAlertSound().isEmpty()) {
                                    soundToPlay = "new-message";
                                }
                                
                                if (soundToPlay != null) {
                                    System.out.println("[DEBUG-SOUND] Tocar JS Chat: " + soundToPlay);
                                    ui.getPage().executeJs("if(window.playAlertSound) { window.playAlertSound($0); }", soundToPlay);
                                }
                            }));
                        }
                    }
                }, () -> {
                    System.out.println("[DEBUG-SOUND] ERRO: Usuário ID " + u.getId() + " não encontrado pelo userService!");
                });
            };
            
            // Registrar canais de áudio
            java.util.List<com.xflprflx.neokioski.models.Group> groups = groupService.findActiveGroupsByUser(u);
            
            // FALLBACK PARA ADMIN: Registrar em todos os grupos ativos se a lista estiver vazia
            if (u.getRole() == com.xflprflx.neokioski.models.enums.Role.ADMIN && (groups == null || groups.isEmpty())) {
                System.out.println("[DEBUG-SOUND] Admin sem grupos explícitos. Registrando fallback global.");
                groups = groupService.findAll();
            }

            System.out.println("[DEBUG-SOUND] Usuário " + u.getUsername() + " registrando em " + (groups != null ? groups.size() : 0) + " grupos.");
            
            if (groups != null) {
                groups.forEach(group -> {
                    chatService.register(group.getId(), chatMessageListener);
                });
            }
        });
    }

    private void showPermissionDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Ativar Alertas Sonoros");
        
        com.vaadin.flow.component.html.Span text = new com.vaadin.flow.component.html.Span(
            "Para ouvir os alertas de novos pedidos e mensagens, é necessário autorizar o áudio no navegador."
        );
        VerticalLayout layout = new VerticalLayout(text);
        layout.setPadding(false);
        dialog.add(layout);

        Button confirmBtn = new Button("Autorizar Áudio", e -> {
            dialog.close();
            // O clique dispara o unlock global via JS injetado
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmBtn.setWidthFull();
        
        dialog.getFooter().add(confirmBtn);
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.open();
    }

    @Override
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        
        if (currentUser != null) {
            presenceService.markOffline(currentUser.getId());
        }

        if (chatMessageListener != null && currentUser != null) {
            groupService.findActiveGroupsByUser(currentUser).forEach(group -> {
                chatService.unregister(group.getId(), chatMessageListener);
            });
        }
    }
}
