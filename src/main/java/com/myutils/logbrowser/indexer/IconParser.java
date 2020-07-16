/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor. 
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.HashMap;

public class IconParser extends Parser {

    long m_CurrentFilePos;

    final int MSG_STRING_LIMIT = 200;
    // parse state contants
    final int STATE_COMMENTS = 0;
    final int STATE_TLIB_MESSAGE_IN = 5;
    /* T-Lib message as T-Server writes it */
    final int STATE_TLIB_MESSAGE_OUT = 6;
    /* T-Lib message as T-Server writes it */

    long m_HeaderOffset;
    int m_ParserState;
    String m_Header;

    int m_dbRecords = 0;

    public IconParser(HashMap<TableType, DBTable> m_tables) {
        super(FileInfoType.type_ICON, m_tables);
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

            m_ParserState = STATE_COMMENTS;

            String str;
            while ((str = input.readLine()) != null) {
                m_CurrentLine++;
                Main.logger.trace("l: " + m_CurrentLine + " [" + str + "]");

                try {
                    ParseLine(str);

                } catch (Exception e) {
                    Main.logger.error("Failure parsing line " + m_CurrentLine + ":"
                            + str + "\n" + e, e);
                    throw e;
                }
                m_CurrentFilePos += str.length() + input.charsSkippedOnReadLine;
            }
            ParseLine(""); // to complete the parsing of the last line/last message
        } catch (Exception e) {
            return m_CurrentLine - line;
        }

        return m_CurrentLine - line;
    }

    void ParseLine(String str) throws Exception {
        String trimmed = str.trim();

        int n = trimmed.indexOf('_');
//        if (n>0 && trimmed.length()>n+5 && trimmed.charAt(n+2)=='_' && trimmed.charAt(n+4)=='_') {
//            m_LastTimeStamp= trimmed.substring(0, n);
//        }

        switch (m_ParserState) {
            case STATE_COMMENTS:

                if (str.contains("Trc 04541 Message") || str.contains("Int 04543 Interaction message")) {
                    m_Header = trimmed;
                    m_HeaderOffset = m_CurrentFilePos;
                    m_MessageContents = new ArrayList();
                    m_ParserState = STATE_TLIB_MESSAGE_IN;
                } else if (str.startsWith("request to")) {
                    m_Header = trimmed;
                    m_HeaderOffset = m_CurrentFilePos;
                    m_MessageContents = new ArrayList();
                    m_ParserState = STATE_TLIB_MESSAGE_OUT;
                }
                break;

            case STATE_TLIB_MESSAGE_IN:
                if (trimmed.length() == 0 || str.charAt(0) == trimmed.charAt(0)) { //message body is over
                    m_ParserState = STATE_COMMENTS;
//		    if (trimmed.contains("[14:0c]") || trimmed.contains("[14:32]")) {
                    if (m_MessageContents.size() > 0) {
                        AddIconMessage(m_MessageContents, m_Header, str);
                    }
                    m_MessageContents = null;
                    /*                   }
                    else { //reparse the last line
                        m_MessageContents= null;
                        ParseLine(str);
                    }*/
                } else { //still inside message
                    if (trimmed.startsWith("Attribute") || trimmed.startsWith("attr_#")) {
                        // Make sure Attribute and value are separate with one tab
                        trimmed = trimmed.replaceFirst(" +", "\t");
                        m_MessageContents.add(trimmed);
                    } else if (trimmed.startsWith("'")) {
                        // Assume it is contents of AttributeUserData or AttributeExtensions
                        m_MessageContents.add("\t" + trimmed);
                    }
                }

                break;

            case STATE_TLIB_MESSAGE_OUT:
                if (trimmed.length() == 0 || str.charAt(0) == trimmed.charAt(0)) { //message body is over
                    m_ParserState = STATE_COMMENTS;
                    if (trimmed.startsWith("..sent to")) {
                        if (m_MessageContents.size() > 0) {
                            AddIconMessage(m_MessageContents, m_Header, getCurrentTimestamp().date);
                        }
                        m_MessageContents = null;
                    } else { //reparse the last line
                        m_MessageContents = null;
                        ParseLine(str);
                    }
                } else { //still inside message
                    if (trimmed.startsWith("Attribute") || trimmed.startsWith("attr_#")) {
                        // Make sure Attribute and value are separate with one tab
                        trimmed = trimmed.replaceFirst(" +", "\t");
                        m_MessageContents.add(trimmed);
                    } else if (trimmed.startsWith("'")) {
                        // Assume it is contents of AttributeUserData or AttributeExtensions
                        m_MessageContents.add("\t" + trimmed);
                    }
                }

                break;

        }
    }

    protected void AddIconMessage(ArrayList contents, String header, String urs_message) throws Exception {
        String[] headerList = header.split(" ");
        IconMessage msg = null;
//      TLibMessage msg = null;

        if (headerList.length >= 4) {
            if (headerList[3].equals("Message")) {
                String event = headerList[4];
                msg = new IconMessage(event, contents);
//              msg = new TLibMessage(event, contents);
            } else if (headerList[4].equals("message")) {
                String event = headerList[5].substring(1, headerList[5].length() - 1);
                msg = new IconMessage(event, contents);
            }
        }

        if (msg != null /*&& msg.GetConnID()!=null*/) {
            //String[] ursList = urs_message.split("_");
            //if (ursList.length>0) {
            String timestamp = headerList[0];
            msg.setM_Timestamp(getCurrentTimestamp());

            msg.SetOffset(m_HeaderOffset);
            int headerLine = m_CurrentLine - contents.size();
            if (headerLine < 1) {
                headerLine = 1;
            }
            msg.SetLine(headerLine);
            msg.SetFileBytes((int) (m_CurrentFilePos - m_HeaderOffset));
            msg.AddToDB(m_accessor);
            m_dbRecords++;
            //}
        }
    }

    @Override
    void init(HashMap<TableType, DBTable> m_tables) {
    }
}
