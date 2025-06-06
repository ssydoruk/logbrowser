/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;
import static com.myutils.logbrowser.indexer.Parser.getQueryKey;
import static com.myutils.logbrowser.indexer.Parser.splitQuery;

public class OrsHTTP extends Message {

    //    Set-Cookie:ORSSESSIONID=00FAVPM3BOBM3223H3CA9ATAES00000G.node1576
    private static final Pattern regSIDCookie = Pattern.compile("ORSSESSIONID=([\\w~]+)");
    private static final Pattern regHTTPMethod = Pattern.compile("^(\\w+)");
    private static final Pattern regSessionID = Pattern.compile("session/([^\\/]+)");
    private static final Pattern regHTTPURL = Pattern.compile("^(?:POST|GET) (.+) HTTP/");
    private static final Pattern regHTTPResp = Pattern.compile("^HTTP/.\\.. (.+)");
    JSONObject _jsonBody = null;
    private String ip = null;
    private int socket = 0;
    private int bytes = 0;
    private String sid = null;
    private String method = null;
    private String url = null;
    private String gmsService = null;
    private String httpResponseID;
    private int bodyLineIdx = -2;
    private String httpMessageBody = null;

    public OrsHTTP(ArrayList<String> messageLines, int fileID) {
        super(TableType.ORSHTTP, messageLines, fileID);
    }

    public boolean SessionStart() {
        if (m_MessageLines.size() > 0) {
            return m_MessageLines.get(0).startsWith("POST /scxml/session/start HTTP");
        }
        return false;
    }

    public void SetIP(String ip) {

    }

    public String GetSid() {
        return GetHeaderValueNoQotes("session");
    }

    public String GetCall() {
        return GetHeaderValueNoQotes("call");
    }

    int getSocket() {
        return socket;
    }

    void setSocket(int socket) {
        this.socket = socket;
    }

    int getHTTPBytes() {
        return bytes;
    }

    String GetPeerIp() {
        return ip;
    }

    String getSID() {
        if (sid == null) {
            sid = FindByRx(regSIDCookie, 1, null);
        }
        if (sid == null) {
            String URL = getURL();
            if (URL != null) {
                sid = getRx(URL, regSessionID, 1, null);
            }
        }
        return sid;
    }

    void setSID(String sid) {
        this.sid = sid;
    }

    String getNameSpace() {
        return "";
    }

    void setIP(String ip) {
        this.ip = ip;
    }

    void setBytes(int bytes) {
        this.bytes += bytes;
    }

    private String getHTTPMethod() {
        if (method == null) {
            method = getRXValue(regHTTPMethod, 1, 0, null);
        }
        return method;
    }

    boolean isComplete() {
        Main.logger.debug("isComplete: " + m_MessageLines);
        if (m_MessageLines != null && m_MessageLines.size() > 0) {
            String s1 = m_MessageLines.get(0);
            if (s1.startsWith("PO") || s1.startsWith("HT")) {
                String s = GetHeaderValue("Content-Length", ':');
                if (s != null && s.length() > 0) {
                    int cl = Integer.parseInt(s);

                    return cl == 0 || getContentLengh() >= cl;
                }
                return false;
            }
        }
        return true;

    }

    /**
     * returns httpMessageBody of HTTP message
     *
     * @return empty string if there is no httpMessageBody
     */
    private String getBody() {
        if (httpMessageBody == null) {
            int idx = evalBodyIdx();
            if (idx >= 0) {
                List<String> subList = m_MessageLines.subList(idx, m_MessageLines.size());
                StringBuilder s1 = new StringBuilder();
                for (String string : subList) {
//                    if (s1.length() > 0) {
//                        s1.append("\n");
//                    }
                    if (!string.isEmpty()) {
                        s1.append(string);
                    }
                }
                if (s1.length() > 0) {
                    httpMessageBody = s1.toString();
                } else {
                    httpMessageBody = "";
                }

            }
        }
        return httpMessageBody;

    }

