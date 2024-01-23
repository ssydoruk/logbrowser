package com.myutils.logbrowser.inquirer.gui;

import Utils.Pair;
import com.myutils.logbrowser.inquirer.*;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.myutils.logbrowser.inquirer.gui.TabResultDataModel.TableRow.colPrefix;

/**
 * @author Stepan
 */
public class TabResultDataModel extends AbstractTableModel {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    /*order of columns by default*/
    private static final String[] fixedColumns = new String[]{"Type", "timestamp", "app", "direction", "name", "connid", "sid", "thisdn", "otherdn"};
    private static final HashMap<MsgType, Pair<Color, Color>> msgRowColors = initMsgColors();
    private static final HashMap<MsgType, Pair<Color, Color>> msgAssignedColors = new HashMap<>();
    private static final ArrayList<Pair<Color, Color>> stdColors = initStdColors();
    private static final Matcher normalPattern = Pattern.compile("^[\\s|]*(.+)[\\s|]*$").matcher("");
    private static final String VALUE_KEY = "value";
    private static final String HIDDEN_KEY = "hidden";
    private static int stdColorsIdx = 0;
    private final HashMap<String, Integer> columnTitle;
    private final ArrayList<TableRow> tableData;
    private final HashMap<MsgType, HashSet> columnsWithDataType;
    private final HashMap<MsgType, HashMap> columnIdxAdjusterType;
    private final ColumnParams columnParamsOrig;
    private final HashMap<MsgType, HashMap<Integer, CustomField>> customFields = new HashMap<>();
    MsgType lastRowType = MsgType.UNKNOWN;
    ArrayList<CustomField> searchParams = new ArrayList<>();
    private boolean isAggregate;
    private ArrayList<String> shortAbsoluteFileNames;
    private int maxColumnIdx;
    private ArrayList<String> fullFileNames;
    private ArrayList<String> shortFileNames;
    private ColumnParams columnParams;
    private TableRow currentRow = null;

    TabResultDataModel(MyJTable srcTab, boolean isFullClone) {
        super();
        this.columnIdxAdjusterType = new HashMap<>();

        this.columnsWithDataType = new HashMap<>(((TabResultDataModel) srcTab.getModel()).columnsWithDataType);
        this.tableData = new ArrayList<>();
        copyData(tableData, srcTab, isFullClone);

        this.columnTitle = new HashMap<>(((TabResultDataModel) srcTab.getModel()).columnTitle);
        columnParamsOrig = new ColumnParams(((TabResultDataModel) srcTab.getModel()).columnParamsOrig);
        columnParams = new ColumnParams(columnParamsOrig);

    }

    public TabResultDataModel() {
        super();
        this.columnIdxAdjusterType = new HashMap<>();
        this.columnsWithDataType = new HashMap<>();
        this.tableData = new ArrayList<>();
        this.columnTitle = new HashMap<>();
        maxColumnIdx = 0;

        columnParams = new ColumnParams();
        columnParamsOrig = new ColumnParams();

    }

    /*
    1st color - foreground
    2nd color - background

    http://www.javascripter.net/faq/colornam.htm
    http://www.rapidtables.com/web/color/RGB_Color.htm
     */
    private static HashMap<MsgType, Pair<Color, Color>> initMsgColors() {
        HashMap<MsgType, Pair<Color, Color>> ret = new HashMap<>();
        ret.put(MsgType.TLIB, new Pair<>(Color.decode("#000000"), Color.decode("#FFFFFF")));
        ret.put(MsgType.SIP, new Pair<>(Color.decode("#000000"), Color.decode("#D3D3D3")));
        ret.put(MsgType.URSSTRATEGY, new Pair<>(Color.decode("#000000"), Color.decode("#D3D3D3")));
        ret.put(MsgType.ORSM, new Pair<>(Color.decode("#000000"), Color.decode("#BDB76B")));
        return ret;
    }

