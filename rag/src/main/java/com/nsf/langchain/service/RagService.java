package com.nsf.langchain.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.model.Repo;
import com.nsf.langchain.model.Report;
import com.nsf.langchain.utils.ArchitectureUtils;
import com.nsf.langchain.utils.GitUtils;
import com.nsf.langchain.utils.RepoUtils;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.stream.Collectors;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RagService {

    private final GitUtils gitUtils;

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final IngestionService ingestionService;
    private final ArchitectureUtils architecture;
    

    public RagService(VectorStore vectorStore, ChatModel chatModel, IngestionService ingestionService, ArchitectureUtils architecture, GitUtils gitUtils) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.ingestionService = ingestionService;
        this.architecture = architecture;
        this.gitUtils = gitUtils;
    }

    

    public Report getReport(String url){
        String repoId = RepoUtils.extractRepoId(url);

        String[] parts = url.split("/");
            String user = parts[parts.length - 2];
            String repo = parts[parts.length - 1].replace(".git", "");
            


        try {
            ingestionService.ingestRepo(url);

            String dependencies = architecture.getDependency(user, repo);

            BinaryTreeNode root = gitUtils.gitHubTree(url);
            String archDiagram = architecture.displayArchitecture(root, url, true);
            String analysis = generateAnalysis(repoId);

            String recommendations = generateRecommendations(repoId);
            String architectureRec = generateArchitecture(archDiagram);
            

            Report report = new Report(repoId, dependencies, analysis, archDiagram, recommendations, architectureRec);
            return report;


        } catch (Exception e) {
            log.error("Analysis failed for repo: {}", url, e);

            Report errorReport = new Report(url, "Error extracting dependencies.", "Error running anaylsis on architecture.", "Error display architecture.", "Error providing recommendations on repository.", "Error displaying refactored repository." );
            return errorReport;
        }
   
    }

    public String generateAnalysis(String repoId) {
        String filter = "repo == '" + repoId + "'";

    
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("Summarize architecture patterns and anti-patterns for microservices")
                .filterExpression(filter) 
                .topK(50)
                .build()
        );
    
        if (docs.isEmpty()) return "Unable to generate analysis for the given repository.";
    
        String context = docs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n---\n"));
    
        List<Message> messages = Arrays.asList(
            new SystemMessage("You are a software engineering expert. Summarize detected patterns and anti-patterns based on the following code context."),
            new UserMessage(context)
        );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }
    
    public String generateRecommendations(String repoId) {
        String filter = "repo == '" + repoId + "'";
    
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("What are potential refactoring suggestions for microservice quality and scalability?")
                .filterExpression(filter)
                .topK(50)
                .build()
        );
    
        if (docs.isEmpty()) return "No refactor suggestions available for this repo.";
    
        String context = docs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n---\n"));
    
        List<Message> messages = Arrays.asList(
            new SystemMessage("You are a software engineering expert. Suggest specific refactoring and improvement steps for the following microservices code."),
            new UserMessage(context)
        );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }


    public String generateArchitecture(String archDiagram) {
        List<Message> messages = Arrays.asList(
            new SystemMessage("You are a software engineering expert. Here's the architecture of the repository. Suggest architecture improvement and refactor recommendations"),
            new UserMessage(archDiagram)
        );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }

    
    



    public String answer(String question, String repoId) {
        List<Document> docs;
        Path jsonPath = Paths.get("/Users/madisonmigliori/Documents/NSF/NSF-SAR/rag/doc");


        if (repoId == null || repoId.isBlank()) {
            throw new IllegalArgumentException("Missing repoId for context search.");
        }
        try {
        
            ingestionService.ingestJson(jsonPath, repoId);
            log.info("JSON ingestion completed for file: {}", jsonPath);
        } catch (Exception e) {
            log.error("Failed to ingest JSON before answering:", e);
            return "An error occurred while ingesting the JSON context. Please check the file and try again.";
        }    
        try {
            log.info("Searching vector store for repoId: {}", repoId);
            docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query(question)
                    .topK(50)
                    .filterExpression(repoId)
                    .similarityThreshold(0.01)
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
                                    You are a friendly and expert senior software architect assistant.
                            
                                    Your job is to:
                                    - Identify the architecture patterns in the provided context, with a focus on microservice architecture.
                                    - Display the architecture and bounded context diagram (describe it in text or use internal tools, but DO NOT provide external image URLs or hyperlinks).
                                    - Help the user design robust, scalable, and secure microservice-based architectures.
                                    - Identify any microservice anti-patterns, smells, or bad coding practices in the code.
                                    - Always explain your reasoning.
                                    - Recommend patterns like service discovery, API gateways, event-driven communication, etc.
                                    - Identify specific code sections needing improvement or refactoring.
                            
                                    IMPORTANT RULES:
                                    - Only answer using the provided context.
                                    - Do not include any external URLs, hyperlinks, or references to third-party websites.
                                    - If the context includes external links (e.g., images from Imgur), DO NOT include them in your response. Instead, summarize their content in text.
                                    - If you don’t have enough context, say: "I don’t have enough information to answer that. Please ask questions about the given repository."
                                """),
                                new UserMessage("Here is the context:\n" + context + "\n\nNow, based on that, answer this question:\n" + question)
                            );
                            
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }
}
