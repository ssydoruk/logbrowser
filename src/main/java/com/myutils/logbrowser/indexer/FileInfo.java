/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import static com.myutils.logbrowser.indexer.Main.m_component;
import static java.lang.System.arraycopy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author ssydoruk
 */
public final class FileInfo extends Record {

    public static final int WORKSPACE_BYTES = 1024;
    private static final org.apache.logging.log4j.Logger logger = Main.logger;

    private static final int BUF_SIZE = 1024;
    private static final Pattern regAppType = Pattern.compile("^Application type:\\s+([^\\(]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RegAppCommandLine = Pattern.compile("^Command line:.+[/\\\\\\s]confserv", Pattern.CASE_INSENSITIVE);
    private static final Pattern regGWS = Pattern.compile("^GWS\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern regGWSPIDHost = Pattern.compile("^PID@host: \\[(\\d+)@(.+)\\]$", Pattern.CASE_INSENSITIVE);
    private static final Pattern regGWSStart = Pattern.compile("Start time: \\[(.+)\\]$", Pattern.CASE_INSENSITIVE);
    /*
    public static String InitDB(DBAccessor accessor, int statementId) {
    m_fileId = 1;
    m_runIds = new HashMap();
    m_batchId = statementId;
    String query = "create table if not exists file_" + m_alias + " (id INTEGER PRIMARY KEY ASC,"+
    "name CHAR(255), RunID BIGINT, Component INTEGER, NodeId CHAR(8), App CHAR(32), Host CHAR(32));";
    accessor.ExecuteQuery(query);
    return "INSERT INTO file_" + m_alias + " VALUES(NULL,?,?,?,?,?,?);";
    }
     */
    private static final byte[] WS_BYTES = new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf};
    private static final Pattern patternLocalTime = Pattern.compile("^Local\\s+time:\\s+(\\S+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern startTimePattern = Pattern.compile("^Start\\s+time\\s+\\(UTC\\):\\s+(\\S+)\\s*$");
    private static final Pattern startTimePatternGMS = Pattern.compile("^UTC START TIME:\\s+(\\S+)\\s*$");
    private static final Pattern startTimeSIPEP = Pattern.compile("^UTC\\s+Start\\s+Time:\\s+(\\S+)\\s*$");
    private static final Pattern startTimePatternFinalDot = Pattern.compile("\\.\\d+$");
    private static final Pattern regAppName = Pattern.compile("^Application name:\\s+(\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern regHostName = Pattern.compile("^Host name:\\s+(\\S.+)");
    private static final Pattern regWorkSpaceHostName = Pattern.compile("^\\s+MachineName:\\s+(\\S.+)");
    private static final Pattern regGMSHostName = Pattern.compile("^APPLICATION HOST.+:\\s+(\\S.+)");
    private static final Pattern regHostSIPEP = Pattern.compile("^Application Host.+:\\s+(\\S.+)$");
    private static final Pattern regTimezone = Pattern.compile("^Time zone:\\s+(\\d+)");
    private static final Pattern regFileName = Pattern.compile("^File:\\s+\\((\\d+)\\)\\s+(\\S.+)");
    private static final Pattern regFileNameGMS = Pattern.compile("^FILE:\\s+(\\S.+)");
    private static final Pattern regWWECloud = Pattern.compile("(DEBUG|ERROR|WARN|INFO)\\s+\\[");
    private static final int NUM_PARAMS_TO_READ = 7;
    private static final Pattern regFileNameTypeSIP = Pattern.compile("-(\\d+)\\.\\d{8}_\\d{6}_\\d{3}\\.log$");
    private static final Pattern regFileNameDate = Pattern.compile("\\.(\\d{8}_\\d{6}_\\d{3})\\.log");
    static private final DateTimeFormatter workSpaceDateTime = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    static HashMap m_runIds;
    private final DateParsers dateParsers;
    private final byte[] buf = null;
    private final int bufRead = 0;
    public FileInfoType m_componentType;

    public long m_runId;
    public String m_nodeId;
    public String m_path;
    public String m_name;
    FileInfoType m_fileFilterId;
    boolean m_ServerStartTimeSet;
    private LogFileWrapper logFile;
    private String archiveName;
    private String filePath;
    private String fileDir;
    private String m_app;
    private String m_host;
    private int appID = 0;
    private String appType = "";
    private DateParsed fileLocalTime;
    private String logFileNo;
    private String logFileName;
    private long size;
    private DateParsed fileEndTime = null;
    private DateParsed fileStartTime;
    private boolean ignoring = false;
    private FileInfoType m_componentTypeSuspect = FileInfoType.type_Unknown;

    FileInfo(File file, LogFileWrapper wrapper) throws IOException {
        this(file);
        filePath = FilenameUtils.normalize(file.getAbsolutePath());
        fileDir = new File(filePath).getParent();
        this.logFile = wrapper;

    }

    FileInfo(ZipFile logArchive, ZipEntry entry, ZIPLog aThis) throws IOException {
        this();
        m_path = entry.getName();
        m_name = entry.getName();
        filePath = aThis.getFile().getAbsolutePath();
        fileDir = aThis.getFile().getParent();
        setSize(entry.getSize());
        setFileFilter(m_component);
        Main.logger.trace(this.toString());

        this.logFile = aThis;
        this.archiveName = logFile.getName();
    }

    FileInfo(File _file) throws IOException {
        this();
        m_path = _file.getPath();
        m_name = _file.getName();
        setSize(_file.length());
        setFileFilter(m_component);
        Main.logger.trace(this.toString());
    }

    public FileInfo() {
        super();
        m_nodeId = "";
        m_runId = 0;
        m_componentType = FileInfoType.type_Unknown;
        m_fileFilterId = FileInfoType.type_Unknown;
        m_ServerStartTimeSet = false;
        m_runIds = new HashMap();
        dateParsers = new DateParsers();
    }

    public String getArchiveName() {
        return archiveName;
    }

    private byte[] startBuf = null;

    public byte[] getStartBuf() throws IOException {
        if (startBuf == null) {
            byte[] _startBuf = new byte[BUF_SIZE];
            int bytesRead = readBytes(_startBuf, BUF_SIZE);
            if (bytesRead < BUF_SIZE) {
                startBuf = new byte[bytesRead];
                arraycopy(_startBuf,
                        0,
                        startBuf,
                        0,
                        bytesRead);
            }
            else {
                startBuf=_startBuf;
            }
        }
        return startBuf;
    }

    public boolean fileEqual(FileInfo otherFile) throws IOException {

        if (getM_componentType() != otherFile.getM_componentType()) {
            return false;
        }

        if (getM_componentType() == FileInfoType.type_WorkSpace) {// two workspace files
            byte[] myBuf = getStartBuf();
            byte[] otherBuf = otherFile.getStartBuf();
            int myReadBytes =myBuf.length;
            int otherReadBytes = otherBuf.length;

            return myReadBytes == otherReadBytes
                    && Arrays.equals(myBuf, otherBuf);
        } else if (getM_componentType() == FileInfoType.type_WWE) {
            if (getStartTime() != 0) {
                boolean ret = getStartTime() == otherFile.getStartTime();
                if (ret == true) {
                    return getFileLocalTime() == otherFile.getFileLocalTime();
                } else {
                    return false;
                }
            } else {
                String myLf = getLogFileName();
                String otherLf = otherFile.getLogFileName();

                if (myLf != null && !myLf.isEmpty()
                        && otherLf != null && !otherLf.isEmpty()) {
                    return myLf.equals(otherLf);
                }

            }
        } else {
            String myLf = getLogFileName();
            String otherLf = otherFile.getLogFileName();

            if (myLf != null && !myLf.isEmpty()
                    && otherLf != null && !otherLf.isEmpty()) {
                return myLf.equals(otherLf);
            }
        }

        return false;
    }

    int readBytes(byte[] cbuf, int len) throws IOException {
        InputStream inputStream = logFile.getInputStream(this);
        BufferedReaderCrLf input = new BufferedReaderCrLf(logFile.getInputStream(this));
        int read = input.read(cbuf, 0, len);
        input.close();
        return read;
    }

    protected DateParsed getFileEndTime() {
        return fileEndTime;
    }

    protected void setFileEndTime(DateParsed fileEndTime) {
        this.fileEndTime = fileEndTime;
    }

    protected DateParsed getFileLocalTime() {
        return fileLocalTime;
    }

    public DateParsed getFileStartTime() {
        return fileStartTime;
    }

    @Override
    public String toString() {
        return "FileInfo{" + "m_componentType=" + m_componentType.toString() + ", m_path=" + m_path + ", m_name=" + m_name + ", m_app=" + m_app + ", m_host=" + m_host
                + ", fileLocalTime=" + fileLocalTime
                + ", fileStart=" + fileStartTime
                + ", fileEnd=" + fileEndTime
                + '}' + super.toString();
    }

    public long getM_runId() {
        long startTime = getStartTime();
        if (startTime > 0) {

            double timeIn20SecIntervals = ((double) startTime) / 20000.0;
            long runId = Math.round(timeIn20SecIntervals);

            if (m_runIds.containsKey(runId - 1)) {
                runId = runId - 1;
            } else if (m_runIds.containsKey(runId + 1)) {
                runId = runId + 1;
            } else if (!m_runIds.containsKey(runId)) {
                m_runIds.put(runId, 0);
            }

            m_runId = runId;
        }

        return m_runId;
    }

    public String getM_nodeId() {
        return m_nodeId;
    }

    public String getM_path() {
        return m_path;
    }

    public String getM_name() {
        return m_name;
    }

    public FileInfoType getM_componentType() {
        return m_componentType;
    }

    public String getAppName() {

        switch (getM_componentType()) {
            case type_SIPEP:
            case type_WWE:
            case type_LCA:
            case type_URSHTTP:
                m_app = FileInfoType.getFileType(getM_componentType()) + "@" + getHost();
                break;
        }
        return m_app;

    }

    public String getHost() {
        return m_host;
    }

    public String getAppType() {
        if (appType == null || appType.isEmpty()) {
            appType = FileInfoType.getFileType(m_componentType);
        }
        return appType;
    }

    private boolean CheckCM(String filename) {
        return filename.contains("-001.")
                || filename.contains("-002.")
                || filename.contains("-003.")
                || filename.contains("-004.")
                || filename.contains("-005.")
                || filename.contains("-006.")
                || filename.contains("-007.")
                || filename.contains("-008.")
                || filename.contains("-009.")
                || filename.contains("-010.")
                || filename.contains("-011.")
                || filename.contains("-012.")
                || filename.contains("-013.")
                || filename.contains("-014.")
                || filename.contains("-015.")
                || filename.contains("-016.")
                || filename.contains("-512.");
    }

    FileType getFileType() {

        switch (getM_componentType()) {
            case type_CallManager:
            case type_SessionController: {
                String f = getLogFileName();
                Matcher m;
                if (f != null && (m = regFileNameTypeSIP.matcher(f)).find()) {
                    String typeDigits = m.group(1);
                    if (typeDigits != null && typeDigits.equals("1536")) {
                        return FileType.SIP_1536;
                    }
                }
            }

        }
        return FileType.UNKNOWN;
    }

    InputStream getInputStream() throws FileNotFoundException {
        return logFile.getInputStream(this);
    }

    void startParsing() throws Exception {
        logFile.startParsing();

    }

    void doneParsing() throws Exception {
        logFile.doneParsing();
    }

    boolean getIgnoring() {
        return ignoring;

    }

    void setIgnoring() {
        ignoring = true;
    }

    public FileInfoType CheckLog(BufferedReaderCrLf input) {
        m_handlerInProgress = false;
        int num = 0;
        int numParamsToRead = 0;

        try {
            boolean isTserver = false;
            String str;

            byte[] cbuf = new byte[3];
            int read = input.read(cbuf, 0, 3);
            if (read < 3) {
                Main.logger.info("Unable to read 3 bytes from file [" + m_path + "]");
                return FileInfoType.type_Unknown;
            }
            if (Arrays.equals(cbuf, WS_BYTES)) {
                Main.logger.trace("workspace log found!!!");
                return checkWorkSpace(input);
            }
            String strBuf = new String(cbuf);
            boolean doBreak = false;

            ParserState parserState = ParserState.STATE_HEADER;
            int suspectedWWECloudLines = 0;

            while ((str = input.readLine()) != null) {
                Matcher m;
                if (strBuf != null) {
                    str = strBuf + str;
                    strBuf = null;
                }

                Main.logger.trace(" [" + str + "]");
//                str = str.trim();

                if (doBreak || (num++ > 5000 || numParamsToRead >= NUM_PARAMS_TO_READ)) {
                    break;
                }

                switch (parserState) {
//<editor-fold defaultstate="collapsed" desc="STATE_HEADER">
                    case STATE_HEADER: {
                        if ((m = regAppType.matcher(str)).find()) {
                            numParamsToRead++;
                            if (m_componentType == FileInfoType.type_GMS) {
                                appType = FileInfoType.getFileType(m_componentType);
                            } else {
                                appType = m.group(1).trim();
                            }
                            if (appType.equals("TServer")) {
                                if (CheckCM(m_name) && (m_fileFilterId == FileInfoType.type_CallManager || m_fileFilterId == FileInfoType.type_Unknown)) {
                                    m_componentType = FileInfoType.type_CallManager;
                                } else if (m_name.contains("-1024.") && (m_fileFilterId == FileInfoType.type_InteractionProxy || m_fileFilterId == FileInfoType.type_Unknown)) {
                                    m_componentType = FileInfoType.type_InteractionProxy;
                                } else if (m_name.contains("-1280.") && (m_fileFilterId == FileInfoType.type_tController || m_fileFilterId == FileInfoType.type_Unknown)) {
                                    m_componentType = FileInfoType.type_tController;
                                } else if (m_name.contains("-768.") && (m_fileFilterId == FileInfoType.type_CallManager || m_fileFilterId == FileInfoType.type_Unknown)) {
                                    m_componentType = FileInfoType.type_TransportLayer;
                                } else {
                                    m_componentType = FileInfoType.type_SessionController;
                                    isTserver = true;
                                }
//                    } else if (appType.equals("StatServer") && m_fileFilterId == FileInfoType.type_Unknown) {
//                        m_componentType = FileInfoType.type_StatServer;
//                    } else if (appType.equals("RouterServer") && m_fileFilterId == FileInfoType.type_Unknown) {
//                        m_componentType = FileInfoType.type_URS;
//                    } else if (appType.equals("RouterServer") && m_fileFilterId == FileInfoType.type_Unknown) {
//                        m_componentType = FileInfoType.type_ICON;
//                    } else if (appType.equals("OrchestrationServer") && m_fileFilterId == FileInfoType.type_Unknown) {
//                        m_componentType = FileInfoType.type_ORS;
//                    } else if (appType.equals("OCServer") && m_fileFilterId == FileInfoType.type_Unknown) {
//                        m_componentType = FileInfoType.type_OCS;
//                    } else if (appType.equals("InteractionServer") && m_fileFilterId == FileInfoType.type_Unknown) {
//                        m_componentType = FileInfoType.type_IxnServer;
                            } else {
                                if (m_componentType == FileInfoType.type_Unknown) {
                                    m_componentType = FileInfoType.FileType(appType);
                                }
                            }
                        } else if (str.startsWith("Genesys SIPPROXY") || str.startsWith("SIP Proxy")) {
                            if (m_fileFilterId == FileInfoType.type_SipProxy || m_fileFilterId == FileInfoType.type_Unknown) {
                                m_componentType = FileInfoType.type_SipProxy;
                            }
                        } else if (str.startsWith("Local Control Agent") && m_fileFilterId == FileInfoType.type_Unknown) {
                            m_componentType = FileInfoType.type_LCA;
                        } else if (str.startsWith("VoIP endpoint") && m_fileFilterId == FileInfoType.type_Unknown) {
                            m_componentType = FileInfoType.type_VOIPEP;
                        } else if (str.startsWith("Genesys StatServer") && m_fileFilterId == FileInfoType.type_Unknown) {
                            m_componentType = FileInfoType.type_StatServer;
                        } else if (str.startsWith("Genesys Mobile Server") && m_fileFilterId == FileInfoType.type_Unknown) {
                            m_componentType = FileInfoType.type_GMS;

                        } else if (m_componentType == FileInfoType.type_Unknown && str.contains("InteractionWorkspace - Version:")) {
                            checkWorkSpace(input);
                        } else if ((m = regWorkSpaceHostName.matcher(str)).find()) {
                            numParamsToRead++;
                            setM_app(m.group(1));
                        } else if ((m = regAppName.matcher(str)).find()) {
                            numParamsToRead++;
                            setM_app(m.group(1));
                        } else if ((m = regHostName.matcher(str)).find()) {
                            numParamsToRead++;
                            m_host = m.group(1);
                        } else if ((m = regGMSHostName.matcher(str)).find()) {
                            numParamsToRead++;
                            m_host = m.group(1);
                        } else if ((m = regHostSIPEP.matcher(str)).find()) {
                            numParamsToRead++;
                            m_host = m.group(1);
                        } else if ((m = patternLocalTime.matcher(str)).find()) {
                            numParamsToRead++;
                            try {
                                fileLocalTime = dateParsers.parseFormatDate(m.group(1));
                                Main.logger.debug("local time: " + fileLocalTime);

                            } catch (Exception ex) {
                                logger.error("fatal: ",  ex);
                            }
                        } else if ((startTimePatternGMS.matcher(str)).find()) {
                            numParamsToRead++;
                            try {
                                fileStartTime = dateParsers.parseFormatDate(str);
                                Main.logger.debug("start time: " + fileStartTime);

                            } catch (Exception ex) {
                                logger.error("fatal: ",  ex);
                            }
                        } else if ((m = startTimePattern.matcher(str)).find()) {
                            numParamsToRead++;
                            try {
                                String strDate = m.group(1);
                                if (!startTimePattern.matcher(strDate).find()) {
                                    strDate += ".000";
                                    fileStartTime = dateParsers.parseFormatDate(strDate);
                                }
                                Main.logger.debug("start time: " + fileStartTime);

                            } catch (Exception ex) {
                                logger.error("fatal: ",  ex);
                            }
                        } else if ((m = startTimeSIPEP.matcher(str)).find()) {
                            numParamsToRead++;
                            try {
                                String strDate = m.group(1);
                                fileStartTime = dateParsers.parseFormatDate(strDate);
                                Main.logger.debug("start time: " + fileStartTime);

                            } catch (Exception ex) {
                                logger.error("fatal: ",  ex);
                            }
                        } else if ((m = regTimezone.matcher(str)).find()) {
                            numParamsToRead++;
//                else if (str.startsWith("Time zone: ")) {
                            String tz = m.group(1);
                            try {
                                Message.SetTimezone(Integer.parseInt(tz));
                            } catch (NumberFormatException numberFormatException) {
                                Main.logger.error("Cannot parse timezone [" + tz + "]; default to +18000");
                                Message.SetTimezone(18000);
                            }
                        } else if ((m = regFileName.matcher(str)).find()) {
                            numParamsToRead++;
                            logFileNo = m.group(1);
                            logFileName = m.group(2);

                        } else if ((m = regFileNameGMS.matcher(str)).find()) {
                            numParamsToRead++;
                            logFileName = m.group(1);

                        } else if (str.startsWith("HA Role: ")) {
//                    if (m_fileFilterId == FileInfoType.type_SessionController || m_fileFilterId == FileInfoType.type_Unknown) {
                            m_componentType = FileInfoType.type_SessionController;
//                    }
                        } else if ((regGWS.matcher(str)).find()) {
                            m_componentType = FileInfoType.type_WWE;
                        } else if ((m = regGWSPIDHost.matcher(str)).find()) {
                            m_host = m.group(2);
                        } else if (fileStartTime == null && (m = regGWSStart.matcher(str)).find()) {
                            try {
                                fileStartTime = dateParsers.parseFormatDate(m.group(1));
                            } catch (Exception ex) {
                                logger.error("fatal: ",  ex);
                            }
                        } else if (m_componentType == FileInfoType.type_WWE) {
                            if (str.startsWith("+++++++++++++++++++++++++++++++++++++++++")) {
                                parserState = ParserState.WWE_DATE;
                            }
                        } else if ((ApacheWebLogsParser.getENTRY_BEGIN_PATTERN().matcher(str)).find()) {
                            parserState = ParserState.APACHE_LOG_SUSPECT;
                        } else if (RegAppCommandLine.matcher(str).find()) { // New configServer log does not have Application Type clause
                            m_componentTypeSuspect = FileInfoType.type_ConfServer;
                        } else if (m_componentType == FileInfoType.type_Unknown) {
                            if (suspectedWWECloudLines > 5) {
                                // not sure about the difference between WWE and WWECloud files. Assuming they are all WWE
                                m_componentType = FileInfoType.type_WWE;
                                appType = FileInfoType.getFileType(m_componentType);
                                (new File(FilenameUtils.normalize(this.filePath))).getParentFile().getName();
                                logFileName = ((new File(FilenameUtils.normalize(this.filePath))).getParentFile().getName())
                                        + File.separatorChar + m_name;
                                setM_app(fileDir);
                                m_host = fileDir;
                                doBreak = true;
                            } else {
                                if (regWWECloud.matcher(str).find()) {
                                    suspectedWWECloudLines++;
                                }
                            }
                        }
                        break;
                    }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="WWE_DATE">
                    case WWE_DATE: {
                        try {
                            fileLocalTime = dateParsers.parseFormatDate(str);
                            if (fileLocalTime != null) {
                                doBreak = true;
                            }
                        } catch (Exception ex) {
                            logger.error("fatal: ",  ex);
                            doBreak = true;
                        }
                        break;
                    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="APACHE_LOG_SUSPECT">

                    case APACHE_LOG_SUSPECT: {
                        if (num >= NUM_PARAMS_TO_READ) {
                            m_componentType = FileInfoType.type_ApacheWeb;
                            m_app = "ApacheLogs@" + (new File(fileDir)).getName();

//                            m_app=
                            doBreak = true;
                        } else if (!(ApacheWebLogsParser.getENTRY_BEGIN_PATTERN().matcher(str)).find()) {
                            parserState = ParserState.STATE_HEADER;
                            doBreak = true;
                        }
                        break;
                    }
//</editor-fold>
                }
            }

            if (m_ServerStartTimeSet) {
                String temp = m_app + m_host + m_runId;
                m_runId = temp.hashCode();
            }

            if (m_componentType == FileInfoType.type_tController) {
                int i = 0;
                do {
                    str = input.readLine();
                    i++;
                } while (str != null && !str.contains("My Node ID:") && i < 100);
                if (str != null && str.contains("My Node ID:")) {
                    m_nodeId = str.trim().substring(26);
                }
            }
        } catch (IOException e) {
            Main.logger.error("Error checking file " + m_name + ": ", e);
        }

        if (m_componentType == FileInfoType.type_Unknown) {
            if (appType.equals("0") && (m_app != null && m_app.equals("InteractionWorkspaceSIPEndpoint.exe"))) {
                m_componentType = FileInfoType.type_SIPEP;
                appType = FileInfoType.getFileType(m_componentType);
                logFileName = m_name;
            } else if (m_componentTypeSuspect != FileInfoType.type_Unknown) {
                m_componentType = m_componentTypeSuspect;
                if (m_app == null && m_componentType == FileInfoType.type_ConfServer) {
                    m_app = "confserv";
                }

            }
        }

        return m_componentType;
    }

    public String getM_app() {
        switch (getM_componentType()) {
            case type_SIPEP:
                return "SIPEP@" + getHost();

            case type_WWE:
                return FileInfoType.getFileType(FileInfoType.type_WWE) + "@" + getHost();

            default:
                return m_app;
        }
    }

    public void setM_app(String app) {
        Main.logger.debug("setting app: [" + app + "]");
        if (app.equals("httpinterface")) {
            this.m_componentType = FileInfoType.type_URSHTTP; // postponing name set until we finish analyzing

        } else {
            this.m_app = app;
        }
    }

    public int getLogFileNo() {
        try {
            return Integer.parseInt(logFileNo);
        } catch (NumberFormatException numberFormatException) {
            return 0;
        }
    }

    public String getLogFileName() {
        return logFileName;
    }

    public void setFileFilter(String component) {
        if (component.equalsIgnoreCase("sc")) {
            m_fileFilterId = FileInfoType.type_SessionController;
        } else if (component.equalsIgnoreCase("tc")) {
            m_fileFilterId = FileInfoType.type_tController;
        } else if (component.equalsIgnoreCase("sip")) {
            m_fileFilterId = FileInfoType.type_CallManager;
        } else if (component.equalsIgnoreCase("proxy")) {
            m_fileFilterId = FileInfoType.type_SipProxy;
        }
    }

    /*
    public void AddToDB(DBAccessor accessor) {
        try {
            PreparedStatement stmt = accessor.GetStatement(m_batchId);
            System.out.println("processing file : " + m_path);

            setFieldString(stmt,1, m_path);
            stmt.setLong(2, m_runId);
            stmt.setInt(3, m_componentType);
            setFieldString(stmt,4, m_nodeId);
            setFieldString(stmt,5, m_app);
            setFieldString(stmt,6, m_host);
            ++m_fileId;
            accessor.SubmitStatement(m_batchId);
        } catch (Exception e) {
            System.out.println("SetFileInfo failed: " + e);
        }
    }
     */
    void IncreaseFileID() {
        fileId++;
    }

    boolean alreadyProcessed(long length) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public long getSize() {
        return size;
    }

    void setSize(long length) {
        size = length;
    }

    long getStartTime() {
        if (fileStartTime != null && fileStartTime.fmtDate != null) {
            return fileStartTime.getUTCms();
        }
        return 0;
    }

    FileInfoType CheckLog(InputStream is) throws IOException {
        BufferedReaderCrLf input = new BufferedReaderCrLf(is);
        m_componentType = CheckLog(input);
        input.close();
        return m_componentType;
    }

    long getEndTime() {
        if (fileEndTime != null && fileEndTime.fmtDate != null) {
            return fileEndTime.getUTCms();
        }
        return 0;
    }

    long getFileStartTimeLong() {
        if (fileLocalTime != null && fileLocalTime.fmtDate != null) {
            return fileLocalTime.getUTCms();
        }
        return 0;
    }

    /**
     * Rounds start up time to full minutes
     *
     * @return
     */
    long getFileStartTimeRound() {
//        long dt = 0;
//        dt = getStartTime();
//        if (dt > 0) {
//            double r = Math.ceil(dt / 1000 / 60);
//            long ret = Math.round(r);
//            Main.logger.debug("file: " + this.toString() + " dt: "
//                    + dt + " ret: " + ret
//                    + " before ceil: " + dt / 1000 / 60
//                    + " 1: " + dt / 1000
//                    + " 2: " + Math.round(dt / 1000) % 60
//                    + " 3: " + (dt / 1000) % 60
//            );
//            return ret;
//        }
//
//        return 0;
//        getFileStartTimeRound(getFileStartTimeLong());
        return getFileStartTimeRound(getStartTime());
    }

    private long getFileStartTimeRound(long dt) {
        if (dt > 0) {
            return Math.round(dt / 1000 / 60);
//            double r = Math.ceil(dt / 1000 / 60);
//            long ret = Math.round(r);
//            long sec1 = dt / 1000;
//            long sec = (dt / 1000) % 60;
//            sec1 -= sec;
//            if (sec > 30) {
//                sec1 += 60;
//            }
//            Main.logger.debug("file: " + this.toString() + " dt: "
//                    + dt + " ret: " + ret
//                    + " 1: " + dt / 1000
//                    + " 2: " + Math.round(dt / 1000 / 60)
//                    + " 3: " + (dt / 1000) % 60
//                    + " 4: " + sec1
//                    + " 5: " + sec
//                    + " 6: " + sec1 / 60
//            );
//            return ret;
        }

        return 0;
    }

    int getAppNameID() {
        if (appID == 0) {
            this.appID = Main.getRef(ReferenceType.App, m_app);
        }

        return appID;

    }

    private FileInfoType checkWorkSpace(BufferedReaderCrLf input) {
        logFileName = m_name;
        logFileNo = "";
        m_componentType = FileInfoType.type_WorkSpace;
        appType = FileInfoType.getFileType(m_componentType);
        setM_app(FileInfoType.type_WorkSpace.name());
        Matcher m;

        if ((m = regFileNameDate.matcher(logFileName)).find()) {
            String d = m.group(1);

            try {
                fileStartTime = new DateParsed(logFileName, d, "", LocalDateTime.parse(d, workSpaceDateTime));
            } catch (DateTimeParseException ex) {
                fileStartTime = null;
                logger.error("fatal: ",  ex);
            }
        }

//        buf = new byte[WORKSPACE_BYTES];
//        try {
//            bufRead = input.read(buf, WS_BYTES.length, WORKSPACE_BYTES);
//        } catch (IOException iOException) {
//        }
        return m_componentType;
    }

    protected enum FileType {
        SIP_1536,
        UNKNOWN
    }

    private enum ParserState {

        STATE_HEADER,
        WWE_DATE,
        APACHE_LOG_SUSPECT

    }

}
