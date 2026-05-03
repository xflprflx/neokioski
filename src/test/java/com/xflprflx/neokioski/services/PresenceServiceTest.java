package com.xflprflx.neokioski.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: PresenceService — Gerenciamento de Status Online/Offline")
class PresenceServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private PresenceService presenceService;

    @Test
    @DisplayName("markOnline - userId nulo não altera estado nem chama dependências")
    void markOnline_nullId_doesNothing() {
        presenceService.markOnline(null);

        assertFalse(presenceService.isOnline(null));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("markOnline - userId válido marca usuário como online")
    void markOnline_validId_userBecomesOnline() {
        presenceService.markOnline(1L);

        assertTrue(presenceService.isOnline(1L));
    }

    @Test
    @DisplayName("markOnline múltiplas vezes - contagem de conexões somada, usuário permanece online")
    void markOnline_multipleTimes_connectionCountIsAccumulated() {
        presenceService.markOnline(1L);
        presenceService.markOnline(1L);

        assertTrue(presenceService.isOnline(1L));

        presenceService.markOffline(1L);
        assertTrue(presenceService.isOnline(1L));
    }

    @Test
    @DisplayName("markOffline - userId nulo não faz nada")
    void markOffline_nullId_doesNothing() {
        presenceService.markOffline(null);

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("markOffline - última conexão remove usuário e persiste lastSeen")
    void markOffline_lastConnection_marksUserOfflineAndUpdatesLastSeen() {
        presenceService.markOnline(1L);

        presenceService.markOffline(1L);

        assertFalse(presenceService.isOnline(1L));
        verify(userService).updateLastSeen(eq(1L), any());
    }

    @Test
    @DisplayName("markOffline - decremento não chega a zero mantém usuário online")
    void markOffline_connectionCountRemainsAboveZero_userStaysOnline() {
        presenceService.markOnline(1L);
        presenceService.markOnline(1L);

        presenceService.markOffline(1L);

        assertTrue(presenceService.isOnline(1L));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("markOffline - usuário não estava online não dispara notificação")
    void markOffline_userNotOnline_doesNotNotify() {
        presenceService.markOffline(99L);

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("forceOffline - userId nulo não faz nada")
    void forceOffline_nullId_doesNothing() {
        presenceService.forceOffline(null);

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("forceOffline - remove usuário independentemente da contagem de conexões")
    void forceOffline_multipleConnections_forcesOfflineAndUpdatesLastSeen() {
        presenceService.markOnline(1L);
        presenceService.markOnline(1L);

        presenceService.forceOffline(1L);

        assertFalse(presenceService.isOnline(1L));
        verify(userService).updateLastSeen(eq(1L), any());
    }

    @Test
    @DisplayName("isOnline - userId nulo retorna false")
    void isOnline_nullId_returnsFalse() {
        assertFalse(presenceService.isOnline(null));
    }

    @Test
    @DisplayName("isOnline - usuário sem registro retorna false")
    void isOnline_userNotRegistered_returnsFalse() {
        assertFalse(presenceService.isOnline(42L));
    }

    @Test
    @DisplayName("getOnlineUserIds - retorna cópia imutável dos IDs online")
    void getOnlineUserIds_returnsIdsOfAllOnlineUsers() {
        presenceService.markOnline(1L);
        presenceService.markOnline(2L);

        Set<Long> onlineIds = presenceService.getOnlineUserIds();

        assertTrue(onlineIds.contains(1L));
        assertTrue(onlineIds.contains(2L));
        assertEquals(2, onlineIds.size());
    }

    @Test
    @DisplayName("addListener - ouvinte registrado recebe eventos de presença")
    void addListener_registeredListener_receivesEvents() {
        List<PresenceService.PresenceEvent> received = new ArrayList<>();
        Consumer<PresenceService.PresenceEvent> listener = received::add;

        presenceService.addListener(listener);
        presenceService.markOnline(1L);

        assertEquals(1, received.size());
        assertEquals(1L, received.get(0).userId());
        assertTrue(received.get(0).online());
    }

    @Test
    @DisplayName("removeListener - ouvinte removido não recebe mais eventos")
    void removeListener_removedListener_stopsReceivingEvents() {
        AtomicInteger callCount = new AtomicInteger(0);
        Consumer<PresenceService.PresenceEvent> listener = e -> callCount.incrementAndGet();

        presenceService.addListener(listener);
        presenceService.markOnline(1L);
        assertEquals(1, callCount.get());

        presenceService.removeListener(listener);
        presenceService.markOnline(2L);
        assertEquals(1, callCount.get());
    }

    @Test
    @DisplayName("broadcast - exceção em ouvinte não propaga para o chamador")
    void broadcast_listenerThrowsException_doesNotPropagateError() {
        presenceService.addListener(event -> {
            throw new RuntimeException("Erro simulado em ouvinte");
        });

        assertDoesNotThrow(() -> presenceService.markOnline(1L));
    }

    @Test
    @DisplayName("markOffline disparando notifyOffline - evento offline é enviado aos ouvintes")
    void markOffline_triggersOfflineEvent_notifiesListeners() {
        List<PresenceService.PresenceEvent> received = new ArrayList<>();
        presenceService.addListener(received::add);

        presenceService.markOnline(1L);
        received.clear();

        presenceService.markOffline(1L);

        assertEquals(1, received.size());
        assertFalse(received.get(0).online());
    }
}