package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;

import javax.swing.*;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

public class CallFlowResults extends IQueryResults {

    public static final int TLIB = 0x01;
    public static final int ISCC = 0x02;
    public static final int TC = 0x04;
    public static final int SIP = 0x08;
    public static final int PROXY = 0x10;
    private final Handlers handlerIDs = new Handlers();
    //    private String m_tlibFilter;
//    private int m_componentFilter;
    ArrayList<NameID> appsType;
    //    private static final String[] RequestsToShow = {"RequestMakePredictiveCall", "RequestMakeCall", "RequestMonitorNextCall"};
//    private static final String[] EventsToShow = {"EventDialing", "EventNetworkReached", "EventMonitoringNextCall"};
    private IDsFinder cidFinder;

    public CallFlowResults(QueryDialogSettings qdSettings) throws SQLException {
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
    public String getSearchString() {
        if (cidFinder != null) {
            return cidFinder.getSelection(); //To change body of generated methods, choose Tools | Templates.
        } else {
            return super.getSearchString();
        }
    }

    @Override
    protected void showAllResults() {
        showAllResults(getAllResults); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void Retrieve(QueryDialog dlg, SelectionType key, String searchID) throws SQLException {
        runSelectionQuery(dlg, key, new IDsFinder(dlg, key, searchID));

    }

    @Override
    IDsFinder.RequestLevel getDefaultQueryLevel() {
        return IDsFinder.RequestLevel.Level5; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    FullTableColors getAll(QueryDialog qd, Component c, int x, int y)  throws SQLException {
        try {
            String tmpTable = "callFlowTmp";
            DynamicTreeNode.setNoRefNoLoad(true);

            getAllResults.clear();

            DatabaseConnector.dropTable(tmpTable);
            inquirer.logger.info("Building temp tables");
            String tab = "tlib_logbr";

            tellProgress("Creating temp table");
            DatabaseConnector.runQuery("create temp table " + tmpTable + " ("
                    + "connectionidid int"
                    + ",started timestamp"
                    + ",ended timestamp"
                    + ",thisdnid int"
                    + ",otherdnid int"
                    + ",aniid int"
                    + ",nameid int"
                    + ",appid int"
                    + ",dnisid int"
                    + ",isconsult bit"
                    + ",isiscc bit"
                    + ",hasconstr bit"
                    + ",hasdestr bit"
                    + ",calltype int"
                    + ")\n;"
            );

            Wheres wh = new Wheres();
            wh.addWhere(IQuery.getCheckedWhere("ThisDNid", ReferenceType.DN,
                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, DialogItem.TLIB_CALLS_TEVENT_DN)), "OR");
            wh.addWhere(IQuery.getCheckedWhere("otherDNID", ReferenceType.DN,
                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, DialogItem.TLIB_CALLS_TEVENT_DN)), "OR");

            String dnWhere = wh.makeWhere("AND", false);
            if (DatabaseConnector.TableExist("connid_logbr")) {
                tellProgress("Getting unique calls");
                getAllResults.add(new Pair<>("Unique calls in SIPServer and/or TServer", DatabaseConnector.runQuery("insert into " + tmpTable + " (connectionidid, started, ended)"
                        + "\nselect distinct connectionidid, min(time), max(time) from " + tab
                        + "\nwhere connectionidid >0 and connectionidid not in (select connectionidid from connid_logbr where istemp=1)"
                        + "\n" + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                        + "\n" + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                        + "\n" + dnWhere
                        + "\ngroup by 1\n"
                        + IQuery.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                        + ";")));
            }

            tellProgress("Create index 1");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "connID on " + tmpTable + "(connectionidid);");
            tellProgress("Create index 2");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "started on " + tmpTable + "(started);");
            tellProgress("Finding ani");
            DatabaseConnector.runQuery("update " + tmpTable + " set aniid = "
                    + "\n(select aniid from " + tab
                    + "\nwhere " + tmpTable + ".connectionidid=" + tab + ".connectionidid"
                    + "\nand " + getWhere("nameid", ReferenceType.TEvent, new String[]{"EventCallCreated"}, false)
                    + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                    + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                    + "\n)"
                    + ";");

            if (DatabaseConnector.TableExist("connid_logbr")) {
                String tab1 = "connid_logbr";
                tellProgress("Finding constructed");
                DatabaseConnector.runQuery("update " + tmpTable + " set hasconstr = "
                        + "\n(select 1 from " + tab1
                        + "\nwhere " + tmpTable + ".connectionidid=" + tab1 + ".connectionidid"
                        + "\nand created=1 "
                        + IQuery.getFileFilters(tab1, "fileid", qd.getSearchApps(false), "AND")
                        + IQuery.getDateTimeFilters(tab1, "time", qd.getTimeRange(), "AND")
                        + "\n)"
                        + ";");
                tellProgress("Finding descructed");
                DatabaseConnector.runQuery("update " + tmpTable + " set hasdestr = "
                        + "\n(select 1 from " + tab1
                        + "\nwhere " + tmpTable + ".connectionidid=" + tab1 + ".connectionidid"
                        + "\nand created=0 "
                        + IQuery.getFileFilters(tab1, "fileid", qd.getSearchApps(false), "AND")
                        + IQuery.getDateTimeFilters(tab1, "time", qd.getTimeRange(), "AND")
                        + "\n)"
                        + ";");
            }
            String tmpTable1 = tmpTable + "1";
            DatabaseConnector.dropTable(tmpTable1);
            tellProgress("Creating temp table for requests");
            DatabaseConnector.runQuery("create temp table "
                    + tmpTable1
                    + " as select distinct connectionidid, min(id) as tlibid from " + tab + "\n"
                    + "where connectionidid is not null\n"
                    + "and thisdnid>0\n"
                    + dnWhere
                    + IQuery.getCheckedWhere("nameID", ReferenceType.TEvent,
                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, DialogItem.TLIB_CALLS_TEVENT_NAME), "AND")
                    + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                    + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                    + "\n"
                    + "group by 1"
                    + IQuery.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                    + ";");

