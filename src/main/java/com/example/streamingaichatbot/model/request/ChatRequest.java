package com.example.streamingaichatbot.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    // The user's message — required, max 10,000 chars
    @NotBlank(message = "message cannot be blank")
    @Size(max = 10000, message = "message too long")
    private String message;

    // Ties this request to a conversation history slot.
    // Frontend generates a UUID once per session and sends it every time.
    @NotBlank(message = "sessionId cannot be blank")
    private String sessionId;
}

