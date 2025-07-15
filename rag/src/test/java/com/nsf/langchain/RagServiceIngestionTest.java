package com.nsf.langchain;

import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.git.GitHubApi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class RagServiceIngestionTest {

    @Autowired
    private GitHubApi gitHubApi;

    @Test
    public void testInspectRepo_ingestsAndPrintsStructure() {
        String testUrl = "https://github.com/spring-projects/spring-petclinic-microservices"; 

        try {
            BinaryTreeNode root = gitHubApi.inspectRepo(testUrl);
            assertNotNull(root, "Ingestion returned null BinaryTreeNode");
            System.out.println("Ingested Tree Structure:\n" + root.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Ingestion threw an exception: " + e.getMessage());
        }
    }
}