            tellProgress("Creating indexes on connID for requests");
            DatabaseConnector.runQuery("create index idx_" + tmpTable1 + "connID on " + tmpTable1 + "(connectionidid);");
            tellProgress("Creating indexes on TLIBID on requests");
            DatabaseConnector.runQuery("create index idx_" + tmpTable1 + "tlibid on " + tmpTable1 + "(tlibid);");
//            DatabaseConnector.runQuery("alter table " + tmpTable1 + "tlibid on " + tmpTable1 + "(tlibid);");
            tellProgress("Updating call parameters");
            DatabaseConnector.runQuery("WITH a AS\n"
                    + "(\n"
                    + "    SELECT\n"
                    + "        " + tab + ".connectionIDid,\n"
                    + "        thisdnid, otherdnid, nameid, transferidid, calltype, app.id appid\n"
                    + "    FROM\n"
                    + tab
                    + "\ninner join " + tmpTable1 + " on tlibid=" + tab + ".id"
                    + "\ninner join file_logbr f on " + tab + ".fileid=f.id\n"
                    + "\ninner join App on f.AppNameID = app.id"
                    + ")\n"
                    //                    + "update " + tmpTable
                    //                    + "\nset nameid = \n(select nameid from a "
                    //                    + "\nwhere " + tmpTable + ".connectionidid=a.connectionidid)"
                    //                    + ","
                    //                    + "\nthisdnid = \n(select thisdnid from a "
                    //                    + "\nwhere " + tmpTable + ".connectionidid=a.connectionidid)"
                    //                    + ","
                    //                    + "\nisconsult = \n(select transferidid>0 and transferidid<>connectionidid from a "
                    //                    + "\nwhere " + tmpTable + ".connectionidid=a.connectionidid)"
                    //                    + ","
                    //                    + "\nisiscc = \n(select transferidid>0 and transferidid=connectionidid from a "
                    //                    + "\nwhere " + tmpTable + ".connectionidid=a.connectionidid)"
                    //                    + ","
                    //                    + "\n otherdnid = \n(select otherdnid from a "
                    //                    + "\nwhere " + tmpTable + ".connectionidid=a.connectionidid)"
                    //                    + "\nwhere connectionidid in (select a.connectionidid from a where a.connectionidid=" + tmpTable + ".connectionidid)"
                    //                    + ";");

                    + "update " + tmpTable
                    + "\nset (nameid, thisdnid, isconsult, isiscc, otherdnid, calltype, appid)  = (select nameid ,"
                    + "\nthisdnid ,"
                    + "\ntransferidid>0 and transferidid<>connectionidid ,"
                    + "\ntransferidid>0 and transferidid=connectionidid ,"
                    + "\notherdnid,"
                    + "\ncalltype,"
                    + "\nappid"
                    + "\nfrom a "
                    + "\nwhere " + tmpTable + ".connectionidid=a.connectionidid)"
                    + ";");

