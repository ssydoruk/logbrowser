/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import Utils.Pair;

import java.util.ArrayList;

public class GMSORSMessage extends Message {

    Pair<String, String> parseORSURI = null;
    private String theThreadID;
    private String connID = null;
    private String gmsSessionID = null;
    private boolean isException = false;
    private String req = null;

    GMSORSMessage(ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.GMSORSMessage, m_MessageContents, fileID);
    }

    GMSORSMessage(String threadID, String rest, int fileID) {
        super(TableType.GMSORSMessage, rest, fileID);
        this.theThreadID = threadID;
        parseORSURI = parseORSURI(rest);
    }

    GMSORSMessage(String newThread, ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.GMSORSMessage, m_MessageContents, fileID);
        this.theThreadID = newThread;
    }

    GMSORSMessage(String newThread, int fileID) {
        super(TableType.GMSORSMessage, fileID);
        this.theThreadID = newThread;
    }

    String getGMSSessionID() {
        String ret = getGMSAttributeString("_service_id");
        if (ret == null) {
            ret = getGMSAttributeString("_id");
        }
        if (ret == null) {
            ret = getGMSAttributeString("ids");
            if (ret != null && !ret.isEmpty()) {
                int first = 0;
                int last = ret.length() - 1;
                if (ret.charAt(first) == '[') {
                    first = 1;
                }
                if (ret.charAt(last) == ']') {
                    last--;
                }
                if (ret.indexOf(',') >= 0) {
                    Main.logger.error("Got ids attribute with more than one GMS sessionID: [" + ret + "]");
                }
                return ret.substring(first, last);
            }
        }
        return ret;
    }

    void setGMSSessionID(String gmsSessionID) {
        this.gmsSessionID = gmsSessionID;
    }

    String getORSSessionID() {
        if (parseORSURI != null) {
            return parseORSURI.getKey();
        } else {
            return getGMSAttributeString("ors_session_id");
        }
    }

    void setORSSessionID(String orsSessionID) {
        if (orsSessionID != null && !orsSessionID.isEmpty()) {
            if (parseORSURI == null) {
                parseORSURI = new Pair<>(orsSessionID, null);
            } else {
                parseORSURI.setKey(orsSessionID);
            }
        }
//         else {
//            Main.logger.error("parseORSURI not null");
//        }
    }

    String getConnID() {
        return (connID != null) ? connID : getGMSAttributeString("connid");
    }

    void setConnID(String connIDID) {
        this.connID = connIDID;
    }

    String getORSReq() {
        if (req != null) {
            return req;
        }
        if (isException) {
            return (m_MessageLines.size() >= 2) ? m_MessageLines.get(1) : "[non-identified java exception]";
        } else {
            return (parseORSURI != null) ? parseORSURI.getValue() : null;
        }
    }

    void setException() {
        this.isException = true;
    }

    void setReq(String resp) {
        this.req = resp;
    }

    void adjustResp(String orsReq) {
        if (req != null && req.equalsIgnoreCase("OK")) {
            req = "OK (" + orsReq + ")";
        }
    }

}
