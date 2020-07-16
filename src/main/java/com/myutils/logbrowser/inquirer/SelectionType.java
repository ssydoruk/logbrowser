/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

/**
 *
 * @author ssydoruk
 */
public enum SelectionType {

    NO_SELECTION("(Ignore selection)"),
    CONNID("ConnID"),
    DN("DN"),
    AGENT("Agent name"),
    CG("Campaign Group"),
    SESSION("Session"),
    PEERIP("Peer IP"),
    UUID("UUID"),
    OutboundCall("Outbound call"),
    GUESS_SELECTION("(Guess selection)"),
    CALLID("SIP CallID"),
    RecordHandle("OCS Record handle"),
    ChainID("OCS Chain ID"),
    OCSSID("OCS SID"),
    IXN("Interaction ID"),
    PLACE("Place"),
    QUEUE("Interaction queue"),
    AGENTID("Agent ID"),
    REFERENCEID("Reference ID"),
    DialedNumber("Outbound number"),
    GMSSESSION("GMS Session"),
    MCP_CALLID("MCP Session ID(XXXXXX-XXXXXX)"),
    JSESSIONID("JSessionID"),
    GWS_DEVICEID("GWS Device ID"),
    GWS_USERID("GWS User ID"),
    ANYPARAM("Web request parameter");
    ;

    
   private final String name;

    private SelectionType(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        return (otherName == null) ? false : name.toLowerCase().equals(otherName.toLowerCase());
    }

    @Override
    public String toString() {
        return this.name;
    }

}