            tellProgress("Creating index on thisdn");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "thisdnid on " + tmpTable + "(thisdnid);");

            tellProgress("Creating index on otherdn");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "otherdnid on " + tmpTable + "(otherdnid);");
            tellProgress("Creating index on event name");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "nameid on " + tmpTable + "(nameid);");
//            FullTableColors currTable = DatabaseConnector.getFullTableColors("select "
//                    + "REGEXP_group('([^\\\\/]+)$', intfilename, 1) infilename, "
//                    + "hostname, "
//                    + "app.name as name, "
//                    + "UTCtoDateTime(starttime, \"YYYY-MM-dd HH:mm:ss.SSS\") app_starttime, "
//                    + "UTCtoDateTime(filestarttime, \"YYYY-MM-dd HH:mm:ss.SSS\") filestarttime, "
//                    + "UTCtoDateTime(endtime, \"YYYY-MM-dd HH:mm:ss.SSS\") endtime, "
//                    + "jduration(endtime-filestarttime) duration, "
//                    + "fileno, "
//                    + "filesize(size) size"
//                    + "\nfrom file_logbr "
//                    + "\ninner join app on app.id=file_logbr.appnameid"
//                    //                    + IQuery.getWhere("appnameid", new Integer[]{searchApps.get(i)}, true)
//                    + "\norder by name, starttime, filestarttime",
//                    "app_starttime");
            TableQuery tabReport = new TableQuery(tmpTable);
            tabReport.addOutField("UTCtoDateTime(started, \"YYYY-MM-dd HH:mm:ss.SSS\") started");
            tabReport.addOutField("UTCtoDateTime(ended, \"YYYY-MM-dd HH:mm:ss.SSS\") ended");
            tabReport.addOutField("(ended-started)/1000 \"duration (sec)\"");
            tabReport.addOutField("jduration(ended-started) duration");
            tabReport.addOutField("case isconsult when 1 then \"true\" when 0 then \"false\" end isconsult ");
            tabReport.addOutField("case isiscc when 1 then \"true\" when 0 then \"false\" end isiscc ");
            tabReport.addOutField("case hasconstr when 1 then \"true\" when 0 then \"false\" end constr ");
            tabReport.addOutField("case hasdestr when 1 then \"true\" when 0 then \"false\" end destr ");
