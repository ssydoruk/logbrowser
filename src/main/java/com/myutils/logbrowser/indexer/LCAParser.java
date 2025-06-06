package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author akolo
 */
public class LCAParser extends Parser {

    private static final Pattern reqNotifyStatus = Pattern.compile("^\\s*Notify STATUS: App\\<(\\w+),\\s*([^,]+), pid=(\\w+), ([^,]+), (\\w+)");
    private static final Pattern regNotifyRunmode = Pattern.compile("^\\s*Notify RUNMODE: App\\<(\\w+),\\s*([^,]+), pid=(\\w+), ([^,]+), (\\w+)");

    private static final Pattern reqSCSRequest = Pattern.compile("\\[Request[^\\]]+\\]$");
    private static final Pattern reqSCSRequestStartApplication = Pattern.compile("\\[RequestStartApplication[^\\]]*\\]$");
    private static final Pattern reqSCSDisconnected = Pattern.compile("SCSRequester '(.+)' disconnected, fd=(\\w+)");

    private static final Pattern regNOtificationReceived = Pattern.compile("\\#{4} App\\{(\\w+),(\\w+)\\} status changed to (\\w+) mode=(\\w+), \\[prev=(\\w+)\\]");
//    private static final Pattern regNotifyApp = Pattern.compile("'([^']+)': NotifyApp: status= (\\w+) mode=(\\w+) pid=(\\w+) \\{(\\w+), (\\w+)\\}");
//    private static final Pattern regSelfServer = Pattern.compile("SelfServer: Internal Change RUNMODE from '(\\w+)' to '(\\w+)'");
//    private static final Pattern regSCSReq = Pattern.compile("\\#{5} Request change RUNMODE for Application \\<(\\w+),(\\w+)\\> from '(\\w+)' to '(\\w+)'");

    private static final Pattern regLineSkip = Pattern.compile("^[>\\s]*");
    private static final Pattern regNotParseMessage = Pattern.compile("(30201)");
    private static final Pattern ptServerName = Pattern.compile("^Application\\s+name:\\s+(\\S+)");
    private static final Pattern regCfgObjectName = Pattern.compile("(?:name|userName)='([^']+)'");
    private static final Pattern regCfgObjectType = Pattern.compile("^Cfg([^=]+)=\\{DBID=(\\w+)");
    private static final Pattern regCfgOp = Pattern.compile("^(Creating|Adding\\s[\\w]+)");
    // parse state contants
    String m_ServerName;

    int m_dbRecords = 0;
    private ParserState m_ParserState;

