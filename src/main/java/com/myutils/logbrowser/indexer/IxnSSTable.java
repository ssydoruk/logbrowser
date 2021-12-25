/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import static java.sql.Types.INTEGER;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class IxnSSTable extends DBTable {

    public IxnSSTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"IxnSS");
    }

    @Override
    public void InitDB() {
        addIndex("nameID");
        addIndex("IxnID");
        addIndex("PARENTIXNIDID");
        addIndex("MediaTypeID");
        addIndex("IxnQueueID");
        addIndex("RefId");
        addIndex("connIDID");
        addIndex("AgentID");
        addIndex("PlaceID");
        addIndex("RouterID");
        addIndex("ServiceID");
        addIndex("MethodID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
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
                + ",AgentID INTEGER"
                + ",PlaceID INTEGER"
                + ",RouterID INTEGER"
                + ",serverRef INTEGER"
                + ",clientRef INTEGER"
                + ",ServiceID INTEGER"
                + ",MethodID INTEGER"
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
    public void FinalizeDB() throws Exception {
        createIndexes();
    }


}
