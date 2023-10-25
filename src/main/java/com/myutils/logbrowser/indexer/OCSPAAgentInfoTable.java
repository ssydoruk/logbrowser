/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.SQLException;

/**
 * @author ssydoruk
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class OCSPAAgentInfoTable extends DBTable {

    public OCSPAAgentInfoTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "OcsPAAgI_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

        addIndex("FileId");
        addIndex("cgDBID");
        addIndex("recHandle");
        addIndex("time");
        addIndex("agentNameID");
        addIndex("AgentStatTypeID");
        addIndex("AgentCallTypeID");
        dropIndexes();

        String query = "create table  if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "CGDBID INTEGER"
                + ",PlaceDBID INTEGER"
                + ",AgentDBID INTEGER"
                + ",agentNameID INTEGER"
                + ",AgentStatTypeID INTEGER"
                + ",AgentCallTypeID INTEGER"
                + ",recHandle INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?,"
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
    public void FinalizeDB() throws SQLException {
        createIndexes();
    }

}
