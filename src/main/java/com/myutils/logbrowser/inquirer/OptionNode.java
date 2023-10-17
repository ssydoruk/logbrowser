package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import com.myutils.mygenerictree.TClonable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OptionNode implements Serializable, TClonable, Comparable {

    private static final long serialVersionUID = 2L;
    DialogItem dialogItem = null;
    private Object data;
    private boolean isEmpty = false;
    private int id;
    private Pair thePr = null;
    private boolean checked = true; // is true by default
    private String name = null;

    OptionNode(boolean selected, NameID ch1) {
        this(selected, ch1.getName());
        this.id = ch1.getId();

    }

    OptionNode(NameID ch1) {
        this(ch1.getName());
        this.id = ch1.getId();
    }

    public OptionNode(boolean b, DialogItem dialogItem) {
        this(b, dialogItem.toString());
        this.dialogItem = dialogItem;
    }

    OptionNode(boolean b, Pair pr) {
        this(b, pr.getKey().toString());
        thePr = pr;
    }

    OptionNode(boolean b, DialogItem dialogItem, String itemName) {
        this(b, dialogItem);
        if (itemName != null) {
            setName(itemName);
        }
    }

    public OptionNode(boolean checked, String name) {
        this.checked = checked;
        this.name = name;
//        inquirer.logger.debug("OptionNode constructor item [" + name + "] checked:" + checked );
    }

    public OptionNode(boolean checked, boolean isEmpty, String name) {
        this.checked = checked;
        this.name = name;
        this.isEmpty = isEmpty;
//        inquirer.logger.debug("OptionNode constructor item [" + name + "] checked:" + checked );
    }

    public OptionNode(String name) {
        this(true, name);
    }

    public OptionNode(DialogItem item) {
        this(true, item);
    }

    static ArrayList<String> getChecked(DynamicTreeNode<OptionNode> nameSettings) {
        ArrayList<String> ret = new ArrayList<>();
        for (DynamicTreeNode<OptionNode> col : nameSettings.getChildren()) {
            OptionNode data = (OptionNode) col.getData();
            if (data.isChecked()) {
                ret.add(data.getName());
            }
        }
        return ret;
    }

    static ArrayList<Integer> getCheckedIDs(DynamicTreeNode<OptionNode> nameSettings) {
        ArrayList<Integer> ret = new ArrayList<>();
        for (DynamicTreeNode<OptionNode> col : nameSettings.getChildren()) {
            OptionNode data = (OptionNode) col.getData();
            if (data.isChecked()) {
                ret.add(data.nameID());
            }
        }
        return ret;
    }

    static String getEmptyClause(DynamicTreeNode<OptionNode> nameSettings) {
//        for (DynamicTreeNode<OptionNode> col : nameSettings.getChildren()) {
//            OptionNode data = (OptionNode) col.getData();
//            if (data.isIsEmpty()) {
//                StringBuilder ret = new StringBuilder(15);
//                ret.append("is ");
//                if (!data.isChecked()) {
//                    ret.append("not");
//                }
//                ret.append(" null");
//                return ret.toString();
//            }
//        }
        return null;
    }

    static String getCheckedIDsList(DynamicTreeNode<OptionNode> nameSettings) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    static String checkedChildrenList(String Field, DynamicTreeNode<OptionNode> nameSettings) {
        ArrayList<Integer> ret = new ArrayList<>();
        for (DynamicTreeNode<OptionNode> col : nameSettings.getChildren()) {
            OptionNode data = (OptionNode) col.getData();
            if (data.isChecked() && data.nameID() > 0) {
                ret.add(data.nameID());
            }
        }
        Collections.sort(ret);
        return IQuery.getIDsList(Field, ret, false, false);
    }

    static ChildrenChecked getChildrenChecked(DynamicTreeNode<OptionNode> nameSettings) {
        List<DynamicTreeNode<OptionNode>> children = nameSettings.getChildren();
        int checked = 0, unchecked = 0;

        for (int i = 0; i < children.size(); i++) {
            DynamicTreeNode<OptionNode> get = children.get(i);
            if (get != null) {
                OptionNode data = (OptionNode) get.getData();
                if (!data.isChecked()) {
                    unchecked++;
                } else {
                    checked++;
                }
                if (checked > 0 && unchecked > 0) {
                    return ChildrenChecked.PARTIALLY_CHECKED;
                }
            }
        }
        ChildrenChecked ret = (checked > 0 || children.isEmpty()) ? ChildrenChecked.ALL_CHECKED : ChildrenChecked.NONE_CHECKED;
        return ret;
//        return (checked > 0) ? ChildrenChecked.ALL_CHECKED : ChildrenChecked.NONE_CHECKED;

    }

    static boolean AllChildrenChecked(DynamicTreeNode<OptionNode> nameSettings) {
        for (DynamicTreeNode<OptionNode> col : nameSettings.getChildren()) {
            OptionNode data = (OptionNode) col.getData();
            if (!data.isChecked()) {
                return false;
            }
        }
        return true;

    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean logCheckAndUpdate(String Name) {
        if (Name == null || Name.isEmpty()) {
            return false;
        }

        if (inquirer.getFirstWord(Name).equals(inquirer.getFirstWord(getName()))) {
            this.name = inquirer.commonString(Name, getName());
            return true;
        }
        return false;
    }

    public boolean isIsEmpty() {
        return isEmpty;
    }

    public String nameIfChecked() {
        if (isChecked()) {
            return getName();
        } else {
            return null;
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }

    public DialogItem getDialogItem() {
        return dialogItem;
    }

    public void setDialogItem(DialogItem dialogItem) {
        this.dialogItem = dialogItem;
    }

    public boolean isChecked() {
//        inquirer.logger.debug("isChecked returns " + checked + " item [" + name + "]");
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked != checked) {
//            inquirer.logger.debug("setChecked to " + checked + " item [" + name + "]");
            this.checked = checked;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
//        return "OptionNode{" + "thePr=" + thePr + ", checked=" + checked + ", name=" + name + ", dialogItem=" + dialogItem + '}';
    }

    @Override
    public TClonable copy() {
        OptionNode optionNode = new OptionNode(checked, name);
        optionNode.setDialogItem(dialogItem);
        return optionNode;
    }

    private void writeObject(ObjectOutputStream s)
            throws IOException {
        s.defaultWriteObject();
        s.writeBoolean(checked);
        s.writeUTF(name);
//        inquirer.logger.debug("Saved node ["+name+"] checked: "+checked);
    }

    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        checked = s.readBoolean();
        name = s.readUTF();
//        inquirer.logger.debug("Read node ["+name+"] checked: "+checked);
    }

    String getValue() {
        return (thePr != null && thePr.getValue() != null) ? (String) thePr.getValue() : "";
    }

    private Integer nameID() {
        return id;
    }

    @Override
    public int compareTo(Object o) {
        return getName().compareToIgnoreCase(((OptionNode) o).getName());
    }

    public enum ChildrenChecked {
        ALL_CHECKED,
        NONE_CHECKED,
        PARTIALLY_CHECKED
    }

}
