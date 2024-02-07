/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.ScreenInfo;
import com.myutils.logbrowser.common.JSRunner;
import com.myutils.logbrowser.common.RecordPrintout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;

/**
 * @author ssydoruk
 */
public class ShowFullMessage extends javax.swing.JFrame {

    RSyntaxTextArea detailedMessage;
    RSyntaxTextArea jtaMessageText;
    private ReportFrameQuery parentForm;
    private ToggleButtonToolBar toolbar = null;
    DefaultTableModel infoTableModel;
    private javax.swing.JTable jtAllFields;
    private JScrollPane jspAllFields;
    private final Utils.swing.TableColumnAdjuster tca;
    private TabResultDataModel.TableRow row;
    private String recordDisplayScript;
    JToggleButton btLineWrap;
    JToggleButton btAllFields;
    JCheckBoxMenuItem miWrap;

    /**
     * Creates new form ShowFullMessage
     */
    private ShowFullMessage() {
        initComponents();

        jtAllFields = new javax.swing.JTable();
        jspAllFields = new JScrollPane();
        jspAllFields.setViewportView(jtAllFields);
        infoTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // To change body of generated methods, choose Tools | Templates.
            }

        };
        for (String string : new String[]{"Field", "Value"}) {
            infoTableModel.addColumn(string);
        }
        jtAllFields.setModel(infoTableModel);
        jtAllFields.getColumnModel().getColumn(0).setPreferredWidth(10);

        tca = new Utils.swing.TableColumnAdjuster(jtAllFields);
        tca.setColumnDataIncluded(true);
        tca.setColumnHeaderIncluded(false);
        tca.setDynamicAdjustment(true);

//        tca.setMaxColumnWidth(getFontMetrics(getFont()).stringWidth(getSampleString(MAX_CHARS_IN_TABLE)));

        jtaMessageText = new RSyntaxTextArea();
        jtaMessageText.setCodeFoldingEnabled(false);
        jtaMessageText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_RUBY);
        jtaMessageText.setWrapStyleWord(true);
        jtaMessageText.setLineWrap(true);
        jtaMessageText.setEditable(false);
        jtaMessageText.setTabSize(2);
        ((DefaultCaret) jtaMessageText.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        jtaMessageText.getPopupMenu().add(new JSeparator());

         miWrap = new JCheckBoxMenuItem(new AbstractAction("Line wrap") {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleLineWrap(((JCheckBoxMenuItem) e.getSource()).isSelected());
            }

        });
        miWrap.setSelected(jtaMessageText.getLineWrap());
        jtaMessageText.getPopupMenu().add(miWrap);

        pFullMessage.add(new RTextScrollPane(jtaMessageText));

        detailedMessage = new RSyntaxTextArea();
        detailedMessage.setCodeFoldingEnabled(true);
        detailedMessage.setWrapStyleWord(true);
        detailedMessage.setLineWrap(true);
        detailedMessage.setEditable(false);

        pDetailedMessage.add(new RTextScrollPane(detailedMessage));
        toolbar = new ToggleButtonToolBar();
        JPanel pToolbarPanel = new JPanel(new BorderLayout());
//
        pToolbarPanel.add(toolbar, BorderLayout.PAGE_START);
        pToolbar.add(toolbar, BorderLayout.PAGE_START);

