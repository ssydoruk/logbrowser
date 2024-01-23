/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ssydoruk
 */
public class GMSParser extends Parser {

    public static final int MAX_WEB_PARAMS = 20;
    private static final org.apache.logging.log4j.Logger logger = Main.logger;
    //2 types of thread messages in ORS logs
    private static final Pattern regWThread = Pattern.compile("^\\s*\\[([^\\]]+)[\\]\\s]*");
    private static final Pattern regExceptionStart = Pattern.compile("^([\\w\\.]+Exception[\\w\\.]*):(.*)");
    private static final Pattern regNullExceptionStart = Pattern.compile("^([\\w\\.]+Exception[\\w\\.]*)");
    private static final Pattern regAttemptStartSession = Pattern.compile("Attempting to start session");
    private static final Pattern regPOST = Pattern.compile("^\\(POST\\) Client IP Address");
    private static final Pattern regKVList = Pattern.compile("^\\s+'");
    private static final Pattern regPostJSON = Pattern.compile("JSon: KVList:\\s*$");

    private static final Pattern regORSResponse = Pattern.compile("^OrsService: response from ORS (.+)$");

    private static final Pattern regExceptionContinue = Pattern.compile("^(\\s|Caused by:)");
    private static final Pattern regIxnMsgContinue = Pattern.compile("^(\\s|attr_)");
    private static final Pattern regNotParseMessage = Pattern.compile(
            //07:47:39.182 Trc 09900  [qtp747464370-17] >>> /1/service/callback/RCC_CORK_CallBack - CCID [null]
            "^(09900"
                    + ")");
    private static final Pattern regStatisticMessage = Pattern.compile(
            "^StatisticService: ");
    private static final Pattern regOpenMediaInteraction = Pattern.compile(
            "^CreateOpenMediaInteractionController webRequestId \\[(\\d+)\\], submitInteraction (request received|response) for (\\S+)\\s+");
    private static final Pattern regSendIxnServerMsg = Pattern.compile(
            "^CreateOpenMediaInteractionTask.+(to|from) Interaction Server: '([^']+)'");
    //<editor-fold defaultstate="collapsed" desc="GMSStatServerMsg">
    private static final HashMap<String, Integer> keysPos = new HashMap<>(MAX_WEB_PARAMS);

    private final StartSessionTransition startSessionTransition = new StartSessionTransition();
    private final HashMap<String, String> ThreadAlias = new HashMap<>();
    private final ParserThreadsProcessor ptpsThreadProcessor;
    Message msg = null;
    HashMap<String, GMSORSMessage> threadReqs = new HashMap<>();
    private long m_CurrentFilePos;
    private int m_TotalLen;
    private String m_msgName;

    private long m_HeaderOffset;
    private ParserState m_ParserState;
    private String newThread; // current thread name as parsed fom the line
    private String m_msg1;
    private String savedThreadID;

    GMSParser(DBTables m_tables) {
        super(FileInfoType.type_GMS, m_tables);
        m_MessageContents = new ArrayList<>();
        ThreadAlias.put("ORSInternal", "FMWeb");

        ptpsThreadProcessor = new ParserThreadsProcessor();
        //<editor-fold defaultstate="collapsed" desc="processing of fetch per thread">
        ptpsThreadProcessor.addStateTransition(new StartSessionTransition());
//</editor-fold>

    }

    private void processStatisticNotification(String substring) {
    }

    private void processOpenMediaInteraction(String webReqID, boolean isResponse, String URL, String rest) {
        GMSWebClientMsg gmsWebClientMsg = new GMSWebClientMsg(webReqID, isResponse, URL, rest);
        gmsWebClientMsg.SetStdFieldsAndAdd(this);
    }


    private String getThreadAlias(String thr) {
        String ret = ThreadAlias.get(thr);
        return (ret != null) ? ret : thr;
    }

