package com.nsf.langchain.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import org.springframework.ai.chroma.vectorstore.ChromaApi.Collection;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;

// import com.nsf.langchain.git.GitHubApiHello;
import com.nsf.langchain.git.token.GetToken;
import com.nsf.langchain.service.EmbeddingUtils.EmbedDecision;
import com.nsf.langchain.utils.GitUtils;
import com.nsf.langchain.utils.RepoUtils;
import com.nsf.langchain.utils.TextUtils;
import com.nsf.langchain.utils.PdfUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.git.GitHubApi;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final ChromaVectorStoreManager vectorManager;

    @Autowired
    public IngestionService(ChromaVectorStoreManager vectorManager) {
        this.vectorManager = vectorManager;
    }

    // @Autowired
    // private VectorStore vectorStore;

    @Value("${app.repos-dir}")
    private String baseDir;

    @Value("${app.allowed-extensions}")
    private String allowedExtensions;

    public void ingestRepo(String gitUrl) throws Exception {

        String[] repoInfo = RepoUtils.extractRepoId(gitUrl);
        if (repoInfo.length != 2) {
            log.error("Invalid repo URL: {}", gitUrl);
            throw new IllegalArgumentException("Invalid repo URL");
        }

        String user = repoInfo[0];
        String repo = repoInfo[1];
        String repoId = user + "-" + repo;
        vectorManager.useOrCreateCollection(repoId);

        log.info("Starting building tree: {}", repoId);

        GitHubApi gitRepo = new GitHubApi(user, repo);
        gitRepo.buildTree();
        BinaryTreeNode treeRoot = gitRepo.getTree();
        log.info(treeRoot.getName());
        // gitRepo.printTree(treeRoot, repoId);
        // log.info(treeRoot.toString());
        log.info("Completed building tree: {}", repoId);

        log.info("Starting documenting docs :((((( ): {}", repoId);

        List<Document> doc = new ArrayList<>();
        embedTreeConcurrent(treeRoot, doc, repoId);
        vectorManager.setDocuments(doc);
        log.info("Finished documenting docs :((((( ): {}", repoId);
    }

    // ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private void embedTreeConcurrent(BinaryTreeNode node, List<Document> file, String repoId) {
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
            decision = EmbeddingUtils.shouldEmbed(fileName, Integer.valueOf(node.getSize()));
            log.info("MADE IT" + decision.name());
        }else{
            log.info("MADE IT SKIP FILE" );
            decision = EmbedDecision.SKIP_FILE;
        }
        switch (decision) {
            case EMBED_DIRECTLY -> file.add(new Document(node.getContent(), metadata));
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

            if(node.getChildren() != null) {
                for (BinaryTreeNode child : node.getChildren()) {
                    embedTreeConcurrent(child, file, repoId);
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

    public void ingestJson() {

        Path basePath = Paths.get(System.getProperty("user.dir"))
                     .resolve("src/main/java/com/nsf/langchain/ModelTraining/Criteria");

        Path optimizeScore = basePath.resolve("OptimizeScoring.json");
        Path patternCrit = basePath.resolve("Patterns.json");

        // Path optimizeScore = Paths.get("/Users/cara/Documents/NSF-SAR/rag/src/main/java/com/nsf/langchain/ModelTraining/Criteria/OptimizeScoring.json");
        // Path patternCrit = Paths.get("/Users/cara/Documents/NSF-SAR/rag/src/main/java/com/nsf/langchain/ModelTraining/Criteria/Patterns.json");

        ObjectMapper objectMapper = new ObjectMapper();
        
        try{
            JsonNode optimize = objectMapper.readTree(optimizeScore.toFile());
            JsonNode pattern = objectMapper.readTree(patternCrit.toFile());

            interpretOptimizingJson(optimize);
            interpretPatternsJson(pattern);

        } catch (IOException e) {
            log.error("Failed to ingest JSON '{}': {}", e.getMessage());
        }    
    }

    public void interpretPatternsJson(JsonNode node){
        JsonNode patternsArray = node.get("patterns");
        if (patternsArray != null && patternsArray.isArray()) {
            for (JsonNode pattern : patternsArray) {
                String text = "Name: " + pattern.get("name") + " Description: " + pattern.get("description") + "Advantage: " + pattern.get("advantage") 
                    + "Disadvantage" + pattern.get("disadvantage") + " Common Implementations" + pattern.get("common implementations");
                Map<String, Object> metadata = Map.of( 
                    "Type: ",  "Pattern Type",
                    "Name: ", pattern.get("name")
                );
                vectorManager.addToDocuments(new Document(text, metadata));
            }
        }
        JsonNode antipatternsArray = node.get("patterns");
        if (antipatternsArray != null && antipatternsArray.isArray()) {
            for (JsonNode antipatterns : antipatternsArray) {
                String text = "Name: " + antipatterns.get("name") + " Description: " + antipatterns.get("description") + "Advantage: " + antipatterns.get("advantage") 
                    + "Disadvantage" + antipatterns.get("disadvantage") + " Common Implementations" + antipatterns.get("common implementations");
                Map<String, Object> metadata = Map.of( 
                    "Type: ",  "AntiPattern Type",
                    "Name: ", antipatterns.get("name")
                );
                vectorManager.addToDocuments(new Document(text, metadata));
            }
        }
        // String text = "Name: " + node.get("name") + " Description: " + node.get("description") + "Advantage: " + node.get("advantage") 
        //     + "Disadvantage" + node.get("disadvantage") + " Common Implementations" + node.get("common implementations");
        // vectorManager.addToDocuments(new Document(text));
    }

    public void interpretOptimizingJson(JsonNode node){
        // String text = "Name: " + node.get("name") + " Description: "+ node.get("description") + " Guidance: " 
            // + " Pattern: "+ node.get("patterns") + " Weight: " + node.get("weight");
        // documents.add(new Document(text));
        // vectorManager.addToDocuments(new Document(text));

        JsonNode criteriaArray = node.get("criteria");
        if (criteriaArray != null && criteriaArray.isArray()) {
            for (JsonNode criteria : criteriaArray) {
                String text = "Name: " + criteria.get("name") + " Description: "+ criteria.get("description") + " Guidance: " 
                    + " Pattern: "+ criteria.get("patterns") + " Weight: " + criteria.get("weight");
                Map<String, Object> metadata = Map.of( 
                    "Type: ",  "Criteria Type",
                    "Name: ", criteria.get("name")
                );
                vectorManager.addToDocuments(new Document(text, metadata));
            }
        }
    }
}
