/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class VOIPEPTable extends DBTable {

    public VOIPEPTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
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