    private String getThread(String s) {
        if (s != null) {
            Matcher m;
            if ((m = regWThread.matcher(s)).find()) {
                newThread = getThreadAlias(m.group(1));
                return s.substring(m.end());
            }
        }
        newThread = null;
        return s;
    }

    private ParserState getParserStateByThread()  {
        return m_ParserState;

    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentFilePos = offset;
        m_CurrentLine = line;


        try {
            input.skip(offset);

            m_ParserState = ParserState.STATE_HEADER;
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            String str;
            while ((str = input.readLine()) != null) {
                m_CurrentLine++;
                Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");

                try {
                    int l = input.getLastBytesConsumed();
                    setEndFilePos(getFilePos() + l); // to calculate file bytes

                    while (str != null) {
                        str = ParseLine(str, input);
                    }
                    setFilePos(getFilePos() + l);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                }

            }
        } catch (IOException e) {
            Main.logger.error(e);
        }

        return m_CurrentLine - line;
    }

    private String ParseLine(String str, BufferedReaderCrLf in) throws Exception {
        Matcher m;
        String s = str;
        boolean parsedGenesys = false;

        ParseCustom(str, 0);

        if (ptpsThreadProcessor.threadPending()) {
            if (!ptpsThreadProcessor.isThreadWaitsForNonthreadMessage()) {
                s = ParseGenesysGMS(str, TableType.MsgGMS, regNotParseMessage);
                s = getThread(s);
                parsedGenesys = true;
            }
            if (ptpsThreadProcessor.threadConsume(str, s, newThread, this)) {
                Main.logger.trace("threadConsumed");
                return null;
            }
        }
        Main.logger.trace("main thread state: " + m_ParserState + " thread: [" + newThread + "] s[" + s + "]");

        switch (getParserStateByThread()) {
            case STATE_HEADER: {
                if (str.startsWith("COMPONENTS:")) {
                    m_ParserState = ParserState.STATE_COMMENT;
                }
            }
            break;

            case STATE_COMMENT:
                if (str.isEmpty()) {
                    return null;
                }
                m_lineStarted = m_CurrentLine;
                if (!parsedGenesys) {
                    s = ParseGenesysGMS(str, TableType.MsgGMS, regNotParseMessage);
                    s = getThread(s);
                }
                Main.logger.trace("latest thread ID: [" + (newThread == null ? "(null)" : newThread) + "] s[" + s + "]");

                if ((m = regExceptionStart.matcher(str)).find() || (m = regNullExceptionStart.matcher(s)).find()) {
                    m_msgName = m.group(1);
                    if (m.groupCount() > 1) {
                        m_msg1 = m.group(2);
                    } else {
                        m_msg1 = m.group(1);
                    }
                    m_MessageContents.add(s);
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_EXCEPTION;
                    setSavedFilePos(getFilePos());
                    break;
                } else if ((regPOST.matcher(s)).find()) {
                    m_HeaderOffset = m_CurrentFilePos;
                    m_MessageContents.add(s);
                    m_TotalLen = str.length() + in.charsSkippedOnReadLine;
                    m_ParserState = ParserState.STATE_POST;
                    setSavedFilePos(getFilePos());
                } else if (regAttemptStartSession.matcher(s).find()) {
                    ParserThreadState tps = new ParserThreadState(
                            startSessionTransition,
                            ParserThreadsProcessor.ParserState.STATE_START_SESSION);
                    tps.setFilePos(getFilePos());
                    tps.addString(s);
                    tps.setHeaderOffset(m_CurrentFilePos);
                    ptpsThreadProcessor.addThreadState(newThread, tps);
                    return null;

                } else if (s.startsWith("OrsService: submit to ORS")) {
                    addORSRequest(newThread, dp);
                } else if ((m = regORSResponse.matcher(s)).find()) {
                    String resp = m.group(1);
                    Main.logger.trace("ORS response " + resp);
                    this.savedThreadID = newThread;
                    if (resp.startsWith("KVList")) {
                        m_HeaderOffset = m_CurrentFilePos;
                        m_MessageContents.add(s);
                        m_TotalLen = str.length() + in.charsSkippedOnReadLine;
                        m_ParserState = ParserState.STATE_ORS_RESPONSE;
                        setSavedFilePos(getFilePos());
                    } else {
                        addORSResponseMessage(resp);
                    }

                } else if (s.startsWith("OrsServicesendORSRequest")) {
                    m_HeaderOffset = m_CurrentFilePos;
                    m_MessageContents.add(s);
                    m_TotalLen = str.length() + in.charsSkippedOnReadLine;
                    m_ParserState = ParserState.STATE_ORS_RESPONSE_EXCEPTION;
                    this.savedThreadID = newThread;
                    setSavedFilePos(getFilePos());
                } else if ((m = regStatisticMessage.matcher(s)).find()) {
                    processStatisticNotification(s.substring(m.end()));
                } else if ((m = regOpenMediaInteraction.matcher(s)).find()) {
                    processOpenMediaInteraction(m.group(1), m.group(2).startsWith("resp"), m.group(3), s.substring(m.end()));
                } else if (s.contains("[SESSION]") || s.startsWith("WebBasicAuthenticationFilter:")
                        || s.contains("Client IP Address: ") || s.startsWith("WebAPIApplicationSettings")
                        || s.contains("Storage, getSession, session ")
                        || s.startsWith("ConfigurationService: ") || s.startsWith("Config Task Process ")
                        || s.startsWith("Context Services: ") || s.startsWith("WebAPILoadBalancerImpl") || s.startsWith("MediaServerProtocolFactory ")
                        || s.startsWith("Config Task Remove Agent")) {
                    // ignore these

                } else if ((m = regSendIxnServerMsg.matcher(s)).find()) {
                    m_HeaderOffset = m_CurrentFilePos;
                    m_MessageContents.clear();
                    m_TotalLen = str.length() + in.charsSkippedOnReadLine;
                    m_ParserState = ParserState.STATE_IXNMESSAGE;
                    msg = new IxnGMS(m.group(1), m.group(2), fileInfo.getRecordID());
                    setSavedFilePos(getFilePos());

                } else {
                    GenesysMsg lastLogMsg = getLastLogMsg();
                    if (lastLogMsg != null) {
                        if (!lastLogMsg.isSavedToDB()) {
                            addToDB(lastLogMsg);
                        }
                    } else {
                        logger.info("Not parsed l:" + m_CurrentLine + " " + str);
                    }
                }
                break;

            case STATE_EXCEPTION: {
                if (regExceptionContinue.matcher(str).find()) {
                    m_MessageContents.add(str.substring(1));
                } else {
                    addException();
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }
            }
            break;

            case STATE_IXNMESSAGE: {
                if (regIxnMsgContinue.matcher(str).find()) {
                    m_MessageContents.add(str);
                } else {
                    if (msg instanceof IxnGMS) {
                        ((IxnGMS) msg).setAttributes(m_MessageContents);
                        msg.SetStdFieldsAndAdd(this);
                    } else {
                        Main.logger.error("l:" + m_CurrentLine + " incorrect message type");
                    }
                    msg = null;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }

            }
            break;

            case STATE_TMESSAGE_ATTRIBUTES: {
                if (regKVList.matcher(str).find()) {
                    m_MessageContents.add(str);

                } else {
                    addPostMessage();
                    m_TotalLen = 0;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }

            }
            break;

            case STATE_POST:
                if (regPostJSON.matcher(str).find()) {
                    m_MessageContents.add(str.substring(1));
                } else if (regKVList.matcher(str).find()) {
                    m_TotalLen += str.length() + in.charsSkippedOnReadLine; // str as we cut date
                    m_MessageContents.add(str);
                } else {
                    addPostMessage();
                    m_TotalLen = 0;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }
                break;

            case STATE_ORS_RESPONSE:
                if (regKVList.matcher(str).find()) {
                    m_TotalLen += str.length() + in.charsSkippedOnReadLine; // str as we cut date
                    m_MessageContents.add(str);
                } else {
                    addORSResponseMessage(false);
                    m_TotalLen = 0;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }
                break;

            case STATE_ORS_RESPONSE_EXCEPTION:
                if (str.endsWith("Exception: ") || str.startsWith("\t") || str.startsWith("Caused by")) {
                    m_TotalLen += str.length() + in.charsSkippedOnReadLine; // str as we cut date
                    m_MessageContents.add(str);
                } else {
                    addORSResponseMessage(true);
                    m_TotalLen = 0;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }
                break;
        }

        return null;
    }

