/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class OCSSCXMLScriptTable extends DBTable {

    public OCSSCXMLScriptTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists FileId_OCSSXMLScript;");
            getM_dbAccessor().runQuery("drop index if exists metricid_OCSSXMLScript;");
            getM_dbAccessor().runQuery("drop index if exists SessIDID_OCSSXMLScript;");
            getM_dbAccessor().runQuery("drop index if exists SessIDID_unixtime;");

        }
        String query = "create table if not exists OCSSXMLScript (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",SessIDID INTEGER"
                + ",metricid INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO OCSSXMLScript VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ");";
    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        getM_dbAccessor().runQuery("create index if not exists FileId_OCSSXMLScript on OCSSXMLScript (FileId);");
        getM_dbAccessor().runQuery("create index if not exists metricid_OCSSXMLScript on OCSSXMLScript (metricid);");
        getM_dbAccessor().runQuery("create index if not exists SessIDID_OCSSXMLScript on OCSSXMLScript (SessIDID);");
        getM_dbAccessor().runQuery("create index if not exists SessIDID_unixtime on OCSSXMLScript (time);");

    }


}
