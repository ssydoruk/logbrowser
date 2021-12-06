/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

public class LCAAppStatus extends Message {

    private static final Pattern reqCreateApplication = Pattern.compile("CREATE Application <(\\d+), ([^,]+), pid=(\\d+),");
    private static final Pattern reqRequestor = Pattern.compile("^([^:]+):"); // implying that timestamp for first line is cut out
    private static final Pattern reqModeChange = Pattern.compile("to (\\w+) for App<(\\d+), (.+), pid=(\\d+), ([^,]+), (\\w+)>$");
    private static final Pattern reqApp = Pattern.compile("App\\s*<(\\d+), (.+), pid=(\\d+),");
    private static final Pattern reqEvent = Pattern.compile("\\[(\\w+)"); // implying that timestamp for first line is cut out

    private String event = null;
    private String requestor;

    private String newMode;
    private int appDBID;
    private String appName;
    private int PID;
    private String OldMode;
    private String status;
    private String host;

    public LCAAppStatus( int fileID) {
        super(TableType.LCAAppStatus, fileID);
    }

    LCAAppStatus(ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.LCAAppStatus, m_MessageContents, fileID);
    }

    public String getHost() {
        return host;
    }

    void setHost(String group) {
        this.host = group;
    }

    void setMode(String group) {
        this.newMode = group;

    }

    public String getNewMode() {
        return newMode;
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

    void parseStart() {
        requestor = FindByRx(reqRequestor, m_MessageLines.get(0), 1, null);
        Matcher m = FindRx(reqCreateApplication);
        if (m != null) {
            setAppName(m.group(2));
            setAppDBID(m.group(1));
            setPID(m.group(3));
        }
        setEvent("RequestStartApplication");
    }

    void parseChangeRunMode() {
        requestor = FindByRx(reqRequestor, m_MessageLines.get(0), 1, null);
        Matcher m;
        if (m_MessageLines.size() > 1 && (m = reqModeChange.matcher(m_MessageLines.get(1))).find()) {
            setMode(m.group(1));
            setAppName(m.group(3));
            setAppDBID(m.group(2));
            setPID(m.group(4));
            setOldMode(m.group(6));
            setStatus(m.group(4));
        }
        setEvent("RequestChangeAppRunMode");
    }

    void parseOtherRequest() {
        requestor = FindByRx(reqRequestor, m_MessageLines.get(0), 1, null);
        Matcher m = FindRx(reqApp);
        if (m != null) {
            setAppName(m.group(2));
            setAppDBID(m.group(1));
            setPID(m.group(3));
        }
        setEvent(FindByRx(reqEvent, m_MessageLines.get(0), 1, null));
    }

    String getRequestor() {
        return requestor;
    }

}
