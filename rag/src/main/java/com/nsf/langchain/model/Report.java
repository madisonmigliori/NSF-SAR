package com.nsf.langchain.model;

import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    private String repoId;
    private String dependencies;
    private String analysis;
    private String architecture;
    private String serviceBoundary;
    private String recommendations;
    private String architectureRec;


     private String formatDependencies(String dependencies) {
        if (dependencies == null || dependencies.isBlank()) return "No dependencies found or failed to extract.";
        return Arrays.stream(dependencies.split("\n"))
                     .map(dep -> "- " + dep.trim())
                     .collect(Collectors.joining("\n"));
    }
    
    @Override
    public String toString() {
        return """
            === Repository Analysis Report ===
    
            Repository ID: %s
    
            --- Dependency Overview ---
            %s
    
            --- Architecture Analysis ---
            %s
    
            --- Current Architecture Diagram ---
            %s
    
            --- Identified Service Boundaries ---
            %s
    
            --- Recommended Improvements ---
            %s

            --- Refactored Architecture Plan ---
            %s
            """.formatted(
            repoId,
            dependencies,
            analysis,
            architecture,
            serviceBoundary,
            recommendations,
            architectureRec
        );
    }
    
    
}
