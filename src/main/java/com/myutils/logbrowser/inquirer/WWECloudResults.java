package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

public class WWECloudResults extends IQueryResults {

    public static final int TLIB = 0x01;
    public static final int ISCC = 0x02;
    public static final int TC = 0x04;
    public static final int SIP = 0x08;
    public static final int PROXY = 0x10;
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private static final String[] RequestsToShow = {"RequestMakePredictiveCall", "RequestMakeCall"};
    private static final String[] EventsToShow = {"EventDialing", "EventNetworkReached"};
    private final UTCTimeRange timeRange = null;
    ArrayList<NameID> appsType = null;
    private Object cidFinder;
    private int m_componentFilter;

    /**
     *
     */
    public WWECloudResults(QueryDialogSettings qdSettings) {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.CALLID);
        addSelectionType(SelectionType.DN);
        addSelectionType(SelectionType.PEERIP);
        addSelectionType(SelectionType.AGENTID);
        addSelectionType(SelectionType.UUID);
    }

    @Override
    public void Retrieve(QueryDialog dlg, SelectionType key, String searchID) throws SQLException {
        runSelectionQuery(dlg, key, new IDsFinder(dlg, key, searchID));
        doSort();

    }

        @Override
    public IGetAllProc getAllProc(Component c, int x, int y) {
        return qd -> getAll(qd);
    }
  FullTableColors getAll(QueryDialog qd)  throws Exception {
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

            tabReport.addRef("callidid", "callid", ReferenceType.SIPCALLID.toString(), FieldType.OPTIONAL);
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
        ret.addRecMap(FileInfoType.type_WWECloud, new Pair<>(SelectionType.CALLID, "callid"));
        return ret;
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new String[]{TableType.WWECloud.toString(), "wweexceptions"}, searchApps);
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return DatabaseConnector.getTimeRange(new String[]{TableType.WWECloud.toString(), "wweexceptions"});
    }

    @Override
    public String getReportSummary() {
        return getName() + "\n\t" + "search: " + ((cidFinder != null) ? cidFinder.toString() : "<not specified>");
    }

    @Override
    public String getName() {
        return "WS Cloud Web";
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
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_NAME, ReferenceType.TEvent, TableType.WWECloud.toString(), "nameid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_DN, ReferenceType.DN, TableType.WWECloud.toString(), "thisdnid");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_SOURCE, ReferenceType.App, TableType.WWECloud.toString(), "sourceid");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_AGENT, ReferenceType.Agent, TableType.WWECloud.toString(), "agentidid");

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

    private void loadStdOptions() {
        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.TLIB_CALLS));
        rootA.addChild(nd);

        addTLibReportType(nd);

        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.WWECLOUDAUTH));
        rootA.addChild(nd);

        addAuthType(nd);

        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.WWEEXCEPTIONS));
        rootA.addChild(nd);

        DynamicTreeNode<OptionNode> ndExceptions = new DynamicTreeNode<>(new OptionNode(DialogItem.WWEEXCEPTIONS_EX));
        nd.addChild(ndExceptions);

