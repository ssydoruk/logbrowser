/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.Pair;
import com.myutils.logbrowser.indexer.Main;
import com.myutils.logbrowser.inquirer.InquirerFileIo;
import com.myutils.logbrowser.inquirer.MsgType;
import com.myutils.logbrowser.inquirer.SearchExtract;
import com.myutils.logbrowser.inquirer.gui.TabResultDataModel.ColumnParam;
import com.myutils.logbrowser.inquirer.inquirer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Stepan
 */
public class MyJTable extends JTableCommon {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private String queryName = null;
    private final ShowInfo infoAction;
    private final JMenuItem hideColumnRTMenu;

    public ShowInfo getInfoAction() {
        return infoAction;
    }

    public String getQueryName() {
        return queryName;
    }

    public String getSearchString() {
        return searchString;
    }
    private String searchString = null;

    void setMaxColumnWidth(int i) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void callingPopup() {
        super.callingPopup(); //To change body of generated methods, choose Tools | Templates.
        jmAddFields.removeAll();
        if (getModel() instanceof TabResultDataModel) {
            TabResultDataModel model = (TabResultDataModel) getModel();
            if (popupRow < 0) {
                popupRow = 0;
            }
            MsgType rowType = model.getRowType(popupRow);
            if (rowType != null) {
                hideColumnRTMenu.setText("Hide this column for \"" + rowType.toString() + "\"");
                jmAddFields.setText("Hidden columns for \"" + rowType.toString() + "\"");

                ArrayList<ColumnParam> hidden = model.getHidden(rowType);
                int menusAdded = 0;
                if (hidden != null) {
                    for (ColumnParam cp : hidden) {
                        menusAdded++;
                        jmAddFields.add(new AbstractAction(cp.menuName()) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                model.unHideColumn(popupRow, cp);
                                model.refreshTable();
                            }
                        }
                        );
                    }
                }

                boolean hasHidden = model.hasHidden(popupRow);
                jmAddFields.setVisible(hasHidden);
                if (hasHidden) {
                    jmAddFields.add(new JPopupMenu.Separator());
                    AbstractAction action = new AbstractAction("Show all") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (hidden != null) {
                                for (ColumnParam cp : hidden) {
                                    model.unHideColumn(popupRow, cp);
                                }
                                model.refreshTable();
                            }
                        }
                    };
                    boolean eatenMenu = menusAdded < model.numberStdHidden(popupRow);
                    action.setEnabled(!eatenMenu);
                    jmAddFields.add(action);

