package com.myutils.logbrowser.cmd;

import com.myutils.logbrowser.indexer.ReferenceType;
import com.myutils.logbrowser.inquirer.QueryDialogSettings;
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

public class inquirer {

    public static Logger logger;
    static inquirer inq;
    static EnvInquirer ee;
    private static QueryDialogSettings queryDialogSettings = null;

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
        int ret = -1; // assume not found
        boolean dbNamePrinted=false;
        try {

            ee = new EnvInquirer();
            ee.parserCommandLine(args);

            logger = LogManager.getLogger("inquirer");
            StringBuilder s = new StringBuilder();
            s.append("Command line: ").append(StringUtils.join(args, " "));
            s.append("\n\tCurrent directory: ").append(Utils.FileUtils.getCurrentDirectory());
            s.append("\n\tdbname: ").append(ee.getDbname());

            logger.info(s);

            inq = new inquirer();

            if (!inq.initReport()) {
                return;
            }

            ArrayList<String> tables = ee.getTables();
            ArrayList<ReferenceType> tablesToSearch = new ArrayList<>();
            if (tables == null) { // all tables
                for (ReferenceType t : Arrays.stream(ReferenceType.values()).toList())
                    if (t != ReferenceType.UNKNOWN)
                        tablesToSearch.add(t);
                logger.debug("List of tables - all");
            } else {
                for (String tab : tables) {
                    List<ReferenceType> t = getSubRefType(tab);
                    if (!t.isEmpty())
                        tablesToSearch.addAll(t);
                }
                logger.debug("List of tables: " + StringUtils.join(tablesToSearch, ", "));
            }
            for (ReferenceType tab : tablesToSearch) {
                String[] refNames = inq.dbConnector.getRefNames(tab, ee.getSearch(), ee.isRegex());
                if (ArrayUtils.isNotEmpty(refNames)) {
                    ret = 0;
                    if(!dbNamePrinted){
                        System.out.println("Database: "+ee.getDbname());
                    }
                    System.out.println("Found in tab [" + tab.toString() + "]");
                    for (String name : refNames) {
                        System.out.println("\t-" + name);
                    }
                    System.out.println("<---");
                }
            }
        } catch (Exception e) {
            System.out.println(StringUtils.join(e.getStackTrace(), "\n"));
        } finally {
            inq.dbConnector.GracefulClose();
        }
        System.exit(ret);
    }

    private static List<ReferenceType> getSubRefType(String tab) {
        ArrayList<ReferenceType> ret = new ArrayList<>();
        for (ReferenceType t : Arrays.stream(ReferenceType.values()).toList()) {
            if (StringUtils.containsIgnoreCase(t.toString(), tab))
                ret.add(t);
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

    private boolean initReport() throws Exception {
        logger.info("using db: " + ee.getDbname());

        try {
            Path dbPath = Paths.get(ee.getDbname());
            String name = dbPath.getFileName().toString();
            Path m_currentPath = dbPath.getParent();
            Path m_realPath = m_currentPath;
            dbPath = m_realPath.resolve(name + ".db");

            dbConnector = new DBConnector(Paths.get(ee.getDbname()).toString());

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
