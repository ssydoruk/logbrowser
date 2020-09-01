/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static Utils.Util.pDuration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author ssydoruk
 */
public class Main {

    static Main theParser = null;

    private static final Constants constants = new Constants();
    public static SqliteAccessor m_accessor;
    static String m_component = "all";
    static String m_executableName = "indexer.jar";
    public static Logger logger;
    private static String logBrDir;
    private static boolean ignoreZIP = false;
    public static ExecutionEnvironment clr;
    static long totalBytes = 0;
    private static final Pattern regFilesNotGood = Pattern.compile("(^\\.logbr|logbr.db)");
    static Boolean isAll = null;

    static String lookupConstant(GenesysConstants constName, Integer intOrDef) {
        if (intOrDef != null) {
            HashMap<Integer, String> get = constants.get(constName);
            if (get != null) {
                String ret = get.get(intOrDef);
                if (StringUtils.isNotBlank(ret)) {
                    return ret;
                }
            }

        }
        return null;
    }

    static public Main getMain() {
        return theParser;
    }

    public static SqliteAccessor getM_accessor() {
        return m_accessor;
    }

    public static SqliteAccessor getSQLiteaccessor() {
        return m_accessor;
    }

    public static void PrintUsage() {
        System.out.print("Usage: " + m_executableName
                + " -DbName name"
                + " [-Alias alias]"
                + " [-Regexp regexp]"
                + " [-Logdb name]"
                + " [-component sc | tc | sip | all ]"
                + " [-config config]"
                + " file1|dir1 [file2|dir2 ...]\n");
    }

    public static String getVersion() throws IOException {
//        try {
//            String name = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().substring(1);
//            JarFile jarStream = new JarFile(name);
//            Manifest mf = jarStream.getManifest();
//            Attributes attr = mf.getMainAttributes();
//            return attr.getValue("version");
//        } catch (Exception e) {
//            System.out.println("Error: "+e);
//            Main.logger.error(e);
//        }
        return "";
    }

    static void theTest() {
        String s = "20/02/2018 11:41:03 a.m..557_I_I_017302afa531b580 [09:06] >>>>>>>>>>>>start interp()";
        Pattern p = Pattern.compile("^\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2} ([am\\.]+)\\.\\d{3}");
        Matcher m = p.matcher(s);
        System.out.println(m.replaceFirst("$1"));

        DateTimeFormatter formatter
                = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss a.SSS");
        LocalDate date = LocalDate.parse("20/02/2018 11:41:03 a.m..557", formatter);
        System.out.printf("%s%n", date);

        System.exit(0);
    }

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException, Exception {

        /*
        //<editor-fold defaultstate="collapsed" desc="standard log parsing">
        for (int i = 0; i < args.length;) {
        String currentStr = args[i];
        if (currentStr.equalsIgnoreCase("/alias")
        || currentStr.equalsIgnoreCase("-alias")) {
        if (i + 1 == args.length || !alias.isEmpty()) {
        System.out.println("Invalid alias.");
        PrintUsage();
        return;
        }
        alias = args[i + 1];
        i += 2;
        
        } else if (currentStr.equalsIgnoreCase("/dbname")
        || currentStr.equalsIgnoreCase("-dbname")) {
        if (i + 1 == args.length || !dbname.isEmpty()) {
        System.out.println("Invalid DbName.");
        PrintUsage();
        return;
        }
        dbname = args[i + 1];
        i += 2;
        
        } else if (currentStr.equalsIgnoreCase("/cfgxml")
        || currentStr.equalsIgnoreCase("-cfgxml")) {
        if (i + 1 == args.length || !xmlCFG.isEmpty()) {
        System.out.println("Invalid xmlCFG.");
        PrintUsage();
        return;
        }
        xmlCFG = args[i + 1];
        i += 2;
        
        } else if (currentStr.equalsIgnoreCase("/logdb")
        || currentStr.equalsIgnoreCase("-logdb")) {
        if (i + 1 == args.length || !logdb.isEmpty()) {
        System.out.println("Invalid logs database file.");
        PrintUsage();
        return;
        }
        logdb = args[i + 1];
        i += 2;
        
        } else if (currentStr.equalsIgnoreCase("/component")
        || currentStr.equalsIgnoreCase("-component")) {
        if (i + 1 != args.length) {
        m_component = args[i + 1];
        if (!m_component.equalsIgnoreCase("all")
        && !m_component.equalsIgnoreCase("sip")
        && !m_component.equalsIgnoreCase("sc")
        && !m_component.equalsIgnoreCase("tc")
        && !m_component.equalsIgnoreCase("proxy")) {
        PrintUsage();
        return;
        }
        }
        i += 2;
        } else if (currentStr.equalsIgnoreCase("/config")
        || currentStr.equalsIgnoreCase("-config")) {
        if (i + 1 == args.length) {
        PrintUsage();
        return;
        }
        String configFile = args[i + 1];
        try {
        if (!configFile.isEmpty()) {
        Properties config = new Properties();
        FileInputStream file = new FileInputStream(configFile);
        config.load(file);
        file.close();
        
        //                        String dirs = config.getProperty("ScanSubdirs");
        //                        if (dirs != null && dirs.equalsIgnoreCase("on")) {
        //                            m_scanDir = true;
        //                        }
        //
        //                        String zip = config.getProperty("ParseZip");
        //                        if (zip != null && zip.equalsIgnoreCase("on")) {
        //                            m_parseZip = true;
        //                        }
        }
        } catch (IOException e) {
        }
        i += 2;
        
        } else {
        scanDir = currentStr;
        i++;
        }
        }
        
        if (alias.isEmpty()) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        LocalDateTime date = new LocalDateTime();
        alias = dateFormat.format(date);
        }

        if (dbname.isEmpty()) {
        System.out.println("DbName required.");
        PrintUsage();
        return;
        }

        if (scanDir == null || scanDir.isEmpty()) {
        System.out.println("File or dir required.");
        PrintUsage();
        return;
        }

        if (logdb.isEmpty()) {
        logdb = "logdb"; //default value
        }
        //</editor-fold>
         */
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        System.setProperty("sun.awt.exception.handler",
                ExceptionHandler.class.getName());

        clr = new ExecutionEnvironment();
        logger = LogManager.getLogger("indexer");

        String cmdLine = "";
        for (String arg : args) {
            cmdLine += arg + " ";
        }
        logger.info("parameters:" + cmdLine);
        logger.info("working directory: " + Paths.get(".").toAbsolutePath().normalize().toString());

        clr.parserCommandLine(args);
        logger.info("params: parsing timediff: " + Parser.isParseTimeDiff());

        String alias = clr.getAlias();
        String dbname = clr.getDBName();
        File f = new File(dbname);
        if (!f.isAbsolute()) {
            dbname = f.getAbsolutePath();
        }

        ignoreZIP = clr.isIgnoreZIP();

        String xmlCFG = clr.getXmlCfg();
        String baseDir = clr.getBaseDir();

        theParser = new Main(dbname, xmlCFG);

        theParser.parseAll(baseDir, dbname, alias);
//        Thread.sleep(3000);
    }

    public static String getLogBrDir() {
        return logBrDir;
    }