                    action = new AbstractAction("Restore default") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            model.restoreHidden(popupRow);
                            model.refreshTable();
                        }
                    };

                    action.setEnabled(eatenMenu);
                    jmAddFields.add(action);
                }

            }
        } else {
            jmAddFields.setVisible(false);
        }

    }

    @Override
    void theMousePressed(MouseEvent e) {
//            inquirer.logger.trace("pressed");
        if (e.getClickCount() == 2) { // on doubleclick open in editor
            MyJTable table = (MyJTable) e.getSource();
            int row = table.rowAtPoint(e.getPoint());
            int column = table.columnAtPoint(e.getPoint());
            TabResultDataModel mod = (TabResultDataModel) table.getModel();

            if (row >= 0) {
                TabResultDataModel.TableRow tableRow = mod.getRow(table.convertRowIndexToModel(row));
                try {
                    try {
                        if (!ExternalEditor.getEditor().jumpToFile(tableRow.getFileName(), tableRow.getLine())) {
                            JOptionPane.showMessageDialog(getParent(), "Cannot jump to file [" + tableRow.getFileName() + "]", "Error opening file in VIM", ERROR_MESSAGE);
                        }
                    } catch (com.jacob.com.ComFailException ex) {
                        logger.log(org.apache.logging.log4j.Level.FATAL, ex);

                        JOptionPane.showMessageDialog(getParent(), "Exception loading file [" + tableRow.getFileName() + "]: \n"
                                + ex.getMessage() + "\n\nCheck that you have gvim installed or get one (http://www.vim.org/)", "Error opening file in VIM", ERROR_MESSAGE);
                    }
                } catch (IOException ex) {
                    inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
                }
            }
        } else {
//                inquirer.logger.debug("ff");
////                if( fullMsg.isVisible()){
//                TabResultDataModel.TableRow tableRow = mod.getRow(row);
//                try {
//                    fullMsg.showMessage(InquirerFileIo.GetFileBytes(tableRow.getFileName(), tableRow.getOffset(), tableRow.getFileBytes()));
//                } catch (Exception ex) {
//                    java.util.logging.logger.log(org.apache.logging.log4j.Level.FATAL, ex);
////                    }
//                }
        }
    }

    boolean canPrintFull(TabResultDataModel mod) {
        int totalBytes = 0;
        for (int i = 0; i < getRowCount(); i++) {
            TabResultDataModel.TableRow row = mod.getRow(convertRowIndexToModel(i));
            totalBytes += row.getFileBytes();
        }

        if (totalBytes >= inquirer.getConfig().getFileSizeWarn() * 1024 * 1024) {
            int showConfirmDialog = inquirer.showConfirmDialog(null, "File size for full report is big (" + Main.formatSize(totalBytes) + ")\n"
                    + "Should files be printed?", "Please confirm", JOptionPane.YES_NO_OPTION);
            if (showConfirmDialog == JOptionPane.NO_OPTION) {
                return false;
            }
        }
        return true;
    }

    void setQueryParams(String queryName, String searchString) {
        this.queryName = queryName;
        this.searchString = searchString;
    }

    private void changeHiddenState(boolean isHidden) {
        showAllColumns.setEnabled(isHidden);

    }

    void copyData(MyJTable tableView) {
        setModel(new TabResultDataModel((TabResultDataModel) tableView.getModel()));
    }

    class CopyRecord extends AbstractAction {

        public CopyRecord() {
            super("Copy log record");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            MyJTable table = (MyJTable) popup.getInvoker();
            TabResultDataModel model = (TabResultDataModel) table.getModel();
            SystemClipboard.copy(model.getLogRecord(table.convertRowIndexToModel(popupRow)));
        }

    }

    class OpenNotepad extends AbstractAction {

        public OpenNotepad() {
            super("Notepad++");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getModel() instanceof TabResultDataModel) {

                TabResultDataModel model = (TabResultDataModel) getModel();
                ExternalEditor.openNotepad(model.getRow(popupRow).getFileName().getFileName(), model.getRow(popupRow).getLine());
            }
        }

    }

    class OpenTextPad extends AbstractAction {

        public OpenTextPad() {
            super("Textpad");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (getModel() instanceof TabResultDataModel) {

                TabResultDataModel model = (TabResultDataModel) getModel();
                ExternalEditor.openTextpad(model.getRow(popupRow).getFileName().getFileName(), model.getRow(popupRow).getLine());
            }
        }

    }

    class HideColumn extends AbstractAction {

        public HideColumn() {
            super("Hide whole column");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            MyJTable table = (MyJTable) popup.getInvoker();
            table.removeColumn(table.getColumnModel().getColumn(popupCol));
            changeHiddenState(true);

        }
    }

    class HideColumnRecordType extends AbstractAction {

        public HideColumnRecordType() {
            super();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            MyJTable table = (MyJTable) popup.getInvoker();
            TabResultDataModel model = (TabResultDataModel) table.getModel();
            model.tempHide(table.convertRowIndexToModel(popupRow), table.convertColumnIndexToModel(popupCol));
            model.tempHide(popupRow, popupCol);
            model.refreshTable();
            changeHiddenState(true);

        }
    }

    class ShowAllColumns extends AbstractAction {

        public ShowAllColumns() {
            super("Show all columns");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            MyJTable table = (MyJTable) popup.getInvoker();
            TabResultDataModel model = (TabResultDataModel) table.getModel();
//            table.setAutoCreateColumnsFromModel(false);
//            table.setAutoCreateColumnsFromModel(true);
            changeHiddenState(false);
            model.unhideTemps();
            model.refreshTable();

//            table.createDefaultColumnModel();
        }

    }

    class ShowMessage extends AbstractAction {

        public ShowMessage() {
            super("Show message");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            MyJTable table = (MyJTable) popup.getInvoker();
            TabResultDataModel model = (TabResultDataModel) table.getModel();
//            table.setAutoCreateColumnsFromModel(false);
//            table.setAutoCreateColumnsFromModel(true);
            changeHiddenState(false);
            model.refreshTable();
//            table.createDefaultColumnModel();
        }

    }

    class AppendRecord extends AbstractAction {

        public AppendRecord() {
            super("Append log record");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            MyJTable table = (MyJTable) popup.getInvoker();
            TabResultDataModel model = (TabResultDataModel) table.getModel();
            SystemClipboard.append(model.getLogRecord(table.convertRowIndexToModel(popupRow)));
        }

    }

    class CopyAllRecords extends AbstractAction {

        public CopyAllRecords() {
            super("Copy all (full from log files)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            MyJTable table = (MyJTable) popup.getInvoker();
            TabResultDataModel model = (TabResultDataModel) table.getModel();
            try {
                StringBuilder buf = model.getAllRecords(table);
                SystemClipboard.copy(buf.toString());
            } catch (Exception ex) {
                inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
            }
        }

    }

    class CopyAllShortRecords extends AbstractAction {

        public CopyAllShortRecords() {
            super("Copy all (short Excel format)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            MyJTable table = (MyJTable) popup.getInvoker();
            TabResultDataModel model = (TabResultDataModel) table.getModel();
            StringBuilder buf;
            try {
//                buf = model.getAllRecordsShort(table);
                buf = model.getAllRecordsShort(table, null, "\t", false);
                SystemClipboard.copy(buf.toString());
            } catch (IOException ex) {
                inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
            }
        }

    }

    class CopyAllJiraRecords extends AbstractAction {

        public CopyAllJiraRecords() {
            super("Copy all (Jira table format)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            MyJTable table = (MyJTable) popup.getInvoker();
            TabResultDataModel model = (TabResultDataModel) table.getModel();
            StringBuilder buf;
            try {
                buf = model.getAllRecordsShort(table, "||", "|", false);
                SystemClipboard.copy(buf.toString());
            } catch (IOException ex) {
                inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
            }
        }

    }

    class ListSelectionChanged implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                DefaultListSelectionModel sm = (DefaultListSelectionModel) e.getSource();
                inquirer.logger.trace("Selection Changed" + sm.toString() + "");
                int row = sm.getMinSelectionIndex();
                if (row >= 0) {
                    MyJTable table = MyJTable.this;
                    TabResultDataModel.TableRow tableRow = ((TabResultDataModel) table.getModel()).getRow(table.convertRowIndexToModel(row));
                    MyJTable.this.repaint();
                    try {
                        fullMsg.showMessage(InquirerFileIo.GetFileBytes(tableRow.getFileName(), tableRow.getOffset(), tableRow.getFileBytes()));
                        if (followLog) {
                            ExternalEditor.getEditor().jumpToFile(tableRow.getFileName(), tableRow.getLine(), false);
                        }
                    } catch (Exception ex) {
                        inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
//                    }
                    }
                }
            }
        }
    }

    JMenuItem deleteItem;

    public ShowFullMessage getFullMsg() {
        return fullMsg;
    }

    public void setFullMsg(ShowFullMessage fullMsg) {
        this.fullMsg = fullMsg;
        getSelectionModel().addListSelectionListener(new ListSelectionChanged());

    }
    protected ShowFullMessage fullMsg;
    private boolean followLog = false;

    public boolean isFollowLog() {
        return followLog;
    }

    public void setFollowLog(boolean followLog) {
        this.followLog = followLog;
    }
    HideColumn hideColumn;
    ShowAllColumns showAllColumns;
    ShowMessage showMessage;

    ArrayList<AbstractAction> copyMenus = new ArrayList();
    ArrayList<AbstractAction> filterMenus = new ArrayList();

    JMenu jmAddFields;
    JMenu jmOpenIn;

    public MyJTable() {
        super();

//       sorter = new TableRowSorter<TabResultDataModel>((TabResultDataModel) getModel());
//        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//        setTableHeader(null);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        setCellSelectionEnabled(true);
        setRowSelectionAllowed(true);

//        deleteItem = new JMenuItem("Delete");
//
//        deleteItem.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                Component c = (Component) e.getSource();
//                JPopupMenu popup = (JPopupMenu) c.getParent();
//                JTable table = (JTable) popup.getInvoker();
//                inquirer.logger.debug(table.getSelectedRow() + " : " + table.getSelectedColumn());
//            }
//        });
//        popupMenu.add(deleteItem);
////        JCheckBoxMenuItem followSource = new JCheckBoxMenuItem("Follow source");
////        followSource.setSelected(followLog);
////        followSource.addActionListener(new ActionListener() {
////            @Override
////            public void actionPerformed(ActionEvent e) {
////                followLog = ((JCheckBoxMenuItem) e.getSource()).getModel().isSelected();
////
////            }
////        });
//        popupMenu.add(followSource);
//        setComponentPopupMenu(popupMenu);
        Font font = this.getFont();

        setDefaultRenderer(Object.class, new CustomTableCellRenderer(new Font(font.getName(), Font.BOLD, font.getSize())));
//        showMessage = new ShowMessage();
//        popupMenu.insert(showMessage, copyItemsIdx);

//        popupMenu.insert(new SearchCell(this), copyItemsIdx);
        popupMenu.insert(new RegexField(this), copyItemsIdx);

        jmAddFields = new JMenu();
        popupMenu.insert(jmAddFields, copyItemsIdx);
        showAllColumns = new ShowAllColumns();
        popupMenu.insert(showAllColumns, copyItemsIdx);
        hideColumn = new HideColumn();
        popupMenu.insert(hideColumn, copyItemsIdx);
        hideColumnRTMenu = new JMenuItem(new HideColumnRecordType());
        popupMenu.insert(hideColumnRTMenu, copyItemsIdx);
        changeHiddenState(false);

        copyMenus.add(new CopyRecord());
        copyMenus.add(new AppendRecord());
        copyMenus.add(new CopyAllRecords());
        copyMenus.add(new CopyAllShortRecords());
        copyMenus.add(new CopyAllJiraRecords());

        popupMenu.insert(new JPopupMenu.Separator(), copyItemsIdx);
        for (int i = copyMenus.size() - 1; i >= 0; i--) {
            popupMenu.insert(copyMenus.get(i), copyItemsIdx);

        }

//        popupMenu.add(new CopyRecord());
//        popupMenu.add(new AppendRecord());
//        popupMenu.add(new CopyAllRecords());
//        popupMenu.add(new CopyAllShortRecords());
        popupMenu.addSeparator();
        infoAction = new ShowInfo();
        popupMenu.add(infoAction);

        jmOpenIn = new JMenu("Open record in...");
        popupMenu.add(jmOpenIn);
        jmOpenIn.add(new OpenNotepad());
        jmOpenIn.add(new OpenTextPad());
    }

