package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author akolo
 */
public class SCSParser extends Parser {

    private static final Pattern regAppRunModeChanged = Pattern.compile("^AppRunModeChanged newMode='(\\w+)',\\s+App\\s+\\<DBID=(\\d+),\\s+([^,]+),\\s+PID=(\\d+)\\>,\\s+\\[prevMode=(\\w+)\\],\\s+(\\w+)");
    private static final Pattern regClientMessage = Pattern.compile("^### MESSAGE#=(\\d+),\\s+'(.+)'$");
    private static final Pattern regAlarmCleared = Pattern.compile("^Alarm   (\\S+)   (\\S+)   GCTI-00-04210   Active Alarm \"([^\"]+)\" Cleared");
    private static final Pattern regAlarmCreated = Pattern.compile("^CREATE ActiveAlarm  <(\\d+), ([^>]+)> for  App=<(\\d+),\\s*([^>]+)>.+under control=1");
    private static final Pattern regAlarmActive = Pattern.compile("^ActiveAlarm::SetDeleteTimer  <(\\d+), ([^>]+)> for App=<(\\d+), ([^>]+)>,");

    private static final Pattern regNOtificationReceived = Pattern.compile("\\#{4} App\\{(\\d+),(\\w+)\\} status changed to (\\w+) mode=(\\w+), \\[prev=(\\w+)\\]");
    private static final Pattern regNotifyApp = Pattern.compile("'([^']+)': NotifyApp: status= (\\w+) mode=(\\w+) pid=(\\d+) \\{(\\d+), (\\w+)\\}");
    private static final Pattern regSelfServer = Pattern.compile("SelfServer: Internal Change RUNMODE from '(\\w+)' to '(\\w+)'");
    private static final Pattern regSCSReq = Pattern.compile("\\#{5} Request change RUNMODE for Application \\<(\\d+),(\\w+)\\> from '(\\w+)' to '(\\w+)'");

    private static final Pattern regLineSkip = Pattern.compile("^[>\\s]*");
    private static final Pattern regNotParseMessage = Pattern.compile("(30201|04210)");
    private static final Pattern ptServerName = Pattern.compile("^Application\\s+name:\\s+(\\S+)");
    final int MSG_STRING_LIMIT = 200;
    private final boolean customEvent = false;
    // parse state contants
    long m_CurrentFilePos;
    long m_HeaderOffset;
    String m_ServerName;

    int m_dbRecords = 0;
    private ParserState m_ParserState;
    private String m_LastLine;
    private Message msg = null;
    private String lastMSGID;
    private String lastMSGText;

    public SCSParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_SCS, m_tables);
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

                s = ParseGenesysSCS(str, TableType.MsgSCServer, regNotParseMessage, regLineSkip);

                if ((m = regClientMessage.matcher(s)).find()) {
                    lastMSGID = m.group(1);
                    lastMSGText = m.group(2);
                    m_ParserState = ParserState.STATE_CLIENT_MESSAGE_RECEIVED;
                } else if ((m = regAlarmCreated.matcher(s)).find()) {
                    msg = new SCSAlarm(AlarmState.Creating, fileInfo.getRecordID());
                    ((SCSAlarm) msg).setAlarmDBID(m.group(1));
                    ((SCSAlarm) msg).setAlarmName(m.group(2));
                    ((SCSAlarm) msg).setAppDBID(m.group(3));
                    ((SCSAlarm) msg).setAppName(m.group(4));

                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_ALARM_CREATED;
                    setSavedFilePos(getFilePos());
                } else if ((m = regAlarmCleared.matcher(s)).find()) {
                    msg = new SCSAlarm(AlarmState.Releasing, fileInfo.getRecordID());
                    ((SCSAlarm) msg).setHost(m.group(1));
                    ((SCSAlarm) msg).setAppName(m.group(2));
                    ((SCSAlarm) msg).setAlarmName(m.group(3));

                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_ALARM_PARAMS_CLEARED;
                    setSavedFilePos(getFilePos());
                } else if ((m = regAppRunModeChanged.matcher(s)).find()) {
                    SCSAppStatus _msg = new SCSAppStatus(fileInfo.getRecordID());
                    _msg.setNewMode(m.group(1));
                    _msg.setAppDBID(m.group(2));
                    _msg.setAppName(m.group(3));
                    _msg.setPID(m.group(4));
                    _msg.setOldMode(m.group(5));
                    _msg.setStatus(m.group(6));
                    _msg.setEvent("AppRunModeChanged");
                    SetStdFieldsAndAdd(_msg);
                } else if ((m = regNotifyApp.matcher(s)).find()) {
                    SCSAppStatus _msg = new SCSAppStatus(fileInfo.getRecordID());
                    _msg.setNewMode(m.group(3));
                    _msg.setAppDBID(m.group(5));
                    _msg.setAppName(m.group(6));
                    _msg.setPID(m.group(4));
                    _msg.setStatus(m.group(2));
                    _msg.setHost(m.group(1));
                    _msg.setEvent("NotifyApp");
                    SetStdFieldsAndAdd(_msg);

                } else if ((m = regSCSReq.matcher(s)).find()) {
                    SCSAppStatus _msg = new SCSAppStatus(fileInfo.getRecordID());
                    _msg.setAppDBID(m.group(1));
                    _msg.setAppName(m.group(2));
                    _msg.setOldMode(m.group(3));
                    _msg.setNewMode(m.group(4));
                    _msg.setEvent("Request change RUNMODE");
                    SetStdFieldsAndAdd(_msg);

                } else if ((m = regSelfServer.matcher(s)).find()) {
                    SCSSelfStatus _msg = new SCSSelfStatus(fileInfo.getRecordID());
                    _msg.setNewMode(m.group(2));
                    _msg.setOldMode(m.group(1));
                    SetStdFieldsAndAdd(_msg);

                }

                break;
