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
public class PresenceService {

    public record PresenceEvent(Long userId, boolean online, LocalDateTime lastSeen) {}

    private final Map<Long, Integer> activeConnections = new ConcurrentHashMap<>();
    private final List<Consumer<PresenceEvent>> listeners = new CopyOnWriteArrayList<>();
    private final UserService userService;

    public PresenceService(UserService userService) {
        this.userService = userService;
    }

    public void markOnline(Long userId) {
        if (userId == null) return;
        
        activeConnections.merge(userId, 1, Integer::sum);
        broadcast(new PresenceEvent(userId, true, LocalDateTime.now()));
    }

    public void markOffline(Long userId) {
        if (userId == null) return;

        activeConnections.computeIfPresent(userId, (id, count) -> {
            int newCount = count - 1;
            if (newCount <= 0) {
                notifyOffline(userId);
                return null;
            }
            return newCount;
        });
    }

    public void forceOffline(Long userId) {
        if (userId == null) return;
        activeConnections.remove(userId);
        notifyOffline(userId);
    }

    private void notifyOffline(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        userService.updateLastSeen(userId, now);
        broadcast(new PresenceEvent(userId, false, now));
    }

    public boolean isOnline(Long userId) {
        if (userId == null) return false;
        return activeConnections.containsKey(userId);
    }

    public Set<Long> getOnlineUserIds() {
        return Collections.unmodifiableSet(activeConnections.keySet());
    }

    public void addListener(Consumer<PresenceEvent> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<PresenceEvent> listener) {
        listeners.remove(listener);
    }

    private void broadcast(PresenceEvent event) {
        for (Consumer<PresenceEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                // Log or ignore
            }
        }
    }
}
