package com.nsf.langchain.client;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
                "model", "llama3.2",
                "prompt", prompt
        );

        Request req = new Request.Builder()
                .url(ollamaUrl + "/api/chat")
                .post(RequestBody.create(
                        mapper.writeValueAsString(body),
                        MediaType.get("application/json")
                ))
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                throw new IOException("Ollama request failed with code: " + res.code());
            }
            String json = res.body().string();
            return mapper.readTree(json).get("response").asText();
        }
    }
}