    /**
     * is calculated each time it is called
     *
     * @return
     */
    private int evalBodyIdx() {
        int ret = -1;
        if (m_MessageLines != null) {
            for (int i = 0; i < m_MessageLines.size(); i++) {
                if (m_MessageLines.get(i).length() == 0) {
                    if (m_MessageLines.size() > i + 1) {
                        ret = i + 1;
                        break;
                    }
                }
            }
        }
        return ret;
    }

    /**
     * value calculated once then stored
     *
     * @return
     */
    private int getBodyIdx() {
        if (bodyLineIdx < -1) {
            bodyLineIdx = evalBodyIdx();
        }
        return bodyLineIdx;
    }

    private int getContentLengh() {
        int i = evalBodyIdx();
        int ret = 0;

        if (i > 0) {
            ret++;
            for (; i < m_MessageLines.size(); i++) {
                ret += m_MessageLines.get(i).length();
            }
        }
        Main.logger.trace("bodyidx:" + evalBodyIdx() + " ret:" + ret);
        return ret;
    }

    void AddBytes(OrsHTTP orsHTTP) {
        for (String next : orsHTTP.m_MessageLines) {
            m_MessageLines.add(next);
        }
        Main.logger.trace("after bytes added:" + m_MessageLines);

    }

    JSONObject getJsonBody(String body) {
        if (_jsonBody == null) {
            if (body != null && !body.isEmpty()) {
                try {
                    _jsonBody = new JSONObject(body);
                } catch (JSONException ex) {
                    Main.logger.trace("Not JSON: [" + body + "]", ex);
                }
            }
        }
        return _jsonBody;

    }

    String getGMSService() {
        if (gmsService == null) {
            String body = getBody();
            if (body != null && !body.isEmpty()) {
                gmsService = getQueryKey(splitQuery(body), new String[]{"_service_id", "service_id"});
                if (gmsService == null) { //try reading httpMessageBody as JSON object
                    try {
                        JSONObject obj = getJsonBody(body);
                        if (obj != null) {
                            gmsService = obj.getString("_id");
                        }
                    } catch (JSONException e) {
                        Main.logger.trace("error reading JSON", e);
                    }

                }
            }
        }
        return gmsService;
    }

    String getHTTPRequest() {
        return getURL();
    }

    private String getURL() {
        if (url == null) {
            url = getRXValue(regHTTPURL, 1, 0, null);
        }
        if (url == null) {
            url = getRXValue(regHTTPResp, 1, 0, null);
        }
        return url;
    }

    String getParam1() {
        return jsonKeyValue("_action", getJsonBody(getBody()));
    }

    String getParam2() {
        return jsonKeyValue("_text", getJsonBody(getBody()));
    }

    private String jsonKeyValue(String _action, JSONObject obj) {
        if (obj != null) {
            try {
                return _action + ":" + obj.getString(_action);
            } catch (JSONException ex) {
                Main.logger.trace("error reading JSON", ex);
                return null;
            }
        }
        return null;
    }

    Long getHTTPResponseID() {
        return intOrDef(httpResponseID, null, 16);
    }

    void setHTTPResponseID(String group) {
        if (group != null && group.substring(0, 2).equalsIgnoreCase("0x")) {
            httpResponseID = group.substring(2);
        } else {
            httpResponseID = group;
        }
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        stmt.setInt(6, getSocket());
        stmt.setInt(7, getHTTPBytes());
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.IP, GetPeerIp()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.ORSSID, getSID()));
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.ORSNS, getNameSpace()));
        stmt.setBoolean(11, isInbound());
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.GMSService, getGMSService()));
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.HTTPRequest, getHTTPRequest()));
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.GMSMisc, getParam1()));
        setFieldInt(stmt, 15, Main.getRef(ReferenceType.GMSMisc, getParam2()));
        setFieldLong(stmt, 16, getHTTPResponseID());
        return true;

    }
}
