/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.common;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author stepan_sydoruk
 */
public class ExecutionEnvironment {

    protected CommandLine cmd;
    protected final Options options;
    protected final CommandLineParser parser;

    public ExecutionEnvironment() {
        this.options = new Options();
        this.parser = new DefaultParser();
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
            logger.log(Level.FATAL, ex);
        }
        return (StringUtils.isNotBlank(ret)) ? ret : def;
    }

    private static final Logger logger = LogManager.getLogger("");

}
