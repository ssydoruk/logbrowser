package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author terry
 */
public class WorkspaceParser extends Parser {

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
    private static final Pattern regTMessageOutFinal = Pattern.compile("^\\.\\.sent to");
    private static final Pattern regTMessageInFinal = Pattern.compile("^\\. Processing with  state");
    private static final Pattern regTMessageRequestFinal = Pattern.compile("^(ReferenceId|AttributeReferenceID|attr_ref_id|$)");

    private static final Pattern regCfgUpdate = Pattern.compile("^\\s+PopCfg");

    // keys: multiline function spec, value: pattern that ML is still on
    static HashMap<String, Pattern> regMultiLine = new HashMap<String, Pattern>();
    private static final Pattern regTMessageInStart = Pattern.compile("Proxy got message");
    private static final Pattern regTMessageCompleted = Pattern.compile("- Completed handling");
    private static final Pattern regTMessageSending = Pattern.compile("Sending message to");
    private static final Pattern regTMessageOutFirstLine = Pattern.compile("^(request to|\\tAttribute|\\t\\()");
    private static final Pattern regReceived = Pattern.compile("received from (\\d+)\\(([^\\)]+)\\).+message (Event\\w+)(?:\\(refid=(\\d+)\\))*");
    private static final Pattern regProxyReceived = Pattern.compile("Proxy got message '(\\w+)' \\('(\\d+)'\\)");
    private static final Pattern regSentTo = Pattern.compile("send to ts ([^\\[\\s]+)");
    private static final Pattern regSendingTo = Pattern.compile(" sending event ");
    private static final Pattern regSendingMessageTo = Pattern.compile(" Sending message to (\\w+)");
    private static final Pattern regCompletedHandling = Pattern.compile("- Completed handling (\\w+)");
    private static final Pattern regLastOneOrTwoWords = Pattern.compile("(?:\\s*(?:\\w+)){1,2}$");
    private static final Pattern regReqTo = Pattern.compile("^request to (\\d+).+message (Request\\w+)");
    private static final Pattern regToRequest = Pattern.compile("^'(\\w+)'");
    private static final Pattern regStrategy = Pattern.compile("strategy:.+\\*+(\\S+)");
    //[14:33] strategy: **ORS (1085200831) is attached to the call
    private final static HashSet<String> eventsStatServer = new HashSet<String>(
            Arrays.asList("EventInfo",
                    "EventStatisticOpened",
                    "EventCurrentTargetStateSnapshot",
                    "RequestCloseStatistic",
                    "EventStatisticClosed",
                    "RequestGetStatisticEx",
                    "EventCurrentTargetStateTargetUpdated",
                    "RequestOpenStatistic",
                    "RequestGetStatistic",
                    "RequestSuspendNotification",
                    "RequestOpenStatisticEx"));
    private final static HashSet<String> eventsEServices = new HashSet<String>(
            Arrays.asList("EventCurrentAgentStatus",
                    "Event3rdServerResponse",
                    "EventCurrentAgentStatus",
                    "RequestRemoveMedia",
                    "RequestNotReadyForMedia",
                    "RequestCancelNotReadyForMedia",
                    "Request3rdServer"));
    private final static HashSet<String> eventsConfig = new HashSet<String>(
            Arrays.asList("EventObjectsRead",
                    "EventObjectsSent",
                    "RequestUnregisterNotification",
                    "EventNotificationUnregistered",
                    "RequestReadObjects"));
    private static final Pattern regCfgObjectName = Pattern.compile("(?:name|userName|number|loginCode)='([^']+)'");
    private static final Pattern regCfgObjectType = Pattern.compile("^Cfg([^=]+)=.*\\{DBID=(\\d+)");
    //    private static final Pattern regCfgAGType = Pattern.compile("^Cfg([^=]+)=\\{DBID=(\\d+)");
    private static final Pattern regCfgOp = Pattern.compile("PopCfg.+\\s(\\w+)$");
    long m_CurrentFilePos;
    final int MSG_STRING_LIMIT = 200;
    private String ConnID;
    private String URSRest;
    long m_HeaderOffset;
    ParserState m_ParserState;
    String m_Header;
    int m_dbRecords = 0;

    public WorkspaceParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_WorkSpace, m_tables);

