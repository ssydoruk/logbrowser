/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.ScreenInfo;
import Utils.TDateRange;
import Utils.UTCTimeRange;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;
import com.github.lgooddatepicker.optionalusertools.PickerUtilities;
import com.myutils.logbrowser.inquirer.DatabaseConnector;
import com.myutils.logbrowser.inquirer.gui.AColumnFilter.DateFilter;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * @author ssydoruk
 */
public class jdDateTimeFilter extends javax.swing.JDialog {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private UTCTimeRange timeRange;
    private DateFilter dateFilter;
    private int closeCause = JOptionPane.CANCEL_OPTION;
    private DateTimePicker dtFrom;
    private DateTimePicker dtTo;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btEarlyFirst;
    private javax.swing.JButton btEarlySecond;
    private javax.swing.JButton btLateFirst;
    private javax.swing.JButton btLateSecond;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JButton jbCancel;
    private javax.swing.JButton jbOK;
    private javax.swing.JComboBox<String> jcbSelectionType;
    private javax.swing.JLabel jlAnd;
    private javax.swing.JLabel jlEarliest;
    private javax.swing.JLabel jlLatest;
    private javax.swing.JPanel jpButtons;
    private javax.swing.JPanel jpFirstDate;
    private javax.swing.JPanel jpOthers;
    private javax.swing.JPanel jpRange;
    private javax.swing.JPanel jpSecondDate;
    private javax.swing.JPanel jpSelection;
    private javax.swing.JPanel jpWorkArea;
    private javax.swing.JRadioButton jrbHide;
    private javax.swing.JRadioButton jrbShow;
    /**
     * Creates new form jdDateTimeFilter
     */
    public jdDateTimeFilter(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        dtFrom = newPicker();
//        jpFirstDate.setLayout(new BoxLayout(jpFirstDate, BoxLayout.Y_AXIS));
        jpFirstDate.setLayout(new FlowLayout(FlowLayout.CENTER));
        jpFirstDate.add(dtFrom);

        dtTo = newPicker();
//        jpSecondDate.setLayout(new BoxLayout(jpSecondDate, BoxLayout.Y_AXIS));
        jpSecondDate.setLayout(new FlowLayout(FlowLayout.CENTER));
        jpSecondDate.add(dtTo);
        jrbHide.setSelected(true);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE_KEY");
        getRootPane().getActionMap().put("ESCAPE_KEY", new AbstractAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        dispose();
                    }
                }
        );

        AColumnFilter.FilterComboboxModel aModel = new AColumnFilter.FilterComboboxModel();

//<editor-fold defaultstate="collapsed" desc="Date comparison formulas">
        aModel.add(new AColumnFilter.DateFilter("Equal", false) {
            @Override
            boolean showValue(Object val) {
                long convertToTime = convertToTime(val);
                if (convertToTime > 0) {
                    return (chopMS(convertToTime) == chopMS(getFirstDate()));
                } else {
                    return isShowNonDate();
                }
            }

        });
        aModel.add(new AColumnFilter.DateFilter("Equal or earlier", false) {
            @Override
            boolean showValue(Object val) {
                long convertToTime = convertToTime(val);
//                inquirer.logger.debug(
//                        "val ["
//                        + val.toString()
//                        + "] convertToTime["
//                        + DatabaseConnector.dateToString(convertToTime)
//                        + "] chopMS convertToTime["
//                        + DatabaseConnector.dateToString(chopMS(convertToTime))
//                        + "] getFirstDate["
//                        + DatabaseConnector.dateToString(getFirstDate())
//                        + "] chopMS getFirstDate["
//                        + DatabaseConnector.dateToString(chopMS(getFirstDate()))
//                );
                if (convertToTime > 0) {
                    return (chopMS(convertToTime) <= chopMS(getFirstDate()));
                } else {
                    return isShowNonDate();
                }
            }
        });
        aModel.add(new AColumnFilter.DateFilter("Earlier", false) {
            @Override
            boolean showValue(Object val) {
                long convertToTime = convertToTime(val);
                if (convertToTime > 0) {
                    return (convertToTime < getFirstDate());
                } else {
                    return isShowNonDate();
                }
            }
        });
        aModel.add(new AColumnFilter.DateFilter("Later or equal", false) {
            @Override
            boolean showValue(Object val) {
                long convertToTime = convertToTime(val);
                if (convertToTime > 0) {
                    return (chopMS(convertToTime) >= chopMS(getFirstDate()));
                } else {
                    return isShowNonDate();
                }
            }
        });
        aModel.add(new AColumnFilter.DateFilter("Later", false) {
            @Override
            boolean showValue(Object val) {
                long convertToTime = convertToTime(val);
                if (convertToTime > 0) {
                    return (chopMS(convertToTime) > chopMS(getFirstDate()));
                } else {
                    return isShowNonDate();
                }
            }
        });
        aModel.add(new AColumnFilter.DateFilter("Between", true) {
            @Override
            boolean showValue(Object val) {
                long convertToTime = convertToTime(val);
                if (convertToTime > 0) {
                    return (chopMS(convertToTime) >= chopMS(getFirstDate()) && chopMS(convertToTime) <= chopMS(getSecondDate()));
                } else {
                    return isShowNonDate();
                }
            }
        });
