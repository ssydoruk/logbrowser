package com.myutils.logbrowser.indexer;

import static Utils.Util.intOrDef;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfServParser extends Parser {

    private static final Pattern regChangeRequest = Pattern.compile("^\\s*Message MSGCFG_((?:AD|CH|DE)\\w+)");
    private static final Pattern regAuthRequest = Pattern.compile("^\\s*Message (MSGCFG_\\w+).+from (\\d+) \\((.+) '(.+)'\\)$");
    private static final Pattern regAuthResponse = Pattern.compile("^\\s*Message (MSGCFG_\\w+).+to (\\d+) \\((.+) '(.+)'\\)$");
    private static final Pattern regChangeFinal = Pattern.compile("^\\s*Object: \\[([^\\]]+)\\], name \\[([^\\]]+)\\], DBID: \\[(\\d+)\\] is ([^\\]]+) by client, type \\[([^\\]]+)\\], name: \\[([^\\]]+)\\], user: \\[([^\\]]+)\\]");
    private static final Pattern regChangeProxy = Pattern.compile("^\\s*Object: \\[([^\\]]+)\\], name \\[([^\\]]*)\\], DBID: \\[(\\d+)\\] is ([^\\]]+) at server");
    private static final Pattern regConfigToClients = Pattern.compile("^\\s*There are \\[(\\d+)\\] objects of type \\[([^\\]]+)\\] sent to the client \\[([^\\]]+)\\] \\(application \\[([^\\]]+)\\], type \\[([^\\]]+)\\]\\)");
    private static final Pattern regNewClient = Pattern.compile("^\\s*New client (\\d+) connected");

    private static final Pattern regChangeEnd = Pattern.compile("^\\s*Trc 2421. Object \\[([^\\]]+)\\](?:, DBID \\[(\\d+)\\])? is to be ([^\\]]+) by client, type \\[([^\\]]+)\\], name: \\[([^\\]]+)\\], user: \\[([^\\]]+)\\]");
    private static final Pattern regAuthStart = Pattern.compile("^\\s*(MSGCFG_.+)*?$");
    private static final Pattern regAuthCont = Pattern.compile("^\\s*attr:");
    private static final Pattern regNewClientAuth = Pattern.compile("^\\s*Extended info : Client (\\d+) authorized, name \\[([^\\]]+)\\], type \\[([^\\]]+)\\], user name \\[([^\\]]+)\\], protocol \\[([^\\]]+)\\], address \\[([^\\]]+)\\]");
    private static final Pattern regClientDisconn = Pattern.compile("^\\s*Extended info : Client \\[([^\\]]+)\\] disconnected, application \\[([^\\]]+)\\], type \\[([^\\]]+)\\]");
    private static final Pattern regClientDisconn1 = Pattern.compile("^\\s*Client '(\\w+)' disconnected");

    long m_CurrentFilePos;
    // parse state contants

    long m_HeaderOffset;
    private ParserState m_ParserState;

    String m_ServerName;

    private Message msgTmp;

    public ConfServParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_ConfServer, m_tables);

    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentLine = line;

        m_ServerName = null;

        try {
            input.skip(offset);
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            m_ParserState = ParserState.STATE_HEADER;

            String str;
            while ((str = input.readLine()) != null) {

                try {
//                    int l=str.length()+input.charsSkippedOnReadLine;
//                    int l = str.getBytes("UTF-16").length/2+input.charsSkippedOnReadLine;
                    int l = input.getLastBytesConsumed();
                    setEndFilePos(getFilePos() + l); // to calculate file bytes
                    m_CurrentLine++;
                    Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");

                    while (str != null) {
                        str = ParseLine(str, input);
                    }
                    setFilePos(getFilePos() + l);
//                    addFilePos(str.length()+input.charsSkippedOnReadLine);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                    throw e;
                }
            }
            ParseLine("", null); // to complete the parsing of the last line/last message

        } catch (Exception e) {
            e.printStackTrace();
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }
    private static final Pattern regNotParseMessage = Pattern.compile("^("
            + "04541|"
            + "04524|"
            + "24215|"
            + "24308|"
            + "24200|"
            + "04542"
            + ")");

    String ParseGenesysLocal(String str) throws Exception {
        return ParseGenesys(str, TableType.MsgConfServer, regNotParseMessage);
    }

    private String ParseLine(String str, BufferedReaderCrLf in) throws Exception {
        String s = str;
        Matcher m;
        GenesysMsg lastLogMsg;

        Main.logger.trace("ParseLine " + m_CurrentLine + ": state -" + m_ParserState);
        if (!ParseCustom(str, 0)) {
            switch (m_ParserState) {
                case STATE_HEADER:
                case STATE_COMMENTS:
                    return null;

                default:
                    Main.logger.error("CustomSearch " + getLastCustomSearch().toString() + " has parserRest=\"false\" while the m_ParserState=[" + m_ParserState + "]. The flag ignored");
                    break;
            }
        }

        switch (m_ParserState) {
//<editor-fold defaultstate="collapsed" desc="STATE_HEADER">
            case STATE_HEADER:
                if (m_ServerName == null) {
//                    ParseServerName(trimmed);
                }
                if (s.startsWith("File:")) {
                    m_ParserState = ParserState.STATE_COMMENTS;
                }
                break;
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="STATE_COMMENTS">
            case STATE_COMMENTS:

                m_LineStarted = m_CurrentLine;

                s = ParseGenesysLocal(str);

                String lastGenesysMsgID = getLastGenesysMsgID();
                String lastGenesysMsgLevel = getLastGenesysMsgLevel();

                if (lastGenesysMsgID != null) {
                    if (lastGenesysMsgID.equals("04541")) {
                        if ((m = regChangeRequest.matcher(s)).find()) {
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                            m_ParserState = ParserState.STATE_UPDATE;
                            return null;
                        } else if ((m = regAuthRequest.matcher(s)).find()) {
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                            m_ParserState = ParserState.STATE_AUTH1;
                            msgTmp = new CSClientRequest1(m.group(1), m.group(2),
                                    m.group(3), m.group(4));
                            return null;
                        }
                    } else if (lastGenesysMsgID.equals("04542")) {
                        if ((m = regAuthResponse.matcher(s)).find()) {
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                            m_ParserState = ParserState.STATE_AUTH1;
                            msgTmp = new CSClientRequest1(m.group(1), m.group(2),
                                    m.group(3), m.group(4));
                            return null;
                        }
                    } else if (lastGenesysMsgID.startsWith("2420")) {
                        if (lastGenesysMsgLevel != null && lastGenesysMsgLevel.equalsIgnoreCase("std")) {
                            if ((m = regChangeFinal.matcher(s)).find()) {
                                CSClientRequest1 msg = new CSClientRequest1(m_MessageContents);
                                msg.setObjType(m.group(1));
                                msg.setDBID(m.group(3));
                                msg.setName(m.group(2));

                                msg.setOp(m.group(4));
                                msg.setClientType(m.group(5));
                                msg.setAppName(m.group(6));
                                msg.setUserName(m.group(7));
                                SetStdFieldsAndAdd(msg);
                                m_ParserState = ParserState.STATE_COMMENTS;
                                m_MessageContents.clear();
                                return null;
                            }
                        }

                    } else if (lastGenesysMsgID.startsWith("2421")) {
                        if ((m = regConfigToClients.matcher(s)).find()) {
                            CSConfToClient msg = new CSConfToClient();
                            msg.setObjectsNumber(m.group(1));
                            msg.setObjType(m.group(2));
                            msg.setClientSocket(m.group(3));
                            msg.setAppName(m.group(4));
                            msg.setClientType(m.group(5));

                            SetStdFieldsAndAdd(msg);
                            m_ParserState = ParserState.STATE_COMMENTS;
                            m_MessageContents.clear();
                            return null;
                        }
                    } else if (lastGenesysMsgID.startsWith("04520")) {
                        if ((m = regNewClient.matcher(s)).find()) {
                            CSClientConnect msg = new CSClientConnect();
                            msg.setConnect(true);
                            msg.setClientSocket(m.group(1));

                            SetStdFieldsAndAdd(msg);
                            m_ParserState = ParserState.STATE_COMMENTS;
                            m_MessageContents.clear();
                            return null;
                        }
                    } else if ((m = regNewClientAuth.matcher(s)).find()) {
                        CSClientConnect msg = new CSClientConnect();
                        msg.setConnect(true);
                        msg.setClientSocket(m.group(1));
                        msg.setAppName(m.group(2));
                        msg.setClientType(m.group(3));
                        msg.setUserName(m.group(4));
                        msg.setClientAddress(m.group(6));

                        SetStdFieldsAndAdd(msg);
                        m_ParserState = ParserState.STATE_COMMENTS;
                        m_MessageContents.clear();
                        return null;
                    } else if ((m = regClientDisconn.matcher(s)).find()) {
                        CSClientConnect msg = new CSClientConnect();
                        msg.setConnect(false);
                        msg.setClientSocket(m.group(1));
                        msg.setAppName(m.group(2));
                        msg.setClientType(m.group(3));

                        SetStdFieldsAndAdd(msg);
                        m_ParserState = ParserState.STATE_COMMENTS;
                        m_MessageContents.clear();
                        return null;
                    } else if ((m = regClientDisconn1.matcher(s)).find()) {
                        CSClientConnect msg = new CSClientConnect();
                        msg.setConnect(false);
                        msg.setClientSocket(m.group(1));

                        SetStdFieldsAndAdd(msg);
                        m_ParserState = ParserState.STATE_COMMENTS;
                        m_MessageContents.clear();
                        return null;
                    }

                }

                GenesysMsg.CheckGenesysMsg(dp, this, TableType.MsgConfServer, null);

                break;
//</editor-fold>

            case STATE_AUTH1:
                if ((m = regAuthStart.matcher(s)).find()) {
                    m_MessageContents.add(str);
                } else {
                    m_MessageContents.add(str);
                    m_ParserState = ParserState.STATE_AUTH2;
                }
                break;

            case STATE_AUTH2:
                if ((m = regAuthCont.matcher(s)).find()) {
                    m_MessageContents.add(str);
                } else {
                    m_MessageContents.add(str);
                    msgTmp.setMessageLines(m_MessageContents);
                    SetStdFieldsAndAdd(msgTmp);
                    msgTmp = null;
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                }
                break;

            case STATE_UPDATE: {
                s = ParseGenesysLocal(str);
                if ((m = regChangeEnd.matcher(s)).find()) {
                    m_MessageContents.add(str);
                    CSClientRequest1 msg = new CSClientRequest1(m_MessageContents);
                    msg.setObjType(m.group(1));
                    msg.setDBID(m.group(2));

                    msg.setOp(m.group(3));
                    msg.setClientType(m.group(4));
                    msg.setAppName(m.group(5));
                    msg.setUserName(m.group(6));
                    SetStdFieldsAndAdd(msg);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                } else if ((m = regChangeFinal.matcher(s)).find()) {
                    m_MessageContents.add(str);
                    CSClientRequest1 msg = new CSClientRequest1(m_MessageContents);
                    msg.setObjType(m.group(1));
                    msg.setName(m.group(2));
                    msg.setDBID(m.group(3));

                    msg.setOp(m.group(4));
                    msg.setClientType(m.group(5));
                    msg.setAppName(m.group(6));
                    msg.setUserName(m.group(7));
                    SetStdFieldsAndAdd(msg);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                } else if ((m = regChangeProxy.matcher(s)).find()) {
                    m_MessageContents.add(str);
                    CSClientRequest1 msg = new CSClientRequest1(m_MessageContents);
                    msg.setObjType(m.group(1));
                    msg.setName(m.group(2));
                    msg.setDBID(m.group(3));

                    msg.setOp(m.group(4));
                    msg.setClientType("server");

                    SetStdFieldsAndAdd(msg);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();

                } else if (dp != null
                        && (lastLogMsg = getLastLogMsg()) != null && regNotParseMessage.matcher(lastLogMsg.getLastGenesysMsgID()).find()) {
                    CSClientRequest1 msg = new CSClientRequest1(m_MessageContents);

                    SetStdFieldsAndAdd(msg);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                    return str;
                } else {
                    m_MessageContents.add(str);
                }
                break;
            }
        }
        return null;
    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
        m_tables.put(TableType.CSUpdate, new CSClientRequestTable1(Main.getMain().getM_accessor(), TableType.CSUpdate));
        m_tables.put(TableType.CSClientConf, new CSConfToClientTable(Main.getMain().getM_accessor(), TableType.CSClientConf));
        m_tables.put(TableType.CSClientConnect, new CSClientConnectTable(Main.getMain().getM_accessor(), TableType.CSClientConnect));

    }

    enum ParserState {

        STATE_HEADER,
        STATE_COMMENTS,
        STATE_UPDATE, STATE_AUTH1, STATE_AUTH2
    }
    final static Pattern ptOp = Pattern.compile("(MSGCFG\\S+)");

    private class CSClientRequest1 extends Message {

        private String ObjType;
        private String op;
        private String clientType;
        private String appName;
        private String userName;
        private Integer dbid = null;
        private String objName = null;

        private CSClientRequest1(String req, String descr, String appType, String appName) {
            super(TableType.CSUpdate);
            this.op = req;
            this.clientType = appType;
            this.appName = appName;
        }

        public String getObjType() {
            if (ObjType != null && !ObjType.isEmpty()) {
                return ObjType;
            } else {
                return cfgGetAttrObjType("IATRCFG_OBJECTTYPE");
            }
        }

        public String getOp() {
            if (op != null && !op.isEmpty()) {
                return op;
            } else {
                return FindByRx(ptOp, 1, (String) null);

            }
        }

        public String getClientType() {
            return clientType;
        }

        public String getAppName() {
            return appName;
        }

        public String getUserName() {
            if (userName == null || userName.isEmpty()) {
                return cfgGetAttr("SATRCFG_USERNAME");
            }
            return userName;
        }

        private CSClientRequest1(ArrayList<String> m_MessageContents) {
            super(TableType.CSUpdate, m_MessageContents);
        }

        private void setObjType(String group) {
            this.ObjType = group;
        }

        private void setOp(String group) {
            this.op = group;
        }

        private void setClientType(String group) {
            this.clientType = group;
        }

        private void setAppName(String group) {
            this.appName = group;
        }

        private void setUserName(String group) {
            this.userName = group;
        }

        private void setDBID(String group) {
            this.dbid = getIntOrDef(group, (Integer) null);
        }

        public Integer getDbid() {
            return dbid;
        }

        private String getObjName() {
            return objName;
        }

        private void setName(String group) {
            this.objName = group;
        }

        private Integer getObjectsNumber() {
            return null;
        }

        private Integer getRefID() {
            return intOrDef(cfgGetAttr("IATRCFG_REQUESTID"), (Integer) null);
        }

        private String getErrMessage() {
            return cfgGetAttr("SATRCFG_DESCRIPTION");

        }

    }

    private class CSClientRequestTable1 extends DBTable {

        public CSClientRequestTable1(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");
            addIndex("theAppNameID");
            addIndex("objTypeID");
            addIndex("objNameID");
            addIndex("clientTypeID");
            addIndex("userNameID");
            addIndex("opID");
            addIndex("msgID");
            addIndex("refID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",theAppNameID INTEGER"
                    + ",objTypeID INTEGER"
                    + ",clientTypeID INTEGER"
                    + ",userNameID INTEGER"
                    + ",opID INTEGER"
                    + ",dbid INTEGER"
                    + ",objNameID INTEGER"
                    + ",refID INTEGER"
                    + ",msgID INTEGER"
                    + ");";
            getM_dbAccessor().runQuery(query);
            m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
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
                    + ");");

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
            CSClientRequest1 rec = (CSClientRequest1) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.App, rec.getAppName()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.ObjectType, rec.getObjType()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.AppType, rec.getClientType()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.Agent, Utils.Util.StripQuotes(rec.getUserName())));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.CfgOp, rec.getOp()));
                setFieldInt(stmt, 11, rec.getDbid());
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.CfgObjName, rec.getObjName()));
                setFieldInt(stmt, 13, rec.getRefID());
                setFieldInt(stmt, 14, Main.getRef(ReferenceType.CfgMsg, rec.getErrMessage()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }

    private class CSConfToClient extends Message {

        private String ObjType;
        private String clientType;
        private String appName;
        private Integer objectsNumber;
        private int socket;

        public String getObjType() {
            return ObjType;
        }

        public String getClientType() {
            return clientType;
        }

        public String getAppName() {
            return appName;
        }

        private CSConfToClient() {
            super(TableType.CSClientConf);
        }

        private CSConfToClient(ArrayList<String> m_MessageContents) {
            super(TableType.CSClientConf, m_MessageContents);
        }

        private void setObjType(String group) {
            this.ObjType = group;
        }

        private void setClientType(String group) {
            this.clientType = group;
        }

        private void setAppName(String group) {
            this.appName = group;
        }

        private void setClientSocket(String group) {
            this.socket = getIntOrDef(group, 0);
        }

        public Integer getObjectsNumber() {
            return objectsNumber;
        }

        public int getSocket() {
            return socket;
        }

        private void setObjectsNumber(String group) {
            this.objectsNumber = intOrDef(group, 0);
        }

    }

    private class CSConfToClientTable extends DBTable {

        public CSConfToClientTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");
            addIndex("theAppNameID");
            addIndex("objTypeID");
            addIndex("clientTypeID");
            addIndex("objectsNo");
            addIndex("socketNo");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",theAppNameID INTEGER"
                    + ",objTypeID INTEGER"
                    + ",clientTypeID INTEGER"
                    + ",objectsNo INTEGER"
                    + ",socketNo INTEGER"
                    + ");";
            getM_dbAccessor().runQuery(query);
            m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ");");

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
            CSConfToClient rec = (CSConfToClient) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.App, rec.getAppName()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.ObjectType, rec.getObjType()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.AppType, rec.getClientType()));
                setFieldInt(stmt, 9, rec.getObjectsNumber());
                setFieldInt(stmt, 10, rec.getSocket());

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }

    private class CSClientConnect extends Message {

        private String clientType;
        private String appName;
        private boolean connect;
        private String userName;
        private String clientAddr;
        private Integer socket;

        public String getClientType() {
            return clientType;
        }

        public String getAppName() {
            return appName;
        }

        private CSClientConnect() {
            super(TableType.CSClientConnect);
        }

        private void setClientType(String group) {
            this.clientType = group;
        }

        private void setAppName(String group) {
            this.appName = group;
        }

        private void setConnect(boolean b) {
            this.connect = b;
        }

        private void setUserName(String group) {
            this.userName = group;
        }

        private void setClientAddress(String group) {
            this.clientAddr = group;
        }

        public boolean isConnect() {
            return connect;
        }

        public String getUserName() {
            return userName;
        }

        public String getClientAddr() {
            return clientAddr;
        }

        private void setClientSocket(String group) {
            this.socket = intOrDef(group, 0);
        }

        public Integer getSocket() {
            return socket;
        }

    }

    private class CSClientConnectTable extends DBTable {

        public CSClientConnectTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");
            addIndex("theAppNameID");
            addIndex("clientTypeID");
            addIndex("socketNo");
            addIndex("userNameID");
            addIndex("hostportID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",theAppNameID INTEGER"
                    + ",clientTypeID INTEGER"
                    + ",socketNo INTEGER"
                    + ",userNameID INTEGER"
                    + ",hostportID INTEGER"
                    + ",isConnect bit"
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
                    + ");");

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
            CSClientConnect rec = (CSClientConnect) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.App, rec.getAppName()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.AppType, rec.getClientType()));
                setFieldInt(stmt, 8, rec.getSocket());
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.Agent, Utils.Util.StripQuotes(rec.getUserName())));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.Host, rec.getClientAddr()));
                stmt.setBoolean(11, rec.isConnect());

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }

}
