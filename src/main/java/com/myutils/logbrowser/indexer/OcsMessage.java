/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author terry The class Replicates TLibMessage
 */
public class OcsMessage extends Message {

    /**
     * @param event
     * @param TserverSRC
     * @param newMessageLines
     * @param isTServerReq
     */
    private static final Matcher regStartLine = Pattern.compile("^received from \\d+\\(([^\\)]+)\\).+message (\\S+)").matcher("");
    private static final Matcher regSentMsg = Pattern.compile("^request to (\\d+)\\(([^\\)]+)\\) message (\\w+)").matcher("");
    private static final Matcher regPrimBackup = Pattern.compile("^([^\\/]+)/(^\\/+)$").matcher("");
    private final boolean isTServerReq = false;
    String m_MessageName;
    private String m_TserverSRCp;
    private String m_TserverSRCb;
    private String m_refID;
    private String m_ThisDN;
    private Long recordHandle = null;

    public OcsMessage(ArrayList newMessageLines) {
        super(TableType.OCSTLib);
        m_MessageLines = newMessageLines;
        Matcher m;
        if ((m = regStartLine.reset(m_MessageLines.get(0))).find()) {
            m_MessageName = m.group(2);
            m_TserverSRCp = m.group(1);
        } else {
            for (int i : new int[]{0, 1}) {
                if ((m = regSentMsg.reset(m_MessageLines.get(i))).find()) {
                    m_MessageName = m.group(3);
                    String TSvr = m.group(2);
                    if ((m = regPrimBackup.reset(TSvr)).find()) {
                        m_TserverSRCp = m.group(1);
                        m_TserverSRCb = m.group(2);
                    } else {
                        m_TserverSRCp = TSvr;
                    }
                    break;
                }
            }

        }

//        m_MessageName = event;
//        m_TserverSRC=TserverSRC;
//        this.isTServerReq=isTServerReq;
//        if( newMessageLines != null)
//            m_MessageLines.add(0, event);
    }

    public String getM_TserverSRCp() {
        return m_TserverSRCp;
    }

    public String getM_TserverSRCb() {
        return m_TserverSRCb;
    }

    public boolean isIsTServerReq() {
        return isTServerReq;
    }

    public String GetMessageName() {
        return m_MessageName;
    }

    public String GetConnID() {
        return getAttributeTrim("AttributeConnID");
    }

    public String GetTransferConnID() {
        return getAttributeTrim("AttributeTransferConnID");
    }

    /**
     * @return the m_refID
     */
    public int getM_refID() {

        if (m_refID == null || "".equals(m_refID)) {
            return getAttributeInt("AttributeReferenceID");
        } else {
            return 0;
        }
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
        String ret = getAttributeDN("AttributeThisDN");
        if (ret == null || ret.length() == 0) {
            ret = getAttributeDN("AttributeCommunicationDN");
        }
        return ret;
    }

    /**
     * @param m_ThisDN the m_ThisDN to set
     */
    public void setM_ThisDN(String m_ThisDN) {
        this.m_ThisDN = m_ThisDN;
    }

    String getOtherDN() {
        return getAttributeDN("AttributeOtherDN");
    }

    Long getRecordHandle() {
        long uData = getUData("GSW_RECORD_HANDLE", -1, true);
        if (uData == -1) {
            return recordHandle;
        } else {
            return uData;
        }
    }

    void setRecordHandle(String recHandle) {
        if (recHandle != null && !recHandle.isEmpty()) {
            try {
                this.recordHandle = Long.parseLong(recHandle);
            } catch (NumberFormatException e) {
                recordHandle = null;
            }
        }
    }

}
