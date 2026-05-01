package com.xflprflx.neokioski.services;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.xflprflx.neokioski.models.ReportColumnConfig;
import com.xflprflx.neokioski.models.StoreReportConfig;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.enums.ReportOrientation;
import com.xflprflx.neokioski.repositories.StoreReportConfigRepository;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
/**
 * Serviço responsável por construir e exportar arquivos e relatórios em formato PDF utilizando a biblioteca OpenPDF.
 */
public class PdfService {

    private final StoreReportConfigRepository configRepository;
    private final TaskService taskService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    public PdfService(StoreReportConfigRepository configRepository, TaskService taskService) {
        this.configRepository = configRepository;
        this.taskService = taskService;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ByteArrayInputStream generateStoreReportPdf(List<TaskService.StoreReportEntry> entries) {
        // 1. Agrupar por data de coleta (LocalDate) usando TreeMap para manter a ordem cronológica
        Map<LocalDate, List<TaskService.StoreReportEntry>> entriesByDate = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.collectedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        Document document = new Document(PageSize.A4, 36, 36, 120, 60); // Margens maiores para cabeçalho e rodapé
        
        // Definir orientação inicial baseada no primeiro grupo
        if (!entriesByDate.isEmpty()) {
            LocalDate firstDate = entriesByDate.keySet().iterator().next();
            StoreReportConfig firstConfig = configRepository.findActiveConfig(firstDate).orElse(null);
            if (firstConfig != null && firstConfig.getOrientation() == ReportOrientation.LANDSCAPE) {
                document.setPageSize(PageSize.A4.rotate());
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            // Fontes
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font fontSmall = FontFactory.getFont(FontFactory.HELVETICA, 7);

            // Evento para Cabeçalho e Rodapé em cada página
            StoreReportPageEvent event = new StoreReportPageEvent(fontSmall);
            writer.setPageEvent(event);

            document.open();

            int groupIndex = 1;
            int totalGroups = entriesByDate.size();

            for (Map.Entry<LocalDate, List<TaskService.StoreReportEntry>> group : entriesByDate.entrySet()) {
                LocalDate date = group.getKey();
                List<TaskService.StoreReportEntry> dateEntries = group.getValue();
                
                StoreReportConfig config = configRepository.findActiveConfig(date)
                        .orElseThrow(() -> new RuntimeException("Não foi encontrada uma configuração de relatório ativa para a data: " + date));

                // Atualizar o contexto do evento para a página atual
                event.updateContext(config, groupIndex++, totalGroups);

                // Mudar orientação se necessário
                Rectangle pageSize = config.getOrientation() == ReportOrientation.LANDSCAPE ? 
                        PageSize.A4.rotate() : PageSize.A4;
                document.setPageSize(pageSize);

                if (groupIndex > 2) { // Não é o primeiro grupo
                    document.newPage();
                }

                // Espaço para o cabeçalho (que é desenhado pelo evento)
                document.add(new Paragraph("\n"));

                // --- TÍTULO ---
                Paragraph title = new Paragraph("PEDIDOS RETIRA LOJA", fontHeader);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);
                document.add(new Paragraph("\n"));

                // --- TABELA DE DADOS ---
                List<ReportColumnConfig> columns = config.getColumns();
                PdfPTable table;
                int maxLines = (config.getOrientation() == ReportOrientation.LANDSCAPE) ? 18 : 28;

                if (columns.isEmpty()) {
                    table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{1, 2, 4, 4});

                    table.addCell(createHeaderCell("SQ", fontTitle));
                    table.addCell(createHeaderCell("Data", fontTitle));
                    table.addCell(createHeaderCell("Número da Delivery", fontTitle));
                    table.addCell(createHeaderCell("Entregue para:", fontTitle));
                    table.setHeaderRows(1); // Repetir header se quebrar página

                    int sq = 1;
                    for (TaskService.StoreReportEntry entry : dateEntries) {
                        table.addCell(createDataCell(String.valueOf(sq++), fontNormal));
                        table.addCell(createDataCell(dateFormatter.format(entry.collectedAt()), fontNormal));
                        table.addCell(createDataCell(entry.deliveryNumber(), fontNormal));
                        table.addCell(createDataCell(entry.collectorName(), fontNormal));
                    }
                    for (int i = dateEntries.size(); i < maxLines; i++) {
                        table.addCell(createDataCell(String.valueOf(sq++), fontNormal));
                        table.addCell(createDataCell("", fontNormal)); table.addCell(createDataCell("", fontNormal)); table.addCell(createDataCell("", fontNormal));
                    }
                } else {
                    int numCols = columns.size() + 1;
                    table = new PdfPTable(numCols);
                    table.setWidthPercentage(100);
                    table.addCell(createHeaderCell("SQ", fontTitle));
                    for (ReportColumnConfig col : columns) table.addCell(createHeaderCell(col.getLabel(), fontTitle));
                    table.setHeaderRows(1);

                    int sq = 1;
                    for (TaskService.StoreReportEntry entry : dateEntries) {
                        table.addCell(createDataCell(String.valueOf(sq++), fontNormal));
                        for (ReportColumnConfig col : columns) {
                            String value = taskService.getTaskAttributeValue(entry.task(), col.getAttributeKey());
                            table.addCell(createDataCell(value, fontNormal));
                        }
                    }
                    for (int i = dateEntries.size(); i < maxLines; i++) {
                        table.addCell(createDataCell(String.valueOf(sq++), fontNormal));
                        for (int j = 0; j < columns.size(); j++) table.addCell(createDataCell("", fontNormal));
                    }
                }

                document.add(table);
            }

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ByteArrayInputStream generateGeneralTaskReportPdf(List<Task> tasks) {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        /*
         * Inicia um bloco try-catch para tratar possíveis exceções.
         */
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8);

            Paragraph title = new Paragraph("Relatório Geral de Tarefas", fontHeader);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Gerado em: " + dateFormatter.format(java.time.Instant.now()) + "\n\n", fontNormal));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 3, 3, 3, 3, 2});

            table.addCell(createHeaderCell("ID", fontTitle));
            table.addCell(createHeaderCell("Data", fontTitle));
            table.addCell(createHeaderCell("Tipo", fontTitle));
            table.addCell(createHeaderCell("Grupo", fontTitle));
            table.addCell(createHeaderCell("Criador", fontTitle));
            table.addCell(createHeaderCell("Status", fontTitle));

            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (Task task : tasks) {
                table.addCell(createDataCell(String.valueOf(task.getId()), fontNormal));
                table.addCell(createDataCell(dateFormatter.format(task.getCreatedAt()), fontNormal));
                table.addCell(createDataCell(task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "-", fontNormal));
                table.addCell(createDataCell(task.getGroup() != null ? task.getGroup().getName() : "-", fontNormal));
                table.addCell(createDataCell(task.getCreator() != null ? task.getCreator().getName() : "Sistema", fontNormal));
                table.addCell(createDataCell(task.getCurrentStep() != null ? task.getCurrentStep().getLabel() : "Pendente", fontNormal));
            }

            document.add(table);
            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return new ByteArrayInputStream(out.toByteArray());
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderWidth(0.5f);
        cell.setPadding(2);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return cell;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private PdfPCell createHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 230, 230)); // Cinza claro como na imagem
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderWidth(1f);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return cell;
    }

    /**
     * métodx ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private PdfPCell createDataCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setBorderWidth(0.5f);
        /*
         * Retorna o resultado final processado pelo métodx.
         */
        return cell;
    }


    private PdfPCell createNoBorderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setPadding(2);
        return cell;
    }

    /**
     * Classe auxiliar para gerenciar cabeçalho e rodapé em todas as páginas do relatório.
     */
    private class StoreReportPageEvent extends com.lowagie.text.pdf.PdfPageEventHelper {
        private StoreReportConfig config;
        private int groupIndex;
        private int totalGroups;
        private final Font fontSmall;

        public StoreReportPageEvent(Font fontSmall) {
            this.fontSmall = fontSmall;
        }

        public void updateContext(StoreReportConfig config, int groupIndex, int totalGroups) {
            this.config = config;
            this.groupIndex = groupIndex;
            this.totalGroups = totalGroups;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (config == null) return;

            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();

            // --- DESENHAR CABEÇALHO ---
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setTotalWidth(document.right() - document.left());
            try {
                headerTable.setWidths(new float[]{3, 1});
                
                PdfPTable identTable = new PdfPTable(2);
                identTable.setWidthPercentage(100);
                identTable.setWidths(new float[]{1, 2});
                identTable.addCell(createNoBorderCell("ID / Identificador:", fontSmall));
                identTable.addCell(createNoBorderCell(config.getIdentifier(), fontSmall));
                identTable.addCell(createNoBorderCell("Revisão / Revision:", fontSmall));
                identTable.addCell(createNoBorderCell(config.getRevision(), fontSmall));
                identTable.addCell(createNoBorderCell("Título / Title:", fontSmall));
                identTable.addCell(createNoBorderCell(config.getTitle(), fontSmall));
                identTable.addCell(createNoBorderCell("Responsável / Owner:", fontSmall));
                identTable.addCell(createNoBorderCell(config.getOwner(), fontSmall));

                PdfPCell leftCell = new PdfPCell();
                leftCell.addElement(identTable);
                leftCell.setBorder(Rectangle.NO_BORDER);
                Paragraph langParagraph = new Paragraph("Este documento será escrito em / This document will be written in:", fontSmall);
                langParagraph.setSpacingBefore(5);
                leftCell.addElement(langParagraph);
                leftCell.addElement(new Paragraph("[x] Português/Portuguese / [ ] Inglês/English / [ ] Português-Inglês/Portuguese-English", fontSmall));
                headerTable.addCell(leftCell);

                PdfPCell rightCell = new PdfPCell(new Phrase("Template: " + config.getTemplateCode() + (config.getRevision() != null ? " rev. " + config.getRevision() : ""), fontSmall));
                rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                rightCell.setBorder(Rectangle.NO_BORDER);
                headerTable.addCell(rightCell);

                headerTable.writeSelectedRows(0, -1, document.left(), document.getPageSize().getTop() - 20, cb);

            } catch (DocumentException e) { e.printStackTrace(); }

            // --- DESENHAR RODAPÉ ---
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setTotalWidth(document.right() - document.left());
            try {
                footerTable.setWidths(new float[]{1, 1, 1});
                PdfPCell footerLeft = new PdfPCell(new Phrase("©JGGC Indústria e Comércio\nde Materiais Dentários S.A.", fontSmall));
                footerLeft.setBorder(Rectangle.NO_BORDER);
                footerTable.addCell(footerLeft);

                PdfPCell footerCenter = new PdfPCell(new Phrase("Template: " + config.getIdentifier() + (config.getRevision() != null ? ", revision " + config.getRevision() : ""), fontSmall));
                footerCenter.setBorder(Rectangle.NO_BORDER);
                footerCenter.setHorizontalAlignment(Element.ALIGN_CENTER);
                footerTable.addCell(footerCenter);

                PdfPCell footerRight = new PdfPCell(new Phrase("Page " + groupIndex + "/" + totalGroups, fontSmall));
                footerRight.setBorder(Rectangle.NO_BORDER);
                footerRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
                footerTable.addCell(footerRight);

                footerTable.writeSelectedRows(0, -1, document.left(), document.bottom() - 10, cb);
            } catch (DocumentException e) { e.printStackTrace(); }
        }
    }
}
