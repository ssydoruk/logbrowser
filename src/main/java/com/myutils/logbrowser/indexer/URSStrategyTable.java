/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class URSStrategyTable extends DBTable {

    public URSStrategyTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, "URSSTR_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("ConnIDID");
        addIndex("ixnidID");
        addIndex("strategymsgid");
        addIndex("ref1id");
        addIndex("ref2id");
        dropIndexes();

        String query = "create table if not exists URSSTR_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",ConnIDID INTEGER"
                + ",ixnidID INTEGER"
                + ",strategymsgid integer"
                + ",ref1id integer"
                + ",ref2id integer"
                //                + ",cgMsg char(50)"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO URSSTR_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,"
                /*standard first*/
                + "?,"
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
