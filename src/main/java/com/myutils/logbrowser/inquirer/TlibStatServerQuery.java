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
public class TlibStatServerQuery extends IQuery {

    private ResultSet m_resultSet;
    private DatabaseConnector m_connector;
    private int recCnt;
    private DynamicTreeNode<OptionNode> node;
    private boolean regex = false;
    private String selection = null;
    private SelectionType selectionType = SelectionType.CONNID;
    private IDsFinder cif = null;

    public TlibStatServerQuery() throws SQLException {
        m_resultSet = null;
        addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Mandatory);
        addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("tserverID", "source", ReferenceType.App.toString(), FieldType.Optional);
        addOutField("intToHex( seqno) seqnoHex"); // bug in SQLite lib; does not accept full word

        addNullField("ixnid");
        addNullField("transferid");
        addNullField("err");
        addNullField("errmessage");
        addNullField("attr1");
        addNullField("attr2");

    }

    TlibStatServerQuery(DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cif) throws SQLException {
        this();
        this.node = eventsSettings;
        this.cif = cif;
        if (cif != null) {
            this.searchApps = cif.searchApps;
        }
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        String myWhere = getMyWhere(); //it modifies ref table list, so should be before ref tables are requested

        query = "SELECT tlib.*,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  "
                + "null as handlerId, null as seqno, 1 as inbound,"
                + "files.component as component, app00.name as app, files.nodeId as nodeid";
        query += getRefFields() + getNullFields();

        query += "\nFROM ststevent_" + alias + " AS tlib"
                + "\nINNER JOIN FILE_" + alias + " AS files ON tlib.fileId=files.id"
                + "\ninner join app as app00 on files.appnameid=app00.id\n";
        query += getRefs();
        query += myWhere;

        query += "\nORDER BY tlib.time"
                + "\n" + getLimitClause() + ";";
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

    void setRegexSearch(boolean regex) {
        this.regex = regex;
    }

    void setSearch(String selection) {
        this.selection = selection;
    }

    void setSearchType(SelectionType selectionType) {
        this.selectionType = selectionType;
    }

    private String getMyWhere() throws SQLException {
        String ret = "";

        if (cif != null) {
            Integer[] dnID = null;
            Integer[] dnConnIDs = null;
            switch (cif.getSearchType()) {
                case DN:
                    dnID = cif.getDNs();
                    if (dnID != null) {
                        ret = " ( " + getWhere("tlib.thisdnid", dnID, false)
                                + " or " + getWhere("tlib.otherdnid", dnID, false) + " )";
                    }
                    break;

                case CONNID:
                case CALLID:
                    if (cif != null) {
                        ret = getWhere("tlib.ConnectionIdid", cif.getConnIDs(), false);
                    }
                    break;

                case GUESS_SELECTION:
                    if ((dnID = cif.getDNs()) != null) {
                        ret = " ( " + getWhere("tlib.thisdnid", dnID, false)
                                + " or " + getWhere("tlib.otherdnid", dnID, false) + " )";
                    } else {
                        if ((dnConnIDs = cif.getIDs(IDType.ConnID)) != null && dnConnIDs.length > 0) {
                            ret = getWhere("tlib.ConnectionIdid", dnConnIDs, false);
                        }

                    }
                    break;

                default:
                    break;
            }
        }
        if (node != null) {
            AddCheckedWhere("tlib.nameID", ReferenceType.TEvent, node, "AND", DialogItem.STATCALLS_TEVENT_NAME);
            Wheres wh = new Wheres();
            wh.addWhere(getCheckedWhere("tlib.thisdnid", ReferenceType.DN, node, DialogItem.STATCALLS_TEVENT_DN), "OR");
            wh.addWhere(getCheckedWhere("tlib.otherdnid", ReferenceType.DN, node, DialogItem.STATCALLS_TEVENT_DN), "OR");
            if (!wh.isEmpty()) {
                addWhere(wh, "AND");
            }
        }
        addFileFilters("tlib", "fileid");
        addDateTimeFilters("tlib", "time");

        return addWheres.makeWhere(ret, "AND", true);

    }

}
