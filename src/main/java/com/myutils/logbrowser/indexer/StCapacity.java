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

import static Utils.Util.intOrDef;

public class StCapacity extends Message {

    public static final int MAX_MEDIA = 5;
    private static final Pattern regStatusLineAgent = Pattern.compile("^Status: (Capacity snapshot) for agent '([^']+)'(?:.+, place '([^']+)'.+\\(CR='([^\\']+))?");
    private static final Pattern regStatusLinePlace = Pattern.compile("^Status: (Capacity snapshot) for place '([^']+)'(?:.+, agent '([^']+)'.+\\(CR='([^\\']+))?");
    private static final Pattern regStatusLineDN = Pattern.compile("^Status: (Capacity snapshot) for \\w+ '([^@']+)");
    private static final Pattern regStatusMedia = Pattern.compile("^\\s*\\[\\s*(\\w+)\\s+(\\d+)\\s+(\\d+).+--\\s(\\w+)");
    private final String OldStatus = null;
    private final String NewStatus = null;
    private String plGroup = null;
    private String agent = null;
    private String place = null;
    private String capacityRule = null;

    private String dn = null;

    public StCapacity(String name, ArrayList messageLines, int fileID) {
        super(TableType.StCapacity, messageLines, fileID);
        Matcher m;
        if ((m = regStatusLineAgent.matcher(name)).find()) {
            plGroup = m.group(1);
            agent = m.group(2);
            place = m.group(3);
            capacityRule = m.group(4);
        } else if ((m = regStatusLinePlace.matcher(name)).find()) {
            plGroup = m.group(1);
            place = m.group(2);
            agent = m.group(3);
            capacityRule = m.group(4);
        } else if ((m = regStatusLineDN.matcher(name)).find()) {
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
            if ((m = regStatusMedia.matcher(m_MessageLine)).find()) {
                if (curMediaNo++ < StCapacity.MAX_MEDIA) {
                    ret.add(new MediaStatuses(m.group(1), m.group(2), m.group(3), m.group(4)));
                } else {
                    break;
                }
            }
        }

        return ret;
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
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.DN, SingleQuotes(getDn())));
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.Capacity, getCapacityRule()));

        int curRecNum = 11;

        ArrayList<StCapacity.MediaStatuses> mediaStatuses = getMediaStatuses();
        for (int i = 0; i < MAX_MEDIA; i++) {
            int baseNo = curRecNum + i * 4;
            if (i < mediaStatuses.size()) {
                StCapacity.MediaStatuses stat = mediaStatuses.get(i);
                Main.logger.trace("adding stat: " + stat.toString());
                setFieldInt(stmt, baseNo, Main.getRef(ReferenceType.Media, stat.getMedia()));
                setFieldString(stmt, baseNo + 1, stat.getRoutable());
                setFieldInt(stmt, baseNo + 2, stat.getCurNumber());
                setFieldInt(stmt, baseNo + 3, stat.getMaxNumber());

            } else {
                setFieldInt(stmt, baseNo, null);
                setFieldString(stmt, baseNo + 1, null);
                setFieldInt(stmt, baseNo + 2, null);
                setFieldInt(stmt, baseNo + 3, null);
            }
        }
        return true;
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
