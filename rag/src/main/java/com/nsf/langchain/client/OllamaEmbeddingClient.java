package com.nsf.langchain.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final OkHttpClient httpClient;
    private final String ollamaUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaEmbeddingClient(OkHttpClient httpClient,
                                 @Value("${app.ollama-endpoint}") String ollamaUrl) {
        this.httpClient = httpClient;
        this.ollamaUrl = ollamaUrl;
    }

    @Override
    public List<Double> embed(String text) throws IOException {
        try {
            return tryEmbed("nomic-embed-text:latest", text);
        } catch (IOException e) {
            System.err.println("Primary model failed, trying fallback model: " + e.getMessage());
            return tryEmbed("nomic-embed-text", text); 
        }
    }

    private List<Double> tryEmbed(String model, String text) throws IOException {
        Map<String, Object> body = Map.of(
            "model", model,
            "input", text
        );

        Request request = new Request.Builder()
                .url(ollamaUrl + "/api/embed")
                .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.get("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                throw new IOException("Failed to get embedding with model '" + model + "': " + responseBody);
            }

            JsonNode root = mapper.readTree(responseBody);
            return root.get("data")
                       .get(0)
                       .get("embedding")
                       .findValuesAsText("")
                       .stream()
                       .map(Double::parseDouble)
                       .toList();
        }
    }
}
