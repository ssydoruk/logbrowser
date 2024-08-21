/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.ScreenInfo;
import com.google.gson.*;
import com.myutils.logbrowser.common.JSRunner;
import com.myutils.logbrowser.common.RecordPrintout;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;

import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultCaret;

/**
 * @author ssydoruk
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation"})
public class ShowFullMessage extends javax.swing.JFrame implements PopupMenuListener {

    RSyntaxTextArea detailedMessage;
    RSyntaxTextArea jtaMessageText;
    private ReportFrameQuery parentForm;
    private ToggleButtonToolBar toolbar = null;
    DefaultTableModel infoTableModel;
    private javax.swing.JTable jtAllFields;
    private JScrollPane jspAllFields;
    private final Utils.swing.TableColumnAdjuster tca;
    private TabResultDataModel.TableRow row;

    private final HashMap<TabResultDataModel.TableRow, String> detailedData = new HashMap<>();
    private String recordDisplayScript;
    JToggleButton btLineWrap;
    JToggleButton btAllFields;
    JToggleButton btDetailedMessage;
    JCheckBoxMenuItem miWrap;
    JMenuItem miSplit5050;
    JMenu mSelection;

    JMenuItem miSelectionAsJson;
    static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * Creates new form ShowFullMessage
     */
    private ShowFullMessage() {
        initComponents();
        jtAllFields = new javax.swing.JTable();
        jspAllFields = new JScrollPane();
        jspAllFields.setViewportView(jtAllFields);
        pAllFields.add(jspAllFields, java.awt.BorderLayout.CENTER);

        infoTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0;
            }

        };
        for (String string : new String[]{"Field", "Value"}) {
            infoTableModel.addColumn(string);
        }
        jtAllFields.setModel(infoTableModel);
        jtAllFields.getColumnModel().getColumn(0).setPreferredWidth(10);
        jtAllFields.setColumnSelectionAllowed(true);

        tca = new Utils.swing.TableColumnAdjuster(jtAllFields);
        tca.setColumnDataIncluded(true);
        tca.setColumnHeaderIncluded(false);
        tca.setDynamicAdjustment(true);

        // tca.setMaxColumnWidth(getFontMetrics(getFont()).stringWidth(getSampleString(MAX_CHARS_IN_TABLE)));
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

        miSplit5050 = new JMenuItem(new AbstractAction("Split 50/50") {
            @Override
            public void actionPerformed(ActionEvent e) {
                split5050();
            }

        });
        jtaMessageText.getPopupMenu().add(miSplit5050);
        mSelection = new JMenu("Selection");
        jtaMessageText.getPopupMenu().add(mSelection);
        jtaMessageText.getPopupMenu().addPopupMenuListener(this);

        miSelectionAsJson = new JMenuItem(new AbstractAction("As JSON") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showJson();
            }

        });
        mSelection.add(miSelectionAsJson);

        miSelectionAsJson = new JMenuItem(new AbstractAction("URL decode") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String str = URLDecoder.decode(jtaMessageText.getSelectedText(), "UTF-8");

                    String decodedQuery = Arrays.stream(str.split("&"))
                            // .map(param -> param.split("=")[0] + "=" + decode(param.split("=")[1]))
                            .collect(Collectors.joining("&\n\t"));

                    detailedData.put(row, decodedQuery);
                    detailedMessage.setText(decodedQuery);
                    detailedMessage.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
                    detailedMessage.setCodeFoldingEnabled(false);

                } catch (JsonSyntaxException ex) {
                    detailedMessage.setText(">>SELECTION IS NOT JSON<<");
                } catch (UnsupportedEncodingException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

        });
        mSelection.add(miSelectionAsJson);

        pFullMessage.add(new RTextScrollPane(jtaMessageText));

        detailedMessage = new RSyntaxTextArea();
        detailedMessage.setCodeFoldingEnabled(true);
        detailedMessage.setWrapStyleWord(true);
        detailedMessage.setLineWrap(true);
        detailedMessage.setEditable(false);
        ((DefaultCaret) detailedMessage.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        pDetailedMessage.add(new RTextScrollPane(detailedMessage));
        toolbar = new ToggleButtonToolBar();
        JPanel pToolbarPanel = new JPanel(new BorderLayout());
        pToolbarPanel.add(toolbar, BorderLayout.PAGE_START);
        pToolbar.add(toolbar, BorderLayout.PAGE_START);

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
                updateSPDetails();
                if (selected) {
                    showAllFields();
                    spSplitPane.setDividerLocation(.5);
                } else {
                }
            }
        });
        toolbar.addButton(btAllFields);

        pDetailedMessage.setVisible(false);
        btDetailedMessage = new JToggleButton("Extracts", pDetailedMessage.isVisible());
        btDetailedMessage.setToolTipText("Toggle panel with message extracts on/off");
        btDetailedMessage.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showDetailed(((JToggleButton) e.getSource()).isSelected());
            }

        });
        toolbar.addButton(btDetailedMessage);

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
                parentForm.fullMsgClosed();
                super.windowClosing(e); // To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                inquirer.logger.debug("windowGainedFocus");
                super.windowGainedFocus(e); // To change body of generated methods, choose Tools | Templates.
            }

        });
        spSplitDetails.setVisible(false);
        pack();
        invalidate();
    }

    private void showJson() {
        try {
            String s = gson.toJson(JsonParser.parseString(jtaMessageText.getSelectedText()));
            detailedData.put(row, s);
            detailedMessage.setText(s);
            detailedMessage.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            detailedMessage.setCodeFoldingEnabled(true);
            btDetailedMessage.setSelected(true);
            showDetailed(true);
        } catch (JsonSyntaxException ex) {
            detailedMessage.setText(">>SELECTION IS NOT JSON<<");
        }
    }

    private void showDetailed(boolean selected) {
        pDetailedMessage.setVisible(selected);
        updateSPDetails();
        if (selected) {
            spSplitPane.setDividerLocation(.5);
        } else {
        }

    }

    private void updateSPDetails() {
        boolean bBottomVisible = pAllFields.isVisible() || pDetailedMessage.isVisible();
        spSplitDetails.setVisible(bBottomVisible);
        if (bBottomVisible) {
            spSplitDetails.setDividerLocation(.5);
        }
        spSplitPane.revalidate();
//        jtaMessageText.revalidate();
//        invalidate();
    }

    private void split5050() {
        if (spSplitDetails.isVisible()) {
            spSplitPane.setDividerLocation(.5);
            spSplitDetails.setDividerLocation(.5);
        }
    }

    private void toggleLineWrap(boolean selected) {
        jtaMessageText.setLineWrap(selected);
        jtaMessageText.repaint();
        detailedMessage.setLineWrap(selected);
        detailedMessage.repaint();
        miWrap.setSelected(selected);
        btLineWrap.setSelected(selected);
    }

    ShowFullMessage(ReportFrameQuery aThis) {
        this();
        parentForm = aThis;
        ScreenInfo.CenterWindowTopMaxWidth(this);

    }

    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        pToolbar = new javax.swing.JPanel();
        spSplitPane = new javax.swing.JSplitPane();
        spSplitDetails = new javax.swing.JSplitPane();
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

        spSplitPane.setDividerSize(3);
        spSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spSplitPane.setResizeWeight(0.95);

        spSplitDetails.setDividerSize(3);
        spSplitDetails.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        spSplitDetails.setResizeWeight(0.95);

        pFullMessage.setLayout(new java.awt.BorderLayout());
        spSplitPane.setLeftComponent(pFullMessage);

        pAllFields.setLayout(new java.awt.BorderLayout());
        spSplitPane.setRightComponent(spSplitDetails);

        spSplitDetails.setLeftComponent(pAllFields);
        pDetailedMessage.setLayout(new java.awt.BorderLayout());
        spSplitDetails.setRightComponent(pDetailedMessage);

        jPanel1.add(spSplitPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1);

        pack();
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosing
        parentForm.fullMsgClosed();
        // TODO add your handling code here:
    }// GEN-LAST:event_formWindowClosing

    private void formWindowDeactivated(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowDeactivated
        // TODO add your handling code here:
    }// GEN-LAST:event_formWindowDeactivated

    private void formWindowStateChanged(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowStateChanged
        // TODO add your handling code here:
    }// GEN-LAST:event_formWindowStateChanged

    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel pAllFields;
    private javax.swing.JPanel pFullMessage;
    private javax.swing.JPanel pDetailedMessage;
    private javax.swing.JPanel pToolbar;
    private javax.swing.JSplitPane spSplitPane;
    private javax.swing.JSplitPane spSplitDetails;

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
                if (StringUtils.isNotBlank(recPrintout.fullMessage)) {
                    jtaMessageText.setText(recPrintout.fullMessage);
                } else {
                    jtaMessageText.setText(row.getRecord().getBytes());
                }
                String s = detailedData.get(row);
                if (StringUtils.isBlank(s)) {
                    s = recPrintout.detailsMessage;
                }
                if (StringUtils.isNotEmpty(s)) {
                    detailedMessage.setText(s);
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
                return o1[0].compareToIgnoreCase(o2[0]); // To change body of generated
                // lambdas, choose Tools | Templates.
            });

            for (String[] entry : kvps) {
                infoTableModel.addRow(entry);
            }
            tca.adjustColumns();
        }
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        mSelection.setEnabled(StringUtils.isNotEmpty(jtaMessageText.getSelectedText()));

    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {

    }
}
