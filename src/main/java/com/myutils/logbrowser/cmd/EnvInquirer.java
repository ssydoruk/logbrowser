/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.cmd;

import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author stepan_sydoruk
 */
public class EnvInquirer {

    private final Option optDBName;
    private final Option optIsRegex;
    private final Option optSearchString;
    private final Option optTables;
    protected Options options;
    protected CommandLineParser parser;
    protected CommandLine cmd;
    Option optHelp;
    private String dbname;
    private boolean isRegex;
    private String search;

    private ArrayList<String> tables;
    private final Option optStartDirectory;
    private String startDirectory;
    private final Option optDBNamePattern;
    private String fileNamePattern;
    static private final String DEF_PATTERM = "*.db";
    private final Option optDBPathPattern;
    private String filePathPattern;

    public boolean isRegex() {
        return isRegex;
    }

    public String getSearch() {
        return search;
    }

    public ArrayList<String> getTables() {
        return tables;
    }

    EnvInquirer() {
        this.options = new Options();
        this.parser = new DefaultParser();

        optHelp = Option.builder("h").hasArg(false).required(false).desc("Show help and exit").longOpt("help").build();
        options.addOption(optHelp);

        optDBName = Option.builder("d").hasArg(true).required(false).desc("Name of the SQLite database file")
                .valueSeparator('=').longOpt("dbname").build();
        options.addOption(optDBName);

        optStartDirectory = Option.builder("i").hasArg(true).required(false).desc("Initial directory. If not specified, using current")
                .valueSeparator('=').longOpt("initdir").build();
        options.addOption(optStartDirectory);

        optDBNamePattern = Option.builder("p").hasArg(true).required(false).desc("DB name pattern against file name (shell style) if searching. Default to [" + DEF_PATTERM + "]")
                .valueSeparator('=').longOpt("pattern").build();
        options.addOption(optDBNamePattern);

        optDBPathPattern = Option.builder("f").hasArg(true).required(false).desc("Path pattern (java style) if searching.")
                .valueSeparator('=').longOpt("namepattern").build();
        options.addOption(optDBPathPattern);

        optIsRegex = Option.builder("x").hasArg(false).required(false).desc("Search expression is regular expression")
                .longOpt("regex").valueSeparator('=').build();
        options.addOption(optIsRegex);

        optSearchString = Option.builder("s").hasArg(true).required(true).longOpt("search").valueSeparator(' ')
                .desc("what to search for").build();
        options.addOption(optSearchString);

        optTables = Option.builder("t").hasArg().required(false).longOpt("tables").valueSeparator(' ').desc(
                "Comma separated list of reftables to search in. Can be specified multiple times. Will search in all if not specified or value is 'all'")
                .build();
        options.addOption(optTables);
    }

    public String getDbname() {
        return dbname;
    }

    public void printHelp() {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp("indexer [params] <working directory>", "header", options, "footer");
    }

    private void showHelpExit(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("./app", "where options are", options, "---\nFinal argument can be list of directories to search in", true);

        System.exit(0);
    }

    private String getOptDBName() {
        return getStringOrDef(optDBName, "");
    }

    private String getOptStartDirectory() {
        return getStringOrDef(optStartDirectory, ".");
    }

    private String getOptFileNamePattern() {
        return getStringOrDef(optDBNamePattern, DEF_PATTERM);
    }

    private String getOptFilePathPattern() {
        return getStringOrDef(optDBPathPattern, "");
    }
    public String getFileNamePattern() {
        return fileNamePattern;
    }

    private String getOptSearchString() {
        return getStringOrDef(optSearchString, "");
    }

    private boolean getIsRegex() {
        return getBoolOptionOrDef(optIsRegex, false);
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

    private boolean getBoolOptionOrDef(Option opt, boolean def) {
        boolean ret = cmd.hasOption(opt.getLongOpt());
        return (ret || def);
    }

    public void parserCommandLine(String[] args) {
        try {
            cmd = parser.parse(options, args, false);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            showHelpExit(options);
        }
        if (cmd.hasOption(optHelp.getLongOpt())) {
            showHelpExit(options);
        }

        dbname = getOptDBName();

        isRegex = getIsRegex();

        search = getOptSearchString();

        tables = getOptTables();

        startDirectory = getOptStartDirectory();

        fileNamePattern = getOptFileNamePattern();
        
        filePathPattern = getOptFilePathPattern();

    }

    public List<String> getDirList(){
        return cmd.getArgList();
    }
    
    
    public String getFilePathPattern() {
        return filePathPattern;
    }

    public String getStartDirectory() {
        return startDirectory;
    }

    /**
     *
     * @return null if all tables, otherwise ArrayList of strings
     */
    private ArrayList<String> getOptTables() {
        HashSet<String> ret = new HashSet<>();

        String[] optionValues = cmd.getOptionValues(optTables);
        if (ArrayUtils.isNotEmpty(optionValues)) {
            for (String s : Arrays.stream(optionValues).toList()) {
                ret.addAll(Arrays.stream(StringUtils.split(s.toLowerCase(), ",")).toList());
            }
            return ret.isEmpty() || ret.contains("all") ? null : new ArrayList<>(ret.stream().toList());
        } else {
            return null;
        }
    }
}
