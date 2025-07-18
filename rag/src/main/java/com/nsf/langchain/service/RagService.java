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
import java.util.Set;
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
    // private final GitHubApi gitHubApi;
    private final JsonUtils jsonUtils;
    private final ChromaVectorStoreManager vectorManager;

    // @Autowired
    // public IngestionService(ChromaVectorStoreManager vectorManager) {
    //     this.vectorManager = vectorManager;
    // }

    // public RagService(VectorStore vectorStore, ChatModel chatModel, IngestionService ingestionService, ChromaVectorStoreManager vectorManager) {
    public RagService(VectorStore vectorStore, ChatModel chatModel, IngestionService ingestionService, ArchitectureUtils architecture, ServiceBoundaryUtils serviceBoundary, JsonUtils jsonUtils, ChromaVectorStoreManager vectorManager, GitUtils gitUtils) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.ingestionService = ingestionService;
        this.vectorManager = vectorManager;
        this.architecture =  architecture;
        this.gitUtils = gitUtils;
        this.serviceBoundary = serviceBoundary;
        // this.gitHubApi = gitHubApi;
        this.jsonUtils = jsonUtils;
    }

    // public String answer(String question, String repoId) {
    //     List<Document> docs;

    //     if (repoId == null || repoId.isBlank()) {
    //         throw new IllegalArgumentException("Missing repoId for context search.");
    //     this.architecture = architecture;
    //     // this.gitUtils = gitUtils;
    //     this.serviceBoundary = serviceBoundary;
    //     this.gitHubApi = gitHubApi;
    //     this.jsonUtils = jsonUtils;
    //     }
    // }

    public Report newReport(String url) {
        log.info("NEW REPORT CALLED with URL: " + url);

        String repoId = vectorManager.getRepoId();

        List<Document> criteria = vectorManager.getCriteriaJson(); //confirmed can be retrieved
        List<Document> pattern = vectorManager.getPatternsJson(); //confirmed can be retrieved
        List<Document> antipattern = vectorManager.getAntiPatternsJson(); //confirmed can be retrieved
        List<Document> dependency = vectorManager.getDependencyJson(); //confirmed can be retrieved

        List<Document> dependencies = vectorManager.getDependencies(); 
        List<Document> serviceBoundaries = vectorManager.getServiceBoundary();
        List<Document> artifact = vectorManager.getArchMap(); 
        List<Document> repo = vectorManager.getDocuments(); //confirmed can be retrieved
        BinaryTreeNode root = vectorManager.getRoot(); //confirmed can be retrieved

         String repoContent = repo.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n---\n"));

        List<Document> path = vectorManager.getDocumentPath(); //confirmed can be retrieved


        // String antipatternsFound = findAntiPatterns(repoContent, antipattern);
        // String patternsFound = findPattern(repoContent, pattern);
        // String serviceBoundaryFound = findServiceBoundary(repoContent, serviceBoundaries);
        // String criteriaFound = findCriteria(antipatternsFound, patternsFound, dependencies, serviceBoundaryFound, criteria);

        // PRINT "LAYER" OF ARTIFACT IN ORIGINIAL 
        // PRINT "DISPLAY ARCHITECTER" (LIKE BRANCHING FROM ROOT)
        // String archDiagram = architecture.displayArchitecture(root);
        // Document diagram = new Document(archDiagram, Map.of("Type: ", "Diagram"));

        // String analysis = generateAnalysis(repoId, dependencies, diagram, criteria, repo);
        // String architectureRec = generateArchitecture(repoId, dependencies, diagram, new Document(analysis, Map.of("Type: ", "Repo Analysis")));
        
        // System.out.println(architectureRec);

        // log.info(dependencies.toString());

        // no need to similarity search :)
        // skipping generaterecommendations()


        // generateArchitecture(repo, dependencies, boundary, archDiagram, analysis);

        // Report finalReport = new Report(repoId, dependencies, analysis, archDiagram, architectureRec);

        //********GOT RID OF GENERATEBOUNDARY AND GENERATERECOMMENDATION**********
        // return finalReport;
        log.info("COMPLETELY RAN THROUGH");
        return new Report();
    }

    public String findAntiPatterns(String document, List<Document> antipatternCriteria) {

    String criteriaContext = antipatternCriteria.stream()
        .map(Document::getText)
        .collect(Collectors.joining("\n---\n"));

    List<Message> messages = Arrays.asList(
        new SystemMessage("""
             You are a senior software architect specializing in finding microservice anti-patterns.

            You will receive:
                - Strings of known anti-patterns.
                - Strings of codebase files as input.

            Only respond using this format:
            Anti-pattern: <anti-pattern name>  
            Found in: <filename1>, <filename2>

            STRICT RULES:
            - ONLY output the anti-pattern name and filenames it was found in.
            - DO NOT describe the application or how it works.
            - DO NOT describe frameworks or tools used.
            - DO NOT give summaries or descriptions.
            - DO NOT suggest improvements.
            - DO NOT include code snippets.
            - DO NOT hallucinate patterns or filenames that are not present in the provided input.
            - If no anti-patterns are found, return exactly:
            <no anti-patterns found>
        """),  
            new UserMessage("Anti-pattern criteria:\n" + criteriaContext),
            new UserMessage("Service dependency or codebase context:\n" + document)
    );

    ChatResponse response = chatModel.call(new Prompt(messages));
    return response.getResult().getOutput().getText();
}


    public String findPattern(String document, List<Document> patternCriteria) {

        String criteriaContext = patternCriteria.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n---\n"));

                List<Message> messages = Arrays.asList(
                    new SystemMessage("""
                        You are a static analysis engine.

                        You will be given source code and a predefined list of architectural patterns.

                        Your task is to:
                        - Detect which architectural patterns from the list are explicitly implemented in the provided code.
                        - Only return a list of pattern names that are present, with the corresponding file or location if mentioned.
                        - Do NOT summarize the code.
                        - Do NOT provide explanations or descriptions of the patterns.
                        - Do NOT suggest improvements or mention missing patterns.
                        - Only output the following format (if found):

                        Pattern: <PatternName>  
                        File: <filename or location in code>  

                        STRICT RULES:
                    - DO NOT explain or summarizethe application
                    - DO NOT describe the application
                    - DO NOT interpret the application.
                    - DO NOT make suggestions or improvements.
                    - DO NOT add code snippets.
                    - DO NOT speculate or infer.
                    - DO NOT improve or refactor any part of the system.
                    - DO NOT mention patterns unless clearly and explicitly present.
                    - DO NOT mention files unless clearly named in the input.
                    - If no patterns are found, return exactly:
                    <no patterns found>

                        Repeat only for patterns that can be explicitly confirmed from the code. If nothing matches, return nothing.
                        """),

                //     new SystemMessage("""
                //     You are a senior software architect specializing in identifying architectural patterns in microservice programs.

                //     You will be provided with:
                //     - A list of known architectural patterns.
                //     - Codebase files that may reference those patterns.

                //     Your task is:
                //     - Detect which patterns are explicitly mentioned or clearly implemented in the codebase.
                //     - For each detected pattern, list the filenames where it appears.

                //     Your response must be in this exact format:
                //     Pattern: <pattern name>  
                //     Found in: <filename1>, <filename2>

                //     STRICT RULES:
                //     - DO NOT explain or summarizethe application
                //     - DO NOT describe the application
                //     - DO NOT interpret the application.
                //     - DO NOT make suggestions or improvements.
                //     - DO NOT add code snippets.
                //     - DO NOT speculate or infer.
                //     - DO NOT improve or refactor any part of the system.
                //     - DO NOT mention patterns unless clearly and explicitly present.
                //     - DO NOT mention files unless clearly named in the input.
                //     - If no patterns are found, return exactly:
                //     <no patterns found>
                // """),
            new UserMessage("Known pattern definitions:\n" + criteriaContext),  // use patterns only
            new UserMessage("Codebase files:\n" + document)  // NOT a paragraph, just structured file content
        );

        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }

    public String findServiceBoundary(String document, List<Document> serviceBoundary){
        List<Message> messages = Arrays.asList(
            new SystemMessage("""
                You are a software architecture reasoning engine.

                Your task is to analyze the relationships between the already identified service boundaries in a system. Given:

                - A list of service boundaries (each representing a distinct logical or functional unit)
                - The source code content from which they were derived

                You must:
                - Identify **direct relationships**: explicit interactions such as function calls, REST API calls, shared interfaces, or shared repositories between services.
                - Identify **indirect relationships**: shared dependencies (e.g., common database tables, utility classes), implicit coupling (e.g., shared domain models, configuration files), or inferred usage patterns.
                - Clearly state **which boundaries** are related, and the **type of relationship** (direct or indirect).
                - Base all relationships **only on what is found in the provided file content**.

                Return the relationships as a list in the following format:
                - [BoundaryA] → [BoundaryB]: [Direct|Indirect] - [Reason]

                Only include relationships that are **explicitly evidenced** in the document. Do not speculate or infer anything not grounded in the given code.
                """),
                new UserMessage("Service Boundaries already identified found:\n" + serviceBoundary),
                new UserMessage("File content found:\n" + document)
        );

        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }

    public String findCriteria(String antipatterns, String pattern, String dependencies, String serviceBoundaries, List<Document> criteria) {
//         String dependencyContext = categorizedDependencies.entrySet().stream()
//             .map(entry -> entry.getKey() + ":\n- " + String.join("\n- ", entry.getValue()))
//             .collect(Collectors.joining("\n\n"));

        String criteriaContext = criteria.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n---\n"));

        List<Message> messages = Arrays.asList(
            new SystemMessage("""
            You are a senior software architect performing an architectural quality assessment of a microservices-based system.

            You will be given:
            - A list of architectural patterns identified in the codebase.
            - A list of architectural anti-patterns identified in the codebase.
            - Service boundary definitions.
            - Explicitly listed dependencies used in the system.

            Your task is to:
            1. Evaluate the architecture across **six key criteria**:
                - Scalability (30%)
                - Technology Diversity (10%)
                - Resilience (20%)
                - Agility (10%)
                - Cost-Effectiveness (10%)
                - Reusability (30%)

            2. For each criterion:
                - Identify and list which matching patterns are present from the input.
                - Score the criterion (Low / Medium / High) based only on found patterns.
                - Mention any high-risk anti-patterns found that might negatively impact the criterion.
                - Give a 1–2 line explanation for the score.

            3. Output format must strictly follow this template:

            ---
            **<Criterion Name>**
            - **Detected Patterns**: [List]
            - **Detected Anti-Patterns**: [List, if any]
            - **Score**: Low / Medium / High
            - **Explanation**: [Short rationale based only on what's detected]

            Repeat for each criterion. Do not speculate. Only assess based on explicitly listed patterns, anti-patterns, service boundaries, and dependencies.

            """),
                new UserMessage("Patterns found:\n" + pattern),
                new UserMessage("Antipatterns found:\n" + antipatterns),
                new UserMessage("Dependencies found:\n" + dependencies),
                new UserMessage("ServiceBoundaries found:\n" + serviceBoundaries),
                new UserMessage("Criteria: \n" + criteriaContext)
        );

        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();
    }


    // public Report newReport(String url){
        // String[] repoId = RepoUtils.extractRepoId(url);
        // String[] parts = url.split("/");
        // String user = parts[parts.length - 2];
        // String repo = parts[parts.length - 1].replace(".git", "");
        // String repoId = vectorManager.getRepoId();

        // String dependencies = "";
        // String analysis = "";
        // String archDiagram = "";
        // String recommendations = "";
        // String architectureRec = "";
        // String boundary = "";

        // GitHubApi gitHubApi = new GitHubApi();
        // gitHubApi.buildTree(user, repo);
        // BinaryTreeNode root = gitHubApi.getTree();

        // List<ServiceBoundary> serviceBoundaries = vectorManager.getServiceBoundary();
        // List<ServiceBoundary> artifact = vectorManager

        // log.info("ENTERED RAGGGGGSERVICECSSSSSS");
        // System.out.println("ENTERED RAG SERVICES");
        // // BinaryTreeNode root = vectorManager.get();
        // List<Document> dependencies = vectorManager.getDependencies();
        // List<Document> serviceBoundaries = vectorManager.getServiceBoundary();
        // List<Document> artifact = vectorManager.getArchMap();
        // BinaryTreeNode root = vectorManager.getRoot();

        // log.info(serviceBoundaries.toString());

        // PRINT "LAYER" OF ARTIFACT IN ORIGINIAL 

        // PRINT "DISPLAY ARCHITECTER" (LIKE BRANCHING FROM ROOT)
        // archDiagram = architecture.displayArchitecture(root);

        // analysis = generateAnalysis(repoId, dependencies, boundary, archDiagram);


        // no need to similarity search :)
        // skipping generaterecommendations()


        // generateArchitecture(repo, dependencies, boundary, archDiagram, analysis);

        // Report finalReport = new Report(repoId, dependencies, analysis, archDiagram, boundary, recommendations, architectureRec);


        // return new Report();
        // try {
        // String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        // String fileName = repoId + "-" + timestamp + ".txt";
        // Path outputPath = Path.of("reports", fileName);
        // Files.createDirectories(outputPath.getParent());
        // Files.writeString(outputPath, finalReport.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // log.info("Report saved to {}", outputPath.toAbsolutePath());

        // } catch (IOException e) {
        // log.error("Failed to save report", e);
        // }

        // log.info("\n{}", finalReport.toString());

        // log.info("Total report generation time: {}ms and in {}min", System.currentTimeMillis() - startTime, (totalTime / 6000 ));

        // return finalReport;

    // }
    

    // public Report getReport(String url) {
    //     String[] repoId = RepoUtils.extractRepoId(url);
    //     String[] parts = url.split("/");
    //     String user = parts[parts.length - 2];
    //     String repo = parts[parts.length - 1].replace(".git", "");
    
    //     String dependencies = "";
    //     String analysis = "";
    //     String archDiagram = "";
    //     String recommendations = "";
    //     String architectureRec = "";
    //     String boundary = "";
    
    //     long startTime = System.currentTimeMillis();
    //     long totalTime = 0;
    //     BinaryTreeNode ingestRepo = null;
    
    //     try {
    //         log.info("Starting ingestion for: {}", url);
    //         long stepStart = System.currentTimeMillis();
    //         ingestRepo = gitHubApi.inspectRepo(url);
    //         log.info("Ingestion complete in {}ms", System.currentTimeMillis() - stepStart);
    //         totalTime += (System.currentTimeMillis() - stepStart);

    //     } catch (Exception e) {
    //         log.error("Ingestion failed", e);
    //     }
    
    //     // Map<String, String> codeFiles = gitHubApi.extractCode(ingestRepo);

    //     List<ServiceBoundaryUtils.ServiceBoundary> artifacts = serviceBoundary.extractCode(codeFiles);
    //     ServiceBoundaryUtils.ArchitectureMap archMap = serviceBoundary.fallback(artifacts);

    //     archMap.printLayers();

    //     String diagram = serviceBoundary.generateBoundaryContextDiagram(archMap);
    //     System.out.println(diagram);

        // try {
        //     long stepStart = System.currentTimeMillis();
        //     dependencies  = architecture.getDependency(user, repo);
        //     log.info("Dependency extraction completed in {}ms", System.currentTimeMillis() - stepStart);
        //     totalTime += (System.currentTimeMillis() - stepStart);
        // } catch (Exception e) {
        //     dependencies = "Error extracting dependencies.";
        //     log.error("Dependency extraction failed", e);
        // }
        // try {
        //     ingestionService.ingestJson();
        //     log.info("JSON ingestion completed for file: {}", repoId);
        //     long stepStart = System.currentTimeMillis();
        //     boundary = generateBoundary(repoId, ingestRepo);
        //     log.info("Service boundary generated in {}ms", System.currentTimeMillis() - stepStart);
        //     totalTime += (System.currentTimeMillis() - stepStart);
        // } catch (Exception e) {
        //     boundary = "Error identifying service boundary.";
        //     log.error("Service boundary extraction failed", e);
        // }
        // try {
        //     long stepStart = System.currentTimeMillis();
        //     BinaryTreeNode root = gitUtils.gitHubTree(url);
        //     archDiagram = architecture.displayArchitecture(root);

        //     System.out.println(root);
        //     System.out.println(archDiagram);
        //     log.info("Architecture diagram built in {}ms", System.currentTimeMillis() - stepStart);
        //     totalTime += (System.currentTimeMillis() - stepStart);
        // } catch (Exception e) {
        //     archDiagram = "Error displaying architecture.";
        //     log.error("Architecture diagram generation failed", e);
        // }
        // try {
        //     long stepStart = System.currentTimeMillis();
        //     analysis = generateAnalysis(repoId, dependencies, boundary, archDiagram);
        //     log.info("Analysis complete in {}ms", System.currentTimeMillis() - stepStart);
        //     totalTime += (System.currentTimeMillis() - stepStart);
        // } catch (Exception e) {
        //     log.error("Failed to ingest JSON before answering:", e);
        //     return "An error occurred while ingesting the JSON context. Please check the file and try again.";
        // }
        // try {
        //     log.info("Searching vector store for repoId: {}", repoId);
        //     docs = vectorManager.getDocuments();
        //     analysis = "Error running analysis.";
        //     log.error("Analysis generation failed", e);
        // }
    
        // try {
        //     long stepStart = System.currentTimeMillis();
        //     recommendations = generateRecommendations(repoId, dependencies, boundary, archDiagram, analysis);
        //     log.info("Recommendations generated in {}ms", System.currentTimeMillis() - stepStart);
        //     totalTime += (System.currentTimeMillis() - stepStart);
        // } catch (Exception e) {
        //     log.error("Error during similarity search", e);
        //     return "An error occurred while searching for relevant context. Please try again later.";
        // }

        // if (docs == null || docs.isEmpty()) {
        //     return "No relevant context found.";
        // }

        //     recommendations = "Error providing recommendations.";
        //     log.error("Recommendation generation failed", e);
        // }
    
        // try {
        //     long stepStart = System.currentTimeMillis();
        //     architectureRec = generateArchitecture(repoId,dependencies, boundary, archDiagram, analysis);
        //     log.info("Refactored architecture generated in {}ms", System.currentTimeMillis() - stepStart);
        //     totalTime += (System.currentTimeMillis() - stepStart);
        // } catch (Exception e) {
        //     architectureRec = "Error generating refactored architecture.";
        //     log.error("Refactored architecture generation failed", e);
        // }
    
    
        // // Report finalReport = new Report(repoId, dependencies, analysis, archDiagram, boundary, recommendations, architectureRec);

        // try {
        // String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        // String fileName = repoId + "-" + timestamp + ".txt";
        // Path outputPath = Path.of("reports", fileName);
        // Files.createDirectories(outputPath.getParent());
        // Files.writeString(outputPath, finalReport.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // log.info("Report saved to {}", outputPath.toAbsolutePath());

        // } catch (IOException e) {
        // log.error("Failed to save report", e);
        // }


        // log.info("\n{}", finalReport.toString());
        // log.info("Total report generation time: {}ms and in {}min", System.currentTimeMillis() - startTime, (totalTime / 6000 ));

        // return finalReport;
    // }
    
