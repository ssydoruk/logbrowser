package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ChildTransferQuery {

    private final String m_tab;
    Integer m_connId;
    DatabaseConnector m_connector;
    private ResultSet m_resultSet;
    private int recCnt;

    public ChildTransferQuery(String tab, Integer connId) {
        m_connId = connId;
        m_tab = tab;
    }

    public ChildTransferQuery(Integer connId) {

        this("tlib_logbr", connId);
    }

    public void Attach(IQueryResults queryResult) {
    }

    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        if (m_connId == null || m_connId.intValue() == 0) {
            throw new SQLException("no conn ID defined");
        }

        String query = "SELECT connectionIdid as connid from " + m_tab
                + " WHERE TransferIdid=" + m_connId
                + " GROUP BY connectionIdid;";

        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

    public Integer GetNext() throws SQLException {
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            return m_resultSet.getInt("connid");
        }
        inquirer.logger.debug("++ " + this.getClass() + ": extracted " + recCnt + " records");
        return null;
    }

    public void Reset() throws SQLException {
        if (m_resultSet != null) {
            m_resultSet.close();
        }
        m_connector.releaseConnector(this);
        m_connector = null;
    }
}
