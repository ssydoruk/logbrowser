/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import static com.myutils.logbrowser.indexer.Message.transformDN;

/**
 *
 * @author ssydoruk
 */
public class SIPTable extends DBTable {

    public SIPTable(DBAccessor dbaccessor, TableType t) {
        super(dbaccessor, t);
    }

    @Override
    public void InitDB() {
        setTabName("sip_" + getM_dbAccessor().getM_alias());
        addIndex("time");
        addIndex("FileId");
        addIndex("nameID");
        addIndex("CallIDID");
        addIndex("CSeqID");
        addIndex("ToUriID");
        addIndex("FromUriID");
        addIndex("FromUserpartID");
        addIndex("ViaUriID");
        addIndex("ViaBranchID");
        addIndex("PeerIpID");
        addIndex("HandlerId");
        addIndex("uuidid");
        addIndex("requestURIDNid");
        dropIndexes();

        String query = "create table if not exists sip_" + getM_dbAccessor().getM_alias() + " (id INTEGER PRIMARY KEY ASC,"
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
                + "HandlerId int,"
                + "callRelated bit,"
                + "line int,"
                + "uuidid int,"
                + "requestURIDNid"
                + ");";
        getM_dbAccessor().runQuery(query);
        m_InsertStatementId = getM_dbAccessor().PrepareStatement("INSERT INTO sip_" + getM_dbAccessor().getM_alias() + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);");
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
            generateID(stmt, 1, rec);
            stmt.setTimestamp(2, new Timestamp(theRec.GetAdjustedUsecTime()));
            stmt.setBoolean(3, theRec.isInbound());
            setFieldInt(stmt, 4, Main.getRef(ReferenceType.SIPMETHOD, theRec.GetName()));
            setFieldInt(stmt, 5, Main.getRef(ReferenceType.SIPCALLID, theRec.GetCallId()));
            setFieldInt(stmt, 6, Main.getRef(ReferenceType.SIPCSEQ, theRec.GetCSeq()));
            setFieldInt(stmt, 7, Main.getRef(ReferenceType.SIPURI, theRec.GetTo()));
            setFieldInt(stmt, 8, Main.getRef(ReferenceType.SIPURI, theRec.GetFrom()));
            setFieldInt(stmt, 9, Main.getRef(ReferenceType.DN, transformDN(theRec.GetFrom())));
            setFieldInt(stmt, 10, Main.getRef(ReferenceType.SIPURI, theRec.GetViaUri()));
            setFieldInt(stmt, 11, Main.getRef(ReferenceType.SIPVIABRANCH, theRec.GetViaBranch()));
            setFieldInt(stmt, 12, Main.getRef(ReferenceType.IP, theRec.GetPeerIp()));
            setFieldInt(stmt, 13, theRec.GetPeerPort());
            setFieldInt(stmt, 14, SipMessage.getFileId());
            stmt.setLong(15, theRec.m_fileOffset);
            stmt.setLong(16, theRec.GetFileBytes());
            Main.logger.trace("theRec.m_handlerId :" + SipMessage.m_handlerId + " theRec.m_handlerInProgress" + SipMessage.m_handlerInProgress);

            setFieldInt(stmt, 17, theRec.getHandlerInProgress());
            stmt.setBoolean(18, theRec.isCallRelated());
            setFieldInt(stmt, 19, theRec.m_line);
            setFieldInt(stmt, 20, Main.getRef(ReferenceType.UUID, theRec.getUUID()));
            setFieldInt(stmt, 21, Main.getRef(ReferenceType.DN, transformDN(SipMessage.SingleQuotes(theRec.getRequestURIDN()))));
            theRec.SetSipId(getCurrentID());

            getM_dbAccessor().SubmitStatement(m_InsertStatementId);
        } catch (SQLException e) {
            Main.logger.error("Could not add record type " + m_type.toString() + ": " + e, e);
            throw e;
        }

    }

}
