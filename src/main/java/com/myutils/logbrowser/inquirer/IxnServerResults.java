package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import static com.myutils.logbrowser.inquirer.QueryTools.FindNode;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

public class IxnServerResults extends IQueryResults {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private ArrayList<NameID> appType = null;
    private IDsFinder cidFinder = null;

    public IxnServerResults(QueryDialogSettings qdSettings) throws SQLException {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.CONNID);
        addSelectionType(SelectionType.IXN);
        addSelectionType(SelectionType.QUEUE);
        addSelectionType(SelectionType.AGENT);
        addSelectionType(SelectionType.REFERENCEID);
    }

    @Override
    IDsFinder.RequestLevel getDefaultQueryLevel() {
        return IDsFinder.RequestLevel.Level1;
    }

    @Override
    public String getReportSummary() {
        return "IxnServer \n\t" + (cidFinder != null ? "search: " + cidFinder : "");
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
        return "IxnServer";
    }

    public void addIxnNonIxnReportType(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.IXN_NONEVENT));
        root.addChild(nd);

        nd.addDynamicRef(DialogItem.IXN_NONIXN_EVENT_NAME, ReferenceType.TEvent, TableType.IxnNonIxn.toString(), "nameID");

        nd.addDynamicRef(DialogItem.IXN_NONIXN_EVENT_ATTR1, ReferenceType.TLIBATTR1, TableType.IxnNonIxn.toString(), "attr1ID");
        nd.addDynamicRef(DialogItem.IXN_NONIXN_EVENT_ATTR2, ReferenceType.TLIBATTR2, TableType.IxnNonIxn.toString(), "attr2ID");

        nd.addDynamicRef(DialogItem.IXN_NONIXN_EVENT_DN, ReferenceType.DN, TableType.IxnNonIxn.toString(), "thisDNID");
        nd.addDynamicRef(DialogItem.IXN_NONIXN_EVENT_EVENT, ReferenceType.TEvent, TableType.IxnNonIxn.toString(), "userEventID");

    }

    public void addIxnReportType(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.IXN_EVENT));
        root.addChild(nd);

        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_NAME, ReferenceType.TEvent, "ixn", "nameID");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_APP, ReferenceType.App, "ixn", "appid");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_MEDIA, ReferenceType.IxnMedia, "ixn", "MediaTypeID");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_AGENT, ReferenceType.Agent, "ixn", "AgentID");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_PLACE, ReferenceType.Place, "ixn", "PlaceID");

        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_ATTR1, ReferenceType.TLIBATTR1, "ixn", "attr1ID");
        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_ATTR2, ReferenceType.TLIBATTR2, "ixn", "attr2ID");

        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_DN, ReferenceType.DN, "ixn", "ixnqueueid");
        nd.addDynamicRef(DialogItem.URS_STRATEGY, ReferenceType.URSStrategyName, "ixn", "strategyID");

        DynamicTreeNode<OptionNode> AttrValue;

        DynamicTreeNode<OptionNode> Attr;

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

    private void loadStdOptions() throws SQLException {
        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.IXN));
        rootA.addChild(nd);

        addIxnReportType(nd);
        addIxnNonIxnReportType(nd);
