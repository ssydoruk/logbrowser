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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TLibMessage extends SIPServerBaseMessage {

    private static final Pattern regTEvent = Pattern.compile("(?:: message (\\w+)$| (Request\\w+)\\s+received)");
    private static final Pattern regTRequest = Pattern.compile("^(\\w+)");
    private static final Pattern regTRequesSource = Pattern.compile("\\s+\\(\\w+\\s+([^\\s]+)");
    private static final Pattern regTEvtPrivateI = Pattern.compile("^(\\w+)");
    private static final Pattern regISCCRefID = Pattern.compile("^\\s+ISCCAttributeReferenceID\\s+(\\d+)");
    final private GenesysMsg lastLogMsg;
    String m_MessageName;
    private String m_source;
    private String ConnID = null;
    private String UUID = null;
    private Long _seqNo = null;
    private String thisDN = null;
    private Long _errorCode = null;

    TLibMessage(GenesysMsg lastLogMsg, boolean m_handlerInProgress, int m_handlerId, int fileID) {
        super(TableType.TLib, m_handlerInProgress, m_handlerId, fileID);
        this.lastLogMsg = lastLogMsg;
    }

    TLibMessage(TLibMessage msg, GenesysMsg lastLogMsg) {
        super(TableType.TLib, msg.isM_handlerInProgress(), msg.getM_handlerId(), msg.getFileID());
        this.m_MessageName = msg.GetMessageName();
        this.lastLogMsg = msg.getLastLogMsg();
        m_MessageLines = new ArrayList<>(msg.m_MessageLines.size());
        for (String m_MessageLine : msg.m_MessageLines) {
            m_MessageLines.add(m_MessageLine);
        }

    }

    public String getConnID() {
        if (ConnID == null) {
            ConnID = getAttributeTrim("AttributeConnID");
        }
        return ConnID;
    }

    public void setConnID(String ConnID) {
        if (StringUtils.isNotBlank(ConnID)) {
            this.ConnID = ConnID;
        }
    }

    public String getUUID() {
        if (UUID == null) {
            UUID = getAttributeTrimQuotes("AttributeCallUUID");
        }
        return UUID;
    }

    public void setUUID(String UUID) {
        if (StringUtils.isNotBlank(UUID)) {
            this.UUID = UUID;
        }
    }

    void setData(String hdr, ArrayList<String> newMessageLines) {

        Matcher m;
        boolean inbound = false;

        if ((m = regTEvent.matcher(hdr)).find()) {
            m_MessageName = m.group(1);

            if (m_MessageName == null || m_MessageName.length() == 0) {
                m_MessageName = m.group(2);
                if (m_MessageName != null && !m_MessageName.isEmpty()) {
                    inbound = true;
                    Main.logger.trace("1Inbound; hdr=[" + hdr + "] l[" + hdr.substring(m.end()) + "]");
                    if ((m = regTRequesSource.matcher(hdr.substring(m.end()))).find()) {
                        m_source = m.group(1);
                    }
                    Main.logger.trace("src[" + m_source + "]");
                }
            }
        } else if (lastLogMsg != null) {

            if (lastLogMsg.getLastGenesysMsgID().equals("04541") && (m = regTRequest.matcher(hdr)).find()) {
                m_MessageName = m.group(1);
                Main.logger.trace("1Inbound; hdr=[" + hdr + "] l[" + hdr.substring(m.end()) + "]");
                if ((m = regTRequesSource.matcher(hdr.substring(m.end()))).find()) {
                    m_source = m.group(1);
                }
                Main.logger.trace("src[" + m_source + "]");
            } else if (lastLogMsg.getLastGenesysMsgID().equals("04542") && (m = regTEvtPrivateI.matcher(hdr)).find()) {
                m_MessageName = m.group(1);
            }
        }
        Main.logger.trace("TLibMessage: [" + m_MessageName + "]" + " hdr: [" + hdr + "]");
        m_MessageLines = newMessageLines;
        m_MessageLines.add(0, m_MessageName);

        SetInbound(inbound);

    }

    public GenesysMsg getLastLogMsg() {
        return lastLogMsg;
    }

    @Override
    public String toString() {
        return "TLibMessage{" + "m_MessageName=" + m_MessageName + ", m_source=" + m_source + '}' + super.toString();
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setInt(1, getRecordID());
        stmt.setTimestamp(2, new Timestamp(GetAdjustedUsecTime()));

        String agentID = NoQuotes(getAttributeTrim("AttributeAgentID"));
        String thisDN = cleanDN(getThisDN());
        if (StringUtils.equals(thisDN, agentID)) {
            agentID = null;
        }

        setFieldInt(stmt, 3, Main.getRef(ReferenceType.TEvent, GetMessageName()));
        setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, thisDN));
        setFieldInt(stmt, 5, Main.getRef(ReferenceType.DN, cleanDN(getAttributeDN("AttributeOtherDN"))));
        setFieldInt(stmt, 6, Main.getRef(ReferenceType.Agent, agentID));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.DN, cleanDN((getAttribute("AttributeANI")))));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.ConnID, getConnID()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.ConnID, GetTransferConnID()));
        setFieldLong(stmt, 10, getRefID());
        stmt.setBoolean(11, isInbound());
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.App, getM_source()));
        setFieldString(stmt, 13, getAttributeTrim("AttributePrivateMsgID"));
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.Switch, NoQuotes(getAttributeTrim("AttributeLocation"))));
        setFieldInt(stmt, 15, getFileID());
        setFieldLong(stmt, 16, getM_fileOffset());
        setFieldLong(stmt, 17, getFileBytes());
        setFieldInt(stmt, 18, (isM_handlerInProgress() ? getM_handlerId() : 0));
        Main.logger.trace("m_handlerId:" + getM_handlerId() + " m_handlerInProgress :" + isM_handlerInProgress());

        setFieldInt(stmt, 19, getM_line());
        setFieldLong(stmt, 20, getSeqNo());
        setFieldInt(stmt, 21, Main.getRef(ReferenceType.UUID, getUUID()));
        setFieldLong(stmt, 22, getErrorCode());
        setFieldLong(stmt, 23, getAttributeLong("AttributeCallType"));
        setFieldInt(stmt, 24, Main.getRef(ReferenceType.TLIBERROR, getErrorMessage(GetMessageName())));
        setFieldInt(stmt, 25, Main.getRef(ReferenceType.TLIBATTR1, getAttr1()));
        setFieldInt(stmt, 26, Main.getRef(ReferenceType.TLIBATTR2, getAttr2()));
        setFieldInt(stmt, 27, Main.getRef(ReferenceType.DN, cleanDN(getAttributeDN("AttributeDNIS"))));
        setFieldInt(stmt, 28, Main.getRef(ReferenceType.Place, getAttributeDN("AttributePlace")));
        return true;

    }

    public String getM_source() {
        return m_source;
    }

    public String GetMessageName() {
        return m_MessageName;
    }

    public String GetTransferConnID() {
        String ret = getAttribute("AttributeTransferConnID");
        if (ret != null && ret.length() > 0) {
            return ret;
        }
        ret = getAttribute("AttributePreviousConnID");
        if (ret != null && ret.length() > 0) {
            return ret;
        }
        ret = getAttribute("AttributeLastTransferConnID");
        if (ret != null && ret.length() > 0) {
            return ret;
        }
        ret = getAttribute("AttributeFirstTransferConnID");
        return ret;
    }

    Long getRefID() {
        Long ret = getAttributeLong("AttributeReferenceID");
        if (ret != null) {
            return ret;
        }

        int i = FindByRx(regISCCRefID, 1, -1);
        if (i == -1) {
            return null;
        }
        return new Long(i);
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

    Long getSeqNo() {
        if (_seqNo == null) {
            _seqNo = getAttributeHex("AttributeEventSequenceNumber");
        }
        return _seqNo;
    }

    String getThisDN() {
        if (thisDN == null) {
            thisDN = getAttributeTrim("AttributeThisDN");
        }
        return thisDN;
    }

    Long getErrorCode() {
        if (_errorCode == null) {
            _errorCode = getAttributeLong("AttributeErrorCode");
        }
        return _errorCode;
    }

}
