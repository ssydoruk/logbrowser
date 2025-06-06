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

public class StStatus extends Message {

    private static final Pattern regStatusLineAgent = Pattern.compile("^Status: (Capacity snapshot) for agent '([^']+)'(?:.+, place '([^']+)')?");
    private static final Pattern regStatusLinePlace = Pattern.compile("^Status: (Capacity snapshot) for place '([^']+)'(?:.+, agent '([^']+)')?");
    private static final Pattern regStatusLineDN = Pattern.compile("^Status: (Capacity snapshot) for \\w+ '([^@']+)");

    private String plGroup = null;
    private String agent = null;
    private String place = null;
    private String OldStatus = null;
    private String NewStatus = null;

    private String dn = null;

    public StStatus(String name, ArrayList<String> messageLines, int fileID) {
        super(TableType.StStatus, messageLines, fileID);
        Matcher m;
        if ((m = regStatusLineAgent.matcher(name)).find()) {
            plGroup = m.group(1);
            agent = m.group(2);
            place = m.group(3);
        } else if ((m = regStatusLinePlace.matcher(name)).find()) {
            plGroup = m.group(1);
            place = m.group(2);
            agent = m.group(3);
        } else if ((m = regStatusLineDN.matcher(name)).find()) {
            plGroup = m.group(1);
            dn = m.group(2);
        } else {
            Main.logger.debug("Not matched for [" + name + "]");
        }
    }

    StStatus(String plGroup, String name, String _OldStatus, String _NewStatus, int fileID) {
        super(TableType.StStatus, fileID);
        this.plGroup = plGroup;
        if (plGroup.startsWith("A")) {
            agent = name;
        } else if (plGroup.startsWith("P")) {
            place = name;
        } else {
            Main.logger.error("Unknown objectype: [" + plGroup + "]");
        }
        this.OldStatus = _OldStatus;
        this.NewStatus = _NewStatus;
    }

    public String getDn() {
        return dn;
    }

    String StatusType() {
        return plGroup;
    }

    String AgentName() {
        return agent;
    }

    String PlaceName() {
        return place;
    }

    String OldStatus() {
        return OldStatus;
    }

    String NewStatus() {
        return NewStatus;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.StatServerStatusType, StatusType()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, AgentName()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.Place, PlaceName()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.StatType, OldStatus()));
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.StatType, NewStatus()));
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.DN, SingleQuotes(getDn())));
        return true;

    }
}
