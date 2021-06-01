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
public class ORSSidSidTable extends DBTable {

    public ORSSidSidTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        addIndex("sidid");
        addIndex("newsidid");
        addIndex("FileId");

        dropIndexes();

        String query = "CREATE TABLE if not exists ORSsidsid ("
                + "FileId INTEGER,"
                + "sidid INTEGER,"
                + "newsidid INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
//        accessor.runQuery("create index if not exists ORSsess__HndId_" + m_alias +" on ORSmetr_" + m_alias + " (HandlerId);");

        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO ORSsidsid VALUES("
                + "?,"
                + "?,"
                + "?"
                + ");");

    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void AddToDB(Record aRec) {
        OrsSidSid rec = (OrsSidSid) aRec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setInt(1, Record.getFileId());

            setFieldInt(stmt, 2, Main.getRef(ReferenceType.ORSSID, rec.getSID()));
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.ORSSID, rec.getNewSID()));
            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add message type " + getM_type() + ": " + e, e);
        }

    }

}
