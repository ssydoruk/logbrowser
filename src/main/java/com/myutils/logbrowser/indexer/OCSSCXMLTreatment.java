/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.regex.Pattern;

public class OCSSCXMLTreatment extends Message {

    //Created scxml session '0000017F-040206B5-0001'
    private static final Pattern regSessionID = Pattern.compile("Created scxml session '([\\w-]+)'");
    private final String campDBID;
    private final String recHandle;
    private final String chainID;
    private final String rest;

    OCSSCXMLTreatment(String campDBID, String recHandle, String chainID, String rest) {
        super(TableType.OCSSCXMLTreatment);
        this.campDBID = campDBID;
        this.recHandle = recHandle;
        this.chainID = chainID;
        this.rest = rest;
    }

    int getcgdbid() {
        return getIntOrDef(campDBID, 0);
    }

    int getrecHandle() {
        return getIntOrDef(recHandle, 0);
    }

    int getchID() {
        return getIntOrDef(chainID, 0);
    }

    String getSessID() {
        return FindByRx(regSessionID, rest, 1, "");
    }

}
