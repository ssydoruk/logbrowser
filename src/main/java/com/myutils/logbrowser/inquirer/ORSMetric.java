package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ORSMetric extends ILogRecord {

    private boolean m_isInbound;
    private int m_anchorid;

    public ORSMetric(ResultSet rs) throws SQLException {
        super(rs, MsgType.ORSM);

        try {
//            m_fields.put("component", rs.getInt("component"));
//            m_fields.put("comp", "ORSM");
            m_fields.put("metric", rs.getString("metric"));
        } catch (NullPointerException e) {
            inquirer.ExceptionHandler.handleException("ERROR: mandatory field missing for SIP message with ID " + getID(), e);
            throw new SQLException("Missing parameters");
        }
    }

    public int GetAnchorId() {
        return m_anchorid;
    }

    public boolean IsInbound() {
        return m_isInbound;
    }

}