    public static String formatSize(long v) {
        if (v < 1024) {
            return v + " B";
        }
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    public static Integer getRef(ReferenceType type, String name) {
        Integer ret = getMain().tabRefs.getRef(type, name);
        Main.logger.trace("getRef for [" + type + "] key:[" + name + "]" + " ret=" + ret);

        return ret;
    }

    public static Integer getRef(ReferenceType type, String name, int wordsToCompare) {
        Integer ret = getMain().tabRefs.getRef(type, name, wordsToCompare);
        Main.logger.trace("getRefLog key:[" + name + "]" + " ret=" + ret);

        return ret;
    }

    static boolean IgnoreTable(Message msg) {
        return theParser.checkIgnoreTable(msg);
    }

    static int getRefQuotes(ReferenceType referenceType, String m_ThisDN) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static boolean fileOK(File fileInfo) {
        if (regFilesNotGood.matcher(fileInfo.getName()).find()) {
            return false;
        }
        return true;
    }

    private static void extendFilesList(ArrayList<FileInfo> filesToProcess, LogFileWrapper newLog) throws IOException {
        if (newLog != null) {
            for (FileInfo fileInfo : newLog.getFileInfos()) {
                logger.debug("adding file to process: [" + fileInfo.getM_path() + "] log name [" + fileInfo.getLogFileName() + "]");
                for (FileInfo fiExisting : filesToProcess) {
                    if (fiExisting.fileEqual(fileInfo)) {
                        Main.logger.warn("Duplicate file found: [" + fileInfo.getM_path() + "] vs [" + fiExisting.getM_path() + "]");
                        if (fiExisting.getSize() >= fileInfo.getSize()) {
                            Main.logger.warn("Ignored [" + fileInfo.getM_path() + "]");
                            return;
                        } else {
                            Main.logger.warn("Will use [" + fileInfo.getM_path() + "] as it is larger "
                                    + "(" + fileInfo.getSize()
                                    + "b " + formatSize(fileInfo.getSize())
                                    + " vs " + fiExisting.getSize() + "b " + formatSize(fiExisting.getSize()) + ")");
                            filesToProcess.remove(fiExisting);
                            fiExisting.setIgnoring();
                            break;
                        }
                    }
                }
                filesToProcess.add(fileInfo);
            }
        }
    }

    private static boolean myEqual(String logFileName, String logFileName0) {
        return logFileName != null && !logFileName.isEmpty()
                && logFileName0 != null && !logFileName0.isEmpty()
                && logFileName.equals(logFileName0);
    }

    static public boolean isDbExisted() {
        return Main.getMain().dbExisted;
    }

    private static void initStatic(SqliteAccessor m_accessor) throws Exception {
        Record.setFileId(m_accessor.getID("select max(id) from file_logbr;", 0));
        Record.m_handlerId = m_accessor.getID("select max(HandlerId) from sip_" + m_accessor.getM_alias() + ";", "sip_" + m_accessor.getM_alias(), 0);
        Record.m_handlerId = m_accessor.getID("select max(HandlerId) from tlib_" + m_accessor.getM_alias() + ";", "tlib_" + m_accessor.getM_alias(), Record.m_handlerId);
        Record.m_handlerId = m_accessor.getID("select max(HandlerId) from cireq_" + m_accessor.getM_alias() + ";", "cireq_" + m_accessor.getM_alias(), Record.m_handlerId);
        Record.m_handlerId++;
        Record.m_sipId = m_accessor.getID("select max(SipId) from trigger_" + m_accessor.getM_alias() + ";", "trigger_" + m_accessor.getM_alias(), 0) + 1;
        Record.m_tlibId = m_accessor.getID("select max(TlibId) from trigger_" + m_accessor.getM_alias() + ";", "trigger_" + m_accessor.getM_alias(), 0) + 1;
        Record.m_jsonId = m_accessor.getID("select max(JsonId) from trigger_" + m_accessor.getM_alias() + ";", "trigger_" + m_accessor.getM_alias(), 0) + 1;
    }

    public static boolean setCurrentDirectory(String directory_name) {
        boolean result = false;  // Boolean indicating whether directory was set
        File directory;       // Desired current working directory

        directory = new File(directory_name).getAbsoluteFile();
        if (directory.exists() || directory.mkdirs()) {
            result = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
        }

        return result;
    }

    public static boolean ifSIPLines() {
        String isAll = (String) System.getProperties().get("SIPLINES");
        if (isAll == null || isAll.length() == 0 || !isAll.equals("1")) {
            return false;
        }
        return true;
    }

    public static boolean isIgnoreZIP() {
        return ignoreZIP;
    }

    public static boolean ifAll() {
        if (isAll == null) {
            String sIsAll = (String) System.getProperties().get("all");
            isAll = (sIsAll != null && sIsAll.equals("1"));
        }
        return isAll;
    }

    private static void ParseZip(File file) {
        try {
            ZipFile logArchive = new ZipFile(file, ZipFile.OPEN_READ);
            Enumeration e = logArchive.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                InputStreamReader reader = new InputStreamReader(logArchive.getInputStream(entry));
                /*                BufferedReaderCrLf input = new BufferedReaderCrLf(reader);
                FileInfo fileInfo = new FileInfo();
                fileInfo.m_path = entry.getName();
                fileInfo.m_name = entry.getName();
                fileInfo.setFileFilter(m_component);
                Parser parser = null;
                int componentType = fileInfo.CheckLog(input);
                switch (componentType) {
                case FileInfo.type_SessionController:
                case FileInfo.type_InteractionProxy:
                case FileInfo.type_tController:
                parser = m_parsers.get(0);
                break;
                
                case FileInfo.type_SipProxy:
                case FileInfo.type_CallManager:
                case FileInfo.type_TransportLayer:
                if(m_component.equalsIgnoreCase("all")) {
                parser = m_parsers.get(0);
                } else {
                parser = m_parsers.get(1);
                }
                break;
                
                case FileInfo.type_URS:
                parser = m_parsers.get(2);
                break;
                
                case FileInfo.type_StatServer:
                parser = m_parsers.get(3);
                break;
                
                case FileInfo.type_ICON:
                parser = m_parsers.get(4);
                break;
                
                case FileInfo.type_ORS:
                parser = m_parsers.get(5);
                break;
                
                default:
                return;
                }
                
                fileInfo.AddToDB(m_accessor);
                LogDB.FileEntry logEntry = m_logDB.GetInfo(file);
                if (logEntry.Offset >= entry.getSize()) {
                System.out.println("Nothing new to parse: " + entry.getName());
                return;
                }
                InputStreamReader parsingReader = new InputStreamReader(logArchive.getInputStream(entry));
                BufferedReaderCrLf parsingInput = new BufferedReaderCrLf(parsingReader);
                int lines = parser.ParseFrom(parsingInput, logEntry.Offset, logEntry.Line);
                parsingInput.close();
                logEntry.Line += lines;
                logEntry.Offset = entry.getSize();
                m_logDB.SetInfo(file, logEntry);
                System.out.println("Parsed " + lines + " lines in " + entry.getName());
                
                input.close();
                 */
            }
        } catch (IOException e) {
            System.out.println("ERROR parsing zip archive: " + e);
            return;
        }
    }
    private final String dbname;
    private int totalFiles = 0;
    private boolean dbExisted = false;

    HashMap<FileInfoType, Parser> m_parsers = new HashMap<>();
//    static SqliteAccessor m_accessor; 

    private FileInfoTable m_FileInfoTable;

    private final ArrayList<File> m_files = new ArrayList();
    int m_current;
    private HashMap<TableType, DBTable> m_tables;

