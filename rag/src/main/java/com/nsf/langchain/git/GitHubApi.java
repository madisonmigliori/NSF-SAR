package com.nsf.langchain.git;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.token.*;
import com.nsf.langchain.service.IngestionService;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;


public class GitHubApi {
    
    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);


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
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Authorization", "Bearer " + token) 
                    .GET()
                    .build();

                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                jsonNode = mapper.readTree(response.body());

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
                                .GET()
                                .build();

                            response = client.send(request, HttpResponse.BodyHandlers.ofString());
                            BinaryTreeNode NewNode = new BinaryTreeNode(name, type, url, response.body(), size);
                            curr.addChild(curr, NewNode);
                        }
                    }
                } else {
                    System.out.println("Invalid Github Repo :(");
                }
            }
            // this.root = root;
            // printTree(root, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BinaryTreeNode getTree(){
        return this.root;
    }

    public void printTree(BinaryTreeNode node, String indent) {
        if (node == null) return;

        // System.out.println(indent + node.type + " " + node.name);
        log.info(indent + node.type + " " + node.name);
        
        if (node.children != null) {
            for (BinaryTreeNode child : node.children) {
                if (child != null) {
                    printTree(child, indent + "    ");
                }
            }
        }
    }
}