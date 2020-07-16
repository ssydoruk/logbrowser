package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.ANDOR;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import static com.myutils.logbrowser.inquirer.QueryTools.getWhere;
import static com.myutils.logbrowser.inquirer.QueryTools.isChecked;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;

public class GMSResults extends IQueryResults {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private final static String GMSWEB_WEBREQID = "webReqID";
    private final static String FIELD_REFID = "Refid";
    private final static String FIELD_ID = "id";

    private IDsFinder _cidFinder;
    ArrayList<NameID> appsType = null;

    /**
     *
     */
    public GMSResults(QueryDialogSettings qdSettings) {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.GMSSESSION);
        addSelectionType(SelectionType.CONNID);
        addSelectionType(SelectionType.UUID);
        addSelectionType(SelectionType.IXN);
        addSelectionType(SelectionType.SESSION);
        addSelectionType(SelectionType.ANYPARAM);
    }

    @Override
    public void Retrieve(QueryDialog dlg, SelectionType key, String searchID) throws SQLException {
        runSelectionQuery(dlg, key, new IDsFinder(dlg, key, searchID));
        doSort();

    }

    @Override
    FullTableColors getAll(QueryDialog qd) throws SQLException {
        try {
            String tmpTable = "callFlowTmp";
            DynamicTreeNode.setNoRefNoLoad(true);

            DatabaseConnector.dropTable(tmpTable);
            inquirer.logger.info("Building temp tables");
            String tab = "sipms";

            DatabaseConnector.runQuery("create temp table " + tmpTable + " ("
                    + "callidid int"
                    + ",started timestamp"
                    + ",ended timestamp"
                    + ")\n;"
            );

//            String sWhere = StringUtils.join(new String[]{
//                IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false)),
//                IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange()),
//                getCheckedWhere("sip.nameID", ReferenceType.SIPMETHOD, node, "AND", DialogItem.TLIB_CALLS_SIP_NAME)
//            },
//                    "AND", 1, 3);
            Wheres wh = new Wheres();
            wh.addWhere(IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false)), "AND");
            wh.addWhere(IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange()), "AND");
