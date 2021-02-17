/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import Utils.UnixProcess.ExtProcess;
import Utils.Util;
import com.myutils.logbrowser.inquirer.LogFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author Stepan
 */
public class EditorUnix extends ExternalEditor {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();

    private static ArrayList<String> errBuf;
    private static final String gvim = "/usr/local/bin/mvim";
    private static final String gvimServer = "LOGBROWSER";

    /**
     * Executes external command and returns stdout
     *
     * @param params
     * @return
     * @throws IOException
     */
    private static List<String> execGetStdOut(List params) throws IOException {
        ExtProcess ep = new ExtProcess(params);
        ep.startProcess(true, true);
        List<String> execOuts = ep.execOuts();
        errBuf = ep.getErrBuf();
        try {
//            logger.info("Waiting for process to stop");
            ep.waitFor();
        } catch (InterruptedException ex) {
            logger.error("", ex);
        }

        if (errBuf != null && !errBuf.isEmpty()) {
            logger.error("stdErr: [" + StringUtils.join(errBuf, ",") + "]");
            if (errBuf.get(0).startsWith("E247:")) {
                logger.error("E247: [" + StringUtils.join(errBuf, ",") + "]");
                return null;
            }
        }
        logger.debug("results: " + execOuts);
        return execOuts;
    }

    private static List<String> makeExecCmd(List cmd) {
        ArrayList<String> params = new ArrayList<>(3 + ((cmd == null) ? 0 : cmd.size()));
        params.addAll(Arrays.asList(gvim,
                "--servername",
                gvimServer));
        if (cmd != null) {
            params.addAll(cmd);
        }
        return params;
    }

    /**
     * execCommand add starting commands for the execution of external command
     * and then adds provided parameters
     *
     * @param cmd
     * @return
     * @throws IOException
     */
    private static List<String> execWaitSTDOut(List<String> cmd) throws IOException {
        return execGetStdOut(makeExecCmd(cmd));
    }

    private static void execReturn(List<String> cmd, boolean waitForEnd) throws IOException {
        ExtProcess ep = new ExtProcess(makeExecCmd(cmd));
//        if (startInThread) {
//            ep.startThread();
//        } else {
        ep.runProcess();
        try {
            if (waitForEnd) {
                ep.waitFor();
            }
//        }
        } catch (InterruptedException ex) {
            Logger.getLogger(EditorUnix.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * remoteExprInt executes command on GVIM and returns Integer result
     * provided by the command in STDout
     *
     * @param expr
     * @return - result, or null if first string in stdout is not integer (or
     * any other error)
     * @throws IOException
     */
    private static Integer remoteExprInt(String expr) throws IOException {
        List<String> execOuts = execWaitSTDOut(new ArrayList<>(Arrays.asList("--remote-expr",
                expr)));
        if (execOuts != null && !execOuts.isEmpty()) {
            return Util.intOrDef(execOuts.get(0), (Integer) null);
        }
        return null;
    }

    private static List<String> remoteSend(String expr) throws IOException {

        return execWaitSTDOut(Arrays.asList("--remote-send",
                expr));
    }
    HashMap<String, Character> lastBookmark = new HashMap<>();

    private void show() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void ediFile(LogFile lf) throws IOException {
        loadFile(lf.getFileName(), lf.getFileName());
    }

    protected boolean loadFile(String fileName, String fileModified) throws IOException {
//        runServer();
        Integer bufNumber = getBufNumber(fileName, fileModified);
        if (bufNumber != null) {
            setActive(bufNumber);
        } else {

            execReturn(Arrays.asList(//                "+",
//                "+':set nomodifiable'" //                                        + " | set hlsearch | set wrap | set noconfirm"
//                ,
                    "--remote-tab-wait-silent",
                    fileModified), false);
            confirmServer();

            long startTime = System.currentTimeMillis();
            long endTime;
            bufNumber = null;
            while (bufNumber == null) {

                bufNumber = getBufNumber(fileName, fileModified);
                if (bufNumber != null && bufNumber > 0) {
                    logger.debug("Loaded: " + bufNumber.toString());
                    setActive(bufNumber);
                } else {
                    bufNumber = null;
                    logger.debug("Not loaded yet: " + ((errBuf != null) ? errBuf : ""));
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }

                logger.debug("Waiting for file to load: [" + fileName + "]");
                endTime = System.currentTimeMillis();
                if (endTime - startTime > 10000) //  300 seconds hardcoded time to load file
                {
                    break;
                }
            }
            if (bufNumber != null) {
                setActive(bufNumber);
            } else {
                logger.error("Did not get buffer after 5 seconds");
            }
//
//            bufNumber = getBufNumber(fileName, fileModified);
//            if (bufNumber != null) {
//                logger.debug("Loaded: " + bufNumber.toString());
//                setActive(bufNumber);
//            } else {
//                logger.debug("Not loaded?: ");
//            }
            sendInitCommands();

        }
        return true;
    }

    @Override
    public boolean jumpToFile(String fileName, String fModified, int line, boolean makeActive, boolean modifiable) throws IOException {
        if (loadFile(fileName, fModified)) {
            sendCommand("<ESC>:"
                    + line + "<CR>");
            sendCommand("<ESC>m" + getNextLowerCaseBookmark(fileName)
                    + "<CR>");

//            sendCommand("<C-\\><C-N>:set nomodifiable<CR>"
//                    + ":set nomodifiable<CR>"
//                    + ":set ic<CR>"
//                    + ":set hlsearch<CR>"
//                    + ":set wrap<CR>"
//                    + ":set noconfirm<CR>"
//                    + ":" + line + "<CR>");
            return true;
        }
        return false;
    }

    private List<String> sendCommand(String cmd) throws IOException {

        return execWaitSTDOut(Arrays.asList("--remote-send",
                cmd));

    }

    private Integer getBufNumber(String fileName, String fileModified) throws IOException {
        Integer ret = remoteExprInt("bufnr('" + fileName + "')");
        if (ret != null && ret > 0) {
            return ret;
        } else {
            return null;
        }
    }

    private void setActive(int bufN) throws IOException {
        remoteSend("<ESC>:b" + bufN + "<CR>");
    }

    private void confirmServer() throws IOException {
        while (true) {
            List<String> execWaitSTDOut = execGetStdOut(Arrays.asList(gvim,
                    "--serverlist"));
            if (execWaitSTDOut != null && !execWaitSTDOut.isEmpty()
                    && execWaitSTDOut.contains(gvimServer)) {
                break;
            } else {
                LogManager.getLogger("").trace("Sleeping 20 ");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    LogManager.getLogger("").trace("Sleeping 20 ");
                }
            }

        }
    }

    private void sendInitCommands() throws IOException {
        sendCommand("<C-\\><C-N>:set nomodifiable<CR>"
                + "<C-\\><C-N>:set ic<CR>"
                + "<C-\\><C-N>:set hlsearch<CR>"
                + "<C-\\><C-N>:set wrap<CR>"
                + "<C-\\><C-N>:set noconfirm<CR>"
        //                + "<C-\\><C-N>:set nobackup<CR>"
        //                + "<C-\\><C-N>:set nowritebackup<CR>"
        //                + "<C-\\><C-N>:set noswapfile<CR>"       
        );
    }

    private char getNextLowerCaseBookmark(String fileName) {
        Character get = lastBookmark.get(fileName);
        if (get == null) {
            get = 'a';
        } else {
            get = new Character((char) (get.charValue() + 1));
        }
        lastBookmark.put(fileName, get);
        return get;

    }

}
