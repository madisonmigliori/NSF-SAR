package com.nsf.langchain.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

import org.apache.tomcat.util.json.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreProperties;
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
    private ChromaApi chromaApi;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private ChromaVectorStoreProperties vectorStoreProperties;

    @Value("${app.repos-dir}")
    private String baseDir;

    @Value("${app.allowed-extensions}")
    private String allowedExtensions;


    public void ingestRepo(String gitUrl) throws Exception {
        String repoId = RepoUtils.extractRepoId(gitUrl);
        Path repoFolder = Paths.get(baseDir, repoId);
        Files.createDirectories(repoFolder.getParent());

        if (Files.exists(repoFolder.resolve(".git"))) {
            GitUtils.pull(repoFolder);
        } else {
            GitUtils.clone(gitUrl, Paths.get(baseDir), repoId);
        }

        String collectionName = repoId;
        ensureChromaCollectionExists(collectionName);

        ChromaVectorStore store = createStoreForCollection(collectionName);

        try (Stream<Path> files = Files.walk(repoFolder)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains(".git"))
                    .filter(this::hasAllowedExtension)
                    .forEach(path -> processFile(repoId, repoFolder, path, store));
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
            String text = Files.readString(path);
            List<String> chunks = TextUtils.chunkText(text, 1000, 200);
            String safePath = repoFolder.relativize(path).toString().replaceAll("[/\\\\]", "_");

            for (int i = 0; i < chunks.size(); i++) {
                Document doc = Document.builder()
                        .text(chunks.get(i))
                        .metadata("file", safePath)
                        .metadata("repo", repoId)
                        .build();
                store.add(List.of(doc));
            }
        } catch (IOException e) {
            log.warn("Skipping unreadable file: {}", path);
        } catch (Exception e) {
            log.error("Failed to store doc for '{}': {}", path, e.getMessage());
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
        log.debug("Reading file: {}", path);
        final String text;
        try {
            text = Files.readString(path);
        } catch (IOException e) {
            log.warn("Skipping unreadable file in {}: {}", baseDir, path);
            return;
        }

        
        List<String> chunks = TextUtils.chunkText(text, 1000, 200)
            .stream()
            .filter(chunk -> chunk != null && chunk.trim().length() >= 20)
            .toList();
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
                log.debug("Stored chunk {} of file {}", i, safePath);
            } catch (Exception e) {
                log.error("Failed to store doc for chunk {} of {}: {}", i, safePath, e.getMessage());
            }
        }
    }
}
