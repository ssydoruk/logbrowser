/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSCG extends Message {

    //16:18:52.794 CampaignGroup(180) (CAMP_DORIVETTES@VAG_CAMP_DORIVETTES): CampGrMsg: OCS version: 8.1.500.18;  WorkTime: 24386;
    private static final Pattern RegCGStart = Pattern.compile("^\\s*CampaignGroup\\((\\d+)\\)\\s+\\(([^\\)]+)\\):\\s+CampGrMsg:\\s+OCS version:\\s+([^;]+);\\s+WorkTime:\\s+(\\d+);");

    //17:52:30.488 CampaignGroup(196) (OBN_IP_Skill5_Campaign@OBN_IP_Skill5_Group): Created
    private static final Pattern regCGEnd = Pattern.compile("^\\s*CampaignGroup\\((\\d+)\\)\\s+\\(([^\\)]+)\\):\\s+(\\S+)$");

    private static final Pattern regGroupDBID = Pattern.compile("^\\s*GroupDBID: ([^;]+); GroupName: (.+);$");
    private static final Pattern regCampaignDBID = Pattern.compile("^\\s*CampaignDBID: ([^;]+); CampaignName: (.+);$");
    private int cgDBID;
    private String cgName;
    private String ocsVersion;
    private String workTime;
    private String CgMsg;
    private boolean groupParsed = false;
    private int groupDBID;
    private String groupName;
    private int campaignDBID;
    private boolean campaignParsed = false;
    private String campaignName;

    public OCSCG(ArrayList<String> messageLines) {
        super(TableType.OCSCG, messageLines);

        Matcher m;

        if (messageLines.size() > 0) {
            String s = messageLines.get(0);
            if ((m = RegCGStart.matcher(s)).find()) {
                cgDBID = Integer.parseInt(m.group(1));
                cgName = m.group(2);
                ocsVersion = m.group(3);
                workTime = m.group(4);
            } else if ((m = regCGEnd.matcher(s)).find()) {
                cgDBID = Integer.parseInt(m.group(1));
                cgName = m.group(2);
                CgMsg = m.group(3);
            }
        }
    }

    @Override
    public String toString() {
        return "OCSCG{" + "cgDBID=" + cgDBID + ", cgName=" + cgName + ", ocsVersion=" + ocsVersion + ", workTime=" + workTime + ", CgMsg=" + CgMsg + '}' + super.toString();
    }

    /**
     * @return the cgDBID
     */
    public int getCgDBID() {
        return cgDBID;
    }

    /**
     * @return the cgName
     */
    public String getCgName() {
        return cgName;
    }

    /**
     * @return the ocsVersion
     */
    public String getOcsVersion() {
        return ocsVersion;
    }

    /**
     * @return the workTime
     */
    public String getWorkTime() {
        return workTime;
    }

    private void parseGroupDBID() {
        if (!this.groupParsed) {
            Matcher m = FindRx(regGroupDBID);
            if (m != null) {
                this.groupDBID = getIntOrDef(m.group(1), -1);
                this.groupName = m.group(2);
            }
            this.groupParsed = true;
        }
    }

    int getGroupDBID() {
        parseGroupDBID();
        return groupDBID;
    }

    String getAgentGroup() {
        parseGroupDBID();
        return groupName;
    }

    private void parseCampaignDBID() {
        if (!this.campaignParsed) {
            Matcher m = FindRx(regCampaignDBID);
            if (m != null) {
                this.campaignDBID = getIntOrDef(m.group(1), -1);
                this.campaignName = m.group(2);
            }
            this.campaignParsed = true;
        }
    }

    int getCampaignDBID() {
        parseCampaignDBID();
        return campaignDBID;
    }

    String getCampaign() {
        parseCampaignDBID();
        return campaignName;
    }

    String getCgMsg() {
        return CgMsg;
    }

}
