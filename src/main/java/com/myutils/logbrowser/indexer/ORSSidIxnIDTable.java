/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class ORSSidIxnIDTable extends DBTable {

    public ORSSidIxnIDTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "ORSsessIxn");
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("sidid");
        addIndex("ixnid");
        addIndex("URLID");
        addIndex("appID");
        dropIndexes();

        String query = "CREATE TABLE if not exists ORSsessIxn (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "sidid INTEGER,"
                + "ixnid INTEGER,"
                + "URLID INTEGER,"
                + "appID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
//        accessor.runQuery("create index if not exists ORSsess__HndId_" + m_alias +" on ORSmetr_" + m_alias + " (HandlerId);");


    }

    @Override
    public String getInsert() {
        return "INSERT INTO ORSsessIxn VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?,"
                + "?,"
                + "?"
                + ");";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }


}
