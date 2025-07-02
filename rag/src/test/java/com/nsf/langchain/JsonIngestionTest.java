package com.nsf.langchain;

import com.nsf.langchain.controller.RepoController;
import com.nsf.langchain.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonIngestionTest {

    private IngestionService ingestionService;
    private VectorStore vectorStore;
     private static final Logger log = LoggerFactory.getLogger(JsonIngestionTest.class);


    @BeforeEach
    void setup() {
        vectorStore = new InMemoryVectorStore(); 
        ingestionService = new IngestionService();
        ingestionService.vectorStore = vectorStore; 
    }

    @Test
    void testJsonIngestionAndSearch() {
        String repoId = "test-repo";
        Path jsonPath = Path.of("doc/msa-scoring.json");

        ingestionService.ingestJson(jsonPath, repoId);

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Scalability")
                        .topK(5)
                        .build()
        );

        assertNotNull(results);
        assertFalse(results.isEmpty(), "Expected at least one document mentioning 'Scalability'.");

    }

    @Test
void testJsonIngestionForMSAScoring() {
    String repoId = "test-repo";
    Path jsonPath = Path.of("doc/msa-scoring.json");

    ingestionService.ingestJson(jsonPath, repoId);

    List<Document> results = vectorStore.similaritySearch(
            SearchRequest.builder()
                    .query("Scalability")
                    .topK(10)
                    .build()
    );

    assertNotNull(results);
    assertFalse(results.isEmpty(), "Expected documents for query 'Scalability'.");

    boolean textContainsScalability = results.stream()
            .anyMatch(doc -> doc.getText().toLowerCase().contains("scalability"));

    boolean metadataContainsCriteria = results.stream()
            .anyMatch(doc -> "Scalability".equalsIgnoreCase((String) doc.getMetadata().get("criteria")));

    boolean hasCorrectFileSource = results.stream()
            .anyMatch(doc -> "msa-scoring.json".equalsIgnoreCase((String) doc.getMetadata().get("file")));

    assertTrue(textContainsScalability, "Expected document text to contain 'Scalability'.");
    assertTrue(metadataContainsCriteria, "Expected document metadata 'criteria' to be 'Scalability'.");
    assertTrue(hasCorrectFileSource, "Expected document to come from msa-scoring.json.");
}

    @Test
void testJsonIngestionForMSApatterns(){
    String repoId = "test-repo";
    Path jsonPath = Path.of("doc/msa-patterns.json");

    ingestionService.ingestJson(jsonPath, repoId);

    List<Document> results = vectorStore.similaritySearch(
            SearchRequest.builder()
                    .query("Anti-Corruption Layer")
                    .topK(10)
                    .build()
    );

    assertNotNull(results);
    assertFalse(results.isEmpty(), "Expected documents for query 'Anti-Corruption Layer'.");

    boolean textContains = results.stream()
            .anyMatch(doc -> doc.getText().toLowerCase().contains("anti-corruption layer"));

    boolean metadataContains= results.stream()
            .anyMatch(doc -> "Anti-Corruption Layer".equalsIgnoreCase((String) doc.getMetadata().get("pattern")));

    boolean hasCorrectFileSource = results.stream()
            .anyMatch(doc -> "msa-patterns.json".equalsIgnoreCase((String) doc.getMetadata().get("file")));

    assertTrue(textContains, "Expected document text to contain 'Anti-Corruption Layer'.");
    assertTrue(metadataContains, "Expected document metadata 'patterns' to be 'Anti-Corruption Layer'.");
    assertTrue(hasCorrectFileSource, "Expected document to come from msa-patterns.json.");
}

}


