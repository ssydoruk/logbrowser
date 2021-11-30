/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OCSHTTP extends Message {

    private static final Pattern regIsInbound = Pattern.compile("(::mesage received)$");

    private static final Pattern regMsgName = Pattern.compile("^\\s+'(?:GSW_AGENT_REQ_TYPE|GSW_USER_EVENT)'\\s+'([^']+)'");

    private static final Pattern regIxnID = Pattern.compile("^\\s+'InteractionId'.+= \"([^\"]+)\"");

    private static final Pattern regMediaType = Pattern.compile("^\\s+attr_media_type.+= \"([^\"]+)\"");

    private static final Pattern regQueue = Pattern.compile("^\\s+attr_queue.+= \"([^\"]+)\"");

    private static final Pattern regPhone = Pattern.compile("^\\s+'GSW_PHONE'.+= \"([^\"]+)\"");
    private static final Pattern regcontact_info = Pattern.compile("^\\s+'contact_info'.+= \"([^\"]+)\"");

    private static final Pattern regGSW_RECORD_HANDLE = Pattern.compile("^\\s+'GSW_RECORD_HANDLE'\\s+\\'*(\\d+)");

    private static final Pattern regRefID = Pattern.compile("^\\s+attr_ref_id.+ = (\\d+)");

    private static final Pattern regGSW_CHAIN_ID = Pattern.compile("^\\s+'GSW_CHAIN_ID'.+= (\\d+)");
    private static final Pattern regchain_id = Pattern.compile("^\\s+'chain_id'.+= (\\d+)");
    private static final Pattern regGSW_CAMPAIGN = Pattern.compile("^\\s+'GSW_CAMPAIGN_NAME'.+= '([^']+)'");

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
        return FindByRx(regGSW_CAMPAIGN, 1, null);
    }

}
