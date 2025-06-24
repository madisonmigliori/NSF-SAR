package com.nsf.langchain.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nsf.langchain.utils.GitUtils;
import com.nsf.langchain.utils.RepoUtils;
import com.nsf.langchain.utils.TextUtils;
import com.nsf.langchain.utils.PdfUtils;

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

    private boolean hasAllowedExtension(Path path) {
        String ext = com.google.common.io.Files.getFileExtension(path.toString()).toLowerCase();
        return List.of(allowedExtensions.split(",")).contains("." + ext);
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
