/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static Utils.Util.pDuration;

/**
 * @author ssydoruk
 */
public final class SqliteAccessor implements DBAccessor {

    private static final Logger logger = Main.logger;
    String m_alias;
    Connection m_conn;

    boolean m_exit;
    Stats stats;

    private final HashMap<String, Boolean> tabExists = new HashMap<>();
    private List<Long> filesToDelete;

    public SqliteAccessor() {
        //
    }


    public void init(String dbname, String alias) throws SQLException {
        m_alias = alias;
        filesToDelete = Collections.synchronizedList(new ArrayList<>());

        try {
            String name = dbname + ".db";

            SQLiteDataSource ds = new SQLiteDataSource();
            ds.setUrl("jdbc:sqlite:" + name);
            m_conn=ds.getConnection();

            Main.logger.info("Using DB: ["+name+"]");
            // m_conn = DriverManager.getConnection("jdbc:sqlite:" + name);

            String s = 
            // "PRAGMA page_size = 32768;\n"
            ""
            + "PRAGMA cache_size=10000;\n"
            + "pragma temp_store = memory;\n"
            + "pragma mmap_size = 30000000000;\n"
            + "PRAGMA synchronous=OFF;\n"
            //                    + "PRAGMA main.cache_size=5000;\n"
            + "PRAGMA automatic_index=false;\n";

    if (Main.getInstance().getEe().isSqlPragma()) {
        s += "PRAGMA journal_mode=OFF;\n";
    } else {
        logger.info("SQLite pragmas not turned");
    }


            logger.info("Executing pragmas: " + s);
            runQuery(s);

            ResetAutoCommit(false);

            stats = new Stats();

        } catch (SQLException e) {
            logger.error("Creating accessor failed: " + e, e);
            throw new SQLException(e.getMessage());
        }
    }

    public List<Long> getFilesToDelete() {
        return filesToDelete;
    }

    void setFilesToDelete(ArrayList<Long> filesToDelete) {
        if (!filesToDelete.isEmpty()) {
            this.filesToDelete.addAll(filesToDelete);
        }
    }

    public String getM_alias() {
        return m_alias;
    }

    public Connection getM_conn() {
        return m_conn;
    }

    void Commit() {
        try {
            stats.DoneInserts();

        } catch (SQLException e) {
            logger.error("Error finalizing batch: " + e, e);
        }
    }

    @Override
    public void runQuery(String query) {
        logger.debug("[" + query + "]");
        synchronized (m_conn) {
            try {
                try (Statement statement = m_conn.createStatement()) {
                    statement.executeUpdate(query);
                }
            } catch (SQLException e) {
                logger.error("ExecuteQuery failed: " + e + " query " + query, e);
            }
        }
    }

    public void Commit(PreparedStatement stmt) throws SQLException {
        int[] ret = stmt.executeBatch();
        if (ret.length > 0) {
            for (int i = 0; i < ret.length; i++) {
                if (ret[i] < 0) {
                    throw new SQLException("flush failed: " + ret[i]);
                }
            }
            try {
                Main.logger.debug("Commit");
                m_conn.commit();
            } catch (SQLException sQLException) {
                logger.error("Exception on commit: " + stmt, sQLException);
                for (int i = 0; i < ret.length; i++) {
                    logger.error("\tret[" + i + "]=" + ret[i]);
                }

            }
            ResetAutoCommit();
        }
        stmt.close();
    }

    public void DoneInserts() {
        filesToDelete = null;// just in case, to avoid using of it while finalize
        try {
            stats.DoneInserts();

        } catch (SQLException e) {
            logger.error("Error finalizing batch: " + e, e);
        }
        try {
            String query = "UPDATE file_" + m_alias + " "
                    + "SET NodeId= "
                    + "( "
                    + "SELECT NodeId FROM file_" + m_alias + " file "
                    + "WHERE file.RunId=file_" + m_alias + ".RunId "
                    + "AND file.NodeId!=\"\" "
                    + ") "
                    + "WHERE NodeId=\"\";";
            runQuery(query);


        } catch (Exception e) {
            logger.error("Error finalizing the connection: " + e, e);
        }
    }

