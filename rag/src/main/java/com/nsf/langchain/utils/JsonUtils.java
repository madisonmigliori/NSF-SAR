package com.nsf.langchain.utils;

import com.nsf.langchain.model.Scoring;
import com.nsf.langchain.model.Pattern;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonUtils {
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<Scoring> loadScoringJson(Path filePath){
        try{
            JsonNode root = objectMapper.readTree(filePath.toFile());
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
    public List<Pattern> loadPatternsJson(Path filePath){
        try{
            JsonNode root = objectMapper.readTree(filePath.toFile());
            JsonNode criteriaNode = root.get("patterns");

            if(criteriaNode == null || !criteriaNode.isArray()){
                log.warn("No patterns array found: {}", filePath);
                return List.of();
            }
            return objectMapper.readerForListOf(Pattern.class).readValue(criteriaNode);
        } catch (IOException e){
            log.error("Error reading patterns json {}: {}", filePath, e.getMessage());
            return List.of();
        }

    }
    
}