//            wh.addWhere(IQuery.getCheckedWhere("sipms.nameID", ReferenceType.SIPMETHOD,
//                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_SIP, DialogItem.TLIB_CALLS_SIP_NAME)), "AND");
            wh.addWhere(getWhere("sipms.nameID", ReferenceType.SIPMETHOD, new String[]{"OPTIONS", "200 OK OPTIONS", "NOTIFY", "200 OK NOTIFY"}, false, false, true), "AND");

            DatabaseConnector.runQuery("insert into " + tmpTable + " (callidid, started, ended)"
                    + "\nselect distinct callidid, min(time), max(time) from " + tab
                    + "\n" + wh.makeWhere(true)
                    + "\ngroup by 1;");

            DatabaseConnector.runQuery("create index idx_" + tmpTable + "callidid on " + tmpTable + "(callidid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "started on " + tmpTable + "(started);");

            TableQuery tabReport = new TableQuery(tmpTable);
            tabReport.addOutField("UTCtoDateTime(started, \"YYYY-MM-dd HH:mm:ss.SSS\") started");
            tabReport.addOutField("UTCtoDateTime(ended, \"YYYY-MM-dd HH:mm:ss.SSS\") ended");
            tabReport.addOutField("jduration(ended-started) duration ");
            tabReport.setAddAll(false);

            tabReport.addRef("callidid", "callid", ReferenceType.SIPCALLID.toString(), FieldType.Optional);
            tabReport.setOrderBy(tabReport.getTabAlias() + ".started");
            FullTableColors currTable = tabReport.getFullTable();

            return currTable; //To change body of generated methods, choose Tools | Templates.
        } catch (SQLException ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        } finally {
            DynamicTreeNode.setNoRefNoLoad(false);

        }
        return null;
    }

    @Override
    SearchFields getSearchField() {
        SearchFields ret = new SearchFields();
        ret.addRecMap(FileInfoType.type_WWE, new Pair<>(SelectionType.CALLID, "callid"));
        return ret;
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new String[]{"gmsors"}, searchApps);
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return DatabaseConnector.getTimeRange(new String[]{"gmsors", TableType.GMSWebClientMessage.toString()});
    }

    @Override
    public String getReportSummary() {
        return getName() + "\n\t" + "search: " + ((_cidFinder != null) ? _cidFinder.toString() : "<not specified>");
    }

    @Override
    public String getName() {
        return "GMS";
    }

    private void addGMStoURS(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.GMStoURS));
        root.addChild(nd);

        DynamicTreeNode<OptionNode> nd1 = new DynamicTreeNode<>(new OptionNode(DialogItem.GMStoURS_PARAMS));
        nd.addChild(nd1);

        nd1.addDynamicRef(DialogItem.GMStoURS_PARAMS_NAME, ReferenceType.ORSREQ, "gmsors", "reqID");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_SOURCE, ReferenceType.App, "wwetevents", "sourceid");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_AGENT, ReferenceType.Agent, "wwetevents", "agentidid");

        DynamicTreeNode<OptionNode> AttrValue;

        DynamicTreeNode<OptionNode> Attr;
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventQueued"));      
//        Attr.addChild(AttrValue);
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventRouteRequested"));      
//        Attr.addChild(AttrValue);

        Attr = new DynamicTreeNode<>(new OptionNode(true, "CallType"));
        nd1.addChild(Attr);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Inbound"));
        Attr.addChild(AttrValue);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Outbound"));
        Attr.addChild(AttrValue);

    }

    private void addGMSWebRequests(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.GMSWEBREQUESTS));
        root.addChild(nd);

        DynamicTreeNode<OptionNode> nd1 = new DynamicTreeNode<>(new OptionNode(DialogItem.GMSWEBREQUESTS_PARAMS));
        nd.addChild(nd1);

        nd1.addDynamicRef(DialogItem.GMSWEBREQUESTS_PARAMS_URL, ReferenceType.HTTPURL, TableType.GMSWebClientMessage.toString(), "URLID");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_SOURCE, ReferenceType.App, "wwetevents", "sourceid");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_AGENT, ReferenceType.Agent, "wwetevents", "agentidid");

        DynamicTreeNode<OptionNode> AttrValue;

        DynamicTreeNode<OptionNode> Attr;
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventQueued"));      
//        Attr.addChild(AttrValue);
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventRouteRequested"));      
//        Attr.addChild(AttrValue);

        Attr = new DynamicTreeNode<>(new OptionNode(true, "CallType"));
        nd1.addChild(Attr);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Inbound"));
        Attr.addChild(AttrValue);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Outbound"));
        Attr.addChild(AttrValue);

    }

    private void addGMSPOST(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.GMSPOST));
        root.addChild(nd);

        DynamicTreeNode<OptionNode> nd1 = new DynamicTreeNode<>(new OptionNode(DialogItem.GMSPOST_PARAMS));
        nd.addChild(nd1);

        nd1.addDynamicRef(DialogItem.GMSPOST_PARAMS_EVENT, ReferenceType.GMSEvent, "gmspost", "EventNameID");
        nd1.addDynamicRef(DialogItem.GMSPOST_PARAMS_URIID, ReferenceType.ORSREQ, "gmspost", "URIID");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_SOURCE, ReferenceType.App, "wwetevents", "sourceid");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_AGENT, ReferenceType.Agent, "wwetevents", "agentidid");

        DynamicTreeNode<OptionNode> AttrValue;

        DynamicTreeNode<OptionNode> Attr;
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventQueued"));      
//        Attr.addChild(AttrValue);
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventRouteRequested"));      
//        Attr.addChild(AttrValue);

        Attr = new DynamicTreeNode<>(new OptionNode(true, "CallType"));
        nd1.addChild(Attr);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Inbound"));
        Attr.addChild(AttrValue);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Outbound"));
        Attr.addChild(AttrValue);

    }

    private void addLogMessagesReportType(DynamicTreeNode<OptionNode> root, TableType tableType) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(false, "Log messages"));
        root.addChild(nd);

        DynamicTreeNode<OptionNode> ComponentNode;
        ComponentNode = new DynamicTreeNode<>(new OptionNode(false, "Type"));
        nd.addChild(ComponentNode);

        /**
         * *****************
         */
        ComponentNode.addDynamicRef("Level", ReferenceType.MSGLEVEL);
        ComponentNode.addDynamicRefLogMessage("Message ID", tableType);

    }

    private void loadStdOptions() {
        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        addGMStoURS(rootA);
        addGMSPOST(rootA);
        addGMSWebRequests(rootA);

        addIxnReportType(rootA);

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(false, DialogItem.WWEEXCEPTIONS));
        rootA.addChild(nd);

        addConfigUpdates(rootA);
        try {
            addCustom(rootA, FileInfoType.type_GMS);
        } catch (SQLException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
        }
        rootA.addLogMessagesReportType(TableType.MsgGMS);
        DoneSTDOptions();
    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        if (appsType == null) {
            appsType = getAppsType(FileInfoType.type_GMS);
        }
        return appsType;

