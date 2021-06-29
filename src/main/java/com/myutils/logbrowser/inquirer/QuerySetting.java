/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.jidesoft.swing.CheckBoxListSelectionModel;
import com.jidesoft.swing.SearchableUtils;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.InquirerCfg.GenesysConstant;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author ssydoruk
 */
public final class QuerySetting extends javax.swing.JDialog {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    /**
     * Creates new form QuerySetting
     */
    private final InquirerCfg cr;
    private final JList lPrintFilters;
    private final HashMap<String, String> printFilters;
    Object curSelected = null;
    TableType lastTableType;
    private JList lReferenceType;
    private CheckBoxListSettings clbRefValues;
    private DefaultListModel lmRefValues;
    private int closeCause;
    private CheckBoxListSettings clbLogMessages;
    private HashMap<ReferenceType, ArrayList<OptionNode>> savedRefs;
    private ReferenceType lastRefType;
    private HashMap<TableType, ArrayList<OptionNode>> savedLogMessages;
    private JList lAppType;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btCancel;
    private javax.swing.JButton btOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JButton jbConstantAdd;
    private javax.swing.JButton jbConstantRemove;
    private javax.swing.JButton jbConstantRename;
    private javax.swing.JButton jbTheConstantAdd;
    private javax.swing.JButton jbTheConstantPaste;
    private javax.swing.JButton jbTheConstantRemove;
    private javax.swing.JButton jbTheConstantRename;
    private javax.swing.JCheckBox jcbAccessFiles;
    private javax.swing.JCheckBox jcbDoSkip;
    private javax.swing.JCheckBox jcbIncludeOrderBy;
    private javax.swing.JCheckBox jcbLimitQueryResults;
    private javax.swing.JCheckBox jcbNewTLibSearch;
    private javax.swing.JCheckBox jcbPrintLogFileName;
    private javax.swing.JCheckBox jcbShowFullDate;
    private javax.swing.JCheckBox jcbSort;
    private javax.swing.JCheckBox jcbignoreFormatting;
    private javax.swing.JList<String> jlConstants;
    private javax.swing.JPanel jpAllRefs;
    private javax.swing.JPanel jpConstants;
    private javax.swing.JPanel jpFileTypes;
    private javax.swing.JPanel jpGenesysMessages;
    private javax.swing.JPanel jpLogMessages;
    private javax.swing.JPanel jpOutput;
    private javax.swing.JPanel jpReferences;
    private javax.swing.JPanel jpRegExps;
    private javax.swing.JSpinner jsMaxQueryLines;
    private javax.swing.JSpinner jsMaxRecords;
    private javax.swing.JTable jtConstants;
    private javax.swing.JFormattedTextField jtfFileSize;
    private javax.swing.JTextField jtfTitleRegex;
    private javax.swing.JPanel pAppType;
    private javax.swing.JPanel pRefTypes;
    private javax.swing.JTextArea taRegExs;
    private javax.swing.JTextField tfFileNameExcel;
    private javax.swing.JTextField tfFileNameLong;
    private javax.swing.JTextField tfFileNameShort;
    public QuerySetting(java.awt.Frame parent, boolean modal, InquirerCfg cr) {
        super(parent, modal);
        setTitle("Settings");
        this.cr = cr;
        initComponents();

//        jcbSaveShortFileOutput.setSelected(cr.isSaveFileShort());
        tfFileNameLong.setText(cr.getFileNameLongBase());
        tfFileNameShort.setText(cr.getFileNameShortBase());
        tfFileNameExcel.setText(cr.getFileNameExcelBase());
        jtfFileSize.setText(new Integer(cr.getFileSizeWarn()).toString());
        jsMaxRecords.getModel().setValue(cr.getMaxRecords());
        retrieveLogMessages(cr.retrieveLogMessages());
        retrieveRefs(cr.retrieveRefs());

        /**
         * ********************************************
         */
        DefaultListModel lmFilters = new DefaultListModel();
        lPrintFilters = new JList(lmFilters);
        lPrintFilters.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        lPrintFilters.setLayoutOrientation(JList.VERTICAL);
        jpFileTypes.setLayout(new BorderLayout());
//        spRefTypes.setLayout(new ScrollPaneLayout());
        jpFileTypes.add(new JScrollPane(lPrintFilters));

        printFilters = cr.getPrintFilters();
        for (String str : printFilters.keySet()) {
            lmFilters.addElement(str);
        }

        lPrintFilters.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                lPrintFiltersChanged(evt);
            }

        });

        jcbDoSkip.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean isEnabled = e.getStateChange() == ItemEvent.SELECTED;
                lPrintFilters.setEnabled(isEnabled);
                taRegExs.setEnabled(isEnabled);
                jpFileTypes.setEnabled(isEnabled);
                jpRegExps.setEnabled(isEnabled);
            }
        });

        jcbDoSkip.setSelected(cr.isApplyFullFilter());

        jcbLimitQueryResults.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean isEnabled = e.getStateChange() == ItemEvent.SELECTED;
                jsMaxQueryLines.setEnabled(isEnabled);
            }
        });
        jcbLimitQueryResults.setSelected(cr.isLimitQueryResults());
        jsMaxQueryLines.getModel().setValue(cr.getMaxQueryLines());
        jcbAccessFiles.setSelected(cr.isAccessLogFiles());
        jcbSort.setSelected(cr.isSorting());

        jcbIncludeOrderBy.setSelected(cr.isAddOrderBy());
        jcbPrintLogFileName.setSelected(cr.isPrintLogFileName());
        jcbignoreFormatting.setSelected(cr.isIgnoreFormatting());
        jtfTitleRegex.setText(cr.getTitleRegex());
        jcbNewTLibSearch.setSelected(cr.isNewTLibSearch());

        jcbShowFullDate.setSelected(cr.isFullTimeStamp());

        DefaultListModel model = new DefaultListModel<InquirerCfg.GenesysConstant>();
        InquirerCfg.GenesysConstants constants = cr.getConstants();
        if (constants != null) {
            for (InquirerCfg.GenesysConstant c : constants.getConstants()) {
                model.addElement(c);
            }
        }
        jlConstants.setModel(model);
        jlConstants.addListSelectionListener(new jlConstantsSelectionListener(jtConstants));

        jtConstants.setModel(new ConstantsModel());

