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

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private RagService ragService;

 
    @PostMapping
    public ResponseEntity<Answer> chat(@RequestBody Question q) {
        try {
            String response = ragService.answer(q.getText());
            return ResponseEntity.ok(new Answer(response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .internalServerError()
                    .body(new Answer("Sorry, I couldn't process your question right now."));
        }
    }

    @GetMapping("/ping")
    public String ping() {
        return "RAG assistant is running.";
    }
}
