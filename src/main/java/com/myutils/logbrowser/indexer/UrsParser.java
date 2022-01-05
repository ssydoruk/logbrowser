package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author terry
 */
public class UrsParser extends Parser {

    public static final HashMap<String, String> targetCode = getTargetCodes();
    private static final Pattern regRIGenerated = Pattern.compile("^\\[19:0f\\]connid (\\w+) generated for client=(\\d+)\\(([^\\]]+)\\).+\\*(\\w+)$");
    private static final Pattern regRIRequestStart = Pattern.compile("^\\[19:10\\][^:]+: ([^\\?]+)\\?(.+), client=(\\d+)\\(([^\\)]+)\\).+ref=(\\d+)$");
    private static final Pattern regRIRequestShort = Pattern.compile("^\\[19:10\\][^:]+: (.+), client=(\\d+)\\(([^\\)]+)\\).+ref=(\\d+)$");
    private static final Pattern regRIRequestShort1 = Pattern.compile("\\[19:10\\][^:]+: (.+), client=");
    private static final Pattern regRIResp = Pattern.compile("^\\[19:11\\].+'([\\w~]+)'.+ client (\\d+)\\(([\\w~]+)\\).+ref=(\\d+)$");
    private static final Pattern regRICallStart = Pattern.compile("received: urs/call/start");
    private static final Pattern regRIConnIDGenerated = Pattern.compile("\\[19:10\\] connid ([\\w~]+).+client=(\\d+)\\(([^\\)]+)\\).+ref id=(\\d+), .+name=([\\w~]+)");
    private static final Pattern reIID = Pattern.compile("^\\s+IID:(\\w+)$");
    //17:33:04.587_I_I_01d9027e17ef8b38 [09:05] >>>>>>>>>>>>resume interpretator(0), func:SuspendForDN
//    _I_I_01d9027e17ef8b38 [09:04] ASSIGN: agent_target(LOCAL) <- STRING: "return:timeout"
//    _I_I_01d9027e17ef8b38 [09:04] ASSIGN: N_attempt(LOCAL) <- INTEGER: 1
//17:33:04.587_M_I_01d9027e17ef8b38 [07:0c] default priority 6300
//    _I_I_01d9027e17ef8b38 [07:27] HERE IS WAIT FOR DN 0 (1 sec)
//17:33:04.587_M_I_01d9027e17ef8b38 [10:1f] pulse for one call
//17:33:05.976_M_I_ [10:17] STATOBJECT(06409ed0 208 2) tenant Resources name=AZvyagina_tsk@OBN_StatServerRouting.A: CHANGE OF STATE (CallRinging->CallInbound)
//    _M_I_ [10:17] STATOBJECT(06409ed0 208 2) tenant Resources name=AZvyagina_tsk@OBN_StatServerRouting.A: voice media status: 1 1 0
//    _M_I_ [10:17] -ready DN 1077 @ TMK_SIP_Switch (type 1 CallInbound time=1463063585) for agent AZvyagina_tsk, place Place_1077_TMK, WaitForNextCall time=1463063585
//    _M_I_ [10:17] STATOBJECT(06409ed0 208 2) tenant=Resources name=AZvyagina_tsk@OBN_StatServerRouting.A: 0(1) ready DNs reported, dT=0
//17:33:04.697_T_I_03350282382b5512 [14:02] sending event 85 for virtual queue OBN_IP_Skill1_VQ_OBN_SIP_Outbound_Virtual
    private static final Pattern regStrategyMessage = Pattern.compile("^\\s*_\\S_\\S_(\\S{16})\\s*\\[(\\S{2}:\\S{2})\\]\\s*");
    private static final Pattern regNonStrategyMessage = Pattern.compile("^\\s*_\\S_\\S_\\s*\\[(\\S{2}:\\S{2})\\]\\s*");
    private static final Pattern regRLibMessage = Pattern.compile("^\\s*RLIB: ");
    private static final Pattern regStartsFromSpace = Pattern.compile("^\\s+");
    private static final Pattern regTMessageOutFinal = Pattern.compile("^\\.\\.sent to");
    private static final Pattern regTMessageAttribute = Pattern.compile("^\t");
    private static final Pattern regTMessageInFinal = Pattern.compile("^(\\w.+is received for ts|\\.\\.sent to)");
    //    private static final Pattern regAgentStatusChanged = Pattern.compile("^_M_I_ \\[10:17\\]");
    private static final Pattern regWaitingCalls = Pattern.compile("^WAITING CALLS:\\s*");
    private static final Pattern regWaitingInit = Pattern.compile("_M_I_ \\[10:15\\]");
    private static final Pattern regGroupContent = Pattern.compile("CURRENT CONTENT\\(\\d+\\):");
    private static final Pattern regSCXMLContinue = Pattern.compile("^\\s*_\\S_\\S_(\\S{16})\\s*\\[09:04\\]\\s*");
    private static final Pattern regHTTPBridgeContinue = Pattern.compile("^\\s+");
    private static final Pattern regHTTPResponse = Pattern.compile("^HTTP");
    private static final Pattern regTargetInformation = Pattern.compile("^(============== Target|\\s)");
    private static final Pattern regRIResponse = Pattern.compile("Message: (.+)$");
    private static final Pattern regMediaStatus = Pattern.compile("^\\s+_M_I_ \\[(10:37|10:17)\\]");
    // WAITING CALLS: 0c5202aadb166aa9 0c5202aadb166aa9 0d4b02a63cbddec0
//15:49:45.068_R_I_ [19:10] routing interface request received: urs/call/00e2029a670222cb/func?name=FindConfigObject&params=%5B1%2C+%22name%3ASwitch_VyS%22%5D&sync=1, client=732(OR_Server), ref=674437
    private static final Pattern regRI = Pattern.compile("^_R_I_\\s*");
    private static final Pattern regCfgUpdate = Pattern.compile("^\\s+PopCfg.+ing$");
    private static final Pattern regCfgNotification = Pattern.compile("^Cfg\\w+=\\{");
    private static final Pattern regCfgUnknownOption = Pattern.compile("^\\s+_C_W_");
    //11:50:52.636_R_I_ [19:0f] connid 0150028acff24002 generated for client=23(OR_Server_811), ref id=5, method name=ORS: 0000GaBNKN2Q0021
    private static final Pattern regCONNID_UUID = Pattern.compile("connid ([\\w~]+) generated for client.+[\\* ]([\\w~]+)$");
    //    0083027d73c677aa, 05/23/16@17:54:52.974, RP(##TSERVER) -> @ [ 0 0 1 1], tms: 8418= (N)0 + (T)0 + (X)0 + (S)0 + (W)0 + (F)8418 + (R)0, trg:
    private static final Pattern regVitalik = Pattern.compile("^\\w{16},");
    //    DateParsed dp;
    private static final Pattern regTMessageToStart = Pattern.compile("^.*(send to ts|sending event|received from|request to)");
    private static final Pattern regTMessageOutFirstLine = Pattern.compile("^(request to|\\tAttribute|\\t\\()");
    private static final Pattern regReceived = Pattern.compile("^received from (\\d+)\\(([^\\)]+)\\).+message (Event\\w+)(?:\\(refid=(\\d+)\\))*");
    private static final Pattern regEventAttachedData = Pattern.compile("dn=([^,]+), refid=\\d+\\)$");
    private static final Pattern regSentTo = Pattern.compile("^send to ts (.+) (Request\\w+)");
    private static final Pattern regReqTo = Pattern.compile("^request to (\\d+).+message (Request\\w+)");
    private static final Pattern regStrategy = Pattern.compile("strategy:.+\\*+(\\S+)");
    //[14:33] strategy: **ORS (1085200831) is attached to the call
    private static final Pattern regRequestRef = Pattern.compile("request (\\d+) sent to");
    private static final Pattern regRequestURL = Pattern.compile("session/([^/]+)/(.+)$");
    private static final Pattern regWebResponse = Pattern.compile("^(^.+) is received from server.+ refid=(\\d+)");
    private static final Pattern regWebResponseBody = Pattern.compile("^HTTPBody \\{(.+)\\}$");
    private static final Pattern regWebNotifURL = Pattern.compile("^web notification <(.+)> ");
    private static final Pattern regCfgObjectName = Pattern.compile("(?:name|userName|number|loginCode)='([^']+)'");
    private static final Pattern regCfgObjectType = Pattern.compile("^Cfg([^=]+)=.*\\{DBID=(\\d+)");
    //    private static final Pattern regCfgAGType = Pattern.compile("^Cfg([^=]+)=\\{DBID=(\\d+)");
    private static final Pattern regCfgOp = Pattern.compile("PopCfg.+\\s(\\w+)$");
    private static final Pattern regCfgAgentGroup = Pattern.compile("^CfgAgentGroup=.+agentDBIDs=\\[([^\\]]+)");
    //09:24:35.821_M_I_ [10:15] SO(7f0021ca4cb8 -1 2) ten=Resources name=?BLD_Billing_AG:Chat_SK>0@hc1_statsrvr_urs_p.GA: content updated #503 <>
//CURRENT CONTENT(32): shilpa.c@hc1_statsrvr_urs_p.A,shruthi.p@hc1_statsrvr_urs_p.A...
    private static final Pattern regline1015 = Pattern.compile("name=(.+): content");
    private static final Pattern regCheckRoutingStates = Pattern.compile("^check call routing.+(true|false)$");
    final int MSG_STRING_LIMIT = 200;
    long m_CurrentFilePos;
    long m_HeaderOffset;
    ParserState m_ParserState;
    String m_Header;
    int m_dbRecords = 0;
    private String lastConnID;
    private Pattern MLPattern;
    private String URSFileID;
    private String URSRest;
    private Message msgTmp; // storage for message in progress
    private GenesysMsg genesysMsg;

