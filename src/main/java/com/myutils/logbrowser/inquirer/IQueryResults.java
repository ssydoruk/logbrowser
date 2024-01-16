package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.Parser;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.DynamicTreeNode.ILoadChildrenProc;
import com.myutils.logbrowser.inquirer.IQuery.FieldType;
import com.myutils.logbrowser.inquirer.OutputSpecFormatter.Parameter;
import com.myutils.mygenerictree.GenericTree;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.myutils.logbrowser.indexer.Parser.MAX_CUSTOM_FIELDS;
import static org.sqlite.SQLiteErrorCode.SQLITE_INTERRUPT;


public abstract class IQueryResults extends QueryTools
        implements ActionListener,
        PropertyChangeListener {

    public final static String KEY_ID_FIELD = "keyid";
    private static final org.apache.logging.log4j.Logger logger = inquirer.logger;
    final private static Matcher regFileName = Pattern.compile("([^\\\\/]+)$").matcher("");
    private static final String FILENO = "fileno";
    private static final String APPSTARTTIME = "app_starttime";
    protected String m_startTime;
    protected String m_endTime;
    protected ArrayList m_results = new ArrayList();
    protected ArrayList<ILogRecordFormatter> m_formatters = new ArrayList();
    protected DynamicTree<OptionNode> repComponents;
    protected ArrayList<SelectionType> selectionTypes = new ArrayList<>();
    protected ArrayList<Pair<String, Integer>> getAllResults = new ArrayList<>();
    PrintStream os = null;
    AggregatesPanel aggregateParams = null;
    //</editor-fold>
    CustomStorage cs;
    HashMap<MsgType, ArrayList<OutputSpecFormatter.Parameter>> addOutParams = new HashMap<>();
    private int objectsFound;
    private ArrayList<IAggregateQuery> reportAggregates = null;
    private DynamicTreeNode<OptionNode> savedRoot;
    private ProgressMonitor progressMonitor;
    private JTextArea taskOutput;
    private Task task;
    private boolean suppressConfirms = true;

    //    public void getGenesysMessages(TableType t, UTCTimeRange timeRange) throws SQLException {
//        getGenesysMessages(t, null, timeRange, null);
//    }
//    public void getGenesysMessages(TableType t) throws SQLException {
//        getGenesysMessages(t, null, null, null);
//    }
    public IQueryResults(QueryDialogSettings qdSettings) {
        this();
        if (qdSettings != null) {
            DynamicTree tree = qdSettings.getQParams(this.getClass().getName());
            if (tree != null) {
                DynamicTreeNode<OptionNode> rootA = tree.getRoot();
                if (rootA != null) {
                    repComponents.setRoot(rootA);
                }
            }
        }
    }

    public IQueryResults() {
        repComponents = new DynamicTree<>();
    }

    public static ArrayList<NameID> getAllApps(Object owner, FileInfoType fileInfoType) throws SQLException {
        if (fileInfoType == null) {
            return getRefNameIDs(owner, ReferenceType.App, "id in (select distinct appnameid from file_logbr)", true);
        } else {
            return getRefNameIDs(owner, ReferenceType.App, "id in (select distinct appnameid from file_logbr"
                    + " where apptypeid = (select id from "
                    + ReferenceType.AppType + " where name =\""
                    + FileInfoType.getFileType(fileInfoType) + "\"))", true);
        }
    }

    static public ArrayList<NameID> getAllApps(Object owner) throws Exception {
        return getAllApps(owner, null);
    }

    static public void addUnique(HashSet<Long> idDNs, long thisDNID, long otherDNID) {
        addUnique(idDNs, thisDNID);
        addUnique(idDNs, otherDNID);

    }

    static public void addUnique(HashSet<Long> ids, Long id) {
        if (id != null && id > 0) {
            ids.add(id);

        }
    }

    static public void addUnique(HashSet<Integer> ids, int id1, int id2) {
        addUnique(ids, id1);
        addUnique(ids, id2);

    }

    static public void addUnique(HashSet<Integer> ids, Integer id) {
        if (id != null && id > 0) {
            ids.add(id);

        }
    }

    public int getObjectsFound() {
        return objectsFound;
    }

    public void setObjectsFound(int objectsFound) {
        this.objectsFound = objectsFound;
    }

    protected DynamicTreeNode<Object> getNode(DialogItem dialogItem, String tabName) throws SQLException {
        if (DatabaseConnector.TableExist(tabName)) {
            return new DynamicTreeNode<>(new OptionNode(false, dialogItem));

        }
        return null;
    }

    abstract public String getReportSummary();

    protected abstract ArrayList<IAggregateQuery> loadReportAggregates() throws SQLException;

    public AggregatesPanel getAggregatesPanel() throws SQLException {
        if (aggregateParams == null) {
            if (reportAggregates == null) {
                reportAggregates = loadReportAggregates();
            }
            aggregateParams = new AggregatesPanel(reportAggregates);
        }
        return aggregateParams;
    }

    public String getAllCallsTitle(QueryDialog qd) {
        return getName() + " " + qd.getSettingSummary();
    }

    public boolean callRelated(QueryDialog dlg) {
        return dlg.getSelectionType() == SelectionType.CONNID
                || dlg.getSelectionType() == SelectionType.IXN
                || dlg.getSelectionType() == SelectionType.UUID
                || dlg.getSelectionType() == SelectionType.CALLID
                || dlg.getSelectionType() == SelectionType.ChainID
                || dlg.getSelectionType() == SelectionType.IXN
                || dlg.getSelectionType() == SelectionType.OCSSID
                || dlg.getSelectionType() == SelectionType.SESSION;
    }

    public ArrayList<NameID> getAppsType(FileInfoType fileInfoType, String[] tabAndField) throws Exception {
        if (DatabaseConnector.onlyOneApp()) {
            return getAppsType(fileInfoType);
        } else {
            int i = 0;
            StringBuilder ret = new StringBuilder(tabAndField.length * 50);

//            ret.append("id in (select distinct appnameid from file_logbr\nwhere id in (");
            while (i < tabAndField.length) {
                if (DatabaseConnector.TableExist(tabAndField[i])) {
                    if (ret.length() > 0) {
                        ret.append("\nUNION");
                    }
                    ret.append("\nselect distinct ").append(tabAndField[i + 1]).append(" from ").append(tabAndField[i]);
                }
                i += 2;
            }
            if (ret.length() > 0) {
//                ret.append("))");
                return getRefNameIDs(this, ReferenceType.App,
                        "id in (select distinct appnameid from file_logbr\nwhere id in ("
                                + ret
                                + "))"
                );
            }
        }
        return null;
    }

    public abstract UTCTimeRange refreshTimeRange(ArrayList<Integer> searchApps) throws SQLException;

    public ArrayList<NameID> getAppsType(FileInfoType fileInfoType) throws SQLException {
        return getRefNameIDs(this, ReferenceType.App, "id in (select distinct appnameid from file_logbr"
                + " where apptypeid = (select id from "
                + ReferenceType.AppType + " where name =\""
                + FileInfoType.getFileType(fileInfoType) + "\"))", true);
    }

    public void printDBRecords(PrintStreams ps) throws Exception {
        this.Print(ps);
    }

    void Print(PrintStreams ps) throws Exception {
        /*
        boolean isLayoutRequired = false;
        for (int j = 0; j < m_formatters.size(); j++) {
            ILogRecordFormatter formatter
                    = (ILogRecordFormatter) m_formatters.get(j);

            if (formatter.IsLayoutRequired()) {
                isLayoutRequired = true;
                break;
            }
        }

        if (isLayoutRequired) {
            for (int i = 0; i < m_results.size(); i++) {
                ILogRecord record = (ILogRecord) m_results.get(i);
                for (int j = 0; j < m_formatters.size(); j++) {
                    ILogRecordFormatter formatter
                            = (ILogRecordFormatter) m_formatters.get(j);

                    if (formatter.IsLayoutRequired()) {
                        formatter.Layout(record);
                    }
                }
            }

            for (int j = 0; j < m_formatters.size(); j++) {
                ILogRecordFormatter formatter
                        = (ILogRecordFormatter) m_formatters.get(j);

                if (formatter.IsLayoutRequired()) {
                    formatter.ProcessLayout();
                }
            }
        }
         */
        doPrinting(ps);
        if (os != null) {
            os.close();
        }
    }

    private void doPrinting(PrintStreams ps) throws Exception {
        int i = 0;
//        try {
//            ps.printSummary(getReportSummary());
//        } catch (Exception e) {
//            inquirer.ExceptionHandler.handleException("Header not printed", e);
//        }
        for (; i < m_results.size(); i++) {
            if (Thread.currentThread().isInterrupted()) {
//                inquirer.ExceptionHandler.handleException("interrupted while printing", new InterruptedException());
                throw new RuntimeInterruptException();
            }
            ILogRecord record = (ILogRecord) m_results.get(i);
            ps.newRow(record);
            record.debug();
            for (int j = 0; j < m_formatters.size(); j++) {
                ILogRecordFormatter formatter
                        = m_formatters.get(j);

                formatter.Print(record, ps, this);
//                Thread.sleep(200);
            }
        }
        inquirer.logger.info("Processed all " + m_results.size() + " records");
    }

    public ArrayList<Parameter> getAddOutParams(MsgType t) {
        return addOutParams.get(t);
    }

    public void AddFormatter(ILogRecordFormatter formatter) {
        inquirer.logger.debug("AddFormatter " + formatter.toString());
        m_formatters.add(formatter);
    }

    public void ApplyMark(String mark) {
    }

    public void SetTimeInterval(String start, String end) {
        m_startTime = start;
        m_endTime = end;
    }

    public void getRecords(IQuery query) throws SQLException {

        if (!query.refTablesExist()) //do not send query if reference does not exist
        {
            throw new SQLException("Referenced table does not exist! Cannot execute the query");
        }
        query.Execute();
        int cnt = 0;
        try {
            for (ILogRecord record = query.GetNext();
                 record != null;
                 record = query.GetNext()) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeInterruptException();
                }
                if (printAlone) {
                    for (ILogRecordFormatter m_formatter : m_formatters) {
                        m_formatter.Print(record, ps, this);
                    }

                } else {
                    m_results.add(record);
                }
                cnt++;
            }

        } catch (SQLException sQLException) {
            if (sQLException.getErrorCode() == SQLITE_INTERRUPT.code) {
                inquirer.logger.debug("Statement cancelled");
                query.Reset();
                DatabaseConnector.cancelCurrent();
                Thread.currentThread().interrupt();
                throw new RuntimeInterruptException();
            } else {
                inquirer.ExceptionHandler.handleException("RunQuery failed: " + sQLException + " query " + query, sQLException);
                throw sQLException;
            }
        }

        query.Reset();
        inquirer.logger.debug("Retrieved " + cnt + " records");
        tellProgress("Retrieved " + cnt + " records");
    }

    public void getRecords(IQuery query, HashMap ids) throws Exception {
        query.Execute();

        for (ILogRecord record = query.GetNext();
             record != null;
             record = query.GetNext()) {
            long id = record.getID();
            if (ids.containsKey(id)) {
                continue;
            }
            ids.put(id, record);
            m_results.add(record);
        }
        query.Reset();
    }

    protected void DoneSTDOptions() {
        DynamicTreeNode.setBlockLoad(true);
        savedRoot = new DynamicTreeNode<>(repComponents.getRoot());
        DynamicTreeNode.setBlockLoad(false);
    }

    public void resetOptions() {
        repComponents.setRoot(new DynamicTreeNode<>(savedRoot));
    }

    public abstract String getName();

    public abstract UTCTimeRange getTimeRange() throws SQLException;

    public DynamicTree<OptionNode> getReportItems() {
        return repComponents;
    }

    public abstract ArrayList<NameID> getApps() throws Exception;

    public ArrayList<SelectionType> getSelectionTypes() {
        return selectionTypes;
    }

    void setQParams(GenericTree qParams) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    public final void addSelectionType(SelectionType id) {
        selectionTypes.add(id);
    }

    //    public abstract
    private void ShowProgressDlg() {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }

    public abstract void Retrieve(QueryDialog dlg) throws SQLException;

    public abstract void Retrieve(QueryDialog qd, SelectionType key, String searchID) throws SQLException;

    public void setFormatters(List<ILogRecordFormatter> formatters) {
        for (int i = 0; i < formatters.size(); i++) {
            AddFormatter(formatters.get(i));
        }
    }

    public void doRetrieve(QueryDialog dlg) throws Exception {
        long time3 = new Date().getTime();
        DynamicTreeNode.setNoRefNoLoad(true);
        Retrieve(dlg);
        DynamicTreeNode.setNoRefNoLoad(false);
        long time4 = new Date().getTime();
        inquirer.logger.info("Retrieved " + m_results.size() + " records for query type " + dlg.getName() + ". Took " + Utils.Util.pDuration(time4 - time3));
    }

    public void reset() {
        m_formatters.clear();
        resetOutput();
    }

    public void resetOutput() {
        m_results.clear();
        setSuppressConfirms(false);
    }

    FullTableColors getFileTable(String query) throws Exception {

        FullTableColors ret;
        int fileNoIdx = 0;
        int fileStartTimeIdx = 0;

        try (ResultSet rs = DatabaseConnector.executeQuery(query)) {
            ResultSetMetaData rsmd = rs.getMetaData();

// The column count starts from 1
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                if (FILENO.equals(rsmd.getColumnName(i))) {
                    fileNoIdx = i;
                } else if (APPSTARTTIME.equals(rsmd.getColumnName(i))) {
                    fileStartTimeIdx = i;
                }
                // Do stuff with name
            }

            ret = new FullTableColors();
            ret.setMetaData(rsmd);
            ret.setFieldColorChange(APPSTARTTIME);

            int columnCount = rsmd.getColumnCount();

            int cnt = 0;
            int curFileNo = 0;
            String curFileStart = null;
            while (rs.next()) {
                if (fileNoIdx > 0 && fileStartTimeIdx > 0) {
                    String theFileStart = rs.getString(fileStartTimeIdx);
                    int curNo = rs.getInt(fileNoIdx);
                    if (curFileStart == null || !curFileStart.equals(theFileStart)) {
                        curFileNo = curNo;
                        curFileStart = theFileStart;
                    } else if (curNo == curFileNo + 1) {
                        curFileNo++;
                    } else if (curNo - curFileNo > 0) {
                        ArrayList<Object> row = new ArrayList<>(columnCount);
                        for (int i = 1; i <= columnCount; i++) {
                            row.add("");
                        }
                        row.set(columnCount - 2, "Missing: ");
                        row.set(columnCount - 1, curNo - curFileNo);
                        ret.addRow(row);
                        curFileNo = curNo;
                    }
                }
                ArrayList<Object> row = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getObject(i));
                }
                QueryTools.DebugRec(rs);
                ret.addRow(row);
                cnt++;
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
            rs.getStatement().close();
        }

        return ret;

    }

    public FullTableColors getFileFields(ArrayList<Integer> searchApps) throws Exception {
        FullTableColors currTable;
        FullTableColors retTable = null;
        for (int i = 0; i < searchApps.size(); i++) {
            currTable = getFileTable("select "
                    + "REGEXP_group('([^\\\\/]+)$', intfilename, 1) infilename, "
                    + "hostname, "
                    + "file_logbr.name filename, file_logbr.arcname as arcname, "
                    + "app.name as name, "
                    + "UTCtoDateTime(starttime, \"YYYY-MM-dd HH:mm:ss.SSS\") " + APPSTARTTIME + ","
                    + "UTCtoDateTime(filestarttime, \"YYYY-MM-dd HH:mm:ss.SSS\") file_starttime,"
                    + "UTCtoDateTime(endtime, \"YYYY-MM-dd HH:mm:ss.SSS\") endtime, "
                    + "jduration(endtime-filestarttime) duration, "
                    + "fileno " + FILENO + ","
                    + "filesize(size) size"
                    + "\nfrom file_logbr "
                    + "\ninner join app on app.id=file_logbr.appnameid"
                    + IQuery.getWhere("appnameid", new Integer[]{searchApps.get(i)}, true)
                    + "\norder by name, starttime, filestarttime");
            if (retTable == null) {
                retTable = currTable;
            } else {
                retTable.addAll(currTable);
            }
        }
        return retTable;
    }

    public String[] getAllFiles(ArrayList<Integer> searchApps) throws Exception {
        ArrayList<String> ret = new ArrayList<>();
        for (Integer searchApp : searchApps) {
            String[] names = DatabaseConnector.getNames(this, "file_logbr", "intfilename", IQuery.getWhere("appnameid", new Integer[]{searchApp}, true));
            ArrayList<String> trimNames = new ArrayList<>(names.length);
            for (String name : names) {
                Matcher m;
                if ((m = regFileName.reset(name)).find()) {
                    trimNames.add(m.group(1));
                } else {
                    trimNames.add(name);
                }
            }
            Collections.sort(trimNames);
            ret.addAll(trimNames);
        }
        return ret.toArray(new String[ret.size()]);
    }

    abstract void showAllResults();

    public boolean isPrintAlone() {
        return printAlone;
    }

    IDsFinder.RequestLevel getDefaultQueryLevel() {
        return IDsFinder.RequestLevel.Level3;
    }

    abstract SearchFields getSearchField();

    public boolean isSuppressConfirms() {
        return suppressConfirms;
    }

    public void setSuppressConfirms(boolean suppressConfirms) {
        this.suppressConfirms = suppressConfirms;
    }

    public void doRetrieve(QueryDialog dlg, SelectionType key, String searchID) throws Exception {
        long time3 = new Date().getTime();
        Retrieve(dlg, key, searchID);
        long time4 = new Date().getTime();
        inquirer.logger.info("Retrieved " + m_results.size() + " records. Took " + Utils.Util.pDuration(time4 - time3));
    }

    private void createAndShowGUI() {
        progressMonitor = new ProgressMonitor(null,
                "Running a Long Task",
                "", 0, 100);
        progressMonitor.setProgress(0);
        task = new Task();
        task.addPropertyChangeListener(this);
        task.execute();

    }

    //    public void getConfigMessages(ArrayList<Integer> searchApps, DynamicTreeNode<OptionNode> root) throws Exception {