// }

    public String generateAnalysis(String repoId, List<Document> dependencies, Document archDiagram, List<Document> criteria, List<Document> repo) {
        // String filter = "repo == '" + repoId + "'";
    
        // List<Scoring> criteria = jsonUtils.loadScoringJson(Paths.get("doc/msa-scoring.json"));
        // List<Pattern> allPatterns = jsonUtils.loadPatternsJson(Paths.get("doc/msa-patterns.json"));
    
        // List<Document> jsonLists = ....;
      
        // List<Document> docs = vectorStore.similaritySearch(
        //     SearchRequest.builder()
        //         .query("Summarize architecture patterns and anti-patterns for microservices")
        //         .filterExpression(filter)
        //         .topK(15)
        //         .build()
        // );

        // List<Document> docs = vectorManager.getDocuments();
        List<Document> docs  = repo;
        // log.info(docs.toString());
        // docs.addAll(dependencies);
        // docs.add(archDiagram);
        // docs.addAll(criteria);

        if (docs.isEmpty()) {
            return "Insufficient repository context found for generating analysis.";
        }
    
        // String repoContext = docs.stream()
        //     .map(Document::getText)
        //     .limit(10)
        //     .collect(Collectors.joining("\n---\n"));
    
       
        // String lowerDeps = dependencies.toLowerCase();
        // String lowerArch = archDiagram.toLowerCase();
        // String lowerBoundary = boundary.toLowerCase();

        // List<Pattern> relevantPatterns = allPatterns.stream()
        //     .filter(p ->
        //         Stream.of(lowerDeps, lowerArch, lowerBoundary).anyMatch(text ->
        //             text.contains(p.getName().toLowerCase())
        //         )
        //     ).collect(Collectors.toList());
    
        // String shortPatternSummaries = relevantPatterns.stream()
        //     .map(p -> "- " + p.getName() + ": " + p.getDescription())
        //     .collect(Collectors.joining("\n"));
    
        // String scoringContext = criteria.stream()
        //     .map(c -> {
        //         StringBuilder patternMatches = new StringBuilder();
        //         if (c.getPatterns() != null && !c.getPatterns().isEmpty()) {
        //             patternMatches.append("Patterns:\n");
        //             for (String patternName : c.getPatterns()) {
        //                 allPatterns.stream()
        //                     .filter(p -> p.getName().equalsIgnoreCase(patternName))
        //                     .findFirst()
        //                     .ifPresent(p -> patternMatches
        //                         .append("- ").append(p.getName()).append(": ").append(p.getDescription()).append("\n"));
        //             }
        //         }
        //         return String.format("""
        //             Criterion: %s
        //             Description: %s
        //             Guidance: %s
        //             %s
        //             Weight: %s
        //             """, c.getName(), c.getDescription(), c.getGuidance(), patternMatches, c.getWeight());
        //     })
        //     .collect(Collectors.joining("\n---\n"));
    
      
        // String contextString = """
        //     Repository architecture context:
        //     %s
    
        //     Dependencies:
        //     %s

        //     Architecture Diagram:
        //     %s
    
        //     Relevant Architectural Patterns:
        //     %s
    
        //     Evaluation Criteria:
        //     %s
        //     """.formatted(scoringContext, dependencies, archDiagram);

        String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
    List<Message> messages = Arrays.asList(new SystemMessage("""
        You are a senior microservices architect.

        Your task is to analyze the provided software system's architecture based solely on the provided code, directory structure, and service boundaries.

        ## Important Rules:
        - DO NOT reference files that are not explicitly present in the input.
        - DO NOT assume existence of any files or configurations.
        - Each insight must point to specific file paths or code content.
        - Your recommendations must be actionable and specific.

        ## Your tasks:
        1. **Detect architectural patterns** used in the code (e.g., layered, event-driven, hexagonal).
            - For each pattern, cite specific evidence.
        2. **Summarize advantages and disadvantages** based on the identified patterns.
        3. **Evaluate each scoring criterion**:
            - Name of criterion (e.g., Modularity, Observability)
            - Score from 0–10 (with justification)
            - Strengths & weaknesses from actual code
            - Suggested improvements

        ## Output Format:

        ### Detected Patterns:
        - <Pattern Name>: <Justification based on input>

        ### Pattern-Based Summary:
        **Advantages:**
        - ...
        **Disadvantages:**
        - ...

        ### Criterion: <Criterion Name>
        - Score: <0–10>
        - Strengths: ...
        - Weaknesses: ...
        - Suggested Improvements: ...

        ### Overall Score: <0–10>
        Summary: <Brief summary of findings>
        """),
        new UserMessage(context)
    );
    
    // String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
    // List<Message> messages = Arrays.asList(new SystemMessage("""
    //         You are a senior microservices architect.
            
    //         Your task is to analyze a repository's architecture using the provided context.
            
    //         1. **Detect** which architectural patterns from the list are evident based on the architecture diagram, dependencies, and boundaries.
    //         2. For each detected pattern, give a short reason why it's present.
    //         3. Summarize advantages and disadvantages based on the chosen patterns.
    //         4. Evaluate each scoring criterion:
    //         - Describe how the system meets or fails the criterion.
    //         - Mention patterns that support or hinder this.
    //         - Suggest improvements.
    //         - Score from 0 to 10.
            
    //         At the end, compute an **overall weighted score**.
            
    //         Output format:
            
    //         ## Detected Patterns:
    //         - <Pattern 1>: <Justification>
    //         - ...
            
    //         ## Pattern-Based Summary:
    //         **Advantages:**
    //         - ...
            
    //         **Disadvantages:**
    //         - ...
            
    //         ## Criterion: <Name>
    //         - Score: <0-10>
    //         - Strengths: ...
    //         - Weaknesses: ...
    //         - Suggested Improvements: ...
            
    //         ## Overall Score: <0-10>
    //         Summary: ...
    //         """),
    //         new UserMessage(context)
    //     );
    
        ChatResponse response = chatModel.call(new Prompt(messages));
        return response.getResult().getOutput().getText();

        // return "";
    }
    
    //     List<Pattern> relevantPatterns = allPatterns.stream()
    //         .filter(p ->
    //             Stream.of(lowerDeps, lowerArch, lowerBoundary).anyMatch(text ->
    //                 text.contains(p.getName().toLowerCase())
    //             )
    //         ).collect(Collectors.toList());
    
    //     String shortPatternSummaries = relevantPatterns.stream()
    //         .map(p -> "- " + p.getName() + ": " + p.getDescription())
    //         .collect(Collectors.joining("\n"));
    
    //     String scoringContext = criteria.stream()
    //         .map(c -> {
    //             StringBuilder patternMatches = new StringBuilder();
    //             if (c.getPatterns() != null && !c.getPatterns().isEmpty()) {
    //                 patternMatches.append("Patterns:\n");
    //                 for (String patternName : c.getPatterns()) {
    //                     allPatterns.stream()
    //                         .filter(p -> p.getName().equalsIgnoreCase(patternName))
    //                         .findFirst()
    //                         .ifPresent(p -> patternMatches
    //                             .append("- ").append(p.getName()).append(": ").append(p.getDescription()).append("\n"));
    //                 }
    //             }
    //             return String.format("""
    //                 Criterion: %s
    //                 Description: %s
    //                 Guidance: %s
    //                 %s
    //                 Weight: %s
    //                 """, c.getName(), c.getDescription(), c.getGuidance(), patternMatches, c.getWeight());
    //         })
    //         .collect(Collectors.joining("\n---\n"));
    
      
    //     String contextString = """
    //         Repository architecture context:
    //         %s
    
    //         Dependencies:
    //         %s
    
    //         System Boundaries:
    //         %s
    
    //         Architecture Diagram:
    //         %s
    
    //         Relevant Architectural Patterns:
    //         %s
    
    //         Evaluation Criteria:
    //         %s
    //         """.formatted(repoContext, dependencies, boundary, archDiagram, shortPatternSummaries, scoringContext);
    
    //     List<Message> messages = Arrays.asList(
    //         new SystemMessage("""
    // You are a senior microservices architect.
    
    // Your task is to analyze a repository's architecture using the provided context.
    
    // 1. **Detect** which architectural patterns from the list are evident based on the architecture diagram, dependencies, and boundaries.
    // 2. For each detected pattern, give a short reason why it's present.
    // 3. Summarize advantages and disadvantages based on the chosen patterns.
    // 4. Evaluate each scoring criterion:
    //    - Describe how the system meets or fails the criterion.
    //    - Mention patterns that support or hinder this.
    //    - Suggest improvements.
    //    - Score from 0 to 10.
    
    // At the end, compute an **overall weighted score**.
    
    // Output format:
    
    // ## Detected Patterns:
    // - <Pattern 1>: <Justification>
    // - ...
    
    // ## Pattern-Based Summary:
    // **Advantages:**
    // - ...
    
    // **Disadvantages:**
    // - ...
    
    // ## Criterion: <Name>
    // - Score: <0-10>
    // - Strengths: ...
    // - Weaknesses: ...
    // - Suggested Improvements: ...
    
    // ## Overall Score: <0-10>
    // Summary: ...
    // """),
    //         new UserMessage(contextString)
    //     );
    
    //     ChatResponse response = chatModel.call(new Prompt(messages));
    //     return response.getResult().getOutput().getText();
    // }
    
    
    
    // public String generateRecommendations(String repoId, String dependencies, String boundary, String archDiagram, String analysis) {
    //     String filter = "repo == '" + repoId + "'";
    
    //     List<Document> docs = vectorStore.similaritySearch(
    //         SearchRequest.builder()
    //             .query("What are potential refactoring suggestions for microservice quality based on the analysis?")
    //             .filterExpression(filter)
    //             .topK(50)
    //             .build()
    //     );
    
    //     if (docs.isEmpty()) {
    //         return "No refactor suggestions available for this repository.";
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
    //                                 - If the context includes external links (e.g., images from Imgur), DO NOT include them in your response. Instead, summarize their content in text.
    //                                 - If you don’t have enough context, say: "I don’t have enough information to answer that. Please ask questions about the given repository."
    //                                 - DO NOT hallucinate.
    //                             """),
    //                             new UserMessage("Here is the context:\n" + context + "\n\nNow, based on that, answer this question:\n" + question)
    //                         )
    //         .map(Document::getText)
    //         .collect(Collectors.joining("\n---\n"));
    
    //     String contextString = """
    //         Here is the repository context:
    //         %s
    
    //         Here are the dependencies:
    //         %s
    
    //         Here is the system boundary:
    //         %s
    
    //         Here is the architecture diagram:
    //         %s
    
    //         Here is the repository analysis:
    //         %s
    //         """.formatted(context, dependencies, boundary, archDiagram, analysis);
    
    //     List<Message> messages = Arrays.asList(
    //         new SystemMessage("""
    // You are a senior software architect and microservices expert.
    
    // Based on the repository's analysis, system boundary, architecture diagram, and dependencies, identify concrete **refactoring opportunities** to improve the following quality attributes:
    
    // - Scalability
    // - Resilience
    // - Technology Diversity
    // - Agility
    // - Cost-Effectiveness
    // - Reusability
    
    // Each recommendation must be **prioritized** according to scoring weights (assume higher-weighted criteria are more critical).
    
    // Output format (grouped by **Category**):
    
    // ### Category: <e.g., Scalability>
    
    // | Problem | Recommendation | Benefit | Pattern Reference |
    // |--------|----------------|---------|-------------------|
    // | ...    | ...            | ...     | ...               |
    
    // If applicable, reference architectural patterns that support the recommendation (e.g., CQRS, Event Sourcing, Service Mesh).
    
    // Be practical and specific—avoid vague guidance. Include implementation hints if relevant.
    // """),
    //         new UserMessage(contextString)
    //     );
    
    //     ChatResponse response = chatModel.call(new Prompt(messages));
    //     return response.getResult().getOutput().getText();
    // }
    

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

