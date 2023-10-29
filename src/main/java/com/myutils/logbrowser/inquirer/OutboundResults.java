/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.myutils.logbrowser.inquirer.DatabaseConnector.CampaignDBIDtoName;
import static com.myutils.logbrowser.inquirer.DatabaseConnector.GroupDBIDtoName;

/**
 * @author ssydoruk
 */
enum OutbRequestType {

    CAMPAIGN_GROUP,
    AGENT,
    CALL
}

final class OutboundResults extends IQueryResults {

    IDsFinder cidFinder;
    private ArrayList<NameID> appType = null;
    private UTCTimeRange timeRange = null;

    //    private DynamicTreeNode<OptionNode> repComponents=new DynamicTreeNode<OptionNode>();
    OutboundResults() throws SQLException {
        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        addOutboundCallParams(rootA);
        addOutboundCampaignParams(rootA);
        addOutboundAgentParams(rootA);
        addConfigUpdates(rootA);

        addCustom(rootA, FileInfoType.type_OCS);
        rootA.addLogMessagesReportType(TableType.MsgOCServer);

        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.CG);
        addSelectionType(SelectionType.AGENT);
        addSelectionType(SelectionType.CONNID);
        addSelectionType(SelectionType.RecordHandle);
        addSelectionType(SelectionType.ChainID);
        addSelectionType(SelectionType.DialedNumber);

//        repComponents.add("Campaign Group");
//        repComponents.add("Agent");
//        repComponents.add("Call");
        DoneSTDOptions();

    }

    @Override
    public String getSearchString() {
        if (cidFinder != null) {
            return cidFinder.getSelection(); //To change body of generated methods, choose Tools | Templates.
        } else {
            return super.getSearchString();
        }
    }

    //    private void GeneralRequest() throws SQLException {
