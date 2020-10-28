/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.Pair;
import Utils.ScreenInfo;
import com.myutils.logbrowser.inquirer.EditRegexFields;
import com.myutils.logbrowser.inquirer.EnterRegexDialog;
import static com.myutils.logbrowser.inquirer.EnterRegexDialog.RET_OK;
import com.myutils.logbrowser.inquirer.MsgType;
import com.myutils.logbrowser.inquirer.inquirer;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author ssydoruk
 */
public abstract class JTablePopup extends JTable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Logger logger = LogManager.getLogger();

    protected final JPopupMenu popupMenu;
    protected int popupRow;
    protected int popupCol;
    private EnterRegexDialog findDlg = null;
    private EditRegexFields searchExtract = null;

    public JTablePopup() {
        super();
        popupMenu = new JPopupMenu();

        addMouseListener(new JTableCommon.MyMouseAdapter() {
        });
//        setComponentPopupMenu(popupMenu);
    }

    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    public int getPopupRow() {
        return popupRow;
    }

    public void setPopupRow(int popupRow) {
        this.popupRow = popupRow;
    }

    public void setPopupCol(int popupCol) {
        this.popupCol = popupCol;
    }

    public int getPopupCol() {
        return popupCol;
    }

    void copyAll() {
        StringBuilder out = new StringBuilder(getRowCount() * 120);
        for (int i = 0; i < getRowCount(); i++) {
            out.append(getValueAt(i, 0).toString());
            for (int j = 1; j < getColumnCount(); j++) {
                out.append(',').append(getValueAt(i, j).toString());

            }
            out.append('\n');
        }
        SystemClipboard.copy(out.toString());
    }

    void copySelected() {
        int[] selectedRows = getSelectedRows();

        if (selectedRows != null && selectedRows.length > 0) {
            StringBuilder out = new StringBuilder(selectedRows.length * 120);
            for (int i = 0; i < selectedRows.length; i++) {
                out.append(getValueAt(selectedRows[i], 0).toString()).append('\n');
            }
            SystemClipboard.copy(out.toString());
        }
    }

    void reverseSelection() {
        int[] selectedRows = getSelectedRows();

        if (selectedRows != null && selectedRows.length > 0) {
            HashSet<Integer> saveSelection = new HashSet<>(selectedRows.length);
            for (int i = 0; i < selectedRows.length; i++) {
                saveSelection.add(selectedRows[i]);
            }

            ListSelectionModel selectionModel1 = getSelectionModel();
            selectionModel1.setValueIsAdjusting(true);
            selectionModel1.clearSelection();
            for (int i = 0; i < getRowCount(); i++) {
                if (!saveSelection.contains(i)) {
                    selectionModel1.addSelectionInterval(i, i);
                }
            }
            selectionModel1.setValueIsAdjusting(false);
        }
    }

    private void theMousePressed1(MouseEvent e) {
//        logger.debug("click at " + e.getPoint() + (e.isPopupTrigger() ? " popup!" : ""));

        theMousePressed(e);
        if (e.isPopupTrigger()) {
            popupMenuWillBecomeVisible(e, this);
        }
    }

    private void theMouseRelesed(MouseEvent e) {
//        logger.debug("rel at " + e.getPoint() + (e.isPopupTrigger() ? " popup!" : ""));
        if (e.isPopupTrigger()) {
            popupMenuWillBecomeVisible(e, this);
        }
    }

    public void popupMenuWillBecomeVisible(MouseEvent e, JTable tab) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
//                Point popupPoint = SwingUtilities.convertPoint((Component) e.getSource(), new Point(0, 0), tab);
                Point popupPoint = e.getPoint();
//                 SwingUtilities.convertPointFromScreen(popupPoint, tab);
//                inquirer.logger.debug("convert: " + e.getPoint() + " to " + popupPoint);
                int row = tab.rowAtPoint(e.getPoint());
                int col = tab.columnAtPoint(popupPoint);
