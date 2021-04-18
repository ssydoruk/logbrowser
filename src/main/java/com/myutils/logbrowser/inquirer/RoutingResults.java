/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import Utils.*;
import com.myutils.logbrowser.indexer.*;
import static com.myutils.logbrowser.inquirer.DatabaseConnector.TableExist;
import com.myutils.logbrowser.inquirer.IQuery.IRecordLoadedProc;
import static com.myutils.logbrowser.inquirer.IQuery.getCheckedWhere;
import static com.myutils.logbrowser.inquirer.IQueryResults.addUnique;
import static com.myutils.logbrowser.inquirer.QueryTools.FindNode;
import java.awt.event.*;
import java.beans.*;
import java.sql.*;
import java.util.*;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import org.apache.logging.log4j.*;

/**
 *
 * @author ssydoruk
 */
final public class RoutingResults extends IQueryResults {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    static private final String URS_LOG_MSG = "URS log messages";
    static private final String ORS_LOG_MSG = "ORS log messages";

    private UTCTimeRange timeRange;
    private DynamicTreeNode<OptionNode> msgUrsConfig;
    private DynamicTreeNode<OptionNode> msgOrsConfig;
    IDsFinder cidFinder = null;
    ArrayList<NameID> appsType = null;

    public RoutingResults() throws SQLException {
        super();
        boolean isORS = true;
        boolean isURS = true;
        try {
            isORS = (DatabaseConnector.fileExits(new FileInfoType[]{FileInfoType.type_ORS}));
        } catch (SQLException e) {
            logger.error("fatal: ", e);

        }
        try {
            isURS = (DatabaseConnector.fileExits(new FileInfoType[]{FileInfoType.type_URS, FileInfoType.type_URSHTTP}));
        } catch (SQLException e) {
            logger.error("fatal: ", e);
        }

        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.CONNID);
        addSelectionType(SelectionType.DN);
        addSelectionType(SelectionType.AGENT);
        if (isURS) {
            addSelectionType(SelectionType.SESSION);
        }
        addSelectionType(SelectionType.UUID);
        addSelectionType(SelectionType.IXN);
        addSelectionType(SelectionType.REFERENCEID);

        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        if (isURS) {
            addURSReportType(rootA);
        }

        if (isORS) {
            addORSReportType(rootA);
        }

        addConfigUpdates(rootA);

        try {
            if (isURS) {
                addCustom(rootA, FileInfoType.type_ORS);
            }
            if (isORS) {
                addCustom(rootA, FileInfoType.type_URS);
            }
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }
        if (isURS) {
            msgUrsConfig = rootA.getLogMessagesReportType(TableType.MsgURServer, URS_LOG_MSG);
            rootA.addChild(msgUrsConfig);
        }
        if (isORS) {
            msgOrsConfig = rootA.getLogMessagesReportType(TableType.MsgORServer, ORS_LOG_MSG);
            rootA.addChild(msgOrsConfig);

            if (DatabaseConnector.TableExist(TableType.ORSAlarm.toString())) {
                DynamicTreeNode<Object> tEventsNode = new DynamicTreeNode<>(new OptionNode(false, DialogItem.ORSALARM));
                rootA.addChild(tEventsNode);
//            tEventsNode.addDynamicRef(DialogItem.ORS_IXN_NAME, ReferenceType.TEvent, "orsmm", "nameid");
//        tEventsNode.addDynamicRef(DialogItem.ORS_TEVENTS_DN, ReferenceType.DN, "orsmm", "thisdnid", "otherdnid");
            }

        }
        DoneSTDOptions();

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
    public String getReportSummary() {
        return "ORS/URS \n\t" + (cidFinder != null ? "search: " + cidFinder.toString() : "");
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return refreshTimeRange(null);

    }

    @Override
    IDsFinder.RequestLevel getDefaultQueryLevel() {
        return IDsFinder.RequestLevel.Level2;
    }

    final public void addURSReportType(DynamicTreeNode<OptionNode> root) {
        DynamicTreeNode<OptionNode> reportTypeURSStrategy = new DynamicTreeNode<>(new OptionNode(true, DialogItem.URS));
        root.addChild(reportTypeURSStrategy);

        DynamicTreeNode<OptionNode> tEventsNode;
        try {
            if (DatabaseConnector.TableExist("urs_logbr")) {
                tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.URS_EVENTS));
                reportTypeURSStrategy.addChild(tEventsNode);

                tEventsNode.addDynamicRef(DialogItem.URS_EVENTS_NAME, ReferenceType.TEvent, "urs_logbr", "nameid");
                tEventsNode.addDynamicRef(DialogItem.URS_EVENTS_ATTR1, ReferenceType.TLIBATTR1, "urs_logbr", "attr1ID");

                tEventsNode.addDynamicRef(DialogItem.URS_EVENTS_DN, ReferenceType.DN, "urs_logbr", "thisdnid", "otherdnid");
                tEventsNode.addDynamicRef(DialogItem.URS_EVENTS_SERVER, ReferenceType.App, "urs_logbr", "sourceid");

                tEventsNode.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_ANI, ReferenceType.DN, "urs_logbr", "aniid");
                tEventsNode.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_DNIS, ReferenceType.DN, "urs_logbr", "dnisid");

            }
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }
        if (!DatabaseConnector.TableEmptySilent(TableType.URSGenesysMessage.toString())) {
            tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.URS_GENMESSAGE));
            reportTypeURSStrategy.addChild(tEventsNode);
            tEventsNode.addDynamicRef(DialogItem.URS_GENMESSAGE_TYPE, ReferenceType.MSGLEVEL);
            tEventsNode.addDynamicRef(DialogItem.URS_GENMESSAGE_MSG, ReferenceType.LOGMESSAGE,
                    TableType.URSGenesysMessage.toString(), "msgid");
        }
        try {
            if (DatabaseConnector.TableExist("ursstat")) {
                tEventsNode = new DynamicTreeNode<>(new OptionNode(false, DialogItem.URS_AGENTDN));
                reportTypeURSStrategy.addChild(tEventsNode);
                tEventsNode.addDynamicRef(DialogItem.URS_AGENTDN_NEWSTAT, ReferenceType.StatType, "ursstat", "statNewID");
                tEventsNode.addDynamicRef(DialogItem.URS_AGENTDN_STATSWITCH, ReferenceType.Switch, "ursstat", "VoiceSwitchID");
                tEventsNode.addDynamicRef(DialogItem.URS_AGENTDN_AGENT, ReferenceType.Agent, "ursstat", "AgentNameID");
            }
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }
        tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.URS_STRATEGY));
        reportTypeURSStrategy.addChild(tEventsNode);
        tEventsNode.addDynamicRef(DialogItem.URS_STRATEGY_MESSAGE, ReferenceType.URSStrategyMsg);
        tEventsNode.addDynamicRef(DialogItem.URS_STRATEGY_SCRIPT, ReferenceType.URSStrategyName);

        try {
            if (DatabaseConnector.TableExist("ursrlib")) {
                tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.URS_RLIB));
                reportTypeURSStrategy.addChild(tEventsNode);
                tEventsNode.addDynamicRef(DialogItem.URS_RLIB_METHOD, ReferenceType.ORSMETHOD);
            }
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }
        try {
            if (DatabaseConnector.TableExist("ursri")) {
                tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.URS_RI));
                reportTypeURSStrategy.addChild(tEventsNode);
                tEventsNode.addDynamicRef(DialogItem.URS_RI_FUNCTION, ReferenceType.URSMETHOD, "ursri", "funcID");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM1, ReferenceType.Misc, "ursri", "value1id");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM2, ReferenceType.Misc, "ursri", "value2id");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM3, ReferenceType.Misc, "ursri", "value3id");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM4, ReferenceType.Misc, "ursri", "value4id");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM5, ReferenceType.Misc, "ursri", "value5id");

//                tEventsNode.addDynamicRef(DialogItem.URS_RI_SUBFUNCTION, ReferenceType.URSMETHOD, "ursri", "subfuncID");
            }
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }
        try {
            if (DatabaseConnector.TableExist(TableType.URSHTTP)) {
                tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.URS_HTTP));
                reportTypeURSStrategy.addChild(tEventsNode);
                tEventsNode.addDynamicRef(DialogItem.URS_HTTP_METHOD, ReferenceType.HTTPMethod, TableType.URSHTTP.toString(), "methodID");
                tEventsNode.addDynamicRef(DialogItem.URS_HTTP_URI, ReferenceType.HTTPRequest, TableType.URSHTTP.toString(), "URIID");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM1, ReferenceType.Misc, TableType.URSHTTP.toString(), "value1id");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM2, ReferenceType.Misc, TableType.URSHTTP.toString(), "value2id");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM3, ReferenceType.Misc, TableType.URSHTTP.toString(), "value3id");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM4, ReferenceType.Misc, TableType.URSHTTP.toString(), "value4id");
                tEventsNode.addDynamicRef(DialogItem.URS_RI_PARAM5, ReferenceType.Misc, TableType.URSHTTP.toString(), "value5id");

//                tEventsNode.addDynamicRef(DialogItem.URS_RI_SUBFUNCTION, ReferenceType.URSMETHOD, "ursri", "subfuncID");
            }
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }

        tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.URS_VQ));
        reportTypeURSStrategy.addChild(tEventsNode);

    }

