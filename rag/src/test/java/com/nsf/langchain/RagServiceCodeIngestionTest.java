package com.nsf.langchain;

import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.git.GitHubApi;
import com.nsf.langchain.utils.ArchitectureUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils;

import io.github.cdimascio.dotenv.Dotenv;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

@SpringBootTest
public class RagServiceCodeIngestionTest {

    @MockBean
    private GitHubApi gitHubApi;  

    @Test
    public void testExtractAndPrintCodeFiles() throws Exception {

        BinaryTreeNode fileNode = new BinaryTreeNode(
            "Main.java",
            "file",
            "url",
            "public class Main { int x = 1; }"
        );
        
        BinaryTreeNode root = new BinaryTreeNode("repo", "repo", "url");
        root.children.add(fileNode);
    

        when(gitHubApi.inspectRepo(anyString())).thenReturn(root);
        when(gitHubApi.extractCode(root)).thenCallRealMethod();
    

        BinaryTreeNode returnedRoot = gitHubApi.inspectRepo("https://github.com/spring-projects/spring-petclinic-microservices");
        assertNotNull(returnedRoot, "Root should not be null");
    
        Map<String, String> codeFiles = gitHubApi.extractCode(returnedRoot);
        assertFalse(codeFiles.isEmpty(), "Expected code files to be extracted, but got none.");
    
 
        GitHubApi.printTree(returnedRoot, "  ");
        System.out.println("Extracted Files:");
        codeFiles.forEach((path, content) -> {
            System.out.println("  - " + path);
            assertNotNull(content, "File content for " + path + " is null");
            assertFalse(content.isBlank(), "File content for " + path + " is blank");
            assertTrue(content.length() > 20, "Content of " + path + " looks suspiciously short");
        });

        System.out.println("Extracted File Contents:");
codeFiles.forEach((path, content) -> {
    System.out.println("▶️ " + path);
    System.out.println(content);
    System.out.println("---");
});

    }


    @Test
    public void testRealRepoCodeExtraction() throws Exception {
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("GITHUB_PAT");
        assertNotNull(token, "GITHUB_PAT must be set in .env for this test");
    
        ArchitectureUtils architectureUtils = new ArchitectureUtils();
        ServiceBoundaryUtils serviceBoundary = new ServiceBoundaryUtils();
    

        GitHubApi realApi = new GitHubApi(architectureUtils, serviceBoundary);

        String repoUrl = "https://github.com/spring-projects/spring-petclinic";
        BinaryTreeNode root = realApi.inspectRepo(repoUrl);
        assertNotNull(root, "Root from real repo should not be null");
    
        Map<String, String> codeFiles = realApi.extractCode(root);
        assertFalse(codeFiles.isEmpty(), "Expected code files from real repo");
    
        System.out.println("Extracted file count: " + codeFiles.size());
        codeFiles.forEach((path, content) -> System.out.println(" " + path));
    
        System.out.println("Tree Structure:");
        GitHubApi.printTree(root, "  ");
    }
    


}
    
