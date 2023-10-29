/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;

/**
 * @author ssydoruk
 */
public class PathManager {

    private static Path m_origPath = null; // path to the directory where db was parsed
    private static Path m_realPath; // path to actual db directory
    private static Path m_currentPath; // path from where invoked: $(CURRENT_DIRECTORY)

    public static int Init(String dbName, String alias) throws Exception {

        Path dbPath = Paths.get(dbName);
        String name = dbPath.getFileName().toString();
        m_currentPath = dbPath.getParent();
        m_realPath = m_currentPath;
        dbPath = m_realPath.resolve(name + ".db");
//        while (!Files.exists(dbPath)) {
//            m_realPath = m_realPath.getParent();
//            dbPath = m_realPath.resolve(name+".db");
//        }
//        dbPath = m_realPath.resolve(name);

        DatabaseConnector.initDatabaseConnector(Paths.get(dbName).toString(), alias);

        // using dbPath as owner, it's temporary anyway
        DatabaseConnector dbConnector = DatabaseConnector.getDatabaseConnector(dbPath);
        String query = "SELECT name FROM file_" + alias + " WHERE id=1;";
        try (ResultSet rs = dbConnector.executeQuery(dbPath, query) ) {
            if (rs.next()) {
                String pathname = rs.getString("name");
                m_origPath = Paths.get(pathname);
            }
        }
        dbConnector.releaseConnector(dbPath);

        Integer[] iDs = DatabaseConnector.getIDs("select count(*) from file_" + alias + " WHERE apptypeid>0;");
        if (iDs != null && iDs.length > 0) {
            return iDs[0];
        }
        return 0;

    }

    public static String GetRelativePath(String fullName) {
        if (m_origPath != null) {
            Path file = Paths.get(fullName);
            // get relative to original root
            Path origRel = m_origPath.relativize(file);
            // add to real root
            Path real = m_realPath.resolve(origRel);
            // get relative to current
            Path realRel = m_currentPath.relativize(real);
            return realRel.toString();
        } else {
            return fullName;
        }
    }

    public static String GetAbsolutePath(String fullName) {
        return fullName;
//        Path file = Paths.get(fullName);
//        // get relative to original root
//        Path origRel = m_origPath.relativize(file);
//        // add to real root
//        Path real = m_realPath.resolve(origRel);
//        return real.toString();
    }

    public static String GetPathFromRoot(String name) {
        Path path = m_realPath.resolve(name);
        return path.toString();
    }
}
