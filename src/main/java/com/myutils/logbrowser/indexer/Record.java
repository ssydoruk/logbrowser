/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ssydoruk
 */
public abstract class Record implements Cloneable {

    private static final Pattern regAllQuotes = Pattern.compile("^(['\"]).+\\1$");
    private static final Pattern regNoQuotes = Pattern.compile("^(['\"])(.+)\\1$");
    private static final Pattern regDNWithHost = Pattern.compile("^(\\w+)\\.");
    private static final ConcurrentHashMap<TableType, AtomicInteger> idStorage = new ConcurrentHashMap<>();
    static String m_alias;
    private static int objCreated;
    private final int fileID;
    protected long m_FileBytes;
    private long m_fileOffset;
    private int m_line;
    private TableType m_type;
    //    protected Date m_Timestamp = null;
    private DateParsed m_TimestampDP = null;
    private int recordID;
    public Record(TableType type, int fileID) {
        this.fileID = fileID;
        m_type = type;
        recordID = getAtomicInteger(type).incrementAndGet();
        Main.logger.trace("Creating record type " + type);
        objCreated++;
    }

    public static void setID(TableType file, int id) {
//        getAtomicInteger(file).set(id);
        getAtomicInteger(file).updateAndGet(new IntUnaryOperator() {
            @Override
            public int applyAsInt(int operand) {
                return (id > operand) ? id : operand;
            }
        });
    }

    public static int getID(TableType file) {
        return getAtomicInteger(file).get();
    }

    private static AtomicInteger getAtomicInteger(TableType type) {
        AtomicInteger atomicInteger;
        synchronized (type) {
            atomicInteger = idStorage.get(type);
            if (atomicInteger == null) {
                atomicInteger = new AtomicInteger(0);
                idStorage.put(type, atomicInteger);
            }
        }
        return atomicInteger;
    }

    public static Integer getObjCreated() {
        return objCreated;
    }

    public static void resetCounter() {
        objCreated = 0;
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

    public static void SetAlias(String alias) {
        m_alias = alias;
    }

    public int getFileID() {
        return fileID;
    }

    public int getRecordID() {
        return recordID;
    }

    public String cleanDN(String key) {
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

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Record ret = (Record) super.clone(); //To change body of generated methods, choose Tools | Templates.
//        ret.m_type=this.m_type;
//        ret.m_FileBytes=m_FileBytes;
        return ret;
    }


    @Override
    public String toString() {
        return "Rec{id:" + recordID
                + ";t:" + m_type + ";l:" + m_line + ";off:" + m_fileOffset + ";b:" + m_FileBytes + '}';
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

    public long getM_fileOffset() {
        return m_fileOffset;
    }

    public int getM_line() {
        return m_line;
    }

    public long getM_FileBytes() {
        return m_FileBytes;
    }

    /**
     * @param m_FileBytes the m_FileBytes to set
     */
    public void setM_FileBytes(long m_FileBytes) {
        this.m_FileBytes = m_FileBytes;
    }

    public void SetLine(int line) {
        m_line = line;
    }

    protected void AddToDB(HashMap<TableType, DBTable> m_tables) throws Exception {
        DBTable tab = m_tables.get(getM_type());
        if (tab != null) {
            tab.checkInit();
            try {
                Main.logger.trace("Adding to db " + this);
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

    void PrintMsg() {
        Main.logger.info("line[" + m_line + "]");
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
