/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static Utils.Util.StripQuotes;

/**
 * @author terry The class Replicates TLibMessage
 */
public class WSMessage extends Message {

    final private static Pattern regConnID = Pattern.compile("^AttributeConnID .+= (\\w+)$");
    final private static Pattern regThisDN = Pattern.compile("^AttributeThisDN .+= \\\"([^\\\"]*)\\\"$");
    final private static Pattern regOtherDN = Pattern.compile("^AttributeOtherDN .+= \\\"([^\\\"]*)\\\"$");
    final private static Pattern regTransferConnID = Pattern.compile("^AttributeTransferConnID .+= (\\S+)$");
    final private static Pattern regUUID = Pattern.compile("^AttributeCallUuid .+= \\\"([^\\\"]*)\\\"$");
    final private static Pattern regAgentID = Pattern.compile("^AttributeAgentID .+= \\\"([^\\\"]*)\\\"$");
    final private static Pattern regSeqNo = Pattern.compile("^AttributeEventSequenceNumber .+= (\\w+)$");
    private final String m_MessageName;
    private final String server;
    private final String fileHandle;
    private final String refID;

    public WSMessage(String event, String server, String fileHandle, String _refID, ArrayList newMessageLines) {
        super(TableType.WSTlib);
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

}
