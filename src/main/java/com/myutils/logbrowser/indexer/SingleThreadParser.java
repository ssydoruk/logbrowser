/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;
import static com.myutils.logbrowser.indexer.SingleThreadParser.ParserState.*;

/**
 * @author ssydoruk
 */
public class SingleThreadParser extends Parser {
    public static final int MAX_1536_ATTRIBUTES = 40;
    static final String[] BlockNamesToIgnoreArray = {
            "NET:TCO:EventResourceInfo:",
            "NET:IPR:EventResourceInfo:",
            "SIP:CTI:GVP_RESOURCES_CHANGED:",
            "SIP:CTI:HA_SEND_SYNC_MESSAGE:",
            "SIP:CTI:UNKNOWN_HA_MESSAGE:",
            "NET:CTI:UNKNOWN_HA_MESSAGE:",
            "SIP:CTI:DEVICE_REGISTERED:",
            "NET:CTI:tControllerSync:",
            "CTI:TCO:SIP_CTI_CLUSTER_NODE_CONTROL_SIP_REGISTRATION:",
            "CTI:SIP:HA_TAKE_SYNC_MESSAGE:",
            "SIP:CTI:sipStackSync:",
            "NET:CTI:sipStackSync:",
            "NET:IPR:RequestPrivateService:",
            "SIP:CTI:tControllerSync:",
            "CTI:TCO:SIP_CTI_CLUSTER_NODE_CONTROL_HA_TAKE_SYNC_MESSAGE:",
            "SIP:CTI:callSync:"
    };
    //15:37:21.918 +++ CIFace::Request +++
//   -- new invoke
//  Parsed: RequestQueryAddress
//  From: ICON_Voice[704]/16165
//  Numbers: +<SIP to RM> -<none>
//  Status: parsed:1 queued:0 sent:0 acked:0 preevent:0 event:0 context:0 transferred:0
//  -----
    private static final Pattern regFrom = Pattern.compile("From: .+/([0-9]+)");
    private static final Pattern regNumbers = Pattern.compile("Numbers: \\+<([^>]+)> -<([^>]+)>");
    private static final Pattern regSIPHeaderIP = Pattern.compile("(?:(\\S+) (>>>>>|<<<<<))$");
    private static final Pattern regSIPHeaderNetwork = Pattern.compile("(?:(\\S+) (>>>|<<<))$");
    private static final Pattern regTMessageContinue = Pattern.compile("^(\\t|message|\\d*\\s\\[BSYNC\\] Trace:)");
    private static final Pattern regTBackupMessage = Pattern.compile("^\\d*\\s\\[BSYNC\\] Trace: Send");
    private static final Pattern regTLibMessage = Pattern.compile("(: message)");
    private static final Pattern regTLibMessageLog = Pattern.compile("^0454[12]");
    private static final Pattern regTriggerStart = Pattern.compile("^\\$((\\*:)|\\+|-)");
    private static final Pattern regConfigUpdate = Pattern.compile("^[\\d\\s]+\\[TCONF\\]");
    private static final Pattern regTimerActions = Pattern.compile("^\\s+CIFace: Sent CRequest@(?:\\w+) (\\w+)-internal$");
    private static final Pattern regTimerActionsParams = Pattern.compile("^\\s+-- aTmCall (\\w+)(?:\\sSetCause: (\\w+))?");
    private static final Pattern regConfigOneLineDN = Pattern.compile("^\\s\\[(\\d+)\\] dn = '([^']+)' type = (\\w+)");
    private static final Pattern ptSkip = Pattern.compile("^[:\\s]*");
    private static final Pattern regProxy = Pattern.compile("^Proxy\\((\\w+):");
    private static final Pattern regConnidChangeEnd = Pattern.compile("^\\S");
    private static final Pattern SIPHeaderContinue = Pattern.compile("^\\s*(\\w.+)");
    private static final Pattern SIPHeaderFound = Pattern.compile("^((\\w[\\w_\\-\\.\\s]+:)|\\s+;)");
    private static final Pattern regSIPHeader = Pattern.compile("(\\d+) bytes .+ (>>>>>|<<<<<)$");
    //    15:22:50.980: Sending  [0,UDP] 3384 bytes to 10.82.10.146:5060 >>>>>
    private static final Pattern regNotParseMessage = Pattern.compile("^(0454[1-5]"
            + ")");
    private final static HashSet<String> eventsWithCallInfo = new HashSet<String>(
            Arrays.asList("EventRegistered",
                    "EventAddressInfo"));
    private static final Pattern regISCCHead = Pattern.compile("^[\\d\\s]+\\[ISCC\\] (Received|Send).+: message (\\w+)$");
    private static final Pattern regJSONFinished = Pattern.compile("^GenHttpRequest.+destroyed$");
    private static final Pattern patternHandleBlock = Pattern.compile("^((([0-9a-zA-Z_-]*:)+)\\d+):?\\d*$");
    private static final Pattern regHandlerCutSeconds = Pattern.compile("^((?:(?:\\D[^:]+):)++\\d+)");
    private static final Pattern regOldConnID = Pattern.compile("was (\\w+)\\)$");
    private static final Pattern regNewConnID = Pattern.compile("\\@ c:(\\w+),");
    private static final Pattern regCfgObjectName = Pattern.compile("(?:name|userName|number|loginCode)='([^']+)'");
    private static final Pattern regCfgObjectDBID = Pattern.compile("DBID=(\\d+)");
    private static final Pattern regCfgObjectType = Pattern.compile("object Cfg(\\w+)=");
    private static final Pattern regCfgOp = Pattern.compile("\\(type Object(\\w+)\\)");
    private static final Pattern regSIPServerStartDN = Pattern.compile("^\\s*DN added \\(dbid (\\d+)\\) \\(number ([^\\(]+)\\) ");
    private static final String sipTrunkStatistics = "sipTrunkStatistics ";
    private static final String sipRegGenerated = "requests GENERATED_";
    private static final String sipResGenerated = "response GENERATED_";
    private static final Pattern regKeyValueLine = Pattern.compile("(\\w+)=(?:\"([^\"]+)\"|(\\w+))");
    final int MSG_STRING_LIMIT = 200;
    private final ArrayList<String> extraBuff;
    boolean m_handlerInProgress;
    // parse state contants
    HashSet m_BlockNamesToIgnoreHash;
    StringBuilder sipBuf = new StringBuilder();
    long m_CurrentFilePos;
    long m_HeaderOffset;
    int m_PacketLength;
    Integer m_ContentLength;
    String m_Header;
    int m_headerLine;
    boolean m_isInbound;
    boolean m_isContentLengthUnknown;
    //boolean m_lastMsgSip = false;
    int m_lastMsgType = -1;
    String m_lastTime;
    SIP1536OtherMessage sip1536OtherMessage = null;
    private int m_handlerId;
    private int m_sipId;
    private int m_tlibId;
    private int m_jsonId;
    private boolean ifSIPLines = false;
    // 0-tlib, 1-sip, 2-json
    private boolean ifSIPLinesForce = false;
    private boolean gaveWarning = false;
    private ParserState m_ParserState;
    private String msgName;
    private boolean inbound;
    private DateParsed dpHeader;
    private boolean haMessage;
    private String handleAdd;
    private Message msgInProgress;
    private FileInfo.FileType fileType = FileInfo.FileType.UNKNOWN;
    private long lastSeqNo = 0;

    public SingleThreadParser(DBTables m_tables) {
        super(FileInfoType.type_SessionController, m_tables);
        Main.logger.debug("SingleThreadParser");
        this.extraBuff = new ArrayList<>();
        //m_accessor = accessor;
        m_BlockNamesToIgnoreHash = new HashSet();
        m_BlockNamesToIgnoreHash.addAll(Arrays.asList(BlockNamesToIgnoreArray));
        ifSIPLinesForce = Main.ifSIPLines();

    }

