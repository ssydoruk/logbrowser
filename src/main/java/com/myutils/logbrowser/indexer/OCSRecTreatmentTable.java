/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class OCSRecTreatmentTable extends DBTable {

    public OCSRecTreatmentTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists recHandle_OCSRECCR;");
            getM_dbAccessor().runQuery("drop index if exists chID_OCSRECCR;");
            getM_dbAccessor().runQuery("drop index if exists FileId_OCSRECCR;");
            getM_dbAccessor().runQuery("drop index if exists campBID_OCSRECCR;");
            getM_dbAccessor().runQuery("drop index if exists campBID_unixtime;");

        }
        String query = "create table if not exists OCSTREAT (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",recHandle integer"
                + ",chID integer"
                + ",chNum integer"
                + ",campBID integer"
                + ",recType integer"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO OCSTREAT VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
        getM_dbAccessor().runQuery("create index if not exists recHandle_OCSRECCR on OCSTREAT (recHandle);");
        getM_dbAccessor().runQuery("create index if not exists chID_OCSRECCR on OCSTREAT (chID);");
        getM_dbAccessor().runQuery("create index if not exists FileId_OCSRECCR on OCSTREAT (FileId);");
        getM_dbAccessor().runQuery("create index if not exists campBID_OCSRECCR on OCSTREAT (campBID);");
        getM_dbAccessor().runQuery("create index if not exists campBID_unixtime on OCSTREAT (time);");
    }


}
