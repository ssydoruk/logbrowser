/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Stepan
 */
public class TextFileReader extends RandomFileReader {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    String fileName;

    long currentPos;
    FileInputStream reader;
    File file;

    public TextFileReader(String fileName) throws FileNotFoundException {
        try {
            file = new File(fileName);
            reader = new FileInputStream(file);
            currentPos = 0;
        } catch (FileNotFoundException e) {
            inquirer.logger.error("File [" + fileName + "] not found", e);
            reader = null;
        }
    }

    @Override
    public int Read(long offset, int bytes, byte[] buf) throws IOException {

        inquirer.logger.trace("reading from " + fileName + " off:" + offset + " b:" + bytes);
        if (offset >= currentPos) {
            reader.skip(offset - currentPos);

        } else {
            reader = new FileInputStream(file);
            reader.skip(offset);
        }

        int bytesRead = reader.read(buf, 0, bytes);
        if (bytesRead == bytes) {
            currentPos = offset + bytes;
        } else {
            inquirer.logger.error("Read off:" + offset + " b:" + bytes + " ret: " + bytesRead + " unexpected file size");
        }
        return bytesRead;

    }

    @Override
    void close() {
        try {
            reader.close();

        } catch (IOException ex) {
            logger.error("fatal: ", ex);
        }

    }

    @Override
    public boolean isInited() {
        return reader != null;
    }

}
