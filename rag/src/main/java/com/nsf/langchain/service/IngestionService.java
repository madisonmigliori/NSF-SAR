package com.nsf.langchain.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.apache.tomcat.util.json.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.nsf.langchain.git.GitHubApi;
import com.nsf.langchain.git.token.GetToken;
import com.nsf.langchain.service.EmbeddingUtils.EmbedDecision;
// import com.nsf.langchain.utils.GitUtils;
import com.nsf.langchain.utils.RepoUtils;
import com.nsf.langchain.utils.TextUtils;

import jakarta.annotation.PostConstruct;

import com.nsf.langchain.utils.PdfUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.git.GitHubApi;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final ChromaVectorStoreManager vectorManager;
    
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public IngestionService(ChromaVectorStoreManager vectorManager) {
        this.vectorManager = vectorManager;
    }

    // @Autowired
    // public VectorStore vectorStore;

    @Value("${app.repos-dir}")
    private String baseDir;

    @Value("${app.allowed-extensions}")
    private String allowedExtensions;

    // @PostConstruct
    // public void initIngestion() throws IOException {
    //     // String repoId = "base";
    //     Path scoringPath = Paths.get("doc", "msa-scoring.json");
    //     Path patternsPath = Paths.get("doc", "msa-patterns.json");
    //     Path dependencyPath = Paths.get("resources/static", "dependency-catergories.json");

    //     ingestMsaScoringJson(scoringPath);
    //     ingestMsaPatternsJson(patternsPath);   
    //     ingestDependencyCategory(dependencyPath);
    // }
    
    public void ingestRepo(String gitUrl) throws Exception {
        String[] repoInfo = RepoUtils.extractRepoId(gitUrl);

        log.info("STARTING LOGGING JSON INFORMATION");

        ingestMsaScoringJson();
        ingestMsaPatternsJson();   
        ingestDependencyCategory();

        log.info("ANTIPATTERN" + vectorManager.getAntiPatternsJson().toString());
        log.info("PATTERNS" +vectorManager.getPatternsJson().toString());
        log.info("CRITERIA" + vectorManager.getCriteriaJson().toString());
        log.info("DEPENDENCIES" + vectorManager.getDependencyJson().toString());
        // List<Document> docs = vectorManager.getPatternsJson();
        // vectorManager.getPatternsJson();
        List<Document> docs = vectorManager.getDependencyJson();
        // for(Document doc:docs){
        //     log.info(doc.getText().toString());
        // }

        log.info("FINISHING LOGGING JSON INFORMATION");
        String user = repoInfo[0];
        String repo = repoInfo[1];
        String repoId = user + "-" + repo;
        vectorManager.setRepoId(repoId);
        // vectorManager.useOrCreateCollection(repoId);

        log.info("Starting building tree: {}", repoId);

        GitHubApi gitRepo = new GitHubApi();
        gitRepo.buildTree(user, repo);
        BinaryTreeNode treeRoot = gitRepo.getTree();
        vectorManager.setRoot(treeRoot);
        // log.info(treeRoot.getName());
        // gitRepo.printTree(treeRoot, repoId);
        // log.info(treeRoot.toString());
        log.info("Completed building tree: {}", repoId);

        log.info("Starting documenting docs :((((( ): {}", repoId);

        List<Document> doc = new ArrayList<>();
        List<Document> docPaths = new ArrayList<>();
        embedTreeConcurrent(treeRoot, doc, docPaths, repoId, vectorManager);
        vectorManager.setDocuments(doc);
        vectorManager.setDocumentPath(docPaths);
        log.info("Finished documenting docs :((((( ): {}", repoId);
        // List<Document> serviceBoundaries = vectorManager.getServiceBoundary();
        // List<Document> artifact = vectorManager.getArchMap();
        // log.info(artifact.toString());

    }

    // ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private void embedTreeConcurrent(BinaryTreeNode node, List<Document> file, List<Document> filePath, String repoId, ChromaVectorStoreManager vectorManager) throws Exception {
        if (node == null) return;

        if(node.getType().equals("file")){
            log.info("IS TYPE FILE" + node.getName());
            String fileName = node.getName();
            log.info(node.getUrl());
            Map<String, Object> metadata = Map.of(
                "repoId", repoId,
                "name", node.getName(),
                "type", node.getType(),
                "size", node.getSize(),
                "url", node.getUrl()
            );    

        EmbeddingUtils.EmbedDecision decision;

        if(node.getType().equals("file")){
            decision = EmbeddingUtils.shouldEmbed(node, vectorManager);
            log.info("MADE IT" + decision.name());
        }else{
            log.info("MADE IT SKIP FILE" );
            decision = EmbedDecision.SKIP_FILE;
        }
        switch (decision) {
            case EMBED_DIRECTLY -> {file.add(new Document(node.getContent(), metadata)); filePath.add(new Document(node.getUrl()));}
            case EMBED_CHUNKED -> {
                List<String> chunks = chunkLargeFile(node.getContent(), 800, 100);
                for (String chunk : chunks) {
                    file.add(new Document(chunk, metadata));
                }
            }
            case SUMMARIZE_ONLY -> file.add(new Document("Skipped large file ", metadata));
            case SKIP_FILE -> {}
        }

        }else {
            log.info("IS TYPE DIR" + node.getName());
            filePath.add(new Document(node.getUrl()));

            if(node.getChildren() != null) {
                for (BinaryTreeNode child : node.getChildren()) {
                    embedTreeConcurrent(child, file, filePath, repoId, vectorManager);
                }
            }
        }
    }

    public static List<String> chunkLargeFile(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
            if (end == text.length()) break;
        }
        return chunks;
    }

    public void ingestMsaPatternsJson() throws IOException {
        log.info("Starting MSA pattern JSON ingestion for file: ingestmascoringjson");
        // InputStream inputStream = new ClassPathResource("rag/doc/msa-scoring.json").getInputStream();
        InputStream scoring = getClass().getClassLoader().getResourceAsStream("doc/msa-patterns.json");

        JsonNode node = mapper.readTree(scoring);
        // log.info(node.toString());

        JsonNode patternsArray = node.get("patterns");
        System.out.println(patternsArray.isArray());
        if (patternsArray != null && patternsArray.isArray()) {
            for (JsonNode pattern : patternsArray) {

                String text = "Name: " + pattern.get("name").asText() + " Description: " + pattern.get("description").asText()+ "Advantage: " + pattern.get("advantage").asText() 
                    + "Disadvantage" + pattern.get("disadvantage").asText() + " Common Implementations" + pattern.get("common implementations").asText();
                Map<String, Object> metadata = Map.of( 
                    "Name: ", pattern.get("name"),
                    "Type: ",  "Pattern Type"
                );
                Document d = new Document(text, metadata);
                // log.info(d.toString());
                vectorManager.setPatternsJson(d);
            }
        }

        JsonNode antipatternsArray = node.get("anti-patterns");
        log.info(antipatternsArray.asText());
        if (antipatternsArray != null && antipatternsArray.isArray()) {
            // log.info("PASSED");
            // log.info("stepping into anti");
            for (JsonNode antipatterns : antipatternsArray) {
                log.info("stepping into anti");
                String text = "Name: " + antipatterns.get("name").asText() +
              " Description: " + antipatterns.get("description").asText() +
            //   " Advantage: " + antipatterns.get("advantage").asText() +
              " Impact: " + antipatterns.get("impact").asText() +
              " Severity: " + antipatterns.get("severity").asText();
                Map<String, Object> metadata = Map.of( 
                    "Name: ", antipatterns.get("name"),
                    "Type: ",  "AntiPattern Type"
                );
                Document d = new Document(text, metadata);
                // log.info(d.getMetadata().toString());
                vectorManager.setAntiPatternsJson(d);            
            }
        }
        // String text = "Name: " + node.get("name") + " Description: " + node.get("description") + "Advantage: " + node.get("advantage") 
        //     + "Disadvantage" + node.get("disadvantage") + " Common Implementations" + node.get("common implementations");
        // vectorManager.addToDocuments(new Document(text));
        log.info("Finished MSA scoring");
    }

    public void ingestMsaScoringJson() throws IOException {
        log.info("Starting MSA scoring JSON ingestion for file: ingestmsapattern");
        InputStream scoring = getClass().getClassLoader().getResourceAsStream("doc/msa-scoring.json");
        JsonNode node = mapper.readTree(scoring);
        
        JsonNode scoringArray = node.get("criteria");
        if (scoringArray != null && scoringArray.isArray()) {
            for (JsonNode score : scoringArray) {
                String text = "Name: " + score.get("name").asText() + " Description: " + score.get("description").asText() + "Guidance: " + score.get("guidance").asText() 
                    + "Patterns" + score.get("patterns").asText() + " Weight: " + score.get("weight").asText();
                Map<String, Object> metadata = Map.of( 
                    "Type: ",  "Criteria Type",
                    "Name: ", score.get("name")
                );
                vectorManager.setCriteriaJson(new Document(text, metadata));
            }
        }
        log.info("Finished MSA patterns");
    }

    public void ingestDependencyCategory() throws IOException {
        log.info("Starting dependency JSON ingestion for file: IngestionDependencyCategory");
        
        InputStream dependency = getClass().getClassLoader().getResourceAsStream("static/dependency-catergories.json");

        // Parse the file using Jackson ObjectMapper
        JsonNode root = mapper.readTree(dependency);

        // Loop through all categories in the root JSON
        root.fieldNames().forEachRemaining(category -> {
            JsonNode items = root.get(category);
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    String name = item.asText();

                    // Build the document content
                    String text = "Type: " + category + ", Name: " + name;

                    // Create metadata
                    // Map<String, Object> metadata = Map.of(
                    //     "Type", category,
                    //     "Name", name
                    // );

                    vectorManager.setDependenciesJson(new Document(text));
                }
            }
        });
        log.info("Finished dependency");
    }


    // public void ingestGitRepoAPI(String gitUrl) throws Exception {
    //     GitHubApi repo = new GitHubApi();

    //     String repoId = RepoUtils.extractRepoId(gitUrl);
    //     BinaryTreeNode root = repo.inspectRepo(gitUrl);
    //     List<String>filePaths = repo.flattenFilePaths(root);

    //     String[] parts = gitUrl.split("/");
    //     String user = parts[parts.length - 2];
    //     String repoName = parts[parts.length - 1].replace(".git", "");

    //     for (String path : filePaths) {
    //         String content = repo.fetchFileContent(user, repoName, path);
    //         if (content != null && content.length() >= 20) {
    //             List<String> chunks = TextUtils.chunkText(content, 1000, 200);
    //             for (String chunk : chunks) {
    //                 Document doc = Document.builder()
    //                         .text(chunk)
    //                         .metadata("file", path)
    //                         .metadata("repo", repoId)
    //                         .build();
    //                 vectorStore.add(List.of(doc));
    //             }
    //         }
    //     }
    // }

    // public void debugVectorStoreDocs(){
    //     log.info("Debug: Listing all the docs in vector store.");

    //     List<Document> allDocs = vectorStore.similaritySearch("");

    //     if(allDocs == null || allDocs.isEmpty()){
    //         log.warn("Vector store is empty");
    //         return;
    //     }

    //     for(int i = 0; i < allDocs.size(); i++){
    //         Document doc = allDocs.get(i);
    //         log.info("Doc {}: \nText: {}\nMetadata: {}\n", i + 1, preview(doc.getText()), doc.getMetadata());
    //     }

    //     log.info("Doc size: {}", allDocs.size());

    // }

    // private String preview(String text){
    //     return text.length() > 200 ? text.substring(0, 200) +  "..." : text;

    // }



    // private boolean hasAllowedExtension(Path path) {
    //     String ext = com.google.common.io.Files.getFileExtension(path.toString()).toLowerCase();
    //     return List.of(allowedExtensions.split(",")).contains("." + ext);
    //     }
    //     JsonNode antipatternsArray = node.get("patterns");
    //     if (antipatternsArray != null && antipatternsArray.isArray()) {
    //         for (JsonNode antipatterns : antipatternsArray) {
    //             String text = "Name: " + antipatterns.get("name") + " Description: " + antipatterns.get("description") + "Advantage: " + antipatterns.get("advantage") 
    //                 + "Disadvantage" + antipatterns.get("disadvantage") + " Common Implementations" + antipatterns.get("common implementations");
    //             Map<String, Object> metadata = Map.of( 
    //                 "Type: ",  "AntiPattern Type",
    //                 "Name: ", antipatterns.get("name")
    //             );
    //             vectorManager.addToDocuments(new Document(text, metadata));
    //         }
    //     }
        // String text = "Name: " + node.get("name") + " Description: " + node.get("description") + "Advantage: " + node.get("advantage") 
        //     + "Disadvantage" + node.get("disadvantage") + " Common Implementations" + node.get("common implementations");
        // vectorManager.addToDocuments(new Document(text));
    // }


   
    // private void processFile(String repoId, Path repoFolder, Path path) {
    //     log.info("Processing file: {}", path);
    
    //     String text;
    //     try {
    //         text = Files.readString(path);
    //         log.info("Successfully read file: {}", path);
    //     } catch (IOException e) {
    //         log.warn("Skipping unreadable file in '{}': {} ({})", baseDir, path, e.getMessage());
    //         return;
    //     }
    
     
    //     List<String> chunks = TextUtils.chunkText(text, 1000, 200)
    //         .stream()
    //         .filter(chunk -> chunk != null && chunk.trim().length() >= 20)
    //         .toList();
    
    //     log.info("Total chunks for '{}': {}", path.getFileName(), chunks.size());
    
    //     if (chunks.isEmpty()) {
    //         log.warn("No valid chunks found for '{}'. Possible reasons: file too small, chunking logic issue, or overly strict length filter.", path.getFileName());
    //     }
    
    //     String safePath = repoFolder.relativize(path).toString().replaceAll("[/\\\\]", "_");
    
    //     for (int i = 0; i < chunks.size(); i++) {
    //         String chunk = chunks.get(i);
    //         Document doc = Document.builder()
    //             .text(chunk)
    //             .metadata("file", safePath)
    //             .metadata("repo", repoId)
    //             .build();
    
    //         try {
    //             vectorStore.add(List.of(doc));
    //             log.info("Stored chunk {} of '{}'", i, safePath);
    //         } catch (Exception e) {
    //             log.error("Failed to store doc for chunk {} of '{}': {}", i, safePath, e.getMessage());
    //         }
    //     }
    // }

    //  public void ingestJson() {

    //     Path basePath = Paths.get(System.getProperty("user.dir"))
    //                  .resolve("doc/");
    //     // log.info("Starting MSA patterns JSON ingestion for file: {}", path);
    //     // Path basePath = Paths.get(System.getProperty("user.dir"))
    //     //              .resolve("src/main/java/com/nsf/langchain/ModelTraining/Criteria");

    //     // Path optimizeScore = basePath.resolve("OptimizeScoring.json");

    //     Path patternCrit = basePath.resolve("msa-patterns.json");
    //     Path optimizeScore = basePath.resolve("msa-scoring.json");

    //     try{
    //         JsonNode optimize = objectMapper.readTree(optimizeScore.toFile());
    //         JsonNode pattern = objectMapper.readTree(patternCrit.toFile());

    //         interpretOptimizingJson(optimize);
    //         interpretPatternsJson(pattern);

    //     } catch (IOException e) {
    //         log.error("Failed to ingest JSON '{}': {}", e.getMessage());
    //     }    

    //     // Path optimizeScore = Paths.get("/Users/cara/Documents/NSF-SAR/rag/src/main/java/com/nsf/langchain/ModelTraining/Criteria/OptimizeScoring.json");
    //     // Path patternCrit = Paths.get("/Users/cara/Documents/NSF-SAR/rag/src/main/java/com/nsf/langchain/ModelTraining/Criteria/Patterns.json");

    //     ObjectMapper objectMapper = new ObjectMapper();
        
    //     try{
    //         JsonNode optimize = objectMapper.readTree(optimizeScore.toFile());
    //         JsonNode pattern = objectMapper.readTree(patternCrit.toFile());

    //         interpretOptimizingJson(optimize);
    //         interpretPatternsJson(pattern);

    //     } catch (IOException e) {
    //         log.error("Failed to ingest JSON '{}': {}", e.getMessage());
    //     }    
    // }

    // public void interpretPatternsJson(JsonNode node){
    //     JsonNode patternsArray = node.get("patterns");
    //     if (patternsArray != null && patternsArray.isArray()) {
    //         for (JsonNode pattern : patternsArray) {
    //             String text = "Name: " + pattern.get("name") + " Description: " + pattern.get("description") + "Advantage: " + pattern.get("advantage") 
    //                 + "Disadvantage" + pattern.get("disadvantage") + " Common Implementations" + pattern.get("common implementations");
    //             Map<String, Object> metadata = Map.of( 
    //                 "Type: ",  "Pattern Type",
    //                 "Name: ", pattern.get("name")
    //             );
    //             vectorManager.addToDocuments(new Document(text, metadata));
    //         }
    //     }
    //     JsonNode antipatternsArray = node.get("patterns");
    //     if (antipatternsArray != null && antipatternsArray.isArray()) {
    //         for (JsonNode antipatterns : antipatternsArray) {
    //             String text = "Name: " + antipatterns.get("name") + " Description: " + antipatterns.get("description") + "Advantage: " + antipatterns.get("advantage") 
    //                 + "Disadvantage" + antipatterns.get("disadvantage") + " Common Implementations" + antipatterns.get("common implementations");
    //             Map<String, Object> metadata = Map.of( 
    //                 "Type: ",  "AntiPattern Type",
    //                 "Name: ", antipatterns.get("name")
    //             );
    //             vectorManager.addToDocuments(new Document(text, metadata));
    //         }
    //     }
    //     // String text = "Name: " + node.get("name") + " Description: " + node.get("description") + "Advantage: " + node.get("advantage") 
    //     //     + "Disadvantage" + node.get("disadvantage") + " Common Implementations" + node.get("common implementations");
    //     // vectorManager.addToDocuments(new Document(text));
    // }

    // public void interpretOptimizingJson(JsonNode node){
    //     // String text = "Name: " + node.get("name") + " Description: "+ node.get("description") + " Guidance: " 
    //         // + " Pattern: "+ node.get("patterns") + " Weight: " + node.get("weight");
    //     // documents.add(new Document(text));
    //     // vectorManager.addToDocuments(new Document(text));

    //     JsonNode criteriaArray = node.get("criteria");
    //     if (criteriaArray != null && criteriaArray.isArray()) {
    //         for (JsonNode criteria : criteriaArray) {
    //             String text = "Name: " + criteria.get("name") + " Description: "+ criteria.get("description") + " Guidance: " 
    //                 + " Pattern: "+ criteria.get("patterns") + " Weight: " + criteria.get("weight");
    //             Map<String, Object> metadata = Map.of( 
    //                 "Type: ",  "Criteria Type",
    //                 "Name: ", criteria.get("name")
    //             );
    //             vectorManager.addToDocuments(new Document(text, metadata));
    //         }
    //     }
    // }

    
}
