package com.nsf.langchain;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;

import java.util.ArrayList;
import java.util.List;

public class InMemoryVectorStore implements VectorStore {

    private final List<Document> storedDocs = new ArrayList<>();

    @Override
    public void add(List<Document> documents) {
        storedDocs.addAll(documents);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest searchRequest) {
        String query = searchRequest.getQuery().toLowerCase();
        List<Document> results = new ArrayList<>();

        for (Document doc : storedDocs) {
            if (doc.getText().toLowerCase().contains(query)) {
                results.add(doc);
            }
        }

        return results;
    }

    @Override
    public void delete(List<String> idList) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }

    @Override
    public void delete(Expression filterExpression) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'delete'");
    }
}
