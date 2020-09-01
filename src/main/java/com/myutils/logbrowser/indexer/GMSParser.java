/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

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
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ssydoruk
 */
public class GMSParser extends Parser {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;

    //2 types of thread messages in ORS logs
//21:37:08.194 [T:2560] {ScxmlMetric:3} METRIC <eval_expr sid='VIKO7I50UD32135O60O7ACO4NS0083UP' expression='varConsiderConcierge = _genesys.session.getListItemValue('LIST_CHT_SCT', v_ServiceCallType, 'Concierge Considered').toUpperCase();' result='' />
    private static final Pattern regWThread = Pattern.compile("^\\s*\\[([^\\]]+)[\\]\\s]*");
    //21:37:07.710 {FMWeb:2} HandleRequest: Path '/scxml', file '/session/start', query ''
    private static final Pattern regFMThread = Pattern.compile("\\s*\\{([^:\\}]+)");
    // general buffer
    private static final byte[] m_CharBuf = new byte[8048];
    //16:41:55.299 ors:+OrsEvent[0x01aa91f0]:name=interaction.partystatechanged
//2016-04-20T09:22:09.969 Trc 04541 Message EventAgentNotReady received from 'sipserver_01_p@+43891166406129992'
//09:22:09.970 [LS] '00AGCH16J4BHF6E1GNHNK2LAES00000R' ProcessEvent=AgentEventNotReady dev='+43891166406129992' queue='' mediaType=100  StateStorageCnt=1 Key[102-148-0-100]
    private static final Pattern regDate = Pattern.compile("^((?:\\d{4}-\\d{2}-\\d{2}T)*\\d{2}:\\d{2}:\\d{2}\\.\\d{3} )");
    //16:41:55.299 Int 04543 Interaction message "EventRouteRequest" received from 65200 ("SIPTS750@3020")
//16:39:27.319 Trc 04541 Message EventRegistered received from 'SIPTS750@7593'
    //hopefully URS always print : message EventCallDataChanged after
    private static final Pattern regMsgStart = Pattern.compile("Handling update message:$");
    private static final Pattern regMsgRequest = Pattern.compile("Sent '(\\w+)' \\(\\d+\\) attributes:$");

//    java.net.ConnectException: Connection refused
    private static final Pattern regExceptionStart = Pattern.compile("^([\\w\\.]+Exception[\\w\\.]*):(.*)");
    private static final Pattern regNullExceptionStart = Pattern.compile("^([\\w\\.]+Exception[\\w\\.]*)");

//07:23:14.681 Dbg 09900  [qtp747464370-266] Attempting to start session  with url {callback}
    private static final Pattern regAttemptStartSession = Pattern.compile("Attempting to start session");
    //    static Pattern regMsgStart = Pattern.compile("^\\s*(?:Int 04543|Trc 04541).+essage \\\"*(\\w+).+from \\d+ \\(\\\"([^@\\\"]+)");
    // : message EventServerInfo
//AgentMessage(super=BasicTelephonyMessage(message='EventAgentLogin' (73) attributes:
    private static final Pattern regTMessageStart = Pattern.compile("^(?:\\w+)Message.+message='(\\w+)'");
    //static Pattern regMsgStart=Pattern.compile("^(Int 04543 |Trc 04541 )");
    private static final Pattern regMMsgStart = Pattern.compile("^\\s*attr_");
    // : message EventServerInfo
    private static final Pattern regOutMMsgStart = Pattern.compile("Message '(\\w+)' sent to '(\\w+)'");
    // : message EventServerInfo
    //16:41:55.283 W-NODE[272](RUNNING): >>>> 78 bytes to S-NODE[272] >>>>
//16:41:55.283 S-NODE[272](MASTER): <<<< 78 bytes from NODE[272] <<<<
    //" (W|S)-NODE\\[(\\d+)\\]\\D+(\\d+) bytes (to|from).+NODE\\[(\\d+)\\]"
    private static final Pattern regClusterMsg = Pattern.compile("^(W|S)-NODE\\[(\\d+)\\]\\D+(\\d+) bytes (to|from).+NODE\\[(\\d+)\\]");
    //16:17:19.815 CTITM: R:323 UpdateUData(1,3020)
    //static Pattern regCTITM = Pattern.compile("");
    private static final Pattern m_strCTITM = Pattern.compile("^\\s*CTITM:\\s+(.+)$");
    private static final Pattern regCTITMReq = Pattern.compile("^R:(\\d+) (\\w+)\\(([^,]+),([^\\)]+)");
    /*
    16:41:55.768 METRIC <state_enter sid='KL8938QSD91ER2RN2NR1397738000001' name='Telstra.globalstate' type='state' />
    16:41:55.768 METRIC <transition sid='KL8938QSD91ER2RN2NR1397738000001' target='Telstra.Entry1' line='2859' />
    16:41:55.768 METRIC <log sid='KL8938QSD91ER2RN2NR1397738000001' expr='KL8938QSD91ER2RN2NR1397738000001: Inside Entry Block: Entry1' label='' level='1' />
     */
    static String m_strMetric = "METRIC ";
    //11:19:06.379 [SessionManager]: adding key[IDEN4PA4QH5PJ2F786QKA47CPC0066RU]/value[5PQD29NRV96QVB8GF448P0ICEK0016KC]
    private static final Pattern regGidUuid = Pattern.compile("adding key\\[(\\w+)]/value\\[(\\w+)\\]");

//13:40:18.552 {SessionManager:2} URL [http://orswas.internal.gslb.service.nsw.gov.au/mod-routing/src-gen/IPD_default_mainWorkflow.scxml] associated with script [mod-routing]
    private static final Pattern regAppURL = Pattern.compile("\\}\\sURL \\[([^\\]]+)\\] associated with script \\[([^\\]]+)\\]$");
    //09:25:15.864 {ORSURS:3} InvokeFunctionalModule: <<
//	refID	28
//	call	'028AS5H6BGBHF6E1GNHNK2LAES00000F'
//	session	'004BUJ1C0KBHF8NIG7HNK2LAES000008'
//	module	'urs'
//	method	'SCXMLSubmit'
//	args	'[1,"VQ_Residential_RP",0,"","any",1,0,0,"VQUser@.A"]'
// <<
//09:25:15.864 [T:140084682483456] {ScxmlMetric:3} METRIC <eval_condition sid='004BUJ1C0KBHF8NIG7HNK2LAES000008' condition='system.ANI == ''' result='false' />
    private static final Pattern regPOST = Pattern.compile("^\\(POST\\) Client IP Address");
    private static final Pattern regKVList = Pattern.compile("^\\s+'");
    private static final Pattern regPostJSON = Pattern.compile("JSon: KVList:\\s*$");

