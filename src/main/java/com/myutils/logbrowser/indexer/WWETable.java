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
 */
public class WWETable extends DBTable {

    public WWETable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "WWETEvents");
    }

    @Override
    public void InitDB() {
        
        addIndex("time");
        addIndex("FileId");
        addIndex("ConnectionIDID");
        addIndex("TransferIdID");
        addIndex("UUIDID");
        addIndex("thisDNID");
        addIndex("nameID");
        addIndex("seqno");
        addIndex("IXNIDID");
        addIndex("ServerID");
        addIndex("otherDNID");
        addIndex("agentIDID");
        addIndex("DNISID");
        addIndex("ANIID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC" + ",time timestamp" + ",FileId INTEGER" + ",FileOffset bigint" + ",FileBytes int" + ",line int"
                /* standard first */ + ",ConnectionIDID int" + ",TransferIdID int" + ",UUIDID int" + ",referenceID int" + ",inbound bit" + ",thisDNID int" + ",nameID int" + ",seqno bit" + ",IXNIDID bit" + ",ServerID bit" + ",otherDNID int" + ",agentIDID int" + ",DNISID int" + ",ANIID int" + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/ + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ",?" + ");";

    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }


}
