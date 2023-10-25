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
public class OCSDBActivityTable extends DBTable {

    public OCSDBActivityTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "OCSDB_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

        addIndex("campaignDBID");
        addIndex("groupDBID");
        addIndex("FileId");
        addIndex("chDBID");
        addIndex("DBREQID");
        addIndex("DBSERVERID");
        addIndex("time");
        addIndex("listDBID");
        addIndex("listnameID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",listDBID INTEGER"
                + ",campaignDBID INTEGER"
                + ",groupDBID INTEGER"
                + ",REQID INTEGER"
                + ",chDBID INTEGER"
                + ",DBREQID INTEGER"
                + ",DBSERVERID INTEGER"
                + ",listnameID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
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
    public void FinalizeDB() throws SQLException {
        createIndexes();
    }


}
