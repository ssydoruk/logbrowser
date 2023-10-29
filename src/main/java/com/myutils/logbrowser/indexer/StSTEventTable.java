package com.myutils.logbrowser.indexer;

/**
 * @author akolo
 */
public class StSTEventTable extends DBTable {

    String m_MessageName;

    public StSTEventTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "STSTEVENT_" + dbaccessor.getM_alias());
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("thisDNID");
        addIndex("switchID");
        addIndex("TServerID");
        addIndex("otherDNID");
        addIndex("ConnectionIDID");
        addIndex("TransferIdID");
        addIndex("ReferenceId");
        addIndex("errMessageID");
        dropIndexes();

        //                        ",dec_conn_id CHAR(32)" +
        //                        ",dec_transfer_id CHAR(32)" +
        String query = "create table if not exists STSTEVENT_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "switchID INTEGER,"
                + "TServerID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "ReferenceId INTEGER,"
                + "FileId INT,"
                + "FileOffset BIGINT,"
                + "FileBytes INT,"
                + "line INT,"
                + "errMessageID integer"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO STSTEVENT_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?"
                + ",?,?"
                //                + ",?,?"
                + ",?,?,?,?,?,?,?);";

    }


}
