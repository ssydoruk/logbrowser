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
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class URSRlibTable extends DBTable {

    public URSRlibTable(SqliteAccessor dbaccessor) {
        super(dbaccessor, TableType.URSRlib);
    }

    URSRlibTable(SqliteAccessor m_accessor, TableType tableType) {
        super(m_accessor, tableType,"URSRLIB");
    }

    @Override
    public void InitDB() {
        
        addIndex("time");
        addIndex("FileId");

        addIndex("refid");
        addIndex("reqid");
        addIndex("uuidid");
        addIndex("sidid");
        addIndex("methodid");
        addIndex("sourceid");
        addIndex("resultid");
        addIndex("paramid");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",refid INTEGER"
                + ",reqid INTEGER"
                + ",uuidid INTEGER"
                + ",sidid INTEGER"
                //                + ",moduleid INTEGER"
                + ",methodid INTEGER"
                + ",inbound bit"
                + ",sourceid INTEGER"
                + ",resultid INTEGER"
                + ",paramid INTEGER"
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
                //                + ",?"
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
