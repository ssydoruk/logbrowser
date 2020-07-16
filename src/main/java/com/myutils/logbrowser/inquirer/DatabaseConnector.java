package com.myutils.logbrowser.inquirer;

import Utils.UTCTimeRange;
import static Utils.Util.pDuration;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.Main;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import static com.myutils.logbrowser.inquirer.QueryTools.getWhere;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.sqlite.Function;
import static org.sqlite.SQLiteErrorCode.SQLITE_INTERRUPT;

public class DatabaseConnector {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private static final HashMap<String, DateTimeFormatter> parsedFormats = new HashMap<>();
    private static DatabaseConnector databaseConnector = null;
    // List of function names;
    public static final String CampaignGroupDBIDtoName = "CampaignGroupDBIDtoName";
    public static final String CampaignDBIDtoName = "CampaignDBIDtoName";
    public static final String GroupDBIDtoName = "GroupDBIDtoName";
    public static final String ListDBIDtoName = "ListDBIDtoName";
    private static final HashMap<String, Boolean> tabExists = new HashMap<String, Boolean>();
    static public Statement currentStatement = null;
    static private int noAppTypes = -1;

    static void dropTable(String tab) throws SQLException {
//        if (TableExist(tab, true)) {
//            executeQuery("drop table " + tab + ";");
//        }
        runQuery("drop table if exists " + tab + ";");

    }

    static public long convertToTime(Object val) {
        if (val instanceof Long) {
            return (long) val;
        } else {
            return stringToDate(val.toString());
        }
    }

    public static String dateToString(long time) {
        return dateToString(new Date(time));
    }

    public static String dateToString(Date dateTime) {
        if (inquirer.getCr().isFullTimeStamp()) {
            return (new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS")).format(dateTime);
        } else {
            return (new SimpleDateFormat("HH:mm:ss.SSS")).format(dateTime);
        }
    }

    static UTCTimeRange getTimeRangeByAppID(int id) throws SQLException {
        String[] names = DatabaseConnector.getNames(DatabaseConnector.class, ReferenceType.AppType.toString(), "name",
                IQueryResults.getWhere("id", "select apptypeid from file_logbr where appnameid = " + id,
                        true));
        if (names != null && names.length > 0) {
            String TLibTableByFileType = QueryTools.TLibTableByFileType(names[0]);
            if (TLibTableByFileType != null) {
                ArrayList<Integer> searchApps = new ArrayList<>(1);
                searchApps.add(id);
                return getTimeRange(new String[]{TLibTableByFileType}, searchApps);
            }
        }
        return null;
    }

    private static void checkLimitClause(int recsUpdated) {
//        if (recsUpdated > 1) {
//            if (inquirer.getCr().isLimitQueryResults()) {
//                if(recsUpdated>=getMaxQueryLines())
//                     JOptionPane.showMessageDialog(null, "Affected "+recsUpdated+" records;\nlikely more available.\n Increase limit if needed", "More records available", 0);
//            }
//        };

    }

    private static long parseTime(String dateString, String dateFormat) {
//    DateTimeFormatter formatter =
//                      DateTimeFormatter.ofPattern("MMM d yyyy");
//    LocalDate date = LocalDate.parse(input, formatter);
//    System.out.printf("%s%n", date);

        DateTimeFormatter fmt = parsedFormats.get(dateFormat);
        if (fmt == null) {
            fmt = DateTimeFormatter.ofPattern(dateFormat);
//            fmt.setLenient(false); // Don't automatically convert invalid date.
            parsedFormats.put(dateFormat, fmt);
        }

        try {
            LocalDateTime localDate = LocalDateTime.parse(dateString, fmt);
            return localDate.atZone(Utils.UTCTimeRange.zoneId).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    /**
     * converts string to time.
     *
     * @param valueAt - string
     * @return in case of error returns 0
     */
    public static long stringToDate(Object valueAt) {
        if (valueAt != null) {
            String v = valueAt.toString();
            if (inquirer.getCr().isFullTimeStamp()) {
                // in new java date parsers, YYYY is for year in weaks
                // yyyy in year of epoch
                return parseTime(v, "yyyy-MM-dd HH:mm:ss.SSS");
            } else {
                return parseTime(v, "HH:mm:ss.SSS");
            }
        }
        return 0;
    }

    static CallableStatement prepareStatement(String expr) throws SQLException {
//        PreparedStatement ret = null;

        return databaseConnector.m_conn.prepareCall(expr);

    }

    private static boolean textColumn(int columnType) {
        return columnType == java.sql.Types.CHAR
                || columnType == java.sql.Types.NVARCHAR;
    }

    private static boolean numColumn(int columnType) {
        return columnType == java.sql.Types.BIGINT
                || columnType == java.sql.Types.INTEGER;
    }

    public static void cancelCurrent() {
        if (currentStatement != null) {
            try {
                currentStatement.close();
                currentStatement.cancel();
                currentStatement = null;
            } catch (SQLException ex) {
                inquirer.logger.error("exception cancel: ", ex);
            }
        }
    }

    static boolean TableExist(TableType tableType) throws SQLException {
        return TableExist(tableType.toString());
    }

    public static DatabaseConnector getDatabaseConnector(Object obj) throws SQLException {
        if (databaseConnector != null) {
            if (obj != null) {
                Statement stmt;
                if (!databaseConnector.m_freeStatements.isEmpty()) {
                    stmt = (Statement) databaseConnector.m_freeStatements.remove(0);
                } else {
                    stmt = databaseConnector.m_conn.createStatement();
                }
                databaseConnector.m_activeStatements.put(obj, stmt);
            }
        }
        return databaseConnector;
    }

    public static void initDatabaseConnector(String dbName, String dbAlias) throws SQLException {
        databaseConnector = new DatabaseConnector(dbName, dbAlias);
//        long time3 = new Date().getTime();
//        runQuery("analyze");
//        long time4 = new Date().getTime();
//        inquirer.logger.info("Analyze table took " + com.myutils.logbrowser.indexer.Main.pDuration(time4 - time3));
    }

    private static void Explain(Statement stmt, String query) throws SQLException {
        if (inquirer.logger.isTraceEnabled()) {

            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeInterruptException();
            }

            inquirer.logger.trace("---> explain " + query);
            ResultSet rs = stmt.executeQuery("EXPLAIN QUERY PLAN " + query);

            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                inquirer.logger.trace(metadata.getColumnName(i) + ", ");
            }
//        inquirer.logger.debug();
            while (rs.next()) {
                String row = "";
                for (int i = 1; i <= columnCount; i++) {
                    row += rs.getString(i) + ", ";
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeInterruptException();
                }
                inquirer.logger.trace(row);
            }
        }
    }

    public static boolean refTableExist(ReferenceType rt) throws SQLException {
        return TableExist(rt.toString());
    }

    public static boolean refTableExist(String rt) throws SQLException {
        return TableExist(rt);
    }

    public static boolean TableExist(String tab) throws SQLException {
        return TableExist(tab, false);
    }

    public static boolean fileExits(FileInfoType types) throws SQLException {

        return fileExits(new FileInfoType[]{types});
    }

    public static boolean fileExits(FileInfoType[] types) throws SQLException {
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = FileInfoType.getFileType(types[i]);

        }

//        Integer[] ids = getRefIDs(ReferenceType.AppType, names);
        Integer[] ids = getIDs("file_logbr", "id", getWhere("apptypeid", ReferenceType.AppType, names, true, false));
        return (ids != null && ids.length > 0);
    }

