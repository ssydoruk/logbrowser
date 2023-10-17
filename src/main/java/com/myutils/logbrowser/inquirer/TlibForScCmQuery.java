/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author ssydoruk
 */
public final class TlibForScCmQuery extends IQuery {

    private Integer[] m_connectionIds = null;
    private int m_componentFilter;
    private String m_msgFilter;
    private DynamicTreeNode<OptionNode> node;
    private IDsFinder cif = null;
    private HashSet handlerIDs;
    private Collection<Long> refIDs = null;
    private Collection<Long> recIDs = null;
    private Collection<Long> dnIDs;
    private boolean collectHandlers = false;

    public TlibForScCmQuery() throws SQLException {
        this.handlerIDs = new HashSet();
        m_componentFilter = CallFlowResults.TLIB;
        m_msgFilter = "";
        addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("tlib.nameID", "name", ReferenceType.TEvent.toString(), FieldType.Mandatory);
        addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("TransferIDID", "TransferID", ReferenceType.ConnID.toString(), FieldType.Optional);
        addRef("sourceID", "source", ReferenceType.App.toString(), FieldType.Optional);
        addRef("ErrMessageID", "ErrMessage", ReferenceType.TLIBERROR.toString(), FieldType.Optional);
        addRef("agentIDID", "agentID", ReferenceType.Agent.toString(), FieldType.Optional);
        addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.Optional);
        addRef("DNISID", "DNIS", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("ANIID", "ANI", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("PlaceID", "Place", ReferenceType.Place.toString(), FieldType.Optional);

        addNullField("ixnid");
//        addOutField("constToStr(\"CallType \", calltype) typestr"); // bug in SQLite lib; does not accept full word
        addOutField("intToHex( seqno) seqnoHex"); // bug in SQLite lib; does not accept full word
        addRef("attr1id", "attr1", ReferenceType.TLIBATTR1.toString(), FieldType.Optional);
        addRef("attr2id", "attr2", ReferenceType.TLIBATTR2.toString(), FieldType.Optional);

//        addRef("callIDID", "callID", ReferenceType.SIPCALLID.toString(), FieldType.Optional);
    }

    public TlibForScCmQuery(Integer[] connectionIds) throws SQLException {
        this();
        this.handlerIDs = new HashSet();
        m_connectionIds = connectionIds;
    }

    TlibForScCmQuery(DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cif, boolean collectHandlers) throws SQLException {
        this();
        this.handlerIDs = new HashSet();
        this.node = eventsSettings;
        this.cif = cif;
        if (cif != null) {
            this.searchApps = cif.searchApps;
        }
        this.collectHandlers = collectHandlers;

    }

    TlibForScCmQuery(Collection<Long> refIDs, Collection<Long> seqnoIDs) throws SQLException {
        this();
        this.handlerIDs = new HashSet();
        this.refIDs = refIDs;
        this.recIDs = seqnoIDs;
    }

    TlibForScCmQuery(Collection<Long> refIDs, Collection<Long> ids, Collection<Long> dnIDs) throws SQLException {
        this(refIDs, ids);
        this.handlerIDs = new HashSet();
        this.dnIDs = dnIDs;
    }

    public void SetComponentFilter(int filter) {
        m_componentFilter = filter;
    }

    public void SetMessageFilter(String filter) {
        if (filter != null && !filter.isEmpty()) {
            m_msgFilter = filter;
        }
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("Execute");
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        String myWhere = getMyWhere(); //it modifies ref table list, so should be before ref tables are requested
        StringBuilder qString = new StringBuilder(512);
        boolean addTriggers = false;
//        if (!inquirer.getCr().isNewTLibSearch() && DatabaseConnector.TableExist("HANDLER_logbr") && DatabaseConnector.TableExist("trigger_logbr")) {
//            addTriggers = true;
//        }
        qString.append("SELECT tlib.id,"
                        + "tlib.Inbound,"
                        + "tlib.MsgID,"
                        + "tlib.FileId,"
                        + "tlib.FileOffset,"
                        + "tlib.FileBytes,"
                        + "tlib.line,"
                        + "tlib.seqno,"
                        + "tlib.err,"
                        + "tlib.HandlerId,"
                        + "tlib.ReferenceId,"
                        + "tlib.thisDNID,"
                        + "tlib.otherDNID,"
                        + "tlib.time,"
                        + "tlib.calltype"
                )
                .append(addedFieldString(true, true))
                .append("files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  files.component as component, app00.name as app, files.nodeId as nodeid");
//        if((m_componentFilter & CallFlowResults.SIP) == 0)
//        {
//        	qString += ",\n0 AS SipId,NULL AS CallId,tlib.id as anchorid \n";
//        }
//        else
//        {
        if (addTriggers) {
            qString.append(",\nSipId,trgr.fileid as trigfile, trgr.line as trigline \n");
        } else {
            qString.append(",\n0 as SipId,0 as trigfile, 0 as trigline \n");
        }

//        }
        qString.append(getRefFields())
                .append(getNullFields());

        qString.append("\nFROM TLIB_")
                .append(alias)
                .append(" AS tlib \nINNER JOIN FILE_")
                .append(alias)
                .append(" AS files ON tlib.fileId=files.id ")
                //                .append("AND files.component in (")
                //                .append(FileInfoType.type_SessionController.ordinal())
                //                .append(", ")
                //                .append(FileInfoType.type_tController.ordinal())
                //                .append(")")
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

        if (addTriggers) {
            qString.append("\nLEFT JOIN HANDLER_")
                    .append(alias)
                    .append(" AS handler ON handler.id=tlib.HandlerId ")
                    .append("\nLEFT JOIN (SELECT tr1.*, f.runid AS runid \nFROM trigger_")
                    .append(alias)
                    .append(" AS tr1 \nINNER JOIN file_")
                    .append(alias)
                    .append(" AS f ON f.id=tr1.fileid) AS trgr ON trgr.textid=handler.textid AND trgr.runid=files.runid ");
        }
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

        if ((m_componentFilter & CallFlowResults.TC) != 0) {
            qString.append("\nUNION SELECT tlib.*,files.name, files.component, files.nodeid, NULL,NULL,NULL, NULL, NULL \nFROM tlib_")
                    .append(alias)
                    .append(" AS tlib \nINNER JOIN file_")
                    .append(alias)
                    .append(" as files ON tlib.fileId=files.id AND files.component=")
                    .append(FileInfoType.type_InteractionProxy.ordinal())
                    .append(" ")
                    .append(myWhere);
        }

        qString.append("\nORDER BY tlib.time\n")
                .append(getLimitClause())
                .append(";");
        m_resultSet = m_connector.executeQuery(this, qString.toString());
        recCnt = 0;
    }

    @Override
    public ILogRecord GetNext() throws SQLException {
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            TLibEvent tLibEvent = new TLibEvent(m_resultSet);
            recLoaded(tLibEvent);
            if (collectHandlers) {
                int handlerId = tLibEvent.getHandlerId();
                if (handlerId > 0 && handlerIDs != null) {
                    handlerIDs.add(handlerId);
                }
            }
            return tLibEvent;
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

    private String getMyWhere() throws SQLException {
        String ret = "";

        if (refIDs != null) {
            ret = tlibReqs("tlib", "id", recIDs, "referenceID", refIDs, new String[]{"thisDNID", "otherDNID"}, dnIDs);
        } else if (cif == null) {
            if (m_connectionIds != null) {
                ret = getWhere("tlib.ConnectionIdid", m_connectionIds, false);

                String s = " tlib.nameid NOT IN (select id from "
                        + ReferenceType.TEvent + " where name in (\""
                        + m_msgFilter.replace(",", "\",\"") + "\")" + ") ";

                ret += " AND " + s;
            } else {
//                inquirer.logger.debug(removeRef("ConnectionIDID", ReferenceType.ConnID.toString()));
//                addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
            }

        } else {
            if (inquirer.getCr().isNewTLibSearch()) {
                Wheres wh = new Wheres();
                wh.addWhere(getWhere("tlib.id", cif.getIDs(IDType.TLIBID), false), "OR");
                wh.addWhere(getWhere("tlib.ConnectionIdid", cif.getIDs(IDType.ConnID), false), "OR");
                wh.addWhere(getWhere("tlib.UUIDid", cif.getIDs(IDType.UUID), false), "OR");
                if (!wh.isEmpty()) {
                    ret = "(" + wh.makeWhere(false) + ")";
                } else {
                    ret = getWhere("tlib.thisdnid", "tlib.otherdnid", cif.getIDs(IDType.DN), false);

                    if (ret.isEmpty()) {
                        ret = getWhere("tlib.agentidid", cif.getIDs(IDType.AGENT), false);
                    }
                }
            } else {
                Integer[] dnID;
                Integer[] dnConnIDs;
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
                        ret = getWhere("tlib.ConnectionIdid", cif.getConnIDs(), false);
                        break;

                    case AGENTID:
                        ret = getWhere("tlib.agentidid", cif.getAgentIDs(), false);
                        break;

                    case REFERENCEID:
                        ret = getWhere("tlib.referenceid", cif.getRefIDs(), false);
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
            AddCheckedWhere("tlib.aniid", ReferenceType.DN, node, "AND", DialogItem.TLIB_CALLS_TEVENT_ANI);
            AddCheckedWhere("tlib.dnisid", ReferenceType.DN, node, "AND", DialogItem.TLIB_CALLS_TEVENT_DNIS);
            AddCheckedWhere("tlib.attr1id", ReferenceType.TLIBATTR1, node, "AND", DialogItem.TLIB_CALLS_TEVENT_ATTR1);
            AddCheckedWhere("tlib.attr2id", ReferenceType.TLIBATTR2, node, "AND", DialogItem.TLIB_CALLS_TEVENT_ATTR2);
            AddCheckedWhere("tlib.PlaceID", ReferenceType.Place, node, "AND", DialogItem.TLIB_CALLS_TEVENT_Place);
        }

        return addWheres.makeWhere(ret, "AND", true);

    }

    public HashSet getHandlerIDs() {
        return handlerIDs;
    }

}
