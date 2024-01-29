/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class ORSMM extends Message {

    private String m_TserverSRC;
    private String m_MessageName;
    private boolean isTServerReq;

    public ORSMM(ArrayList<String> messageLines, int fileID) {
        super(TableType.ORSMMessage, messageLines, fileID);
    }

    ORSMM(String m_msgName, String m_TserverSRC, ArrayList<String> m_MessageContents, boolean b, int fileID) {
        super(TableType.ORSMMessage, fileID);
        m_MessageLines = m_MessageContents;
        m_MessageName = m_msgName;
        this.m_TserverSRC = m_TserverSRC;
        this.isTServerReq = b;
        if (m_MessageContents != null) {
            m_MessageLines.add(0, m_msgName);
        }
    }

    String GetMessageName() {
        return m_MessageName;
    }

    String GetIxnID() {
        String ret = getMMAttributeString("attr_itx_id");
        if (ret == null || ret.length() == 0) {
            ret = getMMUserDataString("InteractionId");
        }
        return ret;
    }

    String getMediaType() {
        return getMMAttributeString("attr_itx_media_type");
    }

    String getQueue() {
        return getMMAttributeString("attr_itx_queue");
    }

    String GetNewIxnID() {
        return getMMAttrEnvelopeString("NewInteractionId");
    }

    long getM_refID() {
        return getMMAttributeLong("attr_ref_id");
    }

    boolean isIsTServerReq() {
        return isTServerReq;
    }

    String GetParentIxnID() {
        String ret = getMMAttributeString("attr_itx_prnt_itx_id");
        return ret;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, GetMessageName()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.IxnID, GetIxnID())); // UUID for ORS is the same as IxnID
        stmt.setLong(8, getM_refID());
        stmt.setBoolean(9, isIsTServerReq());
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.IxnID, GetParentIxnID())); // UUID for ORS is the same as IxnID
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.Media, getMediaType())); // UUID for ORS is the same as IxnID
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.DN, getQueue())); // UUID for ORS is the same as IxnID
        return true;
    }
}
