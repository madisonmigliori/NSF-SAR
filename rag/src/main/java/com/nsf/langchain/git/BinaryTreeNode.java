package com.nsf.langchain.git;

import java.util.ArrayList;
import java.util.List;

public class BinaryTreeNode {
    public String name;
    public String type;
    public String url;
    public String content;
    public List<BinaryTreeNode> children;

    // For directories and repos
    public BinaryTreeNode(String name, String type, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.children = new ArrayList<>();
    }

    // For files
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
}
