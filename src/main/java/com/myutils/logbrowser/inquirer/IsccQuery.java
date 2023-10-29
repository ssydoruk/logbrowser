/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.ReferenceType;

import java.sql.SQLException;
import java.util.HashSet;

import static com.myutils.logbrowser.inquirer.IQueryResults.addUnique;

/**
 * @author ssydoruk
 */
final public class IsccQuery extends IQuery {

    HashSet<Long> tmpIDs;
    private Integer[] m_connectionIds;
    private DynamicTreeNode<OptionNode> node;
    private IDsFinder cif;
    private HashSet<Long> refIDs;
    private boolean noSearchRequest = true;

    public IsccQuery(Integer[] connectionIds) throws SQLException {
        this.noSearchRequest = true;
        m_connectionIds = connectionIds;
        defRefs();
    }

    public IsccQuery(DynamicTreeNode<OptionNode> _eventsSettings, QueryDialog dlg, IQueryResults frm) throws SQLException {
        node = _eventsSettings;

        defRefs();
        setSearchApps(dlg.getSearchApps());
        setTimeRange(dlg.getTimeRange());
        setQueryLimit(frm.isLimitQueryResults(), frm.getMaxQueryLines());
    }

    IsccQuery(DynamicTreeNode<OptionNode> _eventsSettings, IDsFinder _cidFinder,
              QueryDialog dlg, IQueryResults frm) throws SQLException {
        this(_eventsSettings, dlg, frm);
        cif = _cidFinder;
    }

    IsccQuery(DynamicTreeNode<OptionNode> _eventsSettings, HashSet<Long> refIDs,
              QueryDialog dlg, IQueryResults frm) throws SQLException {
        this(_eventsSettings, dlg, frm);
        this.refIDs = refIDs;
    }

    public void defRefs() throws SQLException {
        addRef("nameID", "name", ReferenceType.ISCCEvent.toString(), FieldType.MANDATORY);
        addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
        addRef("otherConnectionIDID", "OtherConnectionID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
        addRef("srcTServerID", "source", ReferenceType.App.toString(), FieldType.OPTIONAL);
        addRef("srcSwitchID", "sourceSwitch", ReferenceType.Switch.toString(), FieldType.OPTIONAL);
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        String qString = "SELECT tlib.id,"
                + "tlib.time,"
                + "tlib.FileId,"
                + "tlib.FileOffset,"
                + "tlib.FileBytes,"
                + "tlib.line,"
                + "tlib.Inbound,"
                + "tlib.HandlerId,"
                + "tlib.ReferenceId,"
                + "tlib.thisDNID,"
                + "tlib.otherDNID," +
                " file.name AS filename, file.arcname as arcname, file.fileno as fileno, file.appnameid as appnameid,\n" +
                " 0 as anchorid,null as seqno, file.component as component, app00.name as app, file.nodeId as nodeid " +
                getRefFields() +
                getNullFields() +
                "\nFROM iscc_" + alias + " AS tlib\n" +
                getRefs() +
                "\nINNER JOIN file_" + alias + " AS file ON file.id=tlib.fileId\n" +
                "inner join app as app00 on file.appnameid=app00.id\n" +
                getMyWhere("tlib") +
                ((inquirer.getCr().isAddOrderBy()) ? " ORDER BY tlib.time" : "") +
                "\n" +
                getLimitClause() +
                ";";

        m_resultSet = m_connector.executeQuery(this, qString);
        recCnt = 0;

    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        if (m_resultSet != null && m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            ILogRecord rec = new TLibEvent(m_resultSet, MsgType.TLIB_ISCC);
            recLoaded(rec);
            return rec;
        }
        doneExtracting(MsgType.TLIB_ISCC);
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

    public boolean isNoSearchRequest() {
        return noSearchRequest;
    }

    private String getMyWhere(String alias) throws SQLException {

        if (refIDs != null) {
            addWhere(getWhere(alias + ".referenceid", refIDs, false), "AND");
            addWhere(alias + ".connectionIDID < 1", "AND");
            return stdWhere(alias, null);
        } else {
            String ret = "";
            boolean seachRef = false;

            if (cif == null) {
                if (m_connectionIds != null) {
                    ret = getWhere(alias + ".ConnectionIdid", m_connectionIds, false);
                    seachRef = true;
                } else {
                    inquirer.logger.debug(removeRef("ConnectionIDID", ReferenceType.ConnID.toString()));
                    noSearchRequest = false;
                }

            } else {
                if (inquirer.getCr().isNewTLibSearch()) {
                    Integer[] ids = cif.getIDs(IDType.ConnID);
                    if (ids != null && ids.length > 0) {
                        ret = getWhere(alias + ".ConnectionIdid", ids, false);
                        noSearchRequest = false;
                        seachRef = true;
                    } else if ((ids = cif.getIDs(IDType.DN)) != null && ids.length > 0) {
                        ret = " ( " + getWhere(alias + ".thisdnid", ids, false)
                                + " or " + getWhere(alias + ".otherdnid", ids, false) + " )";
                        noSearchRequest = false;

                    }

                } else {
                    Integer[] ids = null;
                    if ((ids = cif.getDNs()) != null && ids.length > 0) {
                        ret = " ( " + getWhere(alias + ".thisdnid", ids, false)
                                + " or " + getWhere(alias + ".otherdnid", ids, false) + " )";
                        noSearchRequest = false;
                    } else if ((ids = cif.getConnIDs()) != null && ids.length > 0) {
                        ret = getWhere(alias + ".ConnectionIdid", cif.getConnIDs(), false);
                        seachRef = true;
                        noSearchRequest = false;
                    }
                }
            }
            if (seachRef) {
                tmpIDs = new HashSet<>();
                setRecLoadedProc((ILogRecord rec) ->
                        addUnique(tmpIDs, ((TLibEvent) rec).GetReferenceId())
                );
            } else {
                tmpIDs = null;
            }
            return stdWhere(alias, ret);
        }

    }

    public HashSet<Long> getTmpIDs() {
        return tmpIDs;
    }

    private String stdWhere(String alias, String ret) {
        if (node != null) {
            AddCheckedWhere(alias + ".nameID", ReferenceType.ISCCEvent, node, "AND", DialogItem.TLIB_CALLS_ISCC_NAME);
        }
        addWhere(getFileFilters(alias, "fileid"), "AND");
        addWhere(getDateTimeFilters(alias, "time", timeRange), "AND");
        return addWheres.makeWhere(ret, "AND", true);
    }

}
