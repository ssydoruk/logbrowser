/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

public class AnyQuery extends IQuery {

    private final ArrayList<Pair> selectFields;
    private ResultSetMetaData rsmd;
    private String queryTable;

    public AnyQuery() {
        query = null;
        selectFields = new ArrayList<>();
    }

    public AnyQuery(String _query) {
        this();
        query = _query;
    }

    public ResultSetMetaData getRsmd() {
        return rsmd;
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);

        if (query != null) {
            m_resultSet = m_connector.executeQuery(this, query);
        } else {
            StringBuilder q = new StringBuilder(255);
            ArrayList<String> flds = new ArrayList<>();
            for (Pair selectField : selectFields) {
                flds.add(selectField.getKey() + " as " + selectField.getValue());
            }
            q.append(" select ").append(StringUtils.join(flds.toArray(), ", "));
            q.append(getRefFields());
            q.append(" from ").append(getTable());
            q.append(getRefs());

            m_resultSet = m_connector.executeQuery(this, q.toString());
        }
        rsmd = m_resultSet.getMetaData();
        recCnt = 0;
    }

    public String[] GetNextStringArray() throws SQLException {
        on_error:
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            int columnCount = rsmd.getColumnCount();
            String[] ret = new String[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                ret[i - 1] = m_resultSet.getString(i);
            }
            return ret;
        }
        inquirer.logger.debug("++ " + this.getClass().toString() + ": extracted " + recCnt + " records");
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
    public ILogRecord GetNext() throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public String getTable() {
        return queryTable;
    }

    public void setTable(String tmp1) {
        queryTable = tmp1;
    }

    public void addOutField(String fld, String alias) {
        selectFields.add(new Pair(fld, alias));
    }

    public String[] getFieldNames() throws SQLException {
        String[] ret = new String[rsmd.getColumnCount()];

        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            ret[i - 1] = rsmd.getColumnName(i);
        }
        return ret;
    }

    void close() {
        try {
            if (m_resultSet != null && !m_resultSet.isClosed()) {
                m_resultSet.close();
            }
        } catch (SQLException sQLException) {
            inquirer.logger.error("Not able to close statement", sQLException);
        }
    }

}
