package com.example.streamingaichatbot.history;


import com.example.streamingaichatbot.model.ConversationEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores conversation history per session in memory.
 *
 * Data structure:
 *   ConcurrentHashMap<sessionId, LinkedList<ConversationEntry>>
 *
 * - Thread-safe (ConcurrentHashMap + compute())
 * - Bounded: max 50 turns per session (configurable)
 * - TTL eviction: sessions idle > 60 min are purged every 15 min
 */
@Slf4j
@Component
public class ConversationHistoryStore {

    private final Map<String, LinkedList<ConversationEntry>> store
            = new ConcurrentHashMap<>();

    private final Map<String, Instant> lastAccessed
            = new ConcurrentHashMap<>();

    @Value("${ai.conversation.max-history-size:50}")
    private int maxHistorySize;

    @Value("${ai.conversation.ttl-minutes:60}")
    private int ttlMinutes;

    /** Returns all turns for a session. Empty list if session is new. */
    public List<ConversationEntry> getHistory(String sessionId) {
        lastAccessed.put(sessionId, Instant.now());
        LinkedList<ConversationEntry> history =
                store.getOrDefault(sessionId, new LinkedList<>());
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    /** Adds one turn to the session. Trims oldest if over the limit. */
    public void addEntry(String sessionId, ConversationEntry entry) {
        store.compute(sessionId, (id, existing) -> {
            if (existing == null) existing = new LinkedList<>();
            existing.addLast(entry);
            while (existing.size() > maxHistorySize) existing.removeFirst();
            return existing;
        });
        lastAccessed.put(sessionId, Instant.now());
        log.debug("[History] session={} | role={} | totalTurns={}",
                sessionId, entry.getRole(),
                store.get(sessionId).size());
    }

    /** Clears history for a session (called by DELETE /api/chat/{id}). */
    public void clearSession(String sessionId) {
        store.remove(sessionId);
        lastAccessed.remove(sessionId);
        log.info("[History] Cleared session={}", sessionId);
    }

    public int getSessionSize(String sessionId) {
        return store.getOrDefault(sessionId, new LinkedList<>()).size();
    }

    public Set<String> getActiveSessions() {
        return Collections.unmodifiableSet(store.keySet());
    }

    /** Runs every 15 minutes. Removes sessions idle longer than ttlMinutes. */
    @Scheduled(fixedRate = 900_000)
    public void evictExpiredSessions() {
        Instant cutoff = Instant.now().minus(ttlMinutes, ChronoUnit.MINUTES);
        int evicted = 0;
        for (Map.Entry<String, Instant> e : lastAccessed.entrySet()) {
            if (e.getValue().isBefore(cutoff)) {
                store.remove(e.getKey());
                lastAccessed.remove(e.getKey());
                evicted++;
            }
        }
        if (evicted > 0) {
            log.info("[History] Evicted {} idle sessions", evicted);
        }
    }
}

