/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ssydoruk
 */
public class Handler extends Record {

    private String m_text;


    public Handler(int fileID) {
        super(TableType.Handler, fileID);
    }

    @Override
    public String toString() {
        return "Handler{" + "m_text=" + m_text + "m_handlerId=" + getRecordID() + '}' + super.toString();
    }

    public String getM_text() {
        return m_text;
    }

    public void SetText(String text) {
        m_text = text;
    }

    /*
    public static String InitDB(DBAccessor accessor, int batchId) {
        m_batchId = batchId;
        
        String query = "create table if not exists handler_" + m_alias + " (id INTEGER PRIMARY KEY ASC,"+
                    "text CHAR(128),"+
                    "FileId INTEGER,"+
                    "FileOffset bigint,"+
                    "Line INTEGER);";
        accessor.ExecuteQuery(query);
        return "INSERT INTO handler_"+m_alias+" VALUES(NULL,?,?,?,?);";
    }
    
    public void AddToDB(DBAccessor accessor) {
        if( m_text.contains("HA_SWITCH_ACTIVE") 
            || m_text.contains("HA_SWITCH_PASSIVE") 
            || m_text.contains("HA_CONNECT_HANDLER") 
            || m_text.contains("HA_TAKE_SYNC_MESSAGE") )
        {
	    return;
        }
        
        PreparedStatement stmt = accessor.GetStatement(m_batchId);
        
        try {
            setFieldString(stmt,1,m_text);
            stmt.setInt(2, m_fileId);
            stmt.setLong(3,(long)getM_fileOffset());
            stmt.setInt(4, getM_line());
            m_handlerId++;
            accessor.SubmitStatement(m_batchId);
        } catch (Exception e) {
            Main.logger.error("Could not add handler: " + e);
        }
    }
     */
}