    boolean m_scanDir = true;
    boolean m_parseZip = false;

    public XmlCfg xmlCfg = null;

    TableReferences tabRefs;
    ArrayList<FileInfo> filesToAccess = new ArrayList();

    public Main(String dbname, String xmlCFG) throws IOException, Exception {
        setXMLCfg(xmlCFG);
        this.dbname = dbname;
    }

    private void ScanDir(File file) throws IOException {
        logger.info("Processing directory " + file.getAbsolutePath());
        File[] filesInDir = file.listFiles();
        for (File filesInDir1 : filesInDir) {
//            String fileType = Files.probeContentType(filesInDir[j].toPath());
//
//            logger.info(filesInDir[j].getName() + "- type:" + fileType);
            if (filesInDir1.isFile()) {
//                FileInfo fileInfo = getFileInfo(filesInDir[j]);
//                extendFilesList(filesToAccess, fileInfo);
                LogFileWrapper logFile = LogFileWrapper.getContainer(filesInDir1);
                if (logFile != null) {
                    extendFilesList(filesToAccess, logFile);
                }
            } else if (filesInDir1.isDirectory() && m_scanDir && !isLogBRDir(filesInDir1)) {
                ScanDir(filesInDir1);
            }
        }
    }

    private void Parse(FileInfo fileInfo) {
        Instant fileStart = Instant.now();
        Parser parser;
        DBTable tab = null;
        FileInfoType componentType = fileInfo.getM_componentType();
        if (componentType == FileInfoType.type_SessionController
                || componentType == FileInfoType.type_InteractionProxy
                || componentType == FileInfoType.type_tController
                || componentType == FileInfoType.type_CallManager
                || componentType == FileInfoType.type_TransportLayer) {
            parser = GetParser(FileInfoType.type_SessionController);
        } else {
            parser = GetParser(componentType);
        }
        if (parser != null) {
            parser.setFileInfo(fileInfo);

            int lines = 0;
            try {
                fileInfo.startParsing();
                try (InputStream inputStream = fileInfo.getInputStream()) {
                    BufferedReaderCrLf parsingInput = new BufferedReaderCrLf(inputStream);

                    lines = parser.ParseFrom(parsingInput, 0, 0, fileInfo);
                    parser.doneParsingFile();
                    fileInfo.setFileEndTime(parser.getLastTimeStamp());
                    m_FileInfoTable.AddToDB(fileInfo);
                    parsingInput.close();
                }

                fileInfo.doneParsing();
            } catch (Exception exception) {
                logger.error("Uncought exception while parsing: " + exception);
            }

            totalBytes += fileInfo.getSize();
            totalFiles++;
            logger.info("\tParsed " + lines + " lines, " + formatSize(fileInfo.getSize()) + ". Took "
                    + pDuration(Duration.between(fileStart, Instant.now()).toMillis())
            );
        } else {
            if (ifAll()) {
                logger.error("No parser for file [" + fileInfo.m_path + "] type: " + componentType + "; file skippped");
            }

        }
    }

    public BufferedReaderCrLf GetNextFile() {
        try {
            if (m_current < m_files.size() - 1) {
                return new BufferedReaderCrLf(new FileInputStream(m_files.get(m_current + 1)));
            }
            return null;
        } catch (FileNotFoundException e) {
            logger.error("Cannot get next file for continuous parsing: " + e, e);
            return null;
        }
    }

