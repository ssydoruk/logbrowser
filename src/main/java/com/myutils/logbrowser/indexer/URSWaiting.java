/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URSWaiting extends Message {


    private static final Pattern regMedia = Pattern.compile("\\<([^\\>]+)\\> type \\<([^\\>]+)\\>");
    private final String connID;
    private String agent;
    private String targetType;
    public URSWaiting(String ag, String connID) {
        super(TableType.URSWaiting);
        this.connID = connID;
        Matcher m;
        if ((m = regMedia.matcher(ag)).find()) {
            this.agent = m.group(1);
            this.targetType = m.group(2);
        }
        
    }

    public String getConnID() {
        return connID;
    }

    public String getAgent() {
        return agent;
    }

    public String getTargetType() {
        return targetType;
    }


}
