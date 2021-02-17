package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ParentTransferQuery {

    private ResultSet m_resultSet;
    Integer m_connId;
    private DatabaseConnector m_connector;
    private final String m_tab;
    private int recCnt;

    public ParentTransferQuery(String tab, Integer connId) {
        m_connId = connId;
        m_tab = tab;
    }

    public ParentTransferQuery(Integer connId) {
        this("tlib_logbr", connId);
    }

    public void Attach(IQueryResults queryResult) {
    }

    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        if (m_connId == null) {
            throw new SQLException("no conn ID defined");
        }

        String query = "SELECT TransferIdid as connid from " + m_tab
                + " WHERE ConnectionIdid=" + m_connId
                + " AND TransferIdid > 0 GROUP BY TransferIdid;";

        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

    public Integer GetNext() throws SQLException {
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            return m_resultSet.getInt("connid");
        }
        inquirer.logger.debug("++ " + this.getClass().toString() + ": extracted " + recCnt + " records");
        return null;
    }

    public void Reset() throws Exception {
        if (m_resultSet != null) {
            m_resultSet.close();
        }
        m_connector.releaseConnector(this);
        m_connector = null;
    }
}
