/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static com.myutils.logbrowser.indexer.MCPParser.ParserState.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import org.apache.commons.lang3.*;

/**
 * @author ssydoruk
 */
public class MCPParser extends Parser {

    static final String[] BlockNamesToIgnoreArray = {"SIP:CTI:callSync::0",
            "SIP:CTI:sipStackSync:0"};
    private static final Pattern reqSIPRequest = Pattern.compile("Request (sent|received): (\\w+) (.+) SIP\\/2\\.0\\s*$");
    private static final Pattern reqSIPResponse = Pattern.compile("Response (received|sent): SIP\\/2\\.0\\s+(.+)$");
    private static final Pattern regHeaderEnd = Pattern.compile("^File:\\s+\\(");
    private static final Pattern ptSkip = Pattern.compile("^[:\\s]*");
    private static final Pattern ptLogID = Pattern.compile("^n*LogID=(\\w+-\\w+)$");
    private static final Pattern SIPHeaderContinue = Pattern.compile("^\\S");
    //    15:22:50.980: Sending  [0,UDP] 3384 bytes to 10.82.10.146:5060 >>>>>
    private static final Pattern regNotParseMessage = Pattern.compile("^(33009|49005"
            + ")");
    private static final Pattern regCfgObjectName = Pattern.compile("(?:name|userName|number|loginCode)='([^']+)'");
    private static final Pattern regCfgObjectDBID = Pattern.compile("DBID=(\\d+)");
    private static final Pattern regCfgObjectType = Pattern.compile("object Cfg(\\w+)=");
    private static final Pattern regCfgOp = Pattern.compile("\\(type Object(\\w+)\\)");
    private static final Pattern regSIPServerStartDN = Pattern.compile("^\\s*DN added \\(dbid (\\d+)\\) \\(number ([^\\(]+)\\) ");
    private static final Pattern ptGVPCallID = Pattern.compile("^\\w{8}-\\w{8}$");
    private static final Pattern ptFile = Pattern.compile("^(\\S+:\\d+)\\s");
    private static final Pattern ptFirstWord = Pattern.compile("^(\\S+)\\s");
    private static final HashMap<String, String> FunctionMap = getFunctionMap();
    private static final Pattern ptInitURL = Pattern.compile("INIT_URL=([^\\|]+)");
    private static final Pattern ptValidateURL = Pattern.compile("\\((\\w+)\\):(.+)$");
    private static final VXMLStepsParams vXMLStepsParams = new VXMLStepsParams();
    private static final org.apache.logging.log4j.Logger logger = Main.logger;

    private static final HashSet<String> IGNORE_MPC = getIGNORE_MPC();

    private static HashSet<String> getIGNORE_MPC() {
        HashSet<String> ret = new HashSet<>();
        ret.add("MPCConnection::ConnectRoute");
        ret.add("MPCConnection::Destroy");
        ret.add("MPCConnection::DisconnectRoute");
        ret.add("MPCConnection::GetFCRSink");
        ret.add("MPCConnection::GetSinks");
        ret.add("MPCConnection::Initialize");
        ret.add("MPCConnection::MPCConnection");
        ret.add("MPCConnection::Modify");
        ret.add("MPCConnection::ProcessRTPSocketClosed");
        ret.add("MPCConnection::ProcessSinkUnregisteredHelper");
        ret.add("MPCConnection::QueueDestroyedEvent");
        ret.add("MPCConnection::RecordStop");
        ret.add("MPCConnection::RegisterReverseRoute");
        ret.add("MPCConnection::RegisterSink");
        ret.add("MPCConnection::UnregisterReverseRoute");
        ret.add("MPCConnection::UnregisterSink");
        ret.add("MPCConnection::~MPCConnection");
        ret.add("MPCControlObject::RegisterSinkHelper");
        ret.add("MPCControlObject::UnregisterSinkHelper");
        ret.add("MPCDialog::ASRStart,");
        ret.add("MPCDialog::ASRStop,");
        ret.add("MPCDialog::ConnectRoute,");
        ret.add("MPCDialog::Destroy,");
        ret.add("MPCDialog::DisconnectRoute,");
        ret.add("MPCDialog::GetSinks,");
        ret.add("MPCDialog::MPCDialog");
        ret.add("MPCDialog::PlayStart,");
        ret.add("MPCDialog::ProcessPlayDone,");
        ret.add("MPCDialog::QueueDestroyedEvent,");
        ret.add("MPCDialog::RegisterASRSink,");
        ret.add("MPCDialog::RegisterForwardRoute,");
        ret.add("MPCDialog::RegisterReverseRoute,");
        ret.add("MPCDialog::UnregisterASRSink,");
        ret.add("MPCDialog::UnregisterReverseRoute,");
        ret.add("MPCDialog::~MPCDialog");
        return ret;
    }

