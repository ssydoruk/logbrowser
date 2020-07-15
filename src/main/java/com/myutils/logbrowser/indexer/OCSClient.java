/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class OCSClient extends Message {

    final private static Pattern regMsgName = Pattern.compile("^\\s*GSW_CM_MessageType.+\\[([^\\]]+)\\]$");

    final private static Pattern regCampID = Pattern.compile("^\\s*GSW_CM_AttrCampaignID\\s*(\\d+)");

    final private static Pattern regGetGroupID = Pattern.compile("^\\s*GSW_CM_AttrGroupID\\s*(\\d+)");

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
