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
public class OCSIconTable extends DBTable {

    public OCSIconTable(DBAccessor dbaccessor) {
        super(dbaccessor, TableType.SCSSelfStatus);
    }

    @Override
    public void InitDB() {
        setTabName("OCSIcon");
        addIndex("time");
        addIndex("FileId");
        addIndex("eventID");
        addIndex("switchDBID");
        addIndex("groupDBID");
        addIndex("campaignDBID");
        addIndex("listDBID");
        addIndex("chID");
        addIndex("causeID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",eventID int"
                + ",switchDBID int"
                + ",groupDBID int"
                + ",campaignDBID int"
                + ",listDBID int"
                + ",chID bit"
                + ",causeID int"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
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
        OCSIcon rec = (OCSIcon) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, SCSAppStatus.getFileId());
            stmt.setLong(3, rec.m_fileOffset);
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.m_line);

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.OCSIconEvent, rec.getIconEvent()));
            setFieldInt(stmt, 7, rec.getSwitchDBID());
            setFieldInt(stmt, 8, rec.getGroupDBID());
            setFieldInt(stmt, 9, rec.getCampaignDBID());
            setFieldInt(stmt, 10, rec.getListDBID());
            setFieldInt(stmt, 11, rec.getChainDBID());
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.OCSIconCause, rec.getIconCause()));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
