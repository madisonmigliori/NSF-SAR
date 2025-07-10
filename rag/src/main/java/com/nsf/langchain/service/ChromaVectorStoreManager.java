package com.nsf.langchain.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaApi.AddEmbeddingsRequest;
import org.springframework.ai.chroma.vectorstore.ChromaApi.Collection;
import org.springframework.ai.chroma.vectorstore.ChromaApi.CreateCollectionRequest;
import org.springframework.ai.chroma.vectorstore.ChromaApi.GetEmbeddingResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi.EmbeddingsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;


import jakarta.annotation.PostConstruct;

@Service
public class ChromaVectorStoreManager {
    private static final Logger log = LoggerFactory.getLogger(ChromaVectorStoreManager.class);

    // private final OllamaEmbeddingModel embeddingModel;
    private final ChromaApi client;

    private final String tenantName = "LocalRepoTenant";
    private final String databaseName = "LocalRepodb";

    private final Map<String, String> collectionMap = new HashMap<>(); // name -> ID
    private String currentCollectionName;
    private String currentCollectionId;

    private List<Document> interprettedDocuments = new ArrayList<>();

    private List<float[]> embeddings = new ArrayList<>();
    private List<String> ids = new ArrayList<>();
    private List<Map<String, Object>> metadatas = new ArrayList<>();
    private List<String> contents = new ArrayList<>();

    @Autowired
    public ChromaVectorStoreManager(OllamaEmbeddingModel embeddingModel, ChromaApi client) {
        // this.embeddingModel = embeddingModel;
        this.client = client;
    }

    @PostConstruct
    public void init() {
        client.createTenant(tenantName);
        client.createDatabase(tenantName, databaseName);
    }

    public void useOrCreateCollection(String collectionName) {
    try {
        var collection = client.getCollection(tenantName, databaseName, collectionName);
        log.info("Using existing collection: {} with ID: {}", collectionName, collection.id());
        this.currentCollectionId = collection.id();
    } catch (Exception e) {
        log.warn("Collection not found, creating new collection: {}", collectionName);
        client.createCollection(tenantName, databaseName, new CreateCollectionRequest(collectionName));
        
        // Now immediately retrieve to get correct UUID
        var created = client.getCollection(tenantName, databaseName, collectionName);
        this.currentCollectionId = created.id();
        log.info("Created new collection: {} with ID: {}", collectionName, created.id());
    }
    this.currentCollectionName = collectionName;
}

    public void setDocuments(List<Document> docs){
        this.interprettedDocuments = docs;
        log.info("SET {} documents to collection {}", interprettedDocuments.size(), currentCollectionName);
    }

    public void addToDocuments(Document doc){
        this.interprettedDocuments.add(doc);
        log.info("ADDING {} documents to collection {}", interprettedDocuments.size(), currentCollectionName);
    }

    public List<Document> getDocuments() {
        if (currentCollectionId == null) {
            throw new IllegalStateException("No collection selected. Call useOrCreateCollection() first.");
        }

        // for (Document doc : docs) {
            // this.ids.add(UUID.randomUUID().toString()); // doc ID
            // this.metadatas.add(doc.getMetadata());
            // this.contents.add(doc.getText());
            // this.embeddings.add(embeddingModel.embed(doc.getText()));
        // }

        // AddEmbeddingsRequest request = new AddEmbeddingsRequest(ids, embeddings, metadatas, contents);

        // client.upsertEmbeddings(tenantName, databaseName, currentCollectionId, request);
        log.info("RETRIEVE {} documents to collection {}", interprettedDocuments.size(), currentCollectionName);
        return this.interprettedDocuments;
    }

    public List<Document> getCollectionAsDocument(){
        log.info("entered getCOllectionasdoc");
        List<Document> vectors = new ArrayList<>();
        int i = 0;
        while(i < ids.size()){
            vectors.add(new Document(contents.get(i), metadatas.get(i)));
            i+=1;
        }
        return vectors;
    }

}
