/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSPAAgentInfo extends Message {

    //	OwnerDBID: 165; OriginDBID: 0; RecHandle: 0; ConnID: ; PlaceDBID: 279; AgentDBID: 452; Agent/Place: CINTIASA
//	CurrentStatType: AgentBusy; CallType: Inbound; NotAvailableFlag: 0;  
    private static final Pattern regCGDBID = Pattern.compile("\\sOwnerDBID: (\\d+);");
    private static final Pattern regPlaceDBID = Pattern.compile("\\sPlaceDBID: (\\d+);");
    private static final Pattern regAgentDBID = Pattern.compile("\\sAgentDBID: (\\d+);");
    private static final Pattern regAgent = Pattern.compile("\\s+Agent/Place:\\s(\\S.+)");
    private static final Pattern regAgentStatType = Pattern.compile("\\s*CurrentStatType:\\s(\\S[^;]+);");
    private static final Pattern regAgentCallType = Pattern.compile("\\sCallType:\\s(\\S[^;]+);");

    private static final Pattern regRecHandle = Pattern.compile("RecHandle: (\\d+);");

    public OCSPAAgentInfo(ArrayList messageLines, int fileID) {
        super(TableType.OCSPAAgentInfo, messageLines, fileID);
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

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        stmt.setInt(6, GetCGDBID());
        stmt.setInt(7, GetPlaceDBID());
        stmt.setInt(8, GetAgentDBID());
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.Agent, GetAgent()));
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.AgentStatType, GetAgentStatType()));
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.AgentCallType, GetAgentCallType()));
        stmt.setInt(12, GetRecHandle());
        return true;

    }
}
