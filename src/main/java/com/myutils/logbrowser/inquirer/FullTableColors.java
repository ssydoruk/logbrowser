/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import com.myutils.logbrowser.inquirer.gui.TabResultDataModel;

import java.awt.*;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Stepan
 */
public class FullTableColors {

    private final ArrayList<ArrayList<Object>> data;
    private final ArrayList<Pair<Color, Color>> columnColors;
    private final HashSet<String> hiddenFields = new HashSet<>();
    private int curColorIdx;
    private String fieldColorChange = null;
    private int colorIdx = -1;
    private Object prevColorCell = null;
    private ArrayList<String> columnNames = null;
    private ArrayList<Integer> columnTypes = null;

    public FullTableColors() {
        data = new ArrayList<>();
        columnColors = new ArrayList<>();
        curColorIdx = 0;
    }

    public ArrayList<ArrayList<Object>> getData() {
        return data;
    }

    public ArrayList<String> getColumnNames() {
        return columnNames;
    }

    public ArrayList<Pair<Color, Color>> getColumnColors() {
        return columnColors;
    }

    public void setMetaData(ResultSetMetaData rsmd) throws SQLException {
        columnNames = new ArrayList<>(rsmd.getColumnCount());
        columnTypes = new ArrayList<>(rsmd.getColumnCount());
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
//            inquirer.logger.debug(" " + rsmd.getColumnName(i) + " " + rsmd.getColumnTypeName(i)+ " " + rsmd.getColumnType(i)
//            + " " + rsmd.getPrecision(i)+ " " + rsmd.getScale(i));
            columnNames.add(rsmd.getColumnName(i));
            columnTypes.add(rsmd.getColumnType(i));
        }
    }

    public ArrayList<Integer> getColumnTypes() {
        return columnTypes;
    }

    void addAll(FullTableColors currTable) {
        currTable.data.stream().forEach(row->addRow(row));
    }

    public Pair<Color, Color> getRowColors(int row) {
        if (columnColors != null) {
            Pair<Color, Color> get = columnColors.get(row);
            if (get != null) {
                return get;
            }
        }
        return TabResultDataModel.getColorIdx(0);
    }

    void setFieldColorChange(String filestarttime) {
        fieldColorChange = filestarttime;
    }

    public void addRow(ArrayList<Object> row) {
        Pair<Color, Color> rowCol;
        if (colorIdx < 0) {//first row added
            if (fieldColorChange != null) {
                colorIdx = columnNames.indexOf(fieldColorChange);
            }
            curColorIdx = -1; //so that first color would be zero
        }
        if (colorIdx >= 0) {
            Object curColorChangeValue = row.get(colorIdx);
            if (prevColorCell == null || ((!checkEmpty(row.get(0)) && !checkEmpty(row.get(1))) && !curColorChangeValue.toString().equals(prevColorCell.toString()))) {
                prevColorCell = curColorChangeValue;
                curColorIdx++;
//            } else {
//                inquirer.logger.info("equal times");
            }
            if (curColorIdx < 0) {
                curColorIdx = 0;
            }
            rowCol = TabResultDataModel.getColorIdx(curColorIdx % 2 + TabResultDataModel.msgRowColorsSize());
        } else {
            rowCol = TabResultDataModel.getColorIdx(0);
        }
        data.add(row);
//        inquirer.logger.debug("curColorIdx:" + curColorIdx + " Row: " + row);
//        inquirer.logger.debug("Row color: " + rowCol);
        columnColors.add(rowCol);
    }

    public int getColumnIdx(String key) {
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnNames.get(i).equalsIgnoreCase(key)) {
                return i;
            }
        }
        return -1;
    }

    void setHiddenField(String rowType) {
        hiddenFields.add(rowType);

    }

    private boolean checkEmpty(Object obj) {
        return obj == null || obj.toString().isEmpty();
    }
}