//</editor-fold>
        jcbSelectionType.setModel(aModel);
        jcbSelectionType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                DateFilter selectedItem = (DateFilter) cb.getSelectedItem();
                dtTo.setEnabled(selectedItem.isNeedsSecond());
                jlAnd.setEnabled(selectedItem.isNeedsSecond());
                btEarlySecond.setEnabled(selectedItem.isNeedsSecond());
                btLateSecond.setEnabled(selectedItem.isNeedsSecond());
            }
        });
        jcbSelectionType.setSelectedIndex(0);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ScreenInfo.CenterWindow(this);
        pack();
    }

    private static DateTimePicker newPicker() {
        DateTimePicker dateTimePicker1 = new DateTimePicker();
        TimePickerSettings timeSettings = dateTimePicker1.getTimePicker().getSettings();
        timeSettings.setFormatForDisplayTime(PickerUtilities.createFormatterFromPatternString(
                "HH:mm:ss", timeSettings.getLocale()));
        timeSettings.setFormatForMenuTimes(PickerUtilities.createFormatterFromPatternString(
                "HH:mm:ss", timeSettings.getLocale()));
        return dateTimePicker1;
    }

    public int getCloseCause() {
        return closeCause;
    }

    public void setCloseCause(int closeCause) {
        this.closeCause = closeCause;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jpWorkArea = new javax.swing.JPanel();
        jpRange = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jlEarliest = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        btEarlyFirst = new javax.swing.JButton();
        btEarlySecond = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jlLatest = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        btLateFirst = new javax.swing.JButton();
        btLateSecond = new javax.swing.JButton();
        jpSelection = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jcbSelectionType = new javax.swing.JComboBox<>();
        jPanel8 = new javax.swing.JPanel();
        jpFirstDate = new javax.swing.JPanel();
        jlAnd = new javax.swing.JLabel();
        jpSecondDate = new javax.swing.JPanel();
        jpOthers = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jrbShow = new javax.swing.JRadioButton();
        jrbHide = new javax.swing.JRadioButton();
        jpButtons = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jbOK = new javax.swing.JButton();
        jbCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Date filter");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jpWorkArea.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jpWorkArea.setLayout(new javax.swing.BoxLayout(jpWorkArea, javax.swing.BoxLayout.Y_AXIS));

        jpRange.setBorder(javax.swing.BorderFactory.createTitledBorder("Current range"));
        jpRange.setLayout(new javax.swing.BoxLayout(jpRange, javax.swing.BoxLayout.Y_AXIS));

        jPanel5.setLayout(new javax.swing.BoxLayout(jPanel5, javax.swing.BoxLayout.LINE_AXIS));

        jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel2.setText("Earliest");
        jPanel4.add(jLabel2);

        jlEarliest.setFont(new java.awt.Font("Tahoma", 3, 11)); // NOI18N
        jPanel4.add(jlEarliest);

        jPanel5.add(jPanel4);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btEarlyFirst.setText("set first");
        btEarlyFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btEarlyFirstActionPerformed(evt);
            }
        });
        jPanel1.add(btEarlyFirst);

        btEarlySecond.setText("set second");
        btEarlySecond.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btEarlySecondActionPerformed(evt);
            }
        });
        jPanel1.add(btEarlySecond);

        jPanel5.add(jPanel1);

        jpRange.add(jPanel5);

        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.LINE_AXIS));

        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel3.setText("Latest");
        jPanel9.add(jLabel3);

        jlLatest.setFont(new java.awt.Font("Tahoma", 3, 11)); // NOI18N
        jPanel9.add(jlLatest);

        jPanel6.add(jPanel9);

        jPanel10.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btLateFirst.setText("set first");
        btLateFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btLateFirstActionPerformed(evt);
            }
        });
        jPanel10.add(btLateFirst);

        btLateSecond.setText("set second");
        btLateSecond.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btLateSecondActionPerformed(evt);
            }
        });
        jPanel10.add(btLateSecond);

        jPanel6.add(jPanel10);

        jpRange.add(jPanel6);

        jpWorkArea.add(jpRange);

        jpSelection.setBorder(javax.swing.BorderFactory.createTitledBorder("Selection"));
        jpSelection.setLayout(new javax.swing.BoxLayout(jpSelection, javax.swing.BoxLayout.LINE_AXIS));

        jPanel7.add(jcbSelectionType);

        jpSelection.add(jPanel7);

        jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.GroupLayout jpFirstDateLayout = new javax.swing.GroupLayout(jpFirstDate);
        jpFirstDate.setLayout(jpFirstDateLayout);
        jpFirstDateLayout.setHorizontalGroup(
                jpFirstDateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 377, Short.MAX_VALUE)
        );
        jpFirstDateLayout.setVerticalGroup(
                jpFirstDateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 46, Short.MAX_VALUE)
        );

        jPanel8.add(jpFirstDate);

        jlAnd.setText("and");
        jlAnd.setFocusable(false);
        jPanel8.add(jlAnd);

        javax.swing.GroupLayout jpSecondDateLayout = new javax.swing.GroupLayout(jpSecondDate);
        jpSecondDate.setLayout(jpSecondDateLayout);
        jpSecondDateLayout.setHorizontalGroup(
                jpSecondDateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 377, Short.MAX_VALUE)
        );
        jpSecondDateLayout.setVerticalGroup(
                jpSecondDateLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 46, Short.MAX_VALUE)
        );

        jPanel8.add(jpSecondDate);

        jpSelection.add(jPanel8);

        jpWorkArea.add(jpSelection);

        jpOthers.setLayout(new javax.swing.BoxLayout(jpOthers, javax.swing.BoxLayout.LINE_AXIS));

        jLabel1.setText("If not date, than");
        jPanel11.add(jLabel1);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        buttonGroup1.add(jrbShow);
        jrbShow.setText("show");
        jPanel2.add(jrbShow);

        buttonGroup1.add(jrbHide);
        jrbHide.setText("hide");
        jPanel2.add(jrbHide);

        jPanel11.add(jPanel2);

        jpOthers.add(jPanel11);

        jpWorkArea.add(jpOthers);

        getContentPane().add(jpWorkArea);

        jpButtons.setLayout(new javax.swing.BoxLayout(jpButtons, javax.swing.BoxLayout.LINE_AXIS));

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jbOK.setText("OK");
        jbOK.setMaximumSize(new java.awt.Dimension(67, 23));
        jbOK.setMinimumSize(new java.awt.Dimension(67, 23));
        jbOK.setPreferredSize(new java.awt.Dimension(67, 23));
        jbOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbOKActionPerformed(evt);
            }
        });
        jPanel3.add(jbOK);

        jbCancel.setText("Cancel");
        jbCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbCancelActionPerformed(evt);
            }
        });
        jPanel3.add(jbCancel);

        jpButtons.add(jPanel3);

        getContentPane().add(jpButtons);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jbCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbCancelActionPerformed
        dispose();
    }//GEN-LAST:event_jbCancelActionPerformed

    private void jbOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOKActionPerformed
        setCloseCause(JOptionPane.OK_OPTION);
        dateFilter = (DateFilter) jcbSelectionType.getSelectedItem();
        dateFilter.setFirstDate(TDateRange.getUTCTime(dtFrom, 0));
        long utcTime = 0;
        try {
            utcTime = TDateRange.getUTCTime(dtTo, 1);
        } catch (Exception e) {
        }
        dateFilter.setSecondDate(utcTime);
        dateFilter.setShowNonDate(jrbShow.isSelected());

        dispose();
        // TODO add your handling code here:
    }//GEN-LAST:event_jbOKActionPerformed

    public DateFilter getDateFilter() {
        return dateFilter;
    }

    private void btEarlyFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btEarlyFirstActionPerformed
        TDateRange.setTimeRange(dtFrom, timeRange.getStart());
    }//GEN-LAST:event_btEarlyFirstActionPerformed

    private void btEarlySecondActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btEarlySecondActionPerformed
        TDateRange.setTimeRange(dtTo, timeRange.getStart());
    }//GEN-LAST:event_btEarlySecondActionPerformed

    private void btLateFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btLateFirstActionPerformed
        TDateRange.setTimeRange(dtFrom, timeRange.getEnd());
    }//GEN-LAST:event_btLateFirstActionPerformed

    private void btLateSecondActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btLateSecondActionPerformed
        TDateRange.setTimeRange(dtTo, timeRange.getEnd());
    }//GEN-LAST:event_btLateSecondActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        getRootPane().setDefaultButton(jbOK);
        jbOK.requestFocus();
        pack();
    }//GEN-LAST:event_formWindowActivated
    // End of variables declaration//GEN-END:variables

    void setTimeRange(UTCTimeRange timeRange) {
        this.timeRange = timeRange;
        jlEarliest.setText((timeRange.getStart() > 0) ? DatabaseConnector.dateToString(timeRange.getStart()) : "<NONE>");
        jlLatest.setText((timeRange.getEnd() > 0) ? DatabaseConnector.dateToString(timeRange.getEnd()) : "<NONE>");
    }

    void setCurrentCell(Object valueAt) {
        long l = DatabaseConnector.convertToTime(valueAt);
        if (l > 0) {
            TDateRange.setTimeRange(dtFrom, l);
        }

    }

}
