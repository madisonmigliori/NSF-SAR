package com.nsf.langchain.controller;

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
@Tag(name = "Chat Controller", description = "Endpoints for communicating with the model")
public class ChatController {

    @Autowired
    private RagService ragService;

 
    @PostMapping
    @Operation(
            summary = "Chat with the controller",
            description = "The model takes the questions and responses."
    )
    public ResponseEntity<Answer> chat(@RequestBody Question q) {
        try {
            String response = ragService.answer(q.getText(), q.getRepoId());
            return ResponseEntity.ok(new Answer(response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .internalServerError()
                    .body(new Answer("Sorry, I couldn't process your question right now."));
        }
    }

    @GetMapping("/ping")
     @Operation(
            summary = "Controller's Health",
            description = "Verifies that the RAG system is running."
    )
    public String ping() {
        return "Status: UP";
    }
}

