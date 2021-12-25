/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import static com.myutils.logbrowser.indexer.Message.transformDN;

/**
 * @author ssydoruk
 */
public class SIPEPTable extends DBTable {

    public SIPEPTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "sipep");
    }

    @Override
    public void InitDB() {


        addIndex("CallIDID");
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("ViaBranchID");
        addIndex("FromUserpartID");
        addIndex("requestURIDNid");
        addIndex("uuidid");
        addIndex("sipuriid");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "inbound bit,"
                + "nameID int,"
                + "CallIDID int,"
                + "CSeqID int,"
                + "ToUriID int,"
                + "FromUriID int,"
                + "FromUserpartID int,"
                + "ViaUriID int,"
                + "ViaBranchID int,"
                + "PeerIpID int,"
                + "PeerPort int,"
                + "FileId int,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "callRelated bit,"
                + "line int,"
                + "uuidid int,"
                + "requestURIDNid,"
                + "sipuriid int"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();

    }
}

