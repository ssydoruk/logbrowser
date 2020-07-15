/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static Utils.Util.intOrDef;
import java.util.ArrayList;

public class SCSAppStatus extends Message {

    private String event = null;

    public String getHost() {
        return host;
    }

    private String newMode;
    private int appDBID;
    private String appName;
    private int PID;
    private String OldMode;
    private String status;
    private String host;

    public SCSAppStatus() {
        super(TableType.SCSAppStatus);
    }

    SCSAppStatus(ArrayList<String> m_MessageContents) {
        super(TableType.SCSAppStatus, m_MessageContents);
    }

    void setNewMode(String group) {
        this.newMode = group;

    }

    void setAppDBID(String group) {
        this.appDBID = intOrDef(group, -1);
    }

    void setAppName(String group) {
        this.appName = group;
    }

    void setPID(String group) {
        this.PID = intOrDef(group, -1);
    }

    void setOldMode(String group) {
        this.OldMode = group;

    }

    void setStatus(String group) {
        this.status = group;

    }

    public String getNewMode() {
        return newMode;
    }

    public int getAppDBID() {
        return appDBID;
    }

    public String getAppName() {
        return appName;
    }

    public int getPID() {
        return PID;
    }

    public String getOldMode() {
        return OldMode;
    }

    public String getStatus() {
        return status;
    }

    void setHost(String group) {
        this.host = group;
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
