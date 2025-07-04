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
    private String anaylsis;
    private String architecture;
    private String recommendations;
    private String architectureRec;


     private String formatDependencies(String raw) {
        if (raw == null || raw.isBlank()) return "No dependencies found or failed to extract.";
        return Arrays.stream(raw.split("\n"))
                     .map(dep -> "- " + dep.trim())
                     .collect(Collectors.joining("\n"));
    }
    
    public String toString(){
        return """

        Hey I have done an extensive anaylsis on your repository:  %s. 

        Here is a list of all your dependencies:
        
        %s

        Architecture Anaylsis: 
        
        %s

        Recommendations to improve your architecture:
        
        %s

        Current Architecture: 
        
        %s

        Refactored Architecture: 
        
        %s
     
        """.formatted(repoId, formatDependencies(dependencies), anaylsis, recommendations, architecture, architectureRec);

    }

}