    public static void checkIndexes(String tabName, String[] fieldNames) throws SQLException {
        for (String n : fieldNames) {
            String req = " select count(*) from sqlite_master where type='index'\n"
                    //            String req = "select type from sqlite_master where type='index'\n"
                    + " and lower(tbl_name)='" + tabName.toLowerCase() + "' "
                    + "and REGEXP ('\\s*\\(\\s*" + n.toLowerCase() + "\\s*\\)\\s*$', lower(sql) )=1"
                    //                    + "and REGEXP( 'd', 'dd')=1"
                    + ";";
//            inquirer.logger.info(req);
            Integer[] ids;
            boolean b;
            ids = getIDs(req);
            b = (ids != null && ids.length > 0 && ids[0] > 0);
//                inquirer.logger.info(b);
            if (!b) {
                runQuery("create index idx_" + tabName + "_" + n + " on " + tabName + "(" + n + ");");
            }

        }

    }

    public static boolean TableExist(String tab, boolean forceCheck) throws SQLException {
        if (!forceCheck && tabExists.containsKey(tab)) {
            return tabExists.get(tab);
        } else {
            Integer[] ids = getIDs("select count(*) from sqlite_master WHERE "
                    + "type=\"table\" and lower(name)=\"" + tab.toLowerCase() + "\" ");
            boolean b = (ids.length > 0 && ids[0] == 1);
            tabExists.put(tab, b);
            return b;
        }
    }

    public static boolean TableEmpty(String tab) throws SQLException {
        if (TableExist(tab)) {
            Integer[] ids = getIDs("select count(*) from " + tab + ";");
            boolean b = (ids.length > 0 && ids[0] > 0);
            return !b;
        }
        return true;
    }