    private void addPostMessage() {
        GMSPostMessage _msg = new GMSPostMessage(m_MessageContents, fileInfo.getRecordID());
        SetStdFieldsAndAdd(_msg);
    }

    private void addException() {
        Main.logger.trace("addException");

        SetStdFieldsAndAdd(new ExceptionMessage(
                m_MessageContents,
                m_msgName,
                m_msg1, fileInfo.getRecordID()
        ));

    }

    private void addORSRequest(String threadID, DateParsed d) {
        Main.logger.trace("l:" + m_CurrentLine + " - threadID:[" + threadID + "] saving ORS request, [" + d.orig + "]");
//        if (d.rest.endsWith("/scxml/session/start")) {
//            Main.logger.trace("skipping for now");
//            return;
//        }
        if (threadReqs.containsKey(threadID)) {
            GMSORSMessage req = threadReqs.get(savedThreadID);
            Main.logger.error("l:" + m_CurrentLine + " - " + "new ORS request [" + d.orig + "] while there is opened ORS request [" + req + "] without prior response");
            try {
                if (req != null) {
                    addToDB(req);
                }
            } catch (Exception ex) {
                logger.error("fatal: ", ex);
            }
        }
        GMSORSMessage _msg = new GMSORSMessage(threadID, dp.rest, fileInfo.getRecordID());
        _msg.SetInbound(false);
        SetStdFields(_msg);
        threadReqs.put(threadID, _msg);
    }

