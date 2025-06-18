package com.nsf.langchain;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

@SpringBootTest
public class LangchainApplicationTests {


    @Test
    void contextLoads() {
       
    }


    @TestConfiguration
public class MockChromaConfig {
    @Bean
    public ChromaEmbeddingStore chromaEmbeddingStore() {
        return Mockito.mock(ChromaEmbeddingStore.class);
    }
}

}
