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
public class URSCONNIDSIDTable extends DBTable {

    URSCONNIDSIDTable(SqliteAccessor m_accessor, TableType tableType) {
        super(m_accessor, tableType);
    }

    @Override
    public void InitDB() {
        setTabName("URSCONNIDSID");
        addIndex("time");
        addIndex("FileId");
        addIndex("connidid");
        addIndex("sidid");
        dropIndexes();

        String query = "create table if not exists URSCONNIDSID (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",connidid integer"
                + ",sidid INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO URSCONNIDSID VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ");");

    }

    /**
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
        public void AddToDB(Record _rec) throws SQLException {
        URSCONNIDSID rec = (URSCONNIDSID) _rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, rec.GetConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.ORSSID, rec.GetSid()));

            }
        });
    }


}
