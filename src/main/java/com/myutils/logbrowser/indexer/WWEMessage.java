/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

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
    public WWEMessage(String event, String TserverSRC, ArrayList newMessageLines, boolean isTServerReq) {
        super(TableType.WWETable);
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

}
