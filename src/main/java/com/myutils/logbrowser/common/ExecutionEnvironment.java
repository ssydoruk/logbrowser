/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.common;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

/**
 * This class should not contain Logger!!!
 *
 * @author stepan_sydoruk
 */
public class ExecutionEnvironment {

    protected final Options options;
    protected final CommandLineParser parser;
    protected CommandLine cmd;

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
            System.out.println(ex);
        }
        return (StringUtils.isNotBlank(ret)) ? ret : def;
    }

}
