package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.inquirer.Wheres.Where;
import com.myutils.mygenerictree.GenericTreeNode;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static Utils.Util.sortedArray;

@SuppressWarnings("rawtypes")
public abstract class IQuery extends QueryTools {

    private static final int SPLIT_ON = 1000;
    private static final String[] RequestsToShow = {"RequestMakePredictiveCall", "RequestMakeCall", "RequestMonitorNextCall", "EventError"};
    static private int queryTypes;
    private final ArrayList<String> outFields;
    private final ArrayList<refTab> TabRefs;
    private final ArrayList<JoinTab> joinTabs;
    private final ArrayList<String> nullTab;
    private final ArrayList<String> nullFields = new ArrayList<>();
    private final ArrayList<String> idsToCollect = new ArrayList<>();
    private final HashMap<String, HashSet<Long>> collectIDsValues = new HashMap<>();
    protected IRecordLoadedProc recLoadedProc = null;
    protected String query;
    protected ArrayList<Integer> searchApps = null;
    protected UTCTimeRange timeRange = null;
    protected DatabaseConnector m_connector;
    protected ResultSet m_resultSet;
    protected int recCnt;
    Wheres addWheres = new Wheres();
    private ICalculatedFields calcFields = null;

    private boolean limitQueryResults;
    private int maxQueryLines;

    IQuery() {
        TabRefs = new ArrayList<>();
        joinTabs = new ArrayList<>();
        nullTab = new ArrayList<>();
        this.outFields = new ArrayList<>();
        incQueryTypes();
    }

    static void reSetQueryNo() {
        queryTypes = 0;
    }

    static void incQueryTypes() {
        queryTypes++;
    }

    static String getAttrsWhere(String tabColumn, DynamicTreeNode<OptionNode> node) {
        if (!isAllChecked(node)) {
            String checkedItems = getCheckedPairItems(node);
            if (checkedItems.length() > 0) {
                return tabColumn + " in ("
                        + checkedItems + ")";
            }
        }
        return "";
    }

    public static int getQueryTypes() {
        return queryTypes;
    }

    static public String getLimitClause(boolean _limitQueryResults, int _maxQueryLines) {
        if (_limitQueryResults) {
            return " LIMIT " + _maxQueryLines + " ";
        }
        return "";
    }

    public static String getFileFilters(String tab, String fileIDField, ArrayList<Integer> apps, String andOr) {
        String ret = getFileFilters(tab, fileIDField, apps);

        if (ret != null && !ret.isEmpty()) {
            return andOr + " " + ret;
        }
        return "";
    }

    public static String getFileFilters(String tab, String fileIDField, ArrayList<Integer> apps) {
        if (apps != null && !apps.isEmpty()) {
            StringBuilder q = new StringBuilder().append("+(");
            if (tab != null && !tab.isEmpty()) {
                q.append(tab).append(".");
            }

            q.append(fileIDField).append(" in (select id from file_logbr ").append(getWhere("appnameid",
                    apps.toArray(new Integer[apps.size()]), true)).append(" )");
            q.append(")");
            return q.toString();
        }
        return null;
    }

    public static String getDateTimeFilters(String tab, String timeField, UTCTimeRange timeRange, String andOr) {
        String ret = getDateTimeFilters(tab, timeField, timeRange);
        if (ret != null && !ret.isEmpty()) {
            return andOr + " " + ret;
        }
        return "";

    }

    public static String getDateTimeFilters(String tab, String timeField, UTCTimeRange timeRange) {
        if (timeRange != null) {
            return "+(" + tab + "." + timeField + " between " + timeRange.getStart() + " and " + timeRange.getEnd() + ")";
        }
        return null;
    }

    private static String getCheckedItems(DynamicTreeNode<OptionNode> orsMetrics) {
        StringBuilder ret = new StringBuilder(1024);
        for (DynamicTreeNode<OptionNode> node : orsMetrics.getChildren()) {
            if (((OptionNode) node.getData()).isChecked()) {
                if (ret.length() > 0) {
                    ret.append(",");
                }
                ret.append("\"").append(((OptionNode) node.getData()).getName()).append("\"");
            }
        }
        return ret.toString();
    }

    private static String getCheckedPairItems(DynamicTreeNode<OptionNode> orsMetrics) {
        StringBuilder ret = new StringBuilder();
        for (DynamicTreeNode<OptionNode> node : orsMetrics.getChildren()) {
            if (((OptionNode) node.getData()).isChecked()) {
                if (ret.length() > 0) {
                    ret.append(",");
                }
                ret.append("\"").append(((OptionNode) node.getData()).getValue()).append("\"");
            }
        }
        return ret.toString();
    }

