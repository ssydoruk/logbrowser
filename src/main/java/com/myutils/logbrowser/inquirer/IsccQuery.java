/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.ReferenceType;
import static com.myutils.logbrowser.inquirer.IQueryResults.addUnique;
import java.sql.SQLException;
import java.util.HashSet;

/**
 *
 * @author ssydoruk
 */
final public class IsccQuery extends IQuery {

    private Integer[] m_connectionIds;
    private DynamicTreeNode<OptionNode> node;
    private IDsFinder cif;
    private HashSet<Long> refIDs;

    private boolean noSearchRequest = true;
    HashSet<Long> tmpIDs;

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
        addRef("nameID", "name", ReferenceType.ISCCEvent.toString(), FieldType.Mandatory);
        addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("otherConnectionIDID", "OtherConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("srcTServerID", "source", ReferenceType.App.toString(), FieldType.Optional);
        addRef("srcSwitchID", "sourceSwitch", ReferenceType.Switch.toString(), FieldType.Optional);
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        StringBuilder qString = new StringBuilder();

        qString.append("SELECT tlib.id,"
                + "tlib.time,"
                + "tlib.FileId,"
                + "tlib.FileOffset,"
                + "tlib.FileBytes,"
                + "tlib.line,"
                + "tlib.Inbound,"
                + "tlib.HandlerId,"
                + "tlib.ReferenceId,"
                + "tlib.thisDNID,"
                + "tlib.otherDNID,")
                .append(" file.name AS filename, file.arcname as arcname, file.fileno as fileno, file.appnameid as appnameid,\n")
                .append(" 0 as anchorid,null as seqno, file.component as component, app00.name as app, file.nodeId as nodeid ")
                .append(getRefFields())
                .append(getNullFields())
                .append("\nFROM iscc_").append(alias).append(" AS tlib\n")
                .append(getRefs())
                .append("\nINNER JOIN file_").append(alias).append(" AS file ON file.id=tlib.fileId\n")
                .append("inner join app as app00 on file.appnameid=app00.id\n")
                .append(getMyWhere("tlib"))
                .append((inquirer.getCr().isAddOrderBy()) ? " ORDER BY tlib.time" : "")
                .append("\n")
                .append(getLimitClause())
                .append(";");

        m_resultSet = m_connector.executeQuery(this, qString.toString());
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
                setRecLoadedProc(new IQuery.IRecordLoadedProc() {
                    @Override
                    public void recordLoaded(ILogRecord rec) {
                        addUnique(tmpIDs, ((TLibEvent) rec).GetReferenceId());
                    }
                });
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
