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
public class ORSSessionStartTable extends DBTable {

    public ORSSessionStartTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        addIndex("time");
        addIndex("FileId");

        addIndex("EventNameID");
        addIndex("CallBackTypeID");
        addIndex("GMSServiceID");
        addIndex("ORSSessionID");
        addIndex("phoneID");
        addIndex("URIID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",EventNameID int"
                + ",CallBackTypeID int"
                + ",GMSServiceID int"
                + ",ORSSessionID int"
                + ",phoneID int"
                + ",isinbound bit"
                + ",URIID int"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?"
                /*standard first*/
                + ",?"
                + ",?"
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
        ORSSessionStartMessage gmsRec = (ORSSessionStartMessage) rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {

            stmt.setTimestamp(1, new Timestamp(gmsRec.GetAdjustedUsecTime()));
            stmt.setInt(2, ORSSessionStartMessage.getFileId());
            stmt.setLong(3, gmsRec.m_fileOffset);
            stmt.setLong(4, gmsRec.getM_FileBytes());
            stmt.setLong(5, gmsRec.m_line);

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.GMSEvent, gmsRec.getEventName()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.GMSCallbackType, gmsRec.CallBackType()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.GMSService, gmsRec.GMSService()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.ORSSID, gmsRec.ORSSessionID()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.DN, Record.cleanDN(gmsRec.getPhone())));
            stmt.setBoolean(11, gmsRec.isInbound());
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.ORSREQ, gmsRec.ORSURI()));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add message type " + getM_type() + ": " + e, e);
        }

    }

}
