/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ssydoruk
 */
public class IxnServerParser extends Parser {

    private static final Pattern regNotParseMessage = Pattern.compile("^(24102|24101|26015|26016|26019|26020|26021|26202|26201|"
            //            + "25020|25021|25030|25032" // database activity
            + "30010|30004|30006|30013" // database activity
            + ")");

    private static final Pattern regTmessage = Pattern.compile("^(24102|24101|26015|26016|26019|26020|26202|26201|30010|30004|30006|30013)");

    private static final Pattern regDBActivity = Pattern.compile("^([^:]+):.+request id: '(\\d+)'.+interaction id:'([^']+)'");

    //        2016-04-29T16:26:11.580 Trc 50002 TEvent: EventAttachedDataChanged
    private static final Pattern regTMessageEnd = Pattern.compile("^([^\\s\"]+|$)");
    //	private DBAccessor m_accessor;
    private static final HashSet<String> nonIxnMsg = getNonIxnMsg();
    HashMap<String, String> prevSeqno = new HashMap();
    private ParserState m_ParserState;
    IxnServerParser(HashMap<String, DBTable> m_tables) {
        super(FileInfoType.type_IxnServer, m_tables);
    }

    private static HashSet<String> getNonIxnMsg() {
        HashSet<String> ret = new HashSet<>();
        ret.add("RequestDistributeEvent");
        return ret;
    }

    private void AddIxnMessage() {
        Ixn msg = new Ixn(m_MessageContents,  fileInfo.getRecordID());
        if (nonIxnMsg.contains(msg.GetMessageName())) {
            IxnNonIxn nonIxn = new IxnNonIxn(TableType.IxnNonIxn, msg,  fileInfo.getRecordID());
            SetStdFieldsAndAdd(nonIxn);

        } else {
            SetStdFieldsAndAdd(msg);
        }
    }

    //    private static Class<? extends Record>[] avail = new Class[]{Ixn.class};
    @Override
    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentLine = line;

//        for (Class<? extends Record> class1 : avail) {
//            try {
//                class1.getMethod("resetCounter").invoke(null);
//            } catch (NoSuchMethodException ex) {
//                Main.logger.error( ex);
//            } catch (SecurityException ex) {
//                Main.logger.error( ex);
//            } catch (IllegalAccessException ex) {
//                Main.logger.error( ex);
//            } catch (IllegalArgumentException ex) {
//                Main.logger.error( ex);
//            } catch (InvocationTargetException ex) {
//                Main.logger.error( ex);
//            }
//        }
        try {
            input.skip(offset);

            m_ParserState = ParserState.STATE_HEADER;
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

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

        ParseCustom(str, 0);

        switch (m_ParserState) {
            case STATE_HEADER: {
                if (s.isEmpty()) {
                    m_ParserState = ParserState.STATE_COMMENT;
                }
            }
            break;

            case STATE_COMMENT:
                m_lineStarted = m_CurrentLine;

                s = ParseGenesys(str, TableType.MsgIxnServer, regNotParseMessage);

//                Main.logger.debug("using s:["+s+"]");
                GenesysMsg lastLogMsg;
                if ((lastLogMsg = getLastLogMsg()) != null && (regTmessage.matcher(lastLogMsg.getLastGenesysMsgID())).find()) {//starts on clear line
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_TMESSAGE;
                    break;
                } else if ((m = regDBActivity.matcher(s)).find()) {
                    IxnDBActivity msg = new IxnDBActivity(m.group(1), m.group(2), m.group(3),  fileInfo.getRecordID());
                    SetStdFieldsAndAdd(msg);
                    break;
                }

//                else if ((m = regCGStart .matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_CG;
//                } else if ((m = regPAStart .matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_PA_SESSION_INFO;
//                } else if ((m = regPAAgentInfo .matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_AGENT_INFO;
//                } else if ((m = regPAEventInfo .matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_EVENT_INFO;
//                } else if ((m = regStatEvent .matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    statEventType = StatEventType.valueOf(m.group(1));// name of StatEvent
//                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_STAT_EVENT;
//                } else if ((m = regSCXMLTreatment .matcher(s)).find()) {
//                    AddSCXMLTreatmentMessage(m.group(1), m.group(2), m.group(3),
//                            s.substring(m.end()));
//                } else if ((m = regSCXMLScript .matcher(str)).find()) {
//                    AddSCXMLScript(m.group(1), s.substring(m.end()));
//                } else if ((m = regAgentAssignment .matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
////                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_AGENT_ASSIGNMENT;
//                } else if ((m = regRecordCreate .matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_RECORD_CREATE;
//                } else if ((m = regIxnMsg.matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_IXN_MSG;
//                } else if ((m = regHTTPMessage.matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_HTTP;
//                } else if ((m = regClient.matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s);
//                    m_ParserState = ParserState.STATE_CLIENT;
//                }
//                break;
//            case STATE_CLIENT:
//                if (str.length() > 0
//                        && ((m = regClientContinue.matcher(str)).find())) {
//                    m_MessageContents.add(str);
//                } else {
//                    AddClientMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str; //
//                }
//                break;
//
//            case STATE_HTTP:
//                if (str.length() > 0
//                        && ((m = regOCSHTTPContinue.matcher(str)).find())) {
//                    m_MessageContents.add(str);
//                } else {
//                    AddHTTPMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str; //
//                }
//                break;
//
//            case STATE_IXN_MSG:
//                if (str.length() > 0
//                        && ((m = regOCSIxnContinue.matcher(str)).find())) {
//                    m_MessageContents.add(str);
//                } else {
//                    AddIxnMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str; //
//                }
//                break;
//
//            case STATE_RECORD_CREATE:
//                if (str.length() > 0
//                        && ((m = regRecCreateContinue.matcher(str)).find())) {
//                    m_MessageContents.add(str);
//                } else {
//                    AddRecCreateMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str; //
//                }
//                break;
//
//            case STATE_AGENT_ASSIGNMENT:
//                if (str.length() > 0) {
//                    if ((m = regCampaignAssignment.matcher(str)).find()) {
//                        if (m_MessageContents.size() > 0) { //not first message
//                            AddAssignmentMessage();
//                            m_MessageContents.clear();
//                            m_lineStarted = m_CurrentLine;
//                        }
//                        m_MessageContents.add(str);
//                    } else {
//                        m_MessageContents.add(str);
//                    }
//                } else {
//                    AddAssignmentMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                }
//                break;
//
//            case STATE_SCXML_TREATMENT:
//                if (str.length() > 0 && (Character.isWhitespace(str.charAt(0))
//                        || str.charAt(0) != '}')) {
//                    m_MessageContents.add(str);
//                } else {
////                    AddSCXMLTreatmentMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str;
//                }
//                break;
//
//            case STATE_STAT_EVENT:
//                if (isStatEventEnded(statEventType, str)) {
//                    AddStatEventMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str;
//                } else {
//                    if (statEventType == StatEventType.SEventCurrentTargetState_Snapshot
//                            && str.length() == 0) {
//                        AddStatEventMessage();
//                        m_MessageContents.clear();
//                        m_lineStarted = m_CurrentLine;
//                        setSavedFilePos(getFilePos());
//                    } else {
//                        m_MessageContents.add(str);
//                    }
//                }
//                break;
//
//            case STATE_AGENT_INFO:
//                if (str.length() > 0 && (Character.isWhitespace(str.charAt(0)))) {
//                    m_MessageContents.add(str);
//                } else {
//                    AddPAAgentInfoMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str;
//                }
//                break;
//
//            case STATE_EVENT_INFO:
//                if (str.length() > 0 && (Character.isWhitespace(str.charAt(0)))) {
//                    m_MessageContents.add(str);
//                } else {
//                    AddPAEventInfoMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str;
//                }
//                break;
//
//            case STATE_CG:
//                if (str.length() > 0 && Character.isWhitespace(str.charAt(0))) {
//                    m_MessageContents.add(str);
//                } else {
//                    AddCGMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str; //
//                }
//                break;
//
//            case STATE_PA_SESSION_INFO:
//                if (str.length() > 0 && Character.isWhitespace(str.charAt(0))) {
//                    m_MessageContents.add(str);
//                } else {
//                    AddPASessionInfoMessage();
//                    m_MessageContents.clear();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    return str; //
//                }
//                break;
//            case STATE_TMESSAGE_START:
//                //	 : message EventServerInfo
//                //static Matcher regTMessageName=Pattern.compile(": message (.+)");
////                m_msgName = SubstrAfterPrefix(s, ": message ");
//                m_MessageContents=new ArrayList();
//                m_ParserState=ParserState.STATE_TMESSAGE;
//                break;
//            case STATE_TMESSAGESTART:
//                if (str.startsWith("request to")) {
//                    m_MessageContents.add(str);
//                    str = null;
//                }
//                m_ParserState = ParserState.STATE_TMESSAGE;
//                return str;
                break;

            case STATE_TMESSAGE: {
                if (regTMessageEnd.matcher(str).find()) {
                    AddIxnMessage();
                    m_MessageContents.clear();
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                } else {
                    m_MessageContents.add(str);
                }
            }
            break;
        }
        return null;
    }