//        getConfigMessages(searchApps, root, null);
//
//    }
    protected void addConfigUpdates(DynamicTreeNode<OptionNode> rootA) {
        DynamicTreeNode<OptionNode> topConfig = new DynamicTreeNode<>(new OptionNode(false, DialogItem.CONFIGUPDATES));
        rootA.addChild(topConfig);

        DynamicTreeNode<OptionNode> configConfig = new DynamicTreeNode<>(new OptionNode(true, DialogItem.CONFIGUPDATES_CONFIGUPDATES));
        topConfig.addChild(configConfig);
        configConfig.addDynamicRef(DialogItem.CONFIGUPDATES_CONFIGUPDATES_OBJECTTYPE, ReferenceType.CfgObjectType);
    }

    protected void getConfigMessages(DynamicTreeNode<OptionNode> root, QueryDialog dlg, IQueryResults qry) throws SQLException {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//    public void getConfigMessages(ArrayList<Integer> searchApps, DynamicTreeNode<OptionNode> root, UTCTimeRange timeRange) throws SQLException {
        String tabName = "cfg";
        DynamicTreeNode<OptionNode> cfgUpdateNode = FindNode(root, DialogItem.CONFIGUPDATES, null, null);
        if (cfgUpdateNode != null && ((OptionNode) cfgUpdateNode.getData()).isChecked() && DatabaseConnector.TableExist(tabName)) {
            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeInterruptException();
            }
            tellProgress("Extracting Config messages");

            TableQuery genCfg = new TableQuery(MsgType.GENESYS_CFG, "cfg");
            genCfg.addRef("opid", "operation", ReferenceType.CfgOp.toString(), FieldType.OPTIONAL);
            genCfg.addRef("objtypeID", "objtype", ReferenceType.CfgObjectType.toString(), FieldType.OPTIONAL);
            genCfg.addRef("objNameID", "objName", ReferenceType.CfgObjName.toString(), FieldType.OPTIONAL);
            genCfg.addRef("msgID", "msg", ReferenceType.CfgMsg.toString(), FieldType.OPTIONAL);
            genCfg.AddCheckedWhere(genCfg.getTabAlias() + ".objtypeID",
                    ReferenceType.CfgObjectType,
                    FindNode(cfgUpdateNode, DialogItem.CONFIGUPDATES_CONFIGUPDATES, null, null),
                    "AND",
                    DialogItem.CONFIGUPDATES_CONFIGUPDATES_OBJECTTYPE);
//            genCfg.AddCheckedWhere(genCfg.getTabAlias() + ".MSGID", ReferenceType.LOGMESSAGE, logMsgType, "AND", DialogItem.APP_LOG_MESSAGES_TYPE_MESSAGE_ID);
            genCfg.setCommonParams(qry, dlg);
            getRecords(genCfg);

        }
    }

    //    public void getGenesysMessages(TableType tableType, DynamicTreeNode<OptionNode> root) throws Exception {
