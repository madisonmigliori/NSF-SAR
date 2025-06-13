package com.nsf.langchain;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

@SpringBootTest
public class LangchainApplicationTests {

    // This mocks the bean that tries to connect to Chroma
    @MockBean
    private ChromaEmbeddingStore chromaEmbeddingStore;

    @Test
    void contextLoads() {
        // We’re just checking that the Spring context starts up.
        // This test is intentionally minimal.
    }
}
