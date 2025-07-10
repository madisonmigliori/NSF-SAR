package com.nsf.langchain.git;

import java.util.ArrayList;
import java.util.List;

public class BinaryTreeNode {
    String name, type, url, content;
    List<BinaryTreeNode> children;
    String size;

    // for any dirs and the initial repo because allows for children 
    //  and doesnt require content
    public BinaryTreeNode(String name, String type, String url, String size) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.children = new ArrayList<BinaryTreeNode>();
        this.size = size;
    }

    // for files in the github because they cannot have children 
    public BinaryTreeNode(String name, String type, String url, String content, String size) {
        this.name = name;
        this.type = type;
        this.url = url;
        this.content = content;
        this.size = size;
    }

    public void addChild(BinaryTreeNode parent, BinaryTreeNode child){
        parent.children.add(child);
    }

    public String getName(){
        return name;
    }

    public String getType(){
        return type;
    }
    public String getUrl(){
        return url;
    }
    public List<BinaryTreeNode> getChildren(){
        return children;
    }
    public String getContent(){
        return content;
    }
    public String getSize(){
        return size;
    }

}