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
public class OCSRecCreate extends Message {

    private static final Pattern regRecHandle = Pattern.compile("^\\s*RecordHandle = (\\d+)\\s+ChainID = (\\d+)\\s+ChainNum = (\\d+)");
    private static final Pattern regCampaignName = Pattern.compile("^\\s*CampaignName = (.+)");
    private static final Pattern regRecordType = Pattern.compile("^\\s*tRecordType = (\\d+)");
    private static final Pattern regListTab = Pattern.compile("^\\s*ListName = (.+) TableName = (.+)");
    private static final Pattern regPhone = Pattern.compile("^\\s*Phone = (.+)");

    private int recHandle = -2;
    private int chanID = -2;
    private int ChainNum = -2;
    private boolean listTabParsed = false;
    private String listName;
    private String tabName;
    private boolean isCreate = true;

    public OCSRecCreate(ArrayList newMessageLines, int fileID) {
        this( fileID);
        m_MessageLines = newMessageLines;
        Matcher m;
//        Main.logger.info("OCSRecCreate" + ((m_MessageLines == null) ? "NULL" : m_MessageLines.toString()));
        if ((m = FindRx(regRecHandle)) != null) {
            recHandle = toInt(m.group(1), -1);
            chanID = toInt(m.group(2), -1);
            ChainNum = toInt(m.group(3), -1);
//            Main.logger.info("OCSRecCreate: recHandle=" + recHandle);
        }
    }

    OCSRecCreate( int fileID) {
        super(TableType.OCSRecCreate, fileID);
    }

    public void setIsCreate(boolean isCreate) {
        this.isCreate = isCreate;
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
        return FindByRx(regCampaignName, 1, null);
    }

    int getrecType() {
        Matcher m;
        if ((m = FindRx(regRecordType)) != null) {
            return toInt(m.group(1), -1);
        }
        return -1;
    }

    String getCallingList() {
        findListTab();
        return this.listName;
//        return FindByRx(regRecHandle, 1, null);
    }

    String getTabName() {
        findListTab();
        return this.tabName;
    }

    private void findListTab() {
        if (!this.listTabParsed) {
            Matcher m = FindRx(regListTab);
//            Main.logger.trace("findListTab -1-");
            if (m != null) {
//                Main.logger.trace("findListTab -2-");
                listName = m.group(1);
                this.tabName = m.group(2);
            }
            listTabParsed = true;
//            System.exit(0);
        }
    }

    String getDN() {
        return FindByRx(regPhone, 1, null);
    }

    String getAction() {
        return (isCreate) ? "create" : "destroy";
    }

    void setRecHandle(String group) {
        recHandle = toInt(group, -1);
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
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.OCSCallList, getCallingList()));
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.OCSCallListTable, getTabName()));
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.DN, cleanDN(getDN())));
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.OCSRecordAction, getAction()));
        return true;
    }
}