//            tabReport.addOutField(inquirer.getCr().getCaseExpr("calltype", "CallType") + " calltype ");
            tabReport.addOutField("constToStr(\"CallType \", calltype) calltype "); // bug in SQLite lib; does not accept full word
            tabReport.setAddAll(false);

            tabReport.addRef("thisdnid", "thisdn", ReferenceType.DN.toString(), FieldType.OPTIONAL);
            tabReport.addRef("otherdnid", "otherdn", ReferenceType.DN.toString(), FieldType.OPTIONAL);
            tabReport.addRef("aniid", "ani", ReferenceType.DN.toString(), FieldType.OPTIONAL);
            tabReport.addRef("connectionidid", "connectionid", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
            tabReport.addRef("nameid", "\"First TEvent\"", ReferenceType.TEvent.toString(), FieldType.OPTIONAL);
            tabReport.addRef("appid", "\"First TServer\"", ReferenceType.App.toString(), FieldType.OPTIONAL);
            tabReport.setOrderBy(tabReport.getTabAlias() + ".started");
            tellProgress("Extracting all data");
            FullTableColors currTable = tabReport.getFullTable();

            return currTable; //To change body of generated methods, choose Tools | Templates.
        } finally {
            DynamicTreeNode.setNoRefNoLoad(false);

        }
    }

    @Override
    SearchFields getSearchField() {
        SearchFields ret = new SearchFields();
        ret.addRecMap(FileInfoType.type_CallManager, new Pair<>(SelectionType.CONNID, "connectionid"));
        return ret;
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new FileInfoType[]{FileInfoType.type_tController, FileInfoType.type_SipProxy}, searchApps);
//        return DatabaseConnector.getTimeRange(new String[]{"tlib_logbr", "sip_logbr"}, searchApps);
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return refreshTimeRange(null);
    }

    @Override
    public String getReportSummary() {
        return "TServer/SIP Server\n\t" + ((cidFinder != null) ? cidFinder.toString() : "<not specified>");
    }

    @Override
    public String getName() {
        return "TServer/SIP";
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
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_NAME, ReferenceType.TEvent, "tlib_logbr", "nameid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_ATTR1, ReferenceType.TLIBATTR1, "tlib_logbr", "attr1id");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_ATTR2, ReferenceType.TLIBATTR2, "tlib_logbr", "attr2id");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_DN, ReferenceType.DN, "tlib_logbr", "thisdnid", "otherdnid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_ANI, ReferenceType.DN, "tlib_logbr", "aniid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_DNIS, ReferenceType.DN, "tlib_logbr", "dnisid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_SOURCE, ReferenceType.App, "tlib_logbr", "sourceid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_AGENT, ReferenceType.Agent, "tlib_logbr", "agentidid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_Place, ReferenceType.Place, "tlib_logbr", "placeid");

        DynamicTreeNode<OptionNode> AttrValue;

        DynamicTreeNode<OptionNode> Attr;
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventQueued"));      
//        Attr.addChild(AttrValue);
//        AttrValue = new DynamicTreeNode<OptionNode>(new OptionNode(false, "EventRouteRequested"));      
//        Attr.addChild(AttrValue);

        Attr = new DynamicTreeNode<>(new OptionNode(true, "CallType"));
        nd.addChild(Attr);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Inbound"));
        Attr.addChild(AttrValue);
        AttrValue = new DynamicTreeNode<>(new OptionNode(true, "Outbound"));
        Attr.addChild(AttrValue);

        nd = new DynamicTreeNode<>(new OptionNode(DialogItem.TLIB_CALLS_TIMER));
        root.addChild(nd);
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TIMER_NAME, ReferenceType.TEvent, TableType.TLIBTimerRedirect.toString(), "msgid");
        nd.addDynamicRef(DialogItem.TLIB_CALLS_TIMER_CAUSE, ReferenceType.TEventRedirectCause, TableType.TLIBTimerRedirect.toString(), "causeID");

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

    private void loadStdOptions() throws SQLException {
        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>();
        repComponents.setRoot(rootA);

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.TLIB_CALLS));
        rootA.addChild(nd);

        addTLibReportType(nd);

        DynamicTreeNode<OptionNode> ndISCC = new DynamicTreeNode<>(new OptionNode(DialogItem.TLIB_CALLS_ISCC));
        nd.addChild(ndISCC);
        ndISCC.addDynamicRef(DialogItem.TLIB_CALLS_ISCC_NAME, ReferenceType.ISCCEvent, "iscc_logbr", "nameid");

        nd.addChild(new DynamicTreeNode<OptionNode>(new OptionNode(DialogItem.TLIB_CALLS_TSCP)));
        nd.addChild(new DynamicTreeNode<OptionNode>(new OptionNode(DialogItem.TLIB_CALLS_JSON)));

        if (DatabaseConnector.TableExist("sip_logbr")) {
            addSIPReportType(nd);
        }

        DynamicTreeNode<Object> node = getNode(DialogItem.SIP1536, TableType.SIP1536Other.toString());
        if (node != null) {
            rootA.addChild(node);
            DynamicTreeNode<Object> node1;
            node1 = getNode(DialogItem.SIP1536_SIP, TableType.SIP1536ReqResp.toString());
            if (node1 != null) {
                node.addChild(node1);
                node1.addDynamicRef(DialogItem.SIP1536_SIP_MESSAGE, ReferenceType.SIPMETHOD, TableType.SIP1536ReqResp.toString(), "msgid");
            }
            node1 = getNode(DialogItem.SIP1536_TRUNK, TableType.SIP1536Trunk.toString());
            if (node1 != null) {
                node.addChild(node1);
                node1.addDynamicRef(DialogItem.SIP1536_TRUNK_NAME, ReferenceType.DN, TableType.SIP1536Trunk.toString(), "trunkid");

            }
            node1 = getNode(DialogItem.SIP1536_MISC, TableType.SIP1536Other.toString());
            if (node1 != null) {
                node.addChild(node1);
                node1.addDynamicRef(DialogItem.SIP1536_MISC_KEY, ReferenceType.Misc, TableType.SIP1536Other.toString(), "keyid");
            }

        }

        addCustom(rootA, FileInfoType.type_CallManager);

        addConfigUpdates(rootA);

        rootA.addLogMessagesReportType(TableType.MsgTServer);
        DoneSTDOptions();
    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        if (appsType == null) {
//            appsType = getAppsType(FileInfoType.type_TransportLayer, new String[]{"tlib_logbr", "fileid", "sip_logbr", "fileid"});
            appsType = getAppsType(FileInfoType.type_CallManager);
            appsType.addAll(getAppsType(FileInfoType.type_SipProxy));

            return appsType;

        }
        return appsType;
    }

    void SetConfig(InquirerCfg cr) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