//        // report on all running campaigns
//        TableQuery OCSCG = new TableQuery(MsgType.OCSCG, "ocscg_logbr");
//        OCSCG.addRef("cgnameID", "cgname", ReferenceType.OCSCG.toString(), FieldType.Optional);
//        getRecords(OCSCG);
//
//        TableQuery OCSPASI = new TableQuery(MsgType.OCSPASI, "ocspasi_logbr");
//        getRecords(OCSPASI);
//
//        TableQuery OCSAS = new TableQuery(MsgType.OCSASSIGNMENT, "ocsasgn_logbr");
//        OCSAS.addRef("cgnameID", "cgname", ReferenceType.OCSCG.toString(), FieldType.Optional);
//        getRecords(OCSAS);
//
//        getGenesysMessages(TableType.MsgOCServer, timeRange);
//
//        if (DatabaseConnector.getDatabaseConnector(this).TableExist(ReferenceType.StatEvent.toString())
//                && DatabaseConnector.getDatabaseConnector(this).TableExist(ReferenceType.Agent.toString())
//                && DatabaseConnector.getDatabaseConnector(this).TableExist(ReferenceType.Place.toString())) {
//            TableQuery OCSSTATEV = new TableQuery(MsgType.OCSSTATEV, "OcsStatEv_logbr");
//            OCSSTATEV.setIdsSubQuery(OCSSTATEV.getTabAlias() + ".StatEvNameID",
//                    getRefSubQuery(ReferenceType.StatEvent,
//                            new String[]{"SEventStatInvalid", "SEventStatClosed", "SEventError"}));
//            OCSSTATEV.addRef("StatEvNameID", "stat", ReferenceType.StatEvent.toString(), FieldType.Optional);
//            OCSSTATEV.addRef("agentNameID", "agent", ReferenceType.Agent.toString(), FieldType.Optional);
//            OCSSTATEV.addRef("placeNameID", "place", ReferenceType.Place.toString(), FieldType.Optional);
//            getRecords(OCSSTATEV);
//        }
////        getClient();
//    }
//
//    private void getClient() throws SQLException {
////        getClient(null, null, dlg);
//    }
    private void getClient(Integer[] cgDBID, DynamicTreeNode<OptionNode> node, QueryDialog dlg) throws SQLException {
        if (DatabaseConnector.TableExist("OCSClient")) {
            TableQuery OCSClient = new TableQuery(MsgType.OCSClient, "OCSClient");
            OCSClient.addRef("nameID", "name", ReferenceType.OCSClientRequest.toString(), FieldType.OPTIONAL);

            if (cgDBID != null && cgDBID.length > 0) {
//            OCSClient.setWhereType(WhereType.WhereExists);
//            OCSClient.setExistsTable("ocspasi_logbr");
//            OCSClient.addWhereField("CampaignDBID");
//            OCSClient.addWhereField("GroupDBID");
                List<List<Long>> iDsMultiple = DatabaseConnector.getIDsMultiple("select"
                        + "\ndistinct o.campaigndbid, o.groupdbid"
                        + "\nfrom ocspasi_logbr o\n"
                        + getWhere("o.cgdbid", cgDBID, true));
                OCSClient.addWhere(
                        getWhere(new String[]{"OC01.CampaignDBID", "OC01.GroupDBID"}, iDsMultiple, false), "AND");
            }
            if (node != null) {
                OCSClient.AddCheckedWhere(OCSClient.getTabAlias() + ".nameID", ReferenceType.TEvent, node, "AND", DialogItem.OCSCG_CLIENT_REQUEST);
            }
            OCSClient.addOutField(CampaignDBIDtoName + "(campaigndbid) campaign");
            OCSClient.addOutField(GroupDBIDtoName + "(groupdbid) agentGroup");
            OCSClient.setCommonParams(this, dlg);

            getRecords(OCSClient);
        }
    }

    private void QueryCampaignGroup(String cg, QueryDialog dlg, DynamicTreeNode<OptionNode> OCSCallSettings) throws SQLException {
        Integer cgDBID = -1;
        boolean sendRequest = true;
        DynamicTreeNode<OptionNode> node = null;
        ArrayList<Integer> searchApps = dlg.getSearchApps();
        if (cg != null) {
            cgDBID = getCGDBID(cg);
            if (cgDBID <= 0) {
                inquirer.logger.info("Campaign group [" + cg + "] not found");
                return;
            }
        }

//<editor-fold defaultstate="collapsed" desc="OCSCG_PASESSIONINFO">
        sendRequest = true;
        if (OCSCallSettings != null) {
            node = getChildByName(OCSCallSettings, DialogItem.OCSCG_PASESSIONINFO);
            if (node != null && !((OptionNode) node.getData()).isChecked()) {
                sendRequest = false;
            }
        }
        if (sendRequest) {
            TableQuery OCSPASI = null;
            if (cgDBID > 0) {
                OCSPASI = new TableQuery(MsgType.OCSPASI, "ocspasi_logbr", "cgDBID", getWhere("cgDBID", new Integer[]{cgDBID}, false));
            } else {
                OCSPASI = new TableQuery(MsgType.OCSPASI, "ocspasi_logbr");
            }
            OCSPASI.setCommonParams(this, dlg);
            getRecords(OCSPASI);
        }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="OCSCG_CAMPAIGNGROUP">
        sendRequest = true;
        if (OCSCallSettings != null) {
            node = getChildByName(OCSCallSettings, DialogItem.OCSCG_CAMPAIGNGROUP);
            if (node != null && !((OptionNode) node.getData()).isChecked()) {
                sendRequest = false;
            }
        }
        if (sendRequest) {
            TableQuery OCSCG = null;
            if (cgDBID > 0) {
                OCSCG = new TableQuery(MsgType.OCSCG, "ocscg_logbr", "cgDBID", getWhere("cgDBID", new Integer[]{cgDBID}, false));
            } else {
                OCSCG = new TableQuery(MsgType.OCSCG, "ocscg_logbr", "cgDBID");
            }
            OCSCG.addRef("cgnameID", "cgname", ReferenceType.OCSCG.toString(), FieldType.OPTIONAL);
            OCSCG.setSearchApps(searchApps);
            getRecords(OCSCG);
        }
//</editor-fold>        

//<editor-fold defaultstate="collapsed" desc="OCSCG_AGENT_ASSIGNMENT">
        sendRequest = true;
        if (OCSCallSettings != null) {
            node = getChildByName(OCSCallSettings, DialogItem.OCSCG_AGENT_ASSIGNMENT);
            if (node != null && !((OptionNode) node.getData()).isChecked()) {
                sendRequest = false;
            }
        }
        if (sendRequest) {
            TableQuery OCSAS = null;
            if (cgDBID > 0) {
                OCSAS = new TableQuery(MsgType.OCSASSIGNMENT, "ocsasgn_logbr", "cgnameID",
                        getWhere("cgnameID",
                                DatabaseConnector.getDatabaseConnector(this).getIDs(this, ReferenceType.OCSCG.toString(), "id", getWhere("name", new String[]{cg, "Inbound"}, true)),
                                false));

            } else {
                OCSAS = new TableQuery(MsgType.OCSASSIGNMENT, "ocsasgn_logbr");
            }
            OCSAS.addRef("cgnameID", "cgname", ReferenceType.OCSCG.toString(), FieldType.OPTIONAL);
            OCSAS.setCommonParams(this, dlg);
            getRecords(OCSAS);
        }
//</editor-fold>            

//<editor-fold defaultstate="collapsed" desc="OCSCG_CLIENT">
        sendRequest = true;
        if (OCSCallSettings != null) {
            node = getChildByName(OCSCallSettings, DialogItem.OCSCG_CLIENT);
            if (node != null && !((OptionNode) node.getData()).isChecked()) {
                sendRequest = false;
            }
        }
        if (sendRequest) {
            if (cgDBID > 0) {
                getClient(new Integer[]{cgDBID}, null, dlg);
            } else {
                getClient(null, null, dlg);
            }
        }
//</editor-fold>            

//<editor-fold defaultstate="collapsed" desc="OCS_CG_PA_AGENT_INFO">
        sendRequest = true;
        if (OCSCallSettings != null) {
            node = getChildByName(OCSCallSettings, DialogItem.OCS_CG_PA_AGENT_INFO);
            if (node != null && !((OptionNode) node.getData()).isChecked()) {
                sendRequest = false;
            }
        }
        if (sendRequest) {
            TableQuery OCSPAAI = null;
            if (cgDBID > 0) {
                OCSPAAI = new TableQuery(MsgType.OCSPAAGI, "ocspaagi_logbr", "cgDBID", getWhere("cgDBID", new Integer[]{cgDBID}, false));
            } else {
                OCSPAAI = new TableQuery(MsgType.OCSPAAGI, "ocspaagi_logbr");
            }
            OCSPAAI.setSearchApps(searchApps);
            getRecords(OCSPAAI);
        }
//</editor-fold>            

//<editor-fold defaultstate="collapsed" desc="OCSCG_PAEVENTINFO">
        sendRequest = true;
        if (OCSCallSettings != null) {
            node = getChildByName(OCSCallSettings, DialogItem.OCSCG_PAEVENTINFO);
            if (node != null && !((OptionNode) node.getData()).isChecked()) {
                sendRequest = false;
            }
        }
        if (false) {
            TableQuery OCSPAEVI = null;
            if (cgDBID > 0) {
                OCSPAEVI = new TableQuery(MsgType.OCSPAEVI, "ocspaevi_logbr", "cgDBID", getWhere("cgDBID", new Integer[]{cgDBID}, false));
            } else {
                OCSPAEVI = new TableQuery(MsgType.OCSPAEVI, "ocspaevi_logbr", "cgDBID");
            }
            OCSPAEVI.addOutField("constToStr(\"CallResult \", callresult) callresultstr");
            OCSPAEVI.addOutField("CampaignGroupDBIDtoName( cgdbid) campaignGroup");

            OCSPAEVI.setCommonParams(this, dlg);

            getRecords(OCSPAEVI);
        }
//</editor-fold>            

    }

    public void Retrieve() throws SQLException {

//        switch (reqType) {
//            case AGENT:
//                QueryAgent();
//                break;
//
//            case CALL:
//                QueryCall();
//                break;
//
//            case CAMPAIGN_GROUP:
//                QueryCampaignGroup(cg, null, null);
//                break;
//        }
//        Sort();
//        LookupMark();
    }

    @Override
    public void ApplyMark(String mark) {
    }

    private void LookupMark() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Integer getCGDBID(String cg) throws SQLException {
        if (cg != null && cg.length() > 0) {
            Integer[] ids = DatabaseConnector.getDatabaseConnector(this).getIDs(this, ReferenceType.OCSCG.toString(), "id", getWhere("name", new String[]{cg}, true));
            if (ids.length == 1) {
                ids = DatabaseConnector.getDatabaseConnector(this).getIDs(this, "ocscg_logbr", "cgDBID", getWhere("cgNameID", ids, true));
            }
            switch (ids.length) {
                case 1:
                    return ids[0];
                case 0:
                    return 0;

                default:
                    throw new SQLException(this + ": more than one CGDBID returned: " + ids.length + "[" + cg + "]");
            }
        } else {
            return 0;
        }
    }

    private void UserEventsByConnIDs(QueryDialog dlg, IDsFinder cidFinder) throws SQLException {
        tellProgress("Retrieving user events");
        TableQuery OCSUEvents = new TableQuery(MsgType.OCS, "ocs_logbr");

//        Integer[] DNID = DatabaseConnector.getIDs("ocs_logbr", "thisDNID", getWhere("ConnectionIDID", cidFinder.getIDs(IDType.ConnID), true));
//        if (DNID.length > 0) {
//            UserEventsByDN(DNID, cidFinder);
//        }
        if (cidFinder != null) {
//            Integer[] recHandles = cidFinder.getIDs(IDType.OCSRecordHandle);
//            if (recHandles != null) {
//                Wheres wh = new Wheres();
//                wh.addWhere("recHandle<=0");
//                wh.addWhere(getWhere("recHandle", recHandles, false), "or");
//                OCSUEvents.addWhere(wh, "AND");
//            }
//
//            Integer[] ConnIDs = cidFinder.getIDs(IDType.ConnID);
//            if (ConnIDs != null) {
//                Wheres wh = new Wheres();
//                wh.addWhere("ConnectionIDID<=0");
//                wh.addWhere(getWhere("ConnectionIDID", ConnIDs, false), "or");
//                OCSUEvents.addWhere(wh, "AND");
//            }

            Wheres wh = new Wheres();

            Integer[] ids;
            if ((ids = cidFinder.getIDs(IDType.OCSRecordHandle)) != null) {
                wh.addWhere(getWhere("recHandle", ids, false), "OR");
            } else if ((ids = cidFinder.getIDs(IDType.DN)) != null) {
                wh.addWhere(getWhere("thisDNID", ids, false), "");
            }

            if (!wh.isEmpty()) {
                OCSUEvents.addWhere(wh);
            }
        }

        OCSUEvents.addWhere(" nameID in (select id from " + ReferenceType.TEvent + " where name in ('EventUserEvent', 'RequestDistributeUserEvent'))", "AND");

        OCSUEvents.addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        OCSUEvents.addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        OCSUEvents.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.MANDATORY);
        OCSUEvents.addRef("sourceID", "source", ReferenceType.App.toString(), FieldType.OPTIONAL);
        OCSUEvents.addNullField("ixnid");
        OCSUEvents.addNullField("typestr");
        OCSUEvents.addNullField("err");
        OCSUEvents.addNullField("errmessage");
        OCSUEvents.addOutField("intToHex( seqno) seqnoHex"); // bug in SQLite lib; does not accept full word

        OCSUEvents.setCommonParams(this, dlg);

        getRecords(OCSUEvents);

    }

    private void UserEventsByDN(Integer[] DNID, IDsFinder cidFinder, QueryDialog dlg) throws SQLException {
        String handleList = "";
        tellProgress("User events by DN");
        if (cidFinder != null) {
            Integer[] recHandles = cidFinder.getIDs(IDType.OCSRecordHandle);
            if (recHandles != null) {
                handleList = "and ( recHandle<0 or " + getWhere("recHandle", recHandles, false) + " )";
            }
        }
        String DNs = getWhere("thisDNID", DNID, false);
        if (DNs != null && DNs.length() > 0) {
            DNs += " and ";
        }

        TableQuery OCSUEvents = new TableQuery(MsgType.OCS, "ocs_logbr", "agentNameID", DNs
                //                    + " AND ( ConnectionIDID is null OR ConnectionIDID <= 0)"//to avoid duplicate records
                + " nameID in (select id from " + ReferenceType.TEvent + " where name in ('EventUserEvent', 'RequestDistributeUserEvent'))"
                + handleList
        );
        OCSUEvents.addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        OCSUEvents.addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        OCSUEvents.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.MANDATORY);
        OCSUEvents.addRef("sourceID", "source", ReferenceType.App.toString(), FieldType.OPTIONAL);
        OCSUEvents.setCommonParams(this, dlg);
        getRecords(OCSUEvents);
    }

    //    private void UserEventsByConnIDs(Integer[] ids, SearchIDs oIDs) throws SQLException {
