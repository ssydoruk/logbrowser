/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author ssydoruk
 */
public class JsonMessage extends ILogRecord {

    private final boolean m_isInbound;

    public JsonMessage(ResultSet rs) throws SQLException {
        super(rs, MsgType.JSON);
        m_isInbound = rs.getBoolean("inbound");
//        m_fields.put("type", getString(rs,"type"));
        m_fields.put("origin", getString(rs, "origUri"));
        m_fields.put("destination", getString(rs, "destUri"));
        m_fields.put("sips_req", getString(rs, "SipsReqId"));
        m_fields.put("sipid", rs.getInt("sipid"));
        m_fields.put("comp", "CM");
        m_fields.put("anchorid", 0);
    }

    @Override
    public boolean IsInbound() {
        return m_isInbound;
    }
}
