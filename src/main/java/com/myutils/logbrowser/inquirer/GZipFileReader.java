/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * @author Stepan
 */
public class GZipFileReader extends RandomFileReader {

    private final LogFile logFile;
    long currentPos;
    private BufferedInputStream curStream;

    public GZipFileReader(LogFile fileName) throws IOException {
        this.logFile = fileName;
        inquirer.logger.debug("ZIPFileReader: " + logFile);
        openZIPStream();
    }

    @Override
    public int Read(long offset, int bytes, byte[] buf) throws IOException {

        inquirer.logger.trace("reading from " + logFile + " off:" + offset + " b:" + bytes);

        boolean needToReskip = true;
        if (offset >= currentPos) {
            long toSkip = offset - currentPos;
            long skipped = IOUtils.skip(curStream, toSkip);
            needToReskip = skipped != toSkip;

        }

        if (needToReskip) {
            /*
                Next two statements out come out of the fact that ZIP stream
                cannot jump backwards and reads garbage instead
             */
            curStream.close();
            openZIPStream();
            long skipped = IOUtils.skip(curStream, offset);
            if (skipped != offset) {
                inquirer.logger.error("Not skipped1: skipped:" + skipped + " off:" + offset);
            }

        }

        int bytesRead = IOUtils.read(curStream, buf, 0, bytes);
        currentPos = offset + bytes;
        if (bytesRead != bytes) {
            inquirer.logger.error("Read off:" + offset + " b:" + bytes + " ret: " + bytesRead + " unexpected file size file:"
                    + logFile);
//            bytesRead = curStream.read(buf, 0, bytes - bytesRead);
//            inquirer.logger.error("Read ing again:" + offset + " b:" + (bytes - bytesRead) + " ret: " + bytesRead);
        }
//        curStream.close();
        return bytesRead;
    }

    private void openZIPStream() throws IOException {
        try {
            FileInputStream fis = new FileInputStream(logFile.getArcName());
            GzipCompressorInputStream gis = new GzipCompressorInputStream(fis);

            curStream = IOUtils.buffer(gis);

        } catch (FileNotFoundException e) {
            curStream = null;
            inquirer.logger.error("File [" + logFile.getArcName() + "] not found", e);
            throw new IOException();
        }
    }

    @Override
    void close() throws IOException {
        inquirer.logger.trace("closing");
        curStream.close();
        curStream = null;
    }

    @Override
    public boolean isInited() {
        return curStream != null;
    }
}
