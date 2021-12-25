/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;


/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class ProxiedTable extends DBTable {

    public ProxiedTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"proxied_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("refid");
        addIndex("destID");
        addIndex("sourceID");
        addIndex("thisdnID");
        addIndex("tlib_id");
        addIndex("HandlerId");
        dropIndexes();

        String query = "create table if not exists proxied_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",refid int"
                + ",nameID int"
                + ",destID int"
                + ",sourceID int"
                + ",thisdnID int"
                + ",tlib_id INTEGER"
                + ",HandlerId INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO proxied_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");";
    }

    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }


}
