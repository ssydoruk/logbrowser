/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import static com.myutils.logbrowser.indexer.Parser.genList;
import static com.myutils.logbrowser.indexer.Parser.splitQuery;

/**
 *
 * @author stepan_sydoruk
 */
public class URLProcessor {

    private final ArrayList<Pair<String, Pattern>> pathProcessing = new ArrayList<>();

    // in URL query, keyword:replaceWord
    private final HashMap<String, String> queryProcessing = new HashMap<>();

    /**
     * This array is filled once URI is processed
     */
    private final HashMap<String, String> queryValues = new HashMap<>();
    private final HashMap<String, String> pathValues = new HashMap<>();

    public String getpathValues(String key) {
        return pathValues.get(key);
    }

    public String getqueryValues(String key) {
        return queryValues.get(key);
    }

    public String processURL(String u) {
        try {

            queryValues.clear();
            pathValues.clear();

            if (u != null && !u.isEmpty()) {

                URI uri = new URI(u);
                HashMap<String, List<String>> splitQuery = splitQuery(uri.getQuery());
                if (splitQuery != null) {
                    for (Map.Entry<String, String> entry : queryProcessing.entrySet()) {

                        String s1 = findReplace(splitQuery, entry.getKey(), entry.getValue());
                        if (s1 != null) {
                            queryValues.put(entry.getKey(), s1);
                        }
                    }
                }
                String uriPath = uri.getPath();
                for (Pair<String, Pattern> entry : pathProcessing) {
                    Utils.Pair<String, String> rxReplace = null;
                    if ((rxReplace = Message.getRxReplace(uriPath, entry.getValue(), 1, entry.getKey())) != null) {
                        uriPath = rxReplace.getValue();
                        pathValues.put(entry.getKey(), rxReplace.getKey());
                        break;
                    }
                }

                return newURI(uriPath, splitQuery);
            }
        } catch (URISyntaxException uRISyntaxException) {
            Main.logger.error("Incorrect URL [" + u + "]");
        }
        return u;
    }

    private String findReplace(HashMap<String, List<String>> splitQuery, String search, String replace) {
        List<String> userName = splitQuery.get(search);
        if (userName != null && !userName.isEmpty()) {
            splitQuery.put(search, genList(replace));
            return userName.get(0);
        } else {
            return null;
        }
    }

    private String newURI(String path, HashMap<String, List<String>> splitQuery) {
        StringBuilder buf = new StringBuilder();
        buf.append(path);
        if (splitQuery != null && !splitQuery.isEmpty()) {
            buf.append('?');
            buf.append(makeQuery(splitQuery));
        }
        return buf.toString();
    }

    public String newURI(URI uri, HashMap<String, List<String>> splitQuery) {
        return newURI(uri.getPath(), splitQuery);

    }

    private StringBuilder makeQuery(HashMap<String, List<String>> splitQuery) {

        if (splitQuery != null) {
            StringBuilder ret = new StringBuilder();
            for (Map.Entry<String, List<String>> entry : splitQuery.entrySet()) {
                if (ret.length() > 0) {
                    ret.append("&");
                }
                ret.append(entry.getKey()).append('=');
                List<String> value = entry.getValue();
                if (value.size() > 1) {
                    ret.append("[");
                }
                for (int i = 0; i < value.size(); i++) {
                    if (i > 0) {
                        ret.append(",");
                    }
                    ret.append(value.get(i));
                }
                if (value.size() > 1) {
                    ret.append("]");
                }

            }
            return ret;
        } else {
            return null;
        }
    }

    void addPathProcessor(String PATH_UUID, Pattern compile) {
        pathProcessing.add(new Pair(PATH_UUID, compile));
    }

    void addQueryProcessor(String QUERY_userName, String user) {
        queryProcessing.put(QUERY_userName, user);
    }
}
