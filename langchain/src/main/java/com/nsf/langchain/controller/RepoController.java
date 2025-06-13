package com.nsf.langchain.controller;

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

    @Autowired
    private IngestionService ingestionService;

    @PostMapping("/ingest")
    public ResponseEntity<String> ingest(@RequestParam String gitUrl) throws Exception {
        ingestionService.ingestRepo(gitUrl);
        return ResponseEntity.ok("Ingested " + gitUrl);
    }
}
