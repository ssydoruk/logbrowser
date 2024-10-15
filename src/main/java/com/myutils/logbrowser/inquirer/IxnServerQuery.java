/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.ReferenceType;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;

/**
 * @author ssydoruk
 */
public final class IxnServerQuery extends IQuery {

    public static String FIELD_REFID = "refid";
    public static String FIELD_ID = "id";

    private DatabaseConnector m_connector;
    private int recCnt;

    private DynamicTreeNode<OptionNode> node;
    private IDsFinder cif = null;

    public IxnServerQuery() throws SQLException {
        m_resultSet = null;
        addRef("IxnQueueID", "queue", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.MANDATORY);
        addRef("ConnIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
        addRef("IxnID", "InteractionID", ReferenceType.IxnID.toString(), FieldType.OPTIONAL);
        addRef("appid", "client", ReferenceType.App.toString(), FieldType.OPTIONAL);
        addRef("MediaTypeID", "MediaType", ReferenceType.IxnMedia.toString(), FieldType.OPTIONAL);
        addRef("AgentID", "Agent", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
        addRef("PlaceID", "Place", ReferenceType.Place.toString(), FieldType.OPTIONAL);
        addRef("attr1ID", "attr1", ReferenceType.TLIBATTR1.toString(), FieldType.OPTIONAL);
        addRef("attr2ID", "attr2", ReferenceType.TLIBATTR2.toString(), FieldType.OPTIONAL);
        addRef("strategyID", "strategy", ReferenceType.URSStrategyName, FieldType.OPTIONAL);

    }

    IxnServerQuery(DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cif) throws SQLException {
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
                + "files.component as component, app00.name as app, files.nodeId as nodeid";
        query += getRefFields() + getNullFields();

        query += "\nFROM ixn AS tlib"
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
                    ret = getWhere("tlib.ConnIdid", cif.getIDs(IDType.ConnID), false);
                    break;

                case IXN:
                    ret = getWhere("tlib.ixnid", cif.getIDs(IDType.IxnID), false);
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
        if (StringUtils.isNotBlank(ret) || !addWheres.isEmpty()) {
            addCollectIDs(FIELD_REFID);
            addCollectIDs(FIELD_ID);

        }

        addFileFilters("tlib", "fileid");
        addDateTimeFilters("tlib", "time");
        if (node != null) {
            AddCheckedWhere("tlib.nameID", ReferenceType.TEvent, node, "AND", DialogItem.IXN_IXN_EVENT_NAME);
//                        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_DN, ReferenceType.DN, "ixn", "ixnqueueid");
            AddCheckedWhere("tlib.ixnqueueid", ReferenceType.DN, node, "AND", DialogItem.IXN_IXN_EVENT_DN);
            AddCheckedWhere("tlib.attr1ID", ReferenceType.TLIBATTR1, node, "AND", DialogItem.IXN_IXN_EVENT_ATTR1);
            AddCheckedWhere("tlib.attr2ID", ReferenceType.TLIBATTR2, node, "AND", DialogItem.IXN_IXN_EVENT_ATTR2);

            AddCheckedWhere("tlib.appid", ReferenceType.App, node, "AND", DialogItem.IXN_IXN_EVENT_APP);
            AddCheckedWhere("tlib.MediaTypeID", ReferenceType.IxnMedia, node, "AND", DialogItem.IXN_IXN_EVENT_MEDIA);
            AddCheckedWhere("tlib.AgentID", ReferenceType.Agent, node, "AND", DialogItem.IXN_IXN_EVENT_AGENT);
            AddCheckedWhere("tlib.PlaceID", ReferenceType.Place, node, "AND", DialogItem.IXN_IXN_EVENT_PLACE);

        }
        return addWheres.makeWhere(ret, "AND", true);

    }

}
