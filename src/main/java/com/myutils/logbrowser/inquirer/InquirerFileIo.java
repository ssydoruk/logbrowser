package com.myutils.logbrowser.inquirer;

import java.io.*;
import java.util.*;

public class InquirerFileIo {

    static HashMap<LogFile, InqFile> logReaderHash = null;

    public static void doneIO() throws IOException {
        if (logReaderHash != null) {
            for (InqFile obj : logReaderHash.values()) {
                if (obj != null) {
                    obj.close();
                }
            }
            logReaderHash.clear();
        }
    }

    public static byte[] GetLogBytes(LogFile fileName,
            long offset,
            int bytes) throws Exception {
        InqFile retInqFile = null;

        if (logReaderHash == null) {
            logReaderHash = new HashMap();
        }
        if (logReaderHash.containsKey(fileName)) {
            retInqFile = logReaderHash.get(fileName);
        } else {
            try {
                retInqFile = new InqFile(fileName);
                if (retInqFile.isInited()) {
                    logReaderHash.put(fileName, retInqFile);
                } else {
                    retInqFile = null;
                    logReaderHash.put(fileName, null);
                }
            } catch (IOException iOException) {
                logReaderHash.put(fileName, null);
            }
        }

        return (retInqFile == null) ? null : retInqFile.GetLogBytes(offset, bytes);
    }

    public static String GetFileBytes(ILogRecord record) throws Exception {
        String logBytes = "";
        int bytes = record.GetFileBytes();
        try {
            byte[] recordBytes = null;
            try {
                recordBytes = GetLogBytes(
                        record.GetFileName(),
                        record.GetFileOffset(),
                        bytes);
            } catch (IOException iOException) {
                inquirer.logger.debug("Exception accessing file", iOException);
                if (!inquirer.getEe().isIgnoreFileAccessErrors()) {
                    throw iOException;
                } else {
                    recordBytes = null;
                }
            }
            if (recordBytes != null) {
                logBytes = new String(recordBytes,
                        0,
                        bytes);
            }
        } catch (Exception e) {
            inquirer.ExceptionHandler.handleException("Cannot read from file", e);
            throw e;
        }
        inquirer.logger.trace(logBytes);
        return logBytes;
    }

    public static String GetFileBytes(LogFile fileName,
            long offSet, int bytes) {
        String logBytes = "";
        try {
            if (fileName != null) {
                byte[] GetLogBytes = GetLogBytes(fileName, offSet, bytes);
                logBytes = new String(
                        GetLogBytes,
                        0,
                        bytes);
            }
        } catch (Exception e) {
            inquirer.logger.error("error reading: ", e);
        }
        inquirer.logger.trace(logBytes);
        return logBytes;
    }

    private static class InqFile {

        private long lastOffset = 0;
        private int lastBytes = 0;
        private final RandomFileReader rfr;
        byte[] pseudoLogBuffer;
        int pseudoLogBufferSize = 0;

        public InqFile(LogFile lf) throws IOException {
            if (lf.isText()) {
                rfr = new TextFileReader(lf.getFileName());
            } else {
                rfr = new ZIPFileReader(lf);
            }
        }

        public boolean isInited() {
            return rfr.isInited();
        }

        public byte[] GetLogBytes(long offset, int bytes) throws IOException {
            if (offset == lastOffset && bytes == lastBytes) {
                return pseudoLogBuffer;
            } else {
                if (pseudoLogBufferSize < bytes) {
                    pseudoLogBufferSize = (bytes / 1024 + 1) * 1024;
                    pseudoLogBuffer = new byte[pseudoLogBufferSize];
                }
                int i = rfr.Read(offset, bytes, pseudoLogBuffer);
                lastOffset = offset;
                lastBytes = i;
                return pseudoLogBuffer;

//                if (i == bytes) {
//                    return pseudoLogBuffer;
//                } else {
//                    byte[] bufTmp = new byte[i];
//                    System.arraycopy(pseudoLogBuffer, 0, bufTmp, 0, i);
//                    pseudoLogBufferSize = i;
//                    pseudoLogBuffer = new byte[i];
//                    System.arraycopy(bufTmp, 0, pseudoLogBuffer, 0, i);
//                    return pseudoLogBuffer;
//                }
            }
        }

        private void close() throws IOException {
            rfr.close();
            pseudoLogBuffer = null;
        }
    }

}
