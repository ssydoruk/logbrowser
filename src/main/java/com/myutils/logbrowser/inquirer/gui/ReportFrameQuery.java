/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.ScreenInfo;
import com.myutils.logbrowser.inquirer.*;

import static java.awt.Frame.MAXIMIZED_BOTH;

import java.awt.GraphicsEnvironment;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Enumeration;

/**
 * @author ssydoruk
 */
public class ReportFrameQuery extends ReportFrame {

    protected ShowFullMessage fullMsg;
    private JPopupMenu clonePopup;
    private JPopupMenu placementPopup;
    private JButton btPlacement;
    private int fullPlacement = 0; // no full window initially
    private javax.swing.ButtonGroup bgPlacement;
    private final ReportFrameQuery frm;
    private JRadioButtonMenuItem miFullOff;

    public ReportFrameQuery(ReportFrameQuery frm) {
        this(frm, true);
    }

    public ReportFrameQuery(ReportFrameQuery frm, boolean isFullClone) {
        super(frm, isFullClone);
        this.frm = frm;

        createToolbar();
        this.fullMsg = new ShowFullMessage(this);
        tableView.setFullMsg(fullMsg);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                inquirer.logger.debug("windowClosed");
                theWindowClosed(e);
            }

            @Override
            public void windowIconified(WindowEvent e) {
                showFullMsg(false);
                inquirer.logger.debug("windowIconified");
                super.windowIconified(e); // To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                showFullMsg(true);
                inquirer.logger.debug("windowDeiconified");
                super.windowDeiconified(e); // To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void windowActivated(WindowEvent e) {
                showFullMsg(true);

                super.windowActivated(e); // To change body of generated methods, choose Tools | Templates.
            }

        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                showFullMsg(true);
                inquirer.logger.debug("componentShown");
                super.componentShown(e); // To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                showFullMsg(false);
                inquirer.logger.debug("componentHidden");
                super.componentHidden(e); // To change body of generated methods, choose Tools | Templates.
            }

        });
    }

    public ReportFrameQuery() {
        this((ReportFrameQuery) null);
    }

    public ReportFrameQuery(PrintStreams ps) {
        this();
        setPrintStreams(ps);
    }

    @Override
    protected ReportFrame clone() throws CloneNotSupportedException {
        ReportFrameQuery ret = (ReportFrameQuery) super.clone();
        ret.createToolbar();
        return ret;
    }

    private void createToolbar() {
        addToolbarButton("Close", "Close this report", (new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                theWindowClosed(null);
                windowClose();
            }

        }));

        addToolbarButton("Full", "Load full messages into editor", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoadFull();
            }

        });

        addPlacementButton();

        addToolbarButton(new CopyAction());
        addToolbarButton(new FilterAction());
        addToolbarButton(new JCheckBox(new FollowSourceAction()));

        addToolbarButton("Excel", "Open the report in Excel", (new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoadExcel();
            }

        }));

        addCloneButton();
        addToolbarButton(new InfoAction());

    }

    private void addPlacementButton() {
        bgPlacement = new ButtonGroup();
        btPlacement = addToolbarButton("Full message", "Show/hide window with full log message");

        placementPopup = new JPopupMenu();
        JRadioButtonMenuItem mi;

        mi = new JRadioButtonMenuItem(new AbstractAction("Right") {
            public void actionPerformed(ActionEvent e) {
                fullPlacement = SwingConstants.RIGHT;
                showFull();
            }
        });
        // mi.setModel(new MenuItemModel(SwingConstants.RIGHT));
        bgPlacement.add(mi);
        placementPopup.add(mi);

        mi = new JRadioButtonMenuItem(new AbstractAction("Left") {
            public void actionPerformed(ActionEvent e) {
                fullPlacement = SwingConstants.LEFT;
                showFull();
            }
        });
        // mi.setModel(new MenuItemModel(SwingConstants.LEFT));
        bgPlacement.add(mi);
        placementPopup.add(mi);

        mi = new JRadioButtonMenuItem(new AbstractAction("Top") {
            public void actionPerformed(ActionEvent e) {
                fullPlacement = SwingConstants.TOP;
                showFull();
            }
        });
        // mi.setModel(new MenuItemModel(SwingConstants.TOP));
        bgPlacement.add(mi);
        placementPopup.add(mi);

        mi = new JRadioButtonMenuItem(new AbstractAction("Bottom") {
            public void actionPerformed(ActionEvent e) {
                fullPlacement = SwingConstants.BOTTOM;
                showFull();
            }

        });
        bgPlacement.add(mi);
        placementPopup.add(mi);
        // mi.setModel(new MenuItemModel(SwingConstants.BOTTOM));

        miFullOff = new JRadioButtonMenuItem(new AbstractAction("Off") {
            public void actionPerformed(ActionEvent e) {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        fullMsg.setVisible(false);
                        setExtendedState(JFrame.NORMAL);
                        setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                });

            }
        });
        // mi.setModel(new MenuItemModel(0));
        bgPlacement.add(miFullOff);
        placementPopup.add(miFullOff);
        miFullOff.setSelected(true);

        // choosePlacementButton();
        btPlacement.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                placementPopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });

    }

    private void addCloneButton() {
        JButton bt = addToolbarButton("Clone", "Duplicate current report window");

        clonePopup = new JPopupMenu();
        clonePopup.add(new JMenuItem(new AbstractAction("Full unfiltered") {
            public void actionPerformed(ActionEvent e) {
                ReportFrameQuery ch = new ReportFrameQuery((ReportFrameQuery) theForm);
                ch.tableView.getTca().adjustColumns();
                ch.setVisible(true);
            }
        }));
        clonePopup.add(new JMenuItem(new AbstractAction("Current filtered") {
            public void actionPerformed(ActionEvent e) {
                ReportFrameQuery ch = new ReportFrameQuery((ReportFrameQuery) theForm, false);
                ch.tableView.getTca().adjustColumns();
                ch.setVisible(true);
            }
        }));

        bt.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                clonePopup.show(e.getComponent(), e.getX(), e.getY());
            }
        });

    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title); // To change body of generated methods, choose Tools | Templates.
    }

    private void showFullMsg(boolean b) {
        if (fullMsg != null) {
            fullMsg.setTitle("Full message for [" + getTitle() + "]");
            fullMsg.setVisible(!miFullOff.isSelected());
        }
    }

    private void theWindowClosed(WindowEvent e) {
        if (fullMsg != null) {
            fullMsg.dispose();
        }
    }

    public void doShow(boolean shouldFocus) {
        setAutoRequestFocus(shouldFocus);
        setVisible(true);
        toFront();
        if (fullMsg != null && fullMsg.isVisible()) {
            if (getTableView().getSelectedRow() < 0)
                fullMsg.showMessage(null, null);
            else
                fullMsg.doShowMessage();
        }
    }

    public void showReport(IQueryResults qry, QueryDialog qd, SelectionType selType, String searchID) throws Exception {
        inquirer inq = inquirer.getInq();
        InquirerCfg cfg = inquirer.getConfig();
        ps = new PrintStreams(cfg);
        ps.addMemoryPrint();
        String theTitle;
        qry.reset();
        qry.setFormatters(inq.getFormatters());
        qry.setSuppressConfirms(true);
        qry.doRetrieve(qd, selType, searchID);
        qry.doPrint(ps);
        try {
            String reportSummary = qry.getReportSummary().replaceAll("\n", " ");
            theTitle = reportSummary;
        } catch (Exception e) {
            theTitle = qry.getName();
        }
        setTitle(theTitle);
        setPrintStreams(ps);

        // pack();
    }

    private void LoadExcel() {
        TabResultDataModel mod = (TabResultDataModel) tableView.getModel();
        try {
            Excel.reportEdit(mod.exportToExcel(tableView));
            // Excel.aggregateEdit(mod.exportToExcel());
        } catch (IOException ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
    }

    private void LoadFull() {
        TabResultDataModel mod = (TabResultDataModel) tableView.getModel();
        if (tableView.canPrintFull(mod)) {
            // ExternalEditor.editFiles(mod.getFullFileNames());
            ExternalEditor.editFiles(mod.exportFull(tableView));
        }
    }

    void fullMsgClosed() {
        miFullOff.setSelected(true);
    }

    void showFull() {
        fullMsg.doShowMessage();
        fullMsg.setVisible(true);
        fullMsg.toFront();
        ScreenInfo.refitMainToMsg(theForm, fullMsg, fullPlacement);
        // java.awt.EventQueue.invokeLater(new Runnable() {
        //     @Override
        //     public void run() {
        //         theForm.toFront();
        //         theForm.repaint();
        //     }
        // });

    }

    class FollowSourceAction extends AbstractAction {

        public FollowSourceAction() {
            super("Follow source");
            putValue(Action.SHORT_DESCRIPTION, "Toggle row change log jumping");
            putValue(Action.SELECTED_KEY, tableView.isFollowLog());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tableView.setFollowLog(((JToggleButton) e.getSource()).isSelected());
        }
    }

    class InfoAction extends AbstractAction {

        public InfoAction() {
            super("Info");
            putValue(Action.SHORT_DESCRIPTION, "Show report statistics");
            // putValue(Action.SELECTED_KEY, tableView.isFollowLog());

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tableView.showInfo();
        }

    }

    class CopyAction extends AbstractAction {

        public CopyAction() {
            super("Copy");
            putValue(Action.SHORT_DESCRIPTION, "Copy to clipboard");
            // putValue(Action.SELECTED_KEY, tableView.isFollowLog());

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tableView.setFollowLog(((JToggleButton) e.getSource()).isSelected());
        }
    }

    class FilterAction extends AbstractAction {

        public FilterAction() {
            super("Filter");
            putValue(Action.SHORT_DESCRIPTION, "Filter records");
            // putValue(Action.SELECTED_KEY, tableView.isFollowLog());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tableView.setFollowLog(((JToggleButton) e.getSource()).isSelected());
        }
    }

}
