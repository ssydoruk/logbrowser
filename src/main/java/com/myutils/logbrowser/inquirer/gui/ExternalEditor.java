/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.Util;
import com.jacob.com.ComFailException;
import com.myutils.logbrowser.inquirer.LogFile;
import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ssydoruk
 */
public abstract class ExternalEditor {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    final private static Matcher regNum = Pattern.compile("(\\d+)").matcher("");
    private static ExternalEditor editor = null;
    HashMap<String, Integer> loadedFiles = new HashMap<>();

    public static void editFiles(ArrayList<String> shortFileNames) {
        for (String name : shortFileNames) {
            try {
                getEditor().ediFile(new LogFile(name, null));
            } catch (IOException ex) {
                inquirer.ExceptionHandler.handleException("", ex);
            } catch (ComFailException ex) {
                inquirer.ExceptionHandler.handleException("", ex);
            }
        }
    }

    static public boolean editorOpened() {
        return editor == null;
    }

    static public ExternalEditor getEditor() throws IOException, com.jacob.com.ComFailException {
        if (editor == null) {

            if (Utils.Util.getOS() == Util.OS.WINDOWS) {
                inquirer.logger.debug("is windows");
                switch (inquirer.getCr().getEditorType()) {
                    case VIMActiveX:
                        editor = new EditorWindows();
                        break;
                    case VIMCommandLine:
                        editor = new EditorVIMWindows();
                        break;

                    case NotepadPP:
                        editor = new EditorNotepadPP(inquirer.getCr().getNotepadPath());
                        break;

                    case NONE:
                    default:
                        throw new AssertionError();

                }

            } else {
                inquirer.logger.debug("is not windows");
                editor = new EditorUnix();
            }
        }
        return editor;
    }

    static public void openTextpad(String fileName, int line) {
        String[] cmd = {
            "C:\\Program Files\\TextPad 7\\TextPad.exe",
            "-q",
            "-u",
            fileName + "(" + line + ")"
        };
        try {
            Runtime.getRuntime().exec(cmd, null, null);
        } catch (IOException ex) {
            logger.error("fatal: ", ex);
            inquirer.ExceptionHandler.handleException("cannot run notepad", ex);
        }
    }

    static public void execCMD(String[] cmd) {
        try {
            if (logger.isDebugEnabled()) {
                StringBuilder s = new StringBuilder();
                for (String string : cmd) {
                    if (!string.isEmpty()) {
                        s.append(", ");
                    }
                    s.append(string);
                }
                logger.debug("Executing: " + s);
            }
            Runtime.getRuntime().exec(cmd, null, null);
        } catch (IOException ex) {
            logger.error("fatal: ", ex);
            inquirer.ExceptionHandler.handleException("cannot run notepad", ex);
        }

    }

    static public void openNotepad(String fileName, int line) {

        String[] cmd = {
            inquirer.getCr().getNotepadPath(),
            "-n" + line,
            fileName
        };
        try {
            Runtime.getRuntime().exec(cmd, null, null);
        } catch (IOException ex) {
            logger.error("fatal: ", ex);
            inquirer.ExceptionHandler.handleException("cannot run notepad", ex);
        }

    }

    abstract void ediFile(LogFile lf) throws IOException;

    public abstract boolean jumpToFile(String fileName, String fModified, int line, boolean makeActive, boolean modifiable) throws IOException;

    public boolean jumpToFile(LogFile fileName, int line, boolean makeActive, boolean modifiable) throws IOException {
        inquirer.getInq().getLfm().unpackFile(fileName);
        String fName = fileName.getUnarchivedName();
        String fileModified = fName.replace("#", "\\#");
        inquirer.logger.debug("orig: [" + fileModified + "] modified [" + fName + "]");

        return jumpToFile(fName, fileModified, line, makeActive, modifiable);
    }

    public boolean jumpToFile(LogFile fileName, int line, boolean makeActive) throws IOException {
        return jumpToFile(fileName, line, makeActive, false);
    }

    public boolean jumpToFile(LogFile fileName, int line) {
        try {
            return jumpToFile(fileName, line, true);
        } catch (IOException ex) {
            inquirer.ExceptionHandler.handleException(fileName.toString(), ex);
            return false;
        } catch (Exception ex) {
            inquirer.ExceptionHandler.handleException(fileName.toString(), ex);
            return false;
        }
    }

    protected int getIntFromArray(String s, int idx, int errValue) {
        if (s != null && !s.isEmpty()) {
            String[] split = StringUtils.split(s);
            if (split != null && split.length > idx) {
                try {
                    return Integer.parseInt(split[idx]);
                } catch (NumberFormatException e) {
                    inquirer.logger.error("Cannot parse number from [" + split[idx] + "] out of [" + s + "]");
                }
            }
        }
        return errValue;
    }

}
