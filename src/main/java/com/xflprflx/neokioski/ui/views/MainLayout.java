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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.html.Span;
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
@com.vaadin.flow.server.auth.AnonymousAllowed
public class MainLayout extends AppLayout implements AfterNavigationObserver, BeforeEnterObserver {

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

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void createHeader() {
        H1 logo = new H1("NeoKioski");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        Button changePasswordBtn = new Button("Senha", VaadinIcon.KEY.create());
        changePasswordBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        changePasswordBtn.addClickListener(e -> showChangePasswordDialog());
        changePasswordBtn.getStyle().set("cursor", "pointer");

        Button logout = new Button("Sair");
        logout.addClickListener(event -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (currentUser != null) {
                presenceService.forceOffline(currentUser.getId());
            }
            authContext.logout();
        });
        logout.getStyle().set("cursor", "pointer");
        logout.getStyle().set("margin-right", "20px");

        Button themeToggle = new Button(VaadinIcon.ADJUST.create(), e -> {
            com.vaadin.flow.component.UI.getCurrent().getElement().executeJs(
                "const theme = document.documentElement.getAttribute('theme') === 'dark' ? 'light' : 'dark';" +
                "document.documentElement.setAttribute('theme', theme);" +
                "localStorage.setItem('dashboard-theme', theme);" +
                "window.dispatchEvent(new Event('theme-changed'));"
            );
        });
        themeToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggle.getStyle().set("cursor", "pointer");
        // Use margin-left auto to push right-aligned items
        themeToggle.getStyle().set("margin-left", "auto");

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, themeToggle, changePasswordBtn, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void createDrawer() {
        drawerContainer.setSizeFull();
        drawerContainer.setPadding(false);
        drawerContainer.setSpacing(false);

        showPrimaryNav();

        Scroller scroller = new Scroller(drawerContainer);
        scroller.setClassName(LumoUtility.Padding.SMALL);
        addToDrawer(scroller);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void showPrimaryNav() {
        drawerContainer.removeAll();
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Início", HomeView.class, VaadinIcon.DASHBOARD.create()));

        SideNavItem chatsItem = new SideNavItem("Chats", (String) null, VaadinIcon.CHAT.create());
        chatsItem.getElement().addEventListener("click", e -> showChatNav(null));
        nav.addItem(chatsItem);

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (authContext.hasAnyRole("ADMIN")) {
            nav.addItem(new SideNavItem("Grupos", GroupListView.class, VaadinIcon.USERS.create()));
            nav.addItem(new SideNavItem("Usuários", UserListView.class, VaadinIcon.USER_CARD.create()));
            nav.addItem(new SideNavItem("Templates", TaskTemplateListView.class, VaadinIcon.FILE_TEXT.create()));
            nav.addItem(new SideNavItem("Relatório Geral", GeneralTaskReportView.class, VaadinIcon.LIST_OL.create()));
            nav.addItem(new SideNavItem("Relatório Loja", StoreReportView.class, VaadinIcon.CHART.create()));
            nav.addItem(new SideNavItem("Modelos de Relatório", StoreReportConfigListView.class, VaadinIcon.EDIT.create()));
        }

        drawerContainer.add(nav);
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void beforeEnter(BeforeEnterEvent event) {
        securityService.getAuthenticatedInternalUser().ifPresent(u -> {
            // Verificar no banco de dados se o usuário ainda está ativo
            boolean active = userService.findById(u.getId())
                    .map(User::isActive)
                    .orElse(false);
            
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!active) {
                System.out.println("[SECURITY] Usuário inativo detectado na navegação: " + u.getUsername());
                
                // Forçar logout e redirecionar
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (currentUser != null) {
                    presenceService.forceOffline(currentUser.getId());
                }
                
                // Notificar usuário (opcional, pode ser rápido demais para ver)
                getUI().ifPresent(ui -> ui.access(() -> {
                    Notification.show("Sua conta foi desativada. Entre em contato com o administrador.", 
                            5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }));

                authContext.logout();
                event.forwardTo(LoginView.class);
            }
        });
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    public void afterNavigation(AfterNavigationEvent event) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!event.getActiveChain().isEmpty() && event.getActiveChain().get(0) instanceof ChatView) {
            // Extrair groupId da URL para carregar tasks do grupo
            String location = event.getLocation().getPath();
            // URL padrão: /chat/123
            String[] parts = location.split("/");
            Group group = null;
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (parts.length >= 2) {
                /*
                 * Inicia um bloco try-catch para evitar que erros internos quebrem a renderização da interface.
                 */
                try {
                    long groupId = Long.parseLong(parts[parts.length - 1]);
                    group = groupService.findById(groupId).orElse(null);
                } catch (NumberFormatException ignored) {}
            }
            showChatNav(group);
        }
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private String loadAudioAsBase64(String resourcePath) {
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (is == null) return "";
            byte[] bytes = is.readAllBytes();
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            /*
             * Retorna o componente renderizado ou o valor da operação atual.
             */
            return "";
        }
    }

    @Override
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
         */
        for (String sound : availableSounds) {
            String url = "/sounds/" + sound.replace("#", "%23");
            jsBuilder.append(String.format("window.audioData['%s'] = '%s';", sound, url));
        }
        
        // Injetar sons Base64
        jsBuilder.append(String.format("window.audioData['new-message'] = 'data:audio/mp3;base64,%s';", newMessageB64));
        jsBuilder.append(String.format("window.audioData['reply-to-me'] = 'data:audio/mp3;base64,%s';", replyToMeB64));
        jsBuilder.append(String.format("window.audioData['i-was-mentioned'] = 'data:audio/mp3;base64,%s';", mentionedB64));
        jsBuilder.append(String.format("window.audioData['remove-or-edit'] = 'data:audio/mp3;base64,%s';", removeOrEditB64));

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (getUI().isEmpty()) return;
                
                System.out.println("[DEBUG-SOUND] Mensagem recebida por: " + u.getUsername() + " (ID: " + u.getId() + ")");

                // Buscar usuário fresco para garantir preferências atuais (NotifyStoreOrder, etc)
                userService.findById(u.getId()).ifPresentOrElse(freshUser -> {
                    
                    System.out.println("[DEBUG-SOUND] Usuário: " + freshUser.getUsername() + 
                                     " | Role: " + (freshUser.getRole() != null ? freshUser.getRole().name() : "NULL") +
                                     " | Pedidos: " + freshUser.isNotifyStoreOrder() + 
                                     " | Chat: " + freshUser.isNotifyChatSounds());

                    // Prioridade 1: Alertas operacionais (AlertSound preenchido)
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (message.getAlertSound() != null && !message.getAlertSound().isEmpty()) {
                        System.out.println("[DEBUG-SOUND] Operacional: " + message.getAlertSound() + " | Roles: " + message.getAlertRoles());
                        
                        boolean hasRoleAccess = true;
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (message.getAlertRoles() != null && !message.getAlertRoles().isEmpty()) {
                            hasRoleAccess = false;
                            String[] roles = message.getAlertRoles().split(",");
                            /*
                             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
                             */
                            for (String rStr : roles) {
                                String userRole = (freshUser.getRole() != null) ? freshUser.getRole().name() : "NULL";
                                String targetRole = rStr.trim();
                                
                                /*
                                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                 */
                                if (userRole.equalsIgnoreCase(targetRole)) {
                                    hasRoleAccess = true;
                                    break;
                                }
                            }
                        }
                        
                        System.out.println("[DEBUG-SOUND] hasRoleAccess: " + hasRoleAccess);

                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
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
                    
                    /*
                     * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                     */
                    if (freshUser.isNotifyChatSounds() && !isSystemMessageWithNoSound) {
                        /*
                         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                         */
                        if (message.getUser() == null || !message.getUser().getId().equals(freshUser.getId())) {
                            getUI().ifPresent(ui -> ui.access(() -> {
                                String soundToPlay = null;
                                
                                boolean isReplyToMe = message.getReplyTo() != null && 
                                                     message.getReplyTo().getUser() != null &&
                                                     message.getReplyTo().getUser().getId().equals(freshUser.getId());
                                
                                boolean mentioned = message.getContent() != null && 
                                                   message.getContent().contains("@" + freshUser.getUsername());

                                /*
                                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                 */
                                if (isReplyToMe) {
                                    soundToPlay = "reply-to-me";
                                } else if (mentioned) {
                                    soundToPlay = "i-was-mentioned";
                                } else if (message.getAlertSound() == null || message.getAlertSound().isEmpty()) {
                                    soundToPlay = "new-message";
                                }
                                
                                /*
                                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                                 */
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
            // #improve_n+1 - Uso dos grupos já carregados no EntityGraph do SecurityService (Bloco C otimizado)
            java.util.List<com.xflprflx.neokioski.models.Group> groups = u.getGroups().stream()
                    .filter(com.xflprflx.neokioski.models.Group::isActive)
                    .collect(java.util.stream.Collectors.toList());
            
            // FALLBACK PARA ADMIN: Registrar em todos os grupos ativos se a lista estiver vazia
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (u.getRole() == com.xflprflx.neokioski.models.enums.Role.ADMIN && (groups == null || groups.isEmpty())) {
                System.out.println("[DEBUG-SOUND] Admin sem grupos explícitos. Registrando fallback global.");
                groups = groupService.findAll();
            }

            System.out.println("[DEBUG-SOUND] Usuário " + u.getUsername() + " registrando em " + (groups != null ? groups.size() : 0) + " grupos.");
            
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (groups != null) {
                groups.forEach(group -> {
                    chatService.register(group.getId(), chatMessageListener);
                });
            }
        });
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
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
    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (currentUser != null) {
            presenceService.markOffline(currentUser.getId());
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (chatMessageListener != null && currentUser != null) {
            groupService.findActiveGroupsByUser(currentUser).forEach(group -> {
                chatService.unregister(group.getId(), chatMessageListener);
            });
        }
    }

    /**
     * métodx ou construtor responsável por inicializar componentes visuais, vincular dados (binder) ou tratar eventos de UI.
     */
    private void showChangePasswordDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Alterar Minha Senha");

        PasswordField currentPassword = new PasswordField("Senha Atual");
        PasswordField newPassword = new PasswordField("Nova Senha");
        PasswordField confirmPassword = new PasswordField("Confirmar Nova Senha");

        VerticalLayout dialogLayout = new VerticalLayout(currentPassword, newPassword, confirmPassword);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "300px").set("max-width", "100%");
        
        currentPassword.setWidthFull();
        newPassword.setWidthFull();
        confirmPassword.setWidthFull();

        dialog.add(dialogLayout);

        Button saveButton = new Button("Salvar", e -> {
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (currentUser == null) return;

            String currentPass = currentPassword.getValue();
            String newPass = newPassword.getValue();
            String confirmPass = confirmPassword.getValue();

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Notification.show("Por favor, preencha todos os campos", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!userService.isPasswordValid(currentUser, currentPass)) {
                Notification.show("Senha atual incorreta", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!newPass.equals(confirmPass)) {
                Notification.show("As novas senhas não coincidem", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Validar força da senha: min 6, [A-Z], [a-z], [0-9], [@$!%*?&]
            /*
             * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
             */
            if (!(newPass.length() >= 6 && 
                  newPass.matches(".*[A-Z].*") && 
                  newPass.matches(".*[a-z].*") && 
                  newPass.matches(".*\\d.*") && 
                  newPass.matches(".*[@$!%*?&].*"))) {
                Notification.show("A senha deve conter ao menos 6 caracteres, incluindo maiúsculas, minúsculas, números e símbolos (@$!%*?&)", 
                        5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            userService.changePassword(currentUser.getId(), newPass);
            Notification.show("Senha alterada com sucesso!", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            dialog.close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancelar", e -> dialog.close());
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }
}
