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
public class GMSPostTable extends DBTable {

    public GMSPostTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"GMSPost");
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");

        addIndex("EventNameID");
        addIndex("CallBackTypeID");
        addIndex("GMSServiceID");
        addIndex("ORSSessionID");
        addIndex("phoneID");
        addIndex("URIID");
        addIndex("sourceID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",EventNameID int"
                + ",CallBackTypeID int"
                + ",GMSServiceID int"
                + ",ORSSessionID int"
                + ",phoneID int"
                + ",isinbound bit"
                + ",URIID int"
                + ",sourceID int"
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
    public void FinalizeDB() throws Exception {
        createIndexes();
    }



}
