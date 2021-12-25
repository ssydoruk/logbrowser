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
public class JsonTable extends DBTable {

    public JsonTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"json_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
        addIndex("FileId");
        addIndex("origUriID");
        addIndex("destUriID");
        addIndex("handlerID");
        addIndex("time");
        addIndex("SipsReqId");
        addIndex("SipId");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "inbound bit,"
                + "origUriID integer,"
                + "destUriID integer,"
                + "SipsReqId int,"
                + "FileId int,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "SipId int,"
                + "line int,"
                + "handlerID int"
                + ");";
        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?);";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }



}
