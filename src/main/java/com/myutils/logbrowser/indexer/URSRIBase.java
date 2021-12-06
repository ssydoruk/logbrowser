/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

public abstract class URSRIBase extends Message {

    public static final int MAX_WEB_PARAMS = 5;
    private static final HashMap<String, Integer> keysPos = new HashMap<>(MAX_WEB_PARAMS);
    private final List<Pair<String, String>> allParams;
    protected String url;
    private String ConnID;
    private String clientApp;
    private String clientID;
    private String ORSSID;
    private String func;
    private String subFunc;
    private String refid;
    private String UUID = null;
    private String uriParams;
    public URSRIBase(TableType t, ArrayList messageLines, int fileID) {
        this(t, fileID);
        setMessageLines(m_MessageLines);
    }
    URSRIBase(TableType t, int fileID) {
        super(t, fileID);
        allParams = new ArrayList<>();
    }

    static void addIndexes(DBTable tab) {
        for (int i = 1; i <= MAX_WEB_PARAMS; i++) {
            tab.addIndex("attr" + i + "id");
            tab.addIndex("value" + i + "id");
        }
    }

    static StringBuilder getTableFields() {
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i <= URSRIBase.MAX_WEB_PARAMS; i++) {
            buf.append(",attr").append(i).append("id int");
            buf.append(",value").append(i).append("id int");
        }
        return buf;
    }

    static StringBuilder getTableFieldsAliases() {
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i <= URSRIBase.MAX_WEB_PARAMS; i++) {
            buf.append(",?,?");
        }
        return buf;
    }

    static void setTableValues(int baseRecNo, PreparedStatement stmt, URSRIBase rec, DBTable tab) throws SQLException {
        ArrayList<Pair<String, String>> arr = rec.getAttrs();
        for (int i = 0; i < URSRIBase.MAX_WEB_PARAMS; i++) {
            if (arr != null && i < arr.size()) {
                tab.setFieldInt(stmt, baseRecNo + i * 2, Main.getRef(ReferenceType.Misc, arr.get(i).getKey()));
                tab.setFieldInt(stmt, baseRecNo + i * 2 + 1, Main.getRef(ReferenceType.Misc, arr.get(i).getValue()));

            } else {
                tab.setFieldInt(stmt, baseRecNo + i * 2, null);
                tab.setFieldInt(stmt, baseRecNo + i * 2 + 1, null);

            }
        }

    }

    public static Map<String, List<String>> splitQuery(String url) throws UnsupportedEncodingException {
        if (StringUtils.isNotBlank(url)) {
            final Map<String, List<String>> query_pairs = new LinkedHashMap<>();
            final String[] pairs = url.split("&");
            for (String pair : pairs) {
                final int idx = pair.indexOf("=");
                final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                if (!query_pairs.containsKey(key)) {
                    query_pairs.put(key, new LinkedList<>());
                }
                final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                query_pairs.get(key).add(value);
            }
            return query_pairs;
        } else {
            return null;
        }
    }

    public int getRefid() {
        return getIntOrDef(refid, -1);
    }

    public void setRefid(String refid) {
        this.refid = refid;
    }

    public String getSubFunc() {
        return null;
    }

    public void setSubFunc(String subFunc) {
        this.subFunc = subFunc;
    }

    public String getFunc() {
        return func;
    }

    public void setFunc(String func) {
        this.func = func;
    }

    public String getConnID() {
        return ConnID;
    }

    public void setConnID(String ConnID) {
        this.ConnID = ConnID;
    }

    public String getClientApp() {
        return clientApp;
    }

    public void setClientApp(String clientApp) {
        this.clientApp = clientApp;
    }

    public int getClientID() {
        return getIntOrDef(clientID, -1);
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getORSSID() {
        return ORSSID;
    }

    public void setORSSID(String ORSSID) {
        this.ORSSID = ORSSID;
    }

    String getUUID() {
        return UUID;
    }

    void setUUID(String substring) {
        this.UUID = substring;
    }

    public String getUrl() {
        return url;
    }

    String getUriParams() {
        return uriParams;
    }

    private int getIndex(List<Pair<String, String>> allParams, Pair<String, String> pair) {
        Integer get = keysPos.get(pair.getKey());
        if (get != null) {
            return get;
        } else {
            int i = 0;
            for (; i < allParams.size(); i++) {
                if (allParams.get(i) == null) {
                    keysPos.put(pair.getKey(), i);
                    return i;
                }
            }
            i--; // not to get out of bounds
            keysPos.put(pair.getKey(), i);
            return i;

        }
    }

    void setURIParams(String uriParams) {
//        try {
        if (uriParams != null && !uriParams.isEmpty()) {
            this.uriParams = uriParams;

//                Map<String, List<String>> a = splitQuery(reqURI);
//                for (Map.Entry<String, List<String>> entry : a.entrySet()) {
//                    String key = entry.getKey();
//                    List<String> value = entry.getValue();
//                    Pair<String, String> p = new Pair<>(key, StringUtils.join(value, ","));
//
//                    allParams.set(getIndex(allParams, p), p);
//
//                }
        }
//        } catch (UnsupportedEncodingException ex) {
//            this.URL = null;
//            logger.error("fatal: ",  ex);
//        }
    }

    ArrayList<Pair<String, String>> getAttrs() {
        try {
            ArrayList<Pair<String, String>> ret = new ArrayList();
            Map<String, List<String>> a = splitQuery(this.uriParams);
            if (a != null) {
                for (Map.Entry<String, List<String>> entry : a.entrySet()) {
                    String key = entry.getKey();
                    List<String> value = entry.getValue();
                    ret.add(new Pair(key, StringUtils.join(value, ",")));
                }
            }
            return ret;
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
        }
        return null;
    }

    void setURIParts(String url, String uriParams) throws Exception {
        String[] split = StringUtils.split(url, '/');
        setURIParams(uriParams);
        switch (split.length) {
            case 4:
                //ConnID specified
                setConnID(split[2]);
                setFunc(split[3]);
                split[2] = "<ConnID>";
                this.url = StringUtils.join(split, "/");
                break;
            case 3:
                setFunc(split[2]);
                break;
            case 5:
                if (split[3].equals("func") || split[3].equals("reply")) {
                    setFunc(split[4]);
                    String id = "";
                    if (split[2].startsWith("@")) {
                        setUUID(split[2].substring(1));
                    }
                    break;
                } else if (split[3].equals("lvq")) {
                    setFunc(split[3]);
                    setConnID(split[2]);
                    break;
                }

            default:
                throw new Exception("Strange URI [" + url + "] l=" + split.length);
        }
        String subfun = Parser.getQueryKey(Parser.splitQuery(uriParams), new String[]{"name", "strategy"});
        if (subfun != null) {
            setSubFunc(subfun);
        } else {
            setSubFunc("(empty)");
        }
    }

}
