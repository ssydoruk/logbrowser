/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import static com.myutils.logbrowser.inquirer.inquirer.getFirstWord;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.mygenerictree.GenericTreeNode;
import com.myutils.mygenerictree.TClonable;

/**
 *
 * @author ssydoruk
 */
public class DynamicTreeNode<T> extends GenericTreeNode implements Serializable {

    private static final long serialVersionUID = 1L;

    public static boolean isBlockLoad() {
        return blockLoad;
    }

    public static void setBlockLoad(boolean aBlockLoad) {
        blockLoad = aBlockLoad;
    }

    private boolean dynamicChildren = false;
    private boolean ChildrenLoaded = false;

    private static boolean blockLoad = false;
    private static boolean noRefNoLoad = false;

    public static boolean isNoRefNoLoad() {
        return noRefNoLoad;
    }

    public static void setNoRefNoLoad(boolean noRefNoLoad) {
        DynamicTreeNode.noRefNoLoad = noRefNoLoad;
    }
    private TableType tableType = null;

    public DynamicTreeNode() {
        super();
//        inquirer.logger.debug(" DynamicTreeNode()");
    }

//    @Override
//    public String toString() {
//        return "DynamicTreeNode{" + "dynamicChildren=" + dynamicChildren + ", ChildrenLoaded=" + ChildrenLoaded + ", refType=" + refType + ", onlyChildrenChecked=" + onlyChildrenChecked + ", loadChildrenProc=" + loadChildrenProc + '}' + ", data: " + ((getData() != null) ? getData().toString() : "[NULL]");
//    }
    public DynamicTreeNode(DynamicTreeNode<T> src) {
        this();
        if (src != null) {
//            inquirer.logger.debug("Duplicating " + src.toString());
            setData(src.getData());
            if (!src.dynamicChildren || src.ChildrenLoaded) {
                NodeCopy(src);
            } else {
                dynamicChildren = true;
                ChildrenLoaded = false;
                refType = src.refType;
                loadChildrenProc = src.getLoadChildrenProc();
            }
        } else {
            inquirer.logger.debug("Duplicating from null!!");
        }
    }

    protected void NodeCopy(DynamicTreeNode<T> src) {
        if (src != null) {
            TClonable d = src.getData();
            if (d != null) {
                setData(d.copy());
            }
            for (DynamicTreeNode<T> child : src.getChildren()) {
                addChild(new DynamicTreeNode(child));
            }
        }
    }

    private void addDynamicRef(DynamicTreeNode<OptionNode> AttrDynamic, final ReferenceType referenceType) {
        addDynamicRef(AttrDynamic, referenceType, null, null);
    }

    void addDynamicRef(DynamicTreeNode<OptionNode> AttrDynamic, final ReferenceType referenceType, final String CheckTab, final String CheckIDField1) {
        addDynamicRef(AttrDynamic, referenceType, CheckTab, CheckIDField1, null, false);
    }

    private void addDynamicRef(DynamicTreeNode<OptionNode> AttrDynamic, final ReferenceType referenceType, final String CheckTab,
            final String CheckIDField1, final String CheckIDField2, final boolean alwaysCheck) {
        AttrDynamic.setRefType(referenceType);
        AttrDynamic.setLoadChildrenProc(new DynamicTreeNode.ILoadChildrenProc() {
            public ArrayList<NameID> LoadChidlren(DynamicTreeNode node) {
                try {

                    // quick shortcut to make it run faster. Need to add option to check reference 
                    if (inquirer.isRefSyncDisabled()) {
                        return QueryTools.getRefNamesNameID(this, referenceType, null, null, null);
                    } else {
                        return QueryTools.getRefNamesNameID(this, referenceType, CheckTab, CheckIDField1, CheckIDField2);
                    }
//                    }

                } catch (Exception ex) {
                    inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
                }
                return null;
            }

            @Override
            void treeLoad(DynamicTreeNode parent) {
            }
        });
        addChild(AttrDynamic);
    }

//    private void addDynamicRef(DynamicTreeNode<OptionNode> AttrDynamic, final ReferenceType referenceType, final String CheckTab,
//            final String CheckIDField1, final String CheckIDField2, final boolean alwaysCheck) {
//        AttrDynamic.setRefType(referenceType);
//        AttrDynamic.setLoadChildrenProc(new DynamicTreeNode.ILoadChildrenProc() {
//            public String[] LoadChidlren() {
//                try {
//                    if (!alwaysCheck && DatabaseConnector.onlyOneApp()) {
//                        return getRefNames(this, referenceType);
//                    } else {
//                        return getRefNames(this, referenceType, CheckTab, CheckIDField1, CheckIDField2);
//                    }
//
//                } catch (Exception ex) {
//                    inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
//                }
//                return null;
//            }
//        });
//        addChild(AttrDynamic);
//    }
    void addDynamicRef(String name, final ReferenceType referenceType) {
        addDynamicRef(new DynamicTreeNode<>(new OptionNode(name)), referenceType);
    }

