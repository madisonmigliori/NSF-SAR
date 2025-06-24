package com.nsf.langchain.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nsf.langchain.service.IngestionService;

@RestController
@RequestMapping("/api/repos")
public class RepoController {

    private static final Logger log = LoggerFactory.getLogger(RepoController.class);

    private final IngestionService ingestionService;

    public RepoController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

   
    @PostMapping("/ingest")
    public ResponseEntity<String> ingest(@RequestParam String gitUrl) {
        try {
            ingestionService.ingestRepo(gitUrl);
            return ResponseEntity.ok("Ingestion started.");
        } catch (Exception e) {
            log.error("Failed to ingest repo {}", gitUrl, e);
            return ResponseEntity
                    .status(500)
                    .body("Error ingesting repo: " + e.getMessage());
        }
    }

    
}
