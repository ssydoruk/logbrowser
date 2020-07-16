/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Pattern;

public final class OCSHTTP extends Message {

    final private static Pattern regIsInbound = Pattern.compile("(::mesage received)$");

    final private static Pattern regMsgName = Pattern.compile("^\\s+\'(?:GSW_AGENT_REQ_TYPE|GSW_USER_EVENT)\'\\s+\'([^\']+)\'");

    final private static Pattern regIxnID = Pattern.compile("^\\s+'InteractionId'.+= \"([^\"]+)\"");

    final private static Pattern regMediaType = Pattern.compile("^\\s+attr_media_type.+= \"([^\"]+)\"");

    final private static Pattern regQueue = Pattern.compile("^\\s+attr_queue.+= \"([^\"]+)\"");

    final private static Pattern regPhone = Pattern.compile("^\\s+\'GSW_PHONE\'.+= \"([^\"]+)\"");
    final private static Pattern regcontact_info = Pattern.compile("^\\s+\'contact_info\'.+= \"([^\"]+)\"");

    final private static Pattern regGSW_RECORD_HANDLE = Pattern.compile("^\\s+\'GSW_RECORD_HANDLE\'\\s+\\'*(\\d+)");

    final private static Pattern regRefID = Pattern.compile("^\\s+attr_ref_id.+ = (\\d+)");

    final private static Pattern regGSW_CHAIN_ID = Pattern.compile("^\\s+\'GSW_CHAIN_ID\'.+= (\\d+)");
    final private static Pattern regchain_id = Pattern.compile("^\\s+\'chain_id\'.+= (\\d+)");
    final private static Pattern regGSW_CAMPAIGN = Pattern.compile("^\\s+\'GSW_CAMPAIGN_NAME\'.+= \'([^\']+)\'");

    public OCSHTTP(ArrayList messageLines) {
        super(TableType.OCSHTTP, messageLines);
        FindByRx(regIsInbound, 1, null);
        SetInbound(FindByRx(regIsInbound, 1, null) == null);
    }

    String GetMessageName() {
        return FindByRx(regMsgName, 1, "");
    }

    String GetIxnID() {
        return FindByRx(regIxnID, 1, "");
    }

    String GetMedia() {
        return FindByRx(regMediaType, 1, "");
    }

    String GetIxnQueue() {
        return FindByRx(regQueue, 1, "");
    }

    String GetOutPhone() {
        String ret = FindByRx(regPhone, 1, "");
        if (ret == null || ret.length() == 0) {
            ret = FindByRx(regcontact_info, 1, "");
        }
        return ret;
    }

    long getRecHandle() {
        return FindByRx(regGSW_RECORD_HANDLE, 1, -1);
    }

    long getM_refID() {
        return FindByRx(regRefID, 1, 0);
    }

    long getChainID() {
        long ret = FindByRx(regGSW_CHAIN_ID, 1, -1);
        if (ret < 0) {
            ret = FindByRx(regchain_id, 1, -1);
        }
        return ret;
    }

    String GetCampaignName() {
        return FindByRx(regGSW_CAMPAIGN, 1, (String) null);
    }

}