//        Integer[] DNID = DatabaseConnector.getDatabaseConnector(this).getIDs(this, "ocs_logbr", "thisDNID", getWhere("ConnectionIDID", ids, true));
//        if (DNID.length > 0) {
//            UserEventsByDN(DNID, oIDs);
//        }
//    }
//
//    private void UserEventsByDN(Integer[] DNID, SearchIDs oIDs) throws SQLException {
//        String handleList = "";
//        if (oIDs != null) {
//            Integer[] recHandles = oIDs.getIDs(IDType.OCSRecordHandle);
//            if (recHandles != null) {
//                handleList = "and ( recHandle<0 or " + getWhere("recHandle", recHandles, false) + " )";
//            }
//        }
//
//        TableQuery OCSUEvents = new TableQuery(MsgType.OCS, "ocs_logbr", "agentNameID", getWhere("thisDNID", DNID, false)
//                //                    + " AND ( ConnectionIDID is null OR ConnectionIDID <= 0)"//to avoid duplicate records
//                + " and nameID in (select id from " + ReferenceType.TEvent + " where name in ('EventUserEvent', 'RequestDistributeUserEvent'))"
//                + handleList
//        );
//        OCSUEvents.addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.Optional);
//        OCSUEvents.addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.Optional);
//        OCSUEvents.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Mandatory);
//        OCSUEvents.addRef("sourceID", "source", ReferenceType.App.toString(), FieldType.Optional);
//        OCSUEvents.setCommonParams(this, dlg);
//        getRecords(OCSUEvents);
//    }
    @Override
    public String getName() {
        return "Outbound";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public DynamicTree<OptionNode> getReportItems() {
        return repComponents;
    }

    @Override
    public void Retrieve(QueryDialog dlg) throws SQLException {
        SelectionType selectionType = dlg.getSelectionType();
        cidFinder = null;
        String selection = dlg.getSelection();
        timeRange = dlg.getTimeRange();

        if (dlg.getSearchApps() == null) {
            inquirer.logger.info("No apps selected. Nothing to search in");
            return;
        }

        if (selectionType != SelectionType.NO_SELECTION) {
            tellProgress("Searching");
            cidFinder = new IDsFinder(dlg);
            if (!cidFinder.initOutbound()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            } else {
                setObjectsFound(cidFinder.getObjectsFound());
            }

        } else {
            cidFinder = null;
        }

        RetrieveCall(dlg,
                FindNode(repComponents.getRoot(), DialogItem.OCS, null, null),
                cidFinder);

        if (cidFinder == null || !isCallSearch(cidFinder)) {
            RetrieveCG(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.OCSCG, null, null),
                    cidFinder);
        }

        RetrieveAgent(dlg,
                FindNode(repComponents.getRoot(), DialogItem.OCSAGENT, null, null),
                cidFinder);

        getCustom(FileInfoType.type_StatServer, dlg, repComponents.getRoot(), cidFinder, null);
        getGenesysMessages(TableType.MsgOCServer, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);

        if (inquirer.getCr().isSorting()) {
            Sort();
        }
        LookupMark();
    }

    private void addOutboundCallParams(DynamicTreeNode<OptionNode> rootA) {

        DynamicTreeNode<OptionNode> ocsCAll = new DynamicTreeNode<>(new OptionNode(DialogItem.OCS));
        rootA.addChild(ocsCAll);

        DynamicTreeNode<OptionNode> tEventsNode;
        tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCALL_TEVENT));
        ocsCAll.addChild(tEventsNode);

        tEventsNode.addDynamicRef(DialogItem.OCS_CALL_TEVENT_NAME, ReferenceType.TEvent, "ocs_logbr", "nameid");
        tEventsNode.addDynamicRef(DialogItem.OCS_CALL_TEVENT_DN, ReferenceType.DN, "ocs_logbr", "thisdnid", "otherdnid");
        tEventsNode.addDynamicRef(DialogItem.OCS_CALL_TEVENT_SERVER, ReferenceType.App, "ocs_logbr", "sourceid");

        /**
         * ********************
         */
        DynamicTreeNode<OptionNode> nodeSCXML;
        nodeSCXML = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCALL_SCXML));
        ocsCAll.addChild(nodeSCXML);

        nodeSCXML.addDynamicRef("Metric", ReferenceType.METRIC);
        nodeSCXML.addDynamicRef("Method", ReferenceType.ORSMETHOD);

        /**
         * ********************
         */
        DynamicTreeNode<OptionNode> nodeDB;
        nodeDB = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCALL_DATABASE));
        ocsCAll.addChild(nodeDB);

        nodeDB.addDynamicRef(DialogItem.OCS_DATABASE_REQUESTTYPE, ReferenceType.DBRequest);
        nodeDB.addDynamicRef(DialogItem.OCS_DATABASE_DBSERVER, ReferenceType.App, "ocsdb_logbr", "DBSERVERID", true);

        /**
         * ********************
         */
        DynamicTreeNode<OptionNode> nodeIxn;
        nodeIxn = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCALL_IXNSERVER));
        ocsCAll.addChild(nodeIxn);

        DynamicTreeNode<OptionNode> nodePAEVI;
        nodePAEVI = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCALL_PAEVI));
        ocsCAll.addChild(nodePAEVI);
