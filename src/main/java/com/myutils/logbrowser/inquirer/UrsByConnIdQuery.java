/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.ReferenceType;

import java.sql.SQLException;
import java.util.Collection;

/**
 * @author ssydoruk
 */
public final class UrsByConnIdQuery extends IQuery {

    private DynamicTreeNode<OptionNode> node = null;
    private Collection<Long> refIDs;
    private Collection<Long> existingIDs;
    private Collection<Long> dnIDs;
    private IDsFinder cif = null;

    public UrsByConnIdQuery() throws SQLException {
        addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.OPTIONAL);
        addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        addRef("SourceID", "Source", ReferenceType.App.toString(), FieldType.OPTIONAL);
        addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
        addRef("TransferIDID", "TransferID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
        addRef("ixnIDID", "IXNID", ReferenceType.IxnID.toString(), FieldType.OPTIONAL);
        addRef("attr1ID", "attr1", ReferenceType.TLIBATTR1.toString(), FieldType.OPTIONAL);
        addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.OPTIONAL);
        addRef("errMessageID", "errmessage", ReferenceType.TLIBERROR.toString(), FieldType.OPTIONAL);
        addOutField("intToHex( seqno) seqnoHex"); // bug in SQLite lib; does not accept full word
//        addRef("agentIDID", "agentID", ReferenceType.Agent.toString(), FieldType.Optional);
        addNullField("agentID");
        addRef("DNISID", "DNIS", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        addRef("ANIID", "ANI", ReferenceType.DN.toString(), FieldType.OPTIONAL);

        addNullField("attr2");
        addNullField("err");
        addNullField("typestr");
        addNullField("handlerId");
        addNullField("place");
    }

    public UrsByConnIdQuery(Integer[] connIds) throws SQLException {
        this();
    }

    public UrsByConnIdQuery(Integer connId) throws SQLException {
        this(new Integer[]{connId});

    }

    UrsByConnIdQuery(Collection<Long> refIDs, Collection<Long> ids) throws SQLException {
        this();
        this.refIDs = refIDs;
        this.existingIDs = ids;
    }

    UrsByConnIdQuery(Collection<Long> refIDs, Collection<Long> ids, Collection<Long> dnIDs) throws SQLException {
        this(refIDs, ids);
        this.dnIDs = dnIDs;
    }

    UrsByConnIdQuery(IDsFinder cidFinder) throws SQLException {
        this();
        cif = cidFinder;
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();
        String myWhere = getMyWhere();

        String qString = "SELECT urs.*,null as SipId,null as CallId,files.fileno as fileno, " +
                "files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  " +
                "files.component as component, files.nodeId as nodeid, " +
                "app00.name as app, null as anchorId \n" +
                getRefFields() +
                getNullFields() +
                addedFieldString(true, false) +
                "\nFROM URS_logbr AS urs INNER JOIN FILE_logbr AS files " +
                "ON urs.fileId=files.id \ninner join app as app00 on files.appnameid=app00.id\n" +
                getRefs() +
                "\n" +
                myWhere +
                ((inquirer.getCr().isAddOrderBy()) ? " ORDER BY urs.time " : " ") +
                "\n" +
                getLimitClause() +
                ";";

        m_resultSet = m_connector.executeQuery(this, qString);
        recCnt = 0;
    }

    private String getMyWhere() throws SQLException {
        String ret = "";

        if (refIDs != null) {
            ret = tlibReqs("urs", "id", existingIDs, "referenceID", refIDs, new String[]{"thisDNID"}, dnIDs);

        } else {
            if (cif != null) {
                Integer[] ids;
                Wheres wh = new Wheres();
                if ((ids = cif.getIDs(IDType.ConnID)) != null) {
                    wh.addWhere(getWhere("urs.ConnectionIdid", ids, false));
                }
                if ((ids = cif.getIDs(IDType.UUID)) != null) {
                    wh.addWhere(getWhere("urs.UUIDID", ids, false), "OR");
                }
                if (!wh.isEmpty()) {
                    ret = "(" + wh.makeWhere(false) + ")";
                }
            }
        }
        if (node != null) {
            AddCheckedWhere("urs.nameID", ReferenceType.TEvent, node, "AND", DialogItem.URS_EVENTS_NAME);
            AddCheckedWhere("urs.thisDNID", ReferenceType.DN, node, "AND", DialogItem.URS_EVENTS_DN);
            AddCheckedWhere("urs.sourceid", ReferenceType.App, node, "AND", DialogItem.URS_EVENTS_SERVER);

        }
        addFileFilters("urs", "fileid");
        addDateTimeFilters("urs", "time");

        return addWheres.makeWhere(ret, "AND", true);

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

    void setNode(DynamicTreeNode<OptionNode> childByName) {
        this.node = childByName;
    }

    void setSearchSettings(DynamicTreeNode<OptionNode> FindNode) {
        setNode(FindNode);
    }

    void setIDFinder(IDsFinder cidFinder) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
