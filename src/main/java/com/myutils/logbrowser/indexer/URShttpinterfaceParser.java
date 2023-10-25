/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Util;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

/**
 * @author ssydoruk
 */
public class URShttpinterfaceParser extends WebParser {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;

    private static final Pattern regReqReceived = Pattern.compile("^\\[HTTP Server (\\w+)\\] Received (\\d+) bytes from client on socket (\\d+):");
    private static final Pattern regRespSent = Pattern.compile("^\\[HTTP Server (\\w+)\\] Response");

    private static final Pattern regRequestToHandler = Pattern.compile("^\\[HTTP Handler Factory (\\w+)\\] Handler (\\w+) created");
    private static final Pattern regRequestToURS = Pattern.compile("^\\[URS Proxy (?:\\w+)] Router API called. Ref (\\w+)");
    private static final Pattern regResponseFromURS = Pattern.compile("^\\[HTTP Handler (\\w+)\\] Received event 2 from Router. Ref (\\d+)");
    private static final Pattern HTTPHeaderFound = Pattern.compile("^((\\w[\\w_\\-\\.\\s]+:)|\\s+;|HTTP)");
    private static final Pattern HTTPHeaderContinue = Pattern.compile("^\\s*(\\w.+)");
    private static final Pattern HTTPRequestContinue = Pattern.compile("^\\[(HTTP Request|HTTP Server)");
    private static final Pattern HTTPRequestHandler = Pattern.compile("^\\[HTTP Handler (?:(\\w+)\\] Created)?");
    private static final Pattern HTTPResponseContinue = Pattern.compile("^\\[(HTTP Response|HTTP Server)");
    private static final Pattern HTTPResponseHandler = Pattern.compile("^\\[HTTP (?:Handler|Request) (\\w+)\\] Destroyed");
    //<editor-fold defaultstate="collapsed" desc="ursHTTPMsg">
    private static final Pattern regHTTPURL = Pattern.compile("^(GET|HEAD|POST|PUT|DELETE|CONNECT|OPTIONS|TRACE|PATCH) (.+) HTTP/");
    private static final Pattern regHTTPResp = Pattern.compile("^HTTP/.\\.. (.+)");
    private static final Pattern regSIDCookie = Pattern.compile("ORSSESSIONID=([\\w~]+)");
    private static final Pattern regHTTPMethod = Pattern.compile("^(\\w+)");
    private static final Pattern regSessionID = Pattern.compile("session/([^\\/]+)");
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
    private final RequestHandler requestHandler = new RequestHandler();
    private final HashMap<Integer, ursHTTPMsg> partHTTP = new HashMap<>();
    private ParserState m_ParserState;
    private String URL = null;
    private String app = null;
    private String m_msg1;
    private boolean skipNextLine;
    private boolean skipMessage;
    private Message msg;
    private BufferedReaderCrLf m_input;
    private boolean m_isContentLengthUnknown;
    private Integer m_ContentLength;
    private ursHTTPMsg ursHTTP;
    private int m_PacketLength;
    private String thread; // current thread name as parsed fom the line

    URShttpinterfaceParser(DBTables m_tables) {
        super(FileInfoType.type_URSHTTP, m_tables);
        m_MessageContents = new ArrayList();
        ThreadAlias.put("ORSInternal", "FMWeb");
    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentLine = line;
        skipNextLine = false;
        skipMessage = false;
        m_input = input;


        URL = null;
        app = null;

        try {
            input.skip(offset);

            m_ParserState = ParserState.STATE_HEADER;
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            String str;
            while ((str = input.readLine()) != null) {
                m_CurrentLine++;
                Main.logger.trace("l: " + m_CurrentLine + " st:" + m_ParserState + " [" + str + "]");

                try {
                    SetBytesConsumed(input.getLastBytesConsumed());
                    setEndFilePos(getFilePos() + getBytesConsumed()); // to calculate file bytes

                    Object o = str;
                    while (o != null) {
                        o = ParseLine(o, input);
                    }
                    setFilePos(getEndFilePos());

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                }

//                m_CurrentFilePos += str.length()+input.charsSkippedOnReadLine;
            }
//            ParseLine("", null); // to complete the parsing of the last line/last message
        } catch (Exception e) {
            Main.logger.error(e);
        }


        return m_CurrentLine - line;
    }

