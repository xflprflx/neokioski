package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.TaskEvent;
import com.xflprflx.neokioski.models.TaskFieldValue;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.template.TaskFieldDefinition;
import com.xflprflx.neokioski.models.template.TaskStepDefinition;
import com.xflprflx.neokioski.models.template.TaskTemplate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExcelService Unit Tests")
class ExcelServiceTest {

    @InjectMocks
    private ExcelService excelService;

    @Test
    @DisplayName("Deve gerar relatório Excel com sucesso para uma lista de tarefas")
    void shouldGenerateGeneralTaskReportSuccessfully() throws IOException {
        // Given
        TaskTemplate template = new TaskTemplate();
        template.setName("Template Teste");

        Group group = new Group();
        group.setName("Grupo Teste");

        User creator = new User();
        creator.setName("Usuario Teste");

        TaskStepDefinition step = new TaskStepDefinition();
        step.setLabel("Passo Atual");

        TaskFieldDefinition fieldDef = new TaskFieldDefinition();
        fieldDef.setLabel("Campo Teste");

        TaskFieldValue fieldValue = new TaskFieldValue();
        fieldValue.setTaskFieldDefinition(fieldDef);
        fieldValue.setValue("Valor Teste");

        TaskEvent event = new TaskEvent();
        event.setTaskStepDefinition(step);
        event.setResponse("Resposta Teste");

        Task task = new Task();
        task.setId(1L);
        task.setCreatedAt(Instant.now());
        task.setTaskTemplate(template);
        task.setGroup(group);
        task.setCreator(creator);
        task.setCurrentStep(step);
        task.setTaskFieldValues(Set.of(fieldValue));
        task.setTaskEvents(Set.of(event));

        List<Task> tasks = List.of(task);

        // When
        ByteArrayInputStream result = excelService.generateGeneralTaskReport(tasks);

        // Then
        assertNotNull(result);
        try (Workbook workbook = new XSSFWorkbook(result)) {
            Sheet sheet = workbook.getSheet("Relatório de Tarefas");
            assertNotNull(sheet);
            assertEquals(2, sheet.getPhysicalNumberOfRows()); // Header + 1 task row

            Row dataRow = sheet.getRow(1);
            assertEquals(1.0, dataRow.getCell(0).getNumericCellValue());
            assertEquals("Template Teste", dataRow.getCell(2).getStringCellValue());
            assertEquals("Grupo Teste", dataRow.getCell(3).getStringCellValue());
            assertEquals("Usuario Teste", dataRow.getCell(4).getStringCellValue());
            assertEquals("Passo Atual", dataRow.getCell(5).getStringCellValue());
            assertTrue(dataRow.getCell(6).getStringCellValue().contains("Campo Teste: Valor Teste"));
            assertTrue(dataRow.getCell(7).getStringCellValue().contains("Passo Atual: Resposta Teste"));
        }
    }

    @Test
    @DisplayName("Deve lidar com campos nulos na tarefa ao gerar o Excel")
    void shouldHandleNullFieldsInTask() throws IOException {
        // Given
        Task task = new Task();
        task.setId(2L);
        task.setCreatedAt(Instant.now());
        task.setTaskTemplate(null);
        task.setGroup(null);
        task.setCreator(null);
        task.setCurrentStep(null);
        task.setTaskFieldValues(Collections.emptySet());
        task.setTaskEvents(Collections.emptySet());

        List<Task> tasks = List.of(task);

        // When
        ByteArrayInputStream result = excelService.generateGeneralTaskReport(tasks);

        // Then
        assertNotNull(result);
        try (Workbook workbook = new XSSFWorkbook(result)) {
            Sheet sheet = workbook.getSheet("Relatório de Tarefas");
            Row dataRow = sheet.getRow(1);
            assertEquals(2.0, dataRow.getCell(0).getNumericCellValue());
            assertEquals("", dataRow.getCell(2).getStringCellValue());
            assertEquals("", dataRow.getCell(3).getStringCellValue());
            assertEquals("Sistema", dataRow.getCell(4).getStringCellValue());
            assertEquals("Pendente", dataRow.getCell(5).getStringCellValue());
            assertEquals("", dataRow.getCell(6).getStringCellValue());
            assertEquals("", dataRow.getCell(7).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Deve lidar com definições nulas nos valores de campos e eventos")
    void shouldHandleNullDefinitionsInValuesAndEvents() throws IOException {
        // Given
        TaskFieldValue fieldValue = new TaskFieldValue();
        fieldValue.setTaskFieldDefinition(null);
        fieldValue.setValue("Valor Sem Def");

        TaskEvent event = new TaskEvent();
        event.setTaskStepDefinition(null);
        event.setResponse("Resposta Sem Def");

        Task task = new Task();
        task.setId(3L);
        task.setCreatedAt(Instant.now());
        task.setTaskFieldValues(Set.of(fieldValue));
        task.setTaskEvents(Set.of(event));

        List<Task> tasks = List.of(task);

        // When
        ByteArrayInputStream result = excelService.generateGeneralTaskReport(tasks);

        // Then
        assertNotNull(result);
        try (Workbook workbook = new XSSFWorkbook(result)) {
            Sheet sheet = workbook.getSheet("Relatório de Tarefas");
            Row dataRow = sheet.getRow(1);
            assertTrue(dataRow.getCell(6).getStringCellValue().contains("Campo: Valor Sem Def"));
            assertTrue(dataRow.getCell(7).getStringCellValue().contains("Passo: Resposta Sem Def"));
        }
    }
}
