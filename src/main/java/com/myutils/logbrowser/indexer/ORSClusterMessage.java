/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.regex.Pattern;

/**
 * @author terry The class Replicates TLibMessage
 */
public class ORSClusterMessage extends Message {

    private static final Pattern patNewLine = Pattern.compile("\r\n");
    private static int m_statementId;
    private final String[] m_msgBody;
    private String SrcNodeType;
    private int SrcNodeID;
    private String DstNodeType;
    private int DstNodeID;
    private boolean Direction;
    private long m_Bytes;

    ORSClusterMessage(String arr, int fileID) {
        super(TableType.ORSCluster, fileID);
        m_msgBody = arr.split("\r\n");
    }

    public static String InitDB(DBAccessor accessor, int statementId) {
        m_statementId = statementId;

        String query = "create table if not exists ORSCluster_" + m_alias + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                + "srcNodeType char(1),"
                + "srcNodeID INTEGER,"
                + "dstNodeType char(1),"
                + "dstNodeID INTEGER,"
                + "Inbound bit,"
                + "UUID char(32));";
        accessor.runQuery(query);
        return "INSERT INTO ORSCluster_" + m_alias + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?);";
    }

    public void setM_Bytes(long m_Bytes) {
        this.m_Bytes = m_Bytes;
    }

    public void setSrcNodeType(String SrcNodeType) {
        this.SrcNodeType = SrcNodeType;
    }

    public void setSrcNodeID(int SrcNodeID) {
        this.SrcNodeID = SrcNodeID;
    }

    public void setDstNodeType(String DstNodeType) {
        this.DstNodeType = DstNodeType;
    }

    public void setDstNodeID(int DstNodeID) {
        this.DstNodeID = DstNodeID;
    }

    public void setDirection(boolean Direction) {
        this.Direction = Direction;
    }

    public void AddToDB(SqliteAccessor accessor) throws SQLException {
        String sReq = getMsgRequest();
        String sUUID = getMsgValue("e:");


        accessor.addToDB(m_statementId, (PreparedStatement stmt) -> {

            stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
            stmt.setInt(2, getFileID());
            stmt.setLong(3, getM_fileOffset());
            stmt.setLong(4, m_Bytes);
            stmt.setInt(5, getM_line());
            Record.setFieldString(stmt, 6, SrcNodeType);
            stmt.setInt(7, SrcNodeID);
            stmt.setInt(9, DstNodeID);
            stmt.setBoolean(10, Direction);
            Record.setFieldString(stmt, 11, sUUID);
            return false;
        });
    }


    void SetSrcNode(String group) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String getMsgRequest() {
        return m_msgBody[0];
    }

    private String getMsgValue(String e) {
        for (String s : m_msgBody) {
            if (s.startsWith(e)) {
                return s.substring(e.length());
            }
        }
        return "";
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        return false;
    }
}
