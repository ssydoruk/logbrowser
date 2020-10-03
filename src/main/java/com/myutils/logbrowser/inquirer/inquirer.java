package com.myutils.logbrowser.inquirer;

import Utils.ScreenInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import com.myutils.logbrowser.indexer.FileInfoType;
import com.myutils.logbrowser.indexer.Main;
import static com.myutils.logbrowser.indexer.Main.setCurrentDirectory;
import com.myutils.logbrowser.inquirer.gui.LogFileManager;
import com.myutils.logbrowser.inquirer.gui.MySwingWorker;
import com.myutils.logbrowser.inquirer.gui.RequestProgress;
import com.myutils.logbrowser.inquirer.gui.WindowsSystemUtility;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import static org.apache.commons.io.FilenameUtils.getFullPath;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class inquirer {

    static XmlCfg cfg;
    private static String curDirectory;
    private static String logBrDirAbs;

    private static String logBrDir;
    private static ZoneId zoneId = ZoneId.systemDefault();
    private static HashMap<String, DateTimeFormatter> dateFormatters = new HashMap<String, DateTimeFormatter>();
    private static Main parser = null;
    public static boolean refSyncDisabled;
    static Boolean isAll = null;
    public static Logger logger;
    private static String DBPath;
    static HashMap<Integer, String> fileidType = null;
    static HashMap<String, ArrayList<Pattern>> printFiltersPatterns = null;
    static inquirer inq;
    private static final boolean dbg = "true".equalsIgnoreCase(System.getProperty("application.debug"));
    private static String config = null;
    private final static String serFile = ".logbr_dialog.ser";
    private static QueryDialogSettings queryDialogSettings = null;
    static ArrayList<IQueryResults> queries = new ArrayList<IQueryResults>();
    static QueryDialog dlg;
    private static InquirerCfg cr = null;
    private final static String serFileDef = ".logbr_checkedref.ser";
    private static boolean useXMLSerializer = false;

    public static XmlCfg getXMLCfg() {
        return cfg;
    }

    public static String getWD() {
        return System.getProperty("user.dir");
    }

    static String getDialogTitle(String title) {
        String wd = getWD();
        String prefix = "";
        String titleRegex = getCr().getTitleRegex();
        if (titleRegex != null && titleRegex.length() > 0) {
            try {
                Pattern compile = Pattern.compile(titleRegex);
                Matcher m;
                if ((m = compile.matcher(wd)).find()) {
                    prefix = m.group(1);
                }
            } catch (Exception e) {
            }
        }
        if (prefix.length() == 0) {
            prefix = wd;
        }
        return prefix + " - " + title;
    }

    static String UTCtoDateTime(String fieldValue, String dtFormat) {
        try {
            long ts = Long.parseLong(fieldValue);
            DateTimeFormatter format = getDTFormat(dtFormat);
            ZonedDateTime atZone = (Instant.ofEpochMilli(ts)).atZone(zoneId);
            return format.format(atZone);
        } catch (NumberFormatException numberFormatException) {
            return "";
        }
    }

    private static DateTimeFormatter getDTFormat(String dtFormat) {
        DateTimeFormatter get = dateFormatters.get(dtFormat);
        if (get == null) {
            get = DateTimeFormatter.ofPattern(dtFormat);
            dateFormatters.put(dtFormat, get);
        }
        return get;
    }

    public static int showConfirmDialog(Component object, String string, String title, int YES_NO_CANCEL_OPTION) {
        logger.debug("showConfirmDialog [" + title + "]: " + string);
//        synchronized (RequestProgress.dialogStarted) {
//            try {
//                RequestProgress.dialogStarted.wait(500);
//            } catch (InterruptedException ex) {
//                java.util.logging.logger.log(org.apache.logging.log4j.Level.FATAL, ex);
//            }
        return JOptionPane.showConfirmDialog(object, string, title, YES_NO_CANCEL_OPTION);
//        }
    }

    static void reParse() throws Exception {
        if (parser == null) {
            parser = new Main(DBPath, config);
        }
//        parser.parseAll();

    }

    public static boolean isRefSyncDisabled() {
        return refSyncDisabled;
    }

    public static void showInfoPanel(Window p, String t, Container jScrollPane, boolean isModal) {
//        JDialog allFiles = new JDialog(parent, title);
        InfoPanel allFiles = new InfoPanel(p, t, jScrollPane, JOptionPane.DEFAULT_OPTION);

        try {
//            Dimension d = jScrollPane.getPreferredSize();

//            allFiles.setPreferredSize(d);
        } catch (Exception ex) {
            ExceptionHandler.handleException("", ex);

        }

        allFiles.pack();
//        ScreenInfo.CenterWindow(allFiles);
        allFiles.setModal(isModal);
        ScreenInfo.setVisible(p, allFiles, true);
    }

    public static int showYesNoPanel(Window p, String t, JComponent bannerPannel, Container jScrollPane, int buttonOptions) {
//        JDialog allFiles = new JDialog(parent, title);
        InfoPanel infoDialog = new InfoPanel(p, t, bannerPannel, jScrollPane, buttonOptions);

        try {
//            Dimension d = jScrollPane.getPreferredSize();

//            allFiles.setPreferredSize(d);
        } catch (Exception ex) {
            ExceptionHandler.handleException("", ex);

        }
        infoDialog.showModal();

        return infoDialog.getDialogResult();
    }

    public static boolean ifAll() {
        if (isAll == null) {
            String sIsAll = (String) System.getProperties().get("all");
            isAll = (sIsAll != null && sIsAll.equals("1"));
        }
        return isAll;
    }

    public static String getVersion() throws IOException {
        try {
            String name = inquirer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().substring(1);
            JarFile jarStream = new JarFile(name);
            Manifest mf = jarStream.getManifest();
            Attributes attr = mf.getMainAttributes();
            return attr.getValue("version");
        } catch (IOException | URISyntaxException e) {
        }

        return "";
    }

    public static InquirerCfg getConfig() {
        return LoadConfig(config);
    }

    private static InquirerCfg LoadConfig(String configPath) {
        try {
            return getCr(configPath);
//            if (configPath != null && !configPath.isEmpty()) {
//                logger.debug("Loading config for " + configPath);
//                Properties props = new Properties();
//                FileInputStream file = new FileInputStream(configPath);
//                props.load(file);
//                file.close();
//                return props;
//            }
        } catch (Exception e) {
            ExceptionHandler.handleException("Error reading config", e);
        }

        return null;
    }

    public static String getFirstWord(String text) {
        if (text.indexOf(' ') > -1) { // Check if there is more than one word.
            return text.substring(0, text.indexOf(' ')); // Extract first word.
        } else {
            return text; // Text is the first word itself.
        }
    }

    public static String commonString(String s1, String s2) {

        String[] s1split = s1.split("\\s|\\.|\\;");
        String[] s2split = s2.split("\\s|\\.|\\;");
        ArrayList<String> updatedStored = new ArrayList<>();
        for (String string : s1split) {
            if (ArrayUtils.contains(s2split, string)) {
                updatedStored.add(string);
            } else {
                updatedStored.add("<name>");
            }
        }
        StringBuilder ret = new StringBuilder();
        for (String string : updatedStored) {
            if (ret.length() > 0) {
                ret.append(' ');
            }
            ret.append(string);

        }
        return ret.toString();

    }

    static public void saveCR() throws IOException {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        boolean jsonWritten = false;

        if (config.toLowerCase().endsWith(".json")) {
            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(config), "UTF-8"))) {
                Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();
                gson.toJson(cr, bufferedWriter);
                jsonWritten = true;

            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(inquirer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!jsonWritten) {
            try {
                fos = new FileOutputStream(config);
                if (useXMLSerializer) {
                    try (XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(fos))) {
                        encoder.writeObject(cr);
                    }
                } else {
                    GZIPOutputStream gz = new GZIPOutputStream(fos);
                    out = new ObjectOutputStream(gz);
                    out.writeObject(cr);
                    out.close();
                }

//            fos = new FileOutputStream(configXML);
//            XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(fos));
//            encoder.writeObject(cr);
//            encoder.close();
            } catch (IOException ex) {
                ExceptionHandler.handleException("error saving settings", ex);
            }
        }

    }

    public static ArrayList<Pattern> getFullFilters(int GetFileId) throws SQLException, Exception {
        if (fileidType == null) {
            fileidType = new HashMap<>();
            ArrayList<Integer> ret = new ArrayList();
            DatabaseConnector connector = DatabaseConnector.getDatabaseConnector(inquirer.class);

            try (ResultSet resultSet = connector.executeQuery(inquirer.class,
                    "select f.id, a.name from file_logbr f\n"
                    + " inner join apptype a on a.id=f.apptypeid")) {
                ResultSetMetaData rsmd = resultSet.getMetaData();

                int cnt = 0;
                while (resultSet.next()) {
                    fileidType.put(resultSet.getInt(1), resultSet.getString(2));
                    QueryTools.DebugRec(resultSet);
                    cnt++;
                }
                inquirer.logger.debug("\tRetrieved " + cnt + " records");
            }

            if (getCr().isApplyFullFilter()) {
                HashMap<String, String> printFilters = getCr().getPrintFilters();
                printFiltersPatterns = new HashMap<>(printFilters.size());
                for (String fileType : printFilters.keySet()) {
                    String strFilters = printFilters.get(fileType);
                    if (strFilters != null && strFilters.length() > 0) {
                        String[] split = strFilters.split("[\r\n]+");
                        if (split != null && split.length > 0) {
                            ArrayList<Pattern> pts = new ArrayList(split.length);
                            for (String string : split) {
                                if (string != null && string.length() > 0 && !string.startsWith("#")) {
                                    pts.add(Pattern.compile(string));
                                }
                            }
                            printFiltersPatterns.put(fileType, pts);
                        }
                    }
                }
            }
        }
        String fileID = fileidType.get(GetFileId);
        if (fileID != null && printFiltersPatterns != null) {
            return printFiltersPatterns.get(fileID);
        }
        return null;
    }

    public static boolean isDbg() {
        return dbg;
    }

    public static void main(String[] args) throws IOException, Exception {
        try {

            curDirectory = Paths.get(".").toAbsolutePath().normalize().toString();

            logBrDir = System.getProperty("logbr.dir");
            if (logBrDir != null && logBrDir.length() > 0) {
                System.setProperty("logPath", logBrDir);
            } else {
                logBrDir = curDirectory + File.pathSeparator + ".logbr";
                System.setProperty("logPath", logBrDir);
            }

            logBrDirAbs = Paths.get(logBrDir).toAbsolutePath().normalize().toString();

            System.setProperty("log4j2.saveDirectory", "true");

            logger = LogManager.getLogger("inquirer");
            logger.info("starting");

//        DialogItem d1=DialogItem.OCS_CALL_DATABASE;
//        DialogItem d2=DialogItem.OCS_CALL_SCXML;
//        if( d1==d2)
//            logger.info("equal");
//        else
//            logger.info("not equal");
            inq = new inquirer();
            inq.ParseArgs(args);

            // parse command line parameters, then
            // create proper classes and invoke corresponding 
            Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
            System.setProperty("sun.awt.exception.handler",
                    ExceptionHandler.class.getName());

            if (!inq.initReport()) {
                return;
            }

//            Runnable run = new Runnable() {
//                public void run() {
//                    try {
            inq.RunGUI();
//                    } catch (Exception ex) {
//                        java.util.logging.logger.log(org.apache.logging.log4j.Level.FATAL, ex);
//                    }
//                }
//            };
//            SwingUtilities.invokeLater(run);
//            synchronized(run){
//                run.wait();
//                System.out.println("done waiting");
//            }
//            SwingUtilities.invokeAndWait(run);

        } finally {
            WindowsSystemUtility.closeApp();
            DatabaseConnector.GracefulClose();
            inq.lfm.clear();
        }
        System.exit(0);
    }

    public static String getLogBrDirAbs() {
        return logBrDirAbs;
    }

    public static String getFullLogName(String _fileName) {
        return FilenameUtils.normalize(FilenameUtils.concat(getCurDirectory(), _fileName));
    }

    public static String getCurDirectory() {
        return curDirectory;
    }

    public static String getLogBrDir() {
        return logBrDir;
    }

    public static QueryDialogSettings geLocaltQuerySettings() {
        if (queryDialogSettings == null) {
            queryDialogSettings = (QueryDialogSettings) readObj(getSerFile());
            if (queryDialogSettings == null) {
                queryDialogSettings = new QueryDialogSettings();
            }
        }
        return queryDialogSettings;
    }

    public static <T> T readObj(String file) {
        T ret = null;

        Path path = Paths.get(file);
        if (Files.exists(path)) {
            FileInputStream fis = null;
            ObjectInputStream in = null;
            try {
                fis = new FileInputStream(file);
                if (useXMLSerializer) {
                    XMLDecoder decoder = null;
                    decoder = new XMLDecoder(new BufferedInputStream(fis));
                    ret = (T) decoder.readObject();
                    decoder.close();
                } else {
                    GZIPInputStream gz = new GZIPInputStream(fis);
                    in = new ObjectInputStream(gz);
                    ret = (T) in.readObject();
                    in.close();
                }
            } catch (IOException ex) {
                ExceptionHandler.handleException("IOException reading settings", ex);
                ret = null;
//            } catch (ClassNotFoundException ex) {
//                logger.error("ClassNotFoundException reading settings", ex);
//                queryDialigSettings = null;
            } catch (ClassNotFoundException ex) {
                ExceptionHandler.handleException("Exception reading settings", ex);
                ret = null;
            }
        }
        return ret;
    }

    private static void ShowGui() throws Exception {

//        queryDialigSettings = readObject(serFile, <T>o);
        queryDialogSettings = geLocaltQuerySettings();

        if (DatabaseConnector.fileExits(FileInfoType.type_CallManager)) {
            queries.add(new CallFlowResults(queryDialogSettings));
        }

        if (ifAll()) {
            if (DatabaseConnector.fileExits(new FileInfoType[]{FileInfoType.type_URS, FileInfoType.type_ORS,
                FileInfoType.type_URSHTTP})) {
                queries.add(new RoutingResults());
            }
            if (DatabaseConnector.fileExits(FileInfoType.type_OCS)) {
                queries.add(new OutboundResults());
            }
            if (DatabaseConnector.fileExits(FileInfoType.type_StatServer)) {
                queries.add(new StatServerResults(queryDialogSettings));
            }
            if (DatabaseConnector.fileExits(FileInfoType.type_IxnServer)) {
                queries.add(new IxnServerResults(queryDialogSettings));
            }
            if (DatabaseConnector.fileExits(new FileInfoType[]{FileInfoType.type_RM, FileInfoType.type_MCP})) {
                queries.add(new MediaServerResults(queryDialogSettings));
            }
            if (DatabaseConnector.fileExits(new FileInfoType[]{FileInfoType.type_WorkSpace, FileInfoType.type_SIPEP})) {
                queries.add(new WorkspaceResults(queryDialogSettings));
            }
            if (DatabaseConnector.fileExits(FileInfoType.type_VOIPEP)) {
                queries.add(new VOIPEPResults(queryDialogSettings));
            }

            if (DatabaseConnector.fileExits(FileInfoType.type_SCS)) {
                queries.add(new SCServerResults(queryDialogSettings));
            }
            if (DatabaseConnector.fileExits(FileInfoType.type_LCA)) {
                queries.add(new LCAServerResults(queryDialogSettings));
            }

            if (DatabaseConnector.fileExits(FileInfoType.type_DBServer)) {
                queries.add(new DBServerResults(queryDialogSettings));
            }
            if (DatabaseConnector.fileExits(FileInfoType.type_ConfServer)) {
                queries.add(new ConfServerResults(queryDialogSettings));
            }
            if (DatabaseConnector.fileExits(new FileInfoType[]{FileInfoType.type_WWE, FileInfoType.type_ApacheWeb})) {
                queries.add(new WWEResults(queryDialogSettings));
            }
            if (DatabaseConnector.fileExits(FileInfoType.type_WWECloud)) {
                queries.add(new WWECloudResults(queryDialogSettings));
            }

            if (DatabaseConnector.fileExits(FileInfoType.type_GMS)) {
                queries.add(new GMSResults(queryDialogSettings));
            }
        }

        dlg = new QueryDialog(queries);
        logger.info("Created dlgs");
//        /*invokeAndWait - synchronous so that dlg is initialized and we can wait for it in main thread*/
//        SwingUtilities.invokeAndWait(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    dlg = new QueryDialog(queries, id);
//                    logger.info("Created dlgs");
//                } catch (Exception ex) {
//                    java.util.logging.logger.log(org.apache.logging.log4j.Level.FATAL, ex);
//                }
//            }
//        });

//        dlg.addAggrPane();
//        return dlg;
    }

    public static inquirer getInq() {
        return inq;
    }

    static public void showError(
            Component parentComponent,
            Object message,
            String title) {
        logger.error("Error message " + title + "[" + message + "]");

        JTextArea jt = new JTextArea(message.toString(), 40, 100);
        jt.setEditable(false);
        JScrollPane jsp = new JScrollPane(jt);
//            JTextArea.setl
        JOptionPane.showMessageDialog(parentComponent, message, title, JOptionPane.ERROR_MESSAGE);

    }

    static public int[] toArray(ArrayList<Integer> list) {
        int[] ret = new int[list.size()];
        int i = 0;
        for (Iterator<Integer> it = list.iterator();
                it.hasNext();
                ret[i++] = it.next());
        return ret;
    }

    static void saveObject(String fileName, QueryDialogSettings obj) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;

        try {
            fos = new FileOutputStream(fileName);
            if (useXMLSerializer) {
                XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(fos));
                encoder.writeObject(obj);
                encoder.close();
            } else {
                GZIPOutputStream gz = new GZIPOutputStream(fos);
                out = new ObjectOutputStream(gz);
                out.writeObject(obj);
                out.close();
            }
        } catch (IOException ex) {
            ExceptionHandler.handleException("error saving settings", ex);
        }

    }

    public static String getSerFile() {
        return inquirer.getLogBrDir() + File.separator + serFile;
    }

    public static InquirerCfg getCr() {
        return getCr(serFileDef);
    }

    public static InquirerCfg getCr(String file) {
        if (cr == null) {
            File f = new File(file);
            if (f.exists()) {
                boolean crRead = false;
                if (f.getAbsolutePath().toLowerCase().endsWith(".json")) {

                    try (InputStreamReader streamReader = new InputStreamReader(new FileInputStream(file), "UTF-8")) {
                        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
                                .create();
                        cr = gson.fromJson(streamReader, com.myutils.logbrowser.inquirer.InquirerCfg.class);
                        crRead = true;

                    } catch (IOException | com.google.gson.JsonSyntaxException ex) {
                        logger.error("", ex);
                    }
                    if (!crRead) {
                        try {
                            if (useXMLSerializer) {
                                try (XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(f)))) {
                                    cr = (com.myutils.logbrowser.inquirer.InquirerCfg) decoder.readObject();
                                }
                            } else {
                                try (GZIPInputStream gz = new GZIPInputStream(new FileInputStream(f))) {
                                    ObjectInputStream in = new ObjectInputStream(gz);
                                    cr = (com.myutils.logbrowser.inquirer.InquirerCfg) in.readObject();
                                }
                            }
                        } catch (IOException ex) {
                            ExceptionHandler.handleException("IOException reading settings", ex);
                            queryDialogSettings = null;
//            } catch (ClassNotFoundException ex) {
//                logger.error("ClassNotFoundException reading settings", ex);
//                queryDialigSettings = null;
                        } catch (ClassNotFoundException ex) {
                            ExceptionHandler.handleException("Exception reading settings", ex);
                            queryDialogSettings = null;
                        }
                    }

                }
                if (cr == null) {//default hardcoded settings
                    cr = new InquirerCfg();
                } else {
                    cr.LoadRefs();
                }
            }
        }
        return cr;

    }

    public static void getRefSettings() {

    }

    static public boolean matchFound(String val, Pattern pt, String search, boolean matchWholeWordSelected) {
//        logger.debug("search cell:[" + val + "] [" + search + "] " + matchWholeWordSelected + " " + " " + pt + ": [" + val);
        if (val != null && !val.isEmpty()) {
//                            inquirer.logger.debug("search cell:" + search + " " + matchWholeWordSelected + " " + " " +pt + ": [" + val);
            if (pt != null) {
                if (pt.matcher(val).find()) {
                    return true;
                }
            } else {
                if (matchWholeWordSelected) {
                    if (val.equalsIgnoreCase(search)) {
                        return true;
                    }
                } else {
                    if (val.toLowerCase().contains(search)) {
                        return true;
                    }

                }

            }
        }
        return false;
    }

    public static void checkIntr() {
        if (Thread.currentThread().isInterrupted()) {
            throw new RuntimeInterruptException();
        }
    }
    private String baseDir;
    boolean ignoreFileAccessErrors = false;
    private ArrayList<ILogRecordFormatter> formatters = new ArrayList();
    String alias = "";
    String dbname = "";
    String outputSpecFile = null;
    IQueryResults queryResults = null;
    String startTime = "";
    String endTime = "";
    CommandLineReader clr;
    private final LogFileManager lfm = new LogFileManager();

    public ArrayList getFormatters() throws Exception {
        for (ILogRecordFormatter formatter : formatters) {
            formatter.refreshFormatter();
        }
        return formatters;
    }

    public boolean isIgnoreFileAccessErrors() {
        return ignoreFileAccessErrors;
    }

    private void ParseArgs(String[] args) throws IOException {

        ExecutionEnvironment theClr = new ExecutionEnvironment(args);

        alias = theClr.getAlias();
        dbname = theClr.getDBName();
        outputSpecFile = theClr.getXmlCfg();
        config = theClr.getAppConfigFile();
        baseDir = theClr.getBaseDir();
        refSyncDisabled = theClr.isDisableRefSync();
        ignoreFileAccessErrors = theClr.isIgnoreFileAccessErrors();

//
//        
//        for (int i = 0; i < args.length; i++) {
//
//            String currentStr = args[i];
//            if (currentStr.toLowerCase().startsWith("alias=")) {
//                alias = currentStr.substring(6);
//            } else if (currentStr.toLowerCase().startsWith("dbname=")) {
//                dbname = currentStr.substring(7);
//            } else if (currentStr.toLowerCase().startsWith("connid=")) {
//                connid = currentStr.substring(7);
//            } else if (currentStr.toLowerCase().startsWith("callid=")) {
//                callid = currentStr.substring(7);
//            } else if (currentStr.toLowerCase().startsWith("cg=")) {
//                cg = currentStr.substring("cg=".length());
//            } else if (currentStr.toLowerCase().startsWith("id=")) {
//                id = currentStr.substring("id=".length());
//            } else if (currentStr.toLowerCase().startsWith("dn=")) {
//                dn = currentStr.substring(3);
//            } else if (currentStr.toLowerCase().startsWith("agent=")) {
//                agent = currentStr.substring(6);
//            } else if (currentStr.toLowerCase().startsWith("pseudologname=")) {
//                pseudologExplicitName = currentStr.substring(14);
//            } else if (currentStr.toLowerCase().startsWith("outputspec=")) {
//                outputSpecFile = currentStr.substring(11);
//            } else if (currentStr.toLowerCase().equals("terror")) {
//                tlibError = true;
//            } else if (currentStr.toLowerCase().startsWith("siperror=")) {
//                if (currentStr.length() > 27) {
//                    int ind = currentStr.indexOf(' ');
//                    if (ind > -1) {
//                        startTime = currentStr.substring(9, ind);
//                        endTime = currentStr.substring(ind + 1);
//                    } else {
//                        startTime = currentStr.substring(9);
//                    }
//                }
//                sipError = true;
//            } else if (currentStr.toLowerCase().equals("terrorcalls")) {
//                tlibErrorCall = true;
//            } else if (currentStr.toLowerCase().startsWith("siperrorcalls=")) {
//                if (currentStr.length() > 32) {
//                    int ind = currentStr.indexOf(' ');
//                    if (ind > -1) {
//                        startTime = currentStr.substring(14, ind);
//                        endTime = currentStr.substring(ind + 1);
//                    } else {
//                        startTime = currentStr.substring(14);
//                    }
//                }
//                sipErrorCall = true;
//            } else if (currentStr.toLowerCase().startsWith("time=")) {
//                if (currentStr.length() > 23) {
//                    int ind = currentStr.indexOf(' ');
//                    if (ind > -1) {
//                        startTime = currentStr.substring(5, ind);
//                        endTime = currentStr.substring(ind + 1);
//                    } else {
//                        startTime = currentStr.substring(14);
//                    }
//                }
//            } else if (currentStr.toLowerCase().startsWith("config=")) {
//                config = currentStr.substring(7);
//                configXML = config + ".xml";
//            } else if (currentStr.toLowerCase().equals("pseudolog")) {
//                pseudolog = true;
//            } else if (currentStr.toLowerCase().equals("pseudonavigate")) {
//                pseudoNavigate = true;
//            } else if (currentStr.toLowerCase().equals("all")) {
//                all = true;
//            } else if (currentStr.toLowerCase().equals("fullpath")) {
//                shortFileName = false;
//            } else if (currentStr.toLowerCase().startsWith("mark=")) {
//                mark = currentStr.substring(5);
//            } else if (currentStr.toLowerCase().startsWith("filter=")) {
//                filter = currentStr.substring(7);
//            } else if (currentStr.toLowerCase().equals("kazimir")) {
//                kazimir = true;
//            } else if (currentStr.toLowerCase().equals("sccm")) {
//                sccm = true;
//            } else if (currentStr.toLowerCase().equals("sccmtc")) {
//                sccmtc = true;
//            } else if (currentStr.toLowerCase().equals("sccmprx")) {
//                sccm = true;
//                proxy = true;
//            } else if (currentStr.toLowerCase().equals("icon")) {
//                icon = true;
//            } else if (currentStr.toLowerCase().equals("isccsip")) {
//                isccsip = true;
//            } else if (currentStr.toLowerCase().equals("state")) {
//                dnstate = true;
//            } else if (currentStr.toLowerCase().equals("rout")) {
//                routing = true;
//            } else if (currentStr.toLowerCase().equals("outb")) {
//                outbound = true;
//            } else if (currentStr.toLowerCase().equals("outbcall")) {
//                outboundCall = true;
//            } else if (currentStr.toLowerCase().equals("gui")) {
//                gui = true;
//            } else if (currentStr.toLowerCase().equals("outbagent")) {
//                outboundAgent = true;
//            } else if (currentStr.toLowerCase().startsWith("stat=")) {
//                filter = currentStr.substring(5);
//                stat = true;
//            } else {
//                logger.error("Not parsed paremeter [" + currentStr + "]");
//                InvalidDbParameters();
//                return;
//            }
//        }
////        QueryDialog q = new QueryDialog();
////        q.show();
//        if (alias.isEmpty()) {
//            InvalidDbParameters();
//            return;
//        }
//        if (dbname.isEmpty()) {
//            dbname = "test";
//        }
//
//        if (connid.contains("@")) {
//            callid = connid;
//            useCallId = true;
//        }
//
//        if (!tlibError && !sipError && !all && connid.isEmpty() && dn.isEmpty() && agent.isEmpty()
//                && !tlibErrorCall && !sipErrorCall) {
//            if (!callid.isEmpty()) {
//                useCallId = true;
//
//            }
//            /*else
//             {
//             InvalidDbParameters();
//             return;
//             }*/
//
//        }
//        // convert filter
//        String[] vals = filter.toUpperCase().split(",");
//        for (String s : vals) {
//            if (s.equals("SIP")) {
//                components.add("CM"); // call manager
//                components.add("SP"); // SIP proxy
//                components.add("SC"); // session controller
//                components.add("TC"); // tController
//                components.add("IP"); // interaction proxy
//            } else {
//                components.add(s);
//            }
//            if (s.equals("URS")) {
//                components.add("URS");
//                components.add("ORS");
//                components.add("ORSM");
//                urs = true;
//            }
//            if (s.equals("STS")) {
//                sts = true;
//            }
//            if (s.equals("CL")) {
//                custom = true;
//            }
//            if (s.equals("ICON")) {
//                useIcon = true;
//            }
//        }
//        if (stat) {
//            components.add("SC");
//        }
    }

    private boolean initReport() throws Exception {
        //DatabaseConnector.initDatabaseConnector(dbname,alias);
        if (!setCurrentDirectory(baseDir)) {
            logger.error("Cannot cd to directory [" + baseDir + "]. Exiting");
            System.exit(1);
        }
        if (!dbname.startsWith(File.separator)) {
            dbname = FilenameUtils.concat(baseDir, dbname);
        }
        logger.info("using db: " + dbname);

        int filesInDB = 0;
        try {
            filesInDB = PathManager.Init(dbname, alias);
        } catch (Exception e) {
            logger.error("Cannot init", e);
            JOptionPane.showMessageDialog(null, "Not able to init:\n" + e.getMessage(), "Cannot continue", JOptionPane.ERROR_MESSAGE);
//            inquirer.ExceptionHandler.handleException("Not able to init:\n", e);
            return false;
        }
        if (filesInDB <= 0) {
            JOptionPane.showMessageDialog(null, "Nothing to do,  no parsed files in DB found.\nApplication is terminating", "Cannot continue", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        DBPath = getFullPath(dbname);
        if (DBPath != null && DBPath.length() == 0) {
            DBPath = "./";
        }
        logger.info("Using database " + dbname + " path=" + DBPath);
        LoadConfig(config);

//        try {
//            System.setProperty("user.dir", DBPath);
//        } catch (Exception e) {
//            ExceptionHandler.handleException("Not able to change current directory to " + DBPath, e);
//        }
//        logger.debug("working directory: " + Paths.get(".").toAbsolutePath().normalize().toString());
//        if (all || pseudolog) {
//            // create pseudolog formatter and add it to formatter list
//            PseudoLogFormatter pseudoLogFormatter = new PseudoLogFormatter(
//                    pseudologExplicitName, pseudoNavigate, null);
//            ILogRecordFormatter formatter = pseudoLogFormatter;
//            formatters.add(formatter);
//            if (mark != null) {
//                pseudoLogFormatter.ApplyMark(mark);
//            }
//        }
        if (outputSpecFile != null && !outputSpecFile.isEmpty()) {

            logger.debug("About to read XML config from file " + outputSpecFile);
            cfg = new XmlCfg(outputSpecFile);

            // create formatter class by output spec file
            ILogRecordFormatter formatter = new OutputSpecFormatterScreen(
                    cfg, true, null);
            formatters.add(formatter);
        }

//        if (outputSpecFile == null) {
//            if (outputSpecFile == null) {
//                // create default formatter
//                ILogRecordFormatter formatter = new DefaultFormatter(!shortFileName, null);
//                formatters.add(formatter);
//            }
//        }
        return true;
    }

    public void doRetrieve(IQueryResults queryResults, QueryDialog dlg) throws Exception {
        for (int i = 0; i < formatters.size(); i++) {
            queryResults.AddFormatter(
                    (ILogRecordFormatter) formatters.get(i));
        }

        ILogRecord.resetTotalBytes();

        Instant time3 = Instant.now();

        queryResults.Retrieve(dlg);
        inquirer.logger.info("Retrieved " + queryResults.m_results.size() + " records. Took " + Utils.Util.pDuration(Duration.between(time3, Instant.now()).toMillis()));
    }

    public void doPrint(IQueryResults queryResults, QueryDialog dlg, InquirerCfg cfg) throws Exception {
        int dialogResult = JOptionPane.YES_OPTION;
        boolean doPrint = true;
        PrintStreams ps = new PrintStreams();

        if (getCr().getMaxRecords() < queryResults.m_results.size()) {
            int dialogButton = JOptionPane.YES_NO_CANCEL_OPTION;
            dialogResult = inquirer.showConfirmDialog(null, "Number of records seems large ("
                    + getCr().getMaxRecords() + "/" + queryResults.m_results.size()
                    + "). Do you want to print on screen(Yes), to files only (No), or Cancel?", "Warning", dialogButton);
            if (dialogResult == JOptionPane.CANCEL_OPTION) {
                doPrint = false;
            }
        }

        Instant timePrintStart = Instant.now();

        if (doPrint) {

            if (dialogResult != JOptionPane.NO_OPTION) {
                ps.addShortStream(System.out, getCr().getMaxRecords());
            }
//            if (cfg.isSaveFileLong()) {
//                String fileName = cfg.getFileNameLong();
//                inquirer.logger.info("Long output goes to " + fileName);
//                ps.addFullStream(fileName, -1, cfg.isAddShortToFull());
//            }
            if (cfg.isSaveFileShort()) {
                String fileName = cfg.getFileNameShort();
                inquirer.logger.info("Short output goes to " + fileName);
                ps.addShortStream(fileName, -1);
            }
            queryResults.Print(ps);
            ps.closeStreams();
        }

        for (int i = 0; i < formatters.size(); i++) {
            ((ILogRecordFormatter) formatters.get(i)).Close();
        }
        if (doPrint) {
            inquirer.logger.info("Printing took " + Utils.Util.pDuration(
                    Duration.between(timePrintStart, Instant.now()).toMillis()
            ));
        }
        ps.OpenNotepad("openfiles.bat");
    }

    public LogFileManager getLfm() {
        return lfm;
    }

    private void RunGUI() throws Exception {
//<editor-fold defaultstate="collapsed" desc="Query thread definition">
        class QueryTask extends MySwingWorker<Void, String> {

            private RequestProgress rp = null;

            public QueryTask() {
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                rp.addProgress(chunks);
            }

            public void setRp(RequestProgress rp) {
                this.rp = rp;
            }

            @Override
            protected Void myDoInBackground() throws Exception {
                ShowGui();
                return null;
            }

            @Override
            protected void done() {
                inquirer.logger.debug("swingworker done");
                rp.dispose();
                if (isCancelled()) {
                    JOptionPane.showMessageDialog(null, "Query was cancelled", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    dlg.CenterWindow();
//        dlg.setModal(false);

                    ScreenInfo.setVisible(dlg, true);
//                    dlg.setVisible(true);

                }
            }
        }
        ;
        //</editor-fold>
        QueryTools.queryMessagesClear();
        QueryTask tsk = new QueryTask();
        RequestProgress rp = new RequestProgress(null, true, tsk);
        tsk.setRp(rp);
        tsk.execute();
        rp.doShow();
        //        ShowGui(id);
        synchronized (dlg) {
            dlg.wait();
        }
        saveObject(getSerFile(), queryDialogSettings);
    }

    public static class InfoPanel extends StandardDialog {

        private Container mainPanel;
        private final int buttonOptions;
        private JComponent bannerPannel = null;

        private InfoPanel(Window p, String t, JComponent bannerPannel, Container jScrollPane, int YES_NO_OPTION) {
            this(p, t, jScrollPane, YES_NO_OPTION);
            this.bannerPannel = bannerPannel;
        }

        public InfoPanel(Window parent, String title, Container jScrollPane, int buttonOptions) throws HeadlessException {
            super(parent, title);
            this.mainPanel = jScrollPane;
            this.buttonOptions = buttonOptions;
        }

        public void setMainPanel(Container mainPanel) {
            this.mainPanel = mainPanel;
        }

        @Override
        public JComponent createBannerPanel() {
//            return new BannerPanel("pannel tytle", "descrr");
//            if( bannerPannel!=null){
//                bannerPannel = new BannerPanel("pannel tytle", "descrr");
//                return bannerPannel;
//            }
            return bannerPannel;
        }

        @Override
        public JComponent createContentPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

            panel.add(mainPanel, BorderLayout.CENTER);
            return panel;
        }

        @Override
        public ButtonPanel createButtonPanel() {
            ButtonPanel buttonPanel = new ButtonPanel();
            switch (buttonOptions) {

                case JOptionPane.DEFAULT_OPTION: {
                    JButton cancelButton = new JButton();
                    buttonPanel.addButton(cancelButton);

                    cancelButton.setAction(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDialogResult(RESULT_CANCELLED);
                            setVisible(false);
                            dispose();
                        }
                    });
                    cancelButton.setText("Close");

                    setDefaultCancelAction(cancelButton.getAction());
                    getRootPane().setDefaultButton(cancelButton);
                    break;
                }

                case JOptionPane.YES_NO_CANCEL_OPTION: {

                    JButton yesButton = new JButton();
                    buttonPanel.addButton(yesButton);

                    yesButton.setAction(new AbstractAction("Yes") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDialogResult(JOptionPane.YES_OPTION);
                            setVisible(false);
                            dispose();
                        }
                    });

                    JButton noButton = new JButton();
                    buttonPanel.addButton(noButton);

                    noButton.setAction(new AbstractAction("No") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDialogResult(JOptionPane.NO_OPTION);
                            setVisible(false);
                            dispose();
                        }
                    });

                    JButton cancelButton = new JButton();
                    buttonPanel.addButton(cancelButton);

                    cancelButton.setAction(new AbstractAction("Cancel") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDialogResult(JOptionPane.CANCEL_OPTION);
                            setVisible(false);
                            dispose();
                        }
                    });

                    setDefaultCancelAction(cancelButton.getAction());
                    getRootPane().setDefaultButton(noButton);
                    break;
                }

                case JOptionPane.YES_NO_OPTION: {
                    JButton yesButton = new JButton();
                    buttonPanel.addButton(yesButton);

                    yesButton.setAction(new AbstractAction("Yes") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDialogResult(JOptionPane.YES_OPTION);
                            setVisible(false);
                            dispose();
                        }
                    });

                    JButton noButton = new JButton();
                    buttonPanel.addButton(noButton);

                    noButton.setAction(new AbstractAction("No") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDialogResult(JOptionPane.NO_OPTION);
                            setVisible(false);
                            dispose();
                        }
                    });
                    setDefaultCancelAction(noButton.getAction());
                    getRootPane().setDefaultButton(noButton);
                    break;
                }
                case JOptionPane.OK_CANCEL_OPTION: {
                    JButton yesButton = new JButton();
                    buttonPanel.addButton(yesButton);

                    yesButton.setAction(new AbstractAction("OK") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDialogResult(JOptionPane.OK_OPTION);
                            setVisible(false);
                            dispose();
                        }
                    });

                    JButton noButton = new JButton();
                    buttonPanel.addButton(noButton);

                    noButton.setAction(new AbstractAction("Cancel") {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setDialogResult(JOptionPane.CANCEL_OPTION);
                            setVisible(false);
                            dispose();
                        }
                    });
                    setDefaultCancelAction(noButton.getAction());
                    getRootPane().setDefaultButton(noButton);
                    break;

                }
            }
