/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import static org.apache.commons.compress.compressors.gzip.GzipUtils.getCompressedFileName;

/**
 * @author Stepan
 */
public final class SevenZIPLog extends LogFileWrapper {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;
    int filesToProcess = 0;
    private int filesProcessed;
    private InputStream curStream = null;
    private SevenZFile sevenZFile = null;

    public SevenZIPLog(File file, String baseDir) throws IOException {
        super(file, baseDir);
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
        if (sevenZFile != null) {
            try {
                sevenZFile.close();
            } catch (IOException ex) {
                Main.logger.fatal("", ex);
            }
            sevenZFile = null;
        }
    }

    @Override
    public void open() throws IOException {
        boolean goodFileFound = false;

        try (SevenZFile sevenZFile = SevenZFile.builder().setFile(getFile()).get()) {
            SevenZArchiveEntry entry;
            if ((entry = sevenZFile.getNextEntry()) != null) {

                FileInfo fi = new FileInfo(this, entry.getName(), entry.getSize());
                if (fi.CheckLog(sevenZFile.getInputStream(entry)) != FileInfoType.type_Unknown) {
                    goodFileFound = true;
                    addFileInfo(fi, null);
                } else {
                    Main.logger.info("Ignored " + getFile() + " / " + getFile().getAbsolutePath());
                }
                // Create directory before streaming files.

            }
        }
        setIgnoreLog(goodFileFound);
    }

    @Override
    InputStream getInputStream(FileInfo fi) throws FileNotFoundException {
        try {
            if (curStream == null) {
                if (sevenZFile == null) {
                    sevenZFile = SevenZFile.builder().setFile(getFile()).get();
                }
                SevenZArchiveEntry entry = null;

                if ((entry = sevenZFile.getNextEntry()) != null) {
                    curStream = IOUtils.buffer(sevenZFile.getInputStream(entry));
                    return curStream;
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(SevenZIPLog.class.getName()).log(Level.SEVERE, null, ex);
            throw new FileNotFoundException(getFile().getName());
        }
        return null;
    }

    @Override
    void startParsing() throws Exception {

        for (FileInfo fileInfo : getFileInfos()) {
            if (!fileInfo.getIgnoring()) {
                filesToProcess++;
            }
        }
        if (filesToProcess > 0) {
//            logArchive = new ZipFile(getFile(), ZipFile.OPEN_READ);
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
//            if (logArchive != null) {
//                logArchive.close();
//            }
        }
    }

}
