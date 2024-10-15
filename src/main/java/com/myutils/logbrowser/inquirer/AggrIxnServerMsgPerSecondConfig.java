/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import com.jidesoft.swing.SearchableUtils;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.inquirer.gui.JPSecSelect;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author ssydoruk
 */
public class AggrIxnServerMsgPerSecondConfig extends javax.swing.JPanel {

    private DefaultListModel lmAttr;
    private MyCheckBoxList cblAttr;
    private DefaultListModel lmAttrValues;
    private MyCheckBoxList cblAttrValues;

    /**
     * Creates new form TLibDelaysConfig
     */
    private DynamicTreeNode<OptionNode> root = null;
    private MyCheckBoxList clbOrderBy;
    private DefaultListModel lmAttrTLib;
    private MyCheckBoxList cblAttrTLib;
    private DefaultListModel lmAttrValuesTLib;
    private MyCheckBoxList cblAttrValuesTLib;
    private DynamicTreeNode<OptionNode> rootTLib;
    private GroupByPanel gpOrderByTLib;
    private GroupByPanel gpGroupByTLib;
    private final JPSecSelect jpSecSelect;
    private final JPSecSelect jpHavingSelect;

    public DynamicTreeNode<OptionNode> getAttrRoot() {
        return root;
    }

    public AggrIxnServerMsgPerSecondConfig() {
        initComponents();
//        jpAppSelect.setBorder(new BevelBorder(BevelBorder.RAISED));

        jpSecSelect = new JPSecSelect(0, 1);
        jpAppSettings.add(jpSecSelect);
        jpHavingSelect = new JPSecSelect(0, 0);
        jpHavingSelect.setTextStart("Show if more than");
        jpHavingSelect.setTextEnd("cnt");
        jpAppSettings.add(jpHavingSelect);

//        timeRange.setRefreshCB(new TDateRange.IRefresh() {
//            @Override
//            public UTCTimeRange Refresh() {
//                return q.refreshTimeRange(getSearchApps());
//            }
//        });
        InitTLib();
        revalidate();
    }

    public int getSeconds() {
        return jpHavingSelect.getSeconds();
    }

    private void FillElements(DefaultListModel lm, DynamicTreeNode<OptionNode> parent, boolean isChecked) {
        List<DynamicTreeNode<OptionNode>> l = parent.getChildren();
        for (DynamicTreeNode<OptionNode> node : parent.getChildren()) {
//            node.getData().setChecked(isChecked);
            lm.addElement(node);
        }
    }
    boolean isTLibReport() {
        return jtpReportType.getSelectedIndex() == 0;
    }


    private void InitTLib() {
//        jpComponentsTLib.setLayout(new BorderLayout());
//        jpAttrValuesTLib.setLayout(new BorderLayout());

//        lmAttrTLib = new DefaultListModel();
//        cblAttrTLib = new MyCheckBoxList(lmAttrTLib, jpComponentsTLib);
//
//        lmAttrValuesTLib = new DefaultListModel();
//        cblAttrValuesTLib = new MyCheckBoxList(lmAttrValuesTLib, jpAttrValuesTLib);

        rootTLib = new DynamicTreeNode<>(null);

        rootTLib.addDynamicRef(DialogItem.IXN_IXN_EVENT_NAME, ReferenceType.TEvent);
        rootTLib.addDynamicRef(DialogItem.IXN_IXN_EVENT_DN, ReferenceType.DN);
        rootTLib.addDynamicRef(DialogItem.IXN_IXN_EVENT_APP, ReferenceType.App);
        rootTLib.addDynamicRef(DialogItem.URS_STRATEGY, ReferenceType.URSStrategyName);
        
        rootTLib.addPairChildren(DialogItem.TLIB_CALLS_TEVENT_DIRECTION,
                new Pair[]{new Pair("inbound", "1"), new Pair("outbound", "0")});

//        InitCB(cblAttrTLib, cblAttrValuesTLib, jpComponentsTLib, 3);
//        InitCB(cblAttrValuesTLib, null, jpAttrValuesTLib, 4);

//        FillElements(lmAttrTLib, rootTLib, true);
//        lmAttrTLib.insertElementAt(MyCheckBoxList.ALL_ENTRY, 0);
//        cblAttrTLib.checkAll();

        InitOrderByTLib();
        InitGroupByTLib();
    }

