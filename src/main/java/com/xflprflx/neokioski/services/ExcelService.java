package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.TaskFieldValue;
import com.xflprflx.neokioski.models.TaskEvent;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
/**
 * Serviço de utilidade para a geração dinâmica de relatórios em formato Excel (XLSX) utilizando Apache POI.
 */
public class ExcelService {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public ByteArrayInputStream generateGeneralTaskReport(List<Task> tasks) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Relatório de Tarefas");

            // Cabecalho
            Row headerRow = sheet.createRow(0);
            String[] columns = {"ID", "Data Criação", "Tipo (Template)", "Grupo", "Criador", "Status Atual", "Campos Iniciais", "Histórico de Respostas"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (Task task : tasks) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(task.getId());
                row.createCell(1).setCellValue(formatter.format(task.getCreatedAt()));
                row.createCell(2).setCellValue(task.getTaskTemplate() != null ? task.getTaskTemplate().getName() : "");
                row.createCell(3).setCellValue(task.getGroup() != null ? task.getGroup().getName() : "");
                row.createCell(4).setCellValue(task.getCreator() != null ? task.getCreator().getName() : "Sistema");
                row.createCell(5).setCellValue(task.getCurrentStep() != null ? task.getCurrentStep().getLabel() : "Pendente");

                // Campos
                String fields = task.getTaskFieldValues().stream()
                        .map(fv -> (fv.getTaskFieldDefinition() != null ? fv.getTaskFieldDefinition().getLabel() : "Campo") + ": " + fv.getValue())
                        .collect(Collectors.joining(" | "));
                row.createCell(6).setCellValue(fields);

                // Respostas
                String responses = task.getTaskEvents().stream()
                        .filter(ev -> ev.getResponse() != null && !ev.getResponse().isBlank())
                        .map(ev -> (ev.getTaskStepDefinition() != null ? ev.getTaskStepDefinition().getLabel() : "Passo") + ": " + ev.getResponse())
                        .collect(Collectors.joining(" | "));
                row.createCell(7).setCellValue(responses);
            }

            /*
             * Inicia um laço de repetição (for) para iterar sobre os elementos.
             */
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            /*
             * Retorna o resultado final processado pelo método.
             */
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar Excel: " + e.getMessage());
        }
    }
}
