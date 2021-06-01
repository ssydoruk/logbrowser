/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSClient extends Message {

    final private static Matcher regMsgName = Pattern.compile("^\\s*GSW_CM_MessageType.+\\[([^\\]]+)\\]$").matcher("");

    final private static Matcher regCampID = Pattern.compile("^\\s*GSW_CM_AttrCampaignID\\s*(\\d+)").matcher("");

    final private static Matcher regGetGroupID = Pattern.compile("^\\s*GSW_CM_AttrGroupID\\s*(\\d+)").matcher("");

    public OCSClient(ArrayList messageLines) {
        super(TableType.OCSClient, messageLines);
    }

    String GetMessageName() {
        return FindByRx(regMsgName, 1, "");
    }

    int GetCampaignDBID() {
        return FindByRx(regCampID, 1, -1);
    }

    int GetGroupDBID() {
        return FindByRx(regGetGroupID, 1, -1);
    }

}
