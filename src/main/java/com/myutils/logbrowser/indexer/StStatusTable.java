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
public class StStatusTable extends DBTable {

    public StStatusTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "ststatus");
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex("typeid");
        addIndex("agentid");
        addIndex("placeid");
        addIndex("oldstatusid");
        addIndex("newstatusid");
        addIndex("dnid");
        dropIndexes();

        String query = "create table if not exists ststatus (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",typeid integer"
                + ",agentid integer"
                + ",placeid integer"
                + ",oldstatusid integer"
                + ",newstatusid integer"
                + ",dnid integer"
                + ");";
        getM_dbAccessor().runQuery(query);

    }

    @Override
    public String getInsert() {
        return "INSERT INTO ststatus VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
    public void FinalizeDB() throws Exception {
        createIndexes();
    }


}
