package com.nsf.langchain;

import com.nsf.langchain.client.ChatClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class ManualWiringTest {

    @Autowired(required = false)
    ChatClient chatClient;

    @Test
    void checkChatClient() {
        assertNotNull(chatClient, "❌ ChatClient is NOT being wired by Spring!");
        System.out.println("✅ ChatClient wired: " + chatClient.getClass().getName());
    }
}
