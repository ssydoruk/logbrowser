package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

import static com.myutils.logbrowser.inquirer.DatabaseConnector.TableExist;

public class MediaServerResults extends IQueryResults {

    public static final int TLIB = 0x01;
    public static final int ISCC = 0x02;
    public static final int TC = 0x04;
    public static final int SIP = 0x08;
    public static final int PROXY = 0x10;
    ArrayList<NameID> appsType = null;
    private Object cidFinder;

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


    @Override
    public void Retrieve(QueryDialog dlg, SelectionType key, String searchID) throws SQLException {
        runSelectionQuery(dlg, key, new IDsFinder(dlg, key, searchID));
        doSort();

    }

    private static final String C_CREATE_IDX="create index idx_";

    @Override
    FullTableColors getAll(QueryDialog qd, Component c, int x, int y)  throws SQLException {
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


            Wheres wh = new Wheres();
            wh.addWhere(IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false)), "AND");
            wh.addWhere(IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange()), "AND");
            wh.addWhere(getWhere("sipms.nameID", ReferenceType.SIPMETHOD, new String[]{"OPTIONS", "200 OK OPTIONS",
                    "NOTIFY", "200 OK NOTIFY", "503 Service Unavailable OPTIONS"}, false, false, true), "AND");

            DatabaseConnector.runQuery("insert into " + tmpTable + " (callidid, started, ended)"
                    + "\nselect distinct callidid, min(time), max(time) from " + tab
                    + "\n" + wh.makeWhere(true)
                    + "\ngroup by 1;");

            DatabaseConnector.runQuery(C_CREATE_IDX + tmpTable + "callidid on " + tmpTable + "(callidid);");
            DatabaseConnector.runQuery(C_CREATE_IDX + tmpTable + "started on " + tmpTable + "(started);");

            DatabaseConnector.runQuery("update " + tmpTable
                    + "\nset (ToUriID, FromUriID)  = (select "
                    + "ToUriID, FromUriID"
                    + "\nfrom sipms a "
                    + "\nwhere " + tmpTable + ".callIDID=a.callIDID\n"
                    + "and " + tmpTable + ".started=a.time\n"
                    + ")"
                    + ";");
            DatabaseConnector.runQuery(C_CREATE_IDX + tmpTable + "ToUriID on " + tmpTable + "(ToUriID);");
            DatabaseConnector.runQuery(C_CREATE_IDX + tmpTable + "FromUriID on " + tmpTable + "(FromUriID);");

            TableQuery tabReport = new TableQuery(tmpTable);
            tabReport.addOutField("UTCtoDateTime(started, \"YYYY-MM-dd HH:mm:ss.SSS\") started");
            tabReport.addOutField("UTCtoDateTime(ended, \"YYYY-MM-dd HH:mm:ss.SSS\") ended");
            tabReport.addOutField("jduration(ended-started) duration ");
            tabReport.setAddAll(false);

            tabReport.addRef("callidid", "callid", ReferenceType.SIPCALLID.toString(), FieldType.OPTIONAL);

            tabReport.addRef("ToUriID", "ToUri", ReferenceType.SIPURI.toString(), FieldType.OPTIONAL);
            tabReport.addRef("FromUriID", "FromUri", ReferenceType.SIPURI.toString(), FieldType.OPTIONAL);

            tabReport.setOrderBy(tabReport.getTabAlias() + ".started");

            return tabReport.getFullTable(); //To change body of generated methods, choose Tools | Templates.
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
                new Pair[]{new Pair<>("inbound", "1"), new Pair<>("outbound", "0")});

        nd = new DynamicTreeNode<>(new OptionNode(true, DialogItem.MCP_STRATEGY));
        root.addChild(nd);
        nd.addDynamicRef(DialogItem.MCP_STRATEGY_STEPS, ReferenceType.VXMLCommand);
        nd.addDynamicRef(DialogItem.MCP_STRATEGY_STEPSPARAMS1, ReferenceType.VXMLCommandParams, TableType.VXMLIntStepsTable.toString(), "param1ID");
        nd.addDynamicRef(DialogItem.MCP_STRATEGY_STEPSPARAMS2, ReferenceType.VXMLCommandParams, TableType.VXMLIntStepsTable.toString(), "param2ID");

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

    }

    private void RetrieveSIP(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if( (isChecked(reportSettings) && TableExist("SIPMS")) &&
            (dlg.getSelectionType() == SelectionType.NO_SELECTION
                    || cidFinder.getIDs(IDType.SIPCallID) != null || cidFinder.getIDs(IDType.PEERIP) != null)) {
                SipMSQuery sipMsgsByCallid = new SipMSQuery(reportSettings, cidFinder, dlg, this);
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
        /* */
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        /* */
    }

    @Override
    protected ArrayList<IAggregateQuery> loadReportAggregates() {
        return null;
    }

    @Override
    boolean callRelatedSearch(IDsFinder cidFinder) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void RetrieveStrategySteps(QueryDialog dlg, DynamicTreeNode<OptionNode> strategyStepsSettings, IDsFinder cidFinder) throws SQLException {

        if (isChecked(strategyStepsSettings) && TableExist(TableType.VXMLIntStepsTable.toString())) {
            Integer[] mcpCallID = null;
            if ((dlg.getSelectionType() == SelectionType.NO_SELECTION || (mcpCallID = cidFinder.getIDs(IDType.MCPCallID)) != null)) {
                TableQuery strategySteps = new TableQuery(MsgType.VXMLStrategySteps, TableType.VXMLIntStepsTable.toString());
                tellProgress("Retrieving VXMLStrategy steps");
                strategySteps.addRef("commandId", "command", ReferenceType.VXMLCommand.toString(), FieldType.MANDATORY);
                strategySteps.addRef("param1ID", "param1", ReferenceType.VXMLCommandParams.toString(), FieldType.OPTIONAL);
                strategySteps.addRef("param2ID", "param2", ReferenceType.VXMLCommandParams.toString(), FieldType.OPTIONAL);

                strategySteps.addRef("mcpCallIDID", "mcpCall", ReferenceType.VXMLMCPCallID.toString(), FieldType.OPTIONAL);
                strategySteps.addRef("SIPCallID", "SIPCall", ReferenceType.SIPCALLID.toString(), FieldType.OPTIONAL);
                strategySteps.addRef("GVPSessionID", "GVPSession", ReferenceType.GVPSessionID.toString(), FieldType.OPTIONAL);
                strategySteps.addRef("TenantID", "Tenant", ReferenceType.Tenant.toString(), FieldType.OPTIONAL);
                strategySteps.addRef("IVRProfileID", "IVRProfile", ReferenceType.GVPIVRProfile.toString(), FieldType.OPTIONAL);

                if (mcpCallID != null) {
                    strategySteps.addWhere(getWhere("mcpCallIDID", mcpCallID, false), "AND");
                }

                strategySteps.AddCheckedWhere(strategySteps.getTabAlias() + ".commandId",
                        ReferenceType.VXMLCommand,
                        strategyStepsSettings,
                        "AND",
                        DialogItem.MCP_STRATEGY_STEPS);

                strategySteps.AddCheckedWhere(strategySteps.getTabAlias() + ".param1ID",
                        ReferenceType.VXMLCommandParams,
                        strategyStepsSettings,
                        "AND",
                        DialogItem.MCP_STRATEGY_STEPSPARAMS1);

                strategySteps.AddCheckedWhere(strategySteps.getTabAlias() + ".param2ID",
                        ReferenceType.VXMLCommandParams,
                        strategyStepsSettings,
                        "AND",
                        DialogItem.MCP_STRATEGY_STEPSPARAMS2);

                strategySteps.setCommonParams(this, dlg);

                getRecords(strategySteps);
            }
        }
    }

}