    public void FinalizeDB() throws SQLException {
        runQuery("UPDATE STSTEVENT_" + m_alias + " "
                + "SET time= "
                + "( "
                + "SELECT MAX(time) FROM tlib_" + m_alias + " tlib "
                + "WHERE tlib.connectionIdid=STSTEVENT_" + m_alias + ".connectionidid "
                + "AND tlib.nameid=STSTEVENT_" + m_alias + ".nameid "
                + "AND tlib.thisdnid=STSTEVENT_" + m_alias + ".thisdnid "
                + "AND tlib.time<=STSTEVENT_" + m_alias + ".time+1000 "
                + ") WHERE EXISTS "
                + "( "
                + "SELECT time FROM tlib_" + m_alias + " tlib "
                + "WHERE tlib.connectionIdid=STSTEVENT_" + m_alias + ".connectionidid "
                + "AND tlib.nameid=STSTEVENT_" + m_alias + ".nameid "
                + "AND tlib.thisdnid=STSTEVENT_" + m_alias + ".thisdnid "
                + "AND tlib.time<=STSTEVENT_" + m_alias + ".time+1000 "
                + "); ");

        runQuery("Alter table orssess_logbr add connidid INTEGER;\n"
                + "update orssess_logbr\n"
                + "set connidid=(select distinct connectionidid from ors_logbr t\n"
                + "where t.uuidid=orssess_logbr.uuidid);\n");
        runQuery("create index if not exists orssess_connid_" + getM_alias() + " on orssess_" + getM_alias() + " (connidid);");

        runQuery("update ORSURS_logbr set sidid =\n"
                + "(select or1.sidid from ORSURS_logbr or1 where\n"
                + "ORSURS_logbr.refid=or1.refid and or1.refid>0 and or1.sidid>0  \n"
                + "and ORSURS_logbr.fileid in (select id from file_logbr where name in (select name from file_logbr where id=or1.fileid))),\n"
                + "inbound=1\n"
                + "where sidid <=0 \n");
        sidForORSURS();

        //                    + "PRAGMA main.page_size = 4096;\n"
        //                    + "PRAGMA main.cache_size=10000;\n"
        runQuery("PRAGMA main.locking_mode=NORMAL;\n"
                + //                    + "PRAGMA main.journal_mode=WAL;\n"
                //                    + "PRAGMA main.cache_size=5000;\n"
                "PRAGMA optimize;\n");

    }

