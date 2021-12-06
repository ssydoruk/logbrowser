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
public class HandlerTable extends DBTable {

    public HandlerTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("handler_" + getM_dbAccessor().getM_alias());
        addIndex("textid");
        addIndex("FileId");
        dropIndexes();

        String query = "create table if not exists handler_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "textid int,"
                + "FileId INTEGER"
                //                + ","
                //                + "FileOffset bigint,"
                //                + "Line INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);

        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO handler_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?"
                //                + ",?,?"
                + ");");
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void AddToDB(Record arec) throws SQLException {
        Handler theRec = (Handler) arec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{

        String m_text = theRec.getM_text();

//        if( m_text.contains("HA_SWITCH_ACTIVE") 
//            || m_text.contains("HA_SWITCH_PASSIVE") 
//            || m_text.contains("HA_CONNECT_HANDLER") 
//            || m_text.contains("HA_TAKE_SYNC_MESSAGE") )
//        {
//	    return;
//        }
            stmt.setInt(1, Main.getRef(ReferenceType.HANDLER, m_text));
            stmt.setInt(2, theRec.getFileID());
//            stmt.setLong(3, (long) theRec.getM_fileOffset());
//            stmt.setInt(4, theRec.getM_line());
        }
            });
        }


    }
