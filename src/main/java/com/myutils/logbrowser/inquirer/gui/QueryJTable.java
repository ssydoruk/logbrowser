/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.Pair;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.inquirer.FullTableColors;
import com.myutils.logbrowser.inquirer.IQueryResults;
import com.myutils.logbrowser.inquirer.SelectionType;
import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stepan
 */
@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation", "this-escape"})
public class QueryJTable extends JTableCommon {

    private static final DefaultTableCellRenderer DEFAULT_RENDERER = new DefaultTableCellRenderer();
    static ZoneId zoneId = ZoneId.systemDefault();

    public QueryJTable(FullTableColors tabs) throws Exception {
        super();

        this.setModel(new DataModel(tabs));
//        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        Font font = this.getFont();

        setDefaultRenderer(Object.class, new QueryJTableCellRenderer(new Font(font.getName(), Font.BOLD, font.getSize())));
        Utils.swing.TableColumnAdjuster tca = getTca();
        tca.setColumnHeaderIncluded(true);
        tca.adjustColumns();

    }

    public static void setResize(JTable table) {
//        for (int column = 0; column < table.getColumnCount(); column++) {
//            TableColumn tableColumn = table.getColumnModel().getColumn(column);
//            int preferredWidth = tableColumn.getMinWidth();
//            int maxWidth = tableColumn.getMaxWidth();
//
//            for (int row = 0; row < table.getRowCount(); row++) {
//                TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
//                Component c = table.prepareRenderer(cellRenderer, row, column);
//                int width = c.getPreferredSize().width + table.getIntercellSpacing().width;
//                preferredWidth = Math.max(preferredWidth, width);
//
//                //  We've exceeded the maximum width, no need to check other rows
//                if (preferredWidth >= maxWidth) {
//                    preferredWidth = maxWidth;
//                    break;
//                }
//            }
//
//            tableColumn.setPreferredWidth(preferredWidth + 5);
//        }
    }

    @Override
    void theMousePressed(MouseEvent e) {
    }

    @Override
    void callingPopup() {

    }

    public String getField(String filename, int popupRow) {
        QueryJTable.DataModel mod = (QueryJTable.DataModel) getModel();
        int findColumn = mod.findColumn(filename);
        if (findColumn >= 0) {
            return (String) getValueAt(popupRow, findColumn);
        }
        return null;
    }

    protected class DataModel extends AbstractTableModel {

        private final FullTableColors tabData;

        private DataModel(FullTableColors tabs) {
            this.tabData = tabs;
        }

        public ArrayList<String> exportToExcel() throws FileNotFoundException {
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

            ArrayList<String> ret = new ArrayList<>(1);
            ret.add(f.getAbsolutePath());

            return ret;
        }

        @Override
        public int getRowCount() {
            return tabData.getData().size();
        }

        @Override
        public int getColumnCount() {
            if (tabData != null) {
                return tabData.getColumnNames().size();
            }
            return 0;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return tabData.getColumnNames().get(columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            try {
                Object value = tabData.getData().get(rowIndex).get(columnIndex);

                /* below block would try itself to convert longs into datetime. But do not think this is needed 
                as datetime conversion can be put into query                
                 */
//                if (value != null && value instanceof Long) {
//                    int cType = tabData.getColumnTypes().get(columnIndex);
//                    if (cType == java.sql.Types.INTEGER || cType == java.sql.Types.BIGINT) {
//                        try {
//                            long val = (long) value;
//                            if (val > 1000000) {
//                                return Instant.ofEpochMilli(val).atZone(zoneId).toLocalDateTime();
//                            }
//                        } catch (Exception e) {
//                            inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
//
//                        }
//                    }
//                }
                return value;
            } catch (Exception e) {
                inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
                return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
        }

        private Pair<Color, Color> rowColors(int row) {
            return tabData.getRowColors(row);
        }

        /**
         * @param row
         * @param searchField
         * @return pair of Selection Type, value of ID for selectionType
         */
        public Pair<SelectionType, String> getSearchColumn(int row, IQueryResults.SearchFields searchField) throws Exception {
            Pair<SelectionType, String> ret = null;
            List<Pair<SelectionType, String>> searchFieldsList = null;

            String typeField = searchField.getTypeField();

            if (typeField == null) {
                searchFieldsList = searchField.getSingle();
            } else {
                int columnIdx = tabData.getColumnIdx(typeField);
                if (columnIdx > 0) {
                    searchFieldsList = searchField.getSearchFieldName(FileInfoType.GetvalueOf((Integer) getValueAt(row, columnIdx)));
                }
            }
            if (searchFieldsList != null) {
                for (Pair<SelectionType, String> lst : searchFieldsList) {
                    int columnIdx = tabData.getColumnIdx(lst.getValue());
                    if (columnIdx > 0) {
                        String valueAt = (String) getValueAt(row, columnIdx);
                        if (StringUtils.isNotEmpty(valueAt)) {
                            ret = new Pair<>();
                            ret.setKey(lst.getKey());
                            ret.setValue(valueAt);
                            return ret;
                        }
                    }
                }
            }
//            return (String) getValueAt(row, tabData.getColumnIdx(searchField));
            return ret;
        }
    }

    class QueryJTableCellRenderer extends DefaultTableCellRenderer {

        private final Font selectFont;
        private static final int NUM_DOTS = 2;
        private static final String DOTS_STRING = StringUtils.repeat('.', NUM_DOTS);


        public QueryJTableCellRenderer(Font f) {
            selectFont = f;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = DEFAULT_RENDERER.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//        inquirer.logger.debug("getTableCellRendererComponent " + isSelected + " row: " + row + " col: " + column);
            Pair<Color, Color> fontBg = ((DataModel) table.getModel()).rowColors(row);
            if (fontBg == null) {
                inquirer.logger.error("Empty font, row " + row);
            } else {
                Rectangle r = table.getCellRect(row, column, true);
                if (isSelected) {
                    r.x = 0;
                    r.width = table.getWidth() - 5;
                    ((QueryJTable) table).setRect(r, fontBg.getKey());
                }
                c.setForeground(fontBg.getKey());
                c.setBackground(fontBg.getValue());

                if(value!=null) {
                    FontMetrics fontMetrics = table.getGraphics().getFontMetrics();

                    String s = value.toString();
                    if (fontMetrics.stringWidth(s) + table.getIntercellSpacing().width * NUM_DOTS >= r.getWidth()) {
                        int chars = maxChars(s, fontMetrics, Math.round(r.getWidth()), fontMetrics.charWidth('.') * NUM_DOTS + table.getIntercellSpacing().width * 2);

                        inquirer.logger.trace("chars:" + chars + " for [" + s + "] w:" + r.getWidth() + " spacing:" + table.getIntercellSpacing());
                        if (chars > 0) {
                            ((JLabel) c).setText(StringUtils.left(s, chars) + DOTS_STRING + StringUtils.right(s, chars));
                        }
                    }
                }

                c.repaint();
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
}