//</editor-fold>

            case STATE_CLIENT_MESSAGE_RECEIVED: {
                s = ParseGenesysSCS(str, TableType.MsgSCServer, regNotParseMessage, regLineSkip);
                if ((m = regAlarmCreated.matcher(s)).find()) {
                    msg = new SCSAlarm(AlarmState.Creating, fileInfo.getRecordID());
                    ((SCSAlarm) msg).setMsgID(lastMSGID);
                    ((SCSAlarm) msg).setMsgText(lastMSGText);
                    lastMSGID = null;
                    lastMSGText = null;
                    ((SCSAlarm) msg).setAlarmDBID(m.group(1));
                    ((SCSAlarm) msg).setAlarmName(m.group(2));
                    ((SCSAlarm) msg).setAppDBID(m.group(3));
                    ((SCSAlarm) msg).setAppName(m.group(4));

                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_ALARM_CREATED;
                    setSavedFilePos(getFilePos());
                } else if ((m = regAlarmCleared.matcher(s)).find()) {
                    msg = new SCSAlarm(AlarmState.Releasing, fileInfo.getRecordID());
                    ((SCSAlarm) msg).setMsgID(lastMSGID);
                    ((SCSAlarm) msg).setMsgText(lastMSGText);
                    lastMSGID = null;
                    lastMSGText = null;
                    ((SCSAlarm) msg).setHost(m.group(1));
                    ((SCSAlarm) msg).setAppName(m.group(2));
                    ((SCSAlarm) msg).setAlarmName(m.group(3));

                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_ALARM_PARAMS_CLEARED;
                    setSavedFilePos(getFilePos());
                } else if ((m = regAlarmActive.matcher(s)).find()) {
                    msg = new SCSAlarm(AlarmState.ReActivate, fileInfo.getRecordID());
                    ((SCSAlarm) msg).setMsgID(lastMSGID);
                    ((SCSAlarm) msg).setMsgText(lastMSGText);
                    lastMSGID = null;
                    lastMSGText = null;
                    ((SCSAlarm) msg).setAlarmDBID(m.group(1));
                    ((SCSAlarm) msg).setAlarmName(m.group(2));
                    ((SCSAlarm) msg).setAppDBID(m.group(3));
                    ((SCSAlarm) msg).setAppName(m.group(4));

                    SetStdFieldsAndAdd(msg);
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENTS;
                } else {
                    SCSClientLogMessage _msg = new SCSClientLogMessage(lastMSGID, lastMSGText, fileInfo.getRecordID());

                    SetStdFieldsAndAdd(_msg);
                    m_ParserState = ParserState.STATE_COMMENTS;
                    return str;
                }

            }
            break;

            case STATE_ALARM_CREATED: {
                if (str.contains("GCTI-43-10612") || str.contains("### LOCAL MESSAGE#=") || str.contains("GCTI-00-04115 ")) {
                    m_MessageContents.add(str);
                } else if (str.contains("GCTI-00-04200")) {
                    m_MessageContents.add(str);
                    m_ParserState = ParserState.STATE_ALARM_PARAMS_CREATED;
                } else {
                    return completeAlarm(str);

                }
            }
            break;

            case STATE_ALARM_PARAMS_CLEARED: {
                if (!str.isEmpty() && Character.isSpaceChar(str.charAt(0))) {
                    m_MessageContents.add(str);
                    String[] split = StringUtils.split(str.substring(1), ":");
                    if (split != null) {
                        if (split[0].equals("ID")) {
                            ((SCSAlarm) msg).setAlarmID(split[1]);
                        } else if (split[0].equals("CLEARRSN")) {
                            ((SCSAlarm) msg).setReason(split[1]);
                        }
                    }
                } else if (str.contains("~ActiveAlarm") || str.contains("### LOCAL MESSAGE#=") || str.contains("GCTI-43-10611")) {
                    m_MessageContents.add(str);
                } else {
                    return completeAlarm(str);
                }

            }
            break;

            case STATE_ALARM_PARAMS_CREATED: {
                if (!str.isEmpty() && Character.isSpaceChar(str.charAt(0))) {
                    m_MessageContents.add(str);
                    String[] split = StringUtils.split(str.substring(1), ":");
                    if (split != null) {
                        if (split[0].equals("ID")) {
                            ((SCSAlarm) msg).setAlarmID(split[1]);
                        } else if (split[0].equals("RSN")) {
                            ((SCSAlarm) msg).setReason(split[1]);
                        } else if (split[0].equals("CAT")) {
                            ((SCSAlarm) msg).setCategory(split[1]);
                        }
                    }
                } else {
                    return completeAlarm(str);
                }
            }
            break;

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
                    SCSAppStatus _msg = new SCSAppStatus(m_MessageContents, fileInfo.getRecordID());
                    _msg.setAppDBID(m.group(1));
                    _msg.setAppName(m.group(2));
                    _msg.setNewMode(m.group(3));
                    _msg.setOldMode(m.group(5));
                    _msg.setStatus(m.group(4));
                    SetStdFieldsAndAdd(_msg);

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

    protected void AddClientMessage(ArrayList contents, String header) throws Exception {
        String[] headerList = header.split(" ");
        StSRequestHistoryMessage _msg;

        if ((headerList.length == 6) && headerList[3].startsWith("Creating")) {
            String event = headerList[3];
            _msg = new StSRequestHistoryMessage(event, contents, fileInfo.getRecordID());
        } else if (headerList.length > 4) {
            String event = headerList[4].substring(1, headerList[4].length() - 1);
            _msg = new StSRequestHistoryMessage(event, contents, fileInfo.getRecordID());
        } else {
            return;
        }

        if ((_msg.GetMessageName().equals("OpenStat")
                || _msg.GetMessageName().equals("GetStat")
                || _msg.GetMessageName().equals("PeekStat")
                || _msg.GetMessageName().equals("Current")
                || _msg.GetMessageName().equals("StatOpened")
                || _msg.GetMessageName().equals("Info")
                || _msg.GetMessageName().equals("Error"))) {
            String timestamp = headerList[1].substring(0, headerList[1].length() - 1) + ".000";

            _msg.setM_Timestamp(getLastTimeStamp());

            _msg.SetOffset(m_HeaderOffset);
            int headerLine = m_CurrentLine - contents.size() + 3; //extra Name and Socket
            if (headerLine < 1) {
                headerLine = 1;
            }
            _msg.SetLine(headerLine);
            _msg.SetFileBytes((int) (m_CurrentFilePos - m_HeaderOffset));
            addToDB(_msg);

            m_dbRecords++;
        }

    }

    private void addRunModeChanged(String substring) {
        ConfigUpdateRecord _msg = new ConfigUpdateRecord(substring, fileInfo.getRecordID());
        try {
            Matcher m;
//            msg.setObjName(Message.FindByRx(m_MessageContents, regCfgObjectName, 1, ""));
//            msg.setOp(Message.FindByRx(m_MessageContents, regCfgOp, 1, ""));
//
//            if ((m = Message.FindMatcher(m_MessageContents, regCfgObjectType)) != null) {
//                msg.setObjectType(m.group(1));
//                msg.setObjectDBID(m.group(2));
//            }

            SetStdFieldsAndAdd(_msg);
        } catch (Exception e) {
            Main.logger.error("Not added \"" + _msg.getM_type() + "\" record:" + e.getMessage(), e);
        }
    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
        m_tables.put(TableType.SCSClientLogMessage, new SCSClientLogTable(Main.getInstance().getM_accessor(), TableType.SCSClientLogMessage));
        m_tables.put(TableType.SCSAlarm, new SCSAlarmTable(Main.getInstance().getM_accessor(), TableType.SCSAlarm));

    }

    private String completeAlarm(String str) {
        msg.addMessageLines(m_MessageContents);
        SetStdFieldsAndAdd(msg);
        m_MessageContents.clear();
        m_ParserState = ParserState.STATE_COMMENTS;
        msg = null;
        return str;
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
        STATE_TMESSAGESTART,
        STATE_SERVERCUSTOM_MESSAGE,
        STATE_ALARM_CREATED,
        STATE_ALARM_PARAMS_CREATED,
        STATE_ALARM_CLEARED,
        STATE_ALARM_PARAMS_CLEARED,
        STATE_CLIENT_MESSAGE_RECEIVED
        /* message about updated Status */

    }

    public enum AlarmState {

        Creating("Creating"),
        Releasing("Releasing"),
        ReActivate("Existing"),
        ;

        private final String name;

        AlarmState(String s) {
            name = s.toLowerCase();
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equalsIgnoreCase(otherName);
        }

        @Override
        public String toString() {
            return this.name.toLowerCase();
        }

    }

    private class SCSClientLogMessage extends Message {

        private int msgID;
        private String msgText;

        SCSClientLogMessage(TableType t, int fileID) {
            super(t, fileID);
        }

        private SCSClientLogMessage(String msgID, String msgText, int fileID) {
            this(TableType.SCSClientLogMessage, fileID);

            setMsgID(msgID);
            setMsgText(msgText);
        }

        @Override
        public boolean fillStat(PreparedStatement stmt) throws SQLException {
            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, getM_FileBytes());
            stmt.setLong(5, getM_line());

            setFieldInt(stmt, 6, getMsgID());
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.Misc, getMsgText()));
            return true;
        }

        public int getMsgID() {
            return msgID;
        }

        final public void setMsgID(String msgID) {
            try {
                this.msgID = Integer.parseInt(msgID);
            } catch (NumberFormatException e) {
                Main.logger.error("Cannot parse [" + msgID + "]", e);
                this.msgID = 0;
            }
        }

        public String getMsgText() {
            return msgText;
        }

        final public void setMsgText(String msgText) {
            this.msgText = msgText;
        }

    }
