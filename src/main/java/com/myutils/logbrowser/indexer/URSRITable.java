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
public class URSRITable extends DBTable {

    public URSRITable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"ursri");
    }

    @Override
    public void InitDB() {

        
        addIndex("time");
        addIndex("FileId");
        addIndex("connidid");
        addIndex("clientappID");
        addIndex("clientNo");
        addIndex("ORSSidID");
        addIndex("funcID");
        addIndex("subfuncID");
        addIndex("ref");
        addIndex("UUIDID");
        addIndex("URLID");

        URSRI.addIndexes(this);

        dropIndexes();

        String query = "create table if not exists  " + getTabName() + "  (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",connidid integer"
                + ",clientappID INTEGER"
                + ",clientNo INTEGER"
                + ",ORSSidID INTEGER"
                + ",funcID INTEGER"
                + ",subfuncID INTEGER"
                + ",ref INTEGER"
                + ",inbound bit"
                + ",UUIDID INTEGER"
                + ",URLID INTEGER"
                + URSRI.getTableFields()
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO  " + getTabName() + "  VALUES(NULL,?,?,?,?,?"
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
                + URSRI.getTableFieldsAliases()
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