//        return getAppsType(FileInfoType.type_RM, new String[]{"tlib_logbr", "fileid", "sip_logbr", "fileid"});
    }

    void SetConfig(InquirerCfg cr) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Retrieve(QueryDialog dlg) throws SQLException {
        if (dlg.getSearchApps() == null) {
            inquirer.logger.info("No apps selected. Nothing to search in");
            return;
        }

        runSelectionQuery(dlg, dlg.getSelectionType(), new IDsFinder(dlg));

        getCustom(FileInfoType.type_GMS, dlg, repComponents.getRoot(), null, null);
        getGenesysMessages(TableType.MsgGMS, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);

        doSort();
    }

//    private void retrieveSIP
    private void doRetrieve(QueryDialog dlg, SelectionType selectionType, String selection, boolean isRegex) throws SQLException {
        ILogRecord record = null;
        IDsFinder cidFinder ;

        if (selection != null && (selectionType == SelectionType.CONNID
                || selectionType == SelectionType.CALLID)) {
            cidFinder = new IDsFinder();
            if (!cidFinder.initSearch()) {
                inquirer.logger.info("No call ID found; returning");
            }
        }
//        RetrieveSIP(dlg,
//                FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_SIP, null),
//                selectionType,
//                selection,
//                isRegex);
//
//        RetrieveTLib(dlg,
//                FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, null),
//                selectionType,
//                selection,
//                isRegex);

    }

