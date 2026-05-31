package com.example.streamingaichatbot.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One SSE event sent to the frontend.
 *
 * type = "token"  → chunk.content has the next word/token
 * type = "done"   → stream finished, no more chunks coming
 * type = "error"  → something went wrong, chunk.error has the message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamChunk {

    private String type;      // "token" | "done" | "error"
    private String content;   // the actual text (only for type=token)
    private String sessionId;
    private String error;     // only for type=error

    public static StreamChunk token(String content, String sessionId) {
        return StreamChunk.builder()
                .type("token")
                .content(content)
                .sessionId(sessionId)
                .build();
    }

    public static StreamChunk done(String sessionId) {
        return StreamChunk.builder()
                .type("done")
                .sessionId(sessionId)
                .build();
    }

    public static StreamChunk error(String errorMsg, String sessionId) {
        return StreamChunk.builder()
                .type("error")
                .error(errorMsg)
                .sessionId(sessionId)
                .build();
    }
}
