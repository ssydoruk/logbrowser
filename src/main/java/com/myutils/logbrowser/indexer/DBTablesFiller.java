package com.myutils.logbrowser.indexer;

import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * per parser container of prepared statements
 */
class DBTablesFiller {

    private static final Logger logger = Main.logger;
    private final DBTables dbTables;
    private final SqliteAccessor accessor;
    private HashMap<String, StatParser> stats;

    DBTablesFiller(DBTables tables, SqliteAccessor accessor) {
        stats = new HashMap<>();
        this.dbTables = tables;
        this.accessor = accessor;
    }

    public DBTables getDbTables() {
        return dbTables;
    }

    public void addToDB(Record rec) throws SQLException {
        StatParser st = getStatement(rec.getM_type());
        if (rec.fillStat(st.getStat())) {
            Submit(st);
        }
    }

    private StatParser getStatement(String type) throws SQLException {
        StatParser ret = stats.get(type);
        if (ret == null) {
            DBTable dbTable;
            synchronized (dbTables) {
                dbTable = dbTables.get(type);
            }
            if (dbTable == null) {
                logger.fatal("Not found table for type [" + type + "]");
            } else {
                dbTable.checkInit();
                synchronized (accessor) {
                    ret = new StatParser(accessor.getStatement(dbTable.getInsert()));
                }
                stats.put(type, ret);
            }
        }
        return ret;
    }

    public void Submit(StatParser st) throws SQLException {
        logger.trace("Submitting " + st);
        st.Submit();
        if (st.cnt % 10_000 == 0) {
//                Main.m_accessor.flush(st);
            st.flush();
        }
    }

    public void DoneInserts() {
        for (StatParser st : stats.values()) {
            try {
                st.DoneInserts();
            } catch (SQLException e) {
                logger.error("Error finalizing batch: " + e, e);
            }
        }
    }

    public void updateRecCounters() {
        synchronized (dbTables) {
            for (Map.Entry<String, StatParser> stat : stats.entrySet()) {
                if (dbTables.containsKey(stat.getKey())) {
                    dbTables.get(stat.getKey()).addCnt(stat.getValue().cnt);
                }
            }

        }
    }

    class StatParser {

        private PreparedStatement stat;
        private int cnt;

        StatParser(PreparedStatement stat) {
            this.stat = stat;
            cnt = 0;
        }

        public PreparedStatement getStat() {
            return stat;
        }

        private void Submit() throws SQLException {
            stat.addBatch();
            cnt++;
        }

        private void DoneInserts() throws SQLException {
            try {
                flush();
            } catch (SQLException sQLException) {
                logger.error("Exception finalizing " + stat.toString(), sQLException);
//                throw new SQLException("Exception finalizing "+stat.toString());
            }
        }

        private void flush() throws SQLException {
            Connection connection = accessor.getM_conn();
            synchronized (connection) {
                int[] ret = stat.executeBatch();
                logger.trace("flashing " + stat + " ret: " + ret.length);
                if (ret.length > 0) {
                    for (int i = 0; i < ret.length; i++) {
                        if (ret[i] < 0) {
                            throw new SQLException("flush failed: " + ret[i]);
                        }
                    }
                    cnt = 0;
                    Main.logger.trace("flushed " + ret.length + " records");
                    try {
                        connection.commit();
                    } catch (SQLException e) {
                        Main.logger.error("Exception while commit", e);
                    }
                }
            }
        }
    }
}