    private void InitOrderByTLib() {
        ArrayList<DBField> flds = new ArrayList<>();
        flds.add(new DBField("Time", "time", null, null));
        flds.add(new DBField("ConnID", "ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString()));

        gpOrderByTLib = new GroupByPanel(flds, "Order by");
        jpOrderByTLib.setLayout(new BorderLayout());
        jpOrderByTLib.add(gpOrderByTLib);
    }

    private void InitGroupByTLib() {
        ArrayList<DBField> flds = new ArrayList<>();
        flds.add(new DBField("Event name", "nameid", "name", ReferenceType.TEvent.toString()));
        flds.add(new DBField("Strategy", "strategyID", "strategy", ReferenceType.URSStrategyName.toString()));
        flds.add(new DBField("DN/workflow object", "thisDNID", "thisDN", ReferenceType.DN.toString()));
        flds.add(new DBField("Requester app", "SourceID", "Source", ReferenceType.App.toString()));
        flds.add(new DBField("Direction", "inbound", null, null));
        gpGroupByTLib = new GroupByPanel(flds, "Additional group by");
        jpGroupByTLib.setLayout(new BorderLayout());
        jpGroupByTLib.add(gpGroupByTLib);
    }

    DynamicTreeNode<OptionNode> getAttrTLibRoot() {
        return rootTLib;
    }

    DBField[] getGroupByTLib() {
        return gpGroupByTLib.getSelected();
    }

    DBField[] getOrderByTLib() {
        return gpOrderByTLib.getSelected();
    }

    int getSamplePeriod() {
        return jpSecSelect.getSeconds();
    }

    class OrderField {

        private final String fieldName;
        private final String displayName;

        public OrderField(String fieldName, String displayName) {
            this.fieldName = fieldName;
            this.displayName = displayName;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }

    }

    public DBField[] getOrderBy() {
        return gpOrderBy.getSelected();
    }

    private GroupByPanel gpOrderBy;

    private void InitOrderBy() {
        ArrayList<DBField> flds = new ArrayList<>();
        flds.add(new DBField("Time", "time", null, null));
        flds.add(new DBField("SIP CallID", "callidid", "callid", ReferenceType.SIPCALLID.toString()));

        gpOrderBy = new GroupByPanel(flds, "Order by");
//        jpOrderBy.setLayout(new BorderLayout());
//        jpOrderBy.add(gpOrderBy);

//        
//        clbOrderBy = new MyCheckBoxList(jpOrderBy);
//        clbOrderBy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//
//        jpOrderBy.setLayout(new BorderLayout());
//        jpOrderBy.add(new JScrollPane(clbOrderBy));
//
//        clbOrderBy.getLm().addElement(new OrderField("time", "Time"));
//        clbOrderBy.getLm().addElement(new OrderField("callidid", "SIP CallID"));
//        clbOrderBy.checkAll();
//        
//        clbOrderBy.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
//            @Override
//            public void valueChanged(ListSelectionEvent e) {
//                ReportItemChanged(e);
//
//            }
//
//        });
//        clbOrderBy.setSelectedIndex(0);
//        jbUp.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                clbOrderBy.SelectedUp();
//            }
//        });
//        jbDown.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                clbOrderBy.SelectedDown();
//            }
//        });
    }

    class myListCheckListener implements ListSelectionListener {

        private final MyCheckBoxList list;
        private final MyCheckBoxList listChild;

        public myListCheckListener(MyCheckBoxList list, MyCheckBoxList listChild) {
            this.list = list;
            this.listChild = listChild;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            ReportItemChecked(e, list, listChild);
        }

    }

    class myListSelectionListener implements ListSelectionListener {

        private final MyCheckBoxList list;
        private final MyCheckBoxList listChild;

        public myListSelectionListener(MyCheckBoxList list, MyCheckBoxList listChild) {
            this.list = list;
            this.listChild = listChild;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            ReportItemChanged(e, list, listChild);
        }

    }

    class myFocusListener implements FocusListener {

        @Override
        public void focusGained(FocusEvent e) {
            inquirer.logger.debug("focusGained", e);
        }

        @Override
        public void focusLost(FocusEvent e) {
            inquirer.logger.debug("focusLost", e);
        }

    }

    private void InitCB(MyCheckBoxList list, MyCheckBoxList clbChild, JPanel rptType, int i) {

        rptType.add(new JScrollPane(list));

        list.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        SearchableUtils.installSearchable(list);

        list.getCheckBoxListSelectionModel().addListSelectionListener(new myListCheckListener(list, clbChild));
        list.getSelectionModel().addListSelectionListener(new myListSelectionListener(list, clbChild));
        list.addFocusListener(new myFocusListener());

    }

    private final ButtonGroup group = new ButtonGroup();
    private FileInfoType ft = FileInfoType.type_Unknown;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jpOtherSettings = new javax.swing.JPanel();
        jpAppSettings = new javax.swing.JPanel();
        jtpReportType = new javax.swing.JTabbedPane();
        jpTLIB = new javax.swing.JPanel();
        jpOrderByTLib = new javax.swing.JPanel();
        jpGroupByTLib = new javax.swing.JPanel();

        setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());

        jpOtherSettings.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jpOtherSettings.setMinimumSize(new java.awt.Dimension(0, 30));
        jpOtherSettings.setLayout(new javax.swing.BoxLayout(jpOtherSettings, javax.swing.BoxLayout.PAGE_AXIS));
        jpOtherSettings.add(jpAppSettings);

        jPanel1.add(jpOtherSettings, java.awt.BorderLayout.CENTER);

        jpTLIB.setPreferredSize(new java.awt.Dimension(700, 405));
        jpTLIB.setLayout(new javax.swing.BoxLayout(jpTLIB, javax.swing.BoxLayout.X_AXIS));

        javax.swing.GroupLayout jpOrderByTLibLayout = new javax.swing.GroupLayout(jpOrderByTLib);
        jpOrderByTLib.setLayout(jpOrderByTLibLayout);
        jpOrderByTLibLayout.setHorizontalGroup(
            jpOrderByTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 845, Short.MAX_VALUE)
        );
        jpOrderByTLibLayout.setVerticalGroup(
            jpOrderByTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 405, Short.MAX_VALUE)
        );

        jpTLIB.add(jpOrderByTLib);

        javax.swing.GroupLayout jpGroupByTLibLayout = new javax.swing.GroupLayout(jpGroupByTLib);
        jpGroupByTLib.setLayout(jpGroupByTLibLayout);
        jpGroupByTLibLayout.setHorizontalGroup(
            jpGroupByTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jpGroupByTLibLayout.setVerticalGroup(
            jpGroupByTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 405, Short.MAX_VALUE)
        );

        jpTLIB.add(jpGroupByTLib);

        jtpReportType.addTab("TLib", jpTLIB);

        jPanel1.add(jtpReportType, java.awt.BorderLayout.SOUTH);

        add(jPanel1, java.awt.BorderLayout.PAGE_START);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jpAppSettings;
    private javax.swing.JPanel jpGroupByTLib;
    private javax.swing.JPanel jpOrderByTLib;
    private javax.swing.JPanel jpOtherSettings;
    private javax.swing.JPanel jpTLIB;
    private javax.swing.JTabbedPane jtpReportType;
    // End of variables declaration//GEN-END:variables

    private void addRadioButton(String btTitle, final FileInfoType fileInfoType) {
        JRadioButton btn = new JRadioButton(btTitle);
        btn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ft = fileInfoType;
            }
        });
        group.add(btn);
