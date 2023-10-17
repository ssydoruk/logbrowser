package com.myutils.logbrowser.indexer;

/**
 * @author akolo
 */
public class StSRequestHistoryTable extends DBTable {

    public StSRequestHistoryTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t, "STSREQHISTORY_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {

        addIndex("time");
        addIndex("FileId");
        addIndex(new String[]{"clnameid", "rid"});
        addIndex(new String[]{"cluid", "rid"});
        addIndex("actionid");
        addIndex("clnameid");
        dropIndexes();

        String query = "create table if not exists STSREQHISTORY_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "rid INT,"
                + "urid INT,"
                + "arid INT,"
                + "clnameid integer,"
                + "cluid CHAR(64),"
                + "time timestamp,"
                + "actionid integer,"
                + "FileId INT,"
                + "FileOffset BIGINT,"
                + "FileBytes INT,"
                + "HandlerId INT,"
                + "line INT);";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO STSREQHISTORY_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?);";
    }


    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

}