    public static boolean isAllChecked(DynamicTreeNode<OptionNode> orsMetrics) {
        if (orsMetrics != null) {
            for (DynamicTreeNode<OptionNode> node : orsMetrics.getChildren()) {
                if (!((OptionNode) node.getData()).isChecked()) {
                    return false;
                }
            }
        }
        return true;
    }

    static String getCheckedWhere(String nameID, ReferenceType referenceType, DynamicTreeNode<OptionNode> node, DialogItem di) {
        return getCheckedWhere(nameID, referenceType, node, di, false);
    }

    static String getCheckedWhere(String nameID, ReferenceType referenceType, DynamicTreeNode<OptionNode> node, DialogItem di, boolean optimize) {
        if (node != null) {
            DynamicTreeNode<OptionNode> nameSettings = getChildByName(node, di);
            if (isChecked(nameSettings) && !OptionNode.AllChildrenChecked(nameSettings)) {
                return getCheckedWhere(nameID, referenceType, nameSettings, optimize);
            }
        }
        return null;
    }

    static String getCheckedWhere(String nameID, DynamicTreeNode<OptionNode> node, DialogItem di) {
        if (node != null) {
            DynamicTreeNode<OptionNode> nameSettings = getChildByName(node, di);
            if (isChecked(nameSettings) && !OptionNode.AllChildrenChecked(nameSettings)) {
                return getCheckedWhere(nameID, nameSettings);
            }
        }
        return null;
    }

    static String getCheckedWhere(String nameID, ReferenceType referenceType, DynamicTreeNode<OptionNode> nameSettings, String andOr) {
        String ret = getCheckedWhere(nameID, referenceType, nameSettings);
        if (ret != null && !ret.isEmpty()) {
            return " " + andOr + " " + ret;
        } else {
            return "";
        }
    }

    static String getCheckedWhere(String nameID, ReferenceType referenceType, DynamicTreeNode<OptionNode> nameSettings) {
        return getCheckedWhere(nameID, referenceType, nameSettings, false);
    }

    static String getCheckedWhere(String nameID, ReferenceType referenceType, DynamicTreeNode<OptionNode> nameSettings, boolean optimize) {
        if (nameSettings != null) {
            GenericTreeNode parent = nameSettings.getParent();
            if (parent != null
                    && (parent.getData() == null || ((OptionNode) parent.getData()).isChecked() && !OptionNode.AllChildrenChecked(nameSettings))) {
                StringBuilder ret = new StringBuilder();

                if (optimize) {
                    ret.append("+(");
                }
                ret.append(nameID).append(" in \n").append(getRefSubQuery(referenceType, OptionNode.getChecked(nameSettings)));
                if (optimize) {
                    ret.append(")");
                }
                return ret.toString();

            }
        }
        return null;
    }

    static String getCheckedWhere(String nameID, DynamicTreeNode<OptionNode> nameSettings) {
        if (nameSettings != null) {
            GenericTreeNode parent = nameSettings.getParent();
            if (parent != null) {
                if (parent.getData() == null || ((OptionNode) parent.getData()).isChecked() && !OptionNode.AllChildrenChecked(nameSettings)) {
                    String checkedItems = getCheckedItems(nameSettings);
                    if (checkedItems.length() > 0) {
                        return nameID + " in (" + checkedItems + ")";
                    }
                }
            }
        }
        return null;
    }

    public static String getWhere(String Field, String[] ids) {
        return getWhere(Field, ids, false, false);

    }

    public static String getWhere(String[] Fields, ANDOR fieldAndOr, Integer[] ids) {
        return getWhere(Fields, fieldAndOr, ids, false);

    }

    public static String getWhere(String Field, Integer[] ids) {
        return getWhere(Field, ids, false);

    }

    public static String getWhere(String Field, String[] ids, boolean addWhere) {
        return getWhere(Field, ids, addWhere, false);
    }

    public static String getWhere(String Field, ReferenceType rt, String searchString, boolean regexSearch) {
        StringBuilder ret = new StringBuilder(searchString.length() + 120);

        ret.append(Field).append(" in ( select id from ").append(rt.toString()).append(" where name ");
        if (regexSearch) {
            ret.append(" REGEXP \"").append(searchString).append("\"");
        } else {
            ret.append(" = \"").append(searchString).append("\"");
        }
        ret.append(" )");
        return ret.toString();
    }

    public static String getWhere(String Field, ReferenceType rt, String[] searchStrings, boolean regexSearch) {
        return QueryTools.getWhere(Field, rt, searchStrings, false, regexSearch);

    }

