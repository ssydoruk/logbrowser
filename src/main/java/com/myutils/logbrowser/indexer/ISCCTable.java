/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author ssydoruk
 */
public class ISCCTable extends DBTable {

    public ISCCTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"ISCC_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("thisDNID");
        addIndex("otherDNID");
        addIndex("ConnectionIDID");
        addIndex("ReferenceId");
        addIndex("FileId");
        addIndex("srcTServerID");
        addIndex("srcSwitchID");
        addIndex("otherConnectionIDID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "ReferenceId bigint,"
                + "Inbound bit,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "HandlerId INTEGER"
                + ",line int"
                + ",srcTServerID int"
                + ",srcSwitchID int"
                + ",otherConnectionIDID int"
                + ");";
        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");";
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }


}