    DynamicTreeNode<OptionNode> addDynamicRef(DialogItem name, final ReferenceType referenceType) {
        DynamicTreeNode<OptionNode> ret = new DynamicTreeNode<>(new OptionNode(name));
        addDynamicRef(ret, referenceType);
        return ret;
    }

    DynamicTreeNode<OptionNode> addDynamicRef(DialogItem dialogItem, ReferenceType referenceType, String[] onlyChecked) {
        DynamicTreeNode<OptionNode> ret = addDynamicRef(dialogItem, referenceType);
        ret.setOnlyChecked(onlyChecked);
        return ret;
    }

    void addDynamicRef(DialogItem name, final ReferenceType referenceType, final String CheckTab, final String CheckIDField) {
        addDynamicRef(new DynamicTreeNode<>(new OptionNode(name)), referenceType, CheckTab, CheckIDField);
    }

    void addDynamicRef(DialogItem name, final ReferenceType referenceType, final String CheckTab, final String CheckIDField, final boolean alwaysCheck) {
        addDynamicRef(new DynamicTreeNode<>(new OptionNode(name)), referenceType, CheckTab, CheckIDField, null, alwaysCheck);
    }

    void addDynamicRef(DialogItem name, final ReferenceType referenceType, final String CheckTab, final String CheckIDField1, final String CheckIDField2) {
        addDynamicRef(new DynamicTreeNode<>(new OptionNode(name)), referenceType, CheckTab, CheckIDField1, CheckIDField2, false);
    }

    private void addDynamicRefLogMessage(DynamicTreeNode<OptionNode> AttrDynamic, final TableType tableType) {
        AttrDynamic.tableType = tableType;
        AttrDynamic.refType = ReferenceType.LOGMESSAGE;
        AttrDynamic.setLoadChildrenProc(new DynamicTreeNode.ILoadChildrenProc() {
            public ArrayList<NameID> LoadChidlren(DynamicTreeNode node) {
                try {
//                    return DatabaseConnector.getRefNameIDs(this, ReferenceType.LOGMESSAGE.toString(), null);

                    return DatabaseConnector.getRefNamesNameID(null,
                            AttrDynamic.refType.toString(),
                            tableType.toString(), "MSGID", null);
                } catch (Exception ex) {
                    inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
                }
                return null;
            }

            @Override
            void treeLoad(DynamicTreeNode parent) {
            }
        });
        addChild(AttrDynamic);

    }

    void addDynamicRefLogMessage(String name, final TableType tableType) {
        addDynamicRefLogMessage(new DynamicTreeNode<>(new OptionNode(name)), tableType);

    }

    void addDynamicRefLogMessage(DialogItem name, final TableType tableType) {
        addDynamicRefLogMessage(new DynamicTreeNode<>(new OptionNode(name)), tableType);

    }

    public void writeExternal(ObjectOutput out) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private ReferenceType refType = ReferenceType.UNKNOWN;

    private void setRefType(ReferenceType referenceType) {
        refType = referenceType;
    }

    void removeDynamicChildren() {
        if (dynamicChildren) {
//            if (inquirer.logger.isDebugEnabled()) {
//                if ((OptionNode) getData() == null) {
//                    inquirer.logger.debug("removing dynamic children; no data");
//                } else {
//                    inquirer.logger.debug("removing dynamic children of " + ((OptionNode) getData()).toString() + " (childrenloaded: " + ChildrenLoaded);
//                }
//            }
            removeChildren();
            ChildrenLoaded = false;
        } else {
//            if (inquirer.logger.isDebugEnabled()) {
//                if ((OptionNode) getData() == null) {
//                    inquirer.logger.debug("removing nondynamic children; no data");
//                } else {
//                    inquirer.logger.debug("removing nondynamic children of " + ((OptionNode) getData()).toString() + " (childrenloaded: " + ChildrenLoaded);
//                }
//            }
            for (DynamicTreeNode<T> ch : getChildren()) {
                ch.removeDynamicChildren();
            }
        }
    }

    void addPairChildren(DialogItem dialogItem, Pair[] pair) {
        DynamicTreeNode<OptionNode> localRoot;
        localRoot = new DynamicTreeNode<>(new OptionNode(true, dialogItem));
        addChild(localRoot);
        for (Pair pr : pair) {
            DynamicTreeNode<OptionNode> tEventsNode = new DynamicTreeNode<>(new OptionNode(true, pr));
            localRoot.addChild(tEventsNode);
        }
    }
    HashMap<String, Boolean> onlyChildrenChecked = null;

