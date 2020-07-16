/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static Utils.Util.intOrDef;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public LCAAppStatus() {
        super(TableType.LCAAppStatus);
    }

    LCAAppStatus(ArrayList<String> m_MessageContents) {
        super(TableType.LCAAppStatus, m_MessageContents);
    }
    public String getHost() {
        return host;
    }

    void setMode(String group) {
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
