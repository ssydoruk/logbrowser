/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

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

}
