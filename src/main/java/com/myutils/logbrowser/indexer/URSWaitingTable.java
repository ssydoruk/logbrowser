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
public class URSWaitingTable extends DBTable {

    public URSWaitingTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("urswaiting");
        addIndex("time");
        addIndex("FileId");
        addIndex("connIDID");
        addIndex("agentID");
        addIndex("agentTypeID");
        dropIndexes();

        String query = "create table if not exists urswaiting (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",connIDID INTEGER"
                + ",agentID INTEGER"
                + ",agentTypeID INTEGER"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO urswaiting VALUES(NULL,?,?,?,?,?"
                /*standard first*/
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
        URSWaiting rec = (URSWaiting) _rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{
            stmt.setTimestamp(1, new Timestamp(rec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, rec.getM_fileOffset());
            stmt.setLong(4, rec.getM_FileBytes());
            stmt.setLong(5, rec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, rec.getConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, rec.getAgent()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.ObjectType, rec.getTargetType()));

                        }
        });
    }



}
