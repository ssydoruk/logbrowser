package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import org.apache.logging.log4j.LogManager;

public class WWEResults extends IQueryResults {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private static final String[] RequestsToShow = {"RequestMakePredictiveCall", "RequestMakeCall"};
    private static final String[] EventsToShow = {"EventDialing", "EventNetworkReached"};

    private Object cidFinder;
    ArrayList<NameID> appsType = null;
    private final UTCTimeRange timeRange = null;

    /**
     *
     */
    public WWEResults(QueryDialogSettings qdSettings) {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            createFilterMenus();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.CALLID);
        addSelectionType(SelectionType.DN);
        addSelectionType(SelectionType.PEERIP);
        addSelectionType(SelectionType.AGENTID);
        addSelectionType(SelectionType.UUID);
        addSelectionType(SelectionType.JSESSIONID);
        addSelectionType(SelectionType.IXN);
        addSelectionType(SelectionType.GWS_DEVICEID);
        addSelectionType(SelectionType.GWS_USERID);

    }

    @Override
    public void Retrieve(QueryDialog dlg, SelectionType key, String searchID) throws SQLException {
        runSelectionQuery(dlg, key, new IDsFinder(dlg, key, searchID));
        doSort();

    }

    @Override
    FullTableColors getAll(QueryDialog qd) throws Exception {
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
        return DatabaseConnector.getTimeRange(new String[]{"wwetevents", "wweexceptions", TableType.WWEMessage.toString(), TableType.ApacheWeb.toString()}, searchApps);
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return refreshTimeRange(null);
    }

    @Override
    public String getReportSummary() {
        return getName() + "\n\t" + "search: " + ((cidFinder != null) ? cidFinder.toString() : "<not specified>");
    }

    @Override
    public String getName() {
        return "WS Web";
    }

    public void addTLibReportType(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.TLIB_CALLS_TEVENT));
        root.addChild(nd);

//        DynamicTreeNode<OptionNode> ComponentNode;
//        ComponentNode = new DynamicTreeNode<OptionNode>(new OptionNode(EVENTS_REPORT));
//        nd.addChild(ComponentNode);
        /**
         * *****************
         */
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_NAME, ReferenceType.TEvent, "wwetevents", "nameid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_DN, ReferenceType.DN, "wwetevents", "thisdnid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_DNIS, ReferenceType.DN, "wwetevents", "DNISID");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_ANI, ReferenceType.DN, "wwetevents", "ANIID");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_AGENT, ReferenceType.Agent, "wwetevents", "agentIDID");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_SOURCE, ReferenceType.App, "wwetevents", "sourceid");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_AGENT, ReferenceType.Agent, "wwetevents", "agentidid");

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

    private void createFilterMenus() {
        DynamicTreeNode<OptionNode> ndParams;
        DynamicTreeNode<OptionNode> nd;

        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.TLIB_CALLS));
        rootA.addChild(nd);

        addTLibReportType(nd);

