/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public class CustomLine extends Record {

    private static int m_batchId;
    private String m_text;

    public static String InitDB(DBAccessor accessor, int batchId) {
        m_batchId = batchId;

        String query = "CREATE TABLE if not exists custom_" + m_alias + " (id INTEGER PRIMARY KEY ASC,"
                + "text CHAR(255),"
                + "HandlerId INTEGER,"
                + "FileId INTEGER,"
                + "FileOffset BIGINT,"
                + "line int);";
        accessor.runQuery(query);
        accessor.runQuery("create index if not exists custom_HndId_" + m_alias + " on custom_" + m_alias + " (HandlerId);");

        return "INSERT INTO custom_" + m_alias + " VALUES(NULL,?,?,?,?,?);";
    }

    @Override
    public String toString() {
        return "CustomLine{" + "m_text=" + m_text + '}' + super.toString();
    }

    public void SetText(String text) {
        m_text = text;
    }

    public void AddToDB(SqliteAccessor accessor) throws SQLException {

        accessor.addToDB(m_batchId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{

            if (m_text.length() > 255) {
                m_text = m_text.substring(0, 254);
            }

            DBTable.setFieldString(stmt, 1, m_text);
            stmt.setInt(2, m_handlerId);
            stmt.setInt(3, getFileId());
            stmt.setLong(4, getM_fileOffset());
            stmt.setLong(5, getM_line());

            }
        });
    }


}
