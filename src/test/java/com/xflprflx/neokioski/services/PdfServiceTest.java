package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.ReportColumnConfig;
import com.xflprflx.neokioski.models.StoreReportConfig;
import com.xflprflx.neokioski.models.Task;
import com.xflprflx.neokioski.models.enums.ReportOrientation;
import com.xflprflx.neokioski.repositories.StoreReportConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PdfService Unit Tests")
class PdfServiceTest {

    @Mock
    private StoreReportConfigRepository configRepository;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private PdfService pdfService;

    @Test
    @DisplayName("Deve gerar relatório de loja PDF com sucesso")
    void shouldGenerateStoreReportPdfSuccessfully() throws IOException {
        // Given
        Instant now = Instant.now();
        LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
        
        Task task = new Task();
        task.setId(1L);

        TaskService.StoreReportEntry entry = new TaskService.StoreReportEntry(
                1L, "Template", now, now, "Criador", "Coletor", "DEL123", task
        );

        StoreReportConfig config = new StoreReportConfig();
        config.setOrientation(ReportOrientation.PORTRAIT);
        config.setIdentifier("ID-123");
        config.setRevision("1");
        config.setTitle("Relatorio Teste");
        config.setOwner("Dono");
        config.setTemplateCode("T001");
        config.setColumns(Collections.emptyList());

        when(configRepository.findActiveConfig(today)).thenReturn(Optional.of(config));

        // When
        ByteArrayInputStream result = pdfService.generateStoreReportPdf(List.of(entry));

        // Then
        assertNotNull(result);
        assertTrue(result.available() > 0);
    }

    @Test
    @DisplayName("Deve gerar relatório de loja PDF com orientação paisagem e colunas customizadas")
    void shouldGenerateStoreReportPdfLandscapeWithColumns() {
        // Given
        Instant now = Instant.now();
        LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
        
        Task task = new Task();
        task.setId(1L);

        TaskService.StoreReportEntry entry = new TaskService.StoreReportEntry(
                1L, "Template", now, now, "Criador", "Coletor", "DEL123", task
        );

        StoreReportConfig config = new StoreReportConfig();
        config.setOrientation(ReportOrientation.LANDSCAPE);
        config.setIdentifier("ID-LAND");
        
        ReportColumnConfig col = new ReportColumnConfig();
        col.setLabel("Coluna Teste");
        col.setAttributeKey("FIELD:Teste");
        config.setColumns(List.of(col));

        when(configRepository.findActiveConfig(today)).thenReturn(Optional.of(config));
        when(taskService.getTaskAttributeValue(any(), any())).thenReturn("Valor Custom");

        // When
        ByteArrayInputStream result = pdfService.generateStoreReportPdf(List.of(entry));

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Deve lançar exceção quando não houver configuração ativa para a data")
    void shouldThrowExceptionWhenNoConfigFound() {
        // Given
        Instant now = Instant.now();
        TaskService.StoreReportEntry entry = new TaskService.StoreReportEntry(
                1L, "Template", now, now, "Criador", "Coletor", "DEL123", new Task()
        );

        when(configRepository.findActiveConfig(any())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> pdfService.generateStoreReportPdf(List.of(entry)));
    }

    @Test
    @DisplayName("Deve gerar relatório geral de tarefas PDF com sucesso")
    void shouldGenerateGeneralTaskReportPdfSuccessfully() {
        // Given
        Task task = new Task();
        task.setId(1L);
        task.setCreatedAt(Instant.now());

        // When
        ByteArrayInputStream result = pdfService.generateGeneralTaskReportPdf(List.of(task));

        // Then
        assertNotNull(result);
    }
}