    private static final Pattern regORSResponse = Pattern.compile("^OrsService: response from ORS (.+)$");

    private static final Pattern regExceptionContinue = Pattern.compile("^(\\s|Caused by:)");
    private static final Pattern regIxnMsgContinue = Pattern.compile("^(\\s|attr_)");

    static String httpStart = " HTTP[";
//    private static final Pattern regSTATE_HTTPIN = Pattern.compile(" HandleRequest: |ORS_HTTPResponse");
//    private static final Pattern regHTTPINSessionCreate = Pattern.compile("\\s*OnRequestStart: Creating session. SessionID=(\\w+)$");
    //19:36:55.156 HTTP[56,TCP] <<<< 413 bytes from 171.164.220.27 <<<<
//    private static final Pattern regHTTPin = Pattern.compile("^\\s*HTTP\\[(\\d+)\\,TCP\\] <<<< (\\d+) bytes from ([\\d\\.]+) <<<<$");
    //21:06:45.840 HTTP[42,TCP] >>>> 131 bytes >>>>
//    private static final Pattern regHTTPout = Pattern.compile("^\\s*HTTP\\[(\\d+)\\,TCP\\] >>>> (\\d+) bytes >>>>$");
//    private static final Pattern regHTTPignore = Pattern.compile(": OnConnect|client .+ exception|discard pending resonse| client [\\d\\.]+ disconnected");
    private static final StartSessionTransition startSessionTransition = new StartSessionTransition();
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
    private static final Pattern ptStatStringValue = Pattern.compile("^\\s*'STRING_VALUE'.+= \"(.+)\"$");
    private static final Pattern ptStatMessageStart = Pattern.compile("message from (\\S+) - tcp:\\/\\/([^:]+):(\\d+): '([^']+)'.+attributes:\\s+");
    private static final Pattern ptStatMessageAttr = Pattern.compile("(?:(\\S+)\\s+\\[\\S+\\]\\s+=\\s+([\\-\\d]+|\"[^\\\"]+\"|ObjectValue:[^\\}]+)\\s+)");
    public static final int MAX_WEB_PARAMS = 20;
    private static final HashMap<String, Integer> keysPos = new HashMap<>(MAX_WEB_PARAMS);

