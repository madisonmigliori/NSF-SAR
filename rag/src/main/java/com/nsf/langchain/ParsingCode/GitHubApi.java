package com.nsf.langchain.ParsingCode;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.rag.ParsingCode.GitToken.GetToken;

public class GitHubApi {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter Public GitHub Repo URL: ");
        String ProjectURL = scanner.nextLine();

        try {
            URL GitURL = new URI(ProjectURL).toURL();

            String[] GitSections = GitURL.getPath().split("/");

            //making sure github url
            if(!GitURL.getHost().equals("github.com") || GitURL.getPath().split("/").length == 2) { //
                System.out.println("Github repo not found.. Retry?");

                System.out.print("Enter Public GitHub Repo URL: ");
                ProjectURL = scanner.nextLine();
                GitURL = new URI(ProjectURL).toURL();
                GitSections = GitURL.getPath().split("/");
            }

            scanner.close();

            String user = GitSections[1];
            String repo = GitSections[2];

            String token = new GetToken().getToken();
            
            Stack<BinaryTreeNode> tovisit= new Stack<>();
            BinaryTreeNode root = new BinaryTreeNode(repo, "repo", "", new ArrayList<BinaryTreeNode>());
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

                        BinaryTreeNode NewNode = new BinaryTreeNode(name, type, path);

                        if (type.equals("dir")){
                            tovisit.push(NewNode);
                        }else if(!type.equals("file")){
                            System.out.println("******************** NEW TYPE DISCOVERED *********************");
                        }
                        // else assume type = file 
                        curr.addChild(curr, NewNode);
                    }
                } else {
                    System.out.println("Invalid Github Repo :(");
                }
            }

            printTree(root, "");

        } catch (Exception e) {
            e.printStackTrace();
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