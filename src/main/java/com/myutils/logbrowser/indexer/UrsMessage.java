/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static Utils.Util.StripQuotes;

/**
 * @author terry The class Replicates TLibMessage
 */
public class UrsMessage extends Message {

    private final String m_MessageName;
    private final String server;
    private final String fileHandle;
    private String refID;
    private String thisDN;

    public UrsMessage(String event, String server, String fileHandle, String _refID, ArrayList<String> newMessageLines, int fileID) {
        super(TableType.URSTlib, fileID);
        Main.logger.trace("event=[" + event + "] " + "server=[" + server + "] " + "fileHandle=[" + fileHandle + "] " + "newMessageLines=[" + newMessageLines + "] ");
        m_MessageLines = newMessageLines;
        m_MessageName = event;
        this.server = server;
        refID = _refID;
        this.fileHandle = fileHandle;
        if (m_MessageLines != null) {
            m_MessageLines.add(0, event);
        } else {

        }
    }

    public String GetMessageName() {
        return m_MessageName;
    }

    public String GetConnID() {
        return getAttribute("AttributeConnID");
    }

    public String GetTransferConnID() {
        return getAttribute("AttributeTransferConnID");
    }

    String getIxnID() {
        String mediaType = getUData("MediaType", "");
        String InteractionType = getUData("InteractionType", "");

        if (mediaType != null && mediaType.length() > 0
                && InteractionType != null && InteractionType.length() > 0) {
            return StripQuotes(getUData("InteractionId", ""));
        } else {
            return null;
        }
    }

    String getServer() {
        return server;
    }

    int getServerHandle() {
        try {
            return Integer.parseInt(fileHandle);
        } catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    int getRefID() {
        if (refID != null) {
            return toInt(refID, 0);
        } else {
            return toInt(getAttribute("AttributeReferenceID"), -1);
        }
    }

    void setRefID(String refID) {
        this.refID = refID;
    }

    String getThisDN() {
        if (thisDN == null) {
            thisDN = getAttributeDN("AttributeThisDN");
        }
        return thisDN;
    }

    void setThisDN(String thisDN) {
        this.thisDN = thisDN;
    }

    String getOtherDN() {
        return getAttributeDN("AttributeOtherDN");

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

    String getDNIS() {
        return getAttributeDN("AttributeDNIS");
    }

    String getANI() {
        return getAttributeDN("AttributeANI");
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        setFieldInt(stmt, 2, Main.getRef(ReferenceType.TEvent, GetMessageName()));
        setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, cleanDN(getThisDN())));
        setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, cleanDN(getOtherDN())));
        setFieldInt(stmt, 5, Main.getRef(ReferenceType.Agent, getHeaderTrim("AttributeAgentID")));
        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, GetConnID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, GetTransferConnID()));
        stmt.setBoolean(8, isInbound());
        setFieldInt(stmt, 9, getFileID());
        setFieldLong(stmt, 10, getM_fileOffset());
        setFieldLong(stmt, 11, getFileBytes());
        setFieldLong(stmt, 12, getAttributeHex("AttributeEventSequenceNumber"));
        setFieldInt(stmt, 13, getM_line());
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.UUID, NoQuotes(getAttributeTrim("AttributeCallUUID", 32))));
        setFieldInt(stmt, 15, Main.getRef(ReferenceType.IxnID, NoQuotes(getIxnID())));
        setFieldInt(stmt, 16, Main.getRef(ReferenceType.App, getServer()));
        setFieldInt(stmt, 17, getServerHandle());
        setFieldInt(stmt, 18, getRefID());
        setFieldInt(stmt, 19, Main.getRef(ReferenceType.TLIBERROR, getErrorMessage(GetMessageName())));
        setFieldInt(stmt, 20, Main.getRef(ReferenceType.TLIBATTR1, getAttr1()));
        setFieldInt(stmt, 21, Main.getRef(ReferenceType.TLIBATTR2, getAttr2()));
        setFieldInt(stmt, 22, Main.getRef(ReferenceType.DN, cleanDN(getDNIS())));
        setFieldInt(stmt, 23, Main.getRef(ReferenceType.DN, cleanDN(getANI())));
        return true;

    }
}