    public int getM_handlerId() {
        return m_handlerId;
    }

    public int getM_sipId() {
        return m_sipId;
    }

    public int getM_tlibId() {
        return m_tlibId;
    }

    public int getM_jsonId() {
        return m_jsonId;
    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentFilePos = offset;
        m_CurrentLine = line;
        m_handlerInProgress = false;


        long name = fi.getAppNameID() + fi.getFileStartTimeRound();
        this.handleAdd = "|" + name;
        Main.logger.debug("handlerAdd:" + handleAdd);

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
                Main.logger.trace("**l: " + m_CurrentLine + " [" + str + "]");

                try {
                    SetBytesConsumed(input.getLastBytesConsumed());

                    setEndFilePos(getFilePos() + getBytesConsumed()); // to calculate file bytes

//                    if (input.charsSkippedOnReadLine > 0) {
//                        ParseLine(input, str);
//                    } else {
//                        unfinished = str;
//                    }
                    while (str != null) {
                        str = ParseLine(input, str);
                    }
                    if (!extraBuff.isEmpty()) {
                        for (String string : extraBuff) {
                            while (string != null) {
                                Main.logger.trace("Tracing extra: [" + string + "]");
                                string = ParseLine(input, string);
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
                ParseLine(input, unfinished);
            } else {
                ContinueParsing(unfinished);
            }

            ParseLine(input, ""); // to complete the parsing of the last line/last message
        } catch (Exception e) {
            Main.logger.error(e);
        }


        return m_CurrentLine - line;
    }

    void ContinueParsing(String initial) {
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
                ParseLine(input, str);
                if (m_ParserState == STATE_COMMENTS) {
                    break;
                }
            }
        } catch (Exception e) {
            Main.logger.error(e);
        }
    }

    String ParseLine(BufferedReaderCrLf input, String str) throws Exception {
        Matcher m;
        GenesysMsg lastLogMsg;

        Main.logger.trace("l:" + m_CurrentLine + "-" + m_ParserState + " s[" + str + "]");
        String s = str;
        switch (fileType) {
            case SIP_1536:
                return ParseLine1536(input, str);
        }

        ParseCustom(str, (m_handlerInProgress ? m_handlerId : 0));
        switch (m_ParserState) {
            case STATE_HEADER: {
                if (s.startsWith("File:")) {
                    m_ParserState = STATE_COMMENTS;
                }
            }
            break;

            case STATE_COMMENTS:
                m_PacketLength = 0;
                setSavedFilePos(getFilePos());

                try {
                    s = ParseGenesysTServer(str, TableType.MsgTServer, regNotParseMessage, ptSkip);
                } catch (Exception exception) {
                    throw exception; // do nothing so far. Can insert code to ignore certain exceptions
                }

                m_lineStarted = m_CurrentLine;

                if ((m = regConfigOneLineDN.matcher(str)).find()) {
                    ConfigUpdateRecord msg = new ConfigUpdateRecord(str, fileInfo.getRecordID());
                    try {
                        msg.setObjectType("DN");
                        msg.setObjectDBID(m.group(1));
                        msg.setObjName(m.group(2));
                        msg.setMsg(m.group(3));
                        SetStdFieldsAndAdd(msg);
                    } catch (Exception e) {
                        Main.logger.error("Not added \"" + msg.getM_type() + "\" record:" + e.getMessage(), e);
                    }

                } else if ((m = regConfigUpdate.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s.substring(m.end()));
                    m_ParserState = STATE_CONFIG;
                } else if ((m = regTriggerStart.matcher(s)).find()) {
                    HandleBlock(m.group(1), s.substring(m.end()));
//                } else if (trimmed.contains(" Proxy(") || str.startsWith("Proxy(")) {
                } else if ((m = regProxy.matcher(s)).find()) {
                    ProxiedMessage msg = new ProxiedMessage(m.group(1), s.substring(m.end()), getFileID());
                    SetStdFieldsAndAdd(msg);

                    return null;
                } else if (s.contains("{tscp.call")) {
                    HandleConnId(s);
                    return null;
                } //<editor-fold defaultstate="collapsed" desc="reading sip">
                else if ((m = regSIPHeader.matcher(s)).find()) {
                    Main.logger.trace("SIP header");
                    try {
                        m_PacketLength = Integer.parseInt(m.group(1));

                    } catch (NumberFormatException e) {
                        break;
                    }
                    // use case: keep alive messages from F5
                    // TODO: implement SIP message length checking
                    if (m_PacketLength < 50) {
                        break;  // to short for SIP Message
                    }
                    m_Header = s;

                    dpHeader = dp; // so time of the message correctly set in AddSipMessage

                    m_HeaderOffset = m_CurrentFilePos;
                    m_headerLine = m_CurrentLine;
                    if (ifSIPLinesForce || ifSIPLines) {
                        m_MessageContents.clear();
                        setSavedFilePos(getFilePos());
//                        m_MessageContents.add(s);
                        m_ParserState = STATE_SIP_HEADER;
                        return null;
                    } else {
//                    setSavedFilePos(getFilePos());
                        sipBuf.append(input.readBytes(m_PacketLength + 1));
                        int bytesRead = input.getLastBytesConsumed();
                        int addBytes = Utils.StringUtils.CountStrings(sipBuf, "\r\r");
                        int nR = Utils.StringUtils.CharOccurences(sipBuf, '\r');
                        int nN = Utils.StringUtils.CharOccurences(sipBuf, '\n');
                        if (addBytes > 0) {
                            sipBuf.append(input.readBytes(addBytes));
                            bytesRead += input.getLastBytesConsumed();
                            m_CurrentLine += nR;
                        } else {
                            m_CurrentLine += nN;
                        }
                        SetBytesConsumed(0);// file pos already set to, to avoid setting filepos in calling function
                        m_isInbound = m.group(2).endsWith("<");
                        setEndFilePos(getEndFilePos() + bytesRead);

//                        Main.logger.trace("In SIP message: \\r: " + CharOccurences(sipBuf, '\r') + "; \\n" + CharOccurences(sipBuf, '\n'));
                        if (nR == 0) {//workaround for FTP log parsing that strips 0x0d
                            if (!gaveWarning) {
                                JOptionPane.showMessageDialog(null, "No \\n(0x0d) character in SIP header; Log file probably corrupt when file transferred\n"
                                        + "Will now parse SIP only line by line.\n"
                                        + "Check the results after parsing\n"
                                        + "To force SIP by-line reading, use -DSIPLINES=1 in the command line", "File corrupt", JOptionPane.INFORMATION_MESSAGE);
                                ifSIPLines = true;
                                gaveWarning = true;
                            }
                            extraBuff.addAll(Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(sipBuf.toString(), ((nN > 0) ? "\n" : "\r"))));
                            return str;

                        }

                        if (FullSIPMsg()) {
                            m_MessageContents.clear();
                            String theBuf = sipBuf.toString();
                            if (Character.isWhitespace(theBuf.charAt(0))) {
                                /*Normally SIP message header line ends with just \n character. 
                                So another stripping detected when 
                                all there are \n\r only in the file as the result; */
                                m_CurrentLine--;
                                m_MessageContents.addAll(Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(theBuf.substring(1), ((nN > 0) ? "\n" : "\r"))));

                            } else {
//                        if( NormalSIP(theBuf))                        
//                            m_MessageContents.addAll(Arrays.asList(theBuf.split("\r*\r\n")));
//                        else
//                            m_MessageContents.addAll(Arrays.asList(theBuf.split("[\r\n]+")));

//                            m_MessageContents.addAll(Arrays.asList(theBuf.split("\r*\r\n")));
                                m_MessageContents.addAll(Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(theBuf, ((nN > 0) ? "\n" : "\r"))));
                            }
                            AddSipMessage(m_MessageContents, m_Header);
                            m_MessageContents.clear();
                            sipBuf.setLength(0);
                        }
                        setFilePos(getEndFilePos() + bytesRead);
                    }

