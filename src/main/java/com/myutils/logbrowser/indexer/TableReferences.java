/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.lang3.StringUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author ssydoruk
 */
class TableReference {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;
    private final ReferenceType type;
    private final HashMap<CIString, Integer> valRef;
    private int initialID = 0;
    private int maxID;

    TableReference(ReferenceType type) {
        this.type = type;
        valRef = new HashMap<>();
        maxID = 0;
        if (Main.isDbExisted()) {
            SqliteAccessor dbAccessor = Main.getInstance().getM_accessor();
            synchronized (dbAccessor) {
                try {
                    if (dbAccessor.TableExist(type.toString())) {
                        try (ResultSet rs = dbAccessor.executeQuery("select name, id from " + type)) {
                            Main.logger.trace("Reading references from " + type);

                            //                    ResultSetMetaData rsmd = rs.getMetaData();
                            int cnt = 0;
                            while (rs.next()) {
                                String n = rs.getString(1);
                                if (n != null && !n.isEmpty()) {
                                    int aInt = rs.getInt(2);
                                    if (aInt > maxID) {
                                        maxID = aInt;
                                    }
                                    Main.logger.trace("Added name/key: [" + rs.getString(1) + "] / " + aInt);
                                    valRef.put(new CIString(rs.getString(1)), aInt);
                                    cnt++;
                                }
                            }
                            Main.logger.debug("\tRetrieved " + cnt + " records");
                        }
                        this.initialID = maxID;
                    }
                } catch (Exception ex) {
                    logger.error("fatal: ", ex);
                }
            }
        }
    }

    void updateRef(String key, String toString) {
        CIString ciString = new CIString(key);
        Integer id = valRef.get(ciString);
        if (id != null) {
            valRef.remove(ciString);
            valRef.put(new CIString(toString), id);
        }
    }

    public HashMap<CIString, Integer> getValRef() {
        return valRef;
    }

    public ReferenceType getType() {
        return type;
    }

    int getRef(String key, int wordsToCompare) {
        String[] split = StringUtils.split(key);
        Integer ref = null;
        for (Map.Entry<CIString, Integer> entry : valRef.entrySet()) {
            CIString key1 = entry.getKey();
            String[] split1 = StringUtils.split(key1.toString());
            for (int i = 0; i < wordsToCompare && i < split.length; i++) {
                if (split[i] != null && split1[i] != null && split[i].equalsIgnoreCase(split1[i])) {
                    ref = entry.getValue();
                    break;
                }
            }
            if (ref != null) {
                break;
            }
        }
        Main.logger.trace("getRef1 key [" + key + "] ret: " + ((ref == null) ? "NULL" : ref));
        if (ref == null) {
            return putRef(new CIString(key));
        } else {
            return ref;
        }
    }

    synchronized int getRef(String key) {
        CIString cis = new CIString(key);
        Integer ref = valRef.get(cis);
        Main.logger.trace("getRef key[" + key + "] ret[" + ((ref == null) ? "NULL" : ref) + "]");
        if (ref == null) {
            return putRef(cis);
        } else {
            return ref;
        }
    }

    private int putRef(CIString key) {
        if (key != null && key.length() > 0) {
            maxID++;
            valRef.put(key, maxID);
            Main.logger.trace("putRef  key[" + key + "] id: " + maxID);
            return maxID;
        } else {
            return 0;
        }
    }

