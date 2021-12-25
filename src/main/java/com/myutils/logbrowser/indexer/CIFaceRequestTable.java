/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public class CIFaceRequestTable extends DBTable {

    public CIFaceRequestTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "cireq_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("thisDNID");
        addIndex("otherDNID");
        addIndex("HandlerId");
        addIndex(new String[]{"nameid", "refid", "thisdnid", "otherdnid"});

        dropIndexes();

        String query = "CREATE TABLE IF NOT EXISTS cireq_" + getM_dbAccessor().getM_alias() + " ("
                + "id INTEGER PRIMARY KEY ASC,"
                + "nameID INTEGER,"
                + "refId bigint,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "HandlerId INTEGER,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "Line INTEGER);";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO cireq_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,?,?);";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();

    }


}
