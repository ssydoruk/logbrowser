/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class URSTargetSetTable extends DBTable {

    public URSTargetSetTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "urstargetset");
    }

    @Override
    public void InitDB() {


        addIndex("time");
        addIndex("FileId");
        addIndex("targetid");
        addIndex("objectid");
        addIndex("objectTypeID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + "(id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",targetid INTEGER"
                + ",objectid INTEGER"
                + ",objectTypeID INTEGER"
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
