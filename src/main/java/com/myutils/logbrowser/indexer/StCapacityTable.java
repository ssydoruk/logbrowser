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
public class StCapacityTable extends DBTable {

    private static final int MAX_MEDIA = 5;

    public StCapacityTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "stcapacity");
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");

        /* standard first */
        addIndex("typeid");
        addIndex("agentid");
        addIndex("placeid");
        addIndex("dnid");
        addIndex("capacityid");
        for (int i = 0; i < MAX_MEDIA; i++) {
            addIndex("media" + i + "nameid");
        }
        dropIndexes();

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < MAX_MEDIA; i++) {
            buf.append(",media").append(i).append("nameid integer");
            buf.append(",media").append(i).append("state character(2)");
            buf.append(",media").append(i).append("curnumber tinyint");
            buf.append(",media").append(i).append("maxnumber character(2)");
        }

        String query = "create table if not exists stcapacity (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",typeid integer"
                + ",agentid integer"
                + ",placeid integer"
                + ",dnid integer"
                + ",capacityid integer"
                + buf
                + ");";

        buf = new StringBuilder();
        for (int i = 0; i < MAX_MEDIA; i++) {
            buf.append(",?,?,?,?");
        }

        getM_dbAccessor().runQuery(query);


    }

    @Override
    public String getInsert() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < MAX_MEDIA; i++) {
            buf.append(",?,?,?,?");
        }
        return "INSERT INTO stcapacity VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + buf
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
