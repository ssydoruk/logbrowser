/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static com.myutils.logbrowser.indexer.RMParser.ParserState.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ssydoruk
 */
public class RMParser extends Parser {


    static final String[] BlockNamesToIgnoreArray = {"SIP:CTI:callSync::0",
        "SIP:CTI:sipStackSync:0"};
    private static final Pattern ptSkip = Pattern.compile("^[:\\s]*");
    private final static Pattern regProxy = Pattern.compile("^Proxy\\((\\w+):");
    private final static Pattern regConnidChangeEnd = Pattern.compile("^\\S");
    static private boolean ifSIPLines = false;
    private final static Pattern SIPHeaderContinue = Pattern.compile("^\\S");
    private final static Pattern SIPContentLength = Pattern.compile("^\\s");
    private static boolean ifSIPLinesForce = false;
    private static boolean gaveWarning = false;
    private static final Pattern regSIPHeader = Pattern.compile(" SIP Message ");
//    15:22:50.980: Sending  [0,UDP] 3384 bytes to 10.82.10.146:5060 >>>>>
    private static final Pattern regNotParseMessage = Pattern.compile("^(0454[1-5]"
            + ")");
    private static final Pattern regISCCStart = Pattern.compile("(\\d+) bytes .+ (>>>>>|<<<<<)$");
    private static final Pattern regSIPHeaderMsg = Pattern.compile("(sent to|received from) \\[([^:]+):([^\\]]+)\\][^:]+:\\s*(.+)$");
    static private final Pattern regISCCHead = Pattern.compile("^[\\d\\s]+\\[ISCC\\] (Received|Send).+: message (\\w+)$");
    private static final Pattern regCfgObjectName = Pattern.compile("(?:name|userName|number|loginCode)='([^']+)'");
    private static final Pattern regCfgObjectDBID = Pattern.compile("DBID=(\\d+)");
    private static final Pattern regCfgObjectType = Pattern.compile("object Cfg(\\w+)=");
    private static final Pattern regCfgOp = Pattern.compile("\\(type Object(\\w+)\\)");
    private static final Pattern regSIPServerStartDN = Pattern.compile("^\\s*DN added \\(dbid (\\d+)\\) \\(number ([^\\(]+)\\) ");
    final int MSG_STRING_LIMIT = 200;
    // parse state contants
    HashMap m_BlockNamesToIgnoreHash;

    long m_CurrentFilePos;
    long m_HeaderOffset;

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
    private ArrayList<String> extraBuff;

    public RMParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_RM, m_tables);
        this.extraBuff = new ArrayList<>();
        //m_accessor = accessor;
        m_BlockNamesToIgnoreHash = new HashMap();
        for (int i = 0; i < BlockNamesToIgnoreArray.length; i++) {
            m_BlockNamesToIgnoreHash.put(BlockNamesToIgnoreArray[i], 0);
        }
        RMParser.ifSIPLinesForce = Main.ifSIPLines();

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
            Main.logger.error(e);;
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
            Main.logger.error(e);;
        }
    }


    String ParseLine(BufferedReaderCrLf input, String str) throws Exception {
        String s = str;
        Matcher m;

        String trimmed = str.trim();

        Main.logger.trace("Parser state: " + m_ParserState);
        ParseCustom(str, 0);
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
                    s = ParseGenesys(str, TableType.MsgRM, regNotParseMessage, ptSkip);
                } catch (Exception exception) {
                    throw exception; // do nothing so far. Can insert code to ignore certain exceptions
                }

                m_LineStarted = m_CurrentLine;

                if ((m = regProxy.matcher(s)).find()) {

                    ProxiedMessage msg = new ProxiedMessage(m.group(1), s.substring(m.end()));
                    SetStdFieldsAndAdd(msg);

                    return null;
                } //<editor-fold defaultstate="collapsed" desc="reading sip">
                else if ((m = regSIPHeader.matcher(s)).find()) {
                    m_Header = s;

                    dpHeader = dp; // so time of the message correctly set in AddSipMessage

                    m_HeaderOffset = m_CurrentFilePos;
                    m_headerLine = m_CurrentLine;
                    m_MessageContents.clear();
                    setSavedFilePos(getFilePos());
//                        m_MessageContents.add(s);
                    m_ParserState = STATE_SIP_HEADER;
                    return null;

//</editor-fold>
                } else if ((trimmed.contains(" Request received: ") || trimmed.contains(" Response sent: "))
                        || (trimmed.contains(" Response received: ") || trimmed.contains(" Request sent: "))
                        || (trimmed.contains(" SIP Message received ") || trimmed.contains(" SIP Message sent "))) { // GVP logs support
                    m_Header = trimmed;
                    dpHeader = dp;

                    m_HeaderOffset = m_CurrentFilePos;
                    m_headerLine = m_CurrentLine;
                    m_MessageContents.clear();
                    m_isInbound = true;
                    if (trimmed.contains("sent")) {
                        m_isInbound = false;
                    }
                    m_ParserState = STATE_SIP_HEADER_GSIPLIB;
                    setSavedFilePos(getFilePos());
                    m_isContentLengthUnknown = false;
                    break;

                } else if (str.startsWith("...exception")) {
                    // exception message; ignore...
                    break;

                }
                break;

