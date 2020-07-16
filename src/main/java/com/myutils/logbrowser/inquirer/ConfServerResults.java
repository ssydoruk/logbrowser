package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import Utils.Util;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import com.myutils.logbrowser.inquirer.IQuery.IRecordLoadedProc;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

public class ConfServerResults extends IQueryResults {

    private String m_tlibFilter;

    private int m_componentFilter;
    private ArrayList<NameID> appsType;
    private IDsFinder cidFinder = null;

    /**
     *
     */
    public ConfServerResults(QueryDialogSettings qdSettings) throws SQLException {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.AGENT);

    }

    public ConfServerResults() throws SQLException {
        super();
        loadStdOptions();
//        repComponents.getRoot().addChild(addLogMessagesReportType(TableType.MsgTServer));
    }

    @Override
    public String getReportSummary() {
        return "ConfServer \n\t" + (cidFinder != null ? "search: " + cidFinder.toString() : "");
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
        return "ConfServer";
    }

    public void AddComponent(int filter) {
        m_componentFilter = m_componentFilter | filter;
    }

    public void SetConfig(Properties config) {
        if (config != null && !config.isEmpty()) {
            m_tlibFilter = config.getProperty("TlibFilter");
        }
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

//<editor-fold defaultstate="collapsed" desc="requests">
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.CONFSERV_REQUESTS));
        rootA.addChild(nd);
        DynamicTreeNode<OptionNode> ndParams = new DynamicTreeNode<>(new OptionNode(DialogItem.CONFSERV_REQUESTS));
        nd.addChild(ndParams);

//        ndParams.addDynamicRef(DialogItem.CONFSERV_REQUESTS_OBJTYPE, ReferenceType.CfgObjName, TableType.CSUpdate.toString(), "objNameID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_REQUESTS_OBJTYPE, ReferenceType.ObjectType, TableType.CSUpdate.toString(), "objTypeID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_REQUESTS_APP, ReferenceType.App, TableType.CSUpdate.toString(), "theAppNameID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_REQUESTS_USER, ReferenceType.Agent, TableType.CSUpdate.toString(), "userNameID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_REQUESTS_CLIENTTYPE, ReferenceType.AppType, TableType.CSUpdate.toString(), "clientTypeID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_REQUESTS_OPERATION, ReferenceType.CfgOp, TableType.CSUpdate.toString(), "opID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_CONFIGNOTIF_OBJNAME, ReferenceType.CfgObjName, TableType.CSUpdate.toString(), "objNameID");

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="client config messages">
        nd = new DynamicTreeNode<>(new OptionNode(false, DialogItem.CONFSERV_CONFIGNOTIF));
        rootA.addChild(nd);
        ndParams = new DynamicTreeNode<>(new OptionNode(DialogItem.CONFSERV_CONFIGNOTIF));
        nd.addChild(ndParams);

//        ndParams.addDynamicRef(DialogItem.CONFSERV_REQUESTS_OBJTYPE, ReferenceType.CfgObjName, TableType.CSUpdate.toString(), "objNameID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_CONFIGNOTIF_APP, ReferenceType.App, TableType.CSClientConf.toString(), "theAppNameID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_CONFIGNOTIF_CLIENTTYPE, ReferenceType.AppType, TableType.CSClientConf.toString(), "clientTypeID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_CONFIGNOTIF_OBJTYPE, ReferenceType.ObjectType, TableType.CSClientConf.toString(), "objTypeID");
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="connects">
        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.CONFSERV_CLIENTCONN));
        rootA.addChild(nd);
        ndParams = new DynamicTreeNode<>(new OptionNode(DialogItem.CONFSERV_CLIENTCONN));
        nd.addChild(ndParams);

//        ndParams.addDynamicRef(DialogItem.CONFSERV_REQUESTS_OBJTYPE, ReferenceType.CfgObjName, TableType.CSUpdate.toString(), "objNameID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_CONFIGNOTIF_APP, ReferenceType.App, TableType.CSClientConnect.toString(), "theAppNameID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_CONFIGNOTIF_CLIENTTYPE, ReferenceType.AppType, TableType.CSClientConnect.toString(), "clientTypeID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_CLIENTCONN_USER, ReferenceType.Agent, TableType.CSClientConnect.toString(), "userNameID");
        ndParams.addDynamicRef(DialogItem.CONFSERV_CLIENTCONN_HOSTPORT, ReferenceType.Host, TableType.CSClientConnect.toString(), "hostportID");
        ndParams.addPairChildren(DialogItem.CONFSERV_CLIENTCONN_ISCONNECT,
                new Pair[]{new Pair("connected", "1"), new Pair("disconnected", "0")});

//</editor-fold>
        //        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.SCS));
