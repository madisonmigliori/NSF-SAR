package com.nsf.langchain.controller;

import com.nsf.langchain.service.IngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/repos")
public class RepoController {

    @Autowired
    private IngestionService ingestionService;

    @PostMapping("/ingest")
    public ResponseEntity<String> ingest(@RequestParam String gitUrl) throws Exception {
        ingestionService.ingestRepo(gitUrl);
        return ResponseEntity.ok("Ingested " + gitUrl);
    }
}
