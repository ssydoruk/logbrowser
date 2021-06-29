/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.intOrDef;

public class StCapacity extends Message {

    public static final int MAX_MEDIA = 5;
    private static final Matcher regStatusLineAgent = Pattern.compile("^Status: (Capacity snapshot) for agent '([^']+)'(?:.+, place '([^']+)'.+\\(CR='([^\\']+))?").matcher("");
    private static final Matcher regStatusLinePlace = Pattern.compile("^Status: (Capacity snapshot) for place '([^']+)'(?:.+, agent '([^']+)'.+\\(CR='([^\\']+))?").matcher("");
    private static final Matcher regStatusLineDN = Pattern.compile("^Status: (Capacity snapshot) for \\w+ '([^@']+)").matcher("");
    private static final Matcher regStatusMedia = Pattern.compile("^\\s*\\[\\s*(\\w+)\\s+(\\d+)\\s+(\\d+).+--\\s(\\w+)").matcher("");
    private final String OldStatus = null;
    private final String NewStatus = null;
    private String plGroup = null;
    private String agent = null;
    private String place = null;
    private String capacityRule = null;

    private String dn = null;

    public StCapacity(String name, ArrayList messageLines) {
        super(TableType.StCapacity, messageLines);
        Matcher m;
        if ((m = regStatusLineAgent.reset(name)).find()) {
            plGroup = m.group(1);
            agent = m.group(2);
            place = m.group(3);
            capacityRule = m.group(4);
        } else if ((m = regStatusLinePlace.reset(name)).find()) {
            plGroup = m.group(1);
            place = m.group(2);
            agent = m.group(3);
            capacityRule = m.group(4);
        } else if ((m = regStatusLineDN.reset(name)).find()) {
            plGroup = m.group(1);
            dn = m.group(2);
        } else {
            Main.logger.debug("Not matched for [" + name + "]");
        }
    }

    public String getCapacityRule() {
        return capacityRule;
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

    ArrayList<MediaStatuses> getMediaStatuses() {
        Matcher m;
        ArrayList<MediaStatuses> ret = new ArrayList<>();
        int curMediaNo = 0;

        for (String m_MessageLine : m_MessageLines) {
            Main.logger.trace("status regexp against [" + m_MessageLine + "]");
            if ((m = regStatusMedia.reset(m_MessageLine)).find()) {
                if (curMediaNo++ < StCapacity.MAX_MEDIA) {
                    ret.add(new MediaStatuses(m.group(1), m.group(2), m.group(3), m.group(4)));
                } else {
                    break;
                }
            }
        }

        return ret;
    }

    public static class MediaStatuses {

        private String routable;
        private int curNumber;
        private int maxNumber;
        private String media;

        public MediaStatuses() {
        }

        private MediaStatuses(String routable, String curNumber, String maxNumber, String media) {
            this.routable = routable;
            this.curNumber = intOrDef(curNumber, -1);
            this.maxNumber = intOrDef(maxNumber, -1);
            this.media = media;
            Main.logger.trace("MediaStatuses: " + this);
        }

        @Override
        public String toString() {
            return "MediaStatuses{" + "routable=" + routable + ", curNumber=" + curNumber + ", maxNumber=" + maxNumber + ", media=" + media + '}';
        }

        public String getRoutable() {
            return routable;
        }

        public int getCurNumber() {
            return curNumber;
        }

        public int getMaxNumber() {
            return maxNumber;
        }

        public String getMedia() {
            return media;
        }
    }

}
