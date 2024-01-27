/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.myutils.logbrowser.inquirer.PrintStreams;
import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.StringWriter;

/**
 * @author Stepan
 */
public class ReportFrame extends javax.swing.JFrame implements Cloneable {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    static Insets zeroInsets = new Insets(1, 1, 1, 1);
    final protected ReportFrame theForm; // to close from Toolbar
    protected MyJTable tableView;
    /**
     * @param args the command line arguments
     */
    protected PrintStreams ps = null;
    JPanel reportArea;

    public MyJTable getTableView() {
        return tableView;
    }

    JScrollPane pane;
    JPanel toolbarPanel;
    TableSorter sorter;
    // Tool Bar
    private ToggleButtonToolBar toolbar = null;
    private final ButtonGroup toolbarGroup = new ButtonGroup();
    public ReportFrame(ReportFrame theForm) throws HeadlessException {
        this(theForm, true);
    }

    public ReportFrame(ReportFrame theForm, boolean isFullClone) throws HeadlessException {
        this();
        if (theForm != null) {
            setTitle(theForm.getTitle());
            this.tableView.copyData(theForm.tableView, isFullClone);
        }
    }

    public ReportFrame() {
        super();
        initComponents();

        setLayout(new BorderLayout());
        toolbarPanel = new JPanel(new BorderLayout());
        toolbar = new ToggleButtonToolBar();

        toolbarPanel.add(toolbar, BorderLayout.CENTER);

//        sorter = new TableSorter();
        tableView = new MyJTable();
//        tableView.getTca().setMaxColumnWidth(180);
//        tableView.tca.adjustColumns();

//        sorter.addMouseListenerToHeaderInTable(tableView);
//        JScrollPane scrollpane = new JScrollPane(tableView);
//        reportArea.add(tableView);
        pane = new JScrollPane(tableView, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(toolbarPanel, BorderLayout.PAGE_START);

        pane.setColumnHeader(new JViewport() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = 20;
                return d;
            }
        });

        add(pane, BorderLayout.CENTER);
//        ScreenInfo.CenterWindowMaxWidth(this);
        theForm = this;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
//                if (fullMsg != null) {
//                    fullMsg.dispose();
//                    fullMsg = null;
//                }
                super.windowClosing(e); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
//                if (fullMsg != null) {
//                    fullMsg.setVisible(true);
//                }
                super.windowGainedFocus(e); //To change body of generated methods, choose Tools | Templates.
            }

        });
    }

    public ReportFrame(PrintStreams ps) {
        this();
        setPrintStreams(ps);
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
    }

    public JButton addToolbarButton(String name, String hint, ActionListener act) {
        return toolbar.addButton(name, hint, act);
    }

    public JButton addToolbarButton(Action a) {
        return toolbar.add(a);
    }

    public JButton addToolbarButton(String name, String hint) {
        return toolbar.addButton(name, hint);
    }

    public void setPrintStreams(PrintStreams ps) {
        TabResultDataModel tabDataModel = ps.getTabDataModel();
        tableView.setModel(tabDataModel);
        tableView.setQueryParams(ps.getQueryName(), ps.getSearchString());
        tabDataModel.refreshTable();
    }

    protected void windowClose() {
        Gson gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .setPrettyPrinting()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        StringWriter sr = new StringWriter();
        gson.toJson(tableView.getModel(), sr);
        logger.info(sr.getBuffer());

        dispose();
        inquirer.logger.debug("window close");

    }

    protected void addToolbarButton(JToggleButton jToggleButton) {
        toolbar.add(jToggleButton);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 1124, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 552, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // TODO add your handling code here:
//        InputMap inputMap = tableView.getInputMap();
//        for (KeyStroke key : inputMap.keys()) {
//            getRootPane().getInputMap().put(key, inputMap.get(key));
//        }
//        getRootPane().setActionMap(tableView.getActionMap());
        tableView.requestFocusInWindow();
    }//GEN-LAST:event_formWindowOpened


    class ToolBarPanel extends JPanel implements ContainerListener {

        @Override
        public boolean contains(int x, int y) {
            Component c = getParent();
            if (c != null) {
                Rectangle r = c.getBounds();
                return (x >= 0) && (x < r.width) && (y >= 0) && (y < r.height);
            } else {
                return super.contains(x, y);
            }
        }

        @Override
        public void componentAdded(ContainerEvent e) {
            Container c = e.getContainer().getParent();
            if (c != null) {
                c.getParent().validate();
                c.getParent().repaint();
            }
        }

        @Override
        public void componentRemoved(ContainerEvent e) {
            Container c = e.getContainer().getParent();
            if (c != null) {
                c.getParent().validate();
                c.getParent().repaint();
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
