/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.indexer;

import java.io.*;

/**
 * @author Stepan
 */
public final class TextLog extends LogFileWrapper {

    private static final org.apache.logging.log4j.Logger logger = Main.logger;
    private FileInputStream stream = null;

    public TextLog(File file, String baseDir) throws IOException {
        super(file, baseDir);
        FileInfo fi = new FileInfo(file, this);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            if (fi.CheckLog(fileInputStream) == FileInfoType.type_Unknown) {
                Main.logger.info("File not recognized, ignoring: " + file.getAbsolutePath());
                setIgnoreLog(true);
            } else {
                setIgnoreLog(false);
                addFileInfo(fi, file);
            }
        }
    }

    @Override
    public void close() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ex) {
                Main.logger.error("", ex);
            }
            stream = null;
        }

    }

    @Override
    public void open() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    InputStream getInputStream(FileInfo aThis) throws FileNotFoundException {
        if(stream==null) {
            try {
                startParsing();
            }
            catch (Exception e){
                logger.error("Not able to start parsing"+e.getMessage());
                throw new FileNotFoundException(e.getMessage());
            }
        }
        return stream;
    }

    @Override
    void startParsing() throws Exception {
        try {
            stream = new FileInputStream(getFile());
        } catch (FileNotFoundException ex) {
            logger.error("fatal: ", ex);
            stream = null;
        }
    }

    @Override
    void doneParsing() throws Exception {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ex) {
                logger.error("fatal: ", ex);
            }
        }
    }

}