    private void InitTables() {

        DBTable t;
        m_FileInfoTable = new FileInfoTable(m_accessor);
        m_FileInfoTable.InitDB();
        m_FileInfoTable.setCurrentID(Record.getFileId());

        m_tables = new HashMap<>();

//        DBTable dbt = new ORSTable(m_accessor);
//        dbt.InitDB();
//        m_tables.add(dbt);
//        ORSTable tab1 = new ORSTable(m_accessor);
//        try {
//            tab1.FinalizeDB();
//        } catch (Exception ex) {
//            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
//        }
        m_tables.put(TableType.ConfigUpdate, new ConfigUpdateTable(m_accessor, TableType.ConfigUpdate));

        t = new TLibTable(m_accessor, TableType.TLib);
        t.setCurrentID(Record.m_tlibId);
        m_tables.put(TableType.TLib, t);
        m_tables.put(TableType.TLibProxied, new ProxiedTable(m_accessor, TableType.TLibProxied));

        m_tables.put(TableType.Handler, new HandlerTable(m_accessor, TableType.Handler));
        m_tables.put(TableType.CIFaceRequest, new CIFaceRequestTable(m_accessor, TableType.CIFaceRequest));
        m_tables.put(TableType.ISCC, new ISCCTable(m_accessor, TableType.ISCC));
        t = new SIPTable(m_accessor, TableType.SIP);
        t.setCurrentID(Record.m_sipId);
        m_tables.put(TableType.SIP, t);
        m_tables.put(TableType.ConnID, new ConnIDTable(m_accessor, TableType.ConnID));
        m_tables.put(TableType.Trigger, new TriggerTable(m_accessor, TableType.Trigger));
        t = new JsonTable(m_accessor, TableType.JSon);
        t.setCurrentID(Record.m_jsonId);
        m_tables.put(TableType.JSon, t);

        m_tables.put(TableType.URSWaiting, new URSWaitingTable(m_accessor, TableType.URSWaiting));

        m_tables.put(TableType.SIPMS, new SIPMSTable(m_accessor, TableType.SIPMS));
        m_tables.put(TableType.SIPEP, new SIPEPTable(m_accessor, TableType.SIPEP));
        m_tables.put(TableType.VOIPEP, new VOIPEPTable(m_accessor, TableType.VOIPEP));

        m_tables.put(TableType.OCSTLib, new OCSTable(m_accessor, TableType.OCSTLib));
        m_tables.put(TableType.OCSIxn, new OCSIxnTable(m_accessor, TableType.OCSIxn));
        m_tables.put(TableType.OCSClient, new OCSClientTable(m_accessor, TableType.OCSClient));
        m_tables.put(TableType.OCSHTTP, new OCSHTTPTable(m_accessor, TableType.OCSHTTP));
        m_tables.put(TableType.OCSCG, new OCSCGTable(m_accessor, TableType.OCSCG));
        m_tables.put(TableType.OCSDBActivity, new OCSDBActivityTable(m_accessor, TableType.OCSDBActivity));
        m_tables.put(TableType.OCSPAAgentInfo, new OCSPAAgentInfoTable(m_accessor, TableType.OCSPAAgentInfo));
        m_tables.put(TableType.OCSPASessionInfo, new OCSPASessionInfoTable(m_accessor, TableType.OCSPASessionInfo));
        m_tables.put(TableType.OCSStatEvent, new OCSStatEventTable(m_accessor, TableType.OCSStatEvent));
        m_tables.put(TableType.OCSPAEventInfo, new OCSPAEventInfoTable(m_accessor, TableType.OCSPAEventInfo));
        m_tables.put(TableType.OCSAssignment, new OCSAgentAssignmentTable(m_accessor, TableType.OCSAssignment));
        m_tables.put(TableType.OCSRecCreate, new OCSRecCreateTable(m_accessor, TableType.OCSRecCreate));
        m_tables.put(TableType.OCSSCXMLTreatment, new OCSSCXMLTreatmentTable(m_accessor, TableType.OCSSCXMLTreatment));
        m_tables.put(TableType.OCSSCXMLScript, new OCSSCXMLScriptTable(m_accessor, TableType.OCSSCXMLScript));
        m_tables.put(TableType.OCSTreatment, new OCSRecTreatmentTable(m_accessor, TableType.OCSTreatment));
        m_tables.put(TableType.OCSIcon, new OCSIconTable(m_accessor));

        m_tables.put(TableType.StSTEvent, new StSTEventTable(m_accessor, TableType.StSTEvent));
        m_tables.put(TableType.StSAction, new StSActionTable(m_accessor, TableType.StSAction));
        m_tables.put(TableType.StSRequestHistory, new StSRequestHistoryTable(m_accessor, TableType.StSRequestHistory));
        m_tables.put(TableType.StStatus, new StStatusTable(m_accessor, TableType.StStatus));
        m_tables.put(TableType.StCapacity, new StCapacityTable(m_accessor, TableType.StCapacity));
        m_tables.put(TableType.IxnSS, new IxnSSTable(m_accessor, TableType.IxnSS));

//        m_tables.put(new ORSEspTable(m_accessor));
//        m_tables.put(new ORSEventTable(m_accessor));
//        m_tables.put(new ORSLinkTable(m_accessor));
//        m_tables.put(new ORSLogTable(m_accessor));
        m_tables.put(TableType.ORSUrs, new OrsUrsTable(m_accessor, TableType.ORSUrs));
        m_tables.put(TableType.ORSTlib, new ORSTable(m_accessor, TableType.ORSTlib));
        m_tables.put(TableType.ORSSidUUID, new ORSSidUUIDTable(m_accessor, TableType.ORSSidUUID));
        m_tables.put(TableType.ORSSidIxnID, new ORSSidIxnIDTable(m_accessor, TableType.ORSSidIxnID));
        m_tables.put(TableType.URSCONNIDIxnID, new URSCONNIDIxnIDTable(m_accessor, TableType.URSCONNIDIxnID));
        m_tables.put(TableType.URSRI, new URSRITable(m_accessor, TableType.URSRI));

        m_tables.put(TableType.ORSMetric, new ORSMetricTable(m_accessor, TableType.ORSMetric));
        m_tables.put(TableType.ORSMetricExtension, new ORSMetricExtensionTable(m_accessor, TableType.ORSMetricExtension));
        m_tables.put(TableType.ORSSidSid, new ORSSidSidTable(m_accessor, TableType.ORSSidSid));

        m_tables.put(TableType.ORSHTTP, new OrsHTTPTable(m_accessor));
        m_tables.put(TableType.ORSMMessage, new ORSMMTable(m_accessor));

        m_tables.put(TableType.URSStrategy, new URSStrategyTable(m_accessor, TableType.URSStrategy));
        m_tables.put(TableType.URSStrategyInit, new URSStrategyInitTable(m_accessor, TableType.URSStrategyInit));
        m_tables.put(TableType.URSTlib, new URSTlibTable(m_accessor, TableType.URSTlib));
        m_tables.put(TableType.URSSTAT, new URSStatTable(m_accessor, TableType.URSSTAT));
        m_tables.put(TableType.URSRlib, new URSRlibTable(m_accessor, TableType.URSRlib));
        m_tables.put(TableType.URSCONNIDSID, new URSCONNIDSIDTable(m_accessor, TableType.URSCONNIDSID));
        m_tables.put(TableType.URSVQ, new URSVQTable(m_accessor, TableType.URSVQ));
        m_tables.put(TableType.URSTargetSet, new URSTargetSetTable(m_accessor, TableType.URSTargetSet));

        m_tables.put(TableType.WSTlib, new WSTlibTable(m_accessor, TableType.WSTlib));
        m_tables.put(TableType.WSStat, new WSStatTable(m_accessor, TableType.WSStat));
        m_tables.put(TableType.WSConf, new WSConfTable(m_accessor, TableType.WSConf));
        m_tables.put(TableType.WSEServ, new WSEServTable(m_accessor, TableType.WSEServ));

        m_tables.put(TableType.Ixn, new IxnTable(m_accessor, TableType.Ixn));
        m_tables.put(TableType.IxnDB, new IxnDBActivityTable(m_accessor, TableType.IxnDB));

        m_tables.put(TableType.MsgOCServer, new GenesysMsgTable(m_accessor, TableType.MsgOCServer));
        m_tables.put(TableType.MsgIxnServer, new GenesysMsgTable(m_accessor, TableType.MsgIxnServer));
        m_tables.put(TableType.MsgORServer, new GenesysMsgTable(m_accessor, TableType.MsgORServer));
        m_tables.put(TableType.MsgTServer, new GenesysMsgTable(m_accessor, TableType.MsgTServer));
        m_tables.put(TableType.MsgURServer, new GenesysMsgTable(m_accessor, TableType.MsgURServer));
        m_tables.put(TableType.MsgStatServer, new GenesysMsgTable(m_accessor, TableType.MsgStatServer));
        m_tables.put(TableType.MsgRM, new GenesysMsgTable(m_accessor, TableType.MsgRM));
        m_tables.put(TableType.MsgMCP, new GenesysMsgTable(m_accessor, TableType.MsgMCP));
        m_tables.put(TableType.MsgDBServer, new GenesysMsgTable(m_accessor, TableType.MsgDBServer));
        m_tables.put(TableType.MsgGMS, new GenesysMsgTable(m_accessor, TableType.MsgGMS));
        m_tables.put(TableType.MsgConfServer, new GenesysMsgTable(m_accessor, TableType.MsgConfServer));
        m_tables.put(TableType.MsgWWE, new GenesysMsgTable(m_accessor, TableType.MsgWWE));

        m_tables.put(TableType.MsgSCServer, new GenesysMsgTable(m_accessor, TableType.MsgSCServer));
        m_tables.put(TableType.SCSAppStatus, new SCSAppStatusTable(m_accessor));
        m_tables.put(TableType.SCSSelfStatus, new SCSSelfStatusTable(m_accessor));

        m_tables.put(TableType.MsgLCAServer, new GenesysMsgTable(m_accessor, TableType.MsgLCAServer));
        m_tables.put(TableType.LCAAppStatus, new LCAAppStatusTable(m_accessor));
        m_tables.put(TableType.LCAClient, new LCAClientTable(m_accessor));

        m_tables.put(TableType.WWETable, new WWETable(m_accessor, TableType.WWETable));
        m_tables.put(TableType.WWEException, new ExceptionTable(m_accessor, TableType.WWEException));
        m_tables.put(TableType.GMSPOST, new GMSPostTable(m_accessor, TableType.GMSPOST));

        m_tables.put(TableType.GMSORSMessage, new GMSORSTable(m_accessor, TableType.GMSORSMessage));

//        for (DBTable tab : m_tables.values()) {
//            tab.InitDB();
//        }
    }