public String generateArchitecture(String repoId, List<Document> dependencies, Document archDiagram, Document analysis) {
    List<Document> docs = new ArrayList<>(dependencies);
    docs.add(archDiagram);
    docs.add(analysis);

        String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
    List<Message> messages = Arrays.asList(
        new SystemMessage("""
            You are a senior microservices architect.

            You will refactor the monolithic or loosely-coupled codebase described below into a modular microservices architecture.

            ## Requirements:
            - ONLY use services, files, and directories shown in the input.
            - DO NOT invent files or services.
            - Prioritize clear separation of services with domain logic.
            - Group controllers, services, repositories within each new microservice.

            ## Output Format:

            ### 📦 Refactored Architecture (ASCII Tree)
            Begin with a clean ASCII tree like:
            ```plaintext
            microservices-architecture/
            ├── gateway-service/
            │   └── src/main/java/...
            ├── user-service/
            │   └── src/main/java/...
            ├── discovery-server/
            │   └── src/main/java/...
            └── event-bus/
                └── kafka-config.yml
            ```

            ### 🔗 Connections:
            List the inter-service communication links in this format:
            - `order-service` → `payment-service` (via REST)
            - `user-service` → `event-bus` (via Kafka)

            ### 🔍 File Refactor Mapping:
            Show mapping from original to refactored structure:
            - `monolith/src/com/example/UserController.java` → `user-service/src/main/java/com/example/UserController.java`
            - ...

            ### 🧠 Explanation:
            Briefly explain:
            - Purpose of each service
            - Where gateway/config/event/discovery were added
            - How this improves modularity and scalability

            DO NOT add unrelated generic text or assumptions.
        """),
        new UserMessage(context)
    );

    ChatResponse response = chatModel.call(new Prompt(messages));
    String output = response.getResult().getOutput().getText();

    String asciiDiagram = output.replaceAll("(?s).*?```(?:plaintext)?\\s*(.*?)\\s*```.*", "$1").trim();

    if (!asciiDiagram.contains("├──") && !asciiDiagram.contains("└──")) {
        System.out.println("Warning: No valid ASCII diagram found in output: " + output);
        return output;
    }

    // String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
    // List<Message> messages = Arrays.asList(
    //     new SystemMessage("""
    //         You are a senior microservices architect.

    //         Your task is to **refactor** a monolithic or loosely-coupled codebase into a robust, scalable **microservices architecture**, based on the user's input (directory structure, code content, and analysis).

    //         ---

    //         🎯 **Your response must begin with a clean, well-formatted ASCII tree diagram.**

    //         🔒 The first output must only include the refactored structure in ASCII format (see example). Do not include any explanation before the diagram.

    //         ✅ After the diagram, you must provide a brief explanation of:
    //         - The purpose of each service
    //         - Where config/gateway/bus/discovery were added
    //         - How this improves modularity and scalability

    //         ---

    //         ### 🔧 Input
    //         The user will provide:
    //         - Directory structure (original project)
    //         - Brief project analysis
    //         - Repository files and contents

    //         ---

    //         ### 📦 Deliverables (strict order):
    //         1. A **refactored architecture in ASCII tree format** that includes:
    //            - `gateway-service`
    //            - `config-server`
    //            - `event-bus` (Kafka/RabbitMQ)
    //            - `discovery-server` (Eureka/Consul)
    //            - Logically grouped services like `user-service`, `order-service`, etc.

    //         2. The structure must include only relevant subdirectories (e.g. `src/main/java/...`).

    //         3. **Each service must be separated**, and event producers/consumers should be noted.

    //         4. After the diagram, provide a **short explanation only**.

    //         ---

    //         ### 📋 Example format:
    //         ```plaintext
    //         microservices-architecture/
    //         ├── gateway-service/
    //         │   └── src/main/java/com/example/gateway/GatewayApplication.java
    //         ├── user-service/
    //         │   └── src/main/java/com/example/user/UserServiceApplication.java
    //         ├── config-server/
    //         │   └── src/main/java/com/example/config/ConfigServerApplication.java
    //         ├── discovery-server/
    //         │   └── src/main/java/com/example/discovery/DiscoveryServerApplication.java
    //         ├── event-bus/
    //         │   └── kafka-config.yml
    //         ```
    //         > Only include key folders like `src/main/java/...` and main service components.
    //         > Do NOT explain anything before the diagram.
    //     """),
    //     new UserMessage(context)
    // );

    // ChatResponse response = chatModel.call(new Prompt(messages));
    // String output = response.getResult().getOutput().getText();

    // String asciiDiagram = output.replaceAll("(?s).*?```(?:plaintext)?\\s*(.*?)\\s*```.*", "$1").trim();

    // if (!asciiDiagram.contains("├──") && !asciiDiagram.contains("└──")) {
    //     System.out.println("Warning: No valid ASCII diagram found in output: " + output);
    //     return output;
    // }

    return asciiDiagram;
}
}



