/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author terry The class Replicates TLibMessage
 */
public final class OCSRecTreatment extends Message {

    private static final Pattern regRecHandle = Pattern.compile("^\\tRecordHandle = (\\d+)\\s+ChainID = (\\d+)\\s+ChainNum = (\\d+)");
    private static final Pattern regCampaignName = Pattern.compile("^\\tCampaignName = (.+)");
    private static final Pattern regRecordType = Pattern.compile("^\\tRecordType = (\\d+)");

    private int recHandle = -1;
    private int chanID = -1;
    private int ChainNum = -1;

    public OCSRecTreatment(ArrayList newMessageLines, int fileID) {
        super(TableType.OCSTreatment, fileID);
        m_MessageLines = newMessageLines;
        Matcher m;
        if ((m = FindRx(regRecHandle)) != null) {
            recHandle = toInt(m.group(1), -1);
            chanID = toInt(m.group(2), -1);
            ChainNum = toInt(m.group(3), -1);
        }
    }

    int getrecHandle() {
        return recHandle;
    }

    int getchID() {
        return chanID;
    }

    int getchNum() {
        return ChainNum;
    }

    String getcamp() {
        Matcher m;
        if ((m = FindRx(regCampaignName)) != null) {
            return m.group(1);
        }
        return "";
    }

    int getrecType() {
        Matcher m;
        if ((m = FindRx(regRecordType)) != null) {
            return toInt(m.group(1), -1);
        }
        return -1;
    }

}
