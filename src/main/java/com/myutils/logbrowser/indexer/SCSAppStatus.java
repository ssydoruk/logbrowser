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

public class SCSAppStatus extends Message {

    private String event = null;

    private String newMode;
    private int appDBID;
    private String appName;
    private int PID;
    private String OldMode;
    private String status;
    private String host;

    public SCSAppStatus( int fileID) {
        super(TableType.SCSAppStatus, fileID);
    }

    SCSAppStatus(ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.SCSAppStatus, m_MessageContents, fileID);
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

    String getEvent() {
        return event;
    }

    void setEvent(String request_change_RUNMODE) {
        event = request_change_RUNMODE;
    }

    String getRequestor() {
        return null;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.APPRunMode, getNewMode()));
        stmt.setInt(7, getAppDBID());
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.App, getAppName()));
        stmt.setInt(9, getPID());
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.APPRunMode, getOldMode()));
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.appStatus, getStatus()));
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.Host, getHost()));
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.SCSEvent, getEvent()));
        return true;

    }
}