//<editor-fold defaultstate="collapsed" desc="state_SIP_HEADER">
            case STATE_SIP_HEADER: {
                if ((m = SIPHeaderContinue.matcher(str)).find()) {
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
                        m_MessageContents.add(trimmed);

                    } else {
                        m_ParserState = STATE_COMMENTS;
                        m_MessageContents.add("");
                        AddSipMessage(m_MessageContents, m_Header);
                        m_MessageContents.clear();
                    }
                }
            }
            break;
//</editor-fold>

            case STATE_SIP_BODY: {
                m_PacketLength -= trimmed.length() + 2;
                boolean endFound = false;

                if (str.trim().isEmpty()) {
                    endFound = true;
                } else {
                    s = ParseGenesys(str, TableType.MsgRM, regNotParseMessage, ptSkip);
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
                    m_MessageContents.add(trimmed);
                }
            }
            break;

            case STATE_CONFIG:
                AddConfigMessage(m_MessageContents);
                m_MessageContents.clear();
                m_ParserState = STATE_COMMENTS;
                return str;

            case STATE_SIP_HEADER_GSIPLIB:
                m_PacketLength -= trimmed.length() + 2;
                if (trimmed.length() != 0) {
                    m_MessageContents.add(trimmed);

                    String contentL = Message.GetSIPHeader(trimmed, "content-length", "l");
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
                    if (trimmed.length() != 0) {
                        m_MessageContents.add(trimmed);

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
                    if (trimmed.length() > MSG_STRING_LIMIT && !trimmed.startsWith("<?xml version=") && !trimmed.startsWith("<Envelope xmlns=")) {
                        m_MessageContents.add(trimmed.substring(0, MSG_STRING_LIMIT).concat("..."));
                    } else {
                        m_MessageContents.add(trimmed);
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
                if (trimmed.startsWith("INFO sip:")) {
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

            case STATE_TLIB_MESSAGE_SIP_SYNC:
                if (trimmed.length() != 0 && !trimmed.equals("'")) {
                    m_MessageContents.add(trimmed);

                } else {
                    m_MessageContents.add(trimmed);
                    m_ParserState = STATE_TLIB_MESSAGE;
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

        boolean isInbound = true;
        String peerIP = "";
        String peerPort = "";
        String SIPmsg = "";
        if ((m = regSIPHeaderMsg.matcher(header)).find()) {
            String s = m.group(1);
            if (s != null) {
                isInbound = s.substring(0, 1).toLowerCase().startsWith("r");
            }
            peerIP = m.group(2);
            peerPort = m.group(3);
            SIPmsg = m.group(4);
            if (SIPmsg != null) {
                contents.add(0, SIPmsg);
            }
        }
        msg = new SipMessage(contents, TableType.SIPMS);

        if (msg.GetFrom() == null) {
            Main.logger.error("LOG ERROR: no From in SIP message, ignore, line " + m_CurrentLine);
            return;
        }
        msg.setPeerIp(peerIP);
        msg.setPeerPort(peerPort);
        msg.SetInbound(isInbound);

//        if ((m = regSIPHeaderIP.matcher(header)).find()) {
//            msg.SetInbound(m.group(1).startsWith("r"));
//            msg.SetName(m.group(2));
//        }
        m_lastMsgType = 1;

        try {
            SetStdFieldsAndAdd(msg);
        } catch (Exception e) {
            Main.logger.error("Error adding message line:" + m_LineStarted);
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
        STATE_SIP_HEADER1
    }
}