//            buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            buttonPanel.setSizeConstraint(ButtonPanel.NO_LESS_THAN); // since the checkbox is quite wide, we don't want all of them have the same size.
            return buttonPanel;
        }

        public void showModal() {
            pack();
//        ScreenInfo.CenterWindow(allFiles);
            setModal(true);
            ScreenInfo.setVisible(getParent(), this, true);
        }
    }

    private static class CommandLineReader {

        private final Options options = null;
        private final CommandLine cl = null;

        public CommandLineReader() {
        }

        public String getOptionValue(String opt) {
            return cl.getOptionValue(opt);
        }

        public String getOptionValue(String opt, String defaultValue) {
            return cl.getOptionValue(opt, defaultValue);
        }

        public String[] getArgs() {
            return cl.getArgs();
        }

        public java.util.List<String> getArgList() {
            return cl.getArgList();
        }

        public void printHelp() {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("indexer [params] <working directory>", "header", options, "footer");
        }

        private boolean errorArgs() {
            return cl == null;
        }
    }

    private static class ExecutionEnvironment {

        private final Option optOutSpec;
        private final Option optAlias;
        private final Option optDBName;
        private final Option optDisableRefSync;
        private final Option optLogsBaseDir;
        private final Option optAppConfigFile;
        private CommandLine cmd;
        private final CommandLineParser parser;
        private final Option optIgnoreFileAccessErrors;
        private Options options = null;
        private final CommandLine cl = null;
        Option optHelp;

        ExecutionEnvironment(String[] args1) {
            options = new Options();

            optHelp = Option.builder("h")
                    .hasArg(false)
                    .required(false)
                    .desc("Show help and exit")
                    .longOpt("help")
                    .build();
            options.addOption(optHelp);

            optIgnoreFileAccessErrors = Option.builder()
                    .hasArg(false)
                    .required(false)
                    .desc("If set, ignore file access errors")
                    .longOpt("ignore-file-errors")
                    .build();
            options.addOption(optIgnoreFileAccessErrors);

            optOutSpec = Option.builder("x")
                    .hasArg(true)
                    .required(true)
                    .desc("XML config file for output formatting")
                    .valueSeparator('=')
                    .longOpt("outputspec")
                    .build();
            options.addOption(optOutSpec);

            optAlias = Option.builder("a")
                    .hasArg(true)
                    .required(false)
                    .desc("Alias to use")
                    .valueSeparator('=')
                    .longOpt("alias")
                    .build();
            options.addOption(optAlias);

            optDBName = Option.builder("d")
                    .hasArg(true)
                    .required(false)
                    .desc("Name of the SQLite database file")
                    .valueSeparator('=')
                    .longOpt("dbname")
                    .build();
            options.addOption(optDBName);

            optDisableRefSync = Option.builder()
                    .hasArg(false)
                    .required(false)
                    .desc("If set, disable sync of references; to be used if database is large")
                    .longOpt("no-ref-sync")
                    .build();
            options.addOption(optDisableRefSync);

            optLogsBaseDir = Option.builder("b")
                    .hasArg(true)
                    .required(false)
                    .desc("Basic directory for logs. Default to current")
                    .longOpt("basedir")
                    .valueSeparator('=')
                    .build();
            options.addOption(optLogsBaseDir);

            optAppConfigFile = Option.builder("c")
                    .hasArg(true)
                    .required(true)
                    .desc("Configuration file for the application")
                    .longOpt("config")
                    .valueSeparator('=')
                    .build();
            options.addOption(optAppConfigFile);

            parser = new DefaultParser();

            //        theTest();
            logBrDir = System.getProperty("logbr.dir");
            if (logBrDir != null && logBrDir.length() > 0) {
                System.setProperty("logPath", logBrDir);
            } else {
                System.setProperty("logPath", ".");
            }
            System.setProperty("log4j2.saveDirectory", "true");

            String cmdLine = "";
            for (String arg : args1) {
                cmdLine += arg + " ";
            }
            logger.info("parameters:" + cmdLine);
            logger.info("working directory: " + Paths.get(".").toAbsolutePath().normalize().toString());

            parserCommandLine(args1);

        }

        public String[] getArgs() {
            return cl.getArgs();
        }

        public java.util.List<String> getArgList() {
            return cl.getArgList();
        }

        public void printHelp() {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("indexer [params] <working directory>", "header", options, "footer");
        }

        private boolean errorArgs() {
            return cl == null;
        }

        private void showHelpExit(Options options) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("utility-name", options);

            System.exit(0);
        }

        private String getAlias() {
            return getStringOrDef(optAlias, "logbr");
        }

        private boolean isDisableRefSync() {
            return cmd.hasOption(optDisableRefSync.getLongOpt());
        }

        private String getDBName() {
            return getStringOrDef(optDBName, "logbr");
        }

        private String getXmlCfg() {
            return getStringOrDef(optOutSpec, "");
        }

        private String getBaseDir() {
            return getStringOrDef(optLogsBaseDir, Paths.get(".").toAbsolutePath().normalize().toString());
        }

        private String getAppConfigFile() {
            return getStringOrDef(optAppConfigFile, "");
        }

        private boolean getBoolOptionOrDef(Option opt, boolean def) {
            boolean ret = cmd.hasOption(opt.getLongOpt());
            return (ret ? true : def);
        }

        private String getStringOrDef(Option opt, String def) {
            String ret = null;
            try {
                ret = (String) cmd.getParsedOptionValue(opt.getLongOpt());
                ret = (String) cmd.getParsedOptionValue(opt.getOpt());
            } catch (ParseException ex) {
                logger.log(org.apache.logging.log4j.Level.FATAL, ex);
            }
            if (ret == null || ret.isEmpty()) {
                return def;
            } else {
                return ret;
            }
        }

        private void parserCommandLine(String[] args) {
            try {
                cmd = parser.parse(options, args, true);
            } catch (ParseException e) {
                System.out.println(e.getMessage());
                showHelpExit(options);
            }

            if (cmd.hasOption(optHelp.getLongOpt())) {
                showHelpExit(options);
            }
        }

        private boolean isIgnoreFileAccessErrors() {
            return getBoolOptionOrDef(optIgnoreFileAccessErrors, false);
        }
    }

    public static class ExceptionHandler implements Thread.UncaughtExceptionHandler {

        static public void handleException(String t, Throwable e, String errs) {
            String msg = "Uncaught Exception in thread '" + t + "'";
            logger.error(msg, e);
            StringWriter sw = new StringWriter();
            sw.append("msg: " + t);
            sw.append("\nCall stack:\n");
            e.printStackTrace(new PrintWriter(sw));
            if (errs != null) {
                sw.append("\n").append(errs).append("\n");
            }

            JTextArea jt = new JTextArea(sw.toString(), 40, 100);
            jt.setEditable(false);
            JScrollPane jsp = new JScrollPane(jt);
//            JTextArea.setl
            JOptionPane.showMessageDialog(null, jsp, "Exception", JOptionPane.ERROR_MESSAGE);
        }

        static public void handleException(String t, Throwable e) {
            handleException(t, e, null);
        }

        public void handle(Throwable thrown) {
            // for EDT exceptions
            handleException(Thread.currentThread().getName(), thrown);
        }

        @Override
        public void uncaughtException(Thread thread, Throwable thrown) {
            // for other uncaught exceptions
            handleException(thread.getName(), thrown);
        }
    }

}
