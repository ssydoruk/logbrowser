package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.StripQuotes;
import static Utils.Util.intOrDef;


public class ConfServParser extends Parser {

    private static final Pattern regConfigMessage = Pattern.compile("^Message (MSGCFG_\\w+) (received from|sent to) (\\d+) \\((.*)? '(.*)?'\\)");

    private static final Pattern regStartsWithSpace = Pattern.compile("^\\s");

    private static final Pattern ptOp = Pattern.compile("(MSGCFG\\S+)");
    private static final Pattern regChangeRequest = Pattern.compile("^\\s*Message MSGCFG_((?:AD|CH|DE)\\w+)");
    private static final Pattern regAuthRequest = Pattern.compile("^\\s*Message (MSGCFG_\\w+).+from (\\d+) \\((.+) '(.+)'\\)$");
    private static final Pattern regAuthResponse = Pattern.compile("^\\s*Message (MSGCFG_\\w+).+to (\\d+) \\((.+) '(.+)'\\)$");
    private static final Pattern regChangeFinal = Pattern.compile(" Object: \\[([^\\]]+)\\], name \\[([^\\]]+)\\], DBID: \\[(\\d+)\\] is ([^\\]]+) by client, type \\[([^\\]]+)\\], name: \\[([^\\]]+)\\], user: \\[([^\\]]+)\\]");
    private static final Pattern regChangeProxy = Pattern.compile(" Object \\[(\\w+)\\], name \\[([^\\]]*)\\], DBID: \\[(\\d+)\\] is ([^\\]]+) at server");
    private static final Pattern regConfigToClients = Pattern.compile("^\\s*There are \\[(\\d+)\\] objects of type \\[([^\\]]+)\\] sent to the client \\[([^\\]]+)\\] \\(application \\[([^\\]]+)\\], type \\[([^\\]]+)\\]\\)");
    private static final Pattern regNewClient = Pattern.compile("^\\s*New client (\\d+) connected");
    private static final Pattern regChangeEnd = Pattern.compile("^\\s*Trc 2421. Object \\[([^\\]]+)\\](?:, DBID \\[(\\d+)\\])? is to be ([^\\]]+) by client, type \\[([^\\]]+)\\], name: \\[([^\\]]+)\\], user: \\[([^\\]]+)\\]");
    private static final Pattern regAuthStart = Pattern.compile("^\\s*(MSGCFG_.+)*?$");
    private static final Pattern regAuthCont = Pattern.compile("^\\s*attr:");
    private static final Pattern regNewClientAuth = Pattern.compile("^\\s*Extended info : Client (\\d+) authorized, name \\[([^\\]]+)\\], type \\[([^\\]]+)\\], user name \\[([^\\]]+)\\], protocol \\[([^\\]]+)\\], address \\[([^\\]]+)\\]");
    private static final Pattern regClientDisconn = Pattern.compile("^\\s*Extended info : Client \\[([^\\]]+)\\] disconnected, application \\[([^\\]]+)\\], type \\[([^\\]]+)\\]");
    private static final Pattern regClientDisconn1 = Pattern.compile("^\\s*Client '(\\w+)' disconnected");
    private static final Pattern regNotParseMessage = Pattern.compile("^("
            + "04541|"
            + "04524|"
            + "24215|"
            + "24308|"
            + "24200|"
            + "04542"
            + ")");
    long m_CurrentFilePos;
    // parse state contants

    long m_HeaderOffset;
    String m_ServerName;
    private ParserState m_ParserState;
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
            Main.logger.error(e);
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

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

                m_lineStarted = m_CurrentLine;

                s = ParseGenesysLocal(str);

                String lastGenesysMsgID = getLastGenesysMsgID();
                String lastGenesysMsgLevel = getLastGenesysMsgLevel();

