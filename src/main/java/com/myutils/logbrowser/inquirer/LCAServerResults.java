package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

public class LCAServerResults extends IQueryResults {

    private int m_componentFilter;
    private ArrayList<NameID> appType = null;
    private IDsFinder cidFinder = null;
    /**
     *
     */
    public LCAServerResults(QueryDialogSettings qdSettings) throws SQLException {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
//        addSelectionType(SelectionType.GUESS_SELECTION);
    }
    public LCAServerResults() throws SQLException {
        super();
        loadStdOptions();
//        repComponents.getRoot().addChild(addLogMessagesReportType(TableType.MsgTServer));
    }

    @Override
    public String getReportSummary() {
        return "LCAServer \n\t" + (cidFinder != null ? "search: " + cidFinder.toString() : "");
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
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new FileInfoType[]{FileInfoType.type_LCA}, searchApps);
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return refreshTimeRange(null);
    }

    @Override
    public String getName() {
        return "LCAServer";
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

    private void loadStdOptions() throws SQLException {
        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.SCS));
        rootA.addChild(nd);
        DynamicTreeNode<OptionNode> app = new DynamicTreeNode<>(new OptionNode(DialogItem.SCS_Application));
        nd.addChild(app);
        addCustom(rootA, FileInfoType.type_LCA);

        app.addDynamicRef(DialogItem.SCS_Application_NAME, ReferenceType.App, "scs_appstatus", "theAppNameID");

        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.LCAClient));
        rootA.addChild(nd);

        rootA.addLogMessagesReportType(TableType.MsgLCAServer);
        DoneSTDOptions();

    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        if (appType == null) {
            appType = getAppsType(FileInfoType.type_LCA);
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
        if (cidFinder != null && cidFinder.getSearchType() == SelectionType.CALLID) {
            inquirer.logger.info("No TLib events for CallID search without show related");
        } else {
            retrieveApps(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.SCS, null, null),
                    cidFinder);

            retrieveClient(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.LCAClient, null, null),
                    cidFinder);

        }

        getCustom(FileInfoType.type_LCA, dlg, repComponents.getRoot(), cidFinder, null);
        getGenesysMessages(TableType.MsgLCAServer, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);
        Sort();
    }

    private void retrieveApps(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(reportSettings) && DatabaseConnector.TableExist(TableType.LCAAppStatus.toString())) {
            tellProgress("LCA application statuses");
            TableQuery LCAAppStatuses = new TableQuery(MsgType.LCA_APP, TableType.LCAAppStatus.toString());

            LCAAppStatuses.addRef("newModeID", "newMode", ReferenceType.APPRunMode.toString(), FieldType.Optional);
            LCAAppStatuses.addRef("theAppNameID", "appName", ReferenceType.App.toString(), FieldType.Optional);
            LCAAppStatuses.addRef("statusID", "status", ReferenceType.appStatus.toString(), FieldType.Optional);
            LCAAppStatuses.addRef("eventID", "event", ReferenceType.SCSEvent.toString(), FieldType.Optional);
            LCAAppStatuses.addRef("oldModeID", "oldmode", ReferenceType.APPRunMode.toString(), FieldType.Optional);
            LCAAppStatuses.addRef("requestorID", "reqSCS", ReferenceType.App.toString(), FieldType.Optional);

            LCAAppStatuses.AddCheckedWhere(LCAAppStatuses.getTabAlias() + ".theAppNameID",
                    ReferenceType.App,
                    FindNode(reportSettings, DialogItem.SCS_Application, null, null),
                    "AND",
                    DialogItem.SCS_Application_NAME);

            LCAAppStatuses.setCommonParams(this, dlg);
            getRecords(LCAAppStatuses);
        }

    }

    private void retrieveClient(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(reportSettings) && DatabaseConnector.TableExist(TableType.LCAClient.toString())) {
            tellProgress("LCA application statuses");
            TableQuery LCAAppStatuses = new TableQuery(MsgType.LCA_CLIENT, TableType.LCAClient.toString());

            LCAAppStatuses.addRef("theAppNameID", "connectedApp", ReferenceType.App.toString(), FieldType.Optional);
            LCAAppStatuses.addRef("typeID", "appType", ReferenceType.AppType.toString(), FieldType.Optional);
            LCAAppStatuses.addRef("IPID", "IP", ReferenceType.IP.toString(), FieldType.Optional);
            LCAAppStatuses.addOutField("connectedName(isConnected) isConnectedName");

            LCAAppStatuses.AddCheckedWhere(LCAAppStatuses.getTabAlias() + ".theAppNameID",
                    ReferenceType.App,
                    FindNode(reportSettings, DialogItem.SCS_Application, null, null),
                    "AND",
                    DialogItem.SCS_Application_NAME);

            LCAAppStatuses.setCommonParams(this, dlg);
            getRecords(LCAAppStatuses);
        }

    }

    @Override
    public void Retrieve(QueryDialog qd, SelectionType key, String searchID) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    FullTableColors getAll(QueryDialog qd) throws SQLException {
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
