package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.FileInfoType;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;

public class TLibEvent extends ILogRecord {

    private boolean m_isInbound;
    private long m_referenceId;
    private int m_triggerFileId;
    private int m_anchorid;
    private String source;
    private String app;
    private String thisDn;
    private String otherDn;
    private int thisDNID;
    private int otherDNID;

    private long seqNo;


    int handlerId;
    public TLibEvent(ResultSet rs) throws SQLException {
        this(rs, MsgType.TLIB);
    }
    public TLibEvent(ResultSet rs, MsgType msgType) throws SQLException {
        super(rs, msgType);
        initRecord(rs, msgType);
    }
    public String getApp() {
        return app;
    }
    public long getSeqNo() {
        return seqNo;
    }

    public int getHandlerId() {
        return handlerId;
    }

    @Override
    public void initRecord(ResultSet rs, MsgType msgType) throws SQLException {
        super.initRecord(rs, msgType);
        handlerId = (Integer) rs.getInt("handlerId");
//        int sipId = rs.getInt("sipId");
//        String callId = rs.getString("CallId");
//        m_anchorid = rs.getInt("anchorid");
        String connId = rs.getString("ConnectionId");

//        m_fields.put("name", rs.getString("name"));
//        m_fields.put("referenceid", "refid-" + rs.getString("referenceid"));
        seqNo = getInt(rs, "seqno", 10, -1);
        m_fields.put("seqno", seqNo);

        m_fields.put("handlerid", new Integer(handlerId));
//        m_fields.put("sipid", new Integer(sipId));
        m_isInbound = rs.getBoolean("inbound");
//        m_fields.put("callid", callId == null ? "null" : callId);
        m_fields.put("connid", connId == null ? "" : connId);
        m_referenceId = rs.getLong("ReferenceId");

        this.thisDn = StringUtils.defaultString(rs.getString("thisdn"));
        m_fields.put("thisdn", thisDn);
        this.thisDNID = rs.getInt("thisDNID");
        this.otherDNID = rs.getInt("otherDNID");

        this.otherDn = rs.getString("OtherDN");
        m_fields.put("otherdn", otherDn == null ? "" : otherDn);

        m_fields.put("source", StringUtils.defaultString(rs.getString("source")));

        int iType = rs.getInt("component");
        FileInfoType type = FileInfoType.GetvalueOf(iType);
        m_fields.put("comptype", new Integer(iType));
        String nodeId = rs.getString("nodeid");
        String comp = "";
        String shortComp = "";
        switch (type) {
            case type_SessionController:
                comp = nodeId + " sessionController";
                shortComp = "SC";
                break;
            case type_InteractionProxy:
                comp = nodeId + " interactionProxy";
                shortComp = "IP";
                break;
            case type_tController:
                comp = nodeId + " tController";
                shortComp = "TC";
                break;
            case type_URS:
                shortComp = "URS";
                break;
            case type_ORS:
                shortComp = "ORS";
                break;
            case type_StatServer:
                shortComp = "STS";
                break;
            case type_ICON:
                shortComp = "ICON";
                break;
        }
        m_fields.put("component", comp);
        m_fields.put("comp", shortComp);

        try {
            m_triggerFileId = rs.getInt("trigfile");
        } catch (SQLException e) {
            m_triggerFileId = -1;
        }
        try {
            m_fields.put("trigline", new Integer(rs.getInt("trigline")));
        } catch (SQLException e) {
            m_fields.put("trigline", new Integer(-1));
        }
        try {
            m_fields.put("msgid", StringUtils.defaultString(rs.getString("msgid")));
        } catch (SQLException e) {
            m_fields.put("msgid", "");
        }
        try {
            m_fields.put("location", StringUtils.defaultString(rs.getString("location")));
        } catch (SQLException e) {
            m_fields.put("location", "");
        }
    }

    public int getThisDNID() {
        return thisDNID;
    }

    public int getOtherDNID() {
        return otherDNID;
    }

    public String getThisDn() {
        return thisDn;
    }

    public String getOtherDn() {
        return otherDn;
    }


    public long GetReferenceId() {
        return m_referenceId;
    }

    @Override
    public boolean IsInbound() {
        return m_isInbound;
    }

    public int GetTriggerFileId() {
        return m_triggerFileId;
    }

    @Override
    public int GetAnchorId() {
        return m_anchorid;
    }

    public String getSource() {
        return source;
    }

    private String getStringOrEmpty(ResultSet rs, String fld) {
        try {
            String ret = rs.getString(fld);
            if (ret != null) {
                return ret;
            } else {
                return "";
            }
        } catch (SQLException e) {
            inquirer.logger.debug("No field " + fld, e);
            return "";
        }
    }

    private long getInt(ResultSet rs, String fld, int radix, long defValue) {
        String str = getStringOrEmpty(rs, fld);
        if (str.length() == 0) // empty string 
        {
            return defValue;
        } else {
            try {
                return Long.parseLong(str, radix);
            } catch (NumberFormatException e) {
                inquirer.logger.error("Error parsing int " + str, e);
            }
        }
        return defValue;
    }

    public String getTServer() {
        if (!source.isEmpty()) {
            return source;
        } else {
            return app;
        }
    }

}
