package com.nsf.langchain.client;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.stream.Collectors;

public class LLMLogger {
    private static final Logger log = LoggerFactory.getLogger(LLMLogger.class);

    public static void logPrompt(String label, Prompt prompt) {
        String promptText = prompt.getInstructions().stream()
            .map(Message::getText)
            .collect(Collectors.joining("\n---\n"));
        log.info("\n[{}] Prompt:\n{}", label, promptText);
    }

    public static void logResponse(String label, ChatResponse response) {
        log.info("\n[{}] Response:\n{}", label, response.getResult().getOutput().getText());
    }

    public static void logTimed(String label, Runnable action) {
        long start = System.currentTimeMillis();
        action.run();
        long elapsed = System.currentTimeMillis() - start;
        log.info("[{}] completed in {}ms", label, elapsed);
    }
}
