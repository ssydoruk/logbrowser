/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static Utils.Util.StripQuotes;
import static Utils.Util.intOrDef;

/**
 * @author terry The class Replicates TLibMessage
 */
public class ORSMessage extends Message {

    private static final Pattern regShortConnID = Pattern.compile("^\\sIID:(.+)$");
    private static final Pattern regShortThisDN = Pattern.compile("^\\sTDN:(.+)$");
    private static final Pattern regShortThisDNIS = Pattern.compile("^\\sDNIS:(.+)$");
    private static final Pattern regShortANI = Pattern.compile("^\\sANI:(.+)$");
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
    public ORSMessage(String event, String TserverSRC, ArrayList<String> newMessageLines, boolean isTServerReq, int fileID) {
        super(TableType.ORSTlib, fileID);
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

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));

        setFieldInt(stmt, 2, Main.getRef(ReferenceType.TEvent, GetMessageName()));
        setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, cleanDN(getM_ThisDN())));
        setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, cleanDN(getAttribute("AttributeOtherDN"))));
        setFieldInt(stmt, 5, Main.getRef(ReferenceType.Agent, getHeaderTrim("AttributeAgentID")));

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, GetConnID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, getAttributeTrim("AttributeTransferConnID")));
        setFieldInt(stmt, 8, getM_refID());
        stmt.setBoolean(9, isInbound());
        setFieldInt(stmt, 10, getFileID());
        stmt.setLong(11, getM_fileOffset());
        stmt.setLong(12, getFileBytes());
        stmt.setInt(13, 0);
        stmt.setInt(14, getM_line());
        setFieldInt(stmt, 15, Main.getRef(ReferenceType.UUID, StripQuotes(getAttributeTrim("AttributeCallUUID", 32))));
        setFieldLong(stmt, 16, getAttributeHex("AttributeEventSequenceNumber"));
        setFieldInt(stmt, 17, Main.getRef(ReferenceType.App, getM_TserverSRC()));
        stmt.setBoolean(18, isIsTServerReq());
        setFieldInt(stmt, 19, Main.getRef(ReferenceType.TLIBERROR, getErrorMessage(GetMessageName())));
        setFieldInt(stmt, 20, Main.getRef(ReferenceType.TLIBATTR1, getAttr1()));
        setFieldInt(stmt, 21, Main.getRef(ReferenceType.TLIBATTR2, getAttr2()));
        setFieldInt(stmt, 22, getORSCallID());
        setFieldInt(stmt, 23, Main.getRef(ReferenceType.DN, cleanDN(getDNIS())));
        setFieldInt(stmt, 24, Main.getRef(ReferenceType.DN, cleanDN(getANI())));
        return true;

    }
}
