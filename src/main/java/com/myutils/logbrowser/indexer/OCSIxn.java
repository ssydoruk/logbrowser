/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSIxn extends Message {

    private static final Pattern regMsgName = Pattern.compile("^IxNServer .+- (SendRequest|EventReceived) : '([^']+)'");

    private static final Pattern regIxnID = Pattern.compile("^\\s+'InteractionId'.+= \"([^\"]+)\"");

    private static final Pattern regMediaType = Pattern.compile("^\\s+attr_media_type.+= \"([^\"]+)\"");

    private static final Pattern regQueue = Pattern.compile("^\\s+attr_queue.+= \"([^\"]+)\"");

    private static final Pattern regPhone = Pattern.compile("^\\s+'GSW_PHONE'.+= \"([^\"]+)\"");
    private static final Pattern regcontact_info = Pattern.compile("^\\s+'contact_info'.+= \"([^\"]+)\"");

    private static final Pattern regGSW_RECORD_HANDLE = Pattern.compile("^\\s+'GSW_RECORD_HANDLE'.+= (\\d+)");

    private static final Pattern regRefID = Pattern.compile("^\\s+attr_ref_id.+ = (\\d+)");

    private static final Pattern regGSW_CHAIN_ID = Pattern.compile("^\\s+'GSW_CHAIN_ID'.+= (\\d+)");
    private static final Pattern regchain_id = Pattern.compile("^\\s+'chain_id'.+= (\\d+)");

    public OCSIxn(ArrayList messageLines) {
        super(TableType.OCSIxn, messageLines);
    }

    public String GetSid() {
        return GetHeaderValueNoQotes("session");
    }

    public String GetCall() {
        return GetHeaderValueNoQotes("call");
    }

    String GetMessageName() {
        return FindByRx(regMsgName, 2, "");
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

}
