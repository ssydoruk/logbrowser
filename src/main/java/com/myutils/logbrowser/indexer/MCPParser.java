/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static com.myutils.logbrowser.indexer.MCPParser.ParserState.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author kvoroshi
 */
public class MCPParser extends Parser {

    static final String[] BlockNamesToIgnoreArray = {"SIP:CTI:callSync::0",
        "SIP:CTI:sipStackSync:0"};

    private static HashMap<String, String> getFunctionMap() {
        HashMap<String, String> ret = new HashMap<>();
        ret.put("EvaluateExpression():", "expression");
        ret.put("RuntimeHelper::GetStringProperty()", "expression");
        return ret;
    }
    Hashtable m_BlockNamesToIgnoreHash;

    StringBuilder sipBuf = new StringBuilder();

    long m_CurrentFilePos;
    long m_HeaderOffset;

    private final static Pattern regSIPResp = Pattern.compile("Response sent: SIP\\/2\\.0\\s+(.+)$");
    private final static Pattern regSIPReq = Pattern.compile("Request received: (\\w+) (.+) SIP\\/2\\.0$");
    private static final Pattern regHeaderEnd = Pattern.compile("^File:\\s+\\(");

    private ParserState m_ParserState;
    int m_PacketLength;
    int m_ContentLength;
    String m_Header;
    int m_headerLine;
    boolean m_isContentLengthUnknown;

    private DateParsed dpHeader;
    private static final Pattern ptSkip = Pattern.compile("^[:\\s]*");

    private final static Pattern regProxy = Pattern.compile("^Proxy\\((\\w+):");
    private final static Pattern SIPHeaderContinue = Pattern.compile("^\\S");
    private ArrayList<String> extraBuff;

