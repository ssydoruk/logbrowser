/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import org.apache.commons.lang3.StringUtils;
import static com.myutils.logbrowser.indexer.SIPProxyParser.ParserState.*;

/**
 *
 * @author kvoroshi
 */
public class SIPProxyParser extends Parser {

    final int MSG_STRING_LIMIT = 200;
    // parse state contants

    static final String[] BlockNamesToIgnoreArray = {
        "NET:TCO:EventResourceInfo:",
        "NET:IPR:EventResourceInfo:",
        "SIP:CTI:GVP_RESOURCES_CHANGED:",
        "SIP:CTI:HA_SEND_SYNC_MESSAGE:",
        "SIP:CTI:UNKNOWN_HA_MESSAGE:",
        "SIP:CTI:sipStackSync:",
        "SIP:CTI:tControllerSync:",
        "SIP:CTI:callSync:"
    };
    HashSet m_BlockNamesToIgnoreHash;

    StringBuilder sipBuf = new StringBuilder();

    long m_CurrentFilePos;
    long m_HeaderOffset;

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
    private static final Pattern regTriggerStart = Pattern.compile("^\\$((\\*:)|\\+|-)");
    private static final Pattern regConfigUpdate = Pattern.compile("^[\\d\\s]+\\[TCONF\\]");

    private static final Pattern regConfigOneLineDN = Pattern.compile("^\\s\\[(\\d+)\\] dn = '([^']+)' type = (\\w+)");

    private ParserState m_ParserState;
    int m_PacketLength;
    int m_ContentLength;
    String m_Header;
    int m_headerLine;
    boolean m_isInbound;
    boolean m_isContentLengthUnknown;

    //boolean m_lastMsgSip = false;
    int m_lastMsgType = -1;
    // 0-tlib, 1-sip, 2-json

    String m_lastTime;
    private String msgName;
    private boolean inbound;
    private DateParsed dpHeader;
    private static final Pattern ptSkip = Pattern.compile("^[:\\s]*");

    private final static Pattern regProxy = Pattern.compile("^Proxy\\((\\w+):");
    static private boolean ifSIPLines = false;
    private final static Pattern SIPHeaderContinue = Pattern.compile("^\\s*(\\w.+)");
    private ArrayList<String> extraBuff;
    private static boolean ifSIPLinesForce = false;
    private static boolean gaveWarning = false;
    private boolean haMessage;
    private String handleAdd;

