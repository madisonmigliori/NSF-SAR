package com.nsf.langchain.configuration;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfiguration {

    @Value("${app.chroma-endpoint}")
    private String chromaUrl;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

}
