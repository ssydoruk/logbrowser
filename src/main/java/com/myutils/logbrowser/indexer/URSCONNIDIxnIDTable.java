/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class URSCONNIDIxnIDTable extends DBTable {

    public URSCONNIDIxnIDTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "URSConnIDIxnID");
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("connid");
        addIndex("ixnid");
        dropIndexes();

        String query = "CREATE TABLE if not exists URSConnIDIxnID (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "connid INTEGER,"
                + "ixnid INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
//        accessor.runQuery("create index if not exists ORSsess__HndId_" + m_alias +" on ORSmetr_" + m_alias + " (HandlerId);");

    }

    @Override
    public String getInsert() {
        return "INSERT INTO URSConnIDIxnID VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?"
                + ");";

    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

}
