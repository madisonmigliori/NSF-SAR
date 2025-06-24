package com.nsf.langchain.ParsingCode.GitToken;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GitHubTokenFetcher {
    public static String getInstallationToken(String jwt) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/app/installations"))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();
        // System.out.println("Installations: " + responseBody);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(responseBody);
        long installationId = root.get(0).get("id").asLong();

        String url = "https://api.github.com/app/installations/" + installationId + "/access_tokens";
        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + jwt)
                .header("Accept", "application/vnd.github+json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());
        String body = response2.body();
        // System.out.println("Token response: " + body);

        String token = mapper.readTree(body).get("token").asText();
        return token;
    }
}
