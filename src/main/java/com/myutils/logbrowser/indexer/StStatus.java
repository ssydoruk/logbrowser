/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

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

    public StStatus(String name, ArrayList messageLines) {
        super(TableType.StStatus, messageLines);
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

    StStatus(String plGroup, String name, String _OldStatus, String _NewStatus) {
        super(TableType.StStatus);
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

}