//        jpComponents.add(btn);
    }

    /**
     * @return the ft
     */
    public FileInfoType getFt() {
        return ft;
    }

    private void ReportItemChanged(javax.swing.event.ListSelectionEvent evt, MyCheckBoxList l, MyCheckBoxList lChild) {
//        javax.swing.JOptionPane.showMessageDialog(this, "Item changed", "Item changed", JOptionPane.ERROR_MESSAGE);
        if (!evt.getValueIsAdjusting()) {
            inquirer.logger.trace("ReportItemChanged - " + evt);
//            inquirer.logger.info("ReportItemChanged - adjusting");
            ListSelectionModel lsm = (ListSelectionModel) evt.getSource();
            if (lsm.isSelectionEmpty()) {
                inquirer.logger.trace("ReportItemChanged <none>");
                if (lChild != null) {
                    lChild.setValueIsAdjusting(true);
                    DefaultListModel lm = (DefaultListModel) lChild.getModel();
                    lm.removeAllElements();
                    lChild.setValueIsAdjusting(false);
                }
            } else {
                // Find out which indexes are selected.
                int minIndex = lsm.getMinSelectionIndex();
                int maxIndex = lsm.getMaxSelectionIndex();
                for (int i = minIndex; i <= maxIndex; i++) {
                    if (lsm.isSelectedIndex(i)) {
                        inquirer.logger.debug("**current item " + i);
                        DefaultListModel lm = (DefaultListModel) l.getModel();
                        if (lChild != null) {
                            if (i > 0) {
                                changeReportItem(i, (DynamicTreeNode<OptionNode>) lm.get(i), lChild);
                            }
                        }
                    }
                }
            }
        }
    }

    private void changeReportItem(int item, DynamicTreeNode<OptionNode> node, MyCheckBoxList lChild) {
//        ChangeListItem cli = changeList.get(lChild);
//        if( cli!=null ) {
//            cli.changeItem(item, null);
//        }
//        if (lChild != null && rptSelected >= 0) {
////            _listComps.setValueIsAdjusting(true);
//            compChecked.put(rptSelected, lChild.getCheckBoxListSelectedIndices());
//        }
        inquirer.logger.debug("changeReportItem " + item + " node: [" + node + "] lchild: [" + lChild + "]");
        lChild.setValueIsAdjusting(true);
        DefaultListModel lm = (DefaultListModel) lChild.getModel();

        ListSelectionListener[] listSelectionListeners = lChild.getCheckBoxListSelectionModel().getListSelectionListeners();
        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            lChild.getCheckBoxListSelectionModel().removeListSelectionListener(listSelectionListener);
        }

        lm.removeAllElements();
        for (DynamicTreeNode<OptionNode> child : node.getChildren()) {
            lm.addElement(child);
        }
        lm.insertElementAt(MyCheckBoxList.ALL_ENTRY, 0);
        setChildChecked(lChild, ((OptionNode) node.getData()).isChecked());

        for (ListSelectionListener listSelectionListener : listSelectionListeners) {
            lChild.getCheckBoxListSelectionModel().addListSelectionListener(listSelectionListener);
        }
