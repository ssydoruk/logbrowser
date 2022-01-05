package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ssydoruk
 */
public abstract class Parser {

    public static final int MAX_CUSTOM_FIELDS = 3;
    private static final int BASE_CUSTOM_FIELDS = 8;
    private static final Pattern regORSSessionID = Pattern.compile("^[~\\w]{32}$");
    private final static HashMap<String, ArrayList<DateFmt>> appPreferedFormats = new HashMap<>();
    private final String m_StringType;
    private final FileInfoType m_type;
    private final DateParsers dateParsers = new DateParsers();
    private final FilesParseSettings.FileParseSettings fileParseSettings;
    private final ArrayList<CustomRegexLine> printedCustomLines = new ArrayList<>();
    //</editor-fold>
    protected HashMap<TableType, DBTable> m_tables;
    protected ArrayList<String> m_MessageContents;
    protected DateParsed m_LastTimeStamp;
    protected int m_lineStarted; // line where expression (TMessage) started
    protected DateParsed dp;
    protected FileInfo fileInfo;
    //    protected static static final Pattern regGenesysMessage = Pattern.compile(" (None|Debug|Trace|Interaction|Standard|Alarm|Unknown|Non|Dbg|Trc|Int|Std|Alr|Unk) (\\d{5}) ");
    int m_CurrentLine;
    SqliteAccessor m_accessor;
    private boolean collectingDates;
    private boolean foundBodyDates;
    private long bytesConsumed;
    private long filePos;//begining of current line
    private long savedFilePos; // to save the begining of block
    private long endFilePos; //end of block
    private long posDiff = 0;
    private LocalDateTime lastKnownDate = null;
    private int lastChangeValue = 0;
    private String lastMatch;
    private FilesParseSettings.FileParseCustomSearch lastCustomSearch;
    private GenesysMsg lastLogMsg;
    private ArrayList<DateFmt> currentAppDates; //unlikely to have more than 3 date formats in single app
    private DateDiff dateDiff = null;
    private DBTablesFiller dbTablesFiller;

    public Parser(FileInfoType type, HashMap<String, DBTable> tables) {
        super();
        dbTablesFiller = new DBTablesFiller(tables, Main.getMain().getM_accessor());

        String customTab = getCustomTab(type);
        tables.put(customTab, new CustomLineTable(type));



        m_MessageContents = new ArrayList<>();
        m_type = type;
        m_StringType = FileInfoType.getFileType(type);

        /*
        adding default formats for date
         */
        fileParseSettings = Main.getMain().xmlCfg.getFileParseSettings(type);
        if (fileParseSettings != null) {
            ArrayList<Parser.DateFmt> formats = fileParseSettings.getAllFormats();
            if (formats != null) {
                for (Parser.DateFmt f : formats) {
                    if (f != null) {
                        dateParsers.AddFormat(f);
                    }
                }
            }
        } else {
            Main.logger.debug("Parser constructor not found file settings for : " + type);
        }
        if (isParseTimeDiff()) {
            dateDiff = new DateDiff(type);
        }
    }

