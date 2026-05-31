package com.example.streamingaichatbot.controller;


import com.example.streamingaichatbot.history.ConversationHistoryStore;
import com.example.streamingaichatbot.model.request.ChatRequest;
import com.example.streamingaichatbot.model.response.StreamChunk;
import com.example.streamingaichatbot.service.impl.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ConversationHistoryStore historyStore;

    /**
     * POST /api/chat/stream
     *
     * Frontend sends:
     *   { "message": "Hello", "sessionId": "uuid-here" }
     *
     * Returns SSE stream (text/event-stream).
     * Each event:  data: {"type":"token","content":"word","sessionId":"..."}
     * Final event: data: {"type":"done","sessionId":"..."}
     */
    @PostMapping(
            value = "/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<StreamChunk> stream(@Valid @RequestBody ChatRequest request) {
        log.info("[Controller] POST /chat/stream | session={}", request.getSessionId());
        return chatService.streamChat(request);
    }

    /**
     * DELETE /api/chat/{sessionId}
     * Clears conversation history — called when user clicks "New Chat".
     */
    @DeleteMapping("/chat/{sessionId}")
    public ResponseEntity<Map<String, String>> clearChat(@PathVariable String sessionId) {
        chatService.clearConversation(sessionId);
        return ResponseEntity.ok(Map.of("status", "cleared", "sessionId", sessionId));
    }

    /**
     * GET /api/health
     * Render uses this to verify the service is up.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "activeSessions", historyStore.getActiveSessions().size()
        ));
    }
}
