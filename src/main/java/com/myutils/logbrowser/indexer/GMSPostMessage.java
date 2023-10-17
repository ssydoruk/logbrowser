/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author terry The class Replicates TLibMessage
 */
public class GMSPostMessage extends Message {

    private static final Pattern regService = Pattern.compile("/service/callback/([^,\\s]+)");
    private static final Pattern regPOSTMessage = Pattern.compile("^\\(POST\\) Client IP Address: ([0-9\\.]+),.+(http[^,]+)");
    private final boolean isTServerReq = false;
    String m_MessageName;
    private String clientIP;
    private boolean POSTParsed = false;
    private Pair<String, String> parseORSURI = null;

    GMSPostMessage(ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.GMSPOST, m_MessageContents, fileID);
    }

    public boolean isIsTServerReq() {
        return isTServerReq;
    }

    public String GetMessageName() {
        return m_MessageName;
    }

    String getTransferConnID() {
        return null;
    }

    String getEventName() {
        String ret = getGMSAttributeString("EventName");
//        if(ret==null || ret.isEmpty()){
//                    ret= FindByRx(regService, 1, (String)null);
//
//        }
        return ret;
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
        String ret = getGMSAttributeString("service_id");

        return ret;
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
                parseORSURI = parseORSURI(m.group(2));
            }
            POSTParsed = true;
        }

    }

    String getPhone() {
        return getGMSAttributeString(new String[]{"phone_number", "_customer_number"});
    }

    String getSourceIP() {
        return clientIP;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.GMSEvent, getEventName()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.GMSCallbackType, CallBackType()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.GMSService, GMSService()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.ORSSID, ORSSessionID()));
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.DN, cleanDN(getPhone())));
        stmt.setBoolean(11, isInbound());
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.ORSREQ, ORSURI()));
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.IP, getSourceIP()));
        return true;
    }
}
