package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;

public abstract class ILogRecord {

    private int m_appid;
    private int appnameid;

    private static long totalBytes = 0;

    static public void resetTotalBytes() {
        totalBytes = 0;
    }
    private int id;

    @Override
    public String toString() {
        return "ILogRecord{" + "m_fields=" + m_fields + ", m_fieldsAll=" + m_fieldsAll + ", m_isMarked=" + m_isMarked + ", m_id=" + getID() + ", m_unixtime=" + m_unixtime + ", m_fileOffset=" + m_fileOffset + ", m_fileBytes=" + m_fileBytes + ", m_fileId=" + GetFileId() + ", m_line=" + GetLine() + ", m_fileName=" + GetFileName() + ", m_time=" + m_time + ", m_appName=" + getM_appName() + ", m_type=" + m_type + ", m_component=" + m_component + '}';
    }

    public Properties m_fields;
    public Properties m_fieldsAll;

    private boolean m_isMarked;

    private long m_unixtime;
    private long m_fileOffset;
    private int m_fileBytes;
    private int m_fileId = -1;
    private int m_line = -1;
    private String m_time;
    private MsgType m_type;
    private int m_component;
    private ArrayList<String> timeStampFields = new ArrayList<>();

    void setTimestampField(String prevTimestamp) {
        timeStampFields.add(prevTimestamp);
    }

    public int getM_component() {
        return m_component;
    }

    /**
     *
     * @param rs
     * @throws Exception
     */
    public void getAllFields(ResultSet rs) throws Exception {

    }

    private static final HashSet<String> stdFields = new HashSet<String>(Arrays.asList(
            new String[]{"id", "time",
                "fileid", "fileOffset", "filebytes", "line",
                "filename", "time", "app", "component"}));

    private boolean IsStdField(String name) {
        return stdFields.contains(name.toLowerCase());
    }
    private ICalculatedFields calcFields;

    public ICalculatedFields getCalcFields() {
        return calcFields;
    }

    public void setCalcFields(ICalculatedFields calcFields) {
        this.calcFields = calcFields;
        m_fieldsAll.putAll(calcFields.calc(m_fieldsAll));
    }

    public ILogRecord() {
        m_fields = new Properties();
        m_fieldsAll = new Properties();
    }

