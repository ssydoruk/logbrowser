package com.myutils.logbrowser.indexer;

import java.sql.SQLException;

/**
 * @author akolo
 */
public class StSActionTable extends DBTable {

    String m_MessageName;

    public StSActionTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "STSACTION_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("typeid");
        addIndex("valueid");
        addIndex("connidid");
        dropIndexes();

        String query = "create table if not exists STSACTION_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameid integer,"
                + "typeid integer,"
                + "valueid integer,"
                + "connidid integer,"
                + "FileId INT,"
                + "FileOffset BIGINT,"
                + "FileBytes INT,"
                + "line INT);";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO STSACTION_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,?,?,?);";
    }


    @Override
    public void FinalizeDB() throws SQLException {
        createIndexes();

    }

}
