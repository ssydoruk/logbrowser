/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.jidesoft.swing.CheckBoxListSelectionModel;
import com.jidesoft.swing.SearchableUtils;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.inquirer.gui.JPSecSelect;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ssydoruk
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation", "this-escape"})
public class AggrApacheURIPerSecondConfig extends javax.swing.JPanel {

    /**
     * Creates new form TLibDelaysConfig
     */
    private final DynamicTreeNode<OptionNode> root = null;
    private final JPSecSelect jpSecSelect;
    private final JPSecSelect jpHavingSelect;
    private final ButtonGroup group = new ButtonGroup();
    private MyCheckBoxList clbOrderBy;
    private DefaultListModel lmAttrTLib;
    private MyCheckBoxList cblAttrTLib;
    private DefaultListModel lmAttrValuesTLib;
    private MyCheckBoxList cblAttrValuesTLib;
    private DynamicTreeNode<OptionNode> rootTLib;
    private GroupByPanel gpOrderByTLib;
    private GroupByPanel gpGroupByTLib;
    private GroupByPanel gpGroupBy;
    private FileInfoType ft = FileInfoType.type_Unknown;
    // Variables declaration - do not modify                     
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jpAppSettings;
    private javax.swing.JPanel jpAttrValuesTLib;
    private javax.swing.JPanel jpComponentsTLib;
    private javax.swing.JPanel jpGroupByTLib;
    private javax.swing.JPanel jpOrderByTLib;
    private javax.swing.JPanel jpOrderGroup;
    private javax.swing.JPanel jpOtherSettings;
    private javax.swing.JPanel jpTLIB;

    public AggrApacheURIPerSecondConfig() {
        initComponents();
//        jpAppSelect.setBorder(new BevelBorder(BevelBorder.RAISED));

        jpSecSelect = new JPSecSelect(0, 1);
        jpAppSettings.add(jpSecSelect);
        jpHavingSelect = new JPSecSelect(0, 0);
        jpHavingSelect.setTextStart("Show if more than");
        jpHavingSelect.setTextEnd("cnt");
        jpAppSettings.add(jpHavingSelect);

        initFilterPanel();
        jpOrderGroup.setMaximumSize(jpOrderGroup.getPreferredSize());
        jpOrderGroup.setMinimumSize(jpOrderGroup.getPreferredSize());

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

    private void initFilterPanel() {
        jpComponentsTLib.setLayout(new BorderLayout());
        jpAttrValuesTLib.setLayout(new BorderLayout());

        lmAttrTLib = new DefaultListModel();
        cblAttrTLib = new MyCheckBoxList(lmAttrTLib, jpComponentsTLib);

        lmAttrValuesTLib = new DefaultListModel();
        cblAttrValuesTLib = new MyCheckBoxList(lmAttrValuesTLib, jpAttrValuesTLib);

        rootTLib = new DynamicTreeNode<>(null);

        rootTLib.addDynamicRef(DialogItem.APACHEMSG_PARAMS_IP, ReferenceType.IP);
        rootTLib.addDynamicRef(DialogItem.APACHEMSG_PARAMS_HTTPCODE, ReferenceType.HTTPRESPONSE);
        rootTLib.addDynamicRef(DialogItem.APACHEMSG_PARAMS_HTTPMETHOD, ReferenceType.HTTPMethod);
        rootTLib.addDynamicRef(DialogItem.APACHEMSG_PARAMS_HTTPURL, ReferenceType.HTTPURL);

        InitCB(cblAttrTLib, cblAttrValuesTLib, jpComponentsTLib, 3);
        InitCB(cblAttrValuesTLib, null, jpAttrValuesTLib, 4);

        FillElements(lmAttrTLib, rootTLib, true);
        lmAttrTLib.insertElementAt(MyCheckBoxList.ALL_ENTRY, 0);
        cblAttrTLib.checkAll();

        InitOrderBy();
        InitGroupBy();
    }

    private void InitOrderBy() {
        ArrayList<DBField> flds = new ArrayList<>();
        flds.add(new DBField("Time", "ts", null, null));
        flds.add(new DBField("Count", "cnt", null, null));

        gpOrderByTLib = new GroupByPanel(flds, "Order by");
        jpOrderByTLib.setLayout(new BorderLayout());
        jpOrderByTLib.add(gpOrderByTLib);
        jpOrderByTLib.setMaximumSize(jpOrderByTLib.getPreferredSize());
        jpOrderByTLib.setMinimumSize(jpOrderByTLib.getPreferredSize());

    }

    private void InitGroupBy() {
        ArrayList<DBField> flds = new ArrayList<>();
        flds.add(new DBField("IP Address", "IPID", "IP", ReferenceType.IP.toString()));
        flds.add(new DBField("HTTP Error code", "httpCode", "code", ReferenceType.HTTPRESPONSE.toString()));
        flds.add(new DBField("HTTP URL", "urlID", "URL", ReferenceType.HTTPURL.toString()));
        gpGroupByTLib = new GroupByPanel(flds, "Additional group by");
        jpGroupByTLib.setLayout(new BorderLayout());
        jpGroupByTLib.add(gpGroupByTLib);
        jpGroupByTLib.setMaximumSize(jpGroupByTLib.getPreferredSize());
        jpGroupByTLib.setMinimumSize(jpGroupByTLib.getPreferredSize());
    }

    DynamicTreeNode<OptionNode> getAttrRoot() {
        return rootTLib;
    }

    DBField[] getGroupByFields() {
        return gpGroupByTLib.getSelected();
    }

    DBField[] getOrderByFields() {
        return gpOrderByTLib.getSelected();
    }

    int getSamplePeriod() {
        return jpSecSelect.getSeconds();
    }

    private void InitCB(MyCheckBoxList list, MyCheckBoxList clbChild, JPanel rptType, int i) {

        rptType.add(new JScrollPane(list));

        list.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        SearchableUtils.installSearchable(list);

        list.getCheckBoxListSelectionModel().addListSelectionListener(new myListCheckListener(list, clbChild));
        list.getSelectionModel().addListSelectionListener(new myListSelectionListener(list, clbChild));
        list.addFocusListener(new myFocusListener());

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpOtherSettings = new javax.swing.JPanel();
        jpAppSettings = new javax.swing.JPanel();
        jpTLIB = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jpComponentsTLib = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jpAttrValuesTLib = new javax.swing.JPanel();
        jpOrderGroup = new javax.swing.JPanel();
        jpOrderByTLib = new javax.swing.JPanel();
        jpGroupByTLib = new javax.swing.JPanel();

        setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));