//     public String generateArchitecture(String repoId, List<Document> dependencies, Document archDiagram, Document analysis) {
//         // String contextString = """
//         //     Repository ID: %s
    
//         //     Dependency Information:
//         //     %s
    
//         //     Identified Service Boundaries:
//         //     %s
    
//         //     Current Architecture Diagram:
//         //     %s
    
//         //     Architectural Analysis:
//         //     %s
    
//         //     Based on this, refactor the codebase to improve clarity, separation of concerns, and modern best practices.
//         //     """.formatted(repoId, dependencies, archDiagram, analysis);

//         List<Document> docs = dependencies;
//         docs.add(archDiagram);
//         docs.add(analysis);
    
//         String context = docs.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
//         List<Message> messages = Arrays.asList(new SystemMessage("""

//             You are a senior microservices architect.

//             Your task is to **refactor** a monolithic or loosely-coupled codebase into a robust, scalable **microservices architecture**, based on the user's input (directory structure, code content, and analysis).

//             ---

//             🎯 **Your response must begin with a clean, well-formatted ASCII tree diagram.**

//             🔒 The first output must only include the refactored structure in ASCII format (see example). Do not include any explanation before the diagram.

//             ✅ After the diagram, you may provide a brief explanation of:
//             - The purpose of each service
//             - Where config/gateway/bus/discovery were added
//             - How this improves modularity and scalability

