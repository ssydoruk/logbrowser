/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static Utils.Util.StripQuotes;

/**
 * @author terry The class Replicates TLibMessage
 */
public class WSMessage extends Message {

    private static final Pattern regConnID = Pattern.compile("^AttributeConnID .+= (\\w+)$");
    private static final Pattern regThisDN = Pattern.compile("^AttributeThisDN .+= \\\"([^\\\"]*)\\\"$");
    private static final Pattern regOtherDN = Pattern.compile("^AttributeOtherDN .+= \\\"([^\\\"]*)\\\"$");
    private static final Pattern regTransferConnID = Pattern.compile("^AttributeTransferConnID .+= (\\S+)$");
    private static final Pattern regUUID = Pattern.compile("^AttributeCallUuid .+= \\\"([^\\\"]*)\\\"$");
    private static final Pattern regAgentID = Pattern.compile("^AttributeAgentID .+= \\\"([^\\\"]*)\\\"$");
    private static final Pattern regSeqNo = Pattern.compile("^AttributeEventSequenceNumber .+= (\\w+)$");
    private final String m_MessageName;
    private final String server;
    private final String fileHandle;
    private final String refID;

    public WSMessage(String event, String server, String fileHandle, String _refID, ArrayList newMessageLines, int fileID) {
        super(TableType.WSTlib, fileID);
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

    public String GetAgentID() {
        return SingleQuotes(FindByRx(regAgentID, 1, ""));
    }

    public String GetUUID() {
        return SingleQuotes(FindByRx(regUUID, 1, ""));
    }

    long getSeqNo() {
        return FindByRx(regSeqNo, 1, 0);
    }

    public String GetThisDN() {
        return SingleQuotes(FindByRx(regThisDN, 1, ""));
    }

    public String GetOtherDN() {
        return SingleQuotes(FindByRx(regOtherDN, 1, ""));
    }

    public String GetConnID() {
        return FindByRx(regConnID, 1, "");
    }

    public String GetTransferConnID() {
        return SingleQuotes(FindByRx(regTransferConnID, 1, ""));
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
        setFieldInt(stmt, 2, Main.getRef(ReferenceType.TEvent, GetMessageName()));
        setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, cleanDN(GetThisDN())));
        setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, cleanDN(GetOtherDN())));
        setFieldInt(stmt, 5, Main.getRef(ReferenceType.Agent, GetAgentID()));
        setFieldInt(stmt, 6, Main.getRef(ReferenceType.ConnID, GetConnID()));
        setFieldInt(stmt, 7, Main.getRef(ReferenceType.ConnID, GetTransferConnID()));
        stmt.setInt(8, (int) getHeaderLong("AttributeReferenceID"));
        stmt.setBoolean(9, isInbound());
        stmt.setInt(10, getFileID());
        stmt.setLong(11, getM_fileOffset());
        stmt.setLong(12, getFileBytes());
        stmt.setInt(13, 0);
        stmt.setLong(14, getSeqNo());
        stmt.setInt(15, getM_line());
        setFieldInt(stmt, 16, Main.getRef(ReferenceType.UUID, GetUUID()));
        setFieldInt(stmt, 17, Main.getRef(ReferenceType.IxnID, getIxnID()));
        setFieldInt(stmt, 18, Main.getRef(ReferenceType.App, getServer()));
        stmt.setInt(19, getServerHandle());
        stmt.setInt(20, getRefID());
        return true;

    }
}
