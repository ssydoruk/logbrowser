/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class OCSPAAgentInfo extends Message {

    //	OwnerDBID: 165; OriginDBID: 0; RecHandle: 0; ConnID: ; PlaceDBID: 279; AgentDBID: 452; Agent/Place: CINTIASA
//	CurrentStatType: AgentBusy; CallType: Inbound; NotAvailableFlag: 0;  
    final private static Pattern regCGDBID = Pattern.compile("\\sOwnerDBID: (\\d+);");
    final private static Pattern regPlaceDBID = Pattern.compile("\\sPlaceDBID: (\\d+);");
    final private static Pattern regAgentDBID = Pattern.compile("\\sAgentDBID: (\\d+);");
    final private static Pattern regAgent = Pattern.compile("\\s+Agent/Place:\\s(\\S.+)");
    final private static Pattern regAgentStatType = Pattern.compile("\\s*CurrentStatType:\\s(\\S[^;]+);");
    final private static Pattern regAgentCallType = Pattern.compile("\\sCallType:\\s(\\S[^;]+);");

    final private static Pattern regRecHandle = Pattern.compile("RecHandle: (\\d+);");

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
