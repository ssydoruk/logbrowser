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
public class StStatusTable extends DBTable {

    public StStatusTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("ststatus");
        addIndex("time");
        addIndex("FileId");
        addIndex("typeid");
        addIndex("agentid");
        addIndex("placeid");
        addIndex("oldstatusid");
        addIndex("newstatusid");
        addIndex("dnid");
        dropIndexes();

        String query = "create table if not exists ststatus (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",typeid integer"
                + ",agentid integer"
                + ",placeid integer"
                + ",oldstatusid integer"
                + ",newstatusid integer"
                + ",dnid integer"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO ststatus VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
    public void AddToDB(Record _rec) {
        StStatus rec = (StStatus) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, StStatus.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.StatServerStatusType, rec.StatusType()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, rec.AgentName()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.Place, rec.PlaceName()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.StatType, rec.OldStatus()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.StatType, rec.NewStatus()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.DN, StStatus.SingleQuotes(rec.getDn())));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