    public void setOnlyChecked(String[] onlyChecked) {
        onlyChildrenChecked = new HashMap<>(onlyChecked.length);
        for (String string : onlyChecked) {
            onlyChildrenChecked.put(string, true);
        }
    }

    DynamicTreeNode<OptionNode> getLogMessagesReportType(TableType tableType, OptionNode node) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(node);

        DynamicTreeNode<OptionNode> ComponentNode;
        ComponentNode = new DynamicTreeNode<>(new OptionNode(DialogItem.APP_LOG_MESSAGES_TYPE));
        nd.addChild(ComponentNode);

        /**
         * *****************
         */
        ComponentNode.addDynamicRef(DialogItem.APP_LOG_MESSAGES_TYPE_LEVEL, ReferenceType.MSGLEVEL);
//        ComponentNode.addDynamicRef(DialogItem.APP_LOG_MESSAGES_TYPE_MESSAGE_ID, ReferenceType.LOGMESSAGE, tableType.toString(), "MSGID");
        ComponentNode.addDynamicRefLogMessage(DialogItem.APP_LOG_MESSAGES_TYPE_MESSAGE_ID, tableType);
        return nd;
    }

    DynamicTreeNode<OptionNode> getLogMessagesReportType(TableType tableType, String itemName) {
        return getLogMessagesReportType(tableType, new OptionNode(false, DialogItem.APP_LOG_MESSAGES, itemName));
    }

    void addLogMessagesReportType(TableType tableType, String itemName) {
        addChild(getLogMessagesReportType(tableType, itemName));
    }

    public String getSummary(boolean loadDynamicItems, int level) {
        if (level <= 0) {
            return "";
        }
        StringBuilder ret = new StringBuilder(120);
        if (!dynamicChildren || loadDynamicItems) {
            int idx = 0;
            for (DynamicTreeNode<T> object : getChildren()) {
                OptionNode node = (OptionNode) object.getData();
                if (node != null) {
                    String nameIfChecked = node.nameIfChecked();
                    if (nameIfChecked != null) {
                        String summary = object.getSummary(loadDynamicItems, level - 1);
                        if (idx > 0) {
                            ret.append(",");
                        }
                        if (true) {
                            ret.append(nameIfChecked).append("{")
                                    .append(object.getSummary(loadDynamicItems, level - 1))
                                    .append("}");
                        }
                    }

                }
                idx++;
            }
        }
        return ret.toString();
    }

    String checkedChildrenToString() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * For log messages; searches in the refsHash hashmap and compares only
     * first word from first word in name
     *
     * @param name comes from reference table read from database
     * @param refsHash hash for name/selected from options
     * @return
     */
    private Boolean checkSelected(String name, HashMap<String, Boolean> refsHash) {
        if (name == null || refsHash == null || name.isEmpty() || refsHash.size() == 0) {
            return null;
        }
        String search = getFirstWord(name);
        for (Map.Entry<String, Boolean> entry : refsHash.entrySet()) {
            if (entry.getKey().startsWith(search)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static abstract class ILoadChildrenProc {

        abstract ArrayList<NameID> LoadChidlren(DynamicTreeNode parent);

        abstract void treeLoad(DynamicTreeNode parent);
        private boolean treeLoad = false;

        public boolean isTreeLoad() {
            return treeLoad;
        }

        public void setTreeLoad(boolean treeLoad) {
            this.treeLoad = treeLoad;
        }
    }

    private ILoadChildrenProc loadChildrenProc = null;

    public ILoadChildrenProc getLoadChildrenProc() {
        return loadChildrenProc;
    }

    private void LoadChildren() throws Exception {
        if (dynamicChildren) {
            if (loadChildrenProc != null) {
                if (!ChildrenLoaded && !isBlockLoad()) {
                    HashMap<String, Boolean> refsHash = null;
                    if (onlyChildrenChecked != null) {
                        refsHash = onlyChildrenChecked;
                    } else {
                        if (refType != ReferenceType.LOGMESSAGE) {
                            refsHash = inquirer.getCr().getRefsHash(refType);
                        } else if (tableType != null) {
                            refsHash = inquirer.getCr().getLogsHash(tableType);
                        }
                    }
                    if (refsHash != null || !DynamicTreeNode.isNoRefNoLoad()) {
                        if (loadChildrenProc.isTreeLoad()) {
                            loadChildrenProc.treeLoad(this);
                        } else {
                            ArrayList<NameID> ch = loadChildrenProc.LoadChidlren(this);
                            if (ch != null && ch.size() > 0) {
                                for (NameID ch1 : ch) {
                                    OptionNode inst = null;
                                    if (refsHash != null) {
                                        Boolean selected;
                                        if (refType == ReferenceType.LOGMESSAGE) {
                                            selected = checkSelected(ch1.getName(), refsHash);
                                        } else {
                                            selected = refsHash.get(ch1.getName());
                                        }
                                        if (selected != null) {
                                            inst = new OptionNode(selected, ch1);
                                        }
                                    }
                                    if (inst == null) {
                                        if (onlyChildrenChecked != null) {
                                            inst = new OptionNode(false, ch1);
                                        } else {
                                            inst = new OptionNode(ch1);
                                        }
                                    }
                                    DynamicTreeNode<OptionNode> n = new DynamicTreeNode<>(inst);
                                    addChild(n);
                                }
//                            addChildAt(0, new DynamicTreeNode<OptionNode>(new OptionNode(true, true, "<Empty>")));
                            }
                        }
                        ChildrenLoaded = true;
                    }
                }
            } else {
                throw new Exception("Dynamic children, but procedure not defined");
            }
        }
    }

    public void addLogMessagesReportType(TableType tableType) {
        addLogMessagesReportType(tableType, null);
    }

    public void setLoadChildrenProc(ILoadChildrenProc proc) {
        loadChildrenProc = proc;
        dynamicChildren = true;
        ChildrenLoaded = false;
    }

    public DynamicTreeNode(Object data) {
        this();
        setData((OptionNode) data);
    }

    @Override
    public DynamicTreeNode getChildAt(int index) throws IndexOutOfBoundsException {
        try {
            LoadChildren();
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
        return (DynamicTreeNode) super.getChildAt(index); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addChildAt(int index, GenericTreeNode child) throws IndexOutOfBoundsException {
        super.addChildAt(index, (DynamicTreeNode) child); //To change body of generated methods, choose Tools | Templates.
//        dynamicChildren = false;
//            if (inquirer.logger.isDebugEnabled()) {
//                if ((OptionNode) getData() == null) {
//                    inquirer.logger.debug("addChildAt 1 no data");
//                } else {
//                    inquirer.logger.debug("addChildAt 1 " + ((OptionNode) getData()).toString() + " (childrenloaded: " + ChildrenLoaded);
//                }
//            }

    }

    @Override
    public void addChild(GenericTreeNode child) {
        if (child != null) {
            super.addChild(child); //To change body of generated methods, choose Tools | Templates.
        }//        dynamicChildren = false;
//            if (inquirer.logger.isDebugEnabled()) {
//                if ((OptionNode) getData() == null) {
//                    inquirer.logger.debug("addChildAt 2 no data");
//                } else {
//                    inquirer.logger.debug("addChildAt 2  " + ((OptionNode) getData()).toString() + " (childrenloaded: " + ChildrenLoaded);
//                }
//            }

    }

    @Override
    public void setChildren(List children) {
        super.setChildren(children); //To change body of generated methods, choose Tools | Templates.
//            if (inquirer.logger.isDebugEnabled()) {
//                if ((OptionNode) getData() == null) {
//                    inquirer.logger.debug("addChildAt 3 no data");
//                } else {
//                    inquirer.logger.debug("addChildAt 3  " + ((OptionNode) getData()).toString() + " (childrenloaded: " + ChildrenLoaded);
//                }
//            }

//        dynamicChildren = false;
    }

    @Override
    public boolean hasChildren() {
        try {
            LoadChildren();
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
        return super.hasChildren(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int getNumberOfChildren() {
        try {
            LoadChildren();
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
        return super.getNumberOfChildren(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<DynamicTreeNode<T>> getChildren() {
        inquirer.logger.trace("getChildren -1");
        try {
            LoadChildren();
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
        return (List<DynamicTreeNode<T>>) super.getChildren(); //To change body of generated methods, choose Tools | Templates.
    }

    private void writeObject(ObjectOutputStream s)
            throws IOException {
        s.defaultWriteObject();
        s.writeObject((OptionNode) getData());
        if (!dynamicChildren || ChildrenLoaded) {
            s.writeInt(getNumberOfChildren());
            List<DynamicTreeNode<T>> l = this.getChildren();
            for (GenericTreeNode<T> children1 : this.getChildren()) {
                s.writeObject(children1);
            }
        }
    }

    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        setData((OptionNode) s.readObject());
        int chN = s.readInt();
        for (int i = 0; i < chN; i++) {
            addChild((DynamicTreeNode<T>) s.readObject());
        }
    }
}
