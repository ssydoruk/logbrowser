/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URSWaiting extends Message {

    private static final Pattern regMedia = Pattern.compile("\\<([^\\>]+)\\> type \\<([^\\>]+)\\>");
    private final String connID;
    private String agent;
    private String targetType;

    public URSWaiting(String ag, String connID, int fileID) {
        super(TableType.URSWaiting, fileID);
        this.connID = connID;
        Matcher m;
        if ((m = regMedia.matcher(ag)).find()) {
            this.agent = m.group(1);
            this.targetType = m.group(2);
        }

    }

    public String getConnID() {
        return connID;
    }

    public String getAgent() {
        return agent;
    }

    public String getTargetType() {
        return targetType;
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, getConnID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.Agent, getAgent()));
        setFieldInt(stmt, 8, Main.getRef(ReferenceType.ObjectType, getTargetType()));
        return true;

    }
}