    private void sidForORSURS() throws SQLException {
        String ORSURSReq = "select id,sidid,event,refid,reqid from orsurs_logbr where ( sidid>0 and event>0 and refid>0 and reqid>0)\n"
                + "or ( sidid <=0 and refid in (-1,0) and reqid>0 and event>1000)\n"
                + "order by time";

        String ORSURSUpd = "update orsurs_logbr set sidid=? where id=?";
        try (ResultSet ORSURSrecs = GetRecords(ORSURSReq)) {

            if (ORSURSrecs != null) {

                Connection conn = getM_conn();

                try (PreparedStatement stmtORSURS = conn.prepareStatement(ORSURSUpd)) {

                    HashMap<Integer, Integer> reqSid = new HashMap<>();

                    synchronized (conn) {
                        int updCnt = 0;
                        conn.setAutoCommit(false);
                        try {
                            while (ORSURSrecs.next()) {
                                if (updCnt++ >= 1_000) {
                                    Main.logger.debug("Commit1");
                                    conn.commit();
                                    updCnt = 0;
                                }
                                int sid = ORSURSrecs.getInt("sidid");

                                if (sid <= 0) {
                                    sid = reqSid.get(ORSURSrecs.getInt("reqid"));
                                    if (sid > 0) {
                                        stmtORSURS.setInt(1, sid);
                                        stmtORSURS.setInt(2, ORSURSrecs.getInt("id"));
                                        stmtORSURS.executeUpdate();
                                    }
                                } else {
                                    reqSid.put(ORSURSrecs.getInt("reqid"), sid);
                                }

                                // Do whatever you want to do with these 2 values
                            }
                        } catch (SQLException E) {
                            logger.error("Exception finalizing: " + E.getMessage(), E);
                            throw E;
                        } finally {
                            Main.logger.debug("Commit2");
                            conn.commit();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void Close(boolean shouldAnalyze) {
        try {
            if (shouldAnalyze) {
                runQuery("analyze;");
            }
            m_conn.close();
        } catch (SQLException e) {
            logger.error("Error finalizing the connection: " + e, e);
        }
    }

    public void ResetAutoCommit() {
        //
    }

    public ResultSet GetRecords(String query) throws SQLException {
        return executeQuery( query);
    }

    public PreparedStatement getStatement(String query) throws SQLException {
        return m_conn.prepareStatement(query);
    }

    public synchronized int PrepareStatement(String query) {
        try {
            return stats.addStat(m_conn.prepareStatement(query));
        } catch (SQLException e) {
            logger.error("Error while registering record: " + e + " query: " + query, e);
        }
        return -1;
    }


    public synchronized void exit() {
        m_exit = true;
        Main.getInstance().getM_accessor().notify();
    }


    public void ResetAutoCommit(boolean b) {
        try {
            m_conn.setAutoCommit(b);
        } catch (SQLException e) {
            logger.error("Resetting auto commit failed: " + e, e);
        }
    }

    int getID(String query, int def) throws SQLException {
        try {
            Integer[] iDs = getIDs(query);
            if (iDs != null && iDs.length > 0) {
                return iDs[0];
            }
        } catch (Exception ex) {
            logger.error("fatal: ", ex);
        }

        return def;
    }

    int getID(String query, String tab, int def) throws SQLException {
        if (TableExist(tab)) {
            return getID(query, def);
        }

        return def;
    }

    public boolean TableExist(String tab) throws SQLException {
        return TableExist(tab, false);
    }

    public boolean TableExist(String tab, boolean forceCheck) throws SQLException {
        if (!forceCheck && tabExists.containsKey(tab)) {
            return tabExists.get(tab);
        } else {
            Integer[] ids = getIDs("select count(*) from sqlite_master WHERE "
                    + "type=\"table\" and name=\"" + tab + "\" ");
            boolean b = (ids.length > 0 && ids[0] == 1);
            tabExists.put(tab, b);
            return b;
        }
    }

    public boolean TableEmpty(String tab) throws Exception {
        if (TableExist(tab)) {
            Integer[] ids = getIDs("select count(*) from " + tab + ";");
            boolean b = (ids.length > 0 && ids[0] > 0);
            return !b;
        }
        return true;
    }

    public ResultSet executeQuery(String query) throws SQLException {
        Instant timeStart = Instant.now();
        // * @param resultSetType a result set type; one of
        // *        {@code ResultSet.TYPE_FORWARD_ONLY},
        // *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
        // *        {@code ResultSet.TYPE_SCROLL_SENSITIVE}
        // * @param resultSetConcurrency a concurrency type; one of
        // *        {@code ResultSet.CONCUR_READ_ONLY} or
        // *        {@code ResultSet.CONCUR_UPDATABLE}

            Statement stmt = getM_conn().createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
            stmt.closeOnCompletion();
            Main.logger.debug("About to executeQuery [" + query + "]");

            ResultSet ret = stmt.executeQuery(query);
            Main.logger.debug("\tExecuteQuery execution took " + pDuration(Duration.between(Instant.now(), timeStart).toMillis()));
            return ret;
    }

    public Integer[] getIDs(String query) throws SQLException {
        ArrayList<Integer> ret = new ArrayList<>();

        Instant timeStart = Instant.now();

        Main.logger.debug("[" + query + "]");
        try (ResultSet rs = executeQuery(query)) {

            Main.logger.debug("\t Execution took " + pDuration(Duration.between(Instant.now(), timeStart).toMillis()));

            ResultSetMetaData rsmd = rs.getMetaData();

            if (rsmd.getColumnCount() != 1) {
                throw new SQLException("getIDs: there are " + rsmd.getColumnCount() + " output columns; has to be only 1");
            }
            int cnt = 0;
            while (rs.next()) {
                Integer res = rs.getInt(1);
                if (res > 0) {
                    ret.add(res);
                    cnt++;
                }
            }
            Main.logger.debug("\tRetrieved " + cnt + " records");
        }

        return ret.toArray(new Integer[ret.size()]);
    }

    public List<ArrayList<Object>> getObjMultiple(String query, String tab) throws Exception {
        ArrayList<ArrayList<Object>> ret = new ArrayList<>();
        if (TableExist(tab)) {
            Statement stmt = getM_conn().createStatement();
            Main.logger.debug("[" + query + "]");

            try (ResultSet rs = stmt.executeQuery(query)) {
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int cnt = 0;
                while (rs.next()) {
                    ArrayList<Object> row = new ArrayList<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    ret.add(row);
                    cnt++;
                }
                Main.logger.debug("\tRetrieved " + cnt + " records");
                rs.getStatement().close();
            }
        }
        return ret;

    }


    public List<ArrayList<Long>> getIDsMultiple(String query) throws SQLException {

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
                cnt++;
            }
            Main.logger.debug("\tRetrieved " + cnt + " records");
            rs.getStatement().close();
        }

        return ret;

    }

    public void addFileToDelete(long fileID) {
        filesToDelete.add(fileID);
    }

    public void addToDB(int m_insertStatementId, IFillStatement fillStatement) throws SQLException {
        Stat stat = stats.get(m_insertStatementId);
        synchronized (stat.getPreparedStatement()) {
            fillStatement.fillStatement(stat.getPreparedStatement());
            stats.Submit(stat);
        }
    }

    class Stat {

        private final PreparedStatement preparedStatement;
        private int cnt;

        Stat(PreparedStatement stat) {
            this.preparedStatement = stat;
            cnt = 0;
        }

        public PreparedStatement getPreparedStatement() {
            return preparedStatement;
        }

        public void Submit() throws SQLException {
            preparedStatement.addBatch();
            cnt++;
        }

        private synchronized void DoneInserts() throws SQLException {
            try {
                flush();
            } catch (SQLException sQLException) {
                logger.error("Exception finalizing " + preparedStatement.toString(), sQLException);
            }
        }

        private void flush() throws SQLException {
            int[] ret = preparedStatement.executeBatch();
            logger.trace("flashing " + preparedStatement + " ret: " + ret.length);
            if (ret.length > 0) {
                for (int i = 0; i < ret.length; i++) {
                    if (ret[i] < 0) {
                        throw new SQLException("flush failed: " + ret[i]);
                    }
                }
                cnt = 0;
                Main.logger.trace("flushed " + ret.length + " records");
                try {
                    m_conn.commit();
                } catch (SQLException e) {
                    Main.logger.error("Exception while commit", e);
                }
                ResetAutoCommit();
            }
        }
    }

    class Stats {

        ArrayList<Stat> statsList;

        Stats() {
            statsList = new ArrayList<>();
        }

        public int addStat(PreparedStatement st) {
            statsList.add(new Stat(st));
            return statsList.size() - 1;
        }

        public Stat get(int statementId) {
            return statsList.get(statementId);
        }

        public void Submit(int statementId) throws SQLException {
            Submit(statsList.get(statementId));
        }

        public synchronized void Submit(Stat st) throws SQLException {
            logger.trace("Submitting " + st);
            st.Submit();
            if (st.cnt >= 10_000) {
                st.flush();
                st.cnt = 0;
            }
        }

        private void DoneInserts() throws SQLException {
            for (Stat st : statsList) {
                try {
                    st.DoneInserts();
                } catch (SQLException e) {
                    logger.error("Error finalizing batch: " + e, e);
                }
            }
        }
    }

}
