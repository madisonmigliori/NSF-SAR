package com.nsf.langchain.service;

import com.nsf.langchain.client.EmbeddingClient;
import com.nsf.langchain.client.OllamaChatClient;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    @Autowired private EmbeddingClient embedder;
    @Autowired private ChromaEmbeddingStore chromaStore;
    @Autowired private com.nsf.langchain.client.ChatClient chatClient;


    public String answer(String userQuery) throws Exception {
        //Retrieving the string value of the query but maybe should be someting else
        List<Double> list = embedder.embed(userQuery);
        float[] vector = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            vector[i] = list.get(i).floatValue();
        }
        Embedding queryEmbedding = new Embedding(vector);

        
        //Maybe this is the RagRetriever Class??? (Need a way to set boundaries)
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(5)
            .minScore(0.5)
            .build();

        EmbeddingSearchResult<TextSegment> result = chromaStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();

        //Chroma Collections
        String context = matches.stream()
            .map(match -> match.embedded().text())
            .collect(Collectors.joining("\n---\n"));

        String prompt = "You are a system architecture assistant. Use the following retrieved github URL to answer the user's question.%sQuestion: %s".formatted(context, userQuery);

        return chatClient.chat(prompt);
    }
}