//        rptSelected = item;
//        if (compChecked.containsKey(rptSelected)) {
//            lChild.setCheckBoxListSelectedIndices(compChecked.get(rptSelected));
//        }

        lChild.setValueIsAdjusting(false);
//        lChild.revalidate();
//        lChild.repaint();
    }

    private void setChildChecked(MyCheckBoxList lChild, boolean boxEnabled) {
        inquirer.logger.debug("in setChildChecked " + boxEnabled);
        boolean checkedItem = false;
        lChild.setEnabled(boxEnabled);
        DefaultListModel lm = (DefaultListModel) lChild.getModel();
        ArrayList<Integer> idx = new ArrayList<>();
        for (int i = 1; i < lm.size(); i++) {
            DynamicTreeNode<OptionNode> ch = (DynamicTreeNode<OptionNode>) lm.getElementAt(i);
//            inquirer.logger.info("verifying item "+ i+ ch.getData().toString());
            if (((OptionNode) ch.getData()).isChecked()) {
//                inquirer.logger.info("Checking item "+ i );
//                lChild.set
//                lChild.setCheckBoxListSelectedIndex(i);
                idx.add(i);
                checkedItem = true;
            }
        }
//        lChild.s
        lChild.setCheckBoxListSelectedIndices(inquirer.toArray(idx));
        if (boxEnabled && !checkedItem) {
            inquirer.logger.debug("Checking full child");
            lChild.selectAll();
//            lChild.setCheckBoxListSelectedIndex(0);
        }
    }

    private void ReportItemChecked(javax.swing.event.ListSelectionEvent evt, MyCheckBoxList l, MyCheckBoxList lChild) {
        if (!evt.getValueIsAdjusting()) {
            inquirer.logger.debug("ReportItemChecked " + evt);
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
            if (lsm.isSelectionEmpty() && lChild != null) {
                inquirer.logger.debug(" <none>");
//                lChild.setEnabled(false);
            } else {
//                if (inquirer.logger.isDebugEnabled()) {
//                    for (int i = 1; i < lsm.getModel().getSize(); i++) {
//                        inquirer.logger.debug("Item "+i+ " select: " +lsm.isSelectedIndex(i)+" key: "+((DynamicTreeNode<OptionNode>)lsm.getModel().getElementAt(i)).getData());
//                    }
//                    int[] checkBoxListSelectedIndices = l.getCheckBoxListSelectedIndices();
//                    for (int checkBoxListSelectedIndice : checkBoxListSelectedIndices) {
//                        inquirer.logger.debug("selected: " + checkBoxListSelectedIndice);
//
//                    }
//                }
                for (int i = 1; i < lsm.getModel().getSize(); i++) {
                    OptionNode node = ((OptionNode) ((DynamicTreeNode<OptionNode>) lsm.getModel().getElementAt(i)).getData());
                    if (node.isChecked() != lsm.isSelectedIndex(i)) {     //value changed
                        node.setChecked(lsm.isSelectedIndex(i));
                        if (lChild != null) {
                            setChildChecked(lChild, lsm.isSelectedIndex(i));
                        }
                    }
                }
                // Find out which indexes are selected.
//                int minIndex = lsm.getMinSelectionIndex();
//                int maxIndex = lsm.getMaxSelectionIndex();
//                DefaultListModel lm = (DefaultListModel) l.getModel();
////                inquirer.logger.debug("minIndex: "+minIndex+" maxIndex:"+maxIndex);
//                for (int i = 1; i < lm.getSize(); i++) {
////                    DynamicTreeNode<OptionNode> node = (DynamicTreeNode<OptionNode>) lm.get(i);
////                    ((OptionNode)node.getData()).setChecked(findInt(checkBoxListSelectedIndices, i) >= 0);
//
////                    if( i>=minIndex && i<=maxIndex )
////                        node.getData().setChecked(true);
////                    else 
////                        node.getData().setChecked(false);
//                }
//
//                for (int i = minIndex; i <= maxIndex; i++) {
////                    inquirer.logger.info("ReportItemChecked "+i);
//                    if (lChild != null) {
//                        setChildChecked(lChild, lsm.isSelectedIndex(i));
//                    }
////                    changeReportItem(i, (DynamicTreeNode<OptionNode>) lm.get(i), lChild);
////                    if( i>=0 ) {
////                        inquirer.logger.info("all selected? "+lm.get(i).equals(MyCheckBoxList.ALL_ENTRY));
////                        DynamicTreeNode<OptionNode> node = (DynamicTreeNode<OptionNode>)lm.get(i);
////                        node.getData().setChecked(lsm.isSelectedIndex(i));
////                    }
////                    (DynamicTreeNode<OptionNode>) lm.get(i)
//                }
            }
        }

    }

}
