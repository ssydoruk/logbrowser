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

public class URSStat extends Message {

    private static final Pattern regStatObject = Pattern.compile("^(?:STATOBJECT|SO)\\(\\S+ (\\d+) \\d+\\) .+name=([^@]+)@([^\\.]+)\\.\\S+: CHANGE OF STATE \\((\\S+)->([^\\)]+)\\)");
    private static final Pattern regVoiceDN = Pattern.compile("^[\\+\\-]ready DN ([^\\@]+) \\@ (\\S+) .+ for ag(?:ent)* ([^,]+), pl(?:ace)* ([^,]+), (\\S+)");
    private static final Pattern regMedia = Pattern.compile("^[\\+\\-]ready MEDIA (\\w+).+, (\\S+) time=");

    private String statRef;
    private String statTypeOld;
    private String statTypeNew;
    private String voiceDN;
    private String VoiceSwitch;
    private String voiceAgent = null;
    private String voicePlace;
    private String voiceStat;
    private String ChatStat;
    private String EmailStat;
    private String StatServer;

    public URSStat(ArrayList messageLines, int fileID) {
        super(TableType.URSSTAT, messageLines, fileID);

        Matcher m;
        if ((m = regStatObject.matcher(m_MessageLines.get(0))).find()) {
            statRef = m.group(1);
            voiceAgent = m.group(2);
            StatServer = m.group(3);
            statTypeOld = m.group(4);
            statTypeNew = m.group(5);
        }

        if (m_MessageLines != null) {
            for (String s : m_MessageLines) {
                if (s != null) {
                    if ((m = regVoiceDN.matcher(s)).find()) {
                        voiceDN = m.group(1);
                        VoiceSwitch = m.group(2);
                        if (voiceAgent != null) {
                            String group = m.group(3);
                            if (group != null && !group.isEmpty()) {
                                voiceAgent = m.group(3);
                            }
                        }
                        voicePlace = m.group(4);
                        voiceStat = m.group(5);
                    } else if ((m = regMedia.matcher(s)).find()) {
                        String mediaType = m.group(1);

                        if (mediaType.equals("chat")) {
                            ChatStat = m.group(2);
                        }
                        if (mediaType.equals("email")) {
                            EmailStat = m.group(2);
                        }
                    }

                }
            }
        }

    }

    int getStatRef() {
        try {
            if (statRef != null && statRef.length() > 0) {
                return Integer.parseInt(statRef);
            } else {
                return -1;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    String getAgentName() {
        return voiceAgent;
    }

    String getPlaceName() {
        return voicePlace;
    }

    String getStatSApp() {
        return StatServer;
    }

    String getStatPrev() {
        return statTypeOld;
    }

    String getStatNew() {
        return statTypeNew;
    }

    String getVoicSwitchName() {
        return VoiceSwitch;
    }

    String getVoiceDN() {
        return voiceDN;
    }

    String getVoiceStat() {
        return voiceStat;
    }

    String getChatStat() {
        return ChatStat;
    }

    String getEmailStat() {
        return EmailStat;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        stmt.setInt(6, getStatRef());
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, getAgentName()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.Place, getPlaceName()));
        setFieldInt(stmt, 9, Main.getRef(ReferenceType.App, getStatSApp()));
        setFieldInt(stmt, 10, Main.getRef(ReferenceType.StatType, getStatPrev()));
        setFieldInt(stmt, 11, Main.getRef(ReferenceType.StatType, getStatNew()));

        setFieldInt(stmt, 12, Main.getRef(ReferenceType.Switch, getVoicSwitchName()));
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.DN, SingleQuotes(getVoiceDN())));
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.StatType, getVoiceStat()));

        setFieldInt(stmt, 15, Main.getRef(ReferenceType.StatType, getChatStat()));
        setFieldInt(stmt, 16, Main.getRef(ReferenceType.StatType, getEmailStat()));
        return true;


    }
}
