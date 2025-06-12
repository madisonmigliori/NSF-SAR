package com.nsf.langchain.model;

//Template for the model taking in the question
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Question{
    String text;

    public String getText() {
        return text;
    }
}
