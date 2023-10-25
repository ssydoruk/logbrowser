package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.sql.SQLException;
import java.util.ArrayList;

public class ApacheWebResults extends IQueryResults {

    public static final int TLIB = 0x01;
    public static final int ISCC = 0x02;
    public static final int TC = 0x04;
    public static final int SIP = 0x08;
    public static final int PROXY = 0x10;
    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private final UTCTimeRange timeRange = null;
    ArrayList<NameID> appsType = null;
    private Object cidFinder;
    private int m_componentFilter;

    /**
     *
     */
    public ApacheWebResults(QueryDialogSettings qdSettings) {
        super(qdSettings);
        if (repComponents.isEmpty()) {
            loadStdOptions();
        }
        addSelectionType(SelectionType.NO_SELECTION);
        addSelectionType(SelectionType.GUESS_SELECTION);
        addSelectionType(SelectionType.JSESSIONID);
        addSelectionType(SelectionType.UUID);
        addSelectionType(SelectionType.IXN);
        addSelectionType(SelectionType.GWS_DEVICEID);
    }

    @Override
    public void Retrieve(QueryDialog dlg, SelectionType key, String searchID) throws SQLException {
        runSelectionQuery(dlg, key, new IDsFinder(dlg, key, searchID));
        doSort();

    }

    @Override
    FullTableColors getAll(QueryDialog qd) throws Exception {
        try {
            String tmpTable = "callFlowTmp";
            DynamicTreeNode.setNoRefNoLoad(true);

            DatabaseConnector.dropTable(tmpTable);
            inquirer.logger.info("Building temp tables");
            String tab = "sipms";

            DatabaseConnector.runQuery("create temp table " + tmpTable + " ("
                    + "callidid int"
                    + ",started timestamp"
                    + ",ended timestamp"
                    + ")\n;"
            );

//            String sWhere = StringUtils.join(new String[]{
//                IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false)),
//                IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange()),
//                getCheckedWhere("sip.nameID", ReferenceType.SIPMETHOD, node, "AND", DialogItem.TLIB_CALLS_SIP_NAME)
//            },
//                    "AND", 1, 3);
            Wheres wh = new Wheres();
            wh.addWhere(IQuery.getFileFilters(tab, "fileid", qd.getSearchApps(false)), "AND");
            wh.addWhere(IQuery.getDateTimeFilters(tab, "time", qd.getTimeRange()), "AND");
//            wh.addWhere(IQuery.getCheckedWhere("sipms.nameID", ReferenceType.SIPMETHOD,
//                    FindNode(repComponents.getRoot(), DialogItem.TLIB_CALLS, DialogItem.TLIB_CALLS_SIP, DialogItem.TLIB_CALLS_SIP_NAME)), "AND");
            wh.addWhere(getWhere("sipms.nameID", ReferenceType.SIPMETHOD, new String[]{"OPTIONS", "200 OK OPTIONS", "NOTIFY", "200 OK NOTIFY"}, false, false, true), "AND");

            DatabaseConnector.runQuery("insert into " + tmpTable + " (callidid, started, ended)"
                    + "\nselect distinct callidid, min(time), max(time) from " + tab
                    + "\n" + wh.makeWhere(true)
                    + "\ngroup by 1;");

            DatabaseConnector.runQuery("create index idx_" + tmpTable + "callidid on " + tmpTable + "(callidid);");
            DatabaseConnector.runQuery("create index idx_" + tmpTable + "started on " + tmpTable + "(started);");

            TableQuery tabReport = new TableQuery(tmpTable);
            tabReport.addOutField("UTCtoDateTime(started, \"YYYY-MM-dd HH:mm:ss.SSS\") started");
            tabReport.addOutField("UTCtoDateTime(ended, \"YYYY-MM-dd HH:mm:ss.SSS\") ended");
            tabReport.addOutField("jduration(ended-started) duration ");
            tabReport.setAddAll(false);

            tabReport.addRef("callidid", "callid", ReferenceType.SIPCALLID.toString(), FieldType.OPTIONAL);
            tabReport.setOrderBy(tabReport.getTabAlias() + ".started");
            FullTableColors currTable = tabReport.getFullTable();

            return currTable; //To change body of generated methods, choose Tools | Templates.
        } catch (SQLException ex) {
            inquirer.ExceptionHandler.handleException(this.getClass().toString(), ex);
        } finally {
            DynamicTreeNode.setNoRefNoLoad(false);

        }
        return null;
    }

    @Override
    SearchFields getSearchField() {
        SearchFields ret = new SearchFields();
        ret.addRecMap(FileInfoType.type_WWECloud, new Pair<>(SelectionType.CALLID, "callid"));
        return ret;
    }

    @Override
    public UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException {
        return DatabaseConnector.getTimeRange(new String[]{TableType.ApacheWeb.toString()}, searchApps);
    }

    @Override
    public UTCTimeRange getTimeRange() throws SQLException {
        return DatabaseConnector.getTimeRange(new String[]{TableType.ApacheWeb.toString()});
    }

    @Override
    public String getReportSummary() {
        return getName() + "\n\t" + "search: " + ((cidFinder != null) ? cidFinder.toString() : "<not specified>");
    }

    @Override
    public String getName() {
        return "Apache Web";
    }

    private void loadStdOptions() {
        DynamicTreeNode<OptionNode> rootA = new DynamicTreeNode<>(null);
        repComponents.setRoot(rootA);

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(DialogItem.APACHEMSG));
        rootA.addChild(nd);
        DynamicTreeNode<OptionNode> ndParams = new DynamicTreeNode<>(new OptionNode(DialogItem.APACHEMSG_PARAMS));
        nd.addChild(ndParams);