    private final ArrayList<String> extraBuff;
    HashMap m_BlockNamesToIgnoreHash;
    StringBuilder sipBuf = new StringBuilder();
    long m_CurrentFilePos;
    long m_HeaderOffset;
    int m_PacketLength;
    int m_ContentLength;
    int m_headerLine;
    boolean m_isContentLengthUnknown;
    private ParserState m_ParserState;
    private boolean isInbound;
    private String SIPMessage;
    private String SIPURI;

    public MCPParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_MCP, m_tables);
        this.extraBuff = new ArrayList<>();
        //m_accessor = accessor;
        m_BlockNamesToIgnoreHash = new HashMap();
        for (String BlockNamesToIgnoreArray1 : BlockNamesToIgnoreArray) {
            m_BlockNamesToIgnoreHash.put(BlockNamesToIgnoreArray1, 0);
        }
    }

    private static HashMap<String, String> getFunctionMap() {
        HashMap<String, String> ret = new HashMap<>();
        ret.put("EvaluateExpression():", "expression");
        ret.put("RuntimeHelper::GetStringProperty()", "expression");
        ret.put("EvaluateCond():", "cond");
        ret.put("RuntimeFactory():", "runtime");
        return ret;
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
            Main.logger.error(e);
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

    void ContinueParsing(String initial, FileInfo fi) {
        String str;
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
            while ((str = input.readLine()) != null) {
                Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");
                if (str.trim().isEmpty()) {
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
            Main.logger.error(e);
        }
    }

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

                m_lineStarted = m_CurrentLine;
                if (s != null) {
                    if ((m = reqSIPRequest.matcher(s)).find()
                            || (m = reqSIPResponse.matcher(s)).find()) {
                        isInbound = m.group(1).startsWith("r");
                        SIPMessage = m.group(2);
                        if (m.groupCount() > 2) {
                            SIPURI = m.group(3);
                        }
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
                                if (StringUtils.isNotBlank(split[4])) {
                                    String[] msgSplit = StringUtils.split(split[4], null, 4);
                                    if (msgSplit.length >= 3 && StringUtils.isNotBlank(msgSplit[2])
                                            && (m = ptLogID.matcher(msgSplit[2])).find()) {
                                        callMessage(m.group(1), msgSplit[1], msgSplit[3]);
                                    }

                                }
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
                        AddSipMessage(m_MessageContents);
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
                    AddSipMessage(m_MessageContents);
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

    protected void AddSipMessage(ArrayList contents) throws Exception {
        Matcher m;

        // Populate our class representation of the message
        SipMessage msg;

        msg = new SipMessage(contents, TableType.SIPMS);

        if (StringUtils.isNotEmpty(SIPMessage)) {
            msg.SetInbound(isInbound);
            msg.SetName(SIPMessage);
            if (StringUtils.isNotBlank(SIPURI)) {
                msg.setSipURI(SIPURI);
            }
            SIPMessage = null;
            SIPURI = null;
        }

        try {
            SetStdFieldsAndAdd(msg);
        } catch (Exception e) {
            Main.logger.error("Error adding message line:" + m_lineStarted, e);
            PrintMsg(contents);
        }
    }

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
        m_tables.put(TableType.VXMLIntStepsTable, new VXMLIntStepTable(Main.getInstance().getM_accessor(), TableType.VXMLIntStepsTable));
    }

    private void noCallMessage(String[] split, int initalIndex) {
//        Main.logger.debug("noCallMessage "+StringUtils.join(split, ", ")+" "+initalIndex);
    }

    private void callMessage(String callID, String cmd, String messageText) {
        if (!IGNORE_MPC.contains(cmd)) {
            VXMLIntSteps steps = new VXMLIntSteps();
            steps.setMCPCallID(callID);
            steps.setCommand(cmd);
            //    steps.setRequestParams(messageText);
            SetStdFieldsAndAdd(steps);
        }
    }

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

                    String cmd = "";
                    if ((m1 = ptFirstWord.matcher(noFile)).find()) {
                        firstWord = m1.group(1);
                        noFirstWord = noFile.substring(m1.end());
                    }
                    if (firstWord != null) {
                        cmd = FunctionMap.get(firstWord);

                        if (cmd == null) {
                            cmd = "execution step";
                        }
                        if (cmd.equals("runtime") && (noFirstWord.startsWith("Called for"))) {
                            return;
                        }
                        if (cmd.equals("expression") && (noFirstWord.contains("__.savetmpfiles")
                                || noFirstWord.contains("__.savetmpfilesmode"))) {
                            return;
                        }

                        //steps.setParam1(noFirstWord);
                    } else {
                        //steps.setParam1(noFile);
                    }

                    VXMLIntSteps steps = new VXMLIntSteps();
                    steps.setMCPCallID(callID);
                    steps.setCommand(cmd);

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

    private static class VXMLStepsParams {

        private final Map<String, IParamParseProc> procSplitSteps;
        private final Map<Pattern, IPatternProc> procPatternSteps;

        public VXMLStepsParams() {
            this.procSplitSteps = initSplitSteps();
            this.procPatternSteps = initPatternSteps();
        }

        private static void setMsgParams(VXMLIntSteps msg, String[] msgParams, int startingParamIdx) {
            setMsgParams(msg, msgParams, startingParamIdx, true);
        }

        private static void setMsgParams(VXMLIntSteps msg, String[] msgParams, int startingParamIdx, boolean checkDigits) {
            int paramIdx = 1;
            if (ArrayUtils.isNotEmpty(msgParams)) {
                for (int i = startingParamIdx; i < msgParams.length; i++) {
                    String s = msgParams[i];
                    StringBuilder b = new StringBuilder();
                    String[] split = StringUtils.split(s, " |:#");

                    for (String string : split) {
                        if (StringUtils.isNotBlank(string) && !StringUtils.contains(string, msg.getMcpCallID())
                                && (!checkDigits || !Utils.StringUtils.isNumeric(string))) {
                            if (b.length() > 0) {
                                b.append(" ");
                            }
                            b.append(string);
                        }
                    }
                    if (b.length() > 0) {
                        switch (paramIdx) {
                            case 1:
                                msg.setParam1(b.toString());
                                break;

                            default:
                                msg.setParam2(b.toString());
                        }
                        paramIdx++;
                    }
                }
            }
        }

        private Map<String, IParamParseProc> initSplitSteps() {
            HashMap<String, IParamParseProc> ret = new HashMap<>();
            ret.put("call_reference", new IParamParseProc() {
                /**
                 * msgParams is always not null and contains at least one
                 * element msgParams[0] is command
                 */
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    String params1 = msgParams[1];
                    if (params1 != null && !params1.isEmpty()) {
                        String[] split = StringUtils.split(params1, "|");
                        if (split != null && split.length > 3) {
                            msg.setSipCallID(split[0]);
                            msg.setGVPSession(split[1]);
                            msg.setTenant(split[2]);
                            msg.setIVRProfile(split[3]);
                        }
                    }
                }
            });
            ret.put("form_select", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });

            ret.put("form_enter", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });

            ret.put("dtmf", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    if (msgParams.length > 1) {
                        msg.setParam1(clearParam(msgParams[1]));
                    }
                }
            });

            ret.put("dtmf_input", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1, false);
                    if (msgParams.length > 1) {
                        msg.setParam1(msgParams[1]);
                    }
                }
            });

            ret.put("form_exit", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });
            ret.put("prompt_start", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });
            ret.put("prompt_stop", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });
            ret.put("prompt_end", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });
            ret.put("prompt_type", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });

            ret.put("wf_lookup", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });
            ret.put("asr_open", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
//                    msg.setParam1(msgParams[1]);
                }

            });
            ret.put("asr_close", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });
            ret.put("asr_start", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });
            ret.put("asr_end", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });
            ret.put("asr_trace", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });

            ret.put("event", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    if (msgParams.length > 0) {
                        String[] split = StringUtils.split(msgParams[1], ":", 2);
                        if (ArrayUtils.isNotEmpty(split)) {
                            msg.setParam1("event_" + split[0]);
                        }
                    }
                }
            });

            ret.put("event_handler_exit", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });

            ret.put("event_handler_enter", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    if (msgParams.length > 0) {
                        String[] split = StringUtils.split(msgParams[1], "|", 2);
                        if (ArrayUtils.isNotEmpty(split)) {
                            msg.setParam1("event_" + split[0]);
                        }
                    }
                }
            });

            ret.put("filling", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });

            ret.put("fetch_end", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    Matcher m = ptValidateURL.matcher(orig);
                    if (m.find()) {
                        msg.setParam1(m.group(2));
                        msg.setParam2(m.group(1));
                    } else {
                        msg.setParam1(orig);
                    }
                }
            });

            ret.put("appl_begin", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    if (msgParams.length > 1) {
                        Matcher m = ptInitURL.matcher(msgParams[1]);

                        if (m.find()) {
                            msg.setParam1(m.group(1));
                        }
                    }
                }
            });
            ret.put("appl_end", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });

            ret.put("incall_begin", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                }
            });
            ret.put("incall_end", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });
            ret.put("input_end", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });

            ret.put("route_result", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });
            ret.put("subdialog_return", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });
            ret.put("subdialog_start", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });

            ret.put("CMCall", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    if (msgParams.length > 1 && msgParams[1].endsWith("deleted")) {
                        msg.setCommand(StringUtils.join(msgParams[0], " ", msgParams[1]));
                    } else {
                        msg.setCommand(msgParams[0]);
                    }
                }
            });
            ret.put("CMCallLeg", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    if (msgParams.length > 1 && msgParams[1].endsWith("deleted")) {
                        msg.setCommand(StringUtils.join(msgParams[0], " ", msgParams[1]));
                    } else {
                        msg.setCommand(msgParams[0]);
                    }
                }
            });

            ret.put("input_start", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);
                }
            });

            ret.put("incall_initiated", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });
            ret.put("filled_exit", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });
            ret.put("prompt", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }
            });

            ret.put("filled_enter", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }

            });
            ret.put("DTMF:", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }

            });

            ret.put("Error", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }

            });

            ret.put("root_appl", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }

            });

            ret.put("log", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                }
            });

            ret.put("goto", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }

            });
            ret.put("compile_done", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    setMsgParams(msg, msgParams, 1);

                }

            });

            ret.put("prompt_play", new IParamParseProc() {
                @Override
                public void proc(VXMLIntSteps msg, String[] msgParams, String orig) {
                    msg.setCommand(msgParams[0]);
                    String[] split = StringUtils.split(msgParams[1], "|");
                    if (ArrayUtils.isNotEmpty(split)) {
                        if (split.length > 1) {
                            msg.setParam1(split[1]);
                            msg.setParam2(split[0]);
                        } else {
                            msg.setParam1(split[0]);
                        }
                    } else {
                        msg.setParam1(msgParams[1]);
                    }
                }
            });

            return ret;
        }

        private String clearParam(String p) {
            return StringUtils.stripStart(p, ":");
        }

        private Map<Pattern, IPatternProc> initPatternSteps() {
            HashMap<Pattern, IPatternProc> ret = new HashMap<>();
            ret.put(Pattern.compile("^\\|\\d+ms$"), new IPatternProc() {
                @Override
                public void proc(VXMLIntSteps msg, Matcher m) {
                    msg.setCommand(null); // setting command to null ignores record
                }
            });

            ret.put(Pattern.compile("^((?:Fetching|Compile) Done).+\\|([^\\|]+)$"), new IPatternProc() {
                @Override
                public void proc(VXMLIntSteps msg, Matcher m) {
                    msg.setCommand(m.group(1)); // setting command to null ignores record
                    msg.setParam1(m.group(2));
                }
            });
            return ret;
        }

        public void processRequestParams(VXMLIntSteps msg, String msgParam) {
            if (msgParam != null) {
                String[] msgParams = StringUtils.split(msgParam, null);
                if (msgParams.length > 0) {
                    IParamParseProc proc = procSplitSteps.get(msgParams[0]);
                    if (proc != null) {
                        proc.proc(msg, msgParams, msgParam);
                        return;
                    }
                }
                for (Map.Entry<Pattern, IPatternProc> entry : procPatternSteps.entrySet()) {
                    Pattern rx = entry.getKey();
                    IPatternProc proc = entry.getValue();
                    Matcher m;
                    if ((m = rx.matcher(msgParam)).find()) {
                        proc.proc(msg, m);
                        return;
                    }

                }
                msg.setCommand("message");
                msg.setParam1("__ " + msgParam);

            }

        }

        private interface IParamParseProc {

            void proc(VXMLIntSteps msg, String[] msgParams, String strOrig);
        }

        private interface IPatternProc {

            void proc(VXMLIntSteps msg, Matcher m);
        }
    }

    private class VXMLIntSteps extends Message {

        private String command;

        private String mcpCallID;
        private String param1;
        private String param2;
        private String sipCallID;
        private String GVPSession;
        private String Tenant;
        private String IVRProfile;

        private VXMLIntSteps() {
            super(TableType.VXMLIntStepsTable);
        }

        public String getParam2() {
            return param2;
        }

        public void setParam2(String param2) {
            this.param2 = param2;
        }

        public String getCommand() {
            return command;
        }

        private void setCommand(String msgParam) {
            this.command = msgParam;
        }

        public String getMcpCallID() {
            return mcpCallID;
        }

        private void setMCPCallID(String callID) {
            this.mcpCallID = callID;
        }

        /**
         * Get the value of params
         *
         * @return the value of params
         */
        public String getParam1() {
            return param1;
        }

        /**
         * Set the value of params
         *
         * @param params new value of params
         */
        public void setParam1(String params) {
            this.param1 = params;
        }

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

        //        private void setStepParams(String[] msgParams) {
//            if (msgParams != null) {
//                setCommand(msgParams[0]);
//                if (msgParams.length > 1) {
//                    if (getCommand().equals("call_reference")) {
//                        String params1 = msgParams[1];
//                        if (params1 != null && !params1.isEmpty()) {
//                            String[] split = StringUtils.split(params1, "|");
//                            if (split != null && split.length > 3) {
//                                setSipCallID(split[0]);
//                                setGVPSession(split[1]);
//                                setTenant(split[2]);
//                                setIVRProfile(split[3]);
//                            }
//                        }
//                    } else {
//                        setParams(StringUtils.join(ArrayUtils.subarray(msgParams, 1, msgParams.length), " "));
//
//                    }
//                }
//            }
//        }
        private void setRequestParams(String msgParam) {
            vXMLStepsParams.processRequestParams(this, msgParam);
        }

        private String getParamValue1() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private String getParamValue2() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    private class VXMLIntStepTable extends DBTable {

        public VXMLIntStepTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("commandId");
            addIndex("param1ID");
            addIndex("param2ID");
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
                    + ",param1ID INTEGER"
                    + ",param2ID INTEGER"
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
            VXMLIntSteps rec = (VXMLIntSteps) _rec;
            String command = rec.getCommand();
            if (StringUtils.isNotEmpty(command)) {
                getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                    @Override
                    public void fillStatement(PreparedStatement stmt) throws SQLException {
                        stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                        stmt.setInt(2, SCSAppStatus.getFileId());
                        stmt.setLong(3, rec.getM_fileOffset());
                        stmt.setLong(4, rec.getM_FileBytes());
                        stmt.setLong(5, rec.getM_line());

                        setFieldInt(stmt, 6, Main.getRef(ReferenceType.VXMLCommand, command));
                        setFieldInt(stmt, 7, Main.getRef(ReferenceType.VXMLCommandParams, rec.getParam1()));
                        setFieldInt(stmt, 8, Main.getRef(ReferenceType.VXMLCommandParams, rec.getParam2()));
                        setFieldInt(stmt, 9, Main.getRef(ReferenceType.VXMLMCPCallID, rec.getMcpCallID()));
                        setFieldInt(stmt, 10, Main.getRef(ReferenceType.SIPCALLID, rec.getSipCallID()));
                        setFieldInt(stmt, 11, Main.getRef(ReferenceType.GVPSessionID, rec.getGVPSession()));
                        setFieldInt(stmt, 12, Main.getRef(ReferenceType.Tenant, rec.getTenant()));
                        setFieldInt(stmt, 13, Main.getRef(ReferenceType.GVPIVRProfile, rec.getIVRProfile()));

                    }
                });
            }
        }
    }
}