//                Dimension preferredSize = jScrollPane1.getPreferredSize();
//                preferredSize.width-=20;
//
//
//        jtConstants.setPreferredSize(preferredSize);
//        tca = new TableColumnAdjuster(jtConstants);
//        tca.setColumnDataIncluded(true);
//        tca.setColumnHeaderIncluded(false);
//        tca.setDynamicAdjustment(true);
        loadLogMessages();
        loadReferences();

    }

    private void loadReferences() {
        lastRefType = ReferenceType.UNKNOWN;
        lmRefValues = new DefaultListModel();
        clbRefValues = new CheckBoxListSettings(lmRefValues, jpAllRefs);

        jpAllRefs.setLayout(new BorderLayout());
        JScrollPane jScrollPane = new JScrollPane(clbRefValues);
        Dimension d = clbRefValues.getPreferredSize();
        d.width = 300;
        jScrollPane.setPreferredSize(d);
        jpAllRefs.add(jScrollPane);

        clbRefValues.getCheckBoxListSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        SearchableUtils.installSearchable(clbRefValues);

        clbRefValues.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                clbValueChanged(evt);
            }
        });

        DefaultListModel lm = new DefaultListModel();
        pRefTypes.setLayout(new BorderLayout());
        lReferenceType = new JList(lm);
//        lReferenceType.setVisibleRowCount(1000);
//        lReferenceType.setFixedCellWidth(500);
        lReferenceType.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        lReferenceType.setLayoutOrientation(JList.VERTICAL);
//        spRefTypes.setLayout(new ScrollPaneLayout());
        jScrollPane = new JScrollPane(lReferenceType);
        pRefTypes.add(jScrollPane);
        ArrayList<ReferenceType> ar = new ArrayList<>(savedRefs.keySet());
        Collections.sort(ar, (o1, o2) -> {
            return o1.toString().compareToIgnoreCase(o2.toString());
        });

        for (ReferenceType rt : ar) {
            lm.addElement(rt);
        }

        lReferenceType.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                lReferenceTypeChanged(evt);
            }

        });

        lReferenceType.setSelectedIndex(0);

    }

    private void retrieveLogMessages(HashMap<TableType, ArrayList<OptionNode>> msgs) {
        if (msgs != null && msgs.size() > 0) {
            savedLogMessages = new HashMap<>();
            savedLogMessages.putAll(msgs);
        }
    }

    private void retrieveRefs(HashMap<ReferenceType, ArrayList<OptionNode>> msgs) {
        if (msgs != null && msgs.size() > 0) {
            savedRefs = new HashMap<>();
            savedRefs.putAll(msgs);
        }

    }

    private void clbValueChanged(ListSelectionEvent evt) {
        if (!evt.getValueIsAdjusting()) {
            inquirer.logger.debug("ReportItemChecked " + evt);
            CheckBoxListSelectionModel lsm = (CheckBoxListSelectionModel) evt.getSource();
            if (lsm.isSelectionEmpty()) {
                inquirer.logger.debug(" <none>");
            } else {
                lsm.setValueIsAdjusting(true);
                for (int i = 1; i < lsm.getModel().getSize(); i++) {
                    OptionNode node = ((OptionNode) lsm.getModel().getElementAt(i));
                    if (node.isChecked() != lsm.isSelectedIndex(i)) {     //value changed
                        node.setChecked(lsm.isSelectedIndex(i));
                    }
                    lsm.setValueIsAdjusting(false);
                }
            }
        }

    }

    private void loadLogMessages() {
        lastTableType = TableType.UNKNOWN;

        clbLogMessages = new CheckBoxListSettings(new DefaultListModel(), jpLogMessages);

        clbLogMessages.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent evt) {
                clbValueChanged(evt);
            }
        });

        DefaultListModel lm = new DefaultListModel();
        pAppType.setLayout(new BorderLayout());
        lAppType = new JList(lm);