    public static String escapeString(String str, boolean toLower) {
        return (toLower)
                ? StringUtils.replace(str, "\"", "\"\"").toLowerCase()
                : StringUtils.replace(str, "\"", "\"\"");
    }

    public static String getIDsList(String Field, ArrayList<Integer> ids, boolean addWhere, boolean optimize) {
        if (ids != null && !ids.isEmpty()) {
            if (inquirer.logger.isDebugEnabled()) {
                Collections.sort(ids);
            }
            StringBuilder ret = new StringBuilder(ids.size() * 50);
            if (addWhere) {
                ret.append(" where ");
            }

            if (optimize) {
                ret.append("+");
            }
            ret.append("(").append(Field).append(" in ");
            for (int i = 0; i < ids.size(); i++) {
                if (i % SPLIT_ON == 0) {
                    if (i > 0) {
                        ret.append(") or ").append(Field).append(" in ");
                    }
                    ret.append("(");
                } else {
                    ret.append(",");
                }
                ret.append(ids.get(i));

            }
            ret.append(") ");
            ret.append(")");

            return ret.toString();
        }
        return "";
    }

    public static String getWhere(String Field, String[] ids, boolean addWhere, boolean isRegex) {
        if (ids != null && ids.length > 0) {
            StringBuilder ret = new StringBuilder(ids.length * 50);
            if (addWhere) {
                ret.append(" where ");
            }

            ret.append("(");
            for (int i = 0; i < ids.length; i++) {
                if (i % SPLIT_ON == 0) {
                    if (i > 0) {
                        ret.append(") or ");
                    }
                    ret.append("(");
                } else {
                    ret.append(" or ");
                }
                if (isRegex) {
                    ret.append(Field)
                            .append(" REGEXP \"")
                            .append(escapeString(ids[i], false))
                            .append("\"");
                } else {
                    ret.append(Field)
                            .append("=\"")
                            .append(escapeString(ids[i], false))
                            .append("\"");
                }
            }
            ret.append(") ");
            ret.append(")");

            return ret.toString();
        }
        return "";
    }

    public static String getWhere(String Field1, String Field2, Integer[] ids, boolean addWhere) {
        if (ids != null && ids.length > 0) {
            return getWhere(Field1, ids, addWhere) + " or " + getWhere(Field2, ids, false);
        } else {
            return "";
        }
    }

    public static String getWhere(String Field1, String Field2, Long[] ids, boolean addWhere) {
        if (ids != null && ids.length > 0) {
            return getWhere(Field1, ids, addWhere) + " or " + getWhere(Field2, ids, false);
        } else {
            return "";
        }
    }

    public static String getWhere(String[] Fields, ANDOR fieldAndOr, Integer[] ids, boolean addWhere) {
        if (ids != null && ids.length > 0) {

            StringBuilder ret = new StringBuilder(ids.length * 50 * Fields.length);

            if (addWhere) {
                ret.append(" where ");
            }
            ret.append("(");
            for (int i = 0; i < Fields.length; i++) {
                if (i > 0) {
                    ret.append(" ").append(fieldAndOr.toString()).append(" ");
                }

                ret.append("(").append(
                        gatherIDs(Fields[i],
                                (inquirer.logger.isDebugEnabled()) ? sortedArray(ids).toArray(new Integer[ids.length]) : ids
                        ).append(")"));

            }
            ret.append(")");

            return ret.toString();

        } else {
            return "";
        }
    }

    private static StringBuilder gatherIDs(String Field, Integer[] ids) {
        StringBuilder ret = new StringBuilder(ids.length * 50);

        for (int i = 0; i < ids.length; i++) {
            if (i % SPLIT_ON == 0) {
                if (i > 0) {
                    ret.append(") or");
                }
                ret.append("\n").append(Field).append(" in (");
            } else {
                ret.append(",");
            }
            ret.append(ids[i]);
        }
        ret.append(")");

        return ret;
    }

    private static StringBuilder gatherIDs(String Field, Integer[] ids, boolean isAmong) {
        Long[] longIds = new Long[ids.length];
        for (int i = 0; i < ids.length; i++) {
            longIds[i] = Long.valueOf(ids[i]);

        }
        return gatherIDs(Field, longIds, isAmong);

    }

