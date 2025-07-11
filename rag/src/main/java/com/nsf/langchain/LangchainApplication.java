package com.nsf.langchain;

import com.nsf.langchain.service.IngestionService;
import com.nsf.langchain.service.RagService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class LangchainApplication {

    private static final String INGEST_LOG = ".ingested.log";

    public static void main(String[] args) {
        SpringApplication.run(LangchainApplication.class, args);
    }

}