package com.nsf.langchain.client;

import java.io.IOException;

public interface ChatClient {
    String chat(String prompt) throws IOException;
}