//        nodePAEVI.addDynamicRef(DialogItem.OCSCALL_PAEVI_CALLRESULT, null, "ocspaevi_logbr", "DBSERVERID", true);

        DynamicTreeNode<OptionNode> nodeRECCR;
        nodeRECCR = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCALL_RECCR));
        ocsCAll.addChild(nodeRECCR);

        DynamicTreeNode<OptionNode> nodeHTTP;
        nodeHTTP = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCALL_HTTP));
        ocsCAll.addChild(nodeHTTP);
        nodeHTTP.addDynamicRef(DialogItem.OCSCALL_HTTP_REQUEST, ReferenceType.TEvent, "ocshttp", "ReqNameID", true);

        nodeHTTP = new DynamicTreeNode<>(new OptionNode(false, DialogItem.OCSCALL_DESKTOP));
        ocsCAll.addChild(nodeHTTP);

        DynamicTreeNode<OptionNode> nodeTreatment;
        nodeTreatment = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCALL_TREATMENT));
        ocsCAll.addChild(nodeTreatment);

        DynamicTreeNode<OptionNode> nodeOCSIcon;
        nodeOCSIcon = new DynamicTreeNode<>(new OptionNode(false, DialogItem.OCSCALL_OCSICON));
        ocsCAll.addChild(nodeOCSIcon);
        nodeOCSIcon.addDynamicRef(DialogItem.OCSCALL_OCSICON_EVENT, ReferenceType.OCSIconEvent, "OCSIcon", "eventID");
        nodeOCSIcon.addDynamicRef(DialogItem.OCSCALL_OCSICON_CAUSE, ReferenceType.OCSIconCause, "OCSIcon", "causeID");
    }

    private void addOutboundCampaignParams(DynamicTreeNode<OptionNode> rootA) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCG));
        rootA.addChild(nd);

        DynamicTreeNode<OptionNode> AttrValue;
        DynamicTreeNode<OptionNode> Attr;

        DynamicTreeNode<OptionNode> ocsClient;
        ocsClient = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCG_CLIENT));
        nd.addChild(ocsClient);
        ocsClient.addDynamicRef(DialogItem.OCSCG_CLIENT_REQUEST, ReferenceType.OCSClientRequest, "OCSClient", "nameid", true);

        Attr = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCG_CAMPAIGNGROUP));
        nd.addChild(Attr);
        Attr.addDynamicRef(DialogItem.OCSCG_CAMPAIGNGROUP_CGNAME, ReferenceType.OCSCG);
        Attr.addDynamicRef(DialogItem.OCSCG_CAMPAIGNGROUP_CAMPAIGN, ReferenceType.OCSCAMPAIGN);

        Attr = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCG_PASESSIONINFO));
        nd.addChild(Attr);

        Attr = new DynamicTreeNode<>(new OptionNode(false, DialogItem.OCSCG_PAEVENTINFO));
        nd.addChild(Attr);
        Attr.addDynamicRef(DialogItem.OCSCG_PAEVENTINFO_STATTYPE, ReferenceType.StatType, "OCSPAEVI_logbr", "AgentStatTypeID", true);

        DynamicTreeNode<OptionNode> nodeAgentAssignment;
        nodeAgentAssignment = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSCG_AGENT_ASSIGNMENT));
        nd.addChild(nodeAgentAssignment);

    }

    private void addOutboundAgentParams(DynamicTreeNode<OptionNode> rootA) {
        DynamicTreeNode<OptionNode> agentRootNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSAGENT));
        rootA.addChild(agentRootNode);

        DynamicTreeNode<OptionNode> node;
        DynamicTreeNode<OptionNode> subNode;

        node = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSAGENT_PAAI));
        agentRootNode.addChild(node);
        node.addDynamicRef(DialogItem.OCSAGENT_PAAI_AGENT, ReferenceType.Agent, "ocspaagi_logbr", "agentNameID");
        node.addDynamicRef(DialogItem.OCSAGENT_PAAI_STATTYPE, ReferenceType.AgentStatType, "ocspaagi_logbr", "AgentStatTypeID");
        node.addDynamicRef(DialogItem.OCSAGENT_PAAI_CALLTYPE, ReferenceType.AgentCallType, "ocspaagi_logbr", "AgentCallTypeID");

        node = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSAGENT_STAT));
        agentRootNode.addChild(node);
        node.addDynamicRef(DialogItem.OCSAGENT_STAT_STAT, ReferenceType.StatEvent, "OcsStatEv_logbr", "StatEvNameID");
        node.addDynamicRef(DialogItem.OCSAGENT_STAT_AGENT, ReferenceType.Agent, "OcsStatEv_logbr", "agentNameID");
        node.addDynamicRef(DialogItem.OCSAGENT_STAT_PLACE, ReferenceType.Place, "OcsStatEv_logbr", "placeNameID");
        node.addDynamicRef(DialogItem.OCSAGENT_STAT_DN, ReferenceType.DN, "OcsStatEv_logbr", "DNID");
        node.addDynamicRef(DialogItem.OCSAGENT_STAT_STATTYPE, ReferenceType.StatType, "OcsStatEv_logbr", "StatTypeID");

        node = new DynamicTreeNode<>(new OptionNode(true, DialogItem.OCSAGENT_TEVENTS));
        agentRootNode.addChild(node);

