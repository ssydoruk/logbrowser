/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComFailException;
import com.jacob.com.Variant;
import com.myutils.logbrowser.inquirer.LogFile;
import com.myutils.logbrowser.inquirer.inquirer;
import com.sun.jna.platform.win32.WinDef;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;

import static com.jidesoft.utils.SystemInfo.getCurrentDirectory;
import static com.myutils.logbrowser.inquirer.ScreenInfo.getScreenHeight;
import static com.myutils.logbrowser.inquirer.ScreenInfo.getScreenWidth;


/**
 * @author Stepan
 */
public class EditorWindows extends ExternalEditor {

    protected ActiveXComponent vim;

    private WinDef.HWND hwnd;

    public EditorWindows() throws IOException {
        InitDLL();
    }

    private int getTabNo(int fileBufNo) {
        Variant invoke;
        int parseInt = 0;

        invoke = vim.invoke("Eval", "win_findbuf(" + fileBufNo + ")");

        parseInt = getIntFromArray(invoke.getString(), 0, -1);

        inquirer.logger.trace("Found window: " + invoke.getString());
        invoke = vim.invoke("Eval", "win_id2tabwin(" + parseInt + ")");

        parseInt = getIntFromArray(invoke.getString(), 0, -1);
        inquirer.logger.trace("Found tab: " + parseInt);
        return parseInt;
    }

    private void setLocation() {
        for (int i = 0; i < 10; i++) {
            try {
                Variant vx = vim.invoke("Eval", "getwinposx()");
                int wx = Integer.parseInt(vx.getString());

                Variant vy = vim.invoke("Eval", "getwinposy()");
                int wy = Integer.parseInt(vy.getString());

                inquirer.logger.trace("pos: x[" + wx + "] " + " y[" + wy + "] ");
                break;
            } catch (ComFailException e) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    break;
                }
            }

        }
        byte[] windowText = new byte[512];

//        hwnd = User32.INSTANCE.GetForegroundWindow(); // then you can call it!
        hwnd = WindowsSystemUtility.User32.INSTANCE.FindWindowA("Vim", null); // then you can call it!
//        inquirer.logger.info("hwnd: " + ((hwnd != null) ? hwnd.toString() : "[null]"));

        if (hwnd != null) {
//            CheckWinRet(User32.INSTANCE.GetWindowTextA(hwnd, windowText, 512) > 0);
//            inquirer.logger.trace("Window name: " + Native.toString(windowText));
//
//            POINT p1 = new POINT();
//            p1.x = wx;
//            p1.y = wy;
//            HWND h = User32.INSTANCE.WindowFromPoint(p1);
//            inquirer.logger.trace("handle: " + h);

//        b = User32.INSTANCE.SetWindowPos(hwnd, h1, 0, 0, getScreenHeight(0) / 2, getScreenWidth(0), 0);
            CheckWinRet(WindowsSystemUtility.User32.INSTANCE.MoveWindow(hwnd, 0, 0, getScreenWidth(1), getScreenHeight(1) / 2, true).booleanValue());
//        Variant invoke = vim.invoke("GetHwnd");
//        int wHandle = invoke.;
//        inquirer.logger.trace("handle: [" + invoke + "]");
        } else {
            CheckWinRet(false);
            inquirer.logger.error("not found VIM window ");
        }
    }

    private void CheckWinRet(boolean booleanValue) {
        if (!booleanValue) {
            int err = WindowsSystemUtility.Kernel32.INSTANCE.GetLastError();
            if (err != 0) {
                inquirer.logger.error("Windows error: " + err);
            }
        }
    }

    private void InitDLL() throws IOException, com.jacob.com.ComFailException {
        WindowsSystemUtility.InitDLLActiveX();
        vim = new ActiveXComponent("Vim.Application");
        setLocation();
        loadedFiles.clear();
        // unwrap long lines

        vim.invoke("SendKeys", "<Esc>:set ignorecase<Enter>");
        vim.invoke("SendKeys", ":set smartcase<Enter>");
        vim.invoke("SendKeys", ":set shortmess=I<Enter>");
        vim.invoke("SendKeys", ":set noerrorbells visualbell t_vb=<Enter>");
        vim.invoke("SendKeys", ":autocmd GUIEnter * set visualbell t_vb=<Enter>");
        vim.invoke("SendKeys", ":set wrap! <Enter>");
        // do not new ^m for SIP Server that shows it for SIP messages
        //         vim.invoke("SendKeys", ":match Ignore /\\r$/<Enter>");
        // another attempt to hide ^M
        //        vim.invoke("SendKeys", ":e ++ff=dos<Enter>");
        // hide toolbar
        vim.invoke("SendKeys", ":set guioptions -=T<Enter>");
        vim.invoke("SendKeys", ":cd " + getCurrentDirectory() + "<Enter>");
        vim.invoke("SendKeys", ":nmap<C-F> promptfind<Enter>");
        vim.invoke("SendKeys", "x!<Enter>");
//        vim.invoke("SendKeys", ":set switchbuf=usetab<Enter>");

    }

    private int getFileBufNo(String fileName) throws IOException {
        Variant invoke;
        boolean docClosed = false;
        Integer bufNo = loadedFiles.get(fileName);
        inquirer.logger.debug("getFileBufNo: " + FilenameUtils.getBaseName(fileName) + " bufNo: " + bufNo);
        if (bufNo != null && bufNo.intValue() > 0) {
            try {
                invoke = vim.invoke("Eval", "bufloaded(" + bufNo + ")");
                int parseInt = Integer.parseInt(invoke.getString());
                inquirer.logger.debug("bufloaded: " + parseInt);
                if (parseInt == 1) {
                    return bufNo;
                } else {
                    docClosed = true;
                }
            } catch (com.jacob.com.ComFailException e) {
                inquirer.logger.debug("bufloaded failed. Need to reinit", e);
                InitDLL();
                return -1;
            }
        }
        try {
            inquirer.logger.debug("Eval bufnr for [" + fileName + "]");
            invoke = vim.invoke("Eval", "bufnr('" + fileName + "')");
            int parseInt = Integer.parseInt(invoke.getString());
            inquirer.logger.debug("bufnr: " + invoke.getString() + " parseInt:" + parseInt);
            if (parseInt >= 0) {
                loadedFiles.put(fileName, parseInt);
            }
            return parseInt;
        } catch (com.jacob.com.ComFailException e) {
            inquirer.logger.debug("Eval: bufnr. Need to reinit", e);
            InitDLL();
            return -1;
        }
//        invoke = vim.invoke("Eval", "bufloaded('" + fileName + "')");
//        inquirer.logger.trace("fileLoaded: " + invoke.getString());
//        int parseInt = Integer.parseInt(invoke.getString());
//        return parseInt == 1;
    }

    protected boolean loadFile(String fileName, String fileModified) throws IOException {
        int fileBufNo = getFileBufNo(fileName);

        vim.invoke("SendKeys", "<Esc>");
        if (fileBufNo < 0) {
//            vim.invoke("SendKeys", "<Esc>:enew! "+ "<Enter>");
            vim.invoke("SendKeys", "<Esc>:tabnew " + fileModified + "<Enter>");
//            vim.invoke("SendKeys", "<Esc>:tabnew<Enter>");
//            vim.invoke("SendKeys", "<Esc>:edit! " + fileModified + "<Enter>");
//            vim.invoke("SendKeys", ":view" + "<Enter>");

            long startTime = System.currentTimeMillis();
            long endTime;
            while (getFileBufNo(fileModified) < 0) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                }
                inquirer.logger.debug("Waiting for file to load: [" + fileName + "]");
                endTime = System.currentTimeMillis();
                if (endTime - startTime > 60000) //  300 seconds hardcoded time to load file
                {
                    return false;
                }
            }
            // another attempt to hide ^M
