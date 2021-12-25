/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public class TriggerTable extends DBTable {

    public TriggerTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "trigger_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

//        addIndex("time");
        addIndex("FileId");
        addIndex("textid");
        addIndex("HandlerId");
        addIndex("SipId");
        addIndex("TlibId");
        addIndex("JsonId");
        dropIndexes();

        String query = "create table if not exists trigger_" + db_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "textid INTEGER,"
                + "HandlerId INTEGER,"
                + "SipId INTEGER,"
                + "TlibId INTEGER,"
                + "JsonId INTEGER,"
                + "FileId INTEGER"
                + ","
                + "FileOffset bigint,"
                + "Line INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO trigger_" + db_alias() + " VALUES(NULL,?,?,?,?,?,?,?,?);";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }


}