//        node = new DynamicTreeNode<OptionNode>(new OptionNode(true, DialogItem.OCSAGENT_USEREVENTS));
//        agentRootNode.addChild(node);
//        tEventsNode.addDynamicRef(DialogItem.OCS_CALL_TEVENT_NAME, ReferenceType.TEvent, "ocs_logbr", "nameid");
//        tEventsNode.addDynamicRef(DialogItem.OCS_CALL_TEVENT_DN, ReferenceType.DN, "ocs_logbr", "thisdnid", "otherdnid");
//        tEventsNode.addDynamicRef(DialogItem.OCS_CALL_TEVENT_SERVER, ReferenceType.App, "ocs_logbr", "sourceid");
    }

    private void retrieveNoSelection(QueryDialog dlg) throws SQLException {
        DynamicTreeNode<OptionNode> OCSCallSettings = getChildByName(repComponents.getRoot(), DialogItem.OCS);
        if (OCSCallSettings != null && ((OptionNode) OCSCallSettings.getData()).isChecked()) {
//            QueryCampaignGroup(null, dlg, OCSCallSettings);
        }
        DynamicTreeNode<OptionNode> OCSCGSettings = getChildByName(repComponents.getRoot(), DialogItem.OCSCG);
        if (OCSCGSettings != null && ((OptionNode) OCSCGSettings.getData()).isChecked()) {
            QueryCampaignGroup(null, dlg, OCSCGSettings);
        }

        DynamicTreeNode<OptionNode> OCSAgentSettings = getChildByName(repComponents.getRoot(), DialogItem.OCSAGENT);
        if (OCSAgentSettings != null && ((OptionNode) OCSAgentSettings.getData()).isChecked()) {
//            QueryCampaignGroup(null, dlg, OCSCGSettings);
        }

    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        if (appType == null) {
            appType = getAppsType(FileInfoType.type_OCS);
        }
        return appType;
    }

    private void RetrieveCall(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        Integer[] recHandles = null;
        Integer[] chID = null;
        TableQuery OCSIXN = null;
        Integer[] connIDs = null;
        DynamicTreeNode<OptionNode> node;

        if (isChecked(reportSettings)) {
            if (cidFinder == null || (cidFinder.getSearchType() != SelectionType.NO_SELECTION
                    && !cidFinder.outboundCallFound())) {
                int dialogResult = inquirer.showConfirmDialog(dlg, "No call found. "
                        + "Do you want to continue retrieving all calls", "Warning", JOptionPane.OK_CANCEL_OPTION);
                if (dialogResult == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
            if (cidFinder != null) {
                recHandles = cidFinder.getIDs(IDType.OCSRecordHandle);
                connIDs = cidFinder.getIDs(IDType.ConnID);
            }

            node = FindNode(reportSettings, DialogItem.OCSCALL_DATABASE, null, null);
            if (isChecked(node)) {
                tellProgress("Retrieving database requests");
                OCSIXN = new TableQuery(MsgType.OCSDB, "ocsdb_logbr");
                if (cidFinder != null) {
                    if ((chID = cidFinder.getIDs(IDType.OCSChainID)) != null) {
//                    OCSDB = new TableQuery(MsgType.OCSDB, "ocsdb_logbr", "chDBID", getWhere("chDBID", chID, false));
                        OCSIXN.addWhere("( "
                                + getWhere("chDBID", chID, false)
                                + " or reqid in (select reqid from ocsdb_logbr where "
                                + getWhere("chDBID", chID, false) + "))", "AND");
                    }
                    if ((chID = cidFinder.getIDs(IDType.OCSCampaignDBID)) != null) {
//                    OCSDB = new TableQuery(MsgType.OCSDB, "ocsdb_logbr", "chDBID", getWhere("chDBID", chID, false));
                        OCSIXN.addWhere(getWhere("campaignDBID", chID, false), "AND");
                    }

                }
                OCSIXN.AddCheckedWhere(OCSIXN.getTabAlias() + ".dbreqid",
                        ReferenceType.DBRequest,
                        node,
                        "AND",
                        DialogItem.OCS_DATABASE_REQUESTTYPE);
                OCSIXN.AddCheckedWhere(OCSIXN.getTabAlias() + ".dbserverid",
                        ReferenceType.App,
                        node,
                        "AND",
                        DialogItem.OCS_DATABASE_DBSERVER);
                OCSIXN.addRef("dbserverid", "dbserver", ReferenceType.App.toString(), FieldType.OPTIONAL);
                OCSIXN.addRef("dbreqid", "dbreq", ReferenceType.DBRequest.toString(), FieldType.OPTIONAL);

                OCSIXN.addOutField(DatabaseConnector.CampaignDBIDtoName + "( CampaignDBID) campaignName");
                OCSIXN.addOutField(DatabaseConnector.GroupDBIDtoName + "( GroupDBID) groupName");
                OCSIXN.addOutField(DatabaseConnector.ListDBIDtoName + "( listDBID) listName");

                OCSIXN.setCommonParams(this, dlg);

                getRecords(OCSIXN);
            }
            node = FindNode(reportSettings, DialogItem.OCSCALL_TEVENT, null, null);
            if (isChecked(node)) {
//<editor-fold defaultstate="collapsed" desc="TLib events">
                tellProgress("Retrieving TLib events");
                HashSet<Long> refIDs = new HashSet<>();
                ArrayList<Long> ids = new ArrayList<>();
                HashSet<Long> idDNs = new HashSet<>();

                OCSByConnIdQuery ocsq = new OCSByConnIdQuery();
                ocsq.setRecLoadedProc((ILogRecord rec) -> {
                    if (rec instanceof TLibEvent) {
                        addUnique(refIDs, ((TLibEvent) rec).GetReferenceId());

                        long id = rec.getID();
                        if (id > 0) {
                            ids.add(id);
                        }
                        addUnique(idDNs, ((TLibEvent) rec).getThisDNID(), ((TLibEvent) rec).getOtherDNID());

                    }

                });

//                ocsq.setSearchSettings(reportSettings);
                ocsq.setSearchSettings(node);
                ocsq.setIDFinder(cidFinder);
//                ocsq.setSearchSettings(FindNode(reportSettings, DialogItem.OCSCALL_TEVENT, DialogItem.OCS_CALL_TEVENT_NAME, null));

                ocsq.setCommonParams(this, dlg);
                getRecords(ocsq);
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="TLib requests">
                if (cidFinder != null && callRelatedSearch(cidFinder) && !refIDs.isEmpty()) {
                    ocsq = new OCSByConnIdQuery(refIDs, ids, idDNs);

                    ocsq.setSearchSettings(node);
                    ocsq.setIDFinder(cidFinder);

                    ocsq.setCommonParams(this, dlg);
                    getRecords(ocsq);
                }
//</editor-fold>

            }

            if (isChecked(FindNode(reportSettings, DialogItem.OCSCALL_DESKTOP, null, null))) {
                UserEventsByConnIDs(dlg, cidFinder);
            }

            if (isChecked(FindNode(reportSettings, DialogItem.OCSCALL_PAEVI, null, null))) {
                tellProgress("retrieving PAEventInfo");
                TableQuery OCSPAEVI = new TableQuery(MsgType.OCSPAEVI, "ocspaevi_logbr");
                if (cidFinder != null) {
                    Wheres wh = new Wheres();

                    if ((recHandles = cidFinder.getIDs(IDType.OCSRecordHandle)) != null) {
                        wh.addWhere(getWhere("recHandle", recHandles, false), "OR");
                    } else if ((chID = cidFinder.getIDs(IDType.CGDBID)) != null) {
                        wh.addWhere(getWhere("cgdbid", chID, false), "OR");
                    }
                    if (!wh.isEmpty()) {
                        OCSPAEVI.addWhere(wh);
                    }

                }

                OCSPAEVI.addOutField("constToStr(\"CallResult \", callresult) callresultstr");
                OCSPAEVI.addOutField("CampaignGroupDBIDtoName( cgdbid) campaignGroup");

                OCSPAEVI.setCommonParams(this, dlg);
                getRecords(OCSPAEVI);
            }

            if (isChecked(FindNode(reportSettings, DialogItem.OCSCALL_TREATMENT, null, null))
                    && DatabaseConnector.TableExist("ocstreat")) {
                tellProgress("Retrieving treatments");
                TableQuery OCSTreatment = new TableQuery(MsgType.OCSTREATMENT, "ocstreat");
                if ((cidFinder != null)) {
                    Wheres wh = new Wheres();
                    if ((recHandles = cidFinder.getIDs(IDType.OCSRecordHandle)) != null) {
                        wh.addWhere(getWhere("recHandle", recHandles, false), "OR");
                    }

                    if ((chID = cidFinder.getIDs(IDType.OCSChainID)) != null) {
                        wh.addWhere(getWhere("chid", chID, false), "OR");
                    }
                    if (!wh.isEmpty()) {
                        OCSTreatment.addWhere(wh);
                    }
                }
                OCSTreatment.setCommonParams(this, dlg);
                getRecords(OCSTreatment);
            }

            if (isChecked(FindNode(reportSettings, DialogItem.OCSCALL_RECCR, null, null))
                    && DatabaseConnector.TableExist("ocsreccr")) {
                tellProgress("Retrieving record creation");
                TableQuery OCSCR = new TableQuery(MsgType.OCSRECCREATE, "ocsreccr");
                if (cidFinder != null) {
                    if ((recHandles = cidFinder.getIDs(IDType.OCSRecordHandle)) != null) {
                        OCSCR.addWhere(getWhere("recHandle", recHandles, false), "AND");
                    } else if ((chID = cidFinder.getIDs(IDType.OCSCampaign)) != null) {
//                    OCSDB = new TableQuery(MsgType.OCSDB, "ocsdb_logbr", "chDBID", getWhere("chDBID", chID, false));
                        OCSCR.addWhere(getWhere("campBID", chID, false), "AND");
                    }

                }
                OCSCR.addOutField("constToStr(\"RecordType \", rectype) typestr"); // bug in SQLite lib; does not accept full word
                OCSCR.addRef("campBID", "campaign", ReferenceType.OCSCAMPAIGN.toString(), FieldType.OPTIONAL);
                OCSCR.addRef("listNameID", "ocsList", ReferenceType.OCSCallList.toString(), FieldType.OPTIONAL);
                OCSCR.addRef("tableNameID", "ocsTable", ReferenceType.OCSCallListTable.toString(), FieldType.OPTIONAL);
                OCSCR.addRef("actionID", "action", ReferenceType.OCSRecordAction.toString(), FieldType.OPTIONAL);

                OCSCR.setCommonParams(this, dlg);
                getRecords(OCSCR);
            }

            if (isChecked(FindNode(reportSettings, DialogItem.OCSCALL_HTTP, null, null))
                    && DatabaseConnector.TableExist("ocshttp")) {
                tellProgress("HTTP requests");
                TableQuery OCSHTTP;
                if (cidFinder != null && (recHandles = cidFinder.getIDs(IDType.OCSRecordHandle)) != null) {
                    OCSHTTP = new TableQuery(MsgType.OCSHTTP, "ocshttp", "recHandle", getWhere("recHandle", recHandles, false));
                } else {
                    OCSHTTP = new TableQuery(MsgType.OCSHTTP, "ocshttp");
                }
                OCSHTTP.addRef("ReqNameID", "reqname", ReferenceType.TEvent.toString(), FieldType.OPTIONAL);

                OCSHTTP.AddCheckedWhere(OCSHTTP.getTabAlias() + ".ReqNameID",
                        ReferenceType.TEvent,
                        FindNode(reportSettings, DialogItem.OCSCALL_HTTP, null, null),
                        "AND",
                        DialogItem.OCSCALL_HTTP_REQUEST);

                OCSHTTP.setCommonParams(this, dlg);
                getRecords(OCSHTTP);
            }

            if (isChecked(FindNode(reportSettings, DialogItem.OCSCALL_SCXML, null, null)) && DatabaseConnector.TableExist("ocssxmlscript")
                    && DatabaseConnector.TableExist("ocsscxmltr")) {
                Integer[] OCSSID;
                TableQuery OCSSCXMLScript;
                TableQuery OCSSCXMLTr;
                tellProgress("SCXML session");

                if (cidFinder != null && (OCSSID = cidFinder.getIDs(IDType.OCSSID)) != null) {
                    OCSSCXMLScript = new TableQuery(MsgType.OCSSCXMLSCRIPT, "ocssxmlscript", "SessIDID", getWhere("SessIDID", OCSSID, false));
                    OCSSCXMLTr = new TableQuery(MsgType.OCSSCXMLTREATMENT, "ocsscxmltr", "recHandle", getWhere("recHandle", recHandles, false));
                } else {
                    OCSSCXMLScript = new TableQuery(MsgType.OCSSCXMLSCRIPT, "ocssxmlscript");
                    OCSSCXMLTr = new TableQuery(MsgType.OCSSCXMLTREATMENT, "ocsscxmltr", "recHandle", getWhere("recHandle", recHandles, false));
                }
                OCSSCXMLScript.addRef("SessIDID", "SessID", ReferenceType.OCSSCXMLSESSION.toString(), FieldType.MANDATORY);
                OCSSCXMLScript.addRef("metricid", "metric", ReferenceType.METRIC.toString(), FieldType.OPTIONAL);
                OCSSCXMLScript.setCommonParams(this, dlg);
                getRecords(OCSSCXMLScript);

                OCSSCXMLTr.addRef("SessIDID", "SessID", ReferenceType.OCSSCXMLSESSION.toString(), FieldType.OPTIONAL);
                OCSSCXMLTr.setCommonParams(this, dlg);
                getRecords(OCSSCXMLTr);
            }
            if (isChecked(FindNode(reportSettings, DialogItem.OCSCALL_IXNSERVER, null, null)) && DatabaseConnector.TableExist("ocsixn")) {
                tellProgress("Interaction Server");
                OCSIXN = new TableQuery(MsgType.OCSINTERACTION, "ocsixn");
                if (cidFinder != null) {
                    Wheres wh = new Wheres();
                    if ((chID = cidFinder.getIDs(IDType.OCSChainID)) != null) {
                        wh.addWhere(getWhere("chainid", chID, false), "or");
                    }
                    if ((chID = cidFinder.getIDs(IDType.IxnID)) != null) {
                        wh.addWhere(getWhere("ixnid", chID, false), "or");
                    }
                    if ((chID = cidFinder.getIDs(IDType.OCSRecordHandle)) != null) {
                        wh.addWhere(getWhere("recHandle", chID, false), "or");
                    }
//                    OCSDB = new TableQuery(MsgType.OCSDB, "ocsdb_logbr", "chDBID", getWhere("chDBID", chID, false));
                    if (!wh.isEmpty()) {
                        OCSIXN.addWhere(wh);
                    }
                }
                OCSIXN.addRef("nameid", "name", ReferenceType.TEvent.toString(), FieldType.OPTIONAL);
                OCSIXN.addRef("ixnid", "InteractionID", ReferenceType.IxnID.toString(), FieldType.OPTIONAL);
                OCSIXN.addRef("ixnqueueid", "queue", ReferenceType.DN.toString(), FieldType.OPTIONAL);
                OCSIXN.addRef("phoneid", "phone", ReferenceType.DN.toString(), FieldType.OPTIONAL);
                OCSIXN.setCommonParams(this, dlg);

                getRecords(OCSIXN);
            }

            if (isChecked(FindNode(reportSettings, DialogItem.OCSCALL_OCSICON, null, null))
                    && DatabaseConnector.TableExist("OCSIcon")) {
                tellProgress("OCS to ICON messages");
                TableQuery OCSIcon;
                Integer[] ids = null;

                OCSIcon = new TableQuery(MsgType.OCSICON, "OCSIcon");

                if (cidFinder != null) {
                    if ((ids = cidFinder.getIDs(IDType.OCSChainID)) != null) {
                        OCSIcon.addWhere(getWhere("chID", ids, false), "");
                    }
                    if ((ids = cidFinder.getIDs(IDType.OCSCampaignDBID)) != null) {
                        OCSIcon.addWhere(getWhere("campaignDBID", ids, false), "AND");
                    }
                }

                OCSIcon.addRef("eventID", "event", ReferenceType.OCSIconEvent.toString(), FieldType.OPTIONAL);
                OCSIcon.addRef("causeID", "cause", ReferenceType.OCSIconCause.toString(), FieldType.OPTIONAL);

                OCSIcon.AddCheckedWhere(OCSIcon.getTabAlias() + ".eventID",
                        ReferenceType.OCSIconEvent,
                        FindNode(reportSettings, DialogItem.OCSCALL_OCSICON, null, null),
                        "AND",
                        DialogItem.OCSCALL_OCSICON_EVENT);
                OCSIcon.AddCheckedWhere(OCSIcon.getTabAlias() + ".causeID",
                        ReferenceType.OCSIconCause,
                        FindNode(reportSettings, DialogItem.OCSCALL_OCSICON, null, null),
                        "AND",
                        DialogItem.OCSCALL_OCSICON_CAUSE);

                OCSIcon.addOutField(DatabaseConnector.CampaignDBIDtoName + "(CampaignDBID) campaignName");
                OCSIcon.addOutField(DatabaseConnector.GroupDBIDtoName + "( GroupDBID) groupName");
                OCSIcon.addOutField(DatabaseConnector.ListDBIDtoName + "( listDBID) listName");

                OCSIcon.setCommonParams(this, dlg);
                getRecords(OCSIcon);

            }
        }
    }

    private void RetrieveCG(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        boolean sendRequest = true;
        DynamicTreeNode<OptionNode> node = null;
        ArrayList<Integer> searchApps = dlg.getSearchApps();
        Integer[] iDs = (cidFinder != null) ? cidFinder.getIDs(IDType.CGDBID) : null;

        if (isChecked(reportSettings)) {
            if ((cidFinder == null
                    && allChildrenChecked(FindNode(reportSettings, DialogItem.OCSCG_CAMPAIGNGROUP, DialogItem.OCSCG_CAMPAIGNGROUP_CGNAME, null)))
                    || (cidFinder != null && cidFinder.getSearchType() != SelectionType.NO_SELECTION
                    && (iDs == null || iDs.length == 0))) {
                int dialogResult = inquirer.showConfirmDialog(dlg, "No campaign found. "
                        + "Do you want to continue retrieving all campaigns", "Retrieve campaign group", JOptionPane.YES_NO_CANCEL_OPTION);
                if (dialogResult == JOptionPane.CANCEL_OPTION || dialogResult == JOptionPane.NO_OPTION) {
                    return;
                }
            }

//<editor-fold defaultstate="collapsed" desc="OCSCG_PASESSIONINFO">
            if (isChecked(FindNode(reportSettings, DialogItem.OCSCG_PASESSIONINFO, null, null)) && DatabaseConnector.TableExist("ocspasi_logbr")) {
                tellProgress("PASessionInfo");
                TableQuery OCSPASI = new TableQuery(MsgType.OCSPASI, "ocspasi_logbr");

                if (cidFinder != null) {
                    Wheres wh = new Wheres();
                    Integer[] ids;

                    if ((ids = cidFinder.getIDs(IDType.CGDBID)) != null) {
                        wh.addWhere(getWhere("cgDBID", ids, false), "OR");
                    }
                    if ((ids = cidFinder.getIDs(IDType.CG)) != null) {
                        wh.addWhere(getWhere("cgnameid", ids, false), "OR");
                    }
                    if ((ids = cidFinder.getIDs(IDType.OCSCampaignDBID)) != null) {
                        wh.addWhere(getWhere("campaignDBID", ids, false), "OR");
                    }

                    if (!wh.isEmpty()) {
                        OCSPASI.addWhere(wh);
                    }

                } else {
                    OCSPASI.AddCheckedWhere(OCSPASI.getTabAlias() + ".cgNameID",
                            ReferenceType.OCSCG,
                            FindNode(reportSettings, DialogItem.OCSCG_CAMPAIGNGROUP, null, null),
                            "AND",
                            DialogItem.OCSCG_CAMPAIGNGROUP_CGNAME);
                }

                OCSPASI.addRef("cgnameID", "cgname", ReferenceType.OCSCG.toString(), FieldType.OPTIONAL);
                OCSPASI.setCommonParams(this, dlg);
                getRecords(OCSPASI);

                //********************
                if (DatabaseConnector.TableExist(TableType.OCSPredInfo.toString())) {
                    OCSPASI = new TableQuery(MsgType.OCSPREDI, TableType.OCSPredInfo.toString());

                    if (cidFinder != null) {
                        Wheres wh = new Wheres();
                        Integer[] ids;

                        if ((ids = cidFinder.getIDs(IDType.CGDBID)) != null) {
                            wh.addWhere(getWhere("OCSSessionID", ids, false), "");
                        }

                        if (!wh.isEmpty()) {
                            OCSPASI.addWhere(wh);
                        }

                    }

                    OCSPASI.addOutField("CampaignGroupDBIDtoName( OCSSessionID) campaignGroup");
                    OCSPASI.setCommonParams(this, dlg);
                    getRecords(OCSPASI);
                }
            }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="OCSCG_CAMPAIGNGROUP">
            sendRequest = true;
            if (reportSettings != null) {
                node = getChildByName(reportSettings, DialogItem.OCSCG_CAMPAIGNGROUP);
                if (node != null && !((OptionNode) node.getData()).isChecked()) {
                    sendRequest = false;
                }
            }
            if (sendRequest) {
                tellProgress("CampaignGroup messages");
                TableQuery OCSCG = new TableQuery(MsgType.OCSCG, "ocscg_logbr");

                if (cidFinder != null) {
                    Wheres wh = new Wheres();
                    Integer[] ids;

                    if ((ids = cidFinder.getIDs(IDType.CGDBID)) != null) {
                        wh.addWhere(getWhere("cgDBID", ids, false), "OR");
                    }
                    if ((ids = cidFinder.getIDs(IDType.CG)) != null) {
                        wh.addWhere(getWhere("cgnameid", ids, false), "OR");
                    }
                    if ((ids = cidFinder.getIDs(IDType.OCSCampaignDBID)) != null) {
                        wh.addWhere(getWhere("campaignDBID", ids, false), "OR");
                    }

                    if (!wh.isEmpty()) {
                        OCSCG.addWhere(wh);
                    }

                } else {
                    OCSCG.AddCheckedWhere(OCSCG.getTabAlias() + ".cgNameID",
                            ReferenceType.OCSCG,
                            FindNode(reportSettings, DialogItem.OCSCG_CAMPAIGNGROUP, null, null),
                            "AND",
                            DialogItem.OCSCG_CAMPAIGNGROUP_CGNAME);
                }

                OCSCG.addRef("groupnameID", "groupName", ReferenceType.AgentGroup.toString(), FieldType.OPTIONAL);
                OCSCG.addRef("campaignnameID", "campaignName", ReferenceType.OCSCAMPAIGN.toString(), FieldType.OPTIONAL);
                OCSCG.addRef("cgnameID", "cgname", ReferenceType.OCSCG.toString(), FieldType.OPTIONAL);
                OCSCG.setCommonParams(this, dlg);
                getRecords(OCSCG);
            }
//</editor-fold>        

//<editor-fold defaultstate="collapsed" desc="OCSCG_AGENT_ASSIGNMENT">
            sendRequest = true;
            if (reportSettings != null) {
                node = getChildByName(reportSettings, DialogItem.OCSCG_AGENT_ASSIGNMENT);
                if (node != null && !((OptionNode) node.getData()).isChecked()) {
                    sendRequest = false;
                }
            }
            if (sendRequest && DatabaseConnector.TableExist("ocsasgn_logbr")) {
                tellProgress("Agent assignment");
                TableQuery OCSAS = new TableQuery(MsgType.OCSASSIGNMENT, "ocsasgn_logbr");

                if (cidFinder != null) {
                    Wheres wh = new Wheres();
                    Integer[] ids;

                    if ((ids = cidFinder.getIDs(IDType.CG)) != null) {
                        wh.addWhere(getWhere("cgnameid", ids, false), "OR");
                    }

                    if (!wh.isEmpty()) {
                        OCSAS.addWhere(wh);
                    }

                } else {
                    OCSAS.AddCheckedWhere(OCSAS.getTabAlias() + ".cgNameID",
                            ReferenceType.OCSCG,
                            FindNode(reportSettings, DialogItem.OCSCG_CAMPAIGNGROUP, null, null),
                            "AND",
                            DialogItem.OCSCG_CAMPAIGNGROUP_CGNAME);
                }

                OCSAS.addRef("cgnameID", "cgname", ReferenceType.OCSCG.toString(), FieldType.OPTIONAL);

                OCSAS.setCommonParams(this, dlg);
                getRecords(OCSAS);
            }
//</editor-fold>            

//<editor-fold defaultstate="collapsed" desc="OCSCG_CLIENT">
            sendRequest = true;
            if (reportSettings != null) {
                node = getChildByName(reportSettings, DialogItem.OCSCG_CLIENT);
                if (node != null && !((OptionNode) node.getData()).isChecked()) {
                    sendRequest = false;
                }
            }
            if (sendRequest) {
                tellProgress("OCS client records");
                if (iDs != null && iDs.length > 0) {
                    getClient(iDs, node, dlg);
                } else {
                    getClient(null, node, dlg);
                }
            }
//</editor-fold>            

//<editor-fold defaultstate="collapsed" desc="OCS_CG_PA_AGENT_INFO">
            sendRequest = true;
            if (reportSettings != null) {
                node = getChildByName(reportSettings, DialogItem.OCS_CG_PA_AGENT_INFO);
                if (node != null && !((OptionNode) node.getData()).isChecked()) {
                    sendRequest = false;
                }
            }
//</editor-fold>            

//<editor-fold defaultstate="collapsed" desc="OCSCG_PAEVENTINFO">
            sendRequest = true;
            if (reportSettings != null) {
                node = getChildByName(reportSettings, DialogItem.OCSCG_PAEVENTINFO);
                if (node != null && !((OptionNode) node.getData()).isChecked()) {
                    sendRequest = false;
                }
            }
            if (sendRequest) {
                tellProgress("PAEventInfo");
                TableQuery OCSPAEVI = null;
                if (iDs != null && iDs.length > 0) {
                    OCSPAEVI = new TableQuery(MsgType.OCSPAEVI, "ocspaevi_logbr", "cgDBID", getWhere("cgDBID", iDs, false));
                } else {
                    OCSPAEVI = new TableQuery(MsgType.OCSPAEVI, "ocspaevi_logbr", "cgDBID");
                }
                OCSPAEVI.AddCheckedWhere(OCSPAEVI.getTabAlias() + ".AgentStatTypeID", ReferenceType.StatType, node, "AND", DialogItem.OCSCG_PAEVENTINFO_STATTYPE);
                OCSPAEVI.addOutField("constToStr(\"CallResult \", callresult) callresultstr");
                OCSPAEVI.addOutField("CampaignGroupDBIDtoName( cgdbid) campaignGroup");
                OCSPAEVI.setCommonParams(this, dlg);
                getRecords(OCSPAEVI);
            }
//</editor-fold>            

        }
    }

    private void RetrieveAgent(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(reportSettings)) {
            Integer[] agID = (cidFinder != null) ? cidFinder.getIDs(IDType.AGENT) : null;
            int dialogResult = JOptionPane.YES_OPTION;

            if (cidFinder != null && (cidFinder.getSearchType() != SelectionType.NO_SELECTION
                    && (agID == null || agID.length == 0))) {
                dialogResult = inquirer.showConfirmDialog(dlg, "No agent found. "
                        + "Do you want to continue retrieving all agents data", "Retrieve agent", JOptionPane.YES_NO_OPTION);
                if (dialogResult == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            if (isChecked(FindNode(reportSettings, DialogItem.OCSAGENT_TEVENTS, null, null))) {
                UserEventsByDN((cidFinder != null) ? cidFinder.getIDs(IDType.DN) : null, cidFinder, dlg);
            }

//            Integer[] connIds = DatabaseConnector.getDatabaseConnector(this).getIDs(this, "ocs_logbr", "ConnectionIDID", getWhere("thisDNID", DNID, true));
//            if (connIds.length > 0) {
//                OCSByConnIdQuery ocsq = new OCSByConnIdQuery(connIds);
//                getRecords(ocsq);
//            }
            DynamicTreeNode<OptionNode> FindNode = FindNode(reportSettings, DialogItem.OCSAGENT_PAAI, null, null);
            if (isChecked(FindNode)) {
                tellProgress("OCS PA Agent info");
                TableQuery OCSPAAI = new TableQuery(MsgType.OCSPAAGI, "ocspaagi_logbr", "agentNameID", getWhere("agentNameID", agID, false));
                OCSPAAI.addRef("agentNameID", "agent", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
                OCSPAAI.addRef("AgentStatTypeID", "StatType", ReferenceType.AgentStatType.toString(), FieldType.OPTIONAL);
                OCSPAAI.addRef("AgentCallTypeID", "CallType", ReferenceType.AgentCallType.toString(), FieldType.OPTIONAL);
                OCSPAAI.setCommonParams(this, dlg);
                OCSPAAI.AddCheckedWhere(OCSPAAI.getTabAlias() + ".agentNameID",
                        ReferenceType.Agent,
                        FindNode,
                        "AND",
                        DialogItem.OCSAGENT_PAAI_AGENT);

                OCSPAAI.AddCheckedWhere(OCSPAAI.getTabAlias() + ".AgentStatTypeID",
                        ReferenceType.AgentStatType,
                        FindNode,
                        "AND",
                        DialogItem.OCSAGENT_PAAI_STATTYPE);

                OCSPAAI.AddCheckedWhere(OCSPAAI.getTabAlias() + ".AgentCallTypeID",
                        ReferenceType.AgentCallType,
                        FindNode,
                        "AND",
                        DialogItem.OCSAGENT_PAAI_CALLTYPE);

                Wheres wh = new Wheres();
                Integer[] iDs;

                iDs = (cidFinder != null) ? cidFinder.getIDs(IDType.CGDBID) : null;
                if (iDs != null && iDs.length > 0) {
                    wh.addWhere(getWhere("cgDBID", iDs, false), "AND");
                }

                iDs = (cidFinder != null) ? cidFinder.getIDs(IDType.OCSRecordHandle) : null;
                if (iDs != null && iDs.length > 0) {
                    wh.addWhere(getWhere("recHandle", iDs, false), "AND");
                }
                if (!wh.isEmpty()) {
                    OCSPAAI.addWhere(wh);
                }
                OCSPAAI.setCommonParams(this, dlg);
                getRecords(OCSPAAI);
            }

            FindNode = FindNode(reportSettings, DialogItem.OCSAGENT_STAT, null, null);
            if (isChecked(FindNode)) {
                tellProgress("OCS Stat messages");
                TableQuery OCSSTATEV = new TableQuery(MsgType.OCSSTATEV, "OcsStatEv_logbr", "agentNameID", getWhere("agentNameID", agID, false));
                OCSSTATEV.addRef("StatEvNameID", "stat", ReferenceType.StatEvent.toString(), FieldType.OPTIONAL);
                OCSSTATEV.addRef("agentNameID", "agent", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
                OCSSTATEV.addRef("placeNameID", "place", ReferenceType.Place.toString(), FieldType.OPTIONAL);
                OCSSTATEV.addRef("DNID", "DN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
                OCSSTATEV.addRef("StatTypeID", "StatType", ReferenceType.StatType.toString(), FieldType.OPTIONAL);

                OCSSTATEV.AddCheckedWhere(OCSSTATEV.getTabAlias() + ".StatEvNameID",
                        ReferenceType.StatEvent,
                        FindNode,
                        "AND",
                        DialogItem.OCSAGENT_STAT_STAT);
                OCSSTATEV.AddCheckedWhere(OCSSTATEV.getTabAlias() + ".agentNameID",
                        ReferenceType.Agent,
                        FindNode,
                        "AND",
                        DialogItem.OCSAGENT_STAT_AGENT);
                OCSSTATEV.AddCheckedWhere(OCSSTATEV.getTabAlias() + ".placeNameID",
                        ReferenceType.Place,
                        FindNode,
                        "AND",
                        DialogItem.OCSAGENT_STAT_PLACE);
                OCSSTATEV.AddCheckedWhere(OCSSTATEV.getTabAlias() + ".DNID",
                        ReferenceType.DN,
                        FindNode,
                        "AND",
                        DialogItem.OCSAGENT_STAT_DN);

                OCSSTATEV.AddCheckedWhere(OCSSTATEV.getTabAlias() + ".StatTypeID",
                        ReferenceType.StatType,
                        FindNode,
                        "AND",
                        DialogItem.OCSAGENT_STAT_STATTYPE);

                OCSSTATEV.setCommonParams(this, dlg);
                getRecords(OCSSTATEV);
            }
        }
    }

    @Override
    public String getReportSummary() {
        return ("Outbound\n\t" + ((cidFinder != null)
                ? "search: " + cidFinder
                : "[no filter]"));
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return refreshTimeRange(null);
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new FileInfoType[]{FileInfoType.type_OCS}, searchApps);

    }

    @Override
    FullTableColors getAll(QueryDialog qd) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Retrieve(QueryDialog qd, SelectionType key, String searchID) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    SearchFields getSearchField() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void showAllResults() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected ArrayList<IAggregateQuery> loadReportAggregates() throws SQLException {
        ArrayList<IAggregateQuery> ret = new ArrayList<>();
        ret.add(new AggrOCSDBServerDelays());
        ret.add(new AggrTimestampDelays());
        ret.add(new AggrSIPServerCallsPerSecond(false));

        return ret;
    }

    @Override
    boolean callRelatedSearch(IDsFinder cidFinder) throws SQLException {
        if (cidFinder != null) {
            return cidFinder.AnythingTLibRelated();
        }
        return true;
    }

    private boolean isCallSearch(IDsFinder iDsFinder) {
        switch (iDsFinder.getSearchType()) {
            case CONNID:
            case ChainID:
            case OCSSID:
            case UUID:
                return true;
            case GUESS_SELECTION:
                return (iDsFinder.IDsFound(IDType.ConnID)
                        || iDsFinder.IDsFound(IDType.OCSChainID)
                        || iDsFinder.IDsFound(IDType.OCSRecordHandle)
                        || iDsFinder.IDsFound(IDType.IxnID)
                        || iDsFinder.IDsFound(IDType.OCSSID));

        }
        return false;
    }

}
