package com.nsf.langchain.git;

import java.util.ArrayList;
import java.util.List;

public class BinaryTreeNode {
    public String name;
    public String type;
    String url;
    String content;
    public List<BinaryTreeNode> children;

    // for any dirs and the initial repo because allows for children 
    //  and doesnt require content
    public BinaryTreeNode(String name, String type, String url) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.children = new ArrayList<BinaryTreeNode>();
    }

    // for files in the github because they cannot have children 
    public BinaryTreeNode(String name, String type, String url, String content) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.content = content;
    }

    public void addChild(BinaryTreeNode parent, BinaryTreeNode child){
        parent.children.add(child);
    }
}