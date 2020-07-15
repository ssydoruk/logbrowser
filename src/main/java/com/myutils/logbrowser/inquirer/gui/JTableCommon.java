/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.Pair;
import Utils.TableColumnAdjuster;
import Utils.UTCTimeRange;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import static com.jidesoft.dialog.StandardDialog.RESULT_CANCELLED;
import com.myutils.logbrowser.inquirer.DatabaseConnector;
import com.myutils.logbrowser.inquirer.MsgType;
import com.myutils.logbrowser.inquirer.gui.AColumnFilter.PositionalFilter;
import com.myutils.logbrowser.inquirer.inquirer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author ssydoruk
 */
abstract class JTableCommon extends JTablePopup {

    static final String RECORD_EMPTY = "<(Empty)>";

    private HashMap<Integer, AColumnFilter> columnFilters = new HashMap<>();

    @Override
    void callingPopup() {
        miCancelColumnFilter.setEnabled(popupCol > 0 && columnFilters.containsKey(popupCol));
        miCancelFilters.setEnabled(!columnFilters.isEmpty());
    }

    Rectangle r = null;
    private Color c;
    TableRowSorter<TableModel> sorter = null;

    private TableCellRenderer savedHeaderRenderer;
    private final JMenuItem miCancelColumnFilter;

    public void showInfo() {
        TabResultDataModel model = (TabResultDataModel) getModel();
        HashMap<MsgType, Integer> typeStat = model.getTypeStat(this);

        DefaultTableModel infoTableModel = new DefaultTableModel();
        infoTableModel.addColumn("Type name");
        infoTableModel.addColumn("count");
        int grandTotal = 0;
        for (Map.Entry<MsgType, Integer> entry : typeStat.entrySet()) {
            infoTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            grandTotal += entry.getValue();
        }
        infoTableModel.addRow(new Object[]{"TOTAL", grandTotal});

        JTable tab = new JTable(infoTableModel);
        JPanel jScrollPane = new JPanel(new BorderLayout());
        jScrollPane.add(tab);
        inquirer.showInfoPanel((Window) this.getRootPane().getParent(),
                ((this.getRowSorter() == null) ? "Full table" : "Filtered") + " - number of records by type", jScrollPane, true);

        inquirer.logger.trace(this.getSelectedRow() + " : " + this.getSelectedColumn());

    }

    @Override
    protected void paintComponent(Graphics g) {
//        inquirer.logger.debug("paintComponent");
        super.paintComponent(g);
        if (r != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(c);
            g2.draw(r);
//            inquirer.logger.debug("paintComponent: "+r);
        }
    }

    private void doCancelFilters() {
        int sel = popupRow;
        if (sel >= 0) {
            sel = convertRowIndexToModel(sel);
        }
        for (Integer key : columnFilters.keySet()) {
            getColumnModel().getColumn(key).setHeaderRenderer(savedHeaderRenderer);
        }

        getTableHeader().repaint();
        setRowSorter(null);
        columnFilters.clear();
        boolean resizeColumns = tca.isResizeColumns();
        tca.setResizeColumns(false);
        ((AbstractTableModel) getModel()).fireTableDataChanged();
        tca.setResizeColumns(resizeColumns);
        if (sel >= 0) {
            setRowSelectionInterval(sel, sel);
            scrollRectToVisible(new Rectangle(getCellRect(sel, 0, true)));
        } else {
            sel = -1;
        }
    }

    public void setRect(Rectangle r, Color c) {
        this.r = r;
        this.c = c;
//        invalidate();
//        repaint();
    }
    JMenuItem miCancelFilters;
    private final JMenuItem showInfo = null;
    protected int copyItemsIdx = 0;

    public JTableCommon() {
        super();
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        savedHeaderRenderer = getTableHeader().getDefaultRenderer();

        String act = "Search";
        getInputMap().put(KeyStroke.getKeyStroke('F', java.awt.event.InputEvent.CTRL_DOWN_MASK), act);
        getActionMap().put(act, new FindKeys(this));

        act = "SearchForward";
        getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), act);
        getActionMap().put(act, new FindForward(this));

        act = "SearchBack";
        getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, java.awt.event.InputEvent.SHIFT_DOWN_MASK), act);
        getActionMap().put(act, new FindBack(this));

        popupMenu.add(new CopyItem());
        copyItemsIdx = popupMenu.getComponentCount();
        popupMenu.addSeparator();
        popupMenu.add(new UniqueColumnsShown());
        popupMenu.add(new UniqueColumnsAll());
        popupMenu.add(new NumberFilterMenu());
        popupMenu.add(new DateFilterMenu(null));
