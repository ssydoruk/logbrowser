/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public class AllProxyQuery extends IQuery {

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        query = "SELECT pr.id, pr.time, pr.time, IFNULL(pr.refid, tlib.referenceID) as refid, "
                + "pr.destination, pr.tlib_id, pr.fileid, pr.line, file.name as filename,  "
                + "pr.name, file.component, tlib.connectionid as connid, pr.thisdn, tlib.otherdn, "
                + "pr.fileoffset, pr.filebytes, file.nodeid FROM proxied_" + alias + " AS pr "
                + "INNER JOIN file_" + alias + " AS file ON pr.fileid = file.id "
                + "LEFT JOIN tlib_" + alias + " AS tlib ON tlib.id = pr.tlib_id "
                + "AND pr.handlerid!=0 ORDER BY pr.time;";

        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;

    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            ILogRecord rec = new ProxyEvent(m_resultSet);
            recLoaded(rec);
            return rec;
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

}
