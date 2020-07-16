/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static Utils.Util.intOrDef;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ssydoruk
 */
public class OCSParser extends Parser {

    private static final Pattern regNotParseMessage = Pattern.compile("^(50002|50071|04541|04542|61007|50080"
            + ")");
    private static final Pattern regStatEvtContinue = Pattern.compile("^(\\s+|pMediaCapacity)");
    private static final Pattern regCampAssignment = Pattern.compile("^(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) ");

    // general buffer 
    private static final char[] m_CharBuf = new char[1024];

    //14:20:55.847 Trc 52000 Campaign Group SIPProgressive@VAG_English has been loaded
    private static final Pattern regDate = Pattern.compile("^(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) ");

//52000|TRACE|52000|Campaign Group %s has been loaded
//52001|TRACE|52001|Campaign Group %s has been started
//52002|TRACE|52002|Campaign Group %s has been stopped
//52003|TRACE|52003|Campaign Group %s has been force unloaded
//52004|TRACE|52004|Campaign Group %s has been unloaded
    private static final Pattern regCGState = Pattern.compile("^Trc 5200[0-9] Campaign Group (.+) has been (.+)$");

//16:18:52.794 CampaignGroup(180) (CAMP_DORIVETTES@VAG_CAMP_DORIVETTES): CampGrMsg: OCS version: 8.1.500.18;  WorkTime: 24386;
//        static Pattern regCGStart= Pattern.compile("CampaignGroup\\(([0-9]+)\\) \\(([^\\)]+)\\): CampGrMsg: OCS version: ([^;]+); WorkTime: ([^;]+);");
    private static final Pattern regCGStart = Pattern.compile("^\\s*CampaignGroup\\(");

//        16:18:54.159 PA Session Info:
    private static final Pattern regPAStart = Pattern.compile("^\\s*PA Session Info:");
    private static final Pattern regPAInfo = Pattern.compile("Predictive Info for OwnerDBID: (\\d+)  - Density: ([^;]+); a: ([^;]+); PredTime: ([^;]+); Remainder: ([^;]+); SentCalls: (\\d+);");