//        popupMenu.add(directionFilter());
        miCancelFilters = popupMenu.add(new CancelFilters());
        miCancelColumnFilter = popupMenu.add(new CancelColumnFilter());
        popupMenu.addSeparator();
        popupMenu.add(new Find());
        popupMenu.add(new FindNext());
        popupMenu.add(new FindPrevius());
        popupMenu.add(new ExportExcel());

        tca = new TableColumnAdjuster(this);
        tca.setColumnDataIncluded(true);
        tca.setColumnHeaderIncluded(false);
        tca.setDynamicAdjustment(true);
        tca.setMaxColumnWidth(300);
//        if (getModel() != null) {
//            tca.adjustColumns();
//            inquirer.logger.debug("not-empty model");
//        } else {
//            inquirer.logger.debug("empty model");
//        }

    }

    public void adjustColumns() {
        tca.adjustColumns();
    }
    private TableColumnAdjuster tca;

    public TableColumnAdjuster getTca() {
        return tca;
    }

    UTCTimeRange getTimeRange(int tableColumn) {
        UTCTimeRange ret = new UTCTimeRange();
        long min = 0;
        long max = 0;
        long cur = 0;
        for (int i = 0; i < getRowCount(); i++) {
            cur = DatabaseConnector.stringToDate(getValueAt((i), tableColumn));
            if (cur > 0) {
                max = Math.max(max, cur);
                if (min > 0) {
                    min = Math.min(min, cur);
                } else {
                    min = cur;
                }
            }
        }
        ret.setStart(min);
        ret.setEnd(max);
        return ret;
    }

    /*
    allValues - show all unique values (so check the data) or
        show values after filter (so check the table)
     */
    ArrayList<Pair<String, Integer>> getUniqueVals(int popupCol, boolean allValues) {
        HashMap<String, AtomicInteger> hsTmp = new HashMap<>();
        if (allValues) {
            TableModel model = getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object valueAt = model.getValueAt((i), popupCol);
                String name = (valueAt == null || valueAt.toString().isEmpty()) ? RECORD_EMPTY : valueAt.toString();
                AtomicInteger cnt = hsTmp.get(name);
                if (cnt == null) {
                    hsTmp.put(name, new AtomicInteger(1));
                } else {
                    cnt.incrementAndGet();
                }
            }

        } else {
            for (int i = 0; i < getRowCount(); i++) {
                Object valueAt = getValueAt((i), popupCol);
                String name = (valueAt == null || valueAt.toString().isEmpty()) ? RECORD_EMPTY : valueAt.toString();
                AtomicInteger cnt = hsTmp.get(name);
                if (cnt == null) {
                    hsTmp.put(name, new AtomicInteger(1));
                } else {
                    cnt.incrementAndGet();
                }
            }
        }
        ArrayList<String> sorted = new ArrayList<>(hsTmp.keySet());
        Collections.sort(sorted);
        ArrayList<Pair<String, Integer>> ret = new ArrayList<>(sorted.size());
        for (String string : sorted) {
            ret.add(new Pair(string, hsTmp.get(string)));
        }
        return ret;
    }

    protected class DirectionalFilterMenu extends AbstractAction {

        private jdNumberFilter flt;
        private boolean isUp;

        private DirectionalFilterMenu(String current_and_above, boolean b) {
            super(current_and_above);
            this.isUp = b;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            columnFilters.put(popupCol, new PositionalFilter(popupRow, isUp));
            applyFilter();
        }
    }

    private JMenu directionFilter() {
        JMenu sectionsMenu = new JMenu("Directional filter");
        JMenuItem menuItem1 = new JMenuItem(new DirectionalFilterMenu("Current and above", true));
        sectionsMenu.add(menuItem1);
        JMenuItem menuItem2 = new JMenuItem(new DirectionalFilterMenu("Current and below", false));
        sectionsMenu.add(menuItem2);
        return sectionsMenu;
    }

    protected class UniqueColumnsShown extends UniqueColumns {

        public UniqueColumnsShown() {
            super("Unique values in column as shown", false);
        }
    }

    protected class UniqueColumns extends AbstractAction {

        private final boolean showAll;

        public UniqueColumns(String menuTitle, boolean showAll) {
            super(menuTitle);
            this.showAll = showAll;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            uniqueColumnValues(e);
        }

        InfoPanel p = null;

        public void uniqueColumnValues(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            JTableCommon table = (JTableCommon) popup.getInvoker();

            ArrayList<Pair<String, Integer>> uniqueVals = getUniqueVals(popupCol, showAll);
            Object valueAt = getValueAt(popupRow, popupCol);

            JTablePopup tab = getJTablePopup();

            DefaultTableModel infoTableModel = new DefaultTableModel();
            infoTableModel.addColumn("Values");
            infoTableModel.addColumn("Count");
            int grandTotal = 0;
            int selectedRowIdx = -1;
            for (int i = 0; i < uniqueVals.size(); i++) {
                Pair<String, Integer> val = uniqueVals.get(i);
                infoTableModel.addRow(new Object[]{val.getKey(), val.getValue()});
                if (selectedRowIdx < 0) {
                    if (valueAt != null) {
                        if (valueAt.equals(val.getKey())) {
                            selectedRowIdx = i;

                        }
                    }
                }
                grandTotal++;
            }

//        infoTableModel.addRow(new Object[]{"TOTAL(" + grandTotal + ")"});
            tab.setModel(infoTableModel);
            if (selectedRowIdx >= 0) {
                ListSelectionModel selectionModel1 = tab.getSelectionModel();
                selectionModel1.setSelectionInterval(selectedRowIdx, selectedRowIdx);
            }

            String theTitle = "Unique values in column (total " + grandTotal + ")";
            p = new InfoPanel((Window) table.getRootPane().getParent(), theTitle, tab);

            p.doShow();
            if (p.getCloseCause() == JOptionPane.OK_OPTION) { //apply filter
                int[] selectedRows = tab.getSelectedRows();
                if (selectedRows.length == uniqueVals.size()) {
                    doCancelFilters();
                } else {
                    HashSet<String> selValues = new HashSet<>(selectedRows.length);
                    for (int selectedRow : selectedRows) {
                        selValues.add((String) infoTableModel.getValueAt(selectedRow, 0));
                    }
                    columnFilters.put(table.convertColumnIndexToModel(popupCol), new AColumnFilter.StringsFilter(selValues));
                }
                applyFilter();
            }
            inquirer.logger.trace(table.getSelectedRow() + " : " + table.getSelectedColumn());
        }

        private JTablePopup getJTablePopup() {
            if (uniquePopup == null) {

                uniquePopup = new JTablePopup() {
                    @Override
                    void theMousePressed(MouseEvent e) {

                    }

                    @Override
                    void callingPopup() {

                    }
                };
                uniquePopup.getTableHeader().setVisible(false);
                JPopupMenu popupMenu1 = uniquePopup.getPopupMenu();

                String act = "Search (Ctrl-F)";
                uniquePopup.getInputMap().put(KeyStroke.getKeyStroke('F', java.awt.event.InputEvent.CTRL_DOWN_MASK), act);
                uniquePopup.getActionMap().put(act, new FindKeys(uniquePopup));

                act = "SearchForward (F3)";
                uniquePopup.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0), act);
                uniquePopup.getActionMap().put(act, new FindForward(uniquePopup));

                act = "SearchBack (Shift-F3)";
                uniquePopup.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, java.awt.event.InputEvent.SHIFT_DOWN_MASK), act);
                uniquePopup.getActionMap().put(act, new FindBack(uniquePopup));

                popupMenu1.add(new Find());
                popupMenu1.add(new FindNext());
                popupMenu1.add(new FindPrevius());
                popupMenu1.addSeparator();
                popupMenu1.add(new FindAndSelect());
                popupMenu1.add(new ReverseSelection());
                popupMenu1.addSeparator();
                popupMenu1.add(new CopyAll());
                popupMenu1.add(new CopySelected());

            }
            return uniquePopup;
        }

    }

    protected class UniqueColumnsAll extends UniqueColumns {

        public UniqueColumnsAll() {
            super("Unique values in column (all)", true);
        }
    }

    private JTablePopup uniquePopup = null;

    protected class CancelColumnFilter extends AbstractAction {

        public CancelColumnFilter() {
            super("Cancel column filter");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int selRow = popupRow;
            if (selRow >= 0) {
                selRow = convertRowIndexToModel(selRow);
            }
            int selCol = popupCol;
            if (selCol >= 0) {
                selCol = convertColumnIndexToModel(selCol);
            }
            if (columnFilters.containsKey(selCol)) {
                columnFilters.remove(selCol);

//                getTableHeader().repaint();
//                setRowSorter(null);
//                columnFilters.clear();
//                ((AbstractTableModel) getModel()).fireTableDataChanged();
//                if (sel >= 0) {
//                    setRowSelectionInterval(sel, sel);
//                    scrollRectToVisible(new Rectangle(getCellRect(sel, 0, true)));
//                } else {
//                    sel = -1;
//                }
//                filterChanged();
                TableColumn column = getColumnModel().getColumn(selCol);
                if (column != null) {
                    column.setHeaderRenderer(savedHeaderRenderer);
                }
                applyFilter();
                if (selRow >= 0) {
                    selRow = convertRowIndexToView(selRow);
                    if (selRow >= 0) {
                        setRowSelectionInterval(selRow, selRow);
                        scrollRectToVisible(new Rectangle(getCellRect(selRow, 0, true)));
                    }
                } else {
                    selRow = -1;
                }
            }
        }
    }

    protected class CancelFilters extends AbstractAction {

        public CancelFilters() {
            super("Cancel filters");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doCancelFilters();

        }

    }

    protected class NumberFilterMenu extends AbstractAction {

        private jdNumberFilter flt;

        public NumberFilterMenu() {
            super("Number filter");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (flt == null) {
                flt = new jdNumberFilter(null, enabled);
            }
            flt.setCurrentNumber(getValueAt(popupRow, popupCol));
            flt.setLocationRelativeTo(getParent());
            flt.setVisible(true);
            if (flt.getCloseCause() == JOptionPane.OK_OPTION) { //apply filter
                columnFilters.put(popupCol, flt.getNumberSelection());
                applyFilter();
            }
        }
    }

    protected class DateFilterMenu extends AbstractAction {

        jdDateTimeFilter flt = null;
        private final Container theParent;

        private DateFilterMenu(Container f) {
            super("Date filter");
            theParent = f;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (flt == null) {
                flt = new jdDateTimeFilter(null, enabled);
            }
            flt.setCurrentCell(getValueAt(popupRow, popupCol));
            UTCTimeRange timeRange = getTimeRange(popupCol);
            if (timeRange == null || (timeRange.getStart() <= 0 && timeRange.getEnd() <= 0)) {
                JOptionPane.showMessageDialog(null, "No date found in column", "Date filter", JOptionPane.OK_OPTION);
            } else {
                flt.setTimeRange(getTimeRange(popupCol));
                flt.setLocationRelativeTo(getParent());
                flt.setVisible(true);
                if (flt.getCloseCause() == JOptionPane.OK_OPTION) { //apply filter
                    columnFilters.put(popupCol, flt.getDateFilter());
                    applyFilter();

                }
            }
        }
    }

    protected class FindBack extends AbstractAction {

        private final JTablePopup tab;

        public FindBack(JTablePopup aThis) {
            tab = aThis;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tab.showFindDialog();
        }
    }

    protected class FindForward extends AbstractAction {

        private final JTablePopup tab;

        public FindForward(JTablePopup aThis) {
            tab = aThis;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tab.showFindDialog();

        }
    }

    class CopyItem extends AbstractAction {

        public CopyItem() {
            super("Copy cell");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            JTableCommon table = (JTableCommon) popup.getInvoker();
            TableModel model = table.getModel();
            SystemClipboard.copy((String) model.getValueAt(table.convertRowIndexToModel(popupRow), table.convertColumnIndexToModel(popupCol)));
        }

    }

    class CopyAll extends AbstractAction {

        public CopyAll() {
            super("Copy all rows");
            putValue(SHORT_DESCRIPTION, "Search in the table (Ctrl-F)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).copyAll();

        }

    }

    class CopySelected extends AbstractAction {

        public CopySelected() {
            super("Copy selected");
            putValue(SHORT_DESCRIPTION, "Search in the table (Ctrl-F)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).copySelected();

        }

    }

    class Find extends AbstractAction {

        public Find() {
            super("Find (Ctrl-F)");
            putValue(SHORT_DESCRIPTION, "Search in the table (Ctrl-F)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).showFindDialog();

        }

        private Frame getTheParent(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            return (Frame) ((JTable) popup.getInvoker()).getRootPane().getParent();
        }

    }

    class FindAndSelect extends AbstractAction {

        public FindAndSelect() {
            super("Find and select");
            putValue(SHORT_DESCRIPTION, "Find and select");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).findAndSelect();

        }

        private Frame getTheParent(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            return (Frame) ((JTable) popup.getInvoker()).getRootPane().getParent();
        }

    }

    class ReverseSelection extends AbstractAction {

        public ReverseSelection() {
            super("Reverse selection");
            putValue(SHORT_DESCRIPTION, "Find and select");
        }

        @Override
        public void actionPerformed(ActionEvent e) {

//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();

            ((JTablePopup) popup.getInvoker()).reverseSelection();

        }

        private Frame getTheParent(ActionEvent e) {
            Component c = (Component) e.getSource();
            JPopupMenu popup = (JPopupMenu) c.getParent();
            return (Frame) ((JTable) popup.getInvoker()).getRootPane().getParent();
        }

    }

    class FindNext extends AbstractAction {

        public FindNext() {
            super("Find next (F3)");
            putValue(SHORT_DESCRIPTION, "Find next (F3)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    class FindPrevius extends AbstractAction {

        public FindPrevius() {
            super("Find previous (Shift-F3)");
            putValue(SHORT_DESCRIPTION, "Find previous (Shift-F3)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    class ExportExcel extends AbstractAction {

        public ExportExcel() {
            super("Export Excel");
//            putValue(SHORT_DESCRIPTION, "Find previous (Shift-F3)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
//            Frame theParent = getTheParent(e);
            Component c = (Component) e.getSource();
            if (c != null) {
                JPopupMenu popup = (JPopupMenu) c.getParent();
                if (popup != null) {
                    exportExcel((JTableCommon) popup.getInvoker());
                }
            }
        }

    }

    class InfoPanel extends StandardDialog {

        private int closeCause = JOptionPane.CANCEL_OPTION;
        private JTable tab;

        public int getCloseCause() {
            return closeCause;
        }

        public void setCloseCause(int closeCause) {
            this.closeCause = closeCause;
        }

        private void selectionChanged(JButton bt, JTable tab) {
            int rowsSelected = tab.getSelectedRows().length;
            bt.setEnabled(rowsSelected > 0);
            bt.setText((rowsSelected == 0)
                    ? "Empty selection"
                    : "Filter with " + rowsSelected + " items");
        }

        InfoPanel(Window parent, String title, JTable tab) {
            super(parent, title);
            this.tab = tab;

        }

        public void doShow() {
            setModal(true);

            pack();
            if (inquirer.logger.isDebugEnabled()) {
                inquirer.logger.debug("Show info PanelDialog; title=" + getTitle() + "; tab cols:" + tab.getColumnCount() + " rows: " + tab.getRowCount());
                StringBuilder s = new StringBuilder(512);
                for (int i = 0; i < tab.getRowCount(); i++) {
                    s.setLength(0);
                    for (int j = 0; j < tab.getColumnCount(); j++) {
                        s.append("[" + tab.getValueAt(i, j) + "],");
                    }
                    inquirer.logger.trace(s);
                }

            }

//            ScreenInfo.CenterWindow(this);
            this.setLocationRelativeTo(getParent());
            setVisible(
                    true);
        }

        @Override
        public JComponent createBannerPanel() {
            return null;
        }

        @Override
        public JComponent createContentPanel() {
//                        JPanel panel = new JPanel(new BorderLayout(10, 10));
//            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

//            panel.add(mainPanel, BorderLayout.CENTER);
//            return panel;
            JScrollPane jScrollPane = new JScrollPane(tab);
            tab.getTableHeader().setVisible(true);

            JPanel listPane = new JPanel(new BorderLayout(10, 10));

            listPane.add(new JPanel(new BorderLayout()).add(jScrollPane));
            return listPane;
        }

        JButton jbFilter;

        @Override
        public ButtonPanel createButtonPanel() {
            ButtonPanel buttonPanel = new ButtonPanel();
            JButton cancelButton = new JButton();
            buttonPanel.addButton(cancelButton);

            cancelButton.setAction(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setDialogResult(RESULT_CANCELLED);
                    setCloseCause(JOptionPane.CANCEL_OPTION);
                    setVisible(false);
                    dispose();
                }
            });
            cancelButton.setText("Close");

            jbFilter = new JButton("Use as filter");
            buttonPanel.addButton(jbFilter);
            tab.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    selectionChanged(jbFilter, tab);
                }
            });
            selectionChanged(jbFilter, tab);

//            listPane.add(jbFilter);
            jbFilter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tab.editingCanceled(null);
                    setCloseCause(JOptionPane.OK_OPTION);
                    dispose();
                }
            });

            String act = "ApplyFilter";

            tab.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), act);
            tab.getActionMap().put(act, jbFilter.getAction());

            setDefaultCancelAction(cancelButton.getAction());
            setDefaultAction(jbFilter.getAction());
            getRootPane().setDefaultButton(jbFilter);

            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            buttonPanel.setSizeConstraint(ButtonPanel.NO_LESS_THAN); // since the checkbox is quite wide, we don't want all of them have the same size.
            return buttonPanel;
        }

        private void doShow(String theTitle) {
            this.setTitle(theTitle);
            doShow();
        }
    }

    private void applyFilter() {

        ArrayList<RowSetFilter> andFilters = new ArrayList<>();
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(Color.RED);
//        headerRenderer.setForeground(Color.RED);
//        getTableHeader().setDefaultRenderer(headerRenderer);
        for (Map.Entry<Integer, AColumnFilter> entrySet : columnFilters.entrySet()) {
            Integer key = entrySet.getKey();
            AColumnFilter value = entrySet.getValue();

            getColumnModel().getColumn(key).setHeaderRenderer(headerRenderer);

//            for (int i = 0; i < getModel().getColumnCount(); i++) {
//        getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
//}
            andFilters.add(new RowSetFilter(key, value));

        }
        sorter.setRowFilter(RowFilter.andFilter(andFilters));
        setRowSorter(sorter);
        boolean resizeColumns = tca.isResizeColumns();
        tca.setResizeColumns(false);
        ((AbstractTableModel) getModel()).fireTableDataChanged();
        tca.setResizeColumns(resizeColumns);
//        getTableHeader().invalidate();
//        getTableHeader().repaint();

    }

    @Override
    public void setModel(TableModel dataModel) {
        setRowSorter(null);
        super.setModel(dataModel); //To change body of generated methods, choose Tools | Templates.
        sorter = new TableRowSorter<>(dataModel);
//        if (tca != null) {
//            tca.adjustColumns();
//        }

    }

    private class RowSetFilter extends RowFilter<TableModel, Object> {

        private final Integer col;
        private final AColumnFilter filter;

        public RowSetFilter(Integer col, AColumnFilter filter) {
            this.col = col;
            this.filter = filter;
        }

        @Override
        public boolean include(RowFilter.Entry<? extends TableModel, ? extends Object> entry) {
            return filter.showValue(entry.getValue(col));
        }

    }

    class FindKeys extends AbstractAction {

        private final JTablePopup tab;

        public FindKeys(JTablePopup tab) {
            super();
            this.tab = tab;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            tab.showFindDialog();
        }

    }

    public void exportExcel(JTableCommon jTableCommon) {
        ArrayList<String> printFile = null;
        try {
            String fileNameExcel = inquirer.getCr().getFileNameExcel();
            File f = new File(fileNameExcel);
            PrintStream printStream = new PrintStream(new FileOutputStream(f));
            for (int j = 0; j < getColumnCount(); j++) {
                printStream.print("\"" + getColumnName(j) + "\",");
            }
            printStream.print("\n");

            for (int i = 0; i < getRowCount(); i++) {
                for (int j = 0; j < getColumnCount(); j++) {
                    printStream.print("\"" + getValueAt(i, j) + "\",");
                }
                printStream.print("\n");
            }
            printStream.close();

            printFile = new ArrayList<>(1);
            printFile.add(f.getAbsolutePath());
            Excel.reportEdit(printFile);
        } catch (Exception e) {
            inquirer.ExceptionHandler.handleException("export to Excel", e);
        }

    }

}
