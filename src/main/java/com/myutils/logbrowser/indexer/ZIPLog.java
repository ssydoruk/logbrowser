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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author Stepan
 */
public final class ZIPLog extends LogFileWrapper {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;

    private int filesProcessed;
    private InputStream curStream = null;
    int filesToProcess = 0;
    ZipFile logArchive = null;

    public ZIPLog(File file) throws IOException {
        super(file);
        open();
    }

    @Override
    public void close() {
        if (curStream != null) {
            try {
                curStream.close();
            } catch (IOException ex) {
                Main.logger.fatal("", ex);
            }
            curStream = null;
        }
    }

    @Override
    public void open() throws IOException {
        boolean goodFileFound;
        try (ZipFile theArchive = new ZipFile(getFile(), ZipFile.OPEN_READ)) {
            Enumeration e = theArchive.entries();
            goodFileFound = false;
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (!entry.isDirectory()) {
                    FileInfo fi = new FileInfo(theArchive, entry, this);
                    try (InputStream inputStream = theArchive.getInputStream(entry)) {
                        if (fi.CheckLog(inputStream) != FileInfoType.type_Unknown) {
                            goodFileFound = true;
                            addFileInfo(fi, null);
                        } else {
                            Main.logger.info("Ignored " + getFile() + " / " + entry.getName());
                        }
                    }
                }
            }
        }
        setIgnoreLog(goodFileFound);
    }

    @Override
    InputStream getInputStream(FileInfo aThis) throws FileNotFoundException {
        ZipEntry entry = logArchive.getEntry(aThis.getM_name());
        if (entry == null) {
            throw new FileNotFoundException("No file: " + aThis.getM_name());
        }

        try {
            curStream = IOUtils.buffer(logArchive.getInputStream(entry));
        } catch (IOException ex) {
            logger.log(org.apache.logging.log4j.Level.FATAL, ex);
            return null;
        }
        return curStream;

    }

    @Override
    void startParsing() throws Exception {

        for (FileInfo fileInfo : getFileInfos()) {
            if (!fileInfo.getIgnoring()) {
                filesToProcess++;
            }
        }
        if (filesToProcess > 0) {
            logArchive = new ZipFile(getFile(), ZipFile.OPEN_READ);
            filesProcessed = 0;
        }

    }

    @Override
    void doneParsing() throws Exception {
        if (curStream != null) {
            try {
                curStream.close();
            } catch (IOException iOException) {
                Main.logger.error("Error closing stream: ", iOException);
            }
        }
        filesProcessed++;
        if (filesProcessed >= filesToProcess) {
            if (logArchive != null) {
                logArchive.close();
            }
        }
    }

}
