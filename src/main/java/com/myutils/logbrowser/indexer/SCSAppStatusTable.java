/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.SQLException;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class SCSAppStatusTable extends DBTable {

    public SCSAppStatusTable(SqliteAccessor dbaccessor) {
        super(dbaccessor, TableType.SCSAppStatus, "SCS_appStatus");
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
        addIndex("eventID");
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
                + ",eventID int"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");";
    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws SQLException {
        createIndexes();
    }


}
