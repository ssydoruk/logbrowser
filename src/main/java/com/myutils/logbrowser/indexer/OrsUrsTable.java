/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
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
public class OrsUrsTable extends DBTable {

    public OrsUrsTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("ORSURS_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex("uuidid");
        addIndex("sidid");
        addIndex("event");
        addIndex("refid");
        addIndex("reqid");
        addIndex("moduleid");
        addIndex("methodid");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + "(id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "line int,"
                /* standard first */
                + "inbound BIT,"
                + "sidid int,"
                + "uuidid int,"
                + "event BIGINT,"
                + "refid int,"
                + "reqid int,"
                + "moduleid int,"
                + "methodid int"
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
                + ",?"
                + ");");
    }

    @Override
    public void AddToDB(Record aRec) {
        OrsUrsMessage theRec = (OrsUrsMessage) aRec;

        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);
        try {
            stmt.setTimestamp(1, new Timestamp(theRec.GetAdjustedUsecTime()));
            stmt.setInt(2, OrsUrsMessage.getFileId());
            stmt.setLong(3, theRec.getM_fileOffset());
            stmt.setLong(4, theRec.getM_FileBytes());
            stmt.setLong(5, theRec.getM_line());

            stmt.setBoolean(6, theRec.isInbound());
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.ORSSID, theRec.GetSid()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.UUID, theRec.GetCall()));
            stmt.setLong(9, theRec.GetEvent());
            stmt.setInt(10, theRec.GetRefId());
            stmt.setInt(11, theRec.GetReqId());

            setFieldInt(stmt, 12, Main.getRef(ReferenceType.ORSMODULE, theRec.GetHeaderValue("module")));
            setFieldInt(stmt, 13, Main.getRef(ReferenceType.ORSMETHOD, theRec.GetHeaderValue("method")));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add ORSURS message: " + e.getMessage() + "\n", e);
        }
    }

    @Override
    public void FinalizeDB() throws Exception {
        createIndexes();
    }
}