    public UrsParser(DBTables  m_tables) {
        super(FileInfoType.type_URS, m_tables);

//17:33:06.335_I_I_03350282382b556f [07:07] HERE IS TARGETS
//TARGETS: OBN_IP_Skill1_Group@OBN_StatServerRouting.GA
    }

    private static HashMap<String, String> getTargetCodes() {
        HashMap<String, String> ret = new HashMap<>();
        ret.put("Q", "ACDQueue");
        ret.put("A", "Agent");
        ret.put("GA", "Agent Group");
        ret.put("C", "Campaign");
        ret.put("GC", "Campaign Group");
        ret.put("DL", "Destination Label");
        ret.put("DN", "DN");
        ret.put("AP", "Place");
        ret.put("GP", "Place Group");
        ret.put("GQ", "Queue Group");
        ret.put("RP", "Routing Point");
        return ret;
    }

    private void AddStrategyMessage(String FileLine, String URSRest1) {
        if (lastConnID == null) {
            Main.logger.error("Not added StrategyMessage -  connID is null; m_lineStarted:" + m_lineStarted);
        } else {
            URSStrategy msg = new URSStrategy(m_MessageContents, lastConnID, FileLine, URSRest1, fileInfo.getRecordID());
            SetStdFieldsAndAdd(msg);
//            try {
//                msg.setM_TimestampDP(getCurrentTimestamp());
//                msg.SetOffset(getSavedFilePos());
//                msg.SetFileBytes(getFilePos() - getSavedFilePos());
//                msg.SetLine(m_lineStarted);
//                msg.AddToDB(m_tables);
//            } catch (Exception e) {
//                Main.logger.error("Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);
//            }
        }
    }

