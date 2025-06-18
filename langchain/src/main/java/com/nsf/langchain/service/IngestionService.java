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

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    @Autowired
    private VectorStore vectorStore;

    @Value("${app.repos-dir}")
    private String baseDir;

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
                .forEach(path -> processFile(repoId, repoFolder, path));
        }

        log.info("Finished ingestion for repo: {}", repoId);
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

        List<String> chunks = TextUtils.chunkText(text, 1000, 200);
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
