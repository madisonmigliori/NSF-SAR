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

import com.nsf.langchain.git.BinaryTreeNode;
// import com.apple.laf.resources.aqua;
import com.nsf.langchain.utils.ServiceBoundaryUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ArchitectureMap;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ServiceBoundary;

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

    private List<Document> dependencies = new ArrayList<>();
    private List<Document> serviceBoundaries = new ArrayList<>();
    private List<Document> archMap = new ArrayList<>();

    private List<Document> patternsJson= new ArrayList<>();
    private List<Document> antipattersJson= new ArrayList<>();
    private List<Document> dependenciesJson= new ArrayList<>();
    private List<Document> criteriaJson= new ArrayList<>();

    private List<Document> documentPath = new ArrayList<>();

    private BinaryTreeNode root;
    private String id;

    @Autowired
    public ChromaVectorStoreManager(OllamaEmbeddingModel embeddingModel, ChromaApi client) {
        // this.embeddingModel = embeddingModel;
        this.client = client;
    }

    //**********************START JSON INTERPRETTING ****************************

    public void setPatternsJson(Document patternString){
        log.info("LOGGING" + patternString.getMetadata());
        this.patternsJson.add(patternString);
    }

    public List<Document> getPatternsJson(){
        log.info("RETRIEVING ALL");
        return this.patternsJson;
    }

    public void setAntiPatternsJson(Document antipattersJson){
        this.antipattersJson.add(antipattersJson);
    }

    public List<Document> getAntiPatternsJson(){
        return this.antipattersJson;
    }

    public void setDependenciesJson(Document dependencyJson){
        this.dependenciesJson.add(dependencyJson);
    }

    public List<Document> getDependencyJson(){
        return this.dependenciesJson;
    }

    public void setCriteriaJson(Document criteriaJson){
        this.criteriaJson.add(criteriaJson);
    }

    public List<Document> getCriteriaJson(){
        return this.criteriaJson;
    }

//**********************END JSON INTERPRETTING ****************************

//********************** START VECTORIZING TREE ****************************

    public BinaryTreeNode getRoot(){
        return this.root;
    }

    public void setRoot(BinaryTreeNode node){
        this.root = node;
    }

    public void setDocuments(List<Document> docs){
        this.interprettedDocuments = docs;
        log.info("SET {} documents to collection {}", interprettedDocuments.size(), currentCollectionName);
    }

    public void addToDocuments(Document doc){
        this.interprettedDocuments.add(doc);
        log.info("ADDING {} documents to collection {}", interprettedDocuments.size(), currentCollectionName);
    }

    public void setDocumentPath(List<Document> doc){
        this.documentPath = doc;
    }

    public List<Document> getDocumentPath(){
        return this.documentPath;
    }

    public void setRepoId(String id){
        this.id = id;
    }
    public String getRepoId(){
        return id;
    }

//********************** END VECTORIZING TREE ****************************

    // public void addToDocumentsString(String str){
    //     this.interprettedDocuments.add(new Document(str));
    // }

    public List<Document> getDocuments() {
        if (this.interprettedDocuments == null) {
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

    public void setServiceBoundary(List<ServiceBoundary> artifacts, String filename){
        ServiceBoundaryUtils serve = new ServiceBoundaryUtils();
        this.serviceBoundaries.add(new Document(serve.format(artifacts), Map.of("id", filename)));
    } 
    
    public void setArchMap(ArchitectureMap archMap, String filename){
        this.archMap.add(new Document(ArchitectureMap.getLayersAsString(archMap),  Map.of("id", filename)));
    }

    public void setDependencies(String str, String filename){
        this.dependencies.add(new Document(str, Map.of("id", filename)));
    }

    public List<Document> getDependencies(){
        return this.dependencies;
    }

    public List<Document>  getServiceBoundary(){
        log.info("ENTERED SERVICE BOUNDARY");
        return this.serviceBoundaries;
    }
    
    public List<Document>  getArchMap(){
        return this.archMap;
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

    // @PostConstruct
    // public void init() {
    //     try {
    //         log.info("Attempting to create Chroma tenant and database...");
    //         // client.createTenant(tenantName);
    //         // client.createDatabase(tenantName, databaseName);
    //         log.info("Chroma tenant and database setup complete.");
    //     } catch (Exception e) {
    //         log.error("Failed to connect to Chroma during init: {}", e.getMessage(), e);
    //     }
    // }


    // @PostConstruct
    // public void init() {
    //     client.createTenant(tenantName);
    //     client.createDatabase(tenantName, databaseName);
    // }

//     public void useOrCreateCollection(String collectionName) {
//     try {
//         var collection = client.getCollection(tenantName, databaseName, collectionName);
//         log.info("Using existing collection: {} with ID: {}", collectionName, collection.id());
//         this.currentCollectionId = collection.id();
//     } catch (Exception e) {
//         log.warn("Collection not found, creating new collection: {}", collectionName);
//         client.createCollection(tenantName, databaseName, new CreateCollectionRequest(collectionName));
        
//         // Now immediately retrieve to get correct UUID
//         var created = client.getCollection(tenantName, databaseName, collectionName);
//         this.currentCollectionId = created.id();
//         log.info("Created new collection: {} with ID: {}", collectionName, created.id());
//     }
//     this.currentCollectionName = collectionName;
// }