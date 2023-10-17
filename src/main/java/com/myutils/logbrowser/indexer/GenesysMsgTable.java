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
public class GenesysMsgTable extends DBTable {

    public GenesysMsgTable(SqliteAccessor dbaccessor, TableType type) {
        super(dbaccessor, type, type.toString());
    }

    private String tabName() {
        return getM_type().toString();
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("levelID");
        addIndex("MSGID");
        dropIndexes();

        String query = "create table if not exists " + tabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",levelID int2"
                + ",MSGID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
        return "INSERT INTO  " + tabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ");";
    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_fileid;");
        getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_unixtime;");
        getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_levelID;");
        getM_dbAccessor().runQuery("drop index if exists " + tabName() + "_MSGID;");
    }


}
