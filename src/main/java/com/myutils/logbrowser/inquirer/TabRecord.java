package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class TabRecord extends ILogRecord {

    private int m_anchorid;

    public TabRecord(MsgType msgType, ResultSet rs) throws SQLException {
        super(rs, msgType);

    }

    TabRecord(MsgType msgType, ResultSet m_resultSet, ICalculatedFields calcFields) throws SQLException {
        this(msgType, m_resultSet);
        setCalcFields(calcFields);

    }

    @Override
    public int GetAnchorId() {
        return m_anchorid;
    }

    @Override
    void initCustomFields() {

    }

    @Override
    HashMap<String, Object> initCalculatedFields(ResultSet rs) throws SQLException {
        return null;
    }

}
