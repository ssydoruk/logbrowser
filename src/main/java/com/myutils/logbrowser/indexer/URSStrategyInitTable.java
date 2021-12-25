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
public class URSStrategyInitTable extends DBTable {

    public URSStrategyInitTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"URSSTRINIT_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
        addIndex("time");
        addIndex("FileId");
        addIndex("ConnIDID");
        addIndex("ixnidID");
        addIndex("strategymsgid");
        addIndex("strategynameid");
        dropIndexes();

        String query = "create table if not exists URSSTRINIT_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",ConnIDID INTEGER"
                + ",ixnidID INTEGER"
                + ",strategymsgid integer"
                + ",strategynameid integer"
                //                + ",cgMsg char(50)"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO URSSTRINIT_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?,"
                + "?"
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
