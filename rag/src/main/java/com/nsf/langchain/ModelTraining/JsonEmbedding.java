package com.nsf.langchain.ModelTraining;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonObject;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class JsonEmbedding {
    public static void main(String[] args) {
        try {
            FileInputStream OptimizingScore = new FileInputStream("Criteria/OptimizeScoring.json");
            FileInputStream Patterns = new FileInputStream("Criteria/Patterns.json");

            JsonReader OptimizingScoreReader = Json.createReader(OptimizingScore);
            JsonReader PatternsReader = Json.createReader(Patterns);

            JsonObject OptimizeScoreObject = OptimizingScoreReader.readObject();
            JsonObject PatternsObject = PatternsReader.readObject();

            // String name = OptimizeScoreObject.getString("name");

            OptimizingScoreReader.close();
            PatternsReader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
