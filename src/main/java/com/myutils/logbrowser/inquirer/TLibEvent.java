package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.FileInfoType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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

    @Override
    void initCustomFields() {

        stdFields.fieldInit("seqno", new IValueAssessor() {
            @Override
            public Object getValue() {
                return seqNo;
            }

            @Override
            public void setValue(Object val) {
                seqNo = setInt(val);
            }

        });

        stdFields.fieldInit("handlerid", new IValueAssessor() {
            @Override
            public Object getValue() {
                return handlerId;
            }

            @Override
            public void setValue(Object val) {
                handlerId = setInt(val);
            }

        });

//        stdFields.fieldInit("inbound", new IValueAssessor() {
//            @Override
//            public Object getValue() {
//                return m_isInbound ? "<-" : "->";
//            }
//
//            @Override
//            public void setValue(Object val) {
//                m_isInbound = setBoolean(val);
//
//            }
//
//        });
        stdFields.fieldInit("referenceid", new IValueAssessor() {
            @Override
            public Object getValue() {
                return m_referenceId;
            }

            @Override
            public void setValue(Object val) {
                m_referenceId = setLong(val);

            }

        });

        stdFields.fieldInit("thisdn", new IValueAssessor() {
            @Override
            public Object getValue() {
                return thisDn;
            }

            @Override
            public void setValue(Object val) {
                thisDn = StringUtils.defaultString((String) val);
            }
        });

        stdFields.fieldInit("otherdn", new IValueAssessor() {
            @Override
            public Object getValue() {
                return otherDn;
            }

            @Override
            public void setValue(Object val) {
                otherDn = StringUtils.defaultString((String) val);
            }
        });

        stdFields.fieldInit("source", new IValueAssessor() {
            @Override
            public Object getValue() {
                return source;
            }

            @Override
            public void setValue(Object val) {
                source = StringUtils.defaultString((String) val);
            }
        });

        stdFields.fieldInit("thisDNID", new IValueAssessor() {
            @Override
            public Object getValue() {
                return thisDNID;
            }

            @Override
            public void setValue(Object val) {
                thisDNID = (int) val;
            }
        });

        stdFields.fieldInit("otherDNID", new IValueAssessor() {
            @Override
            public Object getValue() {
                return otherDNID;
            }

            @Override
            public void setValue(Object val) {
                otherDNID = (int) val;
            }
        });

        stdFields.fieldInit("trigfile", new IValueAssessor() {
            @Override
            public Object getValue() {
                return m_triggerFileId;
            }

            @Override
            public void setValue(Object val) {
                m_triggerFileId = (int) val;
            }
        });

        stdFields.fieldInit("trigline", new IValueAssessor() {
            private Object val;

            @Override
            public Object getValue() {
                return val;
            }

            @Override
            public void setValue(Object _val) {
                val = _val;
            }
        });

        stdFields.fieldInit("msgid", new IValueAssessor() {
            private Object val;

            @Override
            public Object getValue() {
                return val;
            }

            @Override
            public void setValue(Object _val) {
                val = _val;
            }
        });

        stdFields.fieldInit("location", new IValueAssessor() {
            private Object val;

            @Override
            public Object getValue() {
                return val;
            }

            @Override
            public void setValue(Object _val) {
                val = _val;
            }
        });

    }

    @Override
    HashMap<String, Object> initCalculatedFields(ResultSet rs) throws SQLException {
        HashMap<String, Object> ret = new HashMap<>();
        int iType = rs.getInt("component");
        FileInfoType type = FileInfoType.GetvalueOf(iType);
        ret.put("comptype", iType);
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
        ret.put("component", comp);
        ret.put("comp", shortComp);
        String connId = rs.getString("ConnectionId");

        ret.put("connid", connId == null ? "" : connId);

        return ret;
    }

}
