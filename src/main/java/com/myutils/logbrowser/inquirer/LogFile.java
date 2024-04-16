/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Stepan
 */
public class LogFile {

    private String fileName;
    private String arcName;

    public LogFile(final String _fileName, final String _arcName) {
        this.fileName = _fileName;
        this.arcName = (StringUtils.isBlank(_arcName)) ? null : inquirer.getFullLogName(_arcName);

        if (Files.isRegularFile(Paths.get(this.fileName))) {
            if (arcName != null) {
                inquirer.logger.info(this + ": archive in DB but file exists. Ignored archive");
                arcName = null;
            }
        } else {
            if (arcName != null) {
                if (!Files.isRegularFile(Paths.get(this.arcName))) {
                    //assume archive was unzipped
                    String name = FilenameUtils.getFullPath(arcName) + Paths.get(fileName).getFileName();
                    if (Files.isRegularFile(Paths.get(name))) {
                        arcName = null;
                        fileName = name;

                    } else {
                        inquirer.logger.error(this + ": neither file nor archive exists (tried [" + name + "])");
                    }
                }
            } else {
                for (String ext : new String[]{"zip", "ZIP"}) {
                    StringBuilder a = new StringBuilder();
                    a.append(fileName).append(".").append(ext);
                    boolean arcFound = false;
                    if (Files.isRegularFile(Paths.get(a.toString()))) {
                        fileName = Paths.get(fileName).getFileName().toString();
                        arcName = a.toString();
                        arcFound = true;
                        break;
                    }
                    if (!arcFound) {
                        inquirer.logger.error(this + ": neither file nor archive exists");
                    }
                }
            }

        }
    }

    public RandomFileReader createFileReader() throws IOException {
        if (arcName == null) {
            return  new TextFileReader(fileName);
        }
        else {
            if (arcName.endsWith(".tgz") || arcName.endsWith(".tar.gz")) {
                return new TGZFileReader(this);
            } else if (arcName.endsWith(".zip")) {
                return new ZIPFileReader(this);
            } else if (arcName.endsWith(".gz")) {
                return new GZipFileReader(this);
            }
        }
        return null;
    }

    public static String makeString(String fileName, String arcName) {
        if (arcName == null) {
            return fileName;
        } else {
            return arcName + ":" + fileName;
        }
    }

    @Override
    public String toString() {
        return makeString(fileName, arcName);
    }

    public String getFileName() {
        return fileName;
    }

    public String getArcName() {
        return arcName;
    }

    public boolean isText() {
        return arcName == null;
    }

    public String getArcDir() {
//        inquirer.logger.info("ret: [" + FilenameUtils.getFullPathNoEndSeparator(arcName) + "--"
//                + File.separator + "++.++" + FilenameUtils.getName(getArcName()) + "}");
        StringBuilder ret = new StringBuilder();
        ret.append(inquirer.getEe().getLogBrDir()).append(File.separator);
        String arc = FilenameUtils.getPathNoEndSeparator(arcName);
        if (arc.startsWith("." + File.separator)) {
            ret.append(arc.substring(2));
        } else {
            ret.append(arc);
        }
        ret.append(File.separator).append(FilenameUtils.getName(getArcName()));
        return ret.toString();

    }

    public String getUnarchivedName() {
        if (arcName == null) {
            return fileName;
        } else {
            String ret = getArcDir() + File.separator + getFileName();
            inquirer.logger.info("Unarchive name: [" + ret + "]}");
            return ret;
        }

    }
}
