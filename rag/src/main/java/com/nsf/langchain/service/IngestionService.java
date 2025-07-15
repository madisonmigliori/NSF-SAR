package com.nsf.langchain.service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.tomcat.util.json.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;

import com.nsf.langchain.git.GitHubApi;
import com.nsf.langchain.git.token.GetToken;
import com.nsf.langchain.utils.GitUtils;
import com.nsf.langchain.utils.RepoUtils;
import com.nsf.langchain.utils.TextUtils;

import jakarta.annotation.PostConstruct;

import com.nsf.langchain.utils.PdfUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.BinaryTreeNode;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    
    @Autowired
    public VectorStore vectorStore;

    @Value("${app.repos-dir}")
    private String baseDir;

    @Value("${app.allowed-extensions}")
    private String allowedExtensions;

   

@PostConstruct
public void initIngestion() {
    String repoId = "base";
    Path scoringPath = Paths.get("doc", "msa-scoring.json");
    Path patternsPath = Paths.get("doc", "msa-patterns.json");
    Path antiPatternsPath = Paths.get("doc", "msa-anti-patterns.json");

    ingestMsaScoringJson(scoringPath, repoId);
    ingestMsaPatternsJson(patternsPath, repoId);
    ingestMsaAntiPatternsJson(antiPatternsPath, repoId);
}




    public void ingestRepo(String gitUrl) throws Exception {
        String repoId = RepoUtils.extractRepoId(gitUrl);
        Path repoFolder = Paths.get(baseDir, repoId);

        Files.createDirectories(repoFolder.getParent());

        try {
            if (Files.exists(repoFolder.resolve(".git"))) {
                log.info("Pulling latest for repo: {}", repoId);
                GitUtils.pull(repoFolder);
            } else {
                log.info("Cloning repo: {} → {}", gitUrl, repoFolder);
                GitUtils.clone(gitUrl, Paths.get(baseDir), repoId);
            }
        } catch (Exception e) {
            log.error("Failed to access baseDir: {}", baseDir);
            throw new RuntimeException("Git clone/pull failed: " + e.getMessage(), e);
        }

        log.info("Starting file processing for ingestion: {}", repoId);

        try (Stream<Path> files = Files.walk(repoFolder)) {
            files
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains(".git"))
                .filter(this::hasAllowedExtension)
                .forEach(path -> processFile(repoId, repoFolder, path));
        }

        log.info("Finished ingestion for repo: {}", repoId);
    }


    public void ingestJson(Path path, String repoId) {
        log.info("Starting JSON processing for ingestion: {}", path);
    
        String fileName = path.getFileName().toString();
    
        if (fileName.equalsIgnoreCase("msa-scoring.json")) {
            ingestMsaScoringJson(path, repoId);
            return;
        }
    
        if (fileName.equalsIgnoreCase("msa-patterns.json")) {
            ingestMsaPatternsJson(path, repoId);
            return;
        }
    
        ObjectMapper objectMapper = new ObjectMapper();
        List<Document> documents = new ArrayList<>();
    
        try {
            JsonNode rootNode = objectMapper.readTree(path.toFile());
            flattenJson(rootNode, "", documents, fileName, repoId);
    
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("Ingested {} generic JSON nodes from '{}'", documents.size(), fileName);
            } else {
                log.warn("No valid JSON nodes found in file: {}", fileName);
            }
        } catch (IOException e) {
            log.error("Failed to ingest generic JSON '{}': {}", fileName, e.getMessage());
        }
    }
    

    private void flattenJson(JsonNode node, String pathPrefix, List<Document> documents, String fileName, String repoId){
        if(node.isObject()){
            node.fieldNames().forEachRemaining(field -> {
                String newPath = pathPrefix.isEmpty() ? field : pathPrefix + "/" + field;
                flattenJson(node.get(field), newPath, documents, fileName, repoId);

            });
        } else if (node.isArray()){
            for (int i = 0; i < node.size(); i++){
                flattenJson(node.get(i), pathPrefix + "[" + i +  "]", documents, fileName, repoId);
            }
        } else {
            String text = "Key: " + pathPrefix + ", Value:" + node.asText();
            Document doc = Document.builder().text(text).metadata("file", fileName).metadata("repo", repoId).build();
            documents.add(doc);
        }
    }


    public void ingestMsaScoringJson(Path path, String repoId) {
        log.info("Starting MSA scoring JSON ingestion for file: {}", path);
    
        ObjectMapper objectMapper = new ObjectMapper();
        List<Document> documents = new ArrayList<>();
    
        try {
            if (!Files.exists(path)) {
                log.error("File not found: {}", path);
                return;
            }            
            JsonNode rootNode = objectMapper.readTree(path.toFile());
            JsonNode criteriaArray = rootNode.get("criteria");
    
            if (criteriaArray != null && criteriaArray.isArray()) {
                for (JsonNode criterion : criteriaArray) {
                    String name = criterion.path("name").asText();
                    String description = criterion.path("description").asText();
                    String guidance = criterion.path("guidance").asText();
                    String weight = criterion.path("weight").asText();
    
                    StringBuilder patterns = new StringBuilder();
                    JsonNode patternsNode = criterion.path("patterns");
                    if (patternsNode.isArray()) {
                        for (JsonNode pattern : patternsNode) {
                            patterns.append(pattern.asText()).append(", ");
                        }
                    }
    
                    String text = String.format("""
                            Criteria Name: %s
                            Description: %s
                            Guidance: %s
                            Patterns: %s
                            Weight: %s
                            """, name, description, guidance, patterns.toString(), weight);
    
                    Document doc = Document.builder()
                            .text(text)
                            .metadata("file", path.getFileName().toString())
                            .metadata("repo", repoId)
                            .metadata("criteria", name)
                            .build();
    
                    documents.add(doc);
                }
            }

            for (Document doc : documents) {
                log.info("Doc metadata file: {}, repo: {}", doc.getMetadata().get("file"), doc.getMetadata().get("repo"));
            }
            

            for (Document doc : documents) {
                log.debug("Indexed doc -> file: {}, repo: {}, keys: {}", 
                    doc.getMetadata().get("file"),
                    doc.getMetadata().get("repo"),
                    doc.getMetadata().keySet()
                );
            }
            
            

    
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("Ingested {} criteria from '{}'", documents.size(), path.getFileName());
                debugVectorStoreDocs();
            List<Document> test = vectorStore.similaritySearch("repo == 'test-repo'");
log.info("Test results: {}", test.size());
test.forEach(doc -> log.info("Found: {}", doc.getMetadata()));
            } else {
                log.warn("No criteria found in '{}'", path);
            }
    
        } catch (IOException e) {
            log.error("Failed to ingest MSA scoring JSON '{}': {}", path, e.getMessage());
        }
    }
    


    public void ingestMsaPatternsJson(Path path, String repoId) {
        log.info("Starting MSA patterns JSON ingestion for file: {}", path);
    
        ObjectMapper objectMapper = new ObjectMapper();
        List<Document> documents = new ArrayList<>();
    
        try {
            if (!Files.exists(path)) {
                log.error("File not found: {}", path);
                return;
            }            
            JsonNode rootNode = objectMapper.readTree(path.toFile());
            JsonNode patternsArray = rootNode.get("patterns");
    
            if (patternsArray != null && patternsArray.isArray()) {
                for (JsonNode patternNode : patternsArray) {
                    String name = patternNode.path("name").asText();
                    String description = patternNode.path("description").asText();
                    String advantage = patternNode.path("advantage").asText();
                    String disadvantage = patternNode.path("disadvantage").asText();
                    String implementations = patternNode.path("common implementations").asText();
    
                    String text = String.format("""
                            Pattern Name: %s
                            Description: %s
                            Advantage: %s
                            Disadvantage: %s
                            Common Implementations: %s
                            """, name, description, advantage, disadvantage, implementations);
    
                    Document doc = Document.builder()
                            .text(text)
                            .metadata("file", path.getFileName().toString())
                            .metadata("repo", repoId)
                            .metadata("pattern", name)
                            .build();
    
                    documents.add(doc);
                }
            }

            for (Document doc : documents) {
                log.info("Doc metadata file: {}, repo: {}", doc.getMetadata().get("file"), doc.getMetadata().get("repo"));
            }
            

            for (Document doc : documents) {
                log.debug("Indexed doc -> file: {}, repo: {}, keys: {}", 
                    doc.getMetadata().get("file"),
                    doc.getMetadata().get("repo"),
                    doc.getMetadata().keySet()
                );
            }

           
            
    
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("Ingested {} patterns from '{}'", documents.size(), path.getFileName());
                debugVectorStoreDocs();
          
                List<Document> test = vectorStore.similaritySearch("repo == ''");
                log.info("Test results: {}", test.size());
                test.forEach(doc -> log.info("Found: {}", doc.getMetadata()));
            } else {
                log.warn("No patterns found in '{}'", path);
            }
    
        } catch (IOException e) {
            log.error("Failed to ingest MSA patterns JSON '{}': {}", path, e.getMessage());
        }
    }

    public void ingestMsaAntiPatternsJson(Path path, String repoId) {
        log.info("Starting MSA anti-patterns JSON ingestion for file: {}", path);
    
        ObjectMapper objectMapper = new ObjectMapper();
        List<Document> documents = new ArrayList<>();
    
        try {
            if (!Files.exists(path)) {
                log.error("File not found: {}", path);
                return;
            }
    
            JsonNode rootNode = objectMapper.readTree(path.toFile());
            JsonNode antiArray = rootNode.get("anti-patterns");
    
            if (antiArray != null && antiArray.isArray()) {
                for (JsonNode node : antiArray) {
                    String name = node.path("name").asText();
                    String description = node.path("description").asText();
                    String severity = node.path("severity").asText();
    
                    StringBuilder impacts = new StringBuilder();
                    JsonNode impactNode = node.path("impact");
                    if (impactNode.isArray()) {
                        for (JsonNode impact : impactNode) {
                            impacts.append(impact.asText()).append(", ");
                        }
                    }
    
                    String text = String.format("""
                            Anti-pattern: %s
                            Description: %s
                            Severity: %s
                            Impact: %s
                            """, name, description, severity, impacts.toString());
    
                    Document doc = Document.builder()
                            .text(text)
                            .metadata("file", path.getFileName().toString())
                            .metadata("repo", repoId)
                            .metadata("antipattern", name)
                            .build();
    
                    documents.add(doc);
                }
            }
    
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("Ingested {} anti-patterns from '{}'", documents.size(), path.getFileName());
            } else {
                log.warn("No anti-patterns found in '{}'", path);
            }
    
        } catch (IOException e) {
            log.error("Failed to ingest MSA anti-patterns JSON '{}': {}", path, e.getMessage());
        }
    }
    
    

    

    public void ingestLocalPdf(String repoId, Path path) {
        try {
            List<String> pages = PdfUtils.extractTextByPage(path);
            for (int i = 0; i < pages.size(); i++) {
                Document doc = Document.builder()
                    .text(pages.get(i))
                    .metadata("file", path.getFileName().toString())
                    .metadata("repo", repoId)
                    .build();
                vectorStore.add(List.of(doc));
            }
            log.info("Ingested PDF '{}' into repo '{}'", path.getFileName(), repoId);
        } catch (IOException e) {
            log.error("Failed to ingest PDF '{}': {}", path, e.getMessage());
        }
    }

    public void ingestGitRepoAPI(String gitUrl) throws Exception {
        GitHubApi repo = new GitHubApi();

        String repoId = RepoUtils.extractRepoId(gitUrl);
        BinaryTreeNode root = repo.inspectRepo(gitUrl);
        List<String>filePaths = repo.flattenFilePaths(root);

        String[] parts = gitUrl.split("/");
        String user = parts[parts.length - 2];
        String repoName = parts[parts.length - 1].replace(".git", "");

        for (String path : filePaths) {
            String content = repo.fetchFileContent(user, repoName, path);
            if (content != null && content.length() >= 20) {
                List<String> chunks = TextUtils.chunkText(content, 1000, 200);
                for (String chunk : chunks) {
                    Document doc = Document.builder()
                            .text(chunk)
                            .metadata("file", path)
                            .metadata("repo", repoId)
                            .build();
                    vectorStore.add(List.of(doc));
                }
            }
        }



    }

    public void debugVectorStoreDocs(){
        log.info("Debug: Listing all the docs in vector store.");

        List<Document> allDocs = vectorStore.similaritySearch("");

        if(allDocs == null || allDocs.isEmpty()){
            log.warn("Vector store is empty");
            return;
        }

        for(int i = 0; i < allDocs.size(); i++){
            Document doc = allDocs.get(i);
            log.info("Doc {}: \nText: {}\nMetadata: {}\n", i + 1, preview(doc.getText()), doc.getMetadata());
        }

        log.info("Doc size: {}", allDocs.size());

    }

    private String preview(String text){
        return text.length() > 200 ? text.substring(0, 200) +  "..." : text;

    }



    private boolean hasAllowedExtension(Path path) {
        String ext = com.google.common.io.Files.getFileExtension(path.toString()).toLowerCase();
        return List.of(allowedExtensions.split(",")).contains("." + ext);
    }


   
    private void processFile(String repoId, Path repoFolder, Path path) {
        log.info("Processing file: {}", path);
    
        String text;
        try {
            text = Files.readString(path);
            log.info("Successfully read file: {}", path);
        } catch (IOException e) {
            log.warn("Skipping unreadable file in '{}': {} ({})", baseDir, path, e.getMessage());
            return;
        }
    
     
        List<String> chunks = TextUtils.chunkText(text, 1000, 200)
            .stream()
            .filter(chunk -> chunk != null && chunk.trim().length() >= 20)
            .toList();
    
        log.info("Total chunks for '{}': {}", path.getFileName(), chunks.size());
    
        if (chunks.isEmpty()) {
            log.warn("No valid chunks found for '{}'. Possible reasons: file too small, chunking logic issue, or overly strict length filter.", path.getFileName());
        }
    
        String safePath = repoFolder.relativize(path).toString().replaceAll("[/\\\\]", "_");
    
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            Document doc = Document.builder()
                .text(chunk)
                .metadata("file", safePath)
                .metadata("repo", repoId)
                .build();
    
            try {
                vectorStore.add(List.of(doc));
                log.info("Stored chunk {} of '{}'", i, safePath);
            } catch (Exception e) {
                log.error("Failed to store doc for chunk {} of '{}': {}", i, safePath, e.getMessage());
            }
        }
    }
    
}
