/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import com.myutils.logbrowser.inquirer.gui.TabResultDataModel;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author ssydoruk
 */
public class SearchExtract extends javax.swing.JDialog {

    /**
     * A return status code - returned if Cancel button has been pressed
     */
    public static final int RET_CANCEL = 0;
    /**
     * A return status code - returned if OK button has been pressed
     */
    public static final int RET_OK = 1;
    private List<InquirerCfg.RegexFieldSettings> selectedValuesList = null;
    private String savedReplace;
    private DocumentListener listener;
    private MsgType msgType;

    public Object[] toArray() {
        return lmSavedList.toArray();
    }

    public SearchExtract doShow(Component _parent, boolean b, MsgType t) {
        msgType = t;
        if (lSavedList == null) {
//            JPanel pRefTypes = new JPanel(new BorderLayout());
            lmSavedList = new DefaultListModel();
            jlSearches.setModel(lmSavedList);
        }
        lmSavedList.clear();

        ArrayList<InquirerCfg.RegexFieldSettings> regexFieldsSettings = inquirer.getCr().getRegexFieldsSettings(t);
        if (regexFieldsSettings != null) {

            for (InquirerCfg.RegexFieldSettings regexFieldsSetting : regexFieldsSettings) {
                lmSavedList.addElement(regexFieldsSetting);

            }
        }

        ScreenInfo.setVisible(_parent, this, true);

        if (getReturnStatus() == RET_OK) {
            return this;
        } else {
            return null;
        }
    }

    private static final Logger logger = LogManager.getLogger(SearchExtract.class);

    /**
     * Creates new form EnterRegexDialog
     */
    public SearchExtract(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        // Close the dialog when Esc is pressed
        if (parent != null) {
            Window windowAncestor = (Window) SwingUtilities.getWindowAncestor(parent);
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
                OKPressed();
            }
        });

        ActionListener tfAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("tfAction: " + e.getID() + " " + e.toString());
                searchChanged(true);
            }
        };
        listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
//                System.out.println("insertUpdate: " + " " + e.toString());
                searchChanged(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
//                System.out.println("removeUpdate: " + " " + e.toString());
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

//        jcbCaseSensitive.addItemListener(cbItem);
        jcbCaseSensitive.addActionListener(cbActioned);
        jcbMatchWholeWord.addActionListener(cbActioned);
        setOKButton();
//        jcbRegex.getModel().addListDataListener(jcbRegex);

//        jtfRegex.getDocument().addDocumentListener(new DocumentListener() {
//
//            public void insertUpdate(DocumentEvent e) {
//                setOKButton();
//            }
//
//            public void removeUpdate(DocumentEvent e) {
//                setOKButton();
//            }
//
//            public void changedUpdate(DocumentEvent e) {
//                setOKButton();
//            }
//        });
    }

    private void jlSearchesChanged(ListSelectionEvent evt) {
        JList cb = (JList) evt.getSource();
        boolean shouldCheckAll = true;
        currentRegexSetting = (InquirerCfg.RegexFieldSettings) cb.getSelectedValue();
        searchChanged(false);
        if (currentRegexSetting != null) {
            setCurrentRegex(currentRegexSetting);
        }

    }

    private void searchChanged(boolean b) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jbSavedSave.setEnabled(b);