                    break;

//</editor-fold>
                } else if ((regTBackupMessage.matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
                    m_ParserState = STATE_TLIB_BACKUP_MESSAGE;
                    break;
//                } else if (str.startsWith("@")
//                        && (str.contains("message ISCCEvent") || str.contains("message ISCCRequest"))) {

                } else if ((m = regISCCHead.matcher(s)).find()) {
//                    Main.logger.trace("-8-");
                    String act = m.group(1);
                    inbound = (act.charAt(0) == 'R');
                    msgName = m.group(2);

                    m_Header = s;
                    dpHeader = dp;

                    m_HeaderOffset = m_CurrentFilePos;
                    m_headerLine = m_CurrentLine;
                    m_MessageContents.clear();
                    m_MessageContents.add(str);
                    m_ParserState = STATE_ISCC_MESSAGE;
                    setSavedFilePos(getFilePos());
                    break;
                } else if ((regJSONFinished.matcher(s)).find()) {
                    m_handlerInProgress = false;
                    break;

                } else if (((lastLogMsg = getLastLogMsg()) != null && (regTLibMessageLog.matcher(lastLogMsg.getLastGenesysMsgID())).find())
                        || (regTLibMessage.matcher(s)).find()) {

                    Main.logger.trace("TLib message start");
                    m_Header = s;
                    dpHeader = dp;

                    m_HeaderOffset = m_CurrentFilePos;
                    m_headerLine = m_CurrentLine;
                    m_MessageContents.clear();
                    msgInProgress = new TLibMessage(lastLogMsg, m_handlerInProgress, m_handlerId, fileInfo.getRecordID());
                    m_ParserState = STATE_TLIB_MESSAGE;
                    setSavedFilePos(getFilePos());
                    break;
                } else if (s.contains("ConnectionId changed")) {
//                    Main.logger.trace("-19-");
                    m_MessageContents.clear();
                    m_MessageContents.add(s);
                    m_ParserState = STATE_CONNID_RECORD;
                    setSavedFilePos(getFilePos());
                    break;

                } else if (s.endsWith(" <<<") || s.endsWith(" >>>")) {
//                    Main.logger.trace("-11-");
                    m_Header = s;
                    dpHeader = dp;

                    m_HeaderOffset = m_CurrentFilePos;
                    m_headerLine = m_CurrentLine;
                    m_MessageContents.clear();
                    m_isInbound = !s.endsWith(">");
                    if (s.indexOf("T-Lib") > 0) {
                        m_ParserState = STATE_TLIB_MESSAGE;

                    } else {
                        m_ParserState = STATE_SIP_HEADER;
                        m_isContentLengthUnknown = false;
                    }
                    setSavedFilePos(getFilePos());
                    break;
                } else if ((m = regTimerActions.matcher(str)).find()) {
                    this.msgInProgress = new TLibTimerRedirectMessage(m.group(1));
                    setSavedFilePos(getFilePos());
                    m_ParserState = ParserState.STATE_TIMER_REDIRECT;
                } else if (str.startsWith("@UA")) {
                    // note message; ignore...
                    break;
                } else if (str.startsWith("...exception")) {
                    // exception message; ignore...
                    break;

                } else if (s.startsWith("POST http:") || s.startsWith("HTTP/1.")) {
                    m_Header = s;
                    dpHeader = dp;

                    m_HeaderOffset = m_CurrentFilePos;
                    m_headerLine = m_CurrentLine;
                    m_MessageContents.clear();
                    m_ParserState = STATE_JSON_HEADER;
                    setSavedFilePos(getFilePos());
                    m_isContentLengthUnknown = false;
                    m_ContentLength = null;
                    break;
                } else if (s.startsWith("Parsed:")) {
                    m_Header = s;
                    dpHeader = dp;

                    m_HeaderOffset = m_CurrentFilePos;
                    m_headerLine = m_CurrentLine;
                    m_MessageContents.clear();
                    m_ParserState = STATE_CIREQ_MESSAGE;
                    setSavedFilePos(getFilePos());
                    break;

                } else if (s.length() >= 12) {
//                    Main.logger.trace("-12-");
                    m_lastTime = s.substring(0, 12);
                    // !!!TODO: this is a hack based on knowledge that line before http message
                    // has a timestamp. VERY DANGEROUS
                }
                break;
            case STATE_TIMER_REDIRECT: {
                if ((m = regTimerActionsParams.matcher(str)).find()) {
                    ((TLibTimerRedirectMessage) msgInProgress).setConnID(m.group(1));
                    String cause = m.group(2);
                    if (cause != null && !cause.isEmpty()) {
                        ((TLibTimerRedirectMessage) msgInProgress).setCause(cause);
                    }
                }
                msgInProgress.SetStdFieldsAndAdd(this);
                m_ParserState = STATE_COMMENTS;
                break;
            }

//<editor-fold defaultstate="collapsed" desc="state_SIP_HEADER">
            case STATE_SIP_HEADER: {
                if (!SIPHeaderFound.matcher(str).find()) {
                    m_MessageContents.add(str);
                } else {
                    m_ParserState = STATE_SIP_HEADER1;
                    return str;
                }

                break;
            }

            case STATE_SIP_HEADER1: {
                boolean doneParsing = false;
                if ((SIPHeaderFound.matcher(str)).find()) {
                    ParseTimestamp(str);
                    if (dp != null) { // protection against stupid converters that remove empty line at the end of SIP message
                        doneParsing = true;
                    } else {
                        m_MessageContents.add(str);
                        String contentL = Message.GetSIPHeader(s, "content-length", "l");
                        if (contentL != null) {
                            if (contentL.startsWith("[")) {
                                m_isContentLengthUnknown = true;
                            } else {
                                m_ContentLength = Integer.parseInt(contentL);
                            }
                        }
                    }
                } else {
                    doneParsing = true;
                }
                if (doneParsing) {
                    if ((m_ContentLength != null && m_ContentLength > 0) || m_isContentLengthUnknown) {
                        m_ParserState = STATE_SIP_BODY;
                        m_MessageContents.add(s);

                    } else {
                        m_ParserState = STATE_COMMENTS;
                        m_MessageContents.add("");
                        AddSipMessage(m_MessageContents, m_Header);
                        m_MessageContents.clear();
//                        if (m_PacketLength > 50) {
//                            m_ParserState = STATE_SIP_HEADER_GSIPLIB;
//                            m_MessageContents.clear();
//                            break;
//                        }
//                        ParseLine(input, str);
                    }
                    if (dp != null) {
                        return str;
                    }
                }
                break;
            }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="case STATE_JSON_HEADER:">
            case STATE_JSON_HEADER: {
                if (s.length() != 0) {
                    m_MessageContents.add(s);
                    if (m_ContentLength == null) {
                        String contentL = Message.GetSIPHeader(s, "content-length", "l");
                        if (contentL != null && !contentL.isEmpty()) {
                            if (contentL.startsWith("[")) {
                                m_isContentLengthUnknown = true;
                            } else {
                                m_ContentLength = Utils.Util.intOrDef(contentL, (Integer) null);
                            }
                        } else if (s.toLowerCase().startsWith("transfer-encoding: chunked")) {
                            m_isContentLengthUnknown = true;
                        }
                    }

                } else {
                    if (m_ContentLength > 0 || m_isContentLengthUnknown) {
                        m_ParserState = STATE_JSON_BODY;
                        m_MessageContents.add("");

                    } else {
                        m_ParserState = STATE_COMMENTS;
                        m_MessageContents.add("");
                        AddJsonMessage(m_MessageContents, m_Header);
                        return str;
                    }
                }
            }
            break;
//</editor-fold>
            case STATE_SIP_BODY: {
                m_PacketLength -= s.length() + 2;

                if ((SIPHeaderContinue.matcher(str)).find()) {
                    m_MessageContents.add(s);

                } else {
                    m_ParserState = STATE_COMMENTS;
                    AddSipMessage(m_MessageContents, m_Header);
                    m_MessageContents.clear();
                    return str;
                }

            }
            break;

            case STATE_JSON_BODY:
                if (m_isContentLengthUnknown) {
                    if (s.length() != 0) {
                        m_MessageContents.add(s);

                    } else {
                        m_ParserState = STATE_COMMENTS;
                        AddJsonMessage(m_MessageContents, m_Header);
                        m_MessageContents.clear();
//                        ParseLine(input, str);
                    }

                } else {
                    m_MessageContents.add(s);
                    m_ContentLength -= s.length() + 2;
                    if (m_ContentLength < 0) {
                        m_ParserState = STATE_COMMENTS;
                        m_CurrentFilePos += s.length() + 2;
                        AddJsonMessage(m_MessageContents, m_Header);
                        m_CurrentFilePos -= s.length() + 2;
                        m_MessageContents.clear();
//                        ParseLine(input, str);
                    }
                }
                break;

            case STATE_CONFIG:
                AddConfigMessage(m_MessageContents);
                m_MessageContents.clear();
                m_ParserState = STATE_COMMENTS;
                return str;

            case STATE_SIP_HEADER_GSIPLIB:
                m_PacketLength -= s.length() + 2;
                if (s.length() != 0) {
                    m_MessageContents.add(s);

                    String contentL = Message.GetSIPHeader(s, "content-length", "l");
                    if (contentL != null) {
                        if (contentL.startsWith("[")) {
                            m_isContentLengthUnknown = true;
                        } else {
                            m_isContentLengthUnknown = false;
                            m_ContentLength = Integer.parseInt(contentL);
                        }
                    }
                } else {
                    if (m_ContentLength > 0 || m_isContentLengthUnknown) {

                        m_ParserState = STATE_SIP_BODY_GSIPLIB;
                        m_MessageContents.add("");

                    } else {
                        m_ParserState = STATE_COMMENTS;
                        m_MessageContents.add("");
                        AddSipMessage(m_MessageContents, m_Header);
                        if (m_PacketLength > 50) {
                            m_ParserState = STATE_SIP_HEADER_GSIPLIB;
                            m_MessageContents.clear();
                            break;
                        }
                        ParseLine(input, str);
                    }
                }
                break;

            case STATE_SIP_BODY_GSIPLIB:
                m_PacketLength -= str.length() + 2;
                if (m_isContentLengthUnknown) {
                    if (s.length() != 0) {
                        m_MessageContents.add(s);

                    } else {
                        m_ParserState = STATE_COMMENTS;
                        AddSipMessage(m_MessageContents, m_Header);
                        if (m_PacketLength > 50) {
                            m_ParserState = STATE_SIP_HEADER_GSIPLIB;
                            m_MessageContents.clear();
                            break;
                        }
                        ParseLine(input, str);
                    }

                } else {
                    // lets limit string to MSG_STRING_LIMIT characters
                    if (s.length() > MSG_STRING_LIMIT && !s.startsWith("<?xml version=") && !s.startsWith("<Envelope xmlns=")) {
                        m_MessageContents.add(s.substring(0, MSG_STRING_LIMIT).concat("..."));
                    } else {
                        m_MessageContents.add(s);
                    }

                    m_ContentLength -= str.length() + 2;
                    if (m_ContentLength < 0) {

                        m_ParserState = STATE_POSSIBLE_MULTIPLE_SIP_MESSAGES;
                        AddSipMessage(m_MessageContents, m_Header);
                        if (m_PacketLength > 50) {
                            m_ParserState = STATE_SIP_HEADER_GSIPLIB;
                            m_MessageContents.clear();
                            break;
                        }
                        ParseLine(input, str);
                    }
                }
                break;

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
                ParseLine(input, str);
                break;

            case STATE_TLIB_MESSAGE:
                if (regTMessageContinue.matcher(str).find()) {
                    m_MessageContents.add(s);
                } else {
                    ParseGenesysTServer(str, TableType.MsgTServer, regNotParseMessage, ptSkip);
                    if (dp == null) {
                        m_MessageContents.add(s);
                    } else {
                        m_ParserState = STATE_COMMENTS;
                        if (m_MessageContents.size() > 0) {
                            AddTlibMessage(m_MessageContents, m_Header);
                        }
                        return str;
                    }
                }
                break;

            case STATE_TLIB_BACKUP_MESSAGE:
                if (!regTMessageContinue.matcher(str).find()
                        && !str.contains(": message EventAgent")) {
                    m_ParserState = STATE_COMMENTS;
                    Main.logger.trace("Ignored message to backup");
                    return str;
                }
                break;

            case STATE_TLIB_MESSAGE_SIP_SYNC:
                if (s.length() != 0 && !s.equals("'")) {
                    m_MessageContents.add(s);

                } else {
                    m_MessageContents.add(s);
                    m_ParserState = STATE_TLIB_MESSAGE;
                }
                break;

            case STATE_ISCC_MESSAGE:

                if (regTMessageContinue.matcher(str).find()) {
                    m_MessageContents.add(s);
                } else {
                    m_ParserState = STATE_COMMENTS;
                    if (m_MessageContents.size() > 0) {
                        AddIsccMessage(m_MessageContents, m_Header);
                    }
                    m_MessageContents.clear();
                    return str;
                }
                break;

            case STATE_CIREQ_MESSAGE:
                if (s.startsWith("-----")) {
                    m_ParserState = STATE_COMMENTS;
                    AddCireqMessage(m_MessageContents, m_Header);
                } else {
                    m_MessageContents.add(s);
                }
                break;

            case STATE_CONNID_RECORD:
                if (regConnidChangeEnd.matcher(str).find()) {
                    //new message started
                    HandleConnIdChange(m_MessageContents);
                    m_ParserState = STATE_COMMENTS;
                    return str;
                } else {
                    m_MessageContents.add(s);
                }
                break;
        }

        return null;
    }

    protected void AddSipMessage(ArrayList<String> contents, String header) throws Exception {
        String str;
        Matcher m;

        // Populate our class representation of the message
        SipMessage msg;

//        if (contents.isEmpty()) {
//            Main.logger.error("Could not find message itself!");
//            return;
//        }
//        String s = (String) contents.get(0);
//        if (s.length() == 0) {
//            Main.logger.error("Empty message - blank first line");
//            return;
//        }
//        if (s.indexOf(' ') == -1) {
//            Main.logger.error("Broken message - firstLine doesn't contain empty space; line " + m_lineStarted);
//            PrintMsg(contents);
//            return;
//        }
        msg = new SipMessage(contents, TableType.SIP, m_handlerInProgress, m_handlerId, fileInfo.getRecordID());

        if (msg.GetFrom() == null) {
            Main.logger.error("LOG ERROR: no From in SIP message, ignore, line " + m_CurrentLine);
            return;
        }

        if (msg.GetFrom().startsWith("<sip:gsipsync>")) {
            //  change to readable name
            msg.HaName();
        }

        if ((m = regSIPHeaderIP.matcher(header)).find()) {
//            workarround for this garbage
//            12:53:33.355: {Authorization Failed:297} Wrong Checksum.12:53:33.355: Sending  [0,UDP] 629 bytes to 10.17.29.37:5061 >>>>>

            if (m.group(2).startsWith(">")) // sending
            {
                msg.SetReceiverName(m.group(1));
                m_isInbound = false;
            } else // Receiving
            {
                msg.SetSenderName(m.group(1));
                m_isInbound = true;
            }
        } else if ((m = regSIPHeaderNetwork.matcher(header)).find()) {
            if (m.group(2).startsWith(">")) // sending
            {
                msg.SetReceiverName(m.group(1));
                m_isInbound = false;
            } else // Receiving
            {
                msg.SetSenderName(m.group(1));
                m_isInbound = true;
            }
        } else {
            Main.logger.debug("-----Still used???---");
            String[] headerList = header.split(" ");
            if (Main.logger.isTraceEnabled()) {
                Main.logger.info("header: " + header);
                for (String string : headerList) {
                    Main.logger.info(": [" + string + "]");
                }
            }
            if (headerList[1].equals("Received")
                    || headerList[2].equals("Received")
                    || headerList[1].equals("<<<<")) // Stream Manager
            {
                // Genesys network SIP T-Server format
                // Genesys new premise SIP T-Server format
                String fromStr = headerList[headerList.length - 2];
                msg.SetSenderName(fromStr);

            } else if (headerList[1].equals("Sending")
                    || headerList[1].equals(">>>>")
                    || // Stream Manager
                    headerList[2].equals("Sending")) {
                // Genesys network SIP T-Server format
                // Genesys new premise SIP T-Server format
                String toStr = headerList[headerList.length - 2];
                msg.SetReceiverName(toStr);

            } else if ((headerList.length > 7 && headerList[7].equals("received:"))
                    || (headerList.length > 8 && headerList[8].startsWith("received"))) //GVP
            {
                // GVP MCP
                // 15:22:10.306 Trc 33009 INFO 00000000-00000000 02800FA1 Request received: \
                // INVITE sip:dialog@192.168.50.168:5090;gvp.config.asr.load_once_per_call=1;\
                // gvp.config.sip.enablesendrecvevents=true;gvp.defaultsvxml=\
                // file://C:%5Cvps%5CBlind_Transfer%5Csrc-gen%5CMain.vxml;trunkport=6506903505;\
                // voicexml=file://C:%5Cvps%5CBlind_Transfer%5Csrc-gen%5CMain.vxml SIP/2.0
                // GVP RM
                // 2008-04-11 15:22:10.338 DBUG 00000000-00000000 09400901 CCPSIPMessageInterceptor.h:514\
                // SIP Message received (409): SIP/2.0 100 Trying

                // cut extra string in
                int endIndex = header.indexOf(';');
                if (endIndex == -1) {
                    endIndex = header.length();
                }
                int startIndex = header.indexOf("received:");  // GVP MCP
                if (startIndex != -1) {
                    startIndex += 10;
                } else {
                    startIndex = header.indexOf("):");  // GVP RM
                    if (startIndex != -1) {
                        startIndex += 3;
                    }
                }
                if (startIndex == -1) {
                    startIndex = 0;
                }

                String name = header.substring(startIndex, endIndex);
                msg.InsertHeader(name);

                // todo: set receiver name
            } else if ((headerList.length > 7)
                    && (headerList[7].equals("sent:")
                    || headerList[8].startsWith("sent")
                    || headerList[9].startsWith("sent"))) //GVP
            {
                // GVP MCP
                // 15:22:10.306 Trc 33009 INFO 00000000-00000000 02800FA1 Response sent: SIP/2.0 100 Trying
                // GVP RM
                // 2008-04-11 15:22:10.338 DBUG 00000000-00000000 09400901 CCPSIPMessageInterceptor.h:514 \
                // SIP Message sent (334): SIP/2.0 100 Trying

                // cut extra string in
                int endIndex = header.indexOf(';');
                if (endIndex == -1) {
                    endIndex = header.length();
                }
                int startIndex = header.indexOf("sent:");  // GVP MCP
                if (startIndex != -1) {
                    startIndex += 6;
                } else {
                    startIndex = header.indexOf("):");  // GVP RM
                    if (startIndex != -1) {
                        startIndex += 3;
                    }
                }
                if (startIndex == -1) {
                    startIndex = 0;
                }

                String name = header.substring(startIndex, endIndex);
                msg.InsertHeader(name);

                // TODO: set sender name
            } else if (headerList[1].equalsIgnoreCase("Message")) {
                // manually coded testcase format
                if (headerList[2].equalsIgnoreCase("From")) {

                    // TODO: set Receiver name if we ever run into this format
                    // in modern log files
                    Main.logger.error("Unexpected format. Please notify the tool owner.");

                } else if (headerList[2].equalsIgnoreCase("To")) {
                    // TODO: set Receiver name if we ever run into this format
                    // in modern log files
                    Main.logger.error("Unexpected format. Please notify the tool owner.");
                }

            } else if ((str = msg.GetSIPHeader("Content-Type", "c")) != null) {
                // ESS json

                // TODO: find out if we have to do something special for this format
                if (str.equalsIgnoreCase("application/json")) {
                    msg.SetName(header);
                }

            } else {
                Main.logger.error("Can't parse the string:" + header);
                return;
            }

            if ((headerList.length > 7)
                    && ((headerList[6].equals("SIP") && headerList[7].equals("Message"))
                    || //GVP
                    headerList[7].equals("SIP") && headerList[8].equals("Message"))) {
//                msg.SetTimestamp((dpHeader != null) ? dpHeader : ParseFormatDate(headerList[1]));
            } else if (headerList[0].equals("sipcs:")) {
//                msg.SetTimestamp((dpHeader != null) ? dpHeader : ParseFormatDate(headerList[1]));
            } else {
//                msg.SetTimestamp((dpHeader != null) ? dpHeader : ParseFormatDate(headerList[0]));
            }
        }

        msg.SetInbound(m_isInbound);

        m_lastMsgType = 1;

        String theMsgName = msg.GetName();
        if (theMsgName != null && theMsgName.equals("NOTIFY")) {
            String contentType = msg.GetSIPHeader("Content-Type", "c");
            String contentEvent = msg.GetSIPHeader("Event", null);

            if (contentType != null && contentType.equals("application/x-genesys-mediaserver-status")
                    && contentEvent != null && contentEvent.equals("x-genesys-mediaserver-status")) {
                Main.logger.debug("Cancel trigger on NOTIFY (Event=x-genesys-mediaserver-status)");
                m_handlerInProgress = false;
            }
        }

        try {
            SetStdFieldsAndAdd(msg);
        } catch (Exception e) {
            Main.logger.error("Error adding message line:" + m_lineStarted);
            PrintMsg(contents);
        }
    }

    protected void AddTlibMessage(ArrayList contents, String header) throws Exception {
        String hdr;

        if (haMessage) {
            Main.logger.debug("Ignoring HA message");
            return;
        }

        if (dpHeader == null) {
            hdr = header;
        } else {
            hdr = dpHeader.rest;
        }

        if (msgInProgress != null && msgInProgress instanceof TLibMessage) {
            TLibMessage msg = (TLibMessage) msgInProgress;
            msg.setData(hdr, m_MessageContents);
//            TLibMessage msg = new TLibMessage(hdr, m_MessageContents, getLastLogMsg());
            if (msg.getConnID() == null && msg.getUUID() == null && msg.getRefID() == null && msg.getErrorCode() == null
                    && msg.getThisDN() == null) {
                return;
            }
            if (eventsWithCallInfo.contains(msg.GetMessageName())) {
                ArrayList<ExtentionCall> extentionCalls = ExtentionCall.getExtentionCalls(m_MessageContents);
                if (extentionCalls != null) {
                    TLibMessage msg1 = new TLibMessage(msg, getLastLogMsg());
                    for (ExtentionCall extentionCall : extentionCalls) {
                        msg1.setConnID(extentionCall.getConnID());
                        msg1.setUUID(extentionCall.getUUID());
                        if (newSeqNo(msg1)) {
                            SetStdFieldsAndAdd(msg1);
                            m_tlibId = msg1.getRecordID();
                        }
                    }
                }
                if (newSeqNo(msg)) {
                    SetStdFieldsAndAdd(msg);
                    m_tlibId = msg.getRecordID();
                }

            } else {
                if (newSeqNo(msg)) {
                    SetStdFieldsAndAdd(msg);
                    m_tlibId = msg.getRecordID();
                }
            }
            m_lastMsgType = 0;
            msgInProgress = null;
        } else {
            Main.logger.error("l:" + m_CurrentLine + " incorrect message type");
        }
    }

    protected void AddJsonMessage(ArrayList<String> contents, String header) {
        int i = 0;
        while (!contents.get(i).isEmpty()) {
            i++;
        }
        String body = "";
        for (int j = i; j < contents.size(); j++) {
            body += contents.get(j);
        }

        try {
            if (m_ContentLength < 2) {
                Main.logger.debug("l:" + m_CurrentLine + " json body less then 2. Ignoring JSON message");
                return;
            }
            JSONObject obj = new JSONObject(body);
            try {
                JSONObject operation = obj.getJSONObject("operation");
                String opType = operation.getString("type");
                if (opType.equalsIgnoreCase("HEARTBEAT")) {
                    Main.logger.debug("l:" + m_CurrentLine + " ignoring heartbeat");
                    return;
                }
            } catch (JSONException e) {
                Main.logger.debug("l:" + m_CurrentLine + " - Cannot create JSON object out of [" + body + "]");
                return;
            }

            JsonMessage msg = new JsonMessage(obj, m_handlerInProgress, m_handlerId, fileInfo.getRecordID());
            msg.setM_Timestamp((dpHeader != null) ? dpHeader : ParseFormatDate(m_lastTime));
            msg.SetInbound(header.startsWith("HTTP/1."));
//            int headerLine = m_CurrentLine - contents.size();
//            if (headerLine < 1) {
//                headerLine = 1;
//            }
//            msg.SetLine(m_headerLine);
//            msg.SetFileBytes((int) (m_CurrentFilePos - m_HeaderOffset));
//            msg.SetOffset(m_HeaderOffset);

            if (msg.isRouting() && msg.isInbound()) {
                if (m_handlerInProgress) {
                    Main.logger.error("l: " + m_CurrentLine + " handler in progress");
                } else {
                    m_lastMsgType = 2;
                    saveHandler(true, "JSON 200 OK|" + handleAdd);
                }

            }

            SetStdFieldsAndAdd(msg);
            m_jsonId = msg.getRecordID();

        } catch (Exception e) {
            Main.logger.error("l:" + m_CurrentLine + " - Could not parse JSON object: " + contents + "\n" + e);
            // do not output because it can be XML instead of JSON
        }

    }

    protected void AddIsccMessage(ArrayList contents, String header) throws Exception {
        if (msgName != null) {
            IsccMessage msg = new IsccMessage(msgName, contents, m_handlerInProgress, m_handlerId, fileInfo.getRecordID());
            msg.SetInbound(inbound);
            SetStdFieldsAndAdd(msg);
        } else {
            Main.logger.error("Cannot understand ISCC header: [" + header + "]");
        }
    }

    protected void HandleBlock(String blockPrefix, String rest) throws Exception {
        Matcher m = patternHandleBlock.matcher(rest);
        if (m.find()) {
            String msgPrefix = m.group(2);
            if (msgPrefix != null && !msgPrefix.isEmpty()) {
                if (m_BlockNamesToIgnoreHash.contains(msgPrefix)) {
//                    Main.logger.info("Ignored");
                    return;
                }
            }
            if (Main.logger.isDebugEnabled()) {
                if (!rest.startsWith("NET:SIP::") && !rest.startsWith("TLIB:CTI:Unknown")) {
                    Main.logger.debug("Handling [" + blockPrefix + " - " + rest);
                }
            }

            String msg = m.group(1);
            String s;
            if ((m = regHandlerCutSeconds.matcher(msg)).find()) {
                s = m.group(1);
            } else {
                s = msg;
            }
            String triggerText = s + handleAdd;
//            String triggerText = matcher.group(3);

            if (blockPrefix.equals("*:")) {
                // save trigger in db

                saveTrigger(triggerText);

            } else if (blockPrefix.equals("+")) {
                // save in db and set db-id to pass to messages
                saveHandler(true, triggerText);

            } else if (blockPrefix.equals("-")) {
                // if block open reset db-id
                saveHandler(false, triggerText);

            }
        } else {
            Main.logger.info("Unknown [" + blockPrefix + " - " + rest);
        }
    }
