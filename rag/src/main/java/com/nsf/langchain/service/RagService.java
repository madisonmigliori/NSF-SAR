package com.nsf.langchain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chroma.vectorstore.ChromaApi.Collection;
import org.springframework.ai.chroma.vectorstore.ChromaApi.GetEmbeddingsRequest;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.git.GitHubApi;
import com.nsf.langchain.model.Repo;
import com.nsf.langchain.model.Report;
import com.nsf.langchain.model.Scoring;
import com.nsf.langchain.model.Pattern;
import com.nsf.langchain.utils.ArchitectureUtils;
import com.nsf.langchain.utils.GitUtils;
import com.nsf.langchain.utils.JsonUtils;
import com.nsf.langchain.utils.RepoUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ArchitectureMap;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ServiceBoundary;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Git;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final GitUtils gitUtils;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final IngestionService ingestionService;
    private final ArchitectureUtils architecture;
    private final ServiceBoundaryUtils serviceBoundary;
    private final GitHubApi gitHubApi;
    private final JsonUtils jsonUtils;

    private final ChromaVectorStoreManager vectorManager;

    // @Autowired
    // public IngestionService(ChromaVectorStoreManager vectorManager) {
    //     this.vectorManager = vectorManager;
    // }

    public RagService(VectorStore vectorStore, ChatModel chatModel, IngestionService ingestionService, ChromaVectorStoreManager vectorManager) {
    public RagService(VectorStore vectorStore, ChatModel chatModel, IngestionService ingestionService, ArchitectureUtils architecture, GitUtils gitUtils, ServiceBoundaryUtils serviceBoundary, GitHubApi gitHubApi, JsonUtils jsonUtils) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.ingestionService = ingestionService;
        this.vectorManager = vectorManager;
    }

    public String answer(String question, String repoId) {
        List<Document> docs;

        if (repoId == null || repoId.isBlank()) {
            throw new IllegalArgumentException("Missing repoId for context search.");
        this.architecture = architecture;
        this.gitUtils = gitUtils;
        this.serviceBoundary = serviceBoundary;
        this.gitHubApi = gitHubApi;
        this.jsonUtils = jsonUtils;
    }
    

    public Report getReport(String url) {
        String repoId = RepoUtils.extractRepoId(url);
        String[] parts = url.split("/");
        String user = parts[parts.length - 2];
        String repo = parts[parts.length - 1].replace(".git", "");
    
        String dependencies = "";
        String analysis = "";
        String archDiagram = "";
        String recommendations = "";
        String architectureRec = "";
        String boundary = "";
    
        long startTime = System.currentTimeMillis();
        long totalTime = 0;
        BinaryTreeNode ingestRepo = null;
    
        try {
            log.info("Starting ingestion for: {}", url);
            long stepStart = System.currentTimeMillis();
            ingestRepo = gitHubApi.inspectRepo(url);
            log.info("Ingestion complete in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);

        } catch (Exception e) {
            log.error("Ingestion failed", e);
        }
    
        Map<String, String> codeFiles = gitHubApi.extractCode(ingestRepo);

        List<ServiceBoundaryUtils.ServiceBoundary> artifacts = serviceBoundary.extractCode(codeFiles);
        ServiceBoundaryUtils.ArchitectureMap archMap = serviceBoundary.fallback(artifacts);

        archMap.printLayers();

        String diagram = serviceBoundary.generateBoundaryContextDiagram(archMap);
        System.out.println(diagram);

        try {
            long stepStart = System.currentTimeMillis();
            dependencies  = architecture.getDependency(user, repo);
            log.info("Dependency extraction completed in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            dependencies = "Error extracting dependencies.";
            log.error("Dependency extraction failed", e);
        }
        try {
            ingestionService.ingestJson();
            log.info("JSON ingestion completed for file: {}", repoId);
            long stepStart = System.currentTimeMillis();
            boundary = generateBoundary(repoId, ingestRepo);
            log.info("Service boundary generated in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            boundary = "Error identifying service boundary.";
            log.error("Service boundary extraction failed", e);
        }
    
        try {
            long stepStart = System.currentTimeMillis();
            BinaryTreeNode root = gitUtils.gitHubTree(url);
            archDiagram = architecture.displayArchitecture(root);

            System.out.println(root);
            System.out.println(archDiagram);
            log.info("Architecture diagram built in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            archDiagram = "Error displaying architecture.";
            log.error("Architecture diagram generation failed", e);
        }
    
        try {
            long stepStart = System.currentTimeMillis();
            analysis = generateAnalysis(repoId, dependencies, boundary, archDiagram);
            log.info("Analysis complete in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            log.error("Failed to ingest JSON before answering:", e);
            return "An error occurred while ingesting the JSON context. Please check the file and try again.";
        }    
        try {
            log.info("Searching vector store for repoId: {}", repoId);
            docs = vectorManager.getDocuments();
            analysis = "Error running analysis.";
            log.error("Analysis generation failed", e);
        }
    
        try {
            long stepStart = System.currentTimeMillis();
            recommendations = generateRecommendations(repoId, dependencies, boundary, archDiagram, analysis);
            log.info("Recommendations generated in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            log.error("Error during similarity search", e);
            return "An error occurred while searching for relevant context. Please try again later.";
        }

        if (docs == null || docs.isEmpty()) {
            return "No relevant context found.";
        }

            recommendations = "Error providing recommendations.";
            log.error("Recommendation generation failed", e);
        }
    
        try {
            long stepStart = System.currentTimeMillis();
            architectureRec = generateArchitecture(repoId,dependencies, boundary, archDiagram, analysis);
            log.info("Refactored architecture generated in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            architectureRec = "Error generating refactored architecture.";
            log.error("Refactored architecture generation failed", e);
        }
    
    
      

    
        Report finalReport = new Report(repoId, dependencies, analysis, archDiagram, boundary, recommendations, architectureRec);

        try {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = repoId + "-" + timestamp + ".txt";
        Path outputPath = Path.of("reports", fileName);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, finalReport.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("Report saved to {}", outputPath.toAbsolutePath());

        } catch (IOException e) {
        log.error("Failed to save report", e);
        }


        log.info("\n{}", finalReport.toString());
        log.info("Total report generation time: {}ms and in {}min", System.currentTimeMillis() - startTime, (totalTime / 6000 ));

        return finalReport;
    }
    

    public String generateAnalysis(String repoId, String dependencies, String boundary, String archDiagram) {
        String filter = "repo == '" + repoId + "'";
    
        List<Scoring> criteria = jsonUtils.loadScoringJson(Paths.get("doc/msa-scoring.json"));
        List<Pattern> allPatterns = jsonUtils.loadPatternsJson(Paths.get("doc/msa-patterns.json"));
    
      
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("Summarize architecture patterns and anti-patterns for microservices")
                .filterExpression(filter)
                .topK(15)
                .build()
        );
    
        if (docs.isEmpty()) {
            return "Insufficient repository context found for generating analysis.";
        }
    
        String repoContext = docs.stream()
            .map(Document::getText)
            .limit(10)
            .collect(Collectors.joining("\n---\n"));
    
       
        String lowerDeps = dependencies.toLowerCase();
        String lowerArch = archDiagram.toLowerCase();
        String lowerBoundary = boundary.toLowerCase();
    
        List<Pattern> relevantPatterns = allPatterns.stream()
            .filter(p ->
                Stream.of(lowerDeps, lowerArch, lowerBoundary).anyMatch(text ->
                    text.contains(p.getName().toLowerCase())
                )
            )
            .collect(Collectors.toList());
    
        String shortPatternSummaries = relevantPatterns.stream()
            .map(p -> "- " + p.getName() + ": " + p.getDescription())
            .collect(Collectors.joining("\n"));
    
        String scoringContext = criteria.stream()
            .map(c -> {
                StringBuilder patternMatches = new StringBuilder();
                if (c.getPatterns() != null && !c.getPatterns().isEmpty()) {
                    patternMatches.append("Patterns:\n");
                    for (String patternName : c.getPatterns()) {
                        allPatterns.stream()
                            .filter(p -> p.getName().equalsIgnoreCase(patternName))
                            .findFirst()
                            .ifPresent(p -> patternMatches
                                .append("- ").append(p.getName()).append(": ").append(p.getDescription()).append("\n"));
                    }
                }
                return String.format("""
                    Criterion: %s
                    Description: %s
                    Guidance: %s
                    %s
                    Weight: %s
                    """, c.getName(), c.getDescription(), c.getGuidance(), patternMatches, c.getWeight());
            })
            .collect(Collectors.joining("\n---\n"));
    
      
        String contextString = """
            Repository architecture context:
            %s
    
            Dependencies:
            %s
    
            System Boundaries:
            %s
    
            Architecture Diagram:
            %s
    
            Relevant Architectural Patterns:
            %s
    
            Evaluation Criteria:
            %s
            """.formatted(repoContext, dependencies, boundary, archDiagram, shortPatternSummaries, scoringContext);
    
        List<Message> messages = Arrays.asList(
            new SystemMessage("""
    You are a senior microservices architect.
    
    Your task is to analyze a repository's architecture using the provided context.
    
    1. **Detect** which architectural patterns from the list are evident based on the architecture diagram, dependencies, and boundaries.
    2. For each detected pattern, give a short reason why it's present.
    3. Summarize advantages and disadvantages based on the chosen patterns.
    4. Evaluate each scoring criterion:
       - Describe how the system meets or fails the criterion.
       - Mention patterns that support or hinder this.
       - Suggest improvements.
       - Score from 0 to 10.
    
    At the end, compute an **overall weighted score**.
    
    Output format:
    
    ## Detected Patterns:
    - <Pattern 1>: <Justification>
    - ...
    
    ## Pattern-Based Summary:
    **Advantages:**
    - ...
    
    **Disadvantages:**
    - ...
    
    ## Criterion: <Name>
    - Score: <0-10>
    - Strengths: ...
    - Weaknesses: ...
    - Suggested Improvements: ...
    
    ## Overall Score: <0-10>
    Summary: ...
    """),
            new UserMessage(contextString)
        );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }
    
    
    
    public String generateRecommendations(String repoId, String dependencies, String boundary, String archDiagram, String analysis) {
        String filter = "repo == '" + repoId + "'";
    
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("What are potential refactoring suggestions for microservice quality based on the analysis?")
                .filterExpression(filter)
                .topK(50)
                .build()
        );
    
        if (docs.isEmpty()) {
            return "No refactor suggestions available for this repository.";
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
                                    - DO NOT hallucinate.
                                """),
                                new UserMessage("Here is the context:\n" + context + "\n\nNow, based on that, answer this question:\n" + question)
                            );

            .map(Document::getText)
            .collect(Collectors.joining("\n---\n"));
    
        String contextString = """
            Here is the repository context:
            %s
    
            Here are the dependencies:
            %s
    
            Here is the system boundary:
            %s
    
            Here is the architecture diagram:
            %s
    
            Here is the repository analysis:
            %s
            """.formatted(context, dependencies, boundary, archDiagram, analysis);
    
        List<Message> messages = Arrays.asList(
            new SystemMessage("""
    You are a senior software architect and microservices expert.
    
    Based on the repository's analysis, system boundary, architecture diagram, and dependencies, identify concrete **refactoring opportunities** to improve the following quality attributes:
    
    - Scalability
    - Resilience
    - Technology Diversity
    - Agility
    - Cost-Effectiveness
    - Reusability
    
    Each recommendation must be **prioritized** according to scoring weights (assume higher-weighted criteria are more critical).
    
    Output format (grouped by **Category**):
    
    ### Category: <e.g., Scalability>
    
    | Problem | Recommendation | Benefit | Pattern Reference |
    |--------|----------------|---------|-------------------|
    | ...    | ...            | ...     | ...               |
    
    If applicable, reference architectural patterns that support the recommendation (e.g., CQRS, Event Sourcing, Service Mesh).
    
    Be practical and specific—avoid vague guidance. Include implementation hints if relevant.
    """),
            new UserMessage(contextString)
        );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }
    

    // public String generateArchitecture(String repoId, String dependencies, String boundary, String archDiagram, String analysis) {
    //     String contextString = """
    //         Here are the dependencies:
    //         %s
    
    //         Here is the system boundary:
    //         %s
    
    //         Here is the current architecture diagram:
    //         %s
    
    //         Here is the architectural analysis:
    //         %s
    //         """.formatted(dependencies, boundary, archDiagram, analysis);
    
    //     List<Message> messages = List.of(
    //         new SystemMessage("""
    //             You are a software architecture expert.
    
    //             Based on the current architecture, dependencies, system boundary, and analysis:
    //             - Propose a **fully refactored microservice architecture**.
    //             - The goal is to improve **modularity**, **scalability**, **maintainability**, and **alignment with microservice patterns**.
    //             - Reduce coupling and enforce clear service boundaries.
    //             - **Use the same ASCII format starting with '├──', '└──', etc.**
    //             - Ensure the diagram is complete and readable.
    
    //             After the diagram, include a concise explanation of:
    //             - Key improvements made.
    //             - Which microservice patterns were used (e.g., CQRS, Saga, Service Mesh).
    //             - Why this architecture is more robust.
    //         """),
    //         new UserMessage(contextString)
    //     );
    
    //     ChatResponse response = chatModel.call(new Prompt(messages));
    //     String output = response.getResult().getOutput().getText();


    //     if (!output.contains("├──") && !output.contains("└──")) {
    //         System.out.println("Warning: No valid ASCII diagram found in output: " + output);
    //     }
    //     return output;
    // }

    public String generateArchitecture(String repoId, String dependencies, String boundary, String archDiagram, String analysis) {
        String contextString = """
            Repository ID: %s
    
            Dependency Information:
            %s
    
            Identified Service Boundaries:
            %s
    
            Current Architecture Diagram:
            %s
    
            Architectural Analysis:
            %s
    
            Based on this, refactor the codebase to improve clarity, separation of concerns, and modern best practices.
            """.formatted(repoId, dependencies, boundary, archDiagram, analysis);
    
        List<Message> messages = List.of(
            new SystemMessage("""
                You are a senior microservices architect.
    
                Your task is to **refactor** a monolithic or loosely-coupled codebase into a robust, scalable **microservices architecture** using the provided directory structure and code relationships.
    
                **Deliverables**:
                1. A refactored architecture in **ASCII tree diagram format**, based on directory structure.
                2. Introduce and place:
                   - API Gateway
                   - Config Server
                   - Event Bus (Kafka / RabbitMQ)
                   - Discovery Server (Eureka/Consul if applicable)
                3. Group services logically (e.g., users, payments, orders).
                4. Indicate which services are producers/consumers of events (if any).
                5. Optimize for **modularity, config centralization, and fault tolerance**.
                6. After the diagram, briefly explain:
                   - The new structure
                   - Where config/gateway/bus/discovery were added
                   - How it improves maintainability and scalability
            """),
            new UserMessage(contextString)
        );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        String output = response.getResult().getOutput().getText();
    
        if (!output.contains("├──") && !output.contains("└──")) {
            System.out.println("Warning: No valid ASCII diagram found in output: " + output);
        }
    
        return output;
    }
    
    
    

    public String generateBoundary(String repoId, BinaryTreeNode root) {
        try {
            Map<String, String> files = gitHubApi.extractCode(root);
            if (files == null || files.isEmpty()) {
                return "No source files found for repository: " + repoId;
            }
    
            List<ServiceBoundaryUtils.ServiceBoundary> artifacts = serviceBoundary.extractCode(files);
            if (artifacts.isEmpty()) {
                return "No service boundary artifacts extracted from repository: " + repoId;
            }
    
            String formattedCode = serviceBoundary.format(artifacts);
    
            List<Message> messages = List.of(
                new SystemMessage("""
                    You are a software architecture expert.
                    Based on the following code snippets and identified layers,
                    identify logical service boundaries and their responsibilities.
                    Return ONLY a JSON object without any explanation or formatting.
                    If you cannot, return an empty JSON object {}.
                """),
                new UserMessage("Code context:\n" + formattedCode)
            );
    
            ChatResponse response = chatModel.call(new Prompt(messages));
            String output = response.getResult().getOutput().getText();
            String jsonPart = serviceBoundary.extractJson(output);
    
           
            if (jsonPart == null || jsonPart.isBlank() || jsonPart.trim().equals("{}")) {
                return generateFallback("LLM could not extract boundaries.", artifacts);
            }
    
            try {
                new ObjectMapper().readTree(jsonPart); 
            } catch (JsonProcessingException e) {
                return generateFallback("LLM returned invalid JSON:\n" + jsonPart, artifacts);
            }
    
            String asciiDiagram = serviceBoundary.generateAsciiDiagramFromLLMJson(jsonPart);
            ArchitectureMap map = serviceBoundary.parseResponsbilities(asciiDiagram);
            String boundaryContext = serviceBoundary.generateBoundaryContextDiagram(map);
    
            return asciiDiagram + "\n\n---\n\n" + boundaryContext + "\n\n---\n\n" + jsonPart;
    
        } catch (Exception e) {
            return "An unexpected error occurred: " + e.getMessage();
        }
    }
    
    
    
    
    private String generateFallback(String string, List<ServiceBoundary> artifacts) {
            ArchitectureMap fallbackMap = serviceBoundary.fallback(artifacts);
            String fallbackDiagram = serviceBoundary.generateBoundaryContextDiagram(fallbackMap);
            fallbackMap.printLayers();
        
            return string + "\n\n[FALLBACK DIAGRAM]\n" + fallbackDiagram;
        }
        

    // public String answer(String question, String repoUrl) {
    //     if (repoUrl == null || repoUrl.isBlank()) {
    //         return " Missing Git repo URL. Please provide one.";
    //     }
        
    //     List<Document> docs = new ArrayList<>();
    //     // String repoId = extractRepoId(repoUrl);
    //     String repoId = RepoUtils.extractRepoId(repoUrl);
    //     String[] parts = repoUrl.split("/");
    //     Path jsonPath = Paths.get("/Users/madisonmigliori/Documents/NSF/NSF-SAR/rag/doc");


    //     if (repoId == null || repoId.isBlank()) {
    //         throw new IllegalArgumentException("Missing repoId for context search.");
    //     }
    //     try {
        
    //         ingestionService.ingestJson(jsonPath, repoId);
    //         log.info("JSON ingestion completed for file: {}", jsonPath);
    //     } catch (Exception e) {
    //         log.error("Failed to ingest JSON before answering:", e);
    //         return "An error occurred while ingesting the JSON context. Please check the file and try again.";
    //     }    
    //     try {
    //         log.info("Searching vector store for repoId: {}", repoId);
    //         docs = vectorStore.similaritySearch(
    //             SearchRequest.builder()
    //                 .query(question)
    //                 .topK(15)
    //                 .filterExpression(repoId)
    //                 .similarityThreshold(0.01)
    //                 .build()
    //         );
    //     } catch (Exception e) {
    //         log.error("Error during similarity search", e);
    //         return "An error occurred while searching for relevant context. Please try again later.";
    //     }

    //     if (docs == null || docs.isEmpty()) {
    //         return "No relevant context found.";
    //     }

        
    //     String context = docs.stream()
    //                          .map(Document::getText)
    //                          .collect(Collectors.joining("\n---\n"));

    //                          List<Message> messages = Arrays.asList(
    //                             new SystemMessage("""
    //                                 You are a friendly and expert senior software architect assistant.
                            
    //                                 Your job is to:
    //                                 - Identify the architecture patterns in the provided context, with a focus on microservice architecture.
    //                                 - Display the architecture and bounded context diagram (describe it in text or use internal tools, but DO NOT provide external image URLs or hyperlinks).
    //                                 - Help the user design robust, scalable, and secure microservice-based architectures.
    //                                 - Identify any microservice anti-patterns, smells, or bad coding practices in the code.
    //                                 - Always explain your reasoning.
    //                                 - Recommend patterns like service discovery, API gateways, event-driven communication, etc.
    //                                 - Identify specific code sections needing improvement or refactoring.
                            
    //                                 IMPORTANT RULES:
    //                                 - Only answer using the provided context.
    //                                 - Do not include any external URLs, hyperlinks, or references to third-party websites.
    //                                 - If the context includes external links, DO NOT include them in your response. Instead, summarize their content in text.
    //                                 - If you don’t have enough context, say: "I don’t have enough information to answer that. Please ask questions about the given repository."
    //                             """),
    //                             new UserMessage("Here is the context:\n" + context + "\n\nNow, based on that, answer this question:\n" + question)
    //                         );
                            
    //     ChatResponse response = chatModel.call(new Prompt(messages));
    //     return response.getResult().getOutput().getText();
    // }
}
