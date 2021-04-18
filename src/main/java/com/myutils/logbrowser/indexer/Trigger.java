/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 *
 * @author ssydoruk
 */
public class Trigger extends Record {

    int m_msgId;
    private String m_text;

    Trigger() {
        super(TableType.Trigger);
    }

    @Override
    public String toString() {
        return "Trigger{" + "m_msgId=" + m_msgId + ", m_text=" + m_text + '}' + super.toString();
    }

    public int getM_msgId() {
        return m_msgId;
    }

    public String getM_text() {
        return m_text;
    }

    public void SetText(String text) {
        m_text = text;
    }

    public void SetMsgId(int id) {
        m_msgId = id;
    }
    /*
    public static String InitDB(DBAccessor accessor, int batchId) {
        m_batchId = batchId;
        String query = "create table if not exists trigger_" + m_alias + " (id INTEGER PRIMARY KEY ASC,"+
                    "text CHAR(128),"+
                    "HandlerId INTEGER,"+
                    "SipId INTEGER,"+
                    "TlibId INTEGER,"+
                    "JsonId INTEGER,"+
                    "FileId INTEGER,"+
                    "FileOffset bigint,"+
                    "Line INTEGER);";
        accessor.ExecuteQuery(query);
        return "INSERT INTO trigger_"+m_alias+" VALUES(NULL,?,?,?,?,?,?,?,?);";
    }
    
    public void AddToDB(DBAccessor accessor) {
        PreparedStatement stmt = accessor.GetStatement(m_batchId);
        
        try {
            setFieldString(stmt,1,m_text);
            stmt.setInt(2, m_handlerId);
            stmt.setInt(3, (m_msgId == 1 ? m_sipId : 0));
            stmt.setInt(4, (m_msgId == 0 ? m_tlibId : 0));
            stmt.setInt(5, (m_msgId == 2 ? m_jsonId : 0));
            stmt.setInt(6, m_fileId);
            stmt.setLong(7,getM_fileOffset());
            stmt.setInt(8, getM_line());
            
            accessor.SubmitStatement(m_batchId);
        } catch (Exception e) {
            Main.logger.error("Could not add trigger: " + e);
        }
    }
    * */
}
