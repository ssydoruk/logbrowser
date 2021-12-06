/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSAgentAssignment extends Message {

    private static final Pattern regCampaignAssignment = Pattern.compile("^There are (\\d+) Agents/Places assigned to Session '([^']+)'");
    private String cgName;
    private int agsNo;

    public OCSAgentAssignment(ArrayList messageLines, int fileID) {
        super(TableType.OCSAssignment, messageLines, fileID);
        Matcher m;
        if ((m = regCampaignAssignment.matcher((CharSequence) messageLines.get(0))).find()) {
            agsNo = Integer.parseInt(m.group(1));
            cgName = m.group(2);
        }
    }

    @Override
    public String toString() {
        return "OCSAgentAssignment{" + "cgName=" + cgName + ", agsNo=" + agsNo + '}' + super.toString();
    }

    String getCgName() {
        return cgName;
    }

    int getAgentsNo() {
        return agsNo;
    }

    boolean getAssignmentUsed() {
        return true;
    }

}