    @Override
    void init(HashMap<String, DBTable> m_tables) {
        m_tables.put(TableType.IxnNonIxn.toString(), new IxnNonIxnTable(Main.getInstance().getM_accessor(), TableType.IxnNonIxn));

    }

    enum ParserState {

        STATE_HEADER,
        STATE_TMESSAGE,
        STATE_TMESSAGESTART,
        STATE_COMMENT,
        STATE_CG,
        STATE_PA_SESSION_INFO,
        STATE_AGENT_INFO,
        STATE_EVENT_INFO,
        STATE_STAT_EVENT,
        STATE_RECORD_CREATE,
        STATE_IXN_MSG,
        STATE_HTTP,
        STATE_CLIENT,
        STATE_AGENT_ASSIGNMENT,
        STATE_SCXML_TREATMENT
    }

    enum StatEventType {

        UNKNOWN(""),
        SEventInfo("SEventInfo"),
        SEventCurrentTargetState_Snapshot("SEventCurrentTargetState_Snapshot"),
        SEventCurrentTargetState_TargetUpdated("SEventCurrentTargetState_TargetUpdated"),
        SEventCurrentTargetState_TargetAdded("SEventCurrentTargetState_TargetAdded"),
        SEventRegistered("SEventRegistered"),
        SEventError("SEventError"),
        SEventStatInvalid("SEventStatInvalid"),
        SEventStatOpened("SEventStatOpened"),
        SEventDataStreamOpeningFailure("SEventDataStreamOpeningFailure"),
        SEventDataStreamMessage("SEventDataStreamMessage"),
        SEventDataStreamOpened("SEventDataStreamOpened"),
        SEventCurrentTargetState_TargetRemoved("SEventCurrentTargetState_TargetRemoved"),
        SEventStatClosed("SEventStatClosed");

        private final String name;

        StatEventType(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equals(otherName);
        }

        @Override
        public String toString() {
            if (name != null) {
                return this.name;
            } else {
                return "";
            }
        }

    }

}
