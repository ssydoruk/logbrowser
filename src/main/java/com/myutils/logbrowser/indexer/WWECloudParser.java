/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

public class WWECloudParser extends Parser {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;

    private static final HashMap<Pattern, String> IGNORE_LOG_WORDS = new HashMap<Pattern, String>() {
        {
            put(Pattern.compile("place \\[[^\\]]+\\]"), "place \\[...\\]");
            put(Pattern.compile("User \\[[^\\]]+\\]"), "User \\[...\\]");
            put(Pattern.compile("name \\[[^\\]]+\\]"), "name \\[...\\]");
            put(Pattern.compile("user \\[[^\\]]+\\]"), "user \\[...\\]");
            put(Pattern.compile("Object \\p{XDigit}+"), "Object ...");
            put(Pattern.compile("CustomContact.+ in "), "CustomContact... in ");
            put(Pattern.compile("Contact.+ in "), "Contact... in ");
            put(Pattern.compile("index.html\\?\\S+"), "index.html?<...>");
            put(Pattern.compile("\\[\\p{XDigit}{32}\\]"), "\\[...\\]");
            put(Pattern.compile("\\[Number=[^\\]]+\\]"), "\\[Number=...\\]");
            put(Pattern.compile("\\[[\\p{XDigit}-]+\\]"), "\\[...\\]");
        }
    };
    private static final Pattern regLineSkip = Pattern.compile("^[>\\s]*");
    private static final Pattern regNotParseMessage = Pattern.compile("(30201)");
    private static final Pattern regMsgStart = Pattern.compile("Handling update message:$");
    private static final Pattern regMsgRequest = Pattern.compile(" Sent '([^']+)' ");

    private static final Pattern regAuthResult = Pattern.compile("CloudWebBasicAuthenticationFilter\\s+(.+)$");
    private static final Pattern regTMessageStart = Pattern.compile("message='([^']+)'");
    private static final Pattern regTMessageAttributesContinue = Pattern.compile("^(Attr|\\s|Time|Call)");
    private static final Pattern regAuthAttributesContinue = Pattern.compile("^(SATR|IATR)");
    private static final Pattern regExceptionStart = Pattern.compile("^([\\w_\\.]+Exception[\\w\\.]*):(.*)");
    private static final Pattern regExceptionContinue = Pattern.compile("^(\\s|Caused by:)");
    private static final Pattern regLogMessage = Pattern.compile("^(WARN|ERROR|INFO)");
    private static final Pattern regLogMessageMsg = Pattern.compile("\\s(?:\\b[a-zA-Z][\\w\\.]*)+(.+)");
    private static final Message.Regexs regAuthUsers = new Message.Regexs(new Pair[]{
            new Pair("user \\[([^\\]]+)\\]", 1),
            new Pair("Username: ([^;]+);", 1)
    });
    long m_CurrentFilePos;
    long m_HeaderOffset;
    String m_Header;
    int m_dbRecords = 0;
    private ParserState m_ParserState;
    private String m_LastLine;
    private boolean isInbound;
    private String m_msgName;
    private String m_msg1;
    private Object msg;