//17:33:06.335_I_I_03350282382b556f [07:07] HERE IS TARGETS
//TARGETS: OBN_IP_Skill1_Group@OBN_StatServerRouting.GA
        regMultiLine.put("07:07", Pattern.compile("^TARGETS"));
        regMultiLine.put("10:17", Pattern.compile("^\\s*_\\S_\\S_\\s*\\[(\\S{2}:\\S{2})\\] "));

    }

    private void AddStrategyMessage(String FileLine, String URSRest1) {
        URSStrategy msg = new URSStrategy(m_MessageContents, ConnID, FileLine, URSRest);
        try {
            msg.setM_TimestampDP(getCurrentTimestamp());
            msg.SetOffset(getSavedFilePos());
            msg.SetFileBytes(getFilePos() - getSavedFilePos());
            msg.SetLine(m_lineStarted);
            msg.AddToDB(m_tables);
        } catch (Exception e) {
            Main.logger.error("Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);
        }
    }

    //17:42:51.895_I_I_00d202972e8c2ebc [01:08] call (252087-104e77cd0) deleting truly
//                addStep("01:08", null, "call deleting truly");
    private void AddStrategyMessageLine(String line, String FileLine) {
        Matcher m;
        Message msg;
        if ((FileLine.equals("14:33") || FileLine.equals("1B:01")) && (m = regStrategy.matcher(line)).find()) {
            msg = new URSStrategyInit(line, ConnID, m.group(1), "strategy is attached to the call");
        } else if (FileLine.equals("01:08")) {
            msg = new URSStrategyInit(line, ConnID, null, "call deleting truly");

        } else {
            msg = new URSStrategy(line, ConnID, FileLine, URSRest);
        }
        try {
            msg.setM_TimestampDP(getCurrentTimestamp());
            msg.SetOffset(getFilePos());
            msg.SetFileBytes(getEndFilePos() - getFilePos());
            msg.SetLine(m_lineStarted);
            msg.AddToDB(m_tables);
        } catch (Exception e) {
            Main.logger.error("Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);
        }
    }

    /*
    1st parameter - connid
    2nd depends on interaction
     ******* voice: ***********
    received from 65200(SIPTS750)ssydoruk4:7007(fd=) message EventAttachedDataChanged
    AttributeCallType	2
    AttributePropagatedCallType	2
    AttributeCallID	16778220
    AttributeConnID	0077028a809f7004
    AttributeCallUUID	'QK59HK4FS927D7FUU41G6IE468000004'
    AttributeUserData	[149] 00 01 03 00..
    'ORSI:Cluster1:QK59HK4FS927D7FUU41G6IE468000004'(list) 'Session'	'00PN8M303SBR7E9P04000VTAES000001+'
    'Node'	'272'
    'Url'	'http://co67.step:7005/scxml'
    11:49:08.388_T_I_0077028a809f7004 [14:32] EventAttachedDataChanged is received for ts SIPTS750[SIP_Switch] (this dn=3020, refid=0)
    11:49:08.536 RLIB: received from 23(OR_Server_811) message RequestInvoke(1000000100)
    refid(1)	1
    callid(1000000000)	'*00PN8M303SBR7E9P04000VTAES000001'
    sessionid(1000000010)	'00PN8M303SBR7E9P04000VTAES000001'
    fm(1000000001)	'session'
    function(1000000002)	'getListItemValue'
    args(1000000003)	'["INT_XFER_MAP","OPM","1130046"]'
    11:49:08.536_R_I_ [19:0f] connid 0150028acff24001 generated for client=23(OR_Server_811), ref id=0, method name=ORS: *00PN8M303SBR7E9P04000VTAES000001
    _T_I_0150028acff24001 [01:11] connid 0150028acff24001 is bound to the call 2-7efd54e5d508
    _T_I_0150028acff24001 [01:1b] calluuid *00PN8M303SBR7E9P04000VTAES000001 is bound to the call 2-7efd54e5d508 as 7efd54e5de98
    11:49:08.536_I_I_0150028acff24001 [01:01] call (2-7efd54e5d508) for Resources created (del=0 ts=0,1,0)
     ****** email: **********
    11:50:52.636 RLIB: received from 23(OR_Server_811) message RequestCreateVCall(1000000002)
    refid(1)	5
    tenant(1000000006)	'Resources'
    callid(1000000000)	'0000GaBNKN2Q0021'
    args(1000000003)	'{"MediaType":"email","UserData":{"ReceivedAt":"2016-08-16T18:50:43Z","InteractionId":"0000GaBNKN2Q0021","MediaType":"email","PlacedInQueueAt":"2016-08-16T18:50:52Z","InteractionType":"Inbound","InteractionSubType":"InboundNew","Queue":"ORS_Email_Queue","SubmittedBy":"E-MailServer_ORS","SubmittedAt":"2016-08-16T18:50:52Z","MovedToQueueAt":"2016-08-16T18:50:52Z","TenantId":101,"InteractionState":0,"IsOnline":0,"IsLocked":0,"ContactId":"00001aBD19BV000N","EmailAddress":"customer1@w03kiev.step","FirstName":"customer1@w03kiev.step","FromAddress":"customer1@w03kiev.step","FromPersonal":"customer1@w03kiev.step","Header_Content-Type":"multipart/related; boundary=\"----MIME delimiter for sendEmail-78552.24609375\"","Header_Date":"Tue, 16 Aug 2016 18:50:35 +0000","Header_MIME-Version":"1.0","Header_Message-ID":"<5310.05859375-sendEmail@ssydoruk>","Header_X-Mailer":"sendEmail-1.56","LCA_EmplID_email":"ag7102","LCA_TimeStamp_email":"2016-06-16T21:47:01Z","LastCalledAgent_EmployeeID":"ag7102","LastCalledAgent_TimeStamp":"2016-06-16T21:47:01Z","Mailbox":"w03kiev.step","Origination_Source":"Email","Subject":"Hello","To":"\"companyORS@w03kiev.step\" <companyORS@w03kiev.step>","_AttachmentFileNames":"","_AttachmentsSize":"0","_ContainsAttachment":"false","_AutoReplyCount":0}}'
    11:50:52.636_R_I_ [19:0f] connid 0150028acff24002 generated for client=23(OR_Server_811), ref id=5, method name=ORS: 0000GaBNKN2Q0021
    _T_I_0150028acff24002 [01:11] connid 0150028acff24002 is bound to the call 3-7efd54e117f0
    _T_I_0150028acff24002 [01:1b] calluuid 0000GaBNKN2Q0021 is bound to the call 3-7efd54e117f0 as 7efd54e680e0
     */
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
                    Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");

                    while (str != null) {
                        str = ParseLine(str);
                    }
                    setFilePos(getFilePos() + l);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                    throw e;
                }
//                m_CurrentFilePos += str.length()+input.charsSkippedOnReadLine;
            }
            ParseLine(""); // to complete the parsing of the last line/last message
        } catch (Exception e) {
            Main.logger.error(e);
            return m_CurrentLine - line;
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
                m_ParserState = ParserState.STATE_COMMENTS;
                return s;
            }

            case STATE_COMMENTS: {
                m_lineStarted = m_CurrentLine;
                setSavedFilePos(getFilePos());

                try {
                    s = ParseGenesys(str, TableType.MsgURServer);
                } catch (Exception exception) {
                    Main.logger.error("exception parsing", exception);

                }

                if ((m = regCfgUpdate.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_CONFIG;
                } else if ((m = regTMessageInStart.matcher(s)).find()) {
                    m_Header = s;
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_TLIB_MESSAGE_IN;
                    setSavedFilePos(getFilePos());
                } else if ((m = regTMessageCompleted.matcher(s)).find()) {
                    m_Header = s;
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_TLIB_MESSAGE_IN_COMPLETED;
                    setSavedFilePos(getFilePos());
                } else if ((m = regTMessageSending.matcher(s)).find()) {
                    m_Header = s;
                    m_HeaderOffset = m_CurrentFilePos;
                    m_ParserState = ParserState.STATE_TLIB_MESSAGE_REQUEST;
                    setSavedFilePos(getFilePos());
                }

            }
            break;

            case STATE_CONFIG:
                if (str.contains("PopCfg")) {
                    AddConfigMessage(m_MessageContents);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return null;
                } else {
                    m_MessageContents.add(str);
                }
                break;

            case STATE_TLIB_MESSAGE_REQUEST:
                if ((regTMessageRequestFinal.matcher(str)).find()) {
                    AddTServerMessage(m_MessageContents, m_Header, str);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                    m_Header = null;
                } else {
                    m_MessageContents.add(str);

                }
                break;

//<editor-fold defaultstate="collapsed" desc="STATE_TLIB_MESSAGE_IN">
            case STATE_TLIB_MESSAGE_IN:
                if ((regTMessageInFinal.matcher(str)).find()) {
                    AddTServerMessage(m_MessageContents, m_Header, str);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                    m_Header = null;
                } else {
                    m_MessageContents.add(str);

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="STATE_TLIB_MESSAGE_IN_COMPLETED">
            case STATE_TLIB_MESSAGE_IN_COMPLETED:
                dp = ParseFormatDate(str);
                if (dp != null) {
                    AddTServerMessage(m_MessageContents, m_Header, str);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                    m_Header = null;
                    return str;
                } else {
                    m_MessageContents.add(str);

                }
                break;
//</editor-fold>

            case STATE_TLIB_MESSAGE_OUT:
                if ((m = regTMessageOutFinal.matcher(str)).find()) {
                    AddTServerMessage(m_MessageContents, m_Header, str);
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

    protected void AddTServerMessage(ArrayList<String> contents, String header, String urs_message) throws Exception {
        Matcher m;
        String server = null;
        DateParsed theDP = null;
        String event = null;
        String fileHandle = null;
        String refID = null;
        boolean isInbound = true;
        boolean isConfServ = false;

        try {

            if (header.contains("ConfigServerProtocol")) {
                isConfServ = true;
            }
            if ((m = regProxyReceived.matcher(header)).find()) {
                event = m.group(1);
                fileHandle = m.group(2);
            } else if ((m = regReceived.matcher(header)).find()) {
                fileHandle = m.group(1);
                server = m.group(2);
                event = m.group(3);
                refID = m.group(4);
            } else if ((m = regSendingMessageTo.matcher(header)).find()) {
                server = m.group(1);
                if ((m = regToRequest.matcher(contents.get(0))).find()) {
                    event = m.group(1);
                }
                isInbound = false;
            } else if ((m = regCompletedHandling.matcher(header)).find()) {
                server = m.group(1);
                if (contents.isEmpty()) {
                    if ((m = regLastOneOrTwoWords.matcher(header)).find()) {
                        event = m.group(0);
                    }

                } else if ((m = regToRequest.matcher(contents.get(0))).find()) {
                    event = m.group(1);
                }
            } else {
                theDP = ParseFormatDate(header);
                if (theDP != null) {
                    if ((m = regSentTo.matcher(theDP.rest)).find()) {
                        server = m.group(1);
                        if ((m = regReqTo.matcher(contents.get(0))).find()) {
                            fileHandle = m.group(1);
                            event = m.group(2);
                        }
                        isInbound = false;
                    } else if ((regSendingTo.matcher(theDP.rest)).find()) {
                        if ((m = regReqTo.matcher(contents.get(0))).find()) {
                            fileHandle = m.group(1);
                            event = m.group(2);
                        }
                        isInbound = false;
                    }
                } else {
                    //request to 65467(--) message RequestUpdateUserData
                    //or
                    //request to 65467(--) message RequestRouteCall
                    if (!contents.isEmpty() && (m = regReqTo.matcher(contents.get(0))).find()) {
                        fileHandle = m.group(1);
                        event = m.group(2);
                        isInbound = false;
                    } else if ((m = regReqTo.matcher(header)).find()) {
                        fileHandle = m.group(1);
                        event = m.group(2);
                        isInbound = false;
                    }
                }
            }

            Message msg = null;
            if (event != null) {
                if (isConfServ) {
                    msg = new WSConf(event, server, fileHandle, refID, contents);
                } else if (eventsStatServer.contains(event)) {
                    msg = new WSStat(event, server, fileHandle, refID, contents);
                } else if (eventsEServices.contains(event) || hasMMAttribute()) {
                    msg = new WSEServ(event, server, fileHandle, refID, contents);
//            } else if (eventsConfig.contains(event)) {
//                msg = new WSConf(event, server, fileHandle, refID, contents);
                } else {
                    msg = new WSMessage(event, server, fileHandle, refID, contents);
                }

            }
            if (msg != null) {
                msg.SetInbound(isInbound);
                SetStdFieldsAndAdd(msg);
            }
            m_dbRecords++;
        } catch (Exception e) {
            Main.logger.error("Exception creating record line:" + m_CurrentLine + " header[" + header + "]", e);
        }
    }

    private void AddConfigMessage(ArrayList<String> m_MessageContents) {
        ConfigUpdateRecord msg = new ConfigUpdateRecord(m_MessageContents);
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
        } catch (Exception e) {
            Main.logger.error("Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);
        }
    }

    private boolean hasMMAttribute() {
        for (String m_MessageContent : m_MessageContents) {
            if (m_MessageContent != null && m_MessageContent.startsWith("attr_")) {
                return true;
            }
        }
        return false;
    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
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
        STATE_TLIB_MESSAGE_IN_COMPLETED,
        STATE_TLIB_MESSAGE_REQUEST
    }
}
