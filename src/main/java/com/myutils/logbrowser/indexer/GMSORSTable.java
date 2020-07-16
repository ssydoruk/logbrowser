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
public class GMSORSTable extends DBTable {

    public GMSORSTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("gmsors");
        addIndex("time");
        addIndex("FileId");

        addIndex("GMSServiceID");
        addIndex("ORSSessionID");
        addIndex("connIDID");
        addIndex("reqID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",GMSServiceID int"
                + ",ORSSessionID int"
                + ",connIDID int"
                + ",reqID int"
                + ",isinbound bit"
                + ");";
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
        GMSORSMessage gmsRec = (GMSORSMessage) rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {

            stmt.setTimestamp(1, new Timestamp(gmsRec.GetAdjustedUsecTime()));
            stmt.setInt(2, GMSORSMessage.getFileId());
            stmt.setLong(3, gmsRec.m_fileOffset);
            stmt.setLong(4, gmsRec.getM_FileBytes());
            stmt.setLong(5, gmsRec.m_line);

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.GMSService, gmsRec.getGMSSessionID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.ORSSID, gmsRec.getORSSessionID()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.ConnID, gmsRec.getConnID()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.ORSREQ, gmsRec.getORSReq()));
            stmt.setBoolean(10, gmsRec.isInbound());

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add message type " + getM_type() + ": " + e, e);
        }

    }

}
