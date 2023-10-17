/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class SIPMSTable extends DBTable {

    public SIPMSTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "sipms");
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("CallIDID");
        addIndex("CSeqID");
        addIndex("ToUriID");
        addIndex("FromUriID");
        addIndex("FromUserpartID");
        addIndex("ViaUriID");
        addIndex("ViaBranchID");
        addIndex("PeerIpID");
        addIndex("uuidid");
        addIndex("requestURIDNid");
        addIndex("sipuriid");
        dropIndexes();

        String query = "create table if not exists sipms (id INTEGER PRIMARY KEY ASC,"
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
        return "INSERT INTO sipms VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }


}
