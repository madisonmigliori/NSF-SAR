package com.nsf.langchain.git;

import java.util.ArrayList;
import java.util.List;

public class BinaryTreeNode {
    String name;
    String type;
    String path;
    List<BinaryTreeNode> children;
    ArrayList<BinaryTreeNode> inbounds;

    // for all child and parent nodes other than root
    public BinaryTreeNode(String name, String type, String path) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.children = new ArrayList<BinaryTreeNode>();
    }

    // for root to be able to identify inbounds microservices wtih files from files
    public BinaryTreeNode(String name, String type, String path, ArrayList<BinaryTreeNode> inbounds){
        this.name = name;
        this.type = type;
        this.path = path;
        this.children = new ArrayList<BinaryTreeNode>();
        this.inbounds = inbounds;
    }

    public void addChild(BinaryTreeNode parent, BinaryTreeNode child){
        parent.children.add(child);
    }

}