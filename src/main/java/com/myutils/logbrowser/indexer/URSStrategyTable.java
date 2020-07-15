/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author ssydoruk
 */
public class URSStrategyTable extends DBTable {

    public URSStrategyTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("URSSTR_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex("ConnIDID");
        addIndex("ixnidID");
        addIndex("strategymsgid");
        addIndex("ref1id");
        addIndex("ref2id");
        dropIndexes();

        String query = "create table if not exists URSSTR_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int"
                /* standard first */
                + ",ConnIDID INTEGER"
                + ",ixnidID INTEGER"
                + ",strategymsgid integer"
                + ",ref1id integer"
                + ",ref2id integer"
                //                + ",cgMsg char(50)"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO URSSTR_" + getM_dbAccessor().getM_alias() + " VALUES(NULL,?,?,?,?,?,?,"
                /*standard first*/
                + "?,"
                + "?,"
                + "?,"
                + "?"
                + ");");
    }

    /**
     *
     * @throws Exception
     */
    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }

    @Override
    public void AddToDB(Record _rec) {
        URSStrategy rec = (URSStrategy) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, URSStrategy.getFileId());
            stmt.setLong(3, rec.m_fileOffset);
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.m_line);

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, rec.GetConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, rec.getUUID()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.URSStrategyMsg, rec.getStrategyMsg()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.URSStrategyRef, rec.getStrategyRef1()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.URSStrategyRef, rec.getStrategyRef2()));
//                setFieldString(stmt,11, rec.getCgMsg());

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
