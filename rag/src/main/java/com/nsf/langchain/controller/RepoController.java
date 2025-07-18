package com.nsf.langchain.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nsf.langchain.service.IngestionService;
import com.nsf.langchain.utils.ArchitectureUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/repos")
@Tag(name = "Repository Ingestion", description = "Endpoints for ingesting and analyzing GitHub repositories in the RAG system")
public class RepoController {

    private static final Logger log = LoggerFactory.getLogger(RepoController.class);

    private final IngestionService ingestionService;
    private final ArchitectureUtils architectureUtils;

    public RepoController(IngestionService ingestionService, ArchitectureUtils architectureUtils) {
        this.ingestionService = ingestionService;
        this.architectureUtils = architectureUtils;
    }

    @PostMapping("/ingest")
    @Operation(
            summary = "Ingest repo via Git clone",
            description = "Clones the repository and ingests files from the local filesystem."
    )
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


    // @PostMapping("/git")
    // @Operation(
    //         summary = "Ingest repo via GitHub API",
    //         description = "Uses the GitHub API to crawl the repository and ingest file contents directly."
    // )
    // public ResponseEntity<String> ingestGit(@RequestParam String gitUrl) {
    //     try {
    //         ingestionService.ingestGitRepoAPI(gitUrl);
    //         return ResponseEntity.ok("Ingestion started.");
    //     } catch (Exception e) {
    //         log.error("Failed to ingest repo {}", gitUrl, e);
    //         return ResponseEntity
    //                 .status(500)
    //                 .body("Error ingesting repo: " + e.getMessage());
    //     }
    // }

    
}
