package com.nsf.langchain.client;


import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

public class PromptFactory {

    public static Prompt basic(String systemInstructions, String userContext) {
        return new Prompt(List.of(
            new SystemMessage(systemInstructions),
            new UserMessage(userContext)
        ));
    }

    public static Prompt boundaryPrompt(String formattedCode) {
        return basic(
            """
            You are a software architecture expert.
            Based on the following code snippets and identified layers,
            identify logical service boundaries and their responsibilities.
            Return ONLY a JSON object without any explanation or formatting.
            If you cannot, return an empty JSON object {}.
            """,
            "Code context:\n" + formattedCode
        );
    }

    public static Prompt testPrompt(String tag, String content) {
        return basic("TEST PROMPT: " + tag, content);
    }
}