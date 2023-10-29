package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

public class WorkspaceResults extends IQueryResults {

    public static final int TLIB = 0x01;
    public static final int ISCC = 0x02;
    public static final int TC = 0x04;
    public static final int SIP = 0x08;
    public static final int PROXY = 0x10;
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private static final String[] RequestsToShow = {"RequestMakePredictiveCall", "RequestMakeCall"};
    private static final String[] EventsToShow = {"EventDialing", "EventNetworkReached"};
    private final UTCTimeRange timeRange = null;
    private Object cidFinder;
    private ArrayList<NameID> appsType;
    private int m_componentFilter;

    public WorkspaceResults(QueryDialogSettings qdSettings) {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.CONNID);
        addSelectionType(SelectionType.CALLID);
        addSelectionType(SelectionType.DN);
        addSelectionType(SelectionType.PEERIP);
        addSelectionType(SelectionType.AGENTID);
        addSelectionType(SelectionType.UUID);
        addSelectionType(SelectionType.REFERENCEID);
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
            String tab = "wstlib";

            DatabaseConnector.runQuery("create temp table " + tmpTable + " ("
                    + "connectionidid int"
                    + ",callidid int"
                    + ",started timestamp"
                    + ",ended timestamp"
                    + ",thisdnid int"
                    + ",otherdnid int"
                    + ",aniid int"
                    + ",nameid int"
                    + ",dnisid int"
                    + ",isconsult bit"
                    + ",isiscc bit"
                    + ")\n;"
            );

//            Wheres wh = new Wheres();
//            wh.addWhere(IQuery.getCheckedWhere("ThisDNid", ReferenceType.DN,
//                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, DialogItem.TLIB_CALLS_TEVENT_DN)), "OR");
//            wh.addWhere(IQuery.getCheckedWhere("otherDNID", ReferenceType.DN,
//                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, DialogItem.TLIB_CALLS_TEVENT_DN)), "OR");
//
//            String dnWhere = wh.makeWhere("AND", false);
            Wheres wh = new Wheres();
            wh.addWhere(IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false)), "AND");
            wh.addWhere(IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange()), "AND");
            DatabaseConnector.runQuery("insert into " + tmpTable + " (connectionidid, started, ended)"
                    + "\nselect distinct connectionidid, min(time), max(time) from " + tab
                    + "\n" + wh.makeWhere(true)
                    + "\ngroup by 1;");

            tab = "sipep";
            wh = new Wheres();
            wh.addWhere(IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false)), "AND");
            wh.addWhere(IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange()), "AND");
            DatabaseConnector.runQuery("insert into " + tmpTable + " (callidid, started, ended)"
                    + "\nselect distinct callidid, min(time), max(time) from " + tab
                    + "\n" + wh.makeWhere(true)
                    + "\ngroup by 1;");

            DatabaseConnector.runQuery("create index idx_" + tmpTable + "connID on " + tmpTable + "(connectionidid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "started on " + tmpTable + "(started);");

            DatabaseConnector.runQuery("create index idx_" + tmpTable + "thisdnid on " + tmpTable + "(thisdnid);");

            DatabaseConnector.runQuery("create index idx_" + tmpTable + "otherdnid on " + tmpTable + "(otherdnid);");

            DatabaseConnector.runQuery("create index idx_" + tmpTable + "nameid on " + tmpTable + "(nameid);");
            TableQuery tabReport = new TableQuery(tmpTable);
            tabReport.addOutField("UTCtoDateTime(started, \"YYYY-MM-dd HH:mm:ss.SSS\") started");
            tabReport.addOutField("UTCtoDateTime(ended, \"YYYY-MM-dd HH:mm:ss.SSS\") ended");
            tabReport.addOutField("jduration(ended-started) duration ");
            tabReport.setAddAll(false);

            tabReport.addRef("connectionidid", "connectionid", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
            tabReport.addRef("callidid", "callid", ReferenceType.SIPCALLID.toString(), FieldType.OPTIONAL);
            tabReport.addRef("nameid", "\"First TEvent\"", ReferenceType.TEvent.toString(), FieldType.OPTIONAL);
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
        ret.addRecMap(FileInfoType.type_CallManager, new Pair<>(SelectionType.CONNID, "connectionid"));
        return ret;
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new String[]{"sipep", "wstlib"}, searchApps);
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return DatabaseConnector.getTimeRange(new String[]{"sipep", "wstlib"});
    }

    @Override
    public String getReportSummary() {
        return getName() + "\n\t" + "search: " + ((cidFinder != null) ? cidFinder.toString() : "<not specified>");
    }

    @Override
    public String getName() {
        return "Workspace/SIP EP";
    }

    /**
     *
     */
    public void addTLibReportType(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.TLIB_CALLS_TEVENT));
        root.addChild(nd);