//    private static final boolean TLIBNowRequests = "true".equals(System.getProperty("tlib.norequest"));
    private void runSelectionQuery(QueryDialog dlg, SelectionType selectionType, IDsFinder cidFinder) throws SQLException {
        this._cidFinder = cidFinder;
        if (selectionType != SelectionType.NO_SELECTION) {
            if (!cidFinder.initGMS()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            } else {
                setObjectsFound(cidFinder.getObjectsFound());
            }
        }

//        doRetrieve(dlg, cidFinder);
        boolean runQueries = true;
//        if (selectionType != SelectionType.NO_SELECTION && !cidFinder.anythingSIPRelated()
//                && !DatabaseConnector.TableEmpty("sip_logbr")) {
//            int dialogResult = inquirer.showConfirmDialog(dlg, "No SIP calls found.\n"
//                    + "Do you want to show all SIP messages?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
//            if (dialogResult == JOptionPane.CANCEL_OPTION) {
//                return;
//            }
//            if (dialogResult == JOptionPane.NO_OPTION) {
//                runQueries = false;
//            }
//        }

        IQuery.reSetQueryNo();

        if (runQueries) {
            retrieveGMSORS(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.GMStoURS, DialogItem.GMStoURS_PARAMS, null),
                    cidFinder);
            retrieveGMSPost(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.GMSPOST, DialogItem.GMSPOST_PARAMS, null),
                    cidFinder);
            retrieveGMSStart(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.GMStoURS, DialogItem.GMStoURS_PARAMS, null),
                    cidFinder);
            retrieveGMSWEBRequests(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.GMSWEBREQUESTS, DialogItem.GMSWEBREQUESTS_PARAMS, null),
                    cidFinder);
            retrieveGMSIxn(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.IXN, DialogItem.IXN_EVENT, null),
                    cidFinder);
        }

        RetrieveExceptions(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWEEXCEPTIONS, null, null),
                cidFinder);

    }

    private void retrieveGMSWEBRequests(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.GMSWebClientMessage.toString())) {
            Integer[] iDs;
            tellProgress("Retrieve GMS web request messages");

            TableQuery tab = null;
            tab = new TableQuery(MsgType.GMSWEBREQUESTS, TableType.GMSWebClientMessage.toString());

            tab.addRef("URLID", "URL", ReferenceType.HTTPURL.toString(), FieldType.Optional);
            Wheres wh = new Wheres();

            for (int i = 1; i <= com.myutils.logbrowser.indexer.GMSParser.MAX_WEB_PARAMS; i++) {
                String fldName = "param" + i;
                tab.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);
                String valueFldName = "value" + i;
                tab.addRef(valueFldName + "id", valueFldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);

                if (cidFinder != null) {
                    iDs = cidFinder.getIDs(IDType.ANYPARAM);

                    if (ArrayUtils.isNotEmpty(iDs)) {
                        wh.addWhere(getWhere(tab.getTabAlias() + "." + valueFldName + "id", iDs, false), "OR");
                    }

                }
            }

            if (!wh.isEmpty()) {
                tab.addWhere(wh);
                tab.addCollectIDs(GMSWEB_WEBREQID);
                tab.addCollectIDs("id");
            }

            tab.setCommonParams(this, dlg);