//<editor-fold defaultstate="collapsed" desc="Configuration for APACHEMSG">
        try {
            if (DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) {
                nd = new DynamicTreeNode<>(new OptionNode(DialogItem.APACHEMSG));
                rootA.addChild(nd);
                ndParams = new DynamicTreeNode<>(new OptionNode(DialogItem.APACHEMSG_PARAMS));
                nd.addChild(ndParams);

                ndParams.addDynamicRef(DialogItem.APACHEMSG_PARAMS_IP, ReferenceType.IP, TableType.ApacheWeb.toString(), "IPID");
                ndParams.addDynamicRef(DialogItem.APACHEMSG_PARAMS_HTTPCODE, ReferenceType.HTTPRESPONSE, TableType.ApacheWeb.toString(), "httpCode");
                ndParams.addDynamicRef(DialogItem.APACHEMSG_PARAMS_HTTPMETHOD, ReferenceType.HTTPMethod, TableType.ApacheWeb.toString(), "methodID");
                ndParams.addDynamicRef(DialogItem.APACHEMSG_PARAMS_HTTPURL, ReferenceType.HTTPURL, TableType.ApacheWeb.toString(), "urlID");
            }
        } catch (SQLException e) {
            inquirer.logger.error("Error while adding menu", e);
        }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="Configuration for WWEDEBUGMESSAGE and derived">
        try {
            if (DatabaseConnector.TableExist(TableType.WWEMessage.toString())) {
                nd = new DynamicTreeNode<>(new OptionNode(DialogItem.WWEDEBUGMESSAGE));
                rootA.addChild(nd);
                ndParams = new DynamicTreeNode<>(new OptionNode(DialogItem.WWEDEBUGMESSAGE_PARAMS));
                nd.addChild(ndParams);

                ndParams.addDynamicRef(DialogItem.WWEDEBUGMESSAGE_PARAMS_LEVEL, ReferenceType.MSGLEVEL);
                ndParams.addDynamicRef(DialogItem.WWEDEBUGMESSAGE_PARAMS_IP, ReferenceType.IP);
                ndParams.addDynamicRef(DialogItem.WWEDEBUGMESSAGE_PARAMS_HTTPURL, ReferenceType.HTTPURL);
                ndParams.addDynamicRef(DialogItem.WWEDEBUGMESSAGE_PARAMS_JAVACLASS, ReferenceType.JAVA_CLASS);
                ndParams.addDynamicRef(DialogItem.WWEDEBUGMESSAGE_PARAMS_MESSAGE, ReferenceType.Misc);
                ndParams.addDynamicRef(DialogItem.WWEDEBUGMESSAGE_PARAMS_USER, ReferenceType.Agent);

                if (DatabaseConnector.TableExist(TableType.WWEUCSMessage.toString())) {
                    nd = new DynamicTreeNode<>(new OptionNode(DialogItem.WWEUCSMESSAGE));
                    rootA.addChild(nd);
                    ndParams = new DynamicTreeNode<>(new OptionNode("Shared (go to " + DialogItem.WWEDEBUGMESSAGE + "->" + DialogItem.WWEDEBUGMESSAGE_PARAMS + ")"));
                    nd.addChild(ndParams);
                    ndParams = new DynamicTreeNode<>(new OptionNode(DialogItem.WWEUCSMESSAGE_PARAMS));
                    nd.addChild(ndParams);
                    ndParams.addDynamicRef(DialogItem.WWEUCSMESSAGE_PARAMS_NAME, ReferenceType.TEvent, TableType.WWEUCSMessage.toString(), "nameID");
                }
                if (DatabaseConnector.TableExist(TableType.WWEUCSConfigMessage.toString())) {
                    nd = new DynamicTreeNode<>(new OptionNode(DialogItem.WWECONFIGMESSAGE));
                    rootA.addChild(nd);
                    ndParams = new DynamicTreeNode<>(new OptionNode("Shared (go to " + DialogItem.WWEDEBUGMESSAGE + "->" + DialogItem.WWEDEBUGMESSAGE_PARAMS + ")"));
                    nd.addChild(ndParams);
                    ndParams = new DynamicTreeNode<>(new OptionNode(DialogItem.WWECONFIGMESSAGE_PARAMS));
                    nd.addChild(ndParams);
                    ndParams.addDynamicRef(DialogItem.WWECONFIGMESSAGE_PARAMS_NAME, ReferenceType.TEvent, TableType.WWEUCSConfigMessage.toString(), "nameID");
                }

                if (DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())) {
                    nd = new DynamicTreeNode<>(new OptionNode(DialogItem.WWEBAYEUXMESSAGE));
                    rootA.addChild(nd);
                    ndParams = new DynamicTreeNode<>(new OptionNode("Shared (go to " + DialogItem.WWEDEBUGMESSAGE + "->" + DialogItem.WWEDEBUGMESSAGE_PARAMS + ")"));
                    nd.addChild(ndParams);
                    ndParams = new DynamicTreeNode<>(new OptionNode(DialogItem.WWEBAYEUXMESSAGE_PARAMS));
                    nd.addChild(ndParams);
                    ndParams.addDynamicRef(DialogItem.WWEBAYEUXMESSAGE_PARAM1_NAME, ReferenceType.Misc, TableType.WWEBayeuxMessage.toString(), "param1ID");
                    ndParams.addDynamicRef(DialogItem.WWEBAYEUXMESSAGE_PARAM2_NAME, ReferenceType.Misc, TableType.WWEBayeuxMessage.toString(), "param2ID");
                }
            }
        } catch (SQLException e) {
            inquirer.logger.error("Error while adding menu", e);
        }
