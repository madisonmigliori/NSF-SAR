
package com.nsf.langchain.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.git.GitHubApi;
import com.nsf.langchain.model.Report;
import com.nsf.langchain.model.Scoring;
import com.nsf.langchain.model.AntiPattern;
import com.nsf.langchain.model.MSAPattern;
import com.nsf.langchain.utils.ArchitectureUtils;
import com.nsf.langchain.utils.GitUtils;
import com.nsf.langchain.utils.JsonUtils;
import com.nsf.langchain.utils.RepoUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ArchitectureMap;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;


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

        LocalDateTime timestamp = LocalDateTime.now();
        String timeCsv = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(timestamp);
    
        long startTime = System.currentTimeMillis();
        long ingestionTime = 0;
        long[] dependencyTime = new long[1];
        long[] boundaryTime = new long[1];
        long[] archDiagramTime = new long[1];
        long[] analysisTime = new long[1];
        long[] architectureRecTime = new long[1];
    
        BinaryTreeNode ingestRepo;
        Map<String, String> codeFiles;
        BinaryTreeNode rootTree;
     
    
        try {
            long stepStart = System.currentTimeMillis();
            log.info("Ingestion Started");
            ingestRepo = gitHubApi.inspectRepo(url);
            codeFiles = gitHubApi.extractCode(ingestRepo);
            rootTree = gitUtils.gitHubTree(url);
            
            ingestionTime = System.currentTimeMillis() - stepStart;
            log.info("Ingestion complete in {}ms", ingestionTime);
        } catch (Exception e) {
            log.error("Ingestion failed", e);
            return failedReport(repoId);
        }

        ArchitectureMap map = generateBoundaryFromCodeFiles(codeFiles);
        List<BinaryTreeNode> fileNodes = gitHubApi.collectRelevantFiles(ingestRepo);
        String dependencies = safeStep("Dependency Extraction", () -> architecture.getDependencyFile(fileNodes), t -> dependencyTime[0] = t);
        String boundary = safeStep("Service Boundary", () -> serviceBoundary.printServiceBoundary(codeFiles), t -> boundaryTime[0] = t);
        String archDiagram = safeStep("Architecture Diagram", () -> architecture.displayArchitecture(rootTree), t -> archDiagramTime[0] = t);
        List<AntiPattern> antiPatterns = jsonUtils.loadAntiPatternsJson(Paths.get("doc/msa-anti-patterns.json"));
        List<MSAPattern> patterns = jsonUtils.loadPatternsJson(Paths.get("doc/msa-patterns.json"));
        List<String> anti = serviceBoundary.detectAntiPatterns(map);
        antiPatterns.forEach(System.out::println);


       
        Set<String> codeMatchedPatterns = ingestRepo.patternCounts.keySet();
        Set<String> contextMatchedPatterns = new HashSet<>(matchPatterns(ingestRepo, patterns, dependencies, archDiagram, boundary));
        
        Set<String> allMatchedPatterns = new HashSet<>(codeMatchedPatterns);
        allMatchedPatterns.addAll(contextMatchedPatterns);
        List<String> matchedPatterns = new ArrayList<>(allMatchedPatterns);
        
        Set<String> codeMatchedAntiPatterns = ingestRepo.antiPatternCounts.keySet();
        Set<String> contextMatchedAntiPatterns = new HashSet<>(matchAntiPatterns(ingestRepo, antiPatterns, dependencies, archDiagram, boundary));
        
        Set<String> allMatchedAntiPatterns = new HashSet<>(codeMatchedAntiPatterns);
        allMatchedAntiPatterns.addAll(contextMatchedAntiPatterns);
        List<String> matchedAntiPatterns = new ArrayList<>(allMatchedAntiPatterns);
        Map<String, Double> scoringResults = ingestRepo.scoringResults;
        
        List<String> allWarnings = ingestRepo.allWarnings;

        String warningsSummary = allWarnings.isEmpty()
        ? "No warnings or missing configurations detected."
        : allWarnings.stream().distinct().collect(Collectors.joining("\n- ", "- ", ""));

        List<ServiceBoundaryUtils.ServiceBoundary> services = serviceBoundary.extractFiles(codeFiles);
        Map<String, Set<String>> dependencyGraph = serviceBoundary.buildDependencyGraph(services);
        List<String> issues = ServiceBoundaryUtils.analyzeDependencies(dependencyGraph);
        issues.forEach(issue -> log.warn("Dependency issue detected: {}", issue));
    


    String painPoints = identifyPainPoints(matchedAntiPatterns, scoringResults, issues);

    String analysis = safeStep("Architecture Analysis",
        () -> generateAnalysis(repoId, map, dependencies, boundary, archDiagram, matchedPatterns, matchedAntiPatterns, warningsSummary, issues, scoringResults),
    t -> analysisTime[0] = t
        ); 
    
        String architectureRec = safeStep("Architecture Recommendation", 
    () -> generateArchitecture(repoId, map, dependencies, boundary, archDiagram, analysis, scoringResults, painPoints), 
    t -> architectureRecTime[0] = t);



    
        Report finalReport = new Report(repoId, dependencies, analysis, archDiagram, boundary, architectureRec);
    
        writeTimingCsv(repoId, ingestionTime, dependencyTime[0], boundaryTime[0], archDiagramTime[0], analysisTime[0], architectureRecTime[0], startTime);
        saveReportToFile(finalReport, repoId, timeCsv);
    
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Total report generation time: {}ms ({} min)", totalTime, totalTime / 60000);
        log.info("\n{}", finalReport.toString());
        


        
    
        return finalReport;


    }
    

    private ArchitectureMap generateBoundaryFromCodeFiles(Map<String, String> codeFiles) throws JsonProcessingException {
        List<ServiceBoundaryUtils.ServiceBoundary> artifacts = serviceBoundary.extractFiles(codeFiles);
        ServiceBoundaryUtils.ArchitectureMap archMap = serviceBoundary.fallback(artifacts);
        archMap.serviceCalls = serviceBoundary.inferServiceRelations(archMap);
        archMap.printLayers();
        return archMap;
    }



    private void saveReportToFile(Report report, String repoId, String timestamp) {
        try {
            Path outputPath = Path.of("reports", repoId + "-" + timestamp + ".txt");
            log.info("Saving report to: {}", outputPath.toAbsolutePath());

            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, report.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Report saved to {}", outputPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save report", e);
        }
    }

    private Report failedReport(String repoId) {
        return new Report(repoId, "Error", "Error", "Error", "Error", "Error");
    }

    private String safeStep(String name, CheckedSupplier<String> step, TimeRecorder recorder) {
        long start = System.currentTimeMillis();
        try {
            String result = step.get();
            long time = System.currentTimeMillis() - start;
            log.info("{} completed in {}ms", name, time);
            recorder.record(time);
            return result;
        } catch (Exception e) {
            log.error("{} failed", name, e);
            recorder.record(System.currentTimeMillis() - start);
            return "Error during " + name.toLowerCase();
        }
    }

    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    public interface TimeRecorder {
        void record(long time);
    }

    private void writeTimingCsv(String repoId,
                            long ingestionTime,
                            long dependencyTime,
                            long boundaryTime,
                            long archDiagramTime,
                            long analysisTime,
                            long architectureRecTime,
                            long startTime) {
    String[] labels = {
        "ingestion", "dependency", "boundary", "architectureDiagram", "analysis", "architectureRecommendation", "total"
    };

    long totalTime = System.currentTimeMillis() - startTime;
    long[] durations = {
        ingestionTime, dependencyTime, boundaryTime, archDiagramTime, analysisTime, architectureRecTime, totalTime
    };

    String timeStr = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
    Path csvPath = Path.of("report_timings.csv");

    try {
        Files.createDirectories(csvPath.getParent() != null ? csvPath.getParent() : Path.of("."));
        boolean fileExists = Files.exists(csvPath);

        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (!fileExists) {
                writer.write("repoId,step,duration_ms,time\n");
            }
            for (int i = 0; i < labels.length; i++) {
                writer.write(String.format("%s,%s,%d,%s\n", repoId, labels[i], durations[i], timeStr));
            }
        }
    } catch (IOException e) {
        log.error("Failed to write timing CSV", e);
    }
}