    private void saveRequestToURS(Long handlerID, Long ursRefID, boolean isInbound) {
        Long ursReqID = null;
        if (handlerID != null) {
            ursReqID = requestHandler.getURSReqID(handlerID);
        }
        HTTPtoURS msg = new HTTPtoURS(ursRefID, ursReqID, isInbound, fileInfo.getRecordID());
        SetStdFieldsAndAdd(msg);
        requestHandler.remove(ursReqID);

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

            case STATE_HEADER: {
                if (StringUtils.isBlank(str)) {
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

                if (s != null) {
                    if ((m = regReqReceived.matcher(s)).find()) {
                        int socket = Integer.parseInt(m.group(3));
                        int bytes = Integer.parseInt(m.group(2));
                        String httpServerID = m.group(1);

                        m_MessageContents.clear();
                        ReadHTTP(bytes);

                        ursHTTPMsg _ursHTTP = new ursHTTPMsg(m_MessageContents, fileInfo.getRecordID());
                        _ursHTTP.setHTTPServerID(httpServerID);
                        _ursHTTP.setSocket(socket);
                        _ursHTTP.setBytes(bytes);
                        _ursHTTP.SetInbound(true);
                        SavePos(_ursHTTP);
                        SaveTime(_ursHTTP);

                        _ursHTTP = getFullHTTP(_ursHTTP);

                        if (_ursHTTP != null) {
//                            if (thread == null) {
//                                thread = "HTTP" + _ursHTTP.getSocket();
//                            } else {
//                                Main.logger.debug("thread supposed to be empty but is [" + thread + "]");
//                            }
//                    if (orsHTTP.SessionStart()) {
//                        SetParserThreadState("FMWeb", ParserState.STATE_HTTPIN);
//                        m_ParserState = ParserState.STATE_HTTPIN;
//                        recInProgress = orsHTTP;

                            ursHTTP = _ursHTTP;
                            m_ParserState = ParserState.STATE_WAITING_HANDLER;

                            return null;
                        }
                    } else if ((m = regRespSent.matcher(s)).find()) {
                        ursHTTP = new ursHTTPMsg(fileInfo.getRecordID());
                        ursHTTP.setHTTPServerID(m.group(1));
                        m_PacketLength = 0;
                        m_ContentLength = null;
                        m_MessageContents.clear();
                        setSavedFilePos(getFilePos());
                        m_isContentLengthUnknown = false;
                        m_ContentLength = null;
                        m_ParserState = ParserState.STATE_HTTP_HEADER;

                    } else if ((m = regRequestToHandler.matcher(s)).find()) {
                        requestHandler.store(intOrDef(m.group(1), null, 16), intOrDef(m.group(2), null, 16));
                    } else if ((m = regRequestToURS.matcher(s)).find()) {
                        saveRequestToURS(null, intOrDef(m.group(1), null, 10), false);
                    } else if ((m = regResponseFromURS.matcher(s)).find()) {
                        saveRequestToURS(intOrDef(m.group(1), null, 16), intOrDef(m.group(2), null, 10), true);
                    }
                }
                break;

            case STATE_WAITING_HANDLER: {
                if (StringUtils.isNotBlank(s)) {
                    s = ParseTimestampStr1(str);
                    dateParsed = true;
                    if (HTTPRequestContinue.matcher(s).find()) {
                        return null;
                    } else if ((m = HTTPRequestHandler.matcher(s)).find()) {
                        ursHTTP.setHTTPHandlerID(m.group(1));
                        SetStdFieldsAndAdd(ursHTTP);
                        m_MessageContents.clear();
                        m_ParserState = ParserState.STATE_COMMENT;
                    } else {
                        pError(logger, "Unexpected line in " + m_ParserState + " " + s);
                        m_ParserState = ParserState.STATE_COMMENT;

                        return getDP();
                    }
                }

                break;
            }

            //<editor-fold defaultstate="collapsed" desc="state_SIP_HEADER">
            case STATE_HTTP_HEADER: {
                if (HTTPHeaderFound.matcher(str).find()) {
                    m_MessageContents.add(str);
                    String contentL = Message.GetSIPHeader(str, "content-length", "l");
                    if (contentL != null) {
                        if (contentL.startsWith("[")) {
                            m_isContentLengthUnknown = true;
                        } else {
                            m_ContentLength = Integer.parseInt(contentL);
                        }
                    }
                } else {
                    ursHTTP.setMessageLines(m_MessageContents);
                    m_ParserState = ParserState.STATE_RESPONSE_WAITING_HANDLER;
                    if ((m_ContentLength != null && m_ContentLength > 0) || m_isContentLengthUnknown) {
                        m_MessageContents.add(s);
//                        m_ParserState = ParserState.STATE_HTTP_BODY;
                        ReadHTTP(m_ContentLength + 1);
                    } else {
                        return str;
                    }
                }

                break;
            }

            case STATE_RESPONSE_WAITING_HANDLER: {

                if (!dateParsed) {
                    s = ParseTimestampStr1(str);
                }
                if (StringUtils.isBlank(s)) {
                    break;
                }
                if (HTTPResponseContinue.matcher(s).find()) {
                    return null;
                } else if ((m = HTTPResponseHandler.matcher(s)).find()) {
                    ursHTTP.setHTTPHandlerID(m.group(1));
                    SetStdFieldsAndAdd(ursHTTP);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                } else {
                    pError(logger, "Unexpected line in " + m_ParserState + " " + s);
                    m_ParserState = ParserState.STATE_COMMENT;
                    return getDP();
                }

                break;

            }

            case STATE_HTTP_BODY: {
                m_PacketLength -= s.length() + 2;

                if ((m = HTTPHeaderContinue.matcher(str)).find()) {
                    m_MessageContents.add(s);

                } else {
                    m_ParserState = ParserState.STATE_RESPONSE_WAITING_HANDLER;
                    return str;
                }

            }
            break;

            //</editor-fold>
        }
        return null;
    }

    @Override
    void init(DBTables m_tables) {
        m_tables.put(TableType.URSHTTP.toString(), new ursHTTPMsgTable(Main.getInstance().getM_accessor()));
        m_tables.put(TableType.HTTP_TO_URS.toString(), new HTTPtoURSTable(Main.getInstance().getM_accessor()));

    }

    private ursHTTPMsg getFullHTTP(ursHTTPMsg ursHTTP) {
        ursHTTPMsg tmpUrsHTTP = partHTTP.get(ursHTTP.getSocket());
        if (tmpUrsHTTP != null) { // pending HTTP request
            tmpUrsHTTP.AddBytes(ursHTTP);
            tmpUrsHTTP.SetFileBytes(ursHTTP.getM_fileOffset() + ursHTTP.getFileBytes() - tmpUrsHTTP.getM_fileOffset());
            if (tmpUrsHTTP.isComplete()) {
                partHTTP.remove(ursHTTP.getSocket());
                return tmpUrsHTTP;
            }
        } else {
            if (ursHTTP.isComplete()) {
                return ursHTTP;
            } else {
                partHTTP.put(ursHTTP.getSocket(), ursHTTP);
            }
        }
        return null;

    }

    private int ParseHTTP(StringBuilder buf) {
        int linesRead = 0;
        int i = 0, start = 0, newLine = 0;

        while (i < buf.length()) {
            char c = buf.charAt(i);
            if (i == 0 && c == '\n') {
//                linesRead++;
                i++;
                start = i;
                continue;
            }

            newLine = 0;
            if (i + 1 < buf.length()) { // there is room to look ahead
                if (c == '\r' && buf.charAt(i + 1) == '\n'
                        || c == '\n' && buf.charAt(i + 1) == '\r') {
                    newLine = 2;
                }
            } else if (c == '\r' || c == '\n') {
                newLine = 1;
            }
            if (newLine == 0) {//skipping over normal characters
                i++;
            } else { //new line detected
                if (start < i - 1) {
                    m_MessageContents.add(buf.substring(start, i));
                } else {
                    m_MessageContents.add("");
                }
                i += newLine;
                start = i;
                linesRead++;
            }
        }
        if (start < buf.length()) {
            m_MessageContents.add(buf.substring(start));
        }

        return linesRead;
    }

    private void ReadHTTP(int bytes) throws IOException {
        StringBuilder buf = new StringBuilder();

        setSavedFilePos(getFilePos());
        buf.append(m_input.readBytes(bytes));
//        Main.logger.info("Buf["+buf+"]");
//        setFilePos(getEndFilePos() + m_input.getLastBytesConsumed());
        setEndFilePos(getEndFilePos() + m_input.getLastBytesConsumed());
        SetBytesConsumed(0);// file pos already set to, to avoid setting filepos in calling function
        m_CurrentLine += ParseHTTP(buf);
    }

    enum ParserState {
        STATE_HEADER,
        STATE_TMESSAGE_REQUEST,
        STATE_COMMENT,
        STATE_REQUEST_RECEIVED,
        STATE_READ_INFO,
        STATE_HTTP_HEADER,
        STATE_HTTP_HEADER1,
        STATE_HTTP_BODY, STATE_WAITING_HANDLER, STATE_RESPONSE_WAITING_HANDLER

    }

    class RequestHandler extends HashMap<Long, Long> {

        private void store(Long httpReqID, Long handlerID) throws Exception {
            if (httpReqID == null || handlerID == null || containsKey(httpReqID)) {
                throw new Exception("Request "
                        + String.format("%x", httpReqID)
                        + " already mapped to handler " + String.format("%x", handlerID));
            }
            put(handlerID, handlerID);

        }

        private Long getURSReqID(Long httpReqID) {
            for (Entry<Long, Long> entry : this.entrySet()) {
                Long key = entry.getKey();
                Long value = entry.getValue();
                if (value.equals(httpReqID)) {
                    return key;
                }
            }
            return null;
        }

    }

    public class ursHTTPMsg extends URSRIBase {

        //    Set-Cookie:ORSSESSIONID=00FAVPM3BOBM3223H3CA9ATAES00000G.node1576
        private Long reqID = null;
        private int socket = 0;
        private int bytes = 0;
        private String sid = null;
        private String method = null;
        private String httpResponseID;
        private Long httpHandlerID;
        private int bodyLineIdx = -2;
        private String httpMessageBody = null;

        public ursHTTPMsg(int fileID) {
            super(TableType.URSHTTP, fileID);
        }

        public ursHTTPMsg(ArrayList messageLines, int fileID) {
            this(fileID);
            setMessageLines(messageLines);
        }

        int getSocket() {
            return socket;
        }

        void setSocket(int socket) {
            this.socket = socket;
        }

        int getHTTPBytes() {
            return bytes;
        }

        void setHTTPServerID(String ip) {
            this.reqID = Util.intOrDef(ip, null, 16);
        }

        void setBytes(int bytes) {
            this.bytes += bytes;
        }

        void setSID(String sid) {
            this.sid = sid;
        }

        private String getHTTPMethod() {
            parseURL();
            return method;
        }

        boolean isComplete() {
            Main.logger.debug("isComplete: " + m_MessageLines);
            if (m_MessageLines != null && m_MessageLines.size() > 0) {
                String s1 = m_MessageLines.get(0);
                if (s1.startsWith("PO") || s1.startsWith("HT")) {
                    String s = GetHeaderValue("Content-Length", 'l');
                    if (s != null && s.length() > 0) {
                        int cl = Integer.parseInt(s);

                        return cl == 0 || getContentLengh() >= cl;
                    }
                    return false;
                }
            }
            return true;

        }

        public Long getReqID() {
            return reqID;
        }

        /**
         * returns httpMessageBody of HTTP message
         *
         * @return empty string if there is no httpMessageBody
         */
        private String getBody() {
            if (httpMessageBody == null) {
                int idx = evalBodyIdx();
                if (idx >= 0) {
                    List<String> subList = m_MessageLines.subList(idx, m_MessageLines.size());
                    StringBuilder s1 = new StringBuilder();
                    for (String string : subList) {
//                    if (s1.length() > 0) {
//                        s1.append("\n");
//                    }
                        if (!string.isEmpty()) {
                            s1.append(string);
                        }
                    }
                    if (s1.length() > 0) {
                        httpMessageBody = s1.toString();
                    } else {
                        httpMessageBody = "";
                    }

                }
            }
            return httpMessageBody;

        }

        /**
         * is calculated each time it is called
         *
         * @return
         */
        private int evalBodyIdx() {
            int ret = -1;
            if (m_MessageLines != null) {
                for (int i = 0; i < m_MessageLines.size(); i++) {
                    if (m_MessageLines.get(i).length() == 0) {
                        if (m_MessageLines.size() > i + 1) {
                            ret = i + 1;
                            break;
                        }
                    }
                }
            }
            return ret;
        }

        /**
         * value calculated once then stored
         *
         * @return
         */
        private int getBodyIdx() {
            if (bodyLineIdx < -1) {
                bodyLineIdx = evalBodyIdx();
            }
            return bodyLineIdx;
        }

        private int getContentLengh() {
            int i = evalBodyIdx();
            int ret = 0;

            if (i > 0) {
                ret++;
                for (; i < m_MessageLines.size(); i++) {
                    ret += m_MessageLines.get(i).length();
                }
            }
            Main.logger.trace("bodyidx:" + evalBodyIdx() + " ret:" + ret);
            return ret;
        }

        void AddBytes(ursHTTPMsg orsHTTP) {
            for (String next : orsHTTP.m_MessageLines) {
                m_MessageLines.add(next);
            }
            Main.logger.trace("after bytes added:" + m_MessageLines);

        }

        String getHTTPRequest() {
            parseURL();
            return getUrl();
        }

        private void parseURL() {
            if (url == null) {
                if (!m_MessageLines.isEmpty()) {
                    Matcher m;
                    if ((m = regHTTPURL.matcher(m_MessageLines.get(0))).find()) {
                        url = m.group(2);
                        processURL();
                        method = m.group(1);
                    }

                    if (method == null) {
                        if ((m = regHTTPResp.matcher(m_MessageLines.get(0))).find()) {
                            method = m.group(1);
                        }
                    }
                }
            }

        }

        Long getHTTPResponseID() {
            return intOrDef(httpResponseID, null, 16);
        }

        void setHTTPResponseID(String group) {
            if (group != null && group.substring(0, 2).equalsIgnoreCase("0x")) {
                httpResponseID = group.substring(2);
            } else {
                httpResponseID = group;
            }
        }

        private void setHTTPHandlerID(String hexHandlerID) {
            this.httpHandlerID = Util.intOrDef(hexHandlerID, null, 16);
        }

        public Long getHttpHandlerID() {
            return httpHandlerID;
        }

        private void processURL() {
            if (StringUtils.isNotBlank(url)) {
                String[] split = StringUtils.split(url, "?", 2);
                try {
                    setURIParts(split[0], (split.length == 2) ? split[1] : null);
                } catch (Exception ex) {
                    setURIParams(url);
                }
            }

        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());

            stmt.setInt(6, getSocket());
            stmt.setInt(7, getHTTPBytes());
            stmt.setBoolean(8, isInbound());
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.HTTPRequest, getHTTPRequest()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.HTTPMethod, getHTTPMethod()));
            setFieldLong(stmt, 11, getHTTPResponseID());
            setFieldLong(stmt, 12, getReqID());
            setFieldLong(stmt, 13, getHttpHandlerID());
            setTableValues(14, stmt);
            return true;

        }
    }

    public class ursHTTPMsgTable extends DBTable {

        public ursHTTPMsgTable(SqliteAccessor dbaccessor) {
            super(dbaccessor, TableType.URSHTTP);
        }

        @Override
        public void InitDB() {

            addIndex("time");
            addIndex("FileId");

            addIndex("URIID");
            addIndex("httpserverID");
            addIndex("methodID");
            addIndex("httpHandlerID");
            URSRI.addIndexes(this);

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",socket int"
                    + ",http_bytes int"
                    + ",isinbound bit"
                    + ",URIID int"
                    + ",methodID int"
                    + ",httpresponse bigint"
                    + ",httpserverID bigint"
                    + ",httpHandlerID bigint"
                    + URSRI.getTableFields()
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
                    + ",?"
                    + ",?"
                    + URSRI.getTableFieldsAliases()
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

    //<editor-fold defaultstate="collapsed" desc="comment">
    private class HTTPtoURS extends Message {

        private final Long ursRefID;
        private final Long httpReqID;

        private HTTPtoURS(Long ursRefID, Long httpReqID, boolean b, int fileID) {
            super(TableType.HTTP_TO_URS, fileID);
            this.ursRefID = ursRefID;
            this.httpReqID = httpReqID;
            setM_isInbound(b);
        }

        private Long getHTTPRefID() {
            return httpReqID;
        }

        private Long getURSRefID() {
            return ursRefID;
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());

            setFieldLong(stmt, 6, getHTTPRefID());
            setFieldLong(stmt, 7, getURSRefID());
            stmt.setBoolean(8, isInbound());
            return true;

        }
    }

    private class HTTPtoURSTable extends DBTable {

        public HTTPtoURSTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        private HTTPtoURSTable(SqliteAccessor m_accessor) {
            super(m_accessor, TableType.HTTP_TO_URS);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("refID");
            addIndex("httpHandlerID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",httpHandlerID bigint"
                    + ",refID bigint"
                    + ",isinbound bit"
                    + ");";
            getM_dbAccessor().runQuery(query);

        }

        @Override
        public String getInsert() {
            return "INSERT INTO " + getTabName()
                    + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
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
}