//             ---

//             ### 🔧 Input
//             The user will provide:
//             - Directory structure (original project)
//             - Brief project analysis
//             - Repository files and contents

//             ---

//             ### 📦 Deliverables (strict order):
//             1. A **refactored architecture in ASCII tree format** that includes:
//             - `gateway-service`
//             - `config-server`
//             - `event-bus` (Kafka/RabbitMQ)
//             - `discovery-server` (Eureka/Consul)
//             - Logically grouped services like `user-service`, `order-service`, etc.

//             2. The structure must include only relevant subdirectories (e.g. `src/main/java/...`).

//             3. **Each service must be separated**, and event producers/consumers should be noted.

//             4. After the diagram, provide a **short explanation only**.

//             ---

//             ### 📋 Example format:
//             ```plaintext
//             microservices-architecture/
//             ├── gateway-service/
//             │   └── src/main/java/com/example/gateway/GatewayApplication.java
//             ├── user-service/
//             │   └── src/main/java/com/example/user/UserServiceApplication.java
//             ├── config-server/
//             │   └── src/main/java/com/example/config/ConfigServerApplication.java
//             ├── discovery-server/
//             │   └── src/main/java/com/example/discovery/DiscoveryServerApplication.java
//             ├── event-bus/
//             │   └── kafka-config.yml
                    
