/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.SQLException;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class OrsHTTPTable extends DBTable {

    public OrsHTTPTable(SqliteAccessor dbaccessor) {
        super(dbaccessor, TableType.ORSHTTP, "orshttp");
    }

    @Override
    public void InitDB() {


        addIndex("time");
        addIndex("FileId");

        addIndex("sidID");
        addIndex("GMSServiceID");
        addIndex("URIID");
        addIndex("param1ID");
        addIndex("param2ID");
        dropIndexes();

        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists orshttp_FileId;");
            getM_dbAccessor().runQuery("drop index if exists orshttp_sidID;");
            getM_dbAccessor().runQuery("drop index if exists orshttp_unixtime;");

        }
        String query = "create table if not exists orshttp (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",socket int"
                + ",http_bytes int"
                + ",ipID int"
                + ",sidID int"
                + ",namespaceID int"
                + ",Inbound bit"
                + ",GMSServiceID int"
                + ",URIID int"
                + ",param1ID int"
                + ",param2ID int"
                + ",httpresponse bigint"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO orshttp VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
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
    public void FinalizeDB() throws SQLException {
        createIndexes();
    }


}
