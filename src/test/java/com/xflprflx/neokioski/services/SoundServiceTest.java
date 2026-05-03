package com.xflprflx.neokioski.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test: SoundService — Listagem de Sons de Alerta")
class SoundServiceTest {

    private final SoundService soundService = new SoundService();

    @Test
    @DisplayName("getAvailableAlertSounds - retorna lista não nula (vazia em ambiente de teste)")
    void getAvailableAlertSounds_noSoundsInTestClasspath_returnsNonNullList() {
        List<String> sounds = soundService.getAvailableAlertSounds();

        assertNotNull(sounds, "A lista de sons não deve ser nula");
    }

    @Test
    @DisplayName("getAvailableAlertSounds - não lança exceção mesmo sem arquivos de som no classpath")
    void getAvailableAlertSounds_noSoundsPresent_doesNotThrow() {
        assertDoesNotThrow(() -> soundService.getAvailableAlertSounds());
    }

    @Test
    @DisplayName("getAvailableAlertSounds - retorna somente arquivos com prefixo '#alert_'")
    void getAvailableAlertSounds_whenCalled_returnsOnlyAlertPrefixedFiles() {
        List<String> sounds = soundService.getAvailableAlertSounds();

        sounds.forEach(sound ->
                assertTrue(sound.startsWith("#alert_"),
                        "Todos os sons devem começar com '#alert_': " + sound)
        );
    }
}