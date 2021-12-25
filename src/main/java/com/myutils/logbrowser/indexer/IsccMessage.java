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

/**
 * @author ssydoruk
 */
public class IsccMessage extends SIPServerBaseMessage {

    private static final Pattern regRefID = Pattern.compile("\\s*ISCCAttributeReferenceID\\s*(\\d+)");
    private static final Pattern regSrcSwitch = Pattern.compile("(?:from|to)\\s+server\\s+([^\\@]+)\\@([^/:\\s]+)");
    String m_MessageName;
    private String sw = null;
    private String app = null;
    private String connID = null;

    public IsccMessage(String event, ArrayList newMessageLines, boolean m_handlerInProgress, int m_handlerId, int fileID) {
        super(TableType.ISCC, m_handlerInProgress, m_handlerId, fileID);
        m_MessageLines = newMessageLines;
        m_MessageName = event;
        m_MessageLines.add(0, event);
    }

    @Override
    public String toString() {
        return "IsccMessage{" + "m_MessageName=" + m_MessageName + '}' + super.toString();
    }

    @Override
    public boolean fillStat(PreparedStatement stmt) throws SQLException {
        stmt.setTimestamp(1, new Timestamp(GetAdjustedUsecTime()));
        setFieldInt(stmt, 2, Main.getRef(ReferenceType.ISCCEvent, GetMessageName()));
        setFieldInt(stmt, 3, Main.getRef(ReferenceType.DN, cleanDN(getThisDN())));
        setFieldInt(stmt, 4, Main.getRef(ReferenceType.DN, cleanDN(getOtherDN())));
        setFieldInt(stmt, 5, Main.getRef(ReferenceType.ConnID, GetConnID()));
        stmt.setLong(6, getRefID());
        stmt.setBoolean(7, isInbound());
        stmt.setInt(8, getFileID());
        stmt.setLong(9, getM_fileOffset());
        stmt.setLong(10, getFileBytes());
        stmt.setInt(11, getM_handlerId());
        stmt.setInt(12, getM_line());
        setFieldInt(stmt, 13, Main.getRef(ReferenceType.App, GetSrcApp()));
        setFieldInt(stmt, 14, Main.getRef(ReferenceType.Switch, GetSrcSwitch()));
        setFieldInt(stmt, 15, Main.getRef(ReferenceType.ConnID, GetOtherConnID()));
        return true;
    }

    public String GetMessageName() {
        return m_MessageName;
    }

    public String getThisDN() {
        return GetAttribute(new String[]{"ISCCAttributeOriginationDN", "SDAttributeOriginationDN"});
    }

    public String getOtherDN() {
        return GetAttribute(new String[]{"ISCCAttributeDestinationDN", "SDAttributeDestinationDN", "ISCCAttributeCallDataXferResource"});
    }

    public String GetConnID() {
        if (connID == null) {
            connID = GetAttribute(new String[]{"ISCCAttributeConnID", "SDAttributeConnID"});
        }
        return connID;
    }

    long getRefID() {
        return FindByRx(regRefID, 1, -1);

//        getAttributeLong("ISCCAttributeReferenceID")
    }

    String GetSrcApp() {
        parseSrcSwitch();
        return app;
    }

    String GetSrcSwitch() {
        parseSrcSwitch();
        return sw;
    }

    private void parseSrcSwitch() {
        if (app == null) {
            Matcher m = FindMatcher(m_MessageLines, regSrcSwitch);
            if (m != null) {
                app = m.group(1);
                sw = m.group(2);
            } else {
                app = "";
                sw = "";
            }
        }
    }

    String GetOtherConnID() {
        return checkDifferent(GetConnID(), new String[]{getAttribute("ISCCAttributeLastTransferConnID"),
                getAttribute("ISCCAttributeFirstTransferConnID")});
    }

    private String checkDifferent(String toCompare, String[] strings) {
        for (String string : strings) {
            if (string != null && !string.isEmpty() && !string.equals(toCompare)) {
                return string;
            }
        }
        return null;
    }

}
