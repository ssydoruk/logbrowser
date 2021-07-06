/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.TableDialog;
import com.myutils.logbrowser.inquirer.gui.TabResultDataModel;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author ssydoruk
 */
public class EditRegexFields extends javax.swing.JDialog {

    /**
     * A return status code - returned if Cancel button has been pressed
     */
    public static final int RET_CANCEL = 0;
    /**
     * A return status code - returned if OK button has been pressed
     */
    public static final int RET_OK = 1;
    private static final Logger logger = LogManager.getLogger(EditRegexFields.class);
    private final DocumentListener listener;
    private final inquirer.InfoPanel savedSelect = null;
    private final JList lSavedList = null;
    private List<RegexFieldSettings> selectedValuesList = null;
    private String savedReplace;
    private MsgType msgType;
    private TabResultDataModel.TableRow curRow;
    private String curRec;
    private TableDialog showLDAP;
    private String savedSearch;
    private String editLine;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bAddSaved;
    private javax.swing.ButtonGroup bgJSapplicationType;
    private javax.swing.JButton btEval;
    private javax.swing.JButton btRecFields;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JButton jbRename;
    private javax.swing.JButton jbSavedDelete;
    private javax.swing.JButton jbSavedSave;
    private javax.swing.JButton jbSavedSaveAs;
    private javax.swing.JCheckBox jcbCaseSensitive;
    private javax.swing.JCheckBox jcbMatchWholeWord;
    private javax.swing.JLabel jlEvalText;
    private javax.swing.JList<String> jlSearches;
    private javax.swing.JTextField jtfRegex;
    private javax.swing.JTextField jtfRetValue;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel pJSScript;
    private javax.swing.JPanel pRegex;
    private javax.swing.JRadioButton rbApplyPerLine;
    private javax.swing.JRadioButton rbApplyToText;
    private javax.swing.JTextArea taCurrentRecord;
    private javax.swing.JTextArea taJSScript;
    private javax.swing.JTabbedPane tpCustomFieldType;
    private int returnStatus = RET_CANCEL;
    private DefaultListModel lmSavedList = null;
    private CustomField currentCustomField;
    private boolean debug = false;
    /**
     * Creates new form EnterRegexDialog
     */
    public EditRegexFields(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        jPanel10.setSize(jPanel10.getWidth(), btEval.getHeight());
        // Close the dialog when Esc is pressed
        if (parent != null) {
            Window windowAncestor = SwingUtilities.getWindowAncestor(parent);
            Point location = parent.getMousePosition();
            setLocation(location);
            setLocationRelativeTo(windowAncestor);
        }

        String cancelName = "cancel";
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelName);
        ActionMap actionMap = getRootPane().getActionMap();
        actionMap.put(cancelName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelPressed();
            }
        });

        String okName = "OK";
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), okName);
        actionMap.put(okName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okPressed();
            }
        });

        ActionListener tfAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // System.out.println("tfAction: " + e.getID() + " " + e.toString());
                searchChanged(true);
            }
        };
        listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                // System.out.println("insertUpdate: " + " " + e.toString());
                searchChanged(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // System.out.println("removeUpdate: " + " " + e.toString());
                searchChanged(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                System.out.println("changedUpdate: " + " " + e.toString());
                searchChanged(true);
            }
        };
        jtfRegex.getDocument().addDocumentListener(listener);
        jtfRetValue.getDocument().addDocumentListener(listener);
        taJSScript.getDocument().addDocumentListener(listener);

        jlSearches.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jlSearches.setLayoutOrientation(JList.VERTICAL);

        jlSearches.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent evt) {
                jlSearchesChanged(evt);
            }

        });

        ActionListener cbActioned = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getID() == ActionEvent.ACTION_PERFORMED) {
                    searchChanged(true);
                }
            }
        };

        // jcbCaseSensitive.addItemListener(cbItem);
        jcbCaseSensitive.addActionListener(cbActioned);
        jcbMatchWholeWord.addActionListener(cbActioned);
        setOKButton();

        Utils.swing.Swing.restrictHeight(jtfRetValue);
    }
    EditRegexFields() {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
        // Tools | Templates.
    }

    public static EditRegexFields getInstance() {
        return NewSingletonHolder.INSTANCE;
    }

    public Object[] toArray() {
        return lmSavedList.toArray();
    }

    public EditRegexFields doShow(Component _parent, boolean b, TabResultDataModel.TableRow row) {
        this.curRow = row;
        curRec = row.getBytes();

        jlEvalText.setText(null);
        taCurrentRecord.setText(curRec);
        msgType = row.getRowType();
        if (lSavedList == null) {
            // JPanel pRefTypes = new JPanel(new BorderLayout());
            lmSavedList = new DefaultListModel();
            jlSearches.setModel(lmSavedList);
        }
        lmSavedList.clear();

        ArrayList<CustomField> regexFieldsSettings = inquirer.getCr().getRegexFieldsSettings(msgType);
        if (regexFieldsSettings != null) {

            for (CustomField regexFieldsSetting : regexFieldsSettings) {
                lmSavedList.addElement(regexFieldsSetting);

            }
        }
        ScreenInfo.CenterWindow(this);
        this.setVisible(true);

        if (getReturnStatus() == RET_OK) {
            return this;
        } else {
            return null;
        }
    }

    private boolean checkRegExSettings() {
        if (StringUtils.isBlank(jtfRegex.getText()) || StringUtils.isBlank(jtfRetValue.getText())) {
            JOptionPane.showMessageDialog(this, "Both \"Searched\" and \"Return value\" must be specified", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean checkJSSettings() {
        if (StringUtils.isBlank(taJSScript.getText())) {
            JOptionPane.showMessageDialog(this, "Text of javascript is empty", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;

    }

    private String inputName() {
        return inputName(null);
    }

    private String inputName(String name) {
        String newName = JOptionPane.showInputDialog(this, "Enter search name",
                name);
        return (StringUtils.isBlank(newName)) ? null : newName;
    }

    private ListSelectionListener[] clearActionListeners(JList<String> jlSearches) {
        ListSelectionListener[] actionListeners = jlSearches.getListSelectionListeners();
        for (ListSelectionListener actionListener : actionListeners) {
            jlSearches.removeListSelectionListener(actionListener);
        }
        return actionListeners;
    }

    private void jlSearchesChanged(ListSelectionEvent evt) {
        JList cb = (JList) evt.getSource();

        currentCustomField = (CustomField) cb.getSelectedValue();
        searchChanged(false);
        if (currentCustomField != null) {
            setCurrentField(currentCustomField);
        }

    }

    private void searchChanged(boolean b) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jbSavedSave.setEnabled(b);
                // logger.info("b: " + b);
                // jbSavedSave.updateUI();
            }
        });
    }

    private void setOKButton() {
        // okButton.setEnabled(!jtfRegex.getText().isEmpty());
    }

    /**
     * @return the return status of this dialog - one of RET_OK or RET_CANCEL
     */
    public int getReturnStatus() {
        return returnStatus;
    }

    public boolean isMatchWholeWordSelected() {
        return jcbCaseSensitive.isSelected();
    }

    // public SearchSample findRxSample(String bytes) {
    // if (searchParams != null && !searchParams.isEmpty()) {
    // return ((SearchParams) searchParams.get(0)).findRxSample(bytes);
    // } else {
    // return null;
    // }
    // }
    private ActionListener[] clearActionListeners(JTextField jtf) {
        ActionListener[] actionListeners = jtf.getActionListeners();
        for (ActionListener actionListener : actionListeners) {
            jtf.removeActionListener(actionListener);
        }
        return actionListeners;
    }

    private ActionListener[] clearActionListeners(JCheckBox jtf) {
        ActionListener[] actionListeners = jtf.getActionListeners();
        for (ActionListener actionListener : actionListeners) {
            jtf.removeActionListener(actionListener);
        }
        return actionListeners;
    }

    private void restoreActionListeners(JTextField jtf, ActionListener[] als) {
        for (ActionListener actionListener : als) {
            jtf.addActionListener(actionListener);
        }
    }

    private void restoreActionListeners(JCheckBox jtf, ActionListener[] als) {
        for (ActionListener actionListener : als) {
            jtf.addActionListener(actionListener);
        }
    }

    private CustomField addSetting(CustomField saveRegexField) {
        int i = lmSavedList.getSize();
        lmSavedList.add(i, saveRegexField);
        jlSearches.setSelectedIndex(i);
        return saveRegexField;
    }

    // private static class SearchParams {
    //
    // private Matcher selectedRegEx = null;
    // private final String retValue;
    //
    // private final boolean wholeWorld;
    // private final boolean matchCase;
    //
    // public SearchParams(String search, String replace, boolean wholeWorld,
    // boolean matchCase) throws PatternSyntaxException {
    // this.retValue = replace;
    // this.wholeWorld = wholeWorld;
    // this.matchCase = matchCase;
    // selectedRegEx = getRegex(search, wholeWorld, matchCase);
    // }
    //
    //
    // }
    // ArrayList<SearchParams> searchParams = new ArrayList<>();
    //
    // private void setSearchParams(String search, String replace, boolean
    // wholeWorld, boolean matchCase) {
    // searchParams.clear();
    // searchParams.add(new SearchParams(search, replace, wholeWorld, matchCase));
    // }
    private void okPressed() {
        // String editLine = (String) jtfRegex.getText();
        // if (editLine != null) {
        // try {
        // setSearchParams(jtfRegex.getText(), jtfRetValue.getText(),
        // isMatchWholeWordSelected(), isMatchWholeWordSelected());
        // } catch (PatternSyntaxException e) {
        // inquirer.showError(this,
        // "Cannot compile regex [" + editLine + "]:\n"
        // + e.getMessage(),
        // "Regex error");
        // return;
        // }
        // } else {
        // searchParams.clear();
        // }
        doClose(RET_OK);
    }

    private void evalPressed() {
        try {
            if (tpCustomFieldType.getSelectedComponent() == pRegex) {
                jlEvalText.setText((new CustomField("", new RegexFieldSettings(jtfRegex.getText(), jtfRetValue.getText(), isMatchWholeWordSelected(), isMatchWholeWordSelected())).getCustomValue(curRec)));
            } else {
                jlEvalText.setText((new CustomField("", new JSFieldSettings(taJSScript.getText(), rbApplyPerLine.isSelected())).getCustomValue(curRow.getRecord())));
            }
            jlEvalText.invalidate();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Exception while eval:" + e + "\n" + StringUtils.join(ArrayUtils.subarray(e.getStackTrace(), 0, 5), "\n") + "\n...", "Error", JOptionPane.ERROR_MESSAGE);
            logger.error("", e);
            //TODO: handle exception
        }
    }

    public String getSearch() {
        return editLine;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bgJSapplicationType = new javax.swing.ButtonGroup();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        tpCustomFieldType = new javax.swing.JTabbedPane();
        pRegex = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jtfRegex = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jcbMatchWholeWord = new javax.swing.JCheckBox();
        jcbCaseSensitive = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jtfRetValue = new javax.swing.JTextField();
        pJSScript = new javax.swing.JPanel();
        jPanel21 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        taJSScript = new javax.swing.JTextArea();
        jPanel18 = new javax.swing.JPanel();
        rbApplyToText = new javax.swing.JRadioButton();
        rbApplyPerLine = new javax.swing.JRadioButton();
        jPanel17 = new javax.swing.JPanel();
        jPanel19 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        taCurrentRecord = new javax.swing.JTextArea();
        jPanel20 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jlEvalText = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jlSearches = new javax.swing.JList<>();
        jPanel12 = new javax.swing.JPanel();
        jbRename = new javax.swing.JButton();
        jbSavedSave = new javax.swing.JButton();
        jbSavedSaveAs = new javax.swing.JButton();
        jbSavedDelete = new javax.swing.JButton();
        bAddSaved = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        btEval = new javax.swing.JButton();
        btRecFields = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.PAGE_AXIS));

        jPanel8.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8, javax.swing.BoxLayout.LINE_AXIS));

        jPanel9.setLayout(new javax.swing.BoxLayout(jPanel9, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel16.setLayout(new javax.swing.BoxLayout(jPanel16, javax.swing.BoxLayout.PAGE_AXIS));

        tpCustomFieldType.setName(""); // NOI18N

        pRegex.setName("Regex"); // NOI18N
        pRegex.setLayout(new javax.swing.BoxLayout(pRegex, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Searched"));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.LINE_AXIS));

        jtfRegex.setMaximumSize(new java.awt.Dimension(2147483647, 26));
        jPanel6.add(jtfRegex);

        jPanel1.add(jPanel6);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.PAGE_AXIS));

        jcbMatchWholeWord.setText("match whole word");
        jPanel2.add(jcbMatchWholeWord);

        jcbCaseSensitive.setText("Case Sensitive");
        jPanel2.add(jcbCaseSensitive);

        jPanel1.add(jPanel2);

        pRegex.add(jPanel1);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Returned value"));
        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));
        jPanel3.add(jtfRetValue);

        pRegex.add(jPanel3);

        tpCustomFieldType.addTab("Regex", pRegex);

        pJSScript.setName("JS script"); // NOI18N
        pJSScript.setLayout(new java.awt.BorderLayout());

        jPanel21.setBorder(javax.swing.BorderFactory.createTitledBorder("Script"));
        jPanel21.setLayout(new java.awt.BorderLayout());

        taJSScript.setColumns(20);
        taJSScript.setRows(5);
        jScrollPane3.setViewportView(taJSScript);

        jPanel21.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        pJSScript.add(jPanel21, java.awt.BorderLayout.CENTER);

        jPanel18.setLayout(new javax.swing.BoxLayout(jPanel18, javax.swing.BoxLayout.LINE_AXIS));

        bgJSapplicationType.add(rbApplyToText);
        rbApplyToText.setSelected(true);
        rbApplyToText.setText("apply to whole test once");
        rbApplyToText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rbApplyToTextActionPerformed(evt);
            }
        });
        jPanel18.add(rbApplyToText);

        bgJSapplicationType.add(rbApplyPerLine);
        rbApplyPerLine.setText("Apply per line");
        jPanel18.add(rbApplyPerLine);

        pJSScript.add(jPanel18, java.awt.BorderLayout.PAGE_END);

        tpCustomFieldType.addTab("JS script", pJSScript);

        jPanel16.add(tpCustomFieldType);

        jPanel9.add(jPanel16);

        jPanel17.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED), "Current record"));
        jPanel17.setLayout(new javax.swing.BoxLayout(jPanel17, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel19.setLayout(new java.awt.BorderLayout());

        taCurrentRecord.setEditable(false);
        taCurrentRecord.setColumns(20);
        taCurrentRecord.setRows(5);
        jScrollPane2.setViewportView(taCurrentRecord);

        jPanel19.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jPanel17.add(jPanel19);

        jPanel20.setLayout(new javax.swing.BoxLayout(jPanel20, javax.swing.BoxLayout.LINE_AXIS));

        jLabel1.setText("Result: ");
        jPanel20.add(jLabel1);

        jlEvalText.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jlEvalText.setText("aaa");
        jPanel20.add(jlEvalText);

        jPanel17.add(jPanel20);

        jPanel9.add(jPanel17);

        jPanel8.add(jPanel9);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Saved custom fields"));
        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.LINE_AXIS));

        jPanel13.setLayout(new javax.swing.BoxLayout(jPanel13, javax.swing.BoxLayout.LINE_AXIS));

        jScrollPane1.setViewportView(jlSearches);

        jPanel13.add(jScrollPane1);

        jPanel7.add(jPanel13);

        jPanel12.setMaximumSize(new java.awt.Dimension(95, 65534));
        jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12, javax.swing.BoxLayout.PAGE_AXIS));

        jbRename.setText("Rename");
        jbRename.setMaximumSize(new java.awt.Dimension(90, 29));
        jbRename.setMinimumSize(new java.awt.Dimension(90, 29));
        jbRename.setPreferredSize(new java.awt.Dimension(73, 23));
        jbRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbRenameActionPerformed(evt);
            }
        });
        jPanel12.add(jbRename);

        jbSavedSave.setText("Save");
        jbSavedSave.setMaximumSize(new java.awt.Dimension(90, 29));
        jbSavedSave.setMinimumSize(new java.awt.Dimension(90, 29));
        jbSavedSave.setPreferredSize(new java.awt.Dimension(73, 23));
        jbSavedSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSavedSaveActionPerformed(evt);
            }
        });
        jPanel12.add(jbSavedSave);

        jbSavedSaveAs.setText("Save as");
        jbSavedSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSavedSaveAsActionPerformed(evt);
            }
        });
        jPanel12.add(jbSavedSaveAs);

        jbSavedDelete.setText("Delete");
        jbSavedDelete.setMinimumSize(new java.awt.Dimension(90, 29));
        jbSavedDelete.setPreferredSize(new java.awt.Dimension(73, 23));
        jbSavedDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSavedDeleteActionPerformed(evt);
            }
        });
        jPanel12.add(jbSavedDelete);

        bAddSaved.setText("Add");
        bAddSaved.setMinimumSize(new java.awt.Dimension(90, 29));
        bAddSaved.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bAddSavedActionPerformed(evt);
            }
        });
        jPanel12.add(bAddSaved);

        jPanel7.add(jPanel12);

        jPanel8.add(jPanel7);

        getContentPane().add(jPanel8);

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.LINE_AXIS));

        jPanel10.setLayout(new javax.swing.BoxLayout(jPanel10, javax.swing.BoxLayout.LINE_AXIS));

        btEval.setText("Eval");
        btEval.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btEvalActionPerformed(evt);
            }
        });
        jPanel10.add(btEval);

        btRecFields.setText("Fields");
        btRecFields.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btRecFieldsActionPerformed(evt);
            }
        });
        jPanel10.add(btRecFields);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        jPanel10.add(cancelButton);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        jPanel10.add(okButton);
        getRootPane().setDefaultButton(okButton);

        jPanel4.add(jPanel10);

        getContentPane().add(jPanel4);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void bAddSavedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bAddSavedActionPerformed
        setCurrentField(null);
    }//GEN-LAST:event_bAddSavedActionPerformed

    private void btRecFieldsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btRecFieldsActionPerformed
        if (showLDAP == null) {
            showLDAP = new TableDialog(this, JOptionPane.OK_OPTION);
        }

        ArrayList<String[]> kvps = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : curRow.getRecord().m_fieldsAll.entrySet()) {
            kvps.add(new String[]{entry.getKey().toString(), entry.getValue().toString()});
        }