    private void addORSResponseMessage(String resp) {
        GMSORSMessage msgResp = new GMSORSMessage(newThread, fileInfo.getRecordID());
        msgResp.SetInbound(true);
        SetStdFields(msgResp);
        if (isSessionID(resp)) {
            msgResp.setReq("OK");
            msgResp.setORSSessionID(resp);
        } else {
            msgResp.setReq(resp);
        }
        addORSResponseMessage(false, msgResp);
    }

    private void addORSResponseMessage(boolean isException) {
        GMSORSMessage msgResp = new GMSORSMessage(newThread, m_MessageContents, fileInfo.getRecordID());
        msgResp.SetInbound(true);
        SetStdFields(msgResp);
        msgResp.setReq("OK (KVList)");
        addORSResponseMessage(isException, msgResp);
    }

    private void addORSResponseMessage(boolean isException, GMSORSMessage msgResp) {
        Main.logger.trace("l:" + m_CurrentLine + " - threadID:[" + newThread + "] saving ORS response, [" + dp.orig + "]");
        GMSORSMessage msgReq = null;
        if (!threadReqs.containsKey(savedThreadID)) {
            Main.logger.error("l:" + m_CurrentLine + " - " + "response from ORS without prior request");
        } else {
            msgReq = threadReqs.get(savedThreadID);
        }

        if (msgReq != null) {
            msgResp.setORSSessionID(msgReq.getORSSessionID());

            if (!isException) {
                msgReq.setORSSessionID(msgResp.getORSSessionID());
                msgReq.setGMSSessionID(msgResp.getGMSSessionID());
                msgReq.setConnID(msgResp.getConnID());
                msgResp.adjustResp(msgReq.getORSReq());
            } else {
                msgResp.setException();
            }
        }

        try {
            if (msgReq != null) {
                addToDB(msgReq);
            }
            addToDB(msgResp);

        } catch (Exception ex) {
            logger.error("fatal: ", ex);
        }
        threadReqs.remove(savedThreadID);
        savedThreadID = null;
    }

