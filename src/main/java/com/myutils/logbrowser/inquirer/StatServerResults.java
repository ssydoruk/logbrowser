package com.myutils.logbrowser.inquirer;

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

public class StatServerResults extends IQueryResults {

    public static final int TLIB = 0x01;
    public static final int ISCC = 0x02;
    public static final int TC = 0x04;
    public static final int SIP = 0x08;
    public static final int PROXY = 0x10;
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    IDsFinder cidFinder = null;
    private int m_componentFilter;
    private ArrayList<NameID> appType = null;

    public StatServerResults(QueryDialogSettings qdSettings) throws SQLException {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.CONNID);
        addSelectionType(SelectionType.DN);
        addSelectionType(SelectionType.AGENT);
        addSelectionType(SelectionType.PLACE);
    }

    @Override
    public String getName() {
        return "StatServer";
    }

    /**
     *
     */
    public void addTLibReportType(DynamicTreeNode<OptionNode> root) throws SQLException {
        if (DatabaseConnector.TableExist("ststevent_logbr")) {

            DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.STATCALLS_TEVENT));
            root.addChild(nd);

//        DynamicTreeNode<OptionNode> ComponentNode;
//        ComponentNode = new DynamicTreeNode<OptionNode>(new OptionNode(EVENTS_REPORT));
//        nd.addChild(ComponentNode);
            /**
             * *****************
             */
            nd.addDynamicRef(DialogItem.STATCALLS_TEVENT_NAME, ReferenceType.TEvent, "ststevent_logbr", "nameid");
            nd.addDynamicRef(DialogItem.STATCALLS_TEVENT_DN, ReferenceType.DN, "ststevent_logbr", "thisdnid");
        }

    }

    public void addIxnReportType(DynamicTreeNode<OptionNode> root) throws SQLException {
        if (DatabaseConnector.TableExist("IxnSS")) {

            DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.STATCALLS_IXNEVENTS));
            root.addChild(nd);

//        DynamicTreeNode<OptionNode> ComponentNode;
//        ComponentNode = new DynamicTreeNode<OptionNode>(new OptionNode(EVENTS_REPORT));
//        nd.addChild(ComponentNode);
            /**
             * *****************
             */
            nd.addDynamicRef(DialogItem.STATCALLS_IXNEVENTS_NAME, ReferenceType.TEvent, "IxnSS", "nameid");
            nd.addDynamicRef(DialogItem.STATCALLS_IXNEVENTS_QUEUE, ReferenceType.DN, "IxnSS", "IxnQueueID");
            nd.addDynamicRef(DialogItem.STATCALLS_IXNEVENTS_AGENT, ReferenceType.Agent, "IxnSS", "AgentID");
            nd.addDynamicRef(DialogItem.STATCALLS_IXNEVENTS_place, ReferenceType.Place, "IxnSS", "PlaceID");
        }

    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new FileInfoType[]{FileInfoType.type_StatServer}, searchApps);
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return refreshTimeRange(null);
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
        DynamicTreeNode<OptionNode> ndch;
        DynamicTreeNode<OptionNode> nd;

        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.STAT_CALLS));
        rootA.addChild(nd);

        addTLibReportType(nd);
        addIxnReportType(nd);
