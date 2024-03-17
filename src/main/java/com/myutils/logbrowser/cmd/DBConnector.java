package com.myutils.logbrowser.cmd;

import Utils.UTCTimeRange;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.Main;
import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.indexer.TableType;
import com.myutils.logbrowser.inquirer.*;
import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.sqlite.Function;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Utils.Util.pDuration;
import static Utils.Util.sortedArray;
import static com.myutils.logbrowser.inquirer.QueryTools.getWhere;
import static org.sqlite.SQLiteErrorCode.SQLITE_INTERRUPT;

class DBConnector {

    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private final HashMap<String, DateTimeFormatter> parsedFormats = new HashMap<>();
    private final HashMap<String, Boolean> tabExists = new HashMap<>();
    public Statement currentStatement = null;
    private DBConnector DBConnector = null;
    private int noAppTypes = -1;
    private final Connection m_conn;
    private final Map<ReferenceType, Map<Integer, String>> thePersistentRef = new HashMap<>();
    private final IDsToName idsToName;
    HashMap<Object, Statement> m_activeStatements;
    ArrayList<Statement> m_freeStatements;
    ArrayList<PreparedStatement> m_statements;

    protected DBConnector(String dbName) throws SQLException {

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            logger.error("fatal: ", ex);
            throw new SQLException(ex);
        }
        String name = dbName ;
        File f = new File(name);
        Path p = f.toPath();
        if (!Files.isRegularFile(p)) {
//            Main.logger.info("DB [" + name + "] does exists; terminating");
            throw new SQLException("No DB [" + p + "]");
        }

        idsToName = new IDsToName();

        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + name);
        m_conn = ds.getConnection();


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
                    long sizeL = Long.parseLong(sizeText);
