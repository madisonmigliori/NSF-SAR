package com.nsf.langchain.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nsf.langchain.model.Answer;
import com.nsf.langchain.model.Question;
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

    @PostMapping
    @Operation(
            summary = "Ask a question about your GitHub repo",
            description = "The LLM will analyze your repository (and JSON scoring context) and answer your architecture-related question."
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
                    .body(new Answer("Sorry. I couldn’t process your question right now. Please check logs or try again later."));
        }
    }

    @GetMapping("/ping")
    @Operation(
            summary = "RAG Service Health Check",
            description = "Quick ping to confirm that the chat service is up and running."
    )
    public String ping() {
        return "Status: UP";
    }
}
