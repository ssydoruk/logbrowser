package com.myutils.logbrowser.inquirer.gui;

import Utils.Pair;
import com.myutils.logbrowser.inquirer.InquirerCfg;
import com.myutils.logbrowser.inquirer.InquirerFileIo;
import com.myutils.logbrowser.inquirer.LogFile;
import com.myutils.logbrowser.inquirer.MsgType;
import com.myutils.logbrowser.inquirer.OutputSpecFormatter;
import com.myutils.logbrowser.inquirer.SearchExtract;
import static com.myutils.logbrowser.inquirer.gui.TabResultDataModel.TableRow.colPrefix;
import com.myutils.logbrowser.inquirer.inquirer;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Stepan
 */
public class TabResultDataModel extends AbstractTableModel {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    /*order of columns by default*/
    private static final String[] fixedColumns = new String[]{"Type", "timestamp", "app", "direction", "name", "connid", "sid", "thisdn", "otherdn"};
    private static final HashMap<MsgType, Pair<Color, Color>> msgRowColors = initMsgColors();
    private static HashMap<MsgType, Pair<Color, Color>> msgAssignedColors = new HashMap<>();
    private static final ArrayList<Pair<Color, Color>> stdColors = initStdColors();
    private static int stdColorsIdx = 0;
    private static HashMap<Integer, Pair<Color, Color>> assignedColorsAggregate = new HashMap<>();
    private static final Pattern normalPattern = Pattern.compile("^[\\s|]*(.+)[\\s|]*$");
    /*
    1st color - foreground
    2nd color - background
    
    http://www.javascripter.net/faq/colornam.htm
    http://www.rapidtables.com/web/color/RGB_Color.htm
    */
    private static HashMap<MsgType, Pair<Color, Color>> initMsgColors() {
        HashMap<MsgType, Pair<Color, Color>> ret = new HashMap<>();
        ret.put(MsgType.TLIB, new Pair(Color.decode("#000000"), Color.decode("#FFFFFF")));
        ret.put(MsgType.SIP, new Pair(Color.decode("#000000"), Color.decode("#D3D3D3")));
        ret.put(MsgType.URSSTRATEGY, new Pair(Color.decode("#000000"), Color.decode("#D3D3D3")));
        ret.put(MsgType.ORSM, new Pair(Color.decode("#000000"), Color.decode("#BDB76B")));
        return ret;
    }
    private static ArrayList<Pair<Color, Color>> initStdColors() {
        ArrayList<Pair<Color, Color>> ret = new ArrayList<>();
        ret.add(new Pair(Color.decode("#000000"), Color.decode("#DEB887")));
        ret.add(new Pair(Color.decode("#000000"), Color.decode("#66CDAA")));
        ret.add(new Pair(Color.decode("#FFFFFF"), Color.decode("#006400")));
        ret.add(new Pair(Color.decode("#000000"), Color.decode("#FA8072")));
        ret.add(new Pair(Color.decode("#FFFFFF"), Color.decode("#800000")));
        return ret;
    }
    public static int msgRowColorsSize() {
        return msgRowColors.size();
    }
    public static Pair<Color, Color> getColorIdx(int idx) {
        Pair<Color, Color> ret = null;
        if (idx < msgRowColors.size()) {
            ret = (Pair<Color, Color>) msgRowColors.values().toArray()[idx];
        } else {
            idx -= msgRowColors.size();
            if (idx < stdColors.size()) {
                ret = (Pair<Color, Color>) stdColors.toArray()[idx];
            }
        }
        return ret;
    }

    private boolean isAggregate;
    private ArrayList<String> shortAbsoluteFileNames;
    private int emptyColumns;


    private HashMap<String, Integer> columnTitle;
    private ArrayList<TableRow> tableData;

    private int maxColumnIdx;
    private ArrayList<String> fullFileNames;
    private ArrayList<String> shortFileNames;
//    private HashSet<Integer> columnsWithData;
    private HashMap<MsgType, HashSet> columnsWithDataType;
//    private HashMap<Integer, Integer> columnIdxAdjuster;
    private HashMap<MsgType, HashMap> columnIdxAdjusterType;
    ColumnParams columnParams;
    ColumnParams columnParamsOrig;
    private TableRow currentRow = null;
    //    private int rowTypes = 0;
    MsgType lastRowType = MsgType.UNKNOWN;
    TabResultDataModel(TabResultDataModel srcModel) {
//        this.columnIdxAdjusterType = new HashMap<>(srcModel.columnIdxAdjusterType);
this.columnIdxAdjusterType = new HashMap<>();

this.columnsWithDataType = new HashMap(srcModel.columnsWithDataType);
this.tableData = new ArrayList<>();
copyData(tableData, srcModel.tableData);

this.columnTitle = new HashMap<>(srcModel.columnTitle);
columnParamsOrig = new ColumnParams(srcModel.columnParamsOrig);
columnParams = new ColumnParams(columnParamsOrig);

    }

