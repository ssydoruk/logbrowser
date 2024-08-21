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
@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation"})
public final class QuerySetting extends javax.swing.JDialog {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private static final long serialVersionUID = 1L;
    private final JList<String> lPrintFilters;
    Object curSelected = null;
    TableType lastTableType;
    private JList<ReferenceType> lReferenceType;
    /**
     * Creates new form QuerySetting
     */
    private final InquirerCfg cr;
    private CheckBoxListSettings clbRefValues;
    private DefaultListModel<OptionNode> lmRefValues;
    private int closeCause;
    private final HashMap<String, String> printFilters;
    private CheckBoxListSettings clbLogMessages;
    private HashMap<ReferenceType, ArrayList<OptionNode>> savedRefs;
    private ReferenceType lastRefType;
    private HashMap<TableType, ArrayList<OptionNode>> savedLogMessages;
    private JList<TableType> lAppType;
    // Variables declaration - do not modify                     
    private javax.swing.JButton btCancel;
    private javax.swing.JButton btOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
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
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel31;
    private javax.swing.JPanel jPanel32;
    private javax.swing.JPanel jPanel34;
    private javax.swing.JPanel jPanel35;
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
    private javax.swing.JList<InquirerCfg.GenesysConstant> jlConstants;
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
    private javax.swing.JTextField jtfLinuxEditor;
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
        jtfFileSize.setText(Integer.toString(cr.getFileSizeWarn()));
        jsMaxRecords.getModel().setValue(cr.getMaxRecords());
        retrieveLogMessages(cr.retrieveLogMessages());
        retrieveRefs(cr.retrieveRefs());

        /**
         * ********************************************
         */
        DefaultListModel<String> lmFilters = new DefaultListModel<>();
        lPrintFilters = new JList<String>(lmFilters);
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
        jtfLinuxEditor.setText(cr.getLinuxEditor());
        jcbNewTLibSearch.setSelected(cr.isNewTLibSearch());

        jcbShowFullDate.setSelected(cr.isFullTimeStamp());

        DefaultListModel<InquirerCfg.GenesysConstant> model = new DefaultListModel<InquirerCfg.GenesysConstant>();
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
        lmRefValues = new DefaultListModel<>();
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

        DefaultListModel<ReferenceType> lm = new DefaultListModel<>();
        pRefTypes.setLayout(new BorderLayout());
        lReferenceType = new JList<>(lm);
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

        DefaultListModel<TableType> lm = new DefaultListModel<>();
        pAppType.setLayout(new BorderLayout());
        lAppType = new JList<>(lm);
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

