
package com.nsf.langchain.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.git.GitHubApi;
import com.nsf.langchain.model.Repo;
import com.nsf.langchain.model.Report;
import com.nsf.langchain.model.Scoring;
import com.nsf.langchain.model.BoundaryResult;
import com.nsf.langchain.model.Pattern;
import com.nsf.langchain.utils.ArchitectureUtils;
import com.nsf.langchain.utils.GitUtils;
import com.nsf.langchain.utils.JsonUtils;
import com.nsf.langchain.utils.RepoUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ArchitectureMap;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ServiceBoundary;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.info.ProjectInfoProperties.Git;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    

    public Report getReport(String url) throws JsonProcessingException {
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
        String boundary2 = "";
    
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

        List<ServiceBoundaryUtils.ServiceBoundary> artifacts = serviceBoundary.extractFiles(codeFiles);
        ServiceBoundaryUtils.ArchitectureMap archMap = serviceBoundary.fallback(artifacts);

    
    

        String diagram = serviceBoundary.generateDiagramWithMultiArrowsBetweenBoxes(archMap);
        System.out.println("Here is the digram\n" + diagram);

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
            // boundary = generateBoundary(repoId, ingestRepo);
            BoundaryResult boundaryResult = inferBoundary(ingestRepo);

            boundary = "\n\n---\n\n" + boundaryResult.context();
            boundary2 = generateBoundary2(ingestRepo);
            System.out.println("\"\\n" + //
                                "\\n" + //
                                "---\\n" + //
                                "\\n" + //
                                "\" Anaylze and Print");
            serviceBoundary.analyzeAndPrint(codeFiles);
            


            log.info("Service boundary generated in {}ms", System.currentTimeMillis() - stepStart);
            totalTime += (System.currentTimeMillis() - stepStart);
        } catch (Exception e) {
            boundary2 = "Error identifying service boundary.";
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
            analysis = "Error running analysis.";
            log.error("Analysis generation failed", e);
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
    
    
      

    
        Report finalReport = new Report(repoId, dependencies, analysis, archDiagram, boundary2, recommendations, architectureRec);

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
    
        if (docs.isEmpty()) return "Insufficient repository context found for generating analysis.";
    
        String repoContext = docs.stream().map(Document::getText).limit(10).collect(Collectors.joining("\n---\n"));
    
        String lowerDeps = dependencies.toLowerCase();
        String lowerArch = archDiagram.toLowerCase();
        String lowerBoundary = boundary.toLowerCase();
    
        List<Pattern> relevantPatterns = allPatterns.stream()
            .filter(p -> Stream.of(lowerDeps, lowerArch, lowerBoundary).anyMatch(text -> text.contains(p.getName().toLowerCase())))
            .collect(Collectors.toList());
    
        String shortPatternSummaries = relevantPatterns.stream()
            .map(p -> "- " + p.getName() + ": " + p.getDescription())
            .collect(Collectors.joining("\n"));
    
        String scoringContext = criteria.stream().map(c -> {
            StringBuilder sb = new StringBuilder();
            if (c.getPatterns() != null && !c.getPatterns().isEmpty()) {
                sb.append("Patterns:\n");
                for (String pattern : c.getPatterns()) {
                    allPatterns.stream()
                        .filter(p -> p.getName().equalsIgnoreCase(pattern))
                        .findFirst()
                        .ifPresent(p -> sb.append("- ").append(p.getName()).append(": ").append(p.getDescription()).append("\n"));
                }
            }
            return String.format("""
                Criterion: %s
                Description: %s
                Guidance: %s
                %s
                Weight: %s
                """, c.getName(), c.getDescription(), c.getGuidance(), sb, c.getWeight());
        }).collect(Collectors.joining("\n---\n"));
    
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
    
        Prompt prompt = new Prompt(List.of(
            new SystemMessage("""
                You are a senior microservices architect.
    
                Your task is to analyze a repository's architecture using the provided context.
    
                1. **Detect** which architectural patterns are evident.
                2. Explain each detected pattern.
                3. Summarize strengths and weaknesses.
                4. Evaluate criteria:
                   - Score 0–10
                   - List strengths, weaknesses, and improvements
    
                End with:
                ## Overall Score: <0-10>
                Summary: ...
            """),
            new UserMessage(contextString)
        ));
    
        long start = System.currentTimeMillis();
        ChatResponse response = chatModel.call(prompt);
        log.info("Architecture analysis completed in {}ms", System.currentTimeMillis() - start);
    
        return response.getResult().getOutput().getText();
    }
    
    
    
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
    
        Prompt prompt = new Prompt(List.of(
            new SystemMessage("""
                You are a senior microservices architect.
    
                Refactor the codebase using this structure:
                - Return an **ASCII tree** architecture diagram using '├──' and '└──'
                - Introduce: API Gateway, Config Server, Event Bus, Discovery Server
                - Group services logically (e.g. users, payments)
                - Mark event producers/consumers
                - Optimize for modularity and resilience
    
                Then, explain:
                - New architecture
                - What changed and why
                - Patterns applied
            """),
            new UserMessage(contextString)
        ));
    
        long start = System.currentTimeMillis();
        ChatResponse response = chatModel.call(prompt);
        String output = response.getResult().getOutput().getText();
        log.info("Architecture refactoring completed in {}ms", System.currentTimeMillis() - start);
    
        if (!output.contains("├──") && !output.contains("└──")) {
            log.warn("Expected ASCII diagram not found in architecture output:\n{}", output);
        }
    
        return output;
    }
    
    public String generateBoundary2(BinaryTreeNode repoUrl) throws JsonProcessingException{
        Map<String, String> codeFiles = gitHubApi.extractCode(repoUrl);

        List<ServiceBoundaryUtils.ServiceBoundary> artifacts = serviceBoundary.extractFiles(codeFiles);
        ServiceBoundaryUtils.ArchitectureMap archMap = serviceBoundary.fallback(artifacts);
    
        archMap.serviceCalls = serviceBoundary.inferServiceRelations(archMap);

        archMap.printLayers();

        String diagram = serviceBoundary.generateFlatDiagramWithClearArrows(archMap);
        System.out.println("Here is the digram" + diagram);
                return diagram;

    }

   

    
    public BoundaryResult inferBoundary(BinaryTreeNode root) {
        Map<String, String> codeFiles = gitHubApi.extractCode(root);
        
        if (codeFiles == null || codeFiles.isEmpty()) {
            return new BoundaryResult("No source code found.");
        }
    
        List<ServiceBoundary> artifacts = serviceBoundary.extractFiles(codeFiles);
        if (artifacts.isEmpty()) {
            return new BoundaryResult("No service boundary artifacts extracted.");
        }
    
        ArchitectureMap map = serviceBoundary.fallback(artifacts);
        String context = serviceBoundary.generateBoundaryContextDiagram(map);
    
        return new BoundaryResult(context);
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
                    .topK(15)
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

    public String testPromptOnly(String type, String repoId, String dependencies, String boundary, String archDiagram, String analysis) {
        String promptText = switch (type) {
            case "analysis" -> generateAnalysisPromptOnly(repoId, dependencies, boundary, archDiagram);
            case "architecture" -> generateArchitecturePromptOnly(repoId, dependencies, boundary, archDiagram, analysis);
            default -> throw new IllegalArgumentException("Unknown prompt type: " + type);
        };
        return promptText;
    }

    public String generateAnalysisPromptOnly(String repoId, String dependencies, String boundary, String archDiagram) {
    
        return """
            Repository architecture context: (mocked)
    
            Dependencies:
            %s
    
            System Boundaries:
            %s
    
            Architecture Diagram:
            %s
            """.formatted(dependencies, boundary, archDiagram);
    }
    
    public String generateArchitecturePromptOnly(String repoId, String dependencies, String boundary, String archDiagram, String analysis) {
        return """
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
    }
    
    
}
