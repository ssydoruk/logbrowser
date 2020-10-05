/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import static com.myutils.logbrowser.inquirer.QueryTools.getRefNames;
import com.myutils.logbrowser.inquirer.gui.MyJTable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author ssydoruk
 */
public class InquirerCfg implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final HashSet<Character> filenameProhibited = new HashSet<Character>(
            Arrays.asList(new Character[]{
        ':',
        '\"',
        '/',
        '\\',
        '|',
        '?',
        '*',
        '<',
        '>'}));

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

    private HashMap<ReferenceType, ArrayList<OptionNode>> refsChecked = new HashMap<>(20);
    private HashMap<TableType, ArrayList<OptionNode>> logMessages = new HashMap<>();
    private int maxRecords;

    private boolean LimitQueryResults;
    private int MaxQueryLines;
    private String titleRegex;
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
    private boolean ignorePatternForDBFields;
    private String FileNameShort;
    private String FileNameLong;
    private HashMap<String, String> printFilters = new HashMap<>(2);
    private boolean messagesLoaded = false;
    HashSet<RegexFieldSettings> regexFieldsSettings = null;
    HashMap<MsgType, ArrayList<RegexFieldSettings>> rxFieldsSettings = null;

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

    public String getFileNameShortAbs() {
        return new File(getFileUnique(FileNameShort)).getAbsoluteFile().getAbsolutePath();

    }

    public void setFileNameShort(String FileNameShort) {
        this.FileNameShort = FileNameShort;
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

    public void LoadRefs() {
        LoadRef(ReferenceType.TEvent);
        LoadRef(ReferenceType.METRIC);
        LoadRef(ReferenceType.SIPMETHOD);
        LoadRef(ReferenceType.ORSMETHOD);
        LoadRef(ReferenceType.ORSMODULE);
        LoadRef(ReferenceType.StatType);
        LoadRef(ReferenceType.ISCCEvent);
        LoadRef(ReferenceType.URSStrategyMsg);
        LoadRef(ReferenceType.MSGLEVEL);
        LoadRef(ReferenceType.METRICFUNC);
        LoadRef(ReferenceType.AgentStatType);
        LoadRef(ReferenceType.DBRequest);
        LoadRef(ReferenceType.OCSIconEvent);
        LoadRef(ReferenceType.OCSIconCause);
        LoadRef(ReferenceType.OCSClientRequest);
        LoadRef(ReferenceType.GMSEvent);
        LoadRef(ReferenceType.ORSREQ);
        LoadRef(ReferenceType.VXMLCommand);

        LoadPrintFilters();
    }

    private void LoadRef(ReferenceType refType) {
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
        ArrayList<OptionNode> refs = new ArrayList<>(refNames.length);

        for (String ref : refNames) {
            refs.add(new OptionNode(true, ref));
        }
        refsChecked.put(refType, refs);
    }

    private void AddRefs(ReferenceType refType, String[] refNames, ArrayList<OptionNode> refs) {
        for (String ref : refNames) {
            if (!RefExists(refs, ref)) {
                refs.add(new OptionNode(true, ref));
            }
        }
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

    private void LoadPrintFilters() {
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

    int getMaxQueryLines() {
        return MaxQueryLines;
    }

    void setLimitQueryResults(boolean selected) {
        LimitQueryResults = selected;
    }

    void setMaxQueryLines(Integer integer) {
        MaxQueryLines = integer;
    }

    void setTitleRx(String rx) {
        this.titleRegex = rx;
    }

    void setFileNameExcel(String text) {
        this.FileNameExcel = text;
    }

    public String getFileNameExcelBase() {
        return FileNameExcel;
    }

    public String getFileNameExcel() {
        return getFileUnique(FileNameExcel);
    }

    public void setFileSizeWarn(int fileSizeWarn) {
        this.fileSizeWarn = fileSizeWarn;
    }

    public int getFileSizeWarn() {
        return fileSizeWarn;
    }

    String getCaseExpr(String fieldName, String constantName) {
        return getConsts().getCaseExpr(fieldName, constantName);
    }

    String lookupConst(String constName, int theConstInt) {
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

    private HashMap<ReferenceType, ArrayList<OptionNode>> getRefsChecked() {
        if (refsChecked == null) {
            refsChecked = new HashMap<>();
        }
        return refsChecked;
    }

    public HashMap<String, Boolean> arrToHash(ArrayList<OptionNode> refs) {

        if (refs != null) {
            HashMap<String, Boolean> ret = new HashMap<>(refs.size());
            for (OptionNode ref : refs) {
                ret.put(ref.getName(), ref.isChecked());
            }
            return ret;
        }
        return null;
    }

    HashMap<String, Boolean> getLogsHash(TableType tableType) {
        return arrToHash(getLogMessages(tableType));
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
            LoadRefs();
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

//    RegexFieldSettings saveRegexField(String name, RegexFieldSettings currentRegexSetting) {
//        HashSet<RegexFieldSettings> ret = getRegexFieldsSettings();
//        RegexFieldSettings regexFieldSettings = new RegexFieldSettings(name, currentRegexSetting);
//        ret.add(regexFieldSettings);
//        return regexFieldSettings;
//    }
    public void setRegexSearches(Object[] toArray) {
        regexFieldsSettings = new HashSet<>(toArray.length);
        for (Object object : toArray) {
            regexFieldsSettings.add((RegexFieldSettings) object);
        }
    }

    public void setRegexSearches(MsgType t, Object[] toArray) {
        ArrayList<RegexFieldSettings> l = new ArrayList<>(toArray.length);
        for (Object object : toArray) {
            l.add((RegexFieldSettings) object);
        }
        rxFieldsSettings.put(t, l);
    }

    public ArrayList<RegexFieldSettings> getRegexFieldsSettings(MsgType t) {
        if (rxFieldsSettings == null) {
            rxFieldsSettings = new HashMap<>();
        }
        return rxFieldsSettings.get(t);
    }

    public HashSet<RegexFieldSettings> getRegexFieldsSettings() {
        if (regexFieldsSettings == null) {
            regexFieldsSettings = new HashSet<>();
        }
        return regexFieldsSettings;
    }

//    RegexFieldSettings saveRegexField(String name, String searchString, String returnValue, boolean isCaseSensitive, boolean isWholeWorld) {
//        HashSet<RegexFieldSettings> ret = getRegexFieldsSettings();
//        RegexFieldSettings regexFieldSettings = new RegexFieldSettings(name, searchString, returnValue, isWholeWorld, isCaseSensitive);
//        ret.add(regexFieldSettings);
//        return regexFieldSettings;
//    }
    public static class GenesysConstant implements Serializable {

        private static final long serialVersionUID = 1L;
        private String name;
        private ArrayList<Pair<String, Integer>> values;

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
            for (Map.Entry<Integer, String> entry : theHash.entrySet()) {
                values.add(new Pair<>(entry.getValue(), entry.getKey()));
            }
        }
    }

    public static class GenesysConstants implements Serializable {

        private static final long serialVersionUID = 1L;
        private ArrayList<GenesysConstant> constants;

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
        private String name;
        private ArrayList<Pair<String, Integer>> values;

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
    }

    public static class RegexFieldSettings implements Serializable {

        private static final long serialVersionUID = 1L;
        private String name;
        private String searchString;
        private String retValue;
        private boolean makeWholeWorld;
        private boolean caseSensitive;

        public RegexFieldSettings(String name, String searchString, String retValue, boolean makeWholeWorld, boolean caseSensitive) {
            this.name = name;
            this.searchString = searchString;
            this.retValue = retValue;
            this.makeWholeWorld = makeWholeWorld;
            this.caseSensitive = caseSensitive;
        }

        public RegexFieldSettings(String name, RegexFieldSettings currentRegexSetting) {
            this.name = name;
            this.searchString = currentRegexSetting.getSearchString();
            this.retValue = currentRegexSetting.getRetValue();
            this.makeWholeWorld = currentRegexSetting.isMakeWholeWorld();
            this.caseSensitive = currentRegexSetting.isCaseSensitive();
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

        public String getSearchString() {
            return searchString;
        }

        public void setSearchString(String searchString) {
            this.searchString = searchString;
        }

        public String getRetValue() {
            return retValue;
        }

        public void setRetValue(String retValue) {
            this.retValue = retValue;
        }

        public boolean isMakeWholeWorld() {
            return makeWholeWorld;
        }

        public void setMakeWholeWorld(boolean makeWholeWorld) {
            this.makeWholeWorld = makeWholeWorld;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        void updateParams(String rx, String retVal, boolean isCase, boolean isWord) {
            searchString = rx;
            retValue = retVal;
            caseSensitive = isCase;
            makeWholeWorld = isWord;
        }
    }
}
