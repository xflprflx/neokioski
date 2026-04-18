package com.xflprflx.neokioski.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.TaskFieldValue;
import com.xflprflx.neokioski.ui.utils.TaskUIUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TaskDetailsDialog extends Dialog {

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public TaskDetailsDialog(Task task) {
        setWidth("700px");
        setMaxWidth("90vw");
        setHeaderTitle("Detalhes da Tarefa");

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

        if (!task.getSlaResults().isEmpty()) {
            Span slaTitle = new Span("SLAs:");
            slaTitle.getStyle().set("font-weight", "bold");
            content.add(slaTitle);
            for (com.xflprflx.neokioski.models.TaskSlaResult sla : task.getSlaResults()) {
                String slaName = sla.getSlaConfig() != null ? sla.getSlaConfig().getName() : "SLA";
                String target = sla.getTargetValue() + " " + (sla.getUnit() != null ? sla.getUnit().getLabel().toLowerCase() : "");
                String actual = formatSeconds(sla.getActualSeconds());
                
                Span statusBadge = new Span(sla.getStatus() != null ? sla.getStatus().getDescription() : "");
                statusBadge.getStyle()
                        .set("padding", "1px 6px")
                        .set("border-radius", "8px")
                        .set("font-size", "var(--lumo-font-size-xxs)")
                        .set("font-weight", "bold")
                        .set("margin-left", "8px");
                
                if (sla.getStatus() == com.xflprflx.neokioski.models.enums.SlaStatus.ON_TIME) {
                    statusBadge.getStyle().set("background-color", "#e8f5e9").set("color", "#2e7d32");
                } else if (sla.getStatus() == com.xflprflx.neokioski.models.enums.SlaStatus.DELAYED) {
                    statusBadge.getStyle().set("background-color", "#ffebee").set("color", "#c62828");
                }

                HorizontalLayout slaRow = new HorizontalLayout(
                    new Span("• " + slaName + " (Meta: " + target + ") → " + actual),
                    statusBadge
                );
                slaRow.setSpacing(false);
                slaRow.setAlignItems(FlexComponent.Alignment.CENTER);
                content.add(slaRow);
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

        Scroller scroller = new Scroller(content);
        scroller.setMaxHeight("75vh");
        add(scroller);
        getFooter().add(new Button("Fechar", ev -> close()));
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

    private String formatSeconds(long seconds) {
        if (seconds < 60) return seconds + "s";
        long mins = seconds / 60;
        long secs = seconds % 60;
        if (mins < 60) return mins + "m " + secs + "s";
        long hrs = mins / 60;
        mins = mins % 60;
        return hrs + "h " + mins + "m " + secs + "s";
    }
}
