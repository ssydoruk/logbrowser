/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.ReferenceType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author ssydoruk
 */
public class OrsByConnIdQuery extends IQuery {

    private Integer[] m_ConnIds;
    private ResultSet m_resultSet;
    private DatabaseConnector m_connector;
    private int recCnt;
    private DynamicTreeNode<OptionNode> node = null;
    private Collection<Long> refIDs = null;
    private Collection<Long> recIDs = null;
    private Collection<Long> dnIDs;
    private IDsFinder cif = null;

    public OrsByConnIdQuery() throws SQLException {
        addRef("ors.thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        addRef("ors.nameID", "name", ReferenceType.TEvent.toString(), FieldType.MANDATORY);
        addRef("ors.otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        addRef("ors.sourceid", "source", ReferenceType.App.toString(), FieldType.OPTIONAL);
        addRef("ors.ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
        addRef("ors.UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.OPTIONAL);
        addRef("TransferIDID", "TransferID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
        addRef("agentIDID", "agentID", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
        addRef("DNISID", "DNIS", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        addRef("ANIID", "ANI", ReferenceType.DN.toString(), FieldType.OPTIONAL);

        addOutField("intToHex( seqno) seqnoHex"); // bug in SQLite lib; does not accept full word

        addNullField("err");
        addNullField("errmessage");
        addNullField("typestr");
        addNullField("attr1");
        addNullField("attr2");
        addNullField("place");
        addNullField("ixnid");

//        addRef("ixnIDID", "IXNID", ReferenceType.IxnID.toString(), FieldType.Optional);
    }

    public OrsByConnIdQuery(Integer[] connIds) throws SQLException {
        this();
        m_ConnIds = connIds;
    }

    public OrsByConnIdQuery(Integer connId) throws SQLException {
        this(new Integer[]{connId});
    }

    //    OrsByConnIdQuery(HashSet<Long> refIDs, ArrayList<Long> ids, HashSet<Long> idDNs) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
    OrsByConnIdQuery(Collection<Long> refIDs, Collection<Long> seqnoIDs) throws SQLException {
        this();
        this.refIDs = refIDs;
        this.recIDs = seqnoIDs;
    }

    OrsByConnIdQuery(Collection<Long> refIDs, Collection<Long> ids, Collection<Long> dnIDs) throws SQLException {
        this(refIDs, ids);
        this.dnIDs = dnIDs;
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

//        if (m_ConnIds == null || m_ConnIds.length == 0) {
//            throw new Exception("no conn ID defined");
//        }
//
//        String connIdCondition = "(ors.ConnectionId=\"" + m_ConnIds[0] + "\"";
//        for (int i = 1; i < m_ConnIds.length; i++) {
//            connIdCondition += " OR ors.ConnectionId=\"" + m_ConnIds[i] + "\"";
//        }
//        connIdCondition += ")";
        StringBuilder qString = new StringBuilder(512);
        qString.append("SELECT ")
                .append(stdFields("ors"))
                .append(",ors.Inbound,ors.seqno,ors.HandlerId,ors.ReferenceId,ors.thisDNID,ors.otherDNID,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  \n")
                .append("files.component as component, app00.name as app, files.nodeId as nodeid, null as anchorId \n")
                .append(getRefFields())
                .append(getNullFields())
                .append(addedFieldString(true, false))
                .append("\nFROM ORS_logbr AS ors INNER JOIN FILE_logbr AS files ON ors.fileId=files.id \n")
                .append("inner join app as app00 on files.appnameid=app00.id\n")
                .append(getRefs())
                .append("\n")
                .append(getMyWhere())
                .append((inquirer.getCr().isAddOrderBy()) ? " ORDER BY ors.time " : " ")
                .append("\n")
                .append(getLimitClause())
                .append(";\n ");

//         query = "SELECT ors.*,0 as SipId,null as CallId,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  \n"+
//                 "null as source,\n"+
//                "files.component as component, files.nodeId as nodeid, tlib2.id as anchorId \n"+
//                "FROM orsevent_" + alias + " AS ors INNER JOIN FILE_" + alias +
//                " AS files ON ors.fileId=files.id \n"+
//
//                //LEFT JOIN HANDLER_" + alias +
//                //" AS handler ON handler.id=tlib.HandlerId \n"+
//
//
//                "INNER JOIN TLIB_" + alias + " AS tlib2 \n"+
//                "ON ors.connectionId=tlib2.connectionId AND ors.name=tlib2.name \n"+
//                "AND ors.thisdn=tlib2.thisdn \n"+
//                 
//                 // this is not necessary. Time can be quite different, we cannot ignore events (Stepan)
//                //"AND tlib2.time BETWEEN ors.time-1000 AND ors.time+1000 \n"+
//                "INNER JOIN FILE_"+alias+" AS files2 \n"+
//                "ON tlib2.fileId=files2.id AND files2.Component="+FileInfoType.type_SessionController.ordinal()+" \n"+
//
////                 "LEFT JOIN HANDLER_" + alias +
////                " AS handler ON handler.id=tlib2.HandlerId \n"+
//
////                "LEFT JOIN \n"+
////// ...left join trigger table merged with file to verify runid right away
////// in join condition...
//// "(SELECT tr1.*, f.runid AS runid FROM trigger_"+alias+" AS tr1 INNER JOIN \n"+
//// "file_"+alias+" AS f ON f.id=tr1.fileid) AS trgr ON trgr.text=handler.text AND\n"+
//// " trgr.runid=files2.runid\n"+
////// ... left join sip table ...
////                " LEFT JOIN SIP_" + alias + " AS sip ON sip.handlerid=trgr.HandlerId \n"+
////                "AND sip.handlerid IS NOT NULL AND sip.handlerid!=0 and sip.inbound=1 \n"+
//                " WHERE " + connIdCondition + 
//                 " ORDER BY ors.time;"; 
        m_resultSet = m_connector.executeQuery(this, qString.toString());
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

    void setReportSettings(DynamicTreeNode<OptionNode> reportSettings) {
        this.node = reportSettings;
    }

    private String getMyWhere() throws SQLException {
//        if (node != null) {
//            AddCheckedWhere("ors.nameID", ReferenceType.TEvent, node, "AND", DialogItem.ORS_TEVENTS_NAME);
//            AddCheckedWhere("ors.thisdnid", ReferenceType.DN, node, "AND", DialogItem.ORS_TEVENTS_DN);
//        }
//        addFileFilters("ors", "fileid");
//        return addWheres.makeWhere(getWhere("ors.ConnectionIdid", m_ConnIds, false), "AND", true);

        String ret = "";
        Wheres wh1 = null;

        if (refIDs != null) {
            ret = tlibReqs("ors", "id", recIDs, "referenceID", refIDs, new String[]{"thisDNID", "otherDNID"}, dnIDs);
        } else {
            if (cif == null) {
                if (m_ConnIds != null) {
                    ret = getWhere("ors.ConnectionIdid", m_ConnIds, false);
                } else {
//                inquirer.logger.debug(removeRef("ConnectionIDID", ReferenceType.ConnID.toString()));
//                addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
                }

            } else {
                wh1 = new Wheres();
                Integer[] dnID = cif.getIDs(IDType.DN);
                if (dnID != null && dnID.length > 0) {
                    Wheres wh2 = new Wheres();
                    wh2.addWhere(getWhere("ors.thisdnid", dnID, false));
                    wh2.addWhere(getWhere("ors.otherdnid", dnID, false), "OR");
                    wh1.addWhere(wh2);
                }

                Wheres wh2 = new Wheres();
                wh2.addWhere(getWhere("ors.ConnectionIdid", cif.getIDs(IDType.ConnID), false), "OR");
                wh2.addWhere(getWhere("ors.uuidID", cif.getIDs(IDType.UUID), false), "OR");
                wh2.addWhere(getWhere("ors.orsCallID", cif.getIDs(IDType.ORSCallID), false), "OR");

                if (!wh2.isEmpty()) {
                    wh1.addWhere(wh2);
                }
            }
            if (wh1 != null && !wh1.isEmpty()) {
                addWhere(wh1, "AND");
            }
        }
        addFileFilters("ors", "fileid");
        addDateTimeFilters("ors", "time");
        if (node != null) {
            AddCheckedWhere("ors.nameID", ReferenceType.TEvent, node, "AND", DialogItem.ORS_TEVENTS_NAME);
            AddCheckedWhere("ors.sourceID", ReferenceType.App, node, "AND", DialogItem.ORS_TEVENTS_SOURCE);
//            AddCheckedWhere("ors.sourceID", ReferenceType.App, node, "AND", DialogItem.ors);
            Wheres wh = new Wheres();
            wh.addWhere(getCheckedWhere("ors.thisDNID", ReferenceType.DN, node, DialogItem.ORS_TEVENTS_DN), "OR");
            wh.addWhere(getCheckedWhere("ors.otherDNID", ReferenceType.DN, node, DialogItem.ORS_TEVENTS_DN), "OR");

            addWhere(wh, "AND");
        }

        return addWheres.makeWhere(ret, "AND", true);

    }

    void setIDFinder(IDsFinder cidFinder) {
        cif = cidFinder;
    }

}