//        lAppType.setVisibleRowCount(1000);
//        lReferenceType.setFixedCellWidth(500);
        lAppType.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        lAppType.setLayoutOrientation(JList.VERTICAL);
//        spRefTypes.setLayout(new ScrollPaneLayout());
        JScrollPane jScrollPane = new JScrollPane(lAppType);
        pAppType.add(jScrollPane);

        for (TableType rt : savedLogMessages.keySet()) {
            lm.addElement(rt);
        }

        lAppType.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                lAppTypeChanged(evt);
            }

        });

        lAppType.setSelectedIndex(0);

    }

    private void commitAppTypeChange(TableType newTableType) {
        if (newTableType != lastTableType) {
            if (lastTableType != TableType.UNKNOWN) {
                ArrayList<OptionNode> logMessages = savedLogMessages.get(lastTableType);
                logMessages.clear();
                DefaultListModel model = (DefaultListModel) clbLogMessages.getModel();
                for (int i = 0; i < model.size(); i++) {
                    Object get = model.get(i);
                    if (get instanceof OptionNode) {
                        logMessages.add((OptionNode) get);
                    }
                }
            }
            lastTableType = newTableType;
        }
    }

    private void commitRefTypeChange(ReferenceType newRefType) {
        if (newRefType != lastRefType) {
            if (lastRefType != ReferenceType.UNKNOWN) {
                ArrayList<OptionNode> refs = savedRefs.get(lastRefType);
                refs.clear();
                DefaultListModel model = (DefaultListModel) clbRefValues.getModel();
                for (int i = 0; i < model.size(); i++) {
                    Object get = model.get(i);
                    if (get instanceof OptionNode) {
                        refs.add((OptionNode) get);
                    }
                }
            }
            lastRefType = newRefType;
        }
    }

    private void lAppTypeChanged(ListSelectionEvent evt) {

        JList cb = (JList) evt.getSource();

        TableType newTableType = (TableType) cb.getSelectedValue();
        commitAppTypeChange(newTableType);

        boolean shouldCheckAll = true;
        clbLogMessages.initFullUpdate();
        ArrayList<OptionNode> refs = savedLogMessages.get(newTableType);
        ArrayList<Integer> idx = new ArrayList<>();

        DefaultListModel model = (DefaultListModel) clbLogMessages.getModel();
        for (int i = 0; i < refs.size(); i++) {
            OptionNode node = refs.get(i);
            model.addElement(node);
            if (node.isChecked()) {
                idx.add(i);
            } else {
                shouldCheckAll = false;
            }
        }
        clbLogMessages.setCheckBoxListSelectedIndices(inquirer.toArray(idx));

        clbLogMessages.doneFullUpdate(shouldCheckAll);
    }

    private void lPrintFiltersChanged(ListSelectionEvent evt) {
        JList cb = (JList) evt.getSource();
        Object selectedValue = cb.getSelectedValue();
        if (curSelected != null) {
            printFilters.put((String) curSelected, taRegExs.getText());
        }
        if (selectedValue != null) {
            String get = printFilters.get(selectedValue);
            taRegExs.setText(get);
        }

        curSelected = selectedValue;
    }

    private void lReferenceTypeChanged(ListSelectionEvent evt) {
        JList cb = (JList) evt.getSource();
        boolean shouldCheckAll = true;
        ReferenceType newRefType = (ReferenceType) cb.getSelectedValue();
        commitRefTypeChange(newRefType);
        clbRefValues.initFullUpdate();
        ArrayList<OptionNode> refs = savedRefs.get(newRefType);
        ArrayList<Integer> idx = new ArrayList<>();

        for (int i = 0; i < refs.size(); i++) {
            OptionNode node = refs.get(i);
            lmRefValues.addElement(node);
            if (node.isChecked()) {
                idx.add(i);
            } else {
                shouldCheckAll = false;
            }
        }
        clbRefValues.setCheckBoxListSelectedIndices(inquirer.toArray(idx));

        clbRefValues.doneFullUpdate(shouldCheckAll);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jpReferences = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        pRefTypes = new javax.swing.JPanel();
        jpAllRefs = new javax.swing.JPanel();
        jpOutput = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel10 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jsMaxRecords = new javax.swing.JSpinner();
        jPanel4 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        tfFileNameShort = new javax.swing.JTextField();
        jPanel8 = new javax.swing.JPanel();
        jPanel22 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        tfFileNameLong = new javax.swing.JTextField();
        jPanel7 = new javax.swing.JPanel();
        jpFileTypes = new javax.swing.JPanel();
        jpRegExps = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        taRegExs = new javax.swing.JTextArea();
        jcbDoSkip = new javax.swing.JCheckBox();
        jPanel23 = new javax.swing.JPanel();
        tfFileNameExcel = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jPanel24 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jtfFileSize = new javax.swing.JFormattedTextField();
        jLabel9 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jcbPrintLogFileName = new javax.swing.JCheckBox();
        jcbignoreFormatting = new javax.swing.JCheckBox();
        jcbShowFullDate = new javax.swing.JCheckBox();
        jPanel9 = new javax.swing.JPanel();
        jcbLimitQueryResults = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        jsMaxQueryLines = new javax.swing.JSpinner();
        jcbIncludeOrderBy = new javax.swing.JCheckBox();
        jcbAccessFiles = new javax.swing.JCheckBox();
        jcbSort = new javax.swing.JCheckBox();
        jcbNewTLibSearch = new javax.swing.JCheckBox();
        jPanel12 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jtfTitleRegex = new javax.swing.JTextField();
        jpConstants = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jPanel17 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jlConstants = new javax.swing.JList<>();
        jPanel18 = new javax.swing.JPanel();
        jbConstantAdd = new javax.swing.JButton();
        jbConstantRemove = new javax.swing.JButton();
        jbConstantRename = new javax.swing.JButton();
        jPanel16 = new javax.swing.JPanel();
        jPanel20 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtConstants = new javax.swing.JTable();
        jPanel19 = new javax.swing.JPanel();
        jbTheConstantRemove = new javax.swing.JButton();
        jbTheConstantRename = new javax.swing.JButton();
        jbTheConstantPaste = new javax.swing.JButton();
        jbTheConstantAdd = new javax.swing.JButton();
        jpGenesysMessages = new javax.swing.JPanel();
        jPanel21 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        pAppType = new javax.swing.JPanel();
        jpLogMessages = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        btOK = new javax.swing.JButton();
        btCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));

        jPanel2.setLayout(new java.awt.BorderLayout());

        jLabel1.setText("Reference type");

        javax.swing.GroupLayout pRefTypesLayout = new javax.swing.GroupLayout(pRefTypes);
        pRefTypes.setLayout(pRefTypesLayout);
        pRefTypesLayout.setHorizontalGroup(
                pRefTypesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pRefTypesLayout.setVerticalGroup(
                pRefTypesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 487, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel5Layout.createSequentialGroup()
                                                .addComponent(jLabel1)
                                                .addGap(0, 264, Short.MAX_VALUE))
                                        .addComponent(pRefTypes, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel5Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pRefTypes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jpAllRefsLayout = new javax.swing.GroupLayout(jpAllRefs);
        jpAllRefs.setLayout(jpAllRefsLayout);
        jpAllRefsLayout.setHorizontalGroup(
                jpAllRefsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 589, Short.MAX_VALUE)
        );
        jpAllRefsLayout.setVerticalGroup(
                jpAllRefsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 746, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jpReferencesLayout = new javax.swing.GroupLayout(jpReferences);
        jpReferences.setLayout(jpReferencesLayout);
        jpReferencesLayout.setHorizontalGroup(
                jpReferencesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jpReferencesLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jpAllRefs, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(657, Short.MAX_VALUE))
        );
        jpReferencesLayout.setVerticalGroup(
                jpReferencesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jpReferencesLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jpReferencesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jpReferencesLayout.createSequentialGroup()
                                                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 417, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(jpAllRefs, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        jTabbedPane1.addTab("References", jpReferences);

        jpOutput.setLayout(new javax.swing.BoxLayout(jpOutput, javax.swing.BoxLayout.LINE_AXIS));

        jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Screen output"));

        jLabel2.setText("Max records number");

        jsMaxRecords.setModel(new javax.swing.SpinnerNumberModel(2000, 0, null, 1));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jsMaxRecords, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jsMaxRecords, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel2))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel10.add(jPanel3);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("File output"));
        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Brief output"));

        jLabel3.setText("file name");

        tfFileNameShort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfFileNameShortActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
                jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel6Layout.createSequentialGroup()
                                .addGap(176, 176, 176)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tfFileNameShort, javax.swing.GroupLayout.PREFERRED_SIZE, 438, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
                jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel6Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(tfFileNameShort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel3))
                                .addContainerGap())
        );

        jPanel4.add(jPanel6);

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("Full output"));
        jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel5.setText("file name");

        tfFileNameLong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfFileNameLongActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel22Layout = new javax.swing.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(
                jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel22Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tfFileNameLong, javax.swing.GroupLayout.PREFERRED_SIZE, 438, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        jPanel22Layout.setVerticalGroup(
                jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel22Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(tfFileNameLong, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel5))
                                .addContainerGap())
        );

        jPanel8.add(jPanel22);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Lines to skip"));

        jpFileTypes.setBorder(javax.swing.BorderFactory.createTitledBorder("File type"));

        javax.swing.GroupLayout jpFileTypesLayout = new javax.swing.GroupLayout(jpFileTypes);
        jpFileTypes.setLayout(jpFileTypesLayout);
        jpFileTypesLayout.setHorizontalGroup(
                jpFileTypesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 171, Short.MAX_VALUE)
        );
        jpFileTypesLayout.setVerticalGroup(
                jpFileTypesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 110, Short.MAX_VALUE)
        );

        jpRegExps.setBorder(javax.swing.BorderFactory.createTitledBorder("Regexps list"));
        jpRegExps.setLayout(new java.awt.BorderLayout());

        taRegExs.setColumns(20);
        taRegExs.setRows(5);
        jScrollPane2.setViewportView(taRegExs);

        jpRegExps.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jcbDoSkip.setSelected(true);
        jcbDoSkip.setText("Filter lines");
        jcbDoSkip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jcbDoSkipActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
                jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel7Layout.createSequentialGroup()
                                .addGap(22, 22, 22)
                                .addComponent(jpFileTypes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jpRegExps, javax.swing.GroupLayout.DEFAULT_SIZE, 1355, Short.MAX_VALUE)
                                .addContainerGap())
                        .addGroup(jPanel7Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jcbDoSkip)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
                jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                                .addComponent(jcbDoSkip)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jpRegExps, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(jPanel7Layout.createSequentialGroup()
                                                .addComponent(jpFileTypes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 232, Short.MAX_VALUE)))
                                .addContainerGap())
        );

        jPanel8.add(jPanel7);

        tfFileNameExcel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfFileNameExcelActionPerformed(evt);
            }
        });

        jLabel7.setText("excel file name");

        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(
                jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel23Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(tfFileNameExcel, javax.swing.GroupLayout.PREFERRED_SIZE, 432, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        jPanel23Layout.setVerticalGroup(
                jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel23Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(tfFileNameExcel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel7))
                                .addContainerGap())
        );

        jPanel8.add(jPanel23);

        jLabel8.setText("Warn if file size is more than");

        jtfFileSize.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(java.text.NumberFormat.getIntegerInstance())));

        jLabel9.setText("MB");

        javax.swing.GroupLayout jPanel24Layout = new javax.swing.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(
                jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel24Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel8)
                                .addGap(2, 2, 2)
                                .addComponent(jtfFileSize, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel9)
                                .addContainerGap())
        );
        jPanel24Layout.setVerticalGroup(
                jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel24Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel8)
                                        .addComponent(jLabel9)
                                        .addComponent(jtfFileSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );

        jPanel8.add(jPanel24);

        jPanel4.add(jPanel8);

        jPanel10.add(jPanel4);

        jTabbedPane2.addTab("General", jPanel10);

        jcbPrintLogFileName.setSelected(true);
        jcbPrintLogFileName.setText("Print log file name");

        jcbignoreFormatting.setText("Ignore formatting; print field:value|field:value");

        jcbShowFullDate.setText("show full date in timestamp");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
                jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel11Layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jcbShowFullDate)
                                        .addComponent(jcbignoreFormatting)
                                        .addComponent(jcbPrintLogFileName))
                                .addContainerGap(1348, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
                jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel11Layout.createSequentialGroup()
                                .addGap(19, 19, 19)
                                .addComponent(jcbPrintLogFileName)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbignoreFormatting)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbShowFullDate)
                                .addContainerGap(655, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Formated", jPanel11);

        jpOutput.add(jTabbedPane2);

        jTabbedPane1.addTab("Output", jpOutput);

        jcbLimitQueryResults.setSelected(true);
        jcbLimitQueryResults.setText("Limit results in a query");

        jLabel4.setText("max results in a query");

        jsMaxQueryLines.setModel(new javax.swing.SpinnerNumberModel(2000, 0, null, 1));

        jcbIncludeOrderBy.setSelected(true);
        jcbIncludeOrderBy.setText("Include \"ORDER BY\" in requests");

        jcbAccessFiles.setSelected(true);
        jcbAccessFiles.setText("access log files while printing (uncheck for huge amount of logs)");

        jcbSort.setSelected(true);
        jcbSort.setText("sort results (uncheck for huge amount of logs)");

        jcbNewTLibSearch.setSelected(true);
        jcbNewTLibSearch.setText("use new TLib search procedure");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
                jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel9Layout.createSequentialGroup()
                                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel9Layout.createSequentialGroup()
                                                .addGap(68, 68, 68)
                                                .addComponent(jLabel4)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(jsMaxQueryLines, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addGroup(jPanel9Layout.createSequentialGroup()
                                                .addGap(21, 21, 21)
                                                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(jcbIncludeOrderBy)
                                                        .addComponent(jcbLimitQueryResults)
                                                        .addComponent(jcbAccessFiles)
                                                        .addComponent(jcbSort)
                                                        .addComponent(jcbNewTLibSearch))))
                                .addContainerGap(1263, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
                jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel9Layout.createSequentialGroup()
                                .addGap(19, 19, 19)
                                .addComponent(jcbLimitQueryResults)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jsMaxQueryLines, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel4))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jcbIncludeOrderBy)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbAccessFiles)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jcbSort)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jcbNewTLibSearch)
                                .addContainerGap(604, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Query", jPanel9);

        jLabel6.setText("regex for current path to show in dialog title");

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
                jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel12Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jtfTitleRegex, javax.swing.GroupLayout.PREFERRED_SIZE, 489, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(906, Short.MAX_VALUE))
        );
        jPanel12Layout.setVerticalGroup(
                jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel12Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel6)
                                        .addComponent(jtfTitleRegex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(737, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("GUI", jPanel12);

        jpConstants.setLayout(new java.awt.BorderLayout());

        jLabel10.setText("Constant name");

        jPanel17.setLayout(new java.awt.BorderLayout());

        jlConstants.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(jlConstants);

        jPanel17.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jbConstantAdd.setText("Add");
        jbConstantAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbConstantAddActionPerformed(evt);
            }
        });

        jbConstantRemove.setText("Remove");
        jbConstantRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbConstantRemoveActionPerformed(evt);
            }
        });

        jbConstantRename.setText("Rename");
        jbConstantRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbConstantRenameActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
                jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel18Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jbConstantAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(10, 10, 10)
                                .addComponent(jbConstantRemove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbConstantRename, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(18, 18, 18))
        );
        jPanel18Layout.setVerticalGroup(
                jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel18Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jbConstantAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jbConstantRemove, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jbConstantRename, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
                jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel15Layout.createSequentialGroup()
                                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(jPanel15Layout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(jLabel10)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel15Layout.createSequentialGroup()
                                                .addGap(0, 44, Short.MAX_VALUE)
                                                .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        jPanel15Layout.setVerticalGroup(
                jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel15Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, 258, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        jPanel20.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPane1.setAutoscrolls(true);

        jtConstants.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{

                },
                new String[]{

                }
        ));
        jtConstants.setCellSelectionEnabled(true);
        jScrollPane1.setViewportView(jtConstants);

        jPanel20.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jbTheConstantRemove.setText("Remove");
        jbTheConstantRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbTheConstantRemoveActionPerformed(evt);
            }
        });

        jbTheConstantRename.setText("Rename");

        jbTheConstantPaste.setText("Add from clipboard");
        jbTheConstantPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbTheConstantPasteActionPerformed(evt);
            }
        });

        jbTheConstantAdd.setText("Add");
        jbTheConstantAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbTheConstantAddActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
                jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel19Layout.createSequentialGroup()
                                .addContainerGap(98, Short.MAX_VALUE)
                                .addComponent(jbTheConstantAdd)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jbTheConstantRemove)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jbTheConstantRename)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jbTheConstantPaste)
                                .addContainerGap())
        );
        jPanel19Layout.setVerticalGroup(
                jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel19Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(jbTheConstantRemove)
                                        .addComponent(jbTheConstantRename)
                                        .addComponent(jbTheConstantPaste)
                                        .addComponent(jbTheConstantAdd))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
                jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel16Layout.createSequentialGroup()
                                .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, 450, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel16Layout.setVerticalGroup(
                jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel16Layout.createSequentialGroup()
                                .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, 644, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
                jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel14Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, 454, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel14Layout.setVerticalGroup(
                jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel14Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(24, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
                jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel13Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(834, Short.MAX_VALUE))
        );
        jPanel13Layout.setVerticalGroup(
                jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel13Layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        jpConstants.add(jPanel13, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Constants", jpConstants);

        jpGenesysMessages.setLayout(new javax.swing.BoxLayout(jpGenesysMessages, javax.swing.BoxLayout.LINE_AXIS));

        jLabel11.setText("Application type");

        javax.swing.GroupLayout pAppTypeLayout = new javax.swing.GroupLayout(pAppType);
        pAppType.setLayout(pAppTypeLayout);
        pAppTypeLayout.setHorizontalGroup(
                pAppTypeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 0, Short.MAX_VALUE)
        );
        pAppTypeLayout.setVerticalGroup(
                pAppTypeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 487, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(
                jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel21Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel21Layout.createSequentialGroup()
                                                .addComponent(jLabel11)
                                                .addGap(0, 599, Short.MAX_VALUE))
                                        .addComponent(pAppType, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        jPanel21Layout.setVerticalGroup(
                jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel21Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel11)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pAppType, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(250, Short.MAX_VALUE))
        );

        jpGenesysMessages.add(jPanel21);

        javax.swing.GroupLayout jpLogMessagesLayout = new javax.swing.GroupLayout(jpLogMessages);
        jpLogMessages.setLayout(jpLogMessagesLayout);
        jpLogMessagesLayout.setHorizontalGroup(
                jpLogMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 924, Short.MAX_VALUE)
        );
        jpLogMessagesLayout.setVerticalGroup(
                jpLogMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 768, Short.MAX_VALUE)
        );

        jpGenesysMessages.add(jpLogMessages);

        jTabbedPane1.addTab("Log messages", jpGenesysMessages);

        jPanel2.add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel2);

        btOK.setText("OK");
        btOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btOKActionPerformed(evt);
            }
        });

        btCancel.setText("Cancel");
        btCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btOK, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btCancel)
                                .addGap(88, 88, 88))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(btCancel)
                                        .addComponent(btOK))
                                .addContainerGap())
        );

        getContentPane().add(jPanel1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btOKActionPerformed
        try {
            cr.setFileNameLong(tfFileNameLong.getText());
            cr.setFileNameExcel(tfFileNameExcel.getText());
            cr.setFileNameShort(tfFileNameShort.getText());
//            cr.setSaveFileShort(jcbSaveShortFileOutput.isSelected());
            cr.setApplyFullFilter(jcbDoSkip.isSelected());
            cr.setLimitQueryResults(jcbLimitQueryResults.isSelected());
            cr.setAddOrderBy(jcbIncludeOrderBy.isSelected());
            cr.setAccessLogFiles(jcbAccessFiles.isSelected());
            cr.setSorting(jcbSort.isSelected());
            cr.setFullTimeStamp(jcbShowFullDate.isSelected());
            cr.setNewTlibSearch(jcbNewTLibSearch.isSelected());

            int parseInt;
            try {
                parseInt = Integer.parseInt(jtfFileSize.getText());
                if (parseInt <= 0) {
                    parseInt = 0;
                }
            } catch (NumberFormatException e) {
                parseInt = 0;
            }
            cr.setFileSizeWarn(parseInt);

            cr.setPrintLogFileName(jcbPrintLogFileName.isSelected());
            cr.setIgnoreFormatting(jcbignoreFormatting.isSelected());

            String rx = jtfTitleRegex.getText();
            try {
                Pattern p = Pattern.compile(rx);
            } catch (PatternSyntaxException e) {
                inquirer.ExceptionHandler.handleException("Error compiling regex [" + rx + "]: " + e.getMessage(), e);
            }
            cr.setTitleRegex(rx);

            try {
                jsMaxRecords.commitEdit();
                cr.SetMaxRecords((Integer) jsMaxRecords.getValue());
            } catch (java.text.ParseException e) {
                cr.SetMaxRecords(1000);
            }

            try {
                jsMaxQueryLines.commitEdit();
                cr.setMaxQueryLines((Integer) jsMaxQueryLines.getValue());
            } catch (java.text.ParseException e) {
                cr.setMaxQueryLines(3000);
            }

            Object selectedValue = lPrintFilters.getSelectedValue();
            if (selectedValue != null) {
                printFilters.put((String) selectedValue, taRegExs.getText());
            }
            cr.setPrintFilters(printFilters);

            InquirerCfg.GenesysConstants constants = new InquirerCfg.GenesysConstants();

            DefaultListModel model = (DefaultListModel) jlConstants.getModel();
            for (int i = 0; i < model.size(); i++) {
                constants.add((InquirerCfg.GenesysConstant) model.get(i));

            }
            cr.setConstants(constants);
            commitAppTypeChange(TableType.UNKNOWN);
            cr.setLogMessages(savedLogMessages);

            commitRefTypeChange(ReferenceType.UNKNOWN);
            cr.setReferences(savedRefs);

            inquirer.saveCR();
            closeCause = 1;
            this.dispose();        // TODO add your handling code here:
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving settings: " + e.getMessage(), "Cannot save settings", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btOKActionPerformed

    private void btCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btCancelActionPerformed
        closeCause = 0;
        this.dispose();        // TODO add your handling code here:
        // TODO add your handling code here:
    }//GEN-LAST:event_btCancelActionPerformed

    public int getCloseCause() {
        return closeCause;
    }

    private void tfFileNameShortActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfFileNameShortActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tfFileNameShortActionPerformed

    private void tfFileNameLongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfFileNameLongActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tfFileNameLongActionPerformed

    private void jcbDoSkipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jcbDoSkipActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jcbDoSkipActionPerformed

    private void tfFileNameExcelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tfFileNameExcelActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_tfFileNameExcelActionPerformed

    private void jbConstantAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbConstantAddActionPerformed
        // TODO add your handling code here:
        String showInputDialog = JOptionPane.showInputDialog(this, "Enter new constant alias", "");
        if (showInputDialog != null) {
            DefaultListModel model = (DefaultListModel) jlConstants.getModel();
            model.addElement(new InquirerCfg.GenesysConstant(showInputDialog));
        }

    }//GEN-LAST:event_jbConstantAddActionPerformed

    private void jbTheConstantAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbTheConstantAddActionPerformed
        // TODO add your handling code here:
        ConstantsModel mod = (ConstantsModel) jtConstants.getModel();
        mod.setValueAt("dd", mod.getRowCount() + 1, 0);
        mod.fireTableDataChanged();
    }//GEN-LAST:event_jbTheConstantAddActionPerformed

    private void jbTheConstantPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbTheConstantPasteActionPerformed
        // TODO add your handling code here:

        try {
            String cb;
            cb = SystemClipboard.get();
            if (cb != null && !cb.isEmpty()) {
                ConstantsModel mod = (ConstantsModel) jtConstants.getModel();
//                String[] split = cb.split("[\n,]");
                int id = 0;
                for (String string : cb.split("[\n,]")) {
                    if (string != null && !string.isEmpty()) {
                        String s = string.trim();
                        if (s != null && !s.isEmpty()) {
                            mod.addRow(s, id++);
                        }

                    }
                }
                mod.fireTableDataChanged();
            }
        } catch (Exception ex) {
            logger.error("fatal: ", ex);
        }

    }//GEN-LAST:event_jbTheConstantPasteActionPerformed

    private void jbTheConstantRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbTheConstantRemoveActionPerformed
        int selectedRow = jtConstants.getSelectedRow();
        if (selectedRow >= 0) {
            ConstantsModel mod = (ConstantsModel) jtConstants.getModel();
            mod.deleteRow(selectedRow);
            mod.fireTableDataChanged();
        }
