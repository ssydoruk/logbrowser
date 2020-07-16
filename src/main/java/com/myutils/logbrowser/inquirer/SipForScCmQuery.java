/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author ssydoruk
 */
public class SipForScCmQuery extends IQuery {

    private String[] orderBy;
    private HashSet handlerIDs = new HashSet();
    private boolean collectHandlers = false;

    private Integer[] m_CallIds;
    private boolean m_useProxy = true;
    private ResultSet m_resultSet;
    private DatabaseConnector m_connector;
    private int recCnt;
    private DynamicTreeNode<OptionNode> node = null;
    private IDsFinder cidFinder;

    public SipForScCmQuery() throws SQLException {
        addRef("ToUriID", "ToUri", ReferenceType.SIPURI.toString(), FieldType.Optional);
        addRef("FromUriID", "FromUri", ReferenceType.SIPURI.toString(), FieldType.Optional);
        addRef("ViaUriID", "ViaUri", ReferenceType.SIPURI.toString(), FieldType.Optional);
        addRef("callIDID", "callID", ReferenceType.SIPCALLID.toString(), FieldType.Optional);
        addRef("nameID", "name", ReferenceType.SIPMETHOD.toString(), FieldType.Optional);
        addRef("CSeqID", "CSeq", ReferenceType.SIPCSEQ.toString(), FieldType.Optional);
        addRef("FromUserpartID", "FromUserpart", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("requestURIDNid", "requestURIDN", ReferenceType.DN.toString(), FieldType.Optional);
        addRef("ViaBranchID", "ViaBranch", ReferenceType.SIPVIABRANCH.toString(), FieldType.Optional);
        addRef("PeerIpID", "PeerIp", ReferenceType.IP.toString(), FieldType.Optional);
        addRef("uuidid", "uuid", ReferenceType.UUID.toString(), FieldType.Optional);
        addNullField("sipid");

        setOrderBy(new String[]{"time"});
    }

    public SipForScCmQuery(HashMap callIds, boolean useProxy) throws Exception {
        this();
        m_CallIds = (Integer[]) callIds.keySet().toArray(new Integer[callIds.size()]);
        m_useProxy = useProxy;
    }

    SipForScCmQuery(DynamicTreeNode<OptionNode> sipReportSettings) throws SQLException {
        this();
        this.node = sipReportSettings;
    }

    SipForScCmQuery(DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder, boolean collectHandlers) throws SQLException {
        this();
        this.node = reportSettings;
        this.cidFinder = cidFinder;
        this.collectHandlers = collectHandlers;
    }

    public String getTabAlias() {
        return "sip";
    }

    @Override
    public void Execute() throws SQLException {
        inquirer.logger.debug("**Execute in  " + this.getClass().toString());
        m_connector = DatabaseConnector.getDatabaseConnector(this);
        String alias = m_connector.getAlias();

        query = "SELECT sip.*,files.id as files_id,files.fileno as fileno, files.appnameid as appnameid, files.name as filename, files.arcname as arcname,  app00.name as app, "
                + "files.component as component \n"
                + getRefFields()
                + getNullFields()
                + "\nFROM SIP_"
                + alias + " AS sip \n"
                + "INNER JOIN FILE_"
                + alias + " AS files on fileId=files.id "
                + "\ninner join app as app00 on files.appnameid=app00.id\n"
                + (m_useProxy ? "" : "AND (files.component in (" + FileInfoType.type_CallManager.ordinal() + ", " + FileInfoType.type_SessionController.ordinal() + " ) ")
                + getRefs()
                + "\n" + getJoins()
                + "\n"
                + getMyWhere()
                + "\n "
                + getOrderBy()
                + "\n" + getLimitClause() + ";";

        m_resultSet = m_connector.executeQuery(this, query);
        recCnt = 0;
    }

//    private HashMap sipRecords = new HashMap();
//    public HashMap getSipRecords() {
//        return sipRecords;
//    }
    @Override
    public ILogRecord GetNext() throws SQLException {
        on_error:
        if (m_resultSet.next()) {
            recCnt++;
            QueryTools.DebugRec(m_resultSet);
            try {
                SipMessage rec = new SipMessage(m_resultSet);
//                sipRecords.put(sipMessage.GetId(), sipMessage);
                if (collectHandlers) {
                    int handlerId = rec.getHandlerId();
                    if (handlerId > 0 && handlerIDs != null) {
                        handlerIDs.add(handlerId);
                    }
                }
                recLoaded(rec);
                return rec;
            } catch (SQLException e) {
                break on_error;
            }
        }
        doneExtracting(MsgType.SIP);
        return null;
    }

    public HashSet getHandlerIDs() {
        if (collectHandlers) {
            return handlerIDs;
        } else {
            return null;
        }
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

        if (cidFinder == null) {
            ret = getWhere("sip.callidID", m_CallIds, false);
        } else {
            if (inquirer.getCr().isNewTLibSearch()) {
                Wheres wh = new Wheres();
                wh.addWhere(getWhere("sip.id", cidFinder.getIDs(IDType.SIPID), false), "OR");
                wh.addWhere(getWhere("sip.uuidid", cidFinder.getIDs(IDType.UUID), false), "OR");
                wh.addWhere(ret = getWhere("sip.callidID", cidFinder.getIDs(IDType.SIPCallID), false), "OR");
                if (!wh.isEmpty()) {
                    ret = "(" + wh.makeWhere(false) + ")";
                }
                if (ret.isEmpty()) {
                    ret = getWhere("sip.FromUserpartID", "sip.requestURIDNid", cidFinder.getIDs(IDType.DN), false);
                }
                if (ret.isEmpty()) {
                    ret = getWhere("sip.PeerIpID", cidFinder.getIDs(IDType.PEERIP), false);
                }
            } else {
                SelectionType searchType = cidFinder.getSearchType();
                if (searchType == SelectionType.DN || searchType == SelectionType.GUESS_SELECTION) {
                    Integer[] dnID = cidFinder.getSIPDNs();
                    if (dnID != null) {
                        addWhere(" ( " + getWhere("sip.FromUserpartID", dnID, false) + " or "
                                + getWhere("sip.requestURIDNid", dnID, false) + " )", "AND");
                    }
                }
                if (searchType == SelectionType.PEERIP || searchType == SelectionType.GUESS_SELECTION) {
                    Integer[] peerID = cidFinder.getPeerIDs();
                    if (peerID != null) {
                        addWhere(getWhere("sip.PeerIpID", peerID), "AND");
                    }
                }
                if (searchType == SelectionType.CALLID || searchType == SelectionType.GUESS_SELECTION
                        || searchType == SelectionType.CONNID || searchType == SelectionType.UUID) {
                    {
                        ret = getWhere("sip.callidID", cidFinder.getCallIDs(), false);
                    }
                }
            }
        }
        if (node
                != null) {
            AddCheckedWhere("sip.nameID", ReferenceType.SIPMETHOD, node, "AND", DialogItem.TLIB_CALLS_SIP_NAME);
//            AddCheckedWhere("sip.FromUserpartID", ReferenceType.DN, node, "AND", DialogItem.TLIB_CALLS_SIP_ENDPOINT);            
            addWhere(IQuery.getAttrsWhere(new String[]{"FromUserpartID", "requestURIDNid"},
                    ReferenceType.DN, FindNode(node, DialogItem.TLIB_CALLS_SIP_ENDPOINT, null, null)), "AND");
            AddCheckedWhere("sip.PeerIpID", ReferenceType.IP, node, "AND", DialogItem.TLIB_CALLS_SIP_PEERIP);
            addWhere(IQuery.getAttrsWhere("inbound", FindNode(node, DialogItem.TLIB_CALLS_SIP_DIRECTION, null, null)), "AND");

        }

        addFileFilters(
                "sip", "fileid");
        addDateTimeFilters(
                "sip", "time");
        String r = addWheres.makeWhere(ret,
                "AND", true);
        return r;
    }

//    void setRegexSearch(boolean regex) {
//        this.regexSearch = regex;
//    }
//
//    void setSearch(String selection) {
//        this.searchString = selection;
//    }
//
//    void setSearchType(SelectionType st) {
//        this.searchType = st;
//    }
    void setOrderBy(String[] orderBy
    ) {
        String[] arr = new String[orderBy.length];
        for (int i = 0; i < orderBy.length; i++) {
            arr[i] = getTabAlias() + "." + orderBy[i];

        }
        this.orderBy = arr;
    }

    private String getOrderBy() {
        if (inquirer.getCr().isAddOrderBy() && orderBy.length > 0) {
            String join = StringUtils.join(orderBy, ";");
            return "ORDER BY " + StringUtils.join(orderBy, ";");
//        ORDER BY sip.time;

        } else {
            return "";
        }
    }

}
