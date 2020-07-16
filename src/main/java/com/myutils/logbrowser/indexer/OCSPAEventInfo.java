/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class OCSPAEventInfo extends Message {

//	OwnerDBID: 165; OriginDBID: 0; RecHandle: 0; ConnID: ; PlaceDBID: 279; AgentDBID: 452; Agent/Place: CINTIASA
//	CurrentStatType: AgentBusy; CallType: Inbound; NotAvailableFlag: 0;  
    final private static Pattern regCGDBID = Pattern.compile("\\sOwnerSessionDBID: (\\d+);");
    final private static Pattern regPlaceDBID = Pattern.compile("\\sPlaceDBID: (\\d+);");
    final private static Pattern regAgentDBID = Pattern.compile("\\sAgentDBID: (\\d+);");
    final private static Pattern regAgent = Pattern.compile("\\s+Agent/Place:\\s+(\\S.+)");
    final private static Pattern regAgentCurrentStatType = Pattern.compile("\\s*CurrentStatType:\\s(\\S[^;]+);");
    final private static Pattern regAgentStatType = Pattern.compile("\\s*StatType:\\s+(\\S[^;]+);");
    final private static Pattern regAgentSessionState = Pattern.compile("\\s*SessionState:\\s+(\\S[^;]+);");
    final private static Pattern regAgentCallType = Pattern.compile("\\sCallType:\\s+(\\S[^;]+);");
    final private static Pattern regRecHandle = Pattern.compile("RecHandle: (\\d+);");
    final private static Pattern regCallResult = Pattern.compile("CallResult: ([1-9][^;]*);$");

    public OCSPAEventInfo(ArrayList messageLines) {
        super(TableType.OCSPAEventInfo, messageLines);
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
        String ret = FindByRx(regAgentCurrentStatType, 1, "");
        if (ret != null && ret.length() > 0) {
            return ret;
        }
        ret = FindByRx(regAgentStatType, 1, "");
        if (ret != null && ret.length() > 0) {
            return ret;
        }
        ret = FindByRx(regAgentSessionState, 1, "");
        if (ret != null && ret.length() > 0) {
            return ret;
        }
        return null;
    }

    String GetAgentCallType() {
        return FindByRx(regAgentCallType, 1, "");
    }

    int CallResult() {
        return FindByRx(regCallResult, 1, 0);
    }

}
