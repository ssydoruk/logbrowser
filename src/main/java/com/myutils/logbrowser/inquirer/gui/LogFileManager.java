/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.myutils.logbrowser.inquirer.LogFile;
import com.myutils.logbrowser.inquirer.inquirer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Stepan
 */
public class LogFileManager {

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    HashSet<String> usedArchives = new HashSet<>();
    private final HashMap<String, LogFile> logFileStore = new HashMap();

//    HashMap<LogFile, ZipEntry> openedZIPFiles;
    void unpackFile(LogFile logFile) throws FileNotFoundException, IOException {

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
        ZipFile logArchive = new ZipFile(new File(logFile.getArcName()), ZipFile.OPEN_READ);
        (new File(logFile.getArcDir())).mkdirs();
        byte[] buffer = new byte[1024];
        ZipEntry entry = logArchive.getEntry(logFile.getFileName());

        if (entry != null) {
            InputStream curStream = logArchive.getInputStream(entry);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = curStream.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
        }
        logArchive.close();
        usedArchives.add(extractedFile);
    }

    public void clear() {
        for (String usedArchive : usedArchives) {
            try {
                FileUtils.deleteDirectory(new File(FilenameUtils.getFullPathNoEndSeparator(usedArchive)));
            } catch (IOException ex) {
//                logger.log(org.apache.logging.log4j.Level.FATAL, ex);
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
