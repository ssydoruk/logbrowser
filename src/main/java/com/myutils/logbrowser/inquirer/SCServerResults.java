package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

public class SCServerResults extends IQueryResults {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private int m_componentFilter;
    private ArrayList<NameID> appType = null;
    private IDsFinder cidFinder = null;

    /**
     *
     */
    public SCServerResults(QueryDialogSettings qdSettings) {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
    }

    public SCServerResults() {
        super();
        loadStdOptions();
//        repComponents.getRoot().addChild(addLogMessagesReportType(TableType.MsgTServer));
    }

    @Override
    public String getReportSummary() {
        return "SCServer \n\t" + (cidFinder != null ? "search: " + cidFinder : "");
    }

    @Override
    public String getSearchString() {
        if (cidFinder != null) {
            return cidFinder.getSelection(); //To change body of generated methods, choose Tools | Templates.
        } else {
            return super.getSearchString();
        }
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return refreshTimeRange(null);
    }

    @Override
    public String getName() {
        return "SCServer";
    }

    public void AddComponent(int filter) {
        m_componentFilter = m_componentFilter | filter;
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

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.SCS));
        rootA.addChild(nd);
        DynamicTreeNode<OptionNode> subNode = new DynamicTreeNode<>(new OptionNode(DialogItem.SCS_Application));
        nd.addChild(subNode);
        try {
            addCustom(rootA, FileInfoType.type_SCS);
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }

        subNode.addDynamicRef(DialogItem.SCS_Application_NAME, ReferenceType.App, "scs_appstatus", "theAppNameID");

        if (DatabaseConnector.TableEmptySilent(TableType.SCSClientLogMessage.toString())) {
            nd = new DynamicTreeNode<>(new OptionNode(DialogItem.SCSClientLog));
            rootA.addChild(nd);
            subNode = new DynamicTreeNode<>(new OptionNode(DialogItem.SCSClientLog_params));
            nd.addChild(subNode);
        }

        if (DatabaseConnector.TableEmptySilent(TableType.SCSAlarm.toString())) {
            nd = new DynamicTreeNode<>(new OptionNode(DialogItem.SCSALARMS));
            rootA.addChild(nd);
            subNode = new DynamicTreeNode<>(new OptionNode(DialogItem.SCSALARMS_params));
            nd.addChild(subNode);
            subNode.addDynamicRef(DialogItem.SCSALARMS_params_ALARMNAME, ReferenceType.Misc, TableType.SCSAlarm.toString(), "alarmNameID");
            subNode.addDynamicRef(DialogItem.SCSALARMS_params_APPNAME, ReferenceType.App, TableType.SCSAlarm.toString(), "logAppNameID");
            subNode.addDynamicRef(DialogItem.SCSALARMS_params_CATEGORY, ReferenceType.Misc, TableType.SCSAlarm.toString(), "categoryID");
            subNode.addDynamicRef(DialogItem.SCSALARMS_params_ALARMSTATE, ReferenceType.AlarmState, TableType.SCSAlarm.toString(), "alarmStateID");
        }

        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.SCSSCS));
        rootA.addChild(nd);

        addConfigUpdates(rootA);
        rootA.addLogMessagesReportType(TableType.MsgSCServer);
        DoneSTDOptions();

    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        if (appType == null) {
            appType = getAppsType(FileInfoType.type_SCS);
        }
        return appType;
    }

    void SetConfig(InquirerCfg cr) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Retrieve(QueryDialog dlg) throws SQLException {
        SelectionType selectionType = dlg.getSelectionType();
        String selection = dlg.getSelection();

        if (dlg.getSearchApps() == null) {
            inquirer.logger.info("No apps selected. Nothing to search in");
            return;
        }

        if (selectionType != SelectionType.NO_SELECTION) {
            cidFinder = new IDsFinder(dlg);
            if (!cidFinder.initSCS()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            }
        } else {
            cidFinder = null;
        }

//        doRetrieve(dlg, cidFinder);
        retrieveApps(dlg,
                FindNode(repComponents.getRoot(), DialogItem.SCS, null, null),
                cidFinder);

        retrieveSelf(dlg,
                FindNode(repComponents.getRoot(), DialogItem.SCSSCS, null, null),
                cidFinder);

        retrieveClientLog(dlg,
                FindNode(repComponents.getRoot(), DialogItem.SCSClientLog, DialogItem.SCSClientLog_params, null),
                cidFinder);
        retrieveAlarms(dlg,
                FindNode(repComponents.getRoot(), DialogItem.SCSALARMS, DialogItem.SCSALARMS_params, null),
                cidFinder);

        getCustom(FileInfoType.type_SCS, dlg, repComponents.getRoot(), cidFinder, null);
//        getGenesysMessages(TableType.MsgSCServer, repComponents.getRoot(), dlg.getTimeRange(), dlg.getSearchApps());
        getGenesysMessages(TableType.MsgSCServer, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);
        Sort();
    }

    //    private void retrieveSIP
    private void doRetrieve(QueryDialog dlg, SelectionType selectionType, String selection, boolean isRegex) throws SQLException {
        ILogRecord record = null;

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

    private void retrieveApps(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
//<editor-fold defaultstate="collapsed" desc="OCSCG_PASESSIONINFO">
        if (isChecked(reportSettings) && DatabaseConnector.TableExist("SCS_appStatus")) {
            tellProgress("SCS application statuses");
            TableQuery SCSAppStatuses = new TableQuery(MsgType.SCS_APP, "SCS_appStatus");

            SCSAppStatuses.addRef("newModeID", "newMode", ReferenceType.APPRunMode.toString(), FieldType.OPTIONAL);
            SCSAppStatuses.addRef("theAppNameID", "appName", ReferenceType.App.toString(), FieldType.OPTIONAL);
            SCSAppStatuses.addRef("oldModeID", "oldMode", ReferenceType.APPRunMode.toString(), FieldType.OPTIONAL);
            SCSAppStatuses.addRef("statusID", "status", ReferenceType.appStatus.toString(), FieldType.OPTIONAL);
            SCSAppStatuses.addRef("hostID", "host", ReferenceType.Host.toString(), FieldType.OPTIONAL);
            SCSAppStatuses.addRef("eventID", "event", ReferenceType.SCSEvent.toString(), FieldType.OPTIONAL);

            SCSAppStatuses.AddCheckedWhere(SCSAppStatuses.getTabAlias() + ".theAppNameID",
                    ReferenceType.App,
                    FindNode(reportSettings, DialogItem.SCS_Application, null, null),
                    "AND",
                    DialogItem.SCS_Application_NAME);

            SCSAppStatuses.setCommonParams(this, dlg);
            getRecords(SCSAppStatuses);
        }
//</editor-fold>

    }

    private void retrieveClientLog(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(reportSettings) && DatabaseConnector.TableExist(TableType.SCSClientLogMessage.toString())) {
            tellProgress("SCS application actions");
            TableQuery scsClientLogs = new TableQuery(MsgType.SCS_CLIENT_LOG, TableType.SCSClientLogMessage.toString());

            scsClientLogs.addRef("messageTextID", "messageText", ReferenceType.Misc.toString(), FieldType.OPTIONAL);

            scsClientLogs.setCommonParams(this, dlg);
            getRecords(scsClientLogs);
        }

    }

    private void retrieveAlarms(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(reportSettings) && DatabaseConnector.TableExist(TableType.SCSAlarm.toString())) {
            tellProgress("SCS alarms");
            TableQuery scsAlarms = new TableQuery(MsgType.SCS_ALARMS, TableType.SCSAlarm.toString());

            scsAlarms.addRef("alarmNameID", "alarmName", ReferenceType.Misc.toString(), FieldType.OPTIONAL);
            scsAlarms.addRef("logAppNameID", "logAppName", ReferenceType.App.toString(), FieldType.OPTIONAL);
            scsAlarms.addRef("reasonID", "reason", ReferenceType.Misc.toString(), FieldType.OPTIONAL);
            scsAlarms.addRef("categoryID", "category", ReferenceType.Misc.toString(), FieldType.OPTIONAL);
            scsAlarms.addRef("alarmIDID", "alarmID", ReferenceType.Misc.toString(), FieldType.OPTIONAL);
            scsAlarms.addRef("msgTextID", "msgText", ReferenceType.Misc.toString(), FieldType.OPTIONAL);
            scsAlarms.addRef("alarmStateID", "alarmState", ReferenceType.AlarmState.toString(), FieldType.OPTIONAL);

            scsAlarms.AddCheckedWhere(scsAlarms.getTabAlias() + ".alarmNameID",
                    ReferenceType.Misc,
                    reportSettings,
                    "AND",
                    DialogItem.SCSALARMS_params_ALARMNAME);
            scsAlarms.AddCheckedWhere(scsAlarms.getTabAlias() + ".logAppNameID",
                    ReferenceType.App,
                    reportSettings,
                    "AND",
                    DialogItem.SCSALARMS_params_APPNAME);
            scsAlarms.AddCheckedWhere(scsAlarms.getTabAlias() + ".categoryID",
                    ReferenceType.Misc,
                    reportSettings,
                    "AND",
                    DialogItem.SCSALARMS_params_CATEGORY);
            scsAlarms.AddCheckedWhere(scsAlarms.getTabAlias() + ".alarmStateID",
                    ReferenceType.AlarmState,
                    reportSettings,
                    "AND",
                    DialogItem.SCSALARMS_params_ALARMSTATE);

            scsAlarms.setCommonParams(this, dlg);
            getRecords(scsAlarms);
        }

    }

    private void retrieveSelf(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(reportSettings) && DatabaseConnector.TableExist("SCS_SelfStatus")) {
            tellProgress("SCS application actions");
            TableQuery SCSAppStatuses = new TableQuery(MsgType.SCS_SCS, "SCS_SelfStatus");

            SCSAppStatuses.addRef("newModeID", "newMode", ReferenceType.APPRunMode.toString(), FieldType.OPTIONAL);
            SCSAppStatuses.addRef("oldModeID", "oldMode", ReferenceType.APPRunMode.toString(), FieldType.OPTIONAL);

            SCSAppStatuses.setCommonParams(this, dlg);
            getRecords(SCSAppStatuses);
        }

    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new String[]{"SCS_appStatus", TableType.MsgSCServer.toString()}, searchApps);
    }

    @Override
    public void Retrieve(QueryDialog qd, SelectionType key, String searchID) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public AllProcSettings getAllProc(Window parent) {
        return new AllProcSettings((qd, settings) -> getAll(qd, settings), null);
    }

    FullTableColors getAll(QueryDialog qd, AllInteractionsSettings settings) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void showAllResults() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    SearchFields getSearchField() {
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

}
