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
public class OCSClientTable extends DBTable {

    public OCSClientTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        if (Main.isDbExisted()) {
            getM_dbAccessor().runQuery("drop index if exists OCSClientDBfileid;");
            getM_dbAccessor().runQuery("drop index if exists OCSClientnameID ;");
            getM_dbAccessor().runQuery("drop index if exists OCSClientCampaignDBID;");
            getM_dbAccessor().runQuery("drop index if exists OCSClientGroupDBID;");
            getM_dbAccessor().runQuery("drop index if exists OCSClientunixtime;");

        }
        String query = "create table if not exists OCSClient (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",nameID INTEGER"
                + ",CampaignDBID INTEGER"
                + ",GroupDBID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
        return "INSERT INTO OCSClient VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
        getM_dbAccessor().runQuery("create index if not exists OCSClientDBfileid on OCSClient (FileId);");
        getM_dbAccessor().runQuery("create index if not exists OCSClientnameID  on OCSClient (nameID);");
        getM_dbAccessor().runQuery("create index if not exists OCSClientCampaignDBID on OCSClient (CampaignDBID);");
        getM_dbAccessor().runQuery("create index if not exists OCSClientGroupDBID on  OCSClient (GroupDBID);");
        getM_dbAccessor().runQuery("create index if not exists OCSClientunixtime on  OCSClient (time);");
    }


}
