package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLQuery extends IQuery {

    protected MsgType msgType;
    private DatabaseConnector m_connector;
    private ResultSet m_resultSet;
    private int recCnt;

    public SQLQuery(MsgType msgType) {
        super();
        this.msgType = msgType;
    }

    public SQLQuery(MsgType msgType, String sql) {
        this(msgType);
        setSQL(sql);
    }

    public void setSQL(String sql) {
        query = sql;
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");

        m_connector = DatabaseConnector.getDatabaseConnector(this);
        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        on_error:
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            try {
                ILogRecord rec = new TabRecord(msgType, m_resultSet);
                recLoaded(rec);
                return rec;
            } catch (SQLException e) {
                inquirer.logger.error("Failed to load rec for type [" + msgType.toString() + "] GetNext:" + e.getMessage(), e);
                break on_error;
            }
        }
        inquirer.logger.debug("++ " + this.getClass() + ": extracted " + recCnt + " records");
        return null;
    }

    @Override
    public void Reset() throws SQLException {
        if (m_resultSet != null) {
            m_resultSet.close();
        }
        m_connector.releaseConnector(this);
        m_connector = null;
    }

    @Override
    public String getQuery() {
        return query;
    }

}
