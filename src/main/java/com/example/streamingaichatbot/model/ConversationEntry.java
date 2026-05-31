package com.example.streamingaichatbot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEntry {

    public enum Role { USER, ASSISTANT, SYSTEM }

    private Role role;
    private String content;
    private Instant timestamp;

    public static ConversationEntry user(String content) {
        return ConversationEntry.builder()
                .role(Role.USER).content(content)
                .timestamp(Instant.now()).build();
    }

    public static ConversationEntry assistant(String content) {
        return ConversationEntry.builder()
                .role(Role.ASSISTANT).content(content)
                .timestamp(Instant.now()).build();
    }

    public static ConversationEntry system(String content) {
        return ConversationEntry.builder()
                .role(Role.SYSTEM).content(content)
                .timestamp(Instant.now()).build();
    }
}
