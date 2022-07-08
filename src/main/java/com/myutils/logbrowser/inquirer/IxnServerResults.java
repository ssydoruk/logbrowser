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
import java.util.Properties;

public class IxnServerResults extends IQueryResults {

    private String m_tlibFilter;

    private int m_componentFilter;
    private ArrayList<NameID> appType = null;
    private IDsFinder cidFinder = null;

    @Override
    IDsFinder.RequestLevel getDefaultQueryLevel() {
        return IDsFinder.RequestLevel.Level1;
    }

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

        DynamicTreeNode<OptionNode> AttrValue;

        DynamicTreeNode<OptionNode> Attr;

        Attr = new DynamicTreeNode<>(new OptionNode(true, "CallType"));
        nd.addChild(Attr);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Inbound"));
        Attr.addChild(AttrValue);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Outbound"));
        Attr.addChild(AttrValue);

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
        DoneSTDOptions();

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
        SelectionType selectionType = dlg.getSelectionType();
        String selection = dlg.getSelection();

        if (dlg.getSearchApps() == null) {
            inquirer.logger.info("No apps selected. Nothing to search in");
            return;
        }

        if (selectionType != SelectionType.NO_SELECTION) {
            cidFinder = new IDsFinder(dlg);
            if (!cidFinder.initIxn()) {
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

    private TableQuery IxnTable(MsgType tabType, String tab) throws SQLException {
        TableQuery TLibReq = new TableQuery(tabType, tab);
        TLibReq.addRef("IxnQueueID", "queue", ReferenceType.DN.toString(), FieldType.Optional);
        TLibReq.addRef("nameID", "name", ReferenceType.TEvent.toString(), FieldType.Mandatory);
        TLibReq.addRef("ConnIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.Optional);
        TLibReq.addRef("IxnID", "InteractionID", ReferenceType.IxnID.toString(), FieldType.Optional);
        TLibReq.addRef("appid", "client", ReferenceType.App.toString(), FieldType.Optional);

        TLibReq.addRef("MediaTypeID", "MediaType", ReferenceType.IxnMedia.toString(), FieldType.Optional);
        TLibReq.addRef("AgentID", "Agent", ReferenceType.Agent.toString(), FieldType.Optional);
        TLibReq.addRef("PlaceID", "Place", ReferenceType.Place.toString(), FieldType.Optional);
        TLibReq.addRef("attr1ID", "attr1", ReferenceType.TLIBATTR1.toString(), FieldType.Optional);
        TLibReq.addRef("attr2ID", "attr2", ReferenceType.TLIBATTR2.toString(), FieldType.Optional);

        return TLibReq;
    }

    private void retrieveIxn(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings)) {
            inquirer.logger.debug("Ixn report");
            IxnServerQuery ixnQuery = new IxnServerQuery(eventsSettings, cidFinder);
            ixnQuery.setCommonParams(this, dlg);
            getRecords(ixnQuery);

            if (cidFinder.getQueryLevel()>1 &&
                    ixnQuery.isCollectingID(IxnServerQuery.FIELD_REFID) && !ixnQuery.getCollectID(IxnServerQuery.FIELD_REFID).isEmpty()) {
//                HashSet<Long> refIDs = ixnQuery.getCollectID("refid");

                TableQuery TLibReq = IxnTable(MsgType.INTERACTION, "ixn");
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
            tabIxnNonIxn.addRef("thisDNID", "thisDN", ReferenceType.DN.toString(), FieldType.Optional);
            tabIxnNonIxn.addRef("userEventID", "event", ReferenceType.TEvent.toString(), FieldType.Optional);

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
