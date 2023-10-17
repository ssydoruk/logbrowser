/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class URSVQTable extends DBTable {

    public URSVQTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "ursvq");
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("ConnIDID");
        addIndex("vqidid");
        addIndex("vqnameid");
        addIndex("targetid");
        dropIndexes();

        String query = "create table if not exists ursvq (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",ConnIDID INTEGER"
                + ",vqidid INTEGER"
                + ",vqnameid INTEGER"
                + ",targetid INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO ursvq VALUES(NULL,?,?,?,?,?,"
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