//    private static final String URS_RLIB = "RLIB";
    final public void addORSReportType(DynamicTreeNode<OptionNode> root) throws SQLException {
        DynamicTreeNode<OptionNode> Attr;
        DynamicTreeNode<OptionNode> tEventsNode;

        DynamicTreeNode<OptionNode> reportTypeORSStrategy = new DynamicTreeNode<>(new OptionNode(true, DialogItem.ORS));
        root.addChild(reportTypeORSStrategy);

        if (DatabaseConnector.TableExist("ors_logbr")) {
            tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.ORS_TEVENTS));
            reportTypeORSStrategy.addChild(tEventsNode);
            tEventsNode.addDynamicRef(DialogItem.ORS_TEVENTS_NAME, ReferenceType.TEvent, "ors_logbr", "nameid");
            tEventsNode.addDynamicRef(DialogItem.ORS_TEVENTS_DN, ReferenceType.DN, "ors_logbr", "thisdnid", "otherdnid");
            tEventsNode.addDynamicRef(DialogItem.ORS_TEVENTS_SOURCE, ReferenceType.App, "ors_logbr", "sourceID");
            tEventsNode.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_ANI, ReferenceType.DN, "ors_logbr", "aniid");
            tEventsNode.addDynamicRef(DialogItem.TLIB_CALLS_TEVENT_DNIS, ReferenceType.DN, "ors_logbr", "dnisid");

        }

        if (DatabaseConnector.TableExist("orsmm")) {
            tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.ORS_IXN));
            reportTypeORSStrategy.addChild(tEventsNode);
            tEventsNode.addDynamicRef(DialogItem.ORS_IXN_NAME, ReferenceType.TEvent, "orsmm", "nameid");
