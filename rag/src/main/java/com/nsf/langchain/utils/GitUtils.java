
package com.nsf.langchain.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.nsf.langchain.git.BinaryTreeNode;

@Component
public class GitUtils {


     private final RestTemplate restTemplate = new RestTemplate();
     
    public static void clone(String url, Path baseDir, String repoId) throws IOException, InterruptedException {
        runGit(baseDir, "clone", url, repoId);
    }

    public static void pull(Path repoDir) throws IOException, InterruptedException {
        runGit(repoDir, "pull");
    }

    private static void runGit(Path dir, String... args) throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "git";
        System.arraycopy(args, 0, cmd, 1, args.length);
        var pb = new ProcessBuilder(cmd)
                   .directory(dir.toFile())
                   .redirectErrorStream(true);
        var p = pb.start();
        try (var rdr = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = rdr.readLine()) != null) {
                System.out.println("[git] " + line);
            }
        }
        if (!p.waitFor(3, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            throw new RuntimeException("git timed out: " + String.join(" ", cmd));
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("git failed (" + p.exitValue() + ")");
        }
    }

    public BinaryTreeNode gitHubTree(String gitUrl){
        String[] parts = gitUrl.split("/");
            String user = parts[parts.length - 2];
            String repo = parts[parts.length - 1].replace(".git", "");

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents",user, repo);
        
        BinaryTreeNode root = new BinaryTreeNode(repo, "directory", gitUrl);
        fetchContents(apiUrl, root);
        return root;

    }

    public void fetchContents(String apiUrl, BinaryTreeNode root) {
        try {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response =
                restTemplate.exchange(apiUrl, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

        for (Map<String, Object> item : response.getBody()) {
            String name = (String) item.get("name");
            String type = (String) item.get("type"); 
            String url = (String) item.get("url");

            BinaryTreeNode child = new BinaryTreeNode(name, type, url);
            root.children.add(child);

            if ("dir".equals(type)) {
                fetchContents(url, child); 
            }
        }
    } catch (Exception e) {
        System.err.println("Failed to fetch: " + apiUrl);
        e.printStackTrace();
    }

    }

    

}
