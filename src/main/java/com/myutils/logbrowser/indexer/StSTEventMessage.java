package com.myutils.logbrowser.indexer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * @author akolo
 */
public class StSTEventMessage extends Message {

    private static final Pattern regTMessage = Pattern.compile("^Server: Switch '([^']+)'->'([^']+)'", CASE_INSENSITIVE);
    private static final Pattern regLinking = Pattern.compile("^Server: ((?:Un)?linking) Agent\\s+'([^']+)'.+(?:to|from) DN '([^']+)'\\('([^']+)'\\)", CASE_INSENSITIVE);
    private static final Pattern regConn = Pattern.compile("^Call '([^']+)'.+VQ '([^@]+)@([^']+)", CASE_INSENSITIVE);
    private static final Pattern regTServerSwitch = Pattern.compile("^(?:to open '([^']+)|TServer '([^']+)|Server: '([^']+)).+(?:for '([^']+)|Switch '([^']+))", CASE_INSENSITIVE);
    private static final Pattern regIxnServer = Pattern.compile("^(?:Ixn Server '([^']+)'|Ixn Server.+:([^\\]]+))", CASE_INSENSITIVE);

    private static final Pattern regIxnMessage = Pattern.compile("^Int 04543 .+\\(\"([^\"]+)\"\\)$");
    private static final Pattern regConnID = Pattern.compile("\\sConnID\\s(\\w+)");

    private String m_switch = null;
    private String m_dn = null;
    private String m_agent = null;
    private String m_connid;
    private String m_TServer;
    private String m_MessageName = null;

    public StSTEventMessage(ArrayList newMessageLines, int fileID) {
        super(TableType.StSTEvent, fileID);
        m_MessageLines = newMessageLines;
        Matcher m;
        if (!m_MessageLines.isEmpty()) {
            String header = m_MessageLines.get(0);
            if ((m = regIxnMessage.matcher(header)).find()) {
                m_TServer = m.group(1);
                header = m_MessageLines.get(1);
            }

            Main.logger.debug("header: [" + header + "]");

            if ((m = regTMessage.matcher(header)).find()) {
                m_switch = m.group(1);
                m_MessageName = m.group(2);
            } else if ((m = regLinking.matcher(header)).find()) {
                m_switch = m.group(4);
                m_MessageName = m.group(1);
                m_dn = m.group(3);
                m_agent = m.group(2);
            } else if ((m = regConn.matcher(header)).find()) {
                m_switch = m.group(3);
                m_connid = m.group(1);
                m_dn = m.group(2);
            } else if ((m = regTServerSwitch.matcher(header)).find()) {
                if (m.group(1) != null) {
                    m_TServer = m.group(1);
                } else if (m.group(2) != null) {
                    m_TServer = m.group(2);
                } else if (m.group(3) != null) {
                    m_TServer = m.group(3);
                }
                if (m.group(4) != null) {
                    m_switch = m.group(4);
                } else if (m.group(5) != null) {
                    m_switch = m.group(5);
                }
            } else if ((m = regIxnServer.matcher(header)).find()) {
                if (m.group(1) != null) {
                    m_TServer = m.group(1);
                } else if (m.group(2) != null) {
                    m_TServer = m.group(2);
                }
            }
        }
    }

    public String GetMessageName() {
        return m_MessageName;
    }

    public String GetConnID() {
        if (m_connid != null) {
            return m_connid;
        }
        return FindByRx(regConnID, 1, "");
    }

    public String GetTransferConnID() {
        return GetHeaderValue("AttributeFirstTransferConnID");
    }

    public String GetDecConnID() {

        String str = GetConnID();
        if (str != null && !"".equals(str)) {
            BigInteger biValue = new BigInteger(str, 16);
            return biValue.toString();
        }
        return null;
    }

    public String GetDecTransferConnID() {

        String str = GetTransferConnID();
        if (str != null && !"".equals(str)) {
            BigInteger biValue = new BigInteger(str, 16);
            return biValue.toString();
        }

        return null;
    }

    String getSwitch() {
        return m_switch;
    }

    String getTServer() {
        return m_TServer;
    }

    String getThisDN() {
        if (m_dn != null) {
            return m_dn;
        } else {
            return getAttribute("ThisDN");
        }
    }

    String getAgent() {
        return m_agent;
    }

}