    public WWECloudParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_WWECloud, m_tables);
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
//                    SetBytesConsumed(input.getLastBytesConsumed());
                    setEndFilePos(getFilePos() + l); // to calculate file bytes

                    while (str != null) {
                        str = ParseLine(str);
                    }
                    setFilePos(getFilePos() + l);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                }

                m_LastLine = str; // store previous line for server name
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
        if (!ParseCustom(str, 0)) {
            switch (m_ParserState) {
                case STATE_COMMENT:
                    return null;

                default:
                    Main.logger.error("CustomSearch " + getLastCustomSearch().toString() + " has parserRest=\"false\" while the m_ParserState=[" + m_ParserState + "]. The flag ignored");
                    break;
            }
        }

        switch (m_ParserState) {
            case STATE_COMMENT:

                m_lineStarted = m_CurrentLine;

                String s = ParseGenesys(str, TableType.MsgWWECloud, regNotParseMessage, regLineSkip);

                if ((regMsgStart.matcher(s)).find()) {
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_TMESSAGE_EVENT;
                    setSavedFilePos(getFilePos());
                    isInbound = true;
                } else if ((m = regMsgRequest.matcher(s)).find()) {
                    m_msgName = m.group(1);
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_TMESSAGE_ATTRIBUTES;
                    setSavedFilePos(getFilePos());
                    m_MessageContents.clear();
                    m_MessageContents.add(s);
                    isInbound = false;
                } else if ((m = regExceptionStart.matcher(s)).find()) {
                    m_msgName = m.group(1);
                    m_msg1 = m.group(2);
                    m_MessageContents.add(s);
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_EXCEPTION;
                    setSavedFilePos(getFilePos());
                } else if ((m = regLogMessage.matcher(s)).find()) {
                    m_msgName = m.group(1);
                    String rest = s.substring(m.end());
                    String msgText;
                    if ((m = regLogMessageMsg.matcher(rest)).find()) {
                        msgText = m.group(1);
                    } else {
                        msgText = rest;
                    }
                    WWECloudLogMsg theMsg = new WWECloudLogMsg(m_msgName, msgText);
                    SetStdFieldsAndAdd(theMsg);

                } else if ((m = regAuthResult.matcher(s)).find()) {
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_AUTHRESULT;
                    setSavedFilePos(getFilePos());
                    m_MessageContents.clear();
                    m_MessageContents.add(m.group(1));
                    isInbound = false;

                } else if ((m = regExceptionStart.matcher(s)).find()) {
                    msg = new WWECloudExeptionMsg(m.group(1), m.group(2));
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_EXCEPTION;
                    setSavedFilePos(getFilePos());
                    m_MessageContents.clear();
                    m_MessageContents.add(s);
                    isInbound = false;

                }
                break;

            case STATE_AUTHRESULT: {
                if ((regAuthAttributesContinue.matcher(str)).find()) {
                    m_MessageContents.add(str);
                } else {
                    addAuthResponse();
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;

                }
                break;
            }

            case STATE_EXCEPTION: {
                if ((regExceptionContinue.matcher(str)).find()) {
                    m_MessageContents.add(str);
                } else {
                    if (msg instanceof WWECloudExeptionMsg) {
                        ((WWECloudExeptionMsg) msg).setMessageLines(m_MessageContents);
                        SetStdFieldsAndAdd(((WWECloudExeptionMsg) msg));
                    } else {

                    }
                    msg = null;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }
                break;
            }

            case STATE_TMESSAGE_EVENT: {
                if ((m = regTMessageStart.matcher(str)).find()) {
                    m_msgName = m.group(1);
                    m_ParserState = ParserState.STATE_TMESSAGE_ATTRIBUTES;
                } else {
                    Main.logger.error("l:" + m_CurrentLine + "- unexpected string [" + str + "] in state " + m_ParserState);
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                }
                break;
            }

            case STATE_TMESSAGE_ATTRIBUTES: {
                if (regTMessageAttributesContinue.matcher(str).find()
                        || ParseTimestamp(str) == null) {
                    m_MessageContents.add(str);

                } else {
                    addTLibMessage();
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
        m_tables.put(TableType.WWECloud, new WWECloudTable(Main.getInstance().getM_accessor(), TableType.WWECloud));
        m_tables.put(TableType.WWECloudAuth, new WWECloudAuthTab(Main.getInstance().getM_accessor(), TableType.WWECloudAuth));
        m_tables.put(TableType.WWECloudException, new WWECloudExeptionTab(Main.getInstance().getM_accessor(), TableType.WWECloudException));
        m_tables.put(TableType.WWECloudLog, new WWECloudLogTab(Main.getInstance().getM_accessor(), TableType.WWECloudLog));

    }

    private void addTLibMessage() throws Exception {
        WWECloudMessage theMsg = new WWECloudMessage(m_msgName, m_MessageContents, isParseTimeDiff());
        theMsg.SetInbound(isInbound);
        SetStdFieldsAndAdd(theMsg);
    }

    private void addAuthResponse() {
        try {
            WWECloudAuthMsg theMsg = new WWECloudAuthMsg(m_MessageContents);
            SetStdFieldsAndAdd(theMsg);
        } catch (Exception ex) {
            logger.error("fatal: ", ex);
        }
    }

    enum ParserState {
        STATE_HEADER,
        STATE_TMESSAGE_REQUEST,
        STATE_COMMENT,
        STATE_TMESSAGE_EVENT,
        STATE_CLUSTER,
        STATE_HTTPIN,
        STATE_HTTPHANDLEREQUEST,
        STATE_TMESSAGE_ATTRIBUTES, STATE_EXCEPTION, STATE_AUTHRESULT
    }

    //<editor-fold defaultstate="collapsed" desc="WWECloud">
    public class WWECloudMessage extends Message {

        String m_MessageName;

        private String m_refID;
        private String m_ThisDN;
        private boolean isTServerReq = false;


        public WWECloudMessage(String event, ArrayList newMessageLines, boolean isTServerReq) {
            super(TableType.WWECloud);
            m_MessageLines = newMessageLines;
            m_MessageName = event;
            this.isTServerReq = isTServerReq;
            if (newMessageLines != null) {
                m_MessageLines.add(0, event);
            }
        }

        public boolean isIsTServerReq() {
            return isTServerReq;
        }

        public String GetMessageName() {
            return m_MessageName;
        }

        /**
         * @return the m_refID
         */
        public Integer getM_refID() {
            return intOrDef(getIxnAttributeLong("AttributeReferenceID"), (Integer) null);

        }

        String getConnID() {
            return getIxnAttributeHex("AttributeConnID");
        }

        String getTransferConnID() {
            return getIxnAttributeHex(new String[]{"AttributePreviousConnID", "AttributeTransferConnID"});
        }

        String getUUID() {
            return getIxnAttributeString("AttributeCallUuid");
        }

        String getThisDN() {
            return getIxnAttributeString("AttributeThisDN");
        }

        String getEventName() {
            return m_MessageName;
        }

        Integer getSeqNo() {
            return intOrDef(getIxnAttributeLong("AttributeEventSequenceNumber"), (Integer) null);
        }

        String getIxnID() {
            return null;
        }

        String getServerID() {
            return null;
        }

        String getOtherDN() {
            return getIxnAttributeString("AttributeOtherDN");
        }

        private String getAgent() {
            return getIxnAttributeString("AttributeAgentID");
        }

    }

    private class WWECloudTable extends DBTable {

        public WWECloudTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");
            addIndex("ConnectionIDID");
            addIndex("TransferIdID");
            addIndex("UUIDID");
            addIndex("thisDNID");
            addIndex("nameID");
            addIndex("seqno");
            addIndex("IXNIDID");
            addIndex("ServerID");
            addIndex("otherDNID");
            addIndex("agentIDID");
            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",ConnectionIDID int"
                    + ",TransferIdID int"
                    + ",UUIDID int"
                    + ",referenceID int"
                    + ",inbound bit"
                    + ",thisDNID int"
                    + ",nameID int"
                    + ",seqno bit"
                    + ",IXNIDID bit"
                    + ",ServerID bit"
                    + ",otherDNID int"
                    + ",agentIDID INTEGER"
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
                    + ");");
        }

        /**
         * @throws Exception
         */
        @Override
        public void FinalizeDB() throws Exception {
            createIndexes();
        }

        @Override
        public void AddToDB(Record rec) throws SQLException {
            WWECloudMessage wweRec = (WWECloudMessage) rec;
            getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException {

                    stmt.setTimestamp(1, new Timestamp(wweRec.GetAdjustedUsecTime()));
                    stmt.setInt(2, WWECloudMessage.getFileId());
                    stmt.setLong(3, wweRec.getM_fileOffset());
                    stmt.setLong(4, wweRec.getM_FileBytes());
                    stmt.setLong(5, wweRec.getM_line());

                    setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, wweRec.getConnID()));
                    setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, wweRec.getTransferConnID()));
                    setFieldInt(stmt, 8, Main.getRef(ReferenceType.UUID, wweRec.getUUID()));
                    setFieldInt(stmt, 9, wweRec.getM_refID());
                    stmt.setBoolean(10, wweRec.isInbound());
                    setFieldInt(stmt, 11, Main.getRef(ReferenceType.DN, rec.cleanDN(wweRec.getThisDN())));
                    setFieldInt(stmt, 12, Main.getRef(ReferenceType.TEvent, wweRec.getEventName()));
                    setFieldInt(stmt, 13, wweRec.getSeqNo());
                    setFieldInt(stmt, 14, Main.getRef(ReferenceType.TEvent, wweRec.getIxnID()));
                    setFieldInt(stmt, 15, Main.getRef(ReferenceType.App, wweRec.getServerID()));
                    setFieldInt(stmt, 16, Main.getRef(ReferenceType.DN, rec.cleanDN(wweRec.getOtherDN())));
                    setFieldInt(stmt, 17, Main.getRef(ReferenceType.Agent, wweRec.getAgent()));

                }
            });
        }
    }