        DefaultListModel<OptionNode> model = (DefaultListModel<OptionNode>) clbLogMessages.getModel();
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
        jPanel27 = new javax.swing.JPanel();
        jPanel26 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jsMaxQueryLines = new javax.swing.JSpinner();
        jcbLimitQueryResults = new javax.swing.JCheckBox();
        jcbIncludeOrderBy = new javax.swing.JCheckBox();
        jcbAccessFiles = new javax.swing.JCheckBox();
        jcbSort = new javax.swing.JCheckBox();
        jcbNewTLibSearch = new javax.swing.JCheckBox();
        jPanel12 = new javax.swing.JPanel();
        jPanel35 = new javax.swing.JPanel();
        jPanel34 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jtfLinuxEditor = new javax.swing.JTextField();
        jPanel28 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jtfTitleRegex = new javax.swing.JTextField();
        jpConstants = new javax.swing.JPanel();
        jPanel31 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jPanel30 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jPanel17 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jlConstants = new javax.swing.JList<>();
        jPanel18 = new javax.swing.JPanel();
        jbConstantAdd = new javax.swing.JButton();
        jbConstantRemove = new javax.swing.JButton();
        jbConstantRename = new javax.swing.JButton();
        jPanel29 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jPanel20 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jtConstants = new javax.swing.JTable();
        jPanel19 = new javax.swing.JPanel();
        jbTheConstantRemove = new javax.swing.JButton();
        jbTheConstantRename = new javax.swing.JButton();
        jbTheConstantPaste = new javax.swing.JButton();
        jbTheConstantAdd = new javax.swing.JButton();
        jPanel32 = new javax.swing.JPanel();
        jpGenesysMessages = new javax.swing.JPanel();
        jPanel21 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        pAppType = new javax.swing.JPanel();
        jpLogMessages = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel25 = new javax.swing.JPanel();
        btOK = new javax.swing.JButton();
        btCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));

        jPanel2.setLayout(new java.awt.BorderLayout());

        jpReferences.setLayout(new javax.swing.BoxLayout(jpReferences, javax.swing.BoxLayout.LINE_AXIS));

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel1.setText("Reference type");
        jPanel5.add(jLabel1);

        javax.swing.GroupLayout pRefTypesLayout = new javax.swing.GroupLayout(pRefTypes);
        pRefTypes.setLayout(pRefTypesLayout);
        pRefTypesLayout.setHorizontalGroup(
                pRefTypesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 183, Short.MAX_VALUE)
        );
        pRefTypesLayout.setVerticalGroup(
                pRefTypesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 491, Short.MAX_VALUE)
        );

        jPanel5.add(pRefTypes);

        jpReferences.add(jPanel5);

        jpAllRefs.setLayout(new java.awt.BorderLayout());
        jpReferences.add(jpAllRefs);

        jTabbedPane1.addTab("References", jpReferences);

        jpOutput.setLayout(new javax.swing.BoxLayout(jpOutput, javax.swing.BoxLayout.LINE_AXIS));

        jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Screen output"));
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        jLabel2.setText("Max records number");
        jPanel3.add(jLabel2);

        jsMaxRecords.setModel(new javax.swing.SpinnerNumberModel(2000, 0, null, 1));
        jPanel3.add(jsMaxRecords);

        jPanel10.add(jPanel3);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("File output"));
        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Brief output"));
        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.LINE_AXIS));

        jLabel3.setText("file name");
        jPanel6.add(jLabel3);

        tfFileNameShort.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tfFileNameShortActionPerformed(evt);
            }
        });
        jPanel6.add(tfFileNameShort);

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
                                .addComponent(jpRegExps, javax.swing.GroupLayout.DEFAULT_SIZE, 793, Short.MAX_VALUE)
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
                                                .addGap(0, 0, Short.MAX_VALUE)))
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
                                .addContainerGap(711, Short.MAX_VALUE))
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
                                .addContainerGap(361, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Formated", jPanel11);

        jpOutput.add(jTabbedPane2);

        jTabbedPane1.addTab("Output", jpOutput);

        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.LINE_AXIS));

        jPanel27.setLayout(new javax.swing.BoxLayout(jPanel27, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel26.setLayout(new javax.swing.BoxLayout(jPanel26, javax.swing.BoxLayout.LINE_AXIS));

        jLabel4.setText("max results in a query");
        jPanel26.add(jLabel4);

        jsMaxQueryLines.setModel(new javax.swing.SpinnerNumberModel(2000, 0, null, 1));
        jPanel26.add(jsMaxQueryLines);

        jPanel27.add(jPanel26);

        jcbLimitQueryResults.setSelected(true);
        jcbLimitQueryResults.setText("Limit results in a query");
        jPanel27.add(jcbLimitQueryResults);

        jcbIncludeOrderBy.setSelected(true);
        jcbIncludeOrderBy.setText("Include \"ORDER BY\" in requests");
        jPanel27.add(jcbIncludeOrderBy);

        jcbAccessFiles.setSelected(true);
        jcbAccessFiles.setText("access log files while printing (uncheck for huge amount of logs)");
        jPanel27.add(jcbAccessFiles);

        jcbSort.setSelected(true);
        jcbSort.setText("sort results (uncheck for huge amount of logs)");
        jPanel27.add(jcbSort);

        jcbNewTLibSearch.setSelected(true);
        jcbNewTLibSearch.setText("use new TLib search procedure");
        jPanel27.add(jcbNewTLibSearch);

        jPanel9.add(jPanel27);

        jTabbedPane1.addTab("Query", jPanel9);

        jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel34.setLayout(new java.awt.GridLayout(1, 1));

        jLabel12.setText("Linux editor path and params");
        jPanel34.add(jLabel12);
        jPanel34.add(jtfLinuxEditor);

        jPanel35.add(jPanel34);

        jPanel28.setLayout(new java.awt.GridLayout(1, 1));

        jLabel6.setText("regex for current path to show in dialog title");
        jPanel28.add(jLabel6);
        jPanel28.add(jtfTitleRegex);

        jPanel35.add(jPanel28);

        jPanel12.add(jPanel35);

        jTabbedPane1.addTab("GUI", jPanel12);

        jpConstants.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout jPanel31Layout = new javax.swing.GroupLayout(jPanel31);
        jPanel31.setLayout(jPanel31Layout);
        jPanel31Layout.setHorizontalGroup(
                jPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 1073, Short.MAX_VALUE)
        );
        jPanel31Layout.setVerticalGroup(
                jPanel31Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 100, Short.MAX_VALUE)
        );

        jpConstants.add(jPanel31, java.awt.BorderLayout.PAGE_START);

        jPanel13.setLayout(new javax.swing.BoxLayout(jPanel13, javax.swing.BoxLayout.LINE_AXIS));

        jPanel14.setLayout(new javax.swing.BoxLayout(jPanel14, javax.swing.BoxLayout.LINE_AXIS));

        jPanel30.setLayout(new javax.swing.BoxLayout(jPanel30, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel15.setLayout(new javax.swing.BoxLayout(jPanel15, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel10.setText("Constant name");
        jPanel15.add(jLabel10);

        jPanel17.setLayout(new java.awt.BorderLayout());

        jlConstants.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(jlConstants);

        jPanel17.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel15.add(jPanel17);

        jPanel18.setLayout(new javax.swing.BoxLayout(jPanel18, javax.swing.BoxLayout.LINE_AXIS));

        jbConstantAdd.setText("Add");
        jbConstantAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbConstantAddActionPerformed(evt);
            }
        });
        jPanel18.add(jbConstantAdd);

        jbConstantRemove.setText("Remove");
        jbConstantRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbConstantRemoveActionPerformed(evt);
            }
        });
        jPanel18.add(jbConstantRemove);

        jbConstantRename.setText("Rename");
        jbConstantRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbConstantRenameActionPerformed(evt);
            }
        });
        jPanel18.add(jbConstantRename);

        jPanel15.add(jPanel18);

        jPanel30.add(jPanel15);

        javax.swing.GroupLayout jPanel29Layout = new javax.swing.GroupLayout(jPanel29);
        jPanel29.setLayout(jPanel29Layout);
        jPanel29Layout.setHorizontalGroup(
                jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 578, Short.MAX_VALUE)
        );
        jPanel29Layout.setVerticalGroup(
                jPanel29Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 177, Short.MAX_VALUE)
        );

        jPanel30.add(jPanel29);

        jPanel14.add(jPanel30);

        jPanel16.setLayout(new javax.swing.BoxLayout(jPanel16, javax.swing.BoxLayout.PAGE_AXIS));

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

        jPanel16.add(jPanel20);

        jPanel19.setLayout(new javax.swing.BoxLayout(jPanel19, javax.swing.BoxLayout.LINE_AXIS));

        jbTheConstantRemove.setText("Remove");
        jbTheConstantRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbTheConstantRemoveActionPerformed(evt);
            }
        });
        jPanel19.add(jbTheConstantRemove);

        jbTheConstantRename.setText("Rename");
        jPanel19.add(jbTheConstantRename);

        jbTheConstantPaste.setText("Add from clipboard");
        jbTheConstantPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbTheConstantPasteActionPerformed(evt);
            }
        });
        jPanel19.add(jbTheConstantPaste);

        jbTheConstantAdd.setText("Add");
        jbTheConstantAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbTheConstantAddActionPerformed(evt);
            }
        });
        jPanel19.add(jbTheConstantAdd);

        jPanel16.add(jPanel19);

        jPanel14.add(jPanel16);

        jPanel13.add(jPanel14);

        javax.swing.GroupLayout jPanel32Layout = new javax.swing.GroupLayout(jPanel32);
        jPanel32.setLayout(jPanel32Layout);
        jPanel32Layout.setHorizontalGroup(
                jPanel32Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 26, Short.MAX_VALUE)
        );
        jPanel32Layout.setVerticalGroup(
                jPanel32Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 407, Short.MAX_VALUE)
        );

        jPanel13.add(jPanel32);

        jpConstants.add(jPanel13, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab("Constants", jpConstants);

        jpGenesysMessages.setLayout(new javax.swing.BoxLayout(jpGenesysMessages, javax.swing.BoxLayout.LINE_AXIS));

        jPanel21.setLayout(new javax.swing.BoxLayout(jPanel21, javax.swing.BoxLayout.PAGE_AXIS));

        jLabel11.setText("Application type");
        jPanel21.add(jLabel11);

        pAppType.setLayout(new java.awt.BorderLayout());
        jPanel21.add(pAppType);

        jpGenesysMessages.add(jPanel21);

        jpLogMessages.setLayout(new java.awt.BorderLayout());
        jpGenesysMessages.add(jpLogMessages);

        jTabbedPane1.addTab("Log messages", jpGenesysMessages);

        jPanel2.add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel2);

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(
                jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 933, Short.MAX_VALUE)
        );
        jPanel25Layout.setVerticalGroup(
                jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 29, Short.MAX_VALUE)
        );

        jPanel1.add(jPanel25);

        btOK.setText("OK");
        btOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btOKActionPerformed(evt);
            }
        });
        jPanel1.add(btOK);

        btCancel.setText("Cancel");
        btCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btCancelActionPerformed(evt);
            }
        });
        jPanel1.add(btCancel);

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
            cr.setLinuxEditor(jtfLinuxEditor.getText());

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
                DefaultListModel<GenesysConstant> model = (DefaultListModel<GenesysConstant>) jlConstants.getModel();
                GenesysConstant c = model.getElementAt(selectedIndex);

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
    // End of variables declaration                   

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