//        toolbar.addButton("Excel", "Open the report in Excel", (new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                
//            }
//        }));
        btLineWrap = new JToggleButton("Line wrap", jtaMessageText.getLineWrap());
        btLineWrap.setToolTipText("Toggle line wrap on/off");
        btLineWrap.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleLineWrap(((JToggleButton) e.getSource()).isSelected());
            }
        });
        toolbar.addButton(btLineWrap);

        pAllFields.setVisible(false);
        btAllFields = new JToggleButton("All fields", pAllFields.isVisible());
        btAllFields.setToolTipText("Toggle panel with all fields on/off");
        btAllFields.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = ((JToggleButton) e.getSource()).isSelected();
                pAllFields.setVisible(selected);

                if (selected) {
                    pAllFields.add(jspAllFields, java.awt.BorderLayout.CENTER);
                    spSplitPane.setRightComponent(pAllFields);
                    tca.adjustColumns();
                    showAllFields();

//                    jtAllFields.setPreferredSize( new Dimension( jtAllFields.getSize().width, 200));
//                    pAllFields.setSize((int) pAllFields.getSize().getWidth(), 200);
                } else {
                    pAllFields.remove(jspAllFields);
                }
                spSplitPane.revalidate();
            }
        });
        toolbar.addButton(btAllFields);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                inquirer.logger.debug("windowStateChanged");
                super.windowStateChanged(e);
            }

            @Override
            public void windowActivated(WindowEvent e) {
                inquirer.logger.debug("windowActivated old: " + e.getOldState() + " new:" + e.getNewState());
                doShowMessage();
                super.windowActivated(e);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                inquirer.logger.debug("windowClosing");
//                if (fullMsg != null) {
//                    fullMsg.dispose();
//                    fullMsg = null;
//                }
                parentForm.fullMsgClosed();
                super.windowClosing(e); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                inquirer.logger.debug("windowGainedFocus");
//                if (fullMsg != null) {
//                    fullMsg.setVisible(true);
//                }
                super.windowGainedFocus(e); //To change body of generated methods, choose Tools | Templates.
            }

        });

        pack();

    }

    private void toggleLineWrap(boolean selected) {
        jtaMessageText.setLineWrap(selected);
        miWrap.setSelected(selected);
        btLineWrap.setSelected(selected);
        jtaMessageText.repaint();
    }

    ShowFullMessage(ReportFrameQuery aThis) {
        this();
        parentForm = aThis;
        ScreenInfo.CenterWindowTopMaxWidth(this);


    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        pToolbar = new javax.swing.JPanel();
        spSplitPane = new javax.swing.JSplitPane();
        pFullMessage = new javax.swing.JPanel();
        pDetailedMessage = new javax.swing.JPanel();
        pAllFields = new javax.swing.JPanel();

        setAutoRequestFocus(false);
        addWindowStateListener(new java.awt.event.WindowStateListener() {
            public void windowStateChanged(java.awt.event.WindowEvent evt) {
                formWindowStateChanged(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }

            public void windowDeactivated(java.awt.event.WindowEvent evt) {
                formWindowDeactivated(evt);
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));

        jPanel1.setLayout(new java.awt.BorderLayout());

        pToolbar.setLayout(new java.awt.BorderLayout());
        jPanel1.add(pToolbar, java.awt.BorderLayout.PAGE_START);

        spSplitPane.setDividerSize(2);
        spSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spSplitPane.setResizeWeight(0.95);

        pFullMessage.setLayout(new java.awt.BorderLayout());
        spSplitPane.setLeftComponent(pFullMessage);

        pDetailedMessage.setLayout(new java.awt.BorderLayout());
        spSplitPane.setRightComponent(pDetailedMessage);

        pAllFields.setLayout(new java.awt.BorderLayout());
        spSplitPane.setRightComponent(pAllFields);

        jPanel1.add(spSplitPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        parentForm.fullMsgClosed();
        // TODO add your handling code here:
    }//GEN-LAST:event_formWindowClosing

    private void formWindowDeactivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowDeactivated
        // TODO add your handling code here:
    }//GEN-LAST:event_formWindowDeactivated

    private void formWindowStateChanged(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_formWindowStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel pAllFields;
    private javax.swing.JPanel pDetailedMessage;
    private javax.swing.JPanel pFullMessage;
    private javax.swing.JPanel pToolbar;
    private javax.swing.JSplitPane spSplitPane;
    // End of variables declaration//GEN-END:variables

    void showMessage(String recordDisplayScript, TabResultDataModel.TableRow row) {
        this.row = row;
        this.recordDisplayScript = recordDisplayScript;
        doShowMessage();
    }

    public void doShowMessage() {
        if (row != null) {
            showAllFields();

            detailedMessage.setText("");
            if (StringUtils.isEmpty(recordDisplayScript)) {
                jtaMessageText.setText(row.getRecord().getBytes());
            } else {
                RecordPrintout recPrintout = JSRunner.evalFullRecordPrintout(recordDisplayScript, row.getRecord());
                jtaMessageText.setText(recPrintout.fullMessage);
                if (StringUtils.isNotEmpty(recPrintout.detailsMessage)) {
                    detailedMessage.setText(recPrintout.detailsMessage);
                    detailedMessage.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);

                }
            }
        } else {
            jtaMessageText.setText("");
            showAllFields();
        }
    }

    private void showAllFields() {
        infoTableModel.setRowCount(0);
        if (row != null && jtAllFields.isVisible()) {
            HashMap<Integer, Object> rowData = row.getRowData();
            ArrayList<String[]> kvps = new ArrayList<>();
            for (Map.Entry<Integer, Object> entry : rowData.entrySet()) {
                kvps.add(new String[]{String.format("%03d", entry.getKey()), entry.getValue().toString()});
            }
            for (Map.Entry<Object, Object> entry : row.getRecord().m_fieldsAll.entrySet()) {
                kvps.add(new String[]{entry.getKey().toString() + " (raw)", entry.getValue().toString()});
            }
            Collections.sort(kvps, (o1, o2) -> {
                return ((String[]) o1)[0].compareToIgnoreCase(((String[]) o2)[0]); //To change body of generated lambdas, choose Tools | Templates.
            });

            for (String[] entry : kvps) {
                infoTableModel.addRow(entry);
            }
        }
    }
}