//        nd.addChild(new DynamicTreeNode<OptionNode>(new OptionNode(DialogItem.TLIB_CALLS_ISCC)));

        nd = new DynamicTreeNode<>(new OptionNode(false, DialogItem.CONFIGUPDATES));
        rootA.addChild(nd);

        addCustom(rootA, FileInfoType.type_IxnServer);

        addConfigUpdates(rootA);
        rootA.addLogMessagesReportType(TableType.MsgIxnServer);
        DoneSTDOptions(getQdSettings().getSavedOptions().get(this.getName()));

    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        if (appType == null) {
            appType = getAppsType(FileInfoType.type_IxnServer);
        }
        return appType;
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

        cidFinder = new IDsFinder(dlg);
        runSelectionQuery(dlg, dlg.getSelectionType(), cidFinder);

    }

    private TableQuery IxnTable(MsgType tabType, String tab) throws SQLException {
        TableQuery TLibReq = new TableQuery(tabType, tab);
        TLibReq.addRef("IxnQueueID", "queue", ReferenceType.DN.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.MANDATORY);
        TLibReq.addRef("ConnIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("IxnID", "InteractionID", ReferenceType.IxnID.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("appid", "client", ReferenceType.App.toString(), FieldType.OPTIONAL);

        TLibReq.addRef("MediaTypeID", "MediaType", ReferenceType.IxnMedia.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("AgentID", "Agent", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("PlaceID", "Place", ReferenceType.Place.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("attr1ID", "attr1", ReferenceType.TLIBATTR1.toString(), FieldType.OPTIONAL);
        TLibReq.addRef("attr2ID", "attr2", ReferenceType.TLIBATTR2.toString(), FieldType.OPTIONAL);

        return TLibReq;
    }

    private void retrieveIxn(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings)) {
            inquirer.logger.debug("Ixn report");
            IxnServerQuery ixnQuery = new IxnServerQuery(eventsSettings, cidFinder);
            ixnQuery.setCommonParams(this, dlg);
            getRecords(ixnQuery);

            if ((cidFinder == null || cidFinder.getQueryLevel() > 1)
                    && ixnQuery.isCollectingID(IxnServerQuery.FIELD_REFID) && !ixnQuery.getCollectID(IxnServerQuery.FIELD_REFID).isEmpty()) {
//                HashSet<Long> refIDs = ixnQuery.getCollectID("refid");

                TableQuery TLibReq = IxnTable(MsgType.INTERACTION, "ixn");
                TLibReq.addRef("strategyID", "strategy", ReferenceType.URSStrategyName, FieldType.OPTIONAL);

//                TLibReq.addWhere(TLibReq.getTabAlias() + ".ixnid>0 ", "AND");
                TLibReq.AddCheckedWhere(TLibReq.getTabAlias() + ".nameID", ReferenceType.TEvent, eventsSettings, "AND", DialogItem.IXN_IXN_EVENT_NAME);
//                        nd.addDynamicRef(DialogItem.IXN_IXN_EVENT_DN, ReferenceType.DN, "ixn", "ixnqueueid");
                TLibReq.AddCheckedWhere(TLibReq.getTabAlias() + ".ixnqueueid", ReferenceType.DN, eventsSettings, "AND", DialogItem.IXN_IXN_EVENT_DN);

                Wheres wh1 = new Wheres();
                wh1.addWhere(getWhere(TLibReq.fldName(IxnServerQuery.FIELD_REFID), ixnQuery.getCollectID(IxnServerQuery.FIELD_REFID), false));
                wh1.addWhere(getWhereNot(TLibReq.fldName(IxnServerQuery.FIELD_ID), ixnQuery.getCollectID(IxnServerQuery.FIELD_ID), false), IQuery.ANDOR.AND.toString());
                TLibReq.addWhere(wh1, IQuery.ANDOR.AND.toString());

                TLibReq.setCommonParams(this, dlg);
                getRecords(TLibReq);
            }

        }

    }

    private void retrieveIxnNonIxn(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && !DatabaseConnector.TableEmpty(TableType.IxnNonIxn.toString())) {
            inquirer.logger.debug("Ixn report 1");
//                HashSet<Long> refIDs = ixnQuery.getCollectID("refid");

            TableQuery tabIxnNonIxn = IxnTable(MsgType.INTERACTION_NONIXN, TableType.IxnNonIxn.toString());
            tabIxnNonIxn.addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.OPTIONAL);
            tabIxnNonIxn.addRef("userEventID", "event", ReferenceType.TEvent.toString(), FieldType.OPTIONAL);

            tabIxnNonIxn.AddCheckedWhere(tabIxnNonIxn.getTabAlias() + ".thisDNID", ReferenceType.TEvent, eventsSettings, "AND", DialogItem.IXN_NONIXN_EVENT_DN);
            tabIxnNonIxn.AddCheckedWhere(tabIxnNonIxn.getTabAlias() + ".userEventID", ReferenceType.DN, eventsSettings, "AND", DialogItem.IXN_NONIXN_EVENT_EVENT);
            Wheres wh1 = new Wheres();
            if (cidFinder != null) {
                Integer[] ids = null;
                if ((ids = cidFinder.getIDs(IDType.ConnID)) != null) {
                    wh1.addWhere(getWhere("connIDID", ids, false), "OR");
                }
                if ((ids = cidFinder.getIDs(IDType.IxnID)) != null) {
                    wh1.addWhere(getWhere("IxnID", ids, false), "OR");
                }
                if (!wh1.isEmpty()) {
                    tabIxnNonIxn.addWhere(wh1);
                }
            }
            tabIxnNonIxn.setCommonParams(this, dlg);
            getRecords(tabIxnNonIxn);

        }

    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new FileInfoType[]{FileInfoType.type_IxnServer}, searchApps);
    }

    @Override
    public void Retrieve(QueryDialog qd, SelectionType key, String searchID) throws SQLException {
        runSelectionQuery(qd, key, new IDsFinder(qd, key, searchID));
    }

    @Override
    public AllProcSettings getAllProc(Window parent) {
        AllCalls_IxnSettings rd = AllCalls_IxnSettings.getInstance(this, parent);
        return (rd.doShow()) ? new AllProcSettings(rd.getSelectAllReportTypeGroup().getSelectedObject(), rd) : null;
    }


    @Override
    void showAllResults() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    SearchFields getSearchField() {
        SearchFields ret = new SearchFields();
        ret.addRecMap(FileInfoType.type_IxnServer, new Pair<>(SelectionType.IXN, "ixn"));
        return ret;
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
        ArrayList<IAggregateQuery> ret = new ArrayList<>();
//        ret.add(new GREAggrTimestampDelays());
        ret.add(new AggrIxnServerMsgPerSecond());
//        ret.add(new AggrGREClientDelays());

        return ret;
    }

    @Override
    boolean callRelatedSearch(IDsFinder cidFinder) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    ArrayList<Pair<String, String>> getAllSortFields() {
        ArrayList<Pair<String, String>> ret = new ArrayList<>();
        ret.add(new Pair<>("Time", "started"));
        ret.add(new Pair<>("Interaction ID", "ixnID"));
        return ret;
    }

    FullTableColors getAllInteractions(QueryDialog qd, AllInteractionsSettings settings) throws SQLException{
        try {

            String reportTable = "ixnAllTmp";
            DynamicTreeNode.setNoRefNoLoad(true);

            DatabaseConnector.dropTable(reportTable);
            inquirer.logger.info("Building temp tables");

            tellProgress("Creating temp table");
            DatabaseConnector.runQuery("create temp table " + reportTable + " (" + "ixnid int"
                    + ",parentixnidid int"
                    + ",started timestamp" 
                    + ",ended timestamp" 
                    + ",stoppedat timestamp"
                    + ",mediatypeid int" 
                    + ",appid int" 
                    + ",ixnqueueid int"
                     + ")\n;");

            Wheres wh = new Wheres();
//            wh.addWhere(IQuery.getCheckedWhere("ThisDNid", ReferenceType.DN, FindNode(repComponents.getRoot(),
//                    DialogItem.ORS, DialogItem.ORS_TEVENTS, DialogItem.ORS_TEVENTS_DN)), "OR");
//            wh.addWhere(IQuery.getCheckedWhere("otherDNID", ReferenceType.DN, FindNode(repComponents.getRoot(),
//                    DialogItem.ORS, DialogItem.ORS_TEVENTS, DialogItem.ORS_TEVENTS_DN)), "OR");

            String makeWhere = wh.makeWhere(false);

            String tab = TableType.Ixn.toString();

            if (!StringUtils.isEmpty(tab)) {
                tellProgress("Finding unique IxnIDs in interactions");

                getAllResults.add(new Pair<>("Unique calls in Ixn table",
                        DatabaseConnector.runQuery("insert into " + reportTable + " (ixnid, started, ended)"
                                + "\nselect distinct ixnid, min(time), max(time) from " + tab
                                + "\nwhere ixnid >0 AND ixnqueueid>0\n"
                                + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                                + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                                + ((makeWhere != null && !makeWhere.isEmpty()) ? " and (" + makeWhere + ")" : "")
                                + "\ngroup by 1" + IQuery.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                                + ";")));

                DatabaseConnector
                        .runQuery("create index idx_" + reportTable + "ixnid on " + reportTable + "(ixnid);");
                DatabaseConnector
                        .runQuery("create index idx_" + reportTable + "started on " + reportTable + "(started);");

                tellProgress("Updating parameters of interaction");
                DatabaseConnector.runQuery("update " + reportTable + "\nset (" + "ixnqueueid" + ", mediatypeid" + ", appid"
                        + ") = " + "\n(" + "select "
                        + "ixnqueueid" + ", mediatypeid " + ", appid " 
                        + "\nfrom\n" + "(select " + tab + ".*" + ",file_logbr.appnameid as appid"
                        + "\nfrom " + tab + " inner join file_logbr on " + tab + ".fileid=file_logbr.id" + ") as " + tab
                        + "\nwhere " + reportTable + ".ixnid=" + tab + ".ixnid"
                        + "\nand\n" + reportTable + ".started=" + tab + ".time" + "\n"
                        + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND") + "\n"
                        + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND") + ")" + ";");


                DatabaseConnector.runQuery("update " + reportTable + "\nset (" + "stoppedat" 
                        + ") = " + "\n(" + "select "
                        + "time"                        + "\nfrom " + tab 
                        + "\nwhere " + reportTable + ".ixnid=" + tab + ".ixnid"
                        + " AND " + getWhere(tab +".nameid", ReferenceType.TEvent, new String[]{"EventProcessingStopped"}, false)
                       
                        + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND") + "\n"
                        + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND") + ")" + ";");

            } else {
                logger.error("Neither routing table found");
            }

            tellProgress("Creating indexes");
            DatabaseConnector.runQuery("create index idx_" + reportTable + "appid on " + reportTable + "(appid);");
            DatabaseConnector
                    .runQuery("create index idx_" + reportTable + "mediatypeid on " + reportTable + "(mediatypeid);");
            DatabaseConnector
                    .runQuery("create index idx_" + reportTable + "ixnqueueid on " + reportTable + "(ixnqueueid);");


            tellProgress("Extracting data");
            TableQuery tabReport = new TableQuery(reportTable);
            tabReport.setOrderBy(tabReport.getTabAlias() + "." + settings.getSortField() + ' '
                    + (settings.isAscendingSorting() ? "asc" : "desc") + " ");
            tabReport.addOutField("UTCtoDateTime(started, \"YYYY-MM-dd HH:mm:ss.SSS\") \"first ts\"");
            tabReport.addOutField("UTCtoDateTime(ended, \"YYYY-MM-dd HH:mm:ss.SSS\") \"last ts\"");
            tabReport.addOutField("UTCtoDateTime(stoppedat, \"YYYY-MM-dd HH:mm:ss.SSS\") \"stopped at\"");
            tabReport.setAddAll(false);
            tabReport.addRef("ixnQueueID", "Queue", ReferenceType.DN.toString(), IQuery.FieldType.OPTIONAL);
            tabReport.addRef("ixnid", "ixn", ReferenceType.IxnID.toString(), IQuery.FieldType.OPTIONAL);
            tabReport.addRef("MediaTypeID", "media", ReferenceType.Media.toString(), IQuery.FieldType.OPTIONAL);
            tabReport.addRef("appid", "application", ReferenceType.App.toString(), IQuery.FieldType.OPTIONAL);
            int maxRecs = settings.getMaxRecords();
            if (maxRecs > 0) {
                tabReport.setLimit(maxRecs);
            }

            // tabReport.setOrderBy(tabReport.getTabAlias() + ".started");
            FullTableColors currTable = tabReport.getFullTable();
            currTable.setHiddenField("rowType");
            return currTable; // To change body of generated methods, choose Tools | Templates.
        } finally {
            DynamicTreeNode.setNoRefNoLoad(false);
        }
    }

    FullTableColors getAllAgentLogins(QueryDialog qd, AllInteractionsSettings settings) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private void runSelectionQuery(QueryDialog dlg, SelectionType selectionType, IDsFinder cidFinder) throws SQLException {

        String selection = dlg.getSelection();

        if (dlg.getSearchApps() == null) {
            inquirer.logger.info("No apps selected. Nothing to search in");
            return;
        }

        if (selectionType != SelectionType.NO_SELECTION) {
            if (!cidFinder.initIxn()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            }
        } else {
            cidFinder = null;
        }

        if (cidFinder != null && cidFinder.getSearchType() == SelectionType.CALLID) {
            inquirer.logger.info("No TLib events for CallID search without show related");
        } else {
            retrieveIxn(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.IXN, DialogItem.IXN_EVENT, null),
                    cidFinder);
            retrieveIxnNonIxn(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.IXN, DialogItem.IXN_NONEVENT, null),
                    cidFinder);
        }

        getCustom(FileInfoType.type_StatServer, dlg, repComponents.getRoot(), cidFinder, null);

        getGenesysMessages(TableType.MsgIxnServer, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);
        Sort();

    }

}