    public static String getQueryKey(HashMap<String, List<String>> splitQuery, String[] keys) {
        for (String key : keys) {
            String ret = getQueryKey(splitQuery, key);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static String getQueryKey(HashMap<String, List<String>> splitQuery, String key) {
        if (splitQuery != null && key != null && !key.isEmpty()) {
            List<String> sidL = splitQuery.get(key);
            if (sidL != null && !sidL.isEmpty()) {
                return sidL.get(0);
            }
        }
        return null;
    }

    public static List<String> genList(String entry) {
        List<String> ret = new ArrayList<>();
        ret.add(entry);
        return ret;

    }

    public static HashMap<String, List<String>> splitQuery(String url) {
        if (url != null && !url.isEmpty()) {
            try {
                final HashMap<String, List<String>> query_pairs = new LinkedHashMap<>();
                final String[] pairs = url.split("&");
                for (String pair : pairs) {
                    final int idx = pair.indexOf('=');
                    final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                    if (!query_pairs.containsKey(key)) {
                        query_pairs.put(key, new LinkedList<>());
                    }
                    final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                    query_pairs.get(key).add(value);
                }
                if (query_pairs.isEmpty()) {
                    return null;
                } else {
                    return query_pairs;
                }
            } catch (UnsupportedEncodingException | IllegalArgumentException ex) {
                Main.logger.error("Error spliting [" + url + "]", ex);
            }
        }
        return null;
    }

    public static String fldName(int i) {
        return fldNameFull(i) + "id";
    }

    public static String valName(int i) {
        return valNameFull(i) + "id";

    }

    public static String getCustomTab(FileInfoType fileType) {
        return FileInfoType.getFileType(fileType) + "_custom";
    }

    public static String fldNameFull(int i) {
        return "field" + i;
    }

    public static String valNameFull(int i) {
        return "value" + i;
    }

    public static boolean isParseTimeDiff() {
        return Main.getInstance().getEe().isParseTDiff();
    }

    public static boolean isSessionID(String resp) {
        return resp != null && !resp.isEmpty() && regORSSessionID.matcher(resp).find();
    }

    public int getFileID() {
        return fileInfo.getRecordID();
    }

    protected void logError(String s) {
        Main.logger.error("l" + m_CurrentLine + ": " + s);
    }

    public int getM_CurrentLine() {
        return m_CurrentLine;
    }

    public DateParsed getCurrentTimestamp() {
        if (dp == null) {
            return m_LastTimeStamp;
        }
        return dp;
    }

    public FilesParseSettings.FileParseCustomSearch getLastCustomSearch() {
        return lastCustomSearch;
    }

    /**
     * Parse custom against those configured in backend.xml
     *
     * @param str
     * @param changeValue used for SIP Server parser. If greater than zero, than
     *                    return if found str not changed and this number not changed
     * @return true if match is found and flag parserRest is false on the
     * CustomSearch in backend.xml
     * @throws Exception
     */
    protected boolean ParseCustom(String str, int changeValue) throws Exception {
        if (fileParseSettings != null && str != null && !str.isEmpty()) {
            for (FilesParseSettings.FileParseCustomSearch fileParseCustomSearch : fileParseSettings.getCustomSearch()) {
                Matcher m = fileParseCustomSearch.parseCustom(str, changeValue);
                if (m != null) {
                    lastCustomSearch = fileParseCustomSearch;
                    if (changeValue > 0) {
                        if (lastChangeValue > 0 && changeValue == lastChangeValue
                                && lastMatch != null && lastMatch.equals(m.group(0))) {
                            return fileParseCustomSearch.isParseRest();
                        }
                    }
                    lastChangeValue = changeValue;
                    if (changeValue > 0) {
                        lastMatch = m.group(0);
                    } else {
                        lastMatch = null;
                    }
                    Main.logger.trace("Matched custom [" + str + "]");
                    for (Map.Entry<String, ArrayList<FilesParseSettings.FileParseCustomSearch.SearchComponent>> entry : fileParseCustomSearch.getComponents().entrySet()) {
                        String key = entry.getKey();
                        ArrayList<FilesParseSettings.FileParseCustomSearch.SearchComponent> value = entry.getValue();
                        insertIntoCustom(m, key, value, changeValue, fileParseCustomSearch);
                    }
                    return fileParseCustomSearch.isParseRest();
                }
            }
        }
        return true;
    }

    protected DateParsed getLastTimeStamp() {
        return m_LastTimeStamp;

    }

    protected DateParsed getDP() {
        return dp;
    }

    private String getTServerStart(String ret) {

        if (ret != null && !ret.isEmpty()) {
            if (ret.startsWith("gsip:CL2") && ret.indexOf(']') + 2 < ret.length()) {
                return ret.substring(ret.indexOf(']') + 2);
            } else if (ret.charAt(0) == '@') {
                return ret.substring(1);
            }
        }
        return ret;
    }

    protected String ParseGenesysTServer(String str, TableType tabType, Pattern ptIgnore, Pattern ptSkip) throws Exception {
        return ParseGenesys(getTServerStart(str), tabType, ptIgnore, ptSkip);
    }

    private String parserRet(String ret, Pattern ptSkip) {
        if (dp != null && ptSkip != null && ret != null) {
            Matcher m;
            if ((m = ptSkip.matcher(ret)).find()) {
                dp.rest = ret.substring(m.end());
                Main.logger.trace("Skipped per regex [" + ptSkip + "]; new rest [" + dp.rest + "]");
            }
            return dp.rest;
        }
        return ret;
    }

    protected String ParseGenesysSCS(String str, TableType tabType, Pattern ptIgnore, Pattern ptSkip) throws Exception {
        return parserRet(ParseGenesysSCS(str, tabType, ptIgnore), ptSkip);
    }

    protected String ParseGenesys(String str, TableType tabType, Pattern ptIgnore, Pattern ptSkip) throws Exception {
        return parserRet(ParseGenesys(str, tabType, ptIgnore), ptSkip);

    }

    protected DateParsed ParseTimestamp(String str) throws Exception {
        dp = ParseFormatDate(str);
        if (dp != null) {
            Matcher m;
            if (dp.fmtDate != null) {
                m_LastTimeStamp = dp;
                foundBodyDates = true;
//                Main.logger.info("l:"+m_CurrentLine+"Parsed date: "+dp.toString());
            }
        }
        return dp;
    }

    protected String ParseTimestampStr(String str) throws Exception {
        ParseTimestamp(str);
        if (dp != null) {
            return dp.rest;
        } else {
            return str;
        }
    }

    protected String ParseTimestampStr1(String str) throws Exception {
        ParseTimestamp(str);
        if (dp != null) {
            return dp.rest;
        } else {
            return str;
        }
    }

    protected String ParseGenesysSCS(String str, TableType tabType, Pattern ptMSGsIgnore) throws Exception {
        dp = ParseTimestamp(str);
        if (dp != null) {

            GenesysMsg.CheckGenesysMsgSCS(dp, this, tabType, ptMSGsIgnore); // not using return value. Check through the rest
            return dp.rest;
        } else {
            return str;
        }
    }

    public GenesysMsg getLastLogMsg() {
        return lastLogMsg;
    }

    String getLastGenesysMsgID() {
        return (lastLogMsg != null) ? StringUtils.isNotBlank(lastLogMsg.getLastGenesysMsgID()) ? lastLogMsg.getLastGenesysMsgID() : null : null;
    }

    String getLastGenesysMsgLevel() {
        return (lastLogMsg != null) ? StringUtils.isNotBlank(lastLogMsg.getLastGenesysMsgLevel()) ? lastLogMsg.getLastGenesysMsgLevel() : null : null;
    }

    protected String ParseGenesysGMS(String str, TableType tabType, Pattern ptMSGsIgnore) throws Exception {
        dp = ParseTimestamp(str);
        if (dp != null) {
            lastLogMsg = null;

            lastLogMsg = GenesysMsg.CheckGenesysMsgGMS(dp, this, tabType, ptMSGsIgnore); // not using return value. Check through the rest
            return dp.rest;
        } else {
            return str;
        }
    }

    protected String ParseGenesys(String str, TableType tabType, Pattern ptMSGsIgnore) throws Exception {
        dp = ParseTimestamp(str);
        if (dp != null) {
            lastLogMsg = null;
            lastLogMsg = GenesysMsg.CheckGenesysMsg(dp, this, tabType, ptMSGsIgnore); // not using return value. Check through the rest
            if (dp == null || dp.rest == null) {
                Main.logger.error("!!!DP=null");
                return "";
            } else
                return dp.rest;
        } else {
            return str;
        }
    }

    protected String ParseGenesys(String str, TableType tabType) throws Exception {
        return ParseGenesys(str, tabType, null);
    }

    /**
     * Only parses date. Standard Genesys log message can be parsed later if
     * needed
     *
     * @param str
     * @return
     * @throws Exception
     */
    protected String ParseGenesys(String str) throws Exception {
        dp = ParseTimestamp(str);
        if (dp != null) {
            return dp.rest;
        } else {
            return str;
        }
    }

    public void PrintMsg(ArrayList contents) {
        if (contents == null || contents.isEmpty()) {
            Main.logger.error("Empty contents");
        } else {
            for (Object content : contents) {
                Main.logger.error("[" + content + "]");
            }
        }
    }

    public int getLineStarted() {
        return m_lineStarted;
    }

    public FileInfoType getM_type() {
        return m_type;
    }

    /*Main method implementing parser*/
    public abstract int ParseFrom(BufferedReaderCrLf reader, long offset, int line, FileInfo fi);

    /**
     * @return the filePos
     */
    public long getFilePos() {
        return filePos;
    }

    /**
     * @param filePos the filePos to set
     */
    public void setFilePos(long filePos) {
//        Main.logger.trace("l:" + m_CurrentLine + " setFilePos before:" + this.filePos + " after: " + filePos);
        this.filePos = filePos;
    }

    public void addFilePos(long posDiff) {
        Main.logger.trace("l:" + m_CurrentLine + " addFilePos before:" + this.filePos + ",posDiff:" + this.posDiff + "  after: " + filePos);

        if (this.posDiff > 0) {
            this.filePos += this.posDiff;
        }

        this.posDiff = posDiff;
    }

    /**
     * @return the savedFilePos
     */
    public long getSavedFilePos() {
        return savedFilePos;
    }

    /**
     * @param savedFilePos the savedFilePos to set
     */
    public void setSavedFilePos(long savedFilePos) {
        this.savedFilePos = savedFilePos;
    }

    /**
     * @return the endFilePos
     */
    public long getEndFilePos() {
        return endFilePos;
    }

    /**
     * @param endFilePos the endFilePos to set
     */
    public void setEndFilePos(long endFilePos) {
//        Main.logger.trace("l:" + m_CurrentLine + " setEndFilePos before:" + this.endFilePos + " after: " + endFilePos);

        this.endFilePos = endFilePos;
    }

    public void SetBytesConsumed(long n) {
        bytesConsumed = n;
    }

    public long getBytesConsumed() {
        return bytesConsumed;
    }

    protected void SavePos(Record msg) {
        msg.SetOffset(getSavedFilePos());
        msg.SetFileBytes(getEndFilePos() - getSavedFilePos());
        msg.SetLine(getLineStarted());

    }

    protected void SaveTime(Record msg) {
        if (msg.getM_TimestampDP() == null) {
            msg.setM_TimestampDP(getLastTimeStamp());
        }
    }

    protected void SetStdFields(Record msg) {
        SaveTime(msg);
        if (m_MessageContents.isEmpty()) { // line item
            msg.SetOffset(getFilePos());
            msg.SetFileBytes(getEndFilePos() - getFilePos());
            msg.SetLine(m_lineStarted);
        } else {
            SavePos(msg);
        }
    }

    public long getLine() {
        if (m_MessageContents.isEmpty()) { // line item
            return m_lineStarted;
        } else {
            return getLineStarted();
        }
    }

    public long getFileBytes() {
        if (m_MessageContents.isEmpty()) { // line item
            return getEndFilePos() - getFilePos();
        } else {
            return getEndFilePos() - getSavedFilePos();
        }
    }

    public long getOffset() {
        if (m_MessageContents.isEmpty()) { // line item
            return getFilePos();
        } else {
            return getSavedFilePos();
        }
    }

    public void addToDB(Record msg) {
        try {
            Main.logger.traceExit(msg);
            dbTablesFiller.addToDB(msg);
        } catch (Exception e) {
            Main.logger.error("line " + msg.getM_line() + " Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);
            msg.PrintMsg();

        }

    }

    public void SetStdFieldsAndAdd(Record msg) {
        SetStdFields(msg);
        addToDB(msg);
    }

    //    public DateParsed ParseDate(String s) throws Exception {
//        DateParsed ret = adjustDay(dateParsers.parseDate(s), null);
//        commitDateParsers();
//        return ret;
//    }
    public DateParsed ParseFormatDate(String s) throws Exception {
        DateParsed ret = dateParsers.parseFormatDate(s, lastKnownDate);

        if (ret != null) {
            lastKnownDate = ret.fmtDate;
            if (isParseTimeDiff()) {
                dateDiff.newDate(ret, fileInfo.getRecordID(), fileInfo.getAppNameID(), getFilePos(), getEndFilePos() - getFilePos(), m_lineStarted);
            }

            commitDateParsers();
        }
        return ret;
    }

    synchronized private void commitDateParsers() {
        if (collectingDates) {
            DateFmt lastFmtMatched = dateParsers.getLastFmtMatched();
            if (lastFmtMatched != null) {
                if (!currentAppDates.contains(lastFmtMatched)) {
                    currentAppDates.add(lastFmtMatched);
                }
            }
        }
    }

    public void doneParsing(FileInfo fileInfo) {
        String appID = fileInfo.getAppID();
        synchronized (appPreferedFormats) {
            if (!appPreferedFormats.containsKey(appID)) {
                if (currentAppDates == null || currentAppDates.isEmpty()) {
                    Main.logger.error("No dates found in previous file, too bad");
                } else {
                    appPreferedFormats.put(appID, currentAppDates);
                }
            }
        }
        dbTablesFiller.DoneInserts();

    }

    void setFileInfo(FileInfo _fileInfo) {
        this.fileInfo = _fileInfo;
        setM_LastTimeStamp(fileInfo.getFileLocalTime());

        String appID = fileInfo.getAppID();
        Main.logger.trace("appID = " + appID);
        synchronized (appPreferedFormats) {
            if (appPreferedFormats.containsKey(appID)) {
                dateParsers.setPrefferedFormats(appPreferedFormats.get(appID));
                dateParsers.setCheckRegex(false);
                collectingDates = false;
            } else {
                if (ignoreFileDates(fileInfo)) //file size is less the a 1 MB
                {
                    if (currentAppDates == null) {
                        currentAppDates = new ArrayList<>(3);
                    }
                } else {
                    currentAppDates = new ArrayList<>(3);
                }
                collectingDates = true;
                dateParsers.setPrefferedFormats(null);//
            }
        }
        Main.logger.trace("collectingDates = " + collectingDates);
        this.foundBodyDates = false;
    }

    public void setM_LastTimeStamp(DateParsed m_LastTimeStamp) {
        if (m_LastTimeStamp != null) {
            lastKnownDate = m_LastTimeStamp.fmtDate;
            this.m_LastTimeStamp = m_LastTimeStamp;
        }
    }

    private DateParsed adjustDay(DateParsed parseDate, DateParsed fileLocalDate) throws SQLException {
        if (parseDate != null) {
//            cal.setTime(parseDate.fmtDate);
//            if (cal.get(Calendar.YEAR) == 1970) {
//                if (lastKnownDate == null) {
//                    lastKnownDate = fileLocalDate.fmtDate;
//                }
//                if (lastKnownDate == null) {
//                    Main.logger.error("No last known date");
//                } else {
//                    cal.setTime(lastKnownDate);
//                    int year = cal.get(Calendar.YEAR);
//                    int month = cal.get(Calendar.MONTH);
//                    int day = cal.get(Calendar.DAY_OF_MONTH);
//                    cal.setTime(parseDate.fmtDate);
//                    cal.set(year, month, day);
//                    parseDate.fmtDate = cal.getTime();
//                    long timeLastKnown = lastKnownDate.getTime();
//                    long timeNew = parseDate.fmtDate.getTime();
//                    if (timeNew - timeLastKnown < -36000) {// 10 hours; means we crossed the day while in file
//                        parseDate.addDay();
//                    }
//                    lastKnownDate = parseDate.fmtDate;
//                }
        } else {
            lastKnownDate = parseDate.fmtDate;
        }

        if (isParseTimeDiff()) {
            dateDiff.newDate(parseDate, fileInfo.getRecordID(), fileInfo.getAppNameID(), getFilePos(), getEndFilePos() - getFilePos(), m_lineStarted);
        }
        return parseDate;
    }

    private boolean ignoreFileDates(FileInfo fileInfo) {
        return fileInfo.getSize() < 1048576
                || (fileInfo.getAppType().equals("TServer") && fileInfo.getLogFileName().contains("-1536."));
    }

    void doFinalize() throws Exception {
        if (isParseTimeDiff()) {
            dateDiff.doFinalize();
        }
    }

    abstract void init(HashMap<String, DBTable> m_tables);

    void doneParsingFile(FileInfo fi) {
        if (!foundBodyDates) {
            Main.logger.error("Not parsed single date; check your parser");
        }
        fi.setFileEndTime(getLastTimeStamp());
        addToDB(fi);

    }

    protected void pError(org.apache.logging.log4j.Logger logger, String string) {
        logger.error("l:" + m_CurrentLine + " " + string);
    }

    private void insertIntoCustom(Matcher m, String key, ArrayList<FilesParseSettings.FileParseCustomSearch.SearchComponent> value, int changeValue,
                                  FilesParseSettings.FileParseCustomSearch fileParseCustomSearch) throws Exception {
        CustomRegexLine cl = new CustomRegexLine( getCustomTab(m_type) , fileInfo.getRecordID());
        cl.setParams(m, key, value);
//        int lastPrintedIdx = cl.lastPrintedIdx(fileParseCustomSearch);
//        Main.logger.info("insertIntoCustom custom [" + lastPrintedIdx + "]");
//        if (lastPrintedIdx < 0) {
        SaveTime(cl);
        cl.SetOffset(getFilePos());
        cl.SetFileBytes(getEndFilePos() - getFilePos());
        cl.SetLine(m_lineStarted);
        cl.handlerID(changeValue);
        dbTablesFiller.addToDB(cl);
//        }
//        cl.setPrinted(lastPrintedIdx);
//        for (Pair<String, Integer> pair : value) {
//            CustomAttribute ca = new CustomAttribute(key, pair.getComp(), pair.getValue());
//            ca.setMatcher(m);
//            custAttrTab.AddToDB(ca);
//
//        }

    }

    public enum DateIncluded {
        DATE_INCUDED,
        DATE_NOT_INCLUDED,
        DATE_UNKNOWN
    }

    public static class DateFmt {

        Pattern pattern;

        String fmt;
        ArrayList<Pair<String, String>> repl = null;
        DateIncluded isDateIncluded;
        private DateTimeFormatter formatter = null;

        public DateFmt(String p, String fmt, DateIncluded dateIncluded) {
            this.pattern = Pattern.compile(p);
            this.fmt = fmt;
            isDateIncluded = dateIncluded;
            if (fmt != null) {
                formatter = DateTimeFormatter.ofPattern(fmt);
            }
        }

        DateFmt(String p, String fmt, Element el) {
            this(p, fmt, DateIncluded.DATE_UNKNOWN);

            NodeList nl = el.getChildNodes();
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element spec = (Element) nl.item(i);
//                        Main.logger.info("Type: " + type + " attr[" + spec.getNodeName() + "]");
                        if (spec.getNodeName().equalsIgnoreCase("replace")) {
                            String from = spec.getAttribute("from");
                            String to = spec.getAttribute("to");
                            addReplace(from, to);
                        }
                    }
                }
            }
        }

        public int getFmtLength() {
            return fmt.length();
        }

        @Override
        public String toString() {
            return "pattern: [" + pattern.pattern() + "] fmt: [" + fmt + "] fmt: ["
                    + ((formatter == null) ? "(null)" : formatter
                    + "] ");
        }

        private void addReplace(String from, String to) {
            if (repl == null) {
                repl = new ArrayList<>();
            }
            repl.add(new Pair(from, to));
        }

        public boolean isReplNull() {
            return repl == null;
        }

        public LocalDateTime parseDate(String d, LocalDateTime lastKnownDate) throws ParseException {
//            LocalDateTime parse = formatter.parse(fmt, query);
            switch (isDateIncluded) {
                case DATE_INCUDED:
                    return LocalDateTime.parse(d, formatter);

                default:
                    TemporalAccessor parse = formatter.parse(d);
                    if (parse.isSupported(java.time.temporal.ChronoField.YEAR)) {
                        return LocalDateTime.from(parse);
                    } else {
                        LocalTime t = LocalTime.from(parse);
                        if (lastKnownDate == null) {
                            lastKnownDate = LocalDateTime.now();
                        }
                        LocalDateTime ret = LocalDateTime.of(lastKnownDate.toLocalDate(), t);
                        return (Duration.between(lastKnownDate, ret).toHours() <= -10) ? ret.plusDays(1) : ret;

                    }

            }
        }

        LocalDateTime parseDate(String d, Matcher m, LocalDateTime lastKnownDate) throws ParseException {
            if (repl == null) {
                return parseDate(d, lastKnownDate);
            } else {
                StringBuilder s = new StringBuilder(d);
                for (Pair<String, String> pair : repl) {
                    if (pair.getKey().equals(m.group(1))) {
                        s.replace(m.start(1), m.end(1), pair.getValue());
                        break;
                    }
                }
                Main.logger.trace("ParseDate replaced src[" + d + "] res[" + s + "]");
                return parseDate(s.toString(), lastKnownDate);
            }
        }
    }

