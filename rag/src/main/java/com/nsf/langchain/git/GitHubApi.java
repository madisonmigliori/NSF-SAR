package com.nsf.langchain.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.token.GetToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.Base64;


@Service
public class GitHubApi {

    private static final Logger log = LoggerFactory.getLogger(GitHubApi.class);

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();


    public BinaryTreeNode inspectRepo(String gitUrl) throws Exception {
        String token = new GetToken().getToken();

        String[] parts = gitUrl.replace(".git", "").split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub repo URL: " + gitUrl);
        }

        String user = parts[parts.length - 2];
        String repo = parts[parts.length - 1];

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents", user, repo);
        BinaryTreeNode root = new BinaryTreeNode(repo, "repo", apiUrl);

        Stack<BinaryTreeNode> toVisit = new Stack<>();
        toVisit.push(root);

        


        while (!toVisit.isEmpty()) {
            BinaryTreeNode current = toVisit.pop();

            System.out.println("Fetching URL: " + current.url);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(current.url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode jsonNode = mapper.readTree(response.body());

            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    String name = node.get("name").asText();
                    String type = node.get("type").asText();
                    String url = node.get("url").asText();

                    if (type.equals("dir")) {
                        BinaryTreeNode dirNode = new BinaryTreeNode(name, type, url);
                        current.addChild(current, dirNode);
                        System.out.println("Added " + type + ": " + name + " to " + current.name);
                        toVisit.push(dirNode);
                        
                    } else if (type.equals("file")) {
                        String downloadUrl = node.get("download_url").asText();

                        HttpRequest fileRequest = HttpRequest.newBuilder()
                                .uri(URI.create(downloadUrl))
                                .header("Authorization", "Bearer " + token)
                                .GET()
                                .build();

                        HttpResponse<String> fileResponse = client.send(fileRequest, HttpResponse.BodyHandlers.ofString());

                        BinaryTreeNode fileNode = new BinaryTreeNode(name, type, url, fileResponse.body());
                        current.addChild(current, fileNode);
                    } else {
                        log.warn("Unknown content type '{}' for file '{}'", type, name);
                    }
                }
            } else {
                log.error("Unexpected response when fetching contents of {}", current.url);
            }
        }

        return root;
    }

    public List<String> flattenFilePaths(BinaryTreeNode root) {
        List<String> paths = new ArrayList<>();
        flatten(root, "", paths);
        return paths;
    }

    private void flatten(BinaryTreeNode node, String currentPath, List<String> paths) {
        if (node == null) return;

        String newPath = currentPath.isEmpty() ? node.name : currentPath + "/" + node.name;

        if ("file".equals(node.type)) {
            paths.add(newPath);
            
        }

        if (node.children != null) {
            for (BinaryTreeNode child : node.children) {
                flatten(child, newPath, paths);
            }
        }
    }

    


    public String fetchFileContent(String user, String repo, String path) throws Exception {
    String token = new GetToken().getToken();
    String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s", user, repo, path);


    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .GET()
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode jsonNode = mapper.readTree(response.body());

    if (jsonNode.has("content")) {
        String encodedContent = jsonNode.get("content").asText();
        encodedContent = encodedContent.replaceAll("\\s", ""); 
        byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    } else {
        log.warn("No content found for file: {}", path);
        return null;
    }
}

public List<String> listAllFilePaths(String gitUrl) throws Exception {
    BinaryTreeNode root = inspectRepo(gitUrl);
    GitHubApi.printTree(root, "");
    return flattenFilePaths(root);
}



    public static void printTree(BinaryTreeNode node, String indent) {
        if (node == null) return;

        System.out.println(indent + node.type + " " + node.name);

        if (node.children != null) {
            for (BinaryTreeNode child : node.children) {
                printTree(child, indent + "    ");
            }
        }
    }
}
