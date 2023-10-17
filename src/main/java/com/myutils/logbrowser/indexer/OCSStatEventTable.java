/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class OCSStatEventTable extends DBTable {

    public OCSStatEventTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_fileid_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_time_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_StatEvNameID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_agentNameID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_placeNameID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_DNID_" + getM_dbAccessor().getM_alias() + ";");
            getM_dbAccessor().runQuery("drop index if exists OcsStatEv_StatTypeID_" + getM_dbAccessor().getM_alias() + ";");

        }
        String query = "create table if not exists OcsStatEv_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",StatEvNameID INTEGER"
                + ",agentNameID INTEGER"
                + ",agentDBID INTEGER"
                + ",placeNameID INTEGER"
                + ",placeDBID INTEGER"
                + ",DNID INTEGER"
                + ",StatTypeID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO OcsStatEv_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");";
    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_fileid_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (FileId);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_time_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (time);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_StatEvNameID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (StatEvNameID);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_agentNameID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (agentNameID);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_placeNameID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (placeNameID);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_DNID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (DNID);");
        getM_dbAccessor().runQuery("create index if not exists OcsStatEv_StatTypeID_" + getM_dbAccessor().getM_alias() + " on OcsStatEv_" + getM_dbAccessor().getM_alias() + " (StatTypeID);");
    }


}
