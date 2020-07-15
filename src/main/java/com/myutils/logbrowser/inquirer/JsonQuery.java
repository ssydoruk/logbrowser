/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.ReferenceType;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author kvoroshi
 */
public class JsonQuery extends IQuery {

    private ResultSet m_resultSet;
    private DatabaseConnector m_connector;
    private int recCnt;
    private String orderBy = "json.time";
    private IDsFinder cid;

    public JsonQuery() throws SQLException {
        addJoin("sip_logbr", FieldType.Optional, "sip_logbr.id=json.sipid", "");
        addRef("origUriID", "origUri", ReferenceType.SIPURI.toString(), FieldType.Optional);
        addRef("destUriID", "destUri", ReferenceType.SIPURI.toString(), FieldType.Optional);

    }

    JsonQuery(IDsFinder cidFinder) throws SQLException {
        this();
        this.cid = cidFinder;
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

//        if (m_CallIds == null || m_CallIds.length == 0) {
//            query = "SELECT json.*,"
//                    + "files.id as files_id, "
//                    + "files.fileno as fileno, files.appnameid as appnameid, "
//                    + "files.name as filename, files.arcname as arcname,  files.component as component, app00.name as app "
//                    + "FROM JSON_" + alias + " AS json "
//                    + "INNER JOIN FILE_" + alias + " AS files on json.fileId=files.id\n"
//                    + "inner join app as app00 on files.appnameid=app00.id\n"
//                    + getOrderBy()
//                    + ";";
//            m_resultSet = m_connector.executeQuery(this, query);
//            recCnt = 0;
//            return;
//        }
        query = "SELECT json.*, "
                + "files.id as files_id, "
                + "files.fileno as fileno, files.appnameid as appnameid, "
                + "files.component as component, files.name as filename, files.arcname as arcname, app00.name as app \n"
                + getRefFields()
                + getNullFields()
                + "\nFROM json_logbr as json\n"
                + "\nINNER JOIN file_logbr as files ON json.fileid=files.id \n"
                + "inner join app as app00 on files.appnameid=app00.id\n"
                + getRefs()
                + "\n" + getJoins()
                + "\n" + makeWhere()
                + "\n " + getOrderBy()
                + "\n" + getLimitClause() + ";";

        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            ILogRecord rec = new JsonMessage(m_resultSet);
            recLoaded(rec);
            return rec;
        }
        doneExtracting(MsgType.JSON);

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

    private String makeWhere() throws SQLException {
        if (cid != null) {
            if (inquirer.getCr().isNewTLibSearch()) {
                Wheres wh = new Wheres();
                Integer[] iDs = cid.getIDs(IDType.JSONID);
                if (iDs != null && iDs.length > 0) {
                    wh.addWhere(getWhere("json.id", iDs, false));
                }
                if (!wh.isEmpty()) {
                    addWhere(wh);
                }

            } else {
                Integer[] m_CallIds = cid.getCallIDs();

                if (m_CallIds != null && m_CallIds.length > 0) {
                    addWhere(getWhere("sip_logbr.callidID", m_CallIds, false), "AND");
                }
            }
        }
        addFileFilters("json", "fileid");
        addDateTimeFilters("json", "time");

        return addWheres.makeWhere(true);
    }

    void setOrder(String theOrderBy) {
        orderBy = theOrderBy;
    }

    private String getOrderBy() {
        return ((inquirer.getCr().isAddOrderBy()) ? "\nORDER BY " + orderBy : "\n");
    }

}