    class CustomRegexLine extends Message {

        private boolean mustChange;
        private String key;
        private Matcher m;
        private Integer handlerID = null;
        private ArrayList<FilesParseSettings.FileParseCustomSearch.SearchComponent> value;

        public CustomRegexLine(String type, int fileID) {
            super(type, fileID);
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < value.size(); i++) {
                s.append(getValue(i)).append(" ");
            }
            return "Custom{" + "keys=" + s + '}';
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.CUSTCOMP, getComp()));
            setFieldInt(stmt, 7, getHandlerID());

            for (int i = 0; i < MAX_CUSTOM_FIELDS; i++) {
                setFieldInt(stmt, BASE_CUSTOM_FIELDS + (i * 2), Main.getRef(ReferenceType.CUSTKKEY, getKey(i)));
                setFieldInt(stmt, BASE_CUSTOM_FIELDS + (i * 2) + 1, Main.getRef(ReferenceType.CUSTVALUE, getValue(i)));
            }
            return true;
        }

        public int lastPrintedIdx(FilesParseSettings.FileParseCustomSearch fileParseCustomSearch) {
            for (int j = 0; j < printedCustomLines.size(); j++) {
                CustomRegexLine crl = printedCustomLines.get(j);
                boolean valueEqual = true;
                boolean otherEqual = true;
                Main.logger.info("compare " + crl.toString() + " | " + this);
                for (int i = 0; i < value.size(); i++) {
                    FilesParseSettings.FileParseCustomSearch.SearchComponent savedSearch = crl.getValue().get(i);
                    FilesParseSettings.FileParseCustomSearch.SearchComponent curSearch = value.get(i);
//                    Main.logger.info("compare "+savedSearch.toString()+" | "+curSearch.toString());
                    String value1 = crl.getValue(i);
                    String value2 = getValue(i);
                    if (savedSearch.isMustChange() && curSearch.isMustChange()) {
                        if (value1 != null && value2 != null && !value1.equalsIgnoreCase(value2)) {
                            valueEqual = false;
                        }

                    } else {
                        if (value1 != null && value2 != null && !value1.equalsIgnoreCase(value2)) {
                            // unrelated keys
                            otherEqual = false;
                        }
                    }
                }
                if (j > 0 && otherEqual) {
                    return (valueEqual) ? j : -1;
                }
            }
            return -1;
        }

        public Matcher getM() {
            return m;
        }

        public ArrayList<FilesParseSettings.FileParseCustomSearch.SearchComponent> getValue() {
            return value;
        }

        public Integer getHandlerID() {
            return handlerID;
        }

        public String getComp() {
            return key;
        }

        private String getKey(int i) {
            if (i < value.size()) {
                return value.get(i).getAttrName();
            } else {
                return "";
            }
        }

        private HashMap<Pattern, String> getModifications(int i) {
            if (i < value.size()) {
                return value.get(i).getModifications();
            } else {
                return null;
            }
        }

        @Override
        public int hashCode() {
            return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
        }

        private String getValue(int i) {
            if (i < value.size()) {
                return modifyValue(m.group(value.get(i).getVal()), i);
            } else {
                return null;
            }
        }

        private void setParams(Matcher m, String key, ArrayList<FilesParseSettings.FileParseCustomSearch.SearchComponent> value) {
            this.m = m;
            this.key = key;
            this.value = value;
            for (FilesParseSettings.FileParseCustomSearch.SearchComponent searchComponent : value) {
                if (searchComponent.isMustChange()) {
                    this.mustChange = true;
                    break;
                }
            }
        }

        private void handlerID(int changeValue) {
            if (changeValue > 0) {
                this.handlerID = changeValue;
            }
        }

        private void setPrinted(int lastPrintedIdx) {
            Main.logger.info("setPrinted: lastPrintedIdx:" + lastPrintedIdx + " mustChange:" + mustChange);
            if (mustChange) {
                if (lastPrintedIdx < 0) {
                    Main.logger.info("setPrinted: lastPrintedIdx:" + lastPrintedIdx + " mustChange:" + mustChange + "adding: " + this);
                    printedCustomLines.add(this);
                } else {
                    printedCustomLines.set(lastPrintedIdx, this);
                }
            }
        }

        private String modifyValue(String val, int i) {
            HashMap<Pattern, String> modifications = getModifications(i);
            if (modifications != null) {
                String workString = val;
                StringBuilder buf;
                for (Map.Entry<Pattern, String> entry : modifications.entrySet()) {
                    Pattern key1 = entry.getKey();
                    if ((m = key1.matcher(workString)).find()) {
                        buf = new StringBuilder();
                        buf.append(workString, 0, m.start(0)).append(entry.getValue());
                        if (m.end(0) < workString.length()) {
                            buf.append(workString.substring(m.end(0)));
                        }
                        workString = buf.toString();
                    }
                }
                return workString;
            } else {
                return val;
            }
        }
    }

    private class CustomLineTable extends DBTable {

        private final FileInfoType type;

        public CustomLineTable(FileInfoType t) {
            super(Main.getInstance().getM_accessor(), getCustomTab(t));
            this.type = t;
        }

        private String valName(int i) {
            return "value" + i + "id";
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");
            addIndex("keyid");
            addIndex("handler");

            for (int i = 1; i <= MAX_CUSTOM_FIELDS; i++) {
                addIndex(fldName(i));
                addIndex(valName(i));
            }
            dropIndexes();

            StringBuilder query = new StringBuilder("create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",keyid int"
                    + ",handler int"
            );

            for (int i = 1; i <= MAX_CUSTOM_FIELDS; i++) {
                query.append(",").append(Parser.fldName(i)).append(" int");
                query.append(",").append(Parser.valName(i)).append(" int");
            }

            query.append(");");
            getM_dbAccessor().runQuery(query.toString());
            query.setLength(0);


        }

        @Override
        public String getInsert() {
            StringBuilder query = new StringBuilder();
            query.append("INSERT INTO ").append(getTabName()).append(" VALUES(NULL,?,?,?,?,?"
                    + ",?"
                    + ",?"
            );
            for (int i = 1; i <= MAX_CUSTOM_FIELDS * 2; i++) {
                query.append(",").append("?");
            }
            query.append(");");
            return query.toString();
        }

        @Override
        public void FinalizeDB() throws Exception {
            createIndexes();
        }


    }

    //<editor-fold defaultstate="collapsed" desc="comment">
    class CustomAttribute extends Record {


        public CustomAttribute(TableType type, int fileID) {
            super(type, fileID);
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            //                stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
//                stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());
            return true;

        }

        private void setMatcher(Matcher m) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    private class CustomAttributeTable extends DBTable {

        private FileInfoType type;

        public CustomAttributeTable(SqliteAccessor dbaccessor, String tabName) {
            super(dbaccessor, tabName);
        }

        public CustomAttributeTable(FileInfoType t) {
            this(Main.getInstance().getM_accessor(), FileInfoType.getFileType(t) + "_attr");
            this.type = t;
        }

        @Override
        public void InitDB() {
            addIndex("custom_id");
            addIndex("FileId");
            addIndex("newModeID");
            addIndex("theAppNameID");
            addIndex("oldModeID");
            addIndex("statusID");
            addIndex("hostID");
            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (custom_id INTEGER"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",newModeID int"
                    + ",appDBID int"
                    + ",theAppNameID int"
                    + ",PID int"
                    + ",oldModeID int"
                    + ",statusID bit"
                    + ",hostID int"
                    + ");";
            getM_dbAccessor().runQuery(query);


        }

        @Override
        public String getInsert() {
            return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ");";
        }

        @Override
        public void FinalizeDB() throws Exception {
            createIndexes();
        }

    }

}
