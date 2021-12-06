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
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

/**
 * @author ssydoruk
 */
public class OrsParser extends Parser {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;

    //2 types of thread messages in ORS logs
//21:37:08.194 [T:2560] {ScxmlMetric:3} METRIC <eval_expr sid='VIKO7I50UD32135O60O7ACO4NS0083UP' expression='varConsiderConcierge = _genesys.session.getListItemValue('LIST_CHT_SCT', v_ServiceCallType, 'Concierge Considered').toUpperCase();' result='' />
    private static final Pattern regWThread = Pattern.compile("^\\[([^\\]]+)");
    //21:37:07.710 {FMWeb:2} HandleRequest: Path '/scxml', file '/session/start', query ''
    private static final Pattern regFMThread = Pattern.compile("^\\{([^:\\}]+)");
    // general buffer
    private static final byte[] m_CharBuf = new byte[8048];
    //16:41:55.299 ors:+OrsEvent[0x01aa91f0]:name=interaction.partystatechanged
//2016-04-20T09:22:09.969 Trc 04541 Message EventAgentNotReady received from 'sipserver_01_p@+43891166406129992'
//09:22:09.970 [LS] '00AGCH16J4BHF6E1GNHNK2LAES00000R' ProcessEvent=AgentEventNotReady dev='+43891166406129992' queue='' mediaType=100  StateStorageCnt=1 Key[102-148-0-100]
    //16:41:55.299 Int 04543 Interaction message "EventRouteRequest" received from 65200 ("SIPTS750@3020")
//16:39:27.319 Trc 04541 Message EventRegistered received from 'SIPTS750@7593'
    //hopefully URS always print : message EventCallDataChanged after
    private static final Pattern regMsgStart = Pattern.compile(".+essage \"*(\\w+).+from.*(?: '([^'@]+)| \\(\"([^@\"]+))");
    private static final Pattern regMsgRequest = Pattern.compile("^request to \\d+\\(([^\\)]+)\\) message (\\w+)");
    private static final Pattern regMMMsgResponse = Pattern.compile(" '([^']+)' \\(\\d+\\) message:$");
    //    static Matcher regMsgStart = Pattern.compile("^\\s*(?:Int 04543|Trc 04541).+essage \\\"*(\\w+).+from \\d+ \\(\\\"([^@\\\"]+)");
    // : message EventServerInfo
    private static final Pattern regTMessageStart = Pattern.compile("\\s*: message ");
    private static final Pattern regTMessage1Start = Pattern.compile("^\\s[A-Z]");
    //static Matcher regMsgStart=Pattern.compile("^(Int 04543 |Trc 04541 )");
    private static final Pattern regMMsgStart = Pattern.compile("^\\s*attr_");
    // : message EventServerInfo
    private static final Pattern regOutMMsgStart = Pattern.compile("Message '(\\w+)' sent to '(\\w+)'");
    // : message EventServerInfo
    //16:41:55.283 W-NODE[272](RUNNING): >>>> 78 bytes to S-NODE[272] >>>>
//16:41:55.283 S-NODE[272](MASTER): <<<< 78 bytes from NODE[272] <<<<
    //" (W|S)-NODE\\[(\\d+)\\]\\D+(\\d+) bytes (to|from).+NODE\\[(\\d+)\\]"
    private static final Pattern regClusterMsg = Pattern.compile("^(W|S)-NODE\\[(\\d+)\\]\\D+(\\d+) bytes (to|from).+NODE\\[(\\d+)\\]");
    //16:17:19.815 CTITM: R:323 UpdateUData(1,3020)
    //static Matcher regCTITM = Pattern.compile("");
    private static final Pattern m_strCTITM = Pattern.compile("^\\s*CTITM:\\s+(.+)$");
    private static final Pattern regCTITMReq = Pattern.compile("^R:(\\d+) ([\\w~]+)\\(([^,]+),([^\\)]+)");
    private static final Pattern regNewCallID = Pattern.compile("^\\s*CALL\\[([^\\]]+)\\]: new id (\\d+)");
    private static final Pattern regConfigUpdate = Pattern.compile("Configuration object (?:Cfg)?(\\w+) (\\w+). DBID (\\d+), name (.+)$");
    //11:19:06.379 [SessionManager]: adding key[IDEN4PA4QH5PJ2F786QKA47CPC0066RU]/value[5PQD29NRV96QVB8GF448P0ICEK0016KC]
    private static final Pattern regGidUuid = Pattern.compile("adding key\\[(\\w+)]/value\\[([\\w~]+)\\]");
    //13:40:18.552 {SessionManager:2} URL [http://orswas.internal.gslb.service.nsw.gov.au/mod-routing/src-gen/IPD_default_mainWorkflow.scxml] associated with script [mod-routing]
    private static final Pattern regAppURL = Pattern.compile("^\\}\\s(?:Alternate )*URL \\[([^\\]]+)\\] associated with script \\[([^\\]]+)\\]$");
    //09:25:15.864 {ORSURS:3} InvokeFunctionalModule: <<
//	refID	28
//	call	'028AS5H6BGBHF6E1GNHNK2LAES00000F'
//	session	'004BUJ1C0KBHF8NIG7HNK2LAES000008'
//	module	'urs'
//	method	'SCXMLSubmit'
//	args	'[1,"VQ_Residential_RP",0,"","any",1,0,0,"VQUser@.A"]'
// <<
//09:25:15.864 [T:140084682483456] {ScxmlMetric:3} METRIC <eval_condition sid='004BUJ1C0KBHF8NIG7HNK2LAES000008' condition='system.ANI == ''' result='false' />
    private static final Pattern regORSURS = Pattern.compile(" (InvokeFunctionalModule|HandleREvent): <<$");
    private static final Pattern regAlarm = Pattern.compile("^\\s*([^\\(]+) \\(([\\d\\.]+)\\):\\s*");
    private static final Pattern regORSURSEnd = Pattern.compile("\\s<<");
    private static final Pattern regSTATE_HTTPOUT = Pattern.compile("\\~ORS_HTTPResponse\\[(.+)\\]$");
    //19:36:55.156 HTTP[56,TCP] <<<< 413 bytes from 171.164.220.27 <<<<
    private static final Pattern regHTTPin = Pattern.compile("^HTTP\\[(\\d+)\\,(?:TCP|TLS)\\] <<<< (\\d+) bytes from ([\\d\\.]+) <<<<$");
    //21:06:45.840 HTTP[42,TCP] >>>> 131 bytes >>>>
    private static final Pattern regHTTPout = Pattern.compile("^HTTP\\[(\\d+)\\,(?:TCP|TLS)\\] >>>> (\\d+) bytes >>>>$");
    private static final Pattern regHTTPignore = Pattern.compile(": OnConnect|client .+ exception|discard pending resonse| client [\\d\\.]+ disconnected");
    private static final Pattern regFileURL = Pattern.compile("^[\\w~]+:");
    private static final Pattern regNotParseMessage = Pattern.compile(
            //07:47:39.182 Trc 09900  [qtp747464370-17] >>> /1/service/callback/RCC_CORK_CallBack - CCID [null]
            "^(04543"
                    + "|04541"
                    + ")");
    //    10:38:29.173  -State[102-56433-0-100] processing is ... 10:38:29.173  skipped.
    private static final Pattern regStateProcessing = Pattern.compile("-State\\[[^\\]]+\\] processing is \\.{3}\\s*");
    private static final Pattern regMetricMessage = Pattern.compile("^<([\\w~]+) sid='([\\w~]+)'");
    private static final Pattern regRelatedSession = Pattern.compile("\\s*name='done\\.start\\.([\\w~]+)'");
    private static final Pattern regExtentionName = Pattern.compile("^\\s*name='([^']+)'");
    private static final Pattern regURL = Pattern.compile("^\\s*url='?([^'\\s]+)'?");
    private static final Pattern reInside = Pattern.compile("Inside \\S+ Block:|"
            + "Code Generated by Composer:|"
            + "Diagram created/upgraded by Composer:|"
            + "Project version:|"
            + "Diagram version:");
    /*
    16:41:55.768 METRIC <state_enter sid='KL8938QSD91ER2RN2NR1397738000001' name='Telstra.globalstate' type='state' />
    16:41:55.768 METRIC <transition sid='KL8938QSD91ER2RN2NR1397738000001' target='Telstra.Entry1' line='2859' />
    16:41:55.768 METRIC <log sid='KL8938QSD91ER2RN2NR1397738000001' expr='KL8938QSD91ER2RN2NR1397738000001: Inside Entry Block: Entry1' label='' level='1' />
     */
    static String m_strMetric = "METRIC ";
    static String httpStart = "HTTP[";
    private final HashMap<Integer, OrsHTTP> partHTTP = new HashMap<>();
    private final HashMap<String, String> ThreadAlias = new HashMap<>();
    HashMap<String, String> prevSeqno = new HashMap();
    //    private final HashMap<String, ParserState> threadParserState = new HashMap<>();
    MsgType MessageType;
    ParserThreadsProcessor ptpsThreadProcessor;
    private long m_CurrentFilePos;
    private String m_msgName;
    private String m_TserverSRC;
    private ParserState m_ParserState;
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
    private String thread; // current thread name as parsed fom the line
    private String URL = null;
    private String app = null;
    private boolean isInbound = true;
    private Message msg = null; // used to store method not committed to DB yet

    OrsParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_ORS, m_tables);
        ThreadAlias.put("ORSInternal", "FMWeb");
        ptpsThreadProcessor = new ParserThreadsProcessor();
//        ParserThreadsProcessor ptp = new ParserThreadsProcessor();

    }

    private OrsHTTP getFullHTTP(OrsHTTP orsHTTP) {
        OrsHTTP tmpOrsHTTP = partHTTP.get(orsHTTP.getSocket());
        if (tmpOrsHTTP != null) { // pending HTTP request
            tmpOrsHTTP.AddBytes(orsHTTP);
            tmpOrsHTTP.SetFileBytes(orsHTTP.getM_fileOffset() + orsHTTP.getFileBytes() - tmpOrsHTTP.getM_fileOffset());
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
                thread = getThreadAlias(m.group(1));
                return s.substring(m.end());
            }
            if ((m = regFMThread.matcher(s)).find()) {
                thread = getThreadAlias(m.group(1));
                return s.substring(m.end());
            }
        }
        thread = null;
        return s;
    }

    //    private ParserState getParserStateByThread() throws Exception {
//        if (thread == null) {
//            return m_ParserState;
//        }
//        ParserState ret = threadParserState.get(thread);
//        return (ret != null) ? ret : m_ParserState;
//    }
//
//    private void SetParserThreadState(String thr, ParserState ps) throws Exception {
//        if (thr == null) {
//            throw new Exception("No thread");
//        }
//        if (ps == ParserState.STATE_COMMENT || ps == ParserState.STATE_HEADER) {//by default there is no state to parser
//            threadParserState.remove(thr);
//        } else {
//            threadParserState.put(thr, ps);
//        }
//    }
    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_input = input; // to use from subroutines
        m_CurrentFilePos = offset;
        m_CurrentLine = line;
        setFileInfo(fi);

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
                Main.logger.trace("** l:" + m_CurrentLine
                        + " fp:" + getFilePos()
                        + " cons:" + input.getLastBytesConsumed()
                        + " [" + str + "]");

                try {
                    int l = input.getLastBytesConsumed();
//                    SetBytesConsumed(input.getLastBytesConsumed());
                    setEndFilePos(getFilePos() + l); // to calculate file bytes

                    while (str != null) {
                        str = ParseLine(str, input);
                    }
                    setFilePos(getEndFilePos());

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                }

//                m_CurrentFilePos += str.length()+input.charsSkippedOnReadLine;
            }
