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
public class OCSPAEventInfoTable extends DBTable {

    public OCSPAEventInfoTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "OCSPAEVI_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

        addIndex("FileId");
        addIndex("time");
        addIndex("cgDBID");
        addIndex("AgentStatTypeID");
        addIndex("AgentCallTypeID");
        addIndex("recHandle");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "CGDBID INTEGER"
                + ",PlaceDBID INTEGER"
                + ",AgentDBID INTEGER"
                + ",Agent char(64)"
                + ",AgentStatTypeID integer"
                + ",AgentCallTypeID integer"
                + ",recHandle INTEGER"
                + ",callresult INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?"
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
        createIndexes();
    }


}
