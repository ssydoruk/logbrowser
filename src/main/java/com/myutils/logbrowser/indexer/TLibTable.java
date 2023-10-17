/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class TLibTable extends DBTable {

    public TLibTable(SqliteAccessor dbaccessor, TableType type) {
        super(dbaccessor, type, "tlib_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

        addIndex("ConnectionIDID");
        addIndex("TransferIDID");
        addIndex("HandlerId");
        addIndex("time");
        addIndex("uuidID");
        addIndex("thisDNID");
        addIndex("otherDNID");
        addIndex("nameID");
        addIndex("FileId");
        addIndex("seqno");
        addIndex("agentIDID");
        addIndex("ReferenceId");
        addIndex("SourceID");
        addIndex("errMessageID");
        addIndex("line");
        addIndex("attr1ID");
        addIndex("attr2ID");
        addIndex("DNISID");
        addIndex("ANIID");
        addIndex("PlaceID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentIDID INTEGER,"
                + "aniID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "ReferenceId INTEGER,"
                + "Inbound bit,"
                + "SourceID INTEGER,"
                + "MsgID varchar(16),"
                + "LocationID INTEGER,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "HandlerId INTEGER,"
                + "line int,"
                + "seqno integer"
                + ",uuidID INTEGER"
                + ",err integer"
                + ",calltype integer"
                + ",errMessageID integer"
                + ",attr1ID integer"
                + ",attr2ID integer"
                + ",DNISID integer"
                + ",PlaceID integer"
                + ");";

        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }


}
