package com.myutils.logbrowser.indexer;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author terry
 */
public class DBServerParser extends Parser {

    private static final Pattern regRI = Pattern.compile("^_R_I_\\s*");

    private static final Pattern regCfgUpdate = Pattern.compile("^\\s+PopCfg");

    long m_CurrentFilePos;
    int m_CurrentLine;
    final int MSG_STRING_LIMIT = 200;
    long m_HeaderOffset;
    ParserState m_ParserState;
    String m_Header;
    int m_dbRecords = 0;

    public DBServerParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_DBServer, m_tables);

//17:33:06.335_I_I_03350282382b556f [07:07] HERE IS TARGETS
//TARGETS: OBN_IP_Skill1_Group@OBN_StatServerRouting.GA
    }

    public int ParseFrom(BufferedReaderCrLf input, long offset, int line, FileInfo fi) {
        m_CurrentFilePos = offset;
        m_CurrentLine = line;
        m_dbRecords = 0;

        try {
            input.skip(offset);
            setFilePos(offset);
            setSavedFilePos(getFilePos());
            m_MessageContents.clear();

            m_ParserState = ParserState.STATE_HEADER;

            String str;
            while ((str = input.readLine()) != null) {
                try {
                    int l = input.getLastBytesConsumed();
                    setEndFilePos(getFilePos() + l); // to calculate file bytes
                    m_CurrentLine++;
                    Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");

                    while (str != null) {
                        str = ParseLine(str);
                    }
                    setFilePos(getFilePos() + l);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                    m_ParserState = ParserState.STATE_COMMENTS;
//                    throw e;
                }
//                m_CurrentFilePos += str.length()+input.charsSkippedOnReadLine;
            }
            ParseLine(""); // to complete the parsing of the last line/last message
        } catch (Exception e) {
            e.printStackTrace();
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

    private String ParseLine(String str) throws Exception {
//        String trimmed = str.trim();
        Matcher m;
        String s = str;

        Main.logger.trace("Parser state " + m_ParserState);
        ParseCustom(str, 0);

        switch (m_ParserState) {
            case STATE_HEADER: {
                if (str.trim().isEmpty()) {
                    m_ParserState = ParserState.STATE_COMMENTS;
                }
            }
            break;

            case STATE_COMMENTS:
                m_LineStarted = m_CurrentLine;

                try {
                    s = ParseGenesys(str, TableType.MsgDBServer);
                } catch (Exception exception) {
                    Main.logger.error("Exception in state " + m_ParserState, exception);
                }

                if ((m = regCfgUpdate.matcher(s)).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_CONFIG;
//                } else if ((m = regAgentStatusChanged.matcher(s)).find()) {
//                    setSavedFilePos(getFilePos());
//                    m_MessageContents.add(s.substring(m.end()));
//                    m_ParserState = ParserState.STATEAGENTSTATUSNEW;
                }
                break;

        }

        return null;
    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
    }

// parse state contants
    enum ParserState {

        STATE_HEADER,
        STATE_TLIB_MESSAGE_IN,
        STATE_STRATEGY_MULTILINE,
        STATE_NONSTRATEGY_MULTILINE,
        STATE_TLIB_MESSAGE_OUT,
        STATE_COMMENTS,
        STATE_RLIB,
        STATE_CONFIG,
        STATE_RI_CALLSTART,
        STATE_WAITINGCALLS,
        STATEAGENTSTATUSNEW,
        STATE_STRATEGY_TARGETS,
        STATE_WAITINGCALLSSTART,
        smUpdateObject,
        STATE_STRATEGY_SCXML,
        HTTP_BRIDGE,
        STATE_HTTP_RESPONSE,
        STATE_ROUTING_TARGET
    }

}