    private static final Pattern regRecTreatment = Pattern.compile("^RecTreatment - Apply");

//        16:19:05.382 PA Agent Info:
    private static final Pattern regPAAgentInfo = Pattern.compile("^\\s*PA Agent Info:");

//18:04:00.857 PAEventInfo (ContactInfo):
    private static final Pattern regPAEventInfo = Pattern.compile("^\\s*PAEventInfo ");

//16:59:20.250 TOTAL SCHEDULED AGENTS/PLACES ASSIGNMENT:
    private static final Pattern regAgentAssignment = Pattern.compile("\\s*TOTAL SCHEDULED AGENTS");

//There are 27 Agents/Places assigned to Session 'OBN_IP_EBlock_Campaign@OBN_IP_EBlock_Group'[268] OCS_Session_Loaded | OCS_Stat_Opened:
    private static final Pattern regCampaignAssignment = Pattern.compile("\\s*There are \\d+ Agents/Places assigned to Session");

//16:19:05.382 STAT_EVENT SEventCurrentTargetState_TargetUpdated was received for Campaign Sessions: 137 174
    private static final Pattern regStatEvent = Pattern.compile("\\s*STAT_EVENT (\\S+) ");
    private static final Pattern regRecordCreate = Pattern.compile("^DBList.CreateRecord");
    private static final Pattern regRecordDelete = Pattern.compile("^Call\\[(\\d+):[^\\]]+]::Deleting call");

//IxNServer 'ds12_ncclt_ixn_ocs_01' - SendRequest : 'request_submit' (101) message:
//IxNServer 'ds12_ncclt_ixn_ocs_01' - EventReceived : 'event_ack' (125) message:
    private static final Pattern regIxnMsg = Pattern.compile("^IxNServer ");
    private static final Pattern regHTTPMessage = Pattern.compile("^HTTPProxyClient");
    private static final Pattern regClient = Pattern.compile("^(\\s*OCMClient|Thirdparty, processing)");
    private static final Pattern regRecCreateContinue = Pattern.compile("^(option::|::|SetRecordStatus|\\.assured-connect-field|\\.treatment-preferred-contact-field|\\.am-detection-map|CallingList\\[|Campaign\\[|\\[)");
    private static final Pattern regRecCreateContinueParams = Pattern.compile("^\\s");
    private static final Pattern regTreatmentContinue = Pattern.compile("^\\s+");
    private static final Pattern regOCSIxnContinue = Pattern.compile("^\\t.+");
    private static final Pattern regOCSHTTPContinue = Pattern.compile("^\\t+\\'");
    private static final Pattern regClientContinue = Pattern.compile("^\\t.+");
    private static final Pattern regOCSIconContinue = Pattern.compile("^\\s+");

//16:19:06.050 CM_DBCallList(236-136-336): DBServer 'DBServer_OCS for CL_CALLBACK (1440)' SQL: sp236136336114 @CampID = '136', @GroupID = '336', @ListID = '236', @RecType = 2, @NumRec = 6, @CTime = '69546', @TTime = '1461957646', @SPResult = @out [ReqID=877866]
//16:19:06.052 CM_DBCallList(236-136-336): DBServer 'DBServer_OCS for CL_CALLBACK (1440)' MSG_RETRIEVED(DBM_SUCCESS) [ReqID=877866]
//16:19:06.052 CM_DBCallList(236-136-336): SP return value (ReadyCount):0
    private static final Pattern regDBServer = Pattern.compile("\\s*(?:CM_DBCallList|CM_DBCallRecord)\\(([0-9]+)-([0-9]+)-([0-9]+)\\): ");
    private static final Pattern regChainID = Pattern.compile("chain_id=(\\d+)");

//10:02:02.602 RecSCXMLTreatment[113-12571-2748098]::Engine-> log {Created scxml session '00000143-04000B7B-0001' for treatment session id=2938}
    private static final Pattern regSCXMLTreatment = Pattern.compile("RecSCXMLTreatment\\[([0-9]+)-([0-9]+)-([0-9]+)\\]::");

//-> SCXML : 0000017F-040206B3-0001  METRIC <log expr="CPNDigits SCXML script: Record handle 16287 Option CPNDigits = 8008720829" label="" level="1" />
    private static final Pattern regSCXMLScript = Pattern.compile("^-> SCXML : ([\\w-]+)");

//16:41:55.299 Int 04543 Interaction message "EventRouteRequest" received from 65200 ("SIPTS750@3020")
//16:39:27.319 Trc 04541 Message EventRegistered received from 'SIPTS750@7593'
    //static Pattern regMsgStart=Pattern.compile("^(Int 04543 Interaction message \"(\\w+)\" |Trc 04541 Message (\\S+))");
    //hopefully URS always print : message EventCallDataChanged after 
    //static Pattern regMsgStart=Pattern.compile("^(Int 04543 |Trc 04541 )");
    private static final Pattern regMsgStart = Pattern.compile("^(?:Int 04543|Trc 04541) .+from.*(?: '([^'@]+)| \\(\\\"([^@\"]+))");

    // : message EventServerInfo
    private static final Pattern regTMessageName = Pattern.compile(": message (.+)");

//received from 65200(SIPS)fliptop76.suzano.com.br:5001(fd=648) message EventAttachedDataChanged
    private static final Pattern regTMessageStart = Pattern.compile("^(received from |\\s*Trc 50071 Send|request to)");

//        2016-04-29T16:26:11.580 Trc 50002 TEvent: EventAttachedDataChanged
    private static final Pattern regTMessageEnd = Pattern.compile("^[^\\s]");
    private static final Pattern reg50002TEvent = Pattern.compile("Trc 50080 Predictive call \\(record id = (\\d+)\\)");
    private static final Pattern regSentTo = Pattern.compile("^\\.{2}sent to");
    private static final Pattern regLineSkip = Pattern.compile("^\\s*");
    //	private DBAccessor m_accessor;
    private static final Pattern regCfgObjectName = Pattern.compile("(?:name|userName)='([^']+)'");
    private static final Pattern regCfgObjectType = Pattern.compile("CfgDelta([^=]+)=");
    private static final Pattern regCfgOp = Pattern.compile("PopCfg.+\\s(\\w+)$");
    private static final Pattern regCfgObjectDBID = Pattern.compile("\\WDBID=(\\d+)\\W");

