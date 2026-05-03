package com.xflprflx.neokioski.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.xflprflx.neokioski.models.TaskSlaResult;
import com.xflprflx.neokioski.services.TaskSlaService;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modal obrigatório exibido quando um SLA é descumprido (DELAYED).
 * Força o usuário a selecionar uma justificativa antes de poder continuar.
 */
public class SlaJustificationDialog extends Dialog {

    private final TaskSlaService taskSlaService;
    private final List<TaskSlaResult> pendingResults;
    private int currentIndex = 0;

    public SlaJustificationDialog(TaskSlaService taskSlaService, List<TaskSlaResult> pendingResults, Runnable onComplete) {
        this.taskSlaService = taskSlaService;
        this.pendingResults = pendingResults;

        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);
        setModal(true);

        if (!pendingResults.isEmpty()) {
            showJustificationForm(pendingResults.get(0), onComplete);
        }
    }

    private void showJustificationForm(TaskSlaResult result, Runnable onComplete) {
        removeAll();
        getFooter().removeAll();

        String slaName = result.getSlaConfig() != null ? result.getSlaConfig().getName() : "SLA";
        setHeaderTitle("⚠️ SLA Atrasado: " + slaName);

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        layout.setMinWidth("400px");

        // Informação do atraso
        long actualSeconds = result.getActualSeconds() != null ? result.getActualSeconds() : 0;
        long targetSeconds = switch (result.getUnit()) {
            case SECONDS -> result.getTargetValue();
            case MINUTES -> result.getTargetValue() * 60L;
            case HOURS -> result.getTargetValue() * 3600L;
        };
        long delaySeconds = actualSeconds - targetSeconds;
        
        String delayFormatted = formatDuration(delaySeconds);
        String actualFormatted = formatDuration(actualSeconds);
        String targetFormatted = result.getTargetValue() + " " + result.getUnit().getLabel();

        Span infoSpan = new Span("O SLA \"" + slaName + "\" foi descumprido.");
        infoSpan.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-error-text-color)");

        Span detailSpan = new Span("Meta: " + targetFormatted + " | Tempo real: " + actualFormatted + " | Atraso: " + delayFormatted);
        detailSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        // ComboBox obrigatório
        List<String> options = result.getSlaConfig().getJustificationOptionsList();
        ComboBox<String> justificationCombo = new ComboBox<>("Justificativa *");
        justificationCombo.setItems(options);
        justificationCombo.setWidthFull();
        justificationCombo.setRequired(true);
        justificationCombo.setRequiredIndicatorVisible(true);
        justificationCombo.setPlaceholder("Selecione a justificativa...");

        // TextArea opcional
        TextArea commentsField = new TextArea("Comentários (opcional)");
        commentsField.setWidthFull();
        commentsField.setPlaceholder("Adicione detalhes para enriquecer a análise...");
        commentsField.setMaxLength(2000);

        layout.add(infoSpan, detailSpan, justificationCombo, commentsField);
        add(layout);

        // Botão confirmar
        Button confirmBtn = new Button("Confirmar Justificativa", ev -> {
            if (justificationCombo.isEmpty()) {
                justificationCombo.setInvalid(true);
                justificationCombo.setErrorMessage("Selecione uma justificativa para continuar.");
                Notification.show("Selecione uma justificativa obrigatória.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            taskSlaService.updateJustification(
                    result.getId(),
                    justificationCombo.getValue(),
                    commentsField.getValue()
            );

            // Atualizar o objeto em memória também
            result.setJustification(justificationCombo.getValue());
            result.setJustificationComments(commentsField.getValue());

            currentIndex++;
            if (currentIndex < pendingResults.size()) {
                // Próximo SLA pendente
                showJustificationForm(pendingResults.get(currentIndex), onComplete);
            } else {
                close();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirmBtn.setWidthFull();

        // Contador se houver múltiplos
        if (pendingResults.size() > 1) {
            Span counter = new Span("Justificativa " + (currentIndex + 1) + " de " + pendingResults.size());
            counter.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("text-align", "center");
            getFooter().add(counter);
        }

        getFooter().add(confirmBtn);
    }

    private String formatDuration(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "min";
        } else if (minutes > 0) {
            return minutes + "min " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}
