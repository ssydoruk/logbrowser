/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ssydoruk
 */
public abstract class Record implements Cloneable {

    static boolean m_handlerInProgress;

    static String m_alias;
    static int fileId = 0;

    static int m_handlerId = 0;
    static int m_sipId = 0;
    static int m_tlibId = 0;
    static int m_jsonId = 0;
    static int m_proxiedId = 0;
    private static int objCreated;

    private static final Pattern regAllQuotes = Pattern.compile("^(['\"]).+\\1$");
    private static final Pattern regNoQuotes = Pattern.compile("^(['\"])(.+)\\1$");

    public static void setFileId(int m_fileId) {
        Record.fileId = m_fileId;
    }

    /*initial value - 1 because when record is inserted into
    file_logbr, initial ID is also 1. Bad design, sure, correct it next time*/
    public static int getFileId() {
        return fileId;
    }

    public static Integer getObjCreated() {
        return objCreated;
    }

    public static void resetCounter() {
        objCreated = 0;
    }

    private static final Pattern regDNWithHost = Pattern.compile("^(\\w+)\\.");

    public static String cleanDN(String key) {
        Matcher m;
        String ret = NoQuotes(key);
        if (StringUtils.isNotBlank(ret)) {
            if (ret.startsWith("+")) {
                ret = ret.substring(1);
            }
            if ((m = regDNWithHost.matcher(ret)).find()) {
                ret = m.group(1);
            }
        }

        return ret;
    }

    public static String NoQuotes(String key) {

        Matcher m;

        if (key != null && (m = regNoQuotes.matcher(key)).find()) {
            return m.group(2);
        }
        return key;
    }

    public static String SingleQuotes(String key) {
        if (key != null && key.length() > 0) {
            return NoQuotes(key);
        }
        return null;
    }

    public static void SetHandlerInProgress(boolean handlerInProgress) {
        Main.logger.trace("SetHandlerInProgress: " + handlerInProgress);
        m_handlerInProgress = handlerInProgress;
    }

    public static boolean isM_handlerInProgress() {
        return m_handlerInProgress;
    }

    public static void SetAlias(String alias) {
        m_alias = alias;
    }

    public static void SetHandlerId(int handlerId) {
        m_handlerId = handlerId;
    }

    public static void SetSipId(int sipId) {
        m_sipId = sipId;
    }

    public static void SetTlibId(int tlibId) {
        m_tlibId = tlibId;
    }

    public static void SetJsonId(int jsonId) {
        m_jsonId = jsonId;
    }

    public static void SetProxiedId(int proxiedId) {
        m_proxiedId = proxiedId;
    }
    long m_fileOffset;
    int m_line;
    private TableType m_type;
//    protected Date m_Timestamp = null;
    private DateParsed m_TimestampDP = null;

    protected long m_FileBytes;
    private int lastID = 0;

    public Record(TableType type) {
        this();
        m_type = type;
        Main.logger.trace("Creating record type " + type);
        objCreated++;
    }

    public Record() {

    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Record ret = (Record) super.clone(); //To change body of generated methods, choose Tools | Templates.
//        ret.m_type=this.m_type;
//        ret.m_FileBytes=m_FileBytes;
        return ret;
    }

    public int getLastID() {
        return lastID;
    }

    @Override
    public String toString() {
        return "Rec{type:" + m_type + ";line:" + m_line + ";off:" + m_fileOffset + ";bytes:" + m_FileBytes + '}';
    }

    public String CheckQuotes(String key) {

        Matcher m;

        if ((m = regAllQuotes.matcher(key)).find()) {
            return key;
        }
        String sQ = key.substring(0, 1);

        if (sQ.equals("'") || sQ.equals("\"")) {
            return key + sQ;
        }

        return "'" + key + "'";// default quote is single
    }

    public void SetOffset(long offset) {
        m_fileOffset = offset;
    }

    public void SetLine(int line) {
        m_line = line;
    }

    protected void AddToDB(HashMap<TableType, DBTable> m_tables) throws Exception {
        DBTable tab = m_tables.get(getM_type());
        if (tab != null) {
            tab.checkInit();
            try {
                Main.logger.debug("Adding to db " + this.toString());
                tab.AddToDB(this);
                tab.recordAdded();
            } catch (Exception exception) {
                Main.logger.error("Exception in AddToDB", exception);
            }
        } else {
            throw new Exception("table not found for " + getM_type().toString());
        }
    }

    /**
     * @return the m_type
     */
    public TableType getM_type() {
        return m_type;
    }

    public DateParsed getM_TimestampDP() {
        return m_TimestampDP;
    }

    public void setM_TimestampDP(DateParsed m_TimestampDP) {
        this.m_TimestampDP = m_TimestampDP;
    }

    public void SetFileBytes(long newValue) {
        setM_FileBytes(newValue);
    }

    /**
     * @param m_Timestamp the m_Timestamp to set
     */
    public void setM_Timestamp(DateParsed m_Timestamp) {
        this.m_TimestampDP = m_Timestamp;
    }

    /**
     * @param m_FileBytes the m_FileBytes to set
     */
    public void setM_FileBytes(long m_FileBytes) {
        this.m_FileBytes = m_FileBytes;
    }

    void PrintMsg() {
        Main.logger.info("line[" + m_line + "]");
    }

    void setLastID(int currentID) {
        this.lastID = currentID;
    }

    private void SetStdFields(Parser parser) {
        if (getM_TimestampDP() == null) {
            setM_TimestampDP(parser.getLastTimeStamp());
        }
        SetOffset(parser.getOffset());
        SetFileBytes(parser.getFileBytes());
        SetLine(parser.getLineStarted());
    }

    protected void SetStdFieldsAndAdd(Parser parser) {
        try {
            SetStdFields(parser);
            AddToDB(parser.m_tables);
        } catch (Exception e) {
            Main.logger.error("line " + parser.getLineStarted() + " Not added \"" + getM_type() + "\" record:" + e.getMessage(), e);
            PrintMsg();

        }
    }

}