//        tEventsNode.addDynamicRef(DialogItem.ORS_TEVENTS_DN, ReferenceType.DN, "orsmm", "thisdnid", "otherdnid");
        }

        DynamicTreeNode<OptionNode> strategy = new DynamicTreeNode<>(new OptionNode(true, DialogItem.ORS_STRATEGY));
        reportTypeORSStrategy.addChild(strategy);
        strategy.addDynamicRef(DialogItem.ORS_STRATEGY_METRIC, ReferenceType.METRIC);
        strategy.addDynamicRef(DialogItem.ORS_STRATEGY_METRICPARAM1, ReferenceType.METRIC_PARAM1);
        strategy.addDynamicRef(DialogItem.ORS_STRATEGY_METRICEXT, ReferenceType.METRICFUNC);

        Attr = new DynamicTreeNode<>(new OptionNode(true, DialogItem.ORS_SESSION));
        reportTypeORSStrategy.addChild(Attr);

        if (DatabaseConnector.TableExist("orshttp")) {
            Attr = new DynamicTreeNode<>(new OptionNode(true, DialogItem.ORS_HTTP));
            reportTypeORSStrategy.addChild(Attr);
        }
        if (DatabaseConnector.TableExist("orsurs_logbr")) {
            strategy = new DynamicTreeNode<>(new OptionNode(true, DialogItem.ORS_ORSURS));
            reportTypeORSStrategy.addChild(strategy);
            strategy.addDynamicRef(DialogItem.ORS_ORSURS_METHOD, ReferenceType.ORSMETHOD);
        }

    }

    @Override
    FullTableColors getAll(QueryDialog qd) throws SQLException {
        try {
            String tmpTable = "callFlowTmp";
            DynamicTreeNode.setNoRefNoLoad(true);

            DatabaseConnector.dropTable(tmpTable);
            inquirer.logger.info("Building temp tables");
            String tab = "urs_logbr";

            tellProgress("Creating temp table");
            DatabaseConnector.runQuery("create temp table " + tmpTable + " ("
                    + "connectionidid int"
                    + ",started timestamp"
                    + ",ended timestamp"
                    + ",rowtype int"
                    + ",appid int"
                    + ",thisdnid int"
                    + ",otherdnid int"
                    + ",nameid int"
                    + ",isconsult bit"
                    + ",uuidid int"
                    + ",ixnidid int"
                    + ",strnameid int"
                    + ",urlid int"
                    + ",strstarted timestamp"
                    + ",strended timestamp"
                    + ",sidid timestamp"
                    + ",seqno integer"
                    + ", sourceid int\n"
                    + ")\n;"
            );

//<editor-fold defaultstate="collapsed" desc="URS connIDs">
            Wheres wh = new Wheres();
            wh.addWhere(IQuery.getCheckedWhere("ThisDNid", ReferenceType.DN,
                    FindNode(repComponents.getRoot(), DialogItem.URS, DialogItem.URS_EVENTS, DialogItem.URS_EVENTS_DN)), "OR");
            wh.addWhere(IQuery.getCheckedWhere("otherDNID", ReferenceType.DN,
                    FindNode(repComponents.getRoot(), DialogItem.URS, DialogItem.URS_EVENTS, DialogItem.URS_EVENTS_DN)), "OR");

            String makeWhere = wh.makeWhere(false);

            if (DatabaseConnector.TableExist(tab)) {
                tellProgress("Finding unique connIDs in URS");

                getAllResults.add(new Pair("Unique calls in URS",
                        DatabaseConnector.runQuery("insert into " + tmpTable + " (connectionidid, started, ended)"
                                + "\nselect distinct connectionidid, min(time), max(time) from " + tab
                                + "\nwhere connectionidid >0  "
                                + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                                + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                                + ((makeWhere != null && !makeWhere.isEmpty()) ? " and (" + makeWhere + ")"
                                : "")
                                + "\ngroup by 1"
                                + IQuery.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                                + ";")));

                DatabaseConnector.runQuery(" update " + tmpTable + " set rowtype=" + FileInfoType.type_URS.getValue());

                DatabaseConnector.runQuery("create index idx_" + tmpTable + "connID on " + tmpTable + "(connectionidid);");
                DatabaseConnector.runQuery("create index idx_" + tmpTable + "started on " + tmpTable + "(started);");

                tellProgress("Updating parameters of URS calls");
                DatabaseConnector.runQuery(""
                        + "update " + tmpTable
                        + "\nset ("
                        + "nameid"
                        + ", thisdnid"
                        + ", appid"
                        + ", otherdnid"
                        + ", uuidid"
                        + ", ixnidid"
                        + ", sourceid"
                        + ", isconsult"
                        + ", seqno"
                        + ") = "
                        + "\n("
                        + "select "
                        + "nameid"
                        + ", thisdnid "
                        + ", appid "
                        + ", otherdnid"
                        + ", uuidid"
                        + ", ixnidid"
                        + ", sourceid"
                        + ", transferidid>0 and transferidid<>connectionidid "
                        + ", seqno"
                        + "\nfrom\n"
                        + "(select "
                        + tab + ".*"
                        + ",file_logbr.appnameid as appid"
                        + "\nfrom " + tab
                        + " inner join file_logbr on " + tab + ".fileid=file_logbr.id"
                        + ") as " + tab
                        + "\nwhere "
                        + tmpTable + ".connectionidid=" + tab + ".connectionidid"
                        + "\nand\n"
                        + tmpTable + ".started=" + tab + ".time"
                        + "\n" + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                        + "\n" + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                        + ")"
                        + ";");
            }

            tab = "ursstrinit_logbr";
            tellProgress("Finding strategy init parameters");

            if (DatabaseConnector.TableExist(tab)) {
                String tmpTable1 = tmpTable + "1";
                DatabaseConnector.dropTable(tmpTable1);
                DatabaseConnector.runQuery(""
                        + "create temp table "
                        + tmpTable1
                        + " as  SELECT\n"
                        + "connidid\n"
                        + ", max(time) as strended \n"
                        + "    FROM\n"
                        + tab
                        + " where (strategynameid <=1 or strategynameid is null)"
                        + "\n" + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                        + "\n" + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                        + "\ngroup by 1\n"
                        + IQuery.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                        + "");
                DatabaseConnector.runQuery("create index idx_" + tmpTable1 + "connID on " + tmpTable1 + "(connidid);");

                DatabaseConnector.runQuery(""
                        + "update " + tmpTable
                        + "\nset ("
                        + "strended"
                        + ") = "
                        + "\n( select strended"
                        + "\nfrom "
                        + tmpTable1 + " as a "
                        + "\nwhere "
                        + tmpTable + ".connectionidid=a.connidid"
                        + ")"
                        + ";");

                DatabaseConnector.dropTable(tmpTable1);
                DatabaseConnector.runQuery(""
                        + "create temp table "
                        + tmpTable1
                        + " as  SELECT\n"
                        + "connidid\n"
                        + ",min(time) as strstarted\n"
                        + "    FROM\n"
                        + tab
                        + " where strategynameid>0"
                        + "\n" + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                        + "\n" + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                        + "\ngroup by 1\n"
                        + IQuery.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                        + "");
                DatabaseConnector.runQuery("create index idx_" + tmpTable1 + "connID on " + tmpTable1 + "(connidid);");
                DatabaseConnector.runQuery("create index idx_" + tmpTable1 + "strstarted on " + tmpTable1 + "(strstarted);");

                DatabaseConnector.runQuery(""
                        + "update " + tmpTable
                        + "\nset ("
                        + "strstarted"
                        + ") = "
                        + "\n( select strstarted "
                        + "\nfrom "
                        + tmpTable1 + " as a "
                        + "\nwhere "
                        + tmpTable + ".connectionidid=a.connidid"
                        + ")"
                        + ";");

                DatabaseConnector.runQuery(""
                        + "update " + tmpTable
                        + "\nset ("
                        + "strnameid"
                        + ") = "
                        + "\n( select strategynameid "
                        + "\nfrom "
                        + tab + " as t "
                        + "\n inner join " + tmpTable1 + " as t1 "
                        + " on t.connidid=t1.connidid and t1.strstarted=t.time"
                        + " where connectionidid=t1.connidid"
                        + ")"
                        + ";");
            }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="ORS Session IDs">
            if (DatabaseConnector.TableExist("orssess_logbr")) {
                wh = new Wheres();
                wh.addWhere(IQuery.getCheckedWhere("ThisDNid", ReferenceType.DN,
                        FindNode(repComponents.getRoot(), DialogItem.ORS, DialogItem.ORS_TEVENTS, DialogItem.ORS_TEVENTS_DN)), "OR");
                wh.addWhere(IQuery.getCheckedWhere("otherDNID", ReferenceType.DN,
                        FindNode(repComponents.getRoot(), DialogItem.ORS, DialogItem.ORS_TEVENTS, DialogItem.ORS_TEVENTS_DN)), "OR");

                makeWhere = wh.makeWhere(false);

                tellProgress("Finding unique voice sessions in ORS");
                tab = "orssess_logbr";
                getAllResults.add(new Pair("Unique voice sessions in ORS",
                        DatabaseConnector.runQuery("insert into " + tmpTable + " (appid, sidid, uuidid, strnameid, urlid, started, rowtype)"
                                + "\nselect appid, sidid, uuidid, appid, urlid, time,"
                                + FileInfoType.type_ORS.getValue()
                                + "\nfrom "
                                + "(select "
                                + tab + ".*"
                                + ",file_logbr.appnameid as appid"
                                + "\nfrom " + tab
                                + " inner join file_logbr on " + tab + ".fileid=file_logbr.id"
                                + ") as " + tab
                                + "\nwhere sidid >0  "
                                + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                                + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                                + ((makeWhere != null && !makeWhere.isEmpty()) ? " and (" + makeWhere + ")"
                                : "")
                                + IQuery.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                        )));
            }
            if (DatabaseConnector.TableExist("orssessixn")) {
                tellProgress("Finding interaction sessions in ORS");
                tab = "orssessixn";
                getAllResults.add(new Pair("Unique interaction sessions in ORS",
                        DatabaseConnector.runQuery("insert into " + tmpTable + " (appid, sidid, ixnidid, strnameid, urlid, started, rowtype)"
                                + "\nselect appid, sidid, ixnid, appid, urlid, time,"
                                + FileInfoType.type_ORS.getValue()
                                + "\nfrom "
                                + "(select "
                                + tab + ".*"
                                + ",file_logbr.appnameid as appid"
                                + "\nfrom " + tab
                                + " inner join file_logbr on " + tab + ".fileid=file_logbr.id"
                                + ") as " + tab
                                + "\nwhere sidid >0  "
                                + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                                + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                                + ((makeWhere != null && !makeWhere.isEmpty()) ? " and (" + makeWhere + ")"
                                : "")
                                + IQuery.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                        )));
            }
            if (DatabaseConnector.TableExist("orsmetr_logbr")) {
                /*Searching for sessions not created for voice nor multimedia interactions */
                tab = "orsmetr_logbr";
                tellProgress("Finding unique other sessions in ORS");
                getAllResults.add(new Pair("Unique other sessions in ORS",
                        DatabaseConnector.runQuery("insert into " + tmpTable + " (sidid, rowtype)"
                                + "\nselect sidid,"
                                + FileInfoType.type_ORS.getValue()
                                + "\nfrom "
                                + "(select "
                                + "fileid, metricid, sidid, time"
                                + ",file_logbr.appnameid as appid"
                                + "\nfrom " + tab
                                + " inner join file_logbr on " + tab + ".fileid=file_logbr.id"
                                + ") as " + tab
                                + "\nwhere sidid > 0  "
                                //                        + "\n and sidid not in (select sidid from orssess_logbr) "
                                //                        + "\n and sidid not in (select sidid from orssessixn) "
                                + (DatabaseConnector.TableExist("orssess_logbr") ? "\nand not exists (select sidid from orssess_logbr where sidid=orsmetr_logbr.sidid)" : "")
                                + (DatabaseConnector.TableExist("orssessixn") ? "\nand not exists (select sidid from orssessixn where sidid=orsmetr_logbr.sidid)" : "")
                                + "\nand " + getWhere("metricid", ReferenceType.METRIC, new String[]{"doc_request"}, false)
                                + IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false), "AND")
                                + IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange(), "AND")
                                + ((makeWhere != null && !makeWhere.isEmpty()) ? " and (" + makeWhere + ")"
                                : "")
                                + IQuery.getLimitClause(isLimitQueryResults(), getMaxQueryLines())
                        )));

                tellProgress("Updating ORS sessions parameters");
                tab = "orsmetr_logbr";
                if (DatabaseConnector.TableExist(ReferenceType.METRIC.toString())) {
                    DatabaseConnector.runQuery(""
                            + "update " + tmpTable
                            + "\nset ("
                            + "strstarted"
                            + ") = "
                            + "\n("
                            + "select "
                            + "time"
                            + "\nfrom "
                            + tab
                            + "\nwhere "
                            + tmpTable + ".sidid=" + tab + ".sidid"
                            + "\nand\n"
                            + IQuery.getWhere("metricid", ReferenceType.METRIC, "appl_begin", false)
                            + ")"
                            + ";");

                    DatabaseConnector.runQuery(""
                            + "update " + tmpTable
                            + "\nset ("
                            + "strended"
                            + ") = "
                            + "\n("
                            + "select "
                            + "time"
                            + "\nfrom "
                            + tab
                            + "\nwhere "
                            + tmpTable + ".sidid=" + tab + ".sidid"
                            + "\nand\n"
                            + IQuery.getWhere("metricid", ReferenceType.METRIC, "appl_end", false)
                            + ")"
                            + ";");
                }
            }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="indexes and extract">
            tellProgress("Creating indexes");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "appid on " + tmpTable + "(appid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "thisdnid on " + tmpTable + "(thisdnid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "otherdnid on " + tmpTable + "(otherdnid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "nameid on " + tmpTable + "(nameid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "uuidid on " + tmpTable + "(uuidid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "ixnidid on " + tmpTable + "(ixnidid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "sourceid on " + tmpTable + "(sourceid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "strnameid on " + tmpTable + "(strnameid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "sidid on " + tmpTable + "(sidid);");
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

            tellProgress("Extracting data");
            TableQuery tabReport = new TableQuery(tmpTable);
            tabReport.addOutField("UTCtoDateTime(started, \"YYYY-MM-dd HH:mm:ss.SSS\") started");
            tabReport.addOutField("UTCtoDateTime(strstarted, \"YYYY-MM-dd HH:mm:ss.SSS\") \"Strategy started\"");
            tabReport.addOutField("UTCtoDateTime(ended, \"YYYY-MM-dd HH:mm:ss.SSS\") ended");
            tabReport.addOutField("UTCtoDateTime(strended, \"YYYY-MM-dd HH:mm:ss.SSS\") \"Strategy ended\"");
            tabReport.addOutField("jduration(strended-strstarted) duration ");
            tabReport.addOutField("case isconsult when 1 then \"true\" when 0 then \"false\" end isconsult ");
            tabReport.addOutField("rowType");
            tabReport.setAddAll(false);
            tabReport.addRef("thisdnid", "thisdn", ReferenceType.DN.toString(), IQuery.FieldType.Optional);
            tabReport.addRef("otherdnid", "otherdn", ReferenceType.DN.toString(), IQuery.FieldType.Optional);
            tabReport.addRef("connectionidid", "connectionid", ReferenceType.ConnID.toString(), IQuery.FieldType.Optional);
            tabReport.addOutField("intToHex( seqno) seqnoHex"); // bug in SQLite lib; does not accept full word

            tabReport.addRef("uuidid", "uuid", ReferenceType.UUID.toString(), IQuery.FieldType.Optional);
            tabReport.addRef("ixnidid", "ixnid", ReferenceType.IxnID.toString(), IQuery.FieldType.Optional);
            tabReport.addRef("sourceid", "\"Source Server\"", ReferenceType.App.toString(), IQuery.FieldType.Optional);
            tabReport.addRef("nameid", "\"First TEvent\"", ReferenceType.TEvent.toString(), IQuery.FieldType.Optional);
            tabReport.addRef("strnameid", "Strategy", ReferenceType.URSStrategyName.toString(), IQuery.FieldType.Optional);
            tabReport.addRef("appid", "application", ReferenceType.App.toString(), IQuery.FieldType.Optional);
            tabReport.addRef("sidid", "sid", ReferenceType.ORSSID.toString(), IQuery.FieldType.Optional);
            tabReport.addRef("urlid", "url", ReferenceType.URSStrategyName.toString(), IQuery.FieldType.Optional);
            tabReport.setOrderBy(tabReport.getTabAlias() + ".started");
            FullTableColors currTable = tabReport.getFullTable();
            currTable.setHiddenField("rowType");
//</editor-fold>
            return currTable; //To change body of generated methods, choose Tools | Templates.
//        } catch (Exception ex) {
//            logger.error("fatal: ",  ex);
        } finally {
            DynamicTreeNode.setNoRefNoLoad(false);

        }
//        return null;
    }

    @Override
    void showAllResults() {
        showAllResults(getAllResults); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean runSelectionQuery(QueryDialog dlg, SelectionType selectionType, IDsFinder cidFinder) throws SQLException {
        this.cidFinder = cidFinder;
        timeRange = dlg.getTimeRange();

        inquirer.logger.debug("Retrieve URS");
        if (dlg.getSearchApps() == null) {
            inquirer.logger.info("No apps selected. Nothing to search in");
            return true;
        }
        if (selectionType != SelectionType.NO_SELECTION) {
            tellProgress("Finding selection");
            if (!cidFinder.initRouting()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + selectionType + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return true;
            } else {
                setObjectsFound(cidFinder.getObjectsFound());
            }

        } else {
            cidFinder = null;
        }
        inquirer.logger.debug("cidFinder: " + ((cidFinder == null) ? "[is null]" : cidFinder));
        if (!getURSdata(dlg,
                FindNode(repComponents.getRoot(), DialogItem.URS, null, null),
                cidFinder)) {
            return false;
        }
        if (!getORSdata(dlg,
                FindNode(repComponents.getRoot(), DialogItem.ORS, null, null),
                cidFinder)) {
            return false;
        }
        getCustom(FileInfoType.type_ORS, dlg, repComponents.getRoot(), cidFinder, null);
        getCustom(FileInfoType.type_URS, dlg, repComponents.getRoot(), cidFinder, null);

        return true;
    }

    @Override
    public void Retrieve(QueryDialog qd, SelectionType key, String searchID) throws SQLException {

        if (!runSelectionQuery(qd, key, new IDsFinder(qd, key, searchID))) {
            return;
        }
        Sort();

    }

    @Override
    SearchFields getSearchField() {
        SearchFields ret = new SearchFields();
        ret.addRecMap(com.myutils.logbrowser.indexer.FileInfoType.type_URS, new Pair<>(SelectionType.CONNID, "connectionid"));
        ret.addRecMap(com.myutils.logbrowser.indexer.FileInfoType.type_ORS, new Pair<>(SelectionType.SESSION, "sid"));
        ret.setTypeField("rowType");
        return ret;
    }

    @Override
    public void Retrieve(QueryDialog dlg) throws SQLException {

        if (!runSelectionQuery(dlg, dlg.getSelectionType(), new IDsFinder(dlg))) {
            return;
        }

        getGenesysMessagesApp(TableType.MsgURServer, FindNode(repComponents.getRoot(), URS_LOG_MSG, null, null), dlg, this);
        getGenesysMessagesApp(TableType.MsgORServer, FindNode(repComponents.getRoot(), ORS_LOG_MSG, null, null), dlg, this);

        getConfigMessages(repComponents.getRoot(), dlg, this);
        getORSAlarms(dlg,
                FindNode(repComponents.getRoot(), DialogItem.ORSALARM, null, null),
                cidFinder);
        Sort();
//        LookupMark();
    }

    private void getORSAlarms(QueryDialog dlg, DynamicTreeNode<OptionNode> node, IDsFinder cidFinder) throws SQLException {
        //<editor-fold defaultstate="collapsed" desc="ORSALARM">
        if (isChecked(node)
                && TableExist(TableType.ORSAlarm.toString())) {
            TableQuery ORSAlarm = new TableQuery(MsgType.ORSALARM, TableType.ORSAlarm.toString());

            tellProgress("Retrieving ORS self monitoring messages");

            for (int i = 1; i <= com.myutils.logbrowser.indexer.OrsParser.ORSAlarmTable.MAX_ALARMS; i++) {
                String fldName = "alarm" + i;
                ORSAlarm.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);
                fldName = "values" + i;
                ORSAlarm.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);
            }

            ORSAlarm.setCommonParams(this, dlg);
            getRecords(ORSAlarm);

        }
//</editor-fold>

    }

