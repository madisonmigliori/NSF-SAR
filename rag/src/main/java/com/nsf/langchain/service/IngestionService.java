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

import com.nsf.langchain.git.GitHubApiHello;
import com.nsf.langchain.git.token.GetToken;
import com.nsf.langchain.utils.GitUtils;
import com.nsf.langchain.utils.RepoUtils;
import com.nsf.langchain.utils.TextUtils;
import com.nsf.langchain.utils.PdfUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.BinaryTreeNode;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    
    @Autowired
    private VectorStore vectorStore;

    @Value("${app.repos-dir}")
    private String baseDir;

    @Value("${app.allowed-extensions}")
    private String allowedExtensions;


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
        log.info("Starting json processing for ingestion: {}", path);

        ObjectMapper objectMapper = new ObjectMapper();
        
        try{
            JsonNode rootNode = objectMapper.readTree(path.toFile());
            List<Document> documents = new ArrayList<>();

            flattenJson(rootNode, "", documents, path.getFileName().toString(), repoId);
            
            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("Ingested {} JSON nodes from '{}'", documents.size(), path.getFileName());
            } else {
                log.warn("No valid JSON nodes found for ingestion in file: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to ingest JSON '{}': {}", path, e.getMessage());
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
        GitHubApiHello repo = new GitHubApiHello();

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


    private boolean hasAllowedExtension(Path path) {
        String ext = com.google.common.io.Files.getFileExtension(path.toString()).toLowerCase();
        return List.of(allowedExtensions.split(",")).contains("." + ext);
    }


   
    private void processFile(String repoId, Path repoFolder, Path path) {
        log.info("🔍 Processing file: {}", path);
    
        String text;
        try {
            text = Files.readString(path);
            log.info("Successfully read file: {}", path);
        } catch (IOException e) {
            log.warn("Skipping unreadable file in '{}': {} ({})", baseDir, path, e.getMessage());
            return;
        }
    
        // Chunking step
        List<String> chunks = TextUtils.chunkText(text, 1000, 200)
            .stream()
            .filter(chunk -> chunk != null && chunk.trim().length() >= 20)
            .toList();
    
        log.info("📏 Total chunks for '{}': {}", path.getFileName(), chunks.size());
    
        if (chunks.isEmpty()) {
            log.warn("⚠️ No valid chunks found for '{}'. Possible reasons: file too small, chunking logic issue, or overly strict length filter.", path.getFileName());
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
