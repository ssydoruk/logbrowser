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
public class TriggerTable extends DBTable {

    public TriggerTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("trigger_" + getM_dbAccessor().getM_alias());
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
    public void AddToDB(Record rec) {
        Trigger theRec = (Trigger) rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setInt(1, Main.getRef(ReferenceType.HANDLER, theRec.getM_text()));
            Main.logger.trace("theRec.m_handlerId:"
                    + Trigger.m_handlerId + " theRec.m_msgId:"
                    + theRec.m_msgId + " theRec.m_sipId:"
                    + Trigger.m_sipId + " theRec.m_tlibId:" + Trigger.m_tlibId + " theRec.m_jsonId" + Trigger.m_jsonId);
            stmt.setInt(2, Trigger.m_handlerId);
            stmt.setInt(3, (theRec.m_msgId == 1 ? Trigger.m_sipId : 0));
            stmt.setInt(4, (theRec.m_msgId == 0 ? Trigger.m_tlibId : 0));
            stmt.setInt(5, (theRec.m_msgId == 2 ? Trigger.m_jsonId : 0));
            stmt.setInt(6, Trigger.getFileId());
            stmt.setLong(7, theRec.getM_fileOffset());
            stmt.setInt(8, theRec.getM_line());

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add trigger: " + e, e);
        }

    }

}
