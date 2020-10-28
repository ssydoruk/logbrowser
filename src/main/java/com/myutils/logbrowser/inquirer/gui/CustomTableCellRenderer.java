/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.Pair;
import com.myutils.logbrowser.inquirer.inquirer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author stepan_sydoruk
 */
class CustomTableCellRenderer extends DefaultTableCellRenderer {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
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