//        rootA.addChild(nd);
//        DynamicTreeNode<OptionNode> app = new DynamicTreeNode<>(new OptionNode(DialogItem.SCS_Application));
//        nd.addChild(app);
//        try {
//            addCustom(rootA, FileInfoType.type_SCS);
//        } catch (Exception ex) {
//            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
//        }
//
//        app.addDynamicRef(DialogItem.SCS_Application_NAME, ReferenceType.App, "scs_appstatus", "theAppNameID");
//
//        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.SCSSCS));
//        rootA.addChild(nd);
        addCustom(rootA, FileInfoType.type_ConfServer);

        rootA.addLogMessagesReportType(TableType.MsgConfServer);
        DoneSTDOptions();

    }

    @Override
    public ArrayList<NameID> getApps() throws Exception {
        if (appsType == null) {
            appsType = getAppsType(FileInfoType.type_ConfServer);
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
        cidFinder = new IDsFinder(dlg);
        if (selectionType != SelectionType.NO_SELECTION) {

            if (!cidFinder.initCS()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            }
        }

        retrieveUpdates(dlg,
                FindNode(repComponents.getRoot(), DialogItem.CONFSERV_REQUESTS, DialogItem.CONFSERV_REQUESTS, null),
                cidFinder);
        retrieveNotifs(dlg,
                FindNode(repComponents.getRoot(), DialogItem.CONFSERV_CONFIGNOTIF, DialogItem.CONFSERV_CONFIGNOTIF, null),
                cidFinder);
        retrieveConnect(dlg,
                FindNode(repComponents.getRoot(), DialogItem.CONFSERV_CLIENTCONN, DialogItem.CONFSERV_CLIENTCONN, null),
                cidFinder);

        getCustom(FileInfoType.type_ConfServer, dlg, repComponents.getRoot(), cidFinder, null);
        getGenesysMessages(TableType.MsgConfServer, repComponents.getRoot(), dlg, this);
        Sort();
    }

    //<editor-fold defaultstate="collapsed" desc="retrieveUpdates">
    private void retrieveUpdates(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(reportSettings) && DatabaseConnector.TableExist(TableType.CSUpdate.toString())) {
            tellProgress("ConfigServer update requests");
            TableQuery CSUpdates = newCSUpdates(dlg, reportSettings);

            HashSet<Integer> refIDs = new HashSet<>();
            HashSet<Integer> appNameIDs = new HashSet<>();

            Integer[] iDs = cidFinder.getIDs(IDType.AGENT);
            if (iDs != null) {
                Wheres wh = new Wheres();
                wh.addWhere(getWhere("userNameID", iDs), "OR");
                CSUpdates.addWhere(wh);
            }
            CSUpdates.setRecLoadedProc(new IRecordLoadedProc() {
                @Override
                public void recordLoaded(ILogRecord rec) {
                    addUnique(refIDs, Util.intOrDef(rec.GetField("refID"), (Integer) null));
                    addUnique(appNameIDs, Util.intOrDef(rec.GetField("theAppNameID"), (Integer) null));
//                            addUnique(idFiles, rec.GetFileId());
                }

            });

            CSUpdates.AddCheckedWhere(CSUpdates.getTabAlias() + ".opID",
                    ReferenceType.CfgOp,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_REQUESTS_OPERATION);
            CSUpdates.AddCheckedWhere(CSUpdates.getTabAlias() + ".userNameID",
                    ReferenceType.Agent,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_REQUESTS_USER);
            CSUpdates.AddCheckedWhere(CSUpdates.getTabAlias() + ".theAppNameID",
                    ReferenceType.App,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_REQUESTS_APP);
            CSUpdates.AddCheckedWhere(CSUpdates.getTabAlias() + ".objTypeID",
                    ReferenceType.ObjectType,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_REQUESTS_OBJTYPE);
            CSUpdates.AddCheckedWhere(CSUpdates.getTabAlias() + ".objNameID",
                    ReferenceType.CfgObjName,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_CONFIGNOTIF_OBJNAME);

            getRecords(CSUpdates);
//            inquirer.logger.debug("Refs: "+refIDs.toString());
            if (!refIDs.isEmpty()) {
                CSUpdates = newCSUpdates(dlg, reportSettings);

                Wheres wh = new Wheres();
                wh.addWhere(getWhere("refID", refIDs.toArray(new Integer[refIDs.size()])), "AND");
                wh.addWhere(getWhere("theAppNameID", appNameIDs.toArray(new Integer[appNameIDs.size()])), "AND");
                Wheres topWh = new Wheres();
                topWh.addWhere(wh, "AND");
                topWh.addWhere(CSUpdates.getTabAlias() + ".userNameID is null", "AND");
                CSUpdates.addWhere(topWh);

                getRecords(CSUpdates);

            }
        }

    }

    private TableQuery newCSUpdates(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings) throws SQLException {

        TableQuery CSUpdates = new TableQuery(MsgType.CONFSERV_UPDATES, TableType.CSUpdate.toString());

        CSUpdates.addRef("theAppNameID", "appName", ReferenceType.App.toString(), FieldType.Optional);
        CSUpdates.addRef("objTypeID", "objType", ReferenceType.ObjectType.toString(), FieldType.Optional);
        CSUpdates.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.Optional);
        CSUpdates.addRef("clientTypeID", "clientType", ReferenceType.AppType.toString(), FieldType.Optional);
        CSUpdates.addRef("opID", "op", ReferenceType.CfgOp.toString(), FieldType.Optional);
        CSUpdates.addRef("objNameID", "objName", ReferenceType.CfgObjName.toString(), FieldType.Optional);
        CSUpdates.addRef("msgID", "message", ReferenceType.CfgMsg.toString(), FieldType.Optional);
        CSUpdates.setCommonParams(this, dlg);

        return CSUpdates;

    }

