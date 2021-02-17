/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer.gui;

import com.jacob.com.ComThread;
import com.jacob.com.LibraryLoader;
import com.myutils.logbrowser.inquirer.inquirer;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author ssydoruk
 */
public class WindowsSystemUtility {

    static boolean libraryLoaded = false;

    static void InitDLLActiveX() throws IOException {
        if (libraryLoaded == false) {
            /**
             * `System.getProperty("os.arch")` It'll tell us on which platform
             * Java Program is executing. Based on that we'll load respective
             * DLL file. Placed under same folder of program file(.java/.class).
             */
            //        String libFile = System.getProperty("os.arch").equals("amd64") ? "jacob-1.17-x64.dll" : "jacob-1.17-x86.dll";
            //        /* Read DLL file*/
            //        InputStream inputStream = OLETest.class.getResourceAsStream(libFile);
            //        /**
            //         * Step 1: Create temporary file under
            //         * <%user.home%>\AppData\Local\Temp\jacob.dll Step 2: Write contents of
            //         * `inputStream` to that temporary file.
            //         */
            //        File temporaryDll = File.createTempFile("jacob", ".dll");
            //        FileOutputStream outputStream = new FileOutputStream(temporaryDll);
            //        byte[] array = new byte[8192];
            //        for (int i = inputStream.read(array); i != -1; i = inputStream.read(array)) {
            //            outputStream.write(array, 0, i);
            //        }
            //        outputStream.close();
            //        /**
            //         * `System.setProperty(LibraryLoader.JACOB_DLL_PATH,
            //         * temporaryDll.getAbsolutePath());` Set System property same like
            //         * setting java home path in system.
            //         *
            //         * `LibraryLoader.loadJacobLibrary();` Load JACOB library in current
            //         * System.
            //         */
            //        System.setProperty(LibraryLoader.JACOB_DLL_PATH, temporaryDll.getAbsolutePath());
            String dll = (String) System.getProperties().get("jacob.dll");
            if (dll == null || dll.length() == 0) {
                try {
                    File f = new File(com.jacob.com.LibraryLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath().substring(1));
                    String absolutePath = f.getAbsolutePath();
                    String property = System.getProperty("sun.arch.data.model");
                    String jacobDll = "jacob-1.18-x64.dll";
                    if (property.equals("32")) {
                        jacobDll = "jacob-1.18-x86.dll";
                    }
                    dll = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator)) + File.separator + jacobDll;
                } catch (URISyntaxException ex) {
                    inquirer.ExceptionHandler.handleException("", ex);
                }

            }
            System.setProperty(LibraryLoader.JACOB_DLL_PATH, dll);
            ComThread.InitMTA(true);
            LibraryLoader.loadJacobLibrary();
            libraryLoaded = true;
        }
//        System.setProperty("com.jacob.autogc", "true");

    }

    public static void closeApp() {
        if (libraryLoaded) {
            LogManager.getLogger(WindowsSystemUtility.class.getName()).info("Terminate excel");

            ComThread.Release();
        }
    }

    public interface User32 extends StdCallLibrary {

        User32 INSTANCE = Native.loadLibrary("user32", User32.class);

        int GetWindowTextA(WinDef.HWND hWnd, byte[] lpString, int nMaxCount);

        WinDef.HWND GetForegroundWindow();  // add this

        WinDef.HWND WindowFromPoint(WinDef.POINT p);

        WinDef.BOOL SetWindowPos(WinDef.HWND hWnd, NativeLong hWndInsertAfter, int x, int y, int cx, int cy, int flags);

        WinDef.BOOL MoveWindow(WinDef.HWND hWnd, int x, int y, int nWidth, int nHeight, boolean bRepaint);

        WinDef.BOOL BringWindowToTop(WinDef.HWND hWnd);

        WinDef.HWND SetActiveWindow(WinDef.HWND hWnd);

        WinDef.BOOL SwitchToThisWindow(WinDef.HWND hWnd, boolean fAltTab);

        WinDef.HWND FindWindowA(String lpClassName, String lpWindowName);

    }

    public interface Kernel32 extends StdCallLibrary {

        Kernel32 INSTANCE = Native.loadLibrary("kernel32", Kernel32.class);

        int GetLastError();
    }

}
