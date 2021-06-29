/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.io.File;
import java.io.IOException;
import java.sql.*;

/**
 * @author ssydoruk
 */
public class LogDB {

    Connection m_conn;
    PreparedStatement m_prepFind;
    PreparedStatement m_prepInsert;
    PreparedStatement m_prepUpd;

    String m_lastFile = "";
    boolean m_wasFound = false;

    public LogDB(String dbName) throws Exception {
        Class.forName("org.sqlite.JDBC");
        m_conn = DriverManager.getConnection("jdbc:sqlite:" + dbName + ".db");
        Statement stmt = m_conn.createStatement();
        stmt.executeUpdate("CREATE TABLE if not exists FILES "
                + "(id INTEGER PRIMARY KEY ASC,"
                + "name char(255),"
                + "offset bigint,"
                + "line integer);");
        stmt.executeUpdate("CREATE INDEX if not exists FILES_id "
                + "ON FILES (id);");
        stmt.executeUpdate("CREATE INDEX if not exists FILES_name "
                + "ON FILES (name);");

        m_prepFind = m_conn.prepareStatement("SELECT * FROM FILES "
                + "WHERE FILES.name=?");
        m_prepInsert = m_conn.prepareStatement("INSERT INTO FILES VALUES"
                + "(NULL,?,?,?);");
        m_prepUpd = m_conn.prepareStatement("UPDATE FILES SET "
                + "offset=?,line=? WHERE name=?;");

        m_conn.setAutoCommit(false);
    }

    public FileEntry GetInfo(File file) {
        m_wasFound = false;
        FileEntry fe = new FileEntry();
        fe.Offset = 0;
        fe.Line = 0;
        try {
            m_lastFile = file.getCanonicalPath();
            m_prepFind.setString(1, m_lastFile);
            try (ResultSet res = m_prepFind.executeQuery()) {
                Main.logger.debug("Checking for " + m_lastFile + " sql:" + m_prepFind.toString());
                if (res.next()) {
                    fe.Offset = res.getLong("offset");
                    fe.Line = res.getInt("line");
                    m_wasFound = true;
                    Main.logger.debug("Found file " + m_lastFile + " off: " + fe.Offset + " line: " + fe.Line);
                }
            }
        } catch (IOException | SQLException e) {
            Main.logger.error("Could not fetch info for file " + file.getName()
                    + ": " + e, e);
        }
        return fe;
    }

    public void SetInfo(File file, FileEntry info) {
        try {
            boolean use_update;
            if (file.getCanonicalPath() == null ? m_lastFile == null : file.getCanonicalPath().equals(m_lastFile)) {
                use_update = m_wasFound;
            } else {
                // this should be never called
                m_prepFind.setString(1, file.getCanonicalPath());
                try (ResultSet res = m_prepFind.executeQuery()) {
                    use_update = res.next();
                }
            }

            if (use_update) {
                m_prepUpd.setLong(1, info.Offset);
                m_prepUpd.setInt(2, info.Line);
                m_prepUpd.setString(3, file.getCanonicalPath());
                m_prepUpd.addBatch();
            } else {
                m_prepInsert.setString(1, file.getCanonicalPath());
                m_prepInsert.setLong(2, info.Offset);
                m_prepInsert.setInt(3, info.Line);
                m_prepInsert.addBatch();
            }
        } catch (IOException | SQLException e) {
            Main.logger.error("Could not store information for file: " + e, e);
        }
    }

    public void Close() {
        try {
            m_prepInsert.executeBatch();
            m_prepUpd.executeBatch();
            Main.logger.debug("Commit4");
            m_conn.commit();
            m_conn.close();
        } catch (SQLException e) {
            Main.logger.error("Could not finalize logs DB: " + e, e);
        }
    }

    public static class FileEntry {

        public long Offset;
        public int Line;
    }
}
