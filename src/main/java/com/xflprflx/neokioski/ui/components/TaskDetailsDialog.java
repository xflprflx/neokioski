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

/**
 * Componente visual (Dialog) do Vaadin utilizado para exibir os detalhes minuciosos de uma tarefa específica e interagir com seu fluxo.
 */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (task.getCreator() != null) {
            content.add(new Span("Criado por: @" + task.getCreator().getUsername()));
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!task.getTaskFieldValues().isEmpty()) {
            Span fieldsTitle = new Span("Campos:");
            fieldsTitle.getStyle().set("font-weight", "bold");
            content.add(fieldsTitle);
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
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

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!task.getSlaResults().isEmpty()) {
            Span slaTitle = new Span("SLAs:");
            slaTitle.getStyle().set("font-weight", "bold");
            content.add(slaTitle);
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
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
                
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
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

                if (sla.getStatus() == com.xflprflx.neokioski.models.enums.SlaStatus.DELAYED) {
                    if (sla.getJustification() != null && !sla.getJustification().isBlank()) {
                        Span justLabel = new Span("  Justificativa: " + sla.getJustification());
                        justLabel.getStyle()
                                .set("font-style", "italic")
                                .set("color", "var(--lumo-secondary-text-color)")
                                .set("margin-left", "20px")
                                .set("font-size", "var(--lumo-font-size-s)");
                        content.add(justLabel);
                    }
                    if (sla.getJustificationComments() != null && !sla.getJustificationComments().isBlank()) {
                        Span commentLabel = new Span("  Comentários: " + sla.getJustificationComments());
                        commentLabel.getStyle()
                                .set("font-style", "italic")
                                .set("color", "var(--lumo-secondary-text-color)")
                                .set("margin-left", "20px")
                                .set("font-size", "var(--lumo-font-size-s)");
                        content.add(commentLabel);
                    }
                }
            }
        }

        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (!task.getTaskEvents().isEmpty()) {
            Span histTitle = new Span("Histórico e Tempos:");
            histTitle.getStyle().set("font-weight", "bold");
            content.add(histTitle);

            // Gráfico de tempo por etapa
            java.util.List<com.xflprflx.neokioski.models.TaskEvent> events = new java.util.ArrayList<>(task.getTaskEvents());
            events.sort(java.util.Comparator.comparing(com.xflprflx.neokioski.models.TaskEvent::getTimestamp));
            
            // Identificar IDs de etapas finalizadoras para ocultar na legenda/barra
            java.util.Set<Long> finalizerStepIds = task.getTaskTemplate() != null ? 
                task.getTaskTemplate().getTemplateStepRelation().stream()
                    .filter(com.xflprflx.neokioski.models.template.TemplateStepRelation::isFinalizer)
                    .map(r -> r.getTaskStepDefinition().getId())
                    .collect(java.util.stream.Collectors.toSet()) : java.util.Collections.emptySet();

            java.util.Map<String, Long> stepDurations = new java.util.LinkedHashMap<>();
            for (int i = 0; i < events.size(); i++) {
                com.xflprflx.neokioski.models.TaskEvent current = events.get(i);
                java.time.Instant start = (i == 0 && current.getTimestamp().isAfter(task.getCreatedAt())) 
                                         ? task.getCreatedAt() : current.getTimestamp();
                java.time.Instant end;
                if (i < events.size() - 1) {
                    end = events.get(i + 1).getTimestamp();
                } else {
                    end = (task.getFinalizedAt() != null) ? task.getFinalizedAt() : java.time.Instant.now();
                }
                
                long durationMillis = Math.max(0, end.toEpochMilli() - start.toEpochMilli());
                
                // Se for etapa finalizadora ou tiver tempo desprezível, não incluímos no gráfico
                if (current.getTaskStepDefinition() != null && finalizerStepIds.contains(current.getTaskStepDefinition().getId())) {
                    continue;
                }

                String sLabel = current.getTaskStepDefinition() != null ? current.getTaskStepDefinition().getLabel() : "Aguardando";
                stepDurations.put(sLabel, stepDurations.getOrDefault(sLabel, 0L) + durationMillis);
            }

            java.util.List<String> seriesList = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Long> entry : stepDurations.entrySet()) {
                if (entry.getValue() <= 0) continue; // Pula etapas sem tempo registrado
                double mins = entry.getValue() / 60000.0;
                String valJs = String.format(java.util.Locale.US, "%.2f", mins);
                seriesList.add("{ name: '" + entry.getKey().replace("'", "\\'") + "', data: [" + valJs + "] }");
            }
            String seriesDataJs = "[" + String.join(",", seriesList) + "]";

            com.vaadin.flow.component.html.Div chartDiv = new com.vaadin.flow.component.html.Div();
            chartDiv.setId("chart-step-time-" + task.getId());
            chartDiv.setWidthFull();
            chartDiv.setHeight("75px");
            
            com.vaadin.flow.component.html.Span totalTimeSpan = new com.vaadin.flow.component.html.Span("Calculando tempo total...");
            totalTimeSpan.setId("total-time-" + task.getId());
            totalTimeSpan.getStyle()
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "bold")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("margin-left", "30px")
                    .set("margin-top", "-10px")
                    .set("margin-bottom", "var(--lumo-space-xs)");

            chartDiv.addAttachListener(e -> {
                e.getUI().getPage().executeJs(
                    "if (typeof ApexCharts === 'undefined') {" +
                    "  var script = document.createElement('script');" +
                    "  script.src = 'https://cdn.jsdelivr.net/npm/apexcharts';" +
                    "  script.onload = function() { renderTimeline($0, $1); };" +
                    "  document.head.appendChild(script);" +
                    "} else {" +
                    "  renderTimeline($0, $1);" +
                    "}" +
                    "function renderTimeline(id, totalId) {" +
                    "  setTimeout(function() {" +
                    "    var el = document.getElementById(id);" +
                    "    var totalEl = document.getElementById(totalId);" +
                    "    if (el) {" +
                    "      el.innerHTML = '';" +
                    "      var formatMins = function(val) {" +
                    "         var totalSecs = Math.round(val * 60);" +
                    "         var h = Math.floor(totalSecs / 3600);" +
                    "         var m = Math.floor((totalSecs % 3600) / 60);" +
                    "         var s = totalSecs % 60;" +
                    "         if (h > 0) return h + ':' + (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s);" +
                    "         if (m > 0) return m + ':' + (s < 10 ? '0' + s : s);" +
                    "         return s + 's';" +
                    "      };" +
                    "      var updateTotal = function(chart) {" +
                    "         var total = 0;" +
                    "         var series = chart.w.config.series;" +
                    "         var collapsed = chart.w.globals.collapsedSeriesIndices || [];" +
                    "         series.forEach(function(s, i) {" +
                    "            if (collapsed.indexOf(i) === -1) total += s.data[0];" +
                    "         });" +
                    "         if (totalEl) totalEl.innerText = '⏱️ Tempo Total Visualizado: ' + formatMins(total);" +
                    "      };" +
                    "      var options = {" +
                    "        series: " + seriesDataJs + "," +
                    "        chart: {" +
                    "           type: 'bar', height: 75, stacked: true, toolbar: { show: false }, sparkline: { enabled: true }," +
                    "           events: {" +
                    "              mounted: function(chart) { updateTotal(chart); }," +
                    "              legendClick: function(chart, sIdx, opts) { setTimeout(function(){ updateTotal(chart); }, 100); }" +
                    "           }" +
                    "        }," +
                    "        grid: { padding: { left: 30, right: 30, top: 0, bottom: 0 } }," +
                    "        plotOptions: { bar: { horizontal: true, barHeight: '30%', borderRadius: 3 } }," +
                    "        dataLabels: { " +
                    "           enabled: true, " +
                    "           style: { fontSize: '9px', fontWeight: 'bold' }, " +
                    "           formatter: function(val) { " +
                    "               if (val < 0.1) return ''; " +
                    "               if (val < 1) return Math.round(val * 60) + 's'; " +
                    "               return val.toFixed(1) + 'm'; " +
                    "           } " +
                    "        }," +
                    "        tooltip: { " +
                    "           enabled: true, " +
                    "           y: { formatter: function(val) { return formatMins(val); } } " +
                    "        }," +
                    "        legend: { show: true, position: 'top', horizontalAlign: 'left', fontSize: '10px', markers: { radius: 12, width: 8, height: 8 } }," +
                    "        xaxis: { categories: ['Duração'] }" +
                    "      };" +
                    "      var chart = new ApexCharts(el, options);" +
                    "      chart.render();" +
                    "    }" +
                    "  }, 100);" +
                    "}", chartDiv.getId().get(), totalTimeSpan.getId().get()
                );
            });
            content.add(chartDiv, totalTimeSpan);

            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos visuais ou coleções de dados.
             */
            for (com.xflprflx.neokioski.models.TaskEvent ev : task.getTaskEvents()) {
                String who = ev.getUser() != null ? "@" + ev.getUser().getUsername() : "sistema";
                String when = ev.getTimestamp() != null ? dateTimeFormatter.format(ev.getTimestamp()) : "";
                String step = ev.getTaskStepDefinition() != null ? ev.getTaskStepDefinition().getLabel() : "";
                String line = "• " + who + " → " + step + " (" + when + ")";
                content.add(new Span(line));
                /*
                 * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
                 */
                if (ev.getResponse() != null && !ev.getResponse().isBlank()) {
                    String respLabel = "Resp";
                    if (task.getTaskTemplate() != null && ev.getTaskStepDefinition() != null) {
                        for (com.xflprflx.neokioski.models.template.TemplateStepRelation rel : task.getTaskTemplate().getTemplateStepRelation()) {
                            if (rel.getTaskStepDefinition() != null && rel.getTaskStepDefinition().getId().equals(ev.getTaskStepDefinition().getId())) {
                                if (rel.getResponseFieldLabel() != null && !rel.getResponseFieldLabel().isBlank()) {
                                    respLabel = rel.getResponseFieldLabel();
                                }
                                break;
                            }
                        }
                    }
                    String respStr = ev.getResponse();
                    Span resp = new Span("  " + respLabel + ": " + respStr);
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
        /*
         * Adiciona o(s) componente(s) visual(is) ao layout atual do Vaadin.
         */
        add(scroller);
        getFooter().add(new Button("Fechar", ev -> close()));
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
    private String formatSeconds(long seconds) {
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (seconds < 60) return seconds + "s";
        long mins = seconds / 60;
        long secs = seconds % 60;
        /*
         * Verifica uma condição lógica para alterar o estado ou exibição de um componente na interface.
         */
        if (mins < 60) return mins + "m " + secs + "s";
        long hrs = mins / 60;
        mins = mins % 60;
        /*
         * Retorna o componente renderizado ou o valor da operação atual.
         */
        return hrs + "h " + mins + "m " + secs + "s";
    }
}
