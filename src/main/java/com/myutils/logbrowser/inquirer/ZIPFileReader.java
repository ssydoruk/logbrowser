/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Stepan
 */
public class ZIPFileReader extends RandomFileReader {

    private final LogFile logFile;
    ZipFile logArchive;
    ZipEntry entry;
    long currentPos;
    private BufferedInputStream curStream;

    public ZIPFileReader(LogFile fileName) throws IOException {
        this.logFile = fileName;
        inquirer.logger.debug("ZIPFileReader: " + logFile);
        try {
            logArchive = new ZipFile(new File(logFile.getArcName()), ZipFile.OPEN_READ);
            entry = logArchive.getEntry(FilenameUtils.getName(logFile.getFileName()));
            openZIPStream();
        } catch (FileNotFoundException e) {
            inquirer.logger.error("File [" + fileName + "] not found", e);
            logArchive = null;
        }
    }

    private void openZIPStream() throws IOException {
        curStream = IOUtils.buffer(logArchive.getInputStream(entry));

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

    @Override
    void close() throws IOException {
        inquirer.logger.trace("closing");
        curStream.close();
        logArchive.close();
    }

    @Override
    public boolean isInited() {
        return logArchive != null;
    }
}
