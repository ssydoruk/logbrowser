/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

/**
 * @author terry The class Replicates TLibMessage
 */
public class ORSMessage extends Message {

    private static final Matcher regShortConnID = Pattern.compile("^\\sIID:(.+)$").matcher("");
    private static final Matcher regShortThisDN = Pattern.compile("^\\sTDN:(.+)$").matcher("");
    private static final Matcher regShortThisDNIS = Pattern.compile("^\\sDNIS:(.+)$").matcher("");
    private static final Matcher regShortANI = Pattern.compile("^\\sANI:(.+)$").matcher("");
    private final String m_TserverSRC;
    String m_MessageName;
    private String m_refID;
    private String m_ThisDN;
    private boolean isTServerReq = false;
    private Integer callID;

    /**
     * @param event
     * @param TserverSRC
     * @param newMessageLines
     * @param isTServerReq
     */
    public ORSMessage(String event, String TserverSRC, ArrayList<String> newMessageLines, boolean isTServerReq) {
        super(TableType.ORSTlib);
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

    public String GetConnID() {
        return longOrShort(getAttributeTrim("AttributeConnID"), FindByRx(regShortConnID, 1, null));
    }

    private String longOrShort(String l, String s) {
        if (StringUtils.isBlank(l)) {
            return (StringUtils.isBlank(s)) ? null : s.toLowerCase();
        } else {
            return l;
        }

    }

    public String GetTransferConnID() {
        return getAttributeTrim("AttributeTransferConnID");
    }

    /**
     * @return the m_refID
     */
    public int getM_refID() {
        if (m_refID == null || m_refID.isEmpty()) {

            m_refID = getAttribute("AttributeReferenceID");
            if (m_refID == null || m_refID.isEmpty()) {
                m_refID = getAttribute("getM_refID");
            }
        }
        return intOrDef(m_refID, 0);
    }

    /**
     * @param m_refID the m_refID to set
     */
    public void setM_refID(String m_refID) {
        this.m_refID = m_refID;
    }

    /**
     * @return the m_ThisDN
     */
    public String getM_ThisDN() {
        if (m_ThisDN == null || "".equals(m_ThisDN)) {
            return longOrShort(getAttributeDN("AttributeThisDN"), FindByRx(regShortThisDN, 1, null));

        }

        return m_ThisDN;
    }

    /**
     * @param m_ThisDN the m_ThisDN to set
     */
    public void setM_ThisDN(String m_ThisDN) {
        this.m_ThisDN = m_ThisDN;
    }

    String getAttr1() {
        String msg = GetMessageName();
        if (msg != null && msg.contentEquals("RequestRouteCall")) {
            String attribute = getAttribute("AttributeRouteType");
            if (attribute != null && !attribute.isEmpty()) {
                return "routeType=" + attribute;
            }

        }
        return null;

    }

    String getAttr2() {
        return null;
    }

    void setCallID(String group) {
        this.callID = intOrDef(group, (Integer) null);
    }

    Integer getORSCallID() {
        return callID;
    }

    String getDNIS() {
        return longOrShort(getAttributeDN("AttributeDNIS"), FindByRx(regShortThisDNIS, 1, null));

    }

    String getANI() {
        return longOrShort(getAttributeDN("AttributeANI"), FindByRx(regShortANI, 1, null));

    }

}