    //17:42:51.895_I_I_00d202972e8c2ebc [01:08] call (252087-104e77cd0) deleting truly
//                addStep("01:08", null, "call deleting truly");
    private void AddStrategyMessageLine(String line, String FileLine) {
        Matcher m;
        Message msg;
        if ((FileLine.equals("14:33") || FileLine.equals("1B:01")) && (m = regStrategy.matcher(line)).find()) {
            msg = new URSStrategyInit(line, lastConnID, m.group(1), "strategy is attached to the call", fileInfo.getRecordID());
        } else if (FileLine.equals("01:08")) {
            msg = new URSStrategyInit(line, lastConnID, null, "call deleting truly", fileInfo.getRecordID());

        } else {
            msg = new URSStrategy(line, lastConnID, FileLine, URSRest, fileInfo.getRecordID());
        }
        try {
            msg.setM_TimestampDP(getCurrentTimestamp());
            msg.SetOffset(getFilePos());
            msg.SetFileBytes(getEndFilePos() - getFilePos());
            msg.SetLine(m_lineStarted);
            addToDB(msg);
        } catch (Exception e) {
            Main.logger.error("Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);
        }
    }

    private void AddURSStatMessage(String URSFileID, String URSRest1) throws Exception {
        URSStat msg = new URSStat(m_MessageContents, fileInfo.getRecordID());
        SetStdFieldsAndAdd(msg);
    }

    private void ProcessNonStrategyLine(String URSFileID, String substring) {
        Main.logger.trace("URSFileID: [" + URSFileID + "] substring: [" + substring + "]");
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void AddRLibMessage() throws Exception {
        URSRlib msg = new URSRlib(m_MessageContents, fileInfo.getRecordID());
        if (!msg.isInbound()) {
            String message = msg.getMessage();
            if (message != null && message.equals("RequestInvoke")) {
                Main.logger.debug("Ignored " + msg);
                return;
            }
        }
        SetStdFieldsAndAdd(msg);
    }

    private void AddConnIDUUID(String ConnID, String UUID) throws Exception {
        switch (UUID.length()) {
            case 32: {
                //SID in orchestration
                URSCONNIDSID msg = new URSCONNIDSID(ConnID, UUID, fileInfo.getRecordID());
                SetStdFieldsAndAdd(msg);
                break;
            }
//        SetStdFieldsAndAdd(msg);
            case 16: {
                //IxnID
                URSCONNIDIxnID msg = new URSCONNIDIxnID(ConnID, UUID, fileInfo.getRecordID());
                SetStdFieldsAndAdd(msg);
                break;
            }
            default:
                throw new Exception("Unknown type of IxnID: " + UUID);
        }
    }

    private boolean processHTTPBridgeRequest(String s) {
        URSRI msg = new URSRI(fileInfo.getRecordID());
        msg.setRefid(Message.getRx(m_MessageContents.get(0), regRequestRef, 1, null));
        String theUrl = Message.GetHeaderValue("URL", ':', m_MessageContents);
        if (theUrl != null) {
            Matcher m;
            if ((m = regRequestURL.matcher(theUrl)).find()) {
                msg.setORSSID(m.group(1));
                msg.setFunc(m.group(2));
            }
        }
        if (lastConnID != null) {
            msg.setConnID(lastConnID);
            lastConnID = null;
        }
        msg.SetInbound(false);
        SetStdFieldsAndAdd(msg);
        return true;
    }

    private boolean processWebResponse() {
        URSRI msg = new URSRI(fileInfo.getRecordID());
        Matcher m;
        if ((m = regWebResponse.matcher(m_MessageContents.get(0))).find()) {
            msg.setSubFunc(m.group(1));
            msg.setRefid(m.group(2));
        }
        String body = Message.FindByRx(m_MessageContents, regWebResponseBody, 1, (String) null);
        if (body == null || body.isEmpty()) {
            msg.setFunc(msg.getFunc());
        } else {
            msg.setFunc(body);
        }
        msg.setConnID(lastConnID);
        msg.SetInbound(true);
        SetStdFieldsAndAdd(msg);
        return true;
    }

    private boolean processWebNotification(String URSRest, String lastConnID) {
        URSRI msg = new URSRI(fileInfo.getRecordID());
        String theUrl = Message.getRx(URSRest, regWebNotifURL, 1, null);
        if (theUrl != null) {
            Matcher m;
            if ((m = regRequestURL.matcher(theUrl)).find()) {
                msg.setORSSID(m.group(1));
                msg.setFunc(m.group(2));
            }
        }
        msg.setConnID(lastConnID);
        msg.SetInbound(false);
        SetStdFieldsAndAdd(msg);
        return true;
    }

    private void ProcessRI(String s) throws Exception {
        Main.logger.trace("ProcessRI [" + s + "]");

        Matcher m;
        if ((m = regRIGenerated.matcher(s)).find()) {
            Main.logger.trace("regRIGenerated");
            URSRI msg = new URSRI(fileInfo.getRecordID());
            msg.setConnID(m.group(1));
            msg.setClientID(m.group(2));
            msg.setClientApp(m.group(3));
            msg.setORSSID(m.group(4));
            msg.SetInbound(true);
            SetStdFieldsAndAdd(msg);
        } else if ((m = regRIResp.matcher(s)).find()) {
            Main.logger.trace("regRIResp");
            msgTmp = new URSRI(fileInfo.getRecordID());
            ((URSRI) msgTmp).setFunc(m.group(1));
            ((URSRI) msgTmp).setClientID(m.group(2));
            ((URSRI) msgTmp).setClientApp(m.group(3));
            ((URSRI) msgTmp).setRefid(m.group(4));
            msgTmp.SetInbound(false);

            m_ParserState = ParserState.STATE_RI_RESPONSE;

        } else if (regRICallStart.matcher(s).find()) {
            m_MessageContents.clear();
            m_MessageContents.add(s);
            m_ParserState = ParserState.STATE_RI_CALLSTART;

        } else if ((m = regRIRequestStart.matcher(s)).find()) {
            Main.logger.trace("regRIRequestStart");
            URSRI msg = new URSRI(fileInfo.getRecordID());
            msg.setClientID(m.group(3));
            msg.setClientApp(m.group(4));
            msg.setRefid(m.group(5));
            String reqURIreq = m.group(1);
            String reqURI = m.group(2);
            msg.SetInbound(true);
            msg.setURIParts(reqURIreq, reqURI);

            SetStdFieldsAndAdd(msg);

        } else if ((m = regRIRequestShort.matcher(s)).find()) {
            Main.logger.trace("regRIRequestShort");
            URSRI msg = new URSRI(fileInfo.getRecordID());
            msg.setClientID(m.group(2));
            msg.setClientApp(m.group(3));
            msg.setRefid(m.group(4));
            String reqURIreq = m.group(1);
            msg.SetInbound(true);

            String[] split = StringUtils.split(reqURIreq, '/');
            switch (split.length) {
                case 4:
                    //ConnID specified
                    if (split[2].startsWith("@")) {
                        msg.setUUID(split[2].substring(1));
                    } else {
                        msg.setConnID(split[2]);
                    }
                    msg.setFunc(split[3]);
                    break;
                case 3:
                    msg.setFunc(split[2]);
                    break;
                case 5:
                    if (split[3].equals("func") || split[3].equals("reply")) {
                        msg.setFunc(split[4]);
                        String id = "";
                        if (split[2].startsWith("@")) {
                            msg.setUUID(split[2].substring(1));
                        }
                        break;
                    }

                default:
                    Main.logger.info("Strange URI [" + reqURIreq + "] l=" + split.length + " line " + m_CurrentLine);
                    break;
            }

            SetStdFieldsAndAdd(msg);
        }
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

            m_ParserState = ParserState.STATE_HEADER;

            String str;
            while ((str = input.readLine()) != null) {
                try {
                    int l = input.getLastBytesConsumed();
                    setEndFilePos(getFilePos() + l); // to calculate file bytes
                    m_CurrentLine++;
                    Main.logger.trace("**l: " + m_CurrentLine + " [" + str + "]");

                    while (str != null) {
                        str = ParseLine(str);
                    }
                    setFilePos(getFilePos() + l);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                    m_ParserState = ParserState.STATE_COMMENTS;
//                    throw e;
                }
//                m_CurrentFilePos += str.length()+input.charsSkippedOnReadLine;
            }
            ParseLine(""); // to complete the parsing of the last line/last message
        } catch (Exception e) {
            Main.logger.error(e);
        }


        return m_CurrentLine - line;
    }

    private String ParseLine(String str) throws Exception {
//        String trimmed = str.trim();
        Matcher m;
        String s = str;

        Main.logger.trace("Parser state " + m_ParserState);
        ParseCustom(str, 0);

        switch (m_ParserState) {
            case STATE_HEADER: {
                if (str.trim().isEmpty()) {
                    m_ParserState = ParserState.STATE_COMMENTS;
                }
            }
            break;

            case STATE_COMMENTS:
                m_lineStarted = m_CurrentLine;

                if (genesysMsg != null) {
                    if ((m = reIID.matcher(str)).find()) {
                        URSGenesysMsg msg = new URSGenesysMsg(genesysMsg, m.group(1), fileInfo.getRecordID());
                        addToDB(msg);
                        genesysMsg = null;
                        return null;
                    } else {
                        if (!genesysMsg.isToIgnore()) {
                            addToDB(genesysMsg);
                        }
                        genesysMsg = null;
                    }

                }

                try {
                    s = ParseTimestampStr(str);
                } catch (Exception exception) {
                    Exception e = exception;

                    throw exception; // do nothing so far. Can insert code to ignore certain exceptions
                }

                try {
                    genesysMsg = GenesysMsg.CheckGenesysMsg(dp, this, TableType.MsgURServer, null, false);
                    if (genesysMsg != null) {
                        genesysMsg.SetOffset(getOffset());
                        genesysMsg.SetLine(m_lineStarted);
                        return null;
                    }

                } catch (Exception exception) {
                    if (!(regVitalik.matcher(str)).find()) {
                        throw exception;
                    }
                }
                if (StringUtils.isBlank(s)) {
                    return null;
                }

                if ((regCfgUpdate.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_CONFIG;
                } else if ((m = regRI.matcher(s)).find()) {
                    ProcessRI(s.substring(m.end(0)));
                } else if ((m = regCONNID_UUID.matcher(s)).find()) {
                    AddConnIDUUID(m.group(1), m.group(2));
                } else if ((regRLibMessage.matcher(s)).find()) {
                    m_HeaderOffset = m_CurrentFilePos;
                    setSavedFilePos(getFilePos());
                    m_ParserState = ParserState.STATE_RLIB;
                    m_MessageContents.add(s);
                } else if (str.startsWith("received from")) {
                    m_Header = str;
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_TLIB_MESSAGE_IN;
                    m_MessageContents.add(str);
                    setSavedFilePos(getFilePos());
                } else if ((m = regNonStrategyMessage.matcher(s)).find()) {
                    URSFileID = m.group(1);
                    URSRest = s.substring(m.end(0));

//    _M_I_ [17:0f] VQ dff6b40 [at all 12 0 0] 1 Target(s), flag=80000800a, guid: 0Resources||1|d-1|1|00||||||01StatTimeInReadyState|00{}{}[]transfer-BillPay@urs-voice-statserver-81-1.RP                        
                    if (URSFileID.equals("17:0f")) {
                        processGUID(URSRest, lastConnID);
                    } else if (URSFileID.equals("10:3e")) {
//09:27:52.405_M_I_ [10:3e] SO(7f0022462750 1509 2) ten=Resources name=ajimenezmoreno@hc1_statsrvr_urs_p.A: verification mode - 0 sec
//09:27:52.405_M_I_ [10:15] check queue for SO <ajimenezmoreno> type <Agent> stat <##state> (13 trgs 0 cpu)
//WAITING CALLS:
                        setSavedFilePos(getFilePos());
                        m_MessageContents.add(s.substring(m.end()));
                        m_ParserState = ParserState.STATE_WAITINGCALLSSTART;
                    } else if (URSFileID.equals("10:15")) {
                        setSavedFilePos(getFilePos());
                        m_MessageContents.add(s.substring(m.end()));
                        m_ParserState = ParserState.smUpdateObject;
                    } else if (URSFileID.equals("10:17")) {
                        setSavedFilePos(getFilePos());
                        m_MessageContents.add(s.substring(m.end()));
                        m_ParserState = ParserState.STATEAGENTSTATUSNEW;
                    } else if (URSFileID.equals("08:11")) {
                        setSavedFilePos(getFilePos());
                        m_MessageContents.add(s.substring(m.end()));
                        m_ParserState = ParserState.HTTP_BRIDGE;
                    } else {
                        ProcessNonStrategyLine(URSFileID, URSRest);
                    }
                } else if ((m = regStrategyMessage.matcher(s)).find()) {
                    lastConnID = m.group(1);
                    URSFileID = m.group(2);
                    URSRest = s.substring(m.end());
                    Main.logger.trace("l " + m_CurrentLine + ": regStrategyMessage lastConnID[" + lastConnID + "] URSFileID[" + URSFileID + "] URSRest[" + URSRest + "]");

                    if (URSFileID.equals("07:07") || URSFileID.equals("07:26") || (URSFileID.equals("07:57") && URSRest.startsWith("HERE"))) {
                        setSavedFilePos(getFilePos());
                        m_MessageContents.add(URSRest);
                        m_ParserState = ParserState.STATE_STRATEGY_TARGETS;
                        return null;
                    } else if (URSFileID.equals("09:05")) {
                        setSavedFilePos(getFilePos());
                        m_MessageContents.add(URSRest);
                        m_ParserState = ParserState.STATE_STRATEGY_SCXML;
                        return null;
                    } else if (URSFileID.equals("17:0f")) {
                        processGUID(URSRest, lastConnID);
                    } else if (URSFileID.equals("0E:22")) {
                        if (URSRest.contains("web notification <> cleared")) {
                            AddStrategyMessageLine(s, URSFileID);
                        } else {
                            processWebNotification(URSRest, lastConnID);
                        }
                    } else if (URSFileID.equals("08:08")) {
                        setSavedFilePos(getFilePos());
                        m_MessageContents.add(URSRest);
                        m_ParserState = ParserState.STATE_HTTP_RESPONSE;
                    } else if (URSFileID.equals("0E:19")) {
                        setSavedFilePos(getFilePos());
                        m_MessageContents.add(URSRest);
                        m_ParserState = ParserState.STATE_ROUTING_TARGET;
                    } else if (URSFileID.equals("14:19")) {
                        setSavedFilePos(getFilePos());
                        m_MessageContents.add(URSRest);
                        m_ParserState = ParserState.STATE_TLIB_MESSAGE_OUT;
                    } else {
                        AddStrategyMessageLine(s, URSFileID);
                    }
                } else if ((regTMessageToStart.matcher(s)).find()) {
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_TLIB_MESSAGE_OUT;
                    m_MessageContents.add(str);
                    setSavedFilePos(getFilePos());
                }
                break;

            case STATE_RI_RESPONSE: {
                if ((m = regRIResponse.matcher(str)).find()) {
                    ((URSRI) msgTmp).setSubFunc(m.group(1));
                }
                SetStdFieldsAndAdd(msgTmp);
                msgTmp = null;
                m_ParserState = ParserState.STATE_COMMENTS;
                break;
            }

            case STATE_ROUTING_TARGET: {
                if ((regTargetInformation.matcher(str)).find()) {
                    addStrategyTarget(m_MessageContents);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                } else {
                    m_MessageContents.add(str);
                }
                break;

            }

            case STATE_HTTP_RESPONSE: {
                if ((regHTTPResponse.matcher(str)).find()) {
                    m_MessageContents.add(str);
                } else {
                    processWebResponse();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;

            }

            case HTTP_BRIDGE: {
                if ((m = regHTTPBridgeContinue.matcher(str)).find()) {
                    m_MessageContents.add(str.substring(m.end()));
                } else {
                    processHTTPBridgeRequest(str);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;
            }

            case STATE_STRATEGY_SCXML: {
                if ((m = regSCXMLContinue.matcher(str)).find()) {
                    m_MessageContents.add(str.substring(m.end()));
                } else {
                    AddStrategySCXMLMessage(URSFileID);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;
            }

            case STATE_STRATEGY_TARGETS: {
                if (str.startsWith("TARGETS")) {
                    m_MessageContents.add(str);
                    AddStrategyMessage(URSFileID, str);
                    str = null;
                } else {
                    Main.logger.trace("l " + m_CurrentLine + ": Unexpected line in state " + m_ParserState + ": " + str + "; retry");
                }
                m_ParserState = ParserState.STATE_COMMENTS;
                m_MessageContents.clear();
                return str;
            }

            case smUpdateObject: {
                if ((m = regGroupContent.matcher(str)).find()) {
                    groupUpdated(str.substring(m.end()), m_MessageContents.get(0));
                    str = null;
                } else {
                    Main.logger.trace("l " + m_CurrentLine + ": Unexpected line in state " + m_ParserState + ": " + str + "; retry");
                }
                m_MessageContents.clear();
                m_ParserState = ParserState.STATE_COMMENTS;
                return str;
            }

            case STATE_WAITINGCALLSSTART: {
                if ((m = regWaitingInit.matcher(str)).find()) {
                    m_MessageContents.add(str.substring(m.end()));
                    m_ParserState = ParserState.STATE_WAITINGCALLS;
                    str = null;
                } else {
                    Main.logger.debug("l " + m_CurrentLine + ": Unexpected line in state " + m_ParserState + ": " + str + "; retry");
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                }
                return str;
            }

            case STATE_WAITINGCALLS: {
                if ((m = regWaitingCalls.matcher(str)).find()) {
                    processWaitingCalls(m_MessageContents.get(1), StringUtils.split(s.substring(m.end())));
                    str = null;
                } else {
                    Main.logger.debug("l " + m_CurrentLine + ": Unexpected line in state " + m_ParserState + ": " + str + "; retry");
                }

                m_ParserState = ParserState.STATE_COMMENTS;
                m_MessageContents.clear();
                return str;

            }
            case STATEAGENTSTATUSNEW: {
                if ((m = regMediaStatus.matcher(str)).find()) {
                    m_MessageContents.add(str.substring(m.end(0)));
                }
                if (str != null && str.contains("[0E:25]")) {
//10:29:54.174_M_I_0000000000000000 [0E:25] SO(1063a730 3465 2) ten=Apollo-Group-UCP name=lamundso@rp_statserver_routing_1.A: vcb notification timout: 0 (+0)
                    m_MessageContents.add(str);

                } else {
                    AddURSStatMessage(URSFileID, URSRest);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
            }
            break;

//            case STATEAGENTSTATUSNEW: {
//                if ((m = regMediaStatus.matcher(str)).find()) {
//                    m_MessageContents.add(str.substring(m.end()));
//                    str = null;
//                } else if ((m = regWaitingCalls.matcher(str)).find()) {
//                    processWaitingCalls((String) m_MessageContents.get(0), StringUtils.split(s.substring(m.end())));
//                    str = null;
//                    m_ParserState = ParserState.STATE_COMMENTS;
//                    m_MessageContents.clear();
//                } else {
//                    Main.logger.error("l " + m_CurrentLine + ": Unexpected line in state " + m_ParserState + ": " + str);
//                    m_ParserState = ParserState.STATE_COMMENTS;
//                    m_MessageContents.clear();
//                }
//                return str;
//            }
            case STATE_RI_CALLSTART: {
                if ((m = regRIConnIDGenerated.matcher(str)).find()) {
                    URSRI msg = new URSRI(fileInfo.getRecordID());
                    msg.setFunc(m.group(5));
                    msg.setClientID(m.group(2));
                    msg.setClientApp(m.group(3));
                    msg.setRefid(m.group(4));
                    msg.setConnID(m.group(1));
                    msg.SetInbound(true);
                    if ((m = regRIRequestStart.matcher(m_MessageContents.get(0))).find()) {
                        String reqURI = m.group(2);
                        msg.setORSSID(getQueryKey(splitQuery(reqURI), "ORS_SESSION_ID"));
                    } else if ((m = regRIRequestShort1.matcher(m_MessageContents.get(0))).find()) {
                        String reqURI = m.group(1);
                        msg.setSubFunc(reqURI);

                    } else {
                        Main.logger.error("l:" + m_CurrentLine + "- Expected first line: [" + m_MessageContents.get(0) + "] not parsed");
                    }
                    SetStdFieldsAndAdd(msg);
                    m_ParserState = ParserState.STATE_COMMENTS;
                } else {
                    Main.logger.error("Unexpected line: [" + str + "] after\n" + m_MessageContents.get(0));
                }
                m_ParserState = ParserState.STATE_COMMENTS;
            }
            break;

            case STATE_CONFIG:
                if ((regCfgUnknownOption.matcher(str)).find()) {
//    _C_W_ [0D:07] unknown option 'Loaded' for server 'urs_ursvr_81_ad_dev'
                    break; // ignoring 

                } else if ((regCfgNotification.matcher(str)).find()) {
                    m_MessageContents.add(str);
                    AddConfigMessage(m_MessageContents);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                } else {
                    Main.logger.error("l:" + m_CurrentLine + ", state:" + m_ParserState.toString() + "- unexpected line [" + str + "]");
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                }
                break;

            case STATE_STRATEGY_MULTILINE:
                if ((MLPattern.matcher(str)).find()) {
                    m_MessageContents.add(str);
                } else {
                    AddStrategyMessage(URSFileID, URSRest);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;

            case STATE_RLIB:
                if ((regStartsFromSpace.matcher(str)).find()) {
                    m_MessageContents.add(str);
                } else {
                    AddRLibMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;

            case STATE_TLIB_MESSAGE_IN:
                boolean endFound = false;
                if ((regTMessageInFinal.matcher(str)).find() || str.contains("[14:32]")) {
                    m_MessageContents.add(str);
                    endFound = true;
                } else if (regTMessageAttribute.matcher(str).find()) {
                    m_MessageContents.add(str);
                    return null;
                }
                AddUrsMessage(m_MessageContents, null, null);
                m_ParserState = ParserState.STATE_COMMENTS;
                m_MessageContents.clear();
                m_Header = null;

                if (!endFound) {
                    return str;
                }
                break;

            case STATE_TLIB_MESSAGE_OUT:
                if ((regTMessageOutFinal.matcher(str)).find()) {
                    AddUrsMessage(m_MessageContents, null, str);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                    m_Header = null;
//                    ParseLine(str);
                } else {
                    // oh Vitalik...
//01:54:29.226_T_I_10630289b590f7ae [14:02] sending event 58 for vq vq_MB_Backoffice_Inquiries (9601 0 1 1470815342 28185 50)
//01:54:29.226_M_I_10630289b590f7ae [13:03] call (vq 0000000014a50c20, id=1758045, priority 58) doesn't wait for VQ 000000000d765070 (name="vq_MB_Backoffice_Inquiries" stats=15 281824 10) now (10)
                    boolean endMsg = false;
                    if (m_MessageContents.isEmpty()) {//filter out TEvent without attributes
                        if (str != null && str.length() > 0
                                && !regTMessageOutFirstLine.matcher(str).find()) {
                            endMsg = true;
                        }
                    }
                    if (endMsg) {
                        m_ParserState = ParserState.STATE_COMMENTS;
                        return str;
                    } else {
                        m_MessageContents.add(str);
                    }
                }
                break;

        }

        return null;
    }

    protected void AddUrsMessage(ArrayList<String> contents, String header, String urs_message) throws Exception {
        Matcher m;
        String server = null;
        String event = null;
        String fileHandle = null;
        String refID = null;
        boolean isInbound = false;
        String thisDN = null;

        Main.logger.trace("addURS: " + ((contents == null) ? "null" : contents.toString()) + " h:" + ((header == null) ? "null" : header));
        if (header != null && (m = regReceived.matcher(header)).find()) {
            fileHandle = m.group(1);
            server = m.group(2);
            event = m.group(3);
            refID = m.group(4);
//            dp = ParseFormatDate(urs_message);
            isInbound = true;
        } else {
            if (!contents.isEmpty()) {

                if (((m = regReceived.matcher(contents.get(0))).find())) {
                    fileHandle = m.group(1);
                    server = m.group(2);
                    event = m.group(3);
                    refID = m.group(4);
                    if (contents.size() > 1 && (m = regEventAttachedData.matcher(contents.get(1))).find()) {
                        thisDN = m.group(1);
                    }
                    isInbound = true;

                } else if ((m = regReqTo.matcher(contents.get(0))).find()) {
                    fileHandle = m.group(1);
                    event = m.group(2);
                    Main.logger.trace("1event [" + event + "] fileHandle:" + fileHandle);
                } else if ((m = regSentTo.matcher(contents.get(0))).find()) {
                    server = m.group(1);
                    event = m.group(2);
                    Main.logger.trace("2event [" + event + "] fileHandle:" + fileHandle);
                }

            }
        }

        UrsMessage msg = new UrsMessage(event, server, fileHandle, refID, contents, fileInfo.getRecordID());

        msg.SetInbound(isInbound);
        msg.setRefID(refID);
        msg.setThisDN(thisDN);

        SetStdFieldsAndAdd(msg);

        m_dbRecords++;
    }

    private void AddConfigMessage(ArrayList<String> m_MessageContents) {
        ConfigUpdateRecord msg = new ConfigUpdateRecord(m_MessageContents, fileInfo.getRecordID());
        try {
            Matcher m;
            msg.setObjName(Message.FindByRx(m_MessageContents, regCfgObjectName, 1, ""));
            msg.setOp(Message.FindByRx(m_MessageContents, regCfgOp, 1, ""));

            if ((m = Message.FindMatcher(m_MessageContents, regCfgObjectType)) != null) {
                msg.setObjectType(m.group(1));
                msg.setObjectDBID(m.group(2));
            }
//            else             if ((m = Message.FindMatcher(m_MessageContents, regCfgAGType)) != null) {
//                msg.setObjectType(m.group(1));
//                msg.setObjectDBID(m.group(2));
//            }

            SetStdFieldsAndAdd(msg);
            Main.logger.trace("new ID: " + msg.getRecordID());
            if ((m = Message.FindMatcher(m_MessageContents, regCfgAgentGroup)) != null) {
                String[] split = StringUtils.split(m.group(1), ',');
                if (split != null && split.length > 0) {
                    URSAgentGroup ag = new URSAgentGroup(msg.getRecordID(), fileInfo.getRecordID());
                    for (String string : split) {
                        ag.setAgentDBID(string);
                        addToDB(ag);
                    }
                }
            }
        } catch (NumberFormatException e) {
            Main.logger.error("Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);
        }
    }

    private void processWaitingCalls(String ag, String[] split) {
        HashSet<String> hs = new HashSet();

        hs.addAll(Arrays.asList(split)); //hashset would guarantee unique connIDs if multiple target blocks hit
        for (String h : hs) {
            URSWaiting msg = new URSWaiting(ag, h, fileInfo.getRecordID());
            SetStdFieldsAndAdd(msg);

        }
    }

    private void processGUID(String GUIDMsg, String theConnID) {
        if (lastConnID == null) {
            Main.logger.error("m_lineStarted:" + m_lineStarted + " - processGUID (empty ConnID; message ignored)");
        } else {
            URSVQ msg = new URSVQ(fileInfo.getRecordID());
            msg.setConnID(theConnID);
            msg.setGUIDMsg(GUIDMsg);
            SetStdFieldsAndAdd(msg);

        }
    }

    private void groupUpdated(String agentList, String line1015) {
        Matcher m;
        if ((m = regline1015.matcher(line1015)).find()) {
            String[] agents = StringUtils.split(agentList, ',');
            String target = m.group(1);
            if (agents != null && agents.length > 0) {
                for (String agent : agents) {
                    URSTargetSet msg = new URSTargetSet(target, agent, fileInfo.getRecordID());
                    SetStdFieldsAndAdd(msg);
                }
            } else {
                URSTargetSet msg = new URSTargetSet(target, fileInfo.getRecordID());
                SetStdFieldsAndAdd(msg);
                Main.logger.trace("l: " + m_CurrentLine + " groupUpdated [" + line1015 + "] empty agent list");
            }
        } else {
            Main.logger.error("l: " + m_CurrentLine + " groupUpdated [" + line1015 + "] is not matched");
        }
    }

    private void AddStrategySCXMLMessage(String FileLine) {
        Matcher m;
        URSStrategy msg;
        msg = new URSStrategy(lastConnID, FileLine, m_MessageContents, fileInfo.getRecordID());
        msg.parseSCXMLAttributes();
        SetStdFieldsAndAdd(msg);

    }

    private void addStrategyTarget(ArrayList<String> contents) {
        URSStrategy msg = null;
        if (lastConnID == null) {
            Main.logger.error("Not added StrategyMessage -  connID is null; m_lineStarted:" + m_lineStarted);
        } else {
            msg = new URSStrategy(lastConnID, URSFileID, m_MessageContents, fileInfo.getRecordID());
        }

        if (!contents.isEmpty()) {
            Matcher m;

            if (((m = regCheckRoutingStates.matcher(contents.get(0))).find())) {
                if (m.group(1).startsWith("t")) {//can route; get target
                    if (msg != null) {
                        msg.parseTarget();
                    }
                }
            } else {
                Main.logger.error("l:" + m_lineStarted + " not regCheckRoutingStates; " + contents);
                msg = null;
            }

        }
        if (msg != null) {
            SetStdFieldsAndAdd(msg);
        } else {
            Main.logger.error("l:" + m_lineStarted + " addStrategyTarget not created " + contents);
        }
    }

    @Override
    void init(DBTables  m_tables) {
        m_tables.put(TableType.URSAgentGroupTable.toString(), new URSAgentGroupTable(Main.getInstance().getM_accessor(), TableType.URSAgentGroupTable));
        m_tables.put(TableType.URSGenesysMessage.toString(), new GenesysUrsMsgTable(Main.getInstance().getM_accessor(), TableType.URSGenesysMessage));
    }

    // parse state contants
    enum ParserState {

        STATE_HEADER,
        STATE_TLIB_MESSAGE_IN,
        STATE_STRATEGY_MULTILINE,
        STATE_NONSTRATEGY_MULTILINE,
        STATE_TLIB_MESSAGE_OUT,
        STATE_COMMENTS,
        STATE_RLIB,
        STATE_CONFIG,
        STATE_RI_CALLSTART,
        STATE_WAITINGCALLS,
        STATEAGENTSTATUSNEW,
        STATE_STRATEGY_TARGETS,
        STATE_WAITINGCALLSSTART,
        smUpdateObject,
        STATE_STRATEGY_SCXML,
        HTTP_BRIDGE,
        STATE_HTTP_RESPONSE,
        STATE_ROUTING_TARGET,
        STATE_RI_RESPONSE
    }

    //    public static void main(String args[]) {
//        String[] agents = StringUtils.split("Lori McLaren@stat_server_omaha.A,Bre'anna King@stat_server_omaha.A,Jamie Irvin@stat_server_omaha.A,Howie Brewer@stat_server_omaha.A,Sierra Godinez@stat_server_omaha.A,Beth Swank@stat_server_omaha.A,Paula Shearer@stat_server_omaha.A,Kendra McCallister@stat_server_omaha.A,Cass Hackett@stat_server_omaha.A,Laura Bautista-Marquez@stat_server_omaha.A,Katrina Johnsen@stat_server_omaha.A,Chasidy Kirksey@stat_server_omaha.A,Gabe Knudsen@stat_server_omaha.A,Meagan Anderson@stat_server_omaha.A,Renee Mickens@stat_server_omaha.A,Mindy McGuire@stat_server_omaha.A,Noelle Clarke@stat_server_omaha.A,Shari Roberts@stat_server_omaha.A,Logan Adams@stat_server_omaha.A,Monica Ridout@stat_server_omaha.A,Robert Rice@stat_server_omaha.A,Luke Van Hemert@stat_server_omaha.A,Valerie Butler@stat_server_omaha.A,Erica Smith-Phalen@stat_server_omaha.A,Bonnie Caulder@stat_server_omaha.A,Becka McClusky@stat_server_omaha.A,Daniel Dever Sr@stat_server_omaha.A,Donna Burch@stat_server_omaha.A,Tamie Grzebielski@stat_server_omaha.A,Terrohn Falkner@stat_server_omaha.A,Cynthia Johnson@stat_server_omaha.A,Daryl Mountain@stat_server_omaha.A,Keith Lyle@stat_server_omaha.A,Samantha Brown@stat_server_omaha.A,JaTaYa Johnson@stat_server_omaha.A,Meredith Williams@stat_server_omaha.A,Datrina Cain@stat_server_omaha.A,Julius Sanders@stat_server_omaha.A,Alex Maycher@stat_server_omaha.A,Tim Sobbing@stat_server_omaha.A,Jasmine Butler@stat_server_omaha.A,Dwaynda Mayfield@stat_server_omaha.A,Debra Graveman@stat_server_omaha.A,Joana Rodriguez@stat_server_omaha.A,Marquez Brewer@stat_server_omaha.A,Deborah Lewis@stat_server_omaha.A,Kyle Ging@stat_server_omaha.A,Vanessa Eiras@stat_server_omaha.A,Paulette Logan@stat_server_omaha.A,Isabella Lopez@stat_server_omaha.A,Sykeyah Bass@stat_server_omaha.A,Angelica Sandoval@stat_server_omaha.A,Shannon Crawford@stat_server_omaha.A,Brittney Rockwell@stat_server_omaha.A,Uniquea Davis@stat_server_omaha.A,Amanda Janecek@stat_server_omaha.A,Tia Booker@stat_server_omaha.A,Ziyadah Beacham@stat_server_omaha.A,Letisha Marion@stat_server_omaha.A,Heather Baker@stat_server_omaha.A,Amie Wickman@stat_server_omaha.A,Kylynn Wagner@stat_server_omaha.A,Jade Havlicek@stat_server_omaha.A,Sherry Kaiser@stat_server_omaha.A,Lauren Conway@stat_server_omaha.A,Stacy Schultz@stat_server_omaha.A,Rubi Soto@stat_server_omaha.A,Denys Workman@stat_server_omaha.A,Shanda Hahn Kinkade@stat_server_omaha.A,Tyuanna Thomas@stat_server_omaha.A,Salesia Domina@stat_server_omaha.A,Suzannah Simmons@stat_server_omaha.A,Tionna Andrews@stat_server_omaha.A,Robert Moore@stat_server_omaha.A,Alexandria Phillips@stat_server_omaha.A,Phyllis Gibson@stat_server_omaha.A,Matt Allen@stat_server_omaha.A,Colandra Cooper@stat_server_omaha.A,Yanelh Garcia-Madrid@stat_server_omaha.A,Kerry Foland@stat_server_omaha.A,Monterey Scott (bta91589)@stat_server_omaha.A,Keyane Woodruff@stat_server_omaha.A,Bonnie.Bell@stat_server_omaha.A,Naymond Coss@stat_server_omaha.A,Jason Glynn@stat_server_omaha.A,Emily Phillips@stat_server_omaha.A,Shannon Boatwright@stat_server_omaha.A,Sonda Garton@stat_server_omaha.A,Sharon_Fouquier-Simpson@stat_server_omaha.A,Marcus Parrish@stat_server_omaha.A,Marcus Brown@stat_server_omaha.A,Ashley Winchester@stat_server_omaha.A,Christopher Aragon@stat_server_omaha.A,Zach Sanko@stat_server_omaha.A,Tia Boatwright@stat_server_omaha.A,John Bashonski@stat_server_omaha.A,Debbie Lemr@stat_server_omaha.A,Raul Banderas@stat_server_omaha.A,Tay Kent@stat_server_omaha.A,Lucas Nielsen@stat_server_omaha.A,Maria Suarez Bejar@stat_server_omaha.A,Cindy Maitland@stat_server_omaha.A,Tanya Stewart-Ford@stat_server_omaha.A,Rebecca Pearson@stat_server_omaha.A,Jania White@stat_server_omaha.A,Matt Johnson@stat_server_omaha.A,Danielle Kempkes@stat_server_omaha.A,Miranda Yablonski@stat_server_omaha.A,Sam Wendland@stat_server_omaha.A,Lopez, Alan@stat_server_omaha.A,Gerardo Martinez@stat_server_omaha.A,Hannah Kreitzinger@stat_server_omaha.A,Khambrel Crawford@stat_server_omaha.A,Nakeeta Murray@stat_server_omaha.A,Cachet Irvin@stat_server_omaha.A,Cornell Booth Sr@stat_server_omaha.A,Anna Edmonds@stat_server_omaha.A,Darlene Murry@stat_server_omaha.A,Olivia Bradley@stat_server_omaha.A,Justina Cummings@stat_server_omaha.A,Webb, Paul@stat_server_omaha.A,Michelle Maske@stat_server_omaha.A,Chrissy Willingham@stat_server_omaha.A,Raven Welchel@stat_server_omaha.A,Hunter Morrell@stat_server_omaha.A,Dedra Smith@stat_server_omaha.A,Katris Hayes@stat_server_omaha.A,Nicole Murchison@stat_server_omaha.A,Michael Herod@stat_server_omaha.A,Mikayla Finley@stat_server_omaha.A,Sam Swanson@stat_server_omaha.A,Jerome Dismuke II@stat_server_omaha.A,Alex Stuart@stat_server_omaha.A,Andrew Ryder@stat_server_omaha.A,Kristy Helm@stat_server_omaha.A,Karen Roy@stat_server_omaha.A,Stewart, Joy@stat_server_omaha.A,Bj Bunten@stat_server_omaha.A", ',');
//        for (String agent : agents) {
//            System.out.println(agent);
//        }
//    }
    private class URSAgentGroup extends Message {

        private final int updateID;
        private int agentDBID;

        private URSAgentGroup(int lastID, int fileID) {
            super(TableType.URSAgentGroupTable, fileID);
            this.updateID = lastID;
        }

        private int getUpdateID() {
            return updateID;
        }

        private int getAgentDBID() {
            return agentDBID;
        }

        private void setAgentDBID(String string) throws NumberFormatException {
            this.agentDBID = Integer.parseInt(string);
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setInt(1, getRecordID());
            stmt.setInt(2, getFileID());

            stmt.setInt(3, getUpdateID());
            stmt.setInt(4, getAgentDBID());
            return true;
        }
    }

    private class URSAgentGroupTable extends DBTable {

        public URSAgentGroupTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t, "ursAgentGroupAgent");
        }

        @Override
        public void InitDB() {

            addIndex("updateId");
            addIndex("agentDBID");
            addIndex("FileId");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",FileId INTEGER"
                    + ",updateId INTEGER"
                    + ",agentDBID INTEGER"
                    + ");";
            getM_dbAccessor().runQuery(query);

        }

        @Override
        public String getInsert() {
            return "INSERT INTO " + getTabName() + " VALUES(?"
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
        public void FinalizeDB() throws Exception {
            createIndexes();
        }


    }

    private class ObjectDBID extends Message {

        String objectType;
        String objectName;
        private int objectDBID;

        private ObjectDBID(int objectDBID, String objectType, String objectName, int fileID) {
            super(TableType.ObjectDBID, fileID);
        }

        private int getObjectDBID() {
            return objectDBID;
        }

        private void setAgentDBID(String string) throws NumberFormatException {
            this.objectDBID = Integer.parseInt(string);
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setInt(1, getRecordID());
            return true;

        }
    }

    private class ObjectDBIDTable extends DBTable {

        public ObjectDBIDTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t, "objectDBID");
        }

        @Override
        public void InitDB() {

            addIndex("updateId");
            addIndex("agentDBID");
            addIndex("FileId");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",FileId INTEGER"
                    + ",objTypeId INTEGER"
                    + ",objNameID INTEGER"
                    + ",objDBID INTEGER"
                    + ");";
            getM_dbAccessor().runQuery(query);

        }

        @Override
        public String getInsert() {
            return "INSERT INTO " + getTabName() + " VALUES(?"
                    /*standard first*/
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
        public void FinalizeDB() throws Exception {
            createIndexes();
        }


    }

    private class URSGenesysMsg extends Message {

        private final String connID;
        private final GenesysMsg msg;

        private URSGenesysMsg(GenesysMsg msg, String connID, int fileID) {
            super(TableType.URSGenesysMessage, fileID);
            this.msg = msg;
            this.connID = connID;
        }

        public String getConnID() {
            return connID;
        }

        public GenesysMsg getMsg() {
            return msg;
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(getMsg().GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getMsg().getM_fileOffset());
            stmt.setLong(4, getMsg().getFileBytes());
            stmt.setLong(5, getMsg().getM_line());

            stmt.setInt(6, getMsg().getLevel());
            stmt.setInt(7, getMsg().getMsgID());
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.ConnID, getConnID()));

            return true;

        }
    }

    public class GenesysUrsMsgTable extends DBTable {

        public GenesysUrsMsgTable(SqliteAccessor dbaccessor, TableType type) {
            super(dbaccessor, type, type.toString());
        }

        private String tabName() {
            return getM_type().toString();
        }

        @Override
        public void InitDB() {

            addIndex("time");
            addIndex("FileId");
            addIndex("levelID");
            addIndex("MSGID");
            addIndex("connIDID");

            dropIndexes();

            String query = "create table if not exists " + tabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",levelID int2"
                    + ",MSGID INTEGER"
                    + ",connIDID INTEGER"
                    + ");";
            getM_dbAccessor().runQuery(query);

        }

        @Override
        public String getInsert() {
            return "INSERT INTO  " + tabName() + " VALUES(NULL,?,?,?,?,?"
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
        public void FinalizeDB() throws Exception {
            getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_fileid;");
            getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_unixtime;");
            getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_levelID;");
            getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_MSGID;");
        }


    }

}
