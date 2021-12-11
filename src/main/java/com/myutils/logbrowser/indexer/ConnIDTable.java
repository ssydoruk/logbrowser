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
public class ConnIDTable extends DBTable {

    public ConnIDTable(SqliteAccessor dbaccessor, TableType t) {
        super(dbaccessor, t,"connid_" + dbaccessor.getM_alias());
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");
        addIndex("ConnectionIdID");
        addIndex("HandlerID");
        addIndex("newConnIdID");
        dropIndexes();

        String query = "CREATE TABLE IF NOT EXISTS " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",ConnectionIdID INTEGER"
                + ",HandlerID INTEGER"
                + ",created bit"
                + ",isTemp bit"
                + ",newConnIdID INTEGER"
                + ")";

        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
    }

    @Override
    public void AddToDB(Record rec) throws SQLException {
        ConnIdRecord theRec = (ConnIdRecord) rec;
        getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
            @Override
            public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(theRec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, theRec.getM_fileOffset());
            stmt.setLong(4, theRec.getM_FileBytes());
            stmt.setInt(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, theRec.getM_connId()));
            stmt.setInt(7, (theRec.isM_handlerInProgress()  ? theRec.getM_handlerId() : 0));
            Main.logger.trace("theRec.m_handlerId :" +  theRec.getM_handlerId() + " theRec.m_handlerInProgress" + theRec.isM_handlerInProgress());

            stmt.setBoolean(8, theRec.isM_created());
            stmt.setBoolean(9, theRec.isIsTemp());
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.ConnID, theRec.getNewID()));

            }
        });
    }


}
