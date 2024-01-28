/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.Pair;
import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * @author stepan_sydoruk
 */
public class CustomTableCellRenderer extends DefaultTableCellRenderer {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private static final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();
    private static final int NUM_DOTS = 2;
    private static final String DOTS_STRING = StringUtils.repeat('.', NUM_DOTS);
    private final Font selectFont;

    public CustomTableCellRenderer(Font f) {
        selectFont = f;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        Component c = DEFAULT_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        Pair<Color, Color> fontBg = ((TabResultDataModel) table.getModel()).rowColors(table.convertRowIndexToModel(row));

        if (fontBg == null) {
            inquirer.logger.error("Empty font, row " + row);
        } else {
            Rectangle r = table.getCellRect(row, column, true);
            if (isSelected) {
                r.x = 0;
                r.width = table.getWidth() - 2;
                ((MyJTable) table).setRect(r, fontBg.getKey());
            } else {
            }
            c.setForeground(fontBg.getKey());
            c.setBackground(fontBg.getValue());

            if (value != null) {
                FontMetrics fontMetrics = table.getGraphics().getFontMetrics();

                String s = value.toString();
                if (fontMetrics.stringWidth(s) + table.getIntercellSpacing().width * NUM_DOTS >= r.getWidth()) {
                    int chars = maxChars(s, fontMetrics, Math.round(r.getWidth()), fontMetrics.charWidth('.') * NUM_DOTS + table.getIntercellSpacing().width * 2);

//                inquirer.logger.info("chars:" + chars + " for [" + s + "] w:" + r.getWidth() + " spacing:" + table.getIntercellSpacing());
                    if (chars > 0) {
                        ((JLabel) c).setText(StringUtils.left(s, chars) + DOTS_STRING + StringUtils.right(s, chars));
                        return c;
                    } else {
                    }
                }
            }
        }
        return c;
    }

    private int maxChars(String s, FontMetrics fontMetrics, long cellWidth, int dotsWidth) {
        if (StringUtils.isNotBlank(s)) {
            int currentWidth = 0;
            for (int i = 0; i < s.length() / 2; i++) {
                currentWidth += fontMetrics.charWidth(s.charAt(i)) + fontMetrics.charWidth(s.charAt(s.length() - 1 - i));
//                inquirer.logger.info("maxchars for [" + s + "] currentWidth:" + currentWidth + " dotWidth:" + dotWidth + " cellWidth:" + cellWidth
//                        + " (currentWidth + dotWidth):" + (currentWidth + dotWidth));

                if (currentWidth + dotsWidth >= cellWidth) {
                    return i;
                }
            }
        }
        return 0;
    }

}