//        for (Map.Entry<Object, Object> entry : curRow.getRecord().m_fields.entrySet()) {
//            kvps.add(new String[]{entry.getKey().toString(), entry.getValue().toString()});
//        }
        Collections.sort(kvps, (o1, o2) -> {
            return ((String[]) o1)[0].compareToIgnoreCase(((String[]) o2)[0]); //To change body of generated lambdas, choose Tools | Templates.
        });
        showLDAP.shouldProceed(new String[]{"Field", "Value"}, kvps, "All record fields (" + kvps.size() + ")");
    }//GEN-LAST:event_btRecFieldsActionPerformed

    /**
     * Closes the dialog
     */
    private void closeDialog(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_closeDialog
        doClose(RET_CANCEL);
    }// GEN-LAST:event_closeDialog

    private void jbSavedSaveActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jbSavedSaveActionPerformed
        savedPressed(SaveAction.SAVE);
    }// GEN-LAST:event_jbSavedSaveActionPerformed

    private void jbSavedDeleteActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jbSavedDeleteActionPerformed
        DeletePressed();
    }// GEN-LAST:event_jbSavedDeleteActionPerformed

    private void jbSavedSaveAsActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jbSavedSaveAsActionPerformed
        savedPressed(SaveAction.SAVEAS);
    }// GEN-LAST:event_jbSavedSaveAsActionPerformed

    private void formComponentShown(java.awt.event.ComponentEvent evt) {// GEN-FIRST:event_formComponentShown
        // TODO add your handling code here:
        setCurrentField(null);
        jtfRegex.setText(savedSearch);
        jtfRetValue.setText(savedReplace);

    }// GEN-LAST:event_formComponentShown

    private void jbRenameActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jbRenameActionPerformed
        savedPressed(SaveAction.RENAME);
    }// GEN-LAST:event_jbRenameActionPerformed
    // End of variables declaration//GEN-END:variables

    private void btEvalActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btEvalActionPerformed
        evalPressed();
    }// GEN-LAST:event_btEvalActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cancelButtonActionPerformed
        cancelPressed();
    }// GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_okButtonActionPerformed
        okPressed();
    }// GEN-LAST:event_okButtonActionPerformed

    private void rbApplyToTextActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_rbApplyToTextActionPerformed
        // TODO add your handling code here:
    }// GEN-LAST:event_rbApplyToTextActionPerformed

    private void doClose(int retStatus) {
        logger.debug("Closing with retStatus=" + retStatus);
        returnStatus = retStatus;
        setVisible(false);
        dispose();
    }

    private void cancelPressed() {
        doClose(RET_CANCEL);
    }

    private void savedPressed(SaveAction action) {
        String newName;
        switch (action) {
            case SAVE:
                if (tpCustomFieldType.getSelectedComponent() == pRegex) {
                    if (!checkRegExSettings()) {
                        return;
                    }
                    if (currentCustomField == null) {
                        if ((newName = inputName()) == null) {
                            return;
                        }

                        addSetting(new CustomField(newName, new RegexFieldSettings(jtfRegex.getText(), jtfRetValue.getText(),
                                jcbCaseSensitive.isSelected(), jcbMatchWholeWord.isSelected())));
                    } else {
                        currentCustomField.getRxFieldSettings().updateParams(jtfRegex.getText(), jtfRetValue.getText(),
                                jcbCaseSensitive.isSelected(), jcbMatchWholeWord.isSelected());
                    }
                } else {
                    if (!checkJSSettings()) {
                        return;
                    }
                    if (currentCustomField == null) {
                        if ((newName = inputName()) == null) {
                            return;
                        }
                        addSetting(new CustomField(newName, new JSFieldSettings(taJSScript.getText(), rbApplyPerLine.isSelected())));
                    } else {
                        currentCustomField.getJsFieldSettings().updateParams(taJSScript.getText(), rbApplyPerLine.isSelected());
                    }
                }
                break;
            case SAVEAS:
                if (currentCustomField != null) {
                    if ((newName = inputName(currentCustomField.getName())) == null) {
                        return;
                    }
                    if (tpCustomFieldType.getSelectedComponent() == pRegex) {
                        addSetting(new CustomField(newName,
                                new RegexFieldSettings(currentCustomField.getRxFieldSettings())));
                    } else {
                        addSetting(new CustomField(newName,
                                new JSFieldSettings(currentCustomField.getJsFieldSettings())));
                    }
                }
                break;

            case RENAME:
                if (currentCustomField != null) {
                    if ((newName = inputName(currentCustomField.getName())) == null) {
                        return;
                    }
                    currentCustomField.setName(newName);
                    setCurrentField(currentCustomField);
                }

                break;
        }

        // if (currentRegexSetting == null) {
        // setCurrentRegex(addSetting(new RegexFieldSettings(newName,
        // jtfRegex.getText(),
        // jtfRetValue.getText(),
        // jcbCaseSensitive.isSelected(),
        // jcbMatchWholeWord.isSelected())));
        // ddSetting(new RegexFieldSettings(newName,
        // jtfRegex.getText(),
        // jtfRetValue.getText(),
        // jcbCaseSensitive.isSelected(),
        // jcbMatchWholeWord.isSelected()))
        // } else {
        // if (isSaveAs) {
        // setCurrentRegex(addSetting(new RegexFieldSettings(newName,
        // currentRegexSetting)));
        // } else if (isRename) {
        // currentRegexSetting.setName(newName);
        // setCurrentRegex(currentRegexSetting);
        // jlSearches.repaint();
        // } else { // just save
        // currentRegexSetting.updateParams(
        // jtfRegex.getText(),
        // jtfRetValue.getText(),
        // jcbCaseSensitive.isSelected(),
        // jcbMatchWholeWord.isSelected());
        // }
        // }
        searchChanged(false);

    }

    private void setCurrentField(CustomField saveRegexField) {
        currentCustomField = saveRegexField;
        selectedValuesList = null;
        StringBuilder title = new StringBuilder("Custom field parameter for ");
        title.append(msgType);

        ActionListener[] changeListenerjcbCaseSensitive = clearActionListeners(jcbCaseSensitive);
        ActionListener[] changeListenerjcbMatchWholeWord = clearActionListeners(jcbMatchWholeWord);
        ListSelectionListener[] lSearchesListeners = clearActionListeners(jlSearches);

        jtfRegex.getDocument().removeDocumentListener(listener);
        jtfRetValue.getDocument().removeDocumentListener(listener);
        tpCustomFieldType.removeAll();

        if (currentCustomField == null) {
            title.append(" (not saved)");
            jtfRegex.setText(null);
            jtfRetValue.setText(null);
            jcbCaseSensitive.setSelected(false);
            jcbMatchWholeWord.setSelected(false);
            pJSScript.setEnabled(true);
            pRegex.setEnabled(true);
            taJSScript.setText(null);
            rbApplyToText.setSelected(true);
            tpCustomFieldType.add(pJSScript);
            tpCustomFieldType.add(pRegex);
            jlSearches.clearSelection();

            for (Component component : tpCustomFieldType.getComponents()) {
                component.setEnabled(true);
            }
        } else {
            title.append(" - ").append(currentCustomField.getName());
//            tpCustomFieldType.add
            if (currentCustomField.isJSField()) {
                tpCustomFieldType.add(pJSScript);
                taJSScript.setText(currentCustomField.getJsFieldSettings().getJsScript());
                rbApplyPerLine.setSelected(currentCustomField.getJsFieldSettings().isPerLine());

            }
            if (currentCustomField.isRXField()) {
                tpCustomFieldType.add(pRegex);
                jtfRegex.setText(currentCustomField.getRxFieldSettings().getSearchString());
                jtfRetValue.setText(currentCustomField.getRxFieldSettings().getRetValue());
                jcbCaseSensitive.setSelected(currentCustomField.getRxFieldSettings().isCaseSensitive());
                jcbMatchWholeWord.setSelected(currentCustomField.getRxFieldSettings().isMakeWholeWorld());

            }
        }

        jtfRegex.getDocument().addDocumentListener(listener);
        jtfRetValue.getDocument().addDocumentListener(listener);

        restoreActionListeners(jcbCaseSensitive, changeListenerjcbCaseSensitive);
        restoreActionListeners(jcbMatchWholeWord, changeListenerjcbMatchWholeWord);

        for (ListSelectionListener lSearchesListener : lSearchesListeners) {
            jlSearches.addListSelectionListener(lSearchesListener);
        }

        setTitle(title.toString());
//        jbSavedDelete.setEnabled(currentCustomField != null);
//        jbSavedSaveAs.setEnabled(currentCustomField != null);
//        jbRename.setEnabled(currentCustomField != null);
//        jbSavedSave.setEnabled(currentCustomField != null);
    }

    private void DeletePressed() {
        if (currentCustomField != null) {
            if (JOptionPane.showConfirmDialog(this, "Do you want to delete \"" + currentCustomField.getName() + "\"?",
                    "Please confirm", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                lmSavedList.removeElement(currentCustomField);
                setCurrentField(null);
            }
        }
    }

    public boolean isMultipleRows() {
        return selectedValuesList != null && selectedValuesList.size() > 1;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private enum SaveAction {
        SAVE, SAVEAS, RENAME
    }

    private static class NewSingletonHolder {

        private static final EditRegexFields INSTANCE = new EditRegexFields();
    }

}
