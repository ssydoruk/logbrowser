/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static Utils.Util.intOrDef;

public class OCSIcon extends Message {

    private String newMode;
    private int appDBID;
    private String appName;
    private int PID;
    private String OldMode;
    private String status;
    private String host;

    public OCSIcon(int fileID) {
        super(TableType.OCSIcon, fileID);
    }

    OCSIcon(ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.OCSIcon, m_MessageContents, fileID);
    }

    public String getHost() {
        return host;
    }

    void setHost(String group) {
        this.host = group;
    }

    public String getNewMode() {
        return newMode;
    }

    void setNewMode(String group) {
        this.newMode = group;

    }

    public int getAppDBID() {
        return appDBID;
    }

    void setAppDBID(String group) {
        this.appDBID = intOrDef(group, -1);
    }

    public String getAppName() {
        return appName;
    }

    void setAppName(String group) {
        this.appName = group;
    }

    public int getPID() {
        return PID;
    }

    void setPID(String group) {
        this.PID = intOrDef(group, -1);
    }

    public String getOldMode() {
        return OldMode;
    }

    void setOldMode(String group) {
        this.OldMode = group;

    }

    public String getStatus() {
        return status;
    }

    void setStatus(String group) {
        this.status = group;

    }

    String getIconEvent() {
        return GetHeaderValue("\t\tEvtCode");
    }

    Integer getSwitchDBID() {
        return intOrDef(GetHeaderValue("\t\tSwID"), (Integer) null);
    }

    Integer getGroupDBID() {
        return intOrDef(GetHeaderValue("\t\tGrpID"), (Integer) null);
    }

    String getIconCause() {
        return GetHeaderValue("\t\tCause");
    }

    Integer getCampaignDBID() {
        return intOrDef(GetHeaderValue("\t\tCmpID"), (Integer) null);
    }

    Integer getListDBID() {
        return intOrDef(GetHeaderValue("\t\tLstID"), (Integer) null);
    }

    Integer getChainDBID() {
        return intOrDef(GetHeaderValue("\t\tChID"), (Integer) null);
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.OCSIconEvent, getIconEvent()));
        setFieldInt(stmt, 7, getSwitchDBID());
        setFieldInt(stmt, 8, getGroupDBID());
        setFieldInt(stmt, 9, getCampaignDBID());
        setFieldInt(stmt, 10, getListDBID());
        setFieldInt(stmt, 11, getChainDBID());
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.OCSIconCause, getIconCause()));
        return true;


    }
}
