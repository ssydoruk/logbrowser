/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

/**
 *
 * @author ssydoruk
 */
public enum ReferenceType {
    UNKNOWN("UNKNOWN"),
    TEvent("TEvent"),
    CallType("CallType"),
    StatType("StatType"),
    Switch("Switch"),
    DN("DN"),
    DNTYPE("dntype"),
    ConnID("ConnID"),
    SIPCALLID("SIPCALLID"),
    HANDLER("HANDLER"),
    UUID("UUID"),
    DBRequest("DBRequest"),
    IxnID("IxnID"),
    IxnMedia("IxnMedia"),
    OCSCG("OCSCG"),
    OCSCAMPAIGN("OCSCAMPAIGN"),
    OCSSCXMLSESSION("OCSSCXMLSESSION"),
    MSGLEVEL("MSGLEVEL"),
    LOGMESSAGE("LOGMESSAGE"),
    ORSSID("ORSSID"),
    ORSNS("ORSNS"),
    ORSMODULE("ORSMODULE"),
    ORSMETHOD("ORSMETHOD"),
    METRIC("METRIC"),
    StatEvent("StatEvent"),
    URSStrategyMsg("URSStrategy"),
    StatServerStatusType("StatusType"),
    SIPCSEQ("SIPCSEQ"),
    SIPURI("SIPURI"),
    SIPVIABRANCH("SIPVIABRANCH"),
    SIPMETHOD("SIPMETHOD"),
    IP("IP"),
    App("App"),
    AppType("AppType"),
    Place("Place"),
    AgentStatType("AgentStatType"),
    AgentCallType("AgentCallType"),
    Agent("Agent"),
    ErrorDescr("ErrorDescr"),
    DBActivity("DBActivity"),
    ISCCEvent("ISCCEvent"),
    CfgOp("CfgOp"),
    URSMETHOD("ursmethod"),
    METRICFUNC("extmethod"),
    CfgObjectType("cfgObjType"),
    CfgObjName("cfgObjName"),
    CfgMsg("CfgMsg"),
    URSStrategyName("Script"),
    TLIBERROR("ErrorMessage"),
    Capacity("Capacity"),
    Media("Media"),
    IxnService("IxnService"),
    IxnMethod("IxnMethod"),
    ObjectType("ObjectType"),
    VQID("VQID"),
    URSTarget("URSTarget"),
    URSStrategyRef("URSStrategyRef"),
    URSRLIBRESULT("URSRlibResult"),
    URSRLIBPARAM("URSRlibParam"),
    ConfigMessage("ConfigMessage"),
    APPRunMode("APPRunMode"),
    appStatus("appStatus"),
    Host("Host"),
    SCSEvent("SCSEvent"),
    TLIBATTR1("tlibattr1"),
    TLIBATTR2("tlibattr2"),
    AgentGroup("agentGroup"),
    OCSCallList("CallingList"),
    OCSCallListTable("OCSTable"),
    Exception("Exception"),
    ExceptionMessage("ExceptionMessage"),
    Misc("Misc"),
    CUSTKKEY("custkey"),
    CUSTVALUE("custvalue"),
    CUSTCOMP("custcomp"),
    OCSIconEvent("OCSIconEvent"),
    OCSIconCause("OCSIconCause"),
    OCSClientRequest("ocsclientreq"),
    GMSEvent("GMSEvent"),
    GMSCallbackType("GMSCallbackType"),
    GMSService("GMSService"),
    ORSREQ("orsreq"),
    OCSRecordAction("recordAction"),
    METRIC_PARAM1("metric_param1"),
    METRIC_PARAM2("metric_param2"),
    HTTPRequest("HTTPRequest"),
    GMSMisc("GMSMisc"),
    TEventRedirectCause("TEventRedirectCause"),
    CSRequest("CSRequest"),
    CLOUD_LOG_MESSAGE_TYPE("CLOUD_MESSAGE_TYPE"),
    CLOUD_LOG_MESSAGE("CLOUD_LOG_MESSAGE"),
    VXMLCommand("vxmlCmd"),
    VXMLCommandParams("vxmlCmdParams"),
    VXMLMCPCallID("MCPCallID"),
    GVPSessionID("GVPSessionID"),
    Tenant("Tenant"),
    GVPIVRProfile("IVRProfile"),
    JSessionID("JSessionID"),
    HTTPMethod("HTTPMethod"),
    HTTPURL("HTTPURL"),
    HTTPRESPONSE("HTTPcode"),
    GWSDeviceID("GWSDeviceID"),
    JAVA_CLASS("JavaClass"),
    WWEUserID("WWEUserID"),
    WWEBrowserClient("wweClient"),
    WWEBrowserSession("wweSession"),
    MiscParam("MiscParam"),
    BayeuxChannel("BayeuxChannel"),
    BayeuxMessageType("BayeuxMessageType"),
    AlarmState("AlarmState"),;

    private final String name;

    private ReferenceType(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        return (otherName == null) ? false : name.equals(otherName);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
