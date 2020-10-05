/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import com.myutils.logbrowser.common.ExecutionEnvironment;
import java.nio.file.Paths;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author stepan_sydoruk
 */
public class EnvIndexer extends ExecutionEnvironment {

    private final Option optXMLCfg;
    private final Option optAlias;
    private final Option optDBName;
    private final Option optLogsBaseDir;
    private final Option optIgnoreZIP;
    private final Option optTDiffParse;
    private boolean parseTDiff = false;
    private boolean ignoreZIP = false;

    Option optHelp;
    private Option optLogBrowserDir;
    private final Option optSQLPragma;

    public String getBaseDir() {
        return baseDir;
    }
    private String baseDir;
    private String xmlCFG;
    private String alias;
    private String dbname;
    private String logbrowserDir;

    public String getLogbrowserDir() {
        return logbrowserDir;
    }

    private boolean sqlPragma;

    EnvIndexer() {
        super();

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

        optSQLPragma = Option.builder()
                .hasArg(false)
                .required(false)
                .desc("If specified, use SQL pragmas to speedup sql database operations")
                .longOpt("sqlite.pragma")
                .build();
        options.addOption(optSQLPragma);

        optLogBrowserDir = Option.builder().hasArg(true).required(false)
                .desc("Directory for storing logbrowser temp and log files. Default to current")
                .longOpt("logbr.dir").valueSeparator('=').build();
        options.addOption(optLogBrowserDir);

    }

    public void printHelp() {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("indexer [params] <working directory>", "header", options, "footer");
    }

    private void showHelpExit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("indexer", options);

        System.exit(0);
    }

    private String getOptionAlias() {
        return getStringOrDef(optAlias, "logbr");
    }

    public boolean isParseTDiff() {
        return parseTDiff;
    }

    private String getOptValueDBName() {
        return getStringOrDef(optDBName, "logbr");
    }

    private String getOptValueXMLCfg() {
        return getStringOrDef(optXMLCfg, ".");
    }

    private String getOptBaseDir() {
        return getStringOrDef(optLogsBaseDir, Paths.get(".").toAbsolutePath().normalize().toString());
    }

    public boolean isIgnoreZIP() {
        return ignoreZIP;
    }

    ;

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

        alias = getOptionAlias();
        dbname = getOptValueDBName();

        ignoreZIP = getOptIsIgnoreZIP();

        xmlCFG = getOptValueXMLCfg();
        baseDir = getOptBaseDir();

        parseTDiff = getOptValueParseTDiff();
        logbrowserDir = getOptLogBrowserDir();

        baseDir = getOptBaseDir();

        sqlPragma = getOptSQLPragma();
    }


    public String getXmlCFG() {
        return xmlCFG;
    }

    public String getDbname() {
        return dbname;
    }

    private boolean getOptValueParseTDiff() {

        if (StringUtils.isNotEmpty(optTDiffParse.getLongOpt())
                && cmd.hasOption(optTDiffParse.getLongOpt())) {
            return true;
        }
        return "true".equalsIgnoreCase(System.getProperty("timediff.parse"));
    }

    private boolean getOptSQLPragma() {

        if (StringUtils.isNotEmpty(optSQLPragma.getLongOpt())
                && cmd.hasOption(optSQLPragma.getLongOpt())) {
            return true;
        }
        return "true".equalsIgnoreCase(System.getProperty("sqlite.pragma"));
    }

    public boolean isSqlPragma() {
        return sqlPragma;
    }

    private boolean getOptIsIgnoreZIP() {
        return cmd.hasOption(optIgnoreZIP.getLongOpt());

    }

    public String getAlias() {
        return alias;
    }

    private String getOptLogBrowserDir() {
        String ret = getStringOrDef(optLogBrowserDir, null);
        if (ret == null) {
            ret = System.getProperty("logbr.dir");
        }
        if (StringUtils.isEmpty(ret)) {
            ret = ".logbr";
        }

        return Paths.get(ret).toAbsolutePath().normalize().toString();
    }

}