    public TabResultDataModel() {
        this.columnIdxAdjusterType = new HashMap<>();
        this.columnsWithDataType = new HashMap();
//        this.columnIdxAdjuster = new HashMap<>();
        this.emptyColumns = -1;
        this.tableData = new ArrayList<>();
        this.columnTitle = new HashMap<>();
//        columnsWithData = new HashSet<Integer>();
        maxColumnIdx = 0;

        columnParams = new ColumnParams();
        columnParamsOrig = new ColumnParams();

    }
    public ColumnParams getColumnParams() {
        return columnParams;
    }
    public ArrayList<String> getFullFileNames() {
        return fullFileNames;
    }
    public ArrayList<String> getShortFileNames() {
        return shortFileNames;
    }

    private <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        if (isAggregate) {
            return columnTitle.size();
        }
        if (columnIdxAdjusterType.isEmpty()) {
            HashMap<Integer, Integer> fixedIdx = new HashMap<>();

            int j = 0;
            for (int i = 0; i < fixedColumns.length; i++) {
                Integer idx = columnTitle.get(fixedColumns[i]);
                if (idx != null) {
                    inquirer.logger.trace("fixedIdx: i[" + i + "] - [" + ((idx == null) ? "null" : idx) + "]");
                    fixedIdx.put(j++, idx);
                }
            }

//            columnsWithDataType
//columnIdxAdjusterType
            HashMap<Integer, String> columnTitleIdx = new HashMap<>(columnTitle.size());
            inquirer.logger.trace(columnTitle);
            for (Map.Entry<String, Integer> entry : columnTitle.entrySet()) {
                columnTitleIdx.put(entry.getValue(), entry.getKey());
            }

            for (Map.Entry<MsgType, HashSet> entry : columnsWithDataType.entrySet()) {
//                HashSet<Integer> realColumnIdx = entry.getValue();
                HashSet<Integer> realColumnIdx = columnParams.hideHidden(entry.getKey(), entry.getValue());
                inquirer.logger.trace("key: " + entry.getKey() + " cols: " + realColumnIdx);
                HashMap<Integer, Integer> columnAdjuster = new HashMap<>(realColumnIdx.size());
                //first set columns previously set by names
                for (Integer realIdx : realColumnIdx) {
                    String columnName = columnTitleIdx.get(realIdx);
                    if (!columnName.startsWith(colPrefix)) {
                        Integer prefIdx = getNamed(realIdx);
                        if (prefIdx != null) {
                            inquirer.logger.trace("columnAdjuster put: prefIdx-" + prefIdx + " realIdx-" + realIdx);
                            columnAdjuster.put(prefIdx, realIdx);
                        }
                    }
                }
//                inquirer.logger.trace("columnAdjuster after checking already set: " + columnAdjuster);

                HashMap<Integer, Integer> setSpots = new HashMap<>(columnAdjuster);
                ArrayList<Integer> colsToAdjust = new ArrayList<>();

                for (Integer realIdx : realColumnIdx) {
//                    Integer keyByValue = getKeyByValue(setSpots, realIdx);
                    if (!setSpots.containsValue(realIdx)) {
                        colsToAdjust.add(realIdx);
                    }
                }
                int curIdx = 0;
                for (int i = 0; i < colsToAdjust.size(); i++) {
                    while (setSpots.containsKey(curIdx)) {
                        curIdx++;
                    }
                    columnAdjuster.put(curIdx, colsToAdjust.get(i));
                    curIdx++;
                }
//                inquirer.logger.trace("columnAdjuster after setSpots: " + columnAdjuster);

                // now rearrange for fixedColumns
                for (Entry<Integer, Integer> entry1 : fixedIdx.entrySet()) {
//                    inquirer.logger.trace("Before adjusting fixed: " + columnAdjuster + " " + entry1);
                    columnAdjuster = adjustFixed(columnAdjuster, entry1);
//                    inquirer.logger.trace("After adjusting fixed: " + columnAdjuster + " " + entry1);
                }
                inquirer.logger.trace("columnAdjuster after adjustFixed: " + columnAdjuster);

                columnIdxAdjusterType.put(entry.getKey(), columnAdjuster);
            }
//            for (int i = 0; i < columnTitle.size(); i++) {
//                boolean get = columnsWithData.contains(i);
//                if (columnsWithData.contains(i)) {
//                    columnIdxAdjuster.put(adjustedColumn++, i);
//                }
//            }
        }

//        return columnTitle.size();
        int totalColumns = 0;
        for (HashMap value : columnIdxAdjusterType.values()) {
            if (value.size() > totalColumns) {
                totalColumns = value.size();
            }
        }
//        return columnIdxAdjuster.size();
        return totalColumns;
    }

    @Override
    public int getRowCount() {
        return tableData.size();
    }

    private Integer realColIdx(int colIdx, MsgType msgType) {
        HashMap<Integer, Integer> mapPerType = columnIdxAdjusterType.get(msgType);
        if (mapPerType != null) {
            return mapPerType.get(colIdx);

        }
        return null;
    }

    @Override
    public Object getValueAt(int rowIdx, int colIdx) {
        TableRow row = getRow(rowIdx);

        if (row != null) {
            if (isAggregate) {
                return row.getColumn(colIdx);
            } else {
                Integer adjustedCol = realColIdx(colIdx, row.getRowType());

                if (adjustedCol != null) {
                    return row.getColumn(adjustedCol.intValue());
                }
            }
        }
        return "";
    }

    // The default implementations of these methods in
    // AbstractTableModel would work, but we can refine them.
    @Override
    public String getColumnName(int column) {
        if (isAggregate) {
            return findColumnName(column);
        } else if (getColumnCount() == 1) {
            return findColumnName(column);
        }
        return "";
    }

    private String findColumnName(int column) {
        for (Map.Entry<String, Integer> entry : columnTitle.entrySet()) {
            if (entry.getValue().intValue() == column) {
                return entry.getKey();
            }
        }
        return "";
    }

    @Override
    public Class getColumnClass(int col) {
        return getValueAt(0, col).getClass();
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
    }


    Pair<Color, Color> rowColors(int row) {
        Pair<Color, Color> ret = null;
        TableRow tableRow = getRow(row);
        if (isAggregate) {
            ret = tableRow.getCellColor();
            if (ret == null) {
                ret = getColorIdx(0);
            }
        } else {
            MsgType rowType = tableRow.rowType;

            if ((ret = msgAssignedColors.get(rowType)) != null) {
                return ret;
            }
            if ((ret = msgRowColors.get(rowType)) != null) {
                msgAssignedColors.put(rowType, ret);
                return ret;
            }
            ret = stdColors.get((stdColorsIdx++) % (stdColors.size()));
            if (ret != null) {
                msgAssignedColors.put(rowType, ret);
                return ret;
            }
        }
//        if (inquirer.logger.isDebugEnabled()) {
//            if (ret != null) {
//                String lastVal = (row > 0) ? tableData.get(row - 1).getColumn(0) : "[empty]";
//                String column = tableData.get(row).getColumn(0);
//
//                inquirer.logger.debug("prev " + lastVal + " cur " + column + " row " + row + " - " + ret.getKey().toString() + " " + ret.getValue().toString());
//            }
//        }
        return ret;
    }

    TableRow getRow(int row) {
        return tableData.get(row);
    }

    public void setFullFileNames(ArrayList<String> fullStreamsFileNames) {
        if (fullStreamsFileNames != null && fullStreamsFileNames.size() > 0) {
            fullFileNames = new ArrayList<>(fullStreamsFileNames.size());
            for (String fullStreamsFileName : fullStreamsFileNames) {
                fullFileNames.add(fullStreamsFileName);
            }
        }
    }

    public void setShortFileNames(ArrayList<String> shortStreamsFileNames) {
        if (shortStreamsFileNames != null && shortStreamsFileNames.size() > 0) {
            shortFileNames = new ArrayList<>(shortStreamsFileNames.size());
            for (String shortFileName : shortStreamsFileNames) {
                shortFileNames.add(shortFileName);
            }
        }
    }


    private String normalizeCellData(String data) {
        Matcher m;
        if (data != null && !data.isEmpty() && (m = normalPattern.matcher(data)).find()) {
            return m.group(1);
        }
        return data;
    }

    public void setAggregate(boolean b) {
        isAggregate = b;
    }

    public void setShortAbsoluteFileNames(ArrayList<String> shortStreamsFileNames) {
        if (shortStreamsFileNames != null) {
            shortAbsoluteFileNames = new ArrayList<>(shortStreamsFileNames.size());
            for (String shortFileName : shortStreamsFileNames) {
                shortAbsoluteFileNames.add(shortFileName);
            }
        }
    }

    public ArrayList<String> getShortAbsoluteFileNames() {
        return shortAbsoluteFileNames;
    }

    private Integer getNamed(Integer realIdx) {
        for (HashMap<Integer, Integer> m : columnIdxAdjusterType.values()) {
            for (Map.Entry<Integer, Integer> entry : m.entrySet()) {
                if (entry.getValue().compareTo(realIdx) == 0) {
                    return entry.getKey();
                }

            }
        }
        return null;
    }

    private HashMap<Integer, Integer> adjustFixed(HashMap<Integer, Integer> columnAdjuster, Entry<Integer, Integer> fixedEntry) {
        int curIdx = -1;
        for (Entry<Integer, Integer> entry : columnAdjuster.entrySet()) {
            if (entry.getValue().intValue() == fixedEntry.getValue().intValue()) {
                if (entry.getKey().intValue() == fixedEntry.getKey().intValue()) // already on its valid space
                {
                    return columnAdjuster;
                } else {
                    curIdx = entry.getKey().intValue();
                    break;
                }
            }
        }
//        inquirer.logger.trace("curIdx: " + curIdx + " fixed: " + fixedEntry.getKey().intValue());
        if (curIdx == -1) // field not found
        {
            return columnAdjuster;
        }
        HashMap<Integer, Integer> ret = new HashMap<>(columnAdjuster.size());
        if (curIdx > fixedEntry.getKey().intValue()) {
            for (Entry<Integer, Integer> entry : columnAdjuster.entrySet()) {
//                inquirer.logger.trace("cycle greater: columnAdjuster entry " + entry + " fixed: " + fixedEntry + " curIdx: " + curIdx);
                if (entry.getKey().intValue() == curIdx) {
                    ret.put(fixedEntry.getKey(), fixedEntry.getValue());
                } else if (entry.getKey().intValue() >= fixedEntry.getKey().intValue()
                        && entry.getKey().intValue() < curIdx) {
                    ret.put(entry.getKey().intValue() + 1, entry.getValue());
                } else {
                    ret.put(entry.getKey(), entry.getValue());
                }
            }
        } else { // equal condition already taken care of
            for (Entry<Integer, Integer> entry : columnAdjuster.entrySet()) {
//                inquirer.logger.trace("cycle less: columnAdjuster entry " + entry + " fixed: " + fixedEntry + " curIdx: " + curIdx);
                if (entry.getKey().intValue() == curIdx) {
//                    inquirer.logger.trace("-1-");
                    ret.put(fixedEntry.getKey(), fixedEntry.getValue());
                } else if (entry.getKey().intValue() <= fixedEntry.getKey().intValue()
                        && entry.getKey().intValue() > curIdx) {
//                    inquirer.logger.trace("-2-");
                    ret.put(entry.getKey().intValue() - 1, entry.getValue());
                } else {
//                    inquirer.logger.trace("-3-");
                    ret.put(entry.getKey(), entry.getValue());
                }
            }
        }
//        inquirer.logger.trace("ret: " + ret);
        return ret;
    }

    ArrayList<String> exportToExcel(MyJTable tab) throws FileNotFoundException {
        return exportToExcel(tab, inquirer.getCr().getFileNameExcel());
    }

    ArrayList<String> exportToExcel(MyJTable tab, String fileName) {
        File f = new File(fileName);
        BufferedWriter out = null;

        ArrayList<String> ret = new ArrayList<>(1);
        try {
            FileWriter fstream = new FileWriter(f, false); //true tells to append data.
            out = new BufferedWriter(fstream);
            exportToExcel(tab, out, false);

        } catch (Exception e) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
                }
                ret.add(f.getAbsolutePath());
            }
        }
        return ret;
    }

    String getLogRecord(int rowIdx) {
        TableRow row = getRow(rowIdx);

        if (row != null) {
            try {
                return InquirerFileIo.GetFileBytes(row.getFileName(), row.getOffset(), row.getFileBytes());
            } catch (Exception ex) {
                return "";
            }
        }

        return "";
    }

    ArrayList<String> exportFull(MyJTable tab) {
        InquirerCfg cfg = inquirer.getConfig();
        String fileNameLong = cfg.getFileNameLong(tab);
        ArrayList<String> ret = new ArrayList<>(1);

        BufferedWriter out = null;
        try {
            FileWriter fstream = new FileWriter(fileNameLong, false); //true tells to append data.
            out = new BufferedWriter(fstream);
            exportFull(tab, out);

        } catch (Exception e) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
                }
                ret.add(fileNameLong);

            }
        }

        return ret;
    }

    StringBuilder getAllRecords(MyJTable table) throws Exception {
        StringBuilderWriter b = new StringBuilderWriter();
        exportFull(table, b);
        return b.getBuilder();
    }

    StringBuilder getAllRecordsShort(MyJTable table) throws IOException {
        StringBuilderWriter b = new StringBuilderWriter();
        exportToExcel(table, b, true);
        return b.getBuilder();
    }

    StringBuilder getAllRecordsShort(MyJTable table, String headerDelimiter, String dataDelimiter, boolean addQuotes) throws IOException {
        StringBuilderWriter b = new StringBuilderWriter();
        exportToExcel(table, b, headerDelimiter, dataDelimiter, addQuotes);
        return b.getBuilder();
    }

    private void exportFull(MyJTable tab, Writer out) throws Exception {
        for (int i = 0; i < tab.getRowCount(); i++) {
            TableRow row = getRow(tab.convertRowIndexToModel(i));
            int fileID = 0;
            int fileIDPrev = 0;
            ArrayList<Pattern> filters = null;
            if (row != null) {
                fileID = row.getFileID();
                if (fileID != fileIDPrev) {
                    filters = inquirer.getFullFilters(fileID);
                    fileIDPrev = fileID;
                }
                if (filters == null) {
                    out.write(InquirerFileIo.GetFileBytes(row.getFileName(), row.getOffset(), row.getFileBytes()));
                    out.write("\n");
                } else {
                    for (String string : StringUtils.splitPreserveAllTokens(InquirerFileIo.GetFileBytes(row.getFileName(), row.getOffset(), row.getFileBytes()), '\n')) {
                        boolean print = true;
                        for (Pattern filter : filters) {
                            if (filter.matcher(string).find()) {
                                print = false;
                                break;
                            }
                        }
                        if (print) {
                            out.write(string + "\n");
                        }
                    }
                }
            }
        }
        InquirerFileIo.doneIO();
    }

    private void exportShort(MyJTable tab, Writer out) throws Exception {
        for (int i = 0; i < tab.getRowCount(); i++) {
            TableRow row = getRow(tab.convertRowIndexToModel(i));
            int fileID = 0;
            int fileIDPrev = 0;
            ArrayList<Pattern> filters = null;
            if (row != null) {
                fileID = row.getFileID();
                if (fileID != fileIDPrev) {
                    filters = inquirer.getFullFilters(fileID);
                    fileIDPrev = fileID;
                }
                if (filters == null) {
                    out.write(InquirerFileIo.GetFileBytes(row.getFileName(), row.getOffset(), row.getFileBytes()));
                    out.write("\n");
                } else {
                    for (String string : StringUtils.splitPreserveAllTokens(InquirerFileIo.GetFileBytes(row.getFileName(), row.getOffset(), row.getFileBytes()), '\n')) {
                        boolean print = true;
                        for (Pattern filter : filters) {
                            if (filter.matcher(string).find()) {
                                print = false;
                                break;
                            }
                        }
                        if (print) {
                            out.write(string + "\n");
                        }
                    }
                }
            }
        }
        InquirerFileIo.doneIO();
    }

    private void exportToExcel(MyJTable tab, Writer out, String headerDelimiter, String dataDelimiter, boolean addQuotes) throws IOException {
        for (int j = 0; j < tab.getColumnCount(); j++) {
            TableRow row = getRow(tab.convertRowIndexToModel(0)); // get first visible row
            String colName = "";

            if (row != null) {
                HashMap<Integer, Integer> mapPerType = columnIdxAdjusterType.get(row.getRowType());
                if (mapPerType != null) {
                    Integer realColumnIdx = mapPerType.get(tab.convertColumnIndexToModel(j));
                    if (realColumnIdx != null) {
                        colName = findColumnName(realColumnIdx);
                    }
                }
            }
            if (headerDelimiter != null) {
                out.write(headerDelimiter);
            } else {
                out.write(dataDelimiter);
            }
            if (colName != null && !colName.isEmpty()) {

                if (addQuotes) {
                    out.write("\"");
                }
                out.write(colName);
                if (addQuotes) {
                    out.write("\"");
                }

            } else {
                if (addQuotes) {
                    out.write("\"");
                }
                out.write(" ");
                if (addQuotes) {
                    out.write("\"");
                }
            }

        }
        if (headerDelimiter != null) {
            out.write(headerDelimiter);
        } else {
            out.write(dataDelimiter);
        }

        out.write("\n");

        for (int i = 0; i < tab.getRowCount(); i++) {
            for (int j = 0; j < tab.getColumnCount(); j++) {
                out.write(dataDelimiter);

                if (addQuotes) {
                    out.write("\"");
                }
                String toString = getValueAt(tab.convertRowIndexToModel(i), tab.convertColumnIndexToModel(j)).toString();
                if (toString != null && !toString.isEmpty()) {
                    out.write(getValueAt(tab.convertRowIndexToModel(i), tab.convertColumnIndexToModel(j)).toString());
                } else {
                    out.write(" ");
                }
                if (addQuotes) {
                    out.write("\"");
                }

            }
            out.write(dataDelimiter);
            out.write("\n");
        }

    }

    private void exportToExcel(MyJTable tab, Writer out, boolean tabDelimiter) throws IOException {
        for (int j = 0; j < tab.getColumnCount(); j++) {
            TableRow row = getRow(0);
            String colName = "";

            if (row != null) {
                HashMap<Integer, Integer> mapPerType = columnIdxAdjusterType.get(row.getRowType());
                if (mapPerType != null) {
                    Integer realColumnIdx = mapPerType.get(tab.convertColumnIndexToModel(j));
                    if (realColumnIdx != null) {
                        colName = findColumnName(realColumnIdx);
                    }
                }
            }
            if (tabDelimiter) {
                out.write("" + colName + "\t");
            }

            out.write("\"" + colName + "\",");
        }
        out.write("\n");

        for (int i = 0; i < tab.getRowCount(); i++) {
            for (int j = 0; j < tab.getColumnCount(); j++) {
                if (tabDelimiter) {
                    out.write("" + getValueAt(tab.convertRowIndexToModel(i), tab.convertColumnIndexToModel(j)) + "\t");
                } else {
                    out.write("\"" + getValueAt(tab.convertRowIndexToModel(i), j) + "\",");
                }

            }
            out.write("\n");
        }

    }

    ArrayList<String> exportShort(MyJTable tab) {
        InquirerCfg cfg = inquirer.getConfig();
        String fileNameLong = cfg.getFileNameLong();
        ArrayList<String> ret = new ArrayList<>(1);

        BufferedWriter out = null;
        try {
            FileWriter fstream = new FileWriter(fileNameLong, false); //true tells to append data.
            out = new BufferedWriter(fstream);
            exportShort(tab, out);

        } catch (Exception e) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
                }
                ret.add(fileNameLong);

            }
        }

        return ret;
    }

    void addRegexRow(SearchExtract showFind, int popupRow) {
        TableRow row = getRow(popupRow);
        if (row == null) {
            inquirer.logger.info("Empty row");
            return;
        }
        MsgType rowType = row.getRowType();
        int maxColumn = getColumnCount();
        for (TableRow tableRow : tableData) {
            if (tableRow.getRowType() == rowType) {
                try {
                    showFind.addRxColumns(InquirerFileIo.GetFileBytes(tableRow.getFileName(), tableRow.getOffset(), tableRow.getFileBytes()),
                            tableRow, colPrefix + "rx_", maxColumn + 1);
                } catch (Exception ex) {
                    logger.log(org.apache.logging.log4j.Level.FATAL, ex);
                }
            }
        }
        refreshTable();
    }

    ArrayList<SearchExtract.SearchSample> getSampleRows(SearchExtract showFind, int popupRow, int maxRows) {
        TableRow row = getRow(popupRow);
        if (row == null) {
            inquirer.logger.info("Empty row");
            return null;
        }
        MsgType rowType = row.getRowType();
        ArrayList<SearchExtract.SearchSample> r = new ArrayList<>();
        for (TableRow tableRow : tableData) {
            if (tableRow.getRowType() == rowType) {
                try {
                    SearchExtract.SearchSample ret = showFind.findRxSample(InquirerFileIo.GetFileBytes(row.getFileName(), row.getOffset(), row.getFileBytes()));
                    inquirer.logger.debug("Found val[" + ret + "]");
                    if (ret != null) {
                        r.add(ret);
                        maxRows--;
                        if (maxRows <= 0) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    inquirer.ExceptionHandler.handleException("Failure to generate sample rows", ex);
                }
            }
        }
        if (r.isEmpty()) {
            return null;
        } else {
            return r;
        }

    }

    ArrayList<ColumnParam> getHidden(MsgType rowType) {
        if (rowType != null) {
            return columnParams.getHidden(rowType);
        } else {
            return null;
        }
    }

    MsgType getRowType(int popupRow) {
        TableRow row = getRow(popupRow);
        if (row != null) {
            return row.getRowType();
        } else {
            return null;
        }
    }

    ArrayList<ColumnParam> getHiddenOrig(MsgType t) {
        if (t != null) {
            return columnParamsOrig.getHidden(t);
        } else {
            return null;
        }
    }

    void unHideColumn(int popupRow, ColumnParam cp) {
        cp.setHidden(false);
    }

    public void refreshTable() {
        columnIdxAdjusterType.clear();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                fireTableStructureChanged();
//                fireTableDataChanged();
            }
        });
    }

    private void copyData(ArrayList<TableRow> dst, ArrayList<TableRow> src) {
        for (TableRow tableRow : src) {
            dst.add(new TableRow(tableRow));
        }
    }

    boolean hasHidden(int popupRow) {
        return numberStdHidden(popupRow) > 0;
    }

    int numberStdHidden(int popupRow) {
        MsgType rowType = getRowType(popupRow);
        if (rowType == null) {
            return 0;
        }
        ArrayList<ColumnParam> hiddenOrig = getHiddenOrig(rowType);
        return (hiddenOrig == null) ? 0 : hiddenOrig.size();
    }

    void restoreHidden(int popupRow) {
        MsgType rowType = getRowType(popupRow);
        if (rowType == null) {
            return;
        }
        ArrayList<ColumnParam> hiddenOrig = getHiddenOrig(rowType);
//        ArrayList<ColumnParam> hidden = getHidden(rowType);
//        if (hidden == null) {
        ArrayList<ColumnParam> hidden = new ArrayList<>(hiddenOrig.size());
        columnParams.put(rowType, hidden);
//        }
        for (ColumnParam columnParam : hiddenOrig) {
            addIfNotFound(hidden, columnParam);
        }
    }

    private void addIfNotFound(ArrayList<ColumnParam> hidden, ColumnParam colStd) {
        for (ColumnParam col : hidden) {
            if (col.equalCol(colStd)) {
                return;
            }
        }
        hidden.add(colStd);
    }

    void tempHide(int popupRow, int popupCol) {
        columnParams.tempHide(popupRow, popupCol);

    }

    void unhideTemps() {
        columnParams.unhideTemps();
    }


    private Integer getTitleIdx(String title) {
        Integer ret = columnTitle.get(title);
        if (ret == null) {
            if (!title.startsWith(TableRow.colPrefix) && false) {
                int maxNamed = 0;
                for (Map.Entry<String, Integer> entry : columnTitle.entrySet()) {
                    if (!entry.getKey().startsWith(TableRow.colPrefix)) {
                        if (entry.getValue() > maxNamed) {
                            maxNamed = entry.getValue();
                        }
                    } else {
                        entry.setValue(entry.getValue() + 1);
                    }
                }
                if (columnTitle.size() > 0) {
                    maxNamed++;
                }
                columnTitle.put(title, maxNamed);
            } else {
                columnTitle.put(title, maxColumnIdx);
            }
//            columnTitle.put(title, maxColumnIdx);
            maxColumnIdx++;
            return maxColumnIdx - 1;
        } else {
            return ret;
        }

    }


    public void addCell(OutputSpecFormatter.Parameter param, String data) {
        int columnIdx = addCell(param.getTitle(), data);
        columnParams.setParams(currentRow.getRowType(), columnIdx, param);
        columnParamsOrig.setParams(currentRow.getRowType(), columnIdx, param);
    }

    public int addCell(String title, String data) {
        if (tableData.isEmpty()) {
            addRow();
        }
        return currentRow.addCell(title, normalizeCellData(data));
//        int columnIdx = currentRow.addCell(title, normalizeCellData(data));

//        if (data != null && !data.trim().isEmpty()) {
////            if (!columnsWithData.contains(columnIdx)) {
////                columnsWithData.add(columnIdx);
////            }
//            HashSet hm = columnsWithDataType.get(currentRow.getRowType());
//            if (hm == null) {
//                hm = new HashSet();
//                columnsWithDataType.put(currentRow.getRowType(), hm);
//            }
//            if (!hm.contains(columnIdx)) {
//                hm.add(columnIdx);
//            }
//        }
    }

    public HashMap<MsgType, Integer> getTypeStat(JTable table) {
        HashMap<MsgType, Integer> ret = new HashMap<>();

        for (int row = 0; row < table.getRowCount(); row++) {
            TableRow tableRow = getRow(table.convertRowIndexToModel(row));
            Integer curCnt = ret.get(tableRow.getRowType());
            if (curCnt == null) {
                curCnt = new Integer(0);
            }
            ret.put(tableRow.getRowType(), curCnt.intValue() + 1);
        }
        return ret;
    }


    public TableRow addRow() {
        if (currentRow != null) {
            MsgType rowType = currentRow.getRowType();
            if (rowType != null && rowType != MsgType.UNKNOWN && lastRowType != rowType) {
                lastRowType = rowType;
//                rowTypes++;
            }
        }
        currentRow = new TableRow();
        tableData.add(currentRow);
        return currentRow;
    }

    public TableRow getCurrentRow() {
        return currentRow;
    }

    public class TableRow {

        public final static String colPrefix = "$col$";
        private int fileID = 0;
        private HashMap<Integer, Object> rowData;
        private MsgType rowType;
        private int columntIdx;
        private LogFile FileName;
        private int fileBytes;
        private int line;
        private long offset;
        private Pair<Color, Color> cellColor = null;

        private TableRow(TableRow tableRow) {
            this();
            for (Entry<Integer, Object> row : tableRow.rowData.entrySet()) {
                rowData.put(row.getKey(), row.getValue());
            }
            setFileInfo(tableRow.FileName, tableRow.fileBytes, tableRow.line, tableRow.offset);
            rowType = tableRow.rowType;
        }

        public TableRow() {
            this.rowData = new HashMap<>();
            columntIdx = 0;
        }

        @Override
        public String toString() {
            return "TableRow{" + "fileID=" + fileID + ", rowData=" + rowData + ", rowType=" + rowType + ", columntIdx=" + columntIdx + ", FileName=" + FileName + ", fileBytes=" + fileBytes + ", line=" + line + ", offset=" + offset + ", cellColor=" + cellColor + '}';
        }

        public int getFileID() {
            return fileID;
        }

        public void setFileID(int fileID) {
            this.fileID = fileID;
        }

        public int getFileBytes() {
            return fileBytes;
        }

        public MsgType getRowType() {
            return rowType;
        }

        public Pair<Color, Color> getCellColor() {
            return cellColor;
        }

        public void setCellColor(Pair<Color, Color> cellColor) {
            this.cellColor = cellColor;
        }

        public int size() {
            return rowData.size();
        }

        public int addCell(String title, String data) {
            int columnIdx = 0;
            if (isAggregate) {
                if (data != null && !data.isEmpty() && (title == null || !title.equals("filename"))) {
                    columnIdx = getTitleIdx(title);
                    rowData.put(columnIdx, data);
                }
            } else {
                if ((title == null || !title.equals("filename"))) {
//            if (data != null && !data.isEmpty() && (title==null || !title.equals("filename"))) {
//                rowData.put(getTitleIdx(title), data);
//                    columnIdx = getTitleIdx("col" + columntIdx);
String t;

if (title != null && !title.isEmpty()) {
    t = title;
} else {
    t = colPrefix + columntIdx;
}
//                    t = colPrefix + columntIdx;

if (data != null && !data.isEmpty()) {
    columnIdx = getTitleIdx(t);
    String curData = (String) rowData.get(columnIdx);
    if (curData == null || curData.isEmpty()) {
        putRowData(columnIdx, t, data);
    } else {
        putRowData(columnIdx, t, curData + " | " + data);
        
    }
    HashSet hm = columnsWithDataType.get(getRowType());
    if (hm == null) {
        hm = new HashSet();
        columnsWithDataType.put(getRowType(), hm);
    }
    if (!hm.contains(columnIdx)) {
        hm.add(columnIdx);
    }
}
columntIdx++;
                }
            }
            return columnIdx;
        }

        private String getColumn(int colIdx) {
            Object ret = rowData.get(colIdx);
            if (ret != null && (ret instanceof String) && !((String) ret).isEmpty()) {
                return (String) ret;
            }
            return "";
        }

        public void setType(MsgType GetType) {
            this.rowType = GetType;
        }

        public LogFile getFileName() {
            return FileName;
        }

        public int getLine() {
            return line;
        }

        public void setFileInfo(LogFile GetFileName, int GetFileBytes, int GetLine, long _offset) {
            this.FileName = GetFileName;
//            inquirer.logger.info("b["+GetFileName+"] a["+this.FileName+"]");
this.fileBytes = GetFileBytes;
this.line = GetLine;
this.offset = _offset;
        }

        public long getOffset() {
            return offset;
        }

        private void putRowData(int columnIdx, String t, String data) {
            if (inquirer.logger.isTraceEnabled()) {
                rowData.put(columnIdx, t + "[" + data + "]");
            } else {
                rowData.put(columnIdx, data);
            }
        }
    }

    class FieldParams {

        private boolean hidden;
        private boolean tempHidden;
        private String title;
        private String type;

        public FieldParams() {
            tempHidden = false;

        }

        public FieldParams(FieldParams src) {
            this();
            title = src.title;
            type = src.type;
            hidden = src.hidden;
        }

        public FieldParams(OutputSpecFormatter.Parameter prm) {
            this();
            this.hidden = prm.isHidden();
            this.title = prm.getTitle();
            this.type = prm.getType();
        }

        public boolean isTempHidden() {
            return tempHidden;
        }

        public void setTempHidden(boolean tempHidden) {
            this.tempHidden = tempHidden;
        }

        public String getType() {
            return type;
        }

        public boolean isHidden() {
            return hidden || tempHidden;
        }

        public String getTitle() {
            return title;
        }

        public void setHidden(boolean isHidden) {
            hidden = isHidden;
            tempHidden = isHidden;
        }
    }
    class ColumnParam extends Pair<Integer, FieldParams> {

        private ColumnParam(int columnIdx, OutputSpecFormatter.Parameter param) {
            super(columnIdx, new FieldParams(param));
        }

        private ColumnParam(ColumnParam columnParam) {
            super(columnParam.getKey(), new FieldParams(columnParam.getValue()));
        }

        String menuName() {
            FieldParams value = getValue();
            return value.getTitle() + "(" + value.getType() + ")";
        }

        private void setHidden(boolean isHidden) {
            getValue().setHidden(isHidden);
        }

        private boolean equalCol(ColumnParam colStd) {
            return getKey().intValue() == colStd.getKey().intValue();
        }
    }

    private class ColumnParams extends HashMap<MsgType, ArrayList<ColumnParam>> {

        public ColumnParams() {
        }

        private ColumnParams(ColumnParams columnParams) {
            this();
            for (Entry<MsgType, ArrayList<ColumnParam>> entry : columnParams.entrySet()) {
                put(entry.getKey(), dupl(entry.getValue()));
            }
        }

        private ArrayList<ColumnParam> dupl(ArrayList<ColumnParam> src) {
            ArrayList<ColumnParam> ret = new ArrayList<>(src.size());
            for (ColumnParam columnParam : src) {
                ret.add(new ColumnParam(columnParam));
            }
            return ret;

        }

        private void setParams(MsgType rowType, int columnIdx, OutputSpecFormatter.Parameter param) {
            ArrayList<ColumnParam> map = get(rowType);
            if (map == null) {
                map = new ArrayList<>();
            }
            addToMap(map, columnIdx, param);
            put(rowType, map);
        }

        private HashSet<Integer> hideHidden(MsgType key, HashSet<Integer> value) {
            ArrayList<ColumnParam> paramMap = get(key);
            if (paramMap == null) {
                return value;
            } else {
                HashSet<Integer> ret = new HashSet<>();
                for (Integer colIdx : value) {
                    if (!hidden(paramMap, colIdx)) {
                        ret.add(colIdx);
                    }
                }
                return ret;
            }
        }

        ArrayList<ColumnParam> getHidden(MsgType rowType) {
            ArrayList<ColumnParam> get = get(rowType);
            if (get != null) {
                ArrayList<ColumnParam> ret = new ArrayList<>();
                for (ColumnParam pair : get) {
                    if (pair.getValue().isHidden()) {
                        ret.add(pair);
                    }
                }
                if (!ret.isEmpty()) {
                    return ret;
                }
            }
            return null;
        }

        private void addToMap(ArrayList<ColumnParam> map, int columnIdx, OutputSpecFormatter.Parameter param) {
            for (ColumnParam pair : map) {
                if (pair.getKey() == columnIdx) {
                    return;
                }
            }
            map.add(new ColumnParam(columnIdx, param));
        }

        private boolean hidden(ArrayList<ColumnParam> paramMap, Integer colIdx) {
            for (ColumnParam pair : paramMap) {
                if (pair.getKey() == colIdx) {
                    return pair.getValue().isHidden();
                }
            }
            return false;
        }

        private void tempHide(int popupRow, int popupCol) {
            MsgType rowType = getRowType(popupRow);
            if (rowType != null) {
                ArrayList<ColumnParam> colParams = get(rowType);
                if (colParams != null) {
                    Integer realColIdx = realColIdx(popupCol, rowType);
                    if (realColIdx != null) {
                        for (ColumnParam pair : colParams) {
                            if (pair.getKey().intValue() == realColIdx.intValue()) {
                                pair.getValue().setTempHidden(true);
                                return;
                            }
                        }
                    }
                }
            }
        }

        private void unhideTemps() {
            for (Entry<MsgType, ArrayList<ColumnParam>> entry : this.entrySet()) {
                ArrayList<ColumnParam> value = entry.getValue();
                for (ColumnParam columnParam : value) {
                    columnParam.getValue().setTempHidden(false);
                }
            }
        }
    }

}
