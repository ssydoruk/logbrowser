/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;
import static Utils.Util.intOrDef;
import java.util.*;

/**
 *
 * @author terry The class Replicates TLibMessage
 */
public class ORSSessionStartMessage extends Message {

    String m_MessageName;

    private String m_refID;
    private String m_ThisDN;
    private boolean isTServerReq = false;
    private String clientIP;
    private boolean POSTParsed = false;
    private String newSessionID;
    private Pair<String, String> parseORSURI = null;
    ArrayList<String> params = new ArrayList<>();

    ORSSessionStartMessage(ArrayList<String> m_MessageContents) {
        super(TableType.ORSSessionStart, m_MessageContents);
    }

    ORSSessionStartMessage() {
        super(TableType.ORSSessionStart);
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

}
