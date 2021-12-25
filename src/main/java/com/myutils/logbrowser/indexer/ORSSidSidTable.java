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
public class ORSSidSidTable extends DBTable {

    public ORSSidSidTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        addIndex("sidid");
        addIndex("newsidid");
        addIndex("FileId");

        dropIndexes();

        String query = "CREATE TABLE if not exists ORSsidsid ("
                + "FileId INTEGER,"
                + "sidid INTEGER,"
                + "newsidid INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
//        accessor.runQuery("create index if not exists ORSsess__HndId_" + m_alias +" on ORSmetr_" + m_alias + " (HandlerId);");



    }

    @Override
    public String getInsert() {
        return "INSERT INTO ORSsidsid VALUES("
                + "?,"
                + "?,"
                + "?"
                + ");";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }



}