//                logger.info("b: " + b);
//        jbSavedSave.updateUI();
            }
        });
    }

    private void setOKButton() {
//        okButton.setEnabled(!jtfRegex.getText().isEmpty());
    }

    SearchExtract() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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

    private String savedSearch;

    public void addRxColumns(String GetFileBytes, TabResultDataModel.TableRow tableRow, String colPrefix, int startIdx) {
        int i = 0;
        for (SearchParams searchParam : searchParams) {
            String ret = searchParam.findRx(GetFileBytes);
            inquirer.logger.debug("Found val[" + ret + "] rowSize:" + tableRow.size());
            if (ret != null && !ret.isEmpty()) {
                tableRow.addCell(colPrefix + startIdx + i, ret);
            } else {
                tableRow.addCell(colPrefix + startIdx + i, "");
            }
            i++;
        }
    }

    public SearchSample findRxSample(String bytes) {
        if (searchParams != null && !searchParams.isEmpty()) {
            return ((SearchParams) searchParams.get(0)).findRxSample(bytes);
        } else {
            return null;
        }
    }

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

    private InquirerCfg.RegexFieldSettings addSetting(InquirerCfg.RegexFieldSettings saveRegexField) {
        int i = lmSavedList.getSize();
        lmSavedList.add(i, saveRegexField);
        jlSearches.setSelectedIndex(i);
        return saveRegexField;
    }

    private static class SearchParams {

        private Pattern selectedRegEx = null;
        private final String retValue;

        private final boolean wholeWorld;
        private final boolean matchCase;

        public SearchParams(String search, String replace, boolean wholeWorld, boolean matchCase) throws PatternSyntaxException {
            this.retValue = replace;
            this.wholeWorld = wholeWorld;
            this.matchCase = matchCase;
            selectedRegEx = getRegex(search, wholeWorld, matchCase);
        }

        public Pattern getRegex(String lastRegEx, boolean wholeWord, boolean matchCase) throws PatternSyntaxException {
            if (lastRegEx != null && !lastRegEx.isEmpty()) {
                int flags = 0;
                if (!matchCase) {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                if (wholeWord) {
                    StringBuilder rx = new StringBuilder(lastRegEx.length() + 2);
                    if (lastRegEx.charAt(0) != '^') {
                        rx.append('^');
                    }
                    rx.append(lastRegEx);
                    if (lastRegEx.charAt(lastRegEx.length() - 1) != '$') {
                        rx.append('$');
                    }
                    inquirer.logger.debug("Searching for [" + rx + "]");
                    return Pattern.compile(rx.toString(), flags);
                } else {
                    inquirer.logger.debug("Searching for [" + lastRegEx + "]");
                    return Pattern.compile(lastRegEx, flags);
                }
            }
            return null;
        }

        private String doRepl(String text, ArrayList<Repl> repl, Matcher m) {
            if (!repl.isEmpty()) {
                StringBuilder ret = new StringBuilder();
                int idx = 0;
                for (int i = 0; i < repl.size(); i++) {
                    Repl r = repl.get(i);
                    ret.append(text.substring(idx, r.start));
                    ret.append(m.group(r.id));
                    idx = r.end;
                }
                if (idx < text.length()) {
                    ret.append(text.substring(idx));
                }
                return ret.toString();
            }
            return text;
        }

        public boolean checkMatch(String toString) {
            if (selectedRegEx != null) {
                if (toString == null) {
                    return selectedRegEx.matcher("").find();
                } else {
                    return selectedRegEx.matcher(toString).find();
                }

            } else {
                if (toString == null) {
                    return false;
                }
                if (wholeWorld) {
                    return retValue.equalsIgnoreCase(toString);
                } else {
                    return toString.toLowerCase().contains(retValue.toLowerCase());
                }
            }
        }

        private StringBuilder extendString(StringBuilder s, String add) {
            if (s.length() > 0) {
                s.append(" | ");
            }
            s.append(add);
            return s;
        }

        public SearchSample findRxSample(String bytes) {
            if (bytes != null && bytes.length() > 0 && selectedRegEx != null) {
                SearchSample ret = new SearchSample();
                String[] split = StringUtils.split(bytes, "\r\n");
                String text = retValue;
                Matcher m1 = rxVar.matcher(text);
                ArrayList<Repl> repl = new ArrayList<>();
                while (m1.find()) {
                    repl.add(new Repl(m1.start(0), m1.end(0), Integer.parseInt(m1.group(1))));
                };
                StringBuilder s = new StringBuilder();
                for (String string : split) {
                    Matcher m = selectedRegEx.matcher(string);
                    boolean found = false;
                    inquirer.logger.trace("matching [" + selectedRegEx + "] against [" + string + "]");
                    while (m.find()) {
                        inquirer.logger.debug("found! [");
                        extendString(s, doRepl(text, repl, m));
                        found = true;
                    };
                    if (found) {
                        ret.appendSearched(string);
                        inquirer.logger.debug("matched! ret=[" + ret + "]");
                    }
                }

                if (ret.hasMathes()) {
                    ret.setResult(s.toString());
                    inquirer.logger.debug("returning ret=[" + ret + "]");

                    return ret;
                }
            }
            return null;
        }

        public String findRx(String bytes) {
            if (bytes != null && bytes.length() > 0) {
                String[] split = StringUtils.split(bytes, "\r\n");
                String text = retValue;
                Matcher m1 = rxVar.matcher(text);
                ArrayList<Repl> repl = new ArrayList<>();
                while (m1.find()) {
                    repl.add(new Repl(m1.start(0), m1.end(0), Integer.parseInt(m1.group(1))));
                };
                StringBuilder s = new StringBuilder();
                for (String string : split) {
                    Matcher m = selectedRegEx.matcher(string);
                    while (m.find()) {
                        extendString(s, doRepl(text, repl, m));
                    };
                }
                return s.toString();
            }
            return null;
        }
    }

    ArrayList<SearchParams> searchParams = new ArrayList<>();

    private void setSearchParams(String search, String replace, boolean wholeWorld, boolean matchCase) {
        searchParams.clear();
        searchParams.add(new SearchParams(search, replace, wholeWorld, matchCase));
    }

    private void OKPressed() {
        String editLine = (String) jtfRegex.getText();
        if (editLine != null) {
            try {
                setSearchParams(jtfRegex.getText(), jtfRetValue.getText(), isMatchWholeWordSelected(), isMatchWholeWordSelected());
            } catch (PatternSyntaxException e) {
                inquirer.showError(this,
                        "Cannot compile regex [" + editLine + "]:\n"
                        + e.getMessage(),
                        "Regex error");
                return;
            }
        } else {
            searchParams.clear();
        }
        doClose(RET_OK);
    }

    public String getSearch() {
        return editLine;
    }

    private String editLine;

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jPanel16 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jtfRegex = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jcbMatchWholeWord = new javax.swing.JCheckBox();
        jcbCaseSensitive = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jtfRetValue = new javax.swing.JTextField();
        jPanel17 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jlSearches = new javax.swing.JList<>();
        jPanel12 = new javax.swing.JPanel();
        jPanel14 = new javax.swing.JPanel();
        jbRename = new javax.swing.JButton();
        jbSavedSave = new javax.swing.JButton();
        jbSavedSaveAs = new javax.swing.JButton();
        jbSavedDelete = new javax.swing.JButton();
        jPanel15 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();

        setResizable(false);
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

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Searched"));
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.LINE_AXIS));

        jtfRegex.setText("jTextField1");
        jtfRegex.setMaximumSize(new java.awt.Dimension(2147483647, 26));
        jPanel6.add(jtfRegex);

        jPanel1.add(jPanel6);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.PAGE_AXIS));

        jcbMatchWholeWord.setText("match whole word");
        jPanel2.add(jcbMatchWholeWord);

        jcbCaseSensitive.setText("Case Sensitive");
        jPanel2.add(jcbCaseSensitive);

        jPanel1.add(jPanel2);

        jPanel16.add(jPanel1);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Returned value"));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jtfRetValue, javax.swing.GroupLayout.PREFERRED_SIZE, 449, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jtfRetValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel16.add(jPanel3);

        jPanel9.add(jPanel16);

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 738, Short.MAX_VALUE)
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 112, Short.MAX_VALUE)
        );

        jPanel9.add(jPanel17);

        jPanel8.add(jPanel9);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Saved search"));
        jPanel7.setLayout(new javax.swing.BoxLayout(jPanel7, javax.swing.BoxLayout.LINE_AXIS));

        jPanel13.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setViewportView(jlSearches);

        jPanel13.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel7.add(jPanel13);

        jPanel12.setMaximumSize(new java.awt.Dimension(95, 65534));
        jPanel12.setLayout(new javax.swing.BoxLayout(jPanel12, javax.swing.BoxLayout.PAGE_AXIS));

        jPanel14.setMaximumSize(new java.awt.Dimension(95, 32767));
        jPanel14.setLayout(new java.awt.GridLayout(0, 1, 1, 0));

        jbRename.setText("Rename");
        jbRename.setMaximumSize(new java.awt.Dimension(90, 29));
        jbRename.setMinimumSize(new java.awt.Dimension(73, 23));
        jbRename.setPreferredSize(new java.awt.Dimension(73, 23));
        jbRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbRenameActionPerformed(evt);
            }
        });
        jPanel14.add(jbRename);

        jbSavedSave.setText("Save");
        jbSavedSave.setMaximumSize(new java.awt.Dimension(90, 29));
        jbSavedSave.setMinimumSize(new java.awt.Dimension(73, 23));
        jbSavedSave.setPreferredSize(new java.awt.Dimension(73, 23));
        jbSavedSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSavedSaveActionPerformed(evt);
            }
        });
        jPanel14.add(jbSavedSave);

        jbSavedSaveAs.setText("Save as");
        jbSavedSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSavedSaveAsActionPerformed(evt);
            }
        });
        jPanel14.add(jbSavedSaveAs);

        jbSavedDelete.setText("Delete");
        jbSavedDelete.setPreferredSize(new java.awt.Dimension(73, 23));
        jbSavedDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbSavedDeleteActionPerformed(evt);
            }
        });
        jPanel14.add(jbSavedDelete);

        jPanel12.add(jPanel14);

        jPanel15.setMaximumSize(new java.awt.Dimension(95, 32767));

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 90, Short.MAX_VALUE)
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 113, Short.MAX_VALUE)
        );

        jPanel12.add(jPanel15);

        jPanel7.add(jPanel12);

        jPanel8.add(jPanel7);

        getContentPane().add(jPanel8);

        jPanel4.setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.LINE_AXIS));

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 929, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 43, Short.MAX_VALUE)
        );

        jPanel5.add(jPanel11);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cancelButton)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        getRootPane().setDefaultButton(okButton);

        jPanel5.add(jPanel10);

        jPanel4.add(jPanel5, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel4);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        OKPressed();
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        cancelPressed();

    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Closes the dialog
     */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        doClose(RET_CANCEL);
    }//GEN-LAST:event_closeDialog

    private void jbSavedSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSavedSaveActionPerformed
        savedPressed(false);
    }//GEN-LAST:event_jbSavedSaveActionPerformed

    private void jbSavedDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSavedDeleteActionPerformed
        DeletePressed();
    }//GEN-LAST:event_jbSavedDeleteActionPerformed

    private void jbSavedSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbSavedSaveAsActionPerformed
        savedPressed(true);
    }//GEN-LAST:event_jbSavedSaveAsActionPerformed

    private void formComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentShown
        // TODO add your handling code here:
        setCurrentRegex((InquirerCfg.RegexFieldSettings) null);
        jtfRegex.setText(savedSearch);
        jtfRetValue.setText(savedReplace);

    }//GEN-LAST:event_formComponentShown

    private void jbRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbRenameActionPerformed
        savedPressed(false, true);
    }//GEN-LAST:event_jbRenameActionPerformed

    private void doClose(int retStatus) {
        logger.debug("Closing with retStatus=" + retStatus);
        returnStatus = retStatus;
        setVisible(false);
        dispose();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton jbRename;
    private javax.swing.JButton jbSavedDelete;
    private javax.swing.JButton jbSavedSave;
    private javax.swing.JButton jbSavedSaveAs;
    private javax.swing.JCheckBox jcbCaseSensitive;
    private javax.swing.JCheckBox jcbMatchWholeWord;
    private javax.swing.JList<String> jlSearches;
    private javax.swing.JTextField jtfRegex;
    private javax.swing.JTextField jtfRetValue;
    private javax.swing.JButton okButton;
    // End of variables declaration//GEN-END:variables

    private int returnStatus = RET_CANCEL;

    private void cancelPressed() {
        doClose(RET_CANCEL);
    }

    private static final Pattern rxVar = Pattern.compile("\\\\(\\d+)");

    private void savedPressed(boolean isSaveAs, boolean isRename) {
        String newName = null;
        if (StringUtils.isBlank(jtfRegex.getText())
                || StringUtils.isBlank(jtfRetValue.getText())) {
            JOptionPane.showMessageDialog(this, "Both \"Searched\" and \"Return value\" must be specified", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (isSaveAs || isRename || currentRegexSetting == null) { // query for name
            newName = JOptionPane.showInputDialog(this, "Enter search name", (currentRegexSetting == null) ? "" : currentRegexSetting.getName());
            if (StringUtils.isBlank(newName)) {
                return;
            }
        }
        if (currentRegexSetting == null) {
            setCurrentRegex(addSetting(new InquirerCfg.RegexFieldSettings(newName,
                    jtfRegex.getText(),
                    jtfRetValue.getText(),
                    jcbCaseSensitive.isSelected(),
                    jcbMatchWholeWord.isSelected())));

        } else {
            if (isSaveAs) {
                setCurrentRegex(addSetting(new InquirerCfg.RegexFieldSettings(newName,
                        currentRegexSetting)));
            } else if (isRename) {
                currentRegexSetting.setName(newName);
                setCurrentRegex(currentRegexSetting);
                jlSearches.repaint();
            } else { // just save
                currentRegexSetting.updateParams(
                        jtfRegex.getText(),
                        jtfRetValue.getText(),
                        jcbCaseSensitive.isSelected(),
                        jcbMatchWholeWord.isSelected());
            }
        }
        searchChanged(false);

    }

    private void savedPressed(boolean isSaveAs) {
        savedPressed(isSaveAs, false);

    }

    private inquirer.InfoPanel savedSelect = null;
    private JList lSavedList = null;
    private DefaultListModel lmSavedList = null;

    private void loadSaved() {
        if (savedSelect == null) {
//            JPanel pRefTypes = new JPanel(new BorderLayout());
            lmSavedList = new DefaultListModel();
            lSavedList = new JList(lmSavedList);
            lSavedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            lSavedList.setLayoutOrientation(JList.VERTICAL);
            JScrollPane jScrollPane = new JScrollPane(lSavedList);
//            pRefTypes.add(jScrollPane);

            savedSelect = new inquirer.InfoPanel(this, "name", jScrollPane, JOptionPane.OK_CANCEL_OPTION);
        }
        lmSavedList.clear();
        for (InquirerCfg.RegexFieldSettings regexFieldsSetting : inquirer.getCr().getRegexFieldsSettings()) {
            lmSavedList.addElement(regexFieldsSetting);

        }
        lSavedList.setSelectedIndex(0);
        savedSelect.showModal();
        if (savedSelect.getDialogResult() == JOptionPane.OK_OPTION) {
            List selectedValuesList = lSavedList.getSelectedValuesList();
            if (selectedValuesList.size() > 0) {
                if (selectedValuesList.size() == 1) {
                    setCurrentRegex((InquirerCfg.RegexFieldSettings) lSavedList.getSelectedValue());
                } else {
                    setCurrentRegex(selectedValuesList);
                }
            }

        };
//                        this, "name", null, jScrollPane, JOptionPane.OK_CANCEL_OPTION);
//        inquirer.showYesNoPanel(this, "name", null, jScrollPane, JOptionPane.OK_CANCEL_OPTION);
    }

    private InquirerCfg.RegexFieldSettings currentRegexSetting;

    private void setCurrentRegex(InquirerCfg.RegexFieldSettings saveRegexField) {
        currentRegexSetting = saveRegexField;
        selectedValuesList = null;
        StringBuilder title = new StringBuilder("Regex field parameter for ");
        title.append(msgType);
//        ActionListener[] actionListenersjtfRegex = clearActionListeners(jtfRegex);
        ActionListener[] changeListenerjcbCaseSensitive = clearActionListeners(jcbCaseSensitive);
        ActionListener[] changeListenerjcbMatchWholeWord = clearActionListeners(jcbMatchWholeWord);
        jtfRegex.getDocument().removeDocumentListener(listener);
        jtfRetValue.getDocument().removeDocumentListener(listener);

        if (currentRegexSetting == null) {
            title.append(" (not saved)");
            jtfRegex.setText(null);
            jtfRetValue.setText(null);
            jcbCaseSensitive.setSelected(false);
            jcbMatchWholeWord.setSelected(false);
        } else {
            title.append(" - ").append(currentRegexSetting.getName());
            jtfRegex.setText(currentRegexSetting.getSearchString());
            jtfRetValue.setText(currentRegexSetting.getRetValue());
            jcbCaseSensitive.setSelected(currentRegexSetting.isCaseSensitive());
            jcbMatchWholeWord.setSelected(currentRegexSetting.isMakeWholeWorld());
        }

//        restoreActionListeners(jtfRegex, actionListenersjtfRegex);
//        restoreActionListeners(jtfRetValue, actionListenersjtfRetValue);
        jtfRegex.getDocument().addDocumentListener(listener);
        jtfRetValue.getDocument().addDocumentListener(listener);

        restoreActionListeners(jcbCaseSensitive, changeListenerjcbCaseSensitive);
        restoreActionListeners(jcbMatchWholeWord, changeListenerjcbMatchWholeWord);

        setTitle(title.toString());
        jbSavedDelete.setEnabled(currentRegexSetting != null);
        jbSavedSaveAs.setEnabled(currentRegexSetting != null);
        jbRename.setEnabled(currentRegexSetting != null);
        jbSavedSave.setEnabled(currentRegexSetting != null);
    }

    private void DeletePressed() {
        if (currentRegexSetting != null) {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "Do you want to delete \"" + currentRegexSetting.getName() + "\"?",
                    "Please confirm",
                    JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                lmSavedList.removeElement(currentRegexSetting);
                setCurrentRegex((InquirerCfg.RegexFieldSettings) null);
            }
        }
    }

    private void setCurrentRegex(List<InquirerCfg.RegexFieldSettings> selectedValuesList) {
        this.selectedValuesList = selectedValuesList;
        searchParams.clear();
        for (InquirerCfg.RegexFieldSettings regexFieldSettings : selectedValuesList) {
            searchParams.add(new SearchParams(regexFieldSettings.getSearchString(),
                    regexFieldSettings.getRetValue(),
                    regexFieldSettings.isMakeWholeWorld(), regexFieldSettings.isCaseSensitive()));
        }
        doClose(RET_OK);
    }

    public boolean isMultipleRows() {
        return selectedValuesList != null && selectedValuesList.size() > 1;
    }

    private static class Repl {

        private final int start;
        private final int end;
        private final int id;

        Repl(int start, int end, int id) {
            this.start = start;
            this.end = end;
            this.id = id;
        }
    }

    public static class SearchSample extends Pair<String, String> {

        public SearchSample() {
        }

        private void appendSearched(String string) {
            String s = StringUtils.replaceAll(string, "\t", "        ");
            String key = getKey();
            if (key == null || key.isEmpty()) {
                setKey(s);
            } else {
                setKey(key + " | " + s);
            }
        }

        private boolean hasMathes() {
            String key = getKey();
            return !(key == null || key.isEmpty());
        }

        private void setResult(String toString) {
            setValue(toString);
        }

        @Override
        public String toString() {
            String key = getKey();
            String val = getValue();
            return ((key == null) ? "null" : key) + "->" + ((val == null) ? "null" : val);
        }

    }

    private boolean debug = false;

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}
