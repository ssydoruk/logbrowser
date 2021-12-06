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
public class FileInfoTable extends DBTable {

    private static int filesAdded = 0;

    public FileInfoTable(DBAccessor dbaccessor) {
        super(dbaccessor, TableType.File);
    }

    public static int getFilesAdded() {
        return filesAdded;
    }

    @Override
    public void InitDB() {
        setTabName("file_" + getM_dbAccessor().getM_alias());
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
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(?,?,?,?,?,?,?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ",?"
                + ");");

    }

    @Override
    public void FinalizeDB() {
        createIndexes();

        ////throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void AddToDB(Record rec) throws SQLException {
        FileInfo fiRec = (FileInfo) rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException {
                stmt.setInt(1, rec.getRecordID());

                setFieldString(stmt, 2, fiRec.getM_path());
                stmt.setLong(3, fiRec.getM_runId());
                stmt.setInt(4, fiRec.getM_componentType().ordinal());
                setFieldString(stmt, 5, fiRec.getM_nodeId());
                filesAdded++;
                setFieldInt(stmt, 6, Main.getRef(ReferenceType.App, fiRec.getAppName()));
                setFieldString(stmt, 7, fiRec.getHost());
                setFieldInt(stmt, 8, Main.getRef(ReferenceType.AppType, fiRec.getAppType()));
                stmt.setInt(9, fiRec.getLogFileNo());
                setFieldString(stmt, 10, fiRec.getLogFileName());
                stmt.setTimestamp(11, new Timestamp(fiRec.getStartTime()));
                stmt.setLong(12, fiRec.getSize());
                stmt.setTimestamp(13, new Timestamp(fiRec.getFileStartTimeLong()));
                stmt.setTimestamp(14, new Timestamp(fiRec.getEndTime()));
                setFieldString(stmt, 15, fiRec.getArchiveName());
            }
        });
    }


}
