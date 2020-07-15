/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.myutils.logbrowser.inquirer.inquirer;
import java.awt.Component;
import java.awt.Point;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableModel;

/**
 *
 * @author Stepan
 */
class JTablePopupMenu extends JPopupMenu {

    private final JTablePopup tab;

    JTablePopupMenu(JTablePopup aThis) {
        tab = aThis;
//        addPopupMenuListener(new PopupListener());

    }

    private class PopupListener implements PopupMenuListener {

        public PopupListener() {
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Point popupPoint = SwingUtilities.convertPoint((Component) e.getSource(), new Point(0, 0), tab);
//                    inquirer.logger.info("p: " + popupPoint + " row: " + tab.rowAtPoint(popupPoint) + " col: " + tab.columnAtPoint(popupPoint));
                    int row = tab.rowAtPoint(popupPoint);
                    int col = tab.columnAtPoint(popupPoint);
//                    if (inquirer.logger.isDebugEnabled()) {
//                        TableModel model = tab.getModel();
//                        if (model instanceof TabResultDataModel) {
//                            inquirer.logger.debug("1Popup calling at " + popupPoint.toString() + " r:" + row + " c:" + col
//                                    + " tr:" + tab.getPopupRow() + " tc:" + tab.getPopupCol()
//                                    + " rowSelected?" + tab.isRowSelected(row) + " selC: " + ((TabResultDataModel) model).getValueAt(tab.convertRowIndexToModel(row), tab.convertColumnIndexToModel(col))
//                                    + "\n\tdata1:" + ((TabResultDataModel) model).getRow(tab.convertRowIndexToModel(row))
//                            );
//                        }
//                    }
                    if (!tab.isRowSelected(row)) {
//                        row = tab.convertRowIndexToModel(row);
//                        col = tab.convertColumnIndexToModel(col);
                        tab.changeSelection(row, col, false, false);
                        if (inquirer.logger.isDebugEnabled()) {
                            TableModel model = tab.getModel();
                            if (model instanceof TabResultDataModel) {

//                                inquirer.logger.debug("Popup calling at " + popupPoint.toString() + " r:" + row + " c:" + col
//                                        + " tr:" + tab.getPopupRow() + " tc:" + tab.getPopupCol()
//                                        + " rowSelected?" + tab.isRowSelected(row) + " selC: " + ((TabResultDataModel) model).getValueAt(tab.convertRowIndexToModel(row), tab.convertColumnIndexToModel(col))
//                                        + "\n\tdata1:" + ((TabResultDataModel) model).getRow(tab.convertRowIndexToModel(row))
//                                );
                            }
                        }
                    }
                    tab.setPopupRow(row);
                    tab.setPopupCol(col);

                    tab.callingPopup();
                }
            });

        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {

        }
    }

}
