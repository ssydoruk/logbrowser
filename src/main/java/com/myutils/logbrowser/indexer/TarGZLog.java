/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;

/**
 * @author Stepan
 */
public final class TarGZLog extends LogFileWrapper {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;
    int filesToProcess = 0;
    private int filesProcessed;
    private InputStream curStream = null;

    public TarGZLog(File file, String baseDir) throws IOException {
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
    }

    @Override
    public void open() throws IOException {
        boolean goodFileFound = false;

        TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(getFile())));
        TarArchiveEntry entry = tarInput.getNextTarEntry();
        if (entry != null && !entry.isDirectory()) {
            FileInfo fi = new FileInfo(this, entry.getName(), entry.getSize());
            if (fi.CheckLog(tarInput) != FileInfoType.type_Unknown) {
                goodFileFound = true;
                addFileInfo(fi, null);
            } else {
                Main.logger.info("Ignored " + getFile() + " / " + entry.getName());
            }
//                entry = tarInput.getNextTarEntry(); // You forgot to iterate to the next file
//            tarInput.skip(entry.getSize());
//            try {
//                entry = tarInput.getNextTarEntry(); // You forgot to iterate to the next file
//            }
//            catch (IOException e){
//                logger.error("error reading file: ", e);
//            }
        }
        setIgnoreLog(goodFileFound);
    }

    @Override
    InputStream getInputStream(FileInfo fi) throws FileNotFoundException {
        TarArchiveEntry entry = null;
        try {
            TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(getFile())));

            do {
                entry = tarInput.getNextTarEntry(); // You forgot to iterate to the next file
            } while (entry != null && !entry.isDirectory() && !entry.getName().equals(fi.getM_name()));
            if (entry == null)
                throw new FileNotFoundException("No file: " + fi.getM_name());
            curStream = IOUtils.buffer(tarInput);
            return curStream;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
