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
public class WWETable extends DBTable {

    public WWETable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("WWETEvents");
        addIndex("time");
        addIndex("FileId");
        addIndex("ConnectionIDID");
        addIndex("TransferIdID");
        addIndex("UUIDID");
        addIndex("thisDNID");
        addIndex("nameID");
        addIndex("seqno");
        addIndex("IXNIDID");
        addIndex("ServerID");
        addIndex("otherDNID");
        addIndex("agentIDID");
        addIndex("DNISID");
        addIndex("ANIID");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC"
                + ",time timestamp"
                + ",FileId INTEGER"
                + ",FileOffset bigint"
                + ",FileBytes int"
                + ",line int"
                /* standard first */
                + ",ConnectionIDID int"
                + ",TransferIdID int"
                + ",UUIDID int"
                + ",referenceID int"
                + ",inbound bit"
                + ",thisDNID int"
                + ",nameID int"
                + ",seqno bit"
                + ",IXNIDID bit"
                + ",ServerID bit"
                + ",otherDNID int"
                + ",agentIDID int"
                + ",DNISID int"
                + ",ANIID int"
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
                + ",?"
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
    public void AddToDB(Record rec) throws SQLException {
        WWEMessage wweRec = (WWEMessage) rec;
         getM_dbAccessor().addToDB(m_InsertStatementId, new IFillStatement() {
                @Override
                public void fillStatement(PreparedStatement stmt) throws SQLException{

            stmt.setTimestamp(1, new Timestamp(wweRec.GetAdjustedUsecTime()));
            stmt.setInt(2, rec.getFileID());
            stmt.setLong(3, wweRec.getM_fileOffset());
            stmt.setLong(4, wweRec.getM_FileBytes());
            stmt.setLong(5, wweRec.getM_line());

            setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, wweRec.getConnID()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, wweRec.getTransferConnID()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.UUID, wweRec.getUUID()));
            setFieldInt(stmt, 9, wweRec.getM_refID());
            stmt.setBoolean(10, wweRec.isInbound());
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.DN, rec.cleanDN(wweRec.getThisDN())));
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.TEvent, wweRec.getEventName()));
            setFieldInt(stmt, 13, wweRec.getSeqNo());
            setFieldInt(stmt, 14, Main.getRef(ReferenceType.TEvent, wweRec.getIxnID()));
            setFieldInt(stmt, 15, Main.getRef(ReferenceType.App, wweRec.getServerID()));
            setFieldInt(stmt, 16, Main.getRef(ReferenceType.DN, rec.cleanDN(wweRec.getOtherDN())));
            setFieldInt(stmt, 17, Main.getRef(ReferenceType.Agent, wweRec.getAgentID()));
            setFieldInt(stmt, 18, Main.getRef(ReferenceType.DN, rec.cleanDN(wweRec.getDNIS())));
            setFieldInt(stmt, 19, Main.getRef(ReferenceType.DN, rec.cleanDN(wweRec.getANI())));

                        }
        });
    }



}
