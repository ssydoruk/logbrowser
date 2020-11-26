package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class SipMessage extends ILogRecord {

    private boolean m_isInbound;
    private int m_anchorid;
    int handlerId;
    private String callId;

    public SipMessage(ResultSet rs) throws SQLException {
        super(rs, MsgType.SIP);
        String s;

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

    @Override
    void initCustomFields() {
        stdFields.fieldInit("handlerid", new IValueAssessor() {
            @Override
            public Object getValue() {
                return handlerId;
            }

            @Override
            public void setValue(Object val) {
                if (val != null) {
                    handlerId = (int) val;
                }
            }

        });

//        stdFields.fieldInit("inbound", new IValueAssessor() {
//            @Override
//            public Object getValue() {
//                return m_isInbound ? "<-" : "->";
////                return m_isInbound;
//            }
//
//            @Override
//            public void setValue(Object val) {
//                m_isInbound = (int) val == 1;
//            }
//
//        });
        stdFields.fieldInit("callid", new IValueAssessor() {
            @Override
            public Object getValue() {
                return callId;
            }

            @Override
            public void setValue(Object val) {
                callId = (String) val;
            }
        });
    }

    @Override
    HashMap<String, Object> initCalculatedFields(ResultSet rs) throws SQLException {
        HashMap<String, Object> ret = new HashMap<>();
        String s;

        try {

            String peerIp = rs.getString("PeerIp");
            if (peerIp == null) {
                peerIp = "null";
            }
            String peerPort = rs.getString("PeerPort");
            ret.put("peeraddr", peerIp + ":" + peerPort);

            if ((s = rs.getString("ViaUri")) == null) {
                s = "";
            }
            ret.put("via", s);

            switch (rs.getInt("component")) {
                case 1:
                    ret.put("comp", "CM");
                    break;
                case 6:
                    ret.put("comp", "SP");
                    break;
                default:
                    ret.put("comp", "CM");
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
        return ret;
    }

}
