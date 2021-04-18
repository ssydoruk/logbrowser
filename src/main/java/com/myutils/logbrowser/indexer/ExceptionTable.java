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
public class ExceptionTable extends DBTable {

    public ExceptionTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("WWEExceptions");
        addIndex("time");
        addIndex("FileId");
        addIndex("exceptionID");
        addIndex("exceptionMsgID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",exceptionID int"
                + ",exceptionMsgID int"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
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
    public void AddToDB(Record rec) {
        ExceptionMessage exceptionRec = (ExceptionMessage) rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {

            stmt.setTimestamp(1, new Timestamp(exceptionRec.GetAdjustedUsecTime()));
            stmt.setInt(2, ExceptionMessage.getFileId());
            stmt.setLong(3, exceptionRec.getM_fileOffset());
            stmt.setLong(4, exceptionRec.getM_FileBytes());
            stmt.setLong(5, exceptionRec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.Exception, exceptionRec.getException()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.ExceptionMessage, exceptionRec.getExceptionMessage()));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add message type " + getM_type() + ": " + e, e);
        }

    }

}
