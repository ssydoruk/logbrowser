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
public class LCAClientTable extends DBTable {

    public LCAClientTable(DBAccessor dbaccessor) {
        super(dbaccessor, TableType.LCAClient);
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");
        addIndex("theAppNameID");
        addIndex("typeID");
        addIndex("IPID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",appDBID int"
                + ",theAppNameID int"
                + ",PID int"
                + ",typeID int"
                + ",IPID int"
                + ",FD int"
                + ",isConnected bit"
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
        LCAClient rec = (LCAClient) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, SCSAppStatus.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            stmt.setInt(6, rec.getAppDBID());
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.App, rec.getAppName()));
            stmt.setInt(8, rec.getPID());
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.AppType, rec.getAppType()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.IP, rec.getIP()));
            stmt.setInt(11, rec.getFD());
            stmt.setBoolean(12, rec.isConnected());

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
