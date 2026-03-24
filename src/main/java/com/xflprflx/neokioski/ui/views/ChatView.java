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
import com.xflprflx.neokioski.models.TaskEvent;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.enums.Role;
import com.xflprflx.neokioski.models.template.TaskFieldDefinition;
import com.xflprflx.neokioski.models.TaskFieldValue;

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
import jakarta.annotation.security.PermitAll;

import java.io.IOException;
import java.io.InputStream;
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
    private Element activeMentionTarget;
    private Div activeSuggestionWrapper;
    private ListBox<User> activeSuggestionList;
    private final Map<String, Div> suggestionWrappers = new HashMap<>();
    private final Map<String, ListBox<User>> suggestionLists = new HashMap<>();
    private String activeMentionId = "main-input";
    private final Map<Long, HorizontalLayout> messageWrappers = new HashMap<>();
    private final Map<Long, Long> taskIdToMessageId = new HashMap<>();
    
    private Consumer<ChatMessage> listener;
    private Consumer<Task> taskListener;
    
    private final String newMessageB64;
    private final String replyToMeB64;
    private final String mentionedB64;
    private final String removeOrEditB64;
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
        
        // Load audio data as Base64 from resources
        this.newMessageB64 = loadAudioAsBase64("themes/neokioski/sounds/new-message.mp3");
        this.replyToMeB64 = loadAudioAsBase64("themes/neokioski/sounds/reply-to-me.mp3");
        this.mentionedB64 = loadAudioAsBase64("themes/neokioski/sounds/i-was-mentioned.mp3");
        this.removeOrEditB64 = loadAudioAsBase64("themes/neokioski/sounds/remove-or-edit.mp3");

        configureUI();
        unlockAudio();
    }

    private String loadAudioAsBase64(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Aviso: Recurso de áudio não encontrado em: " + resourcePath);
                return "";
            }
            byte[] bytes = is.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void unlockAudio() {
        // Inject data and prepare unlock listener
        getElement().executeJs(
            "window.audioData = {" +
            "  'new-message': 'data:audio/mp3;base64," + newMessageB64 + "'," +
            "  'reply-to-me': 'data:audio/mp3;base64," + replyToMeB64 + "'," +
            "  'i-was-mentioned': 'data:audio/mp3;base64," + mentionedB64 + "'," +
            "  'remove-or-edit': 'data:audio/mp3;base64," + removeOrEditB64 + "'" +
            "};" +
            "const unlock = () => {" +
            "  const ctx = new (window.AudioContext || window.webkitAudioContext)();" +
            "  ctx.resume().then(() => {" +
            "    console.log('Sistema de áudio desbloqueado.');" +
            "    document.removeEventListener('click', unlock);" +
            "    document.removeEventListener('keydown', unlock);" +
            "  });" +
            "};" +
            "document.addEventListener('click', unlock);" +
            "document.addEventListener('keydown', unlock);"
        );
    }

    private void configureUI() {
        messageContainer.setWidthFull();
        messageContainer.setPadding(true);
        messageContainer.setSpacing(true);

        scroller.setSizeFull();
        messageInput.setWidthFull();

        messageInput.addSubmitListener(event -> {
            if (group != null && currentUser != null) {
                chatService.sendMessage(event.getValue(), currentUser, group, replyTo);
                cancelReply();
            }
        });

        configureReplyBar();

        groupName.getStyle()
                .set("padding-top", "var(--lumo-space-m)")
                .set("padding-bottom", "var(--lumo-space-m)")
                .set("padding-left", "var(--lumo-space-l)")
                .set("padding-right", "var(--lumo-space-l)")
                .set("font-size", "var(--lumo-font-size-xl)");
        
        getStyle().set("position", "relative"); // Para o suggestionWrapper absoluto funcionar
        
        configureTemplateBar();
        add(groupName, scroller, replyBar, templateBar, messageInput);
        configureSuggestions();
    }

    private void configureTemplateBar() {
        templateBar.setWidthFull();
        templateBar.setPadding(true);
        templateBar.setSpacing(true);
        templateBar.setAlignItems(FlexComponent.Alignment.CENTER);
        templateBar.getStyle().set("overflow-x", "auto");
        templateBar.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
        templateBar.getStyle().set("background-color", "var(--lumo-base-color)");
        templateBar.setVisible(false);
    }

    private void loadTemplates() {
        templateBar.removeAll();
        List<TaskTemplate> templates = taskTemplateService.getTemplatesByGroupId(group.getId());
        if (templates.isEmpty()) {
            templateBar.setVisible(false);
            return;
        }

        Span templateLabel = new Span("Criar tarefa com modelo:");
        templateLabel.getStyle().set("font-weight", "bold");
        templateLabel.getStyle().set("margin-right", "8px");
        templateLabel.getStyle().set("white-space", "nowrap");
        
        templateBar.add(templateLabel);

        for (TaskTemplate t : templates) {
            Button btn = new Button(t.getName(), e -> openTemplateDialog(t));
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            btn.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            btn.getStyle().set("border-radius", "8px");
            btn.getStyle().set("white-space", "nowrap");
            btn.getStyle().set("color", "var(--lumo-body-text-color)");
            btn.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
            
            // Icon visual hint
            if (t.getName().toLowerCase().contains("bug")) {
                btn.setIcon(new Span("\uD83D\uDC1B")); // Bug emoji
            } else if (t.getName().toLowerCase().contains("feature") || t.getName().toLowerCase().contains("review")) {
                btn.setIcon(new Span("⭐"));
            } else if (t.getName().toLowerCase().contains("meeting")) {
                btn.setIcon(new Span("\uD83D\uDCC4")); // Paper
            } else if (t.getName().toLowerCase().contains("design")) {
                btn.setIcon(new Span("\uD83C\uDFA8")); // Palette
            } else {
                btn.setIcon(new Span("\uD83D\uDCDD")); // Memo
            }
            
            templateBar.add(btn);
        }
        templateBar.setVisible(true);
    }

    private void openTemplateDialog(TaskTemplate template) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");
        dialog.setMaxWidth("90%");
        dialog.setHeaderTitle("Nova Tarefa: " + template.getName());

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setPadding(false);
        formLayout.setSpacing(true);

        List<TemplateFieldRelation> fields = taskTemplateService.getFieldRelationsByTemplateId(template.getId());

        // Mapa de campo → componente para coletar valor depois
        java.util.LinkedHashMap<TaskFieldDefinition, com.vaadin.flow.component.Component> fieldComponents = new java.util.LinkedHashMap<>();


        for (TemplateFieldRelation rel : fields) {
            InputType type = rel.getTaskFieldDefinition().getInputType();
            String label = rel.getTaskFieldDefinition().getLabel();
            String desc = rel.getTaskFieldDefinition().getDescription();

            List<String> options = new ArrayList<>();
            if (rel.getTaskFieldDefinition().getTaskFieldOptions() != null) {
                options = rel.getTaskFieldDefinition().getTaskFieldOptions().stream()
                        .map(TaskFieldOption::getOptionLabel)
                        .toList();
            }

            com.vaadin.flow.component.Component fieldComponent = null;

            switch (type) {
                case TEXT:
                    com.vaadin.flow.component.textfield.TextField tf = new com.vaadin.flow.component.textfield.TextField(label);
                    tf.setHelperText(desc);
                    tf.setWidthFull();
                    tf.setRequiredIndicatorVisible(rel.isRequired());
                    fieldComponent = tf;
                    break;
                case DATE:
                    com.vaadin.flow.component.datepicker.DatePicker dp = new com.vaadin.flow.component.datepicker.DatePicker(label);
                    dp.setHelperText(desc);
                    dp.setWidthFull();
                    dp.setRequiredIndicatorVisible(rel.isRequired());
                    fieldComponent = dp;
                    break;
                case TIME:
                    com.vaadin.flow.component.timepicker.TimePicker tp = new com.vaadin.flow.component.timepicker.TimePicker(label);
                    tp.setHelperText(desc);
                    tp.setWidthFull();
                    tp.setRequiredIndicatorVisible(rel.isRequired());
                    fieldComponent = tp;
                    break;
                case NUMBER:
                    com.vaadin.flow.component.textfield.NumberField nf = new com.vaadin.flow.component.textfield.NumberField(label);
                    nf.setHelperText(desc);
                    nf.setWidthFull();
                    nf.setRequiredIndicatorVisible(rel.isRequired());
                    fieldComponent = nf;
                    break;
                case CHECKBOX:
                    com.vaadin.flow.component.checkbox.CheckboxGroup<String> cbGroup = new com.vaadin.flow.component.checkbox.CheckboxGroup<>();
                    cbGroup.setLabel(label);
                    cbGroup.setItems(options);
                    cbGroup.setRequiredIndicatorVisible(rel.isRequired());
                    fieldComponent = cbGroup;
                    break;
                case RADIO:
                    com.vaadin.flow.component.radiobutton.RadioButtonGroup<String> radio = new com.vaadin.flow.component.radiobutton.RadioButtonGroup<>();
                    radio.setLabel(label);
                    radio.setHelperText(desc);
                    radio.setItems(options);
                    radio.setRequiredIndicatorVisible(rel.isRequired());
                    fieldComponent = radio;
                    break;
                case DROPDOWN:
                    com.vaadin.flow.component.combobox.ComboBox<String> combo = new com.vaadin.flow.component.combobox.ComboBox<>(label);
                    combo.setHelperText(desc);
                    combo.setWidthFull();
                    combo.setRequiredIndicatorVisible(rel.isRequired());
                    combo.setItems(options);
                    fieldComponent = combo;
                    break;
                case MULTISELECT_DROPDOWN:
                    com.vaadin.flow.component.combobox.MultiSelectComboBox<String> multiCombo = new com.vaadin.flow.component.combobox.MultiSelectComboBox<>(label);
                    multiCombo.setHelperText(desc);
                    multiCombo.setWidthFull();
                    multiCombo.setRequiredIndicatorVisible(rel.isRequired());
                    multiCombo.setItems(options);
                    fieldComponent = multiCombo;
                    break;
                case TEXT_LIST:
                    com.vaadin.flow.component.combobox.MultiSelectComboBox<String> textList = new com.vaadin.flow.component.combobox.MultiSelectComboBox<>(label);
                    textList.setHelperText(desc);
                    textList.setWidthFull();
                    textList.setRequiredIndicatorVisible(rel.isRequired());
                    textList.setAllowCustomValue(true);
                    textList.addCustomValueSetListener(e -> {
                        String customValue = e.getDetail();
                        Set<String> items = new LinkedHashSet<>(textList.getGenericDataView().getItems().toList());
                        items.add(customValue);
                        textList.setItems(items);
                        Set<String> currentValues = new HashSet<>(textList.getValue());
                        currentValues.add(customValue);
                        textList.setValue(currentValues);
                    });
                    fieldComponent = textList;
                    break;
            }

            if (fieldComponent != null) {
                fieldComponents.put(rel.getTaskFieldDefinition(), fieldComponent);
                formLayout.add(fieldComponent);
            }
        }

        if (fields.isEmpty()) {
            formLayout.add(new Span("Este template não possui campos configurados."));
        }

        Button cancelBtn = new Button("Cancelar", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button submitBtn = new Button("Enviar", e -> {
            // Coletar valores dos campos
            java.util.Map<TaskFieldDefinition, String> fieldValues = new java.util.LinkedHashMap<>();
            for (java.util.Map.Entry<TaskFieldDefinition, com.vaadin.flow.component.Component> entry : fieldComponents.entrySet()) {
                String value = extractFieldValue(entry.getValue());
                fieldValues.put(entry.getKey(), value);
            }

            dialog.close();

            // Criar task
            Task task = taskService.createTask(template, fieldValues, currentUser, group);

            // Enviar mensagem vinculada à task
            ChatMessage msg = chatService.sendMessage("📋 Tarefa: " + template.getName(), currentUser, group);
            // Vincular task à mensagem
            chatService.linkTaskToMessage(msg.getId(), task);
        });
        submitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(formLayout);
        dialog.getFooter().add(cancelBtn, submitBtn);
        dialog.open();
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
        activeSuggestionList = suggestionList;
        activeMentionTarget = messageInput.getElement();
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
            if (mentionId.equals("main-input")) {
                this.activeMentionTarget = messageInput.getElement();
            }
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
        this.currentUser = securityService.getAuthenticatedInternalUser().orElse(null);

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
        setupRealTimeUpdate();
    }

    private void loadMessages() {
        messageContainer.removeAll();
        messageWrappers.clear();
        List<ChatMessage> messages = chatService.getMessagesByGroup(group);
        for (ChatMessage msg : messages) {
            addOrUpdateMessageInUI(msg);
        }
        scrollToBottom();
    }

    private void addOrUpdateMessageInUI(ChatMessage msg) {
        if (msg.getTask() != null) {
            taskIdToMessageId.put(msg.getTask().getId(), msg.getId());
        }
        if (messageWrappers.containsKey(msg.getId())) {
            HorizontalLayout oldWrapper = messageWrappers.get(msg.getId());
            HorizontalLayout newWrapper;
            if (msg.getTask() != null) {
                newWrapper = createTaskCardLayout(msg.getTask());
            } else {
                newWrapper = createMessageLayout(msg);
            }
            messageContainer.replace(oldWrapper, newWrapper);
            messageWrappers.put(msg.getId(), newWrapper);
        } else {
            HorizontalLayout wrapper;
            if (msg.getTask() != null) {
                wrapper = createTaskCardLayout(msg.getTask());
            } else {
                wrapper = createMessageLayout(msg);
            }
            messageContainer.add(wrapper);
            messageWrappers.put(msg.getId(), wrapper);
        }
    }

    private HorizontalLayout createMessageLayout(ChatMessage msg) {
        boolean isMine = currentUser != null && msg.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRole() == Role.ADMIN;

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

        // Avatar
        Avatar avatar = new Avatar(msg.getUser().getName());
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
            
            Span replyText = new Span(repliedMsg.getContent());
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
     * Extrai o valor de texto de qualquer tipo de campo do formulário.
     */
    @SuppressWarnings("unchecked")
    private String extractFieldValue(com.vaadin.flow.component.Component component) {
        if (component instanceof com.vaadin.flow.component.textfield.TextField tf) return tf.getValue();
        if (component instanceof com.vaadin.flow.component.textfield.NumberField nf)
            return nf.getValue() != null ? String.valueOf(nf.getValue()) : "";
        if (component instanceof com.vaadin.flow.component.datepicker.DatePicker dp)
            return dp.getValue() != null ? dp.getValue().toString() : "";
        if (component instanceof com.vaadin.flow.component.timepicker.TimePicker tp)
            return tp.getValue() != null ? tp.getValue().toString() : "";
        if (component instanceof com.vaadin.flow.component.checkbox.CheckboxGroup<?> cb)
            return String.join(", ", (Set<String>) cb.getValue());
        if (component instanceof com.vaadin.flow.component.radiobutton.RadioButtonGroup<?> rb)
            return rb.getValue() != null ? rb.getValue().toString() : "";
        if (component instanceof com.vaadin.flow.component.combobox.ComboBox<?> combo)
            return combo.getValue() != null ? combo.getValue().toString() : "";
        if (component instanceof com.vaadin.flow.component.combobox.MultiSelectComboBox<?> multi)
            return String.join(", ", (Set<String>) multi.getValue());
        return "";
    }

    /**
     * Cria o card visual de uma task para exibir no fluxo de mensagens.
     */
    private HorizontalLayout createTaskCardLayout(Task task) {
        // Container principal do card
        Div card = new Div();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-left", "4px solid var(--lumo-primary-color)")
                .set("border-radius", "12px")
                .set("padding", "var(--lumo-space-m)")
                .set("background", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("max-width", "480px")
                .set("min-width", "280px");

        // Emoji + template name (cabeçalho)
        String templateName = task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "Tarefa";
        String emoji = resolveTemplateEmoji(templateName);
        Span header = new Span(emoji + " " + (task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "Tarefa"));
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
                    .set("background-color", colorTheme != null ? colorTheme : "var(--lumo-primary-color-10pct)")
                    .set("color", "var(--lumo-primary-text-color)")
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "bold");
            statusRow.add(statusLabel, stepBadge);
        } else {
            Span pendenteBadge = new Span("⏳ Pendente");
            pendenteBadge.getStyle()
                    .set("background-color", "var(--lumo-contrast-20pct)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("padding", "2px 10px")
                    .set("border-radius", "12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "bold");
            statusRow.add(statusLabel, pendenteBadge);
        }

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
                Div row = new Div();
                Span lbl = new Span(fieldLabel + ": ");
                lbl.getStyle().set("font-weight", "bold");
                row.add(lbl, new Span(fv.getValue()));
                fieldsDiv.add(row);
            }
            card.add(fieldsDiv);
        }

        // Botões dos steps do template
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);

        if (task.getTaskTemplate() != null) {
            List<TemplateStepRelation> allRelations = taskTemplateService.getStepRelationsByTemplateId(task.getTaskTemplate().getId());
            
            // Determinar quais steps mostrar
            List<TemplateStepRelation> visibleRelations;
            if (task.getCurrentStep() != null) {
                // Buscar a relação da etapa atual para ver as próximas permitidas
                Optional<TemplateStepRelation> currentRelOpt = allRelations.stream()
                        .filter(r -> r.getTaskStepDefinition().getId().equals(task.getCurrentStep().getId()))
                        .findFirst();
                
                if (currentRelOpt.isPresent() && !currentRelOpt.get().getAllowedNextSteps().isEmpty()) {
                    List<Long> allowedIds = currentRelOpt.get().getAllowedNextSteps().stream()
                            .map(TaskStepDefinition::getId)
                            .toList();
                    visibleRelations = allRelations.stream()
                            .filter(r -> allowedIds.contains(r.getTaskStepDefinition().getId()))
                            .toList();
                } else {
                    // Se não houver restrição, mostrar todas exceto a atual (opcional, vamos manter todas por enquanto como era)
                    visibleRelations = allRelations;
                }
            } else {
                // Task Pendente: mostrar todas
                visibleRelations = allRelations;
            }

            for (TemplateStepRelation sr : visibleRelations) {
                TaskStepDefinition step = sr.getTaskStepDefinition();
                // Não mostrar botão para a etapa em que já estamos
                if (task.getCurrentStep() != null && step.getId().equals(task.getCurrentStep().getId())) {
                    continue;
                }

                Button stepBtn = new Button(step.getLabel(), ev -> {
                    taskService.executeStep(task.getId(), step, currentUser);
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

    private String resolveTemplateEmoji(String templateName) {
        if (templateName == null) return "📋";
        String lower = templateName.toLowerCase();
        if (lower.contains("bug")) return "🐛";
        if (lower.contains("feature")) return "⭐";
        if (lower.contains("review")) return "🔄";
        if (lower.contains("meeting")) return "📄";
        if (lower.contains("design")) return "🎨";
        if (lower.contains("qa")) return "🔍";
        return "📋";
    }

    private void openTaskDetailsDialog(Task task) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setHeaderTitle("Detalhes da Tarefa");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);

        Span title = new Span(resolveTemplateEmoji(task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "") + " " + (task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "Tarefa"));
        title.getStyle().set("font-weight", "bold").set("font-size", "var(--lumo-font-size-l)");
        content.add(title);

        if (task.getCurrentStep() != null) {
            Span status = new Span("Status: " + task.getCurrentStep().getLabel());
            status.getStyle().set("font-size", "var(--lumo-font-size-s)");
            content.add(status);
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
                content.add(new Span("• " + lbl + ": " + fv.getValue()));
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
                content.add(new Span("• " + who + " → " + step + " (" + when + ")"));
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
            if (getUI().isPresent()) {
                getUI().get().access(() -> {
                    boolean isNew = !messageWrappers.containsKey(msg.getId());
                    addOrUpdateMessageInUI(msg);
                    scrollToBottom();
                    
                    if (!msg.getUser().getId().equals(currentUser.getId())) {
                        boolean isReplyToMe = msg.getReplyTo() != null && 
                                             msg.getReplyTo().getUser().getId().equals(currentUser.getId());
                        boolean mentioned = currentUser != null && isUserMentioned(msg.getContent(), currentUser.getUsername());
                        
                        if (isNew) {
                            if (isReplyToMe) {
                                playNotificationSound("reply-to-me");
                                Notification n = new Notification(msg.getUser().getName() + " respondeu a você!");
                                n.setDuration(5000);
                                n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                                n.open();
                            } else if (mentioned) {
                                playNotificationSound("i-was-mentioned");
                                Notification n = new Notification(msg.getUser().getName() + " mencionou você!");
                                n.setDuration(5000);
                                n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                                n.open();
                            } else {
                                playNotificationSound("new-message");
                            }
                        } else {
                            if (mentioned && !msg.isDeleted()) {
                                playNotificationSound("i-was-mentioned");
                                Notification n = new Notification(msg.getUser().getName() + " mencionou você na edição da mensagem!");
                                n.setDuration(5000);
                                n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                                n.open();
                            } else {
                                playNotificationSound("remove-or-edit");
                            }
                        }
                    }
                });
            }
        };

        if (group != null) {
            chatService.register(group.getId(), listener);
        }

        taskListener = task -> {
            if (getUI().isPresent()) {
                getUI().get().access(() -> {
                    Long messageId = taskIdToMessageId.get(task.getId());
                    if (messageId != null) {
                        // Buscar a mensagem novamente para garantir que tenha o estado mais recente
                        // ou apenas atualizar o card diretamente se possível.
                        // Como addOrUpdateMessageInUI reconstrói o layout, precisamos da ChatMessage completa.
                        chatService.getMessagesByGroup(group).stream()
                            .filter(m -> m.getId().equals(messageId))
                            .findFirst()
                            .ifPresent(this::addOrUpdateMessageInUI);
                    }
                });
            }
        };

        if (group != null) {
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
        this.activeSuggestionWrapper = dialogSuggestionWrapper;
        this.activeSuggestionList = dialogSuggestionList;
        this.activeMentionTarget = textArea.getElement();

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

    private void playNotificationSound(String type) {
        getElement().executeJs(
            "try {" +
            "  const audioSrc = window.audioData['" + type + "'];" +
            "  if (!audioSrc) throw new Error('Dados de áudio não encontrados para: ' + '" + type + "');" +
            "  const audio = new Audio(audioSrc);" +
            "  audio.play().catch(e => console.error('Falha ao tocar áudio (' + '" + type + "' + '):', e));" +
            "} catch (err) {" +
            "  console.error('Erro na função playNotificationSound:', err);" +
            "}"
        );
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        String js = String.format(
            "window.audioData = window.audioData || {};" +
            "window.audioData['new-message'] = 'data:audio/mp3;base64,%s';" +
            "window.audioData['reply-to-me'] = 'data:audio/mp3;base64,%s';" +
            "window.audioData['i-was-mentioned'] = 'data:audio/mp3;base64,%s';",
            newMessageB64, replyToMeB64, mentionedB64
        );
        attachEvent.getUI().getPage().executeJs(js);
        showPermissionDialog();
    }

    private void showPermissionDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Notificações de Áudio");
        
        VerticalLayout dialogLayout = new VerticalLayout(
            new Span("Para ouvir as notificações de novas mensagens, clique no botão abaixo para ativar o sistema de áudio.")
        );
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);
        dialog.add(dialogLayout);

        Button startButton = new Button("Entrar no Chat", e -> {
            dialog.close();
            // O clique no botão já serve para desbloquear o áudio devido aos listeners globais
        });
        startButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        dialog.getFooter().add(startButton);
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);
        dialog.open();
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
