package com.myutils.logbrowser.inquirer;

import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.HostAccess;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

public abstract class ILogRecord {

    private static final String[] sInboundFieldNames = {"inbound"
    };
    private static final String FILEOFFSET = "fileoffset";
    private static final String FILEBYTES = "filebytes";
    private static final String APPNAMEID = "appnameid";
    private static final String COMPONENT = "component";
    private static final String ID = "id";
    public Properties m_fieldsAll;
    protected StdFields stdFields = new StdFields();
    private int m_appid;
    private int appnameid;
    private int id;
    private boolean m_isMarked;
    private long m_unixtime;
    private long m_fileOffset;
    private int m_fileBytes;
    private int m_fileId = -1;
    private int m_line = -1;
    private String m_time;
    private MsgType m_type;
    private int m_component;
    private ICalculatedFields calcFields;
    private Date dateTime = null;
    private LogFile logFile = null;

    public ILogRecord() {
        m_fieldsAll = new Properties();
    }

    public ILogRecord(ResultSet rs, MsgType type) throws SQLException {
        this();
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeInterruptException();
        }
        initRecord(rs, type);
    }

    @Override
    public String toString() {
        return "ILogRecord{" + ", m_fieldsAll=" + m_fieldsAll + ", m_isMarked=" + m_isMarked + ", m_id=" + getID() + ", m_unixtime=" + m_unixtime + ", m_fileOffset=" + m_fileOffset + ", m_fileBytes=" + m_fileBytes + ", m_fileId=" + GetFileId() + ", m_line=" + GetLine() + ", m_fileName=" + GetFileName() + ", m_time=" + m_time + ", m_appName=" + getM_appName() + ", m_type=" + m_type + ", m_component=" + m_component + '}';
    }

    public int getM_component() {
        return m_component;
    }

    /**
     * @param rs
     * @throws Exception
     */
    public void getAllFields(ResultSet rs) throws Exception {

    }

    public ICalculatedFields getCalcFields() {
        return calcFields;
    }

    final public void setCalcFields(ICalculatedFields calcFields) {
        this.calcFields = calcFields;
        m_fieldsAll.putAll(calcFields.calc(m_fieldsAll));
    }

    abstract void initCustomFields();

    abstract HashMap<String, Object> initCalculatedFields(ResultSet rs) throws SQLException;

    final public void initRecord(ResultSet rs, MsgType type) throws SQLException {
        try {
            m_type = type;

            if (!rs.isClosed()) {
                initCustomFields();

                // Init standard fields -->
                stdFields.fieldInit(FILEOFFSET, new IValueAssessor() {
                    @Override
                    public Object getValue() {
                        return m_fileOffset;
                    }

                    @Override
                    public void setValue(Object val) {
                        if (val instanceof Integer) {
                            m_fileOffset = new Long((Integer) val);
                        } else {
                            m_fileOffset = (long) val;
                        }
                    }

                });
                stdFields.fieldInit(FILEBYTES, new IValueAssessor() {
                    @Override
                    public Object getValue() {
                        return m_fileBytes;
                    }

                    @Override
                    public void setValue(Object val) {
                        m_fileBytes = (int) val;
                    }
                });
                stdFields.fieldInit(APPNAMEID, new IValueAssessor() {
                    @Override
                    public Object getValue() {
                        return appnameid;
                    }

                    @Override
                    public void setValue(Object val) {
                        appnameid = (int) val;
                    }
                });
                stdFields.fieldInit(COMPONENT, new IValueAssessor() {
                    @Override
                    public Object getValue() {
                        return m_component;
                    }

                    @Override
                    public void setValue(Object val) {
                        m_component = (int) val;
                    }
                });
                stdFields.fieldInit(ID, new IValueAssessor() {
                    @Override
                    public Object getValue() {
                        return id;
                    }

                    @Override
                    public void setValue(Object val) {
                        id = (int) val;
                    }
                });

                stdFields.fieldInit("time", new IValueAssessor() {
                    @Override
                    public Object getValue() {
                        return m_unixtime;
                    }

                    @Override
                    public void setValue(Object val) {
                        m_unixtime = (long) val;
                    }
                });
                // Init standard fields <--

                ResultSetMetaData meta = rs.getMetaData();
                try {

                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        String cName = meta.getColumnName(i).toLowerCase();
                        Object obj = rs.getObject(i);
                        if (cName == null) {
                            cName = "";
                        }
                        String cVal;

                        if (!stdFields.setValue(cName, obj)) {
                            try {
                                cVal = obj.toString();
                            } catch (Exception e) {
                                cVal = "";
                            }
                            m_fieldsAll.put(cName, cVal);
                        }
                    }
                } catch (SQLException sQLException) {
                    inquirer.logger.error("SQL Exception ", sQLException);
                }
                m_isMarked = false;

                dateTime = new Date();
                dateTime.setTime(rs.getTimestamp("time").getTime());
                m_time = DatabaseConnector.dateToString(dateTime);
                HashMap<String, Object> initCalculatedFields = initCalculatedFields(rs);
                if (initCalculatedFields != null) {
                    m_fieldsAll.putAll(initCalculatedFields);
                }
            }

        } catch (SQLException e) {
            inquirer.logger.error(this.getClass().toString() + " const: " + e.getMessage(), e);
            throw e;
        }
    }

    public long getID() {
        return id;
    }

    public int getAppnameid() {
        return appnameid;
    }

    public int getM_appid() {
        return m_appid;
    }

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
        } catch (NumberFormatException ex) {
            inquirer.ExceptionHandler.handleException("cannot get field", ex);
            return def;
        }
        return def;
    }

    public String GetField(String fieldName) {
        return GetField(fieldName.toLowerCase(), false);
    }

    @HostAccess.Export
    public String getField(String fieldName) {
        return GetField(fieldName.toLowerCase(), true);
    }

    @HostAccess.Export
    public String getBytes() {
        return InquirerFileIo.GetFileBytes(GetFileName(), GetFileOffset(), GetFileBytes());
    }

    @HostAccess.Export
    public String constToStr(String s, int val) {
        if (StringUtils.isNotBlank(s)) {
            String ret = inquirer.getCr().lookupConst(s, val);
            if (StringUtils.isNotBlank(ret)) {
                return ret + " (" + val + ")";
            }
        }
        return Integer.toString(val);
    }

    @HostAccess.Export
    public String constToStr(String s, String _val) {
        int val = Integer.parseInt(StringUtils.defaultIfBlank(_val, "0"));
        if (StringUtils.isNotBlank(s)) {
            String ret = inquirer.getCr().lookupConst(s, val);
            if (StringUtils.isNotBlank(ret)) {
                return ret + " (" + val + ")";
            }
        }
        return Integer.toString(val);
    }

    public String GetField(String fieldName, boolean ignoreException) {
        Object ret;
        if (stdFields.containsKey(fieldName)) {
            ret = stdFields.get(fieldName).getValue();
        } else if (m_fieldsAll.containsKey(fieldName)) {
            ret = m_fieldsAll.get(fieldName);
        } else {
            if (!ignoreException) {
                String s = "Error: \"" + fieldName + "\": no such field in " + GetType() + " message";
                inquirer.logger.error(s);
//                inquirer.showError(null, s, "Field not found");
//                PrintFieldNames();
            }
            return "";
//            throw new Exception("Cannot continue");
        }
        return ret == null ? "" : ret.toString();
    }

    public int GetFieldInt(String fieldName) {
        Object ret = null;
        if (m_fieldsAll != null) {
            ret = m_fieldsAll.get(fieldName);
        }

        if (ret == null) {
            String s = "Error: \"" + fieldName + "\": no such field in " + GetType() + " message";
            inquirer.logger.error(s);
//            inquirer.showError(null, s, "Field not found");
//            PrintFieldNames();
            return 0;
//            throw new Exception("Cannot continue");
        }
        return Integer.parseInt(ret.toString());
    }

    public int GetAnchorId() {
        return 0;
    }

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
        }
        return false;
    }

    private void PrintFieldNames() {
        inquirer.logger.info("Fields available:");
        StringBuilder s = new StringBuilder(255);
        for (Object key : m_fieldsAll.keySet()) {
            s.append(key).append(", ");
        }
        inquirer.logger.info(s);
    }

    boolean hasField(String fldName) {
        if (stdFields.containsKey(fldName)) {
            return true;
        } else return m_fieldsAll.containsKey(fldName);
    }

    void debug() {
        if (inquirer.logger.isTraceEnabled()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeInterruptException();
            } else {
                String s = this.getClass() + "| ";
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

    protected long setLong(Object val) {
        if (val == null) {
            return 0;
        } else if (val instanceof Integer) {
            return new Long((Integer) val);
        } else {
            return (long) val;
        }
    }

    protected int setInt(Object val) {
        if (val == null) {
            return 0;
        } else if (val instanceof Long) {
            return (int) val;
        } else {
            return (int) val;
        }
    }

    protected boolean setBoolean(Object val) {
        if (val == null) {
            return false;
        } else if (val instanceof Integer) {
            return (Integer) val != 0;
        } else {
            return (boolean) val;
        }
    }

    protected interface IValueAssessor {

        Object getValue();

        void setValue(Object val);
    }

    public static class StdFields extends HashMap<String, IValueAssessor> {

        /**
         * @param fieldName
         * @param valueAccessor - if null, this means field from record set is
         *                      ignored
         */
        public void fieldInit(String fieldName, IValueAssessor valueAccessor) {
            put(fieldName, valueAccessor);
        }

        public boolean setValue(String fieldName, Object val) {
            if (containsKey(fieldName)) {
                IValueAssessor get = get(fieldName);

                //null value means ignore field
                if (get != null) {
                    get.setValue(val);
                    return true;
                }
            }
            return false;
        }
    }

}
