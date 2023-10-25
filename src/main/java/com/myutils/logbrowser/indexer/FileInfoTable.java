/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 * @author ssydoruk
 */
public class FileInfoTable extends DBTable {

    private static int filesAdded = 0;

    public FileInfoTable(SqliteAccessor dbaccessor) {
        super(dbaccessor, TableType.File, "file_" + dbaccessor.getM_alias());
    }

    public static void incFilesAdded() {
        filesAdded++;
    }

    public static int getFilesAdded() {
        return filesAdded;
    }

    @Override
    public void InitDB() {
        setFileIDField("id");
        addIndex("name");
        addIndex("Component");
        addIndex("AppNameID");
        addIndex("appTypeID");
        addIndex("intfilename");
        addIndex("size");
        addIndex("RunID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "name CHAR(255), RunID BIGINT, Component INTEGER, NodeId CHAR(8) "
                + ",AppNameID integer "
                + ",HostName char(255)"
                + ",appTypeID integer"
                + ",fileno integer"
                + ",intfilename char(255)"
                + ",starttime timestamp"
                + ",size bigint"
                + ",filestarttime timestamp"
                + ",endtime timestamp"
                + ",arcname char(255)"
                + ");";
        getM_dbAccessor().runQuery(query);
    }

    @Override
    public String getInsert() {
        return "INSERT INTO " + getTabName() + " VALUES(?,?,?,?,?,?,?"
                + ",?"
                + ",?"
                + ",?"
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
