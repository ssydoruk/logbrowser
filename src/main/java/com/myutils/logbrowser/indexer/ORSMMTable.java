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
public class ORSMMTable extends DBTable {

    public ORSMMTable(SqliteAccessor dbaccessor) {
        super(dbaccessor, TableType.ORSMMessage, "ORSMM");
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("IXNIDID");
        addIndex("refid");
        addIndex("PARENTIXNIDID");
        dropIndexes();

        String query = "create table if not exists ORSMM (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",nameID INTEGER"
                + ",IXNIDID INTEGER"
                + ",refid INTEGER"
                + ",inbound bit"
                + ",PARENTIXNIDID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO ORSMM VALUES(NULL,?,?,?,?,?"
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
    public void FinalizeDB() throws SQLException {
        createIndexes();

    }


}
