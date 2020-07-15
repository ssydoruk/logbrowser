/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.ScreenInfo;
import com.myutils.logbrowser.inquirer.gui.AColumnFilter.FilterComboboxModel;
import com.myutils.logbrowser.inquirer.gui.AColumnFilter.NumberFilter;
import static com.myutils.logbrowser.inquirer.gui.AColumnFilter.convertDouble;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author ssydoruk
 */
public class jdNumberFilter extends javax.swing.JDialog {

    public NumberFilter getNumberSelection() {
        return numberSelection;
    }

    private int closeCause = JOptionPane.CANCEL_OPTION;

    public int getCloseCause() {
        return closeCause;
    }
    private NumberFilter numberSelection = null;

    public void setCloseCause(int closeCause) {
        this.closeCause = closeCause;
    }

    /**
     * Creates new form jdNumberFilter
     */
    public jdNumberFilter(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE_KEY");
        getRootPane().getActionMap().put("ESCAPE_KEY", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent ae) {
                dispose();
            }
        }
        );

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        ScreenInfo.CenterWindow(this);

        FilterComboboxModel aModel = new FilterComboboxModel();
        aModel.add(new NumberFilter("Equal", false) {
            @Override
            boolean showValue(Object val) {
                try {
                    return (convertDouble(val) == getFirstNum());
                } catch (NumberFormatException numberFormatException) {
                }
                return isShowNonNumbers();
            }

        });
        aModel.add(new NumberFilter("Equal or less", false) {
            @Override
            boolean showValue(Object val) {
                try {
                    return (convertDouble(val) <= getFirstNum());
                } catch (NumberFormatException numberFormatException) {
                }
                return isShowNonNumbers();
            }
        });
        aModel.add(new NumberFilter("Less", false) {
            @Override
            boolean showValue(Object val) {
                try {
                    return (convertDouble(val) <= getFirstNum());
                } catch (NumberFormatException numberFormatException) {
                }
                return isShowNonNumbers();
            }
        });
        aModel.add(new NumberFilter("Greater or equal", false) {
            @Override
            boolean showValue(Object val) {
                try {
                    return (convertDouble(val) >= getFirstNum());
                } catch (NumberFormatException numberFormatException) {
                }
                return isShowNonNumbers();
            }
        });
        aModel.add(new NumberFilter("Greater", false) {
            @Override
            boolean showValue(Object val) {
                try {
                    return (convertDouble(val) > getFirstNum());
                } catch (NumberFormatException numberFormatException) {
                }
                return isShowNonNumbers();
            }
        });

        aModel.add(new NumberFilter("Between", true) {
            @Override
            boolean showValue(Object val) {
                try {
                    double l = convertDouble(val);
                    return (l >= getFirstNum() && l <= getSecondNum());
                } catch (NumberFormatException numberFormatException) {
                }
                return isShowNonNumbers();
            }
        });

        jrbHide.setSelected(true);

        jcbActionType.setModel(aModel);
        jcbActionType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox) e.getSource();
                NumberFilter selectedItem = (NumberFilter) cb.getSelectedItem();
                jtfSecondNumber.setEnabled(selectedItem.isNeedsSecond());
                jlAnd.setEnabled(selectedItem.isNeedsSecond());
            }
        });
        jcbActionType.setSelectedIndex(0);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jpWorkArea = new javax.swing.JPanel();
        jpSelection = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jcbActionType = new javax.swing.JComboBox<>();
        jPanel8 = new javax.swing.JPanel();
        jtfFirstNumber = new javax.swing.JFormattedTextField();
        jlAnd = new javax.swing.JLabel();
        jtfSecondNumber = new javax.swing.JFormattedTextField();
        jpOthers = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jrbShow = new javax.swing.JRadioButton();
        jrbHide = new javax.swing.JRadioButton();
        jpButtons = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jbOK = new javax.swing.JButton();
        jbCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Set number filter");
        setSize(new java.awt.Dimension(170, 170));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.Y_AXIS));

        jpWorkArea.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jpWorkArea.setLayout(new javax.swing.BoxLayout(jpWorkArea, javax.swing.BoxLayout.Y_AXIS));

        jpSelection.setBorder(javax.swing.BorderFactory.createTitledBorder("Selection"));
        jpSelection.setMinimumSize(new java.awt.Dimension(166, 56));
        jpSelection.setName(""); // NOI18N
        jpSelection.setLayout(new javax.swing.BoxLayout(jpSelection, javax.swing.BoxLayout.X_AXIS));

        jPanel7.add(jcbActionType);

        jpSelection.add(jPanel7);

        jPanel8.setLayout(new javax.swing.BoxLayout(jPanel8, javax.swing.BoxLayout.Y_AXIS));

        jtfFirstNumber.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("###0.###"))));
        jPanel8.add(jtfFirstNumber);

        jlAnd.setText("and");
        jlAnd.setFocusable(false);
        jPanel8.add(jlAnd);

        jtfSecondNumber.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter()));
        jPanel8.add(jtfSecondNumber);

        jpSelection.add(jPanel8);

        jpWorkArea.add(jpSelection);

        jpOthers.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel1.setText("If not numbers, than");
        jpOthers.add(jLabel1);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.Y_AXIS));

        jrbShow.setText("show");
        jPanel2.add(jrbShow);

        jrbHide.setText("hide");
        jPanel2.add(jrbHide);

        jpOthers.add(jPanel2);

        jpWorkArea.add(jpOthers);

        getContentPane().add(jpWorkArea);

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.LINE_AXIS));

        jbOK.setText("OK");
        jbOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbOKActionPerformed(evt);
            }
        });
        jPanel1.add(jbOK);

        jbCancel.setText("Cancel");
        jbCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbCancelActionPerformed(evt);
            }
        });
        jPanel1.add(jbCancel);

        javax.swing.GroupLayout jpButtonsLayout = new javax.swing.GroupLayout(jpButtons);
        jpButtons.setLayout(jpButtonsLayout);
        jpButtonsLayout.setHorizontalGroup(
            jpButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpButtonsLayout.createSequentialGroup()
                .addGap(0, 308, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jpButtonsLayout.setVerticalGroup(
            jpButtonsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jpButtonsLayout.createSequentialGroup()
                .addGap(0, 11, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        getContentPane().add(jpButtons);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jbCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbCancelActionPerformed
        dispose();
    }//GEN-LAST:event_jbCancelActionPerformed

    private void jbOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbOKActionPerformed
        setCloseCause(JOptionPane.OK_OPTION);
        numberSelection = (NumberFilter) jcbActionType.getSelectedItem();
        numberSelection.setFirstNum(jtfFirstNumber.getText());
        numberSelection.setSecondNum(jtfSecondNumber.getText());
        numberSelection.setShowNonNumbers(jrbShow.isSelected());
        dispose();
        // TODO add your handling code here:
    }//GEN-LAST:event_jbOKActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        getRootPane().setDefaultButton(jbOK);
        jbOK.requestFocus();
        pack();

    }//GEN-LAST:event_formWindowActivated

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);

        } catch (InstantiationException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
        } catch (IllegalAccessException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
        }
        //</editor-fold>

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                jdNumberFilter dialog = new jdNumberFilter(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.pack();
                dialog.setVisible(true);

                GraphicsDevice myDevice = dialog.getGraphicsConfiguration().getDevice();
                System.out.println("myDevice:" + myDevice);
                for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                    if (dialog.getLocation().getX() >= gd.getDefaultConfiguration().getBounds().getMinX()
                            && dialog.getLocation().getX() < gd.getDefaultConfiguration().getBounds().getMaxX()
                            && dialog.getLocation().getY() >= gd.getDefaultConfiguration().getBounds().getMinY()
                            && dialog.getLocation().getY() < gd.getDefaultConfiguration().getBounds().getMaxY()) {
                        myDevice = gd;
                        System.out.println("myDevice:" + myDevice);
                    }
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JButton jbCancel;
    private javax.swing.JButton jbOK;
    private javax.swing.JComboBox<String> jcbActionType;
    private javax.swing.JLabel jlAnd;
    private javax.swing.JPanel jpButtons;
    private javax.swing.JPanel jpOthers;
    private javax.swing.JPanel jpSelection;
    private javax.swing.JPanel jpWorkArea;
    private javax.swing.JRadioButton jrbHide;
    private javax.swing.JRadioButton jrbShow;
    private javax.swing.JFormattedTextField jtfFirstNumber;
    private javax.swing.JFormattedTextField jtfSecondNumber;
    // End of variables declaration//GEN-END:variables

    void setCurrentNumber(Object valueAt) {
        if (valueAt != null) {
            try {
                jtfFirstNumber.setText(new Double(convertDouble(valueAt)).toString());
            } catch (NumberFormatException numberFormatException) {
                jtfFirstNumber.setText("");
            }
        }
    }

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

}
