/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author ssydoruk
 */
public class OrsUrsTable extends DBTable {

    public OrsUrsTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "ORSURS_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
        addIndex("time");
        addIndex("FileId");
        addIndex("uuidid");
        addIndex("sidid");
        addIndex("event");
        addIndex("refid");
        addIndex("reqid");
        addIndex("moduleid");
        addIndex("methodid");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + "(id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "inbound BIT,"
                + "sidid int,"
                + "uuidid int,"
                + "event BIGINT,"
                + "refid int,"
                + "reqid int,"
                + "moduleid int,"
                + "methodid int"
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




    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }
}
