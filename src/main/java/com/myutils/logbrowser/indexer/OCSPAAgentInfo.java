/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSPAAgentInfo extends Message {

    //	OwnerDBID: 165; OriginDBID: 0; RecHandle: 0; ConnID: ; PlaceDBID: 279; AgentDBID: 452; Agent/Place: CINTIASA
//	CurrentStatType: AgentBusy; CallType: Inbound; NotAvailableFlag: 0;  
    final private static Matcher regCGDBID = Pattern.compile("\\sOwnerDBID: (\\d+);").matcher("");
    final private static Matcher regPlaceDBID = Pattern.compile("\\sPlaceDBID: (\\d+);").matcher("");
    final private static Matcher regAgentDBID = Pattern.compile("\\sAgentDBID: (\\d+);").matcher("");
    final private static Matcher regAgent = Pattern.compile("\\s+Agent/Place:\\s(\\S.+)").matcher("");
    final private static Matcher regAgentStatType = Pattern.compile("\\s*CurrentStatType:\\s(\\S[^;]+);").matcher("");
    final private static Matcher regAgentCallType = Pattern.compile("\\sCallType:\\s(\\S[^;]+);").matcher("");

    final private static Matcher regRecHandle = Pattern.compile("RecHandle: (\\d+);").matcher("");

    public OCSPAAgentInfo(ArrayList messageLines) {
        super(TableType.OCSPAAgentInfo, messageLines);
    }

    int GetRecHandle() {
        return FindByRx(regRecHandle, 1, -1);
    }

    int GetCGDBID() {
        return FindByRx(regCGDBID, 1, -1);
    }

    int GetPlaceDBID() {
        return FindByRx(regPlaceDBID, 1, -1);
    }

    int GetAgentDBID() {
        return FindByRx(regAgentDBID, 1, -1);
    }

    String GetAgent() {
        return FindByRx(regAgent, 1, "");
    }

    String GetAgentStatType() {
        return FindByRx(regAgentStatType, 1, "");
    }

    String GetAgentCallType() {
        return FindByRx(regAgentCallType, 1, "");
    }

}