    public static boolean TableEmptySilent(String tab) {
        try {
            if (TableExist(tab)) {
                Integer[] ids = getIDs("select count(*) from " + tab + ";");
                boolean b = (ids.length > 0 && ids[0] > 0);
                return !b;
            }
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public static ResultSet executeQuery(String query) throws SQLException {
        long timeStart = new Date().getTime();
        ResultSet ret = null;

        try {
            currentStatement = databaseConnector.m_conn.createStatement();
            currentStatement.closeOnCompletion();
            inquirer.logger.debug("About to executeQuery [" + query + "]");

            Explain(currentStatement, query);

            ret = currentStatement.executeQuery(query);

        } catch (SQLException sQLException) {
            if (sQLException.getErrorCode() == SQLITE_INTERRUPT.code) {
                inquirer.logger.debug("Statement cancelled");
                Thread.currentThread().interrupt();
                throw new RuntimeInterruptException();
            } else {
                inquirer.ExceptionHandler.handleException("RunQuery failed: " + sQLException + " query " + query, sQLException);
                throw sQLException;
            }
        } finally {
            synchronized (currentStatement) {
                currentStatement = null;
            }
        }

        long timeEnd = new Date().getTime();
        inquirer.logger.debug("\tExecuteQuery execution took " + pDuration(timeEnd - timeStart));
        return ret;

    }

    public static int runQuery(String query) throws SQLException {
        long timeStart = new Date().getTime();
        currentStatement = databaseConnector.m_conn.createStatement();
        synchronized (currentStatement) {
            currentStatement.closeOnCompletion();
            Explain(currentStatement, query);
        }
        inquirer.logger.debug("about to runQuery [" + query + "]");
        int executeUpdate = 0;
        try {
            executeUpdate = currentStatement.executeUpdate(query);
        } catch (SQLException sQLException) {
            if (sQLException.getErrorCode() == SQLITE_INTERRUPT.code) {
                inquirer.logger.info("Statement cancelled");
                Thread.currentThread().interrupt();
                throw new RuntimeInterruptException();
            } else {
                inquirer.ExceptionHandler.handleException("RunQuery failed: " + sQLException + " query " + query, sQLException);
                throw sQLException;
            }
        } finally {
//            synchronized (currentStatement) {
            currentStatement = null;
//            }
        }
        long timeEnd = new Date().getTime();
        inquirer.logger.debug("Statement executed: affected " + executeUpdate + " rows; took " + pDuration(timeEnd - timeStart));
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeInterruptException();
        }
        checkLimitClause(executeUpdate);
        return executeUpdate;
//            databaseConnector.m_conn.commit();
    }

    public static boolean onlyOneApp() throws SQLException {
        if (noAppTypes < 0) {
            Integer[] iDs = getIDs("file_logbr", "apptypeid", "where apptypeid>0");
            noAppTypes = iDs.length;
        }
        return noAppTypes < 2;
    }

    public static String[] getNames(Object obj, String tab, String retField, String where) throws SQLException {
//        m_connector.getIDs(this, "select sid from orssess_logbr WHERE "+getWhere("connid", m_ConnIds));
        return (TableExist(tab))
                ? getNames(obj, "select distinct " + retField + " from " + tab + " " + where) : null;
    }

    public static ArrayList<NameID> getNamesNameID(Object obj, String tab, String retField, String where) throws SQLException {
//        m_connector.getIDs(this, "select sid from orssess_logbr WHERE "+getWhere("connid", m_ConnIds));
        return (TableExist(tab))
                ? getNamesNameID(obj, "select distinct " + retField + " from " + tab + " " + where) : null;
    }

    public static String[] getRefNames(ReferenceType refTab, Integer[] ids) throws SQLException {
        return (TableExist(refTab.toString()) && (ids != null && ids.length > 0))
                ? getNames("select distinct name from " + refTab.toString() + getWhere("id", ids, true))
                : null;
    }

    public static Integer[] translateIDs(ReferenceType fromRef, Integer[] fromIDs, ReferenceType toRef) throws SQLException {
        String[] refNames = getRefNames(fromRef, fromIDs);
        if (ArrayUtils.isNotEmpty(refNames)) {
            return getRefIDs(toRef, refNames);
        } else {
            return null;
        }
    }

    public static HashMap<Integer, String> getRefs(ReferenceType refTab) throws SQLException {
        return (TableExist(refTab.toString()))
                ? getRefs("select id, name from " + refTab.toString())
                : null;
    }

    private static HashMap<Integer, String> getRefs(String query) throws SQLException {
        HashMap<Integer, String> ret = new HashMap();

        try (ResultSet resultSet = DatabaseConnector.executeQuery(query)) {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            if (rsmd.getColumnCount() != 2) {
                throw new SQLException("getRefs: there are " + rsmd.getColumnCount() + " output columns; has to be only 2");
            }
            int cnt = 0;
            while (resultSet.next()) {
                Integer id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                if (name != null && name.length() > 0 && id != null) {
                    ret.put(id, name);
                    QueryTools.DebugRec(resultSet);
                    cnt++;
                }
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
//        resultSet.getStatement().close();
        }

        return ret;

    }

    public static String[] getNames(Object obj, String tab, String retField, String where, String orderBy) throws SQLException {
//        m_connector.getIDs(this, "select sid from orssess_logbr WHERE "+getWhere("connid", m_ConnIds));
        return (TableExist(tab))
                ? getNames(obj, "select distinct " + retField + " from " + tab + " " + where + " order by " + orderBy)
                : null;
    }

    static public Integer[] getIDs(String tab, String retField, String where) throws SQLException {
//        m_connector.getIDs(this, "select sid from orssess_logbr WHERE "+getWhere("connid", m_ConnIds));
        return (TableExist(tab))
                ? getIDs("select distinct " + retField + " from " + tab + " " + where)
                : null;
    }

    public static Integer[] getRefIDs(ReferenceType rt, String[] names) throws SQLException {
        if (refTableExist(rt)) {
            return getIDs(rt.toString(), "id", getWhere("name", names, true));
        }
        return new Integer[0];
    }

    public static Integer[] getRefIDs(ReferenceType rt, String[] names, boolean isRegex) throws SQLException {
        if (refTableExist(rt)) {
            return getIDs(rt.toString(), "id", getWhere("name", names, true));
        }
        return new Integer[0];
    }

    public static Integer[] getIDs(Object obj, String query) throws SQLException {
        ArrayList<Integer> ret = new ArrayList();
        DatabaseConnector connector = DatabaseConnector.getDatabaseConnector(obj);

        try (ResultSet resultSet = connector.executeQuery(obj, query)) {
            ResultSetMetaData rsmd = resultSet.getMetaData();

            if (rsmd.getColumnCount() != 1) {
                throw new SQLException("getIDs: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
            }
            int cnt = 0;
            while (resultSet.next()) {
                Integer res = resultSet.getInt(1);
                if (res > 0) {
                    ret.add(res);
                    QueryTools.DebugRec(resultSet);
                    cnt++;
                }
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
        }

        return (Integer[]) ret.toArray(new Integer[ret.size()]);
    }

    public static String[] getNames(String query) throws SQLException {
        ArrayList<String> ret = new ArrayList();

        try (ResultSet resultSet = executeQuery(query)) {
            ResultSetMetaData rsmd = resultSet.getMetaData();

            if (rsmd.getColumnCount() != 1) {
                throw new SQLException("getIDs: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
            }
            int cnt = 0;
            while (resultSet.next()) {
                String res = resultSet.getString(1);
                if (res != null && res.length() > 0) {
                    ret.add(res);
                    QueryTools.DebugRec(resultSet);
                    cnt++;
                }
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
//        resultSet.getStatement().close();
            resultSet.getStatement().close();
        }

        return (String[]) ret.toArray(new String[ret.size()]);

    }

    public static ArrayList<NameID> getNamesNameID(Object obj, String query) throws SQLException {
        ArrayList<NameID> ret = new ArrayList();
        DatabaseConnector connector = DatabaseConnector.getDatabaseConnector(obj);

        try (ResultSet resultSet = connector.executeQuery(obj, query)) {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            boolean noID = rsmd.getColumnCount() == 1;
            if (rsmd.getColumnCount() != 2 && !noID) {
                throw new SQLException("getNamesPair: there are " + rsmd.getColumnCount() + " output columns; has to be only 2");
            }
            if (!noID && (!textColumn(rsmd.getColumnType(2))
                    || !numColumn(rsmd.getColumnType(1)))) {
                throw new SQLException("getNamesPair: col[1] is " + rsmd.getColumnType(1) + " expected int;"
                        + "col[2] is " + rsmd.getColumnType(2) + " expected text;");
            }
            int cnt = 0;
            while (resultSet.next()) {
                String res;
                int aInt = 0;
                if (noID) {
                    res = resultSet.getString(1);
                } else {
                    res = resultSet.getString(2);
                    aInt = resultSet.getInt(1);
                }
                if (res != null && res.length() > 0) {
                    ret.add(new NameID(res, aInt));
                    QueryTools.DebugRec(resultSet);
                    cnt++;
                }
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
            //        resultSet.getStatement().close();
        }

        return ret;
    }

    public static String[] getNames(Object obj, String query) throws SQLException {
        ArrayList<String> ret = new ArrayList();
        DatabaseConnector connector = DatabaseConnector.getDatabaseConnector(obj);

        try (ResultSet resultSet = connector.executeQuery(obj, query)) {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            if (rsmd.getColumnCount() != 1) {
                throw new SQLException("getIDs: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
            }
            int cnt = 0;
            while (resultSet.next()) {
                String res = resultSet.getString(1);
                if (res != null && res.length() > 0) {
                    ret.add(res);
                    QueryTools.DebugRec(resultSet);
                    cnt++;
                }
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
            //        resultSet.getStatement().close();
        }

        return (String[]) ret.toArray(new String[ret.size()]);
    }

    public static UTCTimeRange getTimeRange(String[] tabs) throws SQLException {
        return getTimeRange(tabs, null);
    }

    static UTCTimeRange getTimeRange(FileInfoType[] tabs, ArrayList<Integer> searchApps) throws SQLException {
        UTCTimeRange ret = new UTCTimeRange(0, 0);
        StringBuilder q = new StringBuilder(256);
        Integer[] arrSearchApps;
        q.append("select min(filestarttime), max(endtime) from file_logbr ");

        if (searchApps != null && searchApps.size() > 0) {
            arrSearchApps = (Integer[]) searchApps.toArray(new Integer[searchApps.size()]);
            q.append(getWhere("id", arrSearchApps, true));
        } else {
            String[] n = new String[tabs.length];
            for (int i = 0; i < tabs.length; i++) {
                n[i] = FileInfoType.getFileType(tabs[i]);

            }
            q.append(getWhere("apptypeid", ReferenceType.AppType, n, true));
        }

        ArrayList<ArrayList<Long>> iDsMultiple = getIDsMultiple(q.toString());
        if (iDsMultiple.size() > 0) {
            ArrayList<Long> dbRet = iDsMultiple.get(0);
            Long val = dbRet.get(0);
            if (val != null && val > 0) {
                if (ret.getStart() == 0 || ret.getStart() > val) {
                    ret.setStart(val);
                }
            }

            val = dbRet.get(1);
            if (val != null && val > 0) {
                if (ret.getEnd() == 0 || ret.getEnd() < val) {
                    ret.setEnd(val);

                }
            }
        }
        return ret;

    }

    static UTCTimeRange getTimeRange(String[] tabs, ArrayList<Integer> searchApps) throws SQLException {
        UTCTimeRange ret = new UTCTimeRange(0, 0);
        StringBuilder q = new StringBuilder(256);
        Integer[] arrSearchApps = null;
        if (searchApps != null && searchApps.size() > 0) {
            arrSearchApps = (Integer[]) searchApps.toArray(new Integer[searchApps.size()]);
        }

        for (String tab : tabs) {

            if (TableExist(tab)) {
                q.setLength(0);
                q.append("select min(time), max(time) from ").append(tab);
                if (arrSearchApps != null) {
                    q.append(" where fileid in (select id from file_logbr ")
                            .append(getWhere("appnameid", arrSearchApps, true))
                            .append(")");
                }

                ArrayList<ArrayList<Long>> iDsMultiple = getIDsMultiple(q.toString());
                if (iDsMultiple.size() > 0) {
                    ArrayList<Long> dbRet = iDsMultiple.get(0);
                    Long val = dbRet.get(0);
                    if (val != null && val > 0) {
                        if (ret.getStart() == 0 || ret.getStart() > val) {
                            ret.setStart(val);
                        }
                    }

                    val = dbRet.get(1);
                    if (val != null && val > 0) {
                        if (ret.getEnd() == 0 || ret.getEnd() < val) {
                            ret.setEnd(val);

                        }
                    }
                }

            }

        }
        if (ret.getStart() == 0 && ret.getEnd() == 0) {
            return null;
        }
        return ret;
    }

    public static UTCTimeRange getTimeRange(String tab) throws SQLException {
        return getTimeRange(new String[]{tab});
    }

    public static ArrayList<ArrayList<Long>> getIDsMultiple(String query) throws SQLException {

        ArrayList<ArrayList<Long>> ret;
        try (ResultSet rs = executeQuery(query)) {
            ResultSetMetaData rsmd = rs.getMetaData();
            ret = new ArrayList<>();
            int columnCount = rsmd.getColumnCount();
            int cnt = 0;
            while (rs.next()) {
                ArrayList<Long> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(rs.getLong(i));
                }
                ret.add(row);
                QueryTools.DebugRec(rs);
                cnt++;
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
            rs.getStatement().close();
        }

        return ret;

    }

    public static FullTableColors getFullTableColors(String query) throws SQLException {
        return getFullTableColors(query, null);
    }

    static FullTableColors getFullTableColors(String query, String filestarttime) throws SQLException {

        FullTableColors ret;

        try (ResultSet rs = executeQuery(query)) {
            ResultSetMetaData rsmd = rs.getMetaData();

            ret = new FullTableColors();
            ret.setMetaData(rsmd);
            if (filestarttime != null) {
                ret.setFieldColorChange(filestarttime);
            }
            int columnCount = rsmd.getColumnCount();

            int cnt = 0;
            while (rs.next()) {
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

    public static FullTable getFullTable(String query) throws SQLException {

        FullTable ret;

        try (ResultSet rs = executeQuery(query)) {
            ResultSetMetaData rsmd = rs.getMetaData();

            ret = new FullTable();
            ret.setMetaData(rsmd);

            int columnCount = rsmd.getColumnCount();

            int cnt = 0;
            while (rs.next()) {
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

    public static String getValue(String exp, String p) throws SQLException {

        String s = exp.replaceAll("\\$1\\b", "'" + p + "'");

        Statement createStatement = databaseConnector.m_conn.createStatement();

        ResultSet rs = createStatement.executeQuery(s);

//        ResultSetMetaData rsmd = rs.getMetaData();
//        if (rsmd.getColumnCount() != 1) {
//            throw new Exception("getValue: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
//        }
        while (rs.next()) {
            return rs.getString(1);
        }
        inquirer.logger.info("\tgetValue " + " nothing returned");
        return null;
    }

    public static Integer[] getIDs(String query) throws SQLException {
        ArrayList<Integer> ret = new ArrayList();

        Integer[] ids;
        try (ResultSet rs = executeQuery(query)) {
            ResultSetMetaData rsmd = rs.getMetaData();
            if (rsmd.getColumnCount() != 1) {
                throw new SQLException("getIDs: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
            }
            int cnt = 0;
            while (rs.next()) {
                Integer res = rs.getInt(1);
                if (res != null && res > 0) {
                    ret.add(res);
                    cnt++;
                }
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
            ids = (Integer[]) ret.toArray(new Integer[ret.size()]);
            QueryTools.DebugIDs(ids);
            rs.getStatement().close();
        }
        return ids;
    }

    public static void GracefulClose() {
        try {
            if (databaseConnector != null) {
                for (Object m_activeStatement : databaseConnector.m_activeStatements.values()) {
                    ((Statement) m_activeStatement).close();
                }
                databaseConnector.m_activeStatements.clear();

                for (int i = 0; i < databaseConnector.m_freeStatements.size(); i++) {
                    Statement stmt = (Statement) databaseConnector.m_freeStatements.get(i);
                    stmt.close();
                }
                databaseConnector.m_freeStatements.clear();

                if (databaseConnector.m_conn != null) {
                    databaseConnector.m_conn.close();
                }
            }
        } catch (SQLException sQLException) {
            inquirer.logger.error("Exception closing", sQLException);
        }
    }

    static String[] getRefNames(Object obj, String tab) throws SQLException {
        return getRefNames(obj, tab, null, null);
    }

    static String[] getRefNames(Object obj, String tab, String CheckTab, String CheckIDField) throws SQLException {
        return getRefNames(obj, tab, CheckTab, CheckIDField, null);
    }

    static String[] getRefNames(Object obj, String tab, String CheckTab, String CheckIDField, String CheckIDField1) throws SQLException {

        if (!TableExist(tab)) {
            return null;
        }
        ArrayList<String> ret = new ArrayList();
        DatabaseConnector connector = DatabaseConnector.getDatabaseConnector(obj);

        StringBuilder q = new StringBuilder(256);
        q.append("select name from ").append(tab).append("");//remove 'distinct' from here. Uniquness of names is ensured by parser,
        //so no need to sort again
        if (CheckTab != null && CheckIDField != null) {
            q.append(" where id in (select distinct ").append(CheckIDField).append(" from ").append(CheckTab);
            if (CheckIDField1 != null) {
                q.append("\nunion\nselect distinct ").append(CheckIDField1).append(" from ").append(CheckTab);
            }

            q.append("\n)");
        }
        q.append("\norder by name");
        try (ResultSet resultSet = connector.executeQuery(obj, q.toString())) {
            ResultSetMetaData rsmd = resultSet.getMetaData();

            if (rsmd.getColumnCount() != 1) {
                throw new SQLException("getRefNames: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
            }
            int cnt = 0;
            while (resultSet.next()) {
                String res = resultSet.getString(1);
                if (res != null && res.length() > 0) {
                    ret.add(res);
                    QueryTools.DebugRec(resultSet);
                    cnt++;
                }
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
        }

        return (String[]) ret.toArray(new String[ret.size()]);
    }

    static ArrayList<NameID> getRefNamesNameID(Object obj, String tab, String CheckTab, String CheckIDField, String CheckIDField1) throws SQLException {

        if (!TableExist(tab) || (CheckTab != null && !TableExist(CheckTab))) {
            return null;
        }
        ArrayList<NameID> ret = new ArrayList();
        DatabaseConnector connector = DatabaseConnector.getDatabaseConnector(obj);

        StringBuilder q = new StringBuilder(256);
        q.append("select id, name from ").append(tab).append("");//remove 'distinct' from here. Uniquness of names is ensured by parser,
        //so no need to sort again
        if (CheckTab != null && CheckIDField != null) {
            q.append(" where id in (select distinct ").append(CheckIDField).append(" from ").append(CheckTab);
            if (CheckIDField1 != null) {
                q.append("\nunion\nselect distinct ").append(CheckIDField1).append(" from ").append(CheckTab);
            }

            q.append("\n)");
        }
        q.append("\norder by name");
        try (ResultSet resultSet = connector.executeQuery(obj, q.toString())) {
            if (resultSet == null) {
                return null;
            }
            ResultSetMetaData rsmd = resultSet.getMetaData();

            if (rsmd.getColumnCount() != 2) {
                throw new SQLException("getRefNames: there are " + rsmd.getColumnCount() + " output columns; has to be only 2");
            }
            if (!textColumn(rsmd.getColumnType(2))
                    || !numColumn(rsmd.getColumnType(1))) {
                throw new SQLException("getNamesPair: col[1] is " + rsmd.getColumnType(1) + " expected int;"
                        + "col[2] is " + rsmd.getColumnType(2) + " expected text;");
            }

            int cnt = 0;
            while (resultSet.next()) {
                String res = resultSet.getString(2);
                int aInt = resultSet.getInt(1);
                if (res != null && res.length() > 0) {
                    ret.add(new NameID(res, aInt));
                    QueryTools.DebugRec(resultSet);
                    cnt++;
                }
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
        }

        return ret;
    }

    static ArrayList<NameID> getRefNameIDs(Object obj, String tab, String subQuery) throws SQLException {
        return getRefNameIDs(obj, tab, subQuery, false);
    }

    static ArrayList<NameID> getRefNameIDs(Object obj, String tab, String subQuery, boolean addOrderBy) throws SQLException {
        if (!TableExist(tab)) {
            return null;
        }
        ArrayList<NameID> ret = new ArrayList();
        StringBuilder query = new StringBuilder("select id, name from ");
        query.append(tab);
        if (subQuery != null) {
            query.append(" where ( ").append(subQuery).append(" )");
        }
        if (addOrderBy) {
            query.append(" order by name");
        }
        try (ResultSet resultSet = DatabaseConnector.executeQuery(query.toString())) {
            ResultSetMetaData rsmd = resultSet.getMetaData();

            if (rsmd.getColumnCount() != 2) {
                throw new SQLException("getRefNameIDs: there are " + rsmd.getColumnCount() + " output columns; has to be only 2");
            }
            int cnt = 0;
            while (resultSet.next()) {
                NameID res = new NameID(resultSet.getString(2), resultSet.getInt(1));
                ret.add(res);
                QueryTools.DebugRec(resultSet);
                cnt++;
            }
            inquirer.logger.debug("\tRetrieved " + cnt + " records");
        }

        return ret;
    }
    private String m_dbName;
    private String m_dbAlias;
    private Connection m_conn;
    HashMap m_activeStatements;
    ArrayList m_freeStatements;
    ArrayList<PreparedStatement> m_statements;
    private HashMap<ReferenceType, HashMap<Integer, String>> thePersistentRef = new HashMap<>();
    private IDsToName idsToName;

    protected DatabaseConnector(String dbName, String dbAlias) throws SQLException {
        m_dbName = dbName;
        m_dbAlias = dbAlias;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
            throw new SQLException(ex);
        }
        String name = dbName + ".db";
        File f = new File(name);
        Path p = f.toPath();
        if (!Files.isRegularFile(p)) {
//            Main.logger.info("DB [" + name + "] does exists; terminating");
            throw new SQLException("No DB [" + p.toString() + "]");
        }

        idsToName = new IDsToName();

        m_conn = DriverManager.getConnection("jdbc:sqlite:" + name);
        Function.create(m_conn, "REGEXP", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                String regex = value_text(0);
                String compareString = value_text(1);
                if (compareString == null) {
                    compareString = "";
                }

                Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
//                boolean name = pattern.matcher(compareString).find();
//                inquirer.logger.info("REGEXP: reg [" + regex.toString() + "] comp: [" + compareString + "]; result: " + name);
                result(pattern.matcher(compareString).find() ? 1 : 0);
            }
        });

        Function.create(m_conn, "filesize", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    String sizeText = value_text(0);
//                    inquirer.logger.info("sizeText: [" + value_text(0) + "]");
                    Long sizeL = Long.parseLong(sizeText);
//                    inquirer.logger.info("parsed: [" + sizeL + "]");
                    result(Main.formatSize(sizeL));
                } catch (IllegalFormatException | NumberFormatException e) {
                    result("");
//            inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
                    inquirer.logger.error("sizeText: [" + value_text(0) + "]", e);
                }
            }

        });

        Function.create(m_conn, "connectedName", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    String sizeText = value_text(0);
//                    inquirer.logger.info("sizeText: [" + value_text(0) + "]");
                    Integer val = Integer.parseInt(sizeText);
//                    inquirer.logger.info("parsed: [" + sizeL + "]");
                    if (val != null && val.intValue() == 1) {
                        result("connected");
                    } else {
                        result("disconnected");
                    }
                } catch (IllegalFormatException | NumberFormatException e) {
                    result("");
//            inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
                    inquirer.logger.error("sizeText: [" + value_text(0) + "]", e);
                }
            }

        });

        Function.create(m_conn, "UTCtoDateTime", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                String fieldValue = value_text(0);
                String dtFormat = value_text(1);
//                    inquirer.logger.info("sizeText: [" + value_text(0) + "]");
//                    inquirer.logger.info("parsed: [" + sizeL + "]");
                result(inquirer.UTCtoDateTime(fieldValue, dtFormat));
            }

        });

        Function.create(m_conn, "jduration", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    String sizeText = value_text(0);
                    if (sizeText != null && !sizeText.isEmpty()) {
//                    inquirer.logger.info("sizeText: [" + value_text(0) + "]");
                        Long sizeL = Long.parseLong(sizeText);
//                    inquirer.logger.info("parsed: [" + sizeL + "]");
                        result(Utils.Util.pDuration(sizeL));
                    } else {
                        result("");
                    }
                } catch (IllegalFormatException | NumberFormatException e) {
                    result("0");
                    inquirer.logger.error("sizeText: [" + value_text(0) + "]", e);
                }
            }

        });

        Function.create(m_conn, "constToStr", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    if (args() != 2 || value_text(1) == null) {
                        result((String) null);
                        return;
                    }

                    String constName = value_text(0);
                    int theConst = value_int(1);

//                    inquirer.logger.info("constToStr: constName[" + constName + "] theConst[" + theConst + "] theConst[" + value_text(1) + "]");
                    String ret = null;
                    if (constName != null && !constName.isEmpty()) {
                        String val = inquirer.getCr().lookupConst(constName.trim(), theConst);
                        String key = (val == null) ? constName.trim() : val;

                        ret = key + "(" + theConst + ")";
//                        inquirer.logger.info("ret=" + ret);
                    }
                    if (ret == null) {
                        result((String) null);
                    }
                    if (ret != null && !ret.isEmpty()) {
                        result((String) ret);
                    } else {
                        result(theConst);
                    }
                } catch (IllegalFormatException | NumberFormatException e) {
                    result("");
                    inquirer.logger.error("sizeText: [" + value_text(0) + "]", e);
                }
            }

        });

//<editor-fold defaultstate="collapsed" desc="CampaignGroupDBIDtoName">
        idsToName.addFun(CampaignGroupDBIDtoName, new ILoadFun() {
            @Override
            public String[] getNames(String id) throws SQLException {
                int cgDBID = Integer.parseInt(id);
                if (cgDBID > 0 && (DatabaseConnector.TableExist("ocscg_logbr"))) {
                    Integer[] iDs = getIDs(
                            "select distinct cgnameid "
                            + "from ocscg_logbr "
                            + getWhere("cgDBID",
                                    new Integer[]{cgDBID}, true));

                    String[] refNames = getRefNames(ReferenceType.OCSCG, iDs);
                    if (refNames != null && refNames.length > 0) {
                        return refNames;
                    }
                }
                return null;
            }
        });

        Function.create(m_conn, CampaignGroupDBIDtoName, new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    if (args() != 1 || value_text(0) == null) {
                        result((String) null);
                        return;
                    }
                    String names = idsToName.getNames(CampaignGroupDBIDtoName, value_text(0));
                    if (names != null) {
                        result(names);
                    } else {
                        result((String) null);
                    }

                } catch (SQLException e) {
                    result(value_text(0));
                    inquirer.logger.error(CampaignGroupDBIDtoName + ": [" + value_text(0) + "]", e);
                    throw new SQLException(e);
                }
            }

        });
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="CampaignDBIDtoName">
        idsToName.addFun(CampaignDBIDtoName, new ILoadFun() {
            @Override
            public String[] getNames(String id) throws SQLException {
                int cgDBID = Integer.parseInt(id);
                if (cgDBID > 0 && (DatabaseConnector.TableExist("ocscg_logbr"))) {
                    Integer[] iDs = getIDs(
                            "select distinct campaignNameID "
                            + "from ocscg_logbr "
                            + getWhere("CampaignDBID",
                                    new Integer[]{cgDBID}, true));

                    String[] refNames = getRefNames(ReferenceType.OCSCAMPAIGN, iDs);
                    if (refNames != null && refNames.length > 0) {
                        inquirer.logger.debug(CampaignDBIDtoName + ": return [" + QueryTools.getIDString(refNames) + "]");
                        return refNames;
                    }
                }
                return null;
            }
        });
        Function.create(m_conn, CampaignDBIDtoName, new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    if (args() != 1 || value_text(0) == null) {
                        result((String) null);
                        return;
                    }
                    String names = idsToName.getNames(CampaignDBIDtoName, value_text(0));
                    if (names != null) {
                        result(names);
                    } else {
                        result((String) null);
                    }

                } catch (SQLException e) {
                    result(value_text(0));
                    inquirer.logger.error(CampaignDBIDtoName + ": [" + value_text(0) + "]", e);
                    throw new SQLException(e);
                }
            }

        });
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="GroupDBIDtoName">
        idsToName.addFun(GroupDBIDtoName, new ILoadFun() {
            @Override
            public String[] getNames(String id) throws SQLException {
                int cgDBID = Integer.parseInt(id);
                if (cgDBID > 0 && (DatabaseConnector.TableExist("ocscg_logbr"))) {
                    Integer[] iDs = getIDs(
                            "select distinct groupNameID "
                            + "from ocscg_logbr "
                            + getWhere("GroupDBID",
                                    new Integer[]{cgDBID}, true));

                    String[] refNames = getRefNames(ReferenceType.AgentGroup, iDs);
                    if (refNames != null && refNames.length > 0) {
                        return refNames;
                    }
                }
                return null;
            }
        });
        Function.create(m_conn, GroupDBIDtoName, new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    if (args() != 1 || value_text(0) == null) {
                        result((String) null);
                        return;
                    }
                    String names = idsToName.getNames(GroupDBIDtoName, value_text(0));
                    if (names != null) {
                        result(names);
                    } else {
                        result((String) null);
                    }

                } catch (SQLException e) {
                    result(value_text(0));
                    inquirer.logger.error(GroupDBIDtoName + ": [" + value_text(0) + "]", e);
                    throw new SQLException(e);
                }
            }

        });
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="ListDBIDtoName">
        idsToName.addFun(ListDBIDtoName, new ILoadFun() {
            @Override
            public String[] getNames(String id) throws SQLException {
                int cgDBID = Integer.parseInt(id);
                if (cgDBID > 0 && (DatabaseConnector.TableExist("ocsdb_logbr"))) {
                    Integer[] iDs = getIDs(
                            "select distinct listNameID "
                            + "from ocsdb_logbr "
                            + getWhere("listDBID",
                                    new Integer[]{cgDBID}, true));

                    String[] refNames = getRefNames(ReferenceType.OCSCallList, iDs);
                    if (refNames != null && refNames.length > 0) {
                        return refNames;
                    }
                }
                return null;
            }
        });
        Function.create(m_conn, ListDBIDtoName, new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    if (args() != 1 || value_text(0) == null) {
                        result((String) null);
                        return;
                    }
                    String names = idsToName.getNames(ListDBIDtoName, value_text(0));
                    if (names != null) {
                        result(names);
                    } else {
                        result((String) null);
                    }

                } catch (SQLException e) {
                    result(value_text(0));
                    inquirer.logger.error(ListDBIDtoName + ": [" + value_text(0) + "]", e);
                    throw new SQLException(e);
                }
            }

        });
