/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class GMSStartTable extends DBTable {

    public GMSStartTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");

        addIndex("EventNameID");
        addIndex("CallBackTypeID");
        addIndex("GMSServiceID");
        addIndex("ORSSessionID");
        addIndex("phoneID");
        addIndex("URIID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",EventNameID int"
                + ",CallBackTypeID int"
                + ",GMSServiceID int"
                + ",ORSSessionID int"
                + ",phoneID int"
                + ",isinbound bit"
                + ",URIID int"
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