    private Parser GetParser(FileInfoType type) {
        if (m_parsers.containsKey(type)) {
            return m_parsers.get(type);
        } else {
            Parser parser = initParser(type);
            if (parser == null && ifAll()) {
                logger.error("Parser not yet implemented for " + type.toString());
                return null;
            }
            m_parsers.put(type, parser);
            return parser;
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void parseAll(String scanDir, String dbname, String alias) throws SQLException, Exception {
        if (!setCurrentDirectory(scanDir)) {
            logger.error("Cannot cd to directory [" + scanDir + "]. Exiting");
            System.exit(1);
        }
        String startDir = ".";

        Instant start = Instant.now();

        File f = new File(startDir);
        if (f.isDirectory()) {
            ScanDir(f);
        } else {
            LogFileWrapper log = LogFileWrapper.getContainer(f);
            if (log != null) {
                extendFilesList(filesToAccess, log);
            }
        }

        try {
            m_accessor = new SqliteAccessor(dbname, alias);

        } catch (Exception e) {
            System.out.println("Could not create accessor: " + e);
            return;
        }
        boolean restartParsing = false;
        ArrayList<FileInfo> filesToProcess = new ArrayList();
        ArrayList<Long> filesToDelete = new ArrayList<>();

        try {
            if (m_accessor.TableExist("file_logbr")) {
                setDBExisted(true);
                int i;
                for (i = 0; i < filesToAccess.size(); i++) {
                    String logFileName = filesToAccess.get(i).getLogFileName();
                    if (logFileName != null && !logFileName.isEmpty()) { // fix for error resulting in empty DB. workspace file names are empty
                        ArrayList<ArrayList<Long>> iDs = m_accessor.getIDsMultiple(
                                "select id, size from file_logbr where intfilename = \'"
                                + filesToAccess.get(i).getLogFileName() + "\'"
                                //                            + "and size=" + filesToAccess.get(i).getSize()
                                + " ;");
                        if (iDs == null || iDs.size() == 0) {
                            logger.debug("Will process file [" + filesToAccess.get(i).getM_path() + "]");
                            filesToProcess.add(filesToAccess.get(i));
                        } else {
                            long fileID = iDs.get(0).get(0);
                            long size = iDs.get(0).get(1);
                            if (size < filesToAccess.get(i).getSize()) {
                                logger.info(filesToAccess.get(i).getLogFileName()
                                        + " id(" + fileID + ")"
                                        + ": size[" + filesToAccess.get(i).getSize()
                                        + "] size in DB[" + size + "]; file data to be removed");
                                filesToDelete.add(fileID);
                                filesToProcess.add(filesToAccess.get(i));
//                                restartParsing = true;
//                                break;
                            }
                        }
                    }
                }
            } else {
                restartParsing = true;

            }
        } catch (Exception exception) {
            logger.error("Failed to verify for new log files. Will parse a new");
        }
        if (restartParsing) {
            logger.debug("Restart parsing");
            m_accessor.Close(false);
            for (int i = 0; i < filesToAccess.size(); i++) {
                filesToProcess.add(filesToAccess.get(i));
            }
            f = null;
            for (String file : new String[]{dbname, dbname + ".db"}) {
                try {
                    f = new File(file);
                    if (f.exists()) {
                        boolean delete = f.delete();
                        if (!delete) {
                            throw new Exception("likely busy");
                        } else {
                            logger.info("Removed database " + f);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Unable to delete file " + f, e);
                    JOptionPane.showMessageDialog(null, "Unable to delete file "
                            + f + "\n", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            m_accessor = new SqliteAccessor(dbname, alias);
        } else if (filesToProcess.isEmpty()) {
            logger.info("No new log files found; parser exit");
            return;
        } else {
            initStatic(m_accessor);
        }
        m_accessor.setFilesToDelete(filesToDelete);

        logger.info("Initializing...");
        InitTables();
        logger.info("Starting DB...");
        m_accessor.start();
        logger.info("Done init");

        tabRefs = new TableReferences(m_accessor);
        if (!logger.isDebugEnabled()) {
            tabRefs.doNotSave(ReferenceType.HANDLER);
        }

//        try {
//            m_logDB = new LogDB(logdb);
//        } catch (Exception e) {
//            System.out.println("Could not init logs DB: " + e);
//            return;
//        }
        Path dir = Paths.get(dbname);
        dir = dir.getParent();
        String dirName = dir.toString();
        FileInfo dirInfo = new FileInfo();
        dirInfo.m_path = dirName;
        //dirInfo.AddToDB(m_accessor);
//        m_FileInfoTable.AddToDB(dirInfo);

        for (int i = 0; i < filesToProcess.size(); i++) {
            FileInfo newFile = filesToProcess.get(i);
            Main.logger.info("processing file : " + newFile.getM_path() + ((newFile.getArchiveName() == null) ? "" : ", archive: " + newFile.getArchiveName()) + " (" + (i + 1) + " of " + filesToProcess.size() + ")");
            Parse(newFile);
        }

        m_accessor.DoneInserts();
        m_accessor.Commit();
        m_accessor.exit();
        m_accessor.join();
        GenesysMsg.updateMsg();
        tabRefs.Finalize();

        try {
            logger.info("Parsing took " + pDuration(Duration.between(start, Instant.now()).toMillis()) + ".");
            if (FileInfoTable.getFilesAdded() == 0 || totalFiles == 0) {
                logger.info("No Genesys logs found.");
            } else {
                m_accessor.ResetAutoCommit(true);
                m_FileInfoTable.FinalizeDB();
                for (DBTable tab : m_tables.values()) {
                    if (tab.isTabCreated()) {
                        logger.info("Finalizing " + tab.getM_type() + " (added " + tab.getRecordsAdded() + " recs)");
                        tab.FinalizeDB();
                    }
                }
                FinalizeParsers();

            }
//            m_accessor.exit();
//            m_logDB.Close();
            m_accessor.Commit();
            m_accessor.Close(true);

            logger.info("All done. Completed in " + pDuration(Duration.between(start,Instant.now()).toMillis()) + "; processed " + totalFiles + " files (" + formatSize(totalBytes) + ")");
        } catch (Exception e) {
            logger.error("Exit exception " + e, e);
        }
    }

    private void FinalizeParsers() throws Exception {
        for (Parser m_parser : m_parsers.values()) {
            if (m_parser != null) {
                m_parser.doFinalize();
            }
        }
    }

    private void setDBExisted(boolean b) {
        dbExisted = b;
    }

    void finalizeTable(TableType tableType) throws Exception {
        DBTable tab = m_tables.get(tableType);
        if (tab != null) {
            tab.FinalizeDB();
        }
    }

    private Parser initParser(FileInfoType type) {
        Parser ret = null;
        switch (type) {
            case type_SipProxy:
                if (ifAll()) {
                    ret = new SIPProxyParser(m_tables);
                }
                break;

            case type_VOIPEP:
                if (ifAll()) {
                    ret = new VOIPEPParser(m_tables);
                }
                break;

            case type_URS:
                if (ifAll()) {
                    ret = new UrsParser(m_tables);
                }
                break;

            case type_SessionController:
                ret = new SingleThreadParser(m_tables);
                break;

            case type_StatServer:
                if (ifAll()) {
                    ret = new StSParser(m_tables);
                }
                break;

            case type_ORS:
                if (ifAll()) {
                    ret = new OrsParser(m_tables);
                }
                break;

            case type_OCS:
                if (ifAll()) {
                    ret = new OCSParser(m_tables);
                }
                break;

            case type_IxnServer:
                if (ifAll()) {
                    ret = new IxnServerParser(m_tables);
                }
                break;

            case type_RM:
                if (ifAll()) {
                    ret = new RMParser(m_tables);
                }
                break;

            case type_MCP:
                if (ifAll()) {
                    ret = new MCPParser(m_tables);
                }
                break;

            case type_WorkSpace:
                if (ifAll()) {
                    ret = new WorkspaceParser(m_tables);
                }
                break;

            case type_SIPEP:
                if (ifAll()) {
                    ret = new SIPEPParser(m_tables);
                }
                break;

            case type_SCS:
                if (ifAll()) {
                    ret = new SCSParser(m_tables);
                }
                break;

            case type_LCA:
                if (ifAll()) {
                    ret = new LCAParser(m_tables);
                }
                break;

            case type_WWE:
                if (ifAll()) {
                    ret = new WWEParser(m_tables);
                }
                break;

            case type_URSHTTP:
                if (ifAll()) {
                    ret = new URShttpinterfaceParser(m_tables);
                }
                break;

            case type_WWECloud:
                if (ifAll()) {
                    ret = new WWECloudParser(m_tables);
                }
                break;

            case type_DBServer:
                if (ifAll()) {
                    ret = new DBServerParser(m_tables);
                }
                break;

            case type_GMS:
                if (ifAll()) {
                    ret = new GMSParser(m_tables);
                }
                break;
            case type_ConfServer:
                if (ifAll()) {
                    ret = new ConfServParser(m_tables);
                }
                break;

            case type_ApacheWeb:
                if (ifAll()) {
                    ret = new ApacheWebLogsParser(m_tables);
                }
                break;
        }
        if (ret != null) {
            ret.init(m_tables);
        }
        return ret;

    }

    private void setXMLCfg(String xmlCFG) throws SAXException, IOException, Exception {
        xmlCfg = new XmlCfg((xmlCFG));
    }

    private boolean checkIgnoreTable(Message msg) {
        if (xmlCfg != null) {
            return xmlCfg.CheckIgnore(msg);
        }
        return false;

    }

    private boolean isLogBRDir(File file) {
        return FilenameUtils.normalizeNoEndSeparator(file.getAbsolutePath())
                .equalsIgnoreCase(logBrDir);
    }

    static class Constants extends HashMap<GenesysConstants, HashMap<Integer, String>> {

        public Constants() {
            super();
            put(GenesysConstants.TSERVER, initTServerConstants());
        }

        private HashMap<Integer, String> initTServerConstants() {
            HashMap<Integer, String> ret = new HashMap<>();

            String txt
                    = "RequestRegisterClient, 0\n"
                    + "RequestQueryServer, 1\n"
                    + "RequestQueryAddress, 2\n"
                    + "RequestRegisterAddress, 3\n"
                    + "RequestUnregisterAddress, 4\n"
                    + "RequestRegisterAll, 5\n"
                    + "RequestUnregisterAll, 6\n"
                    + "RequestSetInputMask, 7\n"
                    + "RequestAgentLogin, 8\n"
                    + "RequestAgentLogout, 9\n"
                    + "RequestAgentReady, 10\n"
                    + "RequestAgentNotReady, 11\n"
                    + "RequestSetDNDOn, 12\n"
                    + "RequestSetDNDOff, 13\n"
                    + "RequestMakeCall, 14\n"
                    + "RequestMakePredictiveCall, 15\n"
                    + "RequestAnswerCall, 16\n"
                    + "RequestReleaseCall, 17\n"
                    + "RequestHoldCall, 18\n"
                    + "RequestRetrieveCall, 19\n"
                    + "RequestInitiateConference, 20\n"
                    + "RequestCompleteConference, 21\n"
                    + "RequestDeleteFromConference, 22\n"
                    + "RequestInitiateTransfer, 23\n"
                    + "RequestMuteTransfer, 24\n"
                    + "RequestSingleStepTransfer, 25\n"
                    + "RequestCompleteTransfer, 26\n"
                    + "RequestMergeCalls, 27\n"
                    + "RequestAlternateCall, 28\n"
                    + "RequestReconnectCall, 29\n"
                    + "RequestAttachUserData, 30\n"
                    + "RequestUpdateUserData, 31\n"
                    + "RequestDeleteUserData, 32\n"
                    + "RequestDeletePair, 33\n"
                    + "RequestCallForwardSet, 34\n"
                    + "RequestCallForwardCancel, 35\n"
                    + "RequestRouteCall, 36\n"
                    + "RequestGiveMusicTreatment, 37\n"
                    + "RequestGiveSilenceTreatment, 38\n"
                    + "RequestGiveRingBackTreatment, 39\n"
                    + "RequestLoginMailBox, 40\n"
                    + "RequestLogoutMailBox, 41\n"
                    + "RequestOpenVoiceFile, 42\n"
                    + "RequestCloseVoiceFile, 43\n"
                    + "RequestPlayVoiceFile, 44\n"
                    + "RequestCollectDigits, 45\n"
                    + "RequestSetMessageWaitingOn, 46\n"
                    + "RequestSetMessageWaitingOff, 47\n"
                    + "RequestDistributeUserEvent, 48\n"
                    + "RequestDistributeEvent, 49\n"
                    + "EventServerConnected, 50\n"
                    + "EventServerDisconnected, 51\n"
                    + "EventError, 52\n"
                    + "EventRegistered, 53\n"
                    + "EventUnregistered, 54\n"
                    + "EventRegisteredAll, 55\n"
                    + "EventUnregisteredAll, 56\n"
                    + "EventQueued, 57\n"
                    + "EventDiverted, 58\n"
                    + "EventAbandoned, 59\n"
                    + "EventRinging, 60\n"
                    + "EventDialing, 61\n"
                    + "EventNetworkReached, 62\n"
                    + "EventDestinationBusy, 63\n"
                    + "EventEstablished, 64\n"
                    + "EventReleased, 65\n"
                    + "EventHeld, 66\n"
                    + "EventRetrieved, 67\n"
                    + "EventPartyChanged, 68\n"
                    + "EventPartyAdded, 69\n"
                    + "EventPartyDeleted, 70\n"
                    + "EventRouteRequest, 71\n"
                    + "EventRouteUsed, 72\n"
                    + "EventAgentLogin, 73\n"
                    + "EventAgentLogout, 74\n"
                    + "EventAgentReady, 75\n"
                    + "EventAgentNotReady, 76\n"
                    + "EventDNDOn, 77\n"
                    + "EventDNDOff, 78\n"
                    + "EventMailBoxLogin, 79\n"
                    + "EventMailBoxLogout, 80\n"
                    + "EventVoiceFileOpened, 81\n"
                    + "EventVoiceFileClosed, 82\n"
                    + "EventVoiceFileEndPlay, 83\n"
                    + "EventDigitsCollected, 84\n"
                    + "EventAttachedDataChanged, 85\n"
                    + "EventOffHook, 86\n"
                    + "EventOnHook, 87\n"
                    + "EventForwardSet, 88\n"
                    + "EventForwardCancel, 89\n"
                    + "EventMessageWaitingOn, 90\n"
                    + "EventMessageWaitingOff, 91\n"
                    + "EventAddressInfo, 92\n"
                    + "EventServerInfo, 93\n"
                    + "EventLinkDisconnected, 94\n"
                    + "EventLinkConnected, 95\n"
                    + "EventUserEvent, 96\n"
                    + "RequestSendDTMF, 97\n"
                    + "EventDTMFSent, 98\n"
                    + "EventResourceAllocated, 99\n"
                    + "EventResourceFreed, 100\n"
                    + "EventRemoteConnectionSuccess, 101\n"
                    + "EventRemoteConnectionFailed, 102\n"
                    + "RequestRedirectCall, 103\n"
                    + "RequestListenDisconnect, 104\n"
                    + "RequestListenReconnect, 105\n"
                    + "EventListenDisconnected, 106\n"
                    + "EventListenReconnected, 107\n"
                    + "RequestQueryCall, 108\n"
                    + "EventPartyInfo, 109\n"
                    + "RequestClearCall, 110\n"
                    + "RequestSetCallInfo, 111\n"
                    + "EventCallInfoChanged, 112\n"
                    + "RequestApplyTreatment, 113\n"
                    + "EventTreatmentApplied, 114\n"
                    + "EventTreatmentNotApplied, 115\n"
                    + "EventTreatmentEnd, 116\n"
                    + "EventHardwareError, 117\n"
                    + "EventAgentAfterCallWork, 118\n"
                    + "EventTreatmentRequired, 119\n"
                    + "RequestSingleStepConference, 120\n"
                    + "RequestQuerySwitch, 121\n"
                    + "EventSwitchInfo, 122\n"
                    + "RequestGetAccessNumber, 123\n"
                    + "RequestCancelReqGetAccessNumber, 124\n"
                    + "EventAnswerAccessNumber, 125\n"
                    + "EventReqGetAccessNumberCanceled, 126\n"
                    + "RequestReserveAgent, 127\n"
                    + "EventAgentReserved, 128\n"
                    + "RequestReserveAgentAndGetAccessNumber, 129\n"
                    + "RequestAgentSetIdleReason, 130\n"
                    + "EventAgentIdleReasonSet, 131\n"
                    + "EventRestoreConnection, 132\n"
                    + "EventPrimaryChanged, 133\n"
                    + "RequestLostBackupConnection, 134\n"
                    + "RequestSetDNInfo, 135\n"
                    + "RequestQueryLocation, 136\n"
                    + "EventLocationInfo, 137\n"
                    + "EventACK, 138\n"
                    + "RequestMonitorNextCall, 139\n"
                    + "RequestCancelMonitoring, 140\n"
                    + "EventMonitoringNextCall, 141\n"
                    + "EventMonitoringCancelled, 142\n"
                    + "RequestSetMuteOn, 143\n"
                    + "RequestSetMuteOff, 144\n"
                    + "EventMuteOn, 145\n"
                    + "EventMuteOff, 146\n"
                    + "EventDNOutOfService, 147\n"
                    + "EventDNBackInService, 148\n"
                    + "RequestPrivateService, 149\n"
                    + "EventPrivateInfo, 150\n"
                    + "EventBridged, 151\n"
                    + "EventQueueLogout, 152\n"
                    + "EventReserved_1, 153\n"
                    + "EventReserved_2, 154\n"
                    + "EventReserved_3, 155\n"
                    + "EventReserved_4, 156\n"
                    + "EventReserved_5, 157\n"
                    + "EventReserved_6, 158\n"
                    + "EventReserved_7, 159\n"
                    + "EventCallCreated, 160\n"
                    + "EventCallDataChanged, 161\n"
                    + "EventCallDeleted, 162\n"
                    + "EventCallPartyAdded, 163\n"
                    + "EventCallPartyState, 164\n"
                    + "EventCallPartyMoved, 165\n"
                    + "EventCallPartyDeleted, 166\n"
                    + "RequestStartCallMonitoring, 167\n"
                    + "RequestStopCallMonitoring, 168\n"
                    + "RequestSendReturnReceipt, 169\n"
                    + "EventReturnReceipt, 170\n"
                    + "RequestNetworkConsult, 171\n"
                    + "RequestNetworkAlternate, 172\n"
                    + "RequestNetworkTransfer, 173\n"
                    + "RequestNetworkMerge, 174\n"
                    + "RequestNetworkReconnect, 175\n"
                    + "RequestNetworkSingleStepTransfer, 176\n"
                    + "RequestNetworkPrivateService, 177\n"
                    + "EventNetworkCallStatus, 178\n"
                    + "EventNetworkPrivateInfo, 179\n"
                    + "RequestTransactionMonitoring, 180\n"
                    + "EventTransactionStatus, 181\n"
                    + "EventResourceInfo, 182\n"
                    + "MessageIDMAX, 183";
            for (String l : StringUtils.split(txt, "\n")) {
                String[] split = StringUtils.split(l, " ,");
                ret.put(Integer.parseInt(split[1]), split[0]);
            }
            return ret;
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

    static class XmlCfg {

        private static Element root;
        private final Document doc; // XML doc for the CFG

        private HashMap<String, ArrayList> tablesCfg;
        FilesParseSettings filesSettings = new FilesParseSettings();

        XmlCfg(String filePath) throws ParserConfigurationException, SAXException, IOException, Exception {
            File fXmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            root = doc.getDocumentElement();
            if (root == null || !root.getNodeName().equalsIgnoreCase("Config")) {
                throw new ParserConfigurationException("bad root XML element: " + root);
            }

            tablesCfg = new HashMap<>();

            //optional, but recommended
            //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();

            NodeList nl = root.getChildNodes();
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) nl.item(i);
                        if (el.getNodeName().equalsIgnoreCase("load")) {
                            AddLoad(el);
                        } else if (el.getNodeName().equalsIgnoreCase("finalize")) {
                            AddFinalize(el);
                        } else if (el.getNodeName().equalsIgnoreCase("file")) {
                            AddFile(el);
                        } else {
                            throw new ParserConfigurationException("unsupported element: " + el.getNodeName());
                        }
                    }
                }
            }

        }

        private void AddLoad(Element el) throws ParserConfigurationException {
            NodeList nl = el.getChildNodes();
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element spec = (Element) nl.item(i);
                        if (spec.getNodeName().equalsIgnoreCase("Table")) {
                            String type = spec.getAttribute("type").toLowerCase();
                            AddTable(type, spec);
                        }
                    }
                }
            }
        }

        private void AddFinalize(Element el) {
            //        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private void AddTable(String type, Element spec) throws ParserConfigurationException, PatternSyntaxException {

            NodeList nl = spec.getChildNodes();
            if (nl != null && nl.getLength() > 0) {
                ArrayList<Pattern> pIgnore = new ArrayList<>();
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element tab = (Element) nl.item(i);
                        if (tab.getNodeName().equalsIgnoreCase("ignore")) {
                            String p = tab.getAttribute("pattern");
                            if (p == null || p.length() == 0) {
                                throw new ParserConfigurationException("ignore/pattern not specified");
                            }
                            pIgnore.add(Pattern.compile(p));
                        }
                    }
                }
                tablesCfg.put(type, pIgnore);

            }
        }

        private boolean CheckIgnore(Message msg) {
            if (tablesCfg != null && msg != null) {
                ArrayList<Pattern> pIgnore = tablesCfg.get(msg.getM_type().toString());

                if (pIgnore != null && msg.m_MessageLines != null) {
                    for (String s : msg.m_MessageLines) {
                        for (Pattern pIgn : pIgnore) {
                            if (pIgn.matcher(s).find()) {
                                Main.logger.trace("Ignoring message type " + msg.getM_type().toString() + " msg:" + msg);
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        private void AddFile(Element el) throws ParserConfigurationException {
            String type = el.getAttribute("type").toLowerCase();
            if (type == null || type.length() == 0) {
                throw new ParserConfigurationException("Type of file not specified");
            }

            NodeList nl = el.getChildNodes();
            if (nl != null && nl.getLength() > 0) {
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element spec = (Element) nl.item(i);
//                        Main.logger.info("Type: " + type + " attr[" + spec.getNodeName() + "]");
                        if (spec.getNodeName().equalsIgnoreCase("Date")) {
                            String pat = spec.getAttribute("pattern");
                            String fmt = spec.getAttribute("format");
                            logger.trace("Adding spec t:[" + type + "]" + " pat:[" + pat + "]" + " fmt:[" + fmt + "]");
//                            Parser.DateFmt df = new Parser.DateFmt(pat, fmt);
                            Parser.DateFmt df = new Parser.DateFmt(pat, fmt, spec);
                            filesSettings.addDateFmt(type, df);
                        } else if (spec.getNodeName().equalsIgnoreCase("CustomSearch")) {
                            addCustomSearch(type, spec);
                        }
                    }
                }
            }
        }

        public FilesParseSettings.FileParseSettings getFileParseSettings(FileInfoType t) {
            logger.trace("getDateFormats " + t);
            String type = FileInfoType.getFileType(t);
            if (type != null && type.length() > 0) {
                return filesSettings.retrieveFileParseSettings(type.toLowerCase());
            } else {
                return null;
            }
        }

        private void addCustomSearchMatch(Element spec, FilesParseSettings.FileParseCustomSearch customSearch) {
            customSearch.setTrimmed(spec.getAttribute("trimmed"));
            customSearch.setPtString(spec.getAttribute("pattern"));
            customSearch.setHandlerOnly(spec.getAttribute("handleronly"));
            customSearch.setParseRest(spec.getAttribute("parserRest"));
            customSearch.setName(spec.getAttribute("name"), customSearch.getPtString());

            NodeList nl = spec.getChildNodes();
            if (nl != null) {
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element comp = (Element) nl.item(i);
                        if (comp.getNodeName().equalsIgnoreCase("component")) {
                            ArrayList<FilesParseSettings.FileParseCustomSearch.SearchComponent> compAttr = customSearch.addComponent(comp.getAttribute("name"));

                            NodeList attrs = comp.getChildNodes();

                            if (attrs != null) {
                                for (int j = 0; j < attrs.getLength(); j++) {
                                    if (attrs.item(j).getNodeType() == Node.ELEMENT_NODE) {
                                        Element attr = (Element) attrs.item(j);
                                        if (attr.getNodeName().equalsIgnoreCase("attribute")) {
                                            FilesParseSettings.FileParseCustomSearch.SearchComponent sc = customSearch.addAttributes(compAttr, attr.getAttribute("name"), attr.getAttribute("value"), attr.getAttribute("mustChange"));
                                            NodeList modifNodes = attr.getChildNodes();
                                            if (modifNodes != null) {
                                                for (int k = 0; k < modifNodes.getLength(); k++) {
                                                    if (modifNodes.item(k).getNodeType() == Node.ELEMENT_NODE) {
                                                        Element modifNode = (Element) modifNodes.item(k);
                                                        if (modifNode.getNodeName().equalsIgnoreCase("modification")) {
                                                            sc.addModification(modifNode.getAttribute("search"), modifNode.getAttribute("replace"));
                                                        }

                                                    }
                                                }

                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            customSearch.compilePattern();
        }

        private void addCustomSearch(String type, Element spec) {
            NodeList nl = spec.getChildNodes();
            if (nl != null) {
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Element comp = (Element) nl.item(i);
                        if (comp.getNodeName().equalsIgnoreCase("match")) {
                            addCustomSearchMatch(comp, filesSettings.addFileParseCustomSearch(type));
                        }
                    }
                }
            }
        }

    }

    public static class ExecutionEnvironment {

        private final Option optXMLCfg;
        private final Option optAlias;
        private final Option optDBName;
        private final Option optLogsBaseDir;
        private final Option optIgnoreZIP;
        private CommandLine cmd;
        private final CommandLineParser parser;
        private final Option optTDiffParse;
        private boolean parseTDiff = false;

        private Options options = null;

        Option optHelp;

        ExecutionEnvironment() {
            options = new Options();

            optHelp = Option.builder("h")
                    .hasArg(false)
                    .required(false)
                    .desc("Show help and exit")
                    .longOpt("help")
                    .build();
            options.addOption(optHelp);

            optXMLCfg = Option.builder("x")
                    .hasArg(true)
                    .required(false)
                    .desc("XML config file for parser")
                    .longOpt("cfgxml")
                    .build();
            options.addOption(optXMLCfg);

            optAlias = Option.builder("a")
                    .hasArg(true)
                    .required(false)
                    .desc("Alias to use")
                    .longOpt("alias")
                    .build();
            options.addOption(optAlias);

            optDBName = Option.builder("d")
                    .hasArg(true)
                    .required(false)
                    .desc("Name of the SQLite database file")
                    .longOpt("dbname")
                    .build();
            options.addOption(optDBName);

            optLogsBaseDir = Option.builder("b")
                    .hasArg(true)
                    .required(false)
                    .desc("Basic directory for logs. Default to current")
                    .longOpt("basedir")
                    .build();
            options.addOption(optLogsBaseDir);

            optTDiffParse = Option.builder()
                    .hasArg(false)
                    .required(false)
                    .desc("If set, means parse difference between timestamps")
                    .longOpt("timediff")
                    .build();
            options.addOption(optTDiffParse);

            optIgnoreZIP = Option.builder()
                    .hasArg(false)
                    .required(false)
                    .desc("If specified, ZIP files are ignored while parsing")
                    .longOpt("ignore-zip")
                    .build();
            options.addOption(optIgnoreZIP);

            parser = new DefaultParser();

            //        theTest();
            String s = System.getProperty("logbr.dir");
            if (s == null || s.isEmpty()) {
                s = ".";
            }
            File f = new File(s);
            logBrDir = FilenameUtils.normalizeNoEndSeparator(f.getAbsolutePath());
            System.setProperty("logPath", logBrDir);
            System.setProperty("log4j2.saveDirectory", "true");

        }

        public String[] getArgs() {
            return cmd.getArgs();
        }

        public List<String> getArgList() {
            return cmd.getArgList();
        }

        public void printHelp() {
            HelpFormatter hf = new HelpFormatter();
            hf.printHelp("indexer [params] <working directory>", "header", options, "footer");
        }

        private void showHelpExit(Options options) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("utility-name", options);

            System.exit(0);
        }

        private String getAlias() {
            return getStringOrDef(optAlias, "logbr");
        }

        public boolean isParseTDiff() {
            return parseTDiff;
        }

        private String getDBName() {
            return getStringOrDef(optDBName, "logbr");
        }

        private String getXmlCfg() {
            return getStringOrDef(optXMLCfg, ".");
        }

        private String getBaseDir() {
            return getStringOrDef(optLogsBaseDir, Paths.get(".").toAbsolutePath().normalize().toString());
        }

        private String getStringOrDef(Option opt, String def) {
            String ret = null;
            try {
                ret = (String) cmd.getParsedOptionValue(opt.getLongOpt());
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

            this.parseTDiff = cmd.hasOption(optTDiffParse.getLongOpt());
        }

        private boolean isIgnoreZIP() {
            return cmd.hasOption(optIgnoreZIP.getLongOpt());

        }

    }

}
