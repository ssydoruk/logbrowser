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
public class IxnTable extends DBTable {

    public IxnTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "Ixn");
    }

    @Override
    public void InitDB() {
        addIndex("FileId");
        addIndex("time");
        addIndex("nameID");
        addIndex("IxnID");
        addIndex("PARENTIXNIDID");
        addIndex("MediaTypeID");
        addIndex("IxnQueueID");
        addIndex("RefId");
        addIndex("connIDID");
        addIndex("appid");
        addIndex("AgentID");
        addIndex("PlaceID");
        addIndex("attr1ID");
        addIndex("attr2ID");
        addIndex("strategyID");
        dropIndexes();

        String query = "create table if not exists Ixn (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",nameID INTEGER"
                + ",IxnID INTEGER"
                + ",MediaTypeID INTEGER"
                + ",IxnQueueID INTEGER"
                + ",RefId INTEGER"
                + ",connIDID INTEGER"
                + ",inbound bit"
                + ",PARENTIXNIDID INTEGER"
                + ",appid INTEGER"
                + ",AgentID INTEGER"
                + ",PlaceID INTEGER"
                + ",attr1ID INTEGER"
                + ",attr2ID INTEGER"
                + ",strategyID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO Ixn VALUES(NULL,?,?,?,?,?"
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
