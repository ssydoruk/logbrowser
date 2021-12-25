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
public class IxnDBActivityTable extends DBTable {

    public IxnDBActivityTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "ixndb");
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");
        addIndex("activityid");
        addIndex("ixnid");
        dropIndexes();

        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists ixndb_fileid;");
            getM_dbAccessor().runQuery("drop index if exists ixndb_unixtime;");

        }
        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",activityid INTEGER"
                + ",reqid INTEGER"
                + ",ixnid INTEGER"
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
                + ");";
    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }


}
