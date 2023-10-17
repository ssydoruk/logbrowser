/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class OCSCGTable extends DBTable {

    public OCSCGTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "OCSCG_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {


        addIndex("time");
        addIndex("FileId");
        addIndex("cgDBID");
        addIndex("cgNameID");
        addIndex("GroupDBID");
        addIndex("CampaignDBID");
        addIndex("groupNameID");
        addIndex("campaignNameID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",cgDBID INTEGER"
                + ",cgNameid INTEGER"
                + ",GroupDBID INTEGER"
                + ",CampaignDBID INTEGER"
                + ",groupNameID INTEGER"
                + ",campaignNameID INTEGER"
                //                + ",cgMsg char(50)"
                + ");";
        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
        return "INSERT INTO OCSCG_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                //                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "?"
                + ");";
    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }


}
