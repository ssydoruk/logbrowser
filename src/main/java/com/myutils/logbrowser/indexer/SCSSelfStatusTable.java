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
public class SCSSelfStatusTable extends DBTable {

    public SCSSelfStatusTable(SqliteAccessor dbaccessor) {
        super(dbaccessor, TableType.SCSSelfStatus,"SCS_SelfStatus");
    }

    @Override
    public void InitDB() {
        
        addIndex("time");
        addIndex("FileId");
        addIndex("newModeID");
        addIndex("theAppNameID");
        addIndex("oldModeID");
        addIndex("statusID");
        addIndex("hostID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",newModeID int"
                + ",appDBID int"
                + ",theAppNameID int"
                + ",PID int"
                + ",oldModeID int"
                + ",statusID bit"
                + ",hostID int"
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
        SCSSelfStatus rec = (SCSSelfStatus) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.APPRunMode, rec.getNewMode()));
            stmt.setInt(7, rec.getAppDBID());
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.App, rec.getAppName()));
            stmt.setInt(9, rec.getPID());
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.APPRunMode, rec.getOldMode()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.appStatus, rec.getStatus()));
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.Host, rec.getHost()));

                        }
        });
    }



}
