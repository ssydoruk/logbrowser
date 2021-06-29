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

public class LCAClient extends Message {

    private static final Matcher reqCreateApplication = Pattern.compile("CREATE Application <(\\w+), ([^,]+), pid=(\\w+),").matcher("");
    private static final Matcher reqRequestor = Pattern.compile("^([^:]+):").matcher(""); // implying that timestamp for first line is cut out
    private static final Matcher reqModeChange = Pattern.compile("to (\\w+) for App<(\\w+), (.+), pid=(\\w+), ([^,]+), (\\w+)>$").matcher("");
    private static final Matcher reqApp = Pattern.compile("App\\s*<(\\w+), (.+), pid=(\\w+),").matcher("");
    private static final Matcher reqEvent = Pattern.compile("\\[(\\w+)").matcher(""); // implying that timestamp for first line is cut out
    private static final Matcher reqRegisterApp = Pattern.compile("\\[RRequestRegisterApplication\\] App<(\\w+), ([^,]+), pid=(\\w+)").matcher("");
    private static final Matcher reqIPAddress = Pattern.compile("IP-address: (\\w+\\.\\w+\\.\\w+\\.\\w+)").matcher("");
    private static final Matcher reqSCS = Pattern.compile("CREATE SCSRequester .+:(\\w+),\\s*'(.+)'\\)").matcher("");
    private static final Matcher reqFD = Pattern.compile("new client (\\w+)").matcher("");
    private static final Matcher reqDisconnectApp = Pattern.compile("App<(\\w+), (.+), pid=(\\w+)").matcher("");
    private static final Matcher reqDisconnectSCS = Pattern.compile("SCSRequester '(.+)' disconnected, fd=(\\w+)").matcher("");

    private String event = null;
    private String requestor;
    private String IPAddress = null;
    private String appType = null;
    private int FD;
    private boolean connected = true;

    private String newMode;
    private int appDBID;
    private String appName;
    private int PID;
    private String OldMode;
    private String status;
    private String host;

    LCAClient(String s) {
        super(TableType.LCAClient, s);
    }

    public LCAClient() {
        super(TableType.LCAClient);
    }

    LCAClient(ArrayList<String> m_MessageContents) {
        super(TableType.LCAClient, m_MessageContents);
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
        if (m_MessageLines.size() > 1 && (m = reqModeChange.reset(m_MessageLines.get(1))).find()) {
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

    void parseConnect() {
        FD = intOrDef(FindByRx(reqFD, 1, null), 0);
        Matcher m = FindRx(reqRegisterApp);
        if (m != null) {
            setAppName(m.group(2));
            setAppDBID(m.group(1));
            setPID(m.group(3));
            setSCS(false);
        } else {
            m = FindRx(reqSCS);
            if (m != null) {
                setAppName(m.group(2));
                FD = intOrDef(m.group(1), 0);
                setSCS(true);

            }
        }
        IPAddress = FindByRx(reqIPAddress, 1, null);
    }

    String getAppType() {
        return this.appType;
    }

    String getIP() {
        return this.IPAddress;
    }

    int getFD() {
        return this.FD;
    }

    void setFD(String group) {
        FD = intOrDef(group, -1);
    }

    void parseDisconnect() {
        Matcher m = FindRx(reqDisconnectApp);
        if (m != null) {
            setAppName(m.group(2));
            setAppDBID(m.group(1));
            setPID(m.group(3));
            setSCS(false);
            this.connected = false;
        } else {
            m = FindRx(reqDisconnectSCS);
            if (m != null) {
                setAppName(m.group(1));
                FD = intOrDef(m.group(2), 0);
                setSCS(true);
                connected = false;
            }
        }
    }

    boolean isConnected() {
        return connected;
    }

    void setConnected(boolean b) {
        connected = b;
    }

    void setSCS(boolean isSCS) {
        if (isSCS) {
            appType = FileInfoType.type_SCS.toString();
        } else {
            appType = "LCAClient";
        }
    }

}
