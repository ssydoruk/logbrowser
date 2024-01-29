/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.StripQuotes;

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
    private static final Pattern regStartLine = Pattern.compile("^received from \\d+\\(([^\\)]+)\\).+message (\\S+)");
    private static final Pattern regSentMsg = Pattern.compile("^request to (\\d+)\\(([^\\)]+)\\) message (\\w+)");
    private static final Pattern regPrimBackup = Pattern.compile("^([^\\/]+)/(^\\/+)$");
    private final boolean isTServerReq = false;
    String m_MessageName;
    private String m_TserverSRCp;
    private String m_TserverSRCb;
    private String m_refID;
    private Long recordHandle = null;

    public OcsMessage(ArrayList<String> newMessageLines, int fileID) {
        super(TableType.OCSTLib, fileID);
        m_MessageLines = newMessageLines;
        Matcher m;
        if ((m = regStartLine.matcher(m_MessageLines.get(0))).find()) {
            m_MessageName = m.group(2);
            m_TserverSRCp = m.group(1);
        } else {
            for (int i : new int[]{0, 1}) {
                if ((m = regSentMsg.matcher(m_MessageLines.get(i))).find()) {
                    m_MessageName = m.group(3);
                    String TSvr = m.group(2);
                    if ((m = regPrimBackup.matcher(TSvr)).find()) {
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

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        setFieldLong(stmt, 3, getM_fileOffset());
        setFieldLong(stmt, 4, getM_FileBytes());
        setFieldInt(stmt, 5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.TEvent, GetMessageName()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.DN, cleanDN(getM_ThisDN())));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.DN, cleanDN(getOtherDN())));

        setFieldString(stmt, 9, "");//+ "agentID char(32),"
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.ConnID, GetConnID()));
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.ConnID, GetTransferConnID()));
        stmt.setInt(12, getM_refID());
        stmt.setBoolean(13, isInbound());
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, StripQuotes(getAttributeTrim("AttributeCallUUID", 32))));
        setFieldLong(stmt, 15, getAttributeHex("AttributeEventSequenceNumber"));
        setFieldInt(stmt, 16, Main.getRef(ReferenceType.App, getM_TserverSRCp()));
        setFieldInt(stmt, 17, Main.getRef(ReferenceType.App, getM_TserverSRCb()));
        stmt.setBoolean(18, isIsTServerReq());
        setFieldLong(stmt, 19, getRecordHandle());
        setFieldLong(stmt, 20, getUData("GSW_CHAIN_ID", -1, true));
//            Main.logger.info("CHID: "+getUData("GSW_CHAIN_ID", -1, true));
        setFieldLong(stmt, 21, getUData("GSW_CAMPAIGN_GROUP_DBID", -1, true));
        setFieldInt(stmt, 22, Main.getRef(ReferenceType.TLIBERROR, getErrorMessage(GetMessageName())));
        return true;


    }
}
