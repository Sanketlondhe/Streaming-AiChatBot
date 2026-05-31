package com.example.streamingaichatbot.service.impl;


import com.example.streamingaichatbot.model.request.ChatRequest;
import com.example.streamingaichatbot.model.response.StreamChunk;
import reactor.core.publisher.Flux;

public interface ChatService {

    /** Word-by-word SSE stream. */
    Flux<StreamChunk> streamChat(ChatRequest request);

    /** Clears conversation history for a session. */
    void clearConversation(String sessionId);
}
