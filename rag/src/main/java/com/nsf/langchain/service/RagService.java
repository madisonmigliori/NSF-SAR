package com.nsf.langchain.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

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

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Git;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public RagService(VectorStore vectorStore, ChatModel chatModel, IngestionService ingestionService, ArchitectureUtils architecture, GitUtils gitUtils, ServiceBoundaryUtils serviceBoundary, GitHubApi gitHubApi, JsonUtils jsonUtils) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.ingestionService = ingestionService;
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
        String serviceBoundary = "";
    
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
            long stepStart = System.currentTimeMillis();
            serviceBoundary = generateBoundary(repoId, ingestRepo);
            log.info("Service boundary generated in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            serviceBoundary = "Error identifying service boundary.";
            log.error("Service boundary extraction failed", e);
        }
    
        try {
            long stepStart = System.currentTimeMillis();
            BinaryTreeNode root = gitUtils.gitHubTree(url);
            archDiagram = architecture.displayArchitecture(root);
            log.info("Architecture diagram built in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            archDiagram = "Error displaying architecture.";
            log.error("Architecture diagram generation failed", e);
        }
    
        try {
            long stepStart = System.currentTimeMillis();
            analysis = generateAnalysis(repoId, dependencies, serviceBoundary, architectureRec);
            log.info("Analysis complete in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            analysis = "Error running analysis.";
            log.error("Analysis generation failed", e);
        }
    
        try {
            long stepStart = System.currentTimeMillis();
            recommendations = generateRecommendations(repoId, dependencies, serviceBoundary, archDiagram, analysis);
            log.info("Recommendations generated in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            recommendations = "Error providing recommendations.";
            log.error("Recommendation generation failed", e);
        }
    
        try {
            long stepStart = System.currentTimeMillis();
            architectureRec = generateArchitecture(repoId,dependencies, serviceBoundary, archDiagram, analysis);
            log.info("Refactored architecture generated in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            architectureRec = "Error generating refactored architecture.";
            log.error("Refactored architecture generation failed", e);
        }
    
    
      

    
        Report finalReport = new Report(repoId, dependencies, analysis, archDiagram, serviceBoundary, recommendations, architectureRec);

        log.info("\n{}", finalReport.toString());
        log.info("Total report generation time: {}ms", System.currentTimeMillis() - startTime);
    
        return finalReport;
    }
    

    public String generateAnalysis(String repoId, String dependencies, String boundary, String archDiagram) {
        String filter = "repo == '" + repoId + "'";
    
        // Load JSONs directly (no ingestion)
        List<Scoring> criteria = jsonUtils.loadScoringJson(Paths.get("doc/msa-scoring.json"));
        List<Pattern> patterns = jsonUtils.loadPatternsJson(Paths.get("doc/msa-patterns.json"));
    
        // Only vector search for the code context
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("Summarize architecture patterns and anti-patterns for microservices")
                .filterExpression(filter)
                .topK(10)
                .build()
        );
    
        if (docs.isEmpty()) {
            return "Insufficient repository context found for generating analysis.";
        }
    
        // Join repository code context
        String context = docs.stream()
            .map(Document::getText)
            .limit(10)
            .collect(Collectors.joining("\n---\n"));
    
        // Join scoring criteria with referenced patterns (from loaded JSON)
        String scoringContext = criteria.stream()
            .map(c -> {
                StringBuilder relatedPatterns = new StringBuilder();
                if (c.getPatterns() != null && !c.getPatterns().isEmpty()) {
                    relatedPatterns.append("Related Patterns:\n");
                    for (String patternName : c.getPatterns()) {
                        patterns.stream()
                            .filter(p -> p.getName().equalsIgnoreCase(patternName))
                            .findFirst()
                            .ifPresent(p -> relatedPatterns
                                .append("- ").append(p.getName()).append(": ").append(p.getDescription()).append("\n"));
                    }
                }
    
                return String.format("""
                    Criteria: %s
                    Description: %s
                    Guidance: %s
                    Weight: %s
                    %s
                    """, c.getName(), c.getDescription(), c.getGuidance(), c.getWeight(), relatedPatterns);
            })
            .limit(5)
            .collect(Collectors.joining("\n---\n"));
    
        String patternsContext = patterns.stream()
            .limit(5)
            .map(p -> String.format("""
                Pattern: %s
                Description: %s
                Advantage: %s
                Disadvantage: %s
                Common Implementations: %s
                """, p.getName(), p.getDescription(), p.getAdvantage(), p.getDisadvantage(), p.getCommon()))
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
    
            Here is the MSA Scoring Criteria with related architectural patterns:
            %s
    
            Here are additional MSA architectural patterns:
            %s
            """.formatted(context, dependencies, boundary, archDiagram, scoringContext, patternsContext);
    
        List<Message> messages = Arrays.asList(
            new SystemMessage("""
                You are a software architecture expert. 
                Use the provided scoring criteria and architecture patterns to evaluate the repository.
                For each criterion, describe the strengths and weaknesses of the current architecture, suggest improvements, and assign a numeric score.
                At the end, calculate an overall architecture quality score and summarize.
                Be clear, specific, and structured.
                """),
            new UserMessage(contextString)
        );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }
    
    
    public String generateRecommendations(String repoId, String dependencies, String boundary, String archDiagram, String anaylsis) {
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
        
        String contextString = "Here is the context:\n" + context + "\n\nHere are the dependencies:\n" + dependencies + "\n\nHere is the system boundary:\n" + boundary + "\n\nHere is the architceture diagram:\n" + archDiagram + "\n\n\"Here is the analysis based on the repository:\"\n" + //
                        "\n" + anaylsis ;

    
        List<Message> messages = Arrays.asList(
            new SystemMessage("""
                You are a software engineering expert.
                Based on the following repository, dependencies, system boundary, architecture and analysis: 

                - Identify important concerte refactoring opporuntities to improve scalability, resillence, technology diversity, agility, cost-effictiveness, and reusablility. 
                - Priortize based off criteria weight
                - For each suggestion, include:
                    - Catergory
                    - Problem
                    - Refactoring Suggestion
                    - Benefit
                    - Pattern Reference (if able to)

                Output your response as a markdown list group by category.


                 Suggest specific refactoring and improvement steps for the following microservices code.
                """),
                        
            new UserMessage(contextString)
        );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }


    public String generateArchitecture(String repoId, String dependencies, String boundary, String archDiagram, String anaylsis) {

        String contextString = "\n\nHere are the dependencies:\n" + dependencies + "\n\nHere is the system boundary:\n" + boundary + "\n\nHere are the architceture diagram:\n" + archDiagram;
        
        List<Message> messages = Arrays.asList(
            new SystemMessage("""
                You are a software engineering expert. 
                Here's the architecture of the repository, including the dependencies, system boundary, current architecture digram, and architecture analysis.
                
                Please suggest a full refactorred architecture that: 
                - Improves modularity, scalability, and maintainablity, following microservice patterns. 
                - Reduce coupling between layers and services. 
                - Address issues highlighted in analysis
                - Use the same ASCII diagram format starting with '├──', '└──', etc.
                - Ends with a brief explaination of key improvement and how they align with the microservice patterns
                    
                    """),
            new UserMessage(contextString)
        );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }

    public String generateBoundary(String repoId, BinaryTreeNode root) {
        try {
            Map<String, String> files = gitHubApi.extractCode(root);
            if (files == null || files.isEmpty()) {
                log.warn("No files extracted from repo: {}", root);
                return "No source files found for service boundary extraction.";
            }
    
            List<ServiceBoundaryUtils.ServiceBoundary> artifacts = serviceBoundary.extractCode(files);
            if (artifacts.isEmpty()) {
                log.warn("No service boundary artifacts extracted for repo: {}", repoId);
                return "No service boundary artifacts extracted.";
            }
    
            String formatBoundary = serviceBoundary.format(artifacts);
    
            String filter = "repo == '" + repoId + "'";
            List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                    .query("Identify the system boundary, code layers, and generate ASCII UML for the given repository")
                    .filterExpression(filter)
                    .topK(10)
                    .build()
            );
    
            if (docs.isEmpty()) {
                log.warn("No documents found in vector store for repo: {}", repoId);
                return "Unable to generate system boundary: no context documents found.";
            }
    
            String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    
            String serviceContext = "Extract code is here:\n" + formatBoundary + "\nContext from repository:\n" + context;
    
            List<Message> messages = Arrays.asList(
                new SystemMessage("""
                    You are a software architecture expert.
                    - Based on the following code snippets, class names, REST endpoints, repository usage and identified layers,
                    - Identify logical service boundaries and groupings that reflect cohesive microservices.
                    - Suggest clear service names and their responsibilities.
                    Return **only** the JSON object. Do NOT include any explanation, markdown, or formatting like triple backticks.
                """),
                new UserMessage("Here is the code context:\n" + serviceContext)
            );
    
            ChatResponse response = chatModel.call(new Prompt(messages));
            String output = response.getResult().getOutput().getText();
            log.info("AI response received for repo: {}", repoId);
            System.out.println("LLM raw output:\n" + output); 

            String jsonPart = serviceBoundary.extractJson(output);
            ArchitectureMap archMap;


            if (jsonPart != null) {
                try {
                    archMap = ArchitectureMap.fromJson(jsonPart);
                } catch (Exception e) {
                    System.out.println("LLM raw output:\n" + output); 
                    log.warn("Valid-looking JSON failed to parse. Falling back to manual mode.", e);
                    archMap = serviceBoundary.fallback(artifacts);
                }
            } else {
                log.warn("No JSON found in LLM output. Falling back to manual mode.");
                archMap = serviceBoundary.fallback(artifacts);
            }
    
          
            if (archMap == null || archMap.services == null || archMap.services.isEmpty()) {
                log.warn("No services found in architecture map for repo: {}", repoId);
                return "No services identified in architecture map.";
            }
    
            String asciiDiagram = serviceBoundary.generateSBDiagram(archMap.services);
            if (asciiDiagram == null || asciiDiagram.isEmpty()) {
                log.warn("Failed to generate ASCII diagram for repo: {}", repoId);
                return "Error generating ASCII service boundary diagram.";
            }

            
    
            return asciiDiagram + "\n\n" + output;
        } catch (Exception e) {
            log.error("Exception during service boundary generation for repo: " + repoId, e);
            return "Error identifying service boundary: " + e.getMessage();
        }
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
                    .topK(10)
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
                                    - If the context includes external links, DO NOT include them in your response. Instead, summarize their content in text.
                                    - If you don’t have enough context, say: "I don’t have enough information to answer that. Please ask questions about the given repository."
                                """),
                                new UserMessage("Here is the context:\n" + context + "\n\nNow, based on that, answer this question:\n" + question)
                            );
                            
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }
}
