package com.nsf.langchain;

import org.junit.jupiter.api.Test;

import com.nsf.langchain.git.GitHubApi;

import static org.junit.jupiter.api.Assertions.*;

public class GitHubApiTest {

    private final GitHubApi gitHubApi = new GitHubApi();

    @Test
    void testFetchPomXmlFromPetClinic() throws Exception {
        String user = "spring-projects";
        String repo = "spring-petclinic";
        String path = "pom.xml";

        String content = gitHubApi.fetchFileContent(user, repo, path);

        assertNotNull(content, "Content should not be null for pom.xml");
        assertTrue(content.contains("<project"), "Should contain a Maven project tag");
        System.out.println("Fetched pom.xml successfully:");
        System.out.println(content.substring(0, Math.min(content.length(), 500)));  
    }

    @Test
    void testFetchNonExistentFile() throws Exception {
        String user = "spring-projects";
        String repo = "spring-petclinic";
        String path = "non-existent-file.txt";

        String content = gitHubApi.fetchFileContent(user, repo, path);

        assertNull(content, "Content should be null for non-existent file");
        System.out.println("Correctly handled missing file.");
    }
}