//</editor-fold>d>

    private class SCSClientLogTable extends DBTable {

        public SCSClientLogTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("msgID");
            addIndex("messageTextID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",msgID int"
                    + ",messageTextID int"
                    + ");";
            getM_dbAccessor().runQuery(query);


        }

        @Override
        public String getInsert() {
            return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
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

    //<editor-fold defaultstate="collapsed" desc="SCSAlarm">
    private class SCSAlarm extends SCSClientLogMessage {

        private final AlarmState alarmState;
        private Integer alarmDBID;
        private Integer appDBID;
        private String alarmName;
        private String appName;
        private String alarmID;
        private String reason;
        private String category;
        private String host;

        private SCSAlarm(AlarmState st, int fileID) {
            super(TableType.SCSAlarm, fileID);
            this.alarmState = st;
        }

        public String getAlarmState() {
            return alarmState.toString();
        }

        public Integer getAlarmDBID() {
            return alarmDBID;
        }

        private void setAlarmDBID(String group) {
            alarmDBID = getIntOrDef(group, null, true);
        }

        public Integer getAppDBID() {
            return appDBID;
        }

        private void setAppDBID(String group) {
            appDBID = getIntOrDef(group, null, true);
        }

        public String getAlarmName() {
            return alarmName;
        }

        private void setAlarmName(String group) {
            alarmName = group;
        }

        public String getAppName() {
            return appName;
        }

        private void setAppName(String group) {
            appName = group;
        }

        public String getAlarmID() {
            return alarmID;
        }

        private void setAlarmID(String string) {
            alarmID = string;
        }

        public String getReason() {
            return reason;
        }

        private void setReason(String string) {
            reason = string;
        }

        public String getCategory() {
            return category;
        }

        private void setCategory(String string) {
            category = string;
        }

        private void setHost(String group) {
            this.host = group;
        }

    }
//</editor-fold>

    private class SCSAlarmTable extends DBTable {

        public SCSAlarmTable(SqliteAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("alarmNameID");
            addIndex("logAppNameID");
            addIndex("reasonID");
            addIndex("categoryID");
            addIndex("alarmIDID");
            addIndex("msgID");
            addIndex("msgTextID");
            addIndex("alarmStateID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",alarmStateID int"
                    + ",alarmDBID int"
                    + ",appDBID int"
                    + ",alarmNameID int"
                    + ",logAppNameID int"
                    + ",reasonID int"
                    + ",categoryID int"
                    + ",alarmIDID int"
                    + ",msgID int"
                    + ",msgTextID int"
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
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ",?"
                    + ");"
                    ;
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