//    @Override
//    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
//        Component component = super.prepareRenderer(renderer, row, column);
//        int rendererWidth = component.getMaximumSize().width;
//        int rendererWidthM = component.getMinimumSize().width;
//        TableColumn tableColumn = getColumnModel().getColumn(column);
//        TabResultDataModel mod = (TabResultDataModel) getModel();
//        Pair<Color, Color> fontBg = mod.rowColors(row);
////        tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
////        tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width,0));
////        tableColumn.setMinWidth(rendererWidth + getIntercellSpacing().width);
////        tableColumn.setMinWidth(rendererWidthM + getIntercellSpacing().width);
////        component.setBackground(row % 2 == 0 ? getBackground() : Color.LIGHT_GRAY);
//        component.setForeground(fontBg.getKey());
//        component.setBackground(fontBg.getValue());
//        return component;
//    }
    class RegexField extends AbstractAction {

        private MyJTable tab;

        private RegexField(MyJTable aThis) {
            super("Regex field");
            tab = aThis;
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            TabResultDataModel model = (TabResultDataModel) tab.getModel();

            SearchExtract showFind = null;
            boolean addRows = false;
            int sampleRowsCnt = 3;
            boolean cancelled = false;
            int rowNo = tab.convertRowIndexToModel(popupRow);
            TabResultDataModel.TableRow tableRow = ((TabResultDataModel) getModel()).getRow(rowNo);

            while (!cancelled && (showFind = findExtract(tableRow.getRowType())) != null) {
                if (showFind.isMultipleRows()) {
                    addMultipleRows(model, showFind);
                    cancelled = true;
                } else {

                    ArrayList<SearchExtract.SearchSample> sampleRows = model.getSampleRows(showFind, rowNo, sampleRowsCnt);
                    if (sampleRows == null) {
                        int showConfirmDialog = inquirer.showConfirmDialog(null, "Nothing found", "Do you want to change search parameters", JOptionPane.YES_NO_OPTION);
                        if (showConfirmDialog == JOptionPane.NO_OPTION) {
                            cancelled = true;

                        } else {

                        }
                    } else {
                        DefaultTableModel infoTableModel = new DefaultTableModel();
                        infoTableModel.addColumn("Matched string");
                        infoTableModel.addColumn("result string");
                        for (SearchExtract.SearchSample entry : sampleRows) {
                            infoTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
                        }

                        JTable tab = new JTable(infoTableModel);
                        tab.getTableHeader().setVisible(true);

                        Dimension d = tab.getPreferredSize();
                        JPanel jScrollPane = new JPanel(new BorderLayout());
                        jScrollPane.add(tab);
                        jScrollPane.setPreferredSize(d);

                        JPanel infoPanel = new JPanel(new BorderLayout());
                        JTextArea lb = new JTextArea();
                        lb.setText("Below are samples found by specified expression.\n\n"
                                + "Please make sure that it is what you want\n"
                                + "Press\n"
                                + "Yes - you want add these rows to the table\n"
                                + "No - you want to change search expression\n"
                                + "Cancel - you want to terminate the dialog");
                        lb.setFocusable(false);

                        infoPanel.add(lb, BorderLayout.NORTH);
//                     infoPanel.setPreferredSize(new Dimension(640, 480));
                        switch (inquirer.showYesNoPanel((Window) getRootPane().getParent(),
                                "Found following rows ( out of sample " + sampleRowsCnt + ")", infoPanel, jScrollPane, JOptionPane.YES_NO_CANCEL_OPTION)) {
                            case JOptionPane.CANCEL_OPTION:
                                cancelled = true;
                                break;

                            case JOptionPane.NO_OPTION:
                                break;

                            case JOptionPane.YES_OPTION:
                                cancelled = true;
                                addRows = true;
                                break;
                        };

                    }
                }
                if (addRows && showFind != null) {
                    model.addRegexRow(showFind, rowNo);
                }
            }
        }

        private void addMultipleRows(TabResultDataModel model, SearchExtract showFind) {
            model.addRegexRow(showFind, tab.convertRowIndexToModel(popupRow));
        }
    }

    class SearchCell extends AbstractAction {

        private MyJTable tab;

        private SearchCell(MyJTable aThis) {
            super("Copy current cell into search");
            tab = aThis;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            JTableCommon table = (JTableCommon) popup.getInvoker();
            TableModel model = table.getModel();
            inquirer.logger.info((String) model.getValueAt(table.convertRowIndexToModel(popupRow), table.convertColumnIndexToModel(popupCol)));

        }

    }

    class ShowInfo extends AbstractAction {

        public ShowInfo() {
            super("Show info");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            JTableCommon table = (JTableCommon) popup.getInvoker();
            table.showInfo();

        }
    }

}

