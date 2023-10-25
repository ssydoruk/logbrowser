package com.myutils.logbrowser.indexer;

import java.util.regex.Pattern;

/**
 * @author terry
 */
public class DBServerParser extends Parser {

    private static final Pattern regCfgUpdate = Pattern.compile("^\\s+PopCfg");
    long m_CurrentFilePos;
    ParserState m_ParserState;
    int m_dbRecords = 0;

    public DBServerParser(DBTables tables) {
        super(FileInfoType.type_DBServer, tables);

//17:33:06.335_I_I_03350282382b556f [07:07] HERE IS TARGETS
//TARGETS: OBN_IP_Skill1_Group@OBN_StatServerRouting.GA
    }

    @Override
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
                }
            }
            ParseLine(""); // to complete the parsing of the last line/last message
        } catch (Exception e) {
            Main.logger.error(e);
        }

        return m_CurrentLine - line;
    }

    private String ParseLine(String str) throws Exception {
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
                m_lineStarted = m_CurrentLine;

                try {
                    s = ParseGenesys(str, TableType.MsgDBServer);
                } catch (Exception exception) {
                    Main.logger.error("Exception in state " + m_ParserState, exception);
                }

                if (regCfgUpdate.matcher(s).find()) {
                    setSavedFilePos(getFilePos());
                    m_MessageContents.add(s);
                    m_ParserState = ParserState.STATE_CONFIG;
                }
                break;

        }

        return null;
    }

    @Override
    void init(DBTables tables) {
        /* */
    }

    enum ParserState {

        STATE_HEADER,
        STATE_COMMENTS, STATE_CONFIG,
    }

}