//</editor-fold>

        Function.create(m_conn, "REGEXP_group", new Function() {

            @Override
            protected void xFunc() throws SQLException {
                String regex = value_text(0);
                String compareString = value_text(1);
                Integer grpIdx;
                String grpIdxS;
                if (compareString == null) {
                    compareString = "";
                }
                try {
                    grpIdxS = value_text(2);
                    grpIdx = Integer.parseInt(grpIdxS);
                    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(compareString);
//                    inquirer.logger.debug("regexp: [" + regex + "] compareString[" + compareString + "]+");
                    if (matcher.find()) {
//                        inquirer.logger.debug("ret: [" + matcher.group(grpIdx) + "]");
                        result(matcher.group(grpIdx));
                    } else {
                        result("");
                    }
                } catch (NumberFormatException e) {
                    result("");
                }

            }
        });

        Function.create(m_conn, "timediff", new Function() {

            @Override
            protected void xFunc() throws SQLException {
                String diff = value_text(0);
                if (diff == null) {
                    diff = "";
                }
                try {
                    long val = Long.parseLong(diff);
                    result(pDuration(val));
                } catch (NumberFormatException e) {
                    result("");
                }
            }
        });

        Function.create(m_conn, "intToHex", new Function() {

            @Override
            protected void xFunc() throws SQLException {
                long val = value_long(0);

                if (val <= 0) {
                    result("");
                } else {
                    result(Long.toHexString(val));
                }

            }
        });

        m_activeStatements = new HashMap();
        m_freeStatements = new ArrayList();
        m_statements = new ArrayList<>();
    }

    public String getAlias() {
        return m_dbAlias;
    }

    String persistentRef(ReferenceType referenceType, Integer i) throws SQLException {
        HashMap<Integer, String> theRef = thePersistentRef.get(referenceType);
        if (theRef == null) {
            theRef = getRefs(referenceType);
            thePersistentRef.put(referenceType, theRef);
        }
        return theRef.get(i);
    }

    public void releaseConnector(Object owner) {
        if (m_activeStatements.containsKey(owner)) {
            Statement stmt = (Statement) m_activeStatements.get(owner);
            m_activeStatements.remove(owner);
            m_freeStatements.add(stmt);
        }
        DatabaseConnector.currentStatement = null;
    }

    public ResultSet executeQuery(Object obj, String query) throws SQLException {
        ResultSet ret;
        if (query != null && !query.trim().isEmpty()) {
            long timeStart = new Date().getTime();

            inquirer.logger.debug("[" + query + "]");

            try {
                Statement stat = m_conn.createStatement();
                currentStatement = stat;
                stat.closeOnCompletion();
                Explain(stat, query);
                ret = stat.executeQuery(query);
            } catch (SQLException sQLException) {
                if (sQLException.getErrorCode() == SQLITE_INTERRUPT.code) {
                    inquirer.logger.info("Statement cancelled");
                    Thread.currentThread().interrupt();
                    return null;
//                    throw new RuntimeInterruptException();
                } else {
                    inquirer.ExceptionHandler.handleException("RunQuery failed: " + sQLException + " query " + query, sQLException);
                    throw sQLException;
                }
            }
//        finally {
//            synchronized (currentStatement) {
//                currentStatement = null;
//            }
//        }

            long timeEnd = new Date().getTime();
            inquirer.logger.debug("\t Execution took " + pDuration(timeEnd - timeStart));
            return ret;
        } else {
            return null;
        }

    }

    public Integer[] getIDs(Object obj, String tab, String retField, String where) throws SQLException {
//        m_connector.getIDs(this, "select sid from orssess_logbr WHERE "+getWhere("connid", m_ConnIds));
        return (TableExist(tab))
                ? getIDs(obj, "select distinct " + retField + " from " + tab + " " + where) : null;
    }

    public Integer[] getIDSubquery(Object obj, String tab, String retField, String Subquery) throws SQLException {
        return (TableExist(tab))
                ? getIDs(obj, "select distinct " + retField + " from " + tab + " where " + Subquery) : null;
    }

    public int PrepareStatement(String query) {
        try {
            PreparedStatement stmt = m_conn.prepareStatement(query);
            m_statements.add(stmt);
            int statementID = m_statements.size() - 1;
            return statementID;
        } catch (SQLException e) {
            inquirer.ExceptionHandler.handleException("Error while registering record: " + e + " query: " + query, e);
        }
        return -1;
    }

    public PreparedStatement GetStatement(int statementId) {
        return m_statements.get(statementId);
    }

    public ResultSet getRecords(int statementId) {
        PreparedStatement statement = m_statements.get(statementId);
        try {
            ResultSet rs = statement.executeQuery();
            return rs;
        } catch (SQLException e) {
            inquirer.ExceptionHandler.handleException("SubmitStatement failed: ", e);
        }
        return null;
    }

    abstract class ILoadFun {

        public abstract String[] getNames(String id) throws SQLException;
    }

    class IDsToName {

        private final HashMap<String, IDToName> idsToName;

        public IDsToName() {
            this.idsToName = new HashMap<>();
        }

        public void addFun(String funName, ILoadFun fun) throws SQLException {
            idsToName.put(funName, new IDToName(fun));
        }

        public String getNames(String fName, String id) throws SQLException {
            if (fName != null && id != null) {
                IDToName idn = idsToName.get(fName);
                if (idn != null) {
                    String[] refNames = idn.getNames(id);
                    if (refNames != null && refNames.length > 0) {
                        StringBuilder res = new StringBuilder(120);
                        for (String refName : refNames) {
                            if (res.length() > 0) {
                                res.append(",");
                            }
                            res.append(refName);
                        }
                        res.append("(").append(id).append(")");
                        return (res.toString());
                    }

                }
                inquirer.logger.error("Const not found. fn:[" + fName + "} id:[" + id + "]");
            }
            return id;

        }

        class IDToName {

            private final ILoadFun fun;
            private final HashMap<String, String[]> idToName;

            public IDToName(ILoadFun fun) {
                this.fun = fun;
                idToName = new HashMap<>();
            }

            public String[] getNames(String id) throws SQLException {
                if (idToName.containsKey(id)) {
                    return idToName.get(id);
                } else {
                    String[] names = fun.getNames(id);
                    if (names != null && names.length == 0) {
                        names = null;
                    }
                    idToName.put(id, names);
                    return names;
                }
            }

        }
    }

}
