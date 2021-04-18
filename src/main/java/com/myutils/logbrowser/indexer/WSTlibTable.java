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
public class WSTlibTable extends DBTable {

    public WSTlibTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("WSTlib");
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("thisDNID");
        addIndex("otherDNID");
        addIndex("agentIDID");
        addIndex("ConnectionIDID");
        addIndex("TransferIdID");
        addIndex("ReferenceId");
        addIndex("uuidid");
        addIndex("IXNIDID");
        addIndex("sourceID");
        dropIndexes();

        String query = "create table if not exists WSTlib (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "nameID INTEGER,"
                + "thisDNID INTEGER,"
                + "otherDNID INTEGER,"
                + "agentIDID INTEGER,"
                + "ConnectionIDID INTEGER,"
                + "TransferIdID INTEGER,"
                + "ReferenceId INTEGER,"
                + "Inbound bit,"
                + "FileId INTEGER,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "HandlerId INTEGER,"
                + "seqno integer"
                + ",line int"
                + ",uuidid integer"
                + ",IXNIDID integer"
                + ",sourceID integer"
                + ",ServerHandle integer"
                + ",refID integer"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO WSTlib VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void AddToDB(Record rec) {
        WSMessage theRec = (WSMessage) rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(theRec.GetAdjustedUsecTime()));
            setFieldInt(stmt, 2, Main.getRef(ReferenceType.TEvent, theRec.GetMessageName()));
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, Record.cleanDN(theRec.GetThisDN())));
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, Record.cleanDN(theRec.GetOtherDN())));
            setFieldInt(stmt, 5, Main.getRef(ReferenceType.Agent, theRec.GetAgentID()));
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, theRec.GetConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, theRec.GetTransferConnID()));
            stmt.setInt(8, (int) theRec.getHeaderLong("AttributeReferenceID"));
            stmt.setBoolean(9, theRec.isInbound());
            stmt.setInt(10, UrsMessage.getFileId());
            stmt.setLong(11, theRec.getM_fileOffset());
            stmt.setLong(12, theRec.getFileBytes());
            stmt.setInt(13, 0);
            stmt.setLong(14, theRec.getSeqNo());
            stmt.setInt(15, theRec.getM_line());
            setFieldInt(stmt, 16, Main.getRef(ReferenceType.UUID, theRec.GetUUID()));
            setFieldInt(stmt, 17, Main.getRef(ReferenceType.IxnID, theRec.getIxnID()));
            setFieldInt(stmt, 18, Main.getRef(ReferenceType.App, theRec.getServer()));
            stmt.setInt(19, theRec.getServerHandle());
            stmt.setInt(20, theRec.getRefID());

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add message type " + getM_type() + ": " + e, e);
        }

    }

}
