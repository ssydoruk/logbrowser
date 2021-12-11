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
public class URSStrategyInitTable extends DBTable {

    public URSStrategyInitTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"URSSTRINIT_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        
        addIndex("time");
        addIndex("FileId");
        addIndex("ConnIDID");
        addIndex("ixnidID");
        addIndex("strategymsgid");
        addIndex("strategynameid");
        dropIndexes();

        String query = "create table if not exists URSSTRINIT_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",ConnIDID INTEGER"
                + ",ixnidID INTEGER"
                + ",strategymsgid integer"
                + ",strategynameid integer"
                //                + ",cgMsg char(50)"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO URSSTRINIT_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?,"
                + "?"
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
        URSStrategyInit rec = (URSStrategyInit) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, rec.GetConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, rec.getUUID()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.URSStrategyMsg, rec.getStrategyMsg()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.URSStrategyName, rec.getStrategyName()));
//                setFieldString(stmt,11, rec.getCgMsg());

                        }
        });
    }



}