//            vim.invoke("SendKeys", ":e ++ff=dos<Enter>");

        } else {
            inquirer.logger.trace("switching to buffer: [" + fileBufNo + "] for file [" + fileName + "]");
            int tabNo = getTabNo(fileBufNo);
            if (tabNo > 0) {//            vim.invoke("SendKeys", ":set switchbuf=usetab<Enter>");
                vim.invoke("SendKeys", "<Esc>" + tabNo + "gt<Enter>");
            } else {// doc was closed, reopen it
                vim.invoke("SendKeys", "<Esc>:tabnew " + fileModified + "<Enter>");
            }
        }
        return true;
    }

    protected void show() {
        vim.invoke("SetForeground");
        if (hwnd != null) {
            CheckWinRet(WindowsSystemUtility.User32.INSTANCE.SetActiveWindow(hwnd) != null);
            CheckWinRet(WindowsSystemUtility.User32.INSTANCE.BringWindowToTop(hwnd).booleanValue());
            WindowsSystemUtility.User32.INSTANCE.SwitchToThisWindow(hwnd, true);
        }
    }

    @Override
    public boolean jumpToFile(String fileName, String fModified, int line, boolean makeActive, boolean modifiable) throws IOException {
        if (loadFile(fileName, fModified)) {
            //            :set nomodifiable #this will open files in read-only mode
            //:set ic #this will enable always ignore case search
            if (modifiable) {
                vim.invoke("SendKeys", "<Esc>:set modifiable<Enter>");
            } else {
                vim.invoke("SendKeys", "<Esc>:set nomodifiable<Enter>");
            }
            vim.invoke("SendKeys", ":set ic<Enter>");
            vim.invoke("SendKeys", ":set hlsearch<Enter>");
            vim.invoke("SendKeys", ":set wrap<Enter>");
            vim.invoke("SendKeys", ":set noconfirm<Enter>");
            //            inquirer.logger.info("-9- ");
            //            vim.invoke("SendKeys", ":bufdo e<Enter>");
            //            inquirer.logger.info("-10- ");
            vim.invoke("SendKeys", ":" + line + "<Enter>");
            //            vim.invoke("SendKeys", "zt<Enter>");
            if (makeActive) {
                show();
            }
            //            Thread.sleep(10000);
            //            callFun();
            return true;
        }
        return false;
    }

    @Override
    void ediFile(LogFile lf) throws IOException {
        jumpToFile(lf, 0, true, true);
        //JOptionPane.showMessageDialog(null, "Fix me!!!");
        //this will clean extra ^M from SIP messages
        vim.invoke("SendKeys", ":e ++ff=dos<Enter>");

    }

}
