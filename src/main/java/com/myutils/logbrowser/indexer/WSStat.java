/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.StripQuotes;

/**
 * @author terry The class Replicates TLibMessage
 */
public class WSStat extends Message {

    private static final Pattern regConnID = Pattern.compile("^AttributeConnID .+= (\\w+)$");
    private static final Pattern regThisDN = Pattern.compile("^AttributeThisDN .+= \\\"([^\\\"]*)\\\"$");

    private final String m_MessageName;
    private final String server;
    private final String fileHandle;
    private final String refID;

    public WSStat(String event, String server, String fileHandle, String _refID, ArrayList newMessageLines, int fileID) {
        super(TableType.WSStat, fileID);
        Main.logger.trace("event=[" + event + "] " + "server=[" + server + "] " + "fileHandle=[" + fileHandle + "] " + "newMessageLines=[" + newMessageLines + "] ");
        m_MessageLines = newMessageLines;
        m_MessageName = event;
        this.server = server;
        refID = _refID;
        this.fileHandle = fileHandle;
        if (m_MessageLines != null) {
            m_MessageLines.add(0, event);
        } else {

        }
    }

    public String GetMessageName() {
        return m_MessageName;
    }

    public String GetThisDN() {
        return SingleQuotes(FindByRx(regThisDN, 1, ""));
    }

    public String GetConnID() {
        return FindByRx(regConnID, 1, "");
    }

    public String GetTransferConnID() {
        return getAttribute("AttributeTransferConnID");
    }

    String getIxnID() {
        String mediaType = getUData("MediaType", "");
        String InteractionType = getUData("InteractionType", "");

        if (mediaType != null && mediaType.length() > 0
                && InteractionType != null && InteractionType.length() > 0) {
            return StripQuotes(getUData("InteractionId", ""));
        } else {
            return null;
        }
    }

    String getServer() {
        return server;
    }

    int getServerHandle() {
        try {
            return Integer.parseInt(fileHandle);
        } catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    int getRefID() {
        if (refID != null) {
            return toInt(refID, 0);
        } else {
            return toInt(getAttribute("AttributeReferenceID"), -1);
        }
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        stmt.setInt(2, getFileID());
        stmt.setLong(3, getM_fileOffset());
        stmt.setLong(4, getM_FileBytes());
        stmt.setLong(5, getM_line());

        setFieldInt(stmt, 6, Main.getRef(ReferenceType.StatEvent, GetMessageName()));
        return true;

    }
}
