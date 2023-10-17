/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class URSTlibTable extends DBTable {

    public URSTlibTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "URS_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

        addIndex("ConnectionIDID");
        addIndex("TransferIdID");
        addIndex("UUIDID");
        addIndex("time");
        addIndex("thisDNID");
        addIndex("nameID");
        addIndex("seqno");
        addIndex("IXNIDID");
        addIndex("SourceID");
        addIndex("ServerHandle");
        addIndex("errMessageID");
        addIndex("referenceID");
        addIndex("attr1ID");
        addIndex("attr2ID");
        addIndex("DNISID");
        addIndex("agentID");
        addIndex("ANIID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "Inbound bit,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "seqno integer"
                + ",line int"
                + ",uuidid integer"
                + ",IXNIDID integer"
                + ",SourceID integer"
                + ",ServerHandle integer"
                + ",referenceID integer"
                + ",errMessageID integer"
                + ",attr1ID integer"
                + ",attr2ID integer"
                + ",DNISID integer"
                + ",ANIID integer"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

    }

    @Override
    public void FinalizeDB() {
//            getM_dbAccessor().runQuery("create index if not exists URS_time_" + getM_dbAccessor().getM_alias() +" on URS_" + getM_dbAccessor().getM_alias() + " (time);");
        createIndexes();

        getM_dbAccessor().runQuery("create temp table tmp_urs_server as select distinct SourceID, ServerHandle from "
                + getTabName() + " where SourceID > 0 limit 1;");

        getM_dbAccessor().runQuery("update " + getTabName()
                + " set SourceID = (select SourceID from tmp_urs_server t where t.ServerHandle=ServerHandle) where SourceID<=0 ");

    }


}
