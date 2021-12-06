/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSPAEventInfo extends Message {

    //	OwnerDBID: 165; OriginDBID: 0; RecHandle: 0; ConnID: ; PlaceDBID: 279; AgentDBID: 452; Agent/Place: CINTIASA
//	CurrentStatType: AgentBusy; CallType: Inbound; NotAvailableFlag: 0;  
    private static final Pattern regCGDBID = Pattern.compile("\\sOwnerSessionDBID: (\\d+);");
    private static final Pattern regPlaceDBID = Pattern.compile("\\sPlaceDBID: (\\d+);");
    private static final Pattern regAgentDBID = Pattern.compile("\\sAgentDBID: (\\d+);");
    private static final Pattern regAgent = Pattern.compile("\\s+Agent/Place:\\s+(\\S.+)");
    private static final Pattern regAgentCurrentStatType = Pattern.compile("\\s*CurrentStatType:\\s(\\S[^;]+);");
    private static final Pattern regAgentStatType = Pattern.compile("\\s*StatType:\\s+(\\S[^;]+);");
    private static final Pattern regAgentSessionState = Pattern.compile("\\s*SessionState:\\s+(\\S[^;]+);");
    private static final Pattern regAgentCallType = Pattern.compile("\\sCallType:\\s+(\\S[^;]+);");
    private static final Pattern regRecHandle = Pattern.compile("RecHandle: (\\d+);");
    private static final Pattern regCallResult = Pattern.compile("CallResult: ([1-9][^;]*);$");

    public OCSPAEventInfo(ArrayList<String> messageLines, int fileID) {
        super(TableType.OCSPAEventInfo, messageLines, fileID);
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
