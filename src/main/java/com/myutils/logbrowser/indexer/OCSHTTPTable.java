/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author ssydoruk
 *
 *
 * Make Refactor/Copy of this class for new table type
 */
public class OCSHTTPTable extends DBTable {

    public OCSHTTPTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("OCSHTTP");
        addIndex("FileId");
        addIndex("RecHandle");
        addIndex("ReqNameID");
        addIndex("time");
        addIndex("campNameID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",ReqNameID INTEGER"
                + ",RecHandle INTEGER"
                + ",isinbound bit"
                + ",campNameID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");");

    }

    /**
     *
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
    public void AddToDB(Record _rec) {
        OCSHTTP rec = (OCSHTTP) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, OCSHTTP.getFileId());
            stmt.setLong(3, rec.m_fileOffset);
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.m_line);

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, rec.GetMessageName()));
            stmt.setLong(7, rec.getRecHandle());
            stmt.setBoolean(8, rec.isInbound());
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.OCSCAMPAIGN, rec.GetCampaignName()));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
