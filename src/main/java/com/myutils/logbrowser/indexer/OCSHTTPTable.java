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
public class OCSHTTPTable extends DBTable {

    public OCSHTTPTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "OCSHTTP");
    }

    @Override
    public void InitDB() {

        addIndex("FileId");
        addIndex("RecHandle");
        addIndex("ReqNameID");
        addIndex("time");
        addIndex("campNameID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",ReqNameID INTEGER"
                + ",RecHandle INTEGER"
                + ",isinbound bit"
                + ",campNameID INTEGER"
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
