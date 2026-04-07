package com.xflprflx.neokioski.services;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
            .withZone(ZoneId.systemDefault());

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

            // Cabeçalho superior (Informações fixas)
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 1});

            headerTable.addCell(createCell("Loja/Unidade:", fontSmall));
            headerTable.addCell(createCell("FRM-003921", fontSmall));
            headerTable.addCell(createCell("Nome/Nome:", fontSmall));
            headerTable.addCell(createCell("2", fontSmall)); // Revisão? Na imagem parece ser um campo
            headerTable.addCell(createCell("Título/Title: Controle de Entrega Loja", fontSmall));
            headerTable.addCell(createCell("", fontSmall));
            headerTable.addCell(createCell("Responsável/Owner:", fontSmall));
            headerTable.addCell(createCell("", fontSmall));
            
            document.add(headerTable);

            // Espaçamento
            document.add(new Paragraph("\n"));

            // Título Principal
            Paragraph title = new Paragraph("PEDIDOS RETIRA LOJA", fontHeader);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("\n"));

            // Tabela de Dados
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 2, 4, 4});

            // Headers da Tabela
            table.addCell(createHeaderCell("SQ", fontTitle));
            table.addCell(createHeaderCell("Data", fontTitle));
            table.addCell(createHeaderCell("Número da Delivery", fontTitle));
            table.addCell(createHeaderCell("Entregue para:", fontTitle));

            // Dados
            int sq = 1;
            for (TaskService.StoreReportEntry entry : entries) {
                table.addCell(createDataCell(String.valueOf(sq++), fontNormal));
                table.addCell(createDataCell(dateFormatter.format(entry.collectedAt()), fontNormal));
                table.addCell(createDataCell(entry.deliveryNumber(), fontNormal));
                table.addCell(createDataCell(entry.collectorName(), fontNormal));
            }

            // Completar linhas vazias até 31 (conforme a imagem)
            for (int i = entries.size(); i < 31; i++) {
                table.addCell(createDataCell(String.valueOf(sq++), fontNormal));
                table.addCell(createDataCell("", fontNormal));
                table.addCell(createDataCell("", fontNormal));
                table.addCell(createDataCell("", fontNormal));
            }

            document.add(table);

            // Rodapé
            document.add(new Paragraph("\n"));
            
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            
            PdfPCell footerLeft = new PdfPCell(new Phrase("©JGGC Indústria e Comércio\nde Materiais Dentários S.A.", fontSmall));
            footerLeft.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            footerTable.addCell(footerLeft);

            PdfPCell footerRight = new PdfPCell(new Phrase("Template: FRM-003921, revision 2\nPage 1/1", fontSmall));
            footerRight.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            footerRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            footerTable.addCell(footerRight);

            document.add(footerTable);

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
}
