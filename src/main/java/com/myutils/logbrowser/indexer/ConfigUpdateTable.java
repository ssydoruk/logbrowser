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
public class ConfigUpdateTable extends DBTable {

    public ConfigUpdateTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "cfg");
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");

        addIndex("opid");
        addIndex("objtypeID");
        addIndex("objDBID");
        addIndex("objNameID");
        addIndex("msgID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",opid integer"
                + ",objtypeID INTEGER"
                + ",objDBID INTEGER"
                + ",objNameID INTEGER"
                + ",msgID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(?,?,?,?,?,?"
                /*standard first*/
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
