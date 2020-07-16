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
 * @author ssydoruk
 */
public class IxnSSQuery extends IQuery {

    private DynamicTreeNode<OptionNode> node;
    private boolean regex = false;
    private String selection = null;
    private SelectionType selectionType = SelectionType.CONNID;
    private IDsFinder cif = null;

    public IxnSSQuery() throws SQLException {
        m_resultSet = null;
        addRef("IxnQueueID", "queue", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Mandatory);
        addRef("ConnIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("IxnID", "InteractionID", ReferenceType.IxnID.toString(), FieldType.Optional);
        addRef("MediaTypeID", "MediaType", ReferenceType.IxnMedia.toString(), FieldType.Optional);
        addRef("RouterID", "client", ReferenceType.App.toString(), FieldType.Optional);
        addRef("AgentID", "Agent", ReferenceType.Agent.toString(), FieldType.Optional);
        addRef("PlaceID", "Place", ReferenceType.Place.toString(), FieldType.Optional);
        addRef("ServiceID", "Service", ReferenceType.IxnService.toString(), FieldType.Optional);
        addRef("MethodID", "Method", ReferenceType.IxnMethod.toString(), FieldType.Optional);

    }

    IxnSSQuery(DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cif) throws SQLException {
        this();
        this.node = eventsSettings;
        this.cif = cif;
        if (cif != null) {
            this.searchApps = cif.searchApps;
        }
    }

    public int getRecCnt() {
        return recCnt;
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

        query += "\nFROM ixnss AS tlib"
                + "\nINNER JOIN FILE_" + alias + " AS files ON tlib.fileId=files.id"
                + "\ninner join app as app00 on files.appnameid=app00.id\n";
        query += getRefs();
        query += myWhere;

        query += "\n" + ((inquirer.getCr().isAddOrderBy()) ? " ORDER BY tlib.time" : "")
                + "\n" + getLimitClause() + ";";
        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        if (m_resultSet.next()) {
//            doCollectIDs(m_resultSet);
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            ILogRecord rec = new TabRecord(MsgType.INTERACTION, m_resultSet);
            recLoaded(rec);
            return rec;
        }
        doneExtracting(MsgType.INTERACTION);
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
                case QUEUE:
                    dnID = cif.getIDs(IDType.IxnQueue);
                    if (dnID != null) {
                        ret = " ( " + getWhere("tlib.ixnqueueid", dnID, false)
                                + " )";
                    }
                    break;

                case CONNID:
                case CALLID:
                    if (cif != null) {
                        ret = getWhere("tlib.ConnIdid", cif.getIDs(IDType.ConnID), false);
                    }
                    break;

                case IXN:
                    if (cif != null) {
                        ret = getWhere("tlib.ixnid", cif.getIDs(IDType.IxnID), false);
                    }
                    break;

                case GUESS_SELECTION:
                    if ((dnID = cif.getIDs(IDType.IxnQueue)) != null) {
                        ret = " ( " + getWhere("tlib.ixnqueueid", dnID, false)
                                + " )";
                    } else {
                        Wheres wh = new Wheres();
                        if ((dnConnIDs = cif.getIDs(IDType.ConnID)) != null && dnConnIDs.length > 0) {
                            wh.addWhere(getWhere("tlib.ConnIdid", dnConnIDs, false), "OR");
                        }
                        if ((dnConnIDs = cif.getIDs(IDType.IxnID)) != null && dnConnIDs.length > 0) {
                            wh.addWhere(getWhere("tlib.ixnid", dnConnIDs, false), "OR");
                        }
                        addWhere(wh);
                    }
                    break;

                default:
                    break;
            }
        }
        addFileFilters("tlib", "fileid");
        addDateTimeFilters("tlib", "time");
        if (node != null) {
            AddCheckedWhere("tlib.nameID", ReferenceType.TEvent, node, "AND", DialogItem.IXN_IXN_EVENT_NAME);
//                        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_DN, ReferenceType.DN, "ixn", "ixnqueueid");
            AddCheckedWhere("tlib.ixnqueueid", ReferenceType.DN, node, "AND", DialogItem.IXN_IXN_EVENT_DN);

        }
        return addWheres.makeWhere(ret, "AND", true);

    }

}
