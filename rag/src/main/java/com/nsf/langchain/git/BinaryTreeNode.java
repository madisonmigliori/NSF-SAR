package com.nsf.langchain.git;

import java.util.ArrayList;
import java.util.List;

public class BinaryTreeNode {
    String name;
    String type;
    String path;
    List<BinaryTreeNode> children;
    Boolean inbounds;

    // for all child and parent nodes
    public BinaryTreeNode(String name, String type, String path, Boolean inbounds) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.children = new ArrayList<BinaryTreeNode>();
        inbounds = false;
    }

    public void addChild(BinaryTreeNode parent, BinaryTreeNode child){
        parent.children.add(child);
    }

}