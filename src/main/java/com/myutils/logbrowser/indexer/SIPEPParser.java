/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.myutils.logbrowser.indexer.SIPEPParser.ParserState.*;

/**
 * @author ssydoruk
 */
public class SIPEPParser extends Parser {

    static final String[] BlockNamesToIgnoreArray = {"SIP:CTI:callSync::0",
            "SIP:CTI:sipStackSync:0"};

    //15:37:21.918 +++ CIFace::Request +++
//   -- new invoke
//  Parsed: RequestQueryAddress
//  From: ICON_Voice[704]/16165
//  Numbers: +<SIP to RM> -<none>
//  Status: parsed:1 queued:0 sent:0 acked:0 preevent:0 event:0 context:0 transferred:0
//  -----
    private static final Pattern regSIPReq = Pattern.compile(" bytes (to|from) (\\S+)");

    private static final Pattern regConfigUpdate = Pattern.compile("^[\\d\\s]+\\[TCONF\\]");

    private static final Pattern regConfigOneLineDN = Pattern.compile("^\\s\\[(\\d+)\\] dn = '([^']+)' type = (\\w+)");
    private static final Pattern regHeaderEnd = Pattern.compile("^File:\\s+");
    private static final Pattern ptSkip = Pattern.compile("^[:\\s]*");
    private static final Pattern regProxy = Pattern.compile("^Proxy\\((\\w+):");
    private static final Pattern SIPHeaderContinue = Pattern.compile("^\\S");
    private static final Pattern regSIPHeader = Pattern.compile("(\\d+) bytes .+ (>>>>>|<<<<<)$");
    //    15:22:50.980: Sending  [0,UDP] 3384 bytes to 10.82.10.146:5060 >>>>>
    private static final Pattern regNotParseMessage = Pattern.compile("^(0454[1-5]"
            + ")");
    private static final Pattern regCfgObjectName = Pattern.compile("(?:name|userName|number|loginCode)='([^']+)'");
    private static final Pattern regCfgObjectDBID = Pattern.compile("DBID=(\\d+)");
    private static final Pattern regCfgObjectType = Pattern.compile("object Cfg(\\w+)=");
    private static final Pattern regCfgOp = Pattern.compile("\\(type Object(\\w+)\\)");
    private static final Pattern regSIPServerStartDN = Pattern.compile("^\\s*DN added \\(dbid (\\d+)\\) \\(number ([^\\(]+)\\) ");
    static private boolean ifSIPLines = false;
    private static boolean ifSIPLinesForce = false;
    private static boolean gaveWarning = false;
    final int MSG_STRING_LIMIT = 200;
    private final ArrayList<String> extraBuff;
    // parse state contants
    HashMap m_BlockNamesToIgnoreHash;
    StringBuilder sipBuf = new StringBuilder();
    Pattern m_CustomRegexp;
    long m_CurrentFilePos;
    long m_HeaderOffset;
    int m_PacketLength;
    int m_ContentLength;
    String m_Header;
    int m_headerLine;
    boolean m_isInbound;
    boolean m_isContentLengthUnknown;

    boolean m_handlerInProgress;

    //boolean m_lastMsgSip = false;
    int m_lastMsgType = -1;
    // 0-tlib, 1-sip, 2-json

    String m_lastTime;
    private ParserState m_ParserState;
    private String msgName;
    private boolean inbound;
    private DateParsed dpHeader;

    public SIPEPParser(DBTables m_tables) {
        super(FileInfoType.type_SIPEP, m_tables);
        this.extraBuff = new ArrayList<>();
        //m_accessor = accessor;
        m_BlockNamesToIgnoreHash = new HashMap();
        for (String BlockNamesToIgnoreArray1 : BlockNamesToIgnoreArray) {
            m_BlockNamesToIgnoreHash.put(BlockNamesToIgnoreArray1, 0);
        }
        SIPEPParser.ifSIPLinesForce = Main.ifSIPLines();

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
//                } else if (trimmed.contains(" Proxy(") || str.startsWith("Proxy(")) {
                } else if ((m = regProxy.matcher(s)).find()) {

                    ProxiedMessage msg = new ProxiedMessage(m.group(1), s.substring(m.end()), getFileID());
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
                        sipBuf.append(input.readBytes(m_PacketLength - 2)); // SIP EP logger truncates trailing new line
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

                        m_MessageContents.clear();
                        String theBuf = sipBuf.toString();

                        m_MessageContents.addAll(Arrays.asList(StringUtils.splitByWholeSeparatorPreserveAllTokens(theBuf, ((nN > 0) ? "\n" : "\r"))));
                        AddSipMessage(m_MessageContents, m_Header);
                        m_MessageContents.clear();
                        sipBuf.setLength(0);
                        setFilePos(getEndFilePos() + bytesRead);
                    }

                    break;

//</editor-fold>
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
                    break;
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
                ParseLine(input, str);
                break;

        }

        return null;
    }

    protected void AddSipMessage(ArrayList contents, String header) throws Exception {
        Matcher m;

        // Populate our class representation of the message
        SipMessage msg;

        msg = new SipMessage(contents, TableType.SIPEP, fileInfo.getRecordID());

        if ((m = regSIPReq.matcher(header)).find()) {
            if (m.group(1).startsWith("f")) {
                msg.SetInbound(true);
                msg.SetSenderName(m.group(2));
            } else {
                msg.SetInbound(false);
                msg.SetReceiverName(m.group(2));
            }
        }

        m_lastMsgType = 1;

        try {
            SetStdFieldsAndAdd(msg);
        } catch (Exception e) {
            Main.logger.error("Error adding message line:" + m_lineStarted, e);
            PrintMsg(contents);
        }
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

    @Override
    void init(DBTables m_tables) {
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
