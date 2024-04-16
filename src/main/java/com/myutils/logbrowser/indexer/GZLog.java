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

import static org.apache.commons.compress.compressors.gzip.GzipUtils.getCompressedFileName;

/**
 * @author Stepan
 */
public final class GZLog extends LogFileWrapper {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;
    int filesToProcess = 0;
    private int filesProcessed;
    private InputStream curStream = null;

    public GZLog(File file, String baseDir) throws IOException {
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

        try (FileInputStream fis = new FileInputStream(getFile());
             GzipCompressorInputStream gis = new GzipCompressorInputStream(fis)){
            GzipParameters parameters = gis.getMetaData();
            FileInfo fi = new FileInfo(this, parameters.getFileName(), parameters.getBufferSize());
            if (fi.CheckLog(gis) != FileInfoType.type_Unknown) {
                goodFileFound = true;
                addFileInfo(fi, null);
            } else {
                Main.logger.info("Ignored " + getFile() + " / " + getFile().getAbsolutePath() );
            }

        }
        setIgnoreLog(goodFileFound);
    }

    @Override
    InputStream getInputStream(FileInfo fi) throws FileNotFoundException {
        try {
            FileInputStream fis = new FileInputStream(getFile());
            GzipCompressorInputStream gis = new GzipCompressorInputStream(fis);

            curStream = IOUtils.buffer(gis);
            return curStream;
        } catch (IOException e) {
            throw new FileNotFoundException(getFile().getAbsolutePath());
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
