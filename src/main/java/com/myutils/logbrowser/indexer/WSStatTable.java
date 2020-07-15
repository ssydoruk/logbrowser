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
 *
 *
 * Make Refactor/Copy of this class for new table type
 */
public class WSStatTable extends DBTable {

    public WSStatTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("wsstat");
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("HandlerId");
        dropIndexes();

        String query = "create table if not exists wsstat (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",nameid int"
                + ",HandlerId INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO wsstat VALUES(NULL,?,?,?,?,?"
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
    public void AddToDB(Record _rec) {
        WSStat rec = (WSStat) _rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileId());
            stmt.setLong(3, rec.m_fileOffset);
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.m_line);

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.StatEvent, rec.GetMessageName()));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
        }
    }

}
