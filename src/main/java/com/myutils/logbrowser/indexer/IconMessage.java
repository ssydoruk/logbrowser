/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * @author terry The class Replicates TLibMessage
 */
public class IconMessage extends Message {

    private static int m_statementId;
    String m_MessageName;

    public IconMessage(String event, ArrayList newMessageLines, int fileID) {
        super(TableType.ICONMessage, fileID);
        m_MessageLines = newMessageLines;
        m_MessageName = event;
        m_MessageLines.add(0, event);
    }

    public static String InitDB(DBAccessor accessor, int statementId) {
        m_statementId = statementId;

        String query = "create table if not exists icon_" + m_alias + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "name char(64),"
                + "thisDN char(32),"
                + "otherDN char(32),"
                + "agentID char(32),"
                + "ani char(32),"
                + "ConnectionID char(64),"
                + "TransferId char(64),"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "HandlerId INTEGER,"
                + "line int);";
        accessor.runQuery(query);
        return "INSERT INTO icon_" + m_alias + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?);";
    }

    public String GetMessageName() {
        return m_MessageName;
    }

    public String GetConnID() {
        return GetHeaderValue("AttributeConnID");
    }

    public String GetTransferConnID() {
        return GetHeaderValue("AttributeTransferConnID");
    }

    public void AddToDB(SqliteAccessor accessor) throws SQLException {
        String str;
        String thisDN = GetHeaderValue("AttributeThisDN ");
        if (thisDN == null) {
            thisDN = GetHeaderValue("AttributeThisDN\t");
        }

        String otherDN = GetHeaderValue("AttributeOtherDN ");
        if (otherDN == null) {
            otherDN = GetHeaderValue("AttributeOtherDN\t");
        }

        String agentID = GetHeaderValue("AttributeAgentID ");
        if (agentID == null || "".equals(agentID)) {
            agentID = GetHeaderValue("AttributeAgentID\t");
        }

        String ani = GetHeaderValue("AttributeANI ");
        if (ani == null || "".equals(ani)) {
            ani = GetHeaderValue("AttributeANI\t");
        }

        String finalThisDN = thisDN;
        String finalOtherDN = otherDN;
        String finalAgentID = agentID;
        String finalAni = ani;
        accessor.addToDB(m_statementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            DBTable.setFieldString(stmt, 2, GetMessageName());
            DBTable.setFieldString(stmt, 3, finalThisDN);
            DBTable.setFieldString(stmt, 4, finalOtherDN);
            DBTable.setFieldString(stmt, 5, finalAgentID);
            DBTable.setFieldString(stmt, 6, finalAni);
            DBTable.setFieldString(stmt, 7, GetConnID());
            DBTable.setFieldString(stmt, 8, GetTransferConnID());
            stmt.setInt(9, getFileID());
            stmt.setLong(10, getM_fileOffset());
            stmt.setLong(11, getFileBytes());
            stmt.setInt(12, 0);
            stmt.setInt(13, getM_line());

            }
        });
    }



}
