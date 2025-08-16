/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.Util;
import com.google.gson.annotations.Expose;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.gui.MyJTable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.myutils.logbrowser.inquirer.QueryTools.getRefNames;

/**
 * @author ssydoruk
 */
public class InquirerCfg implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final HashSet<Character> filenameProhibited = new HashSet<Character>(
            Arrays.asList(':',
                    '\"',
                    '/',
                    '\\',
                    '|',
                    '?',
                    '*',
                    '<',
                    '>'));

    HashMap<MsgType, ArrayList<CustomField>> customFieldsSettings = null;

    private HashMap<ReferenceType, ArrayList<OptionNode>> refsChecked = new HashMap<>(20);

    private HashMap<TableType, ArrayList<OptionNode>> logMessages = new HashMap<>();

    private int maxRecords;

    private boolean LimitQueryResults;

    private int MaxQueryLines;

    private String titleRegex;

    private String linuxEditor;

    private boolean fullTimeStamp;

    private String FileNameExcel;

    private int fileSizeWarn = 5; // in mb

    private GenesysConstants1 constants;

    private boolean refsLoaded;

    private boolean newTlibSearch;

    private boolean saveFileShort;

    private boolean saveFileLong;

    private boolean addShortToFull;

    private boolean applyFullFilter;

    private boolean addOrderBy;

    private boolean accessLogFiles;

    private boolean sorting;

    private boolean printLogFileName;

    private boolean ignoreFormatting;

    private String FileNameShort;

    private String FileNameLong;

    private String notepadPath;
    private static final String DEFAULT_VIM = "gvim";
    private static final String DEFAULT_NOTEPAD_PATH = "C:\\Program Files\\Notepad++\\notepad++.exe";


    public String getNotepadPath() {
        if (StringUtils.isBlank(notepadPath)) {
            notepadPath = DEFAULT_NOTEPAD_PATH;
        }
        return notepadPath;
    }

    public void setNotepadPath(String notepadPath) {
        this.notepadPath = notepadPath;
    }


    public EditorType getEditorType() {
        if(editorType==EditorType.NONE){
            if (Utils.Util.getOS() == Util.OS.WINDOWS) {
                editorType=EditorType.VIMActiveX;
            } else {
                editorType=EditorType.VIMCommandLine;
            }
        }

        return editorType;
    }

    public void setEditorType(EditorType editorType) {
        this.editorType = editorType;
    }

    private EditorType editorType;

    private HashMap<String, String> printFilters = new HashMap<>(2);

    private boolean messagesLoaded = false;

    public InquirerCfg() {
        this.constants = new GenesysConstants1();
        FileNameShort = ".logbr_short.txt";
        FileNameLong = ".logbr_full.txt";
        saveFileShort = false;
        saveFileLong = false;
        applyFullFilter = true;
        addShortToFull = true;
        maxRecords = 100;
        MaxQueryLines = 3000;
        LimitQueryResults = true;
        accessLogFiles = true;
        sorting = true;
        addOrderBy = true;
        printLogFileName = true;
        ignoreFormatting = false;
        titleRegex = "";
        fullTimeStamp = false;
        refsLoaded = false;
        messagesLoaded = false;
        newTlibSearch = false;
        editorType = EditorType.VIMActiveX;
        linuxEditor=DEFAULT_VIM;
        notepadPath = DEFAULT_NOTEPAD_PATH;
    }

    public static String getFileUnique(String name) {
        Integer currFileIdx = 0;

        String baseName = FilenameUtils.getBaseName(name);
        String extension = FilenameUtils.getExtension(name);
        String ret;
        while (true) {
            ret = inquirer.getEe().getLogBrDir() + File.separator + baseName + "_" + String.format("%1$03d", currFileIdx) + "." + extension;
            File f = new File(ret);
            if (!f.exists()) {
                return ret;
            }
            currFileIdx++;
        }
    }

    public String getLinuxEditor() {
        if (StringUtils.isBlank(linuxEditor)) {
            linuxEditor = DEFAULT_VIM;
        }
        return linuxEditor;
    }

    public void setLinuxEditor(String linuxEditor) {
        this.linuxEditor = linuxEditor;
    }

    private GenesysConstants1 getConsts() {
        if (constants == null) {
            constants = new GenesysConstants1();
        }
        return constants;
    }

    public GenesysConstants getConstants() {
        return getConsts().toConstants();
    }

    public void setConstants(GenesysConstants constants) {
        this.getConsts().fromConstants(constants);
    }

    public Set<ReferenceType> getRefTypes() {
        checkRefsLoaded();
        return refsChecked.keySet();
    }

    public Set<TableType> getLogTypes() {
        checkMessagesLoaded();
        return logMessages.keySet();
    }

    public boolean isAddOrderBy() {
        return addOrderBy;
    }

    public void setAddOrderBy(boolean addOrderBy) {
        this.addOrderBy = addOrderBy;
    }

    public boolean isAccessLogFiles() {
        return accessLogFiles;
    }

    public void setAccessLogFiles(boolean accessLogFiles) {
        this.accessLogFiles = accessLogFiles;
    }

    public boolean isSaveFileShort() {
        return saveFileShort;
    }

    public void setSaveFileShort(boolean saveFileShort) {
        this.saveFileShort = saveFileShort;
    }

    public boolean isSaveFileLong() {
        return saveFileLong;
    }

    public void setSaveFileLong(boolean saveFileLong) {
        this.saveFileLong = saveFileLong;
    }

    public String getFileNameShort() {
        return getFileUnique(FileNameShort);

    }

    public void setFileNameShort(String FileNameShort) {
        this.FileNameShort = FileNameShort;
    }

    public String getFileNameShortAbs() {
        return new File(getFileUnique(FileNameShort)).getAbsoluteFile().getAbsolutePath();

    }

    public String getFileNameLong() {
        return getFileUnique(FileNameLong);
    }

    public void setFileNameLong(String FileNameLong) {
        this.FileNameLong = FileNameLong;
    }

    public boolean isFullTimeStamp() {
        return fullTimeStamp;
    }

    public void setFullTimeStamp(boolean fullTimeStamp) {
        this.fullTimeStamp = fullTimeStamp;
    }

    public String getTitleRegex() {
        return titleRegex;
    }

    public void setTitleRegex(String titleRegex) {
        this.titleRegex = titleRegex;
    }

    public boolean isPrintLogFileName() {
        return printLogFileName;
    }

    public void setPrintLogFileName(boolean printLogFileName) {
        this.printLogFileName = printLogFileName;
    }

    public boolean isIgnoreFormatting() {
        return ignoreFormatting;
    }

    public void setIgnoreFormatting(boolean ignoreFormatting) {
        this.ignoreFormatting = ignoreFormatting;
    }

    public boolean isAddShortToFull() {
        return addShortToFull;
    }

    public void setAddShortToFull(boolean addShortToFull) {
        this.addShortToFull = addShortToFull;
    }

    public boolean isApplyFullFilter() {
        return applyFullFilter;
    }

    public void setApplyFullFilter(boolean applyFullFilter) {
        this.applyFullFilter = applyFullFilter;
    }

    public HashMap<String, Boolean> getRefsHash(ReferenceType refType) {
        return arrToHash(getRefs(refType));

    }

    public ArrayList<OptionNode> getRefs(ReferenceType refType) {
        checkRefsLoaded();
        return refsChecked.get(refType);
    }

    public ArrayList<OptionNode> getLogMessages(TableType refType) {
        checkMessagesLoaded();
        return logMessages.get(refType);
    }

    public void initCfg() {
        loadRef(ReferenceType.TEvent);
        loadRef(ReferenceType.METRIC);
        loadRef(ReferenceType.SIPMETHOD);
        loadRef(ReferenceType.ORSMETHOD);
        loadRef(ReferenceType.ORSMODULE);
        loadRef(ReferenceType.StatType);
        loadRef(ReferenceType.ISCCEvent);
        loadRef(ReferenceType.URSStrategyMsg);
        loadRef(ReferenceType.MSGLEVEL);
        loadRef(ReferenceType.METRICFUNC);
        loadRef(ReferenceType.AgentStatType);
        loadRef(ReferenceType.DBRequest);
        loadRef(ReferenceType.OCSIconEvent);
        loadRef(ReferenceType.OCSIconCause);
        loadRef(ReferenceType.OCSClientRequest);
        loadRef(ReferenceType.GMSEvent);
        loadRef(ReferenceType.ORSREQ);
        loadRef(ReferenceType.VXMLCommand);
        loadRef(ReferenceType.CfgOp);

        loadPrintFilters();
    }

    private void loadRef(ReferenceType refType) {
        try {
            String[] refNames = getRefNames(this, refType);
            ArrayList<OptionNode> refs = getRefsChecked().get(refType);
            if (refs == null) {
                CreateRefs(refType, refNames);
            } else {
                AddRefs(refType, refNames, refs);
            }
        } catch (SQLException ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
    }

    private void loadMessages(TableType tabType) {
        try {
            if (DatabaseConnector.TableExist(tabType.toString())) {
                ArrayList<NameID> refNamesNameID = QueryTools.getRefNamesNameID(ReferenceType.LOGMESSAGE, tabType.toString(), "msgid");
                ArrayList<OptionNode> msgs = getLogMessages().get(tabType);
                if (msgs == null) {
                    CreateLogs(tabType, refNamesNameID);
                } else {
                    AddLogs(tabType, refNamesNameID, msgs);
                }
            }
        } catch (SQLException ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        }
    }

    private void CreateLogs(TableType tab, ArrayList<NameID> nameIDs) {
        ArrayList<OptionNode> refs = new ArrayList<>(nameIDs.size());

        for (NameID ref : nameIDs) {
            refs.add(new OptionNode(true, ref));
        }
        logMessages.put(tab, refs);
    }

    private void AddLogs(TableType tab, ArrayList<NameID> refNamesNameID, ArrayList<OptionNode> refs) {
        for (NameID ref : refNamesNameID) {
            if (!refCheckAndUpdate(refs, ref.getName())) {
                refs.add(new OptionNode(true, ref));
            }
        }
    }

    private void CreateRefs(ReferenceType refType, String[] refNames) {
        refsChecked.put(refType, Stream.of(refNames).map(ref -> new OptionNode(true, ref)).collect(Collectors.toCollection(ArrayList::new)));
    }

    private void AddRefs(ReferenceType refType, String[] refNames, ArrayList<OptionNode> refs) {
        Stream.of(refNames).filter(ref -> !RefExists(refs, ref)).forEach(ref -> refs.add(new OptionNode(true, ref)));
        Collections.sort(refs);

    }

    private boolean refCheckAndUpdate(ArrayList<OptionNode> refs, String str) {
        for (OptionNode ref1 : refs) {
            if (ref1.logCheckAndUpdate(str)) {
                return true;
            }
        }
        return false;
    }

    private boolean RefExists(ArrayList<OptionNode> refs, String ref) {
        for (OptionNode ref1 : refs) {
            if (ref.equals(ref1.getName())) {
                return true;
            }
        }
        return false;
    }

    String getProperty(String tLibNameTail) {
//        if (tLibNameTail.equals("TlibFilter")) {
//            return "RequestAttachUserData,EventCallDataChanged,EventCallPartyAdded,EventCallPartyState,RequestUpdateUserData,EventAttachedDataChanged,RequestDistributeEvent,EventCallPartyDeleted,EventCallCreated,EventCallDeleted,RequestDeletePair,EventCallPartyMoved,ISCCEventAttachedDataChanged,EventACK,EventReserved_2";
//        }
        return null;
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        saveFileLong = false;
        refsLoaded = false;
        messagesLoaded = false;
    }

    int getMaxRecords() {
        return maxRecords;
    }

    void SetMaxRecords(int i) {
        this.maxRecords = i;
    }

    String getFileNameLongBase() {
        return FileNameLong;
    }

    String getFileNameShortBase() {
        return FileNameShort;
    }

    private void loadPrintFilters() {
        try {
            if (DatabaseConnector.TableExist(ReferenceType.AppType.toString())) {
                for (String refName : getRefNames(this, ReferenceType.AppType)) {
                    if (!printFilters.containsKey(refName)) {
                        printFilters.put(refName, "");
                    }

                }
            }
        } catch (SQLException exception) {

        }
    }

    public HashMap<String, String> getPrintFilters() {
        return printFilters;
    }

    public void setPrintFilters(HashMap<String, String> printFilters) {
        this.printFilters = printFilters;
    }

    boolean isLimitQueryResults() {
        return LimitQueryResults;
    }

    void setLimitQueryResults(boolean selected) {
        LimitQueryResults = selected;
    }

    int getMaxQueryLines() {
        return MaxQueryLines;
    }

    void setMaxQueryLines(Integer integer) {
        MaxQueryLines = integer;
    }

    void setTitleRx(String rx) {
        this.titleRegex = rx;
    }

    public String getFileNameExcelBase() {
        return FileNameExcel;
    }

    public String getFileNameExcel() {
        return getFileUnique(FileNameExcel);
    }

    void setFileNameExcel(String text) {
        this.FileNameExcel = text;
    }

    public int getFileSizeWarn() {
        return fileSizeWarn;
    }

    public void setFileSizeWarn(int fileSizeWarn) {
        this.fileSizeWarn = fileSizeWarn;
    }

    String getCaseExpr(String fieldName, String constantName) {
        return getConsts().getCaseExpr(fieldName, constantName);
    }

    public String lookupConst(String constName, int theConstInt) {
        return getConsts().lookupConst(constName, theConstInt);
    }

    public String getFileNameLong(String name, String searchString) {
        StringBuilder ret = new StringBuilder(120);
        if (name != null && !name.isEmpty()) {
            ret.append(cleanNonFileChars(name));
        }
        if (searchString != null && !searchString.isEmpty()) {
            if (ret.length() > 0) {
                ret.append("_");
            }
            ret.append(cleanNonFileChars(searchString));
        }
        if (ret.length() > 0) {
            ret.append(".txt");
            return getFileUnique(ret.toString());
        } else {
            return getFileNameLong();
        }

    }

    public String getFileNameLong(MyJTable tab) {
        return getFileNameLong(tab.getQueryName(), tab.getSearchString());
    }

    private String cleanNonFileChars(String queryName) {
        StringBuilder ret = new StringBuilder(queryName.length());
        boolean anyGoodChar = false;

        for (char c : queryName.toCharArray()) {
            if (filenameProhibited.contains(c)) {
                ret.append('_');
            } else {
                anyGoodChar = true;
                ret.append(c);
            }
        }
        if (anyGoodChar) {
            return ret.toString();
        } else {
            return null;
        }
    }

    public HashMap<TableType, ArrayList<OptionNode>> retrieveLogMessages() {
        checkMessagesLoaded();
        return logMessages;
    }

    HashMap<ReferenceType, ArrayList<OptionNode>> retrieveRefs() {
        return refsChecked;
    }

    private HashMap<TableType, ArrayList<OptionNode>> getLogMessages() {
        if (logMessages == null) {
            logMessages = new HashMap<>();
        }
        return logMessages;
    }

    void setLogMessages(HashMap<TableType, ArrayList<OptionNode>> savedLogMessages) {
        checkMessagesLoaded();
        for (Map.Entry<TableType, ArrayList<OptionNode>> entry : savedLogMessages.entrySet()) {
            ArrayList<OptionNode> currentLogMessages = logMessages.get(entry.getKey());
            if (currentLogMessages.size() != entry.getValue().size()) {
                logMessages.put(entry.getKey(), entry.getValue());
            }

        }
    }

    private HashMap<ReferenceType, ArrayList<OptionNode>> getRefsChecked() {
        if (refsChecked == null) {
            refsChecked = new HashMap<>();
        }
        return refsChecked;
    }

    public HashMap<String, Boolean> arrToHash(ArrayList<OptionNode> refs) {
        if (refs != null) {
            HashMap<String, Boolean> ret = new HashMap<>(refs.size());
            refs.stream().forEach(ref -> ret.put(ref.getName(), ref.isChecked()));
            return ret;
        }
        return null;

    }

    HashMap<String, Boolean> getLogsHash(TableType tableType) {
        return arrToHash(getLogMessages(tableType));
    }

    void setReferences(HashMap<ReferenceType, ArrayList<OptionNode>> savedRefs) {
        for (Map.Entry<ReferenceType, ArrayList<OptionNode>> entry : savedRefs.entrySet()) {
            ArrayList<OptionNode> currRefs = refsChecked.get(entry.getKey());
            if (currRefs.size() != entry.getValue().size()) {
                refsChecked.put(entry.getKey(), entry.getValue());
            }

        }
    }

    private void checkMessagesLoaded() {
        if (!messagesLoaded) {
            loadMessages(TableType.MsgOCServer);
            loadMessages(TableType.MsgIxnServer);
            loadMessages(TableType.MsgMCP);
            loadMessages(TableType.MsgORServer);
            loadMessages(TableType.MsgRM);
            loadMessages(TableType.MsgSCServer);
            loadMessages(TableType.MsgStatServer);
            loadMessages(TableType.MsgTServer);
            loadMessages(TableType.MsgURServer);
            loadMessages(TableType.MsgDBServer);
            loadMessages(TableType.MsgWWE);
            loadMessages(TableType.MsgConfServer);
            messagesLoaded = true;
        }
    }

    private void checkRefsLoaded() {
        if (!refsLoaded) {
            initCfg();
            refsLoaded = true;
        }
    }

    boolean isSorting() {
        return sorting;
    }

    void setSorting(boolean selected) {
        this.sorting = selected;
    }

    boolean isNewTLibSearch() {
        return this.newTlibSearch;
    }

    void setNewTlibSearch(boolean selected) {
        newTlibSearch = selected;
    }

    public void setRegexSearches(MsgType t, Object[] toArray) {
        customFieldsSettings.put(t, Stream.of(toArray).map(obj -> (CustomField) obj).collect(Collectors.toCollection(ArrayList::new)));
    }

    public ArrayList<CustomField> getRegexFieldsSettings(MsgType t) {
        if (customFieldsSettings == null) {
            customFieldsSettings = new HashMap<>();
        }
        return customFieldsSettings.get(t);
    }

    public static class GenesysConstant implements Serializable {

        private static final long serialVersionUID = 1L;
        private final ArrayList<Pair<String, Integer>> values;
        private String name;

        public GenesysConstant() {
            this.values = new ArrayList<>();
        }

        public GenesysConstant(String n) {
            this();
            name = n;

        }

        @Override
        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int size() {
            return values.size();
        }

        public Integer getKey(int idx) {
            if (idx < 0 || idx > values.size()) {
                return null;
            }
            return values.get(idx).getValue();
        }

        public String getConstant(int idx) {
            if (idx < 0 || idx > values.size()) {
                return null;
            }
            return values.get(idx).getKey();
        }

        void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex < 0 || columnIndex > 1) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            Pair<String, Integer> el;
            if (rowIndex > size()) {
                el = new Pair<>();
                values.add(el);

            } else {
                el = values.get(rowIndex);
            }
            if (el != null) {
                if (columnIndex == 1) {
                    el.setValue((Integer) aValue);
                } else {
                    el.setKey((String) aValue);
                }
            }
        }

        void addRow(String string, int i) {
            values.add(new Pair<>(string, i));
        }

        void deleteRow(int selectedRow) {
            if (selectedRow >= 0 && selectedRow < values.size()) {
                values.remove(selectedRow);
            }

        }

        private void setHash(HashMap<Integer, String> theHash) {
            values.clear();
            theHash.entrySet().stream().forEach(entry -> values.add(new Pair<>(entry.getValue(), entry.getKey())));
        }
    }

    public static class GenesysConstants implements Serializable {

        private static final long serialVersionUID = 1L;
        private final ArrayList<GenesysConstant> constants;

        public GenesysConstants() {
            this.constants = new ArrayList<>();
        }

        public ArrayList<Pair<String, Integer>> getValues(String constName) {
            for (GenesysConstant constant : constants) {
                if (constant.getName().equalsIgnoreCase(constName)) {
                    return constant.values;
                }
            }
            return null;
        }

        public ArrayList<GenesysConstant> getConstants() {
            return constants;
        }

        public boolean add(GenesysConstant e) {
            return constants.add(e);
        }
    }

    public static class GenesysConstants1 implements Serializable {

        private static final long serialVersionUID = 1L;
        private HashMap<String, HashMap<Integer, String>> constants;

        public GenesysConstants1() {
            this.constants = new HashMap<>();
        }

        public HashMap<Integer, String> getValues(String constName) {
            return constants.get(constName);
        }

        public Set<String> getAlias() {
            return constants.keySet();
        }

        public HashMap<Integer, String> add(String name) {
            HashMap<Integer, String> hashMap = new HashMap<>();
            constants.put(name, hashMap);
            return hashMap;
        }

        private GenesysConstants toConstants() {
            GenesysConstants ret = new GenesysConstants();
            for (Map.Entry<String, HashMap<Integer, String>> constant : constants.entrySet()) {
                GenesysConstant c = new GenesysConstant(constant.getKey());
                c.setHash(constant.getValue());
                ret.add(c);
            }
            return ret;
        }

        private void fromConstants(GenesysConstants cs) {
            this.constants = new HashMap<>(cs.getConstants().size());
            for (GenesysConstant constant : cs.getConstants()) {
                HashMap<Integer, String> cHash = add(constant.getName());
                for (Pair<String, Integer> value : constant.values) {
                    cHash.put(value.getValue(), value.getKey());
                }
            }
        }

        private String getCaseExpr(String fieldName, String constantName) {
            HashMap<Integer, String> vals = constants.get(constantName);
            if (vals == null) {
                return fieldName;
            } else {
                StringBuilder ret = new StringBuilder(256);

                ret.append("case ").append(fieldName);
                for (Map.Entry<Integer, String> v : vals.entrySet()) {
                    ret.append(" when ").append(v.getKey()).append(" then ").append("\"").append(v.getValue().trim()).append("\"");
                }
                ret.append(" else ").append(fieldName);
                ret.append(" end ");
                return ret.toString();
            }
        }

        private String lookupConst(String constName, int theConstInt) {

            if (!constants.containsKey(constName)) {
                return null;
            }
            HashMap<Integer, String> vals = constants.get(constName);
            if (vals == null) {
                return null;
            }
            if (!vals.containsKey(theConstInt)) {
                return null;
            }
            String ret = vals.get(theConstInt);
            if (ret == null || ret.isEmpty()) {
                return Integer.toString(theConstInt);
            } else {
                return ret;
            }
        }
    }

    public static class GenesysConstant1 implements Serializable {

        private static final long serialVersionUID = 1L;
        private final ArrayList<Pair<String, Integer>> values;
        private String name;

        public GenesysConstant1() {
            this.values = new ArrayList<>();
        }

        public GenesysConstant1(String n) {
            this();
            name = n;

        }

        @Override
        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int size() {
            return values.size();
        }

        public Integer getKey(int idx) {
            if (idx < 0 || idx > values.size()) {
                return null;
            }
            return values.get(idx).getValue();
        }

        public String getConstant(int idx) {
            if (idx < 0 || idx > values.size()) {
                return null;
            }
            return values.get(idx).getKey();
        }

        void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex < 0 || columnIndex > 1) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
            Pair<String, Integer> el;
            if (rowIndex > size()) {
                el = new Pair<String, Integer>();
                values.add(el);

            } else {
                el = values.get(rowIndex);
            }
            if (el != null) {
                if (columnIndex == 1) {
                    el.setValue((Integer) aValue);
                } else {
                    el.setKey((String) aValue);
                }
            }
        }

        void addRow(String string, int i) {
            values.add(new Pair<>(string, i));
        }

        void deleteRow(int selectedRow) {
            if (selectedRow >= 0 && selectedRow < values.size()) {
                values.remove(selectedRow);
            }

        }
    }

}
