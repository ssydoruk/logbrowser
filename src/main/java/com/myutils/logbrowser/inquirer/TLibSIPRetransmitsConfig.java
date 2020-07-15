/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.jidesoft.swing.CheckBoxListSelectionModel;
import com.jidesoft.swing.SearchableUtils;
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
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;

/**
 *
 * @author ssydoruk
 */
public class TLibSIPRetransmitsConfig extends javax.swing.JPanel {

    private final DefaultListModel lmAttr;
    private final MyCheckBoxList cblAttr;
    private final DefaultListModel lmAttrValues;
    private final MyCheckBoxList cblAttrValues;

    /**
     * Creates new form TLibDelaysConfig
     */
    private DynamicTreeNode<OptionNode> root = null;
    private MyCheckBoxList clbOrderBy;

    public DynamicTreeNode<OptionNode> getAttrRoot() {
        return root;
    }

    public TLibSIPRetransmitsConfig() {
        initComponents();
        jpComponents.setLayout(new BorderLayout());
        jpAttrValues.setLayout(new BorderLayout());

        lmAttr = new DefaultListModel();
        cblAttr = new MyCheckBoxList(lmAttr, jpComponents);

        lmAttrValues = new DefaultListModel();
        cblAttrValues = new MyCheckBoxList(lmAttrValues, jpAttrValues);

        root = new DynamicTreeNode<>(null);

        root.addDynamicRef(DialogItem.TLIB_CALLS_SIP_NAME, ReferenceType.SIPMETHOD);
        root.addDynamicRef(DialogItem.TLIB_CALLS_SIP_PEERIP, ReferenceType.IP);
        root.addDynamicRef(DialogItem.TLIB_CALLS_SIP_ENDPOINT, ReferenceType.DN);

        InitCB(cblAttr, cblAttrValues, jpComponents, 3);
        InitCB(cblAttrValues, null, jpAttrValues, 4);

        FillElements(lmAttr, root, true);
        lmAttr.insertElementAt(MyCheckBoxList.ALL_ENTRY, 0);
        cblAttr.checkAll();

        InitOrderBy();

    }

    private void FillElements(DefaultListModel lm, DynamicTreeNode<OptionNode> parent, boolean isChecked) {
        List<DynamicTreeNode<OptionNode>> l = parent.getChildren();
        for (DynamicTreeNode<OptionNode> node : parent.getChildren()) {
//            node.getData().setChecked(isChecked);
            lm.addElement(node);
        }
    }

    class OrderField {

        private String fieldName;

        public String getFieldName() {
            return fieldName;
        }

        public String getDisplayName() {
            return displayName;
        }
        private String displayName;

        @Override
        public String toString() {
            return displayName;
        }

        public OrderField(String fieldName, String displayName) {
            this.fieldName = fieldName;
            this.displayName = displayName;
        }

    }

    public String[] getOrderBy() {
        ArrayList<String> ret = new ArrayList<>();

        for (int i = 0; i < clbOrderBy.getLm().getSize(); i++) {
            if (clbOrderBy.getCheckBoxListSelectionModel().isSelectedIndex(i)) {
                ret.add(((OrderField) clbOrderBy.getLm().get(i)).getFieldName());
            }

        }
        return ret.toArray(new String[ret.size()]);
    }