    @Override
    void init(DBTables m_tables) {
        m_tables.put(TableType.GMSStart.toString(), new GMSStartTable(Main.getInstance().getM_accessor(), TableType.GMSStart));
        m_tables.put(TableType.GMSStatServerMessage.toString(), new GMSStatServerMsgTable(Main.getInstance().getM_accessor(), TableType.GMSStatServerMessage));
        m_tables.put(TableType.GMSWebClientMessage.toString(), new GMSWebClientMsgTable(Main.getInstance().getM_accessor(), TableType.GMSWebClientMessage));
        m_tables.put(TableType.IxnGMS.toString(), new IxnGMSTable(Main.getInstance().getM_accessor(), TableType.IxnGMS));

    }

    enum ParserState {
        STATE_HEADER,
        STATE_COMMENT,
        STATE_POST,
        STATE_TMESSAGE_ATTRIBUTES,
        STATE_EXCEPTION,
        STATE_ORS_RESPONSE, STATE_ORS_RESPONSE_EXCEPTION, STATE_IXNMESSAGE
    }


    private class StartSessionTransition implements ParserThreadsProcessor.StateTransition {

        private final Pattern regParams = Pattern.compile("^\\s*Params:");

        @Override
        public ParserThreadsProcessor.StateTransitionResult stateTransition(ParserThreadState threadState,
                                                                            String sOrig, String sParsedAndTruncated, String threadID, Parser parser) {

            switch (threadState.getParserState()) {
                case STATE_START_SESSION: {
                    if (regParams.matcher(sParsedAndTruncated).find()) {
                        threadState.setParserState(ParserThreadsProcessor.ParserState.STATE_START_SESSION1);
                        threadState.addString(sParsedAndTruncated);
                        return ParserThreadsProcessor.StateTransitionResult.NON_STATE_LINE_WAITED;
                    } else {
                        Main.logger.error("l:" + parser.getM_CurrentLine() + " - Unexpected thread message: " + sOrig);
                        return ParserThreadsProcessor.StateTransitionResult.ERROR_STATE;
                    }
                }

                case STATE_START_SESSION1:
                    if (regKVList.matcher(sParsedAndTruncated).find()) {
                        threadState.addString(sOrig);
                        return ParserThreadsProcessor.StateTransitionResult.NON_STATE_LINE_WAITED;
                    } else {
                        GMSStartMessage gmsStartMessage = new GMSStartMessage(threadState.getMsgLines(), fileInfo.getRecordID());
                        gmsStartMessage.SetStdFieldsAndAdd(parser);
                        return ParserThreadsProcessor.StateTransitionResult.FINAL_REACHED;

                    }

            }
            return ParserThreadsProcessor.StateTransitionResult.ERROR_STATE;
        }

    }

    private class GMSStatServerMsgTable extends DBTable {

        public GMSStatServerMsgTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("ssApp");
            addIndex("ssHost");
            addIndex("ssPort");
            addIndex("ssEvent");
            addIndex("reqID");
            addIndex("stringValue");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",ssApp int"
                    + ",ssHost int"
                    + ",ssPort int"
                    + ",ssEvent int"
                    + ",reqID int"
                    + ",stringValue int"
                    + ");";
            getM_dbAccessor().runQuery(query);

        }

        @Override
        public String getInsert() {
            return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ");";

        }

        /**
         * @throws Exception
         */
        @Override
        public void FinalizeDB() throws SQLException {
            createIndexes();
        }

    }