    private static ArrayList<Pair<Color, Color>> initStdColors() {
        ArrayList<Pair<Color, Color>> ret = new ArrayList<>();
        ret.add(new Pair<>(Color.decode("#000000"), Color.decode("#DEB887")));
        ret.add(new Pair<>(Color.decode("#000000"), Color.decode("#66CDAA")));
        ret.add(new Pair<>(Color.decode("#FFFFFF"), Color.decode("#006400")));
        ret.add(new Pair<>(Color.decode("#000000"), Color.decode("#FA8072")));
        ret.add(new Pair<>(Color.decode("#FFFFFF"), Color.decode("#800000")));
        return ret;
    }

    public static int msgRowColorsSize() {
        return msgRowColors.size();
    }

    public static Pair<Color, Color> getColorIdx(int idx) {
        Pair<Color, Color> ret = null;
        if (idx < msgRowColors.size()) {
            ret = (Pair<Color, Color>) (Pair<Color, Color>) msgRowColors.values().toArray()[idx];
        } else {
            idx -= msgRowColors.size();
            if (idx < stdColors.size()) {
                ret = (Pair<Color, Color>) stdColors.toArray()[idx];
            }
        }
        return ret;
    }

    final public ColumnParams getColumnParams() {
        return columnParams;
    }

    public ArrayList<String> getFullFileNames() {
        return fullFileNames;
    }

    final public void setFullFileNames(ArrayList<String> fullStreamsFileNames) {
        if (fullStreamsFileNames != null && fullStreamsFileNames.size() > 0) {
            fullFileNames = new ArrayList<>(fullStreamsFileNames.size());
            for (String fullStreamsFileName : fullStreamsFileNames) {
                fullFileNames.add(fullStreamsFileName);
            }
        }
    }

    public ArrayList<String> getShortFileNames() {
        return shortFileNames;
    }