    public MCPParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_MCP, m_tables);
        this.extraBuff = new ArrayList<>();
        //m_accessor = accessor;
        m_BlockNamesToIgnoreHash = new Hashtable();
        for (int i = 0; i < BlockNamesToIgnoreArray.length; i++) {
            m_BlockNamesToIgnoreHash.put(BlockNamesToIgnoreArray[i], 0);
        }
    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentFilePos = offset;
        m_CurrentLine = line;

        String unfinished = "";

        try {
            input.skip(offset);
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            m_ParserState = STATE_HEADER;

            String str;
            while ((str = input.readLine()) != null) {
                m_CurrentLine++;
                Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");

                try {
                    SetBytesConsumed(input.getLastBytesConsumed());

                    setEndFilePos(getFilePos() + getBytesConsumed()); // to calculate file bytes

//                    if (input.charsSkippedOnReadLine > 0) {
//                        ParseLine(input, str);
//                    } else {
//                        unfinished = str;
//                    }
                    while (str != null) {
                        str = ParseLine(input, str, fi);
                    }
                    if (!extraBuff.isEmpty()) {
                        for (String string : extraBuff) {
                            while (string != null) {
                                Main.logger.trace("Tracing extra: [" + string + "]");
                                string = ParseLine(input, string, fi);
                            }

                        }
                        extraBuff.clear();
                    }
                    setFilePos(getEndFilePos());

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                    throw e;
                }
//                m_CurrentFilePos += str.length()+input.charsSkippedOnReadLine;
            }

            if (m_ParserState == STATE_COMMENTS) {
                ParseLine(input, unfinished, fi);
            } else {
                ContinueParsing(unfinished, fi);
            }

            ParseLine(input, "", fi); // to complete the parsing of the last line/last message
        } catch (Exception e) {
            e.printStackTrace();
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

    void ContinueParsing(String initial, FileInfo fi) {
        String str = "";
        BufferedReaderCrLf input = Main.getMain().GetNextFile();
        if (input == null) {
            return;
        }
        try {
            boolean notFound = true;
            while ((str = input.readLine()) != null) {
                Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");
                if (str.startsWith("File: ")) {
                    notFound = false;
                    break;
                }
            }
            if (notFound) {
                return;
            }
            notFound = true;
            while ((str = input.readLine()) != null) {
                Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");
                if (str.trim().isEmpty()) {
                    notFound = false;
                    break;
                }
            }
            boolean isInitial = true;
            while ((str = input.readLine()) != null) {
                Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");
                if (isInitial) {
                    str = initial + str;
                    isInitial = false;
                }
                ParseLine(input, str, fi);
                if (m_ParserState == STATE_COMMENTS) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final Pattern regSIPHeader = Pattern.compile(" (Request received:|Response sent:) ");
//    15:22:50.980: Sending  [0,UDP] 3384 bytes to 10.82.10.146:5060 >>>>>

    private static final Pattern regNotParseMessage = Pattern.compile("^(33009|49005"
            + ")");

    String ParseLine(BufferedReaderCrLf input, String str, FileInfo fi) throws Exception {
        String s = str;
        Matcher m;

        Main.logger.trace("Parser state: " + m_ParserState);
        ParseCustom(str, 0);
        switch (m_ParserState) {
            case STATE_HEADER: {
                if ((regHeaderEnd.matcher(str)).find()) {
                    m_ParserState = STATE_COMMENTS;
                }
            }
            return null;

            case STATE_COMMENTS:
                m_PacketLength = 0;
                m_ContentLength = 0;
                setSavedFilePos(getFilePos());

                try {
                    s = ParseTimestampStr(str);
                } catch (Exception exception) {
                    Exception e = exception;

                    throw exception; // do nothing so far. Can insert code to ignore certain exceptions
                }

                m_LineStarted = m_CurrentLine;
                if (s != null) {
                    if ((regSIPHeader.matcher(s)).find()) {
                        m_Header = s;

                        dpHeader = dp; // so time of the message correctly set in AddSipMessage

                        m_HeaderOffset = m_CurrentFilePos;
                        m_headerLine = m_CurrentLine;
                        m_MessageContents.clear();
                        setSavedFilePos(getFilePos());
//                        m_MessageContents.add(s);
                        m_ParserState = STATE_SIP_HEADER;
                        return null;

                    } else {

                        GenesysMsg genesysMsg = GenesysMsg.CheckGenesysMsg(dp, this, fi.m_componentType == FileInfoType.type_MCP ? TableType.MsgMCP : TableType.MsgRM, regNotParseMessage, false);
                        int maxSplit = 5;
                        int callIDIdx = 1;
                        int messageTextIdx = 4;
                        boolean isInt = false;
                        if (genesysMsg != null) {
                            s = genesysMsg.getLine();
                            if (genesysMsg.getLevelString().equalsIgnoreCase("int")) {
                                maxSplit = 3;
                                callIDIdx = 0;
                                messageTextIdx = 2;
                                isInt = true;
                            }
                        }

                        String[] split = StringUtils.split(s, null, maxSplit);
                        if (split != null && split.length > maxSplit - 1) {
                            String callID = split[callIDIdx];
                            if (callID.equals("00000000-00000000")) {
                                boolean msgDone = false;
                                if (genesysMsg != null) {
                                    if (!genesysMsg.isToIgnore()) {
                                        genesysMsg.setLine(split[messageTextIdx]);
                                        genesysMsg.AddToDB(m_tables);
                                    }
                                    msgDone = true;
                                }

                                if (!msgDone) {

                                    if (split[0].equals("DBUG")) {
                                        Main.logger.trace("l:" + m_CurrentLine + " ignoring debug msg: " + str);
                                    } else {
                                        Main.logger.info("l:" + m_CurrentLine + " ignoring msg: " + str);
                                    }

                                }
                            } else {
                                callMessage(split[callIDIdx], split[messageTextIdx]);
                                if (genesysMsg != null && !genesysMsg.isToIgnore() && !isInt && !split[0].equals("DBUG") && !split[0].equals("INFO")) {
                                    genesysMsg.setLine(split[messageTextIdx]);
                                    genesysMsg.AddToDB(m_tables);
                                }
                            }

//                            if (split[0].equals("DBUG")) {
//                                if (split[callIDIdx].equals("00000000-00000000")) {
//                                    noCallMessage(split, 2);
//                                } else {
//                                    callMessage(split[callIDIdx], split, 2);
//                                }
//                            } else {
//                                intMessage(split[callIDIdx], split, maxSplit - 1);
//                            }
                        }
                    }
                }
                break;

//<editor-fold defaultstate="collapsed" desc="state_SIP_HEADER">
            case STATE_SIP_HEADER: {
                if ((SIPHeaderContinue.matcher(str)).find()) {
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
                    if (m_ContentLength > 0 || m_isContentLengthUnknown) {
                        m_ParserState = STATE_SIP_BODY;
                        m_MessageContents.add(s);

                    } else {
                        m_ParserState = STATE_COMMENTS;
                        m_MessageContents.add("");
                        AddSipMessage(m_MessageContents, m_Header);
                        m_MessageContents.clear();
                    }
                }
            }
            return null;
//</editor-fold>

            case STATE_SIP_BODY: {

                m_PacketLength -= s.length() + 2;
                boolean endFound = false;

                if (str.trim().isEmpty()) {
                    endFound = true;
                } else {
                    s = ParseGenesys(str, fi.m_componentType == FileInfoType.type_MCP ? TableType.MsgMCP : TableType.MsgRM, regNotParseMessage, ptSkip);
                    if (getDP() != null) {
                        endFound = true;
                    }
                }
                if (endFound) {
                    m_ParserState = STATE_COMMENTS;
                    AddSipMessage(m_MessageContents, m_Header);
                    m_MessageContents.clear();
                    return str;
                } else {
                    m_MessageContents.add(s);
                }

            }
            return null;

            case STATE_CONFIG:
                AddConfigMessage(m_MessageContents);
                m_MessageContents.clear();
                m_ParserState = STATE_COMMENTS;
                return str;

            case STATE_POSSIBLE_MULTIPLE_SIP_MESSAGES:
                // multiple sip INFO messages in one TCP packet from Primary to Backup.
                if (s.startsWith("INFO sip:")) {
                    //  header stay the same
                    m_MessageContents.clear();
                    m_ParserState = STATE_SIP_HEADER_GSIPLIB;
                    m_ContentLength = 0;
                    m_isContentLengthUnknown = false;

                } else {
                    m_ParserState = STATE_COMMENTS;
                }
                ParseLine(input, str, fi);
                return null;

        }
        // since many call related messages are parsed, we parse only at the
        // end for Genesys messages

        return null;
    }

    protected void AddSipMessage(ArrayList contents, String header) throws Exception {
        Matcher m;

        // Populate our class representation of the message
        SipMessage msg = null;

        msg = new SipMessage(contents, TableType.SIPMS);

        if ((m = regSIPReq.matcher(header)).find()) {
            msg.SetInbound(true);
            msg.SetName(m.group(1));
            msg.setSipURI(m.group(2));
        } else if ((m = regSIPResp.matcher(header)).find()) {
            msg.SetInbound(false);
            msg.SetName(m.group(1));

        }

        try {
            SetStdFieldsAndAdd(msg);
        } catch (Exception e) {
            Main.logger.error("Error adding message line:" + m_LineStarted, e);
            PrintMsg(contents);
        }
    }

    private static final Pattern regCfgObjectName = Pattern.compile("(?:name|userName|number|loginCode)='([^']+)'");
    private static final Pattern regCfgObjectDBID = Pattern.compile("DBID=(\\d+)");
    private static final Pattern regCfgObjectType = Pattern.compile("object Cfg(\\w+)=");
    private static final Pattern regCfgOp = Pattern.compile("\\(type Object(\\w+)\\)");
    private static final Pattern regSIPServerStartDN = Pattern.compile("^\\s*DN added \\(dbid (\\d+)\\) \\(number ([^\\(]+)\\) ");

    private void AddConfigMessage(ArrayList<String> m_MessageContents) {
        ConfigUpdateRecord msg = new ConfigUpdateRecord(m_MessageContents);
        try {
            Matcher m;
            if (m_MessageContents.size() > 0 && (m = regSIPServerStartDN.matcher(m_MessageContents.get(0))).find()) {
                msg.setObjectType("DN");
                msg.setObjectDBID(m.group(1));
                msg.setObjName(m.group(2));

            } else {
                msg.setObjName(Message.FindByRx(m_MessageContents, regCfgObjectName, 1, ""));
                msg.setOp(Message.FindByRx(m_MessageContents, regCfgOp, 1, ""));
                msg.setObjectDBID(Message.FindByRx(m_MessageContents, regCfgObjectDBID, 1, ""));
                msg.setObjectType(Message.FindByRx(m_MessageContents, regCfgObjectType, 1, ""));
            }
            SetStdFieldsAndAdd(msg);
        } catch (Exception e) {
            Main.logger.error("Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);

        }
    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
        m_tables.put(TableType.VXMLIntStepsTable, new VXMLIntStepTable(Main.getMain().getM_accessor(), TableType.VXMLIntStepsTable));
    }

    private void noCallMessage(String[] split, int initalIndex) {
//        Main.logger.debug("noCallMessage "+StringUtils.join(split, ", ")+" "+initalIndex);
    }

    private static final Pattern ptGVPCallID = Pattern.compile("^\\w{8}-\\w{8}$");
    private static final Pattern ptFile = Pattern.compile("^(\\S+:\\d+)\\s");
    private static final Pattern ptFirstWord = Pattern.compile("^(\\S+)\\s");

    private static final HashMap<String, String> FunctionMap = getFunctionMap();

    private void callMessage(String callID, String messageText) {
        if (ptGVPCallID.matcher(callID).find()) {
//            if (split.length > 4) {
            String s = messageText;
            Matcher m, m1;
            if ((m = ptFile.matcher(s)).find()) {
                String theFile = m.group(1);
                String noFile = s.substring(m.end());
                if (theFile.startsWith("Runtime.c")) {
                    String firstWord = null;
                    String noFirstWord = null;
                    if ((m1 = ptFirstWord.matcher(noFile)).find()) {
                        firstWord = m1.group(1);
                        noFirstWord = noFile.substring(m1.end());
                    }
                    VXMLIntSteps steps = new VXMLIntSteps();
                    steps.setMCPCallID(callID);
                    if (firstWord != null) {
                        String get = FunctionMap.get(firstWord);
                        if (get != null) {
                            steps.setCommand(get);
                        } else {
                            steps.setCommand("execution step");

                        }
                        steps.setParams(noFirstWord);
                    } else {
                        steps.setParams(noFile);
                    }

//                        steps.setStepParams(StringUtils.split(split[initalIndex], null));
                    SetStdFieldsAndAdd(steps);
                }
            } else { // file not specified
                VXMLIntSteps steps = new VXMLIntSteps();
                steps.setMCPCallID(callID);
                steps.setRequestParams(messageText);

                SetStdFieldsAndAdd(steps);
            }
//            }
        } else {
            Main.logger.debug("l:" + m_CurrentLine + " incorrect call ID [" + callID + "]; message ignored (likely about recording)");
        }
    }

//    private void intMessage(String callID, String[] split, int initalIndex) {
//        VXMLIntSteps steps = new VXMLIntSteps();
//        steps.setMCPCallID(callID);
//        steps.setRequestParams(StringUtils.split(split[initalIndex], null));
//
//        SetStdFieldsAndAdd(steps);
//    }
    //<editor-fold defaultstate="collapsed" desc="ParserState">
    enum ParserState {
        STATE_COMMENTS,
        STATE_HEADER,
        STATE_SIP_HEADER,
        /* network t-server style (message is idented) */
        STATE_SIP_BODY,
        /* network t-server style */
        STATE_SIP_HEADER_GSIPLIB,
        /* GSIPLIB style, message is not idented, ends with blank line */
        STATE_SIP_BODY_GSIPLIB,
        /* GSIPLIB style, message is not idented, ends with blank line */
        STATE_TLIB_MESSAGE,
        /* T-Lib message as T-Server writes it */
        STATE_POSSIBLE_MULTIPLE_SIP_MESSAGES,
        /* maltiple sip INFO messages in one packet */
        STATE_TLIB_MESSAGE_SIP_SYNC,
        /* SIP stack UserEvent sync message */
        STATE_JSON_HEADER,
        /* ESS - extended services */
        STATE_JSON_BODY,
        /* ESS - extended services */
        STATE_ISCC_MESSAGE,
        STATE_CONFIG,
        STATE_SIP_HEADER1
    }

//</editor-fold>
    private class VXMLIntSteps extends Message {

        private String command;

        public String getCommand() {
            return command;
        }

        public String getMcpCallID() {
            return mcpCallID;
        }
        private String mcpCallID;

        private VXMLIntSteps() {
            super(TableType.VXMLIntStepsTable);
        }

        private void setMCPCallID(String callID) {
            this.mcpCallID = callID;
        }

        private void setCommand(String msgParam) {
            this.command = msgParam;
        }

        private String params;

        /**
         * Get the value of params
         *
         * @return the value of params
         */
        public String getParams() {
            return params;
        }

        /**
         * Set the value of params
         *
         * @param params new value of params
         */
        public void setParams(String params) {
            this.params = params;
        }

        private String sipCallID;

        /**
         * Get the value of sipCallID
         *
         * @return the value of sipCallID
         */
        public String getSipCallID() {
            return sipCallID;
        }

        /**
         * Set the value of sipCallID
         *
         * @param sipCallID new value of sipCallID
         */
        public void setSipCallID(String sipCallID) {
            this.sipCallID = sipCallID;
        }

        private String GVPSession;

        /**
         * Get the value of GVPSession
         *
         * @return the value of GVPSession
         */
        public String getGVPSession() {
            return GVPSession;
        }

        /**
         * Set the value of GVPSession
         *
         * @param GVPSession new value of GVPSession
         */
        public void setGVPSession(String GVPSession) {
            this.GVPSession = GVPSession;
        }

        private String Tenant;

        /**
         * Get the value of Tenant
         *
         * @return the value of Tenant
         */
        public String getTenant() {
            return Tenant;
        }

        /**
         * Set the value of Tenant
         *
         * @param Tenant new value of Tenant
         */
        public void setTenant(String Tenant) {
            this.Tenant = Tenant;
        }

        private String IVRProfile;

        /**
         * Get the value of IVRProfile
         *
         * @return the value of IVRProfile
         */
        public String getIVRProfile() {
            return IVRProfile;
        }

        /**
         * Set the value of IVRProfile
         *
         * @param IVRProfile new value of IVRProfile
         */
        public void setIVRProfile(String IVRProfile) {
            this.IVRProfile = IVRProfile;
        }

        private void setStepParams(String[] msgParams) {
            if (msgParams != null) {
                setCommand(msgParams[0]);
                if (msgParams.length > 1) {
                    if (getCommand().equals("call_reference")) {
                        String params1 = msgParams[1];
                        if (params1 != null && !params1.isEmpty()) {
                            String[] split = StringUtils.split(params1, "|");
                            if (split != null && split.length > 3) {
                                setSipCallID(split[0]);
                                setGVPSession(split[1]);
                                setTenant(split[2]);
                                setIVRProfile(split[3]);
                            }
                        }
                    } else {
                        setParams(StringUtils.join(ArrayUtils.subarray(msgParams, 1, msgParams.length), " "));

                    }
                }
            }
        }

        private void setRequestParams(String msgParam) {
            if (msgParam != null) {
                String[] msgParams = StringUtils.split(msgParam, null);
                if (msgParams.length > 1 && msgParams[0].equals("call_reference")) {
                    setCommand(msgParams[0]);
                    String params1 = msgParams[1];
                    if (params1 != null && !params1.isEmpty()) {
                        String[] split = StringUtils.split(params1, "|");
                        if (split != null && split.length > 3) {
                            setSipCallID(split[0]);
                            setGVPSession(split[1]);
                            setTenant(split[2]);
                            setIVRProfile(split[3]);
                        }
                    }
                } else {
                    setCommand("message");
                    setParams(msgParam);

                }
            }

        }

    }

    private class VXMLIntStepTable extends DBTable {

        public VXMLIntStepTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("commandId");
            addIndex("paramsID");
            addIndex("mcpCallIDID");
            addIndex("SIPCallID");
            addIndex("GVPSessionID");
            addIndex("TenantID");
            addIndex("IVRProfileID");
            addIndex("time");
            addIndex("FileId");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",commandId INTEGER"
                    + ",paramsID INTEGER"
                    + ",mcpCallIDID INTEGER"
                    + ",SIPCallID INTEGER"
                    + ",GVPSessionID INTEGER"
                    + ",TenantID INTEGER"
                    + ",IVRProfileID INTEGER"
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
            VXMLIntSteps rec = (VXMLIntSteps) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, SCSAppStatus.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.VXMLCommand, rec.getCommand()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.VXMLCommandParams, rec.getParams()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.VXMLMCPCallID, rec.getMcpCallID()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.SIPCALLID, rec.getSipCallID()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.GVPSessionID, rec.getGVPSession()));
                setFieldInt(stmt, 11, Main.getRef(ReferenceType.Tenant, rec.getTenant()));
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.GVPIVRProfile, rec.getIVRProfile()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }

}
