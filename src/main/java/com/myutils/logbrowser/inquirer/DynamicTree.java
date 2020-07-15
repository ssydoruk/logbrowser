/*
 Copyright 2010 Vivin Suresh Paliath
 Distributed under the BSD License
 */
package com.myutils.logbrowser.inquirer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import com.myutils.mygenerictree.GenericTreeTraversalOrderEnum;

public class DynamicTree<OptionNode> implements Serializable {

    private static final long serialVersionUID = 1L;

    private DynamicTreeNode<OptionNode> root;

    public DynamicTree() {
        super();
    }

    public DynamicTree(DynamicTree<OptionNode> src) {
        this();
        setRoot(new DynamicTreeNode<>(src.getRoot()));
    }

    public DynamicTreeNode<OptionNode> getRoot() {
        return this.root;
    }

    public void setRoot(DynamicTreeNode<OptionNode> root) {
        this.root = root;
    }

    public int getNumberOfNodes() {
        int numberOfNodes = 0;

        if (root != null) {
            numberOfNodes = auxiliaryGetNumberOfNodes(root) + 1; //1 for the root!
        }

        return numberOfNodes;
    }

    private int auxiliaryGetNumberOfNodes(DynamicTreeNode<OptionNode> node) {
        int numberOfNodes = node.getNumberOfChildren();

        for (DynamicTreeNode<OptionNode> child : node.getChildren()) {
            numberOfNodes += auxiliaryGetNumberOfNodes(child);
        }

        return numberOfNodes;
    }

    public boolean exists(OptionNode dataToFind) {
        return (find(dataToFind) != null);
    }

    public DynamicTreeNode<OptionNode> find(OptionNode dataToFind) {
        DynamicTreeNode<OptionNode> returnNode = null;

        if (root != null) {
            returnNode = auxiliaryFind(root, dataToFind);
        }

        return returnNode;
    }

    private DynamicTreeNode<OptionNode> auxiliaryFind(DynamicTreeNode<OptionNode> currentNode, OptionNode dataToFind) {
        DynamicTreeNode<OptionNode> returnNode = null;
        int i = 0;

        if (currentNode.getData().equals(dataToFind)) {
            returnNode = currentNode;
        } else if (currentNode.hasChildren()) {
            i = 0;
            while (returnNode == null && i < currentNode.getNumberOfChildren()) {
                returnNode = auxiliaryFind(currentNode.getChildAt(i), dataToFind);
                i++;
            }
        }

        return returnNode;
    }

    public boolean isEmpty() {
        return (root == null);
    }

    public List<DynamicTreeNode<OptionNode>> build(GenericTreeTraversalOrderEnum traversalOrder) {
        List<DynamicTreeNode<OptionNode>> returnList = null;

        if (root != null) {
            returnList = build(root, traversalOrder);
        }

        return returnList;
    }

    public List<DynamicTreeNode<OptionNode>> build(DynamicTreeNode<OptionNode> node, GenericTreeTraversalOrderEnum traversalOrder) {
        List<DynamicTreeNode<OptionNode>> traversalResult = new ArrayList<>();

        if (traversalOrder == GenericTreeTraversalOrderEnum.PRE_ORDER) {
            buildPreOrder(node, traversalResult);
        } else if (traversalOrder == GenericTreeTraversalOrderEnum.POST_ORDER) {
            buildPostOrder(node, traversalResult);
        }

        return traversalResult;
    }

    private void buildPreOrder(DynamicTreeNode<OptionNode> node, List<DynamicTreeNode<OptionNode>> traversalResult) {
        traversalResult.add(node);

        for (DynamicTreeNode<OptionNode> child : node.getChildren()) {
            buildPreOrder(child, traversalResult);
        }
    }

    private void buildPostOrder(DynamicTreeNode<OptionNode> node, List<DynamicTreeNode<OptionNode>> traversalResult) {
        for (DynamicTreeNode<OptionNode> child : node.getChildren()) {
            buildPostOrder(child, traversalResult);
        }

        traversalResult.add(node);
    }

    public Map<DynamicTreeNode<OptionNode>, Integer> buildWithDepth(GenericTreeTraversalOrderEnum traversalOrder) {
        Map<DynamicTreeNode<OptionNode>, Integer> returnMap = null;

        if (root != null) {
            returnMap = buildWithDepth(root, traversalOrder);
        }

        return returnMap;
    }

    public Map<DynamicTreeNode<OptionNode>, Integer> buildWithDepth(DynamicTreeNode<OptionNode> node, GenericTreeTraversalOrderEnum traversalOrder) {
        Map<DynamicTreeNode<OptionNode>, Integer> traversalResult = new LinkedHashMap<>();

        if (traversalOrder == GenericTreeTraversalOrderEnum.PRE_ORDER) {
            buildPreOrderWithDepth(node, traversalResult, 0);
        } else if (traversalOrder == GenericTreeTraversalOrderEnum.POST_ORDER) {
            buildPostOrderWithDepth(node, traversalResult, 0);
        }

        return traversalResult;
    }

    private void buildPreOrderWithDepth(DynamicTreeNode<OptionNode> node, Map<DynamicTreeNode<OptionNode>, Integer> traversalResult, int depth) {
        traversalResult.put(node, depth);

        for (DynamicTreeNode<OptionNode> child : node.getChildren()) {
            buildPreOrderWithDepth(child, traversalResult, depth + 1);
        }
    }

    private void buildPostOrderWithDepth(DynamicTreeNode<OptionNode> node, Map<DynamicTreeNode<OptionNode>, Integer> traversalResult, int depth) {
        for (DynamicTreeNode<OptionNode> child : node.getChildren()) {
            buildPostOrderWithDepth(child, traversalResult, depth + 1);
        }

        traversalResult.put(node, depth);
    }

    public String toString() {
        /*
         We're going to assume a pre-order traversal by default
         */

        String stringRepresentation = "";

        if (root != null) {
            stringRepresentation = build(GenericTreeTraversalOrderEnum.PRE_ORDER).toString();

        }

        return stringRepresentation;
    }

    public String toStringWithDepth() {
        /*
         We're going to assume a pre-order traversal by default
         */

        String stringRepresentation = "";

        if (root != null) {
            stringRepresentation = buildWithDepth(GenericTreeTraversalOrderEnum.PRE_ORDER).toString();
        }

        return stringRepresentation;
    }

    private void writeObject(ObjectOutputStream s)
            throws IOException {
        s.defaultWriteObject();
        s.writeObject(root);
    }

    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        root = (DynamicTreeNode<OptionNode>) s.readObject();
    }

    void removeDynamicChildren() {
        getRoot().removeDynamicChildren();
    }
}