    public void setShortFileNames(ArrayList<String> shortStreamsFileNames) {
        if (shortStreamsFileNames != null && shortStreamsFileNames.size() > 0) {
            shortFileNames = new ArrayList<>(shortStreamsFileNames.size());
            for (String shortFileName : shortStreamsFileNames) {
                shortFileNames.add(shortFileName);
            }
        }
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
                    inquirer.logger.trace("fixedIdx: i[" + i + "] - [" + idx + "]");
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
                HashSet<Integer> realColumnIdx =  columnParams.hideHidden(entry.getKey(), entry.getValue());
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
        HashMap<Integer, Integer> mapPerType =  columnIdxAdjusterType.get(msgType);
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
//                    HashMap<Integer, CustomField> customField = customFields.get(row.getRowType());
//                    if (customField != null) {
//                        logger.debug("customField: " + customField + "colIdx:" + colIdx + " adj:" + adjustedCol);
//                        CustomField fld = customField.get(adjustedCol);
////                        logger.debug("fld: " + fld);
//                        if (fld != null) {
//                            return fld.getCustomValue(
//                                    InquirerFileIo.GetFileBytes(row.getFileName(), row.getOffset(), row.getFileBytes()));
//                        }
//                    }
                    return row.getColumn(adjustedCol);
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
            if (entry.getValue() == column) {
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
        Pair<Color, Color> ret;
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

    private String normalizeCellData(String data) {
        Matcher m;
        if (data != null && !data.isEmpty() && (m = normalPattern.reset(data)).find()) {
            return m.group(1);
        }
        return data;
    }

    public void setAggregate(boolean b) {
        isAggregate = b;
    }

    public ArrayList<String> getShortAbsoluteFileNames() {
        return shortAbsoluteFileNames;
    }

    public void setShortAbsoluteFileNames(ArrayList<String> shortStreamsFileNames) {
        if (shortStreamsFileNames != null) {
            shortAbsoluteFileNames = new ArrayList<>(shortStreamsFileNames.size());
            for (String shortFileName : shortStreamsFileNames) {
                shortAbsoluteFileNames.add(shortFileName);
            }
        }
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
                    curIdx = entry.getKey();
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
        if (curIdx > fixedEntry.getKey()) {
            for (Entry<Integer, Integer> entry : columnAdjuster.entrySet()) {
//                inquirer.logger.trace("cycle greater: columnAdjuster entry " + entry + " fixed: " + fixedEntry + " curIdx: " + curIdx);
                if (entry.getKey() == curIdx) {
                    ret.put(fixedEntry.getKey(), fixedEntry.getValue());
                } else if (entry.getKey() >= fixedEntry.getKey()
                        && entry.getKey() < curIdx) {
                    ret.put(entry.getKey() + 1, entry.getValue());
                } else {
                    ret.put(entry.getKey(), entry.getValue());
                }
            }
        } else { // equal condition already taken care of
            for (Entry<Integer, Integer> entry : columnAdjuster.entrySet()) {
//                inquirer.logger.trace("cycle less: columnAdjuster entry " + entry + " fixed: " + fixedEntry + " curIdx: " + curIdx);
                if (entry.getKey() == curIdx) {
//                    inquirer.logger.trace("-1-");
                    ret.put(fixedEntry.getKey(), fixedEntry.getValue());
                } else if (entry.getKey() <= fixedEntry.getKey()
                        && entry.getKey() > curIdx) {
//                    inquirer.logger.trace("-2-");
                    ret.put(entry.getKey() - 1, entry.getValue());
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

        } catch (IOException e) {
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
            int fileID;
            int fileIDPrev = 0;
            List<Pattern> filters = null;
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
            int fileID;
            int fileIDPrev = 0;
            List<Pattern> filters = null;
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
                HashMap<Integer, Integer> mapPerType = (HashMap<Integer, Integer>) columnIdxAdjusterType.get(row.getRowType());
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
                out.write(colName + "\t");
            }

            out.write("\"" + colName + "\",");
        }
        out.write("\n");

        for (int i = 0; i < tab.getRowCount(); i++) {
            for (int j = 0; j < tab.getColumnCount(); j++) {
                if (tabDelimiter) {
                    out.write(getValueAt(tab.convertRowIndexToModel(i), tab.convertColumnIndexToModel(j)) + "\t");
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

    public void addRxColumns(String GetFileBytes, TabResultDataModel.TableRow tableRow, String colPrefix, int startIdx) {
        int i = 0;
        for (CustomField searchParam : searchParams) {
            String ret = searchParam.getCustomValue(GetFileBytes);
            inquirer.logger.debug("Found val[" + ret + "] rowSize:" + tableRow.size());
            tableRow.addCell(colPrefix + startIdx + i, (StringUtils.isNotBlank(ret)) ? ret : "");
            i++;
        }
    }

    void addCustomColumn(CustomField regexFieldsSetting, int popupRow) {
        TableRow row = getRow(popupRow);
        if (row == null) {
            inquirer.logger.info("Empty row");
            return;
        }
        MsgType rowType = row.getRowType();
        HashMap<Integer, CustomField> rowMapping;
        if ((rowMapping = customFields.get(rowType)) == null) {
            rowMapping = new HashMap<>();
            customFields.put(rowType, rowMapping);
        }
        int maxColumn = row.getMaxColumn() + 1; // this assumes that all rows of the same type has the same number of columns
        rowMapping.put(maxColumn, regexFieldsSetting);

        if (regexFieldsSetting.isJSField()) {
            for (TableRow tableRow : tableData) {
                if (tableRow.getRowType() == rowType) {
                    try {
                        tableRow.addCell(rowType.toString() + "_c" + maxColumn, regexFieldsSetting.getCustomValue(tableRow.getRecord()));

                    } catch (Exception ex) {
                        logger.error("fatal: ", ex);
                    }
                }
            }
        } else {
            for (TableRow tableRow : tableData) {
                if (tableRow.getRowType() == rowType) {
                    try {
                        tableRow.addCell(rowType.toString() + "_c" + maxColumn, regexFieldsSetting.getCustomValue(tableRow.getBytes()));

                    } catch (Exception ex) {
                        logger.error("fatal: ", ex);
                    }
                }
            }
        }

        refreshTable();
    }

    void addCustomColumn(EditRegexFields showFind, int popupRow) {
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
                    addRxColumns(InquirerFileIo.GetFileBytes(tableRow.getFileName(), tableRow.getOffset(), tableRow.getFileBytes()),
                            tableRow, colPrefix + "rx_", maxColumn + 1);
                } catch (Exception ex) {
                    logger.error("fatal: ", ex);
                }
            }
        }
        refreshTable();
    }

    /**
     * @param row - adjusted for table.convertRowIndexToModel(row)
     * @return
     */
    public String getFullRecord(int row) {

        TableRow tableRow = getRow(row);
        if (tableRow == null) {
            inquirer.logger.info("Empty row");
            return null;
        }

        return InquirerFileIo.GetFileBytes(tableRow.getFileName(), tableRow.getOffset(), tableRow.getFileBytes());

    }

    //    ArrayList<EditRegexFields.SearchSample> getSampleRows(EditRegexFields showFind, int popupRow, int maxRows) {
//        TableRow row = getRow(popupRow);
//        if (row == null) {
//            inquirer.logger.info("Empty row");
//            return null;
//        }
//        MsgType rowType = row.getRowType();
//        ArrayList<EditRegexFields.SearchSample> r = new ArrayList<>();
//        for (TableRow tableRow : tableData) {
//            if (tableRow.getRowType() == rowType) {
//                try {
//                    EditRegexFields.SearchSample ret = showFind.findRxSample(InquirerFileIo.GetFileBytes(row.getFileName(), row.getOffset(), row.getFileBytes()));
//                    inquirer.logger.debug("Found val[" + ret + "]");
//                    if (ret != null) {
//                        r.add(ret);
//                        maxRows--;
//                        if (maxRows <= 0) {
//                            break;
//                        }
//                    }
//                } catch (Exception ex) {
//                    inquirer.ExceptionHandler.handleException("Failure to generate sample rows", ex);
//                }
//            }
//        }
//        if (r.isEmpty()) {
//            return null;
//        } else {
//            return r;
//        }
//
//    }
    ArrayList<FieldParams> getHidden(MsgType rowType) {
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

    private void copyData(ArrayList<TableRow> dst, MyJTable srcTab, boolean isFullClone) {
        TabResultDataModel srcModel = (TabResultDataModel) srcTab.getModel();
        if (isFullClone) {
            for (TableRow tableRow : srcModel.tableData) {
                dst.add(new TableRow(tableRow));
            }
        } else {
            for (int i = 0; i < srcModel.getRowCount(); i++) {
                if (srcTab.convertRowIndexToView(i) >= 0) {
                    dst.add(new TableRow(srcModel.tableData.get(i)));
                }
            }
        }
    }

    boolean hasHidden(int popupRow) {
        return numberStdHidden(popupRow) > 0;
    }

    int numberStdHidden(int popupRow) {
        MsgType rowType = getRowType(popupRow);
        return (rowType == null) ? 0
                : columnParamsOrig.countHidden(rowType);
    }

    void restoreHidden(int popupRow) {
        MsgType rowType = getRowType(popupRow);
        if (rowType == null) {
            return;
        }
        columnParams = new ColumnParams(columnParamsOrig);
    }

    //    private void addIfNotFound(ArrayList<ColumnParam> hidden, ColumnParam colStd) {
//        for (ColumnParam col : hidden) {
//            if (col.equalCol(colStd)) {
//                return;
//            }
//        }
//        hidden.add(colStd);
//    }
    void tempHide(int popupRow, int popupCol) {
        columnParams.tempHide(popupRow, popupCol);

    }

    void unhideTemps() {
        columnParams.unhideTemps();
    }

    private Integer getTitleIdx(String title) {
        Integer ret = columnTitle.get(title);
        if (ret == null) {
            if (false) {
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
        int columnIdx;
        if ((columnIdx = addCell(param.getTitle(), data)) >= 0) {
            columnParams.setParams(currentRow.getRowType(), columnIdx, param);
            columnParamsOrig.setParams(currentRow.getRowType(), columnIdx, param);
        }
    }

    public void addCell(String title, Object s) {
        if (s instanceof String) {
            addCell(title, (String) s);
        } else if (s instanceof Map) {
            Map m = ((Map) s);
            Object s1;
            if ((s1 = m.get(VALUE_KEY)) != null) {
                int columnIdx;
                if ((columnIdx = addCell(title, (String) s1)) > 0 && (s1 = m.get(HIDDEN_KEY)) != null) {
                    boolean b = (s1 instanceof Boolean) ? (Boolean) s1 : Boolean.parseBoolean((String) s1);
                    columnParams.setParams(currentRow.getRowType(), columnIdx, b, title, "js");
                    columnParamsOrig.setParams(currentRow.getRowType(), columnIdx, b, title, "js");
                }
            } else {
                inquirer.logger.error("'value' key in added fields needs to be specified");
            }
        } else {
            inquirer.logger.error("Unknown type of object for [" + title + "] value[" + s + ']');

        }
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
                curCnt = 0;
            }
            ret.put(tableRow.getRowType(), curCnt + 1);
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

    public void ignoreRecord() {
        tableData.remove(currentRow);
    }

    public final class TableRow {

        public final static String colPrefix = "$col$";
        private final HashMap<Integer, Object> rowData;
        private int fileID = 0;
        private MsgType rowType;
        private int maxColumn;
        private ILogRecord record;
        private LogFile FileName;
        private int fileBytes;
        private int line;
        private long offset;
        private Pair<Color, Color> cellColor = null;

        private TableRow(TableRow tableRow) {
            this();
            for (Entry<Integer, Object> row : tableRow.rowData.entrySet()) {
                Object val = row.getValue();
                if (val != null) {
                    if (val instanceof String) {
                        if (StringUtils.isNotBlank((String) val)) {
                            rowData.put(row.getKey(), val);
                        } else {
                            System.out.println("blank");
                        }
                    } else {
                        rowData.put(row.getKey(), val);
                    }
                } else {
                    System.out.println("null");
                }
            }
            setFileInfo(tableRow.FileName, tableRow.fileBytes, tableRow.line, tableRow.offset);
            rowType = tableRow.rowType;
        }

        public TableRow() {
            this.rowData = new HashMap<>();
            maxColumn = 0;
        }

        public int getMaxColumn() {
            return maxColumn;
        }

        public String getBytes() {
            return InquirerFileIo.GetFileBytes(getFileName(), getOffset(), getFileBytes());
        }

        @Override
        public String toString() {
            return "TableRow{" + "fileID=" + fileID + ", rowData=" + rowData + ", rowType=" + rowType + ", columntIdx=" + maxColumn + ", FileName=" + FileName + ", fileBytes=" + fileBytes + ", line=" + line + ", offset=" + offset + ", cellColor=" + cellColor + '}';
        }

        public HashMap<Integer, Object> getRowData() {
            return rowData;
        }

        public ILogRecord getRecord() {
            return record;
        }

        public void setRecord(ILogRecord record) {
            this.record = record;
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

        public int addCell(String title, String _data) {
            int columnIdx = -1;
            if (StringUtils.isBlank(_data)) {
                return -1;
            }
            String data = StringUtils.trimToEmpty(StringUtils.defaultIfBlank(_data, "").replaceAll("\\P{Print}", ""));

            if (isAggregate) {
                if (!data.isEmpty() && (title == null || !title.equals("filename"))) {
                    columnIdx = getTitleIdx(title);
                    rowData.put(columnIdx, data);
                }
            } else {
                if ((title == null || !title.equals("filename"))) {
//            if (data != null && !data.isEmpty() && (title==null || !title.equals("filename"))) {
//                rowData.put(getTitleIdx(title), data);
//                    columnIdx = getTitleIdx("col" + columntIdx);
                    String t;

                    t = StringUtils.defaultIfBlank(title, colPrefix + maxColumn);
//                    t = colPrefix + columntIdx;

                    columnIdx = getTitleIdx(t);
                    String curData = (String) rowData.get(columnIdx);

                    if (StringUtils.isBlank(curData)) {
                        putRowData(columnIdx, t, data);
                    } else if (data.length() > 0) {
                        putRowData(columnIdx, t, StringUtils.join(new String[]{curData, data}, " | "));
                    }

                    HashSet hm = columnsWithDataType.get(getRowType());
                    if (hm == null) {
                        hm = new HashSet();
                        columnsWithDataType.put(getRowType(), hm);
                    }
                    hm.add(columnIdx);
                    maxColumn++;
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

        public void setFileInfo(TableRow tableRow) {
            setFileInfo(tableRow.getFileName(), tableRow.getFileBytes(), tableRow.getLine(), tableRow.getOffset());
        }
    }

    class FieldParams {

        private boolean hidden;
        private String title;
        private String type;

        public FieldParams() {

        }

        public FieldParams(FieldParams src) {
            this();
            title = src.title;
            type = src.type;
            hidden = src.hidden;
        }

        public FieldParams(
                boolean _hidden,
                String _title,
                String _type) {
            this();
            this.hidden = _hidden;
            this.title = _title;
            this.type = _type;
        }

        public FieldParams(OutputSpecFormatter.Parameter prm) {
            this(prm.isHidden(),
                    prm.getTitle(),
                    prm.getType());
        }

        public String menuName() {
            return getTitle() + "(" + getType() + ")";
        }

        public String getType() {
            return type;
        }

        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean isHidden) {
            hidden = isHidden;
        }

        public String getTitle() {
            return title;
        }
    }

    //    class ColumnParam extends Pair<Integer, FieldParams> {
//
//        private ColumnParam(int columnIdx,
//                boolean _hidden,
//                String _title,
//                String _type
//        ) {
//            super(columnIdx, new FieldParams(
//                    _hidden,
//                    _title,
//                    _type
//            ));
//        }
//
//        private ColumnParam(int columnIdx, OutputSpecFormatter.Parameter param) {
//            super(columnIdx, new FieldParams(param));
//        }
//
//        private ColumnParam(ColumnParam columnParam) {
//            super(columnParam.getKey(), new FieldParams(columnParam.getValue()));
//        }
//
//        String menuName() {
//            FieldParams value = getValue();
//            return value.getTitle() + "(" + value.getType() + ")";
//        }
//
//        private void setHidden(boolean isHidden) {
//            getValue().setHidden(isHidden);
//        }
//
//        private boolean equalCol(ColumnParam colStd) {
//            return getKey().intValue() == colStd.getKey().intValue();
//        }
//    }
    private class ColumnParams extends HashMap<MsgType, HashMap<Integer, FieldParams>> {

        public ColumnParams() {
        }

        private ColumnParams(ColumnParams columnParams) {
            this();
            for (Entry<MsgType, HashMap<Integer, FieldParams>> theColumn : columnParams.entrySet()) {
                HashMap<Integer, FieldParams> value = theColumn.getValue();
                HashMap<Integer, FieldParams> newVal = new HashMap<>(value.size());
                for (Entry<Integer, FieldParams> theField : value.entrySet()) {
                    newVal.put(theField.getKey(), new FieldParams(theField.getValue()));
                }
                put(theColumn.getKey(), newVal);
            }
        }

        //        private ArrayList<ColumnParam> dupl(ArrayList<ColumnParam> src) {
//            ArrayList<ColumnParam> ret = new ArrayList<>(src.size());
//            for (ColumnParam columnParam : src) {
//                ret.add(new ColumnParam(columnParam));
//            }
//            return ret;
//
//        }
        private void setParams(MsgType rowType, int columnIdx, OutputSpecFormatter.Parameter param) {
            HashMap<Integer, FieldParams> map = getFieldsMap(rowType);

            if (!map.containsKey(columnIdx)) {
                map.put(columnIdx, new FieldParams(param));
            }
        }

        private HashMap<Integer, FieldParams> getFieldsMap(MsgType rowType) {
            HashMap<Integer, FieldParams> map = get(rowType);
            if (map == null) {
                map = new HashMap<>();
                put(rowType, map);
            }
            return map;
        }

        private void setParams(MsgType rowType, int columnIdx, boolean _hidden,
                String _title,
                String _type) {
            HashMap<Integer, FieldParams> map = getFieldsMap(rowType);

            if (!map.containsKey(columnIdx)) {
                map.put(columnIdx, new FieldParams(
                        _hidden,
                        _title,
                        _type
                ));
            }
        }

        private HashSet<Integer> hideHidden(MsgType key, HashSet<Integer> value) {
            HashMap<Integer, FieldParams> paramMap = get(key);
            if (paramMap == null) {
                return value;
            } else {
                HashSet<Integer> ret = new HashSet<>();
                for (Integer colIdx : value) {
                    FieldParams get = paramMap.get(colIdx);
                    if (get == null || !get.isHidden()) {
                        ret.add(colIdx);
                    }
                }
                return ret;
            }
        }

        //        private boolean addToMap(ArrayList<ColumnParam> map, int columnIdx, OutputSpecFormatter.Parameter param) {
//            for (ColumnParam pair : map) {
//                if (pair.getKey() == columnIdx) {
//                    return false;
//                }
//            }
//            map.add(new ColumnParam(columnIdx, param));
//            return true;
//        }
//
//        private boolean hidden(ArrayList<ColumnParam> paramMap, Integer colIdx) {
//            for (ColumnParam pair : paramMap) {
//                if (Objects.equals(pair.getKey(), colIdx)) {
//                    return pair.getValue().isHidden();
//                }
//            }
//            return false;
//        }
        private void tempHide(int popupRow, int popupCol) {
            MsgType rowType = getRowType(popupRow);
            if (rowType != null) {
                HashMap<Integer, FieldParams> colParams = get(rowType);
                if (colParams != null) {
                    Integer realColIdx = realColIdx(popupCol, rowType);
                    if (realColIdx != null) {
                        FieldParams get = colParams.get(realColIdx);
                        if (get != null) {
                            get.setHidden(true);
                        }
                    }
                }
            }
        }

        private void unhideTemps() {
            for (Entry<MsgType, HashMap<Integer, FieldParams>> entry : this.entrySet()) {
                HashMap<Integer, FieldParams> value = entry.getValue();

//                for ( columnParam : value.) {
//                    columnParam.getValue().setTempHidden(false);
//                }
            }
        }

        private ArrayList<FieldParams> getHidden(MsgType rowType) {
            HashMap<Integer, FieldParams> get = get(rowType);
            ArrayList<FieldParams> ret = new ArrayList<>();
            if (get != null) {
                for (FieldParams value : get.values()) {
                    if (value.isHidden()) {
                        ret.add(value);
                    }
                }
            }
            return ret;
        }

        public int countHidden(MsgType rowType) {
            HashMap<Integer, FieldParams> get = get(rowType);
            int ret = 0;
            if (get != null) {
                for (FieldParams value : get.values()) {
                    if (value.isHidden()) {
                        ret++;
                    }
                }
            }
            return ret;
        }
    }

}