//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="CloudAuth">

        private class WWECloudAuthMsg extends Message {

            private WWECloudAuthMsg(ArrayList<String> m_MessageContents) {
                super(TableType.WWECloudAuth, m_MessageContents);
            }

            private String getSuccess() {
                if (m_MessageLines != null && !m_MessageLines.isEmpty()) {
                    if (m_MessageLines.get(0).contains("Authentication success")) {
                        return "success";
                    } else if (m_MessageLines.get(0).contains("authentication failed")) {
                        return "authentication failed";
                    }
                }

                return "(unknown)";
            }

            private String getAgent() {
                return FindByRx(regAuthUsers);
            }

            public String getError() {
                return getIxnAttributeString("SATRCFG_DESCRIPTION");
            }

        }

        private class WWECloudAuthTab extends DBTable {

            public WWECloudAuthTab(DBAccessor dbaccessor, TableType t) {
                super(dbaccessor, t);
            }

            @Override
            public void InitDB() {
                StringBuilder buf = new StringBuilder();
                addIndex("time");
                addIndex("FileId");
                addIndex("successID");
                addIndex("userID");
                addIndex("errorID");

                dropIndexes();

                String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                        + ",time timestamp"
                        + ",FileId INTEGER"
                        + ",FileOffset bigint"
                        + ",FileBytes int"
                        + ",line int"
                        /* standard first */
                        + ",successID int"
                        + ",userID int"
                        + ",errorID int"
                        + ");";
                getM_dbAccessor().runQuery(query);

                m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                        /*standard first*/
                        + ",?"
                        + ",?"
                        + ",?"
                        + ");"
                );

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
                WWECloudAuthMsg rec = (WWECloudAuthMsg) _rec;
                getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                    @Override
                    public void fillStatement(PreparedStatement stmt) throws SQLException {
                        stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                        stmt.setInt(2, WWECloudAuthMsg.getFileId());
                        stmt.setLong(3, rec.getM_fileOffset());
                        stmt.setLong(4, rec.getM_FileBytes());
                        stmt.setLong(5, rec.getM_line());

                        setFieldInt(stmt, 6, Main.getRef(ReferenceType.Misc, rec.getSuccess()));
                        setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, rec.getAgent()));
                        setFieldInt(stmt, 8, Main.getRef(ReferenceType.ErrorDescr, rec.getError()));

                    }
                });
            }


        }
