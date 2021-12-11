/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public class TriggerTable extends DBTable {

    public TriggerTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"trigger_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
//        addIndex("time");
        addIndex("FileId");
        addIndex("textid");
        addIndex("HandlerId");
        addIndex("SipId");
        addIndex("TlibId");
        addIndex("JsonId");
        dropIndexes();

        String query = "create table if not exists trigger_" + db_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "textid INTEGER,"
                + "HandlerId INTEGER,"
                + "SipId INTEGER,"
                + "TlibId INTEGER,"
                + "JsonId INTEGER,"
                + "FileId INTEGER"
                + ","
                + "FileOffset bigint,"
                + "Line INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO trigger_" + db_alias() + " VALUES(NULL,?,?,?,?,?,?,?,?);");

    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void AddToDB(Record rec) throws SQLException {
        Trigger theRec = (Trigger) rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, stmt -> {
        stmt.setInt(1, Main.getRef(ReferenceType.HANDLER, theRec.getM_text()));
        Main.logger.trace("theRec.m_handlerId:"
                + theRec.getM_handlerId() + " theRec.m_msgId:"
                + theRec.m_msgId + " theRec.m_sipId:"
                + theRec.getM_sipId() + " theRec.m_tlibId:" + theRec.getM_tlibId() + " theRec.m_jsonId" +theRec.getM_jsonId());
        stmt.setInt(2, theRec.getM_handlerId());
        stmt.setInt(3, (theRec.m_msgId == 1 ? theRec.getM_sipId() : 0));
        stmt.setInt(4, (theRec.m_msgId == 0 ? theRec.getM_tlibId() :  0));
        stmt.setInt(5, (theRec.m_msgId == 2 ? theRec.getM_jsonId() :  0));
        stmt.setInt(6, rec.getFileID());
        stmt.setLong(7, theRec.getM_fileOffset());
        stmt.setInt(8, theRec.getM_line());

        });
    }


}
