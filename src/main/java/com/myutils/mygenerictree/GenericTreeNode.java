/*
 Copyright 2010 Visin Suresh Paliath
 Distributed under the BSD license
 */
package com.myutils.mygenerictree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"rawtypes", "serial", "this-escape"})

public class GenericTreeNode<T> implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private TClonable data;
    private List<GenericTreeNode<T>> children;
    private GenericTreeNode<T> parent;

    public GenericTreeNode() {
        super();
//        System.out.println(" GenericTreeNode()");
        children = new ArrayList<>();
    }

    public GenericTreeNode(TClonable dataSrc) {
        this();
        data=dataSrc;
    }

    public GenericTreeNode(GenericTreeNode<T> src) {
        this();
        this.NodeCopy(src);
    }

    protected void NodeCopy(GenericTreeNode<T> src) {
        if (src != null) {
            TClonable d = src.getData();
            if (d != null) {
                setData(d.copy());
            }
            for (GenericTreeNode<T> child : src.getChildren()) {
                addChild(new GenericTreeNode<>(child));
            }
        }

    }

    public GenericTreeNode<T> getParent() {
        return this.parent;
    }

    public List<GenericTreeNode<T>> getChildren() {
        return this.children;
    }

    public void setChildren(List<GenericTreeNode<T>> children) {
        for (GenericTreeNode<T> child : children) {
            child.parent = this;
        }

        this.children = children;
    }

    public int getNumberOfChildren() {
        return getChildren().size();
    }

    public boolean hasChildren() {
        return (getNumberOfChildren() > 0);
    }

    public void addChild(GenericTreeNode<T> child) {
        child.parent = this;
        children.add(child);
    }

    public void addChildAt(int index, GenericTreeNode<T> child) throws IndexOutOfBoundsException {
        child.parent = this;
        children.add(index, child);
    }

    public void removeChildren() {
        this.children = new ArrayList<>();
    }

    public void removeChildAt(int index) throws IndexOutOfBoundsException {
        children.remove(index);
    }

    public GenericTreeNode<T> getChildAt(int index) throws IndexOutOfBoundsException {
        return children.get(index);
    }

    public TClonable getData() {
        return this.data;
    }

    public void setData(TClonable data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return getData().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GenericTreeNode<?> other = (GenericTreeNode<?>) obj;
        if (data == null) {
            return other.data == null;
        } else return data.equals(other.data);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        return result;
    }

    public String toStringVerbose() {
        String stringRepresentation = getData().toString() + ":[";

        for (GenericTreeNode<T> node : getChildren()) {
            stringRepresentation += node.getData().toString() + ", ";
        }

        //Pattern.DOTALL causes ^ and $ to match. Otherwise it won't. It's retarded.
        Pattern pattern = Pattern.compile(", $", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(stringRepresentation);

        stringRepresentation = matcher.replaceFirst("");
        stringRepresentation += "]";

        return stringRepresentation;
    }

}