    public void initRecord(ResultSet rs, MsgType type) throws SQLException {
        try {
            m_type = type;

            if (!rs.isClosed()) {
                ResultSetMetaData meta = rs.getMetaData();
                try {
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
//                if (!IsStdField(meta.getColumnName(i))) {
                        String cName = meta.getColumnName(i).toLowerCase();
                        Object obj = rs.getObject(i);
                        if (cName == null) {
                            cName = "";
                        }
                        String cVal;

                        try {
                            cVal = obj.toString();
                        } catch (Exception e) {
                            cVal = "";
                        }
                        m_fieldsAll.put(cName, cVal);
//                }
                    }
                } catch (SQLException sQLException) {
                    inquirer.logger.error("SQL Exception ", sQLException);
                }
                m_isMarked = false;

                m_fileOffset = rs.getLong("fileOffset");
                m_fileBytes = rs.getInt("filebytes");
                totalBytes += m_fileBytes;
                appnameid = rs.getInt("appnameid");
                m_component = rs.getInt("component");

                dateTime = new Date();
                m_unixtime = rs.getLong("time");
                dateTime.setTime(rs.getTimestamp("time").getTime());
                m_time = DatabaseConnector.dateToString(dateTime);

                id = rs.getInt("id");
            }

        } catch (SQLException e) {
            inquirer.logger.error(this.getClass().toString() + " const: " + e.getMessage(), e);
            throw e;
        }
    }

    public long getID() {
        return id;
    }

    public ILogRecord(ResultSet rs, MsgType type) throws SQLException {
        this();
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeInterruptException();
        }
        initRecord(rs, type);
    }

    public int getAppnameid() {
        return appnameid;
    }

    public int getM_appid() {
        return m_appid;
    }

    private Date dateTime = null;

    public Date getDateTime() {
        return dateTime;
    }

    public String getM_appName() {
        return GetField("app", "");
    }

    public boolean isTLibType() {
        MsgType type = GetType();
        return type == MsgType.TLIB || type == MsgType.ORS || type == MsgType.OCS
                || type == MsgType.CONNID;
    }

    public MsgType GetType() {
        return m_type;
    }

    public void SetType(MsgType theType) {
        m_type = theType;
    }

    public long GetUnixTime() {
        return m_unixtime;
    }

    public Integer GetFileId() {
        if (m_fileId <= 0) {
            m_fileId = GetField("fileid", 0);
        }
        return m_fileId;
    }

    public long GetFileOffset() {
        return m_fileOffset;
    }

    public int GetFileBytes() {
        return m_fileBytes;
    }

    public int GetLine() {
        if (m_line <= 0) {
            m_line = GetField("line", 0);
        }
        return m_line;
    }

    private LogFile logFile = null;

    public LogFile GetFileName() {
        if (logFile == null) {
            logFile = inquirer.getInq().getLfm().getLogFile(GetField("filename", ""), GetField("arcname", null));
        }
        return logFile;
    }

    public String GetTime() {
        return m_time;
    }

    public void SetMark(boolean mark) {
        m_isMarked = mark;
    }

    public boolean IsMarked() {
        return m_isMarked;
    }

    public String GetField(String fieldName, String def) {
        try {
            return GetField(fieldName);
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException("cannot get field", ex);
            return def;
        }
    }

    public int GetField(String fieldName, int def) {
        try {
            String value = GetField(fieldName);
            if (value != null && value.length() > 0) {
                return Integer.parseInt(value);
            }
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException("cannot get field", ex);
            return def;
        }
        return def;
    }

    public String GetField(String fieldName) {
        return GetField(fieldName.toLowerCase(), false);
    }

    public String GetField(String fieldName, boolean ignoreException) {
        Object ret = null;
        if (m_fields != null) {
            ret = m_fields.get(fieldName);
        }
        if (ret == null) {
            if (m_fieldsAll != null) {
                ret = m_fieldsAll.get(fieldName);
            }
        }

        if (ret == null) {
            if (!ignoreException) {
                inquirer.logger.error("Error: \"" + fieldName + "\": no such field in " + GetType() + " message");
                PrintFieldNames();
            }
            return "";
//            throw new Exception("Cannot continue");
        }
        return ret.toString();
    }

    public int GetFieldInt(String fieldName) {
        Object ret = null;
        if (m_fields != null) {
            ret = m_fields.get(fieldName);
        }
        if (ret == null) {
            if (m_fieldsAll != null) {
                ret = m_fieldsAll.get(fieldName);
            }
        }

        if (ret == null) {
            inquirer.logger.error("Error: \"" + fieldName + "\": no such field in " + GetType() + " message");
            PrintFieldNames();
            return 0;
//            throw new Exception("Cannot continue");
        }
        return Integer.parseInt(ret.toString());
    }

    public int GetAnchorId() {
        return 0;
    }

    private static String[] sInboundFieldNames = {"inbound"
    };

    private boolean getBool(Object obj) {
        try {
            return Integer.parseInt((String) obj) == 1;
        } catch (NumberFormatException e) {
        }
        return false;
    }

    public boolean IsInbound() {
        for (String fld : sInboundFieldNames) {
            if (m_fieldsAll.containsKey(fld)) {
                return getBool(m_fieldsAll.get(fld));
            }
            if (m_fields.containsKey(fld)) {
                return getBool(m_fields.get(fld));
            }
        }
        return false;
    }

    private void PrintFieldNames() {
        inquirer.logger.info("Fields available:");
        StringBuilder s = new StringBuilder(255);
        for (Object key : m_fields.keySet()) {
            s.append(key).append(", ");
        }
        s.append("|");
        for (Object key : m_fieldsAll.keySet()) {
            s.append(key).append(", ");
        }
        inquirer.logger.info(s);
    }

    boolean hasField(String fldName) {
        if (m_fields.containsKey(fldName)) {
            return true;
        }
        if (m_fieldsAll.containsKey(fldName)) {
            return true;
        }
        return false;
    }

    void debug() {
        if (inquirer.logger.isTraceEnabled()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeInterruptException();
            } else {
                String s = this.getClass() + "| ";
                for (Object key : m_fields.keySet()) {
                    s += key + "[" + m_fields.getProperty((String) key) + "] ";
                }
                for (Object key : m_fieldsAll.keySet()) {
                    s += key + "[" + m_fieldsAll.getProperty((String) key) + "] ";
                }
                inquirer.logger.trace(s);
            }
        }
    }

    protected String notNull(String val, String def) {
        if (val == null) {
            return def;
        }
        return val;
    }

    protected String getString(ResultSet rs, String fld) throws SQLException {
        String ret = rs.getString(fld);
        return (ret == null) ? "" : ret;

    }

    protected Integer getInt(ResultSet rs, String fld) throws SQLException {
        int aInt = rs.getInt(fld);
        return aInt;

    }

}