//</editor-fold>

        try {
            addCustom(rootA, FileInfoType.type_ApacheWeb);
        } catch (SQLException ex) {
            logger.error("fatal: ",  ex);
        }

        nd = new DynamicTreeNode<>(new OptionNode(false, DialogItem.WWEEXCEPTIONS));
        rootA.addChild(nd);

        addConfigUpdates(rootA);
        try {
            addCustom(rootA, FileInfoType.type_WWE);
        } catch (SQLException ex) {
            logger.error("fatal: ",  ex);
        }
        rootA.addLogMessagesReportType(TableType.MsgWWE);
        DoneSTDOptions();
    }

    @Override
    public ArrayList<NameID> getApps() throws Exception {
        if (appsType == null) {
            appsType = getAppsType(FileInfoType.type_WWE);
            appsType.addAll(getAppsType(FileInfoType.type_ApacheWeb));
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

//        getCustom(FileInfoType.type_MCP, dlg, repComponents.getRoot(), null, null);
//        getCustom(FileInfoType.type_RM, dlg, repComponents.getRoot(), null, null);
//        getGenesysMessages(TableType.MsgRM, repComponents.getRoot(), dlg, this);
//        getGenesysMessages(TableType.MsgMCP, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);

        doSort();
    }

//    private void retrieveSIP
    private void doRetrieve(QueryDialog dlg, SelectionType selectionType, String selection, boolean isRegex) throws Exception {
        ILogRecord record = null;
        IDsFinder cidFinder = null;

        if (selection != null && (selectionType == SelectionType.CONNID
                || selectionType == SelectionType.CALLID)) {
            cidFinder = new IDsFinder();
            if (!cidFinder.initSearch()) {
                inquirer.logger.info("No call ID found; returning");
                return;
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

    private void runSelectionQuery(QueryDialog dlg, SelectionType selectionType, IDsFinder cidFinder) throws SQLException {
        this.cidFinder = cidFinder;
        if (selectionType != SelectionType.NO_SELECTION) {
            cidFinder.initWWE();
        } else {
            int dialogResult = inquirer.showConfirmDialog(dlg,
                    "Do you want to retrieve all records?", "Please confirm", JOptionPane.YES_NO_OPTION);
            if (dialogResult == JOptionPane.NO_OPTION) {
                return;
            }
        }

        IQuery.reSetQueryNo();

        retrieveTLib(dlg,
                FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, null),
                cidFinder);

        retrieveApacheWeb(dlg,
                FindNode(repComponents.getRoot(), DialogItem.APACHEMSG, DialogItem.APACHEMSG_PARAMS, null),
                cidFinder);

        retrieveWWEMessage(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWEDEBUGMESSAGE, DialogItem.WWEDEBUGMESSAGE_PARAMS, null),
                cidFinder);

        retrieveWWEStatServerMessage(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWEDEBUGMESSAGE, DialogItem.WWEDEBUGMESSAGE_PARAMS, null),
                cidFinder);

        retrieveWWEUCSMessage(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWEDEBUGMESSAGE, DialogItem.WWEDEBUGMESSAGE_PARAMS, null, true),
                FindNode(repComponents.getRoot(), DialogItem.WWEUCSMESSAGE, DialogItem.WWEUCSMESSAGE_PARAMS, null),
                cidFinder);

        retrieveWWEUCSConfigMessage(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWEDEBUGMESSAGE, DialogItem.WWEDEBUGMESSAGE_PARAMS, null, true),
                FindNode(repComponents.getRoot(), DialogItem.WWECONFIGMESSAGE, DialogItem.WWECONFIGMESSAGE_PARAMS, null),
                cidFinder);

        retrieveWWEIxnMessage(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWEDEBUGMESSAGE, DialogItem.WWEDEBUGMESSAGE_PARAMS, null),
                cidFinder);

        retrieveWWEBayeuxMessage(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWEDEBUGMESSAGE, DialogItem.WWEDEBUGMESSAGE_PARAMS, null, true),
                FindNode(repComponents.getRoot(), DialogItem.WWEBAYEUXMESSAGE, DialogItem.WWEBAYEUXMESSAGE_PARAMS, null),
                cidFinder);

        RetrieveExceptions(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWEEXCEPTIONS, null, null),
                cidFinder);
        getCustom(FileInfoType.type_WWE, dlg, repComponents.getRoot(), cidFinder, null);

    }

    private void retrieveTLib(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("wwetevents")
                && (dlg.getSelectionType() == SelectionType.NO_SELECTION
                || !isEmpty(cidFinder.getIDs(IDType.UUID))
                || !isEmpty(cidFinder.getIDs(IDType.ConnID)))) {
            tellProgress("Retrieve TLIb messages");

            TableQuery tlib = null;
            tlib = new TableQuery(MsgType.TLIB, "wwetevents");

            Wheres wh = new Wheres();
            wh.addWhere(getWhere(new String[]{"ConnectionIDID", "TransferIDID"}, IQuery.ANDOR.OR, cidFinder.getIDs(IDType.ConnID)), "OR");
            wh.addWhere(getWhere("UUIDID", cidFinder.getIDs(IDType.UUID)), "OR");
            tlib.addWhere(wh);

            tlib.AddCheckedWhere(tlib.getTabAlias() + ".nameID",
                    ReferenceType.TEvent,
                    eventsSettings,
                    "AND",
                    DialogItem.TLIB_CALLS_TEVENT_NAME);

            tlib.AddCheckedWhere(tlib.getTabAlias() + ".agentIDID",
                    ReferenceType.Agent,
                    eventsSettings,
                    "AND",
                    DialogItem.TLIB_CALLS_TEVENT_AGENT);

            tlib.AddCheckedWhere(tlib.getTabAlias() + ".DNISID",
                    ReferenceType.DN,
                    eventsSettings,
                    "AND",
                    DialogItem.TLIB_CALLS_TEVENT_DNIS);
            tlib.AddCheckedWhere(tlib.getTabAlias() + ".ANIID",
                    ReferenceType.DN,
                    eventsSettings,
                    "AND",
                    DialogItem.TLIB_CALLS_TEVENT_ANI);
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
            tlib.addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.Optional);
            tlib.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Optional);
            tlib.addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.Optional);
            tlib.addRef("ServerID", "Source", ReferenceType.App.toString(), FieldType.Optional);
            tlib.addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
            tlib.addRef("agentIDID", "agentID", ReferenceType.Agent.toString(), FieldType.Optional);
            tlib.addRef("DNISID", "DNIS", ReferenceType.DN.toString(), FieldType.Optional);
            tlib.addRef("ANIID", "ANI", ReferenceType.DN.toString(), FieldType.Optional);
            tlib.addOutField("intToHex( seqno) seqnoHex"); // bug in SQLite lib; does not accept full word

            tlib.addRef("TransferIDID", "TransferID", ReferenceType.ConnID.toString(), FieldType.Optional);
            tlib.addRef("ixnIDID", "IXNID", ReferenceType.IxnID.toString(), FieldType.Optional);
            tlib.addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.Optional);
            tlib.addNullField("attr1ID");
            tlib.addNullField("attr2");
            tlib.addNullField("attr1");
            tlib.addNullField("err");
            tlib.addNullField("errmessage");
            tlib.addNullField("typestr");
            tlib.addNullField("handlerid");
            tlib.addNullField("nodeid");
            tlib.addNullField("place");

            tlib.setCommonParams(this, dlg);
            getRecords(tlib);

        }

    }

    private void doSort() throws SQLException {
        if (IQuery.getQueryTypes() > 1) {
            if (inquirer.getCr().isSorting()) {
                Sort();
            }
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
    IDsFinder.RequestLevel getDefaultQueryLevel() {
        return IDsFinder.RequestLevel.Level1; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected ArrayList<IAggregateQuery> loadReportAggregates() {
        ArrayList<IAggregateQuery> ret = new ArrayList<>();
        ret.add(new AggrTimestampDelays());
        try {
            if (!(DatabaseConnector.TableEmpty(TableType.ApacheWeb.toString()))) {
                ret.add(new AggrApacheRequests());
            }
        } catch (SQLException ex) {
            Logger.getLogger(WWEResults.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            if (DatabaseConnector.TableExist(TableType.WWEMessage.toString())) {
                ret.add(new AggrWWERequests());
            }
        } catch (SQLException ex) {
            Logger.getLogger(WWEResults.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ret;
    }

    @Override
    boolean callRelatedSearch(IDsFinder cidFinder) throws SQLException {
        if (cidFinder != null) {
            try {
                return cidFinder.AnythingTLibRelated();
            } catch (SQLException ex) {
                logger.error("fatal: ",  ex);
            }
        }
        return true;
    }

    private void retrieveApacheWeb(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.ApacheWeb.toString())
                && (dlg.getSelectionType() == SelectionType.NO_SELECTION
                || !isEmpty(cidFinder.getIDs(IDType.JSessionID))
                || !isEmpty(cidFinder.getIDs(IDType.UUID))
                || !isEmpty(cidFinder.getIDs(IDType.AGENT))
                || !isEmpty(cidFinder.getIDs(IDType.BrowserClientID))
                || !isEmpty(cidFinder.getIDs(IDType.GWS_DeviceID)))) {
            tellProgress("Retrieve messages");

            TableQuery q = null;
            q = new TableQuery(MsgType.APACHEWEB, TableType.ApacheWeb.toString());

            Wheres wh = new Wheres();
            wh.addWhere(getWhere("JSessionIDID", cidFinder.getIDs(IDType.JSessionID)), "AND");
            wh.addWhere(getWhere("UUIDID", cidFinder.getIDs(IDType.UUID)), "OR");
            wh.addWhere(getWhere("userNameID", cidFinder.getIDs(IDType.AGENT)), "OR");
            wh.addWhere(getWhere("DeviceIDID", cidFinder.getIDs(IDType.GWS_DeviceID)), "OR");
            wh.addWhere(getWhere("browserClientID", cidFinder.getIDs(IDType.BrowserClientID)), "OR");
            q.addWhere(wh);

            q.AddCheckedWhere(q.getTabAlias() + ".IPID", ReferenceType.IP, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_IP);
            q.AddCheckedWhere(q.getTabAlias() + ".httpCode", ReferenceType.HTTPRESPONSE, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_HTTPCODE);
            q.AddCheckedWhere(q.getTabAlias() + ".methodID", ReferenceType.HTTPMethod, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_HTTPMETHOD);
            q.AddCheckedWhere(q.getTabAlias() + ".urlID", ReferenceType.HTTPURL, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_HTTPURL);

            q.addRef("httpCode", "Code", ReferenceType.HTTPRESPONSE.toString(), FieldType.Optional);
            q.addRef("IPID", "IP", ReferenceType.IP.toString(), FieldType.Optional);
            q.addRef("JSessionIDID", "JSessionID", ReferenceType.JSessionID.toString(), FieldType.Optional);
            q.addRef("methodID", "method", ReferenceType.HTTPMethod.toString(), FieldType.Optional);
            q.addRef("urlID", "url", ReferenceType.HTTPURL.toString(), FieldType.Optional);
            q.addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.Optional);
            q.addRef("IxnIDID", "IxnID", ReferenceType.IxnID.toString(), FieldType.Optional);
            q.addRef("DeviceIDID", "DeviceID", ReferenceType.GWSDeviceID.toString(), FieldType.Optional);
            q.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.Optional);
            q.addRef("browserClientID", "browserClient", ReferenceType.WWEBrowserClient.toString(), FieldType.Optional);

            q.setCommonParams(this, dlg);
            getRecords(q);

        }

    }

    private void retrieveWWEMessage(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.WWEMessage.toString())
                && (dlg.getSelectionType() == SelectionType.NO_SELECTION
                || !isEmpty(cidFinder.getIDs(IDType.JSessionID))
                || !isEmpty(cidFinder.getIDs(IDType.AGENT))
                || !isEmpty(cidFinder.getIDs(IDType.IxnID))
                || !isEmpty(cidFinder.getIDs(IDType.UUID))
                || !isEmpty(cidFinder.getIDs(IDType.WWEUserID))
                || !isEmpty(cidFinder.getIDs(IDType.BrowserClientID)))) {
            tellProgress("Retrieve messages");

            TableQuery q = null;
            q = new TableQuery(MsgType.WWEMessage, TableType.WWEMessage.toString());

            Wheres wh = new Wheres();
            wh.addWhere(getWhere("JSessionIDID", cidFinder.getIDs(IDType.JSessionID)), "AND");
            wh.addWhere(getWhere("userNameID", cidFinder.getIDs(IDType.AGENT)), "OR");
            wh.addWhere(getWhere("IxnIDID", cidFinder.getIDs(IDType.IxnID)), "OR");
            wh.addWhere(getWhere("UUIDID", cidFinder.getIDs(IDType.UUID)), "OR");
            wh.addWhere(getWhere("userIDID", cidFinder.getIDs(IDType.WWEUserID)), "OR");
            wh.addWhere(getWhere("browserClientID", cidFinder.getIDs(IDType.BrowserClientID)), "OR");
            q.addWhere(wh);

            q.AddCheckedWhere(q.getTabAlias() + ".IPID", ReferenceType.IP, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_IP);
            q.AddCheckedWhere(q.getTabAlias() + ".urlID", ReferenceType.HTTPURL, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_HTTPURL);
            q.AddCheckedWhere(q.getTabAlias() + ".LevelID", ReferenceType.MSGLEVEL, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_LEVEL);
            q.AddCheckedWhere(q.getTabAlias() + ".classNameID", ReferenceType.JAVA_CLASS, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_JAVACLASS);
            q.AddCheckedWhere(q.getTabAlias() + ".msgTextID", ReferenceType.Misc, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_MESSAGE);
            q.AddCheckedWhere(q.getTabAlias() + ".userNameID", ReferenceType.Agent, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_USER);

            q.addRef("IPID", "IP", ReferenceType.IP.toString(), FieldType.Optional);
            q.addRef("JSessionIDID", "JSessionID", ReferenceType.JSessionID.toString(), FieldType.Optional);
            q.addRef("urlID", "url", ReferenceType.HTTPURL.toString(), FieldType.Optional);
            q.addRef("LevelID", "Level", ReferenceType.MSGLEVEL.toString(), FieldType.Optional);
            q.addRef("classNameID", "class", ReferenceType.JAVA_CLASS.toString(), FieldType.Optional);
            q.addRef("msgTextID", "msgText", ReferenceType.Misc.toString(), FieldType.Optional);
            q.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.Optional);
            q.addRef("param1ID", "param1", ReferenceType.MiscParam.toString(), FieldType.Optional);
            q.addRef("param2ID", "param2", ReferenceType.MiscParam.toString(), FieldType.Optional);
            q.addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.Optional);
            q.addRef("IxnIDID", "IxnID", ReferenceType.IxnID.toString(), FieldType.Optional);
            q.addRef("DeviceIDID", "DeviceID", ReferenceType.GWSDeviceID.toString(), FieldType.Optional);
            q.addRef("UserIDID", "UserID", ReferenceType.WWEUserID.toString(), FieldType.Optional);
            q.addRef("browserClientID", "browserClient", ReferenceType.WWEBrowserClient.toString(), FieldType.Optional);

            q.setCommonParams(this, dlg);
            getRecords(q);

        }

    }

    private void retrieveWWEStatServerMessage(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.WWEStatServerMessage.toString())
                && (dlg.getSelectionType() == SelectionType.NO_SELECTION
                || !isEmpty(cidFinder.getIDs(IDType.UUID))
                || !isEmpty(cidFinder.getIDs(IDType.AGENT)))) {

            tellProgress("Retrieve messages");

            TableQuery q = null;
            q = new TableQuery(MsgType.WWEMessage, TableType.WWEStatServerMessage.toString());

            Wheres wh = new Wheres();
            wh.addWhere(getWhere("JSessionIDID", cidFinder.getIDs(IDType.JSessionID)), "AND");
            wh.addWhere(getWhere("userNameID", cidFinder.getIDs(IDType.AGENT)), "OR");
            q.addWhere(wh);

            q.AddCheckedWhere(q.getTabAlias() + ".IPID", ReferenceType.IP, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_IP);
            q.AddCheckedWhere(q.getTabAlias() + ".urlID", ReferenceType.HTTPURL, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_HTTPURL);
            q.AddCheckedWhere(q.getTabAlias() + ".LevelID", ReferenceType.MSGLEVEL, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_LEVEL);
            q.AddCheckedWhere(q.getTabAlias() + ".classNameID", ReferenceType.JAVA_CLASS, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_JAVACLASS);
            q.AddCheckedWhere(q.getTabAlias() + ".msgTextID", ReferenceType.Misc, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_MESSAGE);
            q.AddCheckedWhere(q.getTabAlias() + ".UserIDID", ReferenceType.Agent, eventsSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_USER);

            q.addRef("IPID", "IP", ReferenceType.IP.toString(), FieldType.Optional);
            q.addRef("JSessionIDID", "JSessionID", ReferenceType.JSessionID.toString(), FieldType.Optional);
            q.addRef("urlID", "url", ReferenceType.HTTPURL.toString(), FieldType.Optional);
            q.addRef("LevelID", "Level", ReferenceType.MSGLEVEL.toString(), FieldType.Optional);
            q.addRef("classNameID", "class", ReferenceType.JAVA_CLASS.toString(), FieldType.Optional);
            q.addRef("msgTextID", "msgText", ReferenceType.Misc.toString(), FieldType.Optional);
            q.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.Optional);
            q.addRef("param1ID", "param1", ReferenceType.MiscParam.toString(), FieldType.Optional);
            q.addRef("param2ID", "param2", ReferenceType.MiscParam.toString(), FieldType.Optional);
            q.addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.Optional);
            q.addRef("IxnIDID", "IxnID", ReferenceType.IxnID.toString(), FieldType.Optional);
            q.addRef("DeviceIDID", "DeviceID", ReferenceType.GWSDeviceID.toString(), FieldType.Optional);
            q.addRef("UserIDID", "UserID", ReferenceType.WWEUserID.toString(), FieldType.Optional);

            q.setCommonParams(this, dlg);
            getRecords(q);

        }

    }

    private void retrieveWWEUCSMessage(QueryDialog dlg, DynamicTreeNode<OptionNode> commonSettings, DynamicTreeNode<OptionNode> specificSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(specificSettings) && DatabaseConnector.TableExist(TableType.WWEUCSMessage.toString())
                && (dlg.getSelectionType() == SelectionType.NO_SELECTION
                || !isEmpty(cidFinder.getIDs(IDType.JSessionID))
                || !isEmpty(cidFinder.getIDs(IDType.AGENT))
                || !isEmpty(cidFinder.getIDs(IDType.IxnID))
                || !isEmpty(cidFinder.getIDs(IDType.GWS_DeviceID)))) {
            tellProgress("Retrieve messages");

            TableQuery q = null;
            q = new TableQuery(MsgType.WWEUCS, TableType.WWEUCSMessage.toString());

            Wheres wh = new Wheres();

            wh.addWhere(getWhere("JSessionIDID", cidFinder.getIDs(IDType.JSessionID)), "AND");
            wh.addWhere(getWhere("userNameID", cidFinder.getIDs(IDType.AGENT)), "OR");
            wh.addWhere(getWhere("DeviceIDID", cidFinder.getIDs(IDType.GWS_DeviceID)), "OR");
            q.addWhere(wh);

            q.AddCheckedWhere(q.getTabAlias() + ".IPID", ReferenceType.IP, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_IP);
            q.AddCheckedWhere(q.getTabAlias() + ".urlID", ReferenceType.HTTPURL, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_HTTPURL);
            q.AddCheckedWhere(q.getTabAlias() + ".LevelID", ReferenceType.MSGLEVEL, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_LEVEL);
            q.AddCheckedWhere(q.getTabAlias() + ".classNameID", ReferenceType.JAVA_CLASS, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_JAVACLASS);
            q.AddCheckedWhere(q.getTabAlias() + ".nameID", ReferenceType.TEvent, specificSettings, "AND", DialogItem.WWEUCSMESSAGE_PARAMS_NAME);
            q.AddCheckedWhere(q.getTabAlias() + ".msgTextID", ReferenceType.Misc, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_MESSAGE);
            q.AddCheckedWhere(q.getTabAlias() + ".UserNameID", ReferenceType.Agent, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_USER);

            q.addRef("IPID", "IP", ReferenceType.IP.toString(), FieldType.Optional);
            q.addRef("JSessionIDID", "JSessionID", ReferenceType.JSessionID.toString(), FieldType.Optional);
            q.addRef("urlID", "url", ReferenceType.HTTPURL.toString(), FieldType.Optional);
            q.addRef("LevelID", "Level", ReferenceType.MSGLEVEL.toString(), FieldType.Optional);
            q.addRef("classNameID", "class", ReferenceType.JAVA_CLASS.toString(), FieldType.Optional);
            q.addRef("msgTextID", "msgText", ReferenceType.Misc.toString(), FieldType.Optional);
            q.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.Optional);
            q.addRef("DeviceIDID", "DeviceID", ReferenceType.GWSDeviceID.toString(), FieldType.Optional);

            q.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Optional);
            q.addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.Optional);
            q.addRef("IxnIDID", "IxnID", ReferenceType.IxnID.toString(), FieldType.Optional);

            q.setCommonParams(this, dlg);
            getRecords(q);

        }

    }

    private void retrieveWWEUCSConfigMessage(QueryDialog dlg, DynamicTreeNode<OptionNode> commonSettings, DynamicTreeNode<OptionNode> specificSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(specificSettings) && DatabaseConnector.TableExist(TableType.WWEUCSConfigMessage.toString())
                && (dlg.getSelectionType() == SelectionType.NO_SELECTION
                || !isEmpty(cidFinder.getIDs(IDType.JSessionID))
                || !isEmpty(cidFinder.getIDs(IDType.AGENT))
                || !isEmpty(cidFinder.getIDs(IDType.IxnID))
                || !isEmpty(cidFinder.getIDs(IDType.GWS_DeviceID)))) {
            tellProgress("Retrieve messages");

            TableQuery q = null;
            q = new TableQuery(MsgType.WWEUCS, TableType.WWEUCSConfigMessage.toString());

            Wheres wh = new Wheres();

            wh.addWhere(getWhere("JSessionIDID", cidFinder.getIDs(IDType.JSessionID)), "AND");
            wh.addWhere(getWhere("userNameID", cidFinder.getIDs(IDType.AGENT)), "OR");
            q.addWhere(wh);

            q.AddCheckedWhere(q.getTabAlias() + ".IPID", ReferenceType.IP, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_IP);
            q.AddCheckedWhere(q.getTabAlias() + ".urlID", ReferenceType.HTTPURL, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_HTTPURL);
            q.AddCheckedWhere(q.getTabAlias() + ".LevelID", ReferenceType.MSGLEVEL, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_LEVEL);
            q.AddCheckedWhere(q.getTabAlias() + ".classNameID", ReferenceType.JAVA_CLASS, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_JAVACLASS);
            q.AddCheckedWhere(q.getTabAlias() + ".nameID", ReferenceType.TEvent, specificSettings, "AND", DialogItem.WWECONFIGMESSAGE_PARAMS_NAME);
            q.AddCheckedWhere(q.getTabAlias() + ".msgTextID", ReferenceType.Misc, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_MESSAGE);
            q.AddCheckedWhere(q.getTabAlias() + ".UserNameID", ReferenceType.Agent, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_USER);

            q.addRef("IPID", "IP", ReferenceType.IP.toString(), FieldType.Optional);
            q.addRef("JSessionIDID", "JSessionID", ReferenceType.JSessionID.toString(), FieldType.Optional);
            q.addRef("urlID", "url", ReferenceType.HTTPURL.toString(), FieldType.Optional);
            q.addRef("LevelID", "Level", ReferenceType.MSGLEVEL.toString(), FieldType.Optional);
            q.addRef("classNameID", "class", ReferenceType.JAVA_CLASS.toString(), FieldType.Optional);
            q.addRef("msgTextID", "msgText", ReferenceType.Misc.toString(), FieldType.Optional);
            q.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.Optional);

            q.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Optional);

            q.setCommonParams(this, dlg);
            getRecords(q);

        }

    }

    private void retrieveWWEIxnMessage(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.WWEIxnMessage.toString())) {
            tellProgress("Retrieve messages");

            TableQuery q = null;
            q = new TableQuery(MsgType.WWEIXN, TableType.WWEIxnMessage.toString());

//            Wheres wh = new Wheres();
//            wh.addWhere(getWhere("JSessionIDID", cidFinder.getIDs(IDType.JSessionID)), "AND");
//            wh.addWhere(getWhere("UUIDID", cidFinder.getIDs(IDType.UUID)), "OR");
//            wh.addWhere(getWhere("userNameID", cidFinder.getIDs(IDType.AGENT)), "OR");
//            wh.addWhere(getWhere("DeviceIDID", cidFinder.getIDs(IDType.GWS_DeviceID)), "OR");
//            q.addWhere(wh);
//
//            q.AddCheckedWhere(q.getTabAlias() + ".IPID", ReferenceType.IP, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_IP);
//            q.AddCheckedWhere(q.getTabAlias() + ".httpCode", ReferenceType.HTTPRESPONSE, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_HTTPCODE);
//            q.AddCheckedWhere(q.getTabAlias() + ".methodID", ReferenceType.HTTPMethod, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_HTTPMETHOD);
//            q.AddCheckedWhere(q.getTabAlias() + ".urlID", ReferenceType.HTTPURL, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_HTTPURL);
            q.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Optional);