//                if (inquirer.logger.isDebugEnabled()) {
//                    TableModel model = tab.getModel();
//                    if (model instanceof TabResultDataModel) {
//                        inquirer.logger.debug("1Popup calling at " + popupPoint.toString() + " r:" + row + " c:" + col
//                                + " rowSelected?" + tab.isRowSelected(row) + " selC: " + ((TabResultDataModel) model).getValueAt(tab.convertRowIndexToModel(row), tab.convertColumnIndexToModel(col))
//                                + "\n\tdata1:" + ((TabResultDataModel) model).getRow(tab.convertRowIndexToModel(row))
//                        );
//                    }
//                }
                if (!tab.isRowSelected(row)) {
//                        row = tab.convertRowIndexToModel(row);
//                        col = tab.convertColumnIndexToModel(col);
                    tab.changeSelection(row, col, false, false);
                    if (inquirer.logger.isDebugEnabled()) {
                        TableModel model = tab.getModel();
                        if (model instanceof TabResultDataModel) {

                            inquirer.logger.debug("changed selection to " + popupPoint.toString() + " r:" + row + " c:" + col
                                    + " rowSelected?" + tab.isRowSelected(row) + " selC: " + ((TabResultDataModel) model).getValueAt(tab.convertRowIndexToModel(row), tab.convertColumnIndexToModel(col))
                                    + "\n\tdata1:" + ((TabResultDataModel) model).getRow(tab.convertRowIndexToModel(row))
                            );
                        }
                    }
                }
                setPopupRow(row);
                setPopupCol(col);

                callingPopup();
                popupMenu.show(tab, popupPoint.x, popupPoint.y);
            }
        });

    }

    abstract void theMousePressed(MouseEvent e);

    abstract void callingPopup();

    void showFindDialog() {
        if (findDlg == null) {
            findDlg = new EnterRegexDialog(null, true);
        }
        ScreenInfo.setVisible(this, findDlg, true);
        if (findDlg.getReturnStatus() == RET_OK) {
            nextFind(findDlg.isDownChecked());
        }

    }

    void findAndSelect() {
        if (findDlg == null) {
            findDlg = new EnterRegexDialog(null, true);
        }
        ScreenInfo.setVisible(this, findDlg, true);
        if (findDlg.getReturnStatus() == RET_OK) {

            TableModel model = getModel();
            ListSelectionModel selectionModel1 = getSelectionModel();

            selectionModel1.clearSelection();
            String search = findDlg.getSearch();

            if (search != null && !search.isEmpty()) {
                boolean matchWholeWordSelected = findDlg.isMatchWholeWordSelected();
                Pattern pt = (findDlg.isRegexChecked()) ? EnterRegexDialog.getRegex(search, matchWholeWordSelected) : null;
                search = search.toLowerCase();

                for (int i = 0; i < getRowCount(); i++) {
                    for (int j = 0; j < getColumnCount(); j++) {
                        Object valueAt = model.getValueAt(convertRowIndexToModel(i), convertColumnIndexToModel(j));
                        if (valueAt != null && inquirer.matchFound(valueAt.toString(), pt, search, matchWholeWordSelected)) {
                            selectionModel1.addSelectionInterval(i, i);
                        }
                    }
                }
            }
        }

    }

    protected EnterRegexDialog showFind() {
        if (findDlg == null) {
            findDlg = new EnterRegexDialog(null, true);
        }
        ScreenInfo.setVisible(this, findDlg, true);

        if (findDlg.getReturnStatus() == RET_OK) {
            return findDlg;
        }
        return null;
    }

    protected EditRegexFields editRegexFields(MsgType t) throws IOException {
        return editRegexFields(t, null);

    }
    
        protected EditRegexFields editRegexFields(MsgType t, String fullMsg) throws IOException {
        if (searchExtract == null) {
            searchExtract = new EditRegexFields(null, true);
        }
        
        EditRegexFields ret = searchExtract.doShow(this, true, t, fullMsg);
        if (ret != null) {
            inquirer.getCr().setRegexSearches(t, ret.toArray());
            inquirer.saveCR();
        }
        return ret;

    }

    Pair<Integer, Integer> searchCell(EnterRegexDialog findDlg, int popupRow, int popupCol) {
        TableModel model = getModel();

        String search = findDlg.getSearch();

        if (search != null && !search.isEmpty()) {
            Pair<Integer, Integer> ret = null;
            if (popupRow < 0) {
                popupRow = 0;
            }
            if (popupCol < 0) {
                popupCol = 0;
            }
            int savePopupRow = popupRow;
            int savePopupCol = popupCol;
            boolean matchWholeWordSelected = findDlg.isMatchWholeWordSelected();
            Pattern pt = (findDlg.isRegexChecked()) ? EnterRegexDialog.getRegex(search, matchWholeWordSelected) : null;
            search = search.toLowerCase();

            if (findDlg.isDownChecked()) {
                int startCol = savePopupCol + 1;
                for (int i = savePopupRow; i < getRowCount(); i++) {
                    for (int j = startCol; j < getColumnCount(); j++) {
                        Object valueAt = model.getValueAt(convertRowIndexToModel(i), convertColumnIndexToModel(j));
                        if (valueAt != null && inquirer.matchFound(valueAt.toString(), pt, search, matchWholeWordSelected)) {
                            return new Pair<>(i, j);
                        }
                    }
                    startCol = 0;
                }
            } else {
                int startCol = savePopupCol - 1;
                for (int i = savePopupRow; i >= 0; i--) {
                    for (int j = startCol; j >= 0; j--) {
                        Object valueAt = model.getValueAt(convertRowIndexToModel(i), convertColumnIndexToModel(j));
                        if (valueAt != null && inquirer.matchFound(valueAt.toString(), pt, search, matchWholeWordSelected)) {
                            return new Pair<>(i, j);
                        }
                    }
                    startCol = getColumnCount() - 1;
                }
            }
        }

        return null;
    }

    void nextFind(boolean isDown) {
        findDlg.setDown(isDown);
        Pair<Integer, Integer> cell = searchCell(findDlg, getPopupRow(), getPopupCol());
        inquirer.logger.debug("found cell: " + cell);
        if (cell != null) {
            Integer thePopupRow = cell.getKey();
            Integer thePopupCol = cell.getValue();

            setPopupCol(thePopupCol);
            setPopupRow(thePopupRow);
            scrollToVisible(thePopupRow, thePopupCol);
            changeSelection(thePopupRow, thePopupCol, false, false);
            requestFocus();
        } else {
            JOptionPane.showMessageDialog(null, "No more cell found", "Info", INFORMATION_MESSAGE);
        }

    }

    private void scrollToVisible(int rowIndex, int vColIndex) {
        if (!(getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport) getParent();
        Rectangle rect = getCellRect(rowIndex, vColIndex, true);
        Point pt = viewport.getViewPosition();
        rect.setLocation(rect.x - pt.x, rect.y - pt.y);
        viewport.scrollRectToVisible(rect);
    }

    class MyMouseAdapter extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            theMousePressed1(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {

            theMouseRelesed(e);
        }

    }

}
