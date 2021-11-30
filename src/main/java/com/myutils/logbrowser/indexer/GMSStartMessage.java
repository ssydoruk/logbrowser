/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

/**
 * @author terry The class Replicates TLibMessage
 */
public class GMSStartMessage extends Message {

    private static final Pattern regPOSTMessage = Pattern.compile("^\\(POST\\) Client IP Address: ([0-9\\.]+),\\s*");
    private final boolean isTServerReq = false;
    String m_MessageName;
    private String m_refID;
    private String m_ThisDN;
    private String clientIP;
    private boolean POSTParsed = false;
    private Pair<String, String> parseORSURI = null;

    GMSStartMessage(ArrayList<String> m_MessageContents) {
        super(TableType.GMSStart, m_MessageContents);
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
        parsePOST();
        if (parseORSURI != null) {
            return parseORSURI.getKey();
        } else {
            return null;
        }
    }

    String ORSURI() {
        parsePOST();
        if (parseORSURI != null) {
            return parseORSURI.getValue();
        } else {
            return null;
        }
    }

    private void parsePOST() {
        if (!this.POSTParsed) {
            Matcher m;
            if ((m = regPOSTMessage.matcher(m_MessageLines.get(0))).find()) {
                this.clientIP = m.group(1);
                parseORSURI = parseORSURI(m_MessageLines.get(0).substring(m.end(), m_MessageLines.get(0).indexOf(", Params")));
            }
            POSTParsed = true;
        }

    }

    String getPhone() {
        return getGMSAttributeString(new String[]{"phone_number", "_customer_number"});
    }

}
