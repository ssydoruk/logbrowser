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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ssydoruk
 */
public class WWEParserTemplate extends WebParser {

    private static final Pattern regMsgStart = Pattern.compile("Handling update message:$");

    private ParserState m_ParserState;

    private final HashMap<String, ParserState> threadParserState = new HashMap<>();

    /*	public OrsParser(DBAccessor accessor) {
     m_accessor = accessor;
     }
     */
 /*
     public boolean CheckLog(File file) {
     FileInfo fi = new FileInfo();
     fi.m_componentType = FileInfo.type_Unknown;
     m_fileId = m_accessor.SetFileInfo(file, fi);
     return true;
     }
     */
    private final HashMap<String, String> ThreadAlias = new HashMap<>();
    private String URL = null;
    private String app = null;
    private String m_msg1;
    private boolean skipNextLine;
    private boolean skipMessage;
    private Message msg;

    WWEParserTemplate(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_WWE, m_tables);
        m_MessageContents = new ArrayList();
        ThreadAlias.put("ORSInternal", "FMWeb");
    }

    private void SetParserThreadState(String thr, ParserState ps) throws Exception {
        if (thr == null) {
            throw new Exception("No thread");
        }
        if (ps == ParserState.STATE_COMMENT || ps == ParserState.STATE_HEADER) {//by default there is no state to parser
            threadParserState.remove(thr);
        } else {
            threadParserState.put(thr, ps);
        }
    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentLine = line;
        skipNextLine = false;
        skipMessage = false;

        URL = null;
        app = null;

        try {
            input.skip(offset);

            m_ParserState = ParserState.STATE_INIT;
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            String str;
            while ((str = input.readLine()) != null) {
                m_CurrentLine++;
                Main.logger.trace("l: " + m_CurrentLine + " st:" + m_ParserState + " [" + str + "]");

                try {
                    int l = input.getLastBytesConsumed();
//                    SetBytesConsumed(input.getLastBytesConsumed());
                    setEndFilePos(getFilePos() + l); // to calculate file bytes

                    Object o = str;
                    while (o != null) {
                        o = ParseLine(o, input);
                    }
                    setFilePos(getFilePos() + l);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                }

//                m_CurrentFilePos += str.length()+input.charsSkippedOnReadLine;
            }
//            ParseLine("", null); // to complete the parsing of the last line/last message
        } catch (IOException e) {
            Main.logger.error(e);
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

    private Object ParseLine(Object objToParse, BufferedReaderCrLf in) throws Exception {
        if (objToParse == null) {
            return null;
        }
        String str = null;
        String s = null;
        boolean dateParsed = false;
        if (objToParse instanceof String) {
            str = (String) objToParse;
            s = str;

        } else if (objToParse instanceof DateParsed) {
            dp = (DateParsed) objToParse;
            s = dp.rest;
            str = dp.orig;
            dateParsed = true;
        }
        Matcher m;

        if (skipNextLine) {
            skipNextLine = false;
            return null;
        }
        boolean ParseCustom = ParseCustom(str, 0);

        switch (m_ParserState) {
            case STATE_INIT: {
                if (str.startsWith("Start time:") || str.startsWith("+++++++++++++++++++++++++++++++++++++++++++")) {
                    m_ParserState = ParserState.STATE_HEADER;
                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                }

            }
            break;

            case STATE_HEADER: {
                if (str.startsWith("+++++++++++++++++++++++++++++++++++++++++++++++")) {
                    m_ParserState = ParserState.STATE_COMMENT;
                }
            }
            break;

            case STATE_COMMENT:
                m_lineStarted = m_CurrentLine;
                if (!dateParsed) {
                    s = ParseTimestampStr1(str);
                }
//                getThread(getDP());

                if ((m = regMsgStart.matcher(s)).find()) {
                    m_ParserState = ParserState.STATE_TMESSAGE_EVENT;
                    setSavedFilePos(getFilePos());
                    break;

                }
                break;

            case STATE_READ_INFO: {
                DateParsed dp = ParseFormatDate(str);
                if (dp == null) {
                    m_MessageContents.add(str);

                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }

            }
            break;

        }
        return null;
    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
        m_tables.put(TableType.WWEMessage, new WWEDebugMessageTable(Main.getM_accessor(), TableType.WWEMessage));

    }

    enum ParserState {
        STATE_INIT,
        STATE_HEADER,
        STATE_TMESSAGE_REQUEST,
        STATE_COMMENT,
        STATE_TMESSAGE_EVENT,
        STATE_READ_INFO,
    }

//<editor-fold defaultstate="collapsed" desc="WWEDebugMsg">
    private class WWEDebugMsg extends Message {

        private String ip;
        private String jSessionID;
        private String httpCode;
        private String date;
        private String method;
        private String key;
        private String url;

        private String deviceID;
        private String userName;
        private String level;
        private String className;
        private String msgText;
        private String param1;
        private String param2;
        private String userID;
        private String browserClient;
        private String sessionID;
        private String UUID;
        private String ixnID;

        private WWEDebugMsg(TableType t) {
            super(t);
        }

        private WWEDebugMsg() {
            this(TableType.WWEMessage);
        }

        private WWEDebugMsg(TableType t, WWEDebugMsg orig) {
            this(t);
            this.setClassName(orig.getClassName());
            this.setJSessionID(orig.getjSessionID());
            this.setLevel(orig.getLevel());
            this.setUserName(orig.getUserName());
            this.setIP(orig.getIp());
            this.setURL(orig.getUrl());
            this.setMessageText(orig.getMsgText());
            this.setUUID(orig.getUUID());
            this.setIxnID(orig.getIxnID());
            this.setDeviceID(orig.getDeviceID());
        }

        public String getParam2() {
            return param2;
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
            checkDiffer(this.deviceID, deviceID, "deviceID", m_CurrentLine);
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
            checkDiffer(this.UUID, UUID, "UUID", m_CurrentLine);

            this.UUID = UUID;
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
            checkDiffer(this.ixnID, ixnID, "ixnID", m_CurrentLine);

            this.ixnID = ixnID;
        }

        public String getHttpCode() {

            return httpCode;
        }

        public String getMethod() {
            return method;
        }

        private void setMethod(String s) {
            method = s;
        }

        private void setHttpCode(String s) {
            httpCode = s;
        }

        private void setDate(String s) {
            date = s;
        }

        private void setIP(String s) {
            if (s != null) {
                ip = s.trim();
            }
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

        private void setUserKey(String s) {

            key = s;
        }

        private void setJSessionID(String s) {
            checkDiffer(this.jSessionID, s, "jSessionID", m_CurrentLine);

            jSessionID = s;
        }

        private void setURL(String group) {
            if (group != null && !group.isEmpty()) {
                try {
                    url = URLDecoder.decode(group, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(ApacheWebLogsParser.class.getName()).log(Level.SEVERE, null, ex);
                    url = group;
                }
            }
        }

        private void setUserName(String get) {
            if (userName != null && !userName.isEmpty()) {
                setParam1(get);
            } else {
                checkDiffer(this.userName, get, "userName", m_CurrentLine);
                this.userName = get;
            }

        }

        private void setLevel(String group) {
            this.level = group;

        }

        private void setClassName(String group) {
            this.className = group;

        }

        public void setMsgText(String msgText) {
            this.msgText = msgText;
        }

        public void setParam1(String param1) {
            checkDiffer(this.param1, param1, "param1", m_CurrentLine);

        }

        public void setParam2(String param2) {
            this.param2 = param2;
        }

        private void setMessageText(String group) {
            this.msgText = group;
        }

        public String getLevel() {
            return level;
        }

        public String getClassName() {
            return className;
        }

        public String getMsgText() {
            return msgText;
        }

        private void setURIParam(String group) {
            this.param1 = group;
        }

        public String getParam1() {
            return param1;
        }

        private void setUserID(String key) {
            checkDiffer(this.userID, key, "userID", m_CurrentLine);
            this.userID = key;
        }

        public String getUserID() {
            return userID;
        }

        private void setClientID(String key) {
            this.browserClient = key;
        }

        public String getBrowserClient() {
            return browserClient;
        }

        private void setSessionID(String key) {
            checkDiffer(this.sessionID, key, "sessionID", m_CurrentLine);

            this.sessionID = key;
        }

        public String getSessionID() {
            return sessionID;
        }

    }

    private class WWEDebugMessageTable extends DBTable {

        public WWEDebugMessageTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("IPID");
            addIndex("JSessionIDID");
            addIndex("urlID");
            addIndex("LevelID");
            addIndex("userNameID");
            addIndex("classNameID");
            addIndex("msgTextID");
            addIndex("param1ID");
            addIndex("param2ID");
            addIndex("UUIDID");
            addIndex("IxnIDID");
            addIndex("DeviceIDID");
            addIndex("UserIDID");
            addIndex("browserClientID");
            addIndex("sessionID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",IPID int"
                    + ",JSessionIDID int"
                    + ",urlID int"
                    + ",LevelID int"
                    + ",classNameID int"
                    + ",msgTextID int"
                    + ",userNameID int"
                    + ",param1ID int"
                    + ",UUIDID int"
                    + ",IxnIDID int"
                    + ",DeviceIDID int"
                    + ",param2ID int"
                    + ",UserIDID int"
                    + ",browserClientID int"
                    + ",sessionID int"
                    + ");";
            getM_dbAccessor().runQuery(query);

            m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ");"
            );

        }

        /**
         *
         * @throws Exception
         */
        @Override
        public void FinalizeDB() throws Exception {
            createIndexes();
        }

        @Override
        public void AddToDB(Record _rec) {
            WWEDebugMsg rec = (WWEDebugMsg) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, WWEDebugMsg.getFileId());
                stmt.setLong(3, rec.getM_fileOffset());
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.getM_line());

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.IP, rec.getIp()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.JSessionID, rec.getjSessionID()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.HTTPURL, rec.getUrl()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.MSGLEVEL, rec.getLevel()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.JAVA_CLASS, rec.getClassName()));
                setFieldInt(stmt, 11, Main.getRef(ReferenceType.Misc, rec.getMsgText()));
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.Agent, rec.getUserName()));
                setFieldInt(stmt, 13, Main.getRef(ReferenceType.MiscParam, rec.getParam1()));
                setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, rec.getUUID()));
                setFieldInt(stmt, 15, Main.getRef(ReferenceType.IxnID, rec.getIxnID()));
                setFieldInt(stmt, 16, Main.getRef(ReferenceType.GWSDeviceID, rec.getDeviceID()));
                setFieldInt(stmt, 17, Main.getRef(ReferenceType.MiscParam, rec.getParam2()));
                setFieldInt(stmt, 18, Main.getRef(ReferenceType.WWEUserID, rec.getUserID()));
                setFieldInt(stmt, 19, Main.getRef(ReferenceType.WWEBrowserClient, rec.getBrowserClient()));
                setFieldInt(stmt, 20, Main.getRef(ReferenceType.WWEBrowserSession, rec.getSessionID()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }
//</editor-fold>

}
