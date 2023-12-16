/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.common;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Paths;

/**
 * This class should not contain Logger!!!
 *
 * @author stepan_sydoruk
 */
public class ExecutionEnvironment {
    private Boolean parseTDiff = "true".equalsIgnoreCase(System.getProperty("timediff.parse"));
    private boolean ignoreZIP = false;
    private int maxThreads;
    private int httpPort;

    private String baseDir = Paths.get(".").toAbsolutePath().normalize().toString();
    private String xmlCFG = ".";
    private String alias = "logbr";
    private String dbname = "logbr";
    private String logbrowserDir;
    private boolean sqlPragma;

    public ExecutionEnvironment() {
        String _dir = System.getProperty("logbr.dir");
        if (StringUtils.isEmpty(_dir)) {
            _dir = ".logbr";
        }

        logbrowserDir = Paths.get(_dir).toAbsolutePath().normalize().toString();
        maxThreads = defProcessingThreads();
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public boolean isParseTDiff() {
        return parseTDiff;
    }

    public void setParseTDiff(boolean parseTDiff) {
        this.parseTDiff = parseTDiff;
    }

    public boolean isIgnoreZIP() {
        return ignoreZIP;
    }

    public void setIgnoreZIP(boolean ignoreZIP) {
        this.ignoreZIP = ignoreZIP;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getXmlCFG() {
        return xmlCFG;
    }

    public void setXmlCFG(String xmlCFG) {
        this.xmlCFG = xmlCFG;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getDbname() {
        return dbname;
    }

    public void setDbname(String dbname) {
        this.dbname = dbname;
    }

    public String getLogbrowserDir() {

        return logbrowserDir;
    }

    public void setLogbrowserDir(String _dir) {
        if (StringUtils.isNotEmpty(_dir))
            this.logbrowserDir = Paths.get(_dir).toAbsolutePath().normalize().toString();
    }

    public boolean isSqlPragma() {
        return sqlPragma;
    }

    public void setSqlPragma(boolean sqlPragma) {
        this.sqlPragma = sqlPragma;
    }

    protected int defProcessingThreads() {
        int processors = Runtime.getRuntime().availableProcessors();
        return (processors > 1) ? processors / 2 : 1;
    }


    @Override
    public String toString() {
        return "ExecutionEnvironment{" +
                "parseTDiff=" + parseTDiff +
                ", ignoreZIP=" + ignoreZIP +
                ", maxThreads=" + maxThreads +
                ", baseDir='" + baseDir + '\'' +
                ", xmlCFG='" + xmlCFG + '\'' +
                ", alias='" + alias + '\'' +
                ", dbname='" + dbname + '\'' +
                ", logbrowserDir='" + logbrowserDir + '\'' +
                ", sqlPragma=" + sqlPragma +
                '}';
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }
}
