package com.nsf.langchain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final ChatModel chatModel;
    private final IngestionService ingestionService;
    private final ChromaApi chromaApi;
    private final EmbeddingModel embeddingModel;
    private final ChromaVectorStoreProperties vectorStoreProperties;

    public RagService(ChatModel chatModel,
                      IngestionService ingestionService,
                      ChromaApi chromaApi,
                      EmbeddingModel embeddingModel,
                      ChromaVectorStoreProperties vectorStoreProperties) {
        this.chatModel = chatModel;
        this.ingestionService = ingestionService;
        this.chromaApi = chromaApi;
        this.embeddingModel = embeddingModel;
        this.vectorStoreProperties = vectorStoreProperties;
    }

    public String answer(String question, String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            return "❌ Missing Git repo URL. Please provide one.";
        }

        String repoId = extractRepoId(repoUrl);
        String collectionName = repoId;
        String jsonPath = "/Users/madisonmigliori/Documents/NSF/NSF-SAR/rag/doc";

        String processedQuestion = normalizePossessiveLanguage(question, repoId);

        try {
            log.info("🚀 Starting ingestion for repo '{}'", repoId);
            ingestionService.ingestRepo(repoUrl);
            ingestionService.ingestJson(jsonPath, repoId);
        } catch (Exception e) {
            log.error("❌ Ingestion failed for '{}'", repoId, e);
            return "Ingestion failed. Check server logs for details.";
        }

        ChromaVectorStore repoVectorStore = ChromaVectorStore.builder(chromaApi, embeddingModel)
                .tenantName(vectorStoreProperties.getTenantName())
                .databaseName(vectorStoreProperties.getDatabaseName())
                .collectionName(collectionName)
                .initializeSchema(true)
                .initializeImmediately(true)
                .build();

        List<Document> docs = new ArrayList<>();
        try {
            log.info("🔍 Running vector search on collection '{}'", collectionName);
            docs = repoVectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(processedQuestion)
                            .topK(20)
                            .similarityThreshold(0.1)
                            .build()
            );
        } catch (Exception e) {
            log.error("❌ Vector search failed for question '{}'", question, e);
            return "Vector search failed. Please try again.";
        }

        if (docs == null || docs.isEmpty()) {
            return "🤷 No relevant context found for this question.";
        }

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        if (containsGreeting(question)) {
            return "👋 Hey! I’m your architecture assistant. Ask me about microservice patterns, design smells, or improvements in your repo!";
        }

        List<Message> messages = Arrays.asList(
                new SystemMessage("""
                        You are a friendly, precise, and expert Software Architecture Assistant.

                        Your goals:
                        - Analyze the provided repository and JSON scoring context.
                        - Identify microservice patterns, smells, and architecture concerns.
                        - Suggest improvements, refactorings, and pattern applications.
                        - ONLY use the given context. DO NOT hallucinate or reference external URLs.
                        - If you don’t have enough context, say: "I don’t have enough information to answer that."
                        """),
                new UserMessage("Here is the context:\n" + context + "\n\nNow answer this:\n" + processedQuestion)
        );

        ChatResponse response = chatModel.call(new Prompt(messages));
        String llmAnswer = response.getResult().getOutput().getText();

        // ✅ Optional: Store the LLM answer as its own document for future conversation grounding
        try {
            ChromaVectorStore chatHistoryStore = ChromaVectorStore.builder(chromaApi, embeddingModel)
                    .tenantName(vectorStoreProperties.getTenantName())
                    .databaseName(vectorStoreProperties.getDatabaseName())
                    .collectionName("conversations_" + repoId)
                    .initializeSchema(true)
                    .initializeImmediately(true)
                    .build();

            Document answerDoc = Document.builder()
                    .text(llmAnswer)
                    .metadata("repo", repoId)
                    .metadata("source", "LLMAnswer")
                    .metadata("user_question", question)
                    .build();

            chatHistoryStore.add(List.of(answerDoc));
            log.info("✅ Stored LLM answer for repo '{}'", repoId);

        } catch (Exception e) {
            log.warn("⚠️ Failed to store LLM answer for '{}': {}", repoId, e.getMessage());
        }

        return llmAnswer;
    }

    private String extractRepoId(String gitUrl) {
        String[] parts = gitUrl.replace(".git", "").split("/");
        return parts[parts.length - 1];
    }

    private boolean containsGreeting(String text) {
        String lower = text.toLowerCase();
        return lower.contains("hello") || lower.contains("hi") || lower.contains("hey");
    }

    private String normalizePossessiveLanguage(String question, String repoId) {
        String[] phrases = { "this repo", "mine", "my repo", "this project", "this application" };
        String normalized = question;
        for (String phrase : phrases) {
            normalized = normalized.replaceAll("(?i)\\b" + phrase + "\\b", "the repository " + repoId);
        }
        return normalized;
    }
}
