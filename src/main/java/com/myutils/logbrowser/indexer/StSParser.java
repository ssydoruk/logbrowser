package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author akolo
 */
public class StSParser extends Parser {

    private static final Pattern regServerContinue = Pattern.compile("^\\s");
    private static final Pattern regJavaStart = Pattern.compile("^Java:");

    private static final Pattern regJavaOnData = Pattern.compile("_extension_core::onData");
    private static final Pattern regJava1 = Pattern.compile("Java:.+>\\s*$");
    private static final Pattern regJavaContinue = Pattern.compile("^(\\s|_extension_core|KVList:)");
    private static final Pattern regStatusSingleLine = Pattern.compile("^Status: (Place|Agent) '([^']+)'.+: (\\w+)[ ->]+(\\w+)$");
    private static final Pattern regStatusMultiLine = Pattern.compile("^Status: Capacity");

    private static final Pattern regInit = Pattern.compile("^Init:\\s+");

    private static final Pattern regCapacityContinue = Pattern.compile("^\\s");

    private static final Pattern regLineSkip = Pattern.compile("^[>\\s]*");
    private static final Pattern regTMessageStart = Pattern.compile("^Int 04543");
    private static final Pattern regNotParseMessage = Pattern.compile("^(04543"
            + ")");
    private static final Pattern ptServerName = Pattern.compile("^Application\\s+name:\\s+(\\S+)");
    long m_CurrentFilePos;
    final int MSG_STRING_LIMIT = 200;
    // parse state contants

    long m_HeaderOffset;
    private ParserState m_ParserState;
    String m_Header;

    String m_lastClientName;
    String m_lastClientSocket;
    String m_lastClientUID;
    boolean m_isNewAPIRequest;
    String m_ServerName;

    int m_rHistId = 0;
    int m_actionId = 0;
    int m_dbRecords = 0;
    private final HashMap m_BlockNamesToIgnoreHash;
    private final FileInfoType m_fileFilterId;
    private String m_LastLine;
    private boolean customEvent = false;

    public StSParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_StatServer, m_tables);
        m_BlockNamesToIgnoreHash = new HashMap();

        m_fileFilterId = FileInfoType.type_StatServer;
