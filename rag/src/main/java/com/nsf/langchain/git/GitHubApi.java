package com.nsf.langchain.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.token.GetToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import com.nsf.langchain.git.token.*;
import com.nsf.langchain.service.IngestionService;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.Base64;
import java.util.HashMap;


@Service
public class GitHubApi {

    private BinaryTreeNode root;
    private String user;
    private String repo;
    private String type;

    public GitHubApi(String user, String repo){
        this.user = user;
        this.repo = repo;
    }

    public void buildTree() {

        try {
            String token = new GetToken().getToken();
            
            Stack<BinaryTreeNode> tovisit= new Stack<>();
            
            String apiUrl = "https://api.github.com/repos/" + user  + "/" + repo + "/contents";

            this.root = new BinaryTreeNode(repo, "repo", apiUrl, "0");
            tovisit.push(this.root);
            // this.root = root;

            log.info(apiUrl);

            HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
            
            HttpResponse<String> response;
            JsonNode jsonNode;
            ObjectMapper mapper = new ObjectMapper();

            while(!tovisit.isEmpty()){
                BinaryTreeNode curr = tovisit.pop();
                apiUrl = curr.url;
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
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
                if (jsonNode.isArray()) {
                    for (JsonNode node : jsonNode) {
                        String name = node.get("name").asText();
                        String type = node.get("type").asText();
                        String url = node.get("url").asText();
                        String size = node.get("size").asText();

                        this.type = type;

                        if (type.equals("dir")){
                            BinaryTreeNode NewNode = new BinaryTreeNode(name, type, url, size);
                            tovisit.push(NewNode);
                            curr.addChild(curr, NewNode);
                        }else{ // else assume type = file 
                            String download_url = node.get("download_url").asText();
                        
                            request = HttpRequest.newBuilder()
                                .uri(URI.create(download_url))
                                .header("Accept", "application/vnd.github+json")
                                .header("X-GitHub-Api-Version", "2022-11-28")
                                .header("Authorization", "Bearer " + token) 
                    if (type.equals("dir")) {
                        BinaryTreeNode dirNode = new BinaryTreeNode(name, type, url);
                        current.children.add(dirNode); 
                        toVisit.push(dirNode);
                        
                    } else if (type.equals("file")) {
                        String downloadUrl = node.get("download_url").asText();

                        HttpRequest fileRequest = HttpRequest.newBuilder()
                                .uri(URI.create(downloadUrl))
                                .header("Authorization", "Bearer " + token)
                                .GET()
                                .build();

                        HttpResponse<String> fileResponse = client.send(fileRequest, HttpResponse.BodyHandlers.ofString());

                        BinaryTreeNode fileNode = new BinaryTreeNode(name, type, url, fileResponse.body(), size);
                        current.children.add(fileNode);
                    } else {
                        log.warn("Unknown content type '{}' for file '{}'", type, name);
                    }
                }
            }
            // this.root = root;
            // printTree(root, "");
        } catch (Exception e) {
            e.printStackTrace();
            } else {
                log.error("Unexpected response when fetching contents of {}", current.url);
            }
        }
    }

    public BinaryTreeNode getTree(){
        return this.root;

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
   
    String token = System.getenv("GITHUB_PAT");
    if (token == null || token.isEmpty()) {
        throw new IllegalStateException("GitHub token not found in environment variable 'GITHUB_PAT'");
    }
    String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s", user, repo, path);
    System.out.println("Using GitHub token: " + (token != null ? "YES" : "NO"));

    


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

public Map<String, String> extractCode(BinaryTreeNode root){
    Map<String, String> codeFiles = new HashMap<>();
    collectCodeFiles(root,"", codeFiles);
    return codeFiles;
}

private void collectCodeFiles(BinaryTreeNode node, String path, Map<String,String> codeFiles){
    if(node == null){
        return;
    }

    String fullPath = path.isEmpty() ? node.name : path + "/" + node.name;

    if("file".equals(node.type) && isCodeFile(node.name)){
        codeFiles.put(fullPath, node.content);
    }
    if(node.children != null){
        for (BinaryTreeNode child : node.children){
            collectCodeFiles(child, fullPath, codeFiles);
        }
    }
}

private boolean isCodeFile(String fileName) {
    return fileName.endsWith(".java") || fileName.endsWith(".py") ||
           fileName.endsWith(".js") || fileName.endsWith(".ts") ||
           fileName.endsWith(".go") || fileName.endsWith(".rs") ||
           fileName.endsWith(".cpp") || fileName.endsWith(".hpp") ||
           fileName.endsWith(".h") || fileName.endsWith(".rb") ||
           fileName.endsWith(".jsx") || fileName.endsWith(".tsx");
}


    public static void printTree(BinaryTreeNode node, String indent) {
        if (node == null) return;

        System.out.println(indent + node.type + " " + node.name);

        // System.out.println(indent + node.type + " " + node.name);
        log.info(indent + node.type + " " + node.name);
        
        if (node.children != null) {
            for (BinaryTreeNode child : node.children) {
                printTree(child, indent + "    ");
            }
        }
    }
}
