/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSPASessionInfo extends Message {

    private static final Pattern regCampInfo = Pattern.compile("\\sOwnerDBID: (\\d+); Campaign DBID: (\\d+); Group DBID: (\\d+); Name: (.+);\\s*$");

    private int cgDBID;
    private String cgName;
    private int GroupDBID;
    private int CampaignDBID;

    public OCSPASessionInfo(ArrayList<String> messageLines) {
        super(TableType.OCSPASessionInfo, messageLines);

        try {
            Matcher m;
            if (messageLines.size() >= 2 && (m = regCampInfo.matcher(messageLines.get(1))).find()) {
                cgDBID = Integer.parseInt(m.group(1));
                CampaignDBID = Integer.parseInt(m.group(2));
                GroupDBID = Integer.parseInt(m.group(3));
                cgName = m.group(4);
            }
        } catch (NumberFormatException e) {
        }
    }

    int getCgDBID() {
        return cgDBID;
    }

    String getCgName() {
        return cgName;
    }

    int getGroupDBID() {
        return GroupDBID;
    }

    int getCampaignDBID() {
        return CampaignDBID;
    }

}
