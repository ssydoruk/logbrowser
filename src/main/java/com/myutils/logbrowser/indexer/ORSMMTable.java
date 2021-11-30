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
public class ORSMMTable extends DBTable {

    public ORSMMTable(DBAccessor dbaccessor) {
        super(dbaccessor, TableType.ORSMMessage);
    }

    @Override
    public void InitDB() {
        setTabName("ORSMM");
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("IXNIDID");
        addIndex("refid");
        addIndex("PARENTIXNIDID");
        dropIndexes();

        String query = "create table if not exists ORSMM (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",nameID INTEGER"
                + ",IXNIDID INTEGER"
                + ",refid INTEGER"
                + ",inbound bit"
                + ",PARENTIXNIDID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO ORSMM VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
                + ",?"
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
        ORSMM rec = (ORSMM) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, ORSMM.getFileId());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, rec.GetMessageName()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, rec.GetIxnID())); // UUID for ORS is the same as IxnID
            stmt.setLong(8, rec.getM_refID());
            stmt.setBoolean(9, rec.isIsTServerReq());
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.IxnID, rec.GetParentIxnID())); // UUID for ORS is the same as IxnID

                }
         });
    }


}
