/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.FileInfoType;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author kvoroshi
 */
public class StSByConnIdQuery extends IQuery {

    private Integer[] m_ConnIds;
    private ResultSet m_resultSet;
    private DatabaseConnector m_connector;
    private int recCnt;

    public StSByConnIdQuery(Integer[] connIds) {
        m_ConnIds = connIds;
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        if (m_ConnIds == null || m_ConnIds.length == 0) {
            throw new SQLException("no conn ID defined");
        }

        String connIdCondition = "(sts.connectionid=\"" + m_ConnIds[0] + "\"";
        for (int i = 1; i < m_ConnIds.length; i++) {
            connIdCondition += " OR sts.connectionid=\"" + m_ConnIds[i] + "\"";
        }
        connIdCondition += ")";

        query = "SELECT sts.*,SipId,CallId,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  "
                + " tlib2.id as anchorId, "
                + "1 AS inbound, null as source, "
                + "files.component as component, files.nodeId as nodeid "
                + "FROM STSTEVENT_" + alias + " AS sts INNER JOIN FILE_" + alias
                + " AS files ON sts.fileId=files.id "
                + //LEFT JOIN HANDLER_" + alias +
                //" AS handler ON handler.id=tlib.HandlerId " +
                "INNER JOIN TLIB_" + alias + " AS tlib2 "
                + "ON sts.connectionId=tlib2.connectionId AND sts.name=tlib2.name "
                + "AND sts.thisdn=tlib2.thisdn "
                + "AND tlib2.time BETWEEN sts.time-1000 AND sts.time+1000 "
                + "INNER JOIN FILE_" + alias + " AS files2 "
                + "ON tlib2.fileId=files2.id AND files2.Component=" + FileInfoType.type_SessionController.ordinal() + " "
                + "LEFT JOIN HANDLER_" + alias
                + " AS handler ON handler.id=tlib2.HandlerId "
                + "LEFT JOIN "
                + // ...left join trigger table merged with file to verify runid right away
                // in join condition...
                "(SELECT tr1.*, f.runid AS runid FROM trigger_" + alias + " AS tr1 INNER JOIN "
                + "file_" + alias + " AS f ON f.id=tr1.fileid) AS trgr ON trgr.text=handler.text AND"
                + " trgr.runid=files2.runid"
                + // ... left join sip table ...
                " LEFT JOIN SIP_" + alias + " AS sip ON sip.handlerid=trgr.HandlerId "
                + "AND sip.handlerid IS NOT NULL AND sip.handlerid!=0 and sip.inbound=1 "
                + " WHERE " + connIdCondition + " ORDER BY sts.time"
                + "\n" + getLimitClause() + ";";

        /*query = "SELECT sts.*,SipId,CallId,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  " +
                "1 AS inbound, sts.this_dn AS thisdn, sts.other_dn AS otherdn, "+
                "sts.conn_id as connectionid, null as source, "+
                "files.component as component, files.nodeId as nodeid FROM "+
                "STSTEVENT_" + alias + " AS sts INNER JOIN FILE_" + alias +
                " AS files ON sts.fileId=files.id " +
                "INNER JOIN TLIB_" + alias + " AS tlib2 " +
                "ON sts.conn_id=tlib2.connectionId AND sts.name=tlib2.name " +
                "AND sts.this_dn=tlib2.thisdn " +
                "AND tlib2.time BETWEEN sts.time-1000 AND sts.time+1000 " +
                "INNER JOIN FILE_"+alias+" AS files2 " +
                "ON tlib2.fileId=files2.id " +
                "LEFT JOIN HANDLER_" + alias +
                " AS handler ON handler.id=tlib2.HandlerId "+
                "LEFT JOIN " +
// ...left join trigger table merged with file to verify runid right away
// in join condition...
 "(SELECT tr1.*, f.runid AS runid FROM trigger_"+alias+" AS tr1 INNER JOIN "+
 "file_"+alias+" AS f ON f.id=tr1.fileid) AS trgr ON trgr.text=handler.text AND"+
 " trgr.runid=files2.runid"+
// ... left join sip table ...
                " LEFT JOIN SIP_" + alias + " AS sip ON sip.handlerid=trgr.HandlerId" +
                " WHERE " + connIdCondition + " UNION "+
                "SELECT sts.*, 0 as SipId, null as CallId,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  "+
                "1 AS inbound, sts.this_dn AS thisdn, sts.other_dn AS otherdn, "+
                "sts.conn_id as connectionid, null as source, "+
                "files.component as component, files.nodeId as nodeid FROM "+
                "STSTEVENT_"+alias+" AS sts INNER JOIN FILE_" + alias +
                " AS files ON sts.fileId=files.id "+
                "WHERE "+connIdCondition+" "+
                "AND sts.name=\"EventAttachedDataChanged\";";*/
        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            ILogRecord rec = new TLibEvent(m_resultSet);
            recLoaded(rec);
            return rec;
        }
        doneExtracting(MsgType.TLIB);
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
