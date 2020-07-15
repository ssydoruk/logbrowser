/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

public class IxnDBActivity extends Message {

    private final String activity;
    private String reqid = null;
    private final String ixnid;

    IxnDBActivity(String activity, String reqid, String ixnid) {
        super(TableType.IxnDB);
        this.activity = activity;
        this.reqid = reqid;
        this.ixnid = ixnid;
    }

    public String getActivity() {
        return activity;
    }

    public int getReqid() {
        try {
            return Integer.parseInt(reqid);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getIxnid() {
        return ixnid;
    }

}
