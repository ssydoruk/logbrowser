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
public class SIPTable extends DBTable {

    public SIPTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"sip_" + dbaccessor.getM_alias());
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
        addIndex("HandlerId");
        addIndex("uuidid");
        addIndex("requestURIDNid");
        dropIndexes();

        String query = "create table if not exists sip_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
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
                + "HandlerId int,"
                + "callRelated bit,"
                + "line int,"
                + "uuidid int,"
                + "requestURIDNid"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO sip_" + getM_dbAccessor().getM_alias() + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

}