// TODO add your handling code here:
    }//GEN-LAST:event_jbTheConstantRemoveActionPerformed

    private void jbConstantRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbConstantRenameActionPerformed

        Object selectedValue = jlConstants.getSelectedValue();
        int selectedIndex = jlConstants.getSelectedIndex();
        if (selectedValue != null) {
            String showInputDialog = JOptionPane.showInputDialog(this, "Change constant alias", selectedValue.toString());
            if (showInputDialog != null) {
                jlConstants.setValueIsAdjusting(true);
                DefaultListModel model = (DefaultListModel) jlConstants.getModel();
                GenesysConstant c = (InquirerCfg.GenesysConstant) model.getElementAt(selectedIndex);

                c.setName(showInputDialog);

                model.add(selectedIndex, c);
                model.removeElementAt(selectedIndex);
                jlConstants.setValueIsAdjusting(false);
            }
        }
        // TODO add your handling code here:
    }//GEN-LAST:event_jbConstantRenameActionPerformed

    private void jbConstantRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbConstantRemoveActionPerformed
        Object selectedValue = jlConstants.getSelectedValue();
        int selectedIndex = jlConstants.getSelectedIndex();
        if (selectedValue != null) {
            int ret = inquirer.showConfirmDialog(this, "Really want to delete [" + selectedValue + "]?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                DefaultListModel model = (DefaultListModel) jlConstants.getModel();
                model.remove(selectedIndex);

            }
        }

