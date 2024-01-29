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
import java.util.regex.Pattern;

public class OCSStatEvent extends Message {

    private static final Pattern nAgentDBID = Pattern.compile("^\\s+nAgentDBID: (\\d+)");
    private static final Pattern pszAgentID = Pattern.compile("AgentID:\\s*([^,]+),");
    private static final Pattern nPlaceDBID = Pattern.compile("^\\s+nAgentDBID:\\s*(\\d+)");
    private static final Pattern pszPlaceID = Pattern.compile("PlaceID:\\s*([^,]+),");

    private static final Pattern AgentID = Pattern.compile("^AgentID: (.+)");
    private static final Pattern PlaceID = Pattern.compile("^PlaceID: (.+)");
    private static final Pattern DNID = Pattern.compile("^\\s+DN: ([^,]+),");

    private static final Pattern statTypeTargetUpdated = Pattern.compile("AGENT_VOICE_MEDIA_STATUS.+\\s(\\w+)$");
    private static final Pattern statTypeEventInfo = Pattern.compile("^\\s*Status:\\s*(\\w+)");

    private final OCSParser.StatEventType statEventType;

    OCSStatEvent(OCSParser.StatEventType statEventType, ArrayList<String> m_MessageContents, int fileID) {
        super(TableType.OCSStatEvent, m_MessageContents, fileID);
        this.statEventType = statEventType;
    }

    String getStatEvent() {
        return statEventType.toString();
    }

    String getAgentName() {
//        Main.logger.trace("agent name Stat: "+statEventType+" name:"+FindByRx(pszAgentID, 1, "")+"["+pszAgentID.toString()+"] "+m_MessageLines.toString()+"|\n"+FindByRx(pszAgentID, 1, ""));
        switch (statEventType) {
            case SEventCurrentTargetState_TargetUpdated:
            case SEventCurrentTargetState_Snapshot:
                return FindByRx(pszAgentID, 1, "");

            case SEventInfo:
                return FindByRx(AgentID, 1, "");

            case SEventStatClosed:
            case SEventStatOpened:
            default:
                return "";
        }
    }

    int getAgentDBID() {
        switch (statEventType) {
            case SEventCurrentTargetState_TargetUpdated:
            case SEventCurrentTargetState_Snapshot:
                return FindByRx(nAgentDBID, 1, 0);

            case SEventInfo:
            case SEventStatClosed:
            case SEventStatOpened:
            default:
                return 0;
        }
    }

    String getPlaceName() {
        switch (statEventType) {
            case SEventCurrentTargetState_TargetUpdated:
            case SEventCurrentTargetState_Snapshot:
                return FindByRx(pszPlaceID, 1, "");

            case SEventInfo:
                return FindByRx(PlaceID, 1, "");

            case SEventStatClosed:
            case SEventStatOpened:
            default:
                return "";
        }
    }

    int getPlaceDBID() {
        switch (statEventType) {
            case SEventCurrentTargetState_TargetUpdated:
            case SEventCurrentTargetState_Snapshot:
                return FindByRx(nPlaceDBID, 1, 0);

            case SEventInfo:
            case SEventStatClosed:
            case SEventStatOpened:
            default:
                return 0;
        }
    }

    String getDN() {
        return FindByRx(DNID, 1, "");
    }

    String getStatType() {
//        Main.logger.trace("getStatType agent name Stat: "+statEventType+" name:"+FindByRx(pszAgentID, 1, "")+"["+pszAgentID.toString()+"] "+m_MessageLines.toString()+"|\n"+FindByRx(pszAgentID, 1, ""));
        switch (statEventType) {
            case SEventCurrentTargetState_TargetUpdated:
            case SEventCurrentTargetState_Snapshot:
                return FindByRx(statTypeTargetUpdated, 1, "");

            case SEventInfo:
                return FindByRx(statTypeEventInfo, 1, "");

            case SEventStatClosed:
            case SEventStatOpened:
            default:
                return "";
        }
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.StatEvent, getStatEvent()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, getAgentName()));
        stmt.setInt(8, getAgentDBID());
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.Place, getPlaceName()));
        stmt.setInt(10, getPlaceDBID());
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.DN, cleanDN(getDN())));
        setFieldInt(stmt, 12, Main.getRef(ReferenceType.StatType, getStatType()));
        return true;

    }
}
