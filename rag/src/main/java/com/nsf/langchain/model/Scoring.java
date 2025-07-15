package com.nsf.langchain.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Scoring {
    private String name;
    private String description;
    private String guidance;
    private List<String> patterns;
    private double weight;


    private Map<String, Integer> matchedPatternCounts = new HashMap<>();
    private int totalMatches = 0;

    public void addMatch(String pattern) {
        matchedPatternCounts.merge(pattern, 1, Integer::sum);
        totalMatches++;
    }

    @Override
    public String toString(){
        return String.format("""
            === Scoring ===
            Name: %s

            Description: %s

            Guidance: %s

            Patterns: %s

            Weight: %s

            Total Matches: %d

            Matched Pattern Counts: %s

            """, name, description, guidance, patterns, weight, totalMatches, matchedPatternCounts);
    }
}

