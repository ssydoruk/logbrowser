/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import static com.myutils.logbrowser.indexer.Main.getSQLiteaccessor;

/**
 *
 * @author ssydoruk
 */
class TableReference {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private int initialID = 0;

    void updateRef(String key, String toString) {
        CIString ciString = new CIString(key);
        Integer id = valRef.get(ciString);
        if (id != null) {
            valRef.remove(ciString);
            valRef.put(new CIString(toString), id);
        }
    }

    public final class CIString {

        private String s;

        public CIString(String s) {
            if (s == null) {
                throw new NullPointerException();
            }
            this.s = s;
        }

        public boolean equals(Object o) {
            if (o instanceof CIString) {
                CIString o1 = (CIString) o;
//                Main.logger.trace("TableReference equal: object: [" + o1.toString() + "] compare to [" + s + "]; result: [" + o1.s.equalsIgnoreCase(s) + "]");
                return o1.s.equalsIgnoreCase(s);
            } else {
                return false;
            }
        }

        private int hashCode = 0;

        public int hashCode() {
            if (hashCode == 0) {
                hashCode = s.toUpperCase().hashCode();
            }

            return hashCode;
        }

        public String toString() {
            return s;
        }

        private int length() {
            return s.length();
        }
    }

    private final ReferenceType type;
    private final HashMap<CIString, Integer> valRef;

    public HashMap<CIString, Integer> getValRef() {
        return valRef;
    }
    private int maxID;

    TableReference(ReferenceType type) {
        this.type = type;
        valRef = new HashMap<>();
        maxID = 0;
        if (Main.isDbExisted()) {
            SqliteAccessor dbAccessor = getSQLiteaccessor();
            try {
                if (dbAccessor.TableExist(type.toString())) {
                    ResultSet rs = dbAccessor.executeQuery("select name, id from " + type.toString());
                    Main.logger.trace("Reading references from " + type.toString());

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
                    rs.close();
                    rs.getStatement().close();
                    this.initialID = maxID;
                }
            } catch (Exception ex) {
                logger.log(org.apache.logging.log4j.Level.FATAL, ex);
            }
        }
    }

    public ReferenceType getType() {
        return type;
    }

    int getRef(String key, int wordsToCompare) {
        CIString cis = new CIString(key);
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
//        Main.logger.trace("getRef1 for " + type.toString() + " key [" + key + "] ret: " + ((ref == null) ? "NULL" : ref));
        if (ref == null) {
            return putRef(cis);
        } else {
            return ref;
        }
    }

    int getRef(String key) {
        CIString cis = new CIString(key);
        Integer ref = valRef.get(cis);
//        Main.logger.trace("getRef1 for " + type.toString() + " key [" + key + "] ret: " + ((ref == null) ? "NULL" : ref));
        if (ref == null) {
            return putRef(cis);
        } else {
            return ref;
        }
    }

    private int putRef(CIString key) {
//        Main.logger.trace("putRef1 for " + type.toString() + " key [" + key);
        if (key != null && key.length() > 0) {
            maxID++;
            valRef.put(key, maxID);
            Main.logger.trace("putRef done " + maxID + " key [" + key.toString() + "]");
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
//            Main.logger.debug("Finaling table " + type.toString());
                String query = "create table if not exists " + type.toString() + " (id INTEGER primary key,"
                        + "name char(255) collate nocase);";

//                m_accessor.Commit();
                m_accessor.ResetAutoCommit(true);
                String idxID = type.toString() + "_id";
                String idxName = type.toString() + "_name";
                m_accessor.runQuery("DROP INDEX IF EXISTS " + idxID + " ;");
                m_accessor.runQuery("DROP INDEX IF EXISTS " + idxName + " ;");

                m_accessor.runQuery(query);
                m_accessor.ResetAutoCommit(false);
                int m_InsertStatementId = m_accessor.PrepareStatement("INSERT INTO " + type.toString() + " VALUES(?,?);");
//            Main.logger.error("Ref ["+type.toString()+"]: "+valRef.size()+" recs");
                PreparedStatement stmt = m_accessor.GetStatement(m_InsertStatementId);

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
                        stmt.setInt(1, theID);
                        DBTable.setFieldString(stmt, 2, theKey.toString());
                        m_accessor.SubmitStatement(m_InsertStatementId);
                        i = 1;
                        iTotalRecs = 1;
                    }

                } else {
                    for (Map.Entry<CIString, Integer> entrySet : valRef.entrySet()) {
                        int theID = entrySet.getValue();
                        if (theID > initialID) {
                            Main.logger.trace("inserting [" + theID + ":" + entrySet.getKey() + "]");
                            stmt.setInt(1, theID);
                            DBTable.setFieldString(stmt, 2, entrySet.getKey().toString());
                            m_accessor.SubmitStatement(m_InsertStatementId);
                            i++;
                        }
                        iTotalRecs++;
                    }
                }
                m_accessor.Commit();
                m_accessor.ResetAutoCommit(true);

//                if (type != ReferenceType.HANDLER) {
                m_accessor.runQuery("create unique index if not exists " + type.toString() + "_id on " + type.toString() + " (id);");
                m_accessor.runQuery("create unique index if not exists " + type.toString() + "_name on " + type.toString() + " (name collate nocase);");
//                } else {
//                    Main.logger.info("No indexes for " + type.toString());
//                }
                Main.logger.info("Finalized ref table " + type.toString() + "; added " + i + " recs (total " + iTotalRecs + " recs)");
//            m_accessor.Commit(stmt);
            }
        } catch (SQLException sQLException) {
            Main.logger.error("Not able to finalize " + type.toString(), sQLException);
            for (Map.Entry<CIString, Integer> entry : valRef.entrySet()) {
                Main.logger.info("[" + entry.getKey() + "]->[" + entry.getValue() + "]");
            }
        }
    }

}

public class TableReferences {

    private final HashMap<ReferenceType, TableReference> tabRefs;
    SqliteAccessor m_accessor;
    private HashSet<ReferenceType> doNotSave;

    TableReferences(SqliteAccessor m_accessor) {
        tabRefs = new HashMap();
        this.m_accessor = m_accessor;
        doNotSave = new HashSet<>();

    }

    public void Finalize() throws SQLException {
        for (TableReference tabRef : tabRefs.values()) {
            try {
                if (!doNotSave.contains(tabRef.getType())) {
                    tabRef.Finalize(m_accessor, false);
                } else {
                    tabRef.Finalize(m_accessor, true);
                }
                m_accessor.Commit();
            } catch (Exception sQLException) {
                Main.logger.error("Not able to finalize " + tabRef.getType().toString(), sQLException);
                for (Map.Entry<TableReference.CIString, Integer> entry : tabRef.getValRef().entrySet()) {
                    Main.logger.info("[" + entry.getKey() + "]->[" + entry.getValue() + "]");
                }
            }
        }

    }

    public Integer getRef(ReferenceType type, String key) {
        if (key == null || key.length() == 0) {
            return null;
        }
        TableReference tr = tabRefs.get(type);

        if (tr == null) {
            tr = new TableReference(type);
            tabRefs.put(type, tr);
        }
        return tr.getRef(key);
    }

    public Integer getRef(ReferenceType type, String key, int wordsToCompare) {
        if (key == null || key.length() == 0) {
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
