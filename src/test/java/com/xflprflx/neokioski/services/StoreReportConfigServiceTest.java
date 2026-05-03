package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.StoreReportConfig;
import com.xflprflx.neokioski.repositories.StoreReportConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoreReportConfigService Unit Tests")
class StoreReportConfigServiceTest {

    @Mock
    private StoreReportConfigRepository repository;

    @InjectMocks
    private StoreReportConfigService service;

    @Test
    @DisplayName("Deve salvar configuração quando não houver sobreposição")
    void shouldSaveWhenNoOverlap() {
        // Given
        StoreReportConfig config = new StoreReportConfig();
        config.setStartDate(LocalDate.now());
        config.setEndDate(LocalDate.now().plusDays(5));

        when(repository.findAll()).thenReturn(List.of());

        // When
        service.save(config);

        // Then
        verify(repository).save(config);
    }

    @Test
    @DisplayName("Deve lançar exceção quando houver sobreposição de vigência")
    void shouldThrowExceptionOnOverlap() {
        // Given
        StoreReportConfig existing = new StoreReportConfig();
        existing.setId(1L);
        existing.setIdentifier("OLD");
        existing.setStartDate(LocalDate.now().minusDays(10));
        existing.setEndDate(LocalDate.now().plusDays(10));

        StoreReportConfig incoming = new StoreReportConfig();
        incoming.setStartDate(LocalDate.now());
        incoming.setEndDate(LocalDate.now().plusDays(5));

        when(repository.findAll()).thenReturn(List.of(existing));

        // When & Then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.save(incoming));
        assertTrue(ex.getMessage().contains("Conflito de vigência"));
    }

    @Test
    @DisplayName("Deve permitir edição da própria configuração sem conflito")
    void shouldAllowSelfEditing() {
        // Given
        StoreReportConfig existing = new StoreReportConfig();
        existing.setId(1L);
        existing.setStartDate(LocalDate.now());

        when(repository.findAll()).thenReturn(List.of(existing));

        // When
        service.save(existing);

        // Then
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("Deve deletar configuração com sucesso")
    void shouldDeleteSuccessfully() {
        StoreReportConfig config = new StoreReportConfig();
        service.delete(config);
        verify(repository).delete(config);
    }
}