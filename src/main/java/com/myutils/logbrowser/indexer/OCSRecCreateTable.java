/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public class OCSRecCreateTable extends DBTable {

    public OCSRecCreateTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "OCSRECCR");
    }

    @Override
    public void InitDB() {

        addIndex("recHandle");
        addIndex("chID");
        addIndex("FileId");
        addIndex("campBID");
        addIndex("time");
        addIndex("listNameID");
        addIndex("tableNameID");
        addIndex("dnID");
        addIndex("actionID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",recHandle integer"
                + ",chID integer"
                + ",chNum integer"
                + ",campBID integer"
                + ",recType integer"
                + ",listNameID integer"
                + ",tableNameID integer"
                + ",dnID integer"
                + ",actionID integer"
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
