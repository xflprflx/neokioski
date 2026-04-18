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
public class PdfService {

    private final StoreReportConfigRepository configRepository;
    private final TaskService taskService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    public PdfService(StoreReportConfigRepository configRepository, TaskService taskService) {
        this.configRepository = configRepository;
        this.taskService = taskService;
    }

    public ByteArrayInputStream generateStoreReportPdf(List<TaskService.StoreReportEntry> entries) {
        // 1. Agrupar por data de coleta (LocalDate) usando TreeMap para manter a ordem cronológica
        Map<LocalDate, List<TaskService.StoreReportEntry>> entriesByDate = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.collectedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()
                ));

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        
        // Definir orientação inicial baseada no primeiro grupo
        if (!entriesByDate.isEmpty()) {
            LocalDate firstDate = entriesByDate.keySet().iterator().next();
            StoreReportConfig firstConfig = configRepository.findActiveConfig(firstDate)
                    .orElse(null);
            if (firstConfig != null && firstConfig.getOrientation() == ReportOrientation.LANDSCAPE) {
                document.setPageSize(PageSize.A4.rotate());
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fontes
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font fontSmall = FontFactory.getFont(FontFactory.HELVETICA, 7);

            boolean firstPage = true;
            int pageNumber = 1;
            int totalPages = entriesByDate.size();

            for (Map.Entry<LocalDate, List<TaskService.StoreReportEntry>> group : entriesByDate.entrySet()) {
                LocalDate date = group.getKey();
                List<TaskService.StoreReportEntry> dateEntries = group.getValue();
                // Buscar configuração ativa para ESTA data específica
                StoreReportConfig config = configRepository.findActiveConfig(date)
                        .orElseThrow(() -> new RuntimeException("Não foi encontrada uma configuração de relatório ativa para a data: " + date));

                // Mudar orientação se necessário
                Rectangle pageSize = config.getOrientation() == ReportOrientation.LANDSCAPE ? 
                        PageSize.A4.rotate() : PageSize.A4;
                document.setPageSize(pageSize);

                if (firstPage) {
                    // Na primeira página, o iText precisa do newPage após o open e setPageSize 
                    // para aplicar a rotação se for diferente do padrão inicial (A4 Portrait).
                    // No Lowagie, document.open() cria a primeira página. 
                    // Se mudarmos o pageSize logo após o open, a primeira página assume o novo tamanho 
                    // se nenhuma instrução de escrita tiver sido enviada ainda.
                } else {
                    document.newPage();
                }

                // --- CABEÇALHO ---
                PdfPTable headerTable = new PdfPTable(2);
                headerTable.setWidthPercentage(100);
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
                
                document.add(headerTable);
                document.add(new Paragraph("\n"));

                // Definir limite de linhas para caber o rodapé na mesma página
                int maxLines = (config.getOrientation() == ReportOrientation.LANDSCAPE) ? 18 : 28;

                // --- TÍTULO ---
                Paragraph title = new Paragraph("PEDIDOS RETIRA LOJA", fontHeader);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);
                document.add(new Paragraph("\n"));

                // --- TABELA DE DADOS ---
                List<ReportColumnConfig> columns = config.getColumns();
                PdfPTable table;
                if (columns.isEmpty()) {
                    // Padrão antigo
                    table = new PdfPTable(4);
                    table.setWidthPercentage(100);
                    table.setWidths(new float[]{1, 2, 4, 4});

                    table.addCell(createHeaderCell("SQ", fontTitle));
                    table.addCell(createHeaderCell("Data", fontTitle));
                    table.addCell(createHeaderCell("Número da Delivery", fontTitle));
                    table.addCell(createHeaderCell("Entregue para:", fontTitle));

                    int sq = 1;
                    for (TaskService.StoreReportEntry entry : dateEntries) {
                        table.addCell(createDataCell(String.valueOf(sq++), fontNormal));
                        table.addCell(createDataCell(dateFormatter.format(entry.collectedAt()), fontNormal));
                        table.addCell(createDataCell(entry.deliveryNumber(), fontNormal));
                        table.addCell(createDataCell(entry.collectorName(), fontNormal));
                    }

                    for (int i = dateEntries.size(); i < maxLines; i++) {
                        table.addCell(createDataCell(String.valueOf(sq++), fontNormal));
                        table.addCell(createDataCell("", fontNormal));
                        table.addCell(createDataCell("", fontNormal));
                        table.addCell(createDataCell("", fontNormal));
                    }
                } else {
                    // Customizado
                    int numCols = columns.size() + 1; // +1 para SQ
                    table = new PdfPTable(numCols);
                    table.setWidthPercentage(100);
                    
                    // Header
                    table.addCell(createHeaderCell("SQ", fontTitle));
                    for (ReportColumnConfig col : columns) {
                        table.addCell(createHeaderCell(col.getLabel(), fontTitle));
                    }

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
                        for (int j = 0; j < columns.size(); j++) {
                            table.addCell(createDataCell("", fontNormal));
                        }
                    }
                }

                document.add(table);
                document.add(new Paragraph("\n"));

                // --- RODAPÉ ---
                PdfPTable footerTable = new PdfPTable(3);
                footerTable.setWidthPercentage(100);
                footerTable.setWidths(new float[]{1, 1, 1});
                
                PdfPCell footerLeft = new PdfPCell(new Phrase("©JGGC Indústria e Comércio\nde Materiais Dentários S.A.", fontSmall));
                footerLeft.setBorder(Rectangle.NO_BORDER);
                footerTable.addCell(footerLeft);

                PdfPCell footerCenter = new PdfPCell(new Phrase("Template: " + config.getIdentifier() + (config.getRevision() != null ? ", revision " + config.getRevision() : ""), fontSmall));
                footerCenter.setBorder(Rectangle.NO_BORDER);
                footerCenter.setHorizontalAlignment(Element.ALIGN_CENTER);
                footerTable.addCell(footerCenter);

                PdfPCell footerRight = new PdfPCell(new Phrase("Page " + (pageNumber++) + "/" + totalPages, fontSmall));
                footerRight.setBorder(Rectangle.NO_BORDER);
                footerRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
                footerTable.addCell(footerRight);

                document.add(footerTable);

                firstPage = false;
            }

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateGeneralTaskReportPdf(List<Task> tasks) {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

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

        return new ByteArrayInputStream(out.toByteArray());
    }

    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderWidth(0.5f);
        cell.setPadding(2);
        return cell;
    }

    private PdfPCell createHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 230, 230)); // Cinza claro como na imagem
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderWidth(1f);
        return cell;
    }

    private PdfPCell createDataCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        cell.setBorderWidth(0.5f);
        return cell;
    }

    private PdfPCell createNoBorderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell.setPadding(2);
        return cell;
    }
}
