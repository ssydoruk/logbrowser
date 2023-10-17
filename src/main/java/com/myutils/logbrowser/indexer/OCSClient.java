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

public class OCSClient extends Message {

    private static final Pattern regMsgName = Pattern.compile("^\\s*GSW_CM_MessageType.+\\[([^\\]]+)\\]$");

    private static final Pattern regCampID = Pattern.compile("^\\s*GSW_CM_AttrCampaignID\\s*(\\d+)");

    private static final Pattern regGetGroupID = Pattern.compile("^\\s*GSW_CM_AttrGroupID\\s*(\\d+)");

    public OCSClient(ArrayList messageLines, int fileID) {
        super(TableType.OCSClient, messageLines, fileID);
    }

    String GetMessageName() {
        return FindByRx(regMsgName, 1, "");
    }

    int GetCampaignDBID() {
        return FindByRx(regCampID, 1, -1);
    }

    int GetGroupDBID() {
        return FindByRx(regGetGroupID, 1, -1);
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.OCSClientRequest, GetMessageName()));
        stmt.setInt(7, GetCampaignDBID());
        stmt.setInt(8, GetGroupDBID());
        return true;
    }
}
