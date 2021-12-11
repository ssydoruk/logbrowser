/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class OCSIconTable extends DBTable {

    public OCSIconTable(SqliteAccessor dbaccessor) {
        super(dbaccessor, TableType.SCSSelfStatus,"OCSIcon");
    }

    @Override
    public void InitDB() {
        
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
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
    public void AddToDB(Record _rec) throws SQLException {
        OCSIcon rec = (OCSIcon) _rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.OCSIconEvent, rec.getIconEvent()));
            setFieldInt(stmt, 7, rec.getSwitchDBID());
            setFieldInt(stmt, 8, rec.getGroupDBID());
            setFieldInt(stmt, 9, rec.getCampaignDBID());
            setFieldInt(stmt, 10, rec.getListDBID());
            setFieldInt(stmt, 11, rec.getChainDBID());
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.OCSIconCause, rec.getIconCause()));

            }
        });
    }


}
