package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBServerResults extends IQueryResults {

    private int m_componentFilter;
    private ArrayList<NameID> appsType;
    private IDsFinder cidFinder = null;

    /**
     *
     */
    public DBServerResults(QueryDialogSettings qdSettings) throws SQLException {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
    }

    public DBServerResults() throws SQLException {
        super();
        loadStdOptions();
//        repComponents.getRoot().addChild(addLogMessagesReportType(TableType.MsgTServer));
    }

    @Override
    public String getReportSummary() {
        return "DBServer \n\t" + (cidFinder != null ? "search: " + cidFinder : "");
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
        return "DBServer";
    }

    public void AddComponent(int filter) {
        m_componentFilter = m_componentFilter | filter;
    }

    private void loadStdOptions() throws SQLException {
        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        //        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.SCS));
//        rootA.addChild(nd);
//        DynamicTreeNode<OptionNode> app = new DynamicTreeNode<>(new OptionNode(DialogItem.SCS_Application));
//        nd.addChild(app);
//        try {
//            addCustom(rootA, FileInfoType.type_SCS);
//        } catch (Exception ex) {
//            logger.error("fatal: ",  ex);
//        }
//
//        app.addDynamicRef(DialogItem.SCS_Application_NAME, ReferenceType.App, "scs_appstatus", "theAppNameID");
//
//        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.SCSSCS));
//        rootA.addChild(nd);
        addCustom(rootA, FileInfoType.type_DBServer);

        addConfigUpdates(rootA);
        rootA.addLogMessagesReportType(TableType.MsgDBServer);
        DoneSTDOptions(getQdSettings().getSavedOptions().get(this.getName()));

    }

    @Override
    public ArrayList<NameID> getApps() throws Exception {
        if (appsType == null) {
            appsType = getAppsType(FileInfoType.type_DBServer);
        }
        return appsType;
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
        if (cidFinder != null && cidFinder.getSearchType() == SelectionType.CALLID) {
            inquirer.logger.info("No TLib events for CallID search without show related");
        } else {
            retrieveApps(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.SCS, null, null),
                    cidFinder);

            retrieveSelf(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.SCSSCS, null, null),
                    cidFinder);
        }

        getCustom(FileInfoType.type_DBServer, dlg, repComponents.getRoot(), cidFinder, null);
        getGenesysMessages(TableType.MsgDBServer, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);
        Sort();
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
        return DatabaseConnector.getTimeRange(new FileInfoType[]{FileInfoType.type_DBServer}, searchApps);
    }

    @Override
    public void Retrieve(QueryDialog qd, SelectionType key, String searchID) {
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