//            tab.setRecLoadedProc(new IQuery.IRecordLoadedProc() {
//                @Override
//                public void recordLoaded(ILogRecord rec) {
//                    addUnique(reqIDs, Util.intOrDef(rec.GetField("webReqID"), (Long) null));
//                    addUnique(iDsFound, rec.getID());
//                    addUnique(idFiles, new Long(rec.GetFileId()));
//                }
//
//            });
            getRecords(tab);
            if (tab.isCollectingID(GMSWEB_WEBREQID) && !tab.getCollectID(GMSWEB_WEBREQID).isEmpty()) {
                TableQuery tabRequests = new TableQuery(MsgType.GMSWEBREQUESTS, TableType.GMSWebClientMessage.toString());
                tellProgress("Retrieving URS RLib requests");
                Wheres wh1 = new Wheres();
                wh1.addWhere(getWhere(tabRequests.fldName(GMSWEB_WEBREQID), tab.getCollectID(GMSWEB_WEBREQID), false));
                wh1.addWhere(getWhereNot(tabRequests.fldName("id"), tab.getCollectID("id"), false), "AND");
                tabRequests.addWhere(wh1, "AND");

                for (int i = 1; i <= com.myutils.logbrowser.indexer.GMSParser.MAX_WEB_PARAMS; i++) {
                    String fldName = "param" + i;
                    tabRequests.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);
                    String valueFldName = "value" + i;
                    tabRequests.addRef(valueFldName + "id", valueFldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);

                }

                getRecords(tabRequests);
            }

        }

    }

    private void retrieveGMSPost(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("gmspost")) {
            tellProgress("Retrieve GMS to ORS messages");

            TableQuery tab = null;
            tab = new TableQuery(MsgType.GMSPOST, "gmspost");

            if (cidFinder != null) {
                Wheres wh = new Wheres();
                Integer[] iDs;
                if ((iDs = cidFinder.getIDs(IDType.ORSSID)) != null) {
                    wh.addWhere(getWhere(tab.getTabAlias() + ".ORSSessionid", iDs, false), "OR");
                }
                if ((iDs = cidFinder.getIDs(IDType.GMSSESSION)) != null) {
                    wh.addWhere(getWhere(tab.getTabAlias() + ".GMSServiceID", iDs, false), "OR");
                }

                if (!wh.isEmpty()) {
                    tab.addWhere(wh);
                } else {
                    tellProgress("No search IDs for the query; returning");
                    return;
                }
            }

            tab.AddCheckedWhere(tab.getTabAlias() + ".EventNameID",
                    ReferenceType.GMSEvent,
                    eventsSettings,
                    "AND",
                    DialogItem.GMSPOST_PARAMS_EVENT);

            tab.AddCheckedWhere(tab.getTabAlias() + ".URIID",
                    ReferenceType.ORSREQ,
                    eventsSettings,
                    "AND",
                    DialogItem.GMSPOST_PARAMS_URIID);

//
//            tlib.AddCheckedWhere(tlib.getTabAlias() + ".VoiceSwitchID",
//                    ReferenceType.Switch,
//                    findNode,
//                    "AND",
//                    DialogItem.URS_AGENTDN_STATSWITCH);
//            if (IQuery.isAllChecked(FindNode(findNode, DialogItem.URS_AGENTDN_AGENT, null, null))
//                    && (agentIDs != null && agentIDs.length > 0)) {
//                tlib.addWhere(getWhere("AgentNameID", agentIDs, false), "AND");
//
//            } else {
//                tlib.AddCheckedWhere(tlib.getTabAlias() + ".AgentNameID",
//                        ReferenceType.Agent,
//                        findNode,
//                        "AND",
//                        DialogItem.URS_AGENTDN_AGENT);
//            }
            tab.addRef("EventNameID", "EventName", ReferenceType.GMSEvent.toString(), FieldType.Optional);
            tab.addRef("CallBackTypeID", "CallBackType", ReferenceType.GMSCallbackType.toString(), FieldType.Optional);
            tab.addRef("GMSServiceID", "GMSService", ReferenceType.GMSService.toString(), FieldType.Optional);
            tab.addRef("ORSSessionID", "ORSSession", ReferenceType.ORSSID.toString(), FieldType.Optional);
            tab.addRef("phoneID", "phone", ReferenceType.DN.toString(), FieldType.Optional);
            tab.addRef("URIID", "URI", ReferenceType.ORSREQ.toString(), FieldType.Optional);

            tab.setCommonParams(this, dlg);
            getRecords(tab);

        }

    }

    private void retrieveGMSStart(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("gmsstart")) {
            tellProgress("Retrieve GMS to ORS messages");

            TableQuery tab = null;
            tab = new TableQuery(MsgType.GMSPOST, "gmsstart");

            if (cidFinder != null) {
                Wheres wh = new Wheres();
                Integer[] iDs;
                if ((iDs = cidFinder.getIDs(IDType.ORSSID)) != null) {
                    wh.addWhere(getWhere(tab.getTabAlias() + ".ORSSessionid", iDs, false), "OR");
                }
                if ((iDs = cidFinder.getIDs(IDType.GMSSESSION)) != null) {
                    wh.addWhere(getWhere(tab.getTabAlias() + ".GMSServiceID", iDs, false), "OR");
                }

                if (!wh.isEmpty()) {
                    tab.addWhere(wh);
                } else {
                    tellProgress("No search IDs for the query; returning");
                    return;
                }
            }

            tab.AddCheckedWhere(tab.getTabAlias() + ".EventNameID",
                    ReferenceType.GMSEvent,
                    eventsSettings,
                    "AND",
                    DialogItem.GMSPOST_PARAMS_EVENT);

            tab.AddCheckedWhere(tab.getTabAlias() + ".URIID",
                    ReferenceType.ORSREQ,
                    eventsSettings,
                    "AND",
                    DialogItem.GMSPOST_PARAMS_URIID);

//
//            tlib.AddCheckedWhere(tlib.getTabAlias() + ".VoiceSwitchID",
//                    ReferenceType.Switch,
//                    findNode,
//                    "AND",
//                    DialogItem.URS_AGENTDN_STATSWITCH);
//            if (IQuery.isAllChecked(FindNode(findNode, DialogItem.URS_AGENTDN_AGENT, null, null))
//                    && (agentIDs != null && agentIDs.length > 0)) {
//                tlib.addWhere(getWhere("AgentNameID", agentIDs, false), "AND");
//
//            } else {
//                tlib.AddCheckedWhere(tlib.getTabAlias() + ".AgentNameID",
//                        ReferenceType.Agent,
//                        findNode,
//                        "AND",
//                        DialogItem.URS_AGENTDN_AGENT);
//            }
            tab.addRef("EventNameID", "EventName", ReferenceType.GMSEvent.toString(), FieldType.Optional);
            tab.addRef("CallBackTypeID", "CallBackType", ReferenceType.GMSCallbackType.toString(), FieldType.Optional);
            tab.addRef("GMSServiceID", "GMSService", ReferenceType.GMSService.toString(), FieldType.Optional);
            tab.addRef("ORSSessionID", "ORSSession", ReferenceType.ORSSID.toString(), FieldType.Optional);
            tab.addRef("phoneID", "phone", ReferenceType.DN.toString(), FieldType.Optional);
            tab.addRef("URIID", "URI", ReferenceType.ORSREQ.toString(), FieldType.Optional);

            tab.setCommonParams(this, dlg);
            getRecords(tab);

        }

    }

    private void retrieveGMSORS(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("gmsors")) {
            tellProgress("Retrieve GMS to ORS messages");

            TableQuery tab = null;
            tab = new TableQuery(MsgType.GMSORS, "gmsors");

            if (cidFinder != null) {
                Wheres wh = new Wheres();
                Integer[] iDs;
                if ((iDs = cidFinder.getIDs(IDType.ConnID)) != null) {
                    wh.addWhere(getWhere(tab.getTabAlias() + ".ConnIdid", iDs, false), "OR");
                }
                if ((iDs = cidFinder.getIDs(IDType.ORSSID)) != null) {
                    wh.addWhere(getWhere(tab.getTabAlias() + ".ORSSessionid", iDs, false), "OR");
                }
                if ((iDs = cidFinder.getIDs(IDType.GMSSESSION)) != null) {
                    wh.addWhere(getWhere(tab.getTabAlias() + ".GMSServiceID", iDs, false), "OR");
                }

                if (!wh.isEmpty()) {
                    tab.addWhere(wh);
                } else {
                    tellProgress("No search IDs for the query; returning");
                    return;
                }
            }

            tab.AddCheckedWhere(tab.getTabAlias() + ".reqid",
                    ReferenceType.ORSREQ,
                    eventsSettings,
                    "AND",
                    DialogItem.GMStoURS_PARAMS_NAME);
//
//            tlib.AddCheckedWhere(tlib.getTabAlias() + ".VoiceSwitchID",
//                    ReferenceType.Switch,
//                    findNode,
//                    "AND",
//                    DialogItem.URS_AGENTDN_STATSWITCH);
//            if (IQuery.isAllChecked(FindNode(findNode, DialogItem.URS_AGENTDN_AGENT, null, null))
//                    && (agentIDs != null && agentIDs.length > 0)) {
//                tlib.addWhere(getWhere("AgentNameID", agentIDs, false), "AND");
//
//            } else {
//                tlib.AddCheckedWhere(tlib.getTabAlias() + ".AgentNameID",
//                        ReferenceType.Agent,
//                        findNode,
//                        "AND",
//                        DialogItem.URS_AGENTDN_AGENT);
//            }
            tab.addRef("GMSServiceID", "GMSService", ReferenceType.GMSService.toString(), FieldType.Optional);
            tab.addRef("ORSSessionID", "ORSSession", ReferenceType.ORSSID.toString(), FieldType.Optional);
            tab.addRef("connIDID", "connID", ReferenceType.ConnID.toString(), FieldType.Optional);
            tab.addRef("reqID", "request", ReferenceType.ORSREQ.toString(), FieldType.Optional);

            tab.addNullField("attr1ID");

            tab.setCommonParams(this, dlg);
            getRecords(tab);

        }

    }

    private void doSort() throws SQLException {
        if (IQuery.getQueryTypes() > 1) {
            Sort();
        } else {
            inquirer.logger.info(IQuery.getQueryTypes() + " record types; no sorting ");
        }
    }

    private void RetrieveExceptions(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("wweexceptions")) {
            tellProgress("Retrieve exceptions messages");

            TableQuery exc = null;
            exc = new TableQuery(MsgType.EXCEPTION, "wweexceptions");

//            tlib.AddCheckedWhere(tlib.getTabAlias() + ".nameID",
//                    ReferenceType.TEvent,
//                    eventsSettings,
//                    "AND",
//                    DialogItem.TLIB_CALLS_TEVENT_NAME);
//
            exc.addRef("exceptionid", "exception", ReferenceType.Exception.toString(), FieldType.Optional);
            exc.addRef("exceptionMsgID", "exceptionMsg", ReferenceType.ExceptionMessage.toString(), FieldType.Optional);

            exc.setCommonParams(this, dlg);
            getRecords(exc);

        }
    }

    @Override
    void showAllResults() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    protected ArrayList<IAggregateQuery> loadReportAggregates() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    boolean callRelatedSearch(IDsFinder cidFinder) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void addIxnReportType(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd1 = new DynamicTreeNode<>(new OptionNode(DialogItem.IXN));
        root.addChild(nd1);

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.IXN_EVENT));
        nd1.addChild(nd);

        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_NAME, ReferenceType.TEvent, TableType.IxnGMS.toString(), "nameID");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_APP, ReferenceType.App, TableType.IxnGMS.toString(), "appid");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_MEDIA, ReferenceType.IxnMedia, TableType.IxnGMS.toString(), "MediaTypeID");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_AGENT, ReferenceType.Agent, TableType.IxnGMS.toString(), "AgentID");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_PLACE, ReferenceType.Place, TableType.IxnGMS.toString(), "PlaceID");

        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_ATTR1, ReferenceType.TLIBATTR1, TableType.IxnGMS.toString(), "attr1ID");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_ATTR2, ReferenceType.TLIBATTR2, TableType.IxnGMS.toString(), "attr2ID");

        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_DN, ReferenceType.DN, "ixn", "ixnqueueid");

        DynamicTreeNode<OptionNode> AttrValue;

        DynamicTreeNode<OptionNode> Attr;
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventQueued"));      
//        Attr.addChild(AttrValue);
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventRouteRequested"));      
//        Attr.addChild(AttrValue);

        Attr = new DynamicTreeNode<>(new OptionNode(true, "CallType"));
        nd.addChild(Attr);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Inbound"));
        Attr.addChild(AttrValue);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Outbound"));
        Attr.addChild(AttrValue);

    }

    private void retrieveGMSIxn(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        Integer[] iDs = null;
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.IxnGMS.toString())) {
            if (cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION || (iDs = cidFinder.getIDs(IDType.IxnID)) != null) {
                tellProgress("Retrieve GMS IxnServer messages");
                TableQuery ixnQuery = MakeTQuery(eventsSettings);
                if (iDs != null) {
                    ixnQuery.addWhere(getWhere(ixnQuery.getTabAlias() + ".ixnID", iDs, false), "AND");
                    ixnQuery.addCollectIDs(FIELD_REFID);
                    ixnQuery.addCollectIDs(FIELD_ID);
                }
                ixnQuery.setCommonParams(this, dlg);
                getRecords(ixnQuery);

                if (ixnQuery.isCollectingID(FIELD_REFID) && !ixnQuery.getCollectID(FIELD_REFID).isEmpty()) {
                    TableQuery ixnQueryReq = MakeTQuery(eventsSettings);
                    tellProgress("Retrieving URS RLib requests");
                    Wheres wh1 = new Wheres();
                    wh1.addWhere(getWhere(ixnQueryReq.fldName(FIELD_REFID), ixnQuery.getCollectID(FIELD_REFID), false));
                    wh1.addWhere(getWhereNot(ixnQueryReq.fldName(FIELD_ID), ixnQuery.getCollectID(FIELD_ID), false), ANDOR.AND.toString());
                    ixnQueryReq.addWhere(wh1, ANDOR.AND.toString());

                    getRecords(ixnQueryReq);
                }

            }

        }

    }

    private TableQuery MakeTQuery(DynamicTreeNode<OptionNode> node) throws SQLException {
        TableQuery tLibReq = new TableQuery(MsgType.INTERACTION, TableType.IxnGMS.toString());
        tLibReq.addRef("IxnQueueID", "queue", ReferenceType.DN.toString(), FieldType.Optional);
        tLibReq.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Mandatory);
        tLibReq.addRef("ConnIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
        tLibReq.addRef("IxnID", "InteractionID", ReferenceType.IxnID.toString(), FieldType.Optional);
        tLibReq.addRef("appid", "client", ReferenceType.App.toString(), FieldType.Optional);

        tLibReq.addRef("MediaTypeID", "MediaType", ReferenceType.IxnMedia.toString(), FieldType.Optional);
        tLibReq.addRef("AgentID", "Agent", ReferenceType.Agent.toString(), FieldType.Optional);
        tLibReq.addRef("PlaceID", "Place", ReferenceType.Place.toString(), FieldType.Optional);
        tLibReq.addRef("attr1ID", "attr1", ReferenceType.TLIBATTR1.toString(), FieldType.Optional);
        tLibReq.addRef("attr2ID", "attr2", ReferenceType.TLIBATTR2.toString(), FieldType.Optional);

        tLibReq.AddCheckedWhere(tLibReq.fldName("nameID"), ReferenceType.TEvent, node, "AND", DialogItem.IXN_IXN_EVENT_NAME);

        tLibReq.AddCheckedWhere(tLibReq.fldName("ixnqueueid"), ReferenceType.DN, node, "AND", DialogItem.IXN_IXN_EVENT_DN);
        tLibReq.AddCheckedWhere(tLibReq.fldName("attr1ID"), ReferenceType.TLIBATTR1, node, "AND", DialogItem.IXN_IXN_EVENT_ATTR1);
        tLibReq.AddCheckedWhere(tLibReq.fldName("attr2ID"), ReferenceType.TLIBATTR2, node, "AND", DialogItem.IXN_IXN_EVENT_ATTR2);

        tLibReq.AddCheckedWhere(tLibReq.fldName("appid"), ReferenceType.App, node, "AND", DialogItem.IXN_IXN_EVENT_APP);
        tLibReq.AddCheckedWhere(tLibReq.fldName("MediaTypeID"), ReferenceType.IxnMedia, node, "AND", DialogItem.IXN_IXN_EVENT_MEDIA);
        tLibReq.AddCheckedWhere(tLibReq.fldName("AgentID"), ReferenceType.Agent, node, "AND", DialogItem.IXN_IXN_EVENT_AGENT);
        tLibReq.AddCheckedWhere(tLibReq.fldName("PlaceID"), ReferenceType.Place, node, "AND", DialogItem.IXN_IXN_EVENT_PLACE);

        return tLibReq;
    }
}
