/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

public class URSCONNIDSID extends Message {

    private final String ConnID;
    private final String SIDID;

    URSCONNIDSID(String ConnID, String SIDID) {
        super(TableType.URSCONNIDSID);
        this.ConnID = ConnID;
        this.SIDID = SIDID;
    }

    public String GetSid() {
        return SIDID;
    }

    String GetConnID() {
        return ConnID;
    }

}