//</editor-fold>
    private void retrieveNotifs(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
//<editor-fold defaultstate="collapsed" desc="retrieveUpdates">
        if (isChecked(reportSettings) && DatabaseConnector.TableExist(TableType.CSClientConf.toString())) {
            tellProgress("ConfigServer update requests");
            TableQuery CSRetrieve = new TableQuery(MsgType.CONFSERV_NOTIF, TableType.CSClientConf.toString());
            CSRetrieve.addRef("theAppNameID", "appName", ReferenceType.App.toString(), FieldType.Optional);
            CSRetrieve.addRef("objTypeID", "objType", ReferenceType.ObjectType.toString(), FieldType.Optional);
            CSRetrieve.addRef("clientTypeID", "clientType", ReferenceType.AppType.toString(), FieldType.Optional);

            CSRetrieve.AddCheckedWhere(CSRetrieve.getTabAlias() + ".theAppNameID",
                    ReferenceType.App,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_CONFIGNOTIF_APP);
            CSRetrieve.AddCheckedWhere(CSRetrieve.getTabAlias() + ".objTypeID",
                    ReferenceType.ObjectType,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_CONFIGNOTIF_OBJTYPE);
            CSRetrieve.AddCheckedWhere(CSRetrieve.getTabAlias() + ".clientTypeID",
                    ReferenceType.AppType,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_CONFIGNOTIF_CLIENTTYPE);

            CSRetrieve.setCommonParams(this, dlg);
            getRecords(CSRetrieve);
        }
//</editor-fold>

    }

    private void retrieveConnect(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
//<editor-fold defaultstate="collapsed" desc="retrieveUpdates">
        if (isChecked(reportSettings) && DatabaseConnector.TableExist(TableType.CSClientConnect.toString())) {
            tellProgress("ConfigServer update requests");
            TableQuery CSConnect = new TableQuery(MsgType.CONFSERV_CONNECT, TableType.CSClientConnect.toString());

            Wheres wh = new Wheres();
            wh.addWhere(getWhere("userNameID", cidFinder.getIDs(IDType.AGENT)), "OR");
            CSConnect.addWhere(wh);

            CSConnect.addRef("theAppNameID", "appName", ReferenceType.App.toString(), FieldType.Optional);
            CSConnect.addRef("clientTypeID", "clientType", ReferenceType.AppType.toString(), FieldType.Optional);
            CSConnect.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.Optional);
            CSConnect.addRef("hostportID", "hostport", ReferenceType.Host.toString(), FieldType.Optional);

            CSConnect.addWhere(IQuery.getAttrsWhere("inbound", FindNode(reportSettings, DialogItem.CONFSERV_CLIENTCONN_ISCONNECT, null, null)), "AND");
            CSConnect.AddCheckedWhere(CSConnect.getTabAlias() + ".userNameID",
                    ReferenceType.Agent,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_CLIENTCONN_USER);
            CSConnect.AddCheckedWhere(CSConnect.getTabAlias() + ".theAppNameID",
                    ReferenceType.App,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_CONFIGNOTIF_APP);
            CSConnect.AddCheckedWhere(CSConnect.getTabAlias() + ".clientTypeID",
                    ReferenceType.AppType,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_CONFIGNOTIF_CLIENTTYPE);
            CSConnect.AddCheckedWhere(CSConnect.getTabAlias() + ".hostportID",
                    ReferenceType.Host,
                    reportSettings,
                    "AND",
                    DialogItem.CONFSERV_CLIENTCONN_HOSTPORT);

            CSConnect.setCommonParams(this, dlg);
            getRecords(CSConnect);
        }
//</editor-fold>

    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new FileInfoType[]{FileInfoType.type_ConfServer}, searchApps);
    }

    @Override
    public void Retrieve(QueryDialog qd, SelectionType key, String searchID) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    FullTableColors getAll(QueryDialog qd) throws Exception {
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