//         """),
//         new UserMessage(context)
//         );
    
//         ChatResponse response = chatModel.call(new Prompt(messages));
//         String output = response.getResult().getOutput().getText();
    
//         if (!output.contains("├──") && !output.contains("└──")) {
//             System.out.println("Warning: No valid ASCII diagram found in output: " + output);
//         }
    
//         return output;
//     }
// }
    
//     You are a senior microservices architect.
    
            //     Your task is to **refactor** a monolithic or loosely-coupled codebase into a robust, scalable **microservices architecture** using the provided directory structure and code relationships.
    
            //     **Input**:
            //         The user will provide:
            //         - A directory structure (of the original project)
            //         - A brief analysis of the program
            //         - The whole repositories files and contents

            //     **Deliverables**:
            //     1. A refactored architecture in **ASCII tree diagram format**, based on directory structure.
            //     2. Introduce and place:
            //        - API Gateway
            //        - Config Server
            //        - Event Bus (Kafka / RabbitMQ)
            //        - Discovery Server (Eureka/Consul if applicable)
            //     3. Group services logically (e.g., users, payments, orders).
            //     4. Indicate which services are producers/consumers of events (if any).
            //     5. Optimize for **modularity, config centralization, and fault tolerance**.
            //     6. After the diagram, briefly explain:
            //        - The new structure
            //        - Where config/gateway/bus/discovery were added
            //        - How it improves maintainability and scalability
                
            //     **Example output** (you must follow a similar format):
            //     ```plaintext
            //     └── gateway-service
            //         └── src
            //             └── main
            //                 └── java
            //                     └── com.example.gateway
            //                         └── GatewayApplication.java
            //     └── user-service
            //         └── src
            //             └── main
            //                 └── java
            //                     └── com.example.user
            //                         └── UserServiceApplication.java
            //     └── order-service
            //         └── src/...
            //     └── config-server
            //         └── src/...
            //     └── discovery-server
            //         └── src/...
            //     └── event-bus
            //         └── kafka-config.yml
            //     ```
            //     > Include only key folders like `src/main/java/...` and important service names.
            //     > Group related functionality under clearly named services.

            //     7. Create a **complete and logically grouped refactored directory structure** in ASCII tree format.
            //     - Include separate folders for each microservice (e.g., `user-service`, `visit-service`, `vet-service`, `gateway-service`, `config-server`, `discovery-server`, etc.)
            //     - Show only meaningful directories and files relevant to service structure (e.g., no `.git` or IDE configs unless architecturally relevant).
            //     - Avoid redundancy from the original structure — show a cleaned-up, modular layout optimized for a microservices deployment.

            //     The goal is to clearly separate responsibilities across services and infrastructure components in a cloud-native, scalable architecture.
            //     You are expected to infer service roles based on file and directory names and suggest reasonable separation.
            // """),
    
    

    // public String generateBoundary(String repoId, BinaryTreeNode root) {
    //     try {
    //         Map<String, String> files = gitHubApi.extractCode(root);
    //         if (files == null || files.isEmpty()) {
    //             return "No source files found for repository: " + repoId;
    //         }
    
    //         List<ServiceBoundaryUtils.ServiceBoundary> artifacts = serviceBoundary.extractCode(files);
    //         if (artifacts.isEmpty()) {
    //             return "No service boundary artifacts extracted from repository: " + repoId;
    //         }
    
    //         String formattedCode = serviceBoundary.format(artifacts);
    
    //         List<Message> messages = List.of(
    //             new SystemMessage("""
    //                 You are a software architecture expert.
    //                 Based on the following code snippets and identified layers,
    //                 identify logical service boundaries and their responsibilities.
    //                 Return ONLY a JSON object without any explanation or formatting.
    //                 If you cannot, return an empty JSON object {}.
    //             """),
    //             new UserMessage("Code context:\n" + formattedCode)
    //         );
    
    //         ChatResponse response = chatModel.call(new Prompt(messages));
    //         String output = response.getResult().getOutput().getText();
    //         String jsonPart = serviceBoundary.extractJson(output);
    
           
    //         if (jsonPart == null || jsonPart.isBlank() || jsonPart.trim().equals("{}")) {
    //             return generateFallback("LLM could not extract boundaries.", artifacts);
    //         }
    
    //         try {
    //             new ObjectMapper().readTree(jsonPart); 
    //         } catch (JsonProcessingException e) {
    //             return generateFallback("LLM returned invalid JSON:\n" + jsonPart, artifacts);
    //         }
    
    //         String asciiDiagram = serviceBoundary.generateAsciiDiagramFromLLMJson(jsonPart);
    //         ArchitectureMap map = serviceBoundary.parseResponsbilities(asciiDiagram);
    //         String boundaryContext = serviceBoundary.generateBoundaryContextDiagram(map);
    
    //         return asciiDiagram + "\n\n---\n\n" + boundaryContext + "\n\n---\n\n" + jsonPart;
    
    //     } catch (Exception e) {
    //         return "An unexpected error occurred: " + e.getMessage();
    //     }
    // }
    
    
    // private String generateFallback(String string, List<ServiceBoundary> artifacts) {
    //         ArchitectureMap fallbackMap = serviceBoundary.fallback(artifacts);
    //         String fallbackDiagram = serviceBoundary.generateBoundaryContextDiagram(fallbackMap);
    //         fallbackMap.printLayers();
        
    //         return string + "\n\n[FALLBACK DIAGRAM]\n" + fallbackDiagram;
    //     }
        

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
// }
