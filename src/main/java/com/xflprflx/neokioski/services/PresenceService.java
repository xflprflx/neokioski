package com.xflprflx.neokioski.services;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Service
/**
 * Serviço que gerencia o status de conexão (online/offline) dos usuários na plataforma em tempo real.
 */
public class PresenceService {

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public record PresenceEvent(Long userId, boolean online, LocalDateTime lastSeen) {}

    private final Map<Long, Integer> activeConnections = new ConcurrentHashMap<>();
    private final List<Consumer<PresenceEvent>> listeners = new CopyOnWriteArrayList<>();
    private final UserService userService;

    public PresenceService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void markOnline(Long userId) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (userId == null) return;
        
        activeConnections.merge(userId, 1, Integer::sum);
        broadcast(new PresenceEvent(userId, true, LocalDateTime.now()));
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void markOffline(Long userId) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (userId == null) return;

        activeConnections.computeIfPresent(userId, (id, count) -> {
            int newCount = count - 1;
            /*
             * Avalia uma condição (if) para desvio de fluxo da lógica principal.
             */
            if (newCount <= 0) {
                notifyOffline(userId);
                /*
                 * Retorna o resultado final processado pelo método.
                 */
                return null;
            }
            /*
             * Retorna o resultado final processado pelo método.
             */
            return newCount;
        });
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void forceOffline(Long userId) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (userId == null) return;
        activeConnections.remove(userId);
        notifyOffline(userId);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private void notifyOffline(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        userService.updateLastSeen(userId, now);
        broadcast(new PresenceEvent(userId, false, now));
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public boolean isOnline(Long userId) {
        /*
         * Avalia uma condição (if) para desvio de fluxo da lógica principal.
         */
        if (userId == null) return false;
        /*
         * Retorna o resultado final processado pelo método.
         */
        return activeConnections.containsKey(userId);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public Set<Long> getOnlineUserIds() {
        /*
         * Retorna o resultado final processado pelo método.
         */
        return Collections.unmodifiableSet(activeConnections.keySet());
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void addListener(Consumer<PresenceEvent> listener) {
        listeners.add(listener);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    public void removeListener(Consumer<PresenceEvent> listener) {
        listeners.remove(listener);
    }

    /**
     * Método ou construtor que encapsula parte da lógica de negócio e regras do serviço.
     */
    private void broadcast(PresenceEvent event) {
        /*
         * Inicia um laço de repetição (for) para iterar sobre os elementos.
         */
        for (Consumer<PresenceEvent> listener : listeners) {
            /*
             * Inicia um bloco try-catch para tratar possíveis exceções.
             */
            try {
                listener.accept(event);
            } catch (Exception e) {
                // Log or ignore
            }
        }
    }
}