    void Finalize(SqliteAccessor m_accessor, boolean onlyLastRecord) throws SQLException {
        if (Main.logger.isTraceEnabled()) {
            Main.logger.trace("finalizing " + type.toString());
            for (Map.Entry<CIString, Integer> entry : valRef.entrySet()) {
                Main.logger.trace("[" + entry.getKey() + "]->[" + entry.getValue() + "]");
            }
        }
        try {
            if (!valRef.isEmpty()) {
                String query = "create table if not exists " + type.toString() + " (id INTEGER primary key,"
                        + "name char(255) collate nocase);";

                m_accessor.ResetAutoCommit(true);
                String idxID = type + "_id";
                String idxName = type + "_name";
                m_accessor.runQuery("DROP INDEX IF EXISTS " + idxID + " ;");
                m_accessor.runQuery("DROP INDEX IF EXISTS " + idxName + " ;");

                m_accessor.runQuery(query);
                m_accessor.ResetAutoCommit(false);
                int m_InsertStatementId = m_accessor.PrepareStatement("INSERT INTO " + type + " VALUES(?,?);");

                int i = 0;
                int iTotalRecs = 0;
                if (onlyLastRecord) { // for reference that we do not save, saving only last record so that if need to restart parsing,
                    // correct ID would be generated
                    int theID = 0;
                    CIString theKey = null;
                    for (Map.Entry<CIString, Integer> entrySet : valRef.entrySet()) {
                        if (entrySet.getValue() > theID) {
                            theID = entrySet.getValue();
                            theKey = entrySet.getKey();
                        }
                    }
                    if (theID > 0) {
                        int finalTheID = theID;
                        String finalTheKey = theKey.toString();
                        m_accessor.addToDB(m_InsertStatementId, (PreparedStatement stmt) -> {
                            stmt.setInt(1, finalTheID);
                            Record.setFieldString(stmt, 2, finalTheKey);
                            return true;
                        });
                        i = 1;
                        iTotalRecs = 1;
                    }

                } else {
                    for (Map.Entry<CIString, Integer> entrySet : valRef.entrySet()) {
                        int theID = entrySet.getValue();
                        if (theID > initialID) {
                            Main.logger.trace("inserting [" + theID + ":" + entrySet.getKey() + "]");
                            m_accessor.addToDB(m_InsertStatementId, (PreparedStatement stmt) -> {
                                stmt.setInt(1, theID);
                                Record.setFieldString(stmt, 2, entrySet.getKey().toString());
                                return true;
                            });
                            i++;
                        }
                        iTotalRecs++;
                    }
                }
                m_accessor.Commit();
                m_accessor.ResetAutoCommit(true);

                m_accessor.runQuery("create unique index if not exists " + type + "_id on " + type + " (id);");
                m_accessor.runQuery("create unique index if not exists " + type + "_name on " + type + " (name collate nocase);");
                Main.logger.info("Finalized ref table " + type + "; added " + i + " recs (total " + iTotalRecs + " recs)");
            }
        } catch (SQLException sQLException) {
            Main.logger.error("Not able to finalize " + type, sQLException);
            for (Map.Entry<CIString, Integer> entry : valRef.entrySet()) {
                Main.logger.info("[" + entry.getKey() + "]->[" + entry.getValue() + "]");
            }
        }
    }

    public final class CIString {

        private final String s;
        private int hashCode = 0;

        public CIString(String s) {
            if (s == null) {
                throw new NullPointerException();
            }
            this.s = s;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CIString) {
                CIString o1 = (CIString) o;
//                Main.logger.trace("TableReference equal: object: [" + o1.toString() + "] compare to [" + s + "]; result: [" + o1.s.equalsIgnoreCase(s) + "]");
                return o1.s.equalsIgnoreCase(s);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = s.toUpperCase().hashCode();
            }

            return hashCode;
        }

        @Override
        public String toString() {
            return s;
        }

        private int length() {
            return s.length();
        }
    }

}

public class TableReferences {

    private final HashMap<ReferenceType, TableReference> tabRefs;
    private final HashSet<ReferenceType> doNotSave;
    SqliteAccessor m_accessor;

    TableReferences(SqliteAccessor m_accessor) {
        tabRefs = new HashMap<>();
        this.m_accessor = m_accessor;
        doNotSave = new HashSet<>();

    }

    public void Finalize() throws SQLException {
        for (TableReference tabRef : tabRefs.values()) {
            try {
                tabRef.Finalize(m_accessor, doNotSave.contains(tabRef.getType()));
                m_accessor.Commit();
            } catch (SQLException sQLException) {
                Main.logger.error("Not able to finalize " + tabRef.getType().toString(), sQLException);
                for (Map.Entry<TableReference.CIString, Integer> entry : tabRef.getValRef().entrySet()) {
                    Main.logger.info("[" + entry.getKey() + "]->[" + entry.getValue() + "]");
                }
            }
        }

    }

    public Integer getRef(ReferenceType type, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        TableReference tr;
        synchronized (tabRefs) {
            tr = tabRefs.get(type);

            if (tr == null) {
                tr = new TableReference(type);
                tabRefs.put(type, tr);
            }
        }
        return tr.getRef(key);

    }

    public synchronized Integer getRef(ReferenceType type, String key, int wordsToCompare) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        TableReference tr = tabRefs.get(type);

        if (tr == null) {
            tr = new TableReference(type);
            tabRefs.put(type, tr);
        }
        return tr.getRef(key, wordsToCompare);
    }

    void updateRef(ReferenceType type, String key, String toString) {
        TableReference tr = tabRefs.get(type);

        if (tr != null) {
            tr.updateRef(key, toString);
        }
    }

    void doNotSave(ReferenceType referenceType) {
        doNotSave.add(referenceType);
    }

}
