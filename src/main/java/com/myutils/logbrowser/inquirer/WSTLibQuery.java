/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.sql.ResultSet;
import java.sql.SQLException;
import com.myutils.logbrowser.indexer.ReferenceType;

/**
 *
 * @author kvoroshi
 */
public class WSTLibQuery extends IQuery {

    private Integer[] m_connectionIds = null;
    private ResultSet m_resultSet;
    private DatabaseConnector m_connector;
    private int m_componentFilter;
    private String m_msgFilter;
    private int recCnt;
    private DynamicTreeNode<OptionNode> node;
    private boolean regex = false;
    private String selection = null;
    private SelectionType selectionType = SelectionType.CONNID;
    private IDsFinder cif = null;

    public WSTLibQuery() throws SQLException {
        m_componentFilter = CallFlowResults.TLIB;
        m_resultSet = null;
        m_msgFilter = "";
        addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("tlib.nameID", "name", ReferenceType.TEvent.toString(), FieldType.Optional);
        addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("TransferIDID", "TransferID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("sourceID", "source", ReferenceType.App.toString(), FieldType.Optional);
        addRef("agentidid", "agentid", ReferenceType.Agent.toString(), FieldType.Optional);
//        addRef("ErrMessageID", "ErrMessage", ReferenceType.TLIBERROR.toString(), FieldType.Optional);
        addOutField("intToHex( seqno) seqnoHex"); // bug in SQLite lib; does not accept full word

        addNullField("ErrMessageID");
        addNullField("ixnid");
        addNullField("SipId");
        addNullField("attr1");
        addNullField("attr2");
//        addNullField("source");
//        addOutField("constToStr(\"CallType \", calltype) typestr"); // bug in SQLite lib; does not accept full word

//        addRef("callIDID", "callID", ReferenceType.SIPCALLID.toString(), FieldType.Optional);
    }

    public WSTLibQuery(Integer[] connectionIds) throws SQLException {
        this();
        m_connectionIds = connectionIds;
    }

    WSTLibQuery(DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cif) throws SQLException {
        this();
        this.node = eventsSettings;
        this.cif = cif;
        if (cif != null) {
            this.searchApps = cif.searchApps;
        }
    }

    public void SetComponentFilter(int filter) {
        m_componentFilter = filter;
    }

    public void SetMessageFilter(String filter) {
        if (filter != null && !filter.isEmpty()) {
            m_msgFilter = filter;
        }
    }

    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        String myWhere = getMyWhere(); //it modifies ref table list, so should be before ref tables are requested
        StringBuilder qString = new StringBuilder(512);

        String addedFieldString = addedFieldString(true, true);

        qString.append("SELECT tlib.*")
                .append(addedFieldString)
                .append((addedFieldString.isEmpty() ? "," : ""))
                .append("files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  files.component as component, app00.name as app, files.nodeId as nodeid");
//        if((m_componentFilter & CallFlowResults.SIP) == 0)
//        {
//        	qString += ",\n0 AS SipId,NULL AS CallId,tlib.id as anchorid \n";
//        }
//        else
//        {
        qString.append(",\n0 as anchorId \n");
//        }
        qString.append(getRefFields())
                .append(getNullFields());

        qString.append("\nFROM wstlib")
                .append(" AS tlib \nINNER JOIN FILE_")
                .append(alias)
                .append(" AS files ON tlib.fileId=files.id")
                .append("\ninner join app as app00 on files.appnameid=app00.id\n");
        qString.append(getRefs());
        // ...left join trigger table merged with file to verify runid right away
        // in join condition...
//        qString += "LEFT JOIN HANDLER_logbr AS handler ON handler.id=tlib.HandlerId \n"
//                +          
//                "LEFT JOIN trigger_logbr AS trgr ON (trgr.textid=handler.textid ) \n"
//                                + "inner JOIN FILE_logbr AS tf ON ( trgr.fileId=tf.id and tf.runid=files.runid)\n"
//
////                + "\nLEFT JOIN SIP_" + alias + " AS sip ON sip.handlerid=trgr.HandlerId "
////                + "AND sip.handlerid IS NOT NULL AND sip.handlerid!=0 "
//                ;

//                + "\ninner JOIN SIP_" + alias + " AS sip ON sip.handlerid=trgr.HandlerId "
//                + "AND sip.handlerid IS NOT NULL AND sip.handlerid!=0 ";
//        qString += "\nLEFT JOIN HANDLER_" + alias + " AS handler ON handler.id=tlib.HandlerId "
//                + "\nLEFT JOIN (SELECT tr1.*, f.runid AS runid "
//                + "\nFROM trigger_" + alias + " AS tr1 "
//                + "\nINNER JOIN file_" + alias + " AS f ON f.id=tr1.fileid) "
//                + "AS trgr ON trgr.textid=handler.textid AND trgr.runid=files.runid "
//                + "\nLEFT JOIN SIP_" + alias + " AS sip ON sip.handlerid=trgr.HandlerId "
//                + "AND sip.handlerid IS NOT NULL AND sip.handlerid!=0 ";
//        qString += myWhere+" and tf.runid=hf.runid ";
        qString.append(myWhere).append("  ");

        qString.append("\nORDER BY tlib.time\n")
                .append(getLimitClause())
                .append(";");
        m_resultSet = m_connector.executeQuery(this, qString.toString());
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
        doneExtracting(MsgType.TLIB);
        return null;
    }

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

