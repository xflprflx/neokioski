package com.xflprflx.neokioski.services;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.xflprflx.neokioski.models.StoreReportConfig;
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
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    public PdfService(StoreReportConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public ByteArrayInputStream generateStoreReportPdf(List<TaskService.StoreReportEntry> entries) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fontes
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font fontSmall = FontFactory.getFont(FontFactory.HELVETICA, 7);

            // 1. Agrupar por data de coleta (LocalDate) usando TreeMap para manter a ordem cronológica
            Map<LocalDate, List<TaskService.StoreReportEntry>> entriesByDate = entries.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.collectedAt().atZone(ZoneId.systemDefault()).toLocalDate(),
                            TreeMap::new,
                            Collectors.toList()
                    ));

            boolean firstPage = true;
            int pageNumber = 1;
            int totalPages = entriesByDate.size();

            for (Map.Entry<LocalDate, List<TaskService.StoreReportEntry>> group : entriesByDate.entrySet()) {
                LocalDate date = group.getKey();
                List<TaskService.StoreReportEntry> dateEntries = group.getValue();

                if (!firstPage) {
                    document.newPage();
                }

                // Buscar configuração ativa para ESTA data específica
                StoreReportConfig config = configRepository.findActiveConfig(date)
                        .orElseGet(() -> {
                            StoreReportConfig fallback = new StoreReportConfig();
                            fallback.setIdentifier("FRM-003921");
                            fallback.setRevision("2");
                            fallback.setTitle("Controle de Entrega Loja");
                            fallback.setOwner("Juliano Souza (U112006)");
                            fallback.setTemplateCode("FRM-000922");
                            return fallback;
                        });

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

                // --- TÍTULO ---
                Paragraph title = new Paragraph("PEDIDOS RETIRA LOJA", fontHeader);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);
                document.add(new Paragraph("\n"));

                // --- TABELA DE DADOS ---
                PdfPTable table = new PdfPTable(4);
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

                // Completar até 31 linhas para cada página/data conforme solicitado
                for (int i = dateEntries.size(); i < 31; i++) {
                    table.addCell(createDataCell(String.valueOf(sq++), fontNormal));
                    table.addCell(createDataCell("", fontNormal));
                    table.addCell(createDataCell("", fontNormal));
                    table.addCell(createDataCell("", fontNormal));
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
