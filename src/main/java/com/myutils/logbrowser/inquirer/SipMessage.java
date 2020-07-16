package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SipMessage extends ILogRecord {

    private boolean m_isInbound;
    private int m_anchorid;
    int handlerId;
    private String callId;

    public SipMessage(ResultSet rs) throws SQLException {
        super(rs, MsgType.SIP);
        String s;
        handlerId = (Integer) rs.getInt("handlerId");

        try {
            m_isInbound = rs.getBoolean("inbound");
            m_fields.put("name", rs.getString("name"));
            if ((s = rs.getString("callid")) == null) {
                s = "";
            }
            callId = notNull(rs.getString("callid"), "");
            m_fields.put("callid", callId);
            if ((s = rs.getString("cseq")) == null) {
                s = "";
            }
            m_fields.put("cseq", s);
            if ((s = rs.getString("touri")) == null) {
                s = "";
            }
            m_fields.put("touri", s);
            if ((s = rs.getString("fromuri")) == null) {
                s = "";
            }
            m_fields.put("fromuri", s);
            m_fields.put("handlerid", handlerId);

            String peerIp = rs.getString("PeerIp");
            if (peerIp == null) {
                peerIp = "null";
            }
            String peerPort = rs.getString("PeerPort");
            m_fields.put("peeraddr", peerIp + ":" + peerPort);

            m_fields.put("component", rs.getInt("component"));
            if ((s = rs.getString("ViaUri")) == null) {
                s = "";
            }
            m_fields.put("via", s);

            switch (rs.getInt("component")) {
                case 1:
                    m_fields.put("comp", "CM");
                    break;
                case 6:
                    m_fields.put("comp", "SP");
                    break;
                default:
                    m_fields.put("comp", "CM");
            }

            String anchorstr = callId;
            anchorstr += rs.getString("name");
            anchorstr += rs.getString("cseq");
            anchorstr += rs.getString("fromuri");
            anchorstr += rs.getString("touri");
            anchorstr += rs.getLong("time");
            m_anchorid = anchorstr.hashCode();
        } catch (NullPointerException e) {
            inquirer.logger.error("ERROR: mandatory field missing for SIP message with ID " + getID(), e);
            throw new SQLException("Missing parameters");
        }
    }

    public int getHandlerId() {
        return handlerId;
    }

    public String getCallId() {
        return callId;
    }

    @Override
    public int GetAnchorId() {
        return m_anchorid;
    }

    @Override
    public boolean IsInbound() {
        return m_isInbound;
    }

}
