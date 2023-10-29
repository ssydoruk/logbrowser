/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.TDateRange;
import Utils.TDateRange.IRefresh;
import Utils.UTCTimeRange;
import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.CheckBoxListSelectionModel;
import com.jidesoft.swing.SearchableUtils;
import com.myutils.logbrowser.inquirer.gui.ExternalEditor;
import com.myutils.logbrowser.inquirer.gui.QueryJTable;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author ssydoruk
 */
public class RequestParams extends javax.swing.JPanel {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    static private final int LEVEL = 1;
    static private final int CHILD_LISTBOX = 2;
    private final QueryDialog qd;
    ArrayList<ChangeListItem> CRIs;
    private final HashMap<CheckBoxList, String> lastRegExp = new HashMap<>(4);
    private final MyCheckBoxList cblReportType;
    private final DefaultListModel lmReportType;
    private final MyCheckBoxList cblReportComp;
    private final DefaultListModel lmReportComp;
    private final IQueryResults qry;
    private final MyCheckBoxList cblApps;
    private final DefaultListModel lmApps;
    private final DefaultListModel lmAttr;
    private final MyCheckBoxList cblAttr;
    private final DefaultListModel lmAttrValues;
    private final MyCheckBoxList cblAttrValues;
    private final TDateRange timeRange;
    private final CheckBoxList _listComps = null;
    private final DefaultListModel _modelComps = null;
    private final HashMap<Integer, int[]> compChecked = new HashMap<>();
    private final int rptSelected = -1;
    private final HashMap<CheckBoxList, ChangeListItem> changeList = new HashMap<>();
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> cbSearchLevel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JButton jbShowFiles;
    private javax.swing.JComboBox jcbSelectionType;
    private javax.swing.JPanel jpApps;
    private javax.swing.JPanel jpDateTime;
    private javax.swing.JLabel lbSearchLevel;
    private javax.swing.JPanel pSearchLevel;
    private javax.swing.JPanel pSelectionType;
    private javax.swing.JPanel pShowFiles;
    private javax.swing.JPanel rptAttrValues;
    private javax.swing.JPanel rptCompAttr;
    private javax.swing.JPanel rptComponents;
    private javax.swing.JPanel rptType;

    /**
     * Creates new form RequestParams
     *
     * @param q
     */
    public RequestParams(IQueryResults q, QueryDialog aThis) throws Exception {
        initComponents();
        qd = aThis;
        qry = q;
        rptType.setLayout(new BorderLayout());
        rptType.add(new JLabel("Report items"), BorderLayout.BEFORE_FIRST_LINE);
        rptComponents.setLayout(new BorderLayout());
        rptCompAttr.setLayout(new BorderLayout());
        rptAttrValues.setLayout(new BorderLayout());
        jpApps.setLayout(new BorderLayout());

        lmReportType = new DefaultListModel();
        cblReportType = new MyCheckBoxList(lmReportType, rptType);

        lmReportComp = new DefaultListModel();
        cblReportComp = new MyCheckBoxList(lmReportComp, rptComponents);

        lmAttr = new DefaultListModel();
        cblAttr = new MyCheckBoxList(lmAttr, rptCompAttr);
        cblAttr.setCheckBoxEnabled(false);

        lmAttrValues = new DefaultListModel();
        cblAttrValues = new MyCheckBoxList(lmAttrValues, rptAttrValues);

        InitCB(cblReportType, cblReportComp, rptType, 1);
        InitCB(cblReportComp, cblAttr, rptComponents, 2);
        InitCB(cblAttr, cblAttrValues, rptCompAttr, 3);
        InitCB(cblAttrValues, null, rptAttrValues, 4);

        cblReportType.setValueIsAdjusting(true);
        FillElements(cblReportType, qry.getReportItems().getRoot(), true);

//        cblReportType.setCheckBoxListSelectedIndex(0);
//        for (int i = 0; i < lmReportType.size(); i++) {
//            cblReportType.getCheckBoxListSelectionModel().setSelectionInterval(i, i);
//        }
//        cblReportType.initFullUpdate();
//        jcbSelectionType.removeAll();
        jcbSelectionType.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                qd.enableSearchItems(getSelectionType() != SelectionType.NO_SELECTION);
                lbSearchLevel.setEnabled(getSelectionType() != SelectionType.NO_SELECTION);
                cbSearchLevel.setEnabled(getSelectionType() != SelectionType.NO_SELECTION);
            }
        });

        for (SelectionType t : qry.getSelectionTypes()) {
            jcbSelectionType.addItem(t);
        }
        if (jcbSelectionType.getModel().getSize() > 1) {
            jcbSelectionType.setSelectedIndex(1);
        }

