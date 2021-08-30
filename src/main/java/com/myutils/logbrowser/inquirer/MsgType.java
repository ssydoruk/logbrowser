package com.myutils.logbrowser.inquirer;

public enum MsgType {

    UNKNOWN("UNKNOWN"),
    SIP("SIP"),
    SIPOPTIONS("SIPOPTIONS"),
    SIPREGISTER("SIPREGISTER"),
    TLIB("TLIB"),
    JSON("JSON"),
    CONNID(""),
    CALLID(""),
    CUSTOM("Custom"),
    PROXY("SIP"),
    OCSClient("OCSClient"),
    OCS("OCS"),
    OCSDB("OCSDB"),
    OCSCG("OCSCG"),
    OCSPASI("OCSPASI"),
    OCSPREDI("OCSPedictiveInfo"),
    OCSPAAGI("OCSPAAGI"),
    OCSPAEVI("OCSPAEVI"),
    OCSSTATEV("OCSSTATEV"),
    OCSASSIGNMENT("OCSASSIGNMENT"),
    OCSRECCREATE("OCSRECCREATE"),
    OCSHTTP("OCSHTTP"),
    OCSSCXMLTREATMENT("OCSSCXMLTREATMENT"),
    OCSSCXMLSCRIPT("OCSSCXMLSCRIPT"),
    OCSINTERACTION("OCSINTERACTION"),
    ORS("ORS"),
    ORSURS("ORSURS"),
    ORSHTTP("ORSHTTP"),
    ORSSESSION("ORSSESSION"),
    ORSINTERACTION("ORSINTERACTION"),
    ORSM("ORSM"),
    URSSTRATEGY("URSSTRATEGY"),
    URSRLIB("URSRLIB"),
    TSCPCALL("TSCPCALL"),
    GENESYS_MSG("GENESYS_MSG"),
    INTERACTION("INTERACTION"),
    OCSTREATMENT("OCSTREATMENT"),
    URSRI("URSWEB"),
    STATSAgent("StatServerAgent"),
    STATSCapacity("StatServerCapacity"),
    ORSMEXTENSION("ORSMEXTENSION"),
    GENESYS_CFG("CONFIGURATION"),
    TLIB_ISCC("TLIB_ISCC"),
    URSSTAT("URSSTAT"),
    GENESYS_TIMESTAMP("TIMESTAMP_DIFF"),
    ORS_DELAYS("ORS_DELAYS"),
    URSSTRATEGYINIT("URSSTRATEGYINIT"),
    URSWAITING("URSWAITING"),
    URSVQ("URSGUID"),
    GENESYS_URS_MSG("GENESYS_URS_MSG"),
    SCS_APP("SCSAppStatus"),
    SCS_SCS("SCSmode"),
    OCSICON("OCSICON"),
    EXCEPTION("EXCEPTIONS"),
    GMSORS("GMStoORS"),
    GMSPOST("GMSPOST"),
    GMSWEBREQUESTS("GMSWEB"),
    LCA_APP("LCAAppStatus"),
    LCA_CLIENT("LCAClient"),
    CONFSERV_UPDATES("CS_UPDATES"),
    CONFSERV_CONFIGCHANGE("CS_CONFIG_CHANGE"),
    CONFSERV_CONNECT("CS_CONNECT"),
    CONFSERV_NOTIF("CS_NOTIFICATIONS"),
    ORSALARM("ORSALARM"),
    SIP1536Other("SIPDiagnostics"),
    SIP1536Trunk("SIP1536Trunk"),
    SIP1536SIPStatistics("SIP1536SIPStatistics"),
    TLIBTimer("TLibTimer"),
    CloudAuth("CloudAuth"),
    CloudLog("CloudLog"),
    VXMLStrategySteps("VXMLStrategy"),
    APACHEWEB("apache"),
    WWEMessage("WWELogMessage"),
    WWEUCS("WWEUCS"),
    WWEUCSConfig("UCSConfig"),
    WWEIXN("WWEIXN"), WWEBAYEUX("WWEBAYEUX"), SCS_ALARMS("SCS_ALARMS"), SCS_CLIENT_LOG("CLIENT_LOG"),
    INTERACTION_NONIXN("INTERACTION_VQ"), URSHTTP("URSHTTP"), HTTPtoURS("HTTPtoURS");

    private final String name;

    MsgType(String s) {
        name = s.toLowerCase();
    }

    public boolean equalsName(String otherName) {
        return otherName != null && name.equalsIgnoreCase(otherName);
    }

    @Override
    public String toString() {
        return this.name.toLowerCase();
    }

}
