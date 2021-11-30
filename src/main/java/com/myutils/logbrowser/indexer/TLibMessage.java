/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author aglagole
 */
public class TLibMessage extends Message {

    private static final Pattern regTEvent = Pattern.compile("(?:: message (\\w+)$| (Request\\w+)\\s+received)");
    private static final Pattern regTRequest = Pattern.compile("^(\\w+)");
    private static final Pattern regTRequesSource = Pattern.compile("\\s+\\(\\w+\\s+([^\\s]+)");
    private static final Pattern regTEvtPrivateI = Pattern.compile("^(\\w+)");
    private static final Pattern  regISCCRefID = Pattern.compile("^\\s+ISCCAttributeReferenceID\\s+(\\d+)");
    final private GenesysMsg lastLogMsg;
    String m_MessageName;
    private String m_source;
    private String ConnID = null;
    private String UUID = null;
    private Long _seqNo = null;
    private String thisDN = null;
    private Long _errorCode = null;

    TLibMessage(GenesysMsg lastLogMsg) {
        super(TableType.TLib);
        this.lastLogMsg = lastLogMsg;
    }

    TLibMessage(TLibMessage msg, GenesysMsg lastLogMsg) {
        super(TableType.TLib);
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
