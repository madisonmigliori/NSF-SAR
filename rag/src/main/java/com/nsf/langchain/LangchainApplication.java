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

    @Bean
    public ApplicationRunner runner(IngestionService ingestionService, RagService ragService) {
        return args -> {
            Set<String> ingestedFiles = loadIngestedLog();
            Path docFolder = Paths.get("doc");

            System.out.println("Scanning for new PDFs in /doc...");

            try (Stream<Path> pdfFiles = Files.walk(docFolder)) {
                List<Path> toIngest = pdfFiles
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".pdf"))
                        .filter(path -> !ingestedFiles.contains(path.toString()))
                        .collect(Collectors.toList());

                AtomicInteger count = new AtomicInteger();
                int total = toIngest.size();

                for (Path path : toIngest) {
                    System.out.printf("Ingesting (%d/%d): %s... ", count.incrementAndGet(), total, path.getFileName());
                    ingestionService.ingestLocalPdf("doc", path);
                    System.out.println("done");
                    saveToIngestedLog(path.toString());
                }
            } catch (Exception e) {
                System.err.println("❌ PDF ingestion failed: " + e.getMessage());
            }

            // Scanner scanner = new Scanner(System.in);
            // System.out.print("\n🔗 Enter GitHub repo URL to ingest: ");
            // String repoUrl = scanner.nextLine().trim();
            // String encodedUrl = URLEncoder.encode(repoUrl, "UTF-8");

            // try {
            //     URL requestUrl = new URL("http://localhost:8080/api/repos/ingest?gitUrl=" + encodedUrl);
            //     HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
            //     conn.setRequestMethod("POST");
            //     conn.setConnectTimeout(10_000);
            //     conn.setReadTimeout(10_000);

            //     Instant start = Instant.now();
            //     System.out.print("⏳ Ingesting repo...");
            //     int code = conn.getResponseCode();
            //     Instant end = Instant.now();

            //     System.out.println(" done in " + Duration.between(start, end).toSeconds() + "s.");

            //     if (code == HttpURLConnection.HTTP_OK) {
            //         System.out.println("Repo ingestion triggered successfully.");
            //     } else {
            //         System.err.println("Server responded with: " + code);
            //         try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            //             String line;
            //             while ((line = in.readLine()) != null) System.err.println(line);
            //         }
            //     }
            // } catch (Exception e) {
            //     System.err.println("Ingestion request failed: " + e.getMessage());
            // }

            // String repoId = repoUrl.replaceAll("[^a-zA-Z0-9]", "_");
            // while (true) {
            //     System.out.print("\n Ask about your repo (type /bye to exit): ");
            //     String question = scanner.nextLine().trim();

            //     if (question.equalsIgnoreCase("/bye")) {
            //         System.out.println("👋 Bye for now!");
            //         break;
            //     }

            //     String answer = ragService.answer(question, repoId);
            //     System.out.println("Answer:\n" + answer);
            // }
        };
    }

    private Set<String> loadIngestedLog() {
        try {
            Path logPath = Paths.get(INGEST_LOG);
            if (Files.exists(logPath)) {
                return new HashSet<>(Files.readAllLines(logPath));
            }
        } catch (IOException e) {
            System.err.println("Could not load ingestion log: " + e.getMessage());
        }
        return new HashSet<>();
    }

    private void saveToIngestedLog(String path) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(INGEST_LOG), StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(path);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("⚠️ Failed to update ingestion log: " + e.getMessage());
        }
    }
}