//</editor-fold>

        private class WWECloudLogMsg extends Message {

            private final String msg;
            private final String msgText;

            private WWECloudLogMsg(String m_msgName, String msgText) {
                super(TableType.WWECloudLog);
                this.msg = m_msgName;
                this.msgText = optimizeText(msgText);
            }

            public String getMsg() {
                return msg;
            }

            public String getMsgText() {
                return msgText;
            }

            private String optimizeText(String msgText) {
                String ret = msgText;
                for (Map.Entry<Pattern, String> entry : IGNORE_LOG_WORDS.entrySet()) {
                    ret = entry.getKey().matcher(ret).replaceAll(entry.getValue());
                }
                return ret;

            }
        }

        private class WWECloudExeptionMsg extends Message {

            private final String exceptionName;

            private final String msg;

            private WWECloudExeptionMsg(String exName, String msg) {
                super(TableType.WWECloudException);
                this.exceptionName = exName;
                this.msg = msg;
            }

            public String getExceptionName() {
                return exceptionName;
            }

            public String getMsg() {
                return msg;
            }

        }

        private class WWECloudExeptionTab extends DBTable {

            public WWECloudExeptionTab(DBAccessor dbaccessor, TableType t) {
                super(dbaccessor, t);
            }

            @Override
            public void InitDB() {
                StringBuilder buf = new StringBuilder();
                addIndex("time");
                addIndex("FileId");
                addIndex("exNameID");
                addIndex("msgID");

                dropIndexes();

                String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                        + ",time timestamp"
                        + ",FileId INTEGER"
                        + ",FileOffset bigint"
                        + ",FileBytes int"
                        + ",line int"
                        /* standard first */
                        + ",exNameID int"
                        + ",msgID int"
                        + ");";
                getM_dbAccessor().runQuery(query);

                m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                        /*standard first*/
                        + ",?"
                        + ",?"
                        + ");"
                );

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
                WWECloudExeptionMsg rec = (WWECloudExeptionMsg) _rec;
                getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                    @Override
                    public void fillStatement(PreparedStatement stmt) throws SQLException {
                        stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                        stmt.setInt(2, WWECloudExeptionMsg.getFileId());
                        stmt.setLong(3, rec.getM_fileOffset());
                        stmt.setLong(4, rec.getM_FileBytes());
                        stmt.setLong(5, rec.getM_line());

                        setFieldInt(stmt, 6, Main.getRef(ReferenceType.Exception, rec.getExceptionName()));
                        setFieldInt(stmt, 7, Main.getRef(ReferenceType.ExceptionMessage, rec.getMsg()));

                    }
                });
            }


        }

        private class WWECloudLogTab extends DBTable {

            public WWECloudLogTab(DBAccessor dbaccessor, TableType t) {
                super(dbaccessor, t);
            }

            @Override
            public void InitDB() {
                addIndex("time");
                addIndex("FileId");
                addIndex("msgID");
                addIndex("msgTextID");

                dropIndexes();

                String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                        + ",time timestamp"
                        + ",FileId INTEGER"
                        + ",FileOffset bigint"
                        + ",FileBytes int"
                        + ",line int"
                        /* standard first */
                        + ",msgID int"
                        + ",msgTextID int"
                        + ");";
                getM_dbAccessor().runQuery(query);

                m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                        /*standard first*/
                        + ",?"
                        + ",?"
                        + ");"
                );

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
                WWECloudLogMsg rec = (WWECloudLogMsg) _rec;
                getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                    @Override
                    public void fillStatement(PreparedStatement stmt) throws SQLException {
                        stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                        stmt.setInt(2, WWECloudLogMsg.getFileId());
                        stmt.setLong(3, rec.getM_fileOffset());
                        stmt.setLong(4, rec.getM_FileBytes());
                        stmt.setLong(5, rec.getM_line());

                        setFieldInt(stmt, 6, Main.getRef(ReferenceType.CLOUD_LOG_MESSAGE_TYPE, rec.getMsg()));
                        setFieldInt(stmt, 7, Main.getRef(ReferenceType.CLOUD_LOG_MESSAGE, rec.getMsgText()));

                    }
                });
            }
        }
//</editor-fold>

    }