        ndParams.addDynamicRef(DialogItem.APACHEMSG_PARAMS_IP, ReferenceType.IP, TableType.ApacheWeb.toString(), "IPID");
        ndParams.addDynamicRef(DialogItem.APACHEMSG_PARAMS_HTTPCODE, ReferenceType.HTTPRESPONSE, TableType.ApacheWeb.toString(), "httpCode");
        ndParams.addDynamicRef(DialogItem.APACHEMSG_PARAMS_HTTPMETHOD, ReferenceType.HTTPMethod, TableType.ApacheWeb.toString(), "methodID");
        ndParams.addDynamicRef(DialogItem.APACHEMSG_PARAMS_HTTPURL, ReferenceType.HTTPURL, TableType.ApacheWeb.toString(), "urlID");

        try {
            addCustom(rootA, FileInfoType.type_ApacheWeb);
        } catch (SQLException ex) {
            logger.error("fatal: ", ex);
        }

        DoneSTDOptions();
    }

    @Override
    public ArrayList<NameID> getApps() throws Exception {
        if (appsType == null) {
            appsType = getAppsType(FileInfoType.type_ApacheWeb);
        }
        return appsType;

//        return getAppsType(FileInfoType.type_RM, new String[]{"tlib_logbr", "fileid", "sip_logbr", "fileid"});
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

        runSelectionQuery(dlg, dlg.getSelectionType(), new IDsFinder(dlg));

        doSort();
    }

    private void runSelectionQuery(QueryDialog dlg, SelectionType selectionType, IDsFinder cidFinder) throws SQLException {
        this.cidFinder = cidFinder;
        if (selectionType != SelectionType.NO_SELECTION) {
            if (!cidFinder.initApacheWeb()) {
                inquirer.logger.error("Not found selection: ["
                        + dlg.getSelection() + "] type: [" + dlg.getSelectionType() + "] isRegex: [" + dlg.isRegex()
                        + "]");
                return;
            } else {
                setObjectsFound(cidFinder.getObjectsFound());
            }
        }

//        doRetrieve(dlg, cidFinder);
        boolean runQueries = true;
        if (selectionType != SelectionType.NO_SELECTION && !cidFinder.anythingApacheWebRelated()
                && DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) {
            int dialogResult = inquirer.showConfirmDialog(dlg, "Nothing found.\n"
                    + "Do you want to show all records messages?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
            if (dialogResult == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (dialogResult == JOptionPane.NO_OPTION) {
                runQueries = false;
            }
        }

        IQuery.reSetQueryNo();

        if (runQueries) {
            retrieveApacheWeb(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.APACHEMSG, DialogItem.APACHEMSG_PARAMS, null),
                    cidFinder);

            retrieveApacheWeb(dlg,
                    FindNode(repComponents.getRoot(), DialogItem.APACHEMSG, DialogItem.APACHEMSG_PARAMS, null),
                    cidFinder);
        }

    }

    private void retrieveApacheWeb(QueryDialog dlg, DynamicTreeNode<OptionNode> eventsSettings, IDsFinder cidFinder) throws SQLException {
        if (isChecked(eventsSettings) && DatabaseConnector.TableExist(TableType.ApacheWeb.toString())) {
            tellProgress("Retrieve messages");

            TableQuery q;
            q = new TableQuery(MsgType.APACHEWEB, TableType.ApacheWeb.toString());

            Wheres wh = new Wheres();
            wh.addWhere(getWhere("JSessionIDID", cidFinder.getIDs(IDType.JSessionID)), "AND");
            q.addWhere(wh);

            q.AddCheckedWhere(q.getTabAlias() + ".IPID", ReferenceType.IP, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_IP);
            q.AddCheckedWhere(q.getTabAlias() + ".httpCode", ReferenceType.HTTPRESPONSE, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_HTTPCODE);
            q.AddCheckedWhere(q.getTabAlias() + ".methodID", ReferenceType.HTTPMethod, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_HTTPMETHOD);
            q.AddCheckedWhere(q.getTabAlias() + ".urlID", ReferenceType.HTTPURL, eventsSettings, "AND", DialogItem.APACHEMSG_PARAMS_HTTPURL);

            q.addRef("httpCode", "Code", ReferenceType.HTTPRESPONSE.toString(), FieldType.OPTIONAL);
            q.addRef("IPID", "IP", ReferenceType.IP.toString(), FieldType.OPTIONAL);
            q.addRef("JSessionIDID", "JSessionID", ReferenceType.JSessionID.toString(), FieldType.OPTIONAL);
            q.addRef("methodID", "method", ReferenceType.HTTPMethod.toString(), FieldType.OPTIONAL);
            q.addRef("urlID", "url", ReferenceType.HTTPURL.toString(), FieldType.OPTIONAL);
            q.addRef("UUIDID", "UUID", ReferenceType.UUID.toString(), FieldType.OPTIONAL);
            q.addRef("IxnIDID", "IxnID", ReferenceType.IxnID.toString(), FieldType.OPTIONAL);
            q.addRef("DeviceIDID", "DeviceID", ReferenceType.GWSDeviceID.toString(), FieldType.OPTIONAL);
            q.addRef("userNameID", "userName", ReferenceType.Agent.toString(), FieldType.OPTIONAL);

            q.setCommonParams(this, dlg);
            getRecords(q);

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
        if (cidFinder != null) {
            try {
                return cidFinder.AnythingTLibRelated();
            } catch (SQLException ex) {
                logger.error("fatal: ", ex);
            }
        }
        return true;
    }

}
