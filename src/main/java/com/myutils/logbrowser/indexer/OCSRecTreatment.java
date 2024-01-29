/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
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

    public OCSRecTreatment(ArrayList<String> newMessageLines, int fileID) {
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

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        stmt.setInt(6, getrecHandle());
        stmt.setInt(7, getchID());
        stmt.setInt(8, getchNum());
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.OCSCAMPAIGN, getcamp()));
        stmt.setInt(10, getrecType());
        return true;

    }
}
