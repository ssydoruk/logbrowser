package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class ORSMetric extends ILogRecord {

    private boolean m_isInbound;
    private int m_anchorid;

    public ORSMetric(ResultSet rs) throws SQLException {
        super(rs, MsgType.ORSM);


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

    }

    @Override
    HashMap<String, Object> initCalculatedFields(ResultSet rs) throws SQLException {
        return null;
    }

}