//
//    private boolean isIgnoreMsg(String txt) {
//        if (txt.contains("HA_SWITCH_ACTIVE")
//                || txt.contains("HA_SWITCH_PASSIVE")
//                || txt.contains("HA_CONNECT_HANDLER")
//                || txt.contains("HA_TAKE_SYNC_MESSAGE")) {
//            return true;
//        } else {
//            return false;
//        }
//    }

    protected void saveTrigger(String text) throws Exception {
        Main.logger.trace("saveTrigger [" + text + "] [" + m_lastMsgType + "] [" + m_handlerInProgress + "]");
//        if (!isIgnoreMsg(text)) {
        if (m_lastMsgType >= 0 && m_handlerInProgress) {
            Trigger trigger = new Trigger(getFileID(), this);
            trigger.SetMsgId(m_lastMsgType);
//                trigger.SetLine(m_CurrentLine);
//                trigger.SetOffset(m_CurrentFilePos);
            trigger.SetText(text);
            SetStdFieldsAndAdd(trigger);
        }
//        }
    }

    protected void saveHandler(boolean start, String text) throws Exception {
        if (start) {
//            if (text.startsWith("SIP:CTI:HA_")
//                    || text.startsWith("SIP:CTI:callSync")
//                    || text.startsWith("NET:CTI:sipStackSync")
//                    || text.startsWith("NET:CTI:callSync")
//                    || text.startsWith("NET:CTI:deviceSync")) {
//                haMessage = true;
//            }
            m_handlerInProgress = true;
            Handler handler = new Handler(getFileID());
            m_handlerId = handler.getRecordID();
            handler.SetLine(m_CurrentLine);
            handler.SetOffset(m_CurrentFilePos);
            handler.SetText(text);
            SetStdFieldsAndAdd(handler);

        } else {
            haMessage = false;
            m_handlerInProgress = false;
        }
    }

    protected void AddCireqMessage(ArrayList contents, String header) throws Exception {
        Matcher m;
        CIFaceRequest req = new CIFaceRequest(m_handlerInProgress, m_handlerId, fileInfo.getRecordID());

        String[] headerList = header.split(" ");
        req.SetName(headerList[1]);
        for (Object oLine : contents) {
            String line = (String) oLine;
            if ((m = regFrom.matcher(line)).find()) {
                req.SetRefId(m.group(1));
            } else if ((m = regNumbers.matcher(line)).find()) {
                req.SetThisDn(m.group(1));
                req.SetOtherDn(m.group(2));
            }
        }
        req.SetOffset(m_HeaderOffset);
        req.SetLine(m_headerLine);
        SetStdFieldsAndAdd(req);
    }

    protected void HandleConnId(String str) throws Exception {
        String connId;
        int start = str.indexOf("connection-id ");
        if (start < 0) {
            return;
        }
        start += 14;
        connId = str.substring(start, start + 16);
        if (str.contains("{constructed}")) {
            ConnIdRecord rec = new ConnIdRecord(connId, true, m_handlerInProgress, m_handlerId, fileInfo.getRecordID());
            SetStdFieldsAndAdd(rec);
        } else if (str.contains("{destructed}")) {
            ConnIdRecord rec = new ConnIdRecord(connId, false, m_handlerInProgress, m_handlerId, fileInfo.getRecordID());
            SetStdFieldsAndAdd(rec);
        }
    }

    protected void HandleConnIdChange(ArrayList<String> contents) throws Exception {
        String oldId = "";
        String newId = "";
        Matcher m;
        for (String oLine : contents) {
            if (!oldId.isEmpty() && !newId.isEmpty()) {
                break;
            }
            if ((m = regOldConnID.matcher(oLine)).find()) {
                oldId = m.group(1);
                continue;
            }
            if ((m = regNewConnID.matcher(oLine)).find()) {
                newId = m.group(1);
            }
        }

        if (oldId.isEmpty() || newId.isEmpty()) {
            return; //something's wrong
        }
        ConnIdRecord rec = new ConnIdRecord(oldId, newId, false, m_handlerInProgress, m_handlerId, fileInfo.getRecordID());
        rec.setTempConnid(true);
        SetStdFieldsAndAdd(rec);
//        rec = new ConnIdRecord(newId, true);
//        SetStdFieldsAndAdd(rec);
    }

    private boolean FullSIPMsg() {
        return true;
    }

    private void AddConfigMessage(ArrayList<String> m_MessageContents) {
        ConfigUpdateRecord msg = new ConfigUpdateRecord(m_MessageContents, fileInfo.getRecordID());
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

    private boolean newSeqNo(TLibMessage msg) {
        Long seqNo = msg.getSeqNo();
//        Main.logger.info("newSeqNo: " + seqNo);
        if (seqNo != null && seqNo > 0) {
            if (seqNo == lastSeqNo) {
                return false;
            }
            lastSeqNo = seqNo;
        } else {
            lastSeqNo = 0;
        }
        return true;
    }

    @Override
    void init(DBTables m_tables) {
        m_tables.put(TableType.SIP1536Other.toString(), new SIP1536OtherTable(Main.getInstance().getM_accessor(), TableType.SIP1536Other));
        m_tables.put(TableType.SIP1536Trunk.toString(), new SIP1536TrunkTable(Main.getInstance().getM_accessor(), TableType.SIP1536Trunk));
        m_tables.put(TableType.SIP1536ReqResp.toString(), new SIP1536RequestResponseTable(Main.getInstance().getM_accessor(), TableType.SIP1536ReqResp));
        m_tables.put(TableType.TLIBTimerRedirect.toString(), new TLibTimerRedirectTable(Main.getInstance().getM_accessor(), TableType.TLIBTimerRedirect));
    }

    private String ParseLine1536(BufferedReaderCrLf input, String str) throws Exception {
        Matcher m;
        String s;

        m_PacketLength = 0;
        m_ContentLength = 0;
        setSavedFilePos(getFilePos());

        try {
            s = ParseGenesysTServer(str, TableType.MsgTServer, regNotParseMessage, ptSkip);
        } catch (Exception exception) {
            throw exception; // do nothing so far. Can insert code to ignore certain exceptions
        }

        Main.logger.trace("ParseLine1536 str[" + str + "] s[" + s + "]");
        m_lineStarted = m_CurrentLine;

        switch (m_ParserState) {
            case STATE_HEADER: {
                if (s.contains("------------ SIP Server Statistics ------------")) {
                    m_ParserState = STATE_COMMENTS;
                    m_MessageContents.clear();
                }
            }
            break;

            case STATE_COMMENTS:
                if (s.isEmpty()) {
                    return null;
                }
                if (s.startsWith("-")) {
                    commit1536(input, null);
                    return null;
                }
                if (s.startsWith(sipTrunkStatistics)) {
                    addSIPTrunkStatistics(s.substring(sipTrunkStatistics.length()), input);

                } else if (s.startsWith(sipRegGenerated)) {
                    addSIPRegResp(s.substring(sipRegGenerated.length()), input, true);

                } else if (s.startsWith(sipResGenerated)) {
                    addSIPRegResp(s.substring(sipResGenerated.length()), input, false);
                } else {
                    String[] split = StringUtils.split(s, " ", 2);
                    if (split.length > 0) {
                        if (sip1536OtherMessage != null) {
                            if (sip1536OtherMessage.isChangedKey(split[0])) {
                                commit1536(input, split);
                                sip1536OtherMessage = new SIP1536OtherMessage(split[0]);
                            }
                        } else {
                            sip1536OtherMessage = new SIP1536OtherMessage(split[0]);
                        }
                        sip1536OtherMessage.addValues(split[1]);
                    } else {
                        Main.logger.error("Unsplit [s" + s + "]");
                    }

                }
                break;
        }

        return null;
    }

    private void addSIPTrunkStatistics(String str, BufferedReaderCrLf input) throws Exception {
        SIP1536TrunkStatistics msg = new SIP1536TrunkStatistics(str);
        msg.SetStdFieldsAndAdd(this);

    }

    private void addSIPRegResp(String str, BufferedReaderCrLf input, boolean isRequest) throws Exception {
        SIP1536RequestResponse msg = new SIP1536RequestResponse(str, isRequest);
        msg.SetStdFieldsAndAdd(this);
    }

    private void commit1536(BufferedReaderCrLf input, Object object) {
        if (sip1536OtherMessage != null) {
            sip1536OtherMessage.SetStdFieldsAndAdd(this);
            sip1536OtherMessage = null;
            m_MessageContents.clear();
        }
    }

    private boolean ifSIPServer(String string) {
        return string.equalsIgnoreCase("gtsCore")
                || string.equalsIgnoreCase("sipServer");
    }

    enum ParserState {
        STATE_COMMENTS,
        STATE_HEADER,
        STATE_SIP_HEADER,
        STATE_SIP_HEADER1,
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
        STATE_CIREQ_MESSAGE,
        STATE_CONNID_RECORD,
        STATE_TLIB_BACKUP_MESSAGE,
        STATE_CONFIG,
        STATE_1536_SIPServer,
        STATE_1536_Other, STATE_TIMER_REDIRECT
    }

    private class SIP1536OtherMessage extends Message {

        private final String key;

        private SIP1536OtherMessage(String string) throws Exception {
            super(TableType.SIP1536Other, fileInfo.getRecordID());
            if (string == null || string.isEmpty()) {
                throw new Exception("Key cannot be empty");
            }
            this.key = string.toLowerCase();
        }

        public String getKey() {
            return key;
        }

        private boolean isChangedKey(String string) {
            if (string != null && !string.isEmpty()) {
                String s = string.toLowerCase();
                return !key.equals(s) && ((!key.equals("gtscore") && !key.equals("sipserver"))
                        || (!s.equals("gtscore") && !s.equals("sipserver")));
            }
            return true;
        }

        private void addValues(String key) {
            m_MessageLines.add(key);
        }

        public Map<String, String> getAttrs() {
            HashMap<String, String> ret = new HashMap<>();
            for (String m_MessageLine : m_MessageLines) {
                String[] split = StringUtils.split(m_MessageLine, "=", 2);
                if (split != null && split.length >= 2) {
                    ret.put(split[0], split[1]);
                }
            }
            return ret;
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.Misc, getKey()));

            int baseRecNo = 7;
            Map<String, String> attrs = getAttrs();
            int i = 0;
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                setFieldInt(stmt, baseRecNo + i * 2, Main.getRef(ReferenceType.Misc, entry.getKey()));
                setFieldString(stmt, baseRecNo + i * 2 + 1, entry.getValue());
                if (++i >= MAX_1536_ATTRIBUTES) {
                    Main.logger.debug(getM_line() + ": more attributes then max (" + MAX_1536_ATTRIBUTES + ") - "
                            + attrs.size() + ":" + attrs);
                    break;
                }
            }
            return true;

        }
    }

    public class SIP1536OtherTable extends DBTable {

        public SIP1536OtherTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            StringBuilder buf = new StringBuilder();
            addIndex("time");
            addIndex("FileId");
            addIndex("keyid");
            for (int i = 1; i <= MAX_1536_ATTRIBUTES; i++) {
                addIndex("attr" + i + "id");
                addIndex("value" + i);
            }

            dropIndexes();

            for (int i = 1; i <= MAX_1536_ATTRIBUTES; i++) {
                buf.append(",attr").append(i).append("id int");
                buf.append(",value").append(i);
            }

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    + ",keyid int"
                    /* standard first */
                    + buf
                    + ");";
            getM_dbAccessor().runQuery(query);


        }

        @Override
        public String getInsert() {
            StringBuilder buf = new StringBuilder();
            for (int i = 1; i <= MAX_1536_ATTRIBUTES; i++) {
                buf.append(",?,?");
            }
            return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?"
                    /*standard first*/
                    + buf
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

    private class SIP1536TrunkStatistics extends Message {

        HashMap<String, String> attrs = new HashMap<>();
        private String trunk = null;

        private SIP1536TrunkStatistics(String string) throws Exception {
            super(TableType.SIP1536Trunk, fileInfo.getRecordID());
            if (string == null || string.isEmpty()) {
                throw new Exception("Key cannot be empty");
            }
            Matcher m = regKeyValueLine.matcher(string);
            while (m.find()) {
                if (trunk == null && m.group(1).equalsIgnoreCase("TRUNK")) {
                    trunk = getGroup2Or3(m);
                } else {
                    attrs.put(m.group(1), getGroup2Or3(m));
                }

            }
        }

        public Map<String, String> getAttrs() {
            return attrs;
        }

        private String getTrunk() {
            return trunk;
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.DN, cleanDN(getTrunk())));

            int baseRecNo = 7;
            Map<String, String> attrs = getAttrs();
            int i = 0;
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                setFieldInt(stmt, baseRecNo + i * 2, Main.getRef(ReferenceType.Misc, entry.getKey()));
                setFieldString(stmt, baseRecNo + i * 2 + 1, entry.getValue());
                if (++i >= MAX_1536_ATTRIBUTES) {
                    Main.logger.error(getM_line() + ": more attributes then max (" + MAX_1536_ATTRIBUTES + ") - "
                            + attrs.size() + ":" + attrs);
                    break;
                }
            }
            return true;

        }
    }

    public class SIP1536TrunkTable extends DBTable {

        public SIP1536TrunkTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            StringBuilder buf = new StringBuilder();
            addIndex("time");
            addIndex("FileId");
            addIndex("trunkid");
            for (int i = 1; i <= MAX_1536_ATTRIBUTES; i++) {
                addIndex("attr" + i + "id");
                addIndex("value" + i);
            }

            dropIndexes();

            for (int i = 1; i <= MAX_1536_ATTRIBUTES; i++) {
                buf.append(",attr").append(i).append("id int");
                buf.append(",value").append(i);
            }

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    + ",trunkid int"
                    /* standard first */
                    + buf
                    + ");";
            getM_dbAccessor().runQuery(query);


        }

        @Override
        public String getInsert() {
            StringBuilder buf = new StringBuilder();
            for (int i = 1; i <= MAX_1536_ATTRIBUTES; i++) {
                buf.append(",?,?");
            }
            return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?"
                    /*standard first*/
                    + buf
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

    private class SIP1536RequestResponse extends Message {

        private final boolean request;
        HashMap<String, String> attrs = new HashMap<>();
        private String msg;
        private String amount;

        private SIP1536RequestResponse(String string, boolean request) throws Exception {
            super(TableType.SIP1536ReqResp, fileInfo.getRecordID());
            if (string == null || string.isEmpty()) {
                throw new Exception("Key cannot be empty");
            }
            this.request = request;
            Matcher m = regKeyValueLine.matcher(string);
            if (m.find()) {
                this.msg = m.group(1);
                this.amount = getGroup2Or3(m);
                while (m.find()) {
                    String key = m.group(1);
                    if (key != null) {
                        String[] split = StringUtils.split(key, '_');
                        attrs.put((split != null && split.length > 0) ? split[0] : key,
                                getGroup2Or3(m));
                    }
                }
            }

        }

        public boolean isRequest() {
            return request;
        }

        public String getMsg() {
            return msg;
        }

        public Integer getAmount() {
            return intOrDef(amount, (Integer) null);
        }

        public HashMap<String, String> getAttrs() {
            return attrs;
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.SIPMETHOD, getMsg()));
            setFieldInt(stmt, 7, getAmount());
            stmt.setBoolean(8, isRequest());

            int baseRecNo = 9;
            Map<String, String> attrs = getAttrs();
            int i = 0;
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                setFieldInt(stmt, baseRecNo + i * 2, Main.getRef(ReferenceType.Misc, entry.getKey()));
                setFieldString(stmt, baseRecNo + i * 2 + 1, entry.getValue());
                if (++i >= MAX_1536_ATTRIBUTES) {
                    Main.logger.error(getM_line() + ": more attributes then max (" + MAX_1536_ATTRIBUTES + ") - "
                            + attrs.size() + ":" + attrs);
                    break;
                }
            }
            return true;
        }
    }

    public class SIP1536RequestResponseTable extends DBTable {

        public SIP1536RequestResponseTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            StringBuilder buf = new StringBuilder();
            addIndex("time");
            addIndex("FileId");
            addIndex("msgid");
            for (int i = 1; i <= MAX_1536_ATTRIBUTES; i++) {
                addIndex("attr" + i + "id");
                addIndex("value" + i);
            }

            dropIndexes();

            for (int i = 1; i <= MAX_1536_ATTRIBUTES; i++) {
                buf.append(",attr").append(i).append("id int");
                buf.append(",value").append(i);
            }

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    + ",msgid int"
                    + ",amount int"
                    + ",isrequest bit"
                    /* standard first */
                    + buf
                    + ");";
            getM_dbAccessor().runQuery(query);


        }

        @Override
        public String getInsert() {
            StringBuilder buf = new StringBuilder();
            for (int i = 1; i <= MAX_1536_ATTRIBUTES; i++) {
                buf.append(",?,?");
            }
            return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?,?,?"
                    /*standard first*/
                    + buf
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

    private class TLibTimerRedirectMessage extends Message {

        private final String msg;
        private String cause = null;
        private String connID = null;

        private TLibTimerRedirectMessage(String msg) throws Exception {
            super(TableType.TLIBTimerRedirect, fileInfo.getRecordID());
            this.msg = msg;
        }

        public String getConnID() {
            return connID;
        }

        private void setConnID(String group) {
            this.connID = group;
        }

        public String getCause() {
            return cause;
        }

        private void setCause(String cause) {
            this.cause = cause;
        }

        public String getMsg() {
            return msg;
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, getMsg()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, getConnID()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.TEventRedirectCause, getCause()));
            return true;

        }
    }

    public class TLibTimerRedirectTable extends DBTable {

        public TLibTimerRedirectTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");
            addIndex("msgid");
            addIndex("connIDID");
            addIndex("causeID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",msgid int"
                    + ",connIDID int"
                    + ",causeID int"
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

}
