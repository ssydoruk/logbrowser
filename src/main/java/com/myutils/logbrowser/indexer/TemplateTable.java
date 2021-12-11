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
 * <p>
 * <p>
 * Make Refactor/Copy of this class for new table type
 */
public class TemplateTable extends DBTable {

    public TemplateTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"cfg");
    }

    @Override
    public void InitDB() {
        
        addIndex("time");
        addIndex("FileId");

        addIndex("HandlerId");

        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",name char(64)"
                + ",HandlerId INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(?,?,?,?,?,?"
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
        ORSMetric rec = (ORSMetric) _rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
             stmt.setInt(1, rec.getRecordID());
            stmt.setTimestamp(2, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(3, rec.getFileID());
            stmt.setLong(4, rec.getM_fileOffset());
            stmt.setLong(5, rec.getM_FileBytes());
            stmt.setLong(6, rec.getM_line());

//            stmt.setInt(7, ORSMetric.m_handlerId);
            setFieldString(stmt, 8, rec.sid);

            }
        });
    }


}
