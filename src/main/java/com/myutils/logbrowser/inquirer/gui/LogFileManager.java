/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.myutils.logbrowser.indexer.*;
import com.myutils.logbrowser.inquirer.LogFile;
import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

/**
 * @author Stepan
 */
public class LogFileManager {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private final HashMap<String, LogFile> logFileStore = new HashMap<>();
    HashSet<String> usedArchives = new HashSet<>();

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    //    HashMap<LogFile, ZipEntry> openedZIPFiles;
    void unpackFile(LogFile logFile) throws IOException {

        if (logFile.isText()) {
            return;
        }
        String extractedFile = logFile.getUnarchivedName();
        File newFile = new File(extractedFile);
        if (newFile.exists() && newFile.isFile()) {
            usedArchives.add(extractedFile);
            return;
        }
        inquirer.logger.debug("Unzipping " + logFile + "Out: " + extractedFile);
        String name = logFile.getArcName();
        (new File(logFile.getArcDir())).mkdirs();
        if (name.endsWith(".tgz") || name.endsWith(".tar.gz")) {
            extractTGZ(logFile.getArcName(), logFile.getFileName(), newFile);
        } else if (name.endsWith(".zip")) {
            extractZIP(logFile.getArcName(), logFile.getFileName(), newFile);
        } else if (name.endsWith(".gz")) {
            extractGZIP(logFile.getArcName(), logFile.getFileName(), newFile);
        }

        usedArchives.add(extractedFile);
    }

    private void extractGZIP(String arcName, String fileName, File newFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(arcName);
             GzipCompressorInputStream gis = new GzipCompressorInputStream(fis)){
            copyInputStreamToFile(gis, newFile);

        }
    }

    private void extractZIP(String arcName, String fileName, File newFile) throws IOException {
        ZipFile logArchive = null;
        try {
            logArchive = new ZipFile(new File(arcName), ZipFile.OPEN_READ);
            byte[] buffer = new byte[1024];
            ZipEntry entry = logArchive.getEntry(fileName);

            if (entry != null) {
                InputStream curStream = logArchive.getInputStream(entry);
                copyInputStreamToFile(curStream, newFile);
            }
        } finally {
            if (logArchive != null)
                logArchive.close();
        }
    }

    private void extractTGZ(String arcName, String fileName, File newFile) throws IOException {
        TarArchiveEntry entry = null;
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(arcName)))) {
            do {
                entry = tarInput.getNextTarEntry(); // You forgot to iterate to the next file
            } while (entry != null && !entry.isDirectory() && !entry.getName().equals(fileName));
            if (entry == null)
                throw new IOException("No file: " + fileName);
            copyInputStreamToFile(tarInput, newFile);
        }
    }

    public void clear() {
        logger.info("clearing out");
        for (String usedArchive : usedArchives) {
            try {
                logger.info("Archive [" + usedArchive + "] deleting [" + new File(FilenameUtils.getFullPathNoEndSeparator(usedArchive)) + "]");
                FileUtils.deleteDirectory(new File(FilenameUtils.getFullPathNoEndSeparator(usedArchive)));
            } catch (IOException ex) {
//                logger.error("fatal: ",  ex);
            }
        }
    }

    /**
     * Used to contain uniqueness of LogFile objects for application, because
     * this object is used as index in the hash of opened archives
     *
     * @param fileName
     * @param arcName
     * @return
     */
    public LogFile getLogFile(String fileName, String arcName) {
        String s = LogFile.makeString(fileName, arcName);
        LogFile ret = logFileStore.get(s);
        if (ret == null) {
            ret = new LogFile(fileName, arcName);
            logFileStore.put(s, ret);
        }
        return ret;

    }
}