//                    inquirer.logger.info("parsed: [" + sizeL + "]");
                    result(Main.formatSize(sizeL));
                } catch (IllegalFormatException | NumberFormatException e) {
                    result("");
//            inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
                    logger.error("sizeText: [" + value_text(0) + "]", e);
                }
            }

        });

        Function.create(m_conn, "connectedName", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    String sizeText = value_text(0);
//                    inquirer.logger.info("sizeText: [" + value_text(0) + "]");
                    int val = Integer.parseInt(sizeText);
//                    inquirer.logger.info("parsed: [" + sizeL + "]");
                    if (val == 1) {
                        result("connected");
                    } else {
                        result("disconnected");
                    }
                } catch (IllegalFormatException | NumberFormatException e) {
                    result("");
//            inquirer.ExceptionHandler.handleException(this.getClass().toString(), e);
                    logger.error("sizeText: [" + value_text(0) + "]", e);
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
                result(com.myutils.logbrowser.inquirer.inquirer.UTCtoDateTime(fieldValue, dtFormat));
            }

        });

        Function.create(m_conn, "jduration", new Function() {
            @Override
            protected void xFunc() throws SQLException {
                try {
                    String sizeText = value_text(0);
                    if (sizeText != null && !sizeText.isEmpty()) {
//                    inquirer.logger.info("sizeText: [" + value_text(0) + "]");
                        long sizeL = Long.parseLong(sizeText);
//                    inquirer.logger.info("parsed: [" + sizeL + "]");
                        result(pDuration(sizeL));
                    } else {
                        result("");
                    }
                } catch (IllegalFormatException | NumberFormatException e) {
                    result("0");
                    logger.error("sizeText: [" + value_text(0) + "]", e);
                }
            }

        });

        Function.create(m_conn, "REGEXP_group", new Function() {

            @Override
            protected void xFunc() throws SQLException {
                String regex = value_text(0);
                String compareString = value_text(1);
                int grpIdx;
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

        m_activeStatements = new HashMap<>();
        m_freeStatements = new ArrayList<>();
        m_statements = new ArrayList<>();
    }

    void dropTable(String tab) throws SQLException {
//        if (TableExist(tab, true)) {
//            executeQuery("drop table " + tab + ";");
//        }
        runQuery("drop table if exists " + tab + ";");

    }

    public long convertToTime(Object val) {
        if (val instanceof Long) {
            return (long) val;
        } else {
            return stringToDate(val.toString());
        }
    }

    public String dateToString(long time) {
        return dateToString(new Date(time));
    }

    public String dateToString(Date dateTime) {
        return (new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS")).format(dateTime);
    }


    private void checkLimitClause(int recsUpdated) {
//        if (recsUpdated > 1) {
//            if (inquirer.getCr().isLimitQueryResults()) {
//                if(recsUpdated>=getMaxQueryLines())
//                     JOptionPane.showMessageDialog(null, "Affected "+recsUpdated+" records;\nlikely more available.\n Increase limit if needed", "More records available", 0);
//            }
//        };

    }

    private long parseTime(String dateString, String dateFormat) {

        DateTimeFormatter fmt = parsedFormats.get(dateFormat);
        if (fmt == null) {
            fmt = DateTimeFormatter.ofPattern(dateFormat);
            parsedFormats.put(dateFormat, fmt);
        }

        try {
            LocalDateTime localDate = LocalDateTime.parse(dateString, fmt);
            return localDate.atZone(UTCTimeRange.zoneId).toInstant().toEpochMilli();
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
    public long stringToDate(Object valueAt) {
        if (valueAt != null) {
            String v = valueAt.toString();
            return parseTime(v, "yyyy-MM-dd HH:mm:ss.SSS");
        }
        return 0;
    }


    private boolean textColumn(int columnType) {
        return columnType == Types.CHAR
                || columnType == Types.NVARCHAR;
    }

    private boolean numColumn(int columnType) {
        return columnType == Types.BIGINT
                || columnType == Types.INTEGER;
    }

    public void cancelCurrent() {
        if (currentStatement != null) {
            try {
                currentStatement.close();
                currentStatement.cancel();
                currentStatement = null;
            } catch (SQLException ex) {
                logger.error("exception cancel: ", ex);
            }
        }
    }

    boolean TableExist(TableType tableType) throws SQLException {
        return TableExist(tableType.toString());
    }

    public DBConnector getDatabaseConnector(Object obj) throws SQLException {
        if (DBConnector != null && (obj != null)) {
            Statement stmt;
            if (!DBConnector.m_freeStatements.isEmpty()) {
                stmt = DBConnector.m_freeStatements.remove(0);
            } else {
                stmt = DBConnector.m_conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            }
            DBConnector.m_activeStatements.put(obj, stmt);

        }
        return DBConnector;
    }

    private void Explain(Statement stmt, String query) throws SQLException {
        if (logger.isTraceEnabled()) {

            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeInterruptException();
            }

            logger.trace("---> explain " + query);
            ResultSet rs = stmt.executeQuery("EXPLAIN QUERY PLAN " + query);

            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                logger.trace(metadata.getColumnName(i) + ", ");
            }
//        inquirer.logger.debug();
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    row.append(rs.getString(i)).append(", ");
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeInterruptException();
                }
                logger.trace(row);
            }
        }
    }

    public boolean refTableExist(ReferenceType rt) throws SQLException {
        return TableExist(rt.toString());
    }


    public boolean TableExist(String tab) throws SQLException {
        return TableExist(tab, false);
    }

    public boolean fileExits(FileInfoType types) throws SQLException {

        return fileExits(new FileInfoType[]{types});
    }

    public boolean fileExits(FileInfoType[] types) throws SQLException {
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = FileInfoType.getFileType(types[i]);

        }

//        Integer[] ids = getRefIDs(ReferenceType.AppType, names);
        Integer[] ids = getIDs("file_logbr", "id", getWhere("apptypeid", ReferenceType.AppType, names, true, false));
        return (ids != null && ids.length > 0);
    }

    public boolean TableExist(String tab, boolean forceCheck) throws SQLException {
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

    public boolean TableEmpty(String tab) throws SQLException {
        if (TableExist(tab)) {
            Integer[] ids = getIDs("select count(*) from " + tab + ";");
            boolean b = (ids.length > 0 && ids[0] > 0);
            return !b;
        }
        return true;
    }

    public boolean TableEmptySilent(String tab) {
        try {
            if (TableExist(tab)) {
                Integer[] ids = getIDs("select count(*) from " + tab + ";");
                boolean b = (ids.length > 0 && ids[0] > 0);
                return !b;
            }
            return true;
        } catch (SQLException ex) {
            Main.logger.error("", ex);
        }
        return false;
    }

    public int runQuery(String query) throws SQLException {
        long timeStart = new Date().getTime();
        currentStatement = DBConnector.m_conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        synchronized (currentStatement) {
            currentStatement.closeOnCompletion();
            Explain(currentStatement, query);
        }
        logger.debug("about to runQuery [" + query + "]");
        int executeUpdate = 0;
        try {
            executeUpdate = currentStatement.executeUpdate(query);
        } catch (SQLException sQLException) {
            if (sQLException.getErrorCode() == SQLITE_INTERRUPT.code) {
                logger.info("Statement cancelled");
                Thread.currentThread().interrupt();
                throw new RuntimeInterruptException();
            } else {
                throw sQLException;
            }
        } finally {
//            synchronized (currentStatement) {
            currentStatement = null;
//            }
        }
        long timeEnd = new Date().getTime();
        logger.debug("Statement executed: affected " + executeUpdate + " rows; took " + pDuration(timeEnd - timeStart));
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeInterruptException();
        }
        checkLimitClause(executeUpdate);
        return executeUpdate;
//            databaseConnector.m_conn.commit();
    }

    public String[] getNames(String tab, String retField, String where) throws SQLException {
//        m_connector.getIDs(this, "select sid from orssess_logbr WHERE "+getWhere("connid", m_ConnIds));
        return (TableExist(tab))
                ? getNames("select distinct " + retField + " from " + tab + " " + where) : null;
    }


    public String[] getRefNames(ReferenceType refTab, Integer[] ids) throws SQLException {
        return (TableExist(refTab.toString()) && (ids != null && ids.length > 0))
                ? getNames("select distinct name from " + refTab + getWhere("id", ids, true))
                : null;
    }


    public Map<Integer, String> getRefs(ReferenceType refTab) throws SQLException {
        return (TableExist(refTab.toString()))
                ? getRefs("select id, name from " + refTab)
                : null;
    }

    private HashMap<Integer, String> getRefs(String query) throws SQLException {
        HashMap<Integer, String> ret = new HashMap<>();

        try (ResultSet resultSet = DBConnector.executeQuery(query)) {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            if (rsmd.getColumnCount() != 2) {
                throw new SQLException("getRefs: there are " + rsmd.getColumnCount() + " output columns; has to be only 2");
            }
            int cnt = 0;
            while (resultSet.next()) {
                Integer id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                if (name != null && !name.isEmpty() && id != null) {
                    ret.put(id, name);
                    DebugRec(resultSet);
                    cnt++;
                }
            }
            logger.debug("\tRetrieved " + cnt + " records");
//        resultSet.getStatement().close();
        }

        return ret;

    }


    public Integer[] getIDs(String tab, String retField, String where) throws SQLException {
//        m_connector.getIDs(this, "select sid from orssess_logbr WHERE "+getWhere("connid", m_ConnIds));
        return (TableExist(tab))
                ? getIDs("select distinct " + retField + " from " + tab + " " + where)
                : null;
    }

    public Integer[] getRefIDs(ReferenceType rt, String[] names) throws SQLException {
        if (refTableExist(rt)) {
            return getIDs(rt.toString(), "id", getWhere("name", names, true));
        }
        return new Integer[0];
    }

    public Integer[] getRefIDs(ReferenceType rt, String[] names, boolean isRegex) throws SQLException {
        if (refTableExist(rt)) {
            return getIDs(rt.toString(), "id", getWhere("name", names, true));
        }
        return new Integer[0];
    }

    public Integer[] getIDs(Object obj, String query) throws SQLException {
        ArrayList<Integer> ret = new ArrayList<>();
        DBConnector connector = DBConnector.getDatabaseConnector(obj);

        if (connector != null) {
            try (ResultSet resultSet = connector.executeQuery(query)) {
                ResultSetMetaData rsmd = resultSet.getMetaData();

                if (rsmd.getColumnCount() != 1) {
                    throw new SQLException("getIDs: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
                }
                int cnt = 0;
                while (resultSet.next()) {
                    Integer res = resultSet.getInt(1);
                    if (res > 0) {
                        ret.add(res);
                        DebugRec(resultSet);
                        cnt++;
                    }
                }
                logger.debug("\tRetrieved " + cnt + " records");
            }
        }
        return ret.toArray(new Integer[ret.size()]);
    }

    public String[] getNames(String query) throws SQLException {
        ArrayList<String> ret = new ArrayList<>();

        try (ResultSet resultSet = executeQuery(query)) {
            ResultSetMetaData rsmd = resultSet.getMetaData();

            if (rsmd.getColumnCount() != 1) {
                throw new SQLException("getIDs: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
            }
            int cnt = 0;
            while (resultSet.next()) {
                String res = resultSet.getString(1);
                if (res != null && !res.isEmpty()) {
                    ret.add(res);
                    DebugRec(resultSet);
                    cnt++;
                }
            }
            logger.debug("\tRetrieved " + cnt + " records");
//        resultSet.getStatement().close();
            resultSet.getStatement().close();
        }

        return ret.toArray(new String[ret.size()]);

    }


    public String[] getNames(Object obj, String query) throws SQLException {
        ArrayList<String> ret = new ArrayList<>();

        try (ResultSet resultSet = executeQuery(query)) {
            ResultSetMetaData rsmd = resultSet.getMetaData();
            if (rsmd.getColumnCount() != 1) {
                throw new SQLException("getIDs: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
            }
            int cnt = 0;
            while (resultSet.next()) {
                String res = resultSet.getString(1);
                if (res != null && !res.isEmpty()) {
                    ret.add(res);
                    DebugRec(resultSet);
                    cnt++;
                }
            }
            logger.debug("\tRetrieved " + cnt + " records");
            //        resultSet.getStatement().close();
        }

        return ret.toArray(new String[ret.size()]);
    }

    public UTCTimeRange getTimeRange(String[] tabs) throws SQLException {
        return getTimeRange(tabs, null);
    }


    UTCTimeRange getTimeRange(String[] tabs, ArrayList<Integer> searchApps) throws SQLException {
        UTCTimeRange ret = new UTCTimeRange(0, 0);
        StringBuilder q = new StringBuilder(256);
        Integer[] arrSearchApps = null;
        if (searchApps != null && !searchApps.isEmpty()) {
            arrSearchApps = searchApps.toArray(new Integer[searchApps.size()]);
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

                List<List<Long>> iDsMultiple = getIDsMultiple(q.toString());
                if (!iDsMultiple.isEmpty()) {
                    List<Long> dbRet = iDsMultiple.get(0);
                    Long val = dbRet.get(0);
                    if (val != null && val > 0 && (ret.getStart() == 0 || ret.getStart() > val)) {
                        ret.setStart(val);

                    }

                    val = dbRet.get(1);
                    if (val != null && val > 0 && (ret.getEnd() == 0 || ret.getEnd() < val)) {
                        ret.setEnd(val);


                    }
                }

            }

        }
        if (ret.getStart() == 0 && ret.getEnd() == 0) {
            return null;
        }
        return ret;
    }

    public UTCTimeRange getTimeRange(String tab) throws SQLException {
        return getTimeRange(new String[]{tab});
    }

    public List<List<Long>> getIDsMultiple(String query) throws SQLException {

        List<List<Long>> ret;
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
                DebugRec(rs);
                cnt++;
            }
            logger.debug("\tRetrieved " + cnt + " records");
            rs.getStatement().close();
        }

        return ret;

    }


    public FullTable getFullTable(String query) throws SQLException {

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
                DebugRec(rs);
                ret.addRow(row);
                cnt++;
            }
            logger.debug("\tRetrieved " + cnt + " records");
            rs.getStatement().close();
        }

        return ret;

    }

    public String getValue(String exp, String p) throws SQLException {

        String s = exp.replaceAll("\\$1\\b", "'" + p + "'");

        try (Statement createStatement = DBConnector.m_conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

            ResultSet rs = createStatement.executeQuery(s);

            while (rs.next()) {
                return rs.getString(1);
            }
        }
        logger.info("\tgetValue " + " nothing returned");
        return null;
    }

    public Integer[] getIDs(String query) throws SQLException {
        ArrayList<Integer> ret = new ArrayList<>();

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
            logger.debug("\tRetrieved " + cnt + " records");
            ids = ret.toArray(new Integer[0]);
            if (logger.isDebugEnabled()) {
                if (ids == null || ids.length == 0) {
                    logger.debug("ids: empty");
                } else {
                    StringBuilder s = new StringBuilder();
                    s.append("ids: ");
                    for (Integer callid : sortedArray(ids)) {
                        s.append(callid).append(",");
                    }
                    logger.debug(s);
                }
            }

            rs.getStatement().close();
        }
        return ids;
    }

    public void GracefulClose() {
        try {
            if (DBConnector != null) {
                for (Statement m_activeStatement : DBConnector.m_activeStatements.values()) {
                    m_activeStatement.close();
                }
                DBConnector.m_activeStatements.clear();

                for (int i = 0; i < DBConnector.m_freeStatements.size(); i++) {
                    Statement stmt = DBConnector.m_freeStatements.get(i);
                    stmt.close();
                }
                DBConnector.m_freeStatements.clear();

                if (DBConnector.m_conn != null) {
                    DBConnector.m_conn.close();
                }
            }
        } catch (SQLException sQLException) {
            logger.error("Exception closing", sQLException);
        }
    }

    String[] getRefNames(String tab) throws SQLException {
        return getRefNames(tab, null, null);
    }

    String[] getRefNames(String tab, String CheckTab, String CheckIDField) throws SQLException {
        return getRefNames(tab, CheckTab, CheckIDField, null);
    }

    public String[] getRefNames(ReferenceType rt, String[] names, boolean isRegex) throws SQLException {
        return getNames(rt.toString(), "name", IQuery.getWhere("name", names, true, isRegex));
    }

    public String[] getRefNames(ReferenceType rt, String name, boolean isRegex) throws SQLException {
        String[] names = new String[]{name};
        return getRefNames(rt, names, isRegex);
    }

    String[] getRefNames(String tab, String CheckTab, String CheckIDField, String CheckIDField1) throws SQLException {

        if (!TableExist(tab)) {
            return null;
        }
        ArrayList<String> ret = new ArrayList<>();

        StringBuilder q = new StringBuilder(256).
                append("select name from ").append(tab);//remove 'distinct' from here. Uniquness of names is ensured by parser,
        //so no need to sort again
        if (CheckTab != null && CheckIDField != null) {
            q.append(" where id in (select distinct ").append(CheckIDField).append(" from ").append(CheckTab);
            if (CheckIDField1 != null) {
                q.append("\nunion\nselect distinct ").append(CheckIDField1).append(" from ").append(CheckTab);
            }

            q.append("\n)");
        }
        q.append("\norder by name");
        try (ResultSet resultSet = executeQuery(q.toString())) {
            ResultSetMetaData rsmd = resultSet.getMetaData();

            if (rsmd.getColumnCount() != 1) {
                throw new SQLException("getRefNames: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
            }
            int cnt = 0;
            while (resultSet.next()) {
                String res = resultSet.getString(1);
                if (res != null && !res.isEmpty()) {
                    ret.add(res);
                    DebugRec(resultSet);
                    cnt++;
                }
            }
            logger.debug("\tRetrieved " + cnt + " records");
        }


        return ret.toArray(new String[ret.size()]);
    }

    private void DebugRec(ResultSet m_resultSet) {
        if (logger.isTraceEnabled()) {
            try {
                ResultSetMetaData metadata = m_resultSet.getMetaData();
                StringBuilder s = new StringBuilder(256);
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    s.append(" [").append(metadata.getColumnName(i)).append("]: ").append(m_resultSet.getString(i));
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw new RuntimeInterruptException();
                } else {
                    logger.trace(s.toString());
//                    Thread.currentThread().stop();
                }
            } catch (SQLException E) {
                inquirer.ExceptionHandler.handleException("\t error printing records\n", E);
            } catch (RuntimeInterruptException e) {
                System.out.println("e");
                logger.error(e);
                throw e;
            }
        }
    }


    public ResultSet executeQuery(String query) throws SQLException {
        ResultSet ret;
        if (query != null && !query.trim().isEmpty()) {
            long timeStart = new Date().getTime();

            logger.debug("[" + query + "]");

            try {
                Statement stat = m_conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                currentStatement = stat;
                stat.closeOnCompletion();
                Explain(stat, query);
                ret = stat.executeQuery(query);

            } catch (SQLException sQLException) {
                throw sQLException;
            }

            long timeEnd = new Date().getTime();
            logger.debug("\t Execution took " + pDuration(timeEnd - timeStart));
            return ret;
        } else {
            return null;
        }

    }

    public ResultSet getRecords(int statementId) {
        PreparedStatement statement = m_statements.get(statementId);
        try {
            ResultSet rs = statement.executeQuery();
            return rs;
        } catch (SQLException e) {
            logger.error("msg", e);
        }
        return null;
    }

    abstract class ILoadFun {

        public abstract String[] getNames(String id) throws SQLException;
    }

    class IDsToName {

        private final HashMap<String, IDToName> nameHashMap;

        public IDsToName() {
            this.nameHashMap = new HashMap<>();
        }

        public void addFun(String funName, ILoadFun fun) throws SQLException {
            nameHashMap.put(funName, new IDToName(fun));
        }

        public String getNames(String fName, String id) throws SQLException {
            if (fName != null && id != null) {
                IDToName idn = nameHashMap.get(fName);
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
            private final HashMap<String, String[]> stringHashMap;

            public IDToName(ILoadFun fun) {
                this.fun = fun;
                stringHashMap = new HashMap<>();
            }

            public String[] getNames(String id) throws SQLException {
                if (stringHashMap.containsKey(id)) {
                    return stringHashMap.get(id);
                } else {
                    String[] names = fun.getNames(id);
                    if (names != null && names.length == 0) {
                        names = null;
                    }
                    stringHashMap.put(id, names);
                    return names;
                }
            }

        }
    }

}