                if (lastGenesysMsgID != null) {
                    if (lastGenesysMsgID.equals("04541")) {
                        if ((m = regConfigMessage.matcher(s)).find()) {
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                            msgTmp = new CSClientMessage(m.group(1), true,
                                    m.group(3), m.group(4), m.group(5));
                            m_ParserState = ParserState.STATE_CLIENT_REQUEST;
                            return null;
                        }

                        if ((regChangeRequest.matcher(s)).find()) {
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                            m_ParserState = ParserState.STATE_UPDATE;
                            return null;
                        } else if ((m = regAuthRequest.matcher(s)).find()) {
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                            m_ParserState = ParserState.STATE_AUTH1;
                            msgTmp = new CSClientMessage(m.group(1), m.group(2),
                                    m.group(3), m.group(4));
                            return null;
                        }
                    } else if (lastGenesysMsgID.equals("04542")) {
                        if ((m = regConfigMessage.matcher(s)).find()) {
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                            msgTmp = new CSClientMessage(m.group(1), false,
                                    m.group(3), m.group(4), m.group(5));
                            m_ParserState = ParserState.STATE_CLIENT_RESPONSE;
                            return null;
                        }
                        if (s.startsWith("Message MSGCFG_OBJECTCHANGED2 (")) {
                            return null;
                        }

                        if ((m = regAuthResponse.matcher(s)).find()) {
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                            m_ParserState = ParserState.STATE_AUTH1;
                            msgTmp = new CSClientMessage(m.group(1), m.group(2),
                                    m.group(3), m.group(4));
                            return null;
                        }
                    } else if (lastGenesysMsgID.equals("24308")) {
                        m_ParserState = ParserState.STATE_IGNORE_MESSAGE;
                        return null;
                    } else if (lastGenesysMsgID.startsWith("2420")) {// config object update
                        if ((m = regChangeFinal.matcher(s)).find()) {
                            CSObjectChange msg = new CSObjectChange(m_MessageContents);
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
                        } else if ((m = regChangeProxy.matcher(s)).find()) {
                            CSObjectChange msg = new CSObjectChange();
                            msg.setObjType(m.group(1));
                            msg.setDBID(m.group(3));
                            msg.setName(m.group(2));

                            msg.setOp(m.group(4));
                            SetStdFieldsAndAdd(msg);
                            m_ParserState = ParserState.STATE_COMMENTS;
                            return null;
                        }


                    } else if (lastGenesysMsgID.startsWith("2421")) {
                        if ((m = regConfigToClients.matcher(s)).find()) {
                            CSClientMessage csClientRequest1 = new CSClientMessage("OBJECTS_SENT", false,
                                    m.group(3), m.group(5), m.group(4));
                            csClientRequest1.setObjType(m.group(2));
                            csClientRequest1.setObjNumber(m.group(1));
                            SetStdFieldsAndAdd(csClientRequest1);
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

                GenesysMsg.CheckGenesysMsg(dp, this, TableType.MsgConfServer, (Pattern) null);

                break;
//</editor-fold>

            case STATE_CLIENT_REQUEST:
                if (StringUtils.isEmpty(str) ||
                        regStartsWithSpace.matcher(str).find()) {
                    m_MessageContents.add(str);
                } else {
                    msgTmp.setMessageLines(m_MessageContents);
                    SetStdFieldsAndAdd(msgTmp);
                    msgTmp = null;
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;

            case STATE_IGNORE_MESSAGE:
                if (!StringUtils.isEmpty(str) &&
                        !regStartsWithSpace.matcher(str).find()) {
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;


            case STATE_CLIENT_RESPONSE:
                if (StringUtils.isEmpty(str) ||
                        regStartsWithSpace.matcher(str).find()) {
                    m_MessageContents.add(str);
                } else {
                    msgTmp.setMessageLines(m_MessageContents);
                    SetStdFieldsAndAdd(msgTmp);
                    m_MessageContents.clear();
                    msgTmp = null;
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;

            case STATE_AUTH1:
                if ((regAuthStart.matcher(s)).find()) {
                    m_MessageContents.add(str);
                } else {
                    m_MessageContents.add(str);
                    m_ParserState = ParserState.STATE_AUTH2;
                }
                break;

            case STATE_AUTH2:
                if ((regAuthCont.matcher(s)).find()) {
                    m_MessageContents.add(str);
                } else {
                    m_MessageContents.add(str);
                    msgTmp.setMessageLines(m_MessageContents);
                    SetStdFieldsAndAdd(msgTmp);
                    msgTmp = null;
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;

            case STATE_UPDATE: {
                s = ParseGenesysLocal(str);
                if ((m = regChangeEnd.matcher(s)).find()) {
                    m_MessageContents.add(str);
                    CSClientMessage msg = new CSClientMessage(m_MessageContents);
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
                    CSClientMessage msg = new CSClientMessage(m_MessageContents);
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
                    CSClientMessage msg = new CSClientMessage(m_MessageContents);
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
                    CSClientMessage msg = new CSClientMessage(m_MessageContents);

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
        m_tables.put(TableType.CSUpdate, new CSClientMessageTable(Main.getInstance().getM_accessor(), TableType.CSUpdate));
        m_tables.put(TableType.CSConfigChange, new CSObjectChangeTable(Main.getInstance().getM_accessor(), TableType.CSConfigChange));
        m_tables.put(TableType.CSClientConnect, new CSClientConnectTable(Main.getInstance().getM_accessor(), TableType.CSClientConnect));

    }

    enum ParserState {

        STATE_HEADER,
        STATE_COMMENTS,
        STATE_UPDATE,
        STATE_AUTH1,
        STATE_AUTH2,
        STATE_CLIENT_REQUEST,
        STATE_CLIENT_RESPONSE,
        STATE_IGNORE_MESSAGE,
        ;
    }

    private class CSObjectChange extends Message {

        private Integer descriptor;
        private String ObjType;
        private String op;
        private String clientType;
        private String appName;
        private String userName;
        private Integer dbid = null;
        private String objName = null;
        private Integer objNumber;

        public Integer getDescriptor() {
            return descriptor;
        }

        private CSObjectChange(){
            super(TableType.CSConfigChange,  fileInfo.getRecordID());
        }
        private CSObjectChange(String req, String descr, String appType, String appName) {
           this();
            this.op = req;
            this.clientType = appType;
            this.appName = appName;
        }

        private CSObjectChange(String req, boolean isRequest,
                               String descriptor, String appType, String appName) {
            this();
            setM_isInbound(isRequest);
            this.op = req;
            this.descriptor = Integer.parseInt(descriptor);
            this.clientType = appType;
            this.appName = appName;
        }

        private CSObjectChange(ArrayList<String> m_MessageContents)
        {
            this();
            setMessageLines(m_MessageContents);
        }

        public String getObjType() {
            if (ObjType != null && !ObjType.isEmpty()) {
                return ObjType;
            } else {
                return cfgGetAttrObjType("IATRCFG_OBJECTTYPE");
            }
        }

        private void setObjType(String group) {
            this.ObjType = group;
        }

        public String getOp() {
            if (op != null && !op.isEmpty()) {
                return op;
            } else {
                return FindByRx(ptOp, 1, null);

            }
        }

        private void setOp(String group) {
            this.op = group;
        }

        public String getClientType() {
            if (StringUtils.isEmpty(clientType)) {
                return cfgGetAttr("IATRCFG_APPTYPE");
            }
            return clientType;

        }

        private void setClientType(String group) {
            this.clientType = group;
        }

        public String getAppName() {
            if (StringUtils.isEmpty(appName)) {
                return cfgGetAttr("SATRCFG_APPNAME");
            }
            return appName;

        }

        private void setAppName(String group) {
            this.appName = group;
        }

        public String getUserName() {
            if (userName == null || userName.isEmpty()) {
                return cfgGetAttr("SATRCFG_USERNAME");
            }
            return userName;
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


        private Integer getRefID() {
            return intOrDef(cfgGetAttr("IATRCFG_REQUESTID"), (Integer) null);
        }

        private String getErrMessage() {
            return cfgGetAttr("SATRCFG_DESCRIPTION");

        }

        public void setObjNumber(String objNumber) {
            this.objNumber = Integer.parseInt(objNumber);
        }

        public Integer getObjNumber() {
            return objNumber;
        }
    }

    private class CSObjectChangeTable extends DBTable {

        public CSObjectChangeTable(SqliteAccessor dbaccessor, TableType t) {
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
        public void AddToDB(Record _rec) throws SQLException {
            CSObjectChange rec = (CSObjectChange) _rec;

            getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{

                    stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileID());
                stmt.setLong(3, rec.getM_fileOffset());
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.getM_line());

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.App, StripQuotes(rec.getAppName())));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.ObjectType, rec.getObjType()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.AppType, rec.getClientType()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.Agent, StripQuotes(rec.getUserName())));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.CfgOp, rec.getOp()));
                setFieldInt(stmt, 11, rec.getDbid());
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.CfgObjName, rec.getObjName()));

                }
            });
        }

    }


    private class CSClientMessage extends Message {

        private Integer descriptor;
        private String ObjType;
        private String op;
        private String clientType;
        private String appName;
        private String userName;
        private Integer dbid = null;
        private String objName = null;
        private Integer objNumber;

        public Integer getDescriptor() {
            return descriptor;
        }

        private CSClientMessage(String req, String descr, String appType, String appName) {
            super(TableType.CSUpdate,  fileInfo.getRecordID());
            this.op = req;
            this.clientType = appType;
            this.appName = appName;
        }

        private CSClientMessage(String req, boolean isRequest,
                                String descriptor, String appType, String appName) {
            super(TableType.CSUpdate,  fileInfo.getRecordID());
            setM_isInbound(isRequest);
            this.op = req;
            this.descriptor = Integer.parseInt(descriptor);
            this.clientType = appType;
            this.appName = appName;
        }

        private CSClientMessage(ArrayList<String> m_MessageContents) {
            super(TableType.CSUpdate, m_MessageContents,  fileInfo.getRecordID());
        }

        public String getObjType() {
            if (ObjType != null && !ObjType.isEmpty()) {
                return ObjType;
            } else {
                return cfgGetAttrObjType("IATRCFG_OBJECTTYPE");
            }
        }

        private void setObjType(String group) {
            this.ObjType = group;
        }

        public String getOp() {
            if (op != null && !op.isEmpty()) {
                return op;
            } else {
                return FindByRx(ptOp, 1, null);

            }
        }

        private void setOp(String group) {
            this.op = group;
        }

        public String getClientType() {
            if (StringUtils.isEmpty(clientType)) {
                return cfgGetAttr("IATRCFG_APPTYPE");
            }
            return clientType;

        }

        private void setClientType(String group) {
            this.clientType = group;
        }

        public String getAppName() {
            if (StringUtils.isEmpty(appName)) {
                return cfgGetAttr("SATRCFG_APPNAME");
            }
            return appName;

        }

        private void setAppName(String group) {
            this.appName = group;
        }

        public String getUserName() {
            if (userName == null || userName.isEmpty()) {
                return cfgGetAttr("SATRCFG_USERNAME");
            }
            return userName;
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


        private Integer getRefID() {
            return intOrDef(cfgGetAttr("IATRCFG_REQUESTID"), (Integer) null);
        }

        private String getErrMessage() {
            return cfgGetAttr("SATRCFG_DESCRIPTION");

        }

        public void setObjNumber(String objNumber) {
            this.objNumber = Integer.parseInt(objNumber);
        }

        public Integer getObjNumber() {
            return objNumber;
        }
    }

    private class CSClientMessageTable extends DBTable {

        public CSClientMessageTable(SqliteAccessor dbaccessor, TableType t) {
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
            addIndex("descr");


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
                    + ",descr INTEGER"
                    + ",objNumber INTEGER"
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
        public void AddToDB(Record _rec) throws SQLException {
            CSClientMessage rec = (CSClientMessage) _rec;

            if (rec.getOp().equals("MSGCFG_OBJECTCHANGED2")) {
                return;
            }

            getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileID());
                stmt.setLong(3, rec.getM_fileOffset());
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.getM_line());

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.App, StripQuotes(rec.getAppName())));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.ObjectType, rec.getObjType()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.AppType, rec.getClientType()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.Agent, StripQuotes(rec.getUserName())));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.CfgOp, rec.getOp()));
                setFieldInt(stmt, 11, rec.getDbid());
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.CfgObjName, rec.getObjName()));
                setFieldInt(stmt, 13, rec.getRefID());
                setFieldInt(stmt, 14, Main.getRef(ReferenceType.CfgMsg, rec.getErrMessage()));
                setFieldInt(stmt, 15, rec.getDescriptor());
                setFieldInt(stmt, 16, rec.getObjNumber());

                }
            });
        }


    }


    private class CSClientConnect extends Message {

        private String clientType;
        private String appName;
        private boolean connect;
        private String userName;
        private String clientAddr;
        private Integer socket;

        private CSClientConnect() {
            super(TableType.CSClientConnect,  fileInfo.getRecordID());
        }

        public String getClientType() {
            return clientType;
        }

        private void setClientType(String group) {
            this.clientType = group;
        }

        public String getAppName() {
            return appName;
        }

        private void setAppName(String group) {
            this.appName = group;
        }

        private void setClientAddress(String group) {
            this.clientAddr = group;
        }

        public boolean isConnect() {
            return connect;
        }

        private void setConnect(boolean b) {
            this.connect = b;
        }

        public String getUserName() {
            return userName;
        }

        private void setUserName(String group) {
            this.userName = group;
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

        public CSClientConnectTable(SqliteAccessor dbaccessor, TableType t) {
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
         * @throws Exception
         */
        @Override
        public void FinalizeDB() throws Exception {
            createIndexes();
        }

        @Override
        public void AddToDB(Record _rec) throws SQLException {
            CSClientConnect rec = (CSClientConnect) _rec;
            getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileID());
                stmt.setLong(3, rec.getM_fileOffset());
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.getM_line());

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.App, rec.getAppName()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.AppType, rec.getClientType()));
                setFieldInt(stmt, 8, rec.getSocket());
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.Agent, StripQuotes(rec.getUserName())));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.Host, rec.getClientAddr()));
                stmt.setBoolean(11, rec.isConnect());

                }
            });
        }


    }

}