// TODO add your handling code here:
    }//GEN-LAST:event_jbConstantRemoveActionPerformed
    // End of variables declaration//GEN-END:variables

    private static class ConstantsModel extends AbstractTableModel {

        private final Class[] types;
        private final String[] columnNames;
        InquirerCfg.GenesysConstant theConstant;

        public ConstantsModel() {
            this.theConstant = new GenesysConstant();

            columnNames
                    = new String[]{
                    "String value", "Constant ID"
            };

            types = new Class[]{
                    java.lang.String.class, java.lang.Integer.class
            };

        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return types[columnIndex]; //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column]; //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            theConstant.setValueAt(aValue, rowIndex, columnIndex); //To change body of generated methods, choose Tools | Templates.
        }

        public InquirerCfg.GenesysConstant getTheConstant() {
            return theConstant;
        }

        public void setTheConstant(InquirerCfg.GenesysConstant theConstant) {
            this.theConstant = theConstant;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return true; //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int getRowCount() {
            return theConstant.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
//            if (columnIndex == 1) {
//                return theConstant.getKey(rowIndex);
//
//            } else if (columnIndex == 0) {
//                return theConstant.getConstant(rowIndex);
//
//            }
            Object ret = null;
            if (columnIndex == 1) {
                ret = theConstant.getKey(rowIndex);

            } else if (columnIndex == 0) {
                ret = theConstant.getConstant(rowIndex);

            }
            return ret;
        }

        private void addRow(String string, int i) {
            theConstant.addRow(string, i);

        }

        private void deleteRow(int selectedRow) {
            theConstant.deleteRow(selectedRow);

        }
    }

    private class jlConstantsSelectionListener implements ListSelectionListener {

        private JTable jtConstants;

        public jlConstantsSelectionListener() {
        }

        private jlConstantsSelectionListener(JTable jtConstants) {
            this.jtConstants = jtConstants;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            JList jl = (JList) e.getSource();
            InquirerCfg.GenesysConstant selectedValue = (InquirerCfg.GenesysConstant) jl.getSelectedValue();
            if (selectedValue != null) {
                ConstantsModel mod = (ConstantsModel) jtConstants.getModel();
                mod.setTheConstant(selectedValue);
//                tca.adjustColumns();
                mod.fireTableDataChanged();
//                SwingUtilities.getWindowAncestor(jtConstants).pack();
            }
        }
    }
}
