package com.nsf.langchain.client;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Component
public class OllamaChatClient implements ChatClient {

    private final OkHttpClient http;
    private final String ollamaUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaChatClient(OkHttpClient http, @Value("${app.ollama-endpoint}") String ollamaUrl) {
        this.http = http;
        this.ollamaUrl = ollamaUrl;
    }

    @Override
    public String chat(String prompt) throws IOException {
        Map<String, Object> body = Map.of(
            "model", "llama3.2", //maybe 
            "prompt", prompt
        );

        Request req = new Request.Builder()
                .url(ollamaUrl + "/api/chat") 
                .post(RequestBody.create(mapper.writeValueAsString(body), MediaType.get("application/json")))
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) throw new IOException("Ollama request failed");

            String json = res.body().string();
            return mapper.readTree(json).get("response").asText();  // adjust based on actual Ollama output
        }
    }
}
