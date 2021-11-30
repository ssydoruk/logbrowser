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
public class URSCONNIDIxnIDTable extends DBTable {

    public URSCONNIDIxnIDTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("URSConnIDIxnID");
        addIndex("time");
        addIndex("FileId");
        addIndex("connid");
        addIndex("ixnid");
        dropIndexes();

        String query = "CREATE TABLE if not exists URSConnIDIxnID (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "connid INTEGER,"
                + "ixnid INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
//        accessor.runQuery("create index if not exists ORSsess__HndId_" + m_alias +" on ORSmetr_" + m_alias + " (HandlerId);");

        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO URSConnIDIxnID VALUES(NULL,?,?,?,?,?,"
                /*standard first*/
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

        URSCONNIDIxnID rec = (URSCONNIDIxnID) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, URSCONNIDIxnID.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, rec.getConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, rec.getIxnID()));
                }
         });
    }



}
