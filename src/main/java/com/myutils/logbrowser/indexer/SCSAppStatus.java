/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

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

}
