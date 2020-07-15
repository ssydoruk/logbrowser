/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class OCSIxn extends Message {

    final private static Pattern regMsgName = Pattern.compile("^IxNServer .+- (SendRequest|EventReceived) : '([^']+)'");

    final private static Pattern regIxnID = Pattern.compile("^\\s+'InteractionId'.+= \"([^\"]+)\"");

    final private static Pattern regMediaType = Pattern.compile("^\\s+attr_media_type.+= \"([^\"]+)\"");

    final private static Pattern regQueue = Pattern.compile("^\\s+attr_queue.+= \"([^\"]+)\"");

    final private static Pattern regPhone = Pattern.compile("^\\s+\'GSW_PHONE\'.+= \"([^\"]+)\"");
    final private static Pattern regcontact_info = Pattern.compile("^\\s+\'contact_info\'.+= \"([^\"]+)\"");

    final private static Pattern regGSW_RECORD_HANDLE = Pattern.compile("^\\s+\'GSW_RECORD_HANDLE\'.+= (\\d+)");

    final private static Pattern regRefID = Pattern.compile("^\\s+attr_ref_id.+ = (\\d+)");

    final private static Pattern regGSW_CHAIN_ID = Pattern.compile("^\\s+\'GSW_CHAIN_ID\'.+= (\\d+)");
    final private static Pattern regchain_id = Pattern.compile("^\\s+\'chain_id\'.+= (\\d+)");

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
