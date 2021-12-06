/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

public class IxnDBActivity extends Message {

    private final String activity;
    private final String ixnid;
    private String reqid = null;

    IxnDBActivity(String activity, String reqid, String ixnid, int fileID) {
        super(TableType.IxnDB, fileID);
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
