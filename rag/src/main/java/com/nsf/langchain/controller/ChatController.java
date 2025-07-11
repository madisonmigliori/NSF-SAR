package com.nsf.langchain.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nsf.langchain.model.Answer;
import com.nsf.langchain.model.Question;
import com.nsf.langchain.model.Repo;
import com.nsf.langchain.model.Report;
import com.nsf.langchain.service.RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat Controller", description = "Endpoints for interacting with the LLM and retrieving architecture insights.")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private RagService ragService;

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @PostMapping
    @Operation(
            summary = "Chat with the controller",
            description = "Ask the model a question based on a specific repository's ingested context."
    )
    public ResponseEntity<Answer> chat(@RequestBody Question q) {
        try {
            log.info("Received question: '{}' for repo: '{}'", q.getText(), q.getRepoId());
            String response = ragService.answer(q.getText(), q.getRepoId());
            return ResponseEntity.ok(new Answer(response));
        } catch (Exception e) {
            log.error("Chat processing failed for question '{}'", q.getText(), e);
            return ResponseEntity
                    .internalServerError()
                    .body(new Answer("Sorry, I couldn't process your question right now."));
        }
    }

    @PostMapping("/analyze")
@Operation(
    summary = "Analyze a repository",
    description = "Runs dependency extraction, architecture analysis, and gives AI recommendations for the given repo URL."
)
public ResponseEntity<Report> analyze(@RequestBody Repo gitUrl) {
    try {
        Report report = ragService.getReport(gitUrl.getUrl());
        return ResponseEntity.ok(report);
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError().body(
            new Report(
                gitUrl.getUrl(),
                "Error extracting dependencies.",
                "Error running analysis.",
                "Error displaying architecture.",
                "Error providing recommendations.",
                "Error identifying service boundary.",
                "Error displaying refactored architecture"
            )
        );
    }
}

    @GetMapping("/ping")
    @Operation(
            summary = "Controller Health Check",
            description = "Verifies that the RAG system is running and responding."
    )
    public String ping() {
        return "Status: UP";
    }
}