//<editor-fold defaultstate="collapsed" desc="Fill apps list">
        lmApps = new DefaultListModel();
        cblApps = new MyCheckBoxList(lmApps, jpApps);

        ArrayList<NameID> apps = qry.getApps();
        if (apps != null) {
            for (NameID nameID : apps) {
                lmApps.addElement(nameID);
            }
            lmApps.insertElementAt(CheckBoxList.ALL_ENTRY, 0);
            cblApps.getCheckBoxListSelectionModel().addSelectionInterval(0, 0);
        }
        JScrollPane jsp = new JScrollPane(cblApps);

        cblApps.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        SearchableUtils.installSearchable(cblApps);
        jpApps.add(jsp);
        jpApps.setMaximumSize(new Dimension(jpApps.getMaximumSize().width, jpApps.getPreferredSize().height + 10));
        jpApps.setMinimumSize(new Dimension(jpApps.getPreferredSize().width, jpApps.getMinimumSize().height));

//</editor-fold>
        timeRange = new TDateRange();
        timeRange.setTimeRange(qry.getTimeRange());
        timeRange.setRefreshCB(new IRefresh() {
            @Override
            public UTCTimeRange Refresh() {
                try {
                    return q.refreshTimeRange(getSearchApps());
                } catch (SQLException ex) {
                    logger.error("fatal: ", ex);
                }
                return null;
            }
        });
        jpDateTime.add(timeRange);
        cbSearchLevel.setModel(new DefaultComboBoxModel(IDsFinder.RequestLevel.values()));
        cbSearchLevel.setSelectedItem(q.getDefaultQueryLevel());

        pSelectionType.setMaximumSize(new Dimension(pSelectionType.getMaximumSize().width, pSelectionType.getMinimumSize().height));
        pSearchLevel.setMaximumSize(new Dimension(pSearchLevel.getMaximumSize().width, pSearchLevel.getMinimumSize().height));
        pShowFiles.setMaximumSize(new Dimension(pShowFiles.getMaximumSize().width, pShowFiles.getMinimumSize().height));
    }

    ArrayList<Integer> getSearchApps() {
        return getSearchApps(false);
    }

    ArrayList<Integer> getSearchApps(boolean returnAllIfListFull) {
        CheckBoxListSelectionModel checkBoxListSelectionModel = cblApps.getCheckBoxListSelectionModel();
        ListModel model = cblApps.getModel();
        ArrayList<Integer> ret = new ArrayList<>();

        for (int i = 1; i < model.getSize(); i++) {
            if (checkBoxListSelectionModel.isSelectedIndex(i)) {
                ret.add(((NameID) model.getElementAt(i)).getId());
            }
        }
        if (ret.isEmpty()) {
            return null; // no apps selected means do not run request
        } else {
            if (ret.size() == model.getSize() - 1 && !returnAllIfListFull) {
                return new ArrayList<>(); // empty list means 'do not apply filter'
            } else {
                return ret;
            }
        }
    }

    ArrayList<String> getSearchAppsName(boolean returnAllIfListFull) {
        CheckBoxListSelectionModel checkBoxListSelectionModel = cblApps.getCheckBoxListSelectionModel();
        ListModel model = cblApps.getModel();
        ArrayList<String> ret = new ArrayList<>();

        for (int i = 1; i < model.getSize(); i++) {
            if (checkBoxListSelectionModel.isSelectedIndex(i)) {
                ret.add(((NameID) model.getElementAt(i)).getName());
            }
        }
        if (ret.isEmpty()) {
            return null; // no apps selected means do not run request
        } else {
            if (ret.size() == model.getSize() - 1 && !returnAllIfListFull) {
                return new ArrayList<>(); // empty list means 'do not apply filter'
            } else {
                return ret;
            }
        }
    }

    void uncheckReport() {
        cblReportType.unCheck();
    }

    UTCTimeRange getTimeRange() throws SQLException {
        return timeRange.getTimeRange();
    }

    void reSet() {
        qry.resetOptions();
        FillElements(cblReportType, qry.getReportItems().getRoot(), true);
        jcbSelectionType.setSelectedItem(SelectionType.GUESS_SELECTION);
    }

    String getFiltersSummary() {
        DynamicTreeNode<OptionNode> root = qry.getReportItems().getRoot();
        if (root != null) {
            return root.getSummary(false, 3);
        }
        return "";
    }

    private void enableBoxes(Integer level, boolean isEnabled) {
//        inquirer.logger.debug("Freezing controls: " + isEnabled + " level: " + level);
        if (level != null && level == 3) {
//            inquirer.logger.debug("Level 3!!");
            cblReportType.setEnabled(isEnabled);
            cblReportComp.setEnabled(isEnabled);
            cblAttr.setEnabled(isEnabled);
            cblAttrValues.setEnabled(isEnabled);
            cblApps.setEnabled(isEnabled);
            jcbSelectionType.setEnabled(isEnabled);
            jbShowFiles.setEnabled(isEnabled);
            timeRange.setEnabled(isEnabled);

        }
    }

    private void InitCB(CheckBoxList list, CheckBoxList clbChild, JPanel rptType, int i) {
        list.putClientProperty(LEVEL, i);

        rptType.add(new JScrollPane(list));

        list.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        SearchableUtils.installSearchable(list);

        list.getCheckBoxListSelectionModel().addListSelectionListener(new myListCheckListener(list, clbChild));
        list.getSelectionModel().addListSelectionListener(new myListSelectionListener(list, clbChild));

    }

    private void FillElements(DefaultListModel lmReportType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void FillElements(MyCheckBoxList cb, DynamicTreeNode<OptionNode> parent, boolean isChecked) {
        boolean allChecked = true;
        cb.initFullUpdate();
        for (int i = 0; i < parent.getNumberOfChildren(); i++) {
            DynamicTreeNode<OptionNode> node = parent.getChildAt(i);
            OptionNode opt = ((OptionNode) (node).getData());
            cb.getLm().addElement(node);
            if (opt.isChecked()) {
                cb.getCheckBoxListSelectionModel().addSelectionInterval(i, i);
            } else {
                allChecked = false;
            }
        }
        cb.doneFullUpdate(allChecked);
    }

    public IQueryResults getQry() {
        return qry;
    }

    public IDsFinder.RequestLevel getSearchLevel() {
        return (IDsFinder.RequestLevel) cbSearchLevel.getSelectedItem();
    }

    public SelectionType getSelectionType() {

        return (SelectionType) (jcbSelectionType.getModel().getSelectedItem());
    }

    private void setChildChecked(CheckBoxList lChild, boolean boxEnabled, boolean isCurrent) {
//        inquirer.logger.debug("in setChildChecked " + boxEnabled);
        boolean checkedItem = false;
        if (isCurrent) {
            lChild.setEnabled(boxEnabled);
        }
        DefaultListModel lm = (DefaultListModel) lChild.getModel();
        ArrayList<Integer> idx = new ArrayList<>(lm.size());
        int firstIdx = (lChild.isCheckBoxEnabled()) ? 1 : 0;
        for (int i = firstIdx; i < lm.size(); i++) {
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

    private void setChildChecked(CheckBoxList lChild, boolean boxEnabled) {
        setChildChecked(lChild, boxEnabled, true);
    }

    private void changeReportItem(int item, DynamicTreeNode<OptionNode> node, CheckBoxList lChild) {
//        ChangeListItem cli = changeList.get(lChild);
//        if( cli!=null ) {
//            cli.changeItem(item, null);
//        }
//        if (lChild != null && rptSelected >= 0) {
////            _listComps.setValueIsAdjusting(true);
//            compChecked.put(rptSelected, lChild.getCheckBoxListSelectedIndices());
//        }
//        inquirer.logger.debug("changeReportItem " + item + " node: [" + node + "] lchild: [" + lChild + "]");
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
        if (lChild.isCheckBoxEnabled()) {
            lm.insertElementAt(CheckBoxList.ALL_ENTRY, 0);
        }
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

    private void enableDependentList(javax.swing.event.ListSelectionEvent evt, CheckBoxList list) {
        if (!evt.getValueIsAdjusting()) {
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
            if (lsm.isSelectionEmpty()) {
//                inquirer.logger.info(" <none>");
                list.setEnabled(false);
            } else {
                // Find out which indexes are selected.
                int minIndex = lsm.getMinSelectionIndex();
                int maxIndex = lsm.getMaxSelectionIndex();
                for (int i = minIndex; i <= maxIndex; i++) {
//                    inquirer.logger.info("ReportItemChecked "+i);
                    list.setEnabled(lsm.isSelectedIndex(i));
                }
            }
        }

    }

    public int findInt(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private void ReportItemChecked(javax.swing.event.ListSelectionEvent evt, CheckBoxList l, CheckBoxList lChild) {
        if (!evt.getValueIsAdjusting()) {
//            inquirer.logger.debug("ReportItemChecked " + evt);
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
            if (lsm.isSelectionEmpty() && lChild != null) {
//                inquirer.logger.debug(" <none>");
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
                int firstIdx = (l.isCheckBoxEnabled()) ? 1 : 0;
                for (int i = firstIdx; i < lsm.getModel().getSize(); i++) {
                    OptionNode node = ((OptionNode) ((DynamicTreeNode<OptionNode>) lsm.getModel().getElementAt(i)).getData());
                    if (node.isChecked() != lsm.isSelectedIndex(i)) {     //value changed
                        node.setChecked(lsm.isSelectedIndex(i));
                        if (lChild != null) {
                            setChildChecked(lChild, lsm.isSelectedIndex(i), l.getSelectedIndex() == i);
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
////                        inquirer.logger.info("all selected? "+lm.get(i).equals(CheckBoxList.ALL_ENTRY));
////                        DynamicTreeNode<OptionNode> node = (DynamicTreeNode<OptionNode>)lm.get(i);
////                        node.getData().setChecked(lsm.isSelectedIndex(i));
////                    }
////                    (DynamicTreeNode<OptionNode>) lm.get(i)
//                }
            }
        }

    }

    private void ReportItemChanged(javax.swing.event.ListSelectionEvent evt, CheckBoxList l, CheckBoxList lChild) {
//        javax.swing.JOptionPane.showMessageDialog(this, "Item changed", "Item changed", JOptionPane.ERROR_MESSAGE);
        if (!evt.getValueIsAdjusting()) {
//            inquirer.logger.trace("ReportItemChanged - " + evt);
//            inquirer.logger.info("ReportItemChanged - adjusting");
            ListSelectionModel lsm = (ListSelectionModel) evt.getSource();

            Integer level = (Integer) l.getClientProperty(LEVEL);
            enableBoxes(level, false);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
//                    inquirer.logger.info("changing item 1");
                    if (lsm.isSelectionEmpty()) {
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
//                        inquirer.logger.debug("**current item " + i);
                                DefaultListModel lm = (DefaultListModel) l.getModel();
                                int firstID = lm.get(i).equals(CheckBoxList.ALL_ENTRY) ? 1 : 0;
                                if (lChild != null) {

                                    if (i >= firstID) {
                                        changeReportItem(i, (DynamicTreeNode<OptionNode>) lm.get(i), lChild);
                                    }
                                }
                            }
                        }
                    }
                    enableBoxes(level, true);

                }
            });
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel5 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        rptType = new javax.swing.JPanel();
        rptComponents = new javax.swing.JPanel();
        rptCompAttr = new javax.swing.JPanel();
        rptAttrValues = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jpDateTime = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        pSelectionType = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jcbSelectionType = new javax.swing.JComboBox();
        pSearchLevel = new javax.swing.JPanel();
        lbSearchLevel = new javax.swing.JLabel();
        cbSearchLevel = new javax.swing.JComboBox<>();
        jPanel4 = new javax.swing.JPanel();
        jpApps = new javax.swing.JPanel();
        pShowFiles = new javax.swing.JPanel();
        jbShowFiles = new javax.swing.JButton();

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        rptType.setBorder(javax.swing.BorderFactory.createTitledBorder("Reporting on"));

        javax.swing.GroupLayout rptTypeLayout = new javax.swing.GroupLayout(rptType);
        rptType.setLayout(rptTypeLayout);
        rptTypeLayout.setHorizontalGroup(
                rptTypeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 563, Short.MAX_VALUE)
        );
        rptTypeLayout.setVerticalGroup(
                rptTypeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel3.add(rptType);

        rptComponents.setBorder(javax.swing.BorderFactory.createTitledBorder("Report components"));

        javax.swing.GroupLayout rptComponentsLayout = new javax.swing.GroupLayout(rptComponents);
        rptComponents.setLayout(rptComponentsLayout);
        rptComponentsLayout.setHorizontalGroup(
                rptComponentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        rptComponentsLayout.setVerticalGroup(
                rptComponentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 186, Short.MAX_VALUE)
        );

        jPanel3.add(rptComponents);

        rptCompAttr.setBorder(javax.swing.BorderFactory.createTitledBorder("Component attributes"));

        javax.swing.GroupLayout rptCompAttrLayout = new javax.swing.GroupLayout(rptCompAttr);
        rptCompAttr.setLayout(rptCompAttrLayout);
        rptCompAttrLayout.setHorizontalGroup(
                rptCompAttrLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 587, Short.MAX_VALUE)
        );
        rptCompAttrLayout.setVerticalGroup(
                rptCompAttrLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel3.add(rptCompAttr);

        rptAttrValues.setBorder(javax.swing.BorderFactory.createTitledBorder("Attribute values"));

        javax.swing.GroupLayout rptAttrValuesLayout = new javax.swing.GroupLayout(rptAttrValues);
        rptAttrValues.setLayout(rptAttrValuesLayout);
        rptAttrValuesLayout.setHorizontalGroup(
                rptAttrValuesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        rptAttrValuesLayout.setVerticalGroup(
                rptAttrValuesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 186, Short.MAX_VALUE)
        );

        jPanel3.add(rptAttrValues);

        jPanel5.add(jPanel3);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.PAGE_AXIS));

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
                jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 143, Short.MAX_VALUE)
        );
        jPanel10Layout.setVerticalGroup(
                jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel6.add(jPanel10);

        jpDateTime.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jpDateTime.setLayout(new javax.swing.BoxLayout(jpDateTime, javax.swing.BoxLayout.LINE_AXIS));
        jPanel6.add(jpDateTime);

        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.PAGE_AXIS));

        pSelectionType.setLayout(new javax.swing.BoxLayout(pSelectionType, javax.swing.BoxLayout.LINE_AXIS));

        jLabel1.setText("Search type");
        pSelectionType.add(jLabel1);

        pSelectionType.add(jcbSelectionType);

        jPanel9.add(pSelectionType);

        pSearchLevel.setLayout(new javax.swing.BoxLayout(pSearchLevel, javax.swing.BoxLayout.LINE_AXIS));

        lbSearchLevel.setText("Search level");
        pSearchLevel.add(lbSearchLevel);

        pSearchLevel.add(cbSearchLevel);

        jPanel9.add(pSearchLevel);

        jPanel6.add(jPanel9);

        jPanel2.add(jPanel6);

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        jpApps.setBorder(javax.swing.BorderFactory.createTitledBorder("Applications"));
        jpApps.setLayout(new java.awt.BorderLayout());
        jPanel4.add(jpApps);

        pShowFiles.setLayout(new javax.swing.BoxLayout(pShowFiles, javax.swing.BoxLayout.LINE_AXIS));

        jbShowFiles.setText("Show files");
        jbShowFiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbShowFilesActionPerformed(evt);
            }
        });
        pShowFiles.add(jbShowFiles);

        jPanel4.add(pShowFiles);

        jPanel2.add(jPanel4);

        jPanel5.add(jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, 1199, Short.MAX_VALUE)
                                .addGap(22, 22, 22))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jbShowFilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbShowFilesActionPerformed

        try {
            QueryJTable allAppsT = new QueryJTable(qry.getFileFields(getSearchApps(true)));

            Dimension d = allAppsT.getPreferredSize();
//            Dimension d = allApps.getPreferredSize();
//            d.height = 300;
//            d.width += 50;

            JScrollPane jScrollPane = new JScrollPane(allAppsT);
            d.height = 500;
            d.width += jScrollPane.getVerticalScrollBar().getMaximumSize().width + 4;
//            jScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
//            d = allAppsT.getPreferredScrollableViewportSize();
            jScrollPane.setPreferredSize(d);
            JMenu menu = new JMenu("Open file in...");
            allAppsT.getPopupMenu().add(menu);
            menu.add(new OpenVIM(allAppsT));
            menu.add(new OpenNotepad(allAppsT));
            menu.add(new OpenTextPad(allAppsT));

            inquirer.showInfoPanel((Window) this.getRootPane().getParent(), "All files", jScrollPane, false);
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
    }//GEN-LAST:event_jbShowFilesActionPerformed

    private class OpenVIM extends AbstractAction {

        private QueryJTable tab;

        public OpenVIM() {
            super("VIM");
        }

        private OpenVIM(QueryJTable allAppsT) {
            this();
            this.tab = allAppsT;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String fileName = tab.getField("filename", tab.getPopupRow());
            if (fileName != null) {
                ArrayList<String> shortFileNames = new ArrayList<>(1);
                shortFileNames.add(fileName);
                ExternalEditor.editFiles(shortFileNames);
            }
        }

    }

    private class OpenNotepad extends AbstractAction {

        private QueryJTable tab;

        public OpenNotepad() {
            super("Notepad++");
        }

        private OpenNotepad(QueryJTable allAppsT) {
            this();
            this.tab = allAppsT;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String fileName = tab.getField("filename", tab.getPopupRow());
            if (fileName != null) {
                ExternalEditor.openNotepad(fileName, 0);
            }
        }

    }

    private class OpenTextPad extends AbstractAction {

        private QueryJTable tab;

        public OpenTextPad() {
            super("Textpad");
        }

        private OpenTextPad(QueryJTable allAppsT) {
            this();
            this.tab = allAppsT;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String fileName = tab.getField("filename", tab.getPopupRow());
            if (fileName != null) {
                ExternalEditor.openTextpad(fileName, 0);
            }
        }

    }

    class myListSelectionListener implements ListSelectionListener {

        private final CheckBoxList list;
        private final CheckBoxList listChild;

        public myListSelectionListener(CheckBoxList list, CheckBoxList listChild) {
            this.list = list;
            this.listChild = listChild;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            ReportItemChanged(e, list, listChild);
        }

    }

    class myListCheckListener implements ListSelectionListener {

        private final CheckBoxList list;
        private final CheckBoxList listChild;

        public myListCheckListener(CheckBoxList list, CheckBoxList listChild) {
            this.list = list;
            this.listChild = listChild;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            ReportItemChecked(e, list, listChild);
        }

    }

    class ChangeListItem {

        IReportConfigItem item = null;
        private int parentSelected = -1;
        private CheckBoxList chList = null;
        private DefaultListModel chModel = null;
        private JPanel parentPanel = null;
        private ArrayList<String> valuesSource = null;
        private ChangeListItem childList = null;

        public ChangeListItem(IReportConfigItem item, JPanel parentPanel) {
            this.parentPanel = parentPanel;
            this.item = item;
        }

        public ChangeListItem(CheckBoxList _list, DefaultListModel _model) {
            this.chList = _list;
            this.chModel = _model;
        }

        public void changeItem(int item, ArrayList<String> values) {
            if (chList != null && parentSelected >= 0) {
                //            _listComps.setValueIsAdjusting(true);
                compChecked.put(parentSelected, chList.getCheckBoxListSelectedIndices());
            }
            chModel.removeAllElements();
            if (values != null) {
                chModel.insertElementAt(CheckBoxList.ALL_ENTRY, 0);
                for (String value : values) {
                    chModel.addElement(value);
                }
                chList.setEnabled(chList.getCheckBoxListSelectionModel().isSelectedIndex(item));
                parentSelected = item;
                if (compChecked.containsKey(parentSelected)) {
                    chList.setCheckBoxListSelectedIndices(compChecked.get(parentSelected));
                }

                chList.revalidate();
                chList.repaint();
            }

        }

        /**
         * @param parentPanel the parentPanel to set
         */
        public void setParentPanel(JPanel parentPanel) {
            this.parentPanel = parentPanel;
        }

        /**
         * @param valuesSource the valuesSource to set
         */
        public void setValuesSource(ArrayList<String> valuesSource) {
            this.valuesSource = valuesSource;
        }

        /**
         * @param childList the childList to set
         */
        public void setChildList(ChangeListItem childList) {
            this.childList = childList;
        }

    }
    // End of variables declaration//GEN-END:variables
}
