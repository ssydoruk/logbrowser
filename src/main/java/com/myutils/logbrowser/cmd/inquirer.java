package com.myutils.logbrowser.cmd;

import com.myutils.logbrowser.indexer.ReferenceType;
import java.io.File;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;

public class inquirer {

    public static Logger logger;
    static inquirer inq;
    static EnvInquirer ee;
    private static Pattern filePathPattern = null;

    private static int doSearchDB(String dbname) {
        int ret = 1;
        boolean dbNamePrinted = false;
        try {
            inq = new inquirer();

            if (!inq.initReport(dbname)) {
                return ret;
            }

            ArrayList<String> tables = ee.getTables();
            ArrayList<ReferenceType> tablesToSearch = new ArrayList<>();
            if (tables == null) { // all tables
                for (ReferenceType t : Arrays.stream(ReferenceType.values()).toList()) {
                    if (t != ReferenceType.UNKNOWN) {
                        tablesToSearch.add(t);
                    }
                }
                logger.debug("List of tables - all");
            } else {
                for (String tab : tables) {
                    List<ReferenceType> t = getSubRefType(tab);
                    if (!t.isEmpty()) {
                        tablesToSearch.addAll(t);
                    }
                }
                logger.debug("List of tables: " + StringUtils.join(tablesToSearch, ", "));
            }
            for (ReferenceType tab : tablesToSearch) {
                String[] refNames = inq.dbConnector.getRefNames(tab, ee.getSearch(), ee.isRegex());
                if (ArrayUtils.isNotEmpty(refNames)) {
                    ret = 0;
                    if (!dbNamePrinted) {
                        System.out.println("Database: " + dbname);
                        dbNamePrinted = true;
                    }
                    System.out.println("Found in tab [" + tab.toString() + "]");
                    for (String name : refNames) {
                        System.out.println("\t-" + name);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(StringUtils.join(e.getStackTrace(), "\n"));
        } finally {
            inq.dbConnector.GracefulClose();
        }
        return ret;
    }

    DBConnector dbConnector;

    public inquirer() {
    }

    public static String getVersion() {
        try {
            String name = inquirer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()
                    .substring(1);
            try (JarFile jarStream = new JarFile(name)) {
                Manifest mf = jarStream.getManifest();
                Attributes attr = mf.getMainAttributes();
                return attr.getValue("version");
            }
        } catch (IOException | URISyntaxException e) {
            //
        }

        return "";
    }

    public static void main(String[] args) throws Exception {
        int ret = 1; // assume not found

        ee = new EnvInquirer();
        ee.parserCommandLine(args);

        logger = LogManager.getLogger("inquirer");
        StringBuilder s = new StringBuilder();
        s.append("Command line: ").append(StringUtils.join(args, " "));
        s.append("\n\tCurrent directory: ").append(Utils.FileUtils.getCurrentDirectory());
        if (StringUtils.isNotBlank(ee.getDbname())) {
            s.append("\n\tChecking database dbname: ").append(ee.getDbname());
        } else {
            s.append("\n\tSearching directory: ").append(ee.getStartDirectory());
        }

        logger.info(s);

        if (StringUtils.isNotBlank(ee.getDbname())) {
            ret=doSearchDB(ee.getDbname());
        } else {
            try {
                if (StringUtils.isNotBlank(ee.getFilePathPattern())) {
                    filePathPattern = Pattern.compile(ee.getFilePathPattern());
                }
                File f = new File(ee.getStartDirectory());
                if (f.isDirectory()) {
                    ScanDir(f);
                } else {
                    doSearchDB(ee.getDbname());
                }
            } catch (Exception exception) {
                logger.error("Exception while scanning", exception);
            }
        }
        System.exit(ret);
    }

    static private void ScanDir(File dir) throws Exception {
        logger.debug("Processing directory " + dir.getAbsolutePath());
        File[] filesInDir = dir.listFiles();
        for (File filesInDir1 : filesInDir) {
            if (filesInDir1.isFile()) {
                logger.debug("Checking file [" + filesInDir1.getName() + "] against match [" + ee.getFileNamePattern() + "]");
                if (FilenameUtils.wildcardMatch(filesInDir1.getName(), ee.getFileNamePattern())) {
                    if (filePathPattern != null) {
                        logger.debug("Checking file path [" + filesInDir1.getAbsolutePath() + "] against match [" + filePathPattern.toString() + "]");
                        if (!filePathPattern.matcher(filesInDir1.getAbsolutePath()).find()) {
                            logger.debug("skipping");
                            continue;
                        }
                    }
                    doSearchDB(filesInDir1.getAbsolutePath());
                }
            } else if (filesInDir1.isDirectory()) {
                ScanDir(filesInDir1);
            } else {
                logger.info("[" + filesInDir1.getAbsolutePath() + "] is not regular file or directory, skippping");
            }
        }
    }

    private static List<ReferenceType> getSubRefType(String tab) {
        ArrayList<ReferenceType> ret = new ArrayList<>();
        for (ReferenceType t : Arrays.stream(ReferenceType.values()).toList()) {
            if (StringUtils.containsIgnoreCase(t.toString(), tab)) {
                ret.add(t);
            }
        }
        return ret.stream().toList();
    }

    public static EnvInquirer getEe() {
        return ee;
    }

    static public int[] toArray(ArrayList<Integer> list) {
        int[] ret = new int[list.size()];
        int i = 0;
        for (int j = 0; j < list.size(); j++) {
            ret[j] = list.get(j);
        }
        return ret;
    }

    private boolean initReport(String dbname) throws Exception {
        logger.debug("using db: " + ee.getDbname());

        try {
            Path dbPath = Paths.get(dbname);
            String name = dbPath.getFileName().toString();
            Path m_currentPath = dbPath.getParent();
            Path m_realPath = m_currentPath;
            dbPath = m_realPath.resolve(name + ".db");

            dbConnector = new DBConnector(Paths.get(dbname).toString());

            String query = "SELECT name FROM file_logbr;";
            try (ResultSet rs = dbConnector.executeQuery(query)) {
                if (rs.next()) {
                    String pathname = rs.getString("name");
                    Path m_origPath = Paths.get(pathname);
                }
            }

        } catch (Exception e) {

            logger.error("Cannot init", e);
            return false;
        }
        return true;
    }

}