public String identifyPainPoints(List<String> matchedAntiPatterns, Map<String, Double> scoringResults, List<String> dependencyIssues) {
    StringBuilder sb = new StringBuilder();

    if (!matchedAntiPatterns.isEmpty()) {
        sb.append("Anti-Patterns Detected:\n");
        matchedAntiPatterns.forEach(p -> sb.append("- ").append(p).append("\n"));
    }

    if (scoringResults != null && !scoringResults.isEmpty()) {
        sb.append("\nLow Scoring Areas:\n");
        scoringResults.entrySet().stream()
            .filter(e -> e.getValue() < 5.0)
            .sorted(Map.Entry.comparingByValue())
            .forEach(e -> sb.append(String.format("- %s: %.2f/1\n", e.getKey(), e.getValue())));
    }

    if (dependencyIssues != null && !dependencyIssues.isEmpty()) {
        sb.append("\nDependency Issues:\n");
        dependencyIssues.forEach(issue -> sb.append("- ").append(issue).append("\n"));
    }

    return sb.toString().isBlank() ? "No major pain points detected." : sb.toString();
}


public String generateAnalysis(String repoId, ArchitectureMap map, String dependencies, String boundary, String archDiagram, List<String> matchedPatterns, List<String> matchedAntiPatterns, String warningsSummary, List<String> dependencyIssues, Map<String, Double> scoringResults ) {
    String filter = "repo == '" + repoId + "'";
    List<Scoring> criteria = jsonUtils.loadScoringJson(Paths.get("doc/msa-scoring.json"));
    List<MSAPattern> allPatterns = jsonUtils.loadPatternsJson(Paths.get("doc/msa-patterns.json"));
    List<AntiPattern> antiPatterns = jsonUtils.loadAntiPatternsJson(Paths.get("doc/msa-anti-patterns.json"));
    Map<String, List<String>> dependencyJson = jsonUtils.loadDependencyJson(Paths.get("src/main/resources/static/dependency-catergories.json"));

    List<Document> docs = vectorStore.similaritySearch(
        SearchRequest.builder()
            .query("Summarize architecture patterns and anti-patterns for microservices")
            .filterExpression(filter)
            .topK(15)
            .build()
    );

    String repoContext = docs.isEmpty()
        ? "No additional context available from the repository content."
        : docs.stream().map(Document::getText).limit(10).collect(Collectors.joining("\n---\n"));




    Map<String, String> depCategory = dependencyJson.entrySet().stream()
        .flatMap(e -> e.getValue().stream()
            .map(lib -> Map.entry(lib.toLowerCase(), e.getKey())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));


    Set<String> depTokens = Stream.of(dependencies.split("[,\\s]+"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(String::toLowerCase)
        .collect(Collectors.toSet());

    Set<String> matchedCategories = new HashSet<>();
    for (String dep : depTokens) {
        if (depCategory.containsKey(dep)) {
            matchedCategories.add(depCategory.get(dep));
        }
    }

    String dependencyIssuesSummary = dependencyIssues.isEmpty()
        ? "No dependency issues detected."
        : dependencyIssues.stream()
            .map(issue -> "- " + issue)
            .collect(Collectors.joining("\n"));

 
        Set<String> matchedPatternNames = matchedPatterns.stream()
    .map(String::toLowerCase)
    .collect(Collectors.toSet());

    Set<String> matchedAntiPatternNames = matchedAntiPatterns.stream()
    .map(String::toLowerCase)
    .collect(Collectors.toSet());


    double totalWeight = criteria.stream().mapToDouble(Scoring::getWeight).sum();

Map<String, Double> normalizedWeights = criteria.stream()
    .collect(Collectors.toMap(
        Scoring::getName,
        c -> c.getWeight() / totalWeight
    ));

List<String> individualScores = new ArrayList<>();
double totalWeightedScore = 0.0;

double overallScore = criteria.stream()
    .mapToDouble(c -> {
        double score = scoringResults.getOrDefault(c.getName(), 0.0);
        return (c.getWeight() / totalWeight) * score;
    }).sum();

for (Scoring criterion : criteria) {
    List<String> expectedPatterns = criterion.getPatterns();
    if (expectedPatterns == null || expectedPatterns.isEmpty()) continue;

    double weight = criterion.getWeight();

    long matchedCount = expectedPatterns.stream()
        .map(String::toLowerCase)
        .filter(matchedPatternNames::contains)
        .count();

    double matchFraction = (double) matchedCount / expectedPatterns.size();
    double matchScore = matchFraction * 10;

    double normalizedWeight = normalizedWeights.get(criterion.getName());
    double weightedScore = normalizedWeight * matchScore;
    totalWeightedScore += weightedScore;

    individualScores.add(String.format("%s: %.1f/1 (weighted: %.2f)", criterion.getName(), matchScore, weightedScore));
}

String matchedCriteriaSummary = criteria.stream()
    .map(c -> {
        List<String> matchedInCriterion = c.getPatterns().stream()
            .filter(p -> matchedPatternNames.contains(p.toLowerCase()))
            .toList();
        if (matchedInCriterion.isEmpty()) return null;
        String joined = matchedInCriterion.stream()
            .map(p -> "  - " + p)
            .collect(Collectors.joining("\n"));
        return "- " + c.getName() + " (weight: " + c.getWeight() + ")\n" + joined;
    })
    .filter(Objects::nonNull)
    .collect(Collectors.joining("\n"));

String patternSummary = allPatterns.stream()
    .filter(p -> matchedPatternNames.contains(p.getName().toLowerCase()))
    .map(p -> "- " + p.getName() + ": " + p.getDescription())
    .collect(Collectors.joining("\n"));

String antiPatternSummary = antiPatterns.stream()
    .filter(p -> matchedAntiPatternNames.contains(p.getName().toLowerCase()))
    .map(p -> "- " + p.getName() + ": " + p.getDescription())
    .collect(Collectors.joining("\n"));

StringBuilder scoringSummary = new StringBuilder();
if (scoringResults != null && !scoringResults.isEmpty()) {
    scoringSummary.append("Detailed Scoring Results:\n");
    scoringResults.forEach((criteriaKey, score) -> 
        scoringSummary.append(String.format("- %s: %.2f\n", criteriaKey, score))
    );
} else {
    scoringSummary.append("No scoring results available.\n");
}



    String categorySummary = matchedCategories.isEmpty()
        ? "No known dependency categories matched."
        : matchedCategories.stream().sorted().collect(Collectors.joining(", "));

    

        String contextString = String.format("""
            Repository architecture context:
            %s
        
            Dependencies:
            %s

            Category Summary:
            %s
        
            System Boundaries:
            %s
        
            Architecture Diagram:
            %s
        
            Matched Architectural Patterns:
            %s
        
            Matched Architectural Anti-Patterns:
            %s

            Dependency Issues:
            %s
        
            Warnings and Missing Configurations:
            %s
        
            Evaluation Criteria:
            %s
            """, repoContext, dependencies, categorySummary, boundary, archDiagram, 
                patternSummary, antiPatternSummary, dependencyIssuesSummary, warningsSummary, matchedCriteriaSummary);
        
        String evaluationSummary = scoringResults.entrySet().stream()
    .map(entry -> String.format("- %s: %.2f/1", entry.getKey(), entry.getValue()))
    .collect(Collectors.joining("\n"));

String overall = String.format("""
    **Overall Score:** %.2f/1
    """, scoringResults.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));


    contextString += "\n" + evaluationSummary;
    
    contextString += "\n" + overall;


    Prompt prompt = new Prompt(List.of(
        new SystemMessage("""
            You are a senior microservices architect.

            Your task is to analyze a repository's architecture using the provided context.

            1. Detect which architectural patterns are evident.
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

    ChatResponse response = chatModel.call(prompt);
    String analysisText = response.getResult().getOutput().getText();

StringBuilder debugInfo = new StringBuilder();
debugInfo.append("\n\n--- Debug Info ---\n");
debugInfo.append("Matched Patterns:\n");
matchedPatterns.forEach(p -> debugInfo.append("- ").append(p).append("\n"));
debugInfo.append("Matched Anti-Patterns:\n");
matchedAntiPatterns.forEach(ap -> debugInfo.append("- ").append(ap).append("\n"));
debugInfo.append("Scoring Results:\n");
if (scoringResults != null && !scoringResults.isEmpty()) {
    scoringResults.forEach((key, score) -> 
        debugInfo.append(String.format("- %s: %.2f\n", key, score))
    );
} else {
    debugInfo.append("No scoring results available.\n");
}

return analysisText + debugInfo.toString();
}

 
    
    public String generateArchitecture(String repoId, ArchitectureMap map, String dependencies, String boundary, String archDiagram, String analysis, Map<String, Double> scoringResults, String painPoints) {
        StringBuilder scoringSummary = new StringBuilder();
        if (scoringResults != null && !scoringResults.isEmpty()) {
            scoringSummary.append("Scoring Results:\n");
            scoringResults.forEach((key, score) -> scoringSummary.append(String.format("- %s: %.2f\n", key, score)));
        }

        

    
        String contextString = """
            Repository ID: %s
            
            --- Repositoty Files ---
            %s
            --- Dependency Summary ---
            %s
            
            --- Identified Service Boundaries ---
            %s
            
            --- Current Architecture Diagram ---
            %s
            
            --- Architectural Analysis ---
            %s
            
            --- Scoring Results ---
            %s

            --- Pain Points ---
            %s
            
            --- Objective ---
            Design a **refactored target architecture** for the codebase described above.
            
            --- Instructions ---
            1. Begin with a complete **ASCII architecture tree diagram**, using:
               - `├──`, `└──`, and indentation for structure
               - Logical groupings (e.g., gateways, shared libs, domain services)
               - Nodes like: `API Gateway`, `Config Server`, `Event Bus`, `Discovery Server`, `Monitoring`, etc.
               - Mark event producers (`(pub)`) and consumers (`(sub)`) where applicable
            
            2. After the diagram, provide a concise explanation with these sections:
               - **Changes Made**: What was added/removed/moved and why
               - **Patterns Applied**: e.g., Smart Endpoints, Gateway Routing, Pub/Sub, Bulkhead
               - **Scoring Influence**: How the low/high scores informed restructuring decisions
               - **Scalability & Resilience**: Justify improvements made to support these concerns
               - **Domain Alignment**: Note how boundaries were clarified or improved
               Include a breakdown of:
                - **Pain Points**: What architectural or design issues the refactor addresses
            Be specific. Avoid generic fluff.
            """
            .formatted(repoId, map, dependencies, boundary, archDiagram, analysis, scoringSummary, painPoints);
            
    
        Prompt prompt = new Prompt(List.of(
            new SystemMessage("""
            You are a senior microservices architect.

            Your job:
            1. Generate a clear **ASCII diagram** of the full refactored architecture
                2. Explain your choices across key architectural dimensions (modularity, boundaries, fault isolation, observability)

            Return output in **this format**:

            """),
            new UserMessage(contextString)
        ));
    
    
        AtomicReference<StringBuilder> output = new AtomicReference<>(new StringBuilder());

        chatModel.stream(prompt)
        .doOnNext(chunk -> output.get().append(chunk.getResult().getOutput().getText()))
    .doOnError(e -> log.error("Streaming error: ", e))
    .doOnComplete(() -> log.info("Streaming complete."))
    .blockLast();

        String finalOutput = output.get().toString();

        String afterDiagram = extractDiagramFromResponse(finalOutput);
    String diff = generateDiffSummary(archDiagram, afterDiagram);
log.info("Architecture diff:\n{}", diff);

    if (!afterDiagram.contains("├──") && !afterDiagram.contains("└──")) {
        log.warn("Expected ASCII diagram not found in architecture output:\n{}", finalOutput);
        }   

        return finalOutput;
    }
    
    

    public static String extractDiagramFromResponse(String response) {
        String[] lines = response.split("\n");
        StringBuilder diagram = new StringBuilder();
    
        boolean inDiagram = false;
        for (String line : lines) {
            if (line.trim().startsWith("├──") || line.trim().startsWith("└──")) {
                inDiagram = true;
            }
    
            if (inDiagram) {
                if (line.trim().isEmpty()) break; 
                diagram.append(line).append("\n");
            }
        }
    
        return diagram.toString().trim();
    }
    
   

    public String generateDiffSummary(String before, String after) {
        List<String> beforeLines = List.of(before.split("\n"));
        List<String> afterLines = List.of(after.split("\n"));
    
        Set<String> beforeSet = new HashSet<>(beforeLines);
        Set<String> afterSet = new HashSet<>(afterLines);
    
        List<String> added = afterLines.stream().filter(l -> !beforeSet.contains(l)).toList();
        List<String> removed = beforeLines.stream().filter(l -> !afterSet.contains(l)).toList();
    
        StringBuilder diff = new StringBuilder("\n--- Architecture Changes Summary ---\n");
    
        if (!added.isEmpty()) {
            diff.append("➕ Added:\n");
            added.forEach(line -> diff.append("  + ").append(line).append("\n"));
        }
        if (!removed.isEmpty()) {
            diff.append("➖ Removed:\n");
            removed.forEach(line -> diff.append("  - ").append(line).append("\n"));
        }
        if (added.isEmpty() && removed.isEmpty()) {
            diff.append("No structural changes detected.\n");
        }
    
        return diff.toString();
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


    
    public List<String> matchGlobalArchitecturePatterns(BinaryTreeNode root, List<AntiPattern> antiPatterns, String dependencies, String archDiagram, String boundaries) {
        String lowerDeps = dependencies.toLowerCase();
        String lowerArch = archDiagram.toLowerCase();
        String lowerBoundaries = boundaries.toLowerCase();
    
        List<String> matched = antiPatterns.stream()
            .filter(p -> {
                String patternText = p.getName().toLowerCase();
                return lowerDeps.contains(patternText)
                    || lowerArch.contains(patternText)
                    || lowerBoundaries.contains(patternText);
            })
            .map(AntiPattern::getName)
            .distinct()
            .toList();
    
        if (matched.isEmpty()) {
            log.info("No anti-patterns matched.");
        } else {
            log.warn("Detected architecture anti-patterns: {}", String.join(", ", matched));
        }
    
        return matched;
    }
    public List<String> matchAntiPatterns(BinaryTreeNode root, List<AntiPattern> antiPatterns, String deps, String arch, String boundaries) {
        return matchGlobalArchitectureConcepts(root, antiPatterns, deps, arch, boundaries, AntiPattern::getName, "anti-pattern");
    }
    
    public List<String> matchPatterns(BinaryTreeNode root, List<MSAPattern> patterns, String deps, String arch, String boundaries) {
        return matchGlobalArchitectureConcepts(root, patterns, deps, arch, boundaries, MSAPattern::getName, "pattern");
    }

    private <T> List<String> matchGlobalArchitectureConcepts(
        BinaryTreeNode root,
        List<T> concepts,
        String dependencies,
        String archDiagram,
        String boundaries,
        Function<T, String> nameExtractor,
        String label 
) {
    String lowerDeps = dependencies.toLowerCase();
    String lowerArch = archDiagram.toLowerCase();
    String lowerBoundaries = boundaries.toLowerCase();

    return concepts.stream()
        .map(c -> Map.entry(nameExtractor.apply(c), nameExtractor.apply(c).toLowerCase()))
        .filter(entry ->
            lowerDeps.contains(entry.getValue()) ||
            lowerArch.contains(entry.getValue()) ||
            lowerBoundaries.contains(entry.getValue()))
        .map(Map.Entry::getKey)
        .distinct()
        .peek(name -> log.warn("Detected architecture {}: {}", label, name))
        .toList();
}

    
    
}