    private long m_CurrentFilePos;
    private int m_TotalLen;
    private int m_dbRecords;
    private String m_msgName;
    private String m_TserverSRC;
    private String m_TMessageHeader;
    HashMap<String, String> prevSeqno = new HashMap();

    private long m_HeaderOffset;

    private ParserState m_ParserState;

    private final HashMap<Integer, OrsHTTP> partHTTP = new HashMap<>();

    private final HashMap<String, ParserState> threadParserState = new HashMap<>();

    MsgType MessageType;

    private String m_MetricType;
    private String m_SessionID;

    private String m_LastLine = "";


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
    private BufferedReaderCrLf m_input;
    private final HashMap<String, String> ThreadAlias = new HashMap<>();
    private String newThread; // current thread name as parsed fom the line
    private Record recInProgress;
    private String URL = null;
    private String app = null;
    private boolean isInbound = true;
    private String m_msg1;
    private String oldThread;
    private String savedThreadID;
    private final ParserThreadsProcessor ptpsThreadProcessor;
    Message msg = null;
    HashMap<String, GMSORSMessage> threadReqs = new HashMap<>();

    GMSParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_GMS, m_tables);
        m_MessageContents = new ArrayList();
        ThreadAlias.put("ORSInternal", "FMWeb");

        ptpsThreadProcessor = new ParserThreadsProcessor();
        //<editor-fold defaultstate="collapsed" desc="processing of fetch per thread">
        ptpsThreadProcessor.addStateTransition(new StartSessionTransition());
//        ptpsThreadProcessor.add(ptp);
//</editor-fold>

    }

    private void processStatisticNotification(String substring) {
//        GMSStatServerMsg msg = new GMSStatServerMsg(substring);
//        msg.SetStdFieldsAndAdd(this);
    }

    private void processOpenMediaInteraction(String webReqID, boolean isResponse, String URL, String rest) {
        GMSWebClientMsg msg = new GMSWebClientMsg(webReqID, isResponse, URL, rest);
        msg.SetStdFieldsAndAdd(this);
    }

    private OrsHTTP getFullHTTP(OrsHTTP orsHTTP) {
        OrsHTTP tmpOrsHTTP = partHTTP.get(orsHTTP.getSocket());
        if (tmpOrsHTTP != null) { // pending HTTP request
            tmpOrsHTTP.AddBytes(orsHTTP);
            tmpOrsHTTP.SetFileBytes(orsHTTP.m_fileOffset + orsHTTP.GetFileBytes() - tmpOrsHTTP.m_fileOffset);
            if (tmpOrsHTTP.isComplete()) {
                partHTTP.remove(orsHTTP.getSocket());
                return tmpOrsHTTP;
            }
        } else {
            if (orsHTTP.isComplete()) {
                return orsHTTP;
            } else {
                partHTTP.put(orsHTTP.getSocket(), orsHTTP);
            }
        }
        return null;

    }

    private String getThreadAlias(String thr) {
        String ret = ThreadAlias.get(thr);
        return (ret != null) ? ret : thr;
    }

    private String getThread(String s) {
        if (s != null) {
            Matcher m;
            if ((m = regWThread.matcher(s)).find()) {
                oldThread = newThread;
                newThread = getThreadAlias(m.group(1));
                return s.substring(m.end());
            }
//            if ((m = regFMThread.matcher(dp.rest)).find()) {
//                thread = getThreadAlias(m.group(1));
//                return;
//            }
        }
        newThread = null;
        return s;
    }

    private ParserState getParserStateByThread() throws Exception {
        return m_ParserState;
//        if (newThread == null) {
//            return m_ParserState;
//        }
//        ParserState ret = threadParserState.get(newThread);
//        return (ret != null) ? ret : m_ParserState;
    }

    private void SetParserThreadState(String thr, ParserState ps) throws Exception {
        if (thr == null) {
            throw new Exception("No thread");
        }
        if (ps == ParserState.STATE_COMMENT || ps == ParserState.STATE_HEADER) {//by default there is no state to parser
            threadParserState.remove(thr);
        } else {
            threadParserState.put(thr, ps);
        }
    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_input = input; // to use from subroutines
        m_CurrentFilePos = offset;
        m_CurrentLine = line;

        m_dbRecords = 0;
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
                Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");

                try {
                    int l = input.getLastBytesConsumed();
//                    SetBytesConsumed(input.getLastBytesConsumed());
                    setEndFilePos(getFilePos() + l); // to calculate file bytes

                    while (str != null) {
                        str = ParseLine(str, input);
                    }
                    setFilePos(getFilePos() + l);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                }

                m_LastLine = str; // store previous line for server name
//                m_CurrentFilePos += str.length()+input.charsSkippedOnReadLine;
            }