//        DynamicTreeNode<OptionNode> ComponentNode;
//        ComponentNode = new DynamicTreeNode<OptionNode>(new OptionNode(EVENTS_REPORT));
//        nd.addChild(ComponentNode);
        /**
         * *****************
         */
        ndExceptions.addDynamicRef(DialogItem.WWEEXCEPTIONS_EX_NAME, ReferenceType.Exception, TableType.WWECloudException.toString(), "exNameID");
        ndExceptions.addDynamicRef(DialogItem.WWEEXCEPTIONS_EX_MSG, ReferenceType.ExceptionMessage, TableType.WWECloudException.toString(), "msgID");

        addConfigUpdates(rootA);
        try {
            addCustom(rootA, FileInfoType.type_WWECloud);
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }
        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.WWELOGMESSAGE));
        rootA.addChild(nd);
        DynamicTreeNode<Object> ndChild = new DynamicTreeNode<>(new OptionNode(DialogItem.WWELOGMESSAGE_MSG));
        nd.addChild(ndChild);
        ndChild.addDynamicRef(DialogItem.WWELOGMESSAGE_MSG_LEVEL,
                ReferenceType.CLOUD_LOG_MESSAGE_TYPE,
                TableType.WWECloudLog.toString(), "msgID");
        ndChild.addDynamicRef(DialogItem.WWELOGMESSAGE_MSG_TEXT,
                ReferenceType.CLOUD_LOG_MESSAGE,
                TableType.WWECloudLog.toString(), "msgTextID");

        DoneSTDOptions();
    }

    @Override
    public ArrayList<NameID> getApps() throws Exception {
        if (appsType == null) {
            appsType = getAppsType(FileInfoType.type_WWECloud);
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

    private TableQuery MakeTLibReq() throws Exception {
        TableQuery TLibReq = new TableQuery(MsgType.TLIB, "tlib_logbr");
        TLibReq.addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.MANDATORY);
        TLibReq.addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("sourceID", "source", ReferenceType.App.toString(), FieldType.OPTIONAL);
        TLibReq.addNullField("connid");
        TLibReq.addNullField("SipId");

        return TLibReq;
    }

    private void runSelectionQuery(QueryDialog dlg, SelectionType selectionType, IDsFinder cidFinder) throws SQLException {
        this.cidFinder = cidFinder;
        if (selectionType != SelectionType.NO_SELECTION) {
            if (!cidFinder.initWWECloud()) {
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
        if (selectionType != SelectionType.NO_SELECTION && !cidFinder.anythingTLibRelated()
                && DatabaseConnector.TableExist(TableType.WWECloud.toString())) {
            int dialogResult = inquirer.showConfirmDialog(dlg, "No calls found.\n"
                    + "Do you want to show all TLibrary messages?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
            if (dialogResult == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (dialogResult == JOptionPane.NO_OPTION) {
                runQueries = false;
            }
        }

        IQuery.reSetQueryNo();

        if (runQueries) {
            RetrieveTLib(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, null),
                    cidFinder);
        }

        RetrieveAuth(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWECLOUDAUTH, DialogItem.WWECLOUDAUTH_RESPONSE, null),
                cidFinder);

        RetrieveExceptions(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWEEXCEPTIONS, null, null),
                cidFinder);

        getCustom(FileInfoType.type_WWECloud, dlg, repComponents.getRoot(), cidFinder, null);

        RetrieveWWELog(dlg,
                FindNode(repComponents.getRoot(), DialogItem.WWELOGMESSAGE, DialogItem.WWELOGMESSAGE_MSG, null),
                cidFinder);

    }

    private void RetrieveTLib(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.WWECloud.toString())) {
            tellProgress("Retrieve TLIb messages");

            TableQuery tlib = null;
            tlib = new TableQuery(MsgType.TLIB, TableType.WWECloud.toString());

            Wheres wh = new Wheres();
            wh.addWhere(getWhere(new String[]{"ConnectionIDID", "TransferIDID"}, IQuery.ANDOR.OR, cidFinder.getIDs(IDType.ConnID)), "OR");
            wh.addWhere(getWhere("UUIDID", cidFinder.getIDs(IDType.UUID)), "OR");
            tlib.addWhere(wh);

            tlib.AddCheckedWhere(tlib.getTabAlias() + ".nameID",
                    ReferenceType.TEvent,
                    eventsSettings,
                    "AND",
                    DialogItem.TLIB_CALLS_TEVENT_NAME);
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
            tlib.addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
            tlib.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.OPTIONAL);
            tlib.addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
            tlib.addRef("ServerID", "Source", ReferenceType.App.toString(), FieldType.OPTIONAL);
            tlib.addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
            tlib.addOutField("intToHex( seqno) seqnoHex"); // bug in SQLite lib; does not accept full word

            tlib.addRef("TransferIDID", "TransferID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
            tlib.addRef("ixnIDID", "IXNID", ReferenceType.IxnID.toString(), FieldType.OPTIONAL);
            tlib.addRef("agentidid", "agentid", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
            tlib.addRef("uuidid", "uuid", ReferenceType.UUID.toString(), FieldType.OPTIONAL);
            tlib.addNullField("attr1ID");
            tlib.addNullField("attr2");
            tlib.addNullField("attr1");
            tlib.addNullField("err");
            tlib.addNullField("errmessage");
            tlib.addNullField("typestr");
            tlib.addNullField("handlerid");
            tlib.addNullField("nodeid");

            tlib.setCommonParams(this, dlg);
            getRecords(tlib);

        }

    }

    private void RetrieveWWELog(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.WWECloudLog.toString())) {
            tellProgress("Retrieve wwe log messages");

            TableQuery tab = new TableQuery(MsgType.CloudLog, TableType.WWECloudLog.toString());

            tab.AddCheckedWhere(tab.getTabAlias() + ".msgID",
                    ReferenceType.CLOUD_LOG_MESSAGE_TYPE,
                    eventsSettings,
                    "AND",
                    DialogItem.WWELOGMESSAGE_MSG_LEVEL);
            tab.AddCheckedWhere(tab.getTabAlias() + ".msgTextID",
                    ReferenceType.CLOUD_LOG_MESSAGE,
                    eventsSettings,
                    "AND",
                    DialogItem.WWELOGMESSAGE_MSG_TEXT);

            tab.addRef("msgID", "level", ReferenceType.CLOUD_LOG_MESSAGE_TYPE.toString(), FieldType.OPTIONAL);
            tab.addRef("msgTextID", "text", ReferenceType.CLOUD_LOG_MESSAGE.toString(), FieldType.OPTIONAL);

            tab.setCommonParams(this, dlg);
            getRecords(tab);

        }

    }

    private void RetrieveAuth(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.WWECloudAuth.toString())) {
            tellProgress("Retrieve client auth messages");

            TableQuery tlib = null;
            tlib = new TableQuery(MsgType.CloudAuth, TableType.WWECloudAuth.toString());

            Wheres wh = new Wheres();

//            tlib.AddCheckedWhere(tlib.getTabAlias() + ".nameID",
//                    ReferenceType.TEvent,
//                    eventsSettings,
//                    "AND",
//                    DialogItem.TLIB_CALLS_TEVENT_NAME);
            tlib.addRef("successID", "result", ReferenceType.Misc.toString(), FieldType.OPTIONAL);
            tlib.addRef("userID", "agent", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
            tlib.addRef("errorID", "error", ReferenceType.ErrorDescr.toString(), FieldType.OPTIONAL);

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
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.WWECloudException.toString())) {
            tellProgress("Retrieve exceptions messages");

            TableQuery exc = null;
            exc = new TableQuery(MsgType.EXCEPTION, TableType.WWECloudException.toString());

            exc.AddCheckedWhere(exc.getTabAlias() + ".exNameid",
                    ReferenceType.Exception,
                    eventsSettings,
                    "AND",
                    DialogItem.WWEEXCEPTIONS_EX_NAME);
            exc.AddCheckedWhere(exc.getTabAlias() + ".MsgID",
                    ReferenceType.ExceptionMessage,
                    eventsSettings,
                    "AND",
                    DialogItem.WWEEXCEPTIONS_EX_MSG);

            exc.addRef("exNameid", "exception", ReferenceType.Exception.toString(), FieldType.OPTIONAL);
            exc.addRef("MsgID", "exceptionMsg", ReferenceType.ExceptionMessage.toString(), FieldType.OPTIONAL);

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
        if (cidFinder != null) {
            try {
                return cidFinder.AnythingTLibRelated();
            } catch (SQLException ex) {
                logger.error("fatal: ", ex);
            }
        }
        return true;
    }

    private void addAuthType(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.WWECLOUDAUTH_RESPONSE));
        root.addChild(nd);

//        DynamicTreeNode<OptionNode> ComponentNode;
//        ComponentNode = new DynamicTreeNode<OptionNode>(new OptionNode(EVENTS_REPORT));
//        nd.addChild(ComponentNode);
        /**
         * *****************
         */
        nd.addDynamicRef(DialogItem.WWECLOUDAUTH_RESPONSE_RESULT, ReferenceType.Misc, TableType.WWECloudAuth.toString(), "successID");
        nd.addDynamicRef(DialogItem.WWECLOUDAUTH_RESPONSE_AGENT, ReferenceType.Agent, TableType.WWECloudAuth.toString(), "userID");
    }

}