//        getGenesysMessages(tableType, root, null, null);
//    }
    public void getGenesysMessagesApp(TableType tableType,
                                      DynamicTreeNode<OptionNode> root,
                                      QueryDialog dlg, IQueryResults qry) throws SQLException {
        boolean shouldRunQuery;
        DynamicTreeNode<OptionNode> logMsgType;
        if (root != null) {
            if (((OptionNode) root.getData()).isChecked()) {
                logMsgType = getChildByName(root, DialogItem.APP_LOG_MESSAGES_TYPE);
                if (logMsgType != null) {
                    shouldRunQuery = ((OptionNode) logMsgType.getData()).isChecked();
                    if (Thread.currentThread().isInterrupted()) {
                        throw new RuntimeInterruptException();
                    }
                    if (shouldRunQuery && DatabaseConnector.TableExist(tableType.toString())) {
                        tellProgress("Getting log messages");

                        TableQuery genMsg = new TableQuery(MsgType.GENESYS_MSG, tableType.toString());
//        genMsg.setIdsSubQuery("fileid", "select id from file_logbr "+ getWhere("component", new Integer[] {ft.getValue()}, true));        
                        genMsg.addRef("levelID", "level", ReferenceType.MSGLEVEL.toString(), FieldType.MANDATORY);
                        genMsg.addRef("msgID", "msg", ReferenceType.LOGMESSAGE.toString(), FieldType.OPTIONAL);
                        genMsg.AddCheckedWhere(genMsg.getTabAlias() + ".levelID", ReferenceType.MSGLEVEL, logMsgType, "AND", DialogItem.APP_LOG_MESSAGES_TYPE_LEVEL);
                        genMsg.AddCheckedWhere(genMsg.getTabAlias() + ".MSGID", ReferenceType.LOGMESSAGE, logMsgType, "AND", DialogItem.APP_LOG_MESSAGES_TYPE_MESSAGE_ID);
                        genMsg.setCommonParams(qry, dlg);
                        getRecords(genMsg);
                    }
                }
            }
        }
    }

    //    private void getGenesysMessages(TableType tableType, DynamicTreeNode<OptionNode> root, QueryDialog dlg, IQueryResults qry) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
    public void getGenesysMessages(TableType tableType, DynamicTreeNode<OptionNode> root,
                                   QueryDialog dlg, IQueryResults qry) throws SQLException {
        if (root != null) {
            getGenesysMessagesApp(tableType, getChildByName(root, DialogItem.APP_LOG_MESSAGES), dlg, qry);
        }
    }

    public void doPrint(InquirerCfg cfg) throws Exception {
        PrintStreams ps = new PrintStreams(cfg);
        doPrint(ps);
        ps.closeStreams();
    }

    public String getSearchString() {
        return null;
    }

    public void doPrint(PrintStreams ps) throws Exception {
        tellProgress("printing");
        ps.setQueryName(getName());
        ps.setSearchString(getSearchString());
        long timePrintStart = new Date().getTime();

        Print(ps);

        for (int i = 0; i < m_formatters.size(); i++) {
            m_formatters.get(i).Close();
        }
        long timePrintEnd = new Date().getTime();
        inquirer.logger.info("Printing took " + Utils.Util.pDuration(timePrintEnd - timePrintStart));
    }

    protected void showAllResults(ArrayList<Pair<String, Integer>> allResults) {
//        DefaultTableModel infoTableModel = new DefaultTableModel();
//        infoTableModel.addColumn("Report");
//        infoTableModel.addColumn("count");
//        int grandTotal = 0;
//        for (Pair<String, Integer> entry : allResults) {
//            infoTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
//        }
//        infoTableModel.setRowCount(allResults.size());
////        for (Map.Entry<MsgType, Integer> entry : typeStat.entrySet()) {
////            infoTableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
////            grandTotal += entry.getValue();
////        }
////        infoTableModel.addRow(new Object[]{"TOTAL", grandTotal});
//
//        JTable tab = new JTable(infoTableModel);
//        tab.getTableHeader().setVisible(true);
//        tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//        Dimension preferredSize = tab.getPreferredSize();
//
//        JPanel jScrollPane = new JPanel(new BorderLayout());
//        jScrollPane.add(tab);
//        jScrollPane.setPreferredSize(preferredSize);
//        inquirer.showInfoPanel(null, "Title", jScrollPane, true);

    }

    protected void addCustom(DynamicTreeNode<OptionNode> node, FileInfoType fileInfoType) throws SQLException {
        if (!DatabaseConnector.TableExist(Parser.getCustomTab(fileInfoType))) {
            return;
        }

        DynamicTreeNode<OptionNode> nd = new DynamicTreeNode<>(new OptionNode(true, DialogItem.CUSTOM));
        node.addChild(nd);
        ILoadChildrenProc iLoadChildrenProc = new ILoadChildrenProc() {
            @Override
            public ArrayList<NameID> LoadChidlren(DynamicTreeNode node) {
                return null;
            }

            @Override
            void treeLoad(DynamicTreeNode parent) {
                fillCustom(fileInfoType);

                for (Map.Entry<Integer, CustomStorage.CustomComponent> item : cs.getItems().entrySet()) {
                    CustomStorage.CustomComponent customItem = item.getValue();
                    DynamicTreeNode<OptionNode> key = new DynamicTreeNode<>(
                            new CustomOptionNode(true, customItem, customItem.getName())
                            //                            new CustomOptionNode(true, customItem.getFieldName(), customItem.getId(), item.getKey())
                    );
                    nd.addChild(key);
                    ILoadChildrenProc keyLoadProc = new ILoadChildrenProc() {
                        @Override
                        ArrayList<NameID> LoadChidlren(DynamicTreeNode node) {
                            return null;
                        }

                        @Override
                        void treeLoad(DynamicTreeNode parent) {
                            CustomStorage.CustomComponent customComponent = (CustomStorage.CustomComponent) ((CustomOptionNode) parent.getData()).getData();
//                            System.out.println(customComponent.getName());
                            for (Map.Entry<String, CustomStorage.CustomComponent.KeyValues> entry : customComponent.getKeyValues().entrySet()) {
                                String fld = entry.getKey();
                                CustomStorage.CustomComponent.KeyValues value = entry.getValue();
                                for (Map.Entry<Integer, HashSet<Integer>> entry1 : value.entrySet()) {
                                    Integer keyID = entry1.getKey();
                                    HashSet<Integer> value1 = entry1.getValue();
                                    try {
                                        String key = DatabaseConnector.getDatabaseConnector(null).persistentRef(
                                                ReferenceType.CUSTKKEY,
                                                keyID);

                                        DynamicTreeNode<OptionNode> attrName = new DynamicTreeNode<>(
                                                new CustomOptionNode(
                                                        true,
                                                        fld,
                                                        keyID,
                                                        key));
                                        parent.addChild(attrName);
                                        for (Integer valueID : value1) {
                                            String val = DatabaseConnector.getDatabaseConnector(null).persistentRef(
                                                    ReferenceType.CUSTVALUE,
                                                    valueID);
                                            DynamicTreeNode<OptionNode> attrValue = new DynamicTreeNode<>(
                                                    new CustomOptionNode(
                                                            true,
                                                            null,
                                                            valueID,
                                                            val));
                                            attrName.addChild(attrValue);

                                        }
                                    } catch (SQLException exception) {
                                        inquirer.logger.error(exception);
                                    }
                                }
//                                
//                                java.lang.Object key1 = entry.getKey();
//                                Map.Entry<MsgType, ArrayList<Parameter>> value = entry.getValue();
//                                
                            }
//                            for (CustomStorage.CustomComponent.KeyValues v : customComponent.getKeyValues().g) {
//                                DynamicTreeNode<OptionNode> attrName = new DynamicTreeNode<>(new CustomOptionNode(true, v.field, null, v.key));
//                                parent.addChild(attrName);
//                                for (CustomStorage.CustomComponent value : v.values) {
//                                    DynamicTreeNode<OptionNode> vNode = new DynamicTreeNode<>(new CustomOptionNode(true, value.fieldName, value.getId(), value.getValue()));
//                                    attrName.addChild(vNode);
//                                }
//                            }
                        }
                    };
                    keyLoadProc.setTreeLoad(true);

                    key.setLoadChildrenProc(keyLoadProc);
                }
            }
        };
        iLoadChildrenProc.setTreeLoad(true);
        nd.setLoadChildrenProc(iLoadChildrenProc);

        //node.addDynamicRef(DialogItem.CUSTOM, ReferenceType.CUSTCOMP, FileInfoType.getFileType(FileInfoType.type_CallManager) + "_custom", "keyid");
//        nd.addDynamicRef(DialogItem.TLIB_CALLS_SIP_ENDPOINT, ReferenceType.DN);
    }

    private boolean isCustomField(String col) {
        for (int i = 1; i <= Parser.MAX_CUSTOM_FIELDS; i++) {
            if (col.equalsIgnoreCase(Parser.fldName(i))) {
                return true;
            }

        }
        return false;
    }

    private boolean isCustomVal(String col) {
        for (int i = 1; i <= Parser.MAX_CUSTOM_FIELDS; i++) {
            if (col.equalsIgnoreCase(Parser.valName(i))) {
                return true;
            }

        }
        return false;
    }

    protected void getCustom(FileInfoType ft, QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder) throws SQLException {
        getCustom(ft, dlg, reportSettings, cidFinder, null);
    }

    protected void getCustom(FileInfoType ft, QueryDialog dlg, DynamicTreeNode<OptionNode> reportSettings, IDsFinder cidFinder,
                             HashSet handlerIDs) throws SQLException {

        if (!DatabaseConnector.TableExist(Parser.getCustomTab(ft))) {
            return;
        }
        DynamicTreeNode<OptionNode> node = FindNode(reportSettings, DialogItem.CUSTOM, null, null);
        boolean handlersFound = (handlerIDs != null && !handlerIDs.isEmpty());
        if (isChecked(node) && ((cidFinder == null || cidFinder.getSearchType() == SelectionType.NO_SELECTION
                || handlersFound))) {
//<editor-fold defaultstate="collapsed" desc="Custom searches">
            tellProgress("Custom searches");
            TableQuery customQuery = new TableQuery(MsgType.CUSTOM, Parser.getCustomTab(ft));

            if (handlersFound) {
                Wheres wh = new Wheres();
                wh.addWhere(getWhere("handler", (Integer[]) handlerIDs.toArray(new Integer[handlerIDs.size()]), "", false));
                wh.addWhere("handler is null", "or");
                customQuery.addWhere(wh, "AND");
            }

            customQuery.AddCheckedCustomWhere(customQuery.getTabAlias() + ".keyid", node, "AND");

            customQuery.addRef("keyid", "keycomponent", ReferenceType.CUSTCOMP.toString(), FieldType.OPTIONAL);
            for (int i = 1; i <= Parser.MAX_CUSTOM_FIELDS; i++) {
                customQuery.addRef(Parser.fldName(i), Parser.fldNameFull(i), ReferenceType.CUSTKKEY.toString(), FieldType.OPTIONAL);
                customQuery.addRef(Parser.valName(i), Parser.valNameFull(i), ReferenceType.CUSTVALUE.toString(), FieldType.OPTIONAL);

            }

            customQuery.setCommonParams(this, dlg);
            getRecords(customQuery);
        }
//</editor-fold>      
    }

    private void fillCustom(FileInfoType fileType) {
        try {
            ResultSet rs = DatabaseConnector.executeQuery("PRAGMA table_info ('" + Parser.getCustomTab(fileType) + "')");

            ResultSetMetaData rsmd = rs.getMetaData();
            int idx = getColumnIdx(rsmd, "name");
            if (idx < 0) {
                return;
            }
            int cnt = 0;
            StringBuilder q = new StringBuilder(rsmd.getColumnCount() * 50);
            q.append("select distinct keyid");
            while (rs.next()) {
                String col = rs.getString(idx);
                if (isCustomField(col) || isCustomVal(col)) {
                    q.append(",").append(col);
                }
                QueryTools.DebugRec(rs);
                cnt++;
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
            rs.close();
            q.append(" from ").append(Parser.getCustomTab(fileType));

            FullTable fullTable = DatabaseConnector.getFullTable(q.toString());
            cs = new CustomStorage(fullTable);

        } catch (Exception ex) {
            logger.error("fatal: ", ex);
        }
    }

    private int getColumnIdx(ResultSetMetaData rsmd, String name) throws SQLException {
        for (int i = 1; i <= rsmd.getColumnCount(); i++) {
            if (name.equalsIgnoreCase(rsmd.getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }

    protected void Sort() throws SQLException {
        if (inquirer.getCr().isSorting()) {
            tellProgress("Sorting");
            try {
                DbRecordComparator comparator = new DbRecordComparator();
                Collections.sort(m_results, comparator);
//            comparator.BlockSort(m_results);
                comparator.Close();
            } catch (IllegalArgumentException illegalArgumentException) {
                inquirer.ExceptionHandler.handleException("Sorting", illegalArgumentException);
            }
        }
    }

    protected void addParameter(MsgType msgType, OutputSpecFormatter.Parameter prm) {
        ArrayList<Parameter> get = addOutParams.get(msgType);
        if (get == null) {
            get = new ArrayList<>();
            addOutParams.put(msgType, get);
        }
        get.add(prm);
    }

    abstract boolean callRelatedSearch(IDsFinder cidFinder) throws SQLException;

    public abstract IGetAllProc getAllProc(Component c, int x, int y);

    public static abstract class ProgressNotifications {

        abstract void sayProgress(String s);
    }

    public static class SearchFields {

        private final HashMap<com.myutils.logbrowser.indexer.FileInfoType, Pair<SelectionType, String>> recMap;
        private String typeField = null;

        public SearchFields() {
            recMap = new HashMap<>();
        }

        public String getTypeField() {
            return typeField;
        }

        public void setTypeField(String typeField) {
            this.typeField = typeField;
        }

        public Pair<SelectionType, String> getSearchFieldName(com.myutils.logbrowser.indexer.FileInfoType type) {
            return recMap.get(type);
        }

        public Pair<SelectionType, String> getSearchFieldName() {
            if (!recMap.isEmpty()) {
                return recMap.values().iterator().next();
            }
            return null;
        }

        public void addRecMap(com.myutils.logbrowser.indexer.FileInfoType type, Pair<SelectionType, String> t) {
            recMap.put(type, t);
        }
    }

    public static class CustomOptionNode extends OptionNode {

        private final String fld;
        private final Integer id;

        private CustomOptionNode(boolean b, CustomStorage.CustomComponent customItem, String key) {
            this(b, customItem.getFieldName(), customItem.getId(), key);
            setData(customItem);
        }

        CustomOptionNode(boolean isChecked, String fld, Integer id, String val) {
            super(isChecked, val);
            this.fld = fld;
            this.id = id;
        }

        public String getFld() {
            return fld;
        }

        public Integer getId() {
            return id;
        }

        @Override
        public String toString() {
            return super.toString();
        }

        String getIdsClause() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    class Task extends SwingWorker<Void, Void> {

        @Override
        public Void doInBackground() {
            try {
                doPrinting(null);
            } catch (InterruptedException ignore) {
            } catch (Exception ex) {
                inquirer.logger.error("Interrupted", ex);
            }
            return null;
        }

        @Override
        public void done() {
            Toolkit.getDefaultToolkit().beep();
            progressMonitor.setProgress(0);
        }
    }

    class CustomStorage {

        private final int keyIdx;
        HashMap<Integer, CustomComponent> items = new HashMap<>();

        private CustomStorage(FullTable fullTable) throws Exception {
            keyIdx = fullTable.getColumnIdx(KEY_ID_FIELD);
            for (ArrayList<Object> row : fullTable.getData()) {
                addRow(fullTable, row);
            }
        }

        private CustomComponent newRow(String component, Integer id, String fieldName) {
            CustomComponent item = items.get(id);
            if (item == null) {
                item = new CustomComponent(id, component, fieldName);
                items.put(id, item);
            }
            return item;
        }

        //</editor-fold>//</editor-fold>
        public HashMap<Integer, CustomComponent> getItems() {
            return items;
        }

        private void addRow(FullTable fullTable, ArrayList<Object> row) throws Exception {
            Integer id = (Integer) row.get(keyIdx);
            String component = DatabaseConnector.getDatabaseConnector(null).persistentRef(
                    ReferenceType.CUSTCOMP,
                    id
            );
            CustomComponent customComponent = newRow(component, id, KEY_ID_FIELD);
            customComponent.fillRow(fullTable, row);
        }

        class CustomComponent {

            private final Integer id;
            // keyfield1->val1,val2,val3,...
            private final HashMap<String, KeyValues> keyValues;
            //            private String value;
            private String name;
            private String fieldName = null;

            private CustomComponent(Integer valueID) {
                this.keyValues = new HashMap<>();
                id = valueID;
            }

            private CustomComponent(Integer id, String name, String fieldName) {
                this(id);
                this.name = name;
                this.fieldName = fieldName;
            }

            public HashMap<String, KeyValues> getKeyValues() {
                return keyValues;
            }

            //            public String getValue() {
//                return value;
//            }
            public Integer getId() {
                return id;
            }

            public String getFieldName() {
                return fieldName;
            }

            //            private void setValue(String value) {
//                this.value = value;
//            }
            public String getName() {
                return name;
            }

            private void fillRow(FullTable fullTable, ArrayList<Object> row) throws Exception {
                for (int i = 1; i <= MAX_CUSTOM_FIELDS; i++) {
                    String keyFieldName = Parser.fldName(i);
                    Integer keyID = (Integer) row.get(fullTable.getColumnIdx(keyFieldName));
//                    String key = DatabaseConnector.getDatabaseConnector(null).persistentRef(
//                            ReferenceType.CUSTKKEY,
//                            (Integer) row.get(fullTable.getColumnIdx(keyFieldName))
//                    );
                    String valueFieldName = Parser.valName(i);
                    Integer valueID = (Integer) row.get(fullTable.getColumnIdx(valueFieldName));

//                    String value = DatabaseConnector.getDatabaseConnector(null).persistentRef(
//                            ReferenceType.CUSTVALUE,
//                            valueID
//                    );
                    if (keyID != null && keyID > 0 && valueID != null && valueID > 0) {
                        //using valueFieldName to be used later for search parameter
                        putKeyValue(valueFieldName, keyID, valueID);
                    }
                }

//                for (int i = 1; i <= MAX_CUSTOM_FIELDS; i++) {
//                    String keyFieldName = Parser.fldName(i);
//                    String key = DatabaseConnector.getDatabaseConnector(null).persistentRef(
//                            ReferenceType.CUSTKKEY,
//                            (Integer) row.get(fullTable.getColumnIdx(keyFieldName))
//                    );
//                    String valueFieldName = Parser.valName(i);
//                    Integer valueID = (Integer) row.get(fullTable.getColumnIdx(valueFieldName));
//                    
//                    String value = DatabaseConnector.getDatabaseConnector(null).persistentRef(
//                            ReferenceType.CUSTVALUE,
//                            valueID
//                    );
//                    
//                    if (value != null && !value.isEmpty()
//                            && key != null && !key.isEmpty()) {
//                        ArrayList<CustomItem> val = getPair(valueFieldName, key);
//                        if (!valuePresent(val, valueID)) {
//                            CustomComponent customItem = new CustomComponent(valueID);
//                            customItem.setValue(value);
//                            val.add(customItem);
//                        }
//                    }
//                }
            }

            private void putKeyValue(String keyFieldName, Integer keyID, Integer valueID) {
                KeyValues ret = keyValues.get(keyFieldName);
                if (ret == null) {
                    ret = new KeyValues();
                    keyValues.put(keyFieldName, ret);
                }
                ret.putKeyValue(keyID, valueID);
            }

            class KeyValues extends HashMap<Integer, HashSet<Integer>> {

                private void putKeyValue(Integer keyID, Integer valueID) {
                    HashSet<Integer> hsValues = get(keyID);
                    if (hsValues == null) {
                        hsValues = new HashSet<>();
                        put(keyID, hsValues);
                    }
                    hsValues.add(valueID);
                }
            }
        }
    }
}
