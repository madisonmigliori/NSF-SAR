package com.nsf.langchain.git;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinaryTreeNode {
    public String name;
    public String type;
    public String url;
    public String content;
    public List<BinaryTreeNode> children;
    public List<BinaryTreeNode> fileNodes;
    public List<String> warnings = new ArrayList<>(); 
    public List<String> patterns = new ArrayList<>(); 
    public List<String> antiPatterns = new ArrayList<>(); 
    public List<String> config = new ArrayList<>(); 
    public List<String> allWarnings = new ArrayList<>();
    public Map<String, Integer> patternCounts = new HashMap<>();
    public Map<String, Integer> antiPatternCounts = new HashMap<>();
    public double severityScore = 0.0;
    public Map<String, Double> scoringResults = new HashMap<>();
    public String dependenciesSummary = "";





    public BinaryTreeNode(String name, String type, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.children = new ArrayList<>();
    }


    public BinaryTreeNode(String name, String type, String url, String content) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.content = content;
        this.children = new ArrayList<>(); 
    }

    @Override
    public String toString() {
        return type + ": " + name;
    }

    public static List<BinaryTreeNode> flattenFiles(BinaryTreeNode root) {
        List<BinaryTreeNode> files = new ArrayList<>();
        traverse(root, files);
        return files;
    }
    
    private static void traverse(BinaryTreeNode node, List<BinaryTreeNode> files) {
        if (node == null) return;
        if ("file".equals(node.type)) files.add(node);
        if (node.children != null) {
            for (BinaryTreeNode child : node.children) {
                traverse(child, files);
            }
        }
    }
    
}
