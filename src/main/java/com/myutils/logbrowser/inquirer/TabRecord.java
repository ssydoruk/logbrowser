package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TabRecord extends ILogRecord {

    private boolean m_isInbound;
    private int m_anchorid;

    public TabRecord(MsgType msgType, ResultSet rs) throws SQLException {
        super(rs, msgType);

//        try {
//            m_fields.put("component", rs.getInt("component"));
//            m_fields.put("comp", msgType.toString());
//        } catch (NullPointerException e) {
//            inquirer.ExceptionHandler.handleException("ERROR: mandatory field missing for SIP message with ID " + getID(), e);
//            throw new SQLException("Missing parameters");
//        }
    }

    TabRecord(MsgType msgType, ResultSet m_resultSet, ICalculatedFields calcFields) throws SQLException {
        this(msgType, m_resultSet);
        setCalcFields(calcFields);

    }

    public int GetAnchorId() {
        return m_anchorid;
    }

}
