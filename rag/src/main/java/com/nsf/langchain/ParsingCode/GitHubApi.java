package com.nsf.langchain.ParsingCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.ParsingCode.GitToken.GetToken;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.Stack;
import java.util.Base64;

public class GitHubApi {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter Public GitHub Repo URL: ");
        String ProjectURL = scanner.nextLine();

        try {
            while (ProjectURL.contains("git@github.com") || ProjectURL.contains("gh repo clone")) {
                if (ProjectURL.contains("git@github.com")) {
                    System.out.println("SSH Keys Not Accepted.. Retry?");
                } else if (ProjectURL.contains("gh repo clone")) {
                    System.out.println("GitHub CLIs Not Accepted.. Retry?");
                }

                System.out.print("Enter Public GitHub Repo URL: ");
                ProjectURL = scanner.nextLine();
            }

            URL GitURL = new URI(ProjectURL).toURL();

            String GitClean = GitURL.getPath().split(".git")[0];
            String[] GitSections = GitClean.split("/");

            //making sure github url
            while(!GitURL.getHost().equals("github.com") || GitSections.length == 2) { //
                System.out.println("Github repo not found.. Retry?");

                System.out.print("Enter Public GitHub Repo URL: ");
                ProjectURL = scanner.nextLine();
                GitURL = new URI(ProjectURL).toURL();
                GitClean = GitURL.getPath().split(".git")[0];
                GitSections = GitClean.split("/");
            }

            scanner.close();

            String user = GitSections[1];
            String repo = GitSections[2];

            String token = new GetToken().getToken();
            
            Stack<BinaryTreeNode> tovisit= new Stack<>();
            
            String apiUrl = "https://api.github.com/repos/" + user  + "/" + repo + "/contents";
            BinaryTreeNode root = new BinaryTreeNode(repo, "repo", apiUrl, false);
            tovisit.push(root);

            HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
            
            HttpResponse<String> response;
            JsonNode jsonNode;
            ObjectMapper mapper = new ObjectMapper();

            while(!tovisit.isEmpty()){
                BinaryTreeNode curr = tovisit.pop();
                apiUrl = curr.path;

                // String apiUrl = "https://api.github.com/repos/" + user  + "/" + repo + "/contents/" + curr.path;
                
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
                        // String path = node.get("path").asText();
                        String type = node.get("type").asText();
                        String url = node.get("url").asText();

                        BinaryTreeNode NewNode = new BinaryTreeNode(name, type, url, false);

                        if (type.equals("dir")){
                            tovisit.push(NewNode);
                        }else if(!type.equals("file")){
                            System.out.println("******************** NEW TYPE DISCOVERED *********************");
                        }else{ // else assume type = file 
                            // apiUrl = "https://api.github.com/repos/" + user  + "/" + repo + "/contents/" + path;
                            String download_url = node.get("download_url").asText();

                            request = HttpRequest.newBuilder()
                                .uri(URI.create(download_url))
                                .header("Accept", "application/vnd.github+json")
                                .header("X-GitHub-Api-Version", "2022-11-28")
                                .header("Authorization", "Bearer " + token) 
                                .GET()
                                .build();

                            response = client.send(request, HttpResponse.BodyHandlers.ofString());

                            // Parse JSON using Jackson
                            // JsonNode root_one = mapper.readTree(response.body());
                            // String content = root_one.get("download_url").asText();

                            if(!response.body().contains("�")){
                                System.out.println("Decoded content\n " + apiUrl + " \n");
                                System.out.println(response.body());
                            }
                            // Extract and decode content
                            // String base64Content = root_one.get("content").asText();
                            // base64Content = base64Content.replaceAll("\\s", "").replaceAll("\\n", " "); // Remove all whitespace/newlines
                            // byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
                            // String decodedContent = new String(decodedBytes);
                        }
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