package com.nsf.langchain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Pattern {
    private String name;
    private String description;
    private String advantage;
    private String disadvantage;

    @JsonProperty("common implementations")
    private String common;

    @Override
    public String toString(){
     return String.format("""
             === Patterns ===
             Name: %s

             Description: %s

             Advantage: %s

             Disadvantages: %s

             Common Implemenations: %s

             """, name, description, advantage, disadvantage, common);
    }
}