    public SIPProxyParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_SessionController, m_tables);
        this.extraBuff = new ArrayList<>();
        //m_accessor = accessor;
        m_BlockNamesToIgnoreHash = new HashSet();
        for (int i = 0; i < BlockNamesToIgnoreArray.length; i++) {
            m_BlockNamesToIgnoreHash.add(BlockNamesToIgnoreArray[i]);
        }
        this.ifSIPLinesForce = Main.ifSIPLines();

    }

    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentFilePos = offset;
        m_CurrentLine = line;
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
            e.printStackTrace();
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

    void ContinueParsing(String initial) {
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
                ParseLine(input, str);
                if (m_ParserState == STATE_COMMENTS) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final Pattern regSIPHeader = Pattern.compile("(\\d+) bytes .+ (>>>>>|<<<<<)$");
//    15:22:50.980: Sending  [0,UDP] 3384 bytes to 10.82.10.146:5060 >>>>>

    private static final Pattern regNotParseMessage = Pattern.compile("^(0454[1-5]"
            + ")");

    String ParseLine(BufferedReaderCrLf input, String str) throws Exception {
        Matcher m;

        String s = str;

        Main.logger.trace("Parser state: " + m_ParserState);
        ParseCustom(str, (SipMessage.m_handlerInProgress ? SipMessage.m_handlerId : 0));
        switch (m_ParserState) {
            case STATE_HEADER: {
                if (s.isEmpty()) {
                    m_ParserState = STATE_COMMENTS;
                }
            }
            break;

            case STATE_COMMENTS:
                m_PacketLength = 0;
                m_ContentLength = 0;
                setSavedFilePos(getFilePos());

                try {
                    s = ParseGenesys(str, TableType.MsgTServer, regNotParseMessage, ptSkip);
                } catch (Exception exception) {
                    throw exception; // do nothing so far. Can insert code to ignore certain exceptions
                }

                m_LineStarted = m_CurrentLine;

                if ((m = regConfigOneLineDN.matcher(str)).find()) {
                    Main.logger.trace("-1-");
                    ConfigUpdateRecord msg = new ConfigUpdateRecord(str);
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
                    Main.logger.trace("-2-");
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s.substring(m.end()));
                    m_ParserState = STATE_CONFIG;
                } else if ((m = regProxy.matcher(s)).find()) {
                    Main.logger.trace("-4-");
                    ProxiedMessage msg = new ProxiedMessage(m.group(1), s.substring(m.end()));
                    SetStdFieldsAndAdd(msg);

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
                        int addBytes = CountStrings(sipBuf, "\r\r");
                        int nR = CharOccurences(sipBuf, '\r');
                        int nN = CharOccurences(sipBuf, '\n');
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
                } else if (s.endsWith(" <<<") || s.endsWith(" >>>")) {
                    Main.logger.trace("-11-");
                    m_Header = s;
                    dpHeader = dp;

                    m_HeaderOffset = m_CurrentFilePos;
                    m_headerLine = m_CurrentLine;
                    m_MessageContents.clear();
                    m_isInbound = true;
                    if (s.endsWith(">")) {
                        m_isInbound = false;
                    }
                    m_ParserState = STATE_SIP_HEADER;
                    m_isContentLengthUnknown = false;
                    setSavedFilePos(getFilePos());
                    break;

                } else if (str.startsWith("@UA")) {
                    // note message; ignore...
                    break;
                } else if (str.startsWith("...exception")) {
                    // exception message; ignore...
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
                    Main.logger.trace("-12-");
                    m_lastTime = s.substring(0, 12);
                    // !!!TODO: this is a hack based on knowledge that line before http message
                    // has a timestamp. VERY DANGEROUS
                }
                break;

//<editor-fold defaultstate="collapsed" desc="state_SIP_HEADER">
            case STATE_SIP_HEADER: {
                if ((m = SIPHeaderContinue.matcher(str)).find()) {
                    s = m.group(1);
                    m_MessageContents.add(s);
                    String contentL = Message.GetSIPHeader(s, "content-length", "l");
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
//                        if (m_PacketLength > 50) {
//                            m_ParserState = STATE_SIP_HEADER_GSIPLIB;
//                            m_MessageContents.clear();
//                            break;
//                        }
//                        ParseLine(input, str);
                    }
                }
            }
            break;
//</editor-fold>
//</editor-fold>

            case STATE_SIP_BODY: {
                m_PacketLength -= s.length() + 2;

                if ((m = SIPHeaderContinue.matcher(str)).find()) {
                    m_MessageContents.add(s);

                } else {
                    m_ParserState = STATE_COMMENTS;
                    AddSipMessage(m_MessageContents, m_Header);
                    m_MessageContents.clear();
                    return str;
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

            case STATE_CIREQ_MESSAGE:
                if (s.startsWith("-----")) {
                    m_ParserState = STATE_COMMENTS;
                    AddCireqMessage(m_MessageContents, m_Header);
                } else {
                    m_MessageContents.add(s);
                }
                break;

        }

        return null;
    }

    protected void AddSipMessage(ArrayList contents, String header) throws Exception {
        String str;
        Matcher m;

        // Populate our class representation of the message
        SipMessage msg = null;

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
//            Main.logger.error("Broken message - firstLine doesn't contain empty space; line " + m_LineStarted);
//            PrintMsg(contents);
//            return;
//        }
        msg = new SipMessage(contents, TableType.SIP);

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

        String msgName = msg.GetName();
        if (msgName != null && msgName.equals("NOTIFY")) {
            String contentType = msg.GetSIPHeader("Content-Type", "c");
            String contentEvent = msg.GetSIPHeader("Event", null);

            if (contentType != null && contentType.equals("application/x-genesys-mediaserver-status")
                    && contentEvent != null && contentEvent.equals("x-genesys-mediaserver-status")) {
                Main.logger.debug("Cancel trigger on NOTIFY (Event=x-genesys-mediaserver-status)");
                Record.SetHandlerInProgress(false);
            }
        }

        try {
            SetStdFieldsAndAdd(msg);
        } catch (Exception e) {
            Main.logger.error("Error adding message line:" + m_LineStarted);
            PrintMsg(contents);
        }
    }

    private final static HashSet<String> eventsWithCallInfo = new HashSet<String>(
            Arrays.asList(new String[]{
        "EventRegistered",
        "EventAddressInfo"}));

    protected void AddCireqMessage(ArrayList contents, String header) throws Exception {
        Matcher m;
        CIFaceRequest req = new CIFaceRequest();

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
        int headerLine = m_CurrentLine - contents.size();
        if (headerLine < 1) {
            headerLine = 1;
        }
        req.SetLine(m_headerLine);
        SetStdFieldsAndAdd(req);
    }

    private static final Pattern regOldConnID = Pattern.compile("was (\\w+)\\)$");
    private static final Pattern regNewConnID = Pattern.compile("\\@ c:(\\w+),");

    private boolean FullSIPMsg() {
        return true;
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

    private long lastSeqNo = 0;

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
    void init(HashMap<TableType, DBTable> m_tables) {
    }

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
        STATE_POSSIBLE_MULTIPLE_SIP_MESSAGES,
        /* maltiple sip INFO messages in one packet */
        STATE_CIREQ_MESSAGE,
        STATE_CONFIG,
    }
}
