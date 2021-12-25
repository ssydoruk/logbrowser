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
 */
public class GMSORSTable extends DBTable {

    public GMSORSTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"gmsors");
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");

        addIndex("GMSServiceID");
        addIndex("ORSSessionID");
        addIndex("connIDID");
        addIndex("reqID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",GMSServiceID int"
                + ",ORSSessionID int"
                + ",connIDID int"
                + ",reqID int"
                + ",isinbound bit"
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
