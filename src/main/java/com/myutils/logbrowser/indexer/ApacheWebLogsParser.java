/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApacheWebLogsParser extends WebParser {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;
    private static final Pattern ptJSessionID = Pattern.compile("JSESSIONID=([^;\\s\"\\.]+)");
    private static final Pattern ptBrowserClientID = Pattern.compile("BAYEUX_BROWSER=([^;\\s\"\\.]+)");
    private static final Pattern ENTRY_BEGIN_PATTERN = Pattern.compile(
            "^(.*?) - (.*?) \\[(.*?)\\] \"((\\S+)(?:\\s+)(.+)?(?:\\s+)(HTTP/\\S+)?)?\" (\\d+) (.*)\\s+(\\d+)$");
    long m_CurrentFilePos;
    long m_HeaderOffset;
    String m_Header;
    int m_dbRecords = 0;
    private ParserState m_ParserState;

    public ApacheWebLogsParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_WWECloud, m_tables);
    }

    public static Pattern getENTRY_BEGIN_PATTERN() {
        return ENTRY_BEGIN_PATTERN;
    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentFilePos = offset;
        m_CurrentLine = line;


        m_dbRecords = 0;

        try {
            input.skip(offset);
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            m_ParserState = ParserState.STATE_COMMENT;

            String str;
            while ((str = input.readLine()) != null) {
                m_CurrentLine++;
                Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");

                try {
                    int l = input.getLastBytesConsumed();
                    // SetBytesConsumed(input.getLastBytesConsumed());
                    setEndFilePos(getFilePos() + l); // to calculate file bytes

                    while (str != null) {
                        str = ParseLine(str);
                    }
                    setFilePos(getFilePos() + l);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":" + str + "\n" + e, e);
                }

            }
        } catch (IOException e) {
            Main.logger.error(e);
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

    String ParseLine(String str) throws Exception {
        Matcher m;

        Main.logger.trace("ParseLine " + m_CurrentLine + ": state -" + m_ParserState);

        switch (m_ParserState) {
            case STATE_COMMENT:

                m_lineStarted = m_CurrentLine;

                if ((m = ENTRY_BEGIN_PATTERN.matcher(str)).find()) {
                    ApacheWebMsg entry = new ApacheWebMsg();

                    entry.setIP(m.group(1)); // save IP
                    entry.setUserKey(m.group(2)); // save IP
                    ParseTimestamp(m.group(3));
                    entry.setDate(m.group(3));
                    entry.setMethod(m.group(5)); // HTTP Command like GET, POST, HEAD, PUT, DELETE, ...
                    String u = m.group(6);
                    // URL url = new URL(u);
                    // String query = url.getQuery();

                    entry.setURL(urlProcessor.processURL(u));

                    entry.setUserName(urlProcessor.getqueryValues(QUERY_userName));

                    entry.setUUID(urlProcessor.getpathValues(PATH_UUID));
                    entry.setDeviceID(urlProcessor.getpathValues(PATH_DeviceID));
                    entry.setIxnID(urlProcessor.getpathValues(PATH_IxnID));
                    entry.setUserID(urlProcessor.getpathValues(PATH_USERID));

                    entry.setHttpCode(m.group(8));

                    Matcher m1;
                    if ((m1 = ptJSessionID.matcher(m.group(9))).find()) {
                        entry.setJSessionID(m1.group(1));
                    }
                    if ((m1 = ptBrowserClientID.matcher(m.group(9))).find()) {
                        entry.setBrowserClientID(m1.group(1));
                    }
                    entry.setExecutionTime(m.group(10));
                    SetStdFieldsAndAdd(entry);

                } else {
                    logger.error("l: " + m_CurrentLine + " - not recognized [" + str + "]");
                }
                break;

        }
        return null;
    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
        m_tables.put(TableType.ApacheWeb, new ApacheWebTab(Main.getInstance().getM_accessor(), TableType.ApacheWeb));

    }

    enum ParserState {
        STATE_COMMENT,
    }

    private class ApacheWebMsg extends Message {

        private String ip;
        private String jSessionID;
        private String httpCode;
        private String date;
        private String method;
        private String key;
        private String url;

        private String deviceID;
        private String userName;
        private String browserClientID;
        private Integer executionTime = null;
        private String UUID;
        private String ixnID;

        private ApacheWebMsg() {
            super(TableType.ApacheWeb,  fileInfo.getRecordID());
        }

        /**
         * Get the value of deviceID
         *
         * @return the value of deviceID
         */
        public String getDeviceID() {
            return deviceID;
        }

        /**
         * Set the value of deviceID
         *
         * @param deviceID new value of deviceID
         */
        public void setDeviceID(String deviceID) {
            this.deviceID = deviceID;
        }

        /**
         * Get the value of UUID
         *
         * @return the value of UUID
         */
        public String getUUID() {
            return UUID;
        }

        /**
         * Set the value of UUID
         *
         * @param UUID new value of UUID
         */
        public void setUUID(String UUID) {
            if (UUID != null) {
                this.UUID = UUID;
            }
        }

        /**
         * Get the value of ixnID
         *
         * @return the value of ixnID
         */
        public String getIxnID() {
            return ixnID;
        }

        /**
         * Set the value of ixnID
         *
         * @param ixnID new value of ixnID
         */
        public void setIxnID(String ixnID) {
            this.ixnID = ixnID;
        }

        public String getHttpCode() {

            return httpCode;
        }

        private void setHttpCode(String s) {
            httpCode = s;
        }

        public String getMethod() {
            return method;
        }

        private void setMethod(String s) {
            method = s;
        }

        private void setDate(String s) {
            date = s;
        }

        private void setIP(String s) {
            ip = s;
        }

        public String getIp() {
            return ip;
        }

        public String getjSessionID() {
            return jSessionID;
        }

        public String getKey() {
            return key;
        }

        public String getUrl() {
            return url;
        }

        public String getUserName() {
            return userName;
        }

        private void setUserName(String get) {
            if (get != null) {
                this.userName = get;
            }
        }

        private void setUserKey(String s) {
            key = s;
        }

        private void setJSessionID(String s) {
            jSessionID = s;
        }

        private void setURL(String group) {
            try {
                url = URLDecoder.decode(group, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                logger.fatal("", ex);
                url = group;
            }
        }

        public String getBrowserClientID() {
            return browserClientID;
        }

        private void setBrowserClientID(String group) {
            this.browserClientID = group;
        }

        public Integer getExecutionTime() {
            return executionTime;
        }

        private void setExecutionTime(String group) {
            if (group != null) {
                try {
                    this.executionTime = new Integer(group);
                } catch (NumberFormatException numberFormatException) {
                    Main.logger.error("Error parsing int [" + group + "]");
                }
            }
        }

        private void setUserID(String u) {
            // !!not currently put into the database
        }
    }

    private class ApacheWebTab extends DBTable {

        public ApacheWebTab(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("IPID");
            addIndex("JSessionIDID");
            addIndex("methodID");
            addIndex("urlID");
            addIndex("keyID");
            addIndex("UUIDID");
            addIndex("IxnIDID");
            addIndex("DeviceIDID");
            addIndex("userNameID");
            addIndex("browserClientID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp" + ",FileId INTEGER" + ",FileOffset bigint" + ",FileBytes int" + ",line int"
                    /* standard first */
                    + ",IPID int" + ",JSessionIDID int" + ",httpCode int" + ",methodID int" + ",urlID int"
                    + ",keyID int" + ",UUIDID int" + ",IxnIDID int" + ",DeviceIDID int" + ",userNameID int"
                    + ",browserClientID int" + ",executionTime int" + ");";
            getM_dbAccessor().runQuery(query);

            m_InsertStatementId = getM_dbAccessor()
                    .PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                            /* standard first */
                            + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ");");

        }

        /**
         * @throws Exception
         */
        @Override
        public void FinalizeDB() throws Exception {
            createIndexes();
        }

        @Override
            public void AddToDB(Record _rec) throws SQLException {
            ApacheWebMsg rec = (ApacheWebMsg) _rec;
            getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileID());
                stmt.setLong(3, rec.getM_fileOffset());
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.getM_line());

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.IP, rec.getIp()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.JSessionID, rec.getjSessionID()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.HTTPRESPONSE, rec.getHttpCode()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.HTTPMethod, rec.getMethod()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.HTTPURL, rec.getUrl()));
                setFieldInt(stmt, 11, Main.getRef(ReferenceType.Misc, rec.getKey()));
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.UUID, rec.getUUID()));
                setFieldInt(stmt, 13, Main.getRef(ReferenceType.IxnID, rec.getIxnID()));
                setFieldInt(stmt, 14, Main.getRef(ReferenceType.GWSDeviceID, rec.getDeviceID()));
                setFieldInt(stmt, 15, Main.getRef(ReferenceType.Agent, rec.getUserName()));
                setFieldInt(stmt, 16, Main.getRef(ReferenceType.WWEBrowserClient, rec.getBrowserClientID()));
                setFieldInt(stmt, 17, rec.getExecutionTime());

                }
            });
        }


    }
    // </editor-fold>

}