    public LCAParser(DBTables m_tables) {
        super(FileInfoType.type_LCA, m_tables);
    }

    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
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
//                    m_LastLine = str; // store previous line for server name
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
        }


        return m_CurrentLine - line;
    }

    private String ParseLine(String str, BufferedReaderCrLf in) throws Exception {
        String s = str;
        Matcher m;

        String trimmed = str.trim();

        Main.logger.trace("ParseLine " + m_CurrentLine + ": state -" + m_ParserState);
        if (!ParseCustom(str, 0)) {
            switch (m_ParserState) {
                case STATE_HEADER:
                case STATE_COMMENTS:
                    return null;

                default:
                    Main.logger.error("CustomSearch " + getLastCustomSearch().toString() + " has parserRest=\"false\" while the m_ParserState=[" + m_ParserState + "]. The flag ignored");
                    break;
            }
        }

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

                m_lineStarted = m_CurrentLine;

                s = ParseGenesys(str, TableType.MsgLCAServer, regNotParseMessage, regLineSkip);

                if (s.startsWith("Client: new client")) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_CLIENTCONNECT;
                    break;
                } else if (s.endsWith("disconnected.")) {
                    LCAClient msg = new LCAClient(s, fileInfo.getRecordID());
                    msg.parseDisconnect();
                    SetStdFieldsAndAdd(msg);
                } else if ((m = reqSCSDisconnected.matcher(s)).find()) {
                    LCAClient msg = new LCAClient(s, fileInfo.getRecordID());
                    msg.setAppName(m.group(1));
                    msg.setFD(m.group(2));
                    msg.setConnected(false);
                    msg.setSCS(true);
                    SetStdFieldsAndAdd(msg);
                } else if ((reqSCSRequestStartApplication.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_SCSREQUESTSTARTAPP;
                } else if (s.endsWith("[RequestChangeAppRunMode]")) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_SCSREQUESTCHANGERUNMODE;
                } else if ((reqSCSRequest.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_SCSREQUEST;
                } else if ((m = reqNotifyStatus.matcher(s)).find()) {
                    LCAAppStatus msg = new LCAAppStatus(fileInfo.getRecordID());
                    msg.setMode(m.group(5));
                    msg.setAppDBID(m.group(1));
                    msg.setAppName(m.group(2));
                    msg.setPID(m.group(3));
                    msg.setStatus(m.group(4));
                    SetStdFieldsAndAdd(msg);
                } else if ((m = regNotifyRunmode.matcher(s)).find()) {
                    LCAAppStatus msg = new LCAAppStatus(fileInfo.getRecordID());
                    msg.setMode(m.group(5));
                    msg.setAppDBID(m.group(1));
                    msg.setAppName(m.group(2));
                    msg.setPID(m.group(3));
                    msg.setStatus(m.group(4));
                    SetStdFieldsAndAdd(msg);
                }

                break;
//</editor-fold>

            case STATE_SCSREQUESTSTARTAPP: {
                if (str.contains("CREATE Application")) {
                    m_MessageContents.add(str);
                    LCAAppStatus msg = new LCAAppStatus(m_MessageContents, fileInfo.getRecordID());
                    msg.parseStart();
                    SetStdFieldsAndAdd(msg);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                } else {
                    m_MessageContents.add(str);
                }
                break;
            }

            case STATE_SCSREQUESTCHANGERUNMODE: {
                m_MessageContents.add(str);
                LCAAppStatus msg = new LCAAppStatus(m_MessageContents, fileInfo.getRecordID());
                msg.parseChangeRunMode();
                SetStdFieldsAndAdd(msg);
                m_MessageContents.clear();
                m_ParserState = ParserState.STATE_COMMENTS;
                break;
            }

            case STATE_SCSREQUEST: {
                if (!str.contains("gethostinfo")) {
                    m_MessageContents.add(str);
                    LCAAppStatus msg = new LCAAppStatus(m_MessageContents, fileInfo.getRecordID());
                    msg.parseOtherRequest();
                    SetStdFieldsAndAdd(msg);
                }
                m_MessageContents.clear();
                m_ParserState = ParserState.STATE_COMMENTS;
                break;
            }

            case STATE_CLIENTCONNECT: {
                if (str.contains("Client's IP-address")) {
                    m_MessageContents.add(str);
                    return null;

                } else if (str.startsWith("-A")) { // 
                    return null;
                } else if (str.contains("[RRequestRegisterApplication]")) {
                    m_MessageContents.add(str);
                    AddClientMessage(m_MessageContents, str);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                } else if (str.contains("SCSRequester")) {
                    m_MessageContents.add(str);
                    AddClientMessage(m_MessageContents, str);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                }

                break;

            }

            case STATE_APP_STATUS_CHANGED: {
                if (str.contains("NotifyApp:")) {
                    m_MessageContents.add(str);
                    m_ParserState = ParserState.STATE_APP_STATUS_CHANGED1;

                } else {
                    Main.logger.error("l:" + m_CurrentLine + " Unexpected message in state " + m_ParserState + ": " + str);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    m_MessageContents.clear();
                }
                break;
            }
            case STATE_APP_STATUS_CHANGED1: {
                if ((m = regNOtificationReceived.matcher(str)).find()) {
                    m_MessageContents.add(str);
                    SCSAppStatus msg = new SCSAppStatus(m_MessageContents, fileInfo.getRecordID());
                    msg.setAppDBID(m.group(1));
                    msg.setAppName(m.group(2));
                    msg.setNewMode(m.group(3));
                    msg.setOldMode(m.group(5));
                    msg.setStatus(m.group(4));
                    SetStdFieldsAndAdd(msg);

                } else {
                    Main.logger.error("l:" + m_CurrentLine + " Unexpected message in state " + m_ParserState + ": " + str);
                }
                m_ParserState = ParserState.STATE_COMMENTS;
                m_MessageContents.clear();
                break;
            }
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

    protected void AddClientMessage(ArrayList<String> contents, String header) throws Exception {
        LCAClient msg = new LCAClient(contents, fileInfo.getRecordID());
        msg.parseConnect();
        SetStdFieldsAndAdd(msg);
    }

    private void addRunModeChanged(String substring) {
        ConfigUpdateRecord msg = new ConfigUpdateRecord(substring, fileInfo.getRecordID());
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
    void init(DBTables m_tables) {
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
        STATE_APP_STATUS_CHANGED,
        STATE_APP_STATUS_CHANGED1,
        STATE_TMESSAGESTART, STATE_SERVERCUSTOM_MESSAGE, STATE_CLIENTCONNECT, STATE_SCSREQUEST, STATE_SCSREQUESTSTARTAPP, STATE_SCSREQUESTCHANGERUNMODE
        /* message about updated Status */

    }

}