//            ParseLine("", null); // to complete the parsing of the last line/last message
        } catch (IOException e) {
            Main.logger.error(e);
            return m_CurrentLine - line;
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
                m_LineStarted = m_CurrentLine;
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
                    //m_ParserState=STATE_CLUSTER;
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

                } else if ((m = regSendIxnServerMsg.matcher(s)).find()) {
                    m_HeaderOffset = m_CurrentFilePos;
                    m_MessageContents.clear();
                    m_TotalLen = str.length() + in.charsSkippedOnReadLine;
                    m_ParserState = ParserState.STATE_IXNMESSAGE;
                    msg = new IxnGMS(m.group(1), m.group(2));
                    setSavedFilePos(getFilePos());

                } else {
                    GenesysMsg lastLogMsg = getLastLogMsg();
                    if (lastLogMsg != null) {
                        if (!lastLogMsg.isSavedToDB()) {
                            lastLogMsg.AddToDB(m_tables);
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
                        ((IxnGMS) msg).SetStdFieldsAndAdd(this);
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

    private void addPostMessage() throws Exception {
        GMSPostMessage _msg = new GMSPostMessage(m_MessageContents);
        SetStdFieldsAndAdd(_msg);
    }

    private void addException() {
        Matcher m;
        Main.logger.trace("addException");
        ExceptionMessage _msg = new ExceptionMessage(
                m_MessageContents,
                m_msgName,
                m_msg1
        );
        SetStdFieldsAndAdd(_msg);

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
                    req.AddToDB(m_tables);

                }
            } catch (Exception ex) {
                logger.log(org.apache.logging.log4j.Level.FATAL, ex);
            }
        }
        GMSORSMessage _msg = new GMSORSMessage(threadID, dp.rest);
        _msg.SetInbound(false);
        SetStdFields(_msg);
        threadReqs.put(threadID, _msg);
    }

    private void addORSResponseMessage(String resp) {
        GMSORSMessage msgResp = new GMSORSMessage(newThread);
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
        GMSORSMessage msgResp = new GMSORSMessage(newThread, m_MessageContents);
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
                msgReq.AddToDB(m_tables);
            }
            msgResp.AddToDB(m_tables);

        } catch (Exception ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
        }
        threadReqs.remove(savedThreadID);
        savedThreadID = null;
    }

    @Override

    void init(HashMap<TableType, DBTable> m_tables) {
        m_tables.put(TableType.GMSStart, new GMSStartTable(Main.getM_accessor(), TableType.GMSStart));
        m_tables.put(TableType.GMSStatServerMessage, new GMSStatServerMsgTable(Main.getM_accessor(), TableType.GMSStatServerMessage));
        m_tables.put(TableType.GMSWebClientMessage, new GMSWebClientMsgTable(Main.getM_accessor(), TableType.GMSWebClientMessage));
        m_tables.put(TableType.IxnGMS, new IxnGMSTable(Main.getM_accessor(), TableType.IxnGMS));

    }

    private static class StartSessionTransition implements ParserThreadsProcessor.StateTransition {

        private final static Pattern regParams = Pattern.compile("^\\s*Params:");

        @Override
        public ParserThreadsProcessor.StateTransitionResult stateTransition(ParserThreadState threadState,
                String sOrig, String sParsedAndTruncated, String threadID, Parser parser) {
            Matcher m;

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
                    if ((m = regKVList.matcher(sParsedAndTruncated)).find()) {
                        threadState.addString(sOrig);
                        return ParserThreadsProcessor.StateTransitionResult.NON_STATE_LINE_WAITED;
                    } else {
                        GMSStartMessage msg = new GMSStartMessage(threadState.getMsgLines());
                        msg.SetStdFieldsAndAdd(parser);
                        return ParserThreadsProcessor.StateTransitionResult.FINAL_REACHED;

                    }

            }
            return ParserThreadsProcessor.StateTransitionResult.ERROR_STATE;
        }

    }

    enum ParserState {
        STATE_HEADER,
        STATE_TMESSAGE_REQUEST,
        STATE_ORSMESSAGE,
        STATE_COMMENT,
        STATE_TMESSAGE_EVENT,
        STATE_CLUSTER,
        STATE_HTTPIN,
        STATE_HTTPHANDLEREQUEST,
        STATE_POST,
        STATE_TMESSAGE_ATTRIBUTES,
        STATE_EXCEPTION,
        STATE_ORS_RESPONSE, STATE_ORS_RESPONSE_EXCEPTION, STATE_IXNMESSAGE
    }

    enum MsgType {
        MSG_UNKNOWN,
        MSG_MM_OUT,
        MSG_MM_IN,
        MSG_TLIB
    }

    private class GMSStatServerMsg extends Message {

        private String ssApp;
        private String ssHost;
        private String ssPort;
        private String ssEvent;
        private String reqID;
        private String stringValue;
        private boolean corruptMessage = false;

        private GMSStatServerMsg(String substring) {
            this();
            Matcher m;

            if ((m = ptStatMessageStart.matcher(substring)).find()) {
                this.ssApp = m.group(1);
                this.ssHost = m.group(2);
                this.ssPort = m.group(3);
                this.ssEvent = m.group(4);
                String attrs = substring.substring(m.end());
                m = ptStatMessageAttr.matcher(attrs);
                while (m.find()) {
                    if (m.group(1).equals("REQ_ID")) {
                        this.reqID = m.group(2);
                    } else if (m.group(1).equals("STRING_VALUE")) {
                        this.stringValue = m.group(2);
                    }
                }
            } else {
                this.corruptMessage = true;
            }
        }

        private GMSStatServerMsg(TableType t) {
            super(t);
        }

        private GMSStatServerMsg() {
            this(TableType.GMSStatServerMessage);
        }

        public boolean isCorruptMessage() {
            return corruptMessage;
        }

        public String getSsApp() {
            return ssApp;
        }

        public String getSsHost() {
            return ssHost;
        }

        public Integer getSsPort() {
            return getIntOrDef(ssPort, (Integer) null);
        }

        public String getSsEvent() {
            return ssEvent;
        }

        public Long getReqID() {
            return getIntOrDef(reqID, (Long) null);
        }

        public String getStringValue() {
            return stringValue;
        }

    }

    private class GMSStatServerMsgTable extends DBTable {

        public GMSStatServerMsgTable(DBAccessor dbaccessor, TableType t) {
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

            m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
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
            GMSStatServerMsg rec = (GMSStatServerMsg) _rec;
            if (!rec.isCorruptMessage()) {
                PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

                try {
                    stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                    stmt.setInt(2, GMSStatServerMsg.getFileId());
                    stmt.setLong(3, rec.m_fileOffset);
                    stmt.setLong(4, rec.getM_FileBytes());
                    stmt.setLong(5, rec.m_line);

                    setFieldInt(stmt, 6, Main.getRef(ReferenceType.App, rec.getSsApp()));
                    setFieldInt(stmt, 7, Main.getRef(ReferenceType.Host, rec.getSsHost()));
                    setFieldInt(stmt, 8, rec.getSsPort());
                    setFieldInt(stmt, 9, Main.getRef(ReferenceType.TEvent, rec.getSsEvent()));
                    setFieldLong(stmt, 10, rec.getReqID());
                    setFieldString(stmt, 11, rec.getStringValue());

                    getM_dbAccessor().SubmitStatement(m_InsertStatementId);
                } catch (SQLException e) {
                    Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
                }
            }
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
            allParams = Arrays.asList(new Pair[MAX_WEB_PARAMS]);
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
//                    System.out.println(string);
                }

            }
        }

        private GMSWebClientMsg() {
            super(TableType.GMSWebClientMessage);
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

    }

    private class GMSWebClientMsgTable extends DBTable {

        public GMSWebClientMsgTable(DBAccessor dbaccessor, TableType t) {
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
                    + buf.toString()
                    + ");";
            getM_dbAccessor().runQuery(query);

            buf = new StringBuilder();
            for (int i = 1; i <= MAX_WEB_PARAMS; i++) {
                buf.append(",?,?");
            }

            m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
                    + ",?"
                    + ",?"
                    + ",?"
                    + buf.toString()
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
            GMSWebClientMsg rec = (GMSWebClientMsg) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, GMSWebClientMsg.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.HTTPURL, rec.getURL()));
                stmt.setBoolean(7, rec.isResponse());
                setFieldInt(stmt, 8, rec.getWebReqID());

                List<Pair<String, String>> allParams = rec.getAllParams();
                int baseRecNo = 9;
                for (int i = 0; i < MAX_WEB_PARAMS; i++) {
                    Pair<String, String> get = null;
                    String val = null;
                    if (i < allParams.size()) {
                        get = allParams.get(i);
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

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }
//</editor-fold> 

}