    private String getMyWhere() {
        String ret = "";

        if (cif == null) {
            if (m_connectionIds != null) {
                ret = getWhere("tlib.ConnectionIdid", m_connectionIds, false);

                String s = " tlib.nameid NOT IN (select id from "
                        + ReferenceType.TEvent.toString() + " where name in (\""
                        + m_msgFilter.replace(",", "\",\"") + "\")" + ") ";

                ret += " AND " + s;
            } else {
//                inquirer.logger.debug(removeRef("ConnectionIDID", ReferenceType.ConnID.toString()));
//                addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
            }

        } else {
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

                case AGENTID:
                    if (cif != null) {
                        ret = getWhere("tlib.agentidid", cif.getAgentIDs(), false);
                    }
                    break;

                case GUESS_SELECTION:
                    if ((dnID = cif.getDNs()) != null && dnID.length > 0) {
                        ret = " ( " + getWhere("tlib.thisdnid", dnID, false)
                                + " or " + getWhere("tlib.otherdnid", dnID, false) + " )";
                    } else {
                        if ((dnConnIDs = cif.getConnIDs()) != null && dnConnIDs.length > 0) {
                            ret = getWhere("tlib.ConnectionIdid", dnConnIDs, false);
                        }

                    }
                    break;

                default:
                    break;
            }
        }
        addFileFilters("tlib", "fileid");
        addDateTimeFilters("tlib", "time");
        if (node != null) {
            AddCheckedWhere("tlib.nameID", ReferenceType.TEvent, node, "AND", DialogItem.TLIB_CALLS_TEVENT_NAME);
            AddCheckedWhere("tlib.sourceID", ReferenceType.App, node, "AND", DialogItem.TLIB_CALLS_SOURCE);
            Wheres wh = new Wheres();
            wh.addWhere(getCheckedWhere("tlib.thisDNID", ReferenceType.DN, node, DialogItem.TLIB_CALLS_TEVENT_DN), "OR");
            wh.addWhere(getCheckedWhere("tlib.otherDNID", ReferenceType.DN, node, DialogItem.TLIB_CALLS_TEVENT_DN), "OR");
            addWhere(wh, "AND");
            AddCheckedWhere("tlib.agentidid", ReferenceType.Agent, node, "AND", DialogItem.TLIB_CALLS_TEVENT_AGENT);
        }

        return addWheres.makeWhere(ret, "AND", true);

    }

}
