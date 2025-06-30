package com.nsf.langchain.service;

import org.springframework.stereotype.Service;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


@Service
public class JsonEmbeddingService {

    public Map<Path, String> readJson() {
        Map<Path, String> result = new HashMap<>();

        try {
            String scoringJson = Files.readString(Path.of("doc/msa-scoring.json"));
            String patternsJson = Files.readString(Path.of("doc/msa-patterns.json"));


        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
