package com.nsf.langchain.model;

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
     
        """.formatted(repoId, dependencies, anaylsis, recommendations, architecture, architectureRec);

    }

}
