/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static Utils.Util.StripQuotes;
import java.util.*;

/**
 *
 * @author terry The class Replicates TLibMessage
 */
public class UrsMessage extends Message {

    private final String m_MessageName;
    private final String server;
    private final String fileHandle;
    private String refID;
    private String thisDN;

    public UrsMessage(String event, String server, String fileHandle, String _refID, ArrayList newMessageLines) {
        super(TableType.URSTlib);
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

    void setThisDN(String thisDN) {
        this.thisDN = thisDN;
    }

    String getThisDN() {
        if (thisDN == null) {
            thisDN = getAttributeDN("AttributeThisDN");
        }
        return thisDN;
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

}
