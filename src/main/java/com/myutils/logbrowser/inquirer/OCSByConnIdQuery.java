/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import com.myutils.logbrowser.indexer.ReferenceType;

/**
 *
 * @author kvoroshi
 */
public class OCSByConnIdQuery extends IQuery {

    private Integer[] m_ConnIds;
    private ResultSet m_resultSet;
    private DatabaseConnector m_connector;
    private int recCnt;
    private IDsFinder cidFinder = null;
    private DynamicTreeNode<OptionNode> node = null;
    private Collection<Long> recIDs = null;
    private Collection<Long> refIDs = null;
    private Collection<Long> dnIDs;

    public OCSByConnIdQuery(Integer[] connIds) throws SQLException {
        this();
        m_ConnIds = connIds;
    }

    public OCSByConnIdQuery() throws SQLException {
        addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Optional);
        addRef("sourceID", "source", ReferenceType.App.toString(), FieldType.Optional);
        addRef("TransferIDID", "TransferID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.Optional);

        addRef("agentIDID", "agentID", ReferenceType.Agent.toString(), FieldType.Optional);
        addRef("DNISID", "DNIS", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("ANIID", "ANI", ReferenceType.DN.toString(), FieldType.Optional);

        addNullField("ixnid");
        addNullField("typestr");
        addNullField("err");
        addNullField("errmessage");
        addNullField("attr1");
        addNullField("attr2");
    }

    public OCSByConnIdQuery(Integer connId) throws SQLException {
        this(new Integer[1]);
    }

    OCSByConnIdQuery(Collection<Long> refIDs, Collection<Long> ids) {
        this.refIDs = refIDs;
        this.recIDs = ids;
    }

    OCSByConnIdQuery(Collection<Long> refIDs, Collection<Long> ids, Collection<Long> dnIDs) throws SQLException {
        this(refIDs, ids);
        this.dnIDs = dnIDs;
    }

    public void Execute() throws SQLException {
        inquirer.logger.debug("**Execute in  " + this.getClass().toString());
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        query = "SELECT OCS.*,0 as SipId,null as CallId,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  0 as handlerId, \n"
                + "files.component as component, app00.name as app, files.nodeId as nodeid, null as anchorId \n" + getNullFields()
                + getRefFields()
                + "\nFROM \n"
                + "\tocs_logbr AS OCS\n"
                + "INNER JOIN FILE_" + alias + " AS files ON ocs.fileId=files.id \n"
                + "inner join app as app00 on files.appnameid=app00.id\n"
                + getRefs() + "\n"
                + getWhere()
                + "\n"
                + ((inquirer.getCr().isAddOrderBy()) ? " ORDER BY OCS.time" : "")
                + "\n" + getLimitClause()
                + ";\n";

        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

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

    public void Reset() throws SQLException {
        if (m_resultSet != null) {
            m_resultSet.close();
        }
        m_connector.releaseConnector(this);
        m_connector = null;
    }

    void setIDFinder(IDsFinder cidFinder) {
        this.cidFinder = cidFinder;
    }

    private String getWhere() throws SQLException {
        Integer[] ids;

        if (refIDs != null) {
            addWhere(tlibReqs("OCS", "id", recIDs, "referenceID", refIDs, new String[]{"thisDNID"}, dnIDs), "AND");
        }
        if (cidFinder != null && (ids = cidFinder.getIDs(IDType.ConnID)) != null) {
            Wheres wh = new Wheres();
            wh.addWhere(getWhere("OCS.ConnectionIdid", ids, false), "OR");
            if ((ids = cidFinder.getIDs(IDType.OCSRecordHandle)) != null) {
                wh.addWhere(getWhere("recHandle", ids, false), "OR");
            }

            if ((ids = cidFinder.getIDs(IDType.OCSChainID)) != null) {
                wh.addWhere(getWhere("chID", ids, false), "OR");
            }
            addWhere(wh);
        }

        if (node != null) {
            AddCheckedWhere("OCS.nameID", ReferenceType.TEvent, node, "AND", DialogItem.OCS_CALL_TEVENT_NAME);
            AddCheckedWhere("OCS.sourceID", ReferenceType.App, node, "AND", DialogItem.OCS_CALL_TEVENT_SERVER);
        }

        addWhere("OCS.nameID not in (select id from " + ReferenceType.TEvent.toString() + " where name = \"EventUserEvent\")", "AND");
        addFileFilters("OCS", "fileid");
        addDateTimeFilters("OCS", "time");

        return addWheres.makeWhere(true);
//        String sWhere = addWheres.makeWhere(false);
//        if (sWhere.length() > 0) {
//            sWhere = " where " + sWhere;
//        }
//        return sWhere;
    }

    void setSearchSettings(DynamicTreeNode<OptionNode> reportSettings) {
        this.node = reportSettings;
    }

    void setConnIDs(Integer[] connIDs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setRecHandles(Integer[] recHandles) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    void setChainID(Integer[] chIDs) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
