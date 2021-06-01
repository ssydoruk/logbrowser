/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSStatEvent extends Message {

    private static final Matcher nAgentDBID = Pattern.compile("^\\s+nAgentDBID: (\\d+)").matcher("");
    private static final Matcher pszAgentID = Pattern.compile("AgentID:\\s*([^,]+),").matcher("");
    private static final Matcher nPlaceDBID = Pattern.compile("^\\s+nAgentDBID:\\s*(\\d+)").matcher("");
    private static final Matcher pszPlaceID = Pattern.compile("PlaceID:\\s*([^,]+),").matcher("");

    private static final Matcher AgentID = Pattern.compile("^AgentID: (.+)").matcher("");
    private static final Matcher PlaceID = Pattern.compile("^PlaceID: (.+)").matcher("");
    private static final Matcher DNID = Pattern.compile("^\\s+DN: ([^,]+),").matcher("");

    private static final Matcher statTypeTargetUpdated = Pattern.compile("AGENT_VOICE_MEDIA_STATUS.+\\s(\\w+)$").matcher("");
    private static final Matcher statTypeEventInfo = Pattern.compile("^\\s*Status:\\s*(\\w+)").matcher("");

    private final OCSParser.StatEventType statEventType;

    OCSStatEvent(OCSParser.StatEventType statEventType, ArrayList m_MessageContents) {
        super(TableType.OCSStatEvent, m_MessageContents);
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

}
