// package com.nsf.langchain;

// import com.nsf.langchain.service.IngestionService;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.ai.document.Document;
// import org.springframework.ai.vectorstore.SearchRequest;
// import org.springframework.ai.vectorstore.VectorStore;

// import java.nio.file.Path;
// import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;

// class JsonIngestionTest {

//     private IngestionService ingestionService;
//     private VectorStore vectorStore;

//     @BeforeEach
//     void setup() {
//         vectorStore = new InMemoryVectorStore(); 
//         ingestionService = new IngestionService();
//         ingestionService.vectorStore = vectorStore;
//     }

//     @Test
//     void testJsonIngestionForMSAScoring() {
//         String repoId = "test-repo";
//         Path jsonPath = Path.of("doc/msa-scoring.json");

//         ingestionService.ingestJson(jsonPath, repoId);

//         List<Document> results = vectorStore.similaritySearch(
//                 SearchRequest.builder()
//                         .query("Scalability")
//                         .topK(10)
//                         .build()
//         );

//         assertNotNull(results);
//         assertFalse(results.isEmpty(), "Expected documents for query 'Scalability'.");

//         boolean textContainsScalability = results.stream()
//                 .anyMatch(doc -> doc.getText().toLowerCase().contains("scalability"));

//         boolean metadataContainsCriteria = results.stream()
//                 .anyMatch(doc -> "Scalability".equalsIgnoreCase((String) doc.getMetadata().get("criteria")));

//         boolean hasCorrectFileSource = results.stream()
//                 .anyMatch(doc -> "msa-scoring.json".equalsIgnoreCase((String) doc.getMetadata().get("file")));

//         assertTrue(textContainsScalability, "Expected document text to contain 'Scalability'.");
//         assertTrue(metadataContainsCriteria, "Expected document metadata 'criteria' to be 'Scalability'.");
//         assertTrue(hasCorrectFileSource, "Expected document to come from msa-scoring.json.");
//     }

//     @Test
//     void testJsonIngestionForMSAPatterns() {
//         String repoId = "test-repo";
//         Path jsonPath = Path.of("doc/msa-patterns.json");

//         ingestionService.ingestJson(jsonPath, repoId);

//         List<Document> results = vectorStore.similaritySearch(
//                 SearchRequest.builder()
//                         .query("Anti-Corruption Layer")
//                         .topK(10)
//                         .build()
//         );

//         assertNotNull(results);
//         assertFalse(results.isEmpty(), "Expected documents for query 'Anti-Corruption Layer'.");

//         boolean textContains = results.stream()
//                 .anyMatch(doc -> doc.getText().toLowerCase().contains("anti-corruption layer"));

//         boolean metadataContains = results.stream()
//                 .anyMatch(doc -> "Anti-Corruption Layer".equalsIgnoreCase((String) doc.getMetadata().get("pattern")));

//         boolean hasCorrectFileSource = results.stream()
//                 .anyMatch(doc -> "msa-patterns.json".equalsIgnoreCase((String) doc.getMetadata().get("file")));

//         assertTrue(textContains, "Expected document text to contain 'Anti-Corruption Layer'.");
//         assertTrue(metadataContains, "Expected document metadata 'pattern' to be 'Anti-Corruption Layer'.");
//         assertTrue(hasCorrectFileSource, "Expected document to come from msa-patterns.json.");
//     }

//     @Test
//     void testMsaIngestionGeneralCheck() {
//         String repoId = "test-repo";

//         List<Document> docs = vectorStore.similaritySearch(
//                 SearchRequest.builder()
//                         .query("criteria")
//                         .filterExpression("metadata.file == 'msa-scoring.json' OR metadata.file == 'msa-patterns.json'")
//                         .topK(10)
//                         .build()
//         );

//         System.out.println("=== MSA Ingestion Test Results ===");
//         if (docs.isEmpty()) {
//             System.out.println("No documents found for msa-scoring.json or msa-patterns.json");
//         } else {
//             for (Document doc : docs) {
//                 System.out.println("File: " + doc.getMetadata().get("file"));
//                 System.out.println("Repo: " + doc.getMetadata().get("repo"));
//                 System.out.println("Text snippet: " + doc.getText().substring(0, Math.min(100, doc.getText().length())) + "...");
//                 System.out.println("--------");
//             }
//         }
//     }
// }
