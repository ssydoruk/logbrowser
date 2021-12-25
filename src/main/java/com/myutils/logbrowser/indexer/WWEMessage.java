/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static Utils.Util.intOrDef;

/**
 * @author terry The class Replicates TLibMessage
 */
public class WWEMessage extends Message {

    private final String m_TserverSRC;
    String m_MessageName;
    private boolean isTServerReq = false;

    /**
     * @param event
     * @param TserverSRC
     * @param newMessageLines
     * @param isTServerReq
     */
    public WWEMessage(String event, String TserverSRC, ArrayList newMessageLines, boolean isTServerReq, int fileID) {
        super(TableType.WWETable, fileID);
        m_MessageLines = newMessageLines;
        m_MessageName = event;
        m_TserverSRC = TserverSRC;
        this.isTServerReq = isTServerReq;
        if (newMessageLines != null) {
            m_MessageLines.add(0, event);
        }
    }

    public String getM_TserverSRC() {
        return m_TserverSRC;
    }

    public boolean isIsTServerReq() {
        return isTServerReq;
    }

    public String GetMessageName() {
        return m_MessageName;
    }

    /**
     * @return the m_refID
     */
    public Integer getM_refID() {
        return intOrDef(getIxnAttributeLong("AttributeReferenceID"), (Integer) null);

    }

    String getConnID() {
        return getIxnAttributeHex("AttributeConnID");
    }

    String getTransferConnID() {
        return getIxnAttributeHex(new String[]{"AttributePreviousConnID", "AttributeTransferConnID"});
    }

    String getUUID() {
        return getIxnAttributeString("AttributeCallUuid");
    }

    String getThisDN() {
        return getIxnAttributeString("AttributeThisDN");
    }

    String getEventName() {
        return m_MessageName;
    }

    Integer getSeqNo() {
        return intOrDef(getIxnAttributeLong("AttributeEventSequenceNumber"), (Integer) null);
    }

    String getIxnID() {
        return null;
    }

    String getServerID() {
        return null;
    }

    String getOtherDN() {
        return getIxnAttributeString("AttributeOtherDN");
    }

    String getAgentID() {
        return getIxnAttributeString("AttributeAgentID");
    }

    String getDNIS() {
        return getIxnAttributeString("AttributeDNIS");
    }

    String getANI() {
        return getIxnAttributeString("AttributeANI");
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, getConnID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, getTransferConnID()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.UUID, getUUID()));
        setFieldInt(stmt, 9, getM_refID());
        stmt.setBoolean(10, isInbound());
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.DN, cleanDN(getThisDN())));
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.TEvent, getEventName()));
        setFieldInt(stmt, 13, getSeqNo());
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.TEvent, getIxnID()));
        setFieldInt(stmt, 15, Main.getRef(ReferenceType.App, getServerID()));
        setFieldInt(stmt, 16, Main.getRef(ReferenceType.DN, cleanDN(getOtherDN())));
        setFieldInt(stmt, 17, Main.getRef(ReferenceType.Agent, getAgentID()));
        setFieldInt(stmt, 18, Main.getRef(ReferenceType.DN, cleanDN(getDNIS())));
        setFieldInt(stmt, 19, Main.getRef(ReferenceType.DN, cleanDN(getANI())));
        return true;

    }
}
