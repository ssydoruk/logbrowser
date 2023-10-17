/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class ConnIDTable extends DBTable {

    public ConnIDTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "connid_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");
        addIndex("ConnectionIdID");
        addIndex("HandlerID");
        addIndex("newConnIdID");
        dropIndexes();

        String query = "CREATE TABLE IF NOT EXISTS " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",ConnectionIdID INTEGER"
                + ",HandlerID INTEGER"
                + ",created bit"
                + ",isTemp bit"
                + ",newConnIdID INTEGER"
                + ")";

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

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

}
