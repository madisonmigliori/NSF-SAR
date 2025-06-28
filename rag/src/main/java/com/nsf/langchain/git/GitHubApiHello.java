package com.nsf.langchain.git;

import com.nsf.langchain.git.BinaryTreeNode;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.controller.RepoController;
import com.nsf.langchain.git.token.GetToken;

public class GitHubApiHello {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiHello.class);

    public BinaryTreeNode inspectRepo(String repoUrl) throws Exception {
        String token = new GetToken().getToken();
        URL gitUrl = new URI(repoUrl).toURL();
        String[] gitSections = gitUrl.getPath().split("/");
        
        if(!gitUrl.getHost().equals("github.com") || gitUrl.getPath().split("/").length == 2) { //
                log.error("Github repo not found.. Retry?");
        }

            String user = gitSections[1];
            String repo = gitSections[2];
            
            
            Stack<BinaryTreeNode> tovisit= new Stack<>();
            BinaryTreeNode root = new BinaryTreeNode(repo, "repo", "", false);
            tovisit.push(root);

            while(!tovisit.isEmpty()){
                BinaryTreeNode curr = tovisit.pop();

                String apiUrl = "https://api.github.com/repos/" + user  + "/" + repo + "/contents/" + curr.path;
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Authorization", "Bearer " + token) 
                    .GET()
                    .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(response.body());

                if (jsonNode.isArray()) {
                    for (JsonNode node : jsonNode) {
                        String name = node.get("name").asText();
                        String path = node.get("path").asText();
                        String type = node.get("type").asText();

                        BinaryTreeNode NewNode = new BinaryTreeNode(name, type, path, false);

                        if (type.equals("dir")){
                            tovisit.push(NewNode);
                        }else if(!type.equals("file")){
                            log.warn("******************** NEW TYPE DISCOVERED *********************");
                        }
                     
                        curr.addChild(curr, NewNode);
                    }
                } else {
                    log.error("Invalid Github Repo :(");
                }
            }
            printTree(root, "");
            return root;

        } 
        

    public List<String> flattenFilePaths(BinaryTreeNode root) {
        List<String> paths = new ArrayList<>();
        flatten(root, paths);
        return paths;
    }

    private void flatten(BinaryTreeNode node, List<String> paths) {
        if (node == null) return;
        if (node.type.equals("file")) {
            paths.add(node.path);
        }
        if (node.children != null) {
            for (BinaryTreeNode child : node.children) {
                flatten(child, paths);
            }
        }
    }

    public String fetchFileContent(String user, String repo, String path) throws Exception {
            String token = new GetToken().getToken();
            String apiUrl = "https://api.github.com/repos/" + user  + "/" + repo + "/contents/" + path;
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("Authorization", "Bearer " + token) 
                .GET()
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.body());

            if (jsonNode.has("content")) {
                String encodedContent = jsonNode.get("content").asText();
                return new String(java.util.Base64.getDecoder().decode(encodedContent));
            } else {
                log.warn("No content found for file: {}", path);
                return null;
            }
        
    }

    public static void printTree(BinaryTreeNode node, String indent) {
        if (node == null) return;

        System.out.println(indent + node.type + " " + node.name);
        
        if (node.children != null) {
            for (BinaryTreeNode child : node.children) {
                if (child != null) {
                    printTree(child, indent + "    ");
                }
            }
        }
    }
}