    private static StringBuilder gatherIDs(String Field, Long[] ids, boolean isAmong) {
        StringBuilder ret = new StringBuilder(ids.length * 50);

        for (int i = 0; i < ids.length; i++) {
            if (i % SPLIT_ON == 0) {
                if (i > 0) {
                    ret.append(") \n");
                    if (!isAmong) {
                        ret.append("and");
                    } else {
                        ret.append("or");
                    }
                }
                ret.append("\n").append(Field);
                if (!isAmong) {
                    ret.append(" not");
                }

                ret.append(" in (");
            } else {
                ret.append(",");
            }
            ret.append(ids[i]);
        }
        ret.append(")");

        return ret;
    }

    public static String getWhereNot(String Field, Integer[] ids, boolean addWhere, boolean isIn) {
        if (ids != null && ids.length > 0) {
            StringBuilder ret = new StringBuilder(ids.length * 50);

            if (addWhere) {
                ret.append(" where ");
            }
            ret.append("(").append(gatherIDs(Field,
                    (inquirer.logger.isDebugEnabled()) ? sortedArray(ids).toArray(new Integer[ids.length]) : ids,
                    isIn).append(")"));

            return ret.toString();

        } else {
            return "";
        }

    }

    public static String getWhere(String Field, Long[] ids, boolean addWhere, boolean isIn) {
        if (ids != null && ids.length > 0) {
            StringBuilder ret = new StringBuilder(ids.length * 50);

            if (addWhere) {
                ret.append(" where ");
            }
            ret.append("(").append(gatherIDs(Field,
                    (inquirer.logger.isDebugEnabled()) ? sortedArray(ids).toArray(new Long[ids.length]) : ids,
                    isIn).append(")"));

            return ret.toString();

        } else {
            return "";
        }
    }

    public static String getWhere(String Field, Integer[] ids, boolean addWhere) {
        if (ids != null && ids.length > 0) {

            StringBuilder ret = new StringBuilder(ids.length * 50);

            if (addWhere) {
                ret.append(" where ");
            }
            ret.append("(").append(
                    gatherIDs(Field,
                            (inquirer.logger.isDebugEnabled()) ? sortedArray(ids).toArray(new Integer[ids.length]) : ids
                    ).append(")")
            );

            return ret.toString();

        } else {
            return "";
        }
    }

    public static String getAttrsWhere(String tabColumn, String refIdColumn, String refNameColumn, String refTable, DynamicTreeNode<OptionNode> node) {
        String checkedItems = getCheckedItems(node);
        if (checkedItems.length() > 0) {
            return tabColumn + " in (select " + refIdColumn
                    + " from " + refTable
                    + " where " + refNameColumn + " in (" + checkedItems + ")" + ")";
        }
        return "";
    }

    public static String getAttrsWhere(String tabColumn, ReferenceType referenceType, DynamicTreeNode<OptionNode> node) {
        if (!isAllChecked(node)) {
            return getAttrsWhere(tabColumn, "id", "name", referenceType.toString(), node);
        } else {
            return "";
        }
    }

    public static String getAttrsWhere(String[] tabColumns, ReferenceType referenceType, DynamicTreeNode<OptionNode> node) {
        if (!isAllChecked(node)) {
            StringBuilder ret = new StringBuilder();
            for (String tabColumn : tabColumns) {
                if (ret.length() > 0) {
                    ret.append(" or ");
                }
                ret.append(getAttrsWhere(tabColumn, "id", "name", referenceType.toString(), node));
            }
            if (ret.length() > 0) {
                return " ( " + ret.append(" ) ");
            }
        }
        return "";
    }

    public boolean isWheresEmpty() {
        return addWheres.isEmpty();
    }

    public IRecordLoadedProc getRecLoadedProc() {
        return recLoadedProc;
    }

    public void setRecLoadedProc(IRecordLoadedProc recLoadedProc) {
        this.recLoadedProc = recLoadedProc;
    }

