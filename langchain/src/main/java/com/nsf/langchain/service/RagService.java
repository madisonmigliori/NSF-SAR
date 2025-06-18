package com.nsf.langchain.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    public RagService(VectorStore vectorStore, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }

    public String answer(String question) {
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(question)
                .topK(5)
                .similarityThreshold(0.5)
                .build()
        );

        if (docs == null || docs.isEmpty()) {
            return "No relevant context found.";
        }

        String context = docs.stream()
                             .map(Document::getText)
                             .collect(Collectors.joining("\n---\n"));

        List<Message> messages = List.of(
            new SystemMessage("""
                You are a senior software architect assistant. 
                Help the user design robust, scalable, and secure microservice-based architectures. 
                Always explain your reasoning and suggest patterns like service discovery, API gateways, event-driven communication, etc.
            """),
            new UserMessage("Here is the context:\n" + context + "\n\nNow, based on that, answer this question:\n" + question)
        );

        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().toString();
    }
}

