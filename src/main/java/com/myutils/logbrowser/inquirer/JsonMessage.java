/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author ssydoruk
 */
public class JsonMessage extends ILogRecord {

    private final boolean m_isInbound;

    public JsonMessage(ResultSet rs) throws SQLException {
        super(rs, MsgType.JSON);
        m_isInbound = rs.getBoolean("inbound");

    }

    @Override
    public boolean IsInbound() {
        return m_isInbound;
    }

    @Override
    void initCustomFields() {
        stdFields.fieldInit("origUri", null);
        stdFields.fieldInit("destUri", null);
        stdFields.fieldInit("SipsReqId", null);
    }

    @Override
    HashMap<String, Object> initCalculatedFields(ResultSet rs) throws SQLException {
        HashMap<String, Object> ret = new HashMap<>();
        ret.put("origin", getString(rs, "origUri"));
        ret.put("destination", getString(rs, "destUri"));
        ret.put("sips_req", getString(rs, "SipsReqId"));
        ret.put("comp", "CM");
        ret.put("anchorid", 0);
        return ret;
    }
}
