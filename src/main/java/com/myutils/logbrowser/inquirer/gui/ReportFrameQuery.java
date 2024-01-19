/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.ScreenInfo;
import com.myutils.logbrowser.inquirer.*;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * @author ssydoruk
 */
public class ReportFrameQuery extends ReportFrame {

    protected ShowFullMessage fullMsg;
    private JCheckBox msgCheckBox;
    private JPopupMenu clonePopup;
    private JPopupMenu placementPopup;
    private JButton btPlacement;
    private int fullPlacement = SwingConstants.TOP;
    private javax.swing.ButtonGroup bgPlacement;
    

    public ReportFrameQuery(ReportFrameQuery frm) {
        this(frm, true);
    }

    public ReportFrameQuery(ReportFrameQuery frm, boolean isFullClone) {
        super(frm, isFullClone);
        bgPlacement = new ButtonGroup();
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
                super.windowIconified(e); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                showFullMsg(true);
                inquirer.logger.debug("windowDeiconified");
                super.windowDeiconified(e); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void windowActivated(WindowEvent e) {
                showFullMsg(true);
                inquirer.logger.debug("windowActivated");

                super.windowActivated(e); //To change body of generated methods, choose Tools | Templates.
            }

        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                showFullMsg(true);
                inquirer.logger.debug("componentShown");
                super.componentShown(e); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                showFullMsg(false);
                inquirer.logger.debug("componentHidden");
                super.componentHidden(e); //To change body of generated methods, choose Tools | Templates.
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

        this.msgCheckBox = new JCheckBox(new ShowMsgAction());
        addPlacementButton();

        addToolbarButton(msgCheckBox);

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
        btPlacement = addToolbarButton("Full placement", "Placement of full message window");

        placementPopup = new JPopupMenu();

        JRadioButtonMenuItem mi = new JRadioButtonMenuItem(new AbstractAction("Top") {
            public void actionPerformed(ActionEvent e) {
                fullPlacement=SwingConstants.TOP;
                showFull();
            }
        });
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
        
        mi=new JRadioButtonMenuItem(new AbstractAction("Left") {
            public void actionPerformed(ActionEvent e) {
                fullPlacement = SwingConstants.LEFT;
                showFull();
            }
        });
        bgPlacement.add(mi);
        placementPopup.add(mi);
        
        mi = new JRadioButtonMenuItem(new AbstractAction("Right") {
            public void actionPerformed(ActionEvent e) {
                fullPlacement = SwingConstants.RIGHT;
                showFull();
            }
        });
        bgPlacement.add(mi);
        placementPopup.add(mi);
        

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
        if (fullMsg != null) {
            fullMsg.setTitle("Full message for [" + title + "]");
        }
        super.setTitle(title); //To change body of generated methods, choose Tools | Templates.
    }

    private void showFullMsg(boolean b) {
        if (fullMsg != null && msgCheckBox.isSelected()) {
            fullMsg.setVisible(b);
        }
    }

    private void theWindowClosed(WindowEvent e) {
        if (fullMsg != null) {
            fullMsg.dispose();
        }
    }

    public void doShow(boolean shouldFocus) {
        setAutoRequestFocus(shouldFocus);
        toFront();
        setVisible(true);
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

//        pack();
    }

    private void LoadShort() {
        TabResultDataModel mod = (TabResultDataModel) tableView.getModel();
        ExternalEditor.editFiles(mod.exportShort(tableView));
    }

    private void LoadExcel() {
        TabResultDataModel mod = (TabResultDataModel) tableView.getModel();
        try {
            Excel.reportEdit(mod.exportToExcel(tableView));
//            Excel.aggregateEdit(mod.exportToExcel());
        } catch (IOException ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
    }

    private void LoadFull() {
        TabResultDataModel mod = (TabResultDataModel) tableView.getModel();
        if (tableView.canPrintFull(mod)) {
//        ExternalEditor.editFiles(mod.getFullFileNames());
            ExternalEditor.editFiles(mod.exportFull(tableView));
        }
    }

    private void OpenNotepad() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void OpenSearch() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void printConsole() {
        ps.printMemConsole();
    }

    void fullMsgClosed() {
        if (msgCheckBox.isSelected()) {
            msgCheckBox.setSelected(false);
        }
    }

    class ShowMsgAction extends AbstractAction {

        public ShowMsgAction() {
            super("Message");
            putValue(Action.SHORT_DESCRIPTION, "Toggle full message view window");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JToggleButton tBtn = (JToggleButton) e.getSource();
            if (tBtn.isSelected()) {
                showFull();

            } else {
                fullMsg.setVisible(false);
            }
            btPlacement.setEnabled(tBtn.isSelected());
            
        }

    }
    
    void showFull(){
        ScreenInfo.refitMainToMsg(theForm, fullMsg, fullPlacement);
        fullMsg.setVisible(true);
        fullMsg.toFront();
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                theForm.toFront();
                theForm.repaint();
            }
        });
        
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
//            putValue(Action.SELECTED_KEY, tableView.isFollowLog());

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
//            putValue(Action.SELECTED_KEY, tableView.isFollowLog());

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
//            putValue(Action.SELECTED_KEY, tableView.isFollowLog());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tableView.setFollowLog(((JToggleButton) e.getSource()).isSelected());
        }
    }

}
