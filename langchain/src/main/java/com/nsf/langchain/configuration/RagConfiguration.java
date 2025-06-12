package com.nsf.langchain.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import okhttp3.OkHttpClient;

@Configuration
public class RagConfiguration {

    //Configuring the Chat with the Llama3.2 model
    @Bean
    public OkHttpClient ollamaClient() {
        return new OkHttpClient();
    }

    //Storing Based Value 
    @Bean
    public ChromaEmbeddingStore chromaEmbeddingStore(@Value("${app.chroma-endpoint}") String chromaUrl) {
    return ChromaEmbeddingStore.builder()
            .baseUrl(chromaUrl)
            .collectionName("repository")  
            .logRequests(false)
            .logResponses(false)
            .build();
}

    //Storing Repository
    public ChromaEmbeddingStore getStoreForRepo(String repoName, @Value("${app.chroma-endpoint}") String chromaUrl) {
        return ChromaEmbeddingStore.builder()
        .baseUrl(chromaUrl)
        .collectionName(repoName)
        .build();
    }


}