//        DynamicTreeNode<OptionNode> ComponentNode;
//        ComponentNode = new DynamicTreeNode<OptionNode>(new OptionNode(EVENTS_REPORT));
//        nd.addChild(ComponentNode);
        /**
         * *****************
         */
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_NAME, ReferenceType.TEvent, "wstlib", "nameid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_DN, ReferenceType.DN, "wstlib", "thisdnid", "otherdnid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_SOURCE, ReferenceType.App, "wstlib", "sourceid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_AGENT, ReferenceType.Agent, "wstlib", "agentidid");

        DynamicTreeNode<OptionNode> AttrValue;

        DynamicTreeNode<OptionNode> Attr;
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventQueued"));      
//        Attr.addChild(AttrValue);
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventRouteRequested"));      
//        Attr.addChild(AttrValue);

        Attr = new DynamicTreeNode<>(new OptionNode(false, "CallType"));
        nd.addChild(Attr);
        AttrValue = new DynamicTreeNode<>(new OptionNode(false, "Inbound"));
        Attr.addChild(AttrValue);
        AttrValue = new DynamicTreeNode<>(new OptionNode(false, "Outbound"));
        Attr.addChild(AttrValue);

    }

    public void AddComponent(int filter) {
        m_componentFilter = m_componentFilter | filter;
    }

    private void addSIPReportType(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(true, DialogItem.TLIB_CALLS_SIP));
        root.addChild(nd);

        /**
         * *****************
         */
        nd.addDynamicRef(DialogItem.TLIB_CALLS_SIP_NAME, ReferenceType.SIPMETHOD);
        nd.addDynamicRef(DialogItem.TLIB_CALLS_SIP_ENDPOINT, ReferenceType.DN);
        nd.addDynamicRef(DialogItem.TLIB_CALLS_SIP_PEERIP, ReferenceType.IP);
        nd.addPairChildren(DialogItem.TLIB_CALLS_SIP_DIRECTION,
                new Pair[]{new Pair("inbound", "1"), new Pair("outbound", "0")});

//        nd.addDynamicRef(DialogItem.TLIB_CALLS_SIP_CALL_ID, ReferenceType.SIPCALLID);
//        DynamicTreeNode<OptionNode> AttrValue;
//
//        DynamicTreeNode<OptionNode> Attr;
//        Attr = new DynamicTreeNode<OptionNode>(new OptionNode(false, "CallType"));
//        nd.addChild(Attr);
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "Inbound"));
//        Attr.addChild(AttrValue);
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "Outbound"));
//        Attr.addChild(AttrValue);
    }

    //    private void addSIPReportType(DynamicTreeNode<OptionNode> root) {