    private void InitOrderBy() {
        clbOrderBy = new MyCheckBoxList(jpOrderBy);
        clbOrderBy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        jpOrderBy.setLayout(new BorderLayout());
        jpOrderBy.add(new JScrollPane(clbOrderBy));

        clbOrderBy.getLm().addElement(new OrderField("time", "Time"));
        clbOrderBy.getLm().addElement(new OrderField("callidid", "SIP CallID"));
        clbOrderBy.checkAll();

        clbOrderBy.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ReportItemChanged(e);

            }

        });
        clbOrderBy.setSelectedIndex(0);
        jbUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clbOrderBy.SelectedUp();
            }
        });
        jbDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clbOrderBy.SelectedDown();
            }
        });

    }

    private void ReportItemChanged(ListSelectionEvent evt) {
//        javax.swing.JOptionPane.showMessageDialog(this, "Item changed", "Item changed", JOptionPane.ERROR_MESSAGE);
        if (!evt.getValueIsAdjusting()) {
//            inquirer.logger.info("ReportItemChanged - adjusting");
            ListSelectionModel lsm = (ListSelectionModel) evt.getSource();
            if (lsm.isSelectionEmpty()) {
                jbUp.setEnabled(false);
                jbDown.setEnabled(false);
            } else {
                // Find out which indexes are selected.
                int minIndex = lsm.getMinSelectionIndex();
                int maxIndex = lsm.getMaxSelectionIndex();

                jbUp.setEnabled(clbOrderBy.getLm().size() > 1 && minIndex > 0);
                jbDown.setEnabled(maxIndex < clbOrderBy.getLm().size() - 1);
            }
        }
    }

    class myListCheckListener implements ListSelectionListener {

        private final MyCheckBoxList list;
        private final MyCheckBoxList listChild;

        public myListCheckListener(MyCheckBoxList list, MyCheckBoxList listChild) {
            this.list = list;
            this.listChild = listChild;
        }

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

        public void valueChanged(ListSelectionEvent e) {
            ReportItemChanged(e, list, listChild);
        }

    }

    class myFocusListener implements FocusListener {

        public void focusGained(FocusEvent e) {
            inquirer.logger.debug("focusGained", e);
        }

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

    private ButtonGroup group = new ButtonGroup();
    private FileInfoType ft = FileInfoType.type_Unknown;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jpComponents = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jpOrderBy = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jbUp = new javax.swing.JButton();
        jbDown = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jpAttrValues = new javax.swing.JPanel();

        setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        setLayout(new java.awt.BorderLayout());

        jpComponents.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));

        javax.swing.GroupLayout jpComponentsLayout = new javax.swing.GroupLayout(jpComponents);
        jpComponents.setLayout(jpComponentsLayout);
        jpComponentsLayout.setHorizontalGroup(
            jpComponentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jpComponentsLayout.setVerticalGroup(
            jpComponentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 112, Short.MAX_VALUE)
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Order by"));

        javax.swing.GroupLayout jpOrderByLayout = new javax.swing.GroupLayout(jpOrderBy);
        jpOrderBy.setLayout(jpOrderByLayout);
        jpOrderByLayout.setHorizontalGroup(
            jpOrderByLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 284, Short.MAX_VALUE)
        );
        jpOrderByLayout.setVerticalGroup(
            jpOrderByLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jbUp.setText("Up");

        jbDown.setText("Down");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jbDown, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jbUp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jbUp)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jbDown)
                .addContainerGap(144, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jpOrderBy, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 29, Short.MAX_VALUE))
                    .addComponent(jpOrderBy, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jLabel2.setText("Attribute value");

        jLabel1.setText("Attribute type");

        jpAttrValues.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout jpAttrValuesLayout = new javax.swing.GroupLayout(jpAttrValues);
        jpAttrValues.setLayout(jpAttrValuesLayout);
        jpAttrValuesLayout.setHorizontalGroup(
            jpAttrValuesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 389, Short.MAX_VALUE)
        );
        jpAttrValuesLayout.setVerticalGroup(
            jpAttrValuesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 133, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jpComponents, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 211, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jpAttrValues, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jpComponents, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jpAttrValues, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        add(jPanel2, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JButton jbDown;
    private javax.swing.JButton jbUp;
    private javax.swing.JPanel jpAttrValues;
    private javax.swing.JPanel jpComponents;
    private javax.swing.JPanel jpOrderBy;
    // End of variables declaration//GEN-END:variables

    private void addRadioButton(String btTitle, final FileInfoType fileInfoType) {
        JRadioButton btn = new JRadioButton(btTitle);
        btn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ft = fileInfoType;
            }
        });
        group.add(btn);
        jpComponents.add(btn);
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
