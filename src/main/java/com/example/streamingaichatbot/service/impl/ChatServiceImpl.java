package com.example.streamingaichatbot.service.impl;


import com.example.streamingaichatbot.history.ConversationHistoryStore;
import com.example.streamingaichatbot.model.ConversationEntry;
import com.example.streamingaichatbot.model.request.ChatRequest;
import com.example.streamingaichatbot.model.response.StreamChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    // Spring AI autoconfigures this from application.properties.
    // Using the ChatModel interface (not OpenAiChatModel directly)
    // keeps the service provider-agnostic — later when you add Gemini
    // you won't need to touch this file at all.
    private final ChatModel chatModel;
    private final ConversationHistoryStore historyStore;
 
    private static final String SYSTEM_PROMPT =
            "You are a helpful AI assistant for FlowGen AI, a business automation company. " +
            "Answer clearly and concisely. Be friendly and professional.";
 
    @Override
    public Flux<StreamChunk> streamChat(ChatRequest request) {
        String sessionId = request.getSessionId();
 
        // 1. Load existing conversation history
        List<ConversationEntry> history = historyStore.getHistory(sessionId);
        log.info("[ChatService] stream | session={} | historySize={}", sessionId, history.size());
 
        // 2. Build prompt with full history for context
        Prompt prompt = buildPrompt(request.getMessage(), history);
 
        // 3. Accumulate tokens so we can save the full response to history
        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());
 
        // Save user message to history IMMEDIATELY before streaming starts.
        // This way it's already in history even if stream is interrupted.
        historyStore.addEntry(sessionId, ConversationEntry.user(request.getMessage()));
 
        return chatModel.stream(prompt)
                .flatMap(response -> {
                    String token = "";
                    try {
                        if (response.getResult() != null
                                && response.getResult().getOutput() != null) {
                            String t = response.getResult().getOutput().getText();
                            if (t != null) token = t;
                        }
                    } catch (Exception e) {
                        log.debug("[ChatService] token extract issue: {}", e.getMessage());
                    }
                    if (!token.isEmpty()) {
                        buffer.get().append(token);
                        return Flux.just(StreamChunk.token(token, sessionId));
                    }
                    return Flux.empty();
                })
                .concatWith(Flux.just(StreamChunk.done(sessionId)))
                .doFinally(signalType -> {
                    // doFinally fires on complete AND cancel AND error —
                    // much more reliable than doOnComplete for SSE streams
                    String fullResponse = buffer.get().toString();
                    if (!fullResponse.isBlank()) {
                        historyStore.addEntry(sessionId, ConversationEntry.assistant(fullResponse));
                        log.debug("[ChatService] History saved | session={} | signal={} | chars={}",
                                sessionId, signalType, fullResponse.length());
                    }
                })
                .onErrorResume(ex -> {
                    log.error("[ChatService] Stream error | session={} | {}", sessionId, ex.getMessage());
                    return Flux.just(StreamChunk.error(ex.getMessage(), sessionId));
                });
    }
 
    @Override
    public void clearConversation(String sessionId) {
        historyStore.clearSession(sessionId);
    }
 
    /**
     * Builds a Spring AI Prompt from conversation history + current message.
     * All Message types (SystemMessage, UserMessage, AssistantMessage)
     * are from spring-ai-core — provider-agnostic.
     */
    private Prompt buildPrompt(String userMessage, List<ConversationEntry> history) {
        List<Message> messages = new ArrayList<>();
 
        // System instruction always first
        messages.add(new SystemMessage(SYSTEM_PROMPT));
 
        // Past conversation turns
        for (ConversationEntry entry : history) {
            switch (entry.getRole()) {
                case USER      -> messages.add(new UserMessage(entry.getContent()));
                case ASSISTANT -> messages.add(new AssistantMessage(entry.getContent()));
                case SYSTEM    -> messages.add(new SystemMessage(entry.getContent()));
            }
        }
 
        // Current user message
        messages.add(new UserMessage(userMessage));
 
        return new Prompt(messages);
        // Options (model, temperature, maxTokens) come from
        // application.properties → Spring AI autoconfigures them
        // into the ChatModel bean automatically.
    }
}