//    }
    @Override
    public String getName() {
        return "Routing";
    }

    private boolean getURSdata(QueryDialog dlg, DynamicTreeNode<OptionNode> ursReportSettings, IDsFinder cidFinder) throws SQLException {
        Integer[] SIDID = null;
        Integer[] IxnID = null;
        Integer[] CONNID = null;
        Integer[] UUID = null;
        DynamicTreeNode<OptionNode> childByName;
        ArrayList<Integer> searchApps = dlg.getSearchApps();

        if (!isChecked(ursReportSettings)) {
            return true;
        }

        if (cidFinder != null) {
            SIDID = cidFinder.getIDs(IDType.ORSSID);
            IxnID = cidFinder.getIDs(IDType.IxnID);
            CONNID = cidFinder.getIDs(IDType.ConnID);
            UUID = cidFinder.getIDs(IDType.UUID);
        }

        DynamicTreeNode<OptionNode> ursEventNode = FindNode(ursReportSettings, DialogItem.URS_EVENTS, null, null);
        if (isChecked(ursEventNode) && DatabaseConnector.TableExist("URS_logbr")) {

            if (cidFinder == null
                    || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                    || cidFinder.AnythingTLibRelated()) {

                UrsByConnIdQuery ursByConnIdQuery = new UrsByConnIdQuery(cidFinder);
                HashSet<Long> refIDs = new HashSet<>();
                ArrayList<Long> ids = new ArrayList<>();
                HashSet<Long> idDNs = new HashSet<>();

                tellProgress("Retrieving URS TLib messages");
                ursByConnIdQuery.setNode(ursEventNode);
                ursByConnIdQuery.setSearchSettings(ursEventNode);
                ursByConnIdQuery.setCommonParams(this, dlg);
                ursByConnIdQuery.setRecLoadedProc(new IQuery.IRecordLoadedProc() {
                    @Override
                    public void recordLoaded(ILogRecord rec) {
                        if (rec instanceof TLibEvent) {
                            addUnique(refIDs, ((TLibEvent) rec).GetReferenceId());

                            long id = rec.getID();
                            if (id > 0) {
                                ids.add(id);
                            }
                            addUnique(idDNs, ((TLibEvent) rec).getThisDNID(), ((TLibEvent) rec).getOtherDNID());
                        }
                    }
                });
                getRecords(ursByConnIdQuery);
                if (cidFinder != null && callRelatedSearch(cidFinder) && !refIDs.isEmpty()) {
                    inquirer.logger.debug("TLib requests");
                    ursByConnIdQuery = new UrsByConnIdQuery(refIDs, ids, new ArrayList<>(idDNs));
                    ursByConnIdQuery.setNode(ursEventNode);
                    ursByConnIdQuery.setCommonParams(this, dlg);
                    getRecords(ursByConnIdQuery);
                }

//            getURSTLibRequests(dlg, ursReportSettings, cidFinder);
            }

        }
        DynamicTreeNode<OptionNode> findNode = FindNode(ursReportSettings, DialogItem.URS_AGENTDN, null, null);
        if (isChecked(findNode) && DatabaseConnector.TableExist("ursstat")) {
            Integer[] agentIDs = null;
            if (cidFinder != null) {
                agentIDs = cidFinder.getIDs(IDType.AGENT);
            }
            if (cidFinder == null
                    || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                    || !isEmpty(agentIDs)) {
                TableQuery URSAgentPlace;
                URSAgentPlace = new TableQuery(MsgType.URSSTAT, "ursstat");

                tellProgress("Retrieving URS agent/place messages");

                URSAgentPlace.AddCheckedWhere(URSAgentPlace.getTabAlias() + ".statNewID",
                        ReferenceType.StatType,
                        findNode,
                        "AND",
                        DialogItem.URS_AGENTDN_NEWSTAT);

                URSAgentPlace.AddCheckedWhere(URSAgentPlace.getTabAlias() + ".VoiceSwitchID",
                        ReferenceType.Switch,
                        findNode,
                        "AND",
                        DialogItem.URS_AGENTDN_STATSWITCH);

                if (IQuery.isAllChecked(FindNode(findNode, DialogItem.URS_AGENTDN_AGENT, null, null))
                        && (agentIDs != null && agentIDs.length > 0)) {
                    URSAgentPlace.addWhere(getWhere("AgentNameID", agentIDs, false), "AND");

                } else {
                    URSAgentPlace.AddCheckedWhere(URSAgentPlace.getTabAlias() + ".AgentNameID",
                            ReferenceType.Agent,
                            findNode,
                            "AND",
                            DialogItem.URS_AGENTDN_AGENT);
                }

                URSAgentPlace.addRef("agentNameID", "agentName", ReferenceType.Agent.toString(), IQuery.FieldType.Optional);
                URSAgentPlace.addRef("placeNameID", "placeName", ReferenceType.Place.toString(), IQuery.FieldType.Optional);
                URSAgentPlace.addRef("ssNameID", "StatServer", ReferenceType.App.toString(), IQuery.FieldType.Optional);
                URSAgentPlace.addRef("statPrevID", "statPrev", ReferenceType.StatType.toString(), IQuery.FieldType.Optional);
                URSAgentPlace.addRef("statNewID", "statNew", ReferenceType.StatType.toString(), IQuery.FieldType.Optional);
                URSAgentPlace.addRef("VoiceSwitchID", "VoiceSwitch", ReferenceType.Switch.toString(), IQuery.FieldType.Optional);
                URSAgentPlace.addRef("VoiceDNID", "VoiceDN", ReferenceType.DN.toString(), IQuery.FieldType.Optional);
                URSAgentPlace.addRef("VoiceStatID", "VoiceStat", ReferenceType.StatType.toString(), IQuery.FieldType.Optional);
                URSAgentPlace.addRef("ChatStatID", "ChatStat", ReferenceType.StatType.toString(), IQuery.FieldType.Optional);
                URSAgentPlace.addRef("EmailStatID", "EmailStat", ReferenceType.StatType.toString(), IQuery.FieldType.Optional);

                URSAgentPlace.setCommonParams(this, dlg);
                getRecords(URSAgentPlace);
            }

        }

        if (isChecked(FindNode(ursReportSettings, DialogItem.URS_STRATEGY, null, null))
                && DatabaseConnector.TableExist("ursstr_logbr")) {
            TableQuery UrsStrategy;

            if (cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                    || !isEmpty(CONNID)
                    || !isEmpty(UUID)) {
                if (DatabaseConnector.TableExist("ursstr_logbr")) {
                    UrsStrategy = null;
                    if (cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION) {
                        UrsStrategy = new TableQuery(MsgType.URSSTRATEGY, "ursstr_logbr");
                    } else if (!isEmpty(CONNID) || !isEmpty(IxnID)) {
                        UrsStrategy = new TableQuery(MsgType.URSSTRATEGY, "ursstr_logbr");
                        Wheres wh = new Wheres();
                        wh.addWhere(getWhere("connidid", CONNID, false), "OR");
                        wh.addWhere(getWhere("ixnidid", IxnID, false), "OR");
                        UrsStrategy.addWhere(wh);
                    }

                    if (UrsStrategy == null) {
                        tellProgress("Not found related URS strategy messages");

                    } else {
                        tellProgress("Retrieving URS strategy messages");

                        UrsStrategy.addRef("connidid", "connid", ReferenceType.ConnID.toString(), IQuery.FieldType.Optional);
                        UrsStrategy.addRef("ref1id", "ref1", ReferenceType.URSStrategyRef.toString(), IQuery.FieldType.Optional);
                        UrsStrategy.addRef("ref2id", "ref2", ReferenceType.URSStrategyRef.toString(), IQuery.FieldType.Optional);
//                UrsStrategy.setSearchApps(searchApps);
                        UrsStrategy.AddCheckedWhere(UrsStrategy.getTabAlias() + ".strategymsgid",
                                ReferenceType.URSStrategyMsg,
                                FindNode(ursReportSettings, DialogItem.URS_STRATEGY, null, null),
                                "AND",
                                DialogItem.URS_STRATEGY_MESSAGE);

                        UrsStrategy.setCommonParams(this, dlg);
                        getRecords(UrsStrategy);
                    }
                }

                if (DatabaseConnector.TableExist("ursstrinit_logbr")) {
                    UrsStrategy = null;
                    if (cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION) {
                        UrsStrategy = new TableQuery(MsgType.URSSTRATEGYINIT, "ursstrinit_logbr");
                    } else if (!isEmpty(CONNID) || !isEmpty(IxnID)) {
                        UrsStrategy = new TableQuery(MsgType.URSSTRATEGYINIT, "ursstrinit_logbr");
                        Wheres wh = new Wheres();
                        wh.addWhere(getWhere("connidid", CONNID, false), "OR");
                        wh.addWhere(getWhere("ixnidid", IxnID, false), "OR");
                        UrsStrategy.addWhere(wh);
                    }

                    if (UrsStrategy == null) {
                        tellProgress("Not found related URS strategy init messages");

                    } else {
                        tellProgress("Retrieving URS strategy init messages");

                        UrsStrategy.addRef("connidid", "connid", ReferenceType.ConnID.toString(), IQuery.FieldType.Optional);
                        UrsStrategy.addRef("strategynameid", "strategyname", ReferenceType.URSStrategyName.toString(), IQuery.FieldType.Optional);
                        UrsStrategy.addRef("strategymsgid", "strategymsg", ReferenceType.URSStrategyMsg.toString(), IQuery.FieldType.Optional);
//                UrsStrategy.setSearchApps(searchApps);
                        UrsStrategy.AddCheckedWhere(UrsStrategy.getTabAlias() + ".strategynameid",
                                ReferenceType.URSStrategyName,
                                FindNode(ursReportSettings, DialogItem.URS_STRATEGY, null, null),
                                "AND",
                                DialogItem.URS_STRATEGY_SCRIPT);

                        UrsStrategy.setCommonParams(this, dlg);
                        getRecords(UrsStrategy);
                    }
                }

                if (DatabaseConnector.TableExist("urswaiting")) {
                    UrsStrategy = null;

                    if (cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION) {
                        UrsStrategy = new TableQuery(MsgType.URSWAITING, "urswaiting");
                    } else if (!isEmpty(CONNID)) {
                        UrsStrategy = new TableQuery(MsgType.URSWAITING, "urswaiting");
                        Wheres wh = new Wheres();
                        wh.addWhere(getWhere("connidid", CONNID, false), "OR");
                        UrsStrategy.addWhere(wh);
                    }

                    if (UrsStrategy == null) {
                        tellProgress("Not found related URS strategy urswaiting messages");

                    } else {
                        tellProgress("Retrieving URS strategy urswaiting messages");
                        UrsStrategy.addWhere(getWhere("connidid", CONNID, false), "AND");
                        UrsStrategy.addRef("connidid", "connid", ReferenceType.ConnID.toString(), IQuery.FieldType.Optional);
                        UrsStrategy.addRef("agentID", "object", ReferenceType.Agent.toString(), IQuery.FieldType.Optional);
                        UrsStrategy.addRef("agentTypeID", "ObjectType", ReferenceType.ObjectType.toString(), IQuery.FieldType.Optional);

                        UrsStrategy.setCommonParams(this, dlg);

                        getRecords(UrsStrategy);
                    }

                }

            }
        }

//<editor-fold defaultstate="collapsed" desc="ursrlib">
        if (isChecked(FindNode(ursReportSettings, DialogItem.URS_RLIB, null, null)) && TableExist("ursrlib")) {
            if (cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                    || !isEmpty(SIDID)
                    || !isEmpty(UUID)) {
                tellProgress("Retrieving URS RLib messages");
                TableQuery UrsRLib = newRLib(dlg, ursReportSettings);

                Wheres whIDs = null;
                HashSet<Long> refIDs = new HashSet<>();
                HashSet<Long> reqIDs = new HashSet<>();
                HashSet<Long> iDs = new HashSet<>();
                HashSet<Long> idFiles = new HashSet<>();

                if (SIDID != null || UUID != null) {
                    whIDs = new Wheres();
                    whIDs.addWhere(getWhere(UrsRLib.getTabAlias() + ".uuidid", UUID, false), "OR");
                    whIDs.addWhere(getWhere(UrsRLib.getTabAlias() + ".sidid", SIDID, false), "OR");
                    UrsRLib.addWhere(whIDs, "AND");

                    UrsRLib.setRecLoadedProc(new IRecordLoadedProc() {
                        @Override
                        public void recordLoaded(ILogRecord rec) {
                            addUnique(refIDs, Util.intOrDef(rec.GetField("refid"), (Long) null));
                            addUnique(reqIDs, Util.intOrDef(rec.GetField("reqid"), (Long) null));
                            addUnique(iDs, rec.getID());
                            addUnique(idFiles, new Long(rec.GetFileId()));
                        }

                    });
                }
                getRecords(UrsRLib);

                if (!refIDs.isEmpty()) {
                    UrsRLib = newRLib(dlg, ursReportSettings);
                    tellProgress("Retrieving URS RLib requests");
                    Wheres wh1 = new Wheres();
                    wh1.addWhere(getWhere(UrsRLib.fldName("refid"), refIDs, false));
//                    wh1.addWhere(getWhere(UrsRLib.fldName("fileid"), idFiles, false), "AND");
                    wh1.addWhere(getWhereNot(UrsRLib.fldName("id"), iDs, false), "AND");
                    UrsRLib.addWhere(wh1, "AND");

                    UrsRLib.setRecLoadedProc(new IRecordLoadedProc() {
                        @Override
                        public void recordLoaded(ILogRecord rec) {
                            addUnique(reqIDs, Util.intOrDef(rec.GetField("reqid"), (Long) null));
                            addUnique(iDs, rec.getID());
                            addUnique(idFiles, new Long(rec.GetFileId()));
                        }

                    });

                    getRecords(UrsRLib);
                }

                if (!reqIDs.isEmpty()) {
                    UrsRLib = newRLib(dlg, ursReportSettings);
                    tellProgress("Retrieving URS RLib requests");
                    Wheres wh1 = new Wheres();
                    wh1.addWhere(getWhere(UrsRLib.fldName("reqid"), reqIDs, false));
//                    wh1.addWhere(getWhere(UrsRLib.fldName("fileid"), idFiles, false), "AND");
                    wh1.addWhere(getWhereNot(UrsRLib.fldName("id"), iDs, false), "AND");
                    UrsRLib.addWhere(wh1, "AND");
                    getRecords(UrsRLib);
                }

            }

        }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="ursri">
        if (isChecked(FindNode(ursReportSettings, DialogItem.URS_RI, null, null)) && TableExist("ursri")) {
            if (cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                    || !isEmpty(CONNID) || !isEmpty(UUID)) {
                TableQuery URSRI = newURSRI(dlg);
                HashSet<Long> refIDs = new HashSet<>();
                HashSet<Long> iDs = new HashSet<>();
//                HashSet<Long> idFiles = new HashSet<>();
                boolean whereAdded = URSRI.AddCheckedWhere(URSRI.getTabAlias() + ".funcID",
                        ReferenceType.URSMETHOD,
                        FindNode(ursReportSettings, DialogItem.URS_RI, null, null),
                        "AND",
                        DialogItem.URS_RI_FUNCTION) != null;
                boolean v1added = URSRI.AddCheckedWhere(URSRI.getTabAlias() + ".value1id",
                        ReferenceType.Misc,
                        FindNode(ursReportSettings, DialogItem.URS_RI, null, null),
                        "AND",
                        DialogItem.URS_RI_PARAM1) == null;
                boolean v2added = URSRI.AddCheckedWhere(URSRI.getTabAlias() + ".value2id",
                        ReferenceType.Misc,
                        FindNode(ursReportSettings, DialogItem.URS_RI, null, null),
                        "AND",
                        DialogItem.URS_RI_PARAM2) == null;
                boolean v3added = URSRI.AddCheckedWhere(URSRI.getTabAlias() + ".value3id",
                        ReferenceType.Misc,
                        FindNode(ursReportSettings, DialogItem.URS_RI, null, null),
                        "AND",
                        DialogItem.URS_RI_PARAM3) == null;
                boolean v4added = URSRI.AddCheckedWhere(URSRI.getTabAlias() + ".value4id",
                        ReferenceType.Misc,
                        FindNode(ursReportSettings, DialogItem.URS_RI, null, null),
                        "AND",
                        DialogItem.URS_RI_PARAM4) == null;
                boolean v5added = URSRI.AddCheckedWhere(URSRI.getTabAlias() + ".value5id",
                        ReferenceType.Misc,
                        FindNode(ursReportSettings, DialogItem.URS_RI, null, null),
                        "AND",
                        DialogItem.URS_RI_PARAM5) == null;

                if (CONNID != null || UUID != null) {
                    Wheres wh1 = new Wheres();
                    wh1.addWhere(getWhere("connidid", CONNID, false));
                    wh1.addWhere(getWhere("uuidid", UUID, false), "OR");

                    URSRI.addWhere(wh1, "OR");

                }
                if (whereAdded || CONNID != null || UUID != null
                        || v1added || v2added || v3added || v4added || v5added) {
                    URSRI.setRecLoadedProc(new IRecordLoadedProc() {
                        @Override
                        public void recordLoaded(ILogRecord rec) {
                            addUnique(refIDs, Util.intOrDef(rec.GetField("ref"), (Long) null));
                            addUnique(iDs, rec.getID());
//                            addUnique(idFiles, rec.GetFileId());
                        }

                    });
                }

                tellProgress("Retrieving URS RI messages");
                getRecords(URSRI);
                if (!refIDs.isEmpty()) {
                    URSRI = newURSRI(dlg);
                    tellProgress("Retrieving URS RI requests");
                    Wheres wh1 = new Wheres();
                    wh1.addWhere(getWhere(URSRI.fldName("ref"), refIDs, false));
//                    wh1.addWhere(getWhere(URSRI.fldName("fileid"), idFiles, false), "AND");
                    wh1.addWhere(getWhereNot(URSRI.fldName("id"), iDs, false), "AND");
                    URSRI.addWhere(wh1, "AND");
                    getRecords(URSRI);
                }
            }

        }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="ursvq">
        if (isChecked(FindNode(ursReportSettings, DialogItem.URS_VQ, null, null)) && TableExist("ursvq")) {
            if (cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                    || !isEmpty(CONNID)) {
                TableQuery URSVQ = null;
                if (CONNID != null) {
                    URSVQ = new TableQuery(MsgType.URSVQ, "ursvq", "connidid", getWhere("connidid", CONNID, false));
                } else {
                    URSVQ = new TableQuery(MsgType.URSVQ, "ursvq");
                }
                if (URSVQ != null) {
                    tellProgress("Retrieving URS VQ messages");
                    URSVQ.addRef("connidid", "connid", ReferenceType.ConnID.toString(), IQuery.FieldType.Optional);
                    URSVQ.addRef("vqidid", "vqid", ReferenceType.VQID.toString(), IQuery.FieldType.Optional);
                    URSVQ.addRef("vqnameid", "vqname", ReferenceType.DN.toString(), IQuery.FieldType.Optional);
                    URSVQ.addRef("targetid", "target", ReferenceType.URSTarget.toString(), IQuery.FieldType.Optional);
                    URSVQ.setCommonParams(this, dlg);
                    getRecords(URSVQ);
                }
            }

        }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="urs_genesys_message">
//        if (!DatabaseConnector.TableEmptySilent(TableType.URSGenesysMessage.toString())) {
//            tEventsNode = new DynamicTreeNode<>(new OptionNode(true, DialogItem.URS_GENMESSAGE));
//            reportTypeURSStrategy.addChild(tEventsNode);
//            tEventsNode.addDynamicRef(DialogItem.URS_GENMESSAGE_TYPE, ReferenceType.MSGLEVEL);
//            tEventsNode.addDynamicRef(DialogItem.URS_GENMESSAGE_MSG, ReferenceType.LOGMESSAGE,
//                    TableType.URSGenesysMessage.toString(), "msgid");
//        }

        if (isChecked(FindNode(ursReportSettings, DialogItem.URS_GENMESSAGE, null, null))
                && TableExist(TableType.URSGenesysMessage.toString())) {
            tellProgress("Retrieving URS genesys messages");
            TableQuery URS_GENMSG = new TableQuery(MsgType.GENESYS_URS_MSG, TableType.URSGenesysMessage.toString());
            if (cidFinder != null && cidFinder.getSearchType() != SelectionType.NO_SELECTION
                    && !isEmpty(CONNID)) {
                URS_GENMSG.addWhere(getWhere("connidid", CONNID, false), "AND");
            }
            //todo: add filter on log level
//            URS_GENMSG.AddCheckedWhere(URS_GENMSG.getTabAlias() + ".levelID",
//                    ReferenceType.MSGLEVEL,
//                    FindNode(ursReportSettings, DialogItem.URS_GENMESSAGE, null, null),
//                    "AND",
//                    DialogItem.URS_GENMESSAGE_TYPE);
            URS_GENMSG.AddCheckedWhere(URS_GENMSG.getTabAlias() + ".msgID",
                    ReferenceType.LOGMESSAGE,
                    FindNode(ursReportSettings, DialogItem.URS_GENMESSAGE, null, null),
                    "AND",
                    DialogItem.URS_GENMESSAGE_MSG);

            URS_GENMSG.addRef("connidid", "connid", ReferenceType.ConnID.toString(), IQuery.FieldType.Optional);
            URS_GENMSG.addRef("levelid", "level", ReferenceType.MSGLEVEL.toString(), IQuery.FieldType.Optional);
            URS_GENMSG.addRef("msgid", "msg", ReferenceType.LOGMESSAGE.toString(), IQuery.FieldType.Optional);
            URS_GENMSG.setCommonParams(this, dlg);
            getRecords(URS_GENMSG);
        }

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="urshttp">
        if (isChecked(FindNode(ursReportSettings, DialogItem.URS_HTTP, null, null)) && TableExist(TableType.URSHTTP)) {
            if (cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                    || !isEmpty(CONNID)) {
                tellProgress("Retrieving URS http messages");

                TableQuery URSHTTP = newURSHTTP(dlg, ursReportSettings);
                TableQuery HTTPtoURS = newHTTPToURS(dlg, ursReportSettings);

                boolean addedFilter = false;
                addedFilter = addedFilter || null != URSHTTP.AddCheckedWhere(URSHTTP.getTabAlias() + ".methodid",
                        ReferenceType.HTTPMethod,
                        FindNode(ursReportSettings, DialogItem.URS_HTTP, null, null),
                        "AND",
                        DialogItem.URS_HTTP_METHOD);

                addedFilter = addedFilter || null != URSHTTP.AddCheckedWhere(URSHTTP.getTabAlias() + ".URIID",
                        ReferenceType.HTTPRequest,
                        FindNode(ursReportSettings, DialogItem.URS_HTTP, null, null),
                        "AND",
                        DialogItem.URS_HTTP_URI);

                HashSet<Long> httpHandlerID = new HashSet<>();
                HashSet<Long> iDs = new HashSet<>();
                HashSet<Long> idFiles = new HashSet<>();
                if (addedFilter) {
                    URSHTTP.setRecLoadedProc(new IRecordLoadedProc() {
                        @Override
                        public void recordLoaded(ILogRecord rec) {
                            addUnique(httpHandlerID, Util.intOrDef(rec.GetField("httpHandlerID"), (Long) null));
                            addUnique(iDs, rec.getID());
                            addUnique(idFiles, new Long(rec.GetFileId()));
                        }

                    });
                }
                getRecords(URSHTTP);

                if (!httpHandlerID.isEmpty()) {
                    URSHTTP = newURSHTTP(dlg, ursReportSettings);
                    tellProgress("Retrieving URS RLib requests");
                    Wheres wh1 = new Wheres();
                    wh1.addWhere(getWhere(URSHTTP.fldName("httpHandlerID"), httpHandlerID, false));
//                    wh1.addWhere(getWhere(UrsRLib.fldName("fileid"), idFiles, false), "AND");
                    wh1.addWhere(getWhereNot(URSHTTP.fldName("id"), iDs, false), "AND");
                    URSHTTP.addWhere(wh1, "AND");

                    getRecords(URSHTTP);

                    wh1 = new Wheres();
                    wh1.addWhere(getWhere(HTTPtoURS.fldName("httpHandlerID"), httpHandlerID, false));
                    HTTPtoURS.addWhere(wh1, "AND");
                    httpHandlerID.clear();
                    iDs.clear();

                    HTTPtoURS.setRecLoadedProc(new IRecordLoadedProc() {
                        @Override
                        public void recordLoaded(ILogRecord rec) {
                            addUnique(httpHandlerID, Util.intOrDef(rec.GetField("refID"), (Long) null));
                            addUnique(iDs, rec.getID());
                        }
                    });
                    getRecords(HTTPtoURS);
                    if (!httpHandlerID.isEmpty()) {
                        HTTPtoURS = newHTTPToURS(dlg, ursReportSettings);
                        wh1 = new Wheres();
                        wh1.addWhere(getWhere(HTTPtoURS.fldName("refID"), httpHandlerID, false));
//                    wh1.addWhere(getWhere(UrsRLib.fldName("fileid"), idFiles, false), "AND");
                        wh1.addWhere(getWhereNot(HTTPtoURS.fldName("id"), iDs, false), "AND");
                        HTTPtoURS.addWhere(wh1, "AND");

                        getRecords(HTTPtoURS);
                    }

                } else {
                    getRecords(HTTPtoURS);
                }

            }

        }
//</editor-fold>

        return true;
    }

    private TableQuery newHTTPToURS(QueryDialog dlg, DynamicTreeNode<OptionNode> ursReportSettings) throws SQLException {
        TableQuery HTTPtoURS = new TableQuery(MsgType.HTTPtoURS, TableType.HTTP_TO_URS);
//        HTTPtoURS.addRef("methodid", "method", ReferenceType.HTTPMethod.toString(), IQuery.FieldType.Optional);
//        HTTPtoURS.addRef("uriid", "uri", ReferenceType.HTTPRequest.toString(), IQuery.FieldType.Optional);

//                    URSHTTP.addRef("vqnameid", "vqname", ReferenceType.DN.toString(), IQuery.FieldType.Optional);
//                    URSHTTP.addRef("targetid", "target", ReferenceType.URSTarget.toString(), IQuery.FieldType.Optional);
        HTTPtoURS.setCommonParams(this, dlg);
        return HTTPtoURS;
    }

    private TableQuery newURSHTTP(QueryDialog dlg, DynamicTreeNode<OptionNode> ursReportSettings) throws SQLException {
        TableQuery URSHTTP = new TableQuery(MsgType.URSHTTP, TableType.URSHTTP);
        URSHTTP.addRef("methodid", "method", ReferenceType.HTTPMethod.toString(), IQuery.FieldType.Optional);
        URSHTTP.addRef("uriid", "uri", ReferenceType.HTTPRequest.toString(), IQuery.FieldType.Optional);

//                    URSHTTP.addRef("vqnameid", "vqname", ReferenceType.DN.toString(), IQuery.FieldType.Optional);
//                    URSHTTP.addRef("targetid", "target", ReferenceType.URSTarget.toString(), IQuery.FieldType.Optional);
        URSHTTP.setCommonParams(this, dlg);
        return URSHTTP;
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {

//                    appsType = getAppsType(FileInfoType.type_ORS);
//            appsType.addAll(getAppsType(FileInfoType.type_URS));
        return DatabaseConnector.getTimeRange(new FileInfoType[]{FileInfoType.type_ORS, FileInfoType.type_URS, FileInfoType.type_URSHTTP}, searchApps);
//        return DatabaseConnector.getTimeRange(new String[]{"urs_logbr", "ors_logbr", "ursstr_logbr", ",orsmetr_logbr"}, searchApps);
    }

    private boolean getORSdata(QueryDialog dlg, DynamicTreeNode<OptionNode> orsReportSettings, IDsFinder cidFinder) throws SQLException {
        Integer[] SIDID = null;
        Integer[] IxnID = null;
        Integer[] CONNID = null;
        Integer[] UUID = null;

        if (cidFinder != null) {
            SIDID = cidFinder.getIDs(IDType.ORSSID);
            IxnID = cidFinder.getIDs(IDType.IxnID);
            CONNID = cidFinder.getIDs(IDType.ConnID);
            UUID = cidFinder.getIDs(IDType.UUID);
        }

        if (isChecked(FindNode(orsReportSettings, DialogItem.ORS_TEVENTS, null, null)) && TableExist("ors_logbr")) {

            if (cidFinder == null
                    || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                    || !isEmpty(CONNID)
                    || !isEmpty(UUID)) {
                OrsByConnIdQuery orsByConnIdQuery = new OrsByConnIdQuery();

                if (cidFinder != null) {
                    orsByConnIdQuery.setIDFinder(cidFinder);
                }
                tellProgress("Retrieving ORS TLib messages");
                orsByConnIdQuery.setReportSettings(FindNode(orsReportSettings, DialogItem.ORS_TEVENTS, null, null));
                orsByConnIdQuery.setCommonParams(this, dlg);
                HashSet<Long> refIDs = new HashSet<>();
                ArrayList<Long> ids = new ArrayList<>();
                HashSet<Long> idDNs = new HashSet<>();
                tellProgress("Retrieve TLIb messages");
                inquirer.logger.debug("TLib report");
                orsByConnIdQuery.setRecLoadedProc(new IRecordLoadedProc() {
                    @Override
                    public void recordLoaded(ILogRecord rec) {
                        if (rec instanceof TLibEvent) {
                            addUnique(refIDs, ((TLibEvent) rec).GetReferenceId());
                            long id = rec.getID();
                            if (id > 0) {
                                ids.add(id);
                            }
                            addUnique(idDNs, ((TLibEvent) rec).getThisDNID(), ((TLibEvent) rec).getOtherDNID());
                        }
                    }

                });
                getRecords(orsByConnIdQuery);
                if (cidFinder != null && callRelatedSearch(cidFinder) && !refIDs.isEmpty()) {
                    inquirer.logger.debug("TLib requests");
                    orsByConnIdQuery = new OrsByConnIdQuery(refIDs, ids, idDNs);
                    orsByConnIdQuery.setCommonParams(this, dlg);
                    getRecords(orsByConnIdQuery);

                }
            }
        }

//<editor-fold defaultstate="collapsed" desc="ORS_IXN">
        if (isChecked(FindNode(orsReportSettings, DialogItem.ORS_IXN, null, null)) && TableExist("orsmm")) {
            if (cidFinder == null
                    || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                    || !isEmpty(IxnID)) {
                tellProgress("Retrieving ORS multimedia messages");
                TableQuery ORSMM = newORSMMTable(orsReportSettings, dlg);
                HashSet<Long> tmpIDs;
                if (IxnID != null) {
                    ORSMM.addWhere(getWhere("IXNIDID", IxnID, false), "AND");
                    tmpIDs = new HashSet<>();

                    ORSMM.setRecLoadedProc(new IQuery.IRecordLoadedProc() {
                        @Override
                        public void recordLoaded(ILogRecord rec) {
                            addUnique(tmpIDs, Util.intOrDef(rec.GetField("refid"), (Long) null));
                        }
                    });
                } else {
                    tmpIDs = null;
                }
                getRecords(ORSMM);
                if (tmpIDs != null) {
                    ORSMM = newORSMMTable(orsReportSettings, dlg);
                    ORSMM.addWhere(getWhere(ORSMM.getTabAlias() + ".refid", tmpIDs, false), "AND");
                    ORSMM.addWhere(ORSMM.getTabAlias() + ".IXNIDID < 1", "AND");
                    getRecords(ORSMM);
                }
            }
        }
//</editor-fold>
        if (isChecked(FindNode(orsReportSettings, DialogItem.ORS_STRATEGY, null, null))) {
            DynamicTreeNode<OptionNode> orsMetric = FindNode(orsReportSettings, DialogItem.ORS_STRATEGY, null, null);
            if (TableExist("orsmetr_logbr")) {
                if (orsMetric != null && ((OptionNode) orsMetric.getData()).isChecked()) {
                    if (cidFinder == null
                            || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                            || !isEmpty(SIDID)) {
                        ORSMetricByCallIdQuery orsMetricByCallIdQuery = null;
                        if (SIDID != null) {
                            orsMetricByCallIdQuery = new ORSMetricByCallIdQuery(SIDID, orsMetric);
                        } else if (cidFinder == null) {
                            orsMetricByCallIdQuery = new ORSMetricByCallIdQuery(orsMetric);
                        }
                        if (orsMetricByCallIdQuery != null) {
                            tellProgress("Retrieving ORS metrics");
                            orsMetricByCallIdQuery.setCommonParams(this, dlg);

                            getRecords(orsMetricByCallIdQuery);
                        }
                    }
                }
            }

            DynamicTreeNode<OptionNode> orsMetricExt = FindNode(orsReportSettings, DialogItem.ORS_STRATEGY, DialogItem.ORS_STRATEGY_METRICEXT, null);
            if (isChecked(orsMetricExt) && TableExist("ORSmetrExt_logbr")) {
                TableQuery orsMetricExtension = null;
                if (SIDID != null) {
                    orsMetricExtension = new TableQuery(MsgType.ORSMEXTENSION, "ORSmetrExt_logbr", "sidid", getWhere("sidid", SIDID, false));
                } else if (cidFinder == null
                        || (isEmpty(IxnID) && isEmpty(CONNID) && isEmpty(UUID))) {
                    orsMetricExtension = new TableQuery(MsgType.ORSMEXTENSION, "ORSmetrExt_logbr");
                }
                if (orsMetricExtension != null) {
                    tellProgress("Retrieving ORS metric extentions");
                    orsMetricExtension.addRef("nsid", "ns", ReferenceType.ORSNS.toString(), IQuery.FieldType.Optional);
                    orsMetricExtension.addRef("sidid", "sid", ReferenceType.ORSSID.toString(), IQuery.FieldType.Mandatory);
                    orsMetricExtension.addRef("funcid", "func", ReferenceType.METRICFUNC.toString(), IQuery.FieldType.Optional);
                    orsMetricExtension.addRef("param1id", "param1", ReferenceType.METRIC_PARAM1.toString(), IQuery.FieldType.Optional);
                    orsMetricExtension.addRef("param2id", "param2", ReferenceType.METRIC_PARAM2.toString(), IQuery.FieldType.Optional);

//                UrsStrategy.setSearchApps(searchApps);
                    orsMetricExtension.AddCheckedWhere(orsMetricExtension.getTabAlias() + ".funcid",
                            ReferenceType.METRICFUNC,
                            FindNode(orsReportSettings, DialogItem.ORS_STRATEGY, null, null),
                            "AND",
                            DialogItem.ORS_STRATEGY_METRICEXT);
                    orsMetricExtension.setCommonParams(this, dlg);

                    getRecords(orsMetricExtension);
                }
            }
        }

        if (isChecked(FindNode(orsReportSettings, DialogItem.ORS_SESSION, null, null))) {
            TableQuery OrsSess;
            if (DatabaseConnector.TableExist("orssess_logbr")) {
                if (cidFinder == null
                        || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                        || !isEmpty(SIDID)) {
                    OrsSess = new TableQuery(MsgType.ORSSESSION, "orssess_logbr");
                    if (SIDID != null) {
                        OrsSess.addWhere(getWhere("sidid", SIDID, false), "AND");
                    }

                    OrsSess.addRef("sidid", "sid", ReferenceType.ORSSID.toString(), IQuery.FieldType.Mandatory);
                    OrsSess.addRef("uuidid", "uuid", ReferenceType.UUID.toString(), IQuery.FieldType.Mandatory);
                    OrsSess.addRef("urlid", "url", ReferenceType.URSStrategyName.toString(), IQuery.FieldType.Optional);
                    OrsSess.addRef("appid", "script", ReferenceType.URSStrategyName.toString(), IQuery.FieldType.Optional);
                    OrsSess.setCommonParams(this, dlg);

                    getRecords(OrsSess);
                }
            }

            if (DatabaseConnector.TableExist("orssessixn")) {
                if (cidFinder == null
                        || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                        || !isEmpty(SIDID)) {
                    OrsSess = new TableQuery(MsgType.ORSSESSION, "orssessixn");
                    if (SIDID != null) {
                        OrsSess.addWhere(getWhere("sidid", SIDID, false), "AND");
                    }
                    tellProgress("Retrieving ORS sessions");

                    OrsSess.addRef("sidid", "sid", ReferenceType.ORSSID.toString(), IQuery.FieldType.Mandatory);
                    OrsSess.addRef("ixnid", "uuid", ReferenceType.IxnID.toString(), IQuery.FieldType.Mandatory);
                    OrsSess.addRef("urlid", "url", ReferenceType.URSStrategyName.toString(), IQuery.FieldType.Optional);
                    OrsSess.addRef("appid", "script", ReferenceType.URSStrategyName.toString(), IQuery.FieldType.Optional);
                    OrsSess.setCommonParams(this, dlg);

                    getRecords(OrsSess);
                }
            }
        }

        if (isChecked(FindNode(orsReportSettings, DialogItem.ORS_HTTP, null, null))) {
            if (DatabaseConnector.TableExist("orshttp")) {
                Integer[] GMSIDs = cidFinder.getIDs(IDType.GMSSESSION);
                if (cidFinder.getSearchType() == SelectionType.NO_SELECTION
                        || !isEmpty(SIDID)
                        || !isEmpty(GMSIDs)) {
                    TableQuery OrsHTTP = new TableQuery(MsgType.ORSHTTP, "orshttp");

                    Wheres wh = new Wheres();
                    if (SIDID != null) {
                        wh.addWhere(getWhere("sidid", SIDID, false), "OR");
                        wh.addWhere(
                                getWhere("httpresponse",
                                        MakeQueryID("httpresponse", "orshttp", getWhere("sidid", SIDID, true)),
                                        false), "OR");
                    }
                    if (GMSIDs != null) {
                        wh.addWhere(getWhere("GMSServiceid", GMSIDs, false), "OR");
                    }
                    if (!wh.isEmpty()) {
                        OrsHTTP.addWhere(wh);
                    }

                    tellProgress("Retrieving ORS http");

                    OrsHTTP.addRef("sidid", "sid", ReferenceType.ORSSID.toString(), IQuery.FieldType.Optional);
                    OrsHTTP.addRef("ipid", "ip", ReferenceType.IP.toString(), IQuery.FieldType.Optional);
                    OrsHTTP.addRef("GMSServiceid", "GMSService", ReferenceType.GMSService.toString(), IQuery.FieldType.Optional);
                    OrsHTTP.addRef("URIID", "URI", ReferenceType.HTTPRequest.toString(), IQuery.FieldType.Optional);
                    OrsHTTP.addRef("param1ID", "param1", ReferenceType.GMSMisc.toString(), IQuery.FieldType.Optional);
                    OrsHTTP.addRef("param2ID", "param2", ReferenceType.GMSMisc.toString(), IQuery.FieldType.Optional);
                    OrsHTTP.addOutField("intToHex( httpresponse) httpResponseID"); // bug in SQLite lib; does not accept full word

                    OrsHTTP.setCommonParams(this, dlg);

                    getRecords(OrsHTTP);
                }
            }
        }
        DynamicTreeNode<OptionNode> node = FindNode(orsReportSettings, DialogItem.ORS_ORSURS, null, null);
        if (isChecked(node)) {
            if (DatabaseConnector.TableExist("orsurs_logbr")) {
                if (cidFinder == null
                        || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                        || !isEmpty(SIDID)) {
                    TableQuery OrsUrs = null;
                    String checkedWhere = getCheckedWhere("methodid",
                            ReferenceType.ORSMETHOD,
                            FindNode(node, DialogItem.ORS_ORSURS_METHOD, null, null));

                    if (SIDID != null) {
                        String where = getWhere("sidid", SIDID, false);
                        if (checkedWhere != null && checkedWhere.length() > 0) {
                            where += " and " + checkedWhere;
                        }
                        OrsUrs = new TableQuery(MsgType.ORSURS, "orsurs_logbr", "sidid", "(" + where //+ " or ( refid = 0 and sidid <=0 ) "
                                + " or ("
                                + getWhere("refid", "select refid from orsurs_logbr where " + where + " and refid > 0", false)
                                + ")"
                                + "or (event=1000000102\n"
                                + " and reqid in (select reqid from orsurs_logbr \n"
                                + " where refid in (select refid from orsurs_logbr where " + where + "  and refid > 0 ))"
                                + ") "
                                + "\n)"
                        );
                    } else if (cidFinder == null) {
                        OrsUrs = new TableQuery(MsgType.ORSURS, "orsurs_logbr");
                        OrsUrs.addWhere(checkedWhere, "AND");
                    }
                    if (OrsUrs != null) {
                        tellProgress("Retrieving ORS to URS messages");
                        OrsUrs.addRef("sidid", "sid", ReferenceType.ORSSID.toString(), IQuery.FieldType.Optional);
                        OrsUrs.addRef("moduleid", "module", ReferenceType.ORSMODULE.toString(), IQuery.FieldType.Optional);
                        OrsUrs.addRef("methodid", "method", ReferenceType.ORSMETHOD.toString(), IQuery.FieldType.Optional);

//                    OrsUrs.AddCheckedWhere(OrsUrs.getTabAlias() + ".methodid",
//                            ReferenceType.ORSMETHOD,
//                            FindNode(orsReportSettings, DialogItem.ORS_STRATEGY, null, null),
//                            "AND",
//                            DialogItem.ORS_ORSURS);
//                OrsUrs.AddCheckedWhere(OrsUrs.getTabAlias() + ".methodid", ReferenceType.ORSMETHOD, orsReportComponent, "AND", DialogItem.ORS_ORSURS_METHOD);
                        OrsUrs.setCommonParams(this, dlg);
                        OrsUrs.addOutField("inbound");
                        OrsUrs.addOutField("reqid");
                        OrsUrs.addOutField("refid");
                        getRecords(OrsUrs);

                    }
                }
            }
        }

        return true;
    }

    private void getRecords(ORSMetricByCallIdQuery orsMetricByCallIdQuery, DynamicTreeNode<OptionNode> orsReportComponent) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void retrieveNoSelection(QueryDialog dlg) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<NameID> getApps() throws SQLException {
        if (appsType == null) {
            appsType = getAppsType(FileInfoType.type_ORS);
            appsType.addAll(getAppsType(FileInfoType.type_URS));
            appsType.addAll(getAppsType(FileInfoType.type_URSHTTP));

        }
        return appsType;
    }

    @Override
    protected ArrayList<IAggregateQuery> loadReportAggregates() {
        ArrayList<IAggregateQuery> ret = new ArrayList<>();
        try {
            ret.add(new AggrTimestampDelays());
            ret.add(new AggrSIPServerCallsPerSecond(false));

            return ret;
        } catch (Exception ex) {
            logger.error("fatal: ", ex);
        }
        return null;
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
    boolean callRelatedSearch(IDsFinder cidFinder) throws SQLException {
        return true;
    }

    private TableQuery newORSMMTable(DynamicTreeNode<OptionNode> orsReportSettings, QueryDialog dlg) throws SQLException {
        TableQuery ORSMM = new TableQuery(MsgType.ORSINTERACTION, "orsmm");

        tellProgress("Retrieving ORS multimedia messages");
        ORSMM.AddCheckedWhere(ORSMM.getTabAlias() + ".nameid",
                ReferenceType.TEvent,
                FindNode(orsReportSettings, DialogItem.ORS_IXN, null, null),
                "AND",
                DialogItem.ORS_IXN_NAME);

        ORSMM.addRef("IXNIDID", "ixnid", ReferenceType.IxnID.toString(), IQuery.FieldType.Optional);
        ORSMM.addRef("nameid", "name", ReferenceType.TEvent.toString(), IQuery.FieldType.Mandatory);
        ORSMM.setCommonParams(this, dlg);
        return ORSMM;
    }

    private TableQuery newURSRI(QueryDialog dlg) throws SQLException {
        TableQuery URSRI = new TableQuery(MsgType.URSRI, "ursri");
        URSRI.addRef("connidid", "connid", ReferenceType.ConnID.toString(), IQuery.FieldType.Optional);
        URSRI.addRef("funcID", "func", ReferenceType.URSMETHOD.toString(), IQuery.FieldType.Optional);
        URSRI.addRef("subfuncID", "subfunc", ReferenceType.URSMETHOD.toString(), IQuery.FieldType.Optional);
        URSRI.addRef("clientappID", "client", ReferenceType.App.toString(), IQuery.FieldType.Optional);
        URSRI.addRef("URLID", "URL", ReferenceType.HTTPRequest.toString(), IQuery.FieldType.Optional);
        URSRI.addOutFields(new String[]{"ref", "clientno"});

        for (int i = 1; i <= com.myutils.logbrowser.indexer.URSRI.MAX_WEB_PARAMS; i++) {
            String fldName = "attr" + i;
            URSRI.addRef(fldName + "id", fldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);
            String valueFldName = "value" + i;
            URSRI.addRef(valueFldName + "id", valueFldName, ReferenceType.Misc.toString(), IQuery.FieldType.Optional);
        }

        URSRI.setCommonParams(this, dlg);
        return URSRI;
    }

    private TableQuery newRLib(QueryDialog dlg, DynamicTreeNode<OptionNode> ursReportSettings) throws SQLException {
        TableQuery UrsRLib = new TableQuery(MsgType.URSRLIB, "ursrlib");
        UrsRLib.addRef("sidid", "sid", ReferenceType.ORSSID.toString(), IQuery.FieldType.Optional);
        UrsRLib.addRef("uuidid", "uuid", ReferenceType.UUID.toString(), IQuery.FieldType.Optional);
        UrsRLib.addRef("resultid", "result", ReferenceType.URSRLIBRESULT.toString(), IQuery.FieldType.Optional);
        UrsRLib.addRef("paramid", "param", ReferenceType.URSRLIBPARAM.toString(), IQuery.FieldType.Optional);
        UrsRLib.addRef("methodid", "method", ReferenceType.ORSMETHOD.toString(), IQuery.FieldType.Optional);
        UrsRLib.addRef("sourceid", "source", ReferenceType.App.toString(), IQuery.FieldType.Optional);

        UrsRLib.addOutFields(new String[]{"refid", "reqid", "inbound"});
        String methodFilter = getCheckedWhere(UrsRLib.getTabAlias() + ".methodid",
                ReferenceType.ORSMETHOD,
                FindNode(ursReportSettings, DialogItem.URS_RLIB, null, null),
                DialogItem.URS_RLIB_METHOD);
        UrsRLib.addWhere(methodFilter, "");

        UrsRLib.setCommonParams(this, dlg);
        return UrsRLib;
    }

}