    protected void recLoaded(ILogRecord tLibEvent) {
        if (recLoadedProc != null) {
            recLoadedProc.recordLoaded(tLibEvent);
        }
        try {
            doCollectIDs(m_resultSet);
        } catch (SQLException ex) {
            Logger.getLogger(IQuery.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void setCalculatedFields(ICalculatedFields iCalculatedFields) {
        calcFields = iCalculatedFields;
    }

    public ICalculatedFields getCalcFields() {
        return calcFields;
    }

    public abstract void Execute() throws SQLException;

    public abstract ILogRecord GetNext() throws SQLException;

    public abstract void Reset() throws SQLException;

    public void setSearchApps(ArrayList<Integer> searchApps) {
        inquirer.logger.trace("Search app: " + searchApps.size());
        this.searchApps = searchApps;
    }

    @Override
    public boolean isLimitQueryResults() {
        return limitQueryResults;
    }

    @Override
    public int getMaxQueryLines() {
        return maxQueryLines;
    }

    @Override
    void setQueryLimit(boolean _limitQueryResults, int _maxQueryLines) {
        this.limitQueryResults = _limitQueryResults;
        this.maxQueryLines = _maxQueryLines;
    }

    protected String getLimitClause() {
        return getLimitClause(isLimitQueryResults(), getMaxQueryLines());
    }

    public String getFileFilters(String tab, String fileIDField) {
        return getFileFilters(tab, fileIDField, searchApps);
    }

    protected void addFileFilters(String tab, String fileIDField) {
        inquirer.logger.trace("File filters defined: " + (searchApps != null && searchApps.size() > 0));
        if (searchApps != null && searchApps.size() > 0) {
            addWhere(getFileFilters(tab, fileIDField), "AND");
        }

    }

    protected void addDateTimeFilters(String tab, String timeField) {

        addWhere(getDateTimeFilters(tab, timeField, timeRange), "AND");

    }

    public UTCTimeRange getTimeRange() throws SQLException {
        return timeRange;
    }

    public void setTimeRange(UTCTimeRange timeRange) {
        this.timeRange = timeRange;
    }

    public String getQuery() {
        return query;
    }

    public boolean refTablesExist() throws SQLException {
        boolean ret = true;

        for (refTab TabRef : TabRefs) {
            String tab = TabRef.getRefTableName();
            inquirer.logger.trace("Checking is table [" + tab + "] exists");
            if (!(ret = DatabaseConnector.TableExist(tab))) {
                break;
            }
        }
        return ret;
    }

    Where addWhere(String where, String or) {
        return addWheres.addWhere(where, or);
    }

    void addWhere(Wheres _wh, String or) {
        addWheres.addWhere(_wh, or);
    }

    void addWhere(Wheres wh) {
        addWheres.addWhere(wh);
    }

    Where AddCheckedWhere(String nameID, ReferenceType referenceType, DynamicTreeNode<OptionNode> node, String AndOr, DialogItem di) {
        if (node != null) {
            DynamicTreeNode<OptionNode> nameSettings = getChildByName(node, di);
            if (isChecked(nameSettings)) {

                switch (OptionNode.getChildrenChecked(nameSettings)) {
                    case PARTIALLY_CHECKED:
                        return AddCheckedWhere(nameID, referenceType, nameSettings, AndOr);

                    case NONE_CHECKED:
                        return addWhere("0=1", AndOr);
                }

            }
        }
        return null;

    }

    void AddCheckedCustomWhere(String nameID, DynamicTreeNode<OptionNode> node, String AndOr) {
        if (node != null) {
            if (node.getData() != null && ((OptionNode) node.getData()).isChecked()) {
                Wheres wh = new Wheres();
                String isNull = OptionNode.getEmptyClause(node);
                String isNullClause = "";
                if (isNull != null) {
                    isNullClause = " or " + nameID + " " + isNull;
                }
                ArrayList<Integer> keyIDs = new ArrayList<>();
                for (DynamicTreeNode<OptionNode> n : node.getChildren()) {
                    if (isChecked(n)) {
                        for (DynamicTreeNode<OptionNode> n1 : n.getChildren()) {
                            if (isChecked(n1)) {
                                IQueryResults.CustomOptionNode data1 = (IQueryResults.CustomOptionNode) n1.getData();
                                String fldName = data1.getFld();
                                ArrayList<Integer> attrIDs = new ArrayList<>();
                                for (DynamicTreeNode<OptionNode> n2 : n1.getChildren()) {
                                    if (isChecked(n2)) {
                                        IQueryResults.CustomOptionNode data = (IQueryResults.CustomOptionNode) n2.getData();
                                        attrIDs.add(data.getId());
                                    }
                                }
                                wh.addWhere(getWhere(fldName, attrIDs.toArray(new Integer[attrIDs.size()])), "OR");
                            }
                        }
                    }
//                    TClonable data = n.getData();
//                    if (data instanceof IQueryResults.CustomOptionNode) {
//                        IQueryResults.CustomOptionNode n1 = (IQueryResults.CustomOptionNode) data;
//                        if (n1.isChecked()) {
//                            keyIDs.add(n1.getId());
//                            for (DynamicTreeNode<OptionNode> attName : n1.getChildren()) {
//                                if ((attName.getData() != null && !OptionNode.AllChildrenChecked(attName))) {
//                                    ArrayList<Integer> attrIDs = new ArrayList<>();
//                                    IQueryResults.CustomOptionNode n2 = (IQueryResults.CustomOptionNode) attName.getData();
//                                    String fldName = n2.getFld();
//                                    for (DynamicTreeNode<OptionNode> attr : attName.getChildren()) {
//                                        IQueryResults.CustomOptionNode attrNode = (IQueryResults.CustomOptionNode) attr.getData();
//                                        if (attrNode.isChecked()) {
//                                            attrIDs.add(attrNode.getId());
//                                        }
//                                    }
//                                    wh.addWhere(getWhere(fldName, (Integer[]) attrIDs.toArray(new Integer[attrIDs.size()])), "AND");
//                                }
//                            }
//                        }
//                    } else {
//                        inquirer.logger.info("Is not custom node: " + data.toString() + "; type: " + data.getClass().getName());
//                    }
                }
                if (keyIDs.size() < node.getNumberOfChildren()) {
                    wh.addWhere(getWhere(nameID, keyIDs.toArray(new Integer[keyIDs.size()])), "AND");
                }
                addWhere(wh, AndOr);
            }
        }
    }

    Where AddCheckedWhere(String nameID, ReferenceType referenceType, DynamicTreeNode<OptionNode> nameSettings, String AndOr) {
        GenericTreeNode parent = nameSettings.getParent();
        if (parent != null) {
            if (parent.getData() == null || ((OptionNode) parent.getData()).isChecked() && !OptionNode.AllChildrenChecked(nameSettings)) {
                String isNullClause = "";
                return addWhere(
                        OptionNode.checkedChildrenList(nameID, nameSettings) + isNullClause, " " + AndOr + " ");
            }
        }
        return null;
    }

    void AddCheckedWhere(String nameID, ReferenceType referenceType, DynamicTreeNode<OptionNode> nameSettings) throws Exception {
        AddCheckedWhere(nameID, referenceType, nameSettings, "or");
    }

    void addOutFields(String [] strings) {
        Collections.addAll(this.outFields, strings);
    }


    final void addOutField(String fld) {
        outFields.add(fld);
    }

    public String addedFieldString(boolean startWithComa, boolean finishWithComa) {
        StringBuilder qString = new StringBuilder();
        if (outFields != null && !outFields.isEmpty()) {
            if (startWithComa) {
                qString.append(",");
            }
            boolean fieldsAdded = false;
            for (String outField : outFields) {
                if (fieldsAdded) {
                    qString.append(",");
                }
                qString.append(outField);
                fieldsAdded = true;
            }
        }
        if (qString.length() > 0 && finishWithComa) {
            qString.append(" ,");
        }
        return qString.toString();
    }

    final public void addRef(String refField, String outField, ReferenceType refTable, FieldType ft) throws SQLException {
        addRef(refField, outField, refTable.toString(), ft);
    }

    final public void addRef(String refField, String outField, String refTable, FieldType ft) throws SQLException {
        if (DatabaseConnector.refTableExist(refTable)) {
            TabRefs.add(new refTab(refField, outField, refTable, ft));
        } else {
            nullTab.add(refTable);
            addNullField(outField);
        }
    }

    public boolean removeRef(String refField, String refTable) {
        int i = 0;
        for (i = 0; i < TabRefs.size(); i++) {
            refTab tr = TabRefs.get(i);
            if (tr.eqRef(refField, refTable)) {
                break;
            }
        }
        if (i < TabRefs.size()) {
            refTab remove = TabRefs.remove(i);
            inquirer.logger.debug("removing " + i + ": " + remove);
            return true;
        }
        return false;
    }

    public void addJoin(String joinTable, FieldType fieldType, String joinCond, String joinWhere) {
        addJoin(joinTable, fieldType, joinCond, joinWhere, joinTable);
    }

    public void addJoin(String joinTable, FieldType fieldType, String joinCond, String joinWhere, String joinTableAlias) {
        joinTabs.add(new JoinTab(joinTable, joinCond, joinWhere, fieldType, joinTableAlias));
        addWhere(joinWhere, "");
    }

    private String UniqueTable(String refTable) {
        int id = 0;
        for (int i = 0; i < TabRefs.size(); i++) {
            String s = TabRefs.get(i).getRefTable();

            if (s.startsWith(refTable)) {
                int idCur = Integer.parseInt(s.substring(refTable.length()));
                if (idCur > id) {
                    id = idCur;
                }

            }
        }
        id++;
        return refTable + String.format("%02d", id);
    }

    public boolean isTabRefsEmpty() {
        return TabRefs == null || TabRefs.isEmpty();
    }

    public String getRefFields() {
        StringBuilder ret = new StringBuilder(TabRefs.size() * 30 + 10);
        for (refTab r : TabRefs) {
            ret.append(", ").append(r.getRetField());
        }
        return ret.toString();
    }

    public String getJoins() {
        if (!joinTabs.isEmpty()) {
            StringBuilder ret = new StringBuilder();
            for (JoinTab r : joinTabs) {
                ret.append(" ").append(r.getJoin());
            }
            return ret.toString();
        } else {
            return "";
        }
    }

    public String getJoinFields() {
        if (!joinTabs.isEmpty()) {
            StringBuilder ret = new StringBuilder(joinTabs.size() * 100 + 10);
            for (JoinTab r : joinTabs) {
                ret.append(", ").append(r.getJoinField());
            }
            return ret.toString();
        } else {
            return "";
        }
    }

    public String getRefs() {
        if (!TabRefs.isEmpty()) {
            StringBuilder ret = new StringBuilder();
            for (refTab r : TabRefs) {
                ret.append(" ").append(r.getJoin());
            }
            return ret.toString();
        } else {
            return "";
        }
    }

    final public void addNullField(String fld) {
        nullFields.add(fld);
    }

    protected String getNullFields() {

        if (nullFields.size() > 0) {
            StringBuilder ret = new StringBuilder(nullFields.size() * 30 + 20);
            for (String fld : nullFields) {
                if (ret.length() > 0) {
                    ret.append(", ");
                }
                ret.append("null as ").append(fld);
            }
            return ", " + ret;
        }
        return "";
    }

    public boolean isnullFieldsEmpty() {
        return nullFields == null || nullFields.isEmpty();
    }

    protected Integer[] locateRef(String dn, ReferenceType rt, boolean isRegex) throws Exception {
        String tmpDN = com.myutils.logbrowser.indexer.Record.SingleQuotes(dn);
        Integer[] DNIDs = getRefIDs(rt, new String[]{tmpDN}, isRegex);
        if (DNIDs.length == 0) {
            return null;
        }
        return DNIDs;
    }

    void addCollectIDs(String refid) {
        idsToCollect.add(refid);
    }

    protected boolean isCollectingID(String field) throws SQLException {
        return collectIDsValues.containsKey(field);
    }

    protected HashSet<Long> getCollectID(String field) throws SQLException {
        if (collectIDsValues.containsKey(field)) {
            return collectIDsValues.get(field);
        } else {
            throw new SQLException("Not collecting IDs for field [" + field + "]");
        }
    }

    protected void doCollectIDs(ResultSet m_resultSet) throws SQLException {
        for (String collectID : idsToCollect) {
            HashSet<Long> ids;
            Long l = m_resultSet.getLong(collectID);
            if (l != null && l > 0) {
                if (collectIDsValues.containsKey(collectID)) {
                    ids = collectIDsValues.get(collectID);
                } else {
                    ids = new HashSet<>();
                    collectIDsValues.put(collectID, ids);
                }
                ids.add(l);
            }
        }
    }

    protected void doneExtracting(MsgType msgType) {
        inquirer.logger.debug("++ " + msgType + ": extracted " + recCnt + " records");
        if (isLimitQueryResults() && getMaxQueryLines() <= recCnt) {
            String msg = "More records available for " + msgType.toString() + "; Go to Settings->Query to increase the limit (" + getMaxQueryLines() + ")";
            queryMessagesAdd(msg);
            inquirer.logger.info(msg);
//            JOptionPane.showMessageDialog(null, msg, "Too many records", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String fullFieldName(String tab, String col) {
        if (tab != null && !tab.isEmpty()) {
            return tab + "." + col;
        } else {
            return col;
        }
    }

    protected String tlibReqs(String tabName, String id, Collection<Long> recIDs, String referenceID,
            Collection<Long> refIDs,
            String[] dnIDsFields, Collection<Long> dnIDs) {
        String s = null;
        if (recIDs != null && !recIDs.isEmpty()) {
            s = getWhere(fullFieldName(tabName, "id"), recIDs, false);
        }
        StringBuilder r = new StringBuilder();
        if (s != null) {
            r.append("(");
        }
        r.append(getWhere(fullFieldName(tabName, "referenceID"), refIDs, false));
        if (s != null) {
            r.append("\nand not ").append(s).append(")");
        }
        Wheres whDNs = new Wheres();
        for (String dnIDsField : dnIDsFields) {
            if (dnIDsField != null && !dnIDsField.isEmpty()) {
                whDNs.addWhere(IQuery.getWhere(fullFieldName(tabName, dnIDsField), dnIDs, false), "OR");
            }
            whDNs.addWhere(fullFieldName(tabName, dnIDsField) + " is null", "OR");
            whDNs.addWhere(fullFieldName(tabName, dnIDsField) + "<=0", "OR");
        }
        if (!whDNs.isEmpty()) {
            r.append("\nand (").append(whDNs.makeWhere(false)).append(")");
        }
        r.append("\nand ").append(getWhere(fullFieldName(tabName, "nameid"), ReferenceType.TEvent, RequestsToShow, false));

        return r.toString();
    }

    void setCommonParams(IQueryResults qry, QueryDialog dlg) throws SQLException {
        setQueryLimit(qry.isLimitQueryResults(), qry.getMaxQueryLines());
        setTimeRange(dlg.getTimeRange());
        setSearchApps(dlg.getSearchApps());
    }

    public String stdFields(String alias) {
        StringBuilder ret = new StringBuilder();
        for (String s : new String[]{
            "id",
            "time",
            "FileId",
            "FileOffset",
            "FileBytes",
            "line"
        }) {
            if (ret.length() > 0) {
                ret.append(",");
            }
            ret.append(alias).append(".").append(s);

        }
        return ret.toString();
    }

    public enum ANDOR {

        AND("AND"),
        OR("OR"),;

        private final String name;

        ANDOR(String s) {
            name = s;
        }

        //    public boolean equals(DialogItem item) {
//
//    }
        public boolean equalsName(String otherName) {
            return name.equalsIgnoreCase(otherName);
        }

        @Override
        public String toString() {
            return this.name;
        }

    }

    enum FieldType {
        MANDATORY,
        OPTIONAL
    }

    @FunctionalInterface
    public interface IRecordLoadedProc {

        void recordLoaded(ILogRecord rec);
    }

    class refTab {

        private final String refField;
        private final String outField;
        private final String refTable;
        private final String refTabAlias;
        private final FieldType ft;

        refTab(String refField, String outField, String refTable, FieldType ft) {
            this.outField = outField;
            this.refField = refField;
            this.refTable = refTable;
            this.refTabAlias = UniqueTable(refTable);
            this.ft = ft;

            inquirer.logger.trace(this.toString());
        }

        public String getRefTable() {
            return refTabAlias;
        }

        public String getRefTableName() {
            return refTable;
        }

        @Override
        public String toString() {
            return "refTab{" + "refField=" + refField + ", outField=" + outField + ", refTable=" + refTable + ", refTabAlias=" + refTabAlias + ", ft=" + ft + '}';
        }

        String getJoin() {
            String jType = "";

            switch (ft) {
                case MANDATORY:
                    jType = "INNER";
                    break;
                case OPTIONAL:
                    jType = "LEFT";
                    break;
            }
            return jType + " JOIN " + refTable + " AS " + refTabAlias + " on "
                    + refField + "=" + refTabAlias + ".id\n";
        }

        String getRetField() {
            return refTabAlias + ".name as " + outField;
        }

        private boolean eqRef(String _refField, String _refTable) {
            inquirer.logger.trace("Comparing " + this.refField + " vs " + _refField
                    + "; " + this.refTable + " vs " + _refTable);
            return (this.refField.equals(_refField) && this.refTable.equals(_refTable));
        }
    }

    class JoinTab {

        private final String tabAlias;
        private final FieldType ft;
        private String joinTable = null;
        private String JoinExpr = null;
        private String JoinWhere = null;

        public JoinTab(String joinTable, String JoinExpr, String JoinWhere, FieldType ft, String tabAlias) {
            this.joinTable = joinTable;
            this.JoinExpr = JoinExpr;
            this.JoinWhere = JoinWhere;
            this.ft = ft;
            this.tabAlias = tabAlias;
        }

        public JoinTab(String joinTable, String JoinExpr, String JoinWhere, FieldType ft) {
            this(joinTable, JoinExpr, JoinWhere, ft, joinTable);
        }

        String getJoin() {
            String jType = "";

            switch (ft) {
                case MANDATORY:
                    jType = "INNER";
                    break;
                case OPTIONAL:
                    jType = "LEFT";
                    break;
            }

            //            if (JoinWhere != null) {
//                ret.append(" where " + JoinWhere);
//            }
            return jType + " JOIN " + joinTable + " AS " + tabAlias + " on " + JoinExpr + "\n" //            if (JoinWhere != null) {
                    //                ret.append(" where " + JoinWhere);
                    //            }
                    ;
        }

        private String getJoinField() {
            return "";
        }
    }

}
