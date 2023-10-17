/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class ORSMetricTable extends DBTable {

    public ORSMetricTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "ORSmetr_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("sidid");
        addIndex("metricid");
        addIndex("reqid");
        addIndex("param1id");
        dropIndexes();

        String query = "CREATE TABLE if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "sidid INTEGER"
                + ",metricid INTEGER"
                + ",reqid INTEGER"
                + ",param1id INTEGER"
                + ");";

        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
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