//        nd.addChild(new DynamicTreeNode<OptionNode>(new OptionNode(DialogItem.TLIB_CALLS_ISCC)));

        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.STATAGENTPLACE));
        rootA.addChild(nd);

        ndch = new DynamicTreeNode<>(new OptionNode(DialogItem.STATAGENTPLACE_AGENT));
        nd.addChild(ndch);
        ndch.addDynamicRef(DialogItem.STATAGENTPLACE_AGENT_NAME, ReferenceType.Agent);

        ndch = new DynamicTreeNode<>(new OptionNode(DialogItem.STATAGENTPLACE_PLACE));
        nd.addChild(ndch);
        ndch.addDynamicRef(DialogItem.STATAGENTPLACE_PLACE_NAME, ReferenceType.Place);

        ndch = new DynamicTreeNode<>(new OptionNode(DialogItem.STATAGENTPLACE_STATUS));
        nd.addChild(ndch);
        ndch.addDynamicRef(DialogItem.STATAGENTPLACE_STATUS_NAME, ReferenceType.StatType);

        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.STATCLIENT));
        rootA.addChild(nd);

        ndch = new DynamicTreeNode<>(new OptionNode(DialogItem.STATCLIENT_NAME));
        nd.addChild(ndch);

        ndch.addDynamicRef(DialogItem.STATCLIENT_NAME_NAME, ReferenceType.App, "STSREQHISTORY_logbr", "clnameid", true);

        try {
            addCustom(rootA, FileInfoType.type_StatServer);
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }
        addConfigUpdates(rootA);
        rootA.addLogMessagesReportType(TableType.MsgStatServer);
        DoneSTDOptions();
    }

    @Override
    public ArrayList<NameID> getApps() throws Exception {
        if (appType == null) {
            appType = getAppsType(FileInfoType.type_StatServer);
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
            inquirer.logger.info("No apps selected. Nothing to search for");
            return;
        }

        if (selectionType != SelectionType.NO_SELECTION) {
            cidFinder = new IDsFinder(dlg);
            if (!cidFinder.initStatServer()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            }
        } else {
            cidFinder = null;
        }

        boolean canRunTLib = true;
        if (cidFinder != null && cidFinder.getSearchType() != SelectionType.NO_SELECTION
                && !cidFinder.AnythingStatServerRelated()) {
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
                    FindNode(repComponents.getRoot(), DialogItem.STAT_CALLS, DialogItem.STATCALLS_TEVENT, null),
                    cidFinder);
            RetrieveIxn(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.STAT_CALLS, DialogItem.STATCALLS_IXNEVENTS, null),
                    cidFinder);
        }
        if (!RetrieveAgent(dlg,
                FindNode(repComponents.getRoot(), DialogItem.STATAGENTPLACE, null, null),
                cidFinder)) {
            return;
        }
        getCustom(FileInfoType.type_StatServer, dlg, repComponents.getRoot(), cidFinder, null);
        getGenesysMessages(TableType.MsgStatServer, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);

        Sort();
    }

    private void RetrieveTLib(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("ststevent_logbr")) {
            inquirer.logger.debug("TLib report");
            TlibStatServerQuery tlibQuery = new TlibStatServerQuery(eventsSettings, cidFinder);
            tlibQuery.setCommonParams(this, dlg);
            getRecords(tlibQuery);

        }
    }

    private void RetrieveIxn(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("ixnss")) {
            inquirer.logger.debug("Ixn report");

            IxnSSQuery ixnSSQuery = new IxnSSQuery(eventsSettings, cidFinder);
            ixnSSQuery.setCommonParams(this, dlg);
            getRecords(ixnSSQuery);
        }
    }

    private boolean RetrieveAgent(QueryDialog dlg, DynamicTreeNode<OptionNode> agentRoot, IDsFinder cidFinder) throws SQLException {
        if (isChecked(agentRoot)) {
            if (cidFinder != null && cidFinder.getSearchType() != SelectionType.NO_SELECTION
                    && !cidFinder.AnythingStatServerRelated()) {
                int dialogResult = inquirer.showConfirmDialog(dlg, "No Agent found.\n"
                        + "Do you want to show all agents messages?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
                if (dialogResult == JOptionPane.CANCEL_OPTION) {
                    return false;
                }
                if (dialogResult == JOptionPane.NO_OPTION) {
                    return true;
                }
            }

            TableQuery statsAgents = null;
            Wheres wh1 = null;
            if (DatabaseConnector.TableExist("ststatus")) {
                statsAgents = new TableQuery(MsgType.STATSAgent, "ststatus");

                Wheres wh = new Wheres();
                wh.addWhere(TableQuery.getCheckedWhere("oldstatusid", ReferenceType.StatType,
                        FindNode(agentRoot, DialogItem.STATAGENTPLACE_STATUS, DialogItem.STATAGENTPLACE_STATUS_NAME, null)), "OR");

                wh.addWhere(TableQuery.getCheckedWhere("newstatusid", ReferenceType.StatType,
                        FindNode(agentRoot, DialogItem.STATAGENTPLACE_STATUS, DialogItem.STATAGENTPLACE_STATUS_NAME, null)), "OR");

                wh1 = new Wheres();
                wh1.addWhere(TableQuery.getCheckedWhere("placeid", ReferenceType.Place,
                        FindNode(agentRoot, DialogItem.STATAGENTPLACE_PLACE, DialogItem.STATAGENTPLACE_PLACE_NAME, null)), "OR");

                wh1.addWhere(TableQuery.getCheckedWhere("agentid", ReferenceType.Agent,
                        FindNode(agentRoot, DialogItem.STATAGENTPLACE_AGENT, DialogItem.STATAGENTPLACE_AGENT_NAME, null)), "OR");

                statsAgents.addRef("placeid", "place", ReferenceType.Place.toString(), FieldType.OPTIONAL);
                statsAgents.addRef("agentid", "agent", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
                statsAgents.addRef("oldstatusid", "oldstatus", ReferenceType.StatType.toString(), FieldType.OPTIONAL);
                statsAgents.addRef("newstatusid", "newstatus", ReferenceType.StatType.toString(), FieldType.OPTIONAL);
                statsAgents.addWhere(wh, "AND");
                statsAgents.addWhere(wh1, "AND");

                statsAgents.setCommonParams(this, dlg);
                getRecords(statsAgents);
            }
//            ----------------------------------------------------
            if (DatabaseConnector.TableExist("stcapacity")) {
                statsAgents = new TableQuery(MsgType.STATSCapacity, "stcapacity");

                wh1 = new Wheres();
                wh1.addWhere(TableQuery.getCheckedWhere("placeid", ReferenceType.Place,
                        FindNode(agentRoot, DialogItem.STATAGENTPLACE_PLACE, DialogItem.STATAGENTPLACE_PLACE_NAME, null)), "OR");

                wh1.addWhere(TableQuery.getCheckedWhere("agentid", ReferenceType.Agent,
                        FindNode(agentRoot, DialogItem.STATAGENTPLACE_AGENT, DialogItem.STATAGENTPLACE_AGENT_NAME, null)), "OR");

                statsAgents.addRef("placeid", "place", ReferenceType.Place.toString(), FieldType.OPTIONAL);
                statsAgents.addRef("capacityid", "capacity", ReferenceType.Capacity.toString(), FieldType.OPTIONAL);
                statsAgents.addRef("agentid", "agent", ReferenceType.Agent.toString(), FieldType.OPTIONAL);
                for (int i = 0; i < com.myutils.logbrowser.indexer.StCapacity.MAX_MEDIA; i++) {
                    String fldIdx = "media" + i + "name";
                    statsAgents.addRef(fldIdx + "id", fldIdx, ReferenceType.Media.toString(), FieldType.OPTIONAL);
                }
                statsAgents.addWhere(wh1, "AND");

                statsAgents.setCommonParams(this, dlg);
                getRecords(statsAgents);
            }
        }
        return true;
    }

    @Override
    public String getReportSummary() {
        return getName() + "\n\t" + "search: " + ((cidFinder != null) ? cidFinder.toString() : "<not specified>");
    }

    @Override
    public void Retrieve(QueryDialog qd, SelectionType key, String searchID) throws SQLException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    FullTableColors getAll(QueryDialog qd, Component c, int x, int y)  throws Exception {
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
