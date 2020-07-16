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
public class RegisterQuery extends IQuery {

    private final String m_dn;

    public RegisterQuery(String dn) {
        m_dn = dn;
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        query = "SELECT sip.*,files.id as files_id,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname, "
                + "files.component as component "
                + "FROM SIP_"
                + alias + " AS sip INNER JOIN FILE_"
                + alias + " AS files on fileId=files.id "
                + "WHERE sip.name=\"REGISTER\" AND fromuserpart=\"" + m_dn + "\" ORDER BY sip.time;";

        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            ILogRecord rec = new SipMessage(m_resultSet);
            recLoaded(rec);
            return rec;
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
}
