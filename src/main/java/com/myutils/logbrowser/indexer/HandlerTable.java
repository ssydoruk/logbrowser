/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public class HandlerTable extends DBTable {

    public HandlerTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"handler_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        addIndex("textid");
        addIndex("FileId");
        dropIndexes();

        String query = "create table if not exists handler_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "textid int,"
                + "FileId INTEGER"
                //                + ","
                //                + "FileOffset bigint,"
                //                + "Line INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
return"INSERT INTO handler_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?"
                //                + ",?,?"
                + ");";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    }
