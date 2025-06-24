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
import java.util.Arrays; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    public RagService(VectorStore vectorStore, ChatModel chatModel) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
    }


    public String answer(String question, String repoId) {
        List<Document> docs;

        if (repoId == null || repoId.isBlank()) {
            throw new IllegalArgumentException("Missing repoId for context search.");
        }        
        try {
            log.info("Searching vector store for repoId:", repoId);
            docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(question)
                    .topK(10)
                    .similarityThreshold(0.3)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error during similarity search", e);
            return "An error occurred while searching for relevant context. Please try again later.";
        }

        if (docs == null || docs.isEmpty()) {
            return "No relevant context found.";
        }

        
        String context = docs.stream()
                             .map(Document::getText)
                             .collect(Collectors.joining("\n---\n"));

        List<Message> messages = Arrays.asList(
            new SystemMessage("""
                You are a friendly assistant. 
                You are a senior software architect assistant. 

                You must identify the architecture patterns, with a focus on microservice architecture. Display the architecture and the bounded context diagram.

                Help the user design robust, scalable, and secure microservice-based architectures. 

                Identify microservice smells or any bad coding practices in the code. 

                Always explain your reasoning and suggest patterns like service discovery, API gateways, event-driven communication, etc.

                Identify section of the code where it needs to be improved or refactored. Provide a generated image of the architecture. Only answer questions using the provided context. If the context is not enough, say "I don’t have enough information to answer that." Do not hallucinate.
            """),
            new UserMessage("Here is the context:\n" + context + "\n\nNow, based on that, answer this question:\n" + question)
        );

        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }
}
