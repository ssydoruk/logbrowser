/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import com.myutils.logbrowser.common.ExecutionEnvironment;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Paths;

/**
 * @author stepan_sydoruk
 */
public class EnvInquirer  {
    protected  Options options;
    protected  CommandLineParser parser;

    private final Option optOutSpec;
    private final Option optAlias;
    private final Option optDBName;
    private final Option optDisableRefSync;
    private final Option optLogsBaseDir;
    private final Option optAppConfigFile;
    private final Option optIgnoreFileAccessErrors;
    private final Option optLogBrowserDir;
    private final Option optNoTLibRequests;
    Option optHelp;
    private String alias;
    private String dbname;
    private boolean noTLibRequest;
    private String outputSpecFile;
    private String config;
    private String baseDir;
    private boolean refSyncDisabled;
    private boolean ignoreFileAccessErrors;
    private String logBrDir;
    protected CommandLine cmd;


    EnvInquirer() {
        this.options = new Options();
        this.parser = new DefaultParser();

        optHelp = Option.builder("h").hasArg(false).required(false).desc("Show help and exit").longOpt("help")
                .build();
        options.addOption(optHelp);

        optIgnoreFileAccessErrors = Option.builder().hasArg(false).required(false)
                .desc("If set, ignore file access errors").longOpt("ignore-file-errors").build();
        options.addOption(optIgnoreFileAccessErrors);

        optOutSpec = Option.builder("x").hasArg(true).required(true).desc("XML config file for output formatting")
                .valueSeparator('=').longOpt("outputspec").build();
        options.addOption(optOutSpec);

        optAlias = Option.builder("a").hasArg(true).required(false).desc("Alias to use").valueSeparator('=')
                .longOpt("alias").build();
        options.addOption(optAlias);

        optDBName = Option.builder("d").hasArg(true).required(false).desc("Name of the SQLite database file")
                .valueSeparator('=').longOpt("dbname").build();
        options.addOption(optDBName);

        optDisableRefSync = Option.builder().hasArg(false).required(false)
                .desc("If set, disable sync of references; to be used if database is large").longOpt("no-ref-sync")
                .build();
        options.addOption(optDisableRefSync);

        optLogsBaseDir = Option.builder("b").hasArg(true).required(false)
                .desc("Basic directory for logs. Default to current").longOpt("basedir").valueSeparator('=')
                .build();
        options.addOption(optLogsBaseDir);

        optLogBrowserDir = Option.builder().hasArg(true).required(false)
                .desc("Directory for storing logbrowser temp and log files. Default to current")
                .longOpt("logbr.dir").valueSeparator('=').build();
        options.addOption(optLogBrowserDir);

        optAppConfigFile = Option.builder("c").hasArg(true).required(true)
                .desc("Configuration file for the application").longOpt("config").valueSeparator('=').build();
        options.addOption(optAppConfigFile);

        optNoTLibRequests = Option.builder()
                .hasArg(false)
                .required(false)
                .desc("If specified, do not search for TLibRequests")
                .longOpt("tlib.norequest")
                .build();
        options.addOption(optNoTLibRequests);

    }

    public String getAlias() {
        return alias;
    }

    public String getDbname() {
        return dbname;
    }

    public String getOutputSpecFile() {
        return outputSpecFile;
    }

    public String getConfig() {
        return config;
    }

    public boolean isRefSyncDisabled() {
        return refSyncDisabled;
    }

    public String getLogBrDir() {
        return logBrDir;
    }

    private boolean getOptNoTLibRequest() {

        if (StringUtils.isNotEmpty(optNoTLibRequests.getLongOpt())
                && cmd.hasOption(optNoTLibRequests.getLongOpt())) {
            return true;
        }
        return "true".equalsIgnoreCase(System.getProperty("tlib.norequest"));
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

    private String getOptAlias() {
        return getStringOrDef(optAlias, "logbr");
    }

    private boolean isDisableRefSync() {
        return cmd.hasOption(optDisableRefSync.getLongOpt());
    }

    private String getOptDBName() {
        return getStringOrDef(optDBName, "logbr");
    }

    private String getXmlCfg() {
        return getStringOrDef(optOutSpec, "");
    }

    private String getOptValBaseDir() {
        return getStringOrDef(optLogsBaseDir, Paths.get(".").toAbsolutePath().normalize().toString());
    }

    protected String getStringOrDef(Option opt, String def) {
        String ret = null;
        try {
            if (StringUtils.isNotEmpty(opt.getLongOpt())) {
                ret = (String) cmd.getParsedOptionValue(opt.getLongOpt());
            }
            if (StringUtils.isEmpty(ret) && StringUtils.isNotEmpty(opt.getOpt())) {
                ret = (String) cmd.getParsedOptionValue(opt.getOpt());
            }
        } catch (ParseException ex) {
            System.out.println(ex);
        }
        return (StringUtils.isNotBlank(ret)) ? ret : def;
    }

    private String getAppConfigFile() {
        return getStringOrDef(optAppConfigFile, "");
    }

    private boolean getBoolOptionOrDef(Option opt, boolean def) {
        boolean ret = cmd.hasOption(opt.getLongOpt());
        return (ret || def);
    }

    public boolean isIgnoreFileAccessErrors() {
        return ignoreFileAccessErrors;
    }

    public void parserCommandLine(String[] args) {
        try {
            cmd = parser.parse(options, args, true);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            showHelpExit(options);
        }
        if (cmd.hasOption(optHelp.getLongOpt())) {
            showHelpExit(options);
        }

        alias = getOptAlias();
        dbname = getOptDBName();
        if (!dbname.startsWith(File.separator)) {
            dbname = FilenameUtils.concat(baseDir, dbname);
        }
        outputSpecFile = getXmlCfg();
        config = getAppConfigFile();
        baseDir = getOptValBaseDir();
        refSyncDisabled = isDisableRefSync();
        ignoreFileAccessErrors = getOptIgnoreFileAccessErrors();
        logBrDir = getLogbrowserDir();
        noTLibRequest = getOptNoTLibRequest();

        if (StringUtils.isEmpty(logBrDir)) {
            logBrDir = Utils.FileUtils.getCurrentDirectory() + File.pathSeparator + ".logbr";
        }

    }

    public boolean isNoTLibRequest() {
        return noTLibRequest;
    }

    public String getBaseDir() {
        return baseDir;
    }

    private boolean getOptIgnoreFileAccessErrors() {
        return getBoolOptionOrDef(optIgnoreFileAccessErrors, false);
    }

    private String getLogbrowserDir() {
        String ret = getStringOrDef(optLogBrowserDir, null);
        if (StringUtils.isEmpty(ret)) {
            ret = System.getProperty("logbr.dir");
        }
        if (StringUtils.isEmpty(ret)) {
            ret = ".logbr";
        }
        return (new File(ret).isAbsolute()) ? ret : FilenameUtils.concat(getBaseDir(), ret);
    }
}
