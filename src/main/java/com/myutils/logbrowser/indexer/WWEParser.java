/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;
import static com.myutils.logbrowser.indexer.Message.getRx;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ssydoruk
 */
public class WWEParser extends WebParser {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private static final Pattern regMsgStart = Pattern.compile("Handling update message:$");
    private static final Pattern regMsgRequest = Pattern.compile("(?: |=|\\[)'(\\w+)' \\(\\d+\\) attributes:\\s*$");
    private static final Pattern regMsgContactServerResponse = Pattern.compile("Received response \\[([^:\\[\\}]+):\\s*$");

    private static final Pattern regMsgEventRegistered = Pattern.compile("message \\[?'(\\w+)' \\(\\d+\\) attributes:$");
//    private static final Pattern regMsgRequest1 = Pattern.compile("sent: \\['(\\w+)' \\(\\d+\\) attributes:$");

//    java.net.ConnectException: Connection refused
    private static final Pattern regExceptionStart = Pattern.compile("^([\\w\\.]+Exception[\\w\\.]*):(.*)");
    //    static Pattern regMsgStart = Pattern.compile("^\\s*(?:Int 04543|Trc 04541).+essage \\\"*(\\w+).+from \\d+ \\(\\\"([^@\\\"]+)");
    // : message EventServerInfo
//AgentMessage(super=BasicTelephonyMessage(message='EventAgentLogin' (73) attributes:
    private static final Pattern regTMessageStart = Pattern.compile("^(?:\\w+)Message.+message='(\\w+)'");
    //static Pattern regMsgStart=Pattern.compile("^(Int 04543 |Trc 04541 )");

//13:40:18.552 {SessionManager:2} URL [http://orswas.internal.gslb.service.nsw.gov.au/mod-routing/src-gen/IPD_default_mainWorkflow.scxml] associated with script [mod-routing]
    private static final Pattern regAppURL = Pattern.compile("\\}\\sURL \\[([^\\]]+)\\] associated with script \\[([^\\]]+)\\]$");

    private static final Pattern regTMessageAttributesContinue = Pattern.compile("^(Attribute|\\s|TimeStamp:|CallHistoryInfo)");
    private static final Pattern regIndexList = Pattern.compile("^(\\s|\\})");

    private static final Pattern regWWELogMessage = Pattern.compile("^(\\w+)\\s+\\[([^\\]]*)\\] \\[([^\\]]*)\\] \\[([^\\]]*)\\] \\[([^\\]]*)\\]( [\\d\\.]*)? (\\S+)?\\s(\\s|(?:\\S+)\\s+)?(?:(\\S+)\\s+(.+))?");

//    private static final Pattern regElasticSearch = Pattern.compile("\\s+o[^\\.]+\\.e[^\\.]+\\.[\\.\\w]+\\s+(.+)");
    private static final Pattern regKVList = Pattern.compile("reasons=KVList:\\s*$");
    private static final Pattern regIndexListStart = Pattern.compile("\\.IndexList = \\{\\s*$");
    private static final Pattern regWWEUCSStart = Pattern.compile("About to submit request .+\\.requests\\.([^:]+):\\s*$");

    private static final Pattern regWWEIxnServerRequestStart = Pattern.compile("bout to submit request '([^']+)'");
    private static final Pattern regWWEIxnServerEventStart = Pattern.compile("eceived response '([^']+)'");

    private static final Pattern regWWEUCSEvent = Pattern.compile("eceived response .+\\.events\\.([^:]+):\\s*$");
    private static final Pattern regWWEUCSEventOther = Pattern.compile("received message .+\\.events\\.([^:]+):\\s*$");
    private static final Pattern regWWEUCSHandlingEvent = Pattern.compile("handling message .+\\.events\\.([^:]+):\\s*$");
    private static final Pattern regWWEUCSHandlingEventContinue = Pattern.compile("^(\\s|\\}|HasNext|\\]|PageSize|ReferenceId|InteractionData)");

    private static final Pattern regWWEUCSPrepareTask = Pattern.compile(" Prepare task \\[com\\.genesyslab\\.cloud\\.|VoiceUcsInteraction was updated with values|Complete operation with interactionResource");
    private static final Pattern regCreateChatContext = Pattern.compile("createChatServerContextV2 ");
    private static final Pattern regItems = Pattern.compile("item\\(s\\)\\] = \\{\\s*$");
    private static final Pattern regGetRootCategories = Pattern.compile("RequestGetRootCategories:\\s*$");
    private static final Pattern regInteractionAttributes = Pattern.compile("contactserver\\.InteractionAttributes \\{\\s*$");
    private static final Pattern regInteractionProperties = Pattern.compile("interactionProperties=InteractionProperties:\\s*$");

    private static final Pattern regWWEUCSConfigErrorUser = Pattern.compile("failed for user \\[([^\\]]+)\\]:");
    private static final Pattern regWWEUCSConfigErrorContinue = Pattern.compile("^(SATRCFG_|IATRCFG)");

    private static final Pattern regBayeuxId = Pattern.compile("channel=[^,]+, id=(\\d+),");
    private static final Pattern regBayeuxLastConnected = Pattern.compile("last connect (\\d+) ms ago$");
    private static final Pattern regBayeuxAction = Pattern.compile("\\s([><]+)\\s+\\{");
    private static final Pattern regBayeuxChannel = Pattern.compile("channel=([\\w/]+)");
    private static final Pattern regBayeuxMessageType = Pattern.compile("messageType=([\\w/]+)");
    private static final Pattern regURIChatIxnID = Pattern.compile("/chats/(\\w+)");

    private static final Pattern regWWEReceivedInteractions = Pattern.compile(" received interactions from Interaction Server (.+)\\s*$");
    private static final Pattern regIxnContinue = Pattern.compile("^\\s");

    private String m_msgName;
    private String m_TserverSRC;

    private ParserState m_ParserState;

    private final HashMap<String, ParserState> threadParserState = new HashMap<>();

    MsgType MessageType;


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
    private final HashMap<String, String> ThreadAlias = new HashMap<>();
    private String URL = null;
    private String app = null;
    private String m_msg1;
    private boolean skipNextLine;
    private boolean skipMessage;
    private Message msg;

    WWEParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_WWE, m_tables);
        m_MessageContents = new ArrayList();
        ThreadAlias.put("ORSInternal", "FMWeb");
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
        m_CurrentLine = line;
        skipNextLine = false;
        skipMessage = false;

        URL = null;
        app = null;

        try {
            input.skip(offset);

            m_ParserState = ParserState.STATE_INIT;
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            String str;
            while ((str = input.readLine()) != null) {
                m_CurrentLine++;
                Main.logger.trace("l: " + m_CurrentLine + " st:" + m_ParserState + " [" + str + "]");

                try {
                    int l = input.getLastBytesConsumed();
//                    SetBytesConsumed(input.getLastBytesConsumed());
                    setEndFilePos(getFilePos() + l); // to calculate file bytes

                    Object o = str;
                    while (o != null) {
                        o = ParseLine(o, input);
                    }
                    setFilePos(getFilePos() + l);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                }

//                m_CurrentFilePos += str.length()+input.charsSkippedOnReadLine;
            }
//            ParseLine("", null); // to complete the parsing of the last line/last message
        } catch (IOException e) {
            e.printStackTrace();
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

    private Object ParseLine(Object objToParse, BufferedReaderCrLf in) throws Exception {
        if (objToParse == null) {
            return null;
        }
        String str = null;
        String s = null;
        boolean dateParsed = false;
        if (objToParse instanceof String) {
            str = (String) objToParse;
            s = str;

        } else if (objToParse instanceof DateParsed) {
            dp = (DateParsed) objToParse;
            s = dp.rest;
            str = dp.orig;
            dateParsed = true;
        }
        Matcher m;

        if (skipNextLine) {
            skipNextLine = false;
            return null;
        }
        boolean ParseCustom = ParseCustom(str, 0);

        switch (m_ParserState) {
            case STATE_INIT: {
                if (str.startsWith("Start time:") || str.startsWith("+++++++++++++++++++++++++++++++++++++++++++")) {
                    m_ParserState = ParserState.STATE_HEADER;
                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                }

            }
            break;

            case STATE_HEADER: {
                if (str.startsWith("+++++++++++++++++++++++++++++++++++++++++++++++")) {
                    m_ParserState = ParserState.STATE_COMMENT;
                }
            }
            break;

            case STATE_COMMENT:
                m_LineStarted = m_CurrentLine;
                if (!dateParsed) {
                    s = ParseTimestampStr1(str);
                }
//                getThread(getDP());

                if ((m = regMsgStart.matcher(s)).find()) {
                    m_ParserState = ParserState.STATE_TMESSAGE_EVENT;
                    setSavedFilePos(getFilePos());
                    break;
                } else if ((m = regMsgRequest.matcher(s)).find() || (m = regMsgContactServerResponse.matcher(s)).find()) {
                    m_msgName = m.group(1);
                    m_ParserState = ParserState.STATE_TMESSAGE_ATTRIBUTES;
                    setSavedFilePos(getFilePos());
                    MessageType = MsgType.MSG_TLIB;
                    break;
                } else if ((m = regMsgRequest.matcher(s)).find() || (m = regMsgEventRegistered.matcher(s)).find()) {
                    m_msgName = m.group(1);
                    m_ParserState = ParserState.STATE_TMESSAGE_ATTRIBUTES;
                    setSavedFilePos(getFilePos());
                    MessageType = MsgType.MSG_TLIB;
                    break;
                } else if ((m = regExceptionStart.matcher(s)).find()) {
                    m_msgName = m.group(1);
                    m_msg1 = m.group(2);
                    m_MessageContents.add(s);
                    msg = new ExceptionMessage(
                            m_MessageContents,
                            m_msgName,
                            m_msg1
                    );
                    m_ParserState = ParserState.STATE_EXCEPTION;
                    setSavedFilePos(getFilePos());
                    break;
                } else if ((m = regAppURL.matcher(s)).find()) {
                    if (URL != null || app != null) {
                        Main.logger.error("URL/app not used; URL: [" + ((URL == null) ? "NULL" : URL) + "]" + " app: [" + ((app == null) ? "NULL" : app) + "] line:" + m_CurrentLine);
                    }
                    this.URL = m.group(1);
                    this.app = m.group(2);
                    //m_ParserState=STATE_CLUSTER;
                } else if ((m = regWWELogMessage.matcher(s)).find()) {
                    WWEDebugMsg entry = new WWEDebugMsg();

                    entry.setLevel(m.group(1)); // save IP
                    entry.setUserName(m.group(2)); // save IP
                    entry.setJSessionID(m.group(3));
                    entry.setIP(m.group(6));
                    String url = m.group(7);
                    String className = m.group(9);
                    String theMessage = m.group(10);
                    boolean stopParsingMsg = false;

                    Matcher m1;
                    if (url != null && !url.trim().isEmpty()) {
                        if (url.equalsIgnoreCase("/api/v2/me") && theMessage != null && (m1 = regConfigObjectRead.matcher(theMessage)).find()) {
                            m_MessageContents.add(s);
                            stopParsingMsg = true;
                            m_ParserState = ParserState.STATE_CONFIG_OBJECT;

                        } else {

                            entry.setURL(urlProcessor.processURL(url));
                            entry.setUserName(urlProcessor.getqueryValues(QUERY_userName));
                            entry.setUUID(urlProcessor.getpathValues(PATH_UUID));
                            entry.setDeviceID(urlProcessor.getpathValues(PATH_DeviceID));
                            entry.setIxnID(urlProcessor.getpathValues(PATH_IxnID));
                            entry.setUserID(urlProcessor.getpathValues(PATH_USERID));

                        }

                    }
//                    entry.setURL(url);

                    if (theMessage != null && !theMessage.trim().isEmpty()) {
                        Pair<String, String> rxReplace;
                        boolean needsCleanup = true;

                        if (className.endsWith("InteractionRepository")) {
                            rxReplace = Message.getRxReplace(theMessage, regMessageIxn1ID, 1, "<IxnID>");
                            if (rxReplace != null) {
                                theMessage = rxReplace.getValue();
                                entry.setIxnID(rxReplace.getKey());
                            }
                            if ((rxReplace = Message.getRxReplace(theMessage, regMessageUserID, 1, "<userID>")) != null) {
                                theMessage = rxReplace.getValue();
                                entry.setUserID(rxReplace.getKey());

                            }

                        } else if (className.endsWith("JoinChatSessionTaskV2") || className.endsWith("LeaveChatSessionTaskV2")) {
                            if ((rxReplace = Message.getRxReplace(theMessage, regMessageIxn2ID, 1, "<IxnID>")) != null) {
                                theMessage = rxReplace.getValue();
                                if (entry.getIxnID() == null) {
                                    entry.setIxnID(rxReplace.getKey());
                                }
                            }

                        } else if (className.endsWith("HttpSessionMonitor")) {
                            if ( //                                    (rxReplace = Message.getRxReplace(theMessage, regUserWithID, 1, "<userID>")) != null|| 
                                    (rxReplace = Message.getRxReplace(theMessage, regUserWithID1, 1, "<userID>")) != null) {
                                theMessage = rxReplace.getValue();
                                entry.setUserName(rxReplace.getKey());
                            }
                            rxReplace = Message.getRxReplace(theMessage, regMessageGSessionID, 1, "<sessionID>");
                            if (rxReplace != null) {
                                theMessage = rxReplace.getValue();
                                entry.setJSessionID(rxReplace.getKey());
                            } else if ((rxReplace = Message.getRxReplace(theMessage, regMessageWWESessionID, 1, "<sessionID>")) != null) {
                                theMessage = rxReplace.getValue();
                                entry.setJSessionID(rxReplace.getKey());

                            }
                        } else if (className.endsWith("IxnServerContextV2")) {
                            if ((rxReplace = Message.getRxReplace(theMessage, regUserID1, 1, "<userID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regUserWithID1, 1, "<userID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regUserID2, 1, "<userID>")) != null) {

                                theMessage = rxReplace.getValue();
                                entry.setUserID(rxReplace.getKey());
                                needsCleanup = false;
                            }

                            if ((rxReplace = Message.getRxReplace(theMessage, regMessageIxn3ID, 1, "<IxnID>")) != null) {
                                theMessage = rxReplace.getValue();
                                entry.setIxnID(rxReplace.getKey());
                            }
//                        } else if (className.endsWith("TelephonyUserMonitorV2")) {
//                            if ((rxReplace = Message.getRxReplace(theMessage, regUserID1, 1, "<userID>")) != null
//                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageUserNameID2, 1, "<userID>")) != null) {
//                                theMessage = rxReplace.getValue();
//                                entry.setUserID(rxReplace.getKey());
//                                needsCleanup=false;
//                            }

                        } else {
                            if ((rxReplace = Message.getRxReplace(theMessage, regMessageStartingHTTP, 1, "<url>")) != null) {
                                theMessage = rxReplace.getValue();
                            }
                            if ((rxReplace = Message.getRxReplace(theMessage, regMessageUUID, 1, "<UUID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageCalUUID1, 1, "<UUID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageCalUUID2, 1, "<UUID>")) != null) {
                                theMessage = rxReplace.getValue();
                                entry.setUUID(rxReplace.getKey());
                            }
                            if ((rxReplace = Message.getRxReplace(theMessage, regMessageUserNameID, 1, "<UserName>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageUserNameID1, 1, "<UserName>")) != null) {
                                theMessage = rxReplace.getValue();
                                entry.setUserName(rxReplace.getKey());
                            }

                            if ((rxReplace = Message.getRxReplace(theMessage, regUserWithID2, 1, "<UserID>")) != null) {
                                theMessage = rxReplace.getValue();
                                entry.setUserName(rxReplace.getKey());
                            }

                            if ((rxReplace = Message.getRxReplace(theMessage, regMessageUserID, 1, "<UserID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regUserWithID, 1, "<UserID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regUserID3ID, 1, "<userID>")) != null
                                    //                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageUserToken, 1, "<UserID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regUserRemovingSession, 1, "<UserID>")) != null //                                    || (rxReplace = Message.getRxReplace(theMessage, regUserSettings, 1, "<UserID>")) != null
                                    ) {
                                theMessage = rxReplace.getValue();
                                entry.setUserID(rxReplace.getKey());
                            }

                            rxReplace = Message.getRxReplace(theMessage, regMessageGSessionID, 1, "<GSessionID>");
                            if (rxReplace != null) {
                                theMessage = rxReplace.getValue();
                                entry.setJSessionID(rxReplace.getKey());
                            }
                            if (((rxReplace = Message.getRxReplace(theMessage, regMessageDeviceID, 1, "<DeviceID>")) != null)
                                    || ((rxReplace = Message.getRxReplace(theMessage, regMessageDevice1ID, 1, "<DeviceID>")) != null)) {
                                theMessage = rxReplace.getValue();
                                entry.setDeviceID(rxReplace.getKey());
                            }

                            if ((rxReplace = Message.getRxReplace(theMessage, regMessageBrowser1ID, 1, "<clientID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageBrowser1ID, 1, "<clientID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageBrowser2ID, 1, "<clientID>")) != null) {
                                theMessage = rxReplace.getValue();
                                entry.setClientID(rxReplace.getKey());
                            }

                            if ((rxReplace = Message.getRxReplace(theMessage, regMessageIxnID, 1, "<IxnID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageIxn2ID, 1, "<IxnID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageIxn4ID, 1, "<IxnID>")) != null
                                    || (rxReplace = Message.getRxReplace(theMessage, regMessageIxnChatID, 1, "<IxnID>")) != null) {
                                theMessage = rxReplace.getValue();
                                entry.setIxnID(rxReplace.getKey());
                            }
                        }
                        String theMessageToLog;
                        if (needsCleanup) {
                            theMessageToLog = msgCleaner.cleanString(theMessage);
                        } else {
                            theMessageToLog = theMessage;
                        }
                        if (theMessageToLog != null && theMessageToLog.length() > 120) {
                            entry.setMessageText(theMessageToLog.substring(0, 120));
                        } else {
                            entry.setMsgText(theMessageToLog);
                        }
                    }

//                    String theMessageToLog = theMessage;
                    entry.setClassName(className);

                    entry.setURIParam(m.group(8));

                    if (!stopParsingMsg && (className != null && !className.isEmpty())) {

                        Matcher m2;
                        if (className.endsWith("CloudWebBasicAuthenticationFilter")) {
                            Matcher m3;
                            if (theMessage.startsWith("Authentication success:")) {
                                msg = new WWEUCSConfigMsg(entry, "AuthSuccess");
                                SetStdFieldsAndAdd(msg);
                                msg = null;
                                return null;
                            } else if ((m3 = regWWEUCSConfigErrorUser.matcher(theMessage)).find()) {
                                msg = new WWEUCSConfigMsg(entry, "EventError");
                                m_ParserState = ParserState.STATE_CONFIG_ERROR;
                                setSavedFilePos(getFilePos());
                                m_MessageContents.add(s);
                                return null;

                            } else {
                                msg = new WWEUCSConfigMsg(entry, "MsgError");
                                SetStdFieldsAndAdd(msg);
                                msg = null;
                                return null;
                            }
                        } else if (className.endsWith("ChatServerContextV2")) {
                            if (theMessage.endsWith("Info:")) {
                                m_ParserState = ParserState.STATE_READ_INFO;
                                setSavedFilePos(getFilePos());
                                m_MessageContents.add(s);

                            }
                        } else if (className.endsWith("UcsInteractionOperationController") || className.endsWith("GetAcceptContentCompletionHandlerV2")
                                || className.endsWith("ChatOperationControllerV2") || className.endsWith("InteractionManageUserDataApiTaskV2")
                                || className.endsWith("GetUserMultimediaControllerV2")
                                || className.endsWith("InteractionManageUserDataTaskV2")
                                || className.endsWith("GetUcsHistoryCompletionHandler")) {
                            m_ParserState = ParserState.STATE_READ_INFO;
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                            stopParsingMsg = true;
                        } else if (className.contains(".BayeuxServerImpl.")) {
                            msg = new WWEBayeuxSMsg(entry);
                            ((WWEBayeuxSMsg) msg).setBayeuxId(getRx(theMessage, regBayeuxId, 1, null));
                            ((WWEBayeuxSMsg) msg).setLastConnected(getRx(theMessage, regBayeuxLastConnected, 1, null));
                            ((WWEBayeuxSMsg) msg).setAction(getRx(theMessage, regBayeuxAction, 1, null));
                            ((WWEBayeuxSMsg) msg).setChannel(getRx(theMessage, regBayeuxChannel, 1, null));
                            ((WWEBayeuxSMsg) msg).setMessageType(getRx(theMessage, regBayeuxMessageType, 1, null));
                            if (((WWEBayeuxSMsg) msg).getIxnID() == null) {
                                ((WWEBayeuxSMsg) msg).setIxnID(getRx(theMessage, regURIChatIxnID, 1, null));
                            }

                            if (theMessage.endsWith("}") || theMessage.endsWith("} null")) {
                                SetStdFieldsAndAdd(msg);
                            } else {
                                m_ParserState = ParserState.STATE_BAYEUX;
                                setSavedFilePos(getFilePos());
                                m_MessageContents.add(s);
                            }
                            return null;
                        } else if (className.endsWith("rtreporting.RTRContextAsync")) {
                            msg = new WWEStatServerMsg(entry);
                            m_ParserState = ParserState.STATE_STATSERVER;
                            setSavedFilePos(getFilePos());
                            m_MessageContents.add(s);
                        }

                        if (!stopParsingMsg && (theMessage != null && !theMessage.isEmpty())) {
//                            entry.setMessageText(theMessage);
                            if (regKVList.matcher(theMessage).find()) {
                                m_ParserState = ParserState.STATE_KVLIST;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                            } else if (theMessage.startsWith("Failed to process message:")) {
                                m_ParserState = ParserState.STATE_TMESSAGE_ATTRIBUTES;
                                setSavedFilePos(getFilePos());
                                m_MessageContents.add(s);
                                skipMessage = true;
                            } else if (regIndexListStart.matcher(theMessage).find()) {
                                m_ParserState = ParserState.STATE_IndexList;
                                setSavedFilePos(getFilePos());
                                skipMessage = true;
                            } else if ((m2 = regWWEUCSStart.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_UCSREQUEST;
                                setSavedFilePos(getFilePos());
                                m_MessageContents.add(s);

                                msg = new WWEUCSMsg(entry, m2.group(1));
                                return null; // do not insert debug message

                            } else if ((m2 = regWWEIxnServerRequestStart.matcher(theMessage)).find() || (m2 = regWWEIxnServerEventStart.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_IXNSERVER_REQUEST;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                                msg = new WWEIxnServerMsg(m2.group(1));
                                return null; // do not insert debug message

                            } else if ((m2 = regWWEUCSEvent.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_UCSRESPONSE;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                                msg = new WWEUCSMsg(entry, m2.group(1));
                                return null; // do not insert debug message

                            } else if ((m2 = regWWEUCSEventOther.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_UCSRESPONSEOTHER;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                                msg = new WWEUCSMsg(entry, m2.group(1));
                                return null; // do not insert debug message

                            } else if ((m2 = regWWEUCSHandlingEvent.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_UCSHANDLINGRESPONSE;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                                msg = new WWEUCSMsg(entry, m2.group(1));
                                return null; // do not insert debug message

//                            } else if ((m2 = regWWEUCSPrepareTask.matcher(theMessage)).find()) {
//                                m_ParserState = ParserState.STATE_UCSPREPARETASK;
//                                m_MessageContents.add(s);
//                                setSavedFilePos(getFilePos());
//                                return null; // do not insert debug message
                            } else if ((m2 = regGetRootCategories.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_GETROOTCATEGORIES;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                            } else if ((m2 = regInteractionAttributes.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_INTERACTION_ATTRIBUTES;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                            } else if ((m2 = regInteractionProperties.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_INTERACTION_PROPERTIES;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                            } else if ((m2 = regCreateChatContext.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_CREATECHATCONTEXT;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                            } else if ((m2 = regItems.matcher(theMessage)).find()) {
                                m_ParserState = ParserState.STATE_ITEMS;
                                m_MessageContents.add(s);
                                setSavedFilePos(getFilePos());
                            } else if ((m2 = regWWEReceivedInteractions.matcher(theMessage)).find()) {
                                String s1 = m2.group(1);
                                if (s1 != null) {
                                    if (s1.startsWith("KVList")) {
                                        m_ParserState = ParserState.STATE_RECEIVED_INTERACTION;
                                        m_MessageContents.add(s);
                                        setSavedFilePos(getFilePos());

                                    }
                                }
                            }

                        }

                    }
                    SetStdFieldsAndAdd(entry);
                    break;
                } //                else if ((m = regElasticSearch.matcher(s)).find()) {
                //                    fakeGenesysMsg(dp, this, TableType.MsgWWE, null, "Custom", "00000", "00000", m.group(1));
                //                } 
                else {
                    if (!ParseCustom) {
                        if (!s.contains("UpdateUserDevicesElasticSearchTask")
                                && !s.contains("at [Source: ") && s.length() > 3 && !s.substring(0, 3).toLowerCase().startsWith("-ap")) {
                            Main.logger.error("l:" + m_CurrentLine + "- unexpected string [" + str + "] in state " + m_ParserState);
                        }
                    }
                }
                break;

            case STATE_READ_INFO: {
                DateParsed dp = ParseFormatDate(str);
                if (dp == null) {
                    m_MessageContents.add(str);

                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }

            }
            break;

            case STATE_CONFIG_OBJECT:
            case STATE_ITEMS: {
                DateParsed dp = ParseFormatDate(str);
                if (dp == null) {
                    m_MessageContents.add(str);

                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }

            }
            break;

            case STATE_STATSERVER: {
                DateParsed dp = ParseFormatDate(str);
                if (dp == null) {
                    m_MessageContents.add(str);

                } else {
                    SetStdFieldsAndAdd(msg);
                    msg = null;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }
            }

            case STATE_INTERACTION_PROPERTIES: {
                DateParsed dp = ParseFormatDate(str);
                if (dp == null) {
                    m_MessageContents.add(str);

                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }

            }
            break;

            case STATE_KVLIST: {
                DateParsed dp1 = ParseFormatDate(str);
                if (dp1 == null) {
                    m_MessageContents.add(str);

                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return dp1;
                }

            }
            break;

            case STATE_INTERACTION_ATTRIBUTES: {
                DateParsed dp1 = ParseFormatDate(str);
                if (dp1 == null) {
                    m_MessageContents.add(str);

                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return dp1;
                }

            }
            break;

            case STATE_GETROOTCATEGORIES: {
                DateParsed dp1 = ParseFormatDate(str);
                if (dp1 == null) {
                    m_MessageContents.add(str);

                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return dp1;
                }

            }
            break;

            case STATE_CREATECHATCONTEXT: {
                DateParsed dp1 = ParseFormatDate(str);
                if (dp1 == null) {
                    m_MessageContents.add(str);

                } else {
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return dp1;
                }

            }
            break;

            case STATE_RECEIVED_INTERACTION: {
                if (regIxnContinue.matcher(str).find()) {
                    m_MessageContents.add(str);
                } else {
                    msg = null;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return null;
                }

            }
            break;

            case STATE_BAYEUX: {
                DateParsed dp1 = ParseFormatDate(str);
                if (dp1 == null) {
                    m_MessageContents.add(str);
                } else {
                    ((WWEBayeuxSMsg) msg).setAdditionalLines(m_MessageContents);
                    SetStdFieldsAndAdd(msg);
                    msg = null;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return dp1;
                }
            }
            break;

            case STATE_CONFIG_ERROR: {
                if (regWWEUCSConfigErrorContinue.matcher(str).find()) {
                    m_MessageContents.add(s.substring(1));
                } else {
                    SetStdFieldsAndAdd(msg);
                    msg = null;
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }
            }
            break;

            case STATE_UCSPREPARETASK:
            case STATE_EXCEPTION:
            case STATE_IXNSERVER_REQUEST:
            case STATE_UCSRESPONSE:
            case STATE_UCSRESPONSEOTHER:
            case STATE_UCSREQUEST:
            case STATE_UCSHANDLINGRESPONSE: {
                DateParsed dp1 = ParseFormatDate(str);
                if (dp1 == null) {
                    m_MessageContents.add(str);
                } else {
                    if (msg != null) {
                        msg.addMessageLines(m_MessageContents);
                        SetStdFieldsAndAdd(msg);
                        msg = null;
                    } else {
                        logger.error("msg is null st:" + m_ParserState + m_MessageContents.toString());
                    }
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return dp1;
                }
            }
            break;

//            case STATE_EXCEPTION: {
//                if (regExceptionContinue.matcher(str).find()) {
//                    m_MessageContents.add(s.substring(1));
//                } else {
//                    addException();
//                    m_ParserState = ParserState.STATE_COMMENT;
//                    m_MessageContents.clear();
//                    return str;
//                }
//            }
//            break;
            case STATE_IndexList: {
                if (regIndexList.matcher(str).find()) {
                    m_MessageContents.add(str);

                } else {
                    if (skipMessage) {
                        skipMessage = false;
                    } else {
                        logger.error("l:" + m_CurrentLine + " cannot create message");
                    }
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return str;
                }

            }
            break;

            case STATE_TMESSAGE_ATTRIBUTES: {
                DateParsed dp1 = ParseFormatDate(str);
                if (dp1 == null) {
                    m_MessageContents.add(str);

                } else {
                    if (skipMessage) {
                        skipMessage = false;
                    } else {
                        addTLibMessage();
                    }
                    m_ParserState = ParserState.STATE_COMMENT;
                    m_MessageContents.clear();
                    return dp1;
                }

            }
            break;

            case STATE_TMESSAGE_EVENT:
                if ((m = regTMessageStart.matcher(str)).find()) {
                    m_msgName = m.group(1);
                    m_ParserState = ParserState.STATE_TMESSAGE_ATTRIBUTES;
                } else {
                    Main.logger.error("l:" + m_CurrentLine + "- unexpected string [" + str + "] in state " + m_ParserState);
                    m_ParserState = ParserState.STATE_COMMENT;
                    return str;
                }
                break;

        }
        return null;
    }

    private void addTLibMessage() throws Exception {
        WWEMessage msg = new WWEMessage(m_msgName, m_TserverSRC, m_MessageContents, isParseTimeDiff());
        SetStdFieldsAndAdd(msg);
    }

    private void addException() {
        Matcher m;
        Main.logger.trace("addException");
        ExceptionMessage msg = new ExceptionMessage(
                m_MessageContents,
                m_msgName,
                m_msg1
        );
        SetStdFieldsAndAdd(msg);

    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
        m_tables.put(TableType.WWEMessage, new WWEDebugMessageTable(Main.getMain().getM_accessor(), TableType.WWEMessage));
        m_tables.put(TableType.WWEUCSMessage, new WWEUCSTable(Main.getMain().getM_accessor(), TableType.WWEUCSMessage));
        m_tables.put(TableType.WWEUCSConfigMessage, new WWEUCSConfigTable(Main.getMain().getM_accessor(), TableType.WWEUCSConfigMessage));
        m_tables.put(TableType.WWEIxnMessage, new WWEIxnTable(Main.getMain().getM_accessor(), TableType.WWEIxnMessage));
        m_tables.put(TableType.WWEBayeuxMessage, new WWEBayeuxSMsgTable(Main.getMain().getM_accessor(), TableType.WWEBayeuxMessage));
        m_tables.put(TableType.WWEStatServerMessage, new WWEWWEStatServerMsgTable(Main.getMain().getM_accessor(), TableType.WWEStatServerMessage));

    }

    enum ParserState {
        STATE_INIT,
        STATE_HEADER,
        STATE_TMESSAGE_REQUEST,
        STATE_ORSMESSAGE,
        STATE_COMMENT,
        STATE_TMESSAGE_EVENT,
        STATE_CLUSTER,
        STATE_HTTPIN,
        STATE_HTTPHANDLEREQUEST,
        STATE_ORSUS,
        STATE_TMESSAGE_ATTRIBUTES,
        STATE_EXCEPTION,
        STATE_IndexList,
        STATE_UCSREQUEST,
        STATE_UCSPREPARETASK,
        STATE_UCSRESPONSE,
        STATE_CONFIG_ERROR,
        STATE_UCSHANDLINGRESPONSE,
        STATE_UCSRESPONSEOTHER,
        STATE_IXNSERVER_REQUEST,
        STATE_BAYEUX,
        STATE_RECEIVED_INTERACTION,
        STATE_CREATECHATCONTEXT,
        STATE_READ_INFO,
        STATE_GETROOTCATEGORIES,
        STATE_INTERACTION_ATTRIBUTES,
        STATE_INTERACTION_PROPERTIES,
        STATE_KVLIST,
        STATE_ITEMS,
        STATE_CONFIG_OBJECT, STATE_STATSERVER
    }

    enum MsgType {
        MSG_UNKNOWN,
        MSG_MM_OUT,
        MSG_MM_IN,
        MSG_TLIB
    }

//<editor-fold defaultstate="collapsed" desc="WWEStatServerMsg">
    private static final Pattern ptStatStringValue = Pattern.compile("^\\s*'STRING_VALUE'.+= \"(.+)\"$");

    private class WWEStatServerMsg extends Message {

        private String ip;
        private String jSessionID;
        private String httpCode;
        private String date;
        private String method;
        private String key;
        private String url;

        private String deviceID;
        private String userName;
        private String level;
        private String className;
        private String msgText;
        private String param1;
        private String param2;
        private String userID;
        private String browserClient;
        private String sessionID;

        public String getParam2() {
            return param2;
        }

        /**
         * Get the value of deviceID
         *
         * @return the value of deviceID
         */
        public String getDeviceID() {
            return deviceID;
        }

        /**
         * Set the value of deviceID
         *
         * @param deviceID new value of deviceID
         */
        public void setDeviceID(String deviceID) {
            checkDiffer(this.deviceID, deviceID, "deviceID", m_CurrentLine);
            this.deviceID = deviceID;
        }

        private String UUID;

        /**
         * Get the value of UUID
         *
         * @return the value of UUID
         */
        public String getUUID() {
            return UUID;
        }

        /**
         * Set the value of UUID
         *
         * @param UUID new value of UUID
         */
        public void setUUID(String UUID) {
            checkDiffer(this.UUID, UUID, "UUID", m_CurrentLine);

            this.UUID = UUID;
        }

        private String ixnID;

        /**
         * Get the value of ixnID
         *
         * @return the value of ixnID
         */
        public String getIxnID() {
            return ixnID;
        }

        /**
         * Set the value of ixnID
         *
         * @param ixnID new value of ixnID
         */
        public void setIxnID(String ixnID) {
            checkDiffer(this.ixnID, ixnID, "ixnID", m_CurrentLine);

            this.ixnID = ixnID;
        }

        private WWEStatServerMsg(TableType t) {
            super(t);
        }

        private WWEStatServerMsg() {
            this(TableType.WWEStatServerMessage);
        }

        private WWEStatServerMsg(WWEDebugMsg orig) {
            this();
            this.setClassName(orig.getClassName());
            this.setJSessionID(orig.getjSessionID());
            this.setLevel(orig.getLevel());
            this.setUserName(orig.getUserName());
            this.setIP(orig.getIp());
            this.setURL(orig.getUrl());
            this.setMessageText(orig.getMsgText());
            this.setUUID(orig.getUUID());
            this.setIxnID(orig.getIxnID());
            this.setDeviceID(orig.getDeviceID());
        }

        public String getHttpCode() {

            return httpCode;
        }

        public String getMethod() {
            return method;
        }

        private void setMethod(String s) {
            method = s;
        }

        private void setHttpCode(String s) {
            httpCode = s;
        }

        private void setDate(String s) {
            date = s;
        }

        private void setIP(String s) {
            ip = s;
        }

        public String getIp() {
            return ip;
        }

        public String getjSessionID() {
            return jSessionID;
        }

        public String getKey() {
            return key;
        }

        public String getUrl() {
            return url;
        }

        public String getUserName() {
            return userName;
        }

        private void setUserKey(String s) {

            key = s;
        }

        private void setJSessionID(String s) {
            checkDiffer(this.jSessionID, s, "jSessionID", m_CurrentLine);

            jSessionID = s;
        }

        private void setURL(String group) {
            if (group != null && !group.isEmpty()) {
                try {
                    url = URLDecoder.decode(group, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(ApacheWebLogsParser.class.getName()).log(Level.SEVERE, null, ex);
                    url = group;
                }
            }
        }

        private void setUserName(String get) {
            if (userName != null && !userName.isEmpty()) {
                setParam1(get);
            } else {
                checkDiffer(this.userName, get, "userName", m_CurrentLine);
                this.userName = get;
            }

        }

        private void setLevel(String group) {
            this.level = group;

        }

        private void setClassName(String group) {
            this.className = group;

        }

        public void setMsgText(String msgText) {
            this.msgText = msgText;
        }

        public void setParam1(String param1) {
            checkDiffer(this.param1, param1, "param1", m_CurrentLine);

        }

        public void setParam2(String param2) {
            this.param2 = param2;
        }

        private void setMessageText(String group) {
            this.msgText = group;
        }

        public String getLevel() {
            return level;
        }

        public String getClassName() {
            return className;
        }

        public String getMsgText() {
            return msgText;
        }

        private void setURIParam(String group) {
            this.param1 = group;
        }

        public String getParam1() {
            return FindByRx(ptStatStringValue, 1, null);
        }

        private void setUserID(String key) {
            checkDiffer(this.userID, key, "userID", m_CurrentLine);
            this.userID = key;
        }

        public String getUserID() {
            return userID;
        }

        private void setClientID(String key) {
            this.browserClient = key;
        }

        public String getBrowserClient() {
            return browserClient;
        }

        private void setSessionID(String key) {
            checkDiffer(this.sessionID, key, "sessionID", m_CurrentLine);

            this.sessionID = key;
        }

        public String getSessionID() {
            return sessionID;
        }

    }

    private class WWEWWEStatServerMsgTable extends DBTable {

        public WWEWWEStatServerMsgTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("IPID");
            addIndex("JSessionIDID");
            addIndex("urlID");
            addIndex("LevelID");
            addIndex("userNameID");
            addIndex("classNameID");
            addIndex("msgTextID");
            addIndex("param1ID");
            addIndex("param2ID");
            addIndex("UUIDID");
            addIndex("IxnIDID");
            addIndex("DeviceIDID");
            addIndex("UserIDID");
            addIndex("browserClientID");
            addIndex("sessionID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",IPID int"
                    + ",JSessionIDID int"
                    + ",urlID int"
                    + ",LevelID int"
                    + ",classNameID int"
                    + ",msgTextID int"
                    + ",userNameID int"
                    + ",param1ID int"
                    + ",UUIDID int"
                    + ",IxnIDID int"
                    + ",DeviceIDID int"
                    + ",param2ID int"
                    + ",UserIDID int"
                    + ",browserClientID int"
                    + ",sessionID int"
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
            WWEStatServerMsg rec = (WWEStatServerMsg) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.IP, rec.getIp()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.JSessionID, rec.getjSessionID()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.HTTPURL, rec.getUrl()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.MSGLEVEL, rec.getLevel()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.JAVA_CLASS, rec.getClassName()));
                setFieldInt(stmt, 11, Main.getRef(ReferenceType.Misc, rec.getMsgText()));
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.Agent, rec.getUserName()));
                setFieldInt(stmt, 13, Main.getRef(ReferenceType.MiscParam, rec.getParam1()));
                setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, rec.getUUID()));
                setFieldInt(stmt, 15, Main.getRef(ReferenceType.IxnID, rec.getIxnID()));
                setFieldInt(stmt, 16, Main.getRef(ReferenceType.GWSDeviceID, rec.getDeviceID()));
                setFieldInt(stmt, 17, Main.getRef(ReferenceType.MiscParam, rec.getParam2()));
                setFieldInt(stmt, 18, Main.getRef(ReferenceType.WWEUserID, rec.getUserID()));
                setFieldInt(stmt, 19, Main.getRef(ReferenceType.WWEBrowserClient, rec.getBrowserClient()));
                setFieldInt(stmt, 20, Main.getRef(ReferenceType.WWEBrowserSession, rec.getSessionID()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }
//</editor-fold> 

//<editor-fold defaultstate="collapsed" desc="WWEDebugMsg">
    private class WWEDebugMsg extends Message {

        private String ip;
        private String jSessionID;
        private String httpCode;
        private String date;
        private String method;
        private String key;
        private String url;

        private String deviceID;
        private String userName;
        private String level;
        private String className;
        private String msgText;
        private String param1;
        private String param2;
        private String userID;
        private String browserClient;
        private String sessionID;

        public String getParam2() {
            return param2;
        }

        /**
         * Get the value of deviceID
         *
         * @return the value of deviceID
         */
        public String getDeviceID() {
            return deviceID;
        }

        /**
         * Set the value of deviceID
         *
         * @param deviceID new value of deviceID
         */
        public void setDeviceID(String deviceID) {
            checkDiffer(this.deviceID, deviceID, "deviceID", m_CurrentLine);
            this.deviceID = deviceID;
        }

        private String UUID;

        /**
         * Get the value of UUID
         *
         * @return the value of UUID
         */
        public String getUUID() {
            return UUID;
        }

        /**
         * Set the value of UUID
         *
         * @param UUID new value of UUID
         */
        public void setUUID(String UUID) {
            checkDiffer(this.UUID, UUID, "UUID", m_CurrentLine);

            this.UUID = UUID;
        }

        private String ixnID;

        /**
         * Get the value of ixnID
         *
         * @return the value of ixnID
         */
        public String getIxnID() {
            return ixnID;
        }

        /**
         * Set the value of ixnID
         *
         * @param ixnID new value of ixnID
         */
        public void setIxnID(String ixnID) {
            checkDiffer(this.ixnID, ixnID, "ixnID", m_CurrentLine);

            this.ixnID = ixnID;
        }

        private WWEDebugMsg(TableType t) {
            super(t);
        }

        private WWEDebugMsg() {
            this(TableType.WWEMessage);
        }

        private WWEDebugMsg(TableType t, WWEDebugMsg orig) {
            this(t);
            this.setClassName(orig.getClassName());
            this.setJSessionID(orig.getjSessionID());
            this.setLevel(orig.getLevel());
            this.setUserName(orig.getUserName());
            this.setIP(orig.getIp());
            this.setURL(orig.getUrl());
            this.setMessageText(orig.getMsgText());
            this.setUUID(orig.getUUID());
            this.setIxnID(orig.getIxnID());
            this.setDeviceID(orig.getDeviceID());
        }

        public String getHttpCode() {

            return httpCode;
        }

        public String getMethod() {
            return method;
        }

        private void setMethod(String s) {
            method = s;
        }

        private void setHttpCode(String s) {
            httpCode = s;
        }

        private void setDate(String s) {
            date = s;
        }

        private void setIP(String s) {
            if (s != null) {
                ip = s.trim();
            }
        }

        public String getIp() {
            return ip;
        }

        public String getjSessionID() {
            return jSessionID;
        }

        public String getKey() {
            return key;
        }

        public String getUrl() {
            return url;
        }

        public String getUserName() {
            return userName;
        }

        private void setUserKey(String s) {

            key = s;
        }

        private void setJSessionID(String s) {
            checkDiffer(this.jSessionID, s, "jSessionID", m_CurrentLine);

            jSessionID = s;
        }

        private void setURL(String group) {
            if (group != null && !group.isEmpty()) {
                try {
                    url = URLDecoder.decode(group, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(ApacheWebLogsParser.class.getName()).log(Level.SEVERE, null, ex);
                    url = group;
                }
            }
        }

        private void setUserName(String get) {
            if (userName != null && !userName.isEmpty()) {
                setParam1(get);
            } else {
                checkDiffer(this.userName, get, "userName", m_CurrentLine);
                this.userName = get;
            }

        }

        private void setLevel(String group) {
            this.level = group;

        }

        private void setClassName(String group) {
            this.className = group;

        }

        public void setMsgText(String msgText) {
            this.msgText = msgText;
        }

        public void setParam1(String param1) {
            checkDiffer(this.param1, param1, "param1", m_CurrentLine);

        }

        public void setParam2(String param2) {
            this.param2 = param2;
        }

        private void setMessageText(String group) {
            this.msgText = group;
        }

        public String getLevel() {
            return level;
        }

        public String getClassName() {
            return className;
        }

        public String getMsgText() {
            return msgText;
        }

        private void setURIParam(String group) {
            this.param1 = group;
        }

        public String getParam1() {
            return param1;
        }

        private void setUserID(String key) {
            checkDiffer(this.userID, key, "userID", m_CurrentLine);
            this.userID = key;
        }

        public String getUserID() {
            return userID;
        }

        private void setClientID(String key) {
            this.browserClient = key;
        }

        public String getBrowserClient() {
            return browserClient;
        }

        private void setSessionID(String key) {
            checkDiffer(this.sessionID, key, "sessionID", m_CurrentLine);

            this.sessionID = key;
        }

        public String getSessionID() {
            return sessionID;
        }

    }

    private class WWEDebugMessageTable extends DBTable {

        public WWEDebugMessageTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("IPID");
            addIndex("JSessionIDID");
            addIndex("urlID");
            addIndex("LevelID");
            addIndex("userNameID");
            addIndex("classNameID");
            addIndex("msgTextID");
            addIndex("param1ID");
            addIndex("param2ID");
            addIndex("UUIDID");
            addIndex("IxnIDID");
            addIndex("DeviceIDID");
            addIndex("UserIDID");
            addIndex("browserClientID");
            addIndex("sessionID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",IPID int"
                    + ",JSessionIDID int"
                    + ",urlID int"
                    + ",LevelID int"
                    + ",classNameID int"
                    + ",msgTextID int"
                    + ",userNameID int"
                    + ",param1ID int"
                    + ",UUIDID int"
                    + ",IxnIDID int"
                    + ",DeviceIDID int"
                    + ",param2ID int"
                    + ",UserIDID int"
                    + ",browserClientID int"
                    + ",sessionID int"
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
            WWEDebugMsg rec = (WWEDebugMsg) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.IP, rec.getIp()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.JSessionID, rec.getjSessionID()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.HTTPURL, rec.getUrl()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.MSGLEVEL, rec.getLevel()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.JAVA_CLASS, rec.getClassName()));
                setFieldInt(stmt, 11, Main.getRef(ReferenceType.Misc, rec.getMsgText()));
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.Agent, rec.getUserName()));
                setFieldInt(stmt, 13, Main.getRef(ReferenceType.MiscParam, rec.getParam1()));
                setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, rec.getUUID()));
                setFieldInt(stmt, 15, Main.getRef(ReferenceType.IxnID, rec.getIxnID()));
                setFieldInt(stmt, 16, Main.getRef(ReferenceType.GWSDeviceID, rec.getDeviceID()));
                setFieldInt(stmt, 17, Main.getRef(ReferenceType.MiscParam, rec.getParam2()));
                setFieldInt(stmt, 18, Main.getRef(ReferenceType.WWEUserID, rec.getUserID()));
                setFieldInt(stmt, 19, Main.getRef(ReferenceType.WWEBrowserClient, rec.getBrowserClient()));
                setFieldInt(stmt, 20, Main.getRef(ReferenceType.WWEBrowserSession, rec.getSessionID()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="WWEBayeuxSMsg">
    private static final Pattern regBayeuxNotificationType = Pattern.compile("notificationType=([^,\\}]+)");
//    private static final Pattern regBayeuxMessageType = Pattern.compile("messageType=([^,\\}]+)");
    private static final Pattern regBayeuxCallUUID = Pattern.compile("CallUUID=([^,]+),", Pattern.CASE_INSENSITIVE);

    private static final Pattern regBayeuxDeviceURI = Pattern.compile("deviceUri=[\\S,]+/api/v2/devices/([^,]+),");

    private static final Pattern regMessageUUID = Pattern.compile("Found call \\[(\\p{Alnum}+)\\]");

    private static final Pattern regMessageStartingHTTP = Pattern.compile("^https?://(\\S+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern regMessageUserID = Pattern.compile("(?:user(?:Id)?|agent):? \\[?(\\p{Alnum}{32})\\]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern regMessageUserNameID = Pattern.compile("(?:(?<!No )[Ll]ogin *[Cc]ode|UserName:) \\[?((?!for)[^\\]\\s]+)\\]?");
    private static final Pattern regMessageUserNameID1 = Pattern.compile(" agentId=([^\\s,]+), ");
    private static final Pattern regMessageUserNameID2 = Pattern.compile(" [Uu]ser \\[([^\\]]+)\\]");
    private static final Pattern regMessageDeviceID = Pattern.compile("device .*(?:\\[(?:Id=)|DeviceV2\\(id=)([\\w\\-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern regMessageDevice1ID = Pattern.compile("[Dd]evice \\[*([\\w\\-]+)\\]*");

    private static final Pattern regMessageCalUUID1 = Pattern.compile("Call \\[(?:Id=)?([\\w\\-]{32})\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern regMessageCalUUID2 = Pattern.compile("call recording details \\[([\\w\\-]{32})\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern regMessageGSessionID = Pattern.compile(" session \\[*(\\p{Alnum}{20,}+)\\]*", Pattern.CASE_INSENSITIVE);
//    private static final Pattern regMessageGSessionMonitorID = Pattern.compile("^Session \\[(\\p{Alnum}+)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern regUserWithID = Pattern.compile("user(?: with ID| settings)? \\[?([^\\s\\]]{12,}+)\\]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern regUserWithID1 = Pattern.compile("for (?:user|agent) \\[([^\\]]+)\\]");
    private static final Pattern regUserWithID2 = Pattern.compile("user \\[([^\\s\\]]+)\\] not", Pattern.CASE_INSENSITIVE);
    private static final Pattern regMessageWWESessionID = Pattern.compile("Session \\[(\\p{Alnum}+)\\] removed", Pattern.CASE_INSENSITIVE);

    private static final Pattern regUserID1 = Pattern.compile("userId=([^\\s,\\>\\<]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern regUserID2 = Pattern.compile("(?:for|with) userId ([^\\s,\\>\\<]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern regUserID3ID = Pattern.compile("sessions from (\\w+)");
    private static final Pattern regUserRemovingSession = Pattern.compile("Removing session (\\w+)");
//    private static final Pattern regUserSettings = Pattern.compile("user settings \\[(\\p{Alnum}+)\\]");

    private static final Pattern regMessageBrowser1ID = Pattern.compile("browserId ([\\w\\-]+)");
    private static final Pattern regMessageBrowser2ID = Pattern.compile("clientId=([\\w\\-]+),");
    private static final Pattern regMessageIxnID = Pattern.compile("﻿interaction with identity \\[([\\w\\-]+)\\]");
    private static final Pattern regMessageIxn1ID = Pattern.compile("interactionId=(\\w+)");
    private static final Pattern regMessageIxn2ID = Pattern.compile("(?:interactionid|chat id) '*\\{*(\\w+)\\}*'*", Pattern.CASE_INSENSITIVE);
    private static final Pattern regMessageIxn3ID = Pattern.compile("ixnId=(\\w+)");
    private static final Pattern regMessageIxn4ID = Pattern.compile("(?:interactionId|interaction with identity) \\[(\\w+)\\]", Pattern.CASE_INSENSITIVE);
//    private static final Pattern regMessageUserToken = Pattern.compile("userToken=(\\w+)");
    private static final Pattern regMessageIxnChatID = Pattern.compile("/(?:chats|calls)/(\\w+)");

    private static class WWEMessageCleanTool {

        static interface IRXFoundProc {

            abstract void foundFun(String newLine, String replacedString);
        }

        private class ReplaceLiteral extends ICleanString {

            private final String searchString;
            private final String replaceString;

            public ReplaceLiteral(String searchString, String replaceString, boolean isFinal) {
                super(isFinal);
                this.searchString = searchString;
                this.replaceString = replaceString;
            }

            @Override
            public Pair<String, String> cleanString1(String s) {
                if (s != null && s.contains(searchString)) {
                    return new Pair(replaceString, searchString);
                } else {
                    return null;
                }

            }

        }

        private class ReplaceRx extends ICleanString {

            private final Pattern p;
            private final int idx;
            private final String replaceString;

            public ReplaceRx(Pattern p, int idx, String replaceString) {
                this(p, idx, replaceString, false);
            }

            public ReplaceRx(Pattern p, int idx, String replaceString, boolean isFinal) {
                super(isFinal);
                this.p = p;
                this.idx = idx;
                this.replaceString = replaceString;

            }

            @Override
            public Pair<String, String> cleanString1(String s) {
                return Message.getRxReplace(s, p, idx, replaceString);
            }

            private boolean cleanString(String s, IRXFoundProc pr) {
                Pair<String, String> rxReplace;
                rxReplace = Message.getRxReplace(s, p, idx, replaceString);
                if (rxReplace != null) {
                    pr.foundFun(rxReplace.getValue(), rxReplace.getKey());
                    return true;
                } else {
                    return false;
                }
            }
        }

        private abstract class ICleanString {

            protected final boolean stopIfFound;

            public ICleanString(boolean b) {
                stopIfFound = b;
            }

            abstract public Pair<String, String> cleanString1(String s);

            public boolean isStopIfFound() {
                return stopIfFound;
            }

        }

        private static ArrayList<ICleanString> rxs = new ArrayList<>();

        public WWEMessageCleanTool() {
            rxs = new ArrayList<>();
//            rxs.add(new ReplaceRx(Pattern.compile("(.+﻿VoiceUcsInteraction was updated with values .+)", Pattern.CASE_INSENSITIVE), 1, "<VoiceUcsInteraction was updated with ..>", true));
//            rxs.add(new ReplaceRx(Pattern.compile("(.+﻿Updating of telephonyProcessingContext .+)", Pattern.CASE_INSENSITIVE), 1, "<Updating of telephonyProcessingContext>", true));
//            rxs.add(new ReplaceRx(Pattern.compile("(.+ UpdateRequest: index .+)", Pattern.CASE_INSENSITIVE), 1, "<UpdateRequest: index>", true));
            rxs.add(new ReplaceLiteral("﻿called with baseUCSInteractionResource", "<https://*ad/v1﻿called with baseUCSInteractionResource>", true));
            rxs.add(new ReplaceLiteral("performUCSContactOperation called with baseUCSContactsResourceV2", "﻿performUCSContactOperation called with baseUCSContactsResourceV2", true));
//            rxs.add(new ReplaceLiteral("VoiceUcsInteraction was updated with values", "<VoiceUcsInteraction was updated with values>", true));
            rxs.add(new ReplaceLiteral(" performInteractionOperation called with baseUCSInteractionResource", "<performInteractionOperation called with baseUCSInteractionResource>", true));
            rxs.add(new ReplaceLiteral(" Execution completed with baseUCSInteractionResource", "<Execution completed with baseUCSInteractionResource>", true));
            rxs.add(new ReplaceLiteral("Read all nodes:", "<Read all nodes:>", true));
            rxs.add(new ReplaceLiteral("Updating setting for object", "<Updating setting for object>", true));
            rxs.add(new ReplaceLiteral("UpdateRequest: index", "<﻿UpdateRequest: index>", true));
            rxs.add(new ReplaceLiteral("Skipped update of context", "<﻿Skipped update of context>", true));
            rxs.add(new ReplaceLiteral("Read object", "<Read object>", true));

            rxs.add(new ReplaceLiteral("Login agent: deviceId", "<Login agent: deviceId>", true));
            rxs.add(new ReplaceLiteral(" setting CustomContact", "<setting CustomContact>", true));
            rxs.add(new ReplaceLiteral(" Requesting extended user settings", "<Requesting extended user settings>", true));
            rxs.add(new ReplaceLiteral(" Starting registration for device", "Starting registration for device", true));
            rxs.add(new ReplaceLiteral(" Result device context", "<Result device contex>", true));
            rxs.add(new ReplaceLiteral(" Creating setting for object", "<Creating setting for object>", true));
            rxs.add(new ReplaceLiteral(": TelephonyUserMonitorV2.UserSessionInfo", "<Creating setting for object>", true));

            rxs.add(new ReplaceRx(Pattern.compile("^(UpdateRequest: index .+)$", Pattern.CASE_INSENSITIVE), 1, "<UpdateRequest: index>", true));
            rxs.add(new ReplaceRx(Pattern.compile("^(Unregistering from TServer device .+)$", Pattern.CASE_INSENSITIVE), 1, "<Unregistering from TServer device>", true));
            rxs.add(new ReplaceRx(Pattern.compile("^(Unregistering DN .+)$", Pattern.CASE_INSENSITIVE), 1, "<Unregistering DN>", true));
            rxs.add(new ReplaceRx(Pattern.compile("^Successfully saved heartbeat qualifier(.+)"), 1, "<...>", true));
            rxs.add(new ReplaceRx(Pattern.compile("\\$Servlet3SaveToSessionRequestWrapper@(\\w+)", Pattern.CASE_INSENSITIVE), 1, "<wrapperNo>"));
            rxs.add(new ReplaceRx(Pattern.compile("last connect (\\d+) ms ago"), 1, "<secs>"));
            rxs.add(new ReplaceRx(Pattern.compile("/meta/(?:dis)?connect, id=(\\d+)"), 1, "<id>"));
//            rxs.add(new ReplaceRx(Pattern.compile("client ([\\w\\-]+) "), 1, "<clientID>"));
//            rxs.add(new ReplaceRx(Pattern.compile("^\\< client ([\\w\\-]+) "), 1, "<clientId>"));
//            rxs.add(new ReplaceRx(Pattern.compile("clientId=(\\w+),"), 1, "<clientID>"));
//            rxs.add(new ReplaceRx(Pattern.compile("Session \\[(\\w+)\\]"), 1, "<session>"));
            rxs.add(new ReplaceRx(Pattern.compile("contact center \\[([\\w\\-]+)\\]"), 1, "<ccID>"));
            rxs.add(new ReplaceRx(Pattern.compile("referenceId \\[([\\w\\-]+)\\]", Pattern.CASE_INSENSITIVE), 1, "<refID>"));
            rxs.add(new ReplaceRx(Pattern.compile("[\\s\\[]Number=?\\[?([\\w\\-]+)\\]?", Pattern.CASE_INSENSITIVE), 1, "<number>"));
            rxs.add(new ReplaceRx(Pattern.compile("session\\.SessionInformation@(\\w+)"), 1, "<SessionInstance>"));
            rxs.add(new ReplaceRx(Pattern.compile(" DN \\[?([\\w\\-]+)\\]?"), 1, "<DN>"));
            rxs.add(new ReplaceRx(Pattern.compile(" Employee ID: (.+)\\]"), 1, "<Employee ID>"));
            rxs.add(new ReplaceRx(Pattern.compile(" Place(?:(?: name| by name))? \\[?([^\\],]+)\\]?", Pattern.CASE_INSENSITIVE), 1, "<Place>"));
            rxs.add(new ReplaceRx(Pattern.compile(" took \\[(\\d+)ms\\]"), 1, "<xx>"));
            rxs.add(new ReplaceRx(Pattern.compile("Object \\[*(\\S+)\\]*"), 1, "<obj>"));
            rxs.add(new ReplaceRx(Pattern.compile("referenceid=(\\w+)", Pattern.CASE_INSENSITIVE), 1, "<refID>"));
            rxs.add(new ReplaceRx(Pattern.compile("userToken=\\[*(\\w+)\\]*"), 1, "<userToken>"));
            rxs.add(new ReplaceRx(Pattern.compile("destination \\[*([^\\s\\]]+)\\]*"), 1, "<DN>"));
            rxs.add(new ReplaceRx(Pattern.compile("DeviceRuntimeContext\\(dn=(\\S+)"), 1, "<DN>"));
            rxs.add(new ReplaceRx(Pattern.compile("Place name \\[*(\\S+)\\]*"), 1, "<Place>"));
            rxs.add(new ReplaceRx(Pattern.compile("[Cc]all \\[(\\S+)\\]"), 1, "<ConnID>"));
            rxs.add(new ReplaceRx(Pattern.compile("(?:with parent|relatedConnId|related connId) \\[(\\S+)\\]"), 1, "<ConnID>"));

            rxs.add(new ReplaceRx(Pattern.compile("java.lang.Object;([^,]+),"), 1, "<SomeID>"));
            rxs.add(new ReplaceRx(Pattern.compile("FirstName: (.+)"), 1, "<The guy>"));
            rxs.add(new ReplaceRx(Pattern.compile("\\[DBID=([^\\]]+)\\]"), 1, "<DBID>"));
            rxs.add(new ReplaceRx(Pattern.compile("Attempting to start contact center.+\\(([^\\)]+)\\)$"), 1, "<UserName>"));
            rxs.add(new ReplaceRx(Pattern.compile("timestampMicros=(\\d+),"), 1, "<timestampMicros>"));
            rxs.add(new ReplaceRx(Pattern.compile("^Starting registration for device \\[Id=([^,]+),"), 1, "<DeviceID>"));
            rxs.add(new ReplaceRx(Pattern.compile("AgentStateDescriptorV2\\(id=([^,\\s]+)"), 1, "<AgentStateDescriptorID>"));
            rxs.add(new ReplaceRx(Pattern.compile("contact \\[(\\w+)\\]"), 1, "<contactID>"));
            rxs.add(new ReplaceRx(Pattern.compile("contact \\[(\\w+)\\]"), 1, "<contactID>"));
            rxs.add(new ReplaceRx(Pattern.compile("with referenceId (\\d+)"), 1, "<refID>"));
            rxs.add(new ReplaceRx(Pattern.compile("found [([^\\]]+)] documents"), 1, "<num>"));
            rxs.add(new ReplaceRx(Pattern.compile("foundDocuments=([^,\\s]+),"), 1, "<num>"));
            rxs.add(new ReplaceRx(Pattern.compile("sessions for (\\w+)"), 1, "<session>"));
            rxs.add(new ReplaceRx(Pattern.compile(" client ([\\w\\-]+)"), 1, "<client>"));
            rxs.add(new ReplaceRx(Pattern.compile("userToken=(\\w+)"), 1, "<userID?>"));
            rxs.add(new ReplaceRx(Pattern.compile("referenceId (\\d+)"), 1, "<refID>"));
            rxs.add(new ReplaceRx(Pattern.compile("universalcontactserver\\.tasks\\.UpdateUcsInteractionTaskV2@(\\w+)"), 1, "<instanceID>"));
            rxs.add(new ReplaceRx(Pattern.compile(", index=(\\d+),"), 1, "<ID>"));
            rxs.add(new ReplaceRx(Pattern.compile("﻿AgentState operation name \\[([^\\]]+)\\]"), 1, "<ACW>"));
            rxs.add(new ReplaceRx(Pattern.compile("Extracted ReasonCode \\[([^\\]]+)\\]"), 1, "<ACW>"));
            rxs.add(new ReplaceRx(Pattern.compile("﻿operationName=([^\\],]+)"), 1, "<ACW>"));
            rxs.add(new ReplaceRx(Pattern.compile("Found \\[(\\d+)\\]"), 1, "<NUM>"));

        }

        public String cleanString(String s) {
            String ret = s;
            for (ICleanString rx : rxs) {
                Pair<String, String> ret1 = rx.cleanString1(ret);
                if (ret1 != null) {
                    ret = ret1.getValue();
                    if (rx.isStopIfFound()) {
                        break;
                    }
                }
            }

            return ret;
        }

    }
    private static final WWEMessageCleanTool msgCleaner = new WWEMessageCleanTool();

//05/30/2019 13:35:39.159 DEBUG [graham.saludares@outreach.airbnb.com] [Ca6138neMXSzsR6ccjA7eA1ekz4a2zuirypscugq9fj67a1] [7c3316db-ae59-43a8-aa67-4990fe4506cc] [hystrix-ApiOperationPool-483] 203.153.14.144 /api/v2/me  c.g.c.v.a.t.u.o.StartContactCenterSessionApiTaskV2 https://esv1-cvt-gws-p.airbnb.biz/ui/crm-workspace/index.html?sfdcIFrameOrigin=https%3A%2F%2Fairbnb.my.salesforce.com&nonce=b205c3a50141a2989a23ce1abfc1f8c7bb46bf0dfa5da4bf898c3257d12dc8ee&isAdapterUrl=true&isdtp=vw& Place: [Configuration object: type = CfgPlace, properties = {
    private static final Pattern regConfigObjectRead = Pattern.compile("Configuration object: type = (\\S+), properties = \\{");

    private class WWEBayeuxSMsg extends WWEDebugMsg {

        private boolean msgParsed = false;
        private Integer bayeuxId;
        private Long lastConnected;
        private String action;
        private String channel;
        private String msgType;

        public String getChannel() {
            return channel;
        }

        public String getMsgType() {
            return msgType;
        }

        private WWEBayeuxSMsg() {
            super(TableType.WWEBayeuxMessage);
        }

        void setBayeuxId(String rx) {
            try {
                if (rx != null && !rx.isEmpty()) {
                    bayeuxId = new Integer(rx);
                }
            } catch (NumberFormatException numberFormatException) {
                Main.logger.error("1error parsing number [" + rx + "]");
            }
        }

        void setLastConnected(String rx) {
            try {
                if (rx != null && !rx.isEmpty()) {
                    lastConnected = new Long(rx);
                }
            } catch (NumberFormatException numberFormatException) {
                Main.logger.error("2error parsing number [" + rx + "]");
            }
        }

        void setAction(String rx) {
            action = rx;
        }

        private WWEBayeuxSMsg(WWEDebugMsg entry) {
            super(TableType.WWEBayeuxMessage, entry);
        }

        private void setAdditionalLines(ArrayList<String> m_MessageContents) {
            addMessageLines(m_MessageLines);
        }

        @Override
        public String getMsgText() {
//            parserMessageText();
            return super.getMsgText(); //To change body of generated methods, choose Tools | Templates.
        }

//        private void parserMessageText() {
//            if (!msgParsed) {
//                String txt = super.getMsgText();
//                if (txt != null && !txt.isEmpty()) {
//                    int i = 1;
//                    if (txt.startsWith("<")) {
//                        Matcher m;
//                        boolean emptyMsg = false;
//                        if ((m = regBayeuxNotificationType.matcher(txt)).find()) {
//                            super.setParam2(m.group(1));
//                            emptyMsg = true;
//                        }
////                        if ((m = regBayeuxMessageType.matcher(txt)).find()) {
////                            super.setParam1(m.group(1));
////                            emptyMsg = true;
////                        }
//                        if (emptyMsg) {
//                            super.setMessageText("");
//                        }
//                        super.setUUID(getRx(txt, regBayeuxCallUUID, 1, null));
//                        super.setDeviceID(getRx(txt, regBayeuxDeviceURI, 1, null));
//                    }
//                }
//                msgParsed = true;
//            }
//        }
        public Integer getBayeuxId() {
            return bayeuxId;
        }

        public Long getLastConnected() {
            return lastConnected;
        }

        public String getAction() {
            return action;
        }

        private void setChannel(String rx) {
            this.channel = rx;
        }

        private void setMessageType(String rx) {
            this.msgType = rx;
        }

    }

    private class WWEBayeuxSMsgTable extends DBTable {

        public WWEBayeuxSMsgTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("IPID");
            addIndex("JSessionIDID");
            addIndex("urlID");
            addIndex("LevelID");
            addIndex("userNameID");
            addIndex("classNameID");
            addIndex("msgTextID");
            addIndex("param1ID");
            addIndex("param2ID");
            addIndex("UUIDID");
            addIndex("IxnIDID");
            addIndex("DeviceIDID");
            addIndex("channelID");
            addIndex("msgTypeID");
            addIndex("browserClientID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",IPID int"
                    + ",JSessionIDID int"
                    + ",urlID int"
                    + ",LevelID int"
                    + ",classNameID int"
                    + ",msgTextID int"
                    + ",userNameID int"
                    + ",param1ID int"
                    + ",UUIDID int"
                    + ",IxnIDID int"
                    + ",DeviceIDID int"
                    + ",param2ID int"
                    + ",bayeuxID int"
                    + ",lastConected int"
                    + ",action char(5)"
                    + ",channelID int"
                    + ",msgTypeID int"
                    + ",browserClientID int"
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
            WWEBayeuxSMsg rec = (WWEBayeuxSMsg) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.IP, rec.getIp()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.JSessionID, rec.getjSessionID()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.HTTPURL, rec.getUrl()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.MSGLEVEL, rec.getLevel()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.JAVA_CLASS, rec.getClassName()));
                setFieldInt(stmt, 11, Main.getRef(ReferenceType.IxnID, rec.getIxnID()));
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.Agent, rec.getUserName()));
                setFieldInt(stmt, 13, Main.getRef(ReferenceType.MiscParam, rec.getParam1()));
                setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, rec.getUUID()));
                setFieldInt(stmt, 15, Main.getRef(ReferenceType.IxnID, rec.getIxnID()));
                setFieldInt(stmt, 16, Main.getRef(ReferenceType.GWSDeviceID, rec.getDeviceID()));
                setFieldInt(stmt, 17, Main.getRef(ReferenceType.MiscParam, rec.getParam2()));
                setFieldInt(stmt, 18, rec.getBayeuxId());
                setFieldLong(stmt, 19, rec.getLastConnected());
                setFieldString(stmt, 20, rec.getAction());
                setFieldInt(stmt, 21, Main.getRef(ReferenceType.BayeuxChannel, rec.getChannel()));
                setFieldInt(stmt, 22, Main.getRef(ReferenceType.BayeuxMessageType, rec.getMsgType()));
                setFieldInt(stmt, 23, Main.getRef(ReferenceType.WWEBrowserClient, rec.getBrowserClient()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="WWEUCSMsg">
    private class WWEUCSMsg extends WWEDebugMsg {

        private final String msgName;

        private WWEUCSMsg(WWEDebugMsg entry, String group) {
            super(TableType.WWEUCSMessage, entry);

            this.msgName = group;
        }

        public String getMsgName() {
            return msgName;
        }

    }

    private class WWEUCSTable extends DBTable {

        public WWEUCSTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("IPID");
            addIndex("JSessionIDID");
            addIndex("urlID");
            addIndex("LevelID");
            addIndex("userNameID");
            addIndex("classNameID");
            addIndex("msgTextID");
            addIndex("DeviceIDID");
            addIndex("nameID");
            addIndex("UUIDID");
            addIndex("IxnIDID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",IPID int"
                    + ",JSessionIDID int"
                    + ",urlID int"
                    + ",LevelID int"
                    + ",classNameID int"
                    + ",msgTextID int"
                    + ",userNameID int"
                    + ",DeviceIDID int"
                    + ",nameID int"
                    + ",UUIDID int"
                    + ",IxnIDID int"
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
            WWEUCSMsg rec = (WWEUCSMsg) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.IP, rec.getIp()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.JSessionID, rec.getjSessionID()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.HTTPURL, rec.getUrl()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.MSGLEVEL, rec.getLevel()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.JAVA_CLASS, rec.getClassName()));
                setFieldInt(stmt, 11, Main.getRef(ReferenceType.Misc, rec.getMsgText()));
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.Agent, rec.getUserName()));
                setFieldInt(stmt, 13, Main.getRef(ReferenceType.GWSDeviceID, rec.getDeviceID()));

                setFieldInt(stmt, 14, Main.getRef(ReferenceType.TEvent, rec.getMsgName()));
                setFieldInt(stmt, 15, Main.getRef(ReferenceType.UUID, rec.getUUID()));
                setFieldInt(stmt, 16, Main.getRef(ReferenceType.IxnID, rec.getIxnID()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="WWEIxnServerMsg">
    private class WWEIxnServerMsg extends Message {

        private String ip;
        private String jSessionID;
        private String httpCode;
        private String date;
        private String method;
        private String key;
        private String url;

        private String deviceID;
        private String userName;
        private String level;
        private String className;
        private String msgText;
        private final String msgName;

        private WWEIxnServerMsg(String group) {
            super(TableType.WWEIxnMessage);
            this.msgName = group;
        }

        /**
         * Get the value of deviceID
         *
         * @return the value of deviceID
         */
        public String getDeviceID() {
            return deviceID;
        }

        /**
         * Set the value of deviceID
         *
         * @param deviceID new value of deviceID
         */
        public void setDeviceID(String deviceID) {
            this.deviceID = deviceID;
        }

        private String UUID;

        /**
         * Get the value of UUID
         *
         * @return the value of UUID
         */
        public String getUUID() {
            return UUID;
        }

        /**
         * Set the value of UUID
         *
         * @param UUID new value of UUID
         */
        public void setUUID(String UUID) {
            this.UUID = UUID;
        }

        private String ixnID;

        /**
         * Get the value of ixnID
         *
         * @return the value of ixnID
         */
        public String getIxnID() {
            return ixnID;
        }

        /**
         * Set the value of ixnID
         *
         * @param ixnID new value of ixnID
         */
        public void setIxnID(String ixnID) {
            this.ixnID = ixnID;
        }

        public String getHttpCode() {

            return httpCode;
        }

        public String getMethod() {
            return method;
        }

        private void setMethod(String s) {
            method = s;
        }

        private void setHttpCode(String s) {
            httpCode = s;
        }

        private void setDate(String s) {
            date = s;
        }

        private void setIP(String s) {
            ip = s;
        }

        public String getIp() {
            return ip;
        }

        public String getjSessionID() {
            return jSessionID;
        }

        public String getKey() {
            return key;
        }

        public String getUrl() {
            return url;
        }

        public String getUserName() {
            return userName;
        }

        private void setUserKey(String s) {
            key = s;
        }

        private void setJSessionID(String s) {
            jSessionID = s;
        }

        private void setURL(String group) {
            if (group != null && !group.isEmpty()) {
                try {
                    url = URLDecoder.decode(group, "UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(ApacheWebLogsParser.class.getName()).log(Level.SEVERE, null, ex);
                    url = group;
                }
            }
        }

        private void setUserName(String get) {
            this.userName = get;
        }

        private void setLevel(String group) {
            this.level = group;

        }

        private void setClassName(String group) {
            this.className = group;

        }

        private void setMessageText(String group) {
            this.msgText = group;
        }

        public String getLevel() {
            return level;
        }

        public String getMsgName() {
            return msgName;
        }

        public String getClassName() {
            return className;
        }

        public String getMsgText() {
            return msgText;
        }
    }

    private class WWEIxnTable extends DBTable {

        public WWEIxnTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("nameID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",nameID int"
                    + ");";
            getM_dbAccessor().runQuery(query);

            m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                    /*standard first*/
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
            WWEIxnServerMsg rec = (WWEIxnServerMsg) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, rec.getMsgName()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="WWEUCSConfigMsg">

    private class WWEUCSConfigMsg extends WWEDebugMsg {

        private final String msgName;

        private WWEUCSConfigMsg(String group) {
            super(TableType.WWEUCSConfigMessage);
            this.msgName = group;
        }

        private WWEUCSConfigMsg(WWEDebugMsg entry, String group) {
            super(TableType.WWEUCSConfigMessage);
            this.msgName = group;
        }

        public String getMsgName() {
            return msgName;
        }

    }

    private class WWEUCSConfigTable extends DBTable {

        public WWEUCSConfigTable(DBAccessor dbaccessor, TableType t) {
            super(dbaccessor, t);
        }

        @Override
        public void InitDB() {
            addIndex("time");
            addIndex("FileId");

            addIndex("IPID");
            addIndex("JSessionIDID");
            addIndex("urlID");
            addIndex("LevelID");
            addIndex("userNameID");
            addIndex("classNameID");
            addIndex("msgTextID");
            addIndex("nameID");

            dropIndexes();

            String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                    + ",time timestamp"
                    + ",FileId INTEGER"
                    + ",FileOffset bigint"
                    + ",FileBytes int"
                    + ",line int"
                    /* standard first */
                    + ",IPID int"
                    + ",JSessionIDID int"
                    + ",urlID int"
                    + ",LevelID int"
                    + ",classNameID int"
                    + ",msgTextID int"
                    + ",userNameID int"
                    + ",nameID int"
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
            WWEUCSConfigMsg rec = (WWEUCSConfigMsg) _rec;
            PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

            try {
                stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
                stmt.setInt(2, rec.getFileId());
                stmt.setLong(3, rec.m_fileOffset);
                stmt.setLong(4, rec.getM_FileBytes());
                stmt.setLong(5, rec.m_line);

                setFieldInt(stmt, 6, Main.getRef(ReferenceType.IP, rec.getIp()));
                setFieldInt(stmt, 7, Main.getRef(ReferenceType.JSessionID, rec.getjSessionID()));
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.HTTPURL, rec.getUrl()));
                setFieldInt(stmt, 9, Main.getRef(ReferenceType.MSGLEVEL, rec.getLevel()));
                setFieldInt(stmt, 10, Main.getRef(ReferenceType.JAVA_CLASS, rec.getClassName()));
                setFieldInt(stmt, 11, Main.getRef(ReferenceType.Misc, rec.getMsgText()));
                setFieldInt(stmt, 12, Main.getRef(ReferenceType.Agent, rec.getUserName()));

                setFieldInt(stmt, 13, Main.getRef(ReferenceType.TEvent, rec.getMsgName()));

                getM_dbAccessor().SubmitStatement(m_InsertStatementId);
            } catch (SQLException e) {
                Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            }
        }

    }
//</editor-fold>
}