class CustomTableCellRenderer extends DefaultTableCellRenderer {

    private static final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();
    private final Font selectFont;

    public CustomTableCellRenderer(Font f) {
        selectFont = f;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//                inquirer.logger.debug("getTableCellRendererComponent");

        Component c = DEFAULT_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//        inquirer.logger.debug("getTableCellRendererComponent " + isSelected + " row: " + row + " col: " + column);
        Pair<Color, Color> fontBg = ((TabResultDataModel) table.getModel()).rowColors(table.convertRowIndexToModel(row));
        if (fontBg == null) {
            inquirer.logger.error("Empty font, row " + row);
        }
//        if (isSelected) {
//            c.setBackground(Color.red);
//        } else {
//            c.setForeground(fontBg.getKey());
//            c.setBackground(fontBg.getValue());
//        }
        if (isSelected) {
//            c.setFont(selectFont);
            Rectangle r = table.getCellRect(row, column, true);
//            inquirer.logger.debug("r: " + r);
            r.x = 0;
            r.width = table.getWidth() - 2;
            ((MyJTable) table).setRect(r, fontBg.getKey());
        } else {
//            ((MyJTable) table).setRect(null);
        }
        c.setForeground(fontBg.getKey());
        c.setBackground(fontBg.getValue());
//        c.repaint();

        return c;
    }

}
