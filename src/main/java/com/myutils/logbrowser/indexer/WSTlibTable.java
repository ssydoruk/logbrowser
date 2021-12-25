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
public class WSTlibTable extends DBTable {

    public WSTlibTable(SqliteAccessor dbaccessor, TableType t) {

        super(dbaccessor, t,"WSTlib");
    }

    @Override
    public void InitDB() {
        
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("thisDNID");
        addIndex("otherDNID");
        addIndex("agentIDID");
        addIndex("ConnectionIDID");
        addIndex("TransferIdID");
        addIndex("ReferenceId");
        addIndex("uuidid");
        addIndex("IXNIDID");
        addIndex("sourceID");
        dropIndexes();

        String query = "create table if not exists WSTlib (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentIDID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "ReferenceId INTEGER,"
                + "Inbound bit,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "HandlerId INTEGER,"
                + "seqno integer"
                + ",line int"
                + ",uuidid integer"
                + ",IXNIDID integer"
                + ",sourceID integer"
                + ",ServerHandle integer"
                + ",refID integer"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO WSTlib VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }


}
