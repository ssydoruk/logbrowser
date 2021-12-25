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

public class OCSPASessionInfo extends Message {

    private static final Pattern regCampInfo = Pattern.compile("\\sOwnerDBID: (\\d+); Campaign DBID: (\\d+); Group DBID: (\\d+); Name: (.+);\\s*$");

    private int cgDBID;
    private String cgName;
    private int GroupDBID;
    private int CampaignDBID;

    public OCSPASessionInfo(ArrayList<String> messageLines, int fileID) {
        super(TableType.OCSPASessionInfo, messageLines, fileID);

        try {
            Matcher m;
            if (messageLines.size() >= 2 && (m = regCampInfo.matcher(messageLines.get(1))).find()) {
                cgDBID = Integer.parseInt(m.group(1));
                CampaignDBID = Integer.parseInt(m.group(2));
                GroupDBID = Integer.parseInt(m.group(3));
                cgName = m.group(4);
            }
        } catch (NumberFormatException e) {
        }
    }

    int getCgDBID() {
        return cgDBID;
    }

    String getCgName() {
        return cgName;
    }

    int getGroupDBID() {
        return GroupDBID;
    }

    int getCampaignDBID() {
        return CampaignDBID;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        stmt.setInt(6, getCgDBID());
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.OCSCG, getCgName()));
        stmt.setInt(8, getGroupDBID());
        stmt.setInt(9, getCampaignDBID());
        return true;

    }
}