        jpOtherSettings.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jpOtherSettings.setMinimumSize(new java.awt.Dimension(0, 30));
        jpOtherSettings.setLayout(new javax.swing.BoxLayout(jpOtherSettings, javax.swing.BoxLayout.PAGE_AXIS));
        jpOtherSettings.add(jpAppSettings);

        add(jpOtherSettings);

        jpTLIB.setPreferredSize(new java.awt.Dimension(700, 405));
        jpTLIB.setLayout(new javax.swing.BoxLayout(jpTLIB, javax.swing.BoxLayout.LINE_AXIS));

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel3.setText("Attribute type");
        jPanel3.add(jLabel3);

        jpComponentsTLib.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        javax.swing.GroupLayout jpComponentsTLibLayout = new javax.swing.GroupLayout(jpComponentsTLib);
        jpComponentsTLib.setLayout(jpComponentsTLibLayout);
        jpComponentsTLibLayout.setHorizontalGroup(
                jpComponentsTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        jpComponentsTLibLayout.setVerticalGroup(
                jpComponentsTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 212, Short.MAX_VALUE)
        );

        jPanel3.add(jpComponentsTLib);

        jPanel4.add(jPanel3);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Messages filter"));
        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel4.setText("Attribute value");
        jPanel6.add(jLabel4);

        jpAttrValuesTLib.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout jpAttrValuesTLibLayout = new javax.swing.GroupLayout(jpAttrValuesTLib);
        jpAttrValuesTLib.setLayout(jpAttrValuesTLibLayout);
        jpAttrValuesTLibLayout.setHorizontalGroup(
                jpAttrValuesTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 384, Short.MAX_VALUE)
        );
        jpAttrValuesTLibLayout.setVerticalGroup(
                jpAttrValuesTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 201, Short.MAX_VALUE)
        );

        jPanel6.add(jpAttrValuesTLib);

        jPanel4.add(jPanel6);

        jpTLIB.add(jPanel4);

        jpOrderGroup.setLayout(new javax.swing.BoxLayout(jpOrderGroup, javax.swing.BoxLayout.PAGE_AXIS));

        javax.swing.GroupLayout jpOrderByTLibLayout = new javax.swing.GroupLayout(jpOrderByTLib);
        jpOrderByTLib.setLayout(jpOrderByTLibLayout);
        jpOrderByTLibLayout.setHorizontalGroup(
                jpOrderByTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 446, Short.MAX_VALUE)
        );
        jpOrderByTLibLayout.setVerticalGroup(
                jpOrderByTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 237, Short.MAX_VALUE)
        );

        jpOrderGroup.add(jpOrderByTLib);

        javax.swing.GroupLayout jpGroupByTLibLayout = new javax.swing.GroupLayout(jpGroupByTLib);
        jpGroupByTLib.setLayout(jpGroupByTLibLayout);
        jpGroupByTLibLayout.setHorizontalGroup(
                jpGroupByTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        jpGroupByTLibLayout.setVerticalGroup(
                jpGroupByTLibLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 237, Short.MAX_VALUE)
        );

        jpOrderGroup.add(jpGroupByTLib);

        jpTLIB.add(jpOrderGroup);

        add(jpTLIB);
    }// </editor-fold>//GEN-END:initComponents

    private void addRadioButton(String btTitle, final FileInfoType fileInfoType) {
        JRadioButton btn = new JRadioButton(btTitle);
        btn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ft = fileInfoType;
            }
        });
        group.add(btn);
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
    // End of variables declaration                   

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

}
