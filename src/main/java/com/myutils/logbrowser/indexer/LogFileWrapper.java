/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author Stepan
 */
public abstract class LogFileWrapper {

    public static LogFileWrapper getContainer(File file) {
        LogFileWrapper ret = null;
        try {
            if (file.getName().toLowerCase().endsWith(".log")) {
                ret = new TextLog(file);
            } else if (!Main.isIgnoreZIP()) {
                if (file.getName().toLowerCase().endsWith(".zip")) {
                    return new ZIPLog(file);
                }
            }
        } catch (IOException ex) {
            Main.logger.error("Cannot determine type of file " + file, ex);
        }
        if (ret != null && !ret.isIgnoreLog()) {
            return ret;
        }

        return null;
    }

    private final File file;

    private boolean ignoreLog = false;
    private final HashMap<FileInfo, Object> fileInfoFileMap = new HashMap<>();

    LogFileWrapper(File file) throws IOException {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public abstract void close();

    public abstract void open() throws IOException;

    public boolean isIgnoreLog() {
        return ignoreLog;
    }

    public void setIgnoreLog(boolean ignoreLog) {
        this.ignoreLog = ignoreLog;
    }

    protected void addFileInfo(FileInfo fi, File file) {
        fileInfoFileMap.put(fi, file);
    }

    public Set<FileInfo> getFileInfos() {
        return fileInfoFileMap.keySet();
    }

    abstract InputStream getInputStream(FileInfo aThis) throws FileNotFoundException;

    /**
     * Is called for each file if Wrapper contains several
     *
     * @throws Exception
     */
    abstract void startParsing() throws Exception;

    /**
     * Is called for each file if Wrapper contains several
     *
     * @throws Exception
     */
    abstract void doneParsing() throws Exception;

    public String getName() {
        return file.getPath();
    }

}
