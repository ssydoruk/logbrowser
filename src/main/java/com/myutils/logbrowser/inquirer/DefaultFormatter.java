package com.myutils.logbrowser.inquirer;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

@SuppressWarnings({"unchecked", "rawtypes", "serial", "deprecation", "this-escape"})
public class DefaultFormatter implements ILogRecordFormatter {

    static protected HashMap<String, Integer> m_callIdHash = new HashMap<>();
    static protected int m_callIdCount = 0;
    protected int m_maxFileNameLength;
    protected boolean m_isLongFileNameEnabled;
    protected HashSet<String> m_components;
    MsgType lastType = MsgType.UNKNOWN;
    private boolean headerOnTop = true;
    private boolean allFields = false;
    private boolean shouldAccessFiles = true;
    private boolean shouldPrintRecordType = true;
    private boolean isExcel;

    private boolean printFileLine = false;
    private Character delimiter;

    public DefaultFormatter(boolean isLongFileNameEnabled, HashSet<String> components) {
        m_isLongFileNameEnabled = isLongFileNameEnabled;
        m_maxFileNameLength = 0;
        m_components = components;
    }

    public static String GetRelativePath(String canonicalPath) {

        return PathManager.GetRelativePath(canonicalPath);
    }

    public boolean isShouldPrintRecordType() {
        return shouldPrintRecordType;
    }

    public boolean isPrintFileLine() {
        return printFileLine;
    }

    void setPrintFileLine(boolean printFileLine) {
        this.printFileLine = printFileLine;
    }

    public boolean isShouldAccessFiles() {
        return shouldAccessFiles;
    }

    void setShouldAccessFiles(boolean shouldAccessFiles) {
        this.shouldAccessFiles = shouldAccessFiles;
    }

    @Override
    public boolean IsLayoutRequired() {
        return true;
    }

    @Override
    public void Layout(ILogRecord loggedEvent) throws SQLException {
        if (loggedEvent.GetType() == MsgType.SIP) {
            String callId = loggedEvent.getFieldValue("callid");
            if (callId != null && !callId.isEmpty()
                    && !callId.equalsIgnoreCase("null")) {
                if (!m_callIdHash.containsKey(callId)) {
                    m_callIdHash.put(callId, m_callIdCount++);
                }
            }
        }

//        String fileName = loggedEvent.GetFileName();
////            if (!m_isLongFileNameEnabled) {
////                fileName = GetRelativePath(fileName);
////            }
//        int fileNameLength = fileName.length();
//        if (fileNameLength > m_maxFileNameLength) {
//            m_maxFileNameLength = fileNameLength;
//        }
    }

    @Override
    public void ProcessLayout() {
    }

    /**
     * @param loggedEvent
     * @param ps
     * @throws Exception
     */
    @Override
    public void Print(ILogRecord loggedEvent, PrintStreams ps, IQueryResults qr) {
        StringBuilder outS = new StringBuilder(512);
        if (headerOnTop) {
            if (this.lastType != loggedEvent.GetType()) {
                if (isShouldPrintRecordType()) {
                    outS.append(excelQuote()).append("rec_type").append(excelQuote());
                }

                if (isPrintFileLine()) {
                    addDelimiter(outS);
                    outS.append(excelQuote()).append("file_line").append(excelQuote());
                }

                addDelimiter(outS);
                outS.append(excelQuote()).append("timestamp").append(excelQuote());

                for (Object object : loggedEvent.m_fieldsAll.keySet()) {
                    addDelimiter(outS);
                    outS.append(excelQuote()).append(object.toString()).append(excelQuote());
                }
                ps.println(outS);
                lastType = loggedEvent.GetType();
            }
        }
        outS.setLength(0);

        if (isShouldPrintRecordType()) {
            outS.append(excelQuote()).append(loggedEvent.GetType().toString()).append(excelQuote());
        }

        if (isPrintFileLine()) {
            addDelimiter(outS);
            outS.append(excelQuote()).append(loggedEvent.GetFileName()).append("(").append(loggedEvent.GetLine()).append("):").append(excelQuote());
        }

        addDelimiter(outS);
        outS.append(excelQuote()).append(loggedEvent.GetTime()).append(excelQuote());

        Enumeration e = loggedEvent.m_fieldsAll.propertyNames();

        while (e.hasMoreElements()) {
            addDelimiter(outS);
            String key = (String) e.nextElement();
            outS.append(excelQuote());
            if (!headerOnTop) {
                outS.append(key).append(":");
            }
            outS.append(loggedEvent.m_fieldsAll.getProperty(key)).append(excelQuote());
        }
        ps.println(outS);

    }

    @Override
    public void Close() {
    }

    @Override
    public void reset() {
        m_callIdHash.clear();
        m_callIdCount = 0;
    }

    @Override
    public void refreshFormatter() throws Exception {
    }

    void setDelimiter(Character delimiter) {
        this.delimiter = delimiter;
    }

    void setAllFields(boolean allFields) {
        this.allFields = allFields;
    }

    void setHeaderOnTop(boolean headerOnTop) {
        this.headerOnTop = headerOnTop;
    }

    protected void addDelimiter(StringBuilder outString) {
        if (outString.length() > 0) {
            outString.append(delimiter);
        }
    }

    void setPrintRecordType(boolean shouldPrintRecordType) {
        this.shouldPrintRecordType = shouldPrintRecordType;
    }

    void setExcel(boolean excel) {
        this.isExcel = excel;
    }

    protected String excelQuote() {
        if (isExcel) {
            return "\"";
        }
        return "";
    }

}