//</editor-fold> 

    private class GMSWebClientMsg extends Message {

        private String webReqID;
        private boolean response;
        private String URL;
        private List<Pair<String, String>> allParams;

        private GMSWebClientMsg(String webReqID, boolean response, String URL, String rest) {
            this();
            this.webReqID = webReqID;
            this.response = response;
            this.URL = URL;
            allParams = new ArrayList<>(MAX_WEB_PARAMS);
            int i = rest.indexOf('{');
            if (i >= 0) {
                String r1 = rest.substring(i);
                r1 = StringUtils.replaceChars(r1, "[]{}", null);

                String[] split = StringUtils.splitByWholeSeparator(r1, ", ");
                for (String string : split) {
                    String[] split1 = StringUtils.split(string, "=", 2);
                    if (split1 != null && split1.length > 1) {
                        Pair<String, String> pair = new Pair<>(split1[0], split1[1]);
                        allParams.set(getIndex(allParams, pair), pair);
                    }
                }

            }
        }

        private GMSWebClientMsg() {
            super(TableType.GMSWebClientMessage, fileInfo.getRecordID());
        }

        public Integer getWebReqID() {
            return getIntOrDef(webReqID, (Integer) null);
        }

        public boolean isResponse() {
            return response;
        }

        public String getURL() {
            return URL;
        }

        private List<Pair<String, String>> getAllParams() {
            return allParams;
        }

        private int getIndex(List<Pair<String, String>> allParams, Pair<String, String> pair) {
            Integer get = keysPos.get(pair.getKey());
            if (get != null) {
                return get;
            } else {
                int i = 0;
                for (; i < allParams.size(); i++) {
                    if (allParams.get(i) == null) {
                        keysPos.put(pair.getKey(), i);
                        return i;
                    }
                }
                i--; // not to get out of bounds
                keysPos.put(pair.getKey(), i);
                return i;

            }
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.HTTPURL, getURL()));
            stmt.setBoolean(7, isResponse());
            setFieldInt(stmt, 8, getWebReqID());

            List<Pair<String, String>> theParams = getAllParams();
            int baseRecNo = 9;
            for (int i = 0; i < MAX_WEB_PARAMS; i++) {
                Pair<String, String> get = null;
                String val = null;
                if (i < theParams.size()) {
                    get = theParams.get(i);
                    if (get != null) {
                        val = Utils.Util.StripQuotes(get.getValue());
                        if (StringUtils.equalsIgnoreCase(val, "null")) {
                            val = null;
                        }
                    }
                }

                if (val != null) {
                    setFieldInt(stmt, baseRecNo + i * 2, Main.getRef(ReferenceType.Misc, get.getKey()));
                    setFieldInt(stmt, baseRecNo + i * 2 + 1, Main.getRef(ReferenceType.Misc, val));
                } else {
                    setFieldInt(stmt, baseRecNo + i * 2, null);
                    setFieldInt(stmt, baseRecNo + i * 2 + 1, null);
                }

            }
            return true;
        }
    }

    private class GMSWebClientMsgTable extends DBTable {

        public GMSWebClientMsgTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("URLID");
            addIndex("webReqID");

            for (int i = 1; i <= MAX_WEB_PARAMS; i++) {
                addIndex("param" + i + "id");
                addIndex("value" + i + "id");
            }

            dropIndexes();

            StringBuilder buf = new StringBuilder();
            for (int i = 1; i <= MAX_WEB_PARAMS; i++) {
                buf.append(",param").append(i).append("id int");
                buf.append(",value").append(i).append("id int");
            }

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",URLID int"
                    + ",isResponse boolean"
                    + ",webReqID int"
                    + buf
                    + ");";
            getM_dbAccessor().runQuery(query);


        }

        @Override
        public String getInsert() {
            StringBuilder buf = new StringBuilder();
            for (int i = 1; i <= MAX_WEB_PARAMS; i++) {
                buf.append(",?,?");
            }

            return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
                    + ",?"
                    + ",?"
                    + ",?"
                    + buf
                    + ");"
                    ;
        }

        /**
         * @throws Exception
         */
        @Override
        public void FinalizeDB() throws SQLException {
            createIndexes();
        }


    }
//</editor-fold> 

}