//        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<OptionNode>(new OptionNode(true, "SIP"));
//        root.addChild(nd);
//        
//        DynamicTreeNode<OptionNode> ComponentNode;
//        ComponentNode = new DynamicTreeNode<OptionNode>(new OptionNode("Messages"));
//        nd.addChild(ComponentNode);
//        
//        /********************/
//        ComponentNode.addDynamicRef( "Name", ReferenceType.SIPMETHOD);
//        ComponentNode.addDynamicRef( "Call-ID", ReferenceType.SIPCALLID);
//        
//        DynamicTreeNode<OptionNode> AttrValue;
//        
//        DynamicTreeNode<OptionNode> Attr;
////        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventQueued"));      
////        Attr.addChild(AttrValue);
////        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventRouteRequested"));      
////        Attr.addChild(AttrValue);
//        
//        Attr = new DynamicTreeNode<OptionNode>(new OptionNode(false, "CallType"));
//        ComponentNode.addChild(Attr);        
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "Inbound"));      
//        Attr.addChild(AttrValue);
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "Outbound"));      
//        Attr.addChild(AttrValue);
//
//         
//    }
//
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

        DynamicTreeNode<OptionNode> ndISCC = new DynamicTreeNode<>(new OptionNode(DialogItem.TLIB_CALLS_ISCC));
        nd.addChild(ndISCC);
        ndISCC.addDynamicRef(DialogItem.TLIB_CALLS_ISCC_NAME, ReferenceType.ISCCEvent, "iscc_logbr", "nameid");

        nd.addChild(new DynamicTreeNode<OptionNode>(new OptionNode(DialogItem.TLIB_CALLS_TSCP)));
        nd.addChild(new DynamicTreeNode<OptionNode>(new OptionNode(DialogItem.TLIB_CALLS_JSON)));

        addSIPReportType(nd);
        try {
            addCustom(rootA, FileInfoType.type_WorkSpace);
            addCustom(rootA, FileInfoType.type_SIPEP);
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }
        addConfigUpdates(rootA);

        rootA.addLogMessagesReportType(TableType.MsgTServer);
        DoneSTDOptions();
    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        if (appsType == null) {
            appsType = getAppsType(FileInfoType.type_WorkSpace);
            appsType.addAll(getAppsType(FileInfoType.type_SIPEP));

        }
        return appsType;
    }

    void SetConfig(InquirerCfg cr) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Retrieve(QueryDialog dlg) throws SQLException {
//        if (dlg.getSearchApps() == null) {
//            inquirer.logger.info("No apps selected. Nothing to search in");
//            return;
//        }

        runSelectionQuery(dlg, dlg.getSelectionType(), new IDsFinder(dlg));

        getGenesysMessages(TableType.MsgTServer, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);

        doSort();
    }

    //    private void retrieveSIP
    private void doRetrieve(QueryDialog dlg, SelectionType selectionType, String selection, boolean isRegex) throws SQLException {
        ILogRecord record = null;
        IDsFinder cidFinder = null;

        if (selection != null && (selectionType == SelectionType.CONNID
                || selectionType == SelectionType.CALLID)) {
            cidFinder = new IDsFinder();
            if (!cidFinder.initSearch()) {
                inquirer.logger.info("No call ID found; returning");
            }
        }
    }

    private TableQuery MakeTLibReq() throws SQLException {
        TableQuery TLibReq = new TableQuery(MsgType.TLIB, "tlib_logbr");
        TLibReq.addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.MANDATORY);
        TLibReq.addRef("otherDNID", "otherDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("sourceID", "source", ReferenceType.App.toString(), FieldType.OPTIONAL);
        TLibReq.addNullField("ConnectionID");
        TLibReq.addNullField("TransferID");
        TLibReq.addNullField("SipId");
        TLibReq.addRef("agentidid", "agentid", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
//        addRef("ErrMessageID", "ErrMessage", ReferenceType.TLIBERROR.toString(), FieldType.Optional);
        TLibReq.addNullField("ErrMessageID");
        TLibReq.addNullField("ixnid");
        TLibReq.addNullField("SipId");
        TLibReq.addNullField("attr1");
        TLibReq.addNullField("attr2");
        TLibReq.addNullField("nodeid");

        return TLibReq;
    }

    //    private void doRetrieveDN(QueryDialog dlg, String selection, boolean isRegex)  throws SQLException {
//        Integer[] dnID = null;
//        Integer[] dnSIPID = null;
//        if (selection != null) {
//            dnID = locateDN(selection, ReferenceType.DN, isRegex);
//            dnSIPID = locateDN(selection, ReferenceType.DN, isRegex);
//        } else {
//            return;
//        }
//        if (dnSIPID != null) {
//            DynamicTreeNode<OptionNode> sipReportSettings = FindNode(repComponents.getRoot(),
//                    DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_SIP, null);
//            if (isChecked(sipReportSettings)) {
//                RetrieveSIPByDN(dlg, sipReportSettings, dnSIPID);
//            }
//        }
//        if (dnID != null) {
//            DynamicTreeNode<OptionNode> tlibReportSettings = FindNode(repComponents.getRoot(),
//                    DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, null);
//            if (isChecked(tlibReportSettings)) {
//                getRecords(new TlibByDnQuery(tlibReportSettings, dnID));
//            }
//        }
//
//    }
    private void RetrieveSIP(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(reportSettings)) {
            inquirer.logger.debug("SIP report");
            SIPEPQuery sipMsgsByCallid = new SIPEPQuery(reportSettings, cidFinder, "sipep");
            sipMsgsByCallid.setCommonParams(this, dlg);
            getRecords(sipMsgsByCallid);
        }
    }

    private void RetrieveTLib(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings)) {
            inquirer.logger.debug("TLib report");
            WSTLibQuery tlibQuery = new WSTLibQuery(eventsSettings, cidFinder);
            tlibQuery.setCommonParams(this, dlg);
            getRecords(tlibQuery);

            if (cidFinder != null && !inquirer.ee.isNoTLibRequest()) {
                Integer[] connIDs = cidFinder.getConnIDs();
                if (connIDs != null && connIDs.length > 0 && DatabaseConnector.TableExist(ReferenceType.TEvent.toString())) {
                    TableQuery TLibReq = MakeTLibReq();
                    TLibReq.addWhere(TLibReq.getTabAlias() + ".connectionidid<=0 and \n"
                                    + getWhere(TLibReq.getTabAlias() + ".nameid", ReferenceType.TEvent, RequestsToShow, false)
                                    + "\n and \n"
                                    + getWhere(TLibReq.getTabAlias() + ".referenceid",
                                    MakeQueryID("referenceid", "TLIB_logbr",
                                            getWhere("connectionidid", connIDs, true)
                                                    + "\n\tand referenceid>0"
                                                    + "\n\tand "
                                                    + getWhere("nameid", ReferenceType.TEvent, EventsToShow, false)), false),
                            "");
                    TLibReq.setCommonParams(this, dlg);

                    getRecords(TLibReq);

                }
            }
        }
    }

    private void runSelectionQuery(QueryDialog dlg, SelectionType selectionType, IDsFinder cidFinder) throws SQLException {
        this.cidFinder = cidFinder;
        if (selectionType != SelectionType.NO_SELECTION) {
            if (!cidFinder.initSearch()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            }
        }

//        doRetrieve(dlg, cidFinder);
        boolean runQueries = true;
        if (DatabaseConnector.TableExist("sipep")) {
            if (selectionType != SelectionType.NO_SELECTION && !cidFinder.anythingSIPRelated()) {
                int dialogResult = inquirer.showConfirmDialog(dlg, "No SIP calls found.\n"
                        + "Do you want to show all SIP messages?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
                if (dialogResult == JOptionPane.CANCEL_OPTION) {
                    return;
                }
                if (dialogResult == JOptionPane.NO_OPTION) {
                    runQueries = false;
                }
            }

            IQuery.reSetQueryNo();

            if (runQueries) {
                RetrieveSIP(dlg,
                        FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_SIP, null),
                        cidFinder);
            }
        }

        if (DatabaseConnector.TableExist("wstlib")) {
            boolean canRunTLib = true;
            if (cidFinder != null && cidFinder.getSearchType() != SelectionType.NO_SELECTION
                    && !cidFinder.AnythingTLibRelated()) {
                int dialogResult = inquirer.showConfirmDialog(dlg, "No TLib calls found.\n"
                        + "Do you want to show all TLib messages?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
                if (dialogResult == JOptionPane.CANCEL_OPTION) {
                    return;
                }
                if (dialogResult == JOptionPane.NO_OPTION) {
                    canRunTLib = false;
                }
            }

            if (canRunTLib) {
                RetrieveTLib(dlg,
                        FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, null),
                        cidFinder);

                boolean connIDFound = true;
//            if (cidFinder != null && cidFinder.getSearchType() != SelectionType.NO_SELECTION
//                    && !cidFinder.ConnIDFound()) {
//                int dialogResult = inquirer.showConfirmDialog(dlg, "No ConnID found.\n"
//                        + "Do you want to show all ISCC, TSCP messages?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
//                if (dialogResult == JOptionPane.CANCEL_OPTION) {
//                    return;
//                }
//                if (dialogResult == JOptionPane.NO_OPTION) {
//                    connIDFound = false;
//                }
//            }

            }
        }
        getCustom(FileInfoType.type_WorkSpace, dlg, repComponents.getRoot(), cidFinder, null);
        getCustom(FileInfoType.type_SIPEP, dlg, repComponents.getRoot(), cidFinder, null);

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

}