    private int m_CurrentLine;
    private StatEventType statEventType;
    HashMap<String, String> prevSeqno = new HashMap();
    private ParserState m_ParserState;

//16:19:14.056 PAEventInfo (ContactInfo):
//	OwnerSessionDBID: 164; OriginSessionDBID: 164;
//	CallType:  PA_CallOutbound; StatType: PA_StatAgentBusy;
//	RecHandle: 18624; CallID: 0; 
//	PlaceDBID: 299; CallResult: 0;
//Distribution time   0.37 has been added. Counter: 641; GroupDBID: 164
//Idle time 179.16 has been added. Idle Time Counter: 739; GroupDBID: 164
//16:19:14.056 PA Outbound Call Info:
//	OwnerDBID: 164; OriginDBID: 164; RecHandle: 18624;
//	CurrentStatType: AgentBusy; State: OutCallSuccess; CPDCompletedFlag: 1; CompletedFlag: 0; CallResult: 33; 
//	StatType Durations:
//	       CallDialed	  18.89
//	       CallQueued	   0.37
//	       AgentBusy	   0.00
//16:19:14.056 PA Agent Info:
//	OwnerDBID: 164; OriginDBID: 0; RecHandle: 18624; ConnID: ; PlaceDBID: 299; AgentDBID: 472; Agent/Place: FABIANAC
//	CurrentStatType: AgentBusy; CallType: Outbound; NotAvailableFlag: 0;  
//	StatType Durations:
//	       AgentBusy	   0.00
//	       AgentReady	 179.16
//	       AgentRingOrDial	   0.27
    private String m_LastLine = "";
    private boolean isRequest;

    OCSParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_OCS, m_tables);
    }

    private void AddPAEventInfoMessage() throws Exception {
        OCSPAEventInfo msg = new OCSPAEventInfo(m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    private boolean isStatEventEnded(StatEventType statEventType, String str) throws Exception {
        switch (statEventType) {
            case SEventCurrentTargetState_TargetUpdated:
            case SEventCurrentTargetState_Snapshot:
            case SEventCurrentTargetState_TargetAdded:
//                statEventType = StatEventType.UNKNOWN;
                return str.length() == 0 || !regStatEvtContinue.matcher(str).find();

            case SEventDataStreamMessage:
            case SEventInfo: {
                statEventType = StatEventType.UNKNOWN;
                return str.length() == 0;

            }

            case SEventDataStreamOpened:
            case SEventStatClosed:
            case SEventStatOpened:
            case SEventRegistered:
            case SEventDataStreamOpeningFailure:
            case SEventError:
            case SEventStatInvalid:
            case SEventCurrentTargetState_TargetRemoved:
                return true;

            default:
                throw new Exception("Unknown statEventType");
        }
    }
//There are 27 Agents/Places assigned to Session 'OBN_IP_EBlock_Campaign@OBN_IP_EBlock_Group'[268] OCS_Session_Loaded | OCS_Stat_Opened:

    private void AddAssignmentMessage() throws Exception {
        OCSAgentAssignment msg = new OCSAgentAssignment(m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    private void AddRecCreateMessage() {
        OCSRecCreate msg = new OCSRecCreate(m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    private void AddSCXMLTreatmentMessage(String campDBID, String recHandle, String chainID,
            String rest) {
        OCSSCXMLTreatment msg = new OCSSCXMLTreatment(campDBID, chainID, recHandle, rest);
        SetStdFieldsAndAdd(msg);
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void AddIxnMessage() {
        OCSIxn msg = new OCSIxn(m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    private void AddHTTPMessage() {
        OCSHTTP msg = new OCSHTTP(m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    private void AddOCSIconMessage() {
        OCSIcon msg = new OCSIcon(m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    private void AddClientMessage() {
        if (m_MessageContents.size() > 1) { //to filter out
            //OCMClient[txrdn_ocs_01_bu]::Notifying backup, record handle: 28734
            OCSClient msg = new OCSClient(m_MessageContents);
            SetStdFieldsAndAdd(msg);
        }
    }

    private void AddSCXMLScript(String sessID, String rest) {
        OCSSCXMLScript msg = new OCSSCXMLScript(sessID, rest);
        SetStdFieldsAndAdd(msg);
    }

    private void AddRecTreatmentMessage() {
        OCSRecTreatment msg = new OCSRecTreatment(m_MessageContents);
        SetStdFieldsAndAdd(msg);

    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {

        Main.logger.trace("ParseFrom offset:" + offset + " line:" + line);
        m_CurrentLine = line;

        try {
            input.skip(offset);

            m_ParserState = ParserState.STATE_HEADER;
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            String str;
            while ((str = input.readLine()) != null) {

                try {
//                    int l=str.length()+input.charsSkippedOnReadLine;
//                    int l = str.getBytes("UTF-16").length/2+input.charsSkippedOnReadLine;
                    int l = input.getLastBytesConsumed();
                    setEndFilePos(getFilePos() + l); // to calculate file bytes
                    m_CurrentLine++;
                    m_LastLine = str; // store previous line for server name
                    Main.logger.trace("l: " + m_CurrentLine + " [" + str + "] bytes:" + l + " pos: " + getFilePos() + " end:" + getEndFilePos() + " diff: " + (getEndFilePos() - getFilePos()));

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

    private String ParseLine(String str, BufferedReaderCrLf in) throws Exception {
        String s = str;
        Matcher m;

        if (!ParseCustom(str, 0)) {
            switch (m_ParserState) {
                case STATE_HEADER:
                case STATE_COMMENT:
                    return null;

                default:
                    Main.logger.error("CustomSearch " + getLastCustomSearch().toString() + " has parserRest=\"false\" while the m_ParserState=[" + m_ParserState + "]. The flag ignored");
                    break;
            }
        }

        switch (m_ParserState) {
            case STATE_HEADER: {
                if (s.isEmpty()) {
                    m_ParserState = ParserState.STATE_COMMENT;
                }
            }
            break;

            case STATE_COMMENT:
                m_LineStarted = m_CurrentLine;

                s = ParseGenesys(str, TableType.MsgOCServer, regNotParseMessage);

//<editor-fold defaultstate="collapsed" desc="inserting DB Server related message">
                if ((m = regDBServer.matcher(s)).find()) {
                    try {
                        OCSDBActivity msg = new OCSDBActivity(m.group(1), m.group(2), m.group(3), s.substring(m.end()));
//                        setSavedFilePos(getFilePos());
//                        SetStdFieldsAndAdd(msg);
                        msg.setM_TimestampDP(getCurrentTimestamp());
                        msg.SetOffset(getFilePos());
                        msg.SetFileBytes(getEndFilePos() - getFilePos());
                        msg.SetLine(m_LineStarted);
                        int e = m.end();
                        if ((m = regChainID.matcher(s.substring(e))).find()) {
                            msg.setChainID(m.group(1));
                        }
                        msg.AddToDB(m_tables);
                    } catch (Exception e) {
                        Main.logger.error("Not added OCS DBServer record:" + e.getMessage(), e);
                    }
                } //</editor-fold>                
                else if ((m = regTMessageStart.matcher(s)).find()) {//starts on clear line
                    this.isRequest = m.group(0).startsWith("request");
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_TMESSAGESTART;
                    break;
                } else if (s.startsWith("CFGObjectInfoChanged")) {
                    AddConfigMessage(s);
                    break;
                } else if ((m = regCGStart.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_CG;
                } else if ((m = regPAInfo.matcher(s)).find()) {
                    OCSPredInfo msg = new OCSPredInfo(m);
                    SetStdFieldsAndAdd(msg);
                    break;
                } else if ((m = regPAStart.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_PA_SESSION_INFO;
                } else if ((m = regRecTreatment.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_RECTREATMENT;
                } else if ((m = regPAAgentInfo.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_AGENT_INFO;
                } else if ((m = regPAEventInfo.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_EVENT_INFO;
                } else if ((m = regStatEvent.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    statEventType = StatEventType.valueOf(m.group(1));// name of StatEvent
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_STAT_EVENT;
                } else if ((m = regSCXMLTreatment.matcher(s)).find()) {
                    AddSCXMLTreatmentMessage(m.group(1), m.group(2), m.group(3),
                            s.substring(m.end()));
                } else if ((m = regSCXMLScript.matcher(str)).find()) {
                    AddSCXMLScript(m.group(1), s.substring(m.end()));
                } else if ((m = regAgentAssignment.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_AGENT_ASSIGNMENT;
                } else if ((m = regRecordCreate.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_RECORD_CREATE;
                } else if ((m = regRecordDelete.matcher(s)).find()) {
                    OCSRecCreate msg = new OCSRecCreate();
                    msg.setRecHandle(m.group(1));
                    msg.setIsCreate(false);
                    SetStdFieldsAndAdd(msg);
                } else if ((m = regIxnMsg.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_IXN_MSG;
                } else if ((m = regHTTPMessage.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_HTTP;
                } else if ((m = regClient.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_CLIENT;
                } else if (s.endsWith("[GOMessageType] EventGOReportingData")) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_OCSICON;
                }
                break;

            case STATE_OCSICON: {
                if (str.length() > 0
                        && ((m = regOCSIconContinue.matcher(str)).find())) {
                    m_MessageContents.add(str);
                } else {
                    AddOCSIconMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str; //
                }
                break;

            }

            case STATE_CLIENT:
                if (str.length() > 0
                        && ((m = regClientContinue.matcher(str)).find())) {
                    m_MessageContents.add(str);
                } else {
                    AddClientMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str; //
                }
                break;

            case STATE_HTTP:
                if (str.length() > 0
                        && ((m = regOCSHTTPContinue.matcher(str)).find())) {
                    m_MessageContents.add(str);
                } else {
                    AddHTTPMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str; //
                }
                break;

            case STATE_IXN_MSG:
                if (str.length() > 0
                        && ((m = regOCSIxnContinue.matcher(str)).find())) {
                    m_MessageContents.add(str);
                } else {
                    AddIxnMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str; //
                }
                break;

            case STATE_RECORD_CREATE:
                if (str.length() > 0
                        && ((m = regRecCreateContinue.matcher(str)).find())) {
                    m_MessageContents.add(str);
                } else {
                    m_ParserState = ParserState.STATE_RECORD_CREATE_PARAMS;
                    return str;
                }
                break;

            case STATE_RECORD_CREATE_PARAMS:
                if (str.length() > 0
                        && ((m = regRecCreateContinueParams.matcher(str)).find())) {
                    m_MessageContents.add(str);
                } else {
                    AddRecCreateMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str; //
                }
                break;

            case STATE_RECTREATMENT:
                if (str.length() > 0
                        && ((m = regTreatmentContinue.matcher(str)).find())) {
                    m_MessageContents.add(str);
                } else {
                    AddRecTreatmentMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str; //
                }
                break;

            case STATE_AGENT_ASSIGNMENT:
                if (str.length() > 0) {
                    if ((m = regCampaignAssignment.matcher(str)).find()) {
                        if (m_MessageContents.size() > 0) { //not first message
                            AddAssignmentMessage();
                            m_MessageContents.clear();
                            m_LineStarted = m_CurrentLine;
                        }
                        m_MessageContents.add(str);
                    } else {
                        m_MessageContents.add(str);
                    }
                } else {
                    AddAssignmentMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                }
                break;

            case STATE_SCXML_TREATMENT:
                if (str.length() > 0 && (Character.isWhitespace(str.charAt(0))
                        || str.charAt(0) != '}')) {
                    m_MessageContents.add(str);
                } else {
//                    AddSCXMLTreatmentMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                }
                break;

            case STATE_STAT_EVENT:
                if (isStatEventEnded(statEventType, str)) {
                    AddStatEventMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                } else {
                    if (statEventType == StatEventType.SEventCurrentTargetState_Snapshot
                            && str.length() == 0) {
                        AddStatEventMessage();
                        m_MessageContents.clear();
                        m_LineStarted = m_CurrentLine;
                        setSavedFilePos(getFilePos());
                    } else {
                        m_MessageContents.add(str);
                    }
                }
                break;

            case STATE_AGENT_INFO:
                if (str.length() > 0 && (Character.isWhitespace(str.charAt(0)))) {
                    m_MessageContents.add(str);
                } else {
                    AddPAAgentInfoMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                }
                break;

            case STATE_EVENT_INFO:
                if (str.length() > 0 && (Character.isWhitespace(str.charAt(0)))) {
                    m_MessageContents.add(str);
                } else {
                    AddPAEventInfoMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                }
                break;

            case STATE_CG:
                if (str.length() > 0 && Character.isWhitespace(str.charAt(0))) {
                    m_MessageContents.add(str);
                } else {
                    AddCGMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str; //
                }
                break;

            case STATE_PA_SESSION_INFO:
                if (str.length() > 0 && Character.isWhitespace(str.charAt(0))) {
                    m_MessageContents.add(str);
                } else {
                    AddPASessionInfoMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str; //
                }
                break;

//            case STATE_TMESSAGE_START:
//                //	 : message EventServerInfo
//                //static Pattern regTMessageName=Pattern.compile(": message (.+)");
////                m_msgName = SubstrAfterPrefix(s, ": message ");
//                m_MessageContents=new ArrayList();
//                m_ParserState=ParserState.STATE_TMESSAGE;
//                break;
            case STATE_TMESSAGESTART: {
                if (str.startsWith("request to")) {
                    m_MessageContents.add(str);
                    isRequest = true;
                    str = null;
                }
                m_ParserState = ParserState.STATE_TMESSAGE;
                return str;
            }

            case STATE_TMESSAGEEND: {
                String recHandle = null;
                m_MessageContents.add(str);
                if ((m = reg50002TEvent.matcher(str)).find()) {
                    recHandle = m.group(1);
                }
                AddOCSTMessage(recHandle);
                m_MessageContents.clear();
                m_ParserState = ParserState.STATE_COMMENT;
                if (recHandle == null) {
                    return str;
                }
            }

            case STATE_TMESSAGE: {
                if (regTMessageEnd.matcher(str).find()) {
//                    if (isRequest && regSentTo.matcher(str).find()) {
//                        m_MessageContents.add(str);
//                        m_ParserState = ParserState.STATE_TMESSAGEEND;
//                    } else {
                    AddOCSTMessage(null);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;

                } else {
                    m_MessageContents.add(str);
                }
            }
            break;
        }

        return null;
    }

    private void AddCGMessage() throws Exception {
        OCSCG msg = new OCSCG(m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    private void AddPASessionInfoMessage() throws Exception {
        OCSPASessionInfo msg = new OCSPASessionInfo(m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    private void AddPAAgentInfoMessage() throws Exception {
        OCSPAAgentInfo msg = new OCSPAAgentInfo(m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    private void AddStatEventMessage() throws Exception {
        try {
            OCSStatEvent msg = new OCSStatEvent(statEventType, m_MessageContents);
            SetStdFieldsAndAdd(msg);
        } catch (Exception e) {
            Main.logger.error(e);
            throw e;
        }
    }

    private void AddOCSTMessage(String recHandle) throws Exception {
        OcsMessage msg = new OcsMessage(m_MessageContents);
        msg.setRecordHandle(recHandle);
        SetStdFieldsAndAdd(msg);

    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
        m_tables.put(TableType.OCSPredInfo, new OCSPredInfoTable(Main.getM_accessor(), TableType.OCSPredInfo));
    }

    private void AddConfigMessage(String s) {
        ConfigUpdateRecord msg = new ConfigUpdateRecord(s);
        try {
            Matcher m;
            msg.setObjectType(Message.getRx(s, regCfgObjectType, 1, ""));
            msg.setObjectDBID(Message.getRx(s, regCfgObjectDBID, 1, ""));
//            msg.setObjName(Message.FindByRx(m_MessageContents, regCfgObjectName, 1, ""));
//            msg.setOp(Message.FindByRx(m_MessageContents, regCfgOp, 1, ""));
//
//            if ((m = Message.FindMatcher(m_MessageContents, regCfgObjectType)) != null) {
//                msg.setObjectType(m.group(1));
//                msg.setObjectDBID(m.group(2));
//            }

            SetStdFieldsAndAdd(msg);
        } catch (Exception e) {
            Main.logger.error("Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);
        }
    }

    enum ParserState {

        STATE_HEADER,
        STATE_TMESSAGE,
        STATE_TMESSAGESTART,
        STATE_COMMENT,
        STATE_CG,
        STATE_PA_SESSION_INFO,
        STATE_AGENT_INFO,
        STATE_EVENT_INFO,
        STATE_STAT_EVENT,
        STATE_RECORD_CREATE,
        STATE_RECORD_CREATE_PARAMS,
        STATE_IXN_MSG,
        STATE_HTTP,
        STATE_CLIENT,
        STATE_AGENT_ASSIGNMENT,
        STATE_SCXML_TREATMENT, STATE_RECTREATMENT, STATE_TMESSAGEEND, STATE_OCSICON
    }

    enum StatEventType {

        UNKNOWN(""),
        SEventInfo("SEventInfo"),
        SEventCurrentTargetState_Snapshot("SEventCurrentTargetState_Snapshot"),
        SEventCurrentTargetState_TargetUpdated("SEventCurrentTargetState_TargetUpdated"),
        SEventCurrentTargetState_TargetAdded("SEventCurrentTargetState_TargetAdded"),
        SEventRegistered("SEventRegistered"),
        SEventError("SEventError"),
        SEventStatInvalid("SEventStatInvalid"),
        SEventStatOpened("SEventStatOpened"),
        SEventDataStreamOpeningFailure("SEventDataStreamOpeningFailure"),
        SEventDataStreamMessage("SEventDataStreamMessage"),
        SEventDataStreamOpened("SEventDataStreamOpened"),
        SEventCurrentTargetState_TargetRemoved("SEventCurrentTargetState_TargetRemoved"),
        SEventStatClosed("SEventStatClosed");

        private final String name;

        private StatEventType(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            return (otherName == null) ? false : name.equals(otherName);
        }

        @Override
        public String toString() {
            if (name != null) {
                return this.name;
            } else {
                return "";
            }
        }

    }

    private class OCSPredInfo extends Message {

        private int sessionID;
        private double density;
        private double a;
        private double predTime;
        private double reminder;
        private double sentCalls;

        private OCSPredInfo() {
            super(TableType.OCSPredInfo);
        }

        private OCSPredInfo(Matcher m) {
            this();
            sessionID = intOrDef(m.group(1), 0);
            density = parseOrDef(m.group(2), 0.0);
            a = parseOrDef(m.group(3), 0.0);
            predTime = parseOrDef(m.group(4), 0.0);
            reminder = parseOrDef(m.group(5), 0.0);
            sentCalls = parseOrDef(m.group(6), 0.0);

        }

        public double getDensity() {
            return density;
        }

        public double getA() {
            return a;
        }

        public double getPredTime() {
            return predTime;
        }

        public double getReminder() {
            return reminder;
        }

        public double getSentCalls() {
            return sentCalls;
        }

        public int getSessionID() {
            return sessionID;
        }

    }

    private class OCSPredInfoTable extends DBTable {

        public OCSPredInfoTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("OCSSessionID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",OCSSessionID INTEGER"
                    + ",density double"
                    + ",a double"
                    + ",predTime double"
                    + ",reminder double"
                    + ",sentCalls double"
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
            OCSPredInfo rec = (OCSPredInfo) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, OCSPredInfo.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                stmt.setInt(6, rec.getSessionID());
                stmt.setDouble(7, rec.getDensity());
                stmt.setDouble(8, rec.getA());
                stmt.setDouble(9, rec.getPredTime());
                stmt.setDouble(10, rec.getReminder());
                stmt.setDouble(11, rec.getSentCalls());
                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }

}
