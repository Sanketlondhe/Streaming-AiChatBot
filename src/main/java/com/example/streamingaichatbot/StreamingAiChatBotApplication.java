package com.example.streamingaichatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling needed for the TTL cleanup job
// in ConversationHistoryStore (runs every 15 minutes)
@SpringBootApplication
@EnableScheduling
public class StreamingAiChatBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingAiChatBotApplication.class, args);
    }

}
