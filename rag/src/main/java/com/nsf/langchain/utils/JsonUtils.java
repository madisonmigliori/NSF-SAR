package com.nsf.langchain.utils;

import com.nsf.langchain.model.Scoring;
import com.nsf.langchain.model.AntiPattern;
import com.nsf.langchain.model.MSAPattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



@Component
public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static List<Scoring> loadScoringJson(Path filePath){
        try{
            String content = Files.readString(filePath);
            JsonNode root = objectMapper.readTree(content);
            JsonNode criteriaNode = root.get("criteria");

            if(criteriaNode == null || !criteriaNode.isArray()){
                log.warn("No criteria array found: {}", filePath);
                return List.of();
            }
            return objectMapper.readerForListOf(Scoring.class).readValue(criteriaNode);
        } catch (IOException e){
            log.error("Error reading scoring json {}: {}", filePath, e.getMessage());
            return List.of();
        }

    }
    public static List<MSAPattern> loadPatternsJson(Path filePath){
        try{
            String content = Files.readString(filePath);
            JsonNode root = objectMapper.readTree(content);
            JsonNode patternNode = root.get("patterns");

            if(patternNode == null || !patternNode.isArray()){
                log.warn("No patterns array found: {}", filePath);
                return List.of();
            }
            return objectMapper.readerForListOf(MSAPattern.class).readValue(patternNode);
        } catch (IOException e){
            log.error("Error reading patterns json {}: {}", filePath, e.getMessage());
            return List.of();
        }

    }

    public Map<String, List<String>> loadDependencyJson(Path filePath) {
    try {
        String content = Files.readString(filePath);
        JsonNode root = objectMapper.readTree(content);

        Map<String, List<String>> categoryMap = new HashMap<>();

        root.fields().forEachRemaining(entry -> {
            String category = entry.getKey();
            JsonNode depsNode = entry.getValue();
            if (depsNode.isArray()) {
                List<String> deps = new ArrayList<>();
                depsNode.forEach(depNode -> deps.add(depNode.asText()));
                categoryMap.put(category, deps);
            }
        });

        return categoryMap;
    } catch (IOException e) {
        log.error("Error reading dependency category json {}: {}", filePath, e.getMessage());
        return Map.of();
    }

    
}
public static List<AntiPattern> loadAntiPatternsJson(Path filePath){
    try{
        String content = Files.readString(filePath);
        JsonNode root = objectMapper.readTree(content);
        JsonNode antiNode = root.get("anti-patterns");

        if(antiNode == null || !antiNode.isArray()){
            log.warn("No anti-patterns array found: {}", filePath);
            return List.of();
        }
        return objectMapper.readerForListOf(AntiPattern.class).readValue(antiNode);

        
    } catch (IOException e){
        log.error("Error reading patterns json {}: {}", filePath, e.getMessage());
        return List.of();

        
    }

}

    
    
}
