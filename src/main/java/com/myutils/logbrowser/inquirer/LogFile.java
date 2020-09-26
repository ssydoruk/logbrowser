/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author Stepan
 */
public class LogFile {

    public static String makeString(String fileName, String arcName) {
        if (arcName == null) {
            return fileName;
        } else {
            return arcName + ":" + fileName;
        }
    }

    private String fileName;
    private String arcName;

    public LogFile(final String _fileName, final String _arcName) {
        this.fileName = inquirer.getFullLogName(_fileName);
        if (_arcName == null || _arcName.isEmpty()) {
            this.arcName = null;
        } else {
            this.arcName = inquirer.getFullLogName(_arcName);
        }
        if (Files.isRegularFile(Paths.get(this.fileName))) {
            if (arcName != null) {
                inquirer.logger.info(this.toString() + ": archive in DB but file exists. Ignored archive");
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
                        inquirer.logger.error(this.toString() + ": neither file nor archive exists (tried [" + name + "])");
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
                        inquirer.logger.error(this.toString() + ": neither file nor archive exists");
                    }
                }
            }

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
        ret.append(inquirer.getLogBrDir()).append(File.separator);
        String arc = FilenameUtils.getFullPathNoEndSeparator(arcName);
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
