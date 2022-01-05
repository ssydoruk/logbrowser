/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import com.myutils.logbrowser.common.ExecutionEnvironment;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

/**
 * @author stepan_sydoruk
 */
public class EnvIndexer extends ExecutionEnvironment {
    protected final Options options;
    protected final CommandLineParser parser;
    protected CommandLine cmd;
    Option optHelp;
    private Option optMaxThreads;
    private Option optXMLCfg;
    private Option optAlias;
    private Option optDBName;
    private Option optLogsBaseDir;
    private Option optIgnoreZIP;
    private Option optTDiffParse;
    private Option optLogBrowserDir;
    private Option optSQLPragma;


    EnvIndexer() {
        super();
        this.options = new Options();
        this.parser = new DefaultParser();


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

        optMaxThreads = Option.builder()
                .hasArg(true)
                .required(false)
                .type(Integer.class)
                .desc("Number of parsing threads")
                .longOpt("threads")
                .build();
        options.addOption(optMaxThreads);

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

    protected int getIntOrDef(Option opt, int def) {
        Integer ret = null;
        if (StringUtils.isNotEmpty(opt.getLongOpt())) {
            String s = cmd.getOptionValue(opt.getLongOpt());
            if (StringUtils.isNotEmpty(s))
                ret = Integer.parseInt(s);
        }
        if (ret == null && StringUtils.isNotEmpty(opt.getOpt())) {
            String s = cmd.getOptionValue(opt.getOpt());
            if (StringUtils.isNotEmpty(s))
                ret = Integer.parseInt(s);
        }
        return (ret != null) ? ret : def;
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


    private String getOptValueDBName() {
        return getStringOrDef(optDBName, "logbr");
    }

    private String getOptValueXMLCfg() {
        return getStringOrDef(optXMLCfg, ".");
    }

    private String getOptBaseDir() {
        return getStringOrDef(optLogsBaseDir, null);
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

        setAlias(getOptionAlias());
        setDbname(getOptValueDBName());

        setIgnoreZIP(getOptIsIgnoreZIP());

        setMaxThreads(getOptMaxThreads());

        setXmlCFG(getOptValueXMLCfg());
        setBaseDir(getOptBaseDir());

        setParseTDiff(getOptValueParseTDiff());
        setLogbrowserDir(getOptLogBrowserDir());

        setSqlPragma(getOptSQLPragma());
    }

    private Integer getOptMaxThreads() {
        Integer ret = getIntOrDef(optMaxThreads, defProcessingThreads());
        return ret;
    }

    private int defProcessingThreads() {
        int processors = Runtime.getRuntime().availableProcessors();
        return (processors > 1) ? processors / 2 : 1;
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


    private boolean getOptIsIgnoreZIP() {
        return cmd.hasOption(optIgnoreZIP.getLongOpt());
    }


    private String getOptLogBrowserDir() {
        return getStringOrDef(optLogBrowserDir, null);
    }

}
