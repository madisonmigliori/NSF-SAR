package com.nsf.langchain.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    
    @Override
    public String toString(){
     return String.format("""
             === Scoring ===
             Name: %s

             Description: %s

             Guidance: %s

             Patterns: %s

             Weight: %s

             """, name, description, guidance, patterns, weight);
    }
}