//    private UTCTimeRange timeRange = null;

    @Override
    public void Retrieve(QueryDialog dlg) throws SQLException {
        if (dlg.getSearchApps() == null) {
            inquirer.logger.info("No apps selected. Nothing to search in");
            return;
        }

        cidFinder = new IDsFinder(dlg);
        runSelectionQuery(dlg, dlg.getSelectionType(), cidFinder);

    }


    private void RetrieveSIP(QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(reportSettings) && DatabaseConnector.TableExist("sip_logbr")) {
            tellProgress("Retrieving SIP messages");
            inquirer.logger.debug("SIP report");
            if (inquirer.getCr().isNewTLibSearch() && (cidFinder != null && cidFinder.getSearchType() != SelectionType.NO_SELECTION)) {
                    Integer[] iDs = cidFinder.getIDs(IDType.SIPCallID);
                    if (iDs == null || iDs.length == 0) {
                        return;
                    }

            }
            SipForScCmQuery sipMsgsByCallid = new SipForScCmQuery(reportSettings, cidFinder, true);
            sipMsgsByCallid.setCommonParams(this, dlg);
            getRecords(sipMsgsByCallid);

            handlerIDs.addAll(sipMsgsByCallid.getHandlerIDs());

//            m_sipMsgHash = sipMsgsByCallid.getSipRecords();
        }
    }

    private void RetrieveTLib(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("tlib_logbr")) {

            HashSet<Long> refIDs = new HashSet<>();
            ArrayList<Long> ids = new ArrayList<>();
            HashSet<Long> idDNs = new HashSet<>();

            tellProgress("Retrieve TLIb messages");

            inquirer.logger.debug("TLib report");
            TlibForScCmQuery tlibQuery = new TlibForScCmQuery(eventsSettings, cidFinder, true);
            tlibQuery.setCommonParams(this, dlg);
            tlibQuery.setRecLoadedProc((ILogRecord rec) -> {
                if (rec instanceof TLibEvent) {
                    addUnique(refIDs, ((TLibEvent) rec).GetReferenceId());
                    long id = rec.getID();
                    if (id > 0) {
                        ids.add(id);
                    }
                    addUnique(idDNs, ((TLibEvent) rec).getThisDNID(), ((TLibEvent) rec).getOtherDNID());
                }


            });
            getRecords(tlibQuery);
            handlerIDs.addAll(tlibQuery.getHandlerIDs());

            if (cidFinder != null && callRelatedSearch(cidFinder) && !refIDs.isEmpty()) {
                inquirer.logger.debug("TLib requests");
                tlibQuery = new TlibForScCmQuery(refIDs, ids, idDNs);
                tlibQuery.setCommonParams(this, dlg);

                ArrayList<Long> idsReqs = new ArrayList<>();

                tlibQuery.setRecLoadedProc((ILogRecord rec) -> {
                    if (rec instanceof TLibEvent) {
                        long id = rec.getID();
                        if (id > 0) {
                            idsReqs.add(id);
                        }
                    }

                });
                getRecords(tlibQuery);
                if (!idsReqs.isEmpty()) {
                    Integer[] textID = DatabaseConnector.getIDs("trigger_logbr", "textid", getWhere("tlibid", idsReqs, true));
                    if (textID != null && textID.length > 0) {
                        handlerIDs.addAll(DatabaseConnector.getIDs("handler_logbr", "id", getWhere("textid", textID, true)));

                    }
                }

//                Integer[] connIDs = cidFinder.getConnIDs();
//                if (connIDs != null && connIDs.length > 0 && DatabaseConnector.TableExist(ReferenceType.TEvent.toString())) {
//                    tellProgress("Retrieve TLib requests");
//
//                    TableQuery TLibReq = MakeTLibReq();
//                    TLibReq.addWhere(TLibReq.getTabAlias() + ".connectionidid is null and \n"
//                            + getWhere(TLibReq.getTabAlias() + ".nameid", ReferenceType.TEvent, RequestsToShow, false)
//                            + "\n and \n"
//                            + getWhere(TLibReq.getTabAlias() + ".referenceid",
//                                    MakeQueryID("referenceid", "TLIB_logbr",
//                                            "\nwhere "
//                                            + getWhere("connectionidid", connIDs, false) + " and "
//                                            + "\n\t referenceid>0"
//                                            + "\n\tand "
//                                            + getWhere("nameid", ReferenceType.TEvent, EventsToShow, false)), false),
//                            "");
//
//                    TLibReq.addNullField("TransferID");
//                    TLibReq.addNullField("ErrMessage");
//                    TLibReq.addNullField("ixnid");
//                    TLibReq.addNullField("agentid");
//                    TLibReq.addNullField("typestr");
//                    TLibReq.AddCheckedWhere("attr1id", ReferenceType.TLIBATTR1, eventsSettings, "AND", DialogItem.TLIB_CALLS_TEVENT_ATTR1);
//                    TLibReq.AddCheckedWhere("attr2id", ReferenceType.TLIBATTR2, eventsSettings, "AND", DialogItem.TLIB_CALLS_TEVENT_ATTR2);
//
//                    TLibReq.setCommonParams(this, dlg);
//                    if (cidFinder != null) {
//                        TLibReq.setSearchApps(cidFinder.searchApps);
//                    }
//                    getRecords(TLibReq);
//                }
            }
        }
    }

    private void RetrieveTLibTimer(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        Integer[] connIDs;
        if (cidFinder == null) {

        } else {
            connIDs = cidFinder.getIDs(IDType.ConnID);
            if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.TLIBTimerRedirect.toString())) {
                TableQuery TLibTimer;
                if (DatabaseConnector.TableExist(TableType.TLIBTimerRedirect.toString())
                        && (cidFinder.getSearchType() == SelectionType.NO_SELECTION
                            || !isEmpty(connIDs))) {
                        TLibTimer = new TableQuery(MsgType.TLIBTimer, TableType.TLIBTimerRedirect.toString());
                        if (connIDs != null) {
                            TLibTimer.addWhere(getWhere("connIDID", connIDs, false), "AND");
                        }

                        TLibTimer.addRef("msgid", "message", ReferenceType.TEvent.toString(), IQuery.FieldType.MANDATORY);
                        TLibTimer.addRef("connIDID", "connID", ReferenceType.ConnID.toString(), IQuery.FieldType.OPTIONAL);
                        TLibTimer.addRef("causeID", "cause", ReferenceType.TEventRedirectCause.toString(), IQuery.FieldType.OPTIONAL);
                        TLibTimer.setCommonParams(this, dlg);

                        getRecords(TLibTimer);

                }
            }
        }
    }

    private void RetrieveCommonLib(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("connid_logbr")) {
            TableQuery tscpCall = new TableQuery(MsgType.TSCPCALL, "connid_logbr");
            Integer[] connIDs;
            if (cidFinder != null) {
                if (inquirer.getCr().isNewTLibSearch()) {
                    connIDs = cidFinder.getIDs(IDType.ConnID);
                    if (cidFinder.getSearchType() != SelectionType.NO_SELECTION && isEmpty(connIDs)) {
                        return;
                    }
                    tscpCall.addWhere(getWhere("ConnectionIdid", connIDs, false), "AND");

                } else {
                    connIDs = cidFinder.getConnIDs();
                    if (cidFinder.getSearchType() != SelectionType.NO_SELECTION && isEmpty(connIDs)) {
                        return;
                    }
                    tscpCall.addWhere(getWhere("ConnectionIdid", connIDs, false), "AND");

                }
            }

            tellProgress("Retrieve commonlib");

            tscpCall.addRef("ConnectionIDID", "ConnectionID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
            tscpCall.addRef("newConnIdID", "newConnID", ReferenceType.ConnID.toString(), FieldType.OPTIONAL);
            tscpCall.addNullField("SipId");
            tscpCall.setCommonParams(this, dlg);
            getRecords(tscpCall);
        }
    }

    private void RetrieveSIPJson(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("json_logbr")) {
            tellProgress("Retrieving JSON messages");
            if (inquirer.getCr().isNewTLibSearch()
                    && (cidFinder != null && cidFinder.getSearchType() != SelectionType.NO_SELECTION)) {
                    Integer[] iDs = cidFinder.getIDs(IDType.JSONID);
                    if (iDs == null || iDs.length == 0) {
                        return;
                    }

            }

            JsonQuery jq = new JsonQuery(cidFinder);

            jq.setCommonParams(this, dlg);
            getRecords(jq);
        }
    }

    private void RetrieveSIPDiagnostics(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.SIP1536Other.toString())) {
            DynamicTreeNode<OptionNode> node;
            TableQuery sip1536Table;
            tellProgress("Retrieve SIP Diagnostics");

            node = FindNode(eventsSettings, DialogItem.SIP1536_MISC, null, null);
            if (isChecked(node)) {
                sip1536Table = new TableQuery(MsgType.SIP1536Other, TableType.SIP1536Other.toString());

                for (int i = 1; i <= com.myutils.logbrowser.indexer.SingleThreadParser.MAX_1536_ATTRIBUTES; i++) {
                    String fldName = "attr" + i;
                    sip1536Table.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.OPTIONAL);
//                fldName = "value" + i;
//                ORSAlarm.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);
                }
                sip1536Table.addRef("keyid", "key", ReferenceType.Misc.toString(), IQuery.FieldType.OPTIONAL);
                sip1536Table.AddCheckedWhere(sip1536Table.getTabAlias() + ".keyid",
                        ReferenceType.Misc,
                        node,
                        "AND",
                        DialogItem.SIP1536_MISC_KEY);
                sip1536Table.setCommonParams(this, dlg);
                getRecords(sip1536Table);
            }

            node = FindNode(eventsSettings, DialogItem.SIP1536_TRUNK, null, null);
            if (isChecked(node)) {
                sip1536Table = new TableQuery(MsgType.SIP1536Trunk, TableType.SIP1536Trunk.toString());

                for (int i = 1; i <= com.myutils.logbrowser.indexer.SingleThreadParser.MAX_1536_ATTRIBUTES; i++) {
                    String fldName = "attr" + i;
                    sip1536Table.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.OPTIONAL);
//                fldName = "value" + i;
//                ORSAlarm.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);
                }
                sip1536Table.addRef("trunkid", "DN", ReferenceType.DN.toString(), IQuery.FieldType.OPTIONAL);
                sip1536Table.AddCheckedWhere(sip1536Table.getTabAlias() + ".trunkid",
                        ReferenceType.DN,
                        node,
                        "AND",
                        DialogItem.SIP1536_TRUNK_NAME);

                sip1536Table.setCommonParams(this, dlg);
                getRecords(sip1536Table);
            }

            node = FindNode(eventsSettings, DialogItem.SIP1536_SIP, null, null);
            if (isChecked(node)) {
                sip1536Table = new TableQuery(MsgType.SIP1536SIPStatistics, TableType.SIP1536ReqResp.toString());

                for (int i = 1; i <= com.myutils.logbrowser.indexer.SingleThreadParser.MAX_1536_ATTRIBUTES; i++) {
                    String fldName = "attr" + i;
                    sip1536Table.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.OPTIONAL);
//                fldName = "value" + i;
//                ORSAlarm.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);
                }
                sip1536Table.addRef("msgid", "name", ReferenceType.SIPMETHOD.toString(), IQuery.FieldType.OPTIONAL);

                sip1536Table.AddCheckedWhere(sip1536Table.getTabAlias() + ".msgid",
                        ReferenceType.SIPMETHOD,
                        node,
                        "AND",
                        DialogItem.SIP1536_SIP_MESSAGE);
                sip1536Table.addOutField("constToStr(\"IsRequest \", isrequest) isrequeststr"); // bug in SQLite lib; does not accept full word

                sip1536Table.setCommonParams(this, dlg);
                getRecords(sip1536Table);
            }

        }
    }

    private void RetrieveISCC(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist("iscc_logbr")) {
            tellProgress("Retrieve ISCC messages");

            IsccQuery query = new IsccQuery(eventsSettings, cidFinder, dlg, this);
            getRecords(query);
            HashSet<Long> tmpIDs = query.getTmpIDs();
            if (tmpIDs != null && !tmpIDs.isEmpty()) {
                query = new IsccQuery(eventsSettings, tmpIDs, dlg, this);
                getRecords(query);
            }
        }
    }

    private void runSelectionQuery(QueryDialog dlg, SelectionType selectionType, IDsFinder cidFinder) throws SQLException {
        handlerIDs.clear();
        this.cidFinder = cidFinder;
        if (selectionType != SelectionType.NO_SELECTION) {
            if (!cidFinder.initSearch()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            } else {
                setObjectsFound(cidFinder.getObjectsFound());
            }
        }
//        inquirer.logger.debug("new IDs: " + cidFinder.getIDs(IDType.ConnID));

//        doRetrieve(dlg, cidFinder);
        boolean runQueries = true;
        if (selectionType != SelectionType.REFERENCEID) {
            try {
                if (selectionType != SelectionType.NO_SELECTION && !cidFinder.anythingSIPRelated()
                        && DatabaseConnector.TableExist("sip_logbr")) {
                    if (!isSuppressConfirms()) {
                        int dialogResult = inquirer.showConfirmDialog(dlg, "No SIP calls found.\n"
                                + "Do you want to show all SIP messages?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (dialogResult == JOptionPane.CANCEL_OPTION) {
                            return;
                        }
                        if (dialogResult == JOptionPane.NO_OPTION) {
                            runQueries = false;
                        }
                    } else {
                        runQueries = false;
                    }
                }
            } catch (SQLException sQLException) {
                inquirer.logger.error("err", sQLException);
            }
            IQuery.reSetQueryNo();

            if (runQueries) {
                RetrieveSIP(dlg,
                        FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_SIP, null),
                        cidFinder);
                RetrieveSIPJson(dlg,
                        FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_JSON, null),
                        cidFinder);
            }
        }

        boolean canRunTLib = true;
        if (cidFinder != null && cidFinder.getSearchType() != SelectionType.NO_SELECTION
                && !cidFinder.AnythingTLibRelated()) {
            if (!isSuppressConfirms()) {
                int dialogResult = inquirer.showConfirmDialog(dlg, "No TLib calls found.\n"
                        + "Do you want to show all TLib messages?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
                if (dialogResult == JOptionPane.CANCEL_OPTION) {
                    return;
                }
                if (dialogResult == JOptionPane.NO_OPTION) {
                    canRunTLib = false;
                }
            } else {
                canRunTLib = false;
            }
        }

        if (canRunTLib) {
            RetrieveTLib(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TEVENT, null),
                    cidFinder);
            RetrieveTLibTimer(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TIMER, null),
                    cidFinder);

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
            RetrieveCommonLib(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_TSCP, null),
                    cidFinder);
            RetrieveISCC(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_ISCC, null),
                    cidFinder);
        }
        getCustom(FileInfoType.type_CallManager, dlg, repComponents.getRoot(), cidFinder, handlerIDs);
        getGenesysMessages(TableType.MsgTServer, repComponents.getRoot(), dlg, this);
        getConfigMessages(repComponents.getRoot(), dlg, this);
        RetrieveSIPDiagnostics(dlg,
                FindNode(repComponents.getRoot(), DialogItem.SIP1536, null, null),
                cidFinder);
        if (inquirer.getCr().isSorting()) {
            Sort();
        }

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
        ret.add(new AggrTimestampDelays());
        ret.add(new AggrSIPRetransmits());
        ret.add(new AggrJSONDuration());
        ret.add(new AggrSIPServerCallsPerSecond(true));

        return ret;
    }

    @Override
    boolean callRelatedSearch(IDsFinder cidFinder) throws SQLException {
        if (cidFinder != null) {
            return cidFinder.AnythingTLibRelated();
        }
        return true;
    }

    private class Handlers extends HashSet {

        @Override
        public boolean addAll(Collection c) {
            if (c != null) {
                return super.addAll(c); //To change body of generated methods, choose Tools | Templates.
            } else {
                return false;
            }
        }

        private void addAll(Integer[] hID) {
            if (hID != null && hID.length > 0) {
                handlerIDs.addAll(Arrays.asList(hID));
            }
        }

    }

}
