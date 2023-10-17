/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static Utils.Util.intOrDef;

/**
 * @author terry The class Replicates TLibMessage
 */
public class ORSSessionStartMessage extends Message {

    private final boolean isTServerReq = false;
    private final boolean POSTParsed = false;
    private final Pair<String, String> parseORSURI = null;
    String m_MessageName;
    ArrayList<String> params = new ArrayList<>();
    private String m_refID;
    private String m_ThisDN;
    private String clientIP;
    private String newSessionID;

    ORSSessionStartMessage(ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.ORSSessionStart, m_MessageContents, fileID);
    }

    ORSSessionStartMessage(int fileID) {
        super(TableType.ORSSessionStart, fileID);
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
        return null;
    }

    String getUUID() {
        return getIxnAttributeString("AttributeCallUuid");
    }

    String getThisDN() {
        return getIxnAttributeString("AttributeThisDN");
    }

    String getEventName() {
        return getGMSAttributeString("EventName");
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

    String CallBackType() {
        return getGMSAttributeString(new String[]{"callback_type", "serviceType"});
    }

    String GMSService() {
        return getGMSAttributeString(new String[]{"_service_id", "service_id"});
    }

    String ORSSessionID() {
        return newSessionID;
    }

    String ORSURI() {

        if (parseORSURI != null) {
            return parseORSURI.getValue();
        } else {
            return null;
        }
    }

    String getPhone() {
        return getGMSAttributeString(new String[]{"phone_number", "_customer_number"});
    }

    void addParam(String str) {
        params.add(str);
    }

    void setNewSessionID(String group) {
        this.newSessionID = group;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {

        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.GMSEvent, getEventName()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.GMSCallbackType, CallBackType()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.GMSService, GMSService()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.ORSSID, ORSSessionID()));
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.DN, cleanDN(getPhone())));
        stmt.setBoolean(11, isInbound());
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.ORSREQ, ORSURI()));
        return true;

    }
}
