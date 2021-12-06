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
public class ORSSidIxnIDTable extends DBTable {

    public ORSSidIxnIDTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("ORSsessIxn");
        addIndex("time");
        addIndex("FileId");
        addIndex("sidid");
        addIndex("ixnid");
        addIndex("URLID");
        addIndex("appID");
        dropIndexes();

        String query = "CREATE TABLE if not exists ORSsessIxn (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "sidid INTEGER,"
                + "ixnid INTEGER,"
                + "URLID INTEGER,"
                + "appID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
//        accessor.runQuery("create index if not exists ORSsess__HndId_" + m_alias +" on ORSmetr_" + m_alias + " (HandlerId);");

        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO ORSsessIxn VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?,"
                + "?,"
                + "?"
                + ");");

    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {

        OrsSidIxnID rec = (OrsSidIxnID) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ORSSID, rec.getGID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, rec.getIxnID()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.URSStrategyName, rec.getURL()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.URSStrategyName, rec.getApp()));
                        }
        });
    }



}
