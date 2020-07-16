package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import static com.myutils.logbrowser.inquirer.DatabaseConnector.TableExist;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import static com.myutils.logbrowser.inquirer.QueryTools.FindNode;
import static com.myutils.logbrowser.inquirer.QueryTools.getWhere;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

public class MediaServerResults extends IQueryResults {

    public static final int TLIB = 0x01;
    public static final int ISCC = 0x02;
    public static final int TC = 0x04;
    public static final int SIP = 0x08;
    public static final int PROXY = 0x10;

    private Object cidFinder;
    ArrayList<NameID> appsType = null;
    private UTCTimeRange timeRange = null;

    /**
     *
     */
    public MediaServerResults(QueryDialogSettings qdSettings) throws SQLException {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.CALLID);
        addSelectionType(SelectionType.MCP_CALLID);
        addSelectionType(SelectionType.DN);
        addSelectionType(SelectionType.PEERIP);
        addSelectionType(SelectionType.AGENTID);
        addSelectionType(SelectionType.UUID);
    }

    public MediaServerResults() throws SQLException {
        super();
        loadStdOptions();
//        repComponents.getRoot().addChild(addLogMessagesReportType(TableType.MsgTServer));
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
            tellProgress("Building temp tables");
            String tab = "sipms";

            DatabaseConnector.runQuery("create temp table " + tmpTable + " ("
                    + "callidid int"
                    + ",started timestamp"
                    + ",ended timestamp"
                    + ",FromUriID int"
                    + ",ToUriID int"
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
            wh.addWhere(getWhere("sipms.nameID", ReferenceType.SIPMETHOD, new String[]{"OPTIONS", "200 OK OPTIONS",
                "NOTIFY", "200 OK NOTIFY", "503 Service Unavailable OPTIONS"}, false, false, true), "AND");

            DatabaseConnector.runQuery("insert into " + tmpTable + " (callidid, started, ended)"
                    + "\nselect distinct callidid, min(time), max(time) from " + tab
                    + "\n" + wh.makeWhere(true)
                    + "\ngroup by 1;");

            DatabaseConnector.runQuery("create index idx_" + tmpTable + "callidid on " + tmpTable + "(callidid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "started on " + tmpTable + "(started);");

            DatabaseConnector.runQuery(""
                    + "update " + tmpTable
                    + "\nset (ToUriID, FromUriID)  = (select "
                    + "ToUriID, FromUriID"
                    + "\nfrom sipms a "
                    + "\nwhere " + tmpTable + ".callIDID=a.callIDID\n"
                    + "and " + tmpTable + ".started=a.time\n"
                    + ")"
                    + ";");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "ToUriID on " + tmpTable + "(ToUriID);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "FromUriID on " + tmpTable + "(FromUriID);");

            TableQuery tabReport = new TableQuery(tmpTable);
            tabReport.addOutField("UTCtoDateTime(started, \"YYYY-MM-dd HH:mm:ss.SSS\") started");
            tabReport.addOutField("UTCtoDateTime(ended, \"YYYY-MM-dd HH:mm:ss.SSS\") ended");
            tabReport.addOutField("jduration(ended-started) duration ");
            tabReport.setAddAll(false);

            tabReport.addRef("callidid", "callid", ReferenceType.SIPCALLID.toString(), FieldType.Optional);

            tabReport.addRef("ToUriID", "ToUri", ReferenceType.SIPURI.toString(), FieldType.Optional);
            tabReport.addRef("FromUriID", "FromUri", ReferenceType.SIPURI.toString(), FieldType.Optional);

            tabReport.setOrderBy(tabReport.getTabAlias() + ".started");
            FullTableColors currTable = tabReport.getFullTable();

            return currTable; //To change body of generated methods, choose Tools | Templates.
        } finally {
            DynamicTreeNode.setNoRefNoLoad(false);

        }
    }

    @Override
    SearchFields getSearchField() {
        SearchFields ret = new SearchFields();
        ret.addRecMap(FileInfoType.type_RM, new Pair<>(SelectionType.CALLID, "callid"));
        return ret;
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {

        return DatabaseConnector.getTimeRange(new FileInfoType[]{FileInfoType.type_MCP, FileInfoType.type_RM}, searchApps);

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
        return "Media Server";
    }

    private void addSIPReportType(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(true, DialogItem.TLIB_CALLS_SIP));
        root.addChild(nd);

        nd.addDynamicRef(DialogItem.TLIB_CALLS_SIP_NAME, ReferenceType.SIPMETHOD);
        nd.addDynamicRef(DialogItem.TLIB_CALLS_SIP_ENDPOINT, ReferenceType.DN);
        nd.addDynamicRef(DialogItem.TLIB_CALLS_SIP_PEERIP, ReferenceType.IP);
        nd.addPairChildren(DialogItem.TLIB_CALLS_SIP_DIRECTION,
                new Pair[]{new Pair("inbound", "1"), new Pair("outbound", "0")});

        nd = new DynamicTreeNode<>(new OptionNode(true, DialogItem.MCP_STRATEGY));
        root.addChild(nd);
        nd.addDynamicRef(DialogItem.MCP_STRATEGY_STEPS, ReferenceType.VXMLCommand);
        nd.addDynamicRef(DialogItem.MCP_STRATEGY_STEPSPARAMS, ReferenceType.VXMLCommandParams);
//                strategySteps.addRef("commandId", "command", ReferenceType.VXMLCommand.toString(), IQuery.FieldType.Mandatory);
//                strategySteps.addRef("paramsID", "params", ReferenceType.VXMLCommandParams.toString(), IQuery.FieldType.Optional);

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

    private void loadStdOptions() throws SQLException {
        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.TLIB_CALLS));
        rootA.addChild(nd);

        addSIPReportType(nd);

        addConfigUpdates(rootA);
        addCustom(rootA, FileInfoType.type_MCP);
        addCustom(rootA, FileInfoType.type_RM);
        if (!DatabaseConnector.TableEmptySilent(TableType.MsgMCP.toString())) {
            rootA.addLogMessagesReportType(TableType.MsgMCP, "MCP log messages");
        }
        if (!DatabaseConnector.TableEmptySilent(TableType.MsgRM.toString())) {
            rootA.addLogMessagesReportType(TableType.MsgRM, "RM log messages");

        }
        DoneSTDOptions();
    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        if (appsType == null) {
            appsType = getAppsType(FileInfoType.type_MCP);
            appsType.addAll(getAppsType(FileInfoType.type_RM));

        }
        return appsType;
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
    }

    private void RetrieveSIP(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        SipMSQuery sipMsgsByCallid = new SipMSQuery(reportSettings, cidFinder, dlg, this);
        if (sipMsgsByCallid.isShouldRun()) {
            inquirer.logger.debug("SIP report");
            getRecords(sipMsgsByCallid);
        }
    }

    private void runSelectionQuery(QueryDialog dlg, SelectionType selectionType, IDsFinder cidFinder) throws SQLException {
        this.cidFinder = cidFinder;
        if (selectionType != SelectionType.NO_SELECTION) {
            if (!cidFinder.initMediaServer()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            } else {
                setObjectsFound(cidFinder.getObjectsFound());
            }
        }

        IQuery.reSetQueryNo();

        RetrieveSIP(dlg,
                FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_SIP, null),
                cidFinder);
        RetrieveStrategySteps(dlg,
                FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.MCP_STRATEGY, null),
                cidFinder);

        getCustom(FileInfoType.type_RM, dlg, repComponents.getRoot(), cidFinder);
        getCustom(FileInfoType.type_MCP, dlg, repComponents.getRoot(), cidFinder);
        getGenesysMessages(TableType.MsgRM, repComponents.getRoot(), dlg, this);
        getGenesysMessages(TableType.MsgMCP, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);

        if (inquirer.getCr().isSorting()) {
            Sort();
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

    @Override
    void showAllResults() {
        showAllResults(getAllResults); //To change body of generated methods, choose Tools | Templates.

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

    private void RetrieveStrategySteps(QueryDialog dlg, DynamicTreeNode<OptionNode> strategyStepsSettings, IDsFinder cidFinder) throws SQLException {

        if (isChecked(strategyStepsSettings) && TableExist(TableType.VXMLIntStepsTable.toString())) {
            Integer[] mcpCallID = null;
            if (((dlg.getSelectionType() != SelectionType.NO_SELECTION) ? (mcpCallID = cidFinder.getIDs(IDType.MCPCallID)) != null : true)) {
                TableQuery strategySteps = new TableQuery(MsgType.VXMLStrategySteps, TableType.VXMLIntStepsTable.toString());
                tellProgress("Retrieving VXMLStrategy steps");
                strategySteps.addRef("commandId", "command", ReferenceType.VXMLCommand.toString(), IQuery.FieldType.Mandatory);
                strategySteps.addRef("paramsID", "params", ReferenceType.VXMLCommandParams.toString(), IQuery.FieldType.Optional);
                strategySteps.addRef("mcpCallIDID", "mcpCall", ReferenceType.VXMLMCPCallID.toString(), IQuery.FieldType.Optional);
                strategySteps.addRef("SIPCallID", "SIPCall", ReferenceType.SIPCALLID.toString(), IQuery.FieldType.Optional);
                strategySteps.addRef("GVPSessionID", "GVPSession", ReferenceType.GVPSessionID.toString(), IQuery.FieldType.Optional);
                strategySteps.addRef("TenantID", "Tenant", ReferenceType.Tenant.toString(), IQuery.FieldType.Optional);
                strategySteps.addRef("IVRProfileID", "IVRProfile", ReferenceType.GVPIVRProfile.toString(), IQuery.FieldType.Optional);

//                UrsStrategy.setSearchApps(searchApps);
                if (mcpCallID != null) {
                    strategySteps.addWhere(getWhere("mcpCallIDID", mcpCallID, false), "AND");
                }

                strategySteps.AddCheckedWhere(strategySteps.getTabAlias() + ".commandId",
                        ReferenceType.VXMLCommand,
                        strategyStepsSettings,
                        "AND",
                        DialogItem.MCP_STRATEGY_STEPS);
                strategySteps.setCommonParams(this, dlg);

                getRecords(strategySteps);
            }
        }
    }

}
