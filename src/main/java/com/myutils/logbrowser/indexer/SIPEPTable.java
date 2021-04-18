/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static com.myutils.logbrowser.indexer.Message.transformDN;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author ssydoruk
 */
public class SIPEPTable extends DBTable {

    public SIPEPTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("sipep");

        addIndex("CallIDID");
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("ViaBranchID");
        addIndex("FromUserpartID");
        addIndex("requestURIDNid");
        addIndex("uuidid");
        addIndex("sipuriid");
        dropIndexes();

        String query = "create table if not exists " + getTabName() + " (id INTEGER PRIMARY KEY ASC,"
                + "time timestamp,"
                + "inbound bit,"
                + "nameID int,"
                + "CallIDID int,"
                + "CSeqID int,"
                + "ToUriID int,"
                + "FromUriID int,"
                + "FromUserpartID int,"
                + "ViaUriID int,"
                + "ViaBranchID int,"
                + "PeerIpID int,"
                + "PeerPort int,"
                + "FileId int,"
                + "FileOffset bigint,"
                + "FileBytes int,"
                + "callRelated bit,"
                + "line int,"
                + "uuidid int,"
                + "requestURIDNid,"
                + "sipuriid int"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO " + getTabName() + " VALUES(NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
    }

    @Override
    public void FinalizeDB() {
        createIndexes();
    }

    @Override
    public void AddToDB(Record rec) throws Exception {
        SipMessage theRec = (SipMessage) rec;
        PreparedStatement stmt = getM_dbAccessor().GetStatement(m_InsertStatementId);

        try {
            stmt.setTimestamp(1, new Timestamp(theRec.GetAdjustedUsecTime()));
            stmt.setBoolean(2, theRec.isInbound());
            setFieldInt(stmt, 3, Main.getRef(ReferenceType.SIPMETHOD, theRec.GetName()));
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.SIPCALLID, theRec.GetCallId()));
            setFieldInt(stmt, 5, Main.getRef(ReferenceType.SIPCSEQ, theRec.GetCSeq()));
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.SIPURI, theRec.GetTo()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.SIPURI, theRec.GetFrom()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.DN, transformDN(SipMessage.SingleQuotes(theRec.GetFromUserpart()))));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.SIPURI, theRec.GetViaUri()));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.SIPVIABRANCH, theRec.GetViaBranch()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.IP, theRec.GetPeerIp()));
            stmt.setInt(12, theRec.GetPeerPort());
            stmt.setInt(13, SipMessage.getFileId());
            stmt.setLong(14, theRec.getM_fileOffset());
            stmt.setLong(15, theRec.getFileBytes());
            Main.logger.trace("theRec.m_handlerId :" + SipMessage.m_handlerId + " theRec.m_handlerInProgress" + SipMessage.m_handlerInProgress);

            stmt.setBoolean(16, theRec.isCallRelated());
            stmt.setInt(17, theRec.getM_line());
            setFieldInt(stmt, 18, Main.getRef(ReferenceType.UUID, theRec.getUUID()));
            setFieldInt(stmt, 19, Main.getRef(ReferenceType.DN, transformDN(SipMessage.SingleQuotes(theRec.getRequestURIDN()))));
            setFieldInt(stmt, 20, Main.getRef(ReferenceType.SIPURI, theRec.getSipURI()));

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            throw e;
        }

    }

}
