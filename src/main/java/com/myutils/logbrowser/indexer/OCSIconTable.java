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
public class OCSIconTable extends DBTable {

    public OCSIconTable(SqliteAccessor dbaccessor) {
        super(dbaccessor, TableType.SCSSelfStatus, "OCSIcon");
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