//        for (int i = 0; i < BlockNamesToIgnoreArray.length; i++) {
//            m_BlockNamesToIgnoreHash.put(BlockNamesToIgnoreArray[i], 0);
//        }
    }

    private void AddJavaMessage() {
        Main.logger.debug("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void AddSingleLineStatus(String plGroup, String name, String OldStatus, String NewStatus) {
        StStatus msg = new StStatus(plGroup, name, OldStatus, NewStatus);
        SetStdFieldsAndAdd(msg);

    }

    private void AddCapacityMessage(String m_Header, ArrayList m_MessageContents) {
        StCapacity msg = new StCapacity(m_Header, m_MessageContents);
        SetStdFieldsAndAdd(msg);
    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentFilePos = offset;
        m_CurrentLine = line;

        m_dbRecords = 0;

        m_ServerName = null;

        try {
            input.skip(offset);
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            m_ParserState = ParserState.STATE_HEADER;

            String str;
            while ((str = input.readLine()) != null) {

                try {
//                    int l=str.length()+input.charsSkippedOnReadLine;
//                    int l = str.getBytes("UTF-16").length/2+input.charsSkippedOnReadLine;
                    int l = input.getLastBytesConsumed();
                    setEndFilePos(getFilePos() + l); // to calculate file bytes
                    m_CurrentLine++;
                    m_LastLine = str; // store previous line for server name
                    Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");

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

        String trimmed = str.trim();

        Main.logger.trace("ParseLine " + m_CurrentLine + ": state -" + m_ParserState);
        ParseCustom(str, 0);

        switch (m_ParserState) {
//<editor-fold defaultstate="collapsed" desc="STATE_HEADER">
            case STATE_HEADER:
                if (m_ServerName == null) {
                    ParseServerName(trimmed);
                }
                if (s.startsWith("File:")) {
                    m_ParserState = ParserState.STATE_COMMENTS;
                }
                break;
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="STATE_COMMENTS">
            case STATE_COMMENTS:

                m_LineStarted = m_CurrentLine;

                s = ParseGenesys(str, TableType.MsgStatServer, regNotParseMessage, regLineSkip);

                if ((m = regInit.matcher(s)).find()) {
                    AddConfigMessage(s.substring(m.end()));
                } else if ((regStatusMultiLine.matcher(s)).find()) {
                    m_HeaderOffset = m_CurrentFilePos;
                    m_Header = s;
                    setSavedFilePos(getFilePos());
                    m_ParserState = ParserState.STATE_CAPACITY;
                } else if ((regTMessageStart.matcher(s)).find()) {//starts on clear line
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_TMESSAGESTART;
                    break;

                } else if ((m = regStatusSingleLine.matcher(s)).find()) {
                    m_HeaderOffset = m_CurrentFilePos;
                    AddSingleLineStatus(m.group(1), m.group(2), m.group(3), m.group(4));
                    //m_ParserState=STATE_CLUSTER;
                } else if (str.contains("Client:")) {
                    m_Header = trimmed;
                    m_HeaderOffset = m_CurrentFilePos;
                    m_MessageContents = new ArrayList();

                    if (m_lastClientName != null) {
                        m_MessageContents.add("Name " + m_lastClientName);
                        m_MessageContents.add("Socket " + m_lastClientSocket);
                        m_MessageContents.add("CLUID " + m_lastClientUID);

                        if (trimmed.contains("NewAPIOpenStat")) {
                            m_isNewAPIRequest = true;
                        }
                    }

                    /*check for single-line client requests*/
                    if (trimmed.contains("Message from")) {
//                        if (!trimmed.contains("Proxy:")) {
//
//                            String[] headerList = trimmed.split(" ");
//                            if (headerList[5].length() > 0) {
//                                int index = headerList[5].indexOf('(');
//                                m_lastClientName = headerList[5].substring(0, index);
//                                m_lastClientSocket = headerList[5].substring(index + 1, headerList[5].length() - 1);
//                                m_lastClientUID = m_ServerName + m_lastClientSocket;
//                                m_ParserState = ParserState.STATE_COMMENTS;
//                                break;
//                            }
//                        } else {
//                            int index = trimmed.indexOf("socket") + 7;
//                            if (index > 7) {
//                                m_lastClientName = "SelfHttpClient";
//                                m_lastClientSocket = trimmed.substring(index, trimmed.length() - 1);
//                                m_lastClientUID = m_ServerName + m_lastClientSocket;
//                                m_ParserState = ParserState.STATE_COMMENTS;
//                                break;
//                            }
//                        }
                    } else if (trimmed.contains("Sending message to")) {
//                        if (!trimmed.contains("Proxy:")) {
//                            String[] headerList = trimmed.split(" ");
//                            if (headerList[6].length() > 0) {
//                                int index = headerList[6].indexOf('(');
//                                m_lastClientName = headerList[6].substring(0, index);
//                                m_lastClientSocket = headerList[6].substring(index + 1, headerList[6].length() - 1);
//                                m_lastClientUID = m_ServerName + m_lastClientSocket;
//                                m_ParserState = ParserState.STATE_COMMENTS;
//                                break;
//                            }
//                        } else {
//                            int index = trimmed.indexOf("socket") + 7;
//                            if (index > 7) {
//                                m_lastClientName = "SelfHttpClient";
//                                m_lastClientSocket = trimmed.substring(index, trimmed.length() - 1);
//                                m_lastClientUID = m_ServerName + m_lastClientSocket;
//                                m_ParserState = ParserState.STATE_COMMENTS;
//                                break;
//                            }
//                        }
                    } else if (trimmed.contains("TimeProfile")) {
                        m_ParserState = ParserState.STATE_COMMENTS;
                        break;
                    } else if (trimmed.contains("'OpenStat'") && m_isNewAPIRequest) {
                        m_isNewAPIRequest = false;
                    }

                    m_ParserState = ParserState.STATE_CLIENT_MESSAGE;
                    break;
                } else if (s.startsWith("Server: IxnEvent 'event_custom'")) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_SERVERCUSTOM_MESSAGE;
                    this.customEvent = true;
                    break;

                } else if (s.startsWith("Server: IxnEvent")) {//starts on clear line
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_SERVER_MESSAGE;
                    this.customEvent = false;
                    break;
                } else if (s.startsWith("Server:")) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_SERVER_MESSAGE;

                } //                else if (str.contains("Server:") || str.contains("Init:")) {
                //                    m_Header = trimmed;
                //                    m_HeaderOffset = m_CurrentFilePos;
                //                    m_MessageContents = new ArrayList();
                //
                //                    /*check for single-line server requests*/
                //                    if (trimmed.contains("Linking") || trimmed.contains("Unlinking")) {
                //
                //                        m_MessageContents.add(trimmed);
                //                        AddServerMessage(m_MessageContents, m_Header);
                //                        m_ParserState = ParserState.STATE_COMMENTS;
                //                        break;
                //                    }
                //                    m_ParserState = ParserState.STATE_SERVER_MESSAGE;
                //                    break;
                //                }
                else if (s.startsWith("Action:")) {
                    setSavedFilePos(getFilePos());
                    m_Header = s;
                    m_ParserState = ParserState.STATE_ACTION_MESSAGE;
                    break;
                } else if ((regJavaStart.matcher(s)).find()) {
                    Main.logger.debug("Java");
                    setSavedFilePos(getFilePos());
                    m_Header = s;
                    m_MessageContents.add(s);
                    if ((regJavaOnData.matcher(s)).find()) {
                        m_ParserState = ParserState.STATE_JAVA_ON_DATA1;
                    } else {
                        AddJavaMessage();
                    }

                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="STATE_TMESSAGESTART">
            case STATE_TMESSAGESTART:
                s = ParseGenesys(str, TableType.MsgStatServer, regNotParseMessage, regLineSkip);
                if (s.startsWith("Server:")) {
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_SERVER_MESSAGE;
                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="STATE_CAPACITY">
            case STATE_CAPACITY:
                if (str.length() > 0
                        && ((regCapacityContinue.matcher(str)).find())) {
                    m_MessageContents.add(str);
                } else {
                    AddCapacityMessage(m_Header, m_MessageContents);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str; //
                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="STATE_JAVA_ON_DATA1">
            case STATE_JAVA_ON_DATA1:
                if ((regJava1.matcher(str)).find()) {
                    m_ParserState = ParserState.STATE_JAVA_ON_DATA2;
                } else {
                    Main.logger.debug("strange in state " + m_ParserState + "]: " + str);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }
                break;
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="STATE_JAVA_ON_DATA2">
            case STATE_JAVA_ON_DATA2:
                if (s.length() == 0 || (regJavaContinue.matcher(s)).find()) {
                    m_MessageContents.add(s);
                } else {
                    AddJavaMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str; //
                }
                break;
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="STATE_CLIENT_MESSAGE">
            case STATE_CLIENT_MESSAGE:
                if (trimmed.length() != 0 && (str.startsWith("\t"))) {
                    // Make sure Attribute and value are separate with one tab
                    m_MessageContents.add(trimmed);

                } else {
                    m_ParserState = ParserState.STATE_COMMENTS;
                    if (m_MessageContents.size() > 0) {
                        AddClientMessage(m_MessageContents, m_Header);
                        m_MessageContents.clear();
                    }

                    if (!m_isNewAPIRequest) {
                        m_lastClientName = null;
                        m_lastClientSocket = null;
                        m_lastClientUID = null;
                    }

                    ParseLine(str, in);
                }
                break;
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="STATE_SERVER_MESSAGE">
            case STATE_SERVER_MESSAGE: {
                if (str.length() > 0
                        && ((regServerContinue.matcher(str)).find())) {
                    m_MessageContents.add(str);
                } else {
                    AddServerMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    customEvent = false;
                    return str; //
                }
            }
            break;

            case STATE_SERVERCUSTOM_MESSAGE: {
                if (str.length() > 0
                        && str.contains("Server: Switch")) {
                    m_MessageContents.add(str);
                    m_ParserState = ParserState.STATE_SERVER_MESSAGE;

                } else {
                    Main.logger.error("l " + m_CurrentLine + ": Unexpected line reading custom state - [" + str + "]");
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                    customEvent = false;
                    return ""; //
                }
                break;
            }

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="STATE_ACTION_MESSAGE">
            case STATE_ACTION_MESSAGE:
                if (trimmed.length() != 0 && (str.startsWith("\t"))) {
                    // Make sure Attribute and value are separate with one tab
                    m_MessageContents.add(trimmed);

                } else {
                    if (m_MessageContents.size() > 0) {
                        AddActionMessage(m_MessageContents, m_Header);
                    }
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                    return str;
                }
                break;
//</editor-fold>
        }
        return null;
    }

    protected void ParseServerName(String str) {

        Matcher matcher = ptServerName.matcher(str);
        if (matcher.find()) {
            try {
                m_ServerName = matcher.group(1);
            } catch (Exception e) {
                Main.logger.error("StatServer name conversion failed: " + e, e);
            }
        }
    }

    protected void AddClientMessage(ArrayList contents, String header) throws Exception {
        String[] headerList = header.split(" ");
        StSRequestHistoryMessage msg;

        if ((headerList.length == 6) && headerList[3].startsWith("Creating")) {
            String event = headerList[3];
            msg = new StSRequestHistoryMessage(event, contents);
        } else if (headerList.length > 4) {
            String event = headerList[4].substring(1, headerList[4].length() - 1);
            msg = new StSRequestHistoryMessage(event, contents);
        } else {
            return;
        }

        if ((msg.GetMessageName().equals("OpenStat")
                || msg.GetMessageName().equals("GetStat")
                || msg.GetMessageName().equals("PeekStat")
                || msg.GetMessageName().equals("Current")
                || msg.GetMessageName().equals("StatOpened")
                || msg.GetMessageName().equals("Info")
                || msg.GetMessageName().equals("Error"))) {
            String timestamp = headerList[1].substring(0, headerList[1].length() - 1) + ".000";

            msg.setM_Timestamp(getLastTimeStamp());

            msg.SetOffset(m_HeaderOffset);
            int headerLine = m_CurrentLine - contents.size() + 3; //extra Name and Socket
            if (headerLine < 1) {
                headerLine = 1;
            }
            msg.SetLine(headerLine);
            msg.SetFileBytes((int) (m_CurrentFilePos - m_HeaderOffset));
            msg.AddToDB(m_tables);
            m_dbRecords++;
        }

    }

    protected void AddServerMessage() throws Exception {
        if (customEvent || (m_MessageContents.size() > 0 && m_MessageContents.get(0).startsWith("Server: IxnEvent"))) {
            Main.logger.trace("is IxnEvent");
            IxnSS msg = new IxnSS(m_MessageContents, customEvent);
            SetStdFieldsAndAdd(msg);
            customEvent = false;
        } else {
            StSTEventMessage msg = new StSTEventMessage(m_MessageContents);
            SetStdFieldsAndAdd(msg);
        }
    }

    protected void AddActionMessage(ArrayList contents, String header) throws Exception {
        StSActionMessage msg = new StSActionMessage(header, contents);
        SetStdFieldsAndAdd(msg);
    }

    private void AddConfigMessage(String substring) {
        ConfigUpdateRecord msg = new ConfigUpdateRecord(substring);
        try {
            Matcher m;
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

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
    }

    enum ParserState {

        STATE_HEADER,
        STATE_COMMENTS,
        STATE_CLIENT_MESSAGE, /* message from/to StatServer client */
        STATE_SERVER_MESSAGE, /* message from/to any Server what StatServer monitors as client (TServer, etc.) */
        STATE_ACTION_MESSAGE, /* message about updated Action */
        STATE_STATUS_MESSAGE,
        STATE_JAVA_ON_DATA2,
        STATE_JAVA_ON_DATA1,
        STATE_CAPACITY,
        STATE_TMESSAGESTART, STATE_SERVERCUSTOM_MESSAGE
        /* message about updated Status */

    }

}