//            q.addRef("IPID", "IP", ReferenceType.IP.toString(), FieldType.Optional);
//            q.addRef("JSessionIDID", "JSessionID", ReferenceType.JSessionID.toString(), FieldType.Optional);
//            q.addRef("methodID", "method", ReferenceType.HTTPMethod.toString(), FieldType.Optional);
//            q.addRef("urlID", "url", ReferenceType.HTTPURL.toString(), FieldType.Optional);
//            q.addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.Optional);
//            q.addRef("IxnIDID", "IxnID", ReferenceType.IxnID.toString(), FieldType.Optional);
//            q.addRef("DeviceIDID", "DeviceID", ReferenceType.GWSDeviceID.toString(), FieldType.Optional);
//            q.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.Optional);

            q.setCommonParams(this, dlg);
            getRecords(q);

        }

    }

    private void retrieveWWEBayeuxMessage(QueryDialog dlg, DynamicTreeNode<OptionNode> commonSettings, DynamicTreeNode<OptionNode> msgSpecific, IDsFinder cidFinder) throws SQLException {
        if (isChecked(msgSpecific) && DatabaseConnector.TableExist(TableType.WWEBayeuxMessage.toString())
                && (dlg.getSelectionType() == SelectionType.NO_SELECTION
                || !isEmpty(cidFinder.getIDs(IDType.JSessionID))
                || !isEmpty(cidFinder.getIDs(IDType.UUID))
                || !isEmpty(cidFinder.getIDs(IDType.AGENT))
                || !isEmpty(cidFinder.getIDs(IDType.IxnID))
                || !isEmpty(cidFinder.getIDs(IDType.BrowserClientID))
                || !isEmpty(cidFinder.getIDs(IDType.GWS_DeviceID)))) {
            tellProgress("Retrieve messages");

            TableQuery q = null;
            q = new TableQuery(MsgType.WWEBAYEUX, TableType.WWEBayeuxMessage.toString());

            Wheres whCommon = new Wheres();
            whCommon.addWhere(getWhere("JSessionIDID", cidFinder.getIDs(IDType.JSessionID)), "AND");
            whCommon.addWhere(getWhere("UUIDID", cidFinder.getIDs(IDType.UUID)), "OR");
            whCommon.addWhere(getWhere("userNameID", cidFinder.getIDs(IDType.AGENT)), "OR");
            whCommon.addWhere(getWhere("DeviceIDID", cidFinder.getIDs(IDType.GWS_DeviceID)), "OR");
            whCommon.addWhere(getWhere("IxnID", cidFinder.getIDs(IDType.IxnID)), "OR");
            whCommon.addWhere(getWhere("browserClientID", cidFinder.getIDs(IDType.BrowserClientID)), "OR");

            Wheres whTop = new Wheres();
            whTop.addWhere(whCommon, "AND");
            q.addWhere(whTop);

            q.AddCheckedWhere(q.getTabAlias() + ".IPID", ReferenceType.IP, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_IP);
            q.AddCheckedWhere(q.getTabAlias() + ".urlID", ReferenceType.HTTPURL, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_HTTPURL);
            q.AddCheckedWhere(q.getTabAlias() + ".LevelID", ReferenceType.MSGLEVEL, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_LEVEL);
            q.AddCheckedWhere(q.getTabAlias() + ".classNameID", ReferenceType.JAVA_CLASS, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_JAVACLASS);
            q.AddCheckedWhere(q.getTabAlias() + ".msgTextID", ReferenceType.Misc, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_MESSAGE);
            q.AddCheckedWhere(q.getTabAlias() + ".param1ID", ReferenceType.Misc, msgSpecific, "AND", DialogItem.WWEBAYEUXMESSAGE_PARAM1_NAME);
            q.AddCheckedWhere(q.getTabAlias() + ".param2ID", ReferenceType.Misc, msgSpecific, "AND", DialogItem.WWEBAYEUXMESSAGE_PARAM2_NAME);
            q.AddCheckedWhere(q.getTabAlias() + ".UserNameID", ReferenceType.Agent, commonSettings, "AND", DialogItem.WWEDEBUGMESSAGE_PARAMS_USER);

            q.addRef("IPID", "IP", ReferenceType.IP.toString(), FieldType.Optional);
            q.addRef("JSessionIDID", "JSessionID", ReferenceType.JSessionID.toString(), FieldType.Optional);
            q.addRef("urlID", "url", ReferenceType.HTTPURL.toString(), FieldType.Optional);
            q.addRef("LevelID", "Level", ReferenceType.MSGLEVEL.toString(), FieldType.Optional);
            q.addRef("classNameID", "class", ReferenceType.JAVA_CLASS.toString(), FieldType.Optional);
            q.addRef("msgTextID", "msgText", ReferenceType.Misc.toString(), FieldType.Optional);
            q.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.Optional);
            q.addRef("param1ID", "param1", ReferenceType.MiscParam.toString(), FieldType.Optional);
            q.addRef("param2ID", "param2", ReferenceType.MiscParam.toString(), FieldType.Optional);
            q.addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.Optional);
            q.addRef("IxnIDID", "IxnID", ReferenceType.IxnID.toString(), FieldType.Optional);
            q.addRef("DeviceIDID", "DeviceID", ReferenceType.GWSDeviceID.toString(), FieldType.Optional);
            q.addRef("channelID", "channel", ReferenceType.BayeuxChannel.toString(), FieldType.Optional);
            q.addRef("msgTypeID", "msgType", ReferenceType.BayeuxMessageType.toString(), FieldType.Optional);
            q.addRef("browserClientID", "browserClient", ReferenceType.WWEBrowserClient.toString(), FieldType.Optional);
//            q.addRef("UserIDID", "UserID", ReferenceType.WWEUserID.toString(), FieldType.Optional);

            q.setCommonParams(this, dlg);
            getRecords(q);

        }

    }

}
