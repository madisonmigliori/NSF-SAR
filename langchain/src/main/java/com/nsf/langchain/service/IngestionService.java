package com.nsf.langchain.service;

import com.nsf.langchain.client.EmbeddingClient;
import com.nsf.langchain.utils.RepoUtils;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionService {

    private final EmbeddingClient embedder;
    private final String baseDir;
    private final String chromaUrl;

    // Don't think the base directory is valuable at all (maybe to see if there is readme.md)
    public IngestionService(EmbeddingClient embedder,
                            @Value("${app.repos-dir}") String baseDir,
                            @Value("${app.chroma-endpoint}") String chromaUrl) {
        this.embedder = embedder;
        this.baseDir = baseDir;
        this.chromaUrl = chromaUrl;
    }

    public void ingestRepo(String gitUrl) throws Exception {
        String repoId = RepoUtils.extractRepoId(gitUrl);
        Path repoPath = Paths.get(baseDir, repoId);

        // Create Chroma store for this repo
        ChromaEmbeddingStore store = ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(repoId)
                .build();

    }

    //Chunking the Text (but dont think this is needed) It needs to look at the directory 
    private List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        for (int start = 0; start < len; start += (chunkSize - overlap)) {
            int end = Math.min(len, start + chunkSize);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }
}