//            ParseLine("", null); // to complete the parsing of the last line/last message
        } catch (IOException e) {
            Main.logger.error(e);
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

    private String parseGenesys(String str) throws Exception {
        return ParseGenesys(str, TableType.MsgORServer, regNotParseMessage);
    }

    private String ParseLine(String str, BufferedReaderCrLf in) throws Exception {
        String s = str;
        Matcher m;
        boolean parsedGenesys = false;
        thread = null;

        // ignore that garbage string and continue processing string that is in the same line
        if ((m = regStateProcessing.matcher(s)).find()) {
            Main.logger.trace("%% [" + s.substring(m.end()) + "]");
            return s.substring(m.end());
        }

        ParseCustom(str, 0);

        if (ptpsThreadProcessor.threadPending()) {
            if (!ptpsThreadProcessor.isThreadWaitsForNonthreadMessage()) {
                s = parseGenesys(str);
                s = getThread(s);
                parsedGenesys = true;
            }
            if (ptpsThreadProcessor.threadConsume(str, s, thread, this)) {
                Main.logger.trace("threadConsumed");
                return null;
            }
        }

        Main.logger.trace("main thread state: " + m_ParserState + " thread: [" + thread + "]");
        switch (m_ParserState) {
            case STATE_HEADER: {
                if (s.isEmpty()) {
                    m_ParserState = ParserState.STATE_COMMENT;
                }
            }
            break;

            case STATE_COMMENT:
                m_lineStarted = m_CurrentLine;
                if (!parsedGenesys) {
                    s = parseGenesys(str);
                    s = getThread(s);
                }
                Main.logger.trace("latest thread ID: [" + thread + "] s[" + s + "]");

                if (s.startsWith(httpStart)) {
//                    Main.logger.info("Http line not supported yet: ["+str+"]");
                    return ProcessHTTP(getDP());
                } else if ((m = regNewCallID.matcher(s)).find()) {
                    addCallID(m.group(1), m.group(2));
                } else if ((m = regConfigUpdate.matcher(s)).find()) {
                    ConfigUpdateRecord theMsg = new ConfigUpdateRecord(m_MessageContents,  fileInfo.getRecordID());
                    theMsg.setObjName(m.group(4));
                    theMsg.setOp(m.group(2));
                    theMsg.setObjectDBID(m.group(3));
                    theMsg.setObjectType(m.group(1));

                    SetStdFieldsAndAdd(theMsg);

                } else if ((m = m_strCTITM.matcher(s)).find()) {
                    AddORSCTIMessage(m.group(1));
                } else if (CheckMetric(s)) {
                    break;
                } // now parsing log message text
                else if (CheckTMessage(s, str)) {
                    break;
                } else if (CheckMMessage(s, str)) {
                    break;
                } else if ((m = regClusterMsg.matcher(s)).find()) {
                    AddClusterMessage(m, in, str.length() + in.charsSkippedOnReadLine);
                    //m_ParserState=STATE_CLUSTER;
                } else if ((m = regGidUuid.matcher(s)).find()) {
                    AddGidUuidMessage(m.group(1), m.group(2));
                    //m_ParserState=STATE_CLUSTER;
                } else if ((m = regAppURL.matcher(s)).find()) {
                    String sTMP = m.group(1);
                    this.app = m.group(2);
                    if (regFileURL.matcher(sTMP).find()) {
                        this.URL = sTMP;
                    }
                    //m_ParserState=STATE_CLUSTER;
                } else if ((regORSURS.matcher(s)).find()) {
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_ORSUS;
                    setSavedFilePos(getFilePos());
                    //m_ParserState=STATE_CLUSTER;
                } else if (thread != null && !thread.isEmpty() && thread.startsWith("SelfMonitoring")) {
                    msg = new ORSAlarm();
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_ALARM;
                    setSavedFilePos(getFilePos());
                }
                break;

            case STATE_ALARM: {
                if ((m = regAlarm.matcher(str)).find()) {
                    if (msg instanceof ORSAlarm) {
                        ((ORSAlarm) msg).addAlarm(m.group(1), m.group(2), str.substring(m.end()));
                        return null;
                    }

                } else {
                    if (msg instanceof ORSAlarm) {
                        SetStdFieldsAndAdd(msg);
                        msg = null;
                    }
                }
                m_ParserState = ParserState.STATE_COMMENT;
                return str;
            }

            case STATE_TMESSAGE_START:
                //	 : message EventServerInfo
                //static Matcher regTMessageName=Pattern.compile(": message (.+)");
                if (regTMessageStart.matcher(s).find()) {
                    m_ParserState = ParserState.STATE_TMESSAGE;
                    MessageType = MsgType.MSG_TLIB;
//                    setSavedFilePos(getFilePos());
//                    m_msgName = SubstrAfterPrefix(s, ": message ");
                } else if (regMMsgStart.matcher(s).find()) {
                    m_ParserState = ParserState.STATE_TMESSAGE;
                    MessageType = MsgType.MSG_MM_IN;
//                    setSavedFilePos(getFilePos());
//                    m_msgName = SubstrAfterPrefix(s, ": message ");
                } else if (regTMessage1Start.matcher(str).find()) {
                    m_MessageContents.add(str);
                    m_ParserState = ParserState.STATE_TMESSAGE;
                    MessageType = MsgType.MSG_TLIB;
                } else {
                    logger.debug("l:" + m_CurrentLine + " unexpected line in state " + m_ParserState);
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                }
                isInbound = true;
                break;

            case STATE_TMESSAGE_REQUEST:
                break;

//            case STATE_EXTENSION_FETCH1:
//                if ((m = regFetchURI.matcher(str)).find()) {
//                    ((ORSMetricExtension) msg).setFetchURI(m.group(1));
//                    m_ParserState = ParserState.STATE_EXTENSION_FETCH2;
//                } else {
//                    commitFetchExtention(msg);
//                }
//                break;
//
//            case STATE_EXTENSION_FETCH2:
//                if ((m = regFetchMethod.matcher(str)).find()) {
//                    ((ORSMetricExtension) msg).setFetchMethod(m.group(1));
//                }
//                commitFetchExtention(msg);
//                break;
            case STATE_TMESSAGE: {
                if (StringUtils.isEmpty(str)
                        || Character.isWhitespace(str.charAt(0))) {
                    m_MessageContents.add(str);
                } else {
                    switch (MessageType) {
                        case MSG_MM_IN:
                        case MSG_MM_OUT:
                            AddMMMessage(m_msgName, m_TserverSRC, MessageType == MsgType.MSG_MM_IN);
                            break;

                        case MSG_TLIB:
                        case MSG_UNKNOWN:
                            AddORSMessage(m_msgName, m_TserverSRC);
                            break;
                    }
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
//                    if (str.length() > 0) {
                    //if(regDate.matcher(str).find()){ // assume that end of TMessage is always timestamp from new line
//                        ParseLine(str, in);
//                    }
                    return str;
                }
            }
            break;

            case STATE_CLUSTER:
                if (s.endsWith("/>")) {
                    AddMetricMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                } else {
                    m_MessageContents.add(s);
                }
                break;

            case STATE_ORSUS:
                if (regORSURSEnd.matcher(str).find()) {
                    AddORSURSMessage();
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;

                } else {
                    if (s.length() > 1) {
                        m_MessageContents.add(s.substring(1));
                    } else {
                        m_MessageContents.add(s);
                    }
                }
                break;

            case STATE_ORSMESSAGE:
                break;

        }

        return null;
    }

    private void AddMMMessage(String m_msgName, String m_TserverSRC, boolean isInbound) throws Exception {
        ORSMM theMsg = new ORSMM(m_msgName, m_TserverSRC, m_MessageContents, isInbound,  fileInfo.getRecordID());

        theMsg.setM_Timestamp(getLastTimeStamp());
        theMsg.SetOffset(getSavedFilePos());
        theMsg.SetFileBytes(getFilePos() - getSavedFilePos());
        theMsg.SetLine(m_lineStarted);
        theMsg.setM_isInbound(isInbound);
        theMsg.AddToDB(m_tables);

//        prevSeqno.put(msg.getM_TserverSRC(), msg.getAttributeTrim("AttributeEventSequenceNumber"));
        m_MessageContents.clear(); // clear messages
    }

    private void AddORSMessage(String msgName, String TServerSRC) throws Exception {
        ORSMessage themsg = new ORSMessage(msgName, TServerSRC, m_MessageContents, false,  fileInfo.getRecordID());

        themsg.setM_Timestamp(getLastTimeStamp());
        themsg.SetOffset(getSavedFilePos());
        themsg.SetFileBytes(getFilePos() - getSavedFilePos());
        themsg.SetLine(m_lineStarted);
        themsg.setM_isInbound(isInbound);
        themsg.AddToDB(m_tables);

        prevSeqno.put(themsg.getM_TserverSRC(), themsg.getAttributeTrim("AttributeEventSequenceNumber"));
        m_MessageContents.clear(); // clear messages
    }

    private String SubstrAfterPrefix(String str, String prefix) {
        int i = str.indexOf(prefix);
        if (i >= 0) {
            return str.substring(i + prefix.length());
        } else {
            return "";
        }
    }

    private void AddClusterMessage(Matcher m, BufferedReaderCrLf in, int startLen) throws Exception {
        try {

            int msgBytes = Integer.parseInt(m.group(3));
            if (msgBytes == 0 || msgBytes > m_CharBuf.length) {
                throw new Exception("error bytes to read");
            }
            int bytesRead = in.read(m_CharBuf, 0, msgBytes);
            if (bytesRead != msgBytes) {
                throw new Exception("error reading");
            }
            ORSClusterMessage themsg = new ORSClusterMessage(new String(m_CharBuf, 0, bytesRead),  fileInfo.getRecordID());
            themsg.setM_Timestamp(getLastTimeStamp());
            themsg.SetOffset(getFilePos());
            themsg.SetFileBytes(getEndFilePos() - getFilePos());

            themsg.SetLine(m_lineStarted);
            themsg.setDirection("from".equals(m.group(4)));
            themsg.setSrcNodeID(Integer.parseInt(m.group(2)));
            themsg.setSrcNodeType(m.group(1));
            themsg.setDstNodeID(Integer.parseInt(m.group(5)));
            m_CurrentFilePos += bytesRead;
//            msg.setM_Bytes(startLen+bytesRead+1); // add 1 for new line added by ORS
            //msg.setDstNodeType(m.group(2));
            themsg.AddToDB(m_accessor);

        } catch (Exception e) {
            throw e;
        }

    }

    private void AddMetricMessage(String MetricClause) throws Exception {
        Matcher m;
        String Method = null;
        String sid = null;
        String rest = null;

        if ((m = regMetricMessage.matcher(MetricClause)).find()) {
            Method = m.group(1);
            sid = m.group(2);
            rest = MetricClause.substring(m.end());
        }
        if (Method != null) {
            if (Method.equals("extension")) {
                msg = new ORSMetricExtension(MetricClause, Method, sid,  fileInfo.getRecordID());
                ((ORSMetricExtension) msg).parseNS(rest);

                if ((m = regExtentionName.matcher(rest)).find()
                        && m.groupCount() > 0) {
                    if (m.group(1).equalsIgnoreCase("fetch")) { // special handling for 'fetch'. Try to determine 'fetch' parameters
                        ParserThreadState tps = new ParserThreadState(
                                new TransitionFetch(),
                                ParserThreadsProcessor.ParserState.STATE_EXTENSION_FETCH1);
                        tps.setFilePos(getFilePos());
                        tps.addString(MetricClause);
                        tps.setHeaderOffset(m_CurrentFilePos);
                        tps.setMsg(msg);

                        ptpsThreadProcessor.addThreadState(thread, tps);
                        return;
                    } else if (m.group(1).equalsIgnoreCase("start")) { // special handling for 'start'. Try to determine 'start' parameters
                        String ns = ((ORSMetricExtension) msg).getNameSpace();
                        if (ns != null && ns.equals("session")) {
                            ParserThreadState tps = new ParserThreadState(
                                    new TransitionSessionStartExt(),
                                    ParserThreadsProcessor.ParserState.STATE_SESSION_START);
                            tps.setFilePos(getFilePos());
                            tps.addString(MetricClause);
                            tps.setHeaderOffset(m_CurrentFilePos);
                            tps.setMsg(new ORSSessionStartMessage(  fileInfo.getRecordID()));

                            SetStdFieldsAndAdd(msg);
                            ptpsThreadProcessor.addThreadState(thread, tps);
                            return;
                        }
                    }
                }
            } else {
                msg = new ORSMetric(MetricClause, Method, sid,  fileInfo.getRecordID());
                if (Method.equals("event_queued")) {
                    if ((m = regRelatedSession.matcher(rest)).find()) {
                        String relatedSid = m.group(1);
                        OrsSidSid sidsid = new OrsSidSid(sid, relatedSid,  fileInfo.getRecordID());
                        sidsid.AddToDB(m_tables);
                    }
                } else if (Method.equals("log")) {
                    if (reInside.matcher(rest).find()) {
                        ((ORSMetric) msg).setMethod("log_inside");
                    }

                } else if (Method.equals("doc_request")) {
                    Matcher m1;
                    if ((m1 = regURL.matcher(rest)).find()) {
                        ((ORSMetric) msg).setParam1(m1.group(1));
                    }

                }
            }
        } else {
            msg = new ORSMetric(MetricClause,  fileInfo.getRecordID());
        }

        msg.setM_Timestamp(getLastTimeStamp());
        msg.SetOffset(getFilePos());
        msg.SetFileBytes(getEndFilePos() - getFilePos());
        msg.SetLine(m_lineStarted);
        msg.AddToDB(m_tables);
        m_MessageContents.clear(); // clear messages
    }

    private void AddGidUuidMessage(String UUID, String GID) throws Exception {
        switch (UUID.length()) {
            case 32: {
                //uuid
                OrsSidUuid themsg = new OrsSidUuid(UUID, GID,  fileInfo.getRecordID());
                themsg.setURI(URL);
                themsg.setApp(app);
                SetStdFieldsAndAdd(themsg);
                break;
            }
            case 16: {
                OrsSidIxnID themsg = new OrsSidIxnID(UUID, GID,  fileInfo.getRecordID());
                themsg.setURI(URL);
                themsg.setApp(app);
                SetStdFieldsAndAdd(themsg);
                break;
            }
            default:
                throw new Exception("Unknownn IxnID: " + UUID);
        }
        URL = null;
        app = null;
    }

    private void AddORSCTIMessage(String str) throws Exception {
        Matcher m;
        Main.logger.trace("AddORSCTIMessage [" + str + "]");
        if ((m = regCTITMReq.matcher(str)).find()) {
            ORSMessage themsg = new ORSMessage(m.group(2), null, null, true,  fileInfo.getRecordID());
            themsg.setM_refID(m.group(1));
            themsg.setCallID(m.group(3));
            themsg.setM_ThisDN(m.group(4));
            themsg.setM_isInbound(false);

            themsg.setM_Timestamp(getLastTimeStamp());
            themsg.SetOffset(getFilePos());
            themsg.SetFileBytes(getEndFilePos() - getFilePos());
            themsg.SetLine(m_lineStarted);
            themsg.AddToDB(m_tables);

            //m_ParserState=STATE_CLUSTER;
        }
    }

    private boolean CheckMetric(String s) throws Exception {
        int p = s.indexOf(m_strMetric);
        if (p >= 0) {
//                m_HeaderOffset = m_CurrentFilePos;
//                m_TotalLen=str.length()+in.charsSkippedOnReadLine; // str as we cut date
            if (s.endsWith("/>")) { // METRIC on one line
                AddMetricMessage(s.substring(p + m_strMetric.length()));
            } else {
                m_MessageContents.add(s.substring(p + m_strMetric.length()));
                m_ParserState = ParserState.STATE_CLUSTER;

            }
            return true;
        }
        return false;
    }

    private boolean CheckTMessage(String s, String str) {
        Matcher m = null;
        GenesysMsg lastLogMsg = getLastLogMsg();
        if (lastLogMsg != null && (lastLogMsg.getLastGenesysMsgID().equals("04543")
                || lastLogMsg.getLastGenesysMsgID().equals("04541"))
                && (m = regMsgStart.matcher(s)).find()) {
            m_msgName = m.group(1);

            if (m.group(2) != null) {
                m_TserverSRC = m.group(2);
            } else if (m.group(3) != null) {
                m_TserverSRC = m.group(3);
            } else {
                m_TserverSRC = "";
            }

            m_ParserState = ParserState.STATE_TMESSAGE_START;
            setSavedFilePos(getFilePos());
            return true;
        } else if ((m = regMsgRequest.matcher(s)).find()) {
            m_msgName = m.group(2);
            m_TserverSRC = m.group(1);

            m_ParserState = ParserState.STATE_TMESSAGE;
            setSavedFilePos(getFilePos());
            isInbound = false;
            MessageType = MsgType.MSG_TLIB;
            return true;
        }

        return false;
    }

    private boolean CheckMMessage(String s, String str) {
        Matcher m;

        if ((m = regOutMMsgStart.matcher(s)).find()) {
            m_msgName = m.group(1);
            if (m.group(2) != null) {
                m_TserverSRC = m.group(2);
            } else {
                m_TserverSRC = "";
            }

            m_ParserState = ParserState.STATE_TMESSAGE;
            setSavedFilePos(getFilePos());
            MessageType = MsgType.MSG_MM_OUT;
            return true;
        } else if ((m = regMMMsgResponse.matcher(s)).find()) {
            m_msgName = m.group(1);

            m_ParserState = ParserState.STATE_TMESSAGE;
            setSavedFilePos(getFilePos());
            isInbound = false;
            MessageType = MsgType.MSG_MM_IN;
            return true;
        }
        return false;
    }

    private void AddMetricMessage() throws Exception {
        ORSMetric themsg = new ORSMetric(m_MessageContents,  fileInfo.getRecordID());

        themsg.setM_Timestamp(getLastTimeStamp());
        themsg.SetOffset(getFilePos());
        themsg.SetFileBytes(getEndFilePos() - getFilePos());
        themsg.SetLine(m_lineStarted);

        themsg.AddToDB(m_tables);
        m_MessageContents.clear(); // clear messages
    }

    private void AddORSURSMessage() throws Exception {
        OrsUrsMessage themsg;
        if (m_MessageContents.size() > 0) {
            Matcher m;
            if ((m = regORSURS.matcher(m_MessageContents.get(0))).find()) {
                themsg = new OrsUrsMessage(m.group(1), m_MessageContents,  fileInfo.getRecordID());
            } else {
                themsg = new OrsUrsMessage("", m_MessageContents,  fileInfo.getRecordID());
            }

            SetStdFieldsAndAdd(themsg);
        }

    }

    private String ProcessHTTP(DateParsed dp) throws Exception {
        Matcher m;

        try {
            if ((m = regHTTPin.matcher(dp.rest)).find()) {
                int socket = Integer.parseInt(m.group(1));
                int bytes = Integer.parseInt(m.group(2));
                String ip = m.group(3);
                ReadHTTP(bytes);

                OrsHTTP orsHTTP = new OrsHTTP(m_MessageContents,  fileInfo.getRecordID());
                orsHTTP.setIP(ip);
                orsHTTP.setSocket(socket);
                orsHTTP.setBytes(bytes);
                orsHTTP.SetInbound(true);
                SavePos(orsHTTP);
                SaveTime(orsHTTP);

                orsHTTP = getFullHTTP(orsHTTP);

                if (orsHTTP != null) {
                    if (thread == null) {
                        thread = "HTTP" + orsHTTP.getSocket();
                    } else {
                        Main.logger.error("thread supposed to be empty but is [" + thread + "]");
                    }
//                    if (orsHTTP.SessionStart()) {
//                        SetParserThreadState("FMWeb", ParserState.STATE_HTTPIN);
//                        m_ParserState = ParserState.STATE_HTTPIN;
//                        recInProgress = orsHTTP;

                    ParserThreadState tps = new ParserThreadState(
                            new TransitionHTTPIn(),
                            ParserThreadsProcessor.ParserState.STATE_HTTPIN);
                    tps.setFilePos(getFilePos());
                    tps.setHeaderOffset(m_CurrentFilePos);
                    tps.setMsg(orsHTTP);
                    tps.setWaitNonthread(true); //'name' of HTTP thread is different from what it expects

                    ptpsThreadProcessor.addThreadState(thread, tps);
                    return null;

//                    } else {
//                        orsHTTP.AddToDB(m_tables);
//                        m_ParserState = ParserState.STATE_COMMENT;
//                        m_MessageContents.clear();
//                    }
                }
            } else if ((m = regHTTPout.matcher(dp.rest)).find()) {
                int socket = Integer.parseInt(m.group(1));
                int bytes = Integer.parseInt(m.group(2));
                ReadHTTP(bytes);
                OrsHTTP orsHTTP = new OrsHTTP(m_MessageContents,  fileInfo.getRecordID());
                orsHTTP.setBytes(bytes);
                orsHTTP.setSocket(socket);
                orsHTTP.SetInbound(false);
                orsHTTP = getFullHTTP(orsHTTP);
                if (orsHTTP != null) {
                    ParserThreadState tps = new ParserThreadState(
                            new TransitionHTTPOut(),
                            ParserThreadsProcessor.ParserState.STATE_HTTPOUT);
                    tps.setFilePos(getFilePos());
                    tps.setHeaderOffset(m_CurrentFilePos);
                    tps.setMsg(orsHTTP);
                    tps.setWaitNonthread(true); //'name' of HTTP thread is different from what it expects

                    ptpsThreadProcessor.addThreadState(thread, tps);
                }
            } else if ((regHTTPignore.matcher(dp.rest)).find()) {
                Main.logger.debug("Ignored HTTP: [" + dp.orig + "]");
                return null;
            } else {
                throw new Exception("Http not recognized, line: " + dp.orig);
            }
        } catch (NumberFormatException numberFormatException) {
            Main.logger.error("Parsing HTTP failed for " + dp.orig, numberFormatException);
        } catch (IOException ex) {
            logger.error("fatal: ", ex);
        }
        return null;
    }

    private int ParseHTTP(StringBuilder buf) {
        int linesRead = 0;
        int i = 0, start = 0, newLine;

        m_MessageContents.clear();
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

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
        m_tables.put(TableType.ORSCallID, new ORSCallIDTable(Main.getInstance().getM_accessor(), TableType.ORSCallID));
        m_tables.put(TableType.ORSAlarm, new ORSAlarmTable(Main.getInstance().getM_accessor(), TableType.ORSAlarm));
        m_tables.put(TableType.ORSSessionStart, new ORSSessionStartTable(Main.getInstance().getM_accessor(), TableType.ORSSessionStart));

    }

    private void addCallID(String UUID, String callID) {
        (new ORSCallID(UUID, callID,  fileInfo.getRecordID())).AddToDB(m_tables);

    }

    enum ParserState {
        STATE_HEADER,
        STATE_TMESSAGE,
        STATE_ORSMESSAGE,
        STATE_COMMENT,
        STATE_TMESSAGE_START,
        STATE_CLUSTER,
        STATE_HTTPIN,
        STATE_HTTPHANDLEREQUEST,
        STATE_ORSUS,
        STATE_TMESSAGE_REQUEST,
        STATE_EXTENSION_FETCH1,
        STATE_EXTENSION_FETCH2, STATE_ALARM
    }

    enum MsgType {
        MSG_UNKNOWN,
        MSG_MM_OUT,
        MSG_MM_IN,
        MSG_TLIB
    }

    private static class TransitionFetch implements ParserThreadsProcessor.StateTransition {

        private static final Pattern regFetchMethod = Pattern.compile("Fetch Method '([^']*)'$");
        private static final Pattern regFetchURI = Pattern.compile("Fetch URI '([^']*)'$");

        @Override
        public ParserThreadsProcessor.StateTransitionResult stateTransition(ParserThreadState threadState,
                                                                            String sOrig, String sParsedAndTruncated, String threadID, Parser parser) {
            Matcher m;
            Message msg1 = threadState.getMsg();

            switch (threadState.getParserState()) {
                case STATE_EXTENSION_FETCH1: {
                    if ((m = regFetchURI.matcher(sParsedAndTruncated)).find()) {
                        String uri = m.group(1);
                        if (uri == null || uri.isEmpty()) {
                            uri = null;
                        }
                        if (uri == null) {
                            ((ORSMetricExtension) msg1).setFetchURI("<EMPTY>");
                        } else {
                            ((ORSMetricExtension) msg1).setFetchURI(m.group(1));
                            if (uri.toLowerCase().startsWith("http")) {
                                commitFetchExtention(msg1, parser);
                                return ParserThreadsProcessor.StateTransitionResult.FINAL_REACHED;
                            } else {
                                threadState.setParserState(ParserThreadsProcessor.ParserState.STATE_EXTENSION_FETCH2);
                                return ParserThreadsProcessor.StateTransitionResult.STATE_CHANGED;
                            }
                        }

                    } else {
                        Main.logger.debug("l:" + parser.getM_CurrentLine() + " 1 - Unexpected thread message: " + sParsedAndTruncated);
                        commitFetchExtention(msg1, parser);
                        return ParserThreadsProcessor.StateTransitionResult.ERROR_STATE;
                    }
                }

                case STATE_EXTENSION_FETCH2:
                    //07:23:25.200 [T:47294533855552] {ORSInternal:3} ~OrsEvent[0x80566090]:name=start
                    // this seem to signify end of paremeters for HTTP request
                    if (!sParsedAndTruncated.endsWith("name=start")) {

                        if ((m = regFetchMethod.matcher(sParsedAndTruncated)).find()) {
                            ((ORSMetricExtension) msg1).setFetchMethod(m.group(1));
                        } else {
                            Main.logger.debug("l:" + parser.getM_CurrentLine() + " 2 - Unexpected thread message: " + sParsedAndTruncated);
                        }
                    }
                    commitFetchExtention(msg1, parser);
                    return ParserThreadsProcessor.StateTransitionResult.FINAL_REACHED;

                default:
                    Main.logger.error("l:" + parser.getM_CurrentLine() + " unexpected state " + threadState.getParserState() + " s[" + sOrig + "]");

            }
            return ParserThreadsProcessor.StateTransitionResult.ERROR_STATE;
        }

        private void commitFetchExtention(Message msg, Parser parser) {
            if (msg instanceof ORSMetricExtension) {
                msg.SetStdFieldsAndAdd(parser);
            } else {
                throw new UnsupportedOperationException("commitFetchExtention: unexpected type of msg"); //To change body of generated methods, choose Tools | Templates.
            }
        }
    }

    private static class TransitionSessionStartExt implements ParserThreadsProcessor.StateTransition {

        private static final Pattern regSessionStartParam = Pattern.compile("Session start param");
        private static final Pattern regSessionStartMessage = Pattern.compile("HandleDataFromThread: Starting new session. SessionID=([\\w~]+)$");
        private static final Pattern regFMSession = Pattern.compile("\\{FMSession:");

        @Override
        public ParserThreadsProcessor.StateTransitionResult stateTransition(ParserThreadState threadState,
                                                                            String sOrig, String sParsedAndTruncated, String threadID, Parser parser) {
            Matcher m;
            ORSSessionStartMessage msg = (ORSSessionStartMessage) threadState.getMsg();

            switch (threadState.getParserState()) {
                case STATE_SESSION_START: {
                    if ((m = regSessionStartParam.matcher(sParsedAndTruncated)).find()) {
//                        threadState.addString(sParsedAndTruncated.substring(m.end()));
                        msg.addParam(sParsedAndTruncated.substring(m.end()));
                        return ParserThreadsProcessor.StateTransitionResult.STATE_CHANGED;
                    } else {
                        threadState.setParserState(ParserThreadsProcessor.ParserState.STATE_WAITING_PARENT_SID);
                        return ParserThreadsProcessor.StateTransitionResult.NON_STATE_LINE_WAITED_NOT_CONSUMED;
                    }
                }

                case STATE_WAITING_PARENT_SID: {
                    if ((m = regFMSession.matcher(sOrig)).find()) {
                        String rest = sOrig.substring(m.end());
                        if ((m = regSessionStartMessage.matcher(rest)).find()) {
                            msg.setNewSessionID(m.group(1));
                        } else {
                            Main.logger.error("Unexpected message in " + threadState.getParserState() + ": [" + sOrig + "]");
                        }
                        msg.SetStdFieldsAndAdd(parser);
                        return ParserThreadsProcessor.StateTransitionResult.FINAL_REACHED;
                    } else {
                        return ParserThreadsProcessor.StateTransitionResult.NON_STATE_LINE_WAITED_NOT_CONSUMED;
                    }
                }
                default:
                    Main.logger.error("l:" + parser.getM_CurrentLine() + " unexpected state " + threadState.getParserState() + " s[" + sOrig + "]");

            }
            Main.logger.trace("SessionStartExtTransition "
                    + "sOrig[" + sOrig + "] "
                    + "threadID[" + threadID + "]"
                    + "parser[" + parser + "]"
            );
            return ParserThreadsProcessor.StateTransitionResult.ERROR_STATE;
        }

    }

    private  class TransitionHTTPOut implements ParserThreadsProcessor.StateTransition {

        //        private static final Pattern regSessionStartParam = Pattern.compile("Session start param");
//        private static final Pattern regSessionStartMessage = Pattern.compile("HandleDataFromThread: Starting new session. SessionID=(\\w+)$");
//        private static final Pattern regFMSession = Pattern.compile("\\{FMSession:");
        @Override
        public ParserThreadsProcessor.StateTransitionResult stateTransition(ParserThreadState threadState,
                                                                            String sOrig, String sParsedAndTruncated, String threadID, Parser parser) {
            Matcher m;
            OrsHTTP msg = (OrsHTTP) threadState.getMsg();

            switch (threadState.getParserState()) {
                case STATE_HTTPOUT:

                    if (sOrig == null || sOrig.isEmpty()) {
                        return ParserThreadsProcessor.StateTransitionResult.NON_STATE_LINE_WAITED;
                    } else if ((m = regSTATE_HTTPOUT.matcher(sOrig)).find()) {
                        msg.setHTTPResponseID(m.group(1));
                        msg.SetStdFieldsAndAdd(parser);
                        return ParserThreadsProcessor.StateTransitionResult.FINAL_REACHED;
                    } else {
                        msg.SetStdFieldsAndAdd(parser);
                        return ParserThreadsProcessor.StateTransitionResult.FINAL_REACHED_CONTINUE;
                    }
                    //error by default

                default:
                    Main.logger.error("l:" + parser.getM_CurrentLine() + " unexpected state " + threadState.getParserState() + " s[" + sOrig + "]");

            }
            Main.logger.trace("SessionStartExtTransition "
                    + "sOrig[" + sOrig + "] "
                    + "threadID[" + threadID + "]"
                    + "parser[" + parser + "]"
            );
            return ParserThreadsProcessor.StateTransitionResult.ERROR_STATE;
        }

    }

    private class ORSCallID extends Message {

        private final String UUID;
        private final Integer callID;

        private ORSCallID(String UUID, String callID, int fileID) {
            super(TableType.ORSCallID, fileID);
            this.UUID = UUID;
            this.callID = intOrDef(callID, -1);
        }

        private int CallID() {
            return callID;
        }

        private String getUUID() {
            return UUID;
        }

    }

    private class ORSCallIDTable extends DBTable {

        public ORSCallIDTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {

            addIndex("callID");
            addIndex("UUIDID");
            addIndex("FileId");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",FileId INTEGER"
                    + ",callID INTEGER"
                    + ",UUIDID INTEGER"
                    + ");";
            getM_dbAccessor().runQuery(query);
            m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(?"
                    /*standard first*/
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
            ORSCallID rec = (ORSCallID) _rec;
            getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
                 stmt.setInt(1, rec.getRecordID());
                stmt.setInt(2, rec.getFileID());
                stmt.setInt(3, rec.CallID());
                setFieldInt(stmt, 4, Main.getRef(ReferenceType.UUID, rec.getUUID()));

                }
            });
        }


    }

    private class ORSAlarm extends Message {

        private final ArrayList<AlarmInstance> allAlarms;

        private ORSAlarm() {
            super(TableType.ORSAlarm,  fileInfo.getRecordID());
            this.allAlarms = new ArrayList<>();
        }

        private void addAlarm(String name, String limit, String rest) {
            allAlarms.add(new AlarmInstance(name, limit, rest));
        }

        public ArrayList<AlarmInstance> getAllAlarms() {
            return allAlarms;
        }

        class AlarmInstance {

            private final String rest;
            private final String limit;
            private final String name;

            private AlarmInstance(String name, String limit, String rest) {
                this.name = name;
                this.limit = limit;
                this.rest = rest;
            }

            @Override
            public String toString() {
                return "AlarmInstance{" + "rest=" + rest + ", limit=" + limit + ", name=" + name + '}';
            }

            public String getName() {
                return name + " (" + limit + ")";
            }

            public String getRest() {
                return rest;
            }
        }

    }

    public class ORSAlarmTable extends DBTable {

        public static final int MAX_ALARMS = 15;

        public ORSAlarmTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            StringBuilder buf = new StringBuilder();
            addIndex("time");
            addIndex("FileId");
            for (int i = 1; i <= MAX_ALARMS; i++) {
                addIndex("alarm" + i + "id");
                addIndex("values" + i + "id");
            }

            dropIndexes();

            for (int i = 1; i <= MAX_ALARMS; i++) {
                buf.append(",alarm").append(i).append("id int");
                buf.append(",values").append(i).append("id int");
            }

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + buf
                    + ");";
            getM_dbAccessor().runQuery(query);

            buf = new StringBuilder();
            for (int i = 1; i <= MAX_ALARMS; i++) {
                buf.append(",?,?");
            }
            m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
                    + buf
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
            ORSAlarm rec = (ORSAlarm) _rec;
            getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileID());
                stmt.setLong(3, rec.getM_fileOffset());
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.getM_line());

                int baseRecNo = 6;
                ArrayList<ORSAlarm.AlarmInstance> allAlarms = rec.getAllAlarms();
//                for (int i = 0; i < allAlarms.size(); i++) {
//                    if (i >= MAX_ALARMS) {
//                        Main.logger.error(rec.getM_line() + ": more alarms then max (" + MAX_ALARMS + "):" + allAlarms.toString());
//                        break;
//                    }
//                    setFieldInt(stmt, baseRecNo + i * 2, Main.getRef(ReferenceType.Misc, allAlarms.get(i).getName()));
//                    setFieldInt(stmt, baseRecNo + i * 2 + 1, Main.getRef(ReferenceType.Misc, allAlarms.get(i).getRest()));
//                }
                for (int i = 0; i < MAX_ALARMS; i++) {
                    if (i < allAlarms.size()) {
                        setFieldInt(stmt, baseRecNo + i * 2, Main.getRef(ReferenceType.Misc, allAlarms.get(i).getName()));
                        setFieldInt(stmt, baseRecNo + i * 2 + 1, Main.getRef(ReferenceType.Misc, allAlarms.get(i).getRest()));
                    } else {
                        setFieldInt(stmt, baseRecNo + i * 2, null);
                        setFieldInt(stmt, baseRecNo + i * 2 + 1, null);
                    }
                }
                }
            });
        }


    }

}
