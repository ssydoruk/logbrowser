/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Stepan
 */
public class FullTable {

    private final ArrayList<ArrayList<Object>> data;
    //    private ArrayList<Integer> columnTypes = null;
    private final HashSet<String> hiddenFields = new HashSet<>();
    private HashMap<String, Pair<Integer, Integer>> columnNames = null;

    //    public ArrayList<String> getColumnNames() {
//        return columnNames;
//    }
    public FullTable() {
        data = new ArrayList<>();
    }

    public ArrayList<ArrayList<Object>> getData() {
        return data;
    }

    public void setMetaData(ResultSetMetaData rsmd) throws SQLException {
        columnNames = new HashMap<>(rsmd.getColumnCount());
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
//            inquirer.logger.debug(" " + rsmd.getColumnName(i) + " " + rsmd.getColumnTypeName(i)+ " " + rsmd.getColumnType(i)
//            + " " + rsmd.getPrecision(i)+ " " + rsmd.getScale(i));
            columnNames.put(rsmd.getColumnName(i), new Pair(i - 1, rsmd.getColumnType(i)));
        }
    }

    void addAll(FullTable currTable) {
        currTable.data.stream().forEach(row->addRow(row));
    }

    public void addRow(ArrayList<Object> row) {
        data.add(row);
    }

    public int getColumnIdx(String key) {
        Pair<Integer, Integer> get = columnNames.get(key);
        if (get != null) {
            Integer aa = get.getKey();
//            inquirer.logger.info("fullTable.getColumnIdx(Parser.valName(i) " +aa);
            return get.getKey();
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
