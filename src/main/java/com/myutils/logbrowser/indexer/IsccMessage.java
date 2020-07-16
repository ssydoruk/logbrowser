/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ssydoruk
 */
public class IsccMessage extends Message {

    final private static Pattern regRefID = Pattern.compile("\\s*ISCCAttributeReferenceID\\s*(\\d+)");
    final private static Pattern regSrcSwitch = Pattern.compile("(?:from|to)\\s+server\\s+([^\\@]+)\\@([^/:\\s]+)");
    String m_MessageName;
    private String sw = null;
    private String app = null;
    private String connID = null;

    public IsccMessage(String event, ArrayList newMessageLines) {
        super(TableType.ISCC);
        m_MessageLines = newMessageLines;
        m_MessageName = event;
        m_MessageLines.add(0, event);
    }

    @Override
    public String toString() {
        return "IsccMessage{" + "m_MessageName=" + m_MessageName + '}' + super.toString();
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

//        theRec.getAttributeLong("ISCCAttributeReferenceID")
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
